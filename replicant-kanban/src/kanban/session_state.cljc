(ns kanban.session-state
  "O'Doyle session state for Kanban.

   Simple architecture:
   - Session atom holds all state as facts
   - Watch session for rendering
   - Dispatch is in kanban.session (CLJS-only)"
  (:require [odoyle.rules :as o]))

;; =============================================================================
;; Session Query Helper (for use inside :then blocks)
;; =============================================================================
;; Since {:then false} syntax is not supported, we query state inside :then blocks

(defn session-get
  "Query a single attribute from an entity in the given session."
  [session entity-id attr]
  (some (fn [[id a v]]
          (when (and (= id entity-id) (= a attr))
            v))
        (o/query-all session)))

;; =============================================================================
;; Static Data
;; =============================================================================

(def columns
  [{:column/status :status/open
    :column/title "Open"
    :column/add-new? true}
   {:column/status :status/in-progress
    :column/title "In Progress"
    :column/limit 3}
   {:column/status :status/closed
    :column/title "Closed"
    :column/sort-by :task/changed-status-at
    :column/sort-order :desc}])

(def tags
  [{:tag/ident :tags/feature :tag/style {:bg "bg-info" :fg "text-info-content"}}
   {:tag/ident :tags/bug :tag/style {:bg "bg-error" :fg "text-error-content"}}
   {:tag/ident :tags/ui :tag/style {:bg "bg-secondary" :fg "text-secondary-content"}}
   {:tag/ident :tags/theme :tag/style {:bg "bg-accent" :fg "text-accent-content"}}
   {:tag/ident :tags/automation :tag/style {:bg "bg-success" :fg "text-success-content"}}
   {:tag/ident :tags/accessibility :tag/style {:bg "bg-warning" :fg "text-warning-content"}}
   {:tag/ident :tags/data :tag/style {:bg "bg-primary" :fg "text-primary-content"}}
   {:tag/ident :tags/animation :tag/style {:bg "bg-neutral" :fg "text-neutral-content"}}])

;; =============================================================================
;; O'Doyle Query Rules
;; =============================================================================
;; O'Doyle requires rules to be defined for facts to be stored.
;; Facts that don't match any rule are discarded.
;; Rules without :then blocks act as "getters" for query-all.

;; =============================================================================
;; Action Processing Rules
;; =============================================================================
;; Pattern: Actions are inserted as facts, rules process them and retract (consume)
;; Use {:then false} for read-only bindings to prevent infinite loops

