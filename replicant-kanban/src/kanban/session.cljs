(ns kanban.session
  "CLJS-only dispatch for Kanban.
   Re-exports state management from session-state.cljc and adds DOM event handling."
  (:require [odoyle.rules :as o]
            [clojure.walk :as walk]
            [clojure.string :as str]
            [kanban.forms :as forms]
            [kanban.id :as id]
            [kanban.session-state :as state]))

;; =============================================================================
;; Re-exports from session-state.cljc
;; =============================================================================

(def columns state/columns)
(def tags state/tags)
(def *session state/*session)
(def *send! state/*send!)
(def *request! state/*request!)
(def *connect! state/*connect!)
(def init-session! state/init-session!)
(def query-all state/query-all)
(def query-one state/query-one)
(def query-tasks state/query-tasks)
(def query-transient state/query-transient)
(def ->render-state state/->render-state)
(def sync-tasks! state/sync-tasks!)

;; =============================================================================
;; O'Doyle Helpers - must call fire-rules after insert/retract
;; =============================================================================

(defn- insert! [id attr val]
  (swap! *session
         (fn [s] (-> s (o/insert id attr val) o/fire-rules))))

(defn- retract! [id attr]
  (swap! *session
         (fn [s] (-> s (o/retract id attr) o/fire-rules))))

(defn- update-session! [f]
  (swap! *session (fn [s] (-> (f s) o/fire-rules))))

;; =============================================================================
;; Action Insertion (Game Dev Pattern)
;; =============================================================================
;; Instead of complex dispatch logic, simply insert action facts.
;; O'Doyle rules in session_state.cljc process these actions.

(defn- insert-action!
  "Insert an action fact for O'Doyle rules to process.
   The rules will consume (retract) the action after processing."
  [action-type data]
  (let [action-id (keyword "action" (id/random-id))]
    (swap! *session
           (fn [s]
             (-> (reduce-kv (fn [sess k v] (o/insert sess action-id k v))
                            (o/insert s action-id :action/type action-type)
                            data)
                 o/fire-rules)))))

;; =============================================================================
;; Connection State (uses O'Doyle action pattern)
;; =============================================================================
;; Called by rheon-client when connection state changes

(defn ^:export connection-opened!
  "Called when Rheon connection opens."
  []
  (insert-action! :connection-opened {}))

(defn ^:export connection-closed!
  "Called when Rheon connection closes."
  []
  (insert-action! :connection-closed {}))

(defn ^:export connection-error!
  "Called when Rheon connection errors."
  [error]
  (insert-action! :connection-error {:action/error (str error)}))

(defn ^:export connection-reconnecting!
  "Called when Rheon is attempting to reconnect."
  []
  (insert-action! :connection-reconnecting {}))

;; =============================================================================
;; Placeholder Resolution
;; =============================================================================

(def placeholder-resolvers
  {:event/form-data
   (fn [{:replicant/keys [^js dom-event]}]
     (some-> dom-event .-target forms/gather-form-data))

   :random/id
   (fn [_] (id/random-id))

   :event/target.value
   (fn [{:replicant/keys [^js dom-event]}]
     (some-> dom-event .-target .-value))

   :event/target.value.keyword
   (fn [{:replicant/keys [^js dom-event]}]
     (some-> dom-event .-target .-value keyword))})

(defn- resolve-placeholders
  "Replace placeholder keywords with their resolved values."
  [dispatch-data actions]
  (walk/postwalk
   (fn [x]
     (if-let [resolver (and (keyword? x) (placeholder-resolvers x))]
       (resolver dispatch-data)
       x))
   actions))

;; =============================================================================
;; Dispatch - Simple case-based action handler
;; =============================================================================

(defn dispatch!
  "Handle actions directly. Called by Replicant on DOM events."
  [dispatch-data actions]
  (let [resolved (resolve-placeholders dispatch-data actions)]
    (doseq [[action-type & args] resolved]
      (case action-type
        ;; Task form - handled by O'Doyle rules
        :actions/open-new-task-form
        (let [[status] args]
          (insert-action! :form-open {:action/status status}))

        :actions/close-new-task-form
        (let [[status] args]
          (insert-action! :form-close {:action/status status}))

        :actions/add-task
        (let [[form-data task-id] args
              now (query-one @*session :system :now)
              tags (->> (str/split (or (:tags form-data) "") #" ")
                        (mapv str/trim)
                        (filter not-empty)
                        (mapv #(keyword "tags" %)))
              task (cond-> (-> (dissoc form-data :tags)
                               (assoc :task/id task-id)
                               (assoc :task/created-at now))
                     (not-empty tags) (assoc :task/tags (set tags)))]
          (when-let [send! @*send!]
            (send! {:command/kind :commands/create-task
                    :command/data task})))

        ;; Modal - handled by O'Doyle rules
        :actions/close-modal
        (insert-action! :modal-close {})

        :actions/open-edit-modal
        (let [[task] args]
          (insert-action! :modal-open {:action/task task}))

        :actions/update-editing
        (let [[field-key value] args]
          (insert-action! :update-editing {:action/field field-key
                                           :action/value value}))

        :actions/save-task
        (let [task (query-one @*session :ui :editing-task)]
          (when task
            ;; Send command to server (browser API - must stay in dispatch)
            (when-let [send! @*send!]
              (send! {:command/kind :commands/update-task
                      :command/data task}))
            ;; Close modal via O'Doyle rule
            (insert-action! :modal-close {})))

        ;; Task status
        :actions/set-task-status
        (let [[task-id status] args]
          (when-let [send! @*send!]
            (send! {:command/kind :commands/set-task-status
                    :command/data {:task/id task-id :task/status status}})))

        ;; Task expansion - handled by O'Doyle rules
        :actions/expand-task
        (let [[task-id] args]
          (insert-action! :expand-task {:action/task-id task-id}))

        :actions/collapse-task
        (let [[task-id] args]
          (insert-action! :collapse-task {:action/task-id task-id}))

        ;; Inline editing - handled by O'Doyle rules
        :actions/start-inline-edit
        (let [[task] args]
          (insert-action! :start-inline-edit {:action/task task}))

        :actions/cancel-inline-edit
        (insert-action! :cancel-inline-edit {})

        :actions/save-inline-edit
        (let [task (query-one @*session :ui :editing-task)]
          (when task
            ;; Send command to server
            (when-let [send! @*send!]
              (send! {:command/kind :commands/update-task
                      :command/data task}))
            ;; Clear inline edit state via O'Doyle rule
            (insert-action! :save-inline-edit {})))

        ;; History
        :actions/open-history
        (let [[task-id] args]
          (when-let [request! @*request!]
            (request!
             {:command/kind :commands/get-task-history
              :command/data {:task/id task-id}}
             (fn [response]
               (when (:success? response)
                 (update-session!
                  #(-> %
                       (o/insert :ui :task-history (:history response))
                       (o/insert :ui :history-index 0))))))))

        ;; History navigation - handled by O'Doyle rules
        :actions/history-prev
        (insert-action! :history-prev {})

        :actions/history-next
        (insert-action! :history-next {})

        :actions/history-goto
        (let [[target-idx] args]
          (insert-action! :history-goto {:action/target-index target-idx}))

        ;; Generic state - handled by O'Doyle rules
        :actions/assoc-in
        (let [[path v] args]
          (insert-action! :assoc-transient {:action/path path
                                            :action/value v}))

        :actions/dissoc-in
        (let [[path] args]
          (insert-action! :dissoc-transient {:action/path path}))

        ;; DOM effects
        :actions/prevent-default
        (some-> dispatch-data :replicant/dom-event .preventDefault)

        :actions/start-drag-move
        (when-let [event (:replicant/dom-event dispatch-data)]
          (js/requestAnimationFrame
           #(-> event .-target .-classList (.add "invisible")))
          (set! (.-effectAllowed (.-dataTransfer event)) "move"))

        :actions/end-drag-move
        (some-> dispatch-data :replicant/dom-event
                .-target .-classList (.remove "invisible"))

        ;; Connection
        :actions/connect
        (when-let [connect! @*connect!]
          (connect!))

        ;; Command dispatch
        :actions/command
        (let [[command] args]
          (when-let [send! @*send!]
            (send! command)))

        ;; Flash (temporary state)
        :actions/flash
        (let [[ms path v] args
              [entity-key & attr-path] path
              entity-id (if (keyword? entity-key)
                          entity-key
                          (keyword "transient" (str entity-key)))
              attr (if (= 1 (count attr-path))
                     (first attr-path)
                     (vec attr-path))]
          (insert! entity-id attr v)
          (js/setTimeout #(retract! entity-id attr) ms))

        ;; Default - unknown action
        (println "Unknown action:" action-type)))))