(def action-rules
  (o/ruleset
   {;; -------------------------------------------------------------------------
    ;; Task Expansion
    ;; -------------------------------------------------------------------------
    ::action-expand-task
    [:what
     [?a :action/type :expand-task]
     [?a :action/task-id ?task-id]
     :then
     (let [transient-id (keyword "transient" (str ?task-id))]
       (-> o/*session*
           (o/insert transient-id :expanded? true)
           (o/retract ?a :action/type)
           (o/retract ?a :action/task-id)
           o/reset!))]

    ::action-collapse-task
    [:what
     [?a :action/type :collapse-task]
     [?a :action/task-id ?task-id]
     :then
     (let [transient-id (keyword "transient" (str ?task-id))]
       (-> o/*session*
           (o/insert transient-id :expanded? false)
           (o/retract ?a :action/type)
           (o/retract ?a :action/task-id)
           o/reset!))]

    ;; -------------------------------------------------------------------------
    ;; Task Form (New Task)
    ;; -------------------------------------------------------------------------
    ::action-form-open
    [:what
     [?a :action/type :form-open]
     [?a :action/status ?status]
     :then
     (let [transient-id (keyword "transient" (name ?status))]
       (-> o/*session*
           (o/insert transient-id :add? true)
           (o/retract ?a :action/type)
           (o/retract ?a :action/status)
           o/reset!))]

    ::action-form-close
    [:what
     [?a :action/type :form-close]
     [?a :action/status ?status]
     :then
     (let [transient-id (keyword "transient" (name ?status))]
       (-> o/*session*
           (o/retract transient-id :add?)
           (o/retract ?a :action/type)
           (o/retract ?a :action/status)
           o/reset!))]

    ;; -------------------------------------------------------------------------
    ;; Edit Modal
    ;; -------------------------------------------------------------------------
    ::action-modal-open
    [:what
     [?a :action/type :modal-open]
     [?a :action/task ?task]
     :then
     (-> o/*session*
         (o/insert :ui :editing-task ?task)
         (o/retract ?a :action/type)
         (o/retract ?a :action/task)
         o/reset!)]

    ::action-modal-close
    [:what
     [?a :action/type :modal-close]
     :then
     (let [session o/*session*
           ;; Helper to safely retract
           safe-retract (fn [s id attr]
                          (if (some (fn [[i a _]] (and (= i id) (= a attr)))
                                    (o/query-all s))
                            (o/retract s id attr)
                            s))]
       (-> session
           (safe-retract :ui :editing-task)
           (safe-retract :ui :task-history)
           (safe-retract :ui :history-index)
           (o/retract ?a :action/type)
           o/reset!))]

    ::action-update-editing
    [:what
     [?a :action/type :update-editing]
     [?a :action/field ?field]
     [?a :action/value ?value]
     :then
     (let [task (session-get o/*session* :ui :editing-task)]
       (when task
         (-> o/*session*
             (o/insert :ui :editing-task (assoc task ?field ?value))
             (o/retract ?a :action/type)
             (o/retract ?a :action/field)
             (o/retract ?a :action/value)
             o/reset!)))]

    ;; -------------------------------------------------------------------------
    ;; Inline Edit Mode
    ;; -------------------------------------------------------------------------
    ::action-start-inline-edit
    [:what
     [?a :action/type :start-inline-edit]
     [?a :action/task ?task]
     :then
     (let [task-id (:task/id ?task)
           transient-id (keyword "transient" (str task-id))]
       (-> o/*session*
           ;; Store original task for editing
           (o/insert :ui :editing-task ?task)
           (o/insert :ui :inline-editing-id task-id)
           (o/retract ?a :action/type)
           (o/retract ?a :action/task)
           o/reset!))]

    ::action-cancel-inline-edit
    [:what
     [?a :action/type :cancel-inline-edit]
     :then
     (let [session o/*session*
           safe-retract (fn [s id attr]
                          (if (some (fn [[i a _]] (and (= i id) (= a attr)))
                                    (o/query-all s))
                            (o/retract s id attr)
                            s))]
       (-> session
           (safe-retract :ui :editing-task)
           (safe-retract :ui :inline-editing-id)
           (o/retract ?a :action/type)
           o/reset!))]

    ::action-save-inline-edit
    [:what
     [?a :action/type :save-inline-edit]
     :then
     ;; Retract both inline edit state AND editing-task to prevent modal from opening
     (let [session o/*session*
           safe-retract (fn [s id attr]
                          (if (some (fn [[i a _]] (and (= i id) (= a attr)))
                                    (o/query-all s))
                            (o/retract s id attr)
                            s))]
       (-> session
           (safe-retract :ui :inline-editing-id)
           (safe-retract :ui :editing-task)
           (o/retract ?a :action/type)
           o/reset!))]

    ;; -------------------------------------------------------------------------
    ;; History Navigation
    ;; -------------------------------------------------------------------------
    ;; Note: These rules query state inside :then blocks to avoid {:then false} syntax

    ::action-history-prev
    [:what
     [?a :action/type :history-prev]
     :then
     (let [idx (or (session-get o/*session* :ui :history-index) 0)
           hist (session-get o/*session* :ui :task-history)]
       (-> o/*session*
           (cond-> (and hist (< idx (dec (count hist))))
             (o/insert :ui :history-index (inc idx)))
           (o/retract ?a :action/type)
           o/reset!))]

    ::action-history-next
    [:what
     [?a :action/type :history-next]
     :then
     (let [idx (or (session-get o/*session* :ui :history-index) 0)]
       (-> o/*session*
           (cond-> (pos? idx)
             (o/insert :ui :history-index (dec idx)))
           (o/retract ?a :action/type)
           o/reset!))]

    ::action-history-goto
    [:what
     [?a :action/type :history-goto]
     [?a :action/target-index ?target-idx]
     :then
     (let [hist (session-get o/*session* :ui :task-history)
           valid? (and hist (>= ?target-idx 0) (< ?target-idx (count hist)))]
       (-> o/*session*
           (cond-> valid?
             (o/insert :ui :history-index ?target-idx))
           (o/retract ?a :action/type)
           (o/retract ?a :action/target-index)
           o/reset!))]

    ;; -------------------------------------------------------------------------
    ;; Generic State (assoc-in / dissoc-in for dragging etc)
    ;; -------------------------------------------------------------------------
    ::action-assoc-transient
    [:what
     [?a :action/type :assoc-transient]
     [?a :action/path ?path]
     [?a :action/value ?v]
     :then
     (let [[entity-key & attr-path] ?path
           entity-id (cond
                       (= :transient entity-key)
                       (keyword "transient" (name (first attr-path)))

                       (keyword? entity-key)
                       entity-key

                       :else
                       (keyword "transient" (str entity-key)))
           attr (cond
                  (= :transient entity-key)
                  (if (> (count attr-path) 1) (second attr-path) :value)

                  (= 1 (count attr-path))
                  (first attr-path)

                  :else
                  (vec attr-path))]
       (-> o/*session*
           (o/insert entity-id attr ?v)
           (o/retract ?a :action/type)
           (o/retract ?a :action/path)
           (o/retract ?a :action/value)
           o/reset!))]

    ::action-dissoc-transient
    [:what
     [?a :action/type :dissoc-transient]
     [?a :action/path ?path]
     :then
     (let [[entity-key & attr-path] ?path
           entity-id (cond
                       (= :transient entity-key)
                       (keyword "transient" (name (first attr-path)))

                       (keyword? entity-key)
                       entity-key

                       :else
                       (keyword "transient" (str entity-key)))
           attr (cond
                  (= :transient entity-key)
                  (if (> (count attr-path) 1) (second attr-path) :value)

                  (= 1 (count attr-path))
                  (first attr-path)

                  :else
                  (vec attr-path))]
       (-> o/*session*
           (o/retract entity-id attr)
           (o/retract ?a :action/type)
           (o/retract ?a :action/path)
           o/reset!))]

    ;; -------------------------------------------------------------------------
    ;; Connectivity Actions
    ;; -------------------------------------------------------------------------
    ::action-connection-opened
    [:what
     [?a :action/type :connection-opened]
     :then
     (-> o/*session*
         (o/insert :system :connected? true)
         (o/insert :system :connection-status :connected)
         (o/retract ?a :action/type)
         o/reset!)]

    ::action-connection-closed
    [:what
     [?a :action/type :connection-closed]
     :then
     (-> o/*session*
         (o/insert :system :connected? false)
         (o/insert :system :connection-status :disconnected)
         (o/retract ?a :action/type)
         o/reset!)]

    ::action-connection-error
    [:what
     [?a :action/type :connection-error]
     [?a :action/error ?error]
     :then
     (-> o/*session*
         (o/insert :system :connected? false)
         (o/insert :system :connection-status :error)
         (o/insert :system :connection-error ?error)
         (o/retract ?a :action/type)
         (o/retract ?a :action/error)
         o/reset!)]

    ::action-connection-reconnecting
    [:what
     [?a :action/type :connection-reconnecting]
     :then
     (-> o/*session*
         (o/insert :system :connection-status :reconnecting)
         (o/retract ?a :action/type)
         o/reset!)]}))

;; =============================================================================
;; Query Rules (no :then - for reading state)
;; =============================================================================

(def rules
  (o/ruleset
    {;; Catch-all rule - stores ALL facts for query-all without rule name
     ::all-facts
     [:what
      [id attr value]]}))

;; =============================================================================
;; Derived Facts Rules (with :then - compute aggregate stats)
;; =============================================================================
;; These rules compute derived data from task facts and insert them as new facts.
;; Use {:then false} for read-only bindings to prevent infinite loops.

(def derived-rules
  (o/ruleset
   {;; Derive task count and priority distribution per status
    ;; Uses :then-finally which runs after all rule firings complete
    ::derive-column-stats
    [:what
     [?id :task/status ?status]
     :then-finally
     (let [;; Get all tasks with their status and priority
           all-facts (o/query-all o/*session*)
           task-facts (->> all-facts
                           (filter (fn [[id attr _]]
                                     (and (or (string? id)
                                              #?(:cljs (uuid? id)
                                                 :clj (instance? java.util.UUID id)))
                                          (#{:task/status :task/priority} attr))))
                           (group-by first))
           ;; Build task maps with status and priority
           tasks (->> task-facts
                      (map (fn [[_id facts]]
                             (reduce (fn [m [_ attr val]]
                                       (assoc m attr val))
                                     {}
                                     facts)))
                      (filter :task/status))
           ;; Group by status
           by-status (group-by :task/status tasks)]
       ;; Insert stats for each status
       (-> (reduce-kv
            (fn [s status status-tasks]
              (let [priorities (frequencies (map :task/priority status-tasks))
                    stats-id (keyword "stats" (name status))]
                (-> s
                    (o/insert stats-id :count (count status-tasks))
                    (o/insert stats-id :high-count (get priorities :priority/high 0))
                    (o/insert stats-id :medium-count (get priorities :priority/medium 0))
                    (o/insert stats-id :low-count (get priorities :priority/low 0)))))
            o/*session*
            by-status)
           o/reset!))]}))

;; =============================================================================
;; Session Atom
;; =============================================================================

(defonce *session (atom nil))

;; Rheon callback atoms - set by rheon-client to avoid circular dependency
(defonce *send! (atom nil))
(defonce *request! (atom nil))
(defonce *connect! (atom nil))

(defn init-session!
  "Initialize session with rules.
   Rules must be added BEFORE inserting facts, otherwise facts are discarded."
  []
  (reset! *session
    (-> (o/->session)
        ;; Add query rules first
        (#(reduce o/add-rule % rules))
        ;; Add action processing rules
        (#(reduce o/add-rule % action-rules))
        ;; Add derived facts rules
        (#(reduce o/add-rule % derived-rules)))))

;; =============================================================================
;; State Queries
;; =============================================================================

(defn query-all
  "Get all facts from session."
  [session]
  (o/query-all session))

(defn query-one
  "Query a single attribute from an entity."
  [session entity-id attr]
  (some (fn [[id a v]]
          (when (and (= id entity-id) (= a attr))
            v))
        (query-all session)))

(defn- task-id?
  "Check if id is a valid task ID (string or UUID)."
  [id]
  (or (string? id)
      #?(:cljs (uuid? id)
         :clj (instance? java.util.UUID id))))

(defn query-tasks
  "Get all tasks as a map of task-id -> task."
  [session]
  (let [facts (query-all session)
        task-facts (filter (fn [[id attr _]]
                             (and (task-id? id)
                                  (namespace attr)
                                  (= "task" (namespace attr))))
                           facts)]
    (->> task-facts
         (group-by first)
         (map (fn [[id tf]]
                [id (reduce (fn [task [_ attr val]]
                              (assoc task attr val))
                            {}
                            tf)]))
         (into {}))))

(defn query-transient
  "Get transient UI state."
  [session]
  (let [facts (query-all session)]
    (->> facts
         (filter (fn [[id _ _]]
                   (and (keyword? id)
                        (= "transient" (namespace id)))))
         (reduce (fn [m [id attr val]]
                   (assoc-in m [(keyword (name id)) attr] val))
                 {}))))

(defn query-stats
  "Get derived statistics per status column.
   Returns map like {:status/open {:count 3 :high 1 :medium 2 :low 0} ...}"
  [session]
  (let [facts (query-all session)
        stats-facts (filter (fn [[id _ _]]
                              (and (keyword? id)
                                   (= "stats" (namespace id))))
                            facts)]
    (->> stats-facts
         (reduce (fn [m [id attr val]]
                   (let [status (keyword "status" (name id))]
                     (assoc-in m [status attr] val)))
                 {}))))

(defn ->render-state
  "Derive render state from session for UI."
  [session]
  (let [tasks-map (query-tasks session)]
    {:tasks tasks-map
     :columns columns
     :tags tags
     :transient (query-transient session)
     :stats (query-stats session)
     :ui/editing-task (query-one session :ui :editing-task)
     :ui/inline-editing-id (query-one session :ui :inline-editing-id)
     :ui/task-history (query-one session :ui :task-history)
     :ui/history-index (query-one session :ui :history-index)
     :connected? (query-one session :system :connected?)
     :connection-status (query-one session :system :connection-status)
     :connection-error (query-one session :system :connection-error)
     :now (query-one session :system :now)}))


;; =============================================================================
;; Task Sync (from Rheon signal)
;; =============================================================================

(defn sync-tasks!
  "Sync tasks from Rheon signal into session.
   Clears existing task facts and inserts new ones."
  [tasks-map]
  (swap! *session
         (fn [session]
           ;; First retract all existing task facts
           (let [existing-facts (when session (query-all session))
                 cleared (reduce (fn [s [id attr _]]
                                   (if (and (task-id? id)
                                            (namespace attr)
                                            (= "task" (namespace attr)))
                                     (o/retract s id attr)
                                     s))
                                 session
                                 (or existing-facts []))
                 ;; Then insert new task facts and fire rules
                 with-facts (reduce-kv
                              (fn [s task-id task]
                                (reduce-kv
                                 (fn [sess attr val]
                                   (o/insert sess task-id attr val))
                                 s
                                 task))
                              cleared
                              tasks-map)]
             ;; IMPORTANT: Must call fire-rules for O'Doyle to process inserts
             (o/fire-rules with-facts)))))

(defn set-connected!
  "Update connection status."
  [connected?]
  (swap! *session
         (fn [session]
           (-> session
               (o/insert :system :connected? connected?)
               o/fire-rules))))
