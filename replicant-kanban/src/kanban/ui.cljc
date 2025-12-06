(ns kanban.ui
  (:require [kanban.task :as task]
            [kanban.ui.elements :as e]
            [phosphor.icons :as icons]
            [sparkline.core :as sp]))

;; =============================================================================
;; Helpers
;; =============================================================================

(defn format-keyword [k]
  (when k (name k)))

(defn render-task-view
  "Render task in view mode (not editing)."
  [state {:task/keys [id title tags priority description] :as task}]
  (let [expanded? (task/expanded? state task)]
    [e/card (cond-> {:on {:dragstart [[:actions/assoc-in [:transient :dragging] id]]}}
              expanded? (assoc ::e/expanded? true)
              (contains? (:new-tasks state) id) (update :class conj :wiggle-in))
     (when (seq tags)
       (conj (let [tag->style (into {} (mapv (juxt :tag/ident :tag/style) (:tags state)))]
               (into [e/badges]
                     (mapv (fn [t]
                             [e/badge {::e/style (tag->style t)} (name t)])
                           tags)))))
     [:div.card-actions.justify-end.gap-1
      (when description
        [::e/toggle-button.btn-xs.btn-ghost
         {::e/on? expanded?
          :on    {:click [[(if expanded? :actions/collapse-task :actions/expand-task) id]]}}
         (icons/icon :phosphor.regular/file)
         (icons/icon :phosphor.regular/x)])
      [::e/button.btn-xs.btn-ghost
       {:type "button"
        :on   {:click [[:actions/start-inline-edit task]]}}
       [e/icon {::e/size :w-4} (icons/icon :phosphor.regular/pencil-simple)]]
      [::e/button.btn-xs.btn-ghost
       {:type "button"
        :on   {:click [[:actions/open-history id]]}}
       [e/icon {::e/size :w-4} (icons/icon :phosphor.regular/clock-counter-clockwise)]]]
     [e/card-title
      (when (= :priority/high priority)
        [e/icon {:class [:text-error]} (icons/icon :phosphor.regular/fire)])
      (when (= :priority/low priority)
        [e/icon {:class [:opacity-50]} (icons/icon :phosphor.regular/tray-arrow-down)])
      title]
     (when expanded?
       [e/card-details
        [:p description]])]))

(defn render-task-edit
  "Render task in inline edit mode with input fields."
  [_state editing-task]
  (let [{:task/keys [_id title description priority status]} editing-task]
    [:article.card.shadow-sm.bg-base-100.relative.ring-2.ring-primary
     [:div.card-body.flex.flex-col.gap-3
      ;; Title input
      [:input.input.input-bordered.w-full.font-bold
       {:type "text"
        :value (or title "")
        :placeholder "Task title"
        :on {:input [[:actions/update-editing :task/title :event/target.value]]}}]

      ;; Description textarea
      [:textarea.textarea.textarea-bordered.w-full
       {:rows 2
        :value (or description "")
        :placeholder "Description (optional)"
        :on {:input [[:actions/update-editing :task/description :event/target.value]]}}]

      ;; Priority and Status selects
      [:div.flex.gap-2
       [:select.select.select-bordered.select-sm.flex-1
        {:value (format-keyword priority)
         :on {:change [[:actions/update-editing :task/priority :event/target.value.keyword]]}}
        [:option {:value "priority/high"} "High"]
        [:option {:value "priority/medium"} "Medium"]
        [:option {:value "priority/low"} "Low"]]
       [:select.select.select-bordered.select-sm.flex-1
        {:value (format-keyword status)
         :on {:change [[:actions/update-editing :task/status :event/target.value.keyword]]}}
        [:option {:value "status/open"} "Open"]
        [:option {:value "status/in-progress"} "In Progress"]
        [:option {:value "status/closed"} "Closed"]]]

      ;; Action buttons
      [:div.flex.gap-2.justify-end
       [::e/button.btn-sm.btn-ghost
        {:type "button"
         :on {:click [[:actions/cancel-inline-edit]]}}
        [e/icon {::e/size :w-4} (icons/icon :phosphor.regular/x)]
        "Cancel"]
       [::e/button.btn-sm.btn-primary
        {:type "button"
         :on {:click [[:actions/save-inline-edit]]}}
        [e/icon {::e/size :w-4} (icons/icon :phosphor.regular/check)]
        "Save"]]]]))

(defn ^{:indent 1} render-task [state {:task/keys [id] :as task}]
  (let [inline-editing-id (:ui/inline-editing-id state)
        editing? (= id inline-editing-id)
        editing-task (:ui/editing-task state)]
    (if editing?
      (render-task-edit state editing-task)
      (render-task-view state task))))

(defn get-drop-action [{:column/keys [status limit]} status->tasks task]
  (when (and task (not= status (:task/status task)))
    (if (or (nil? limit) (< (count (status->tasks status)) limit))
      [[:actions/set-task-status (:task/id task) status]]
      [[:actions/flash 3000 [:transient status :error] :errors/at-limit]])))

(defn render-error [{:column/keys [status limit]} error]
  (e/alert {:class :alert-error
            ::e/actions [[:actions/dissoc-in [:transient status :error]]]}
   (when (= :errors/at-limit error)
     (str "You can't have more than " limit " task" (when-not (= 1 limit) "s") " here"))))

(defn task-sort-k [task]
  [(case (:task/priority task)
     :priority/high -1
     :priority/medium 0
     1)
   (or (:task/changed-status-at task) (:task/created-at task))])

(defn keyword->s [k]
  (str (when-let [ns (namespace k)]
         (str ns "/")) (name k)))

(defn render-task-form [status]
  [:form.bg-base-100.p-4.rounded-md.flex.flex-col.gap-2.z-1.mt-auto
   {:on {:submit [[:actions/prevent-default]
                  [:actions/close-new-task-form status]
                  [:actions/add-task :event/form-data :random/id]]}}
   [:input {:type "hidden"
            :name "task/status"
            :data-type "keyword"
            :value (keyword->s status)}]
   [e/text-input
    {::e/autofocus? true
     :name "task/title"
     :type "text"
     :placeholder "Task"}]
   [:textarea.textarea
    {:name "task/description"
     :placeholder "Description"}]
   [:select.select
    {:name "task/priority"
     :data-type "keyword"}
    [:option {:value "priority/high"} "High priority"]
    [:option {:value "priority/medium" :selected "selected"} "Medium priority"]
    [:option {:value "priority/low"} "Low priority"]]
   [:input.input
    {:name "tags"
     :type "text"
     :placeholder "Tags"}]
   [:div.flex.gap-2
    [::e/button.btn-primary {:type "submit"}
     [e/icon {::e/size :w-4} (icons/icon :phosphor.regular/plus-circle)]
     "Add task"]
    [::e/button.btn-ghost
     {:type "button"
      :on {:click [[:actions/close-new-task-form status]]}}
     [e/icon {::e/size :w-4} (icons/icon :phosphor.regular/x-circle)]
     "Cancel"]]])

(defn column-sparkline
  "Render a stacked sparkline showing priority distribution.
   Uses pre-computed stats from O'Doyle derived facts when available,
   falls back to computing from tasks if not."
  [column-status stats column-tasks]
  (let [column-stats (get stats column-status)
        ;; Use derived stats if available, otherwise compute from tasks
        [high med low] (if column-stats
                         [(get column-stats :high-count 0)
                          (get column-stats :medium-count 0)
                          (get column-stats :low-count 0)]
                         (let [priority-counts (frequencies (map :task/priority column-tasks))]
                           [(get priority-counts :priority/high 0)
                            (get priority-counts :priority/medium 0)
                            (get priority-counts :priority/low 0)]))
        total (+ high med low)]
    (when (pos? total)
      [::sp/sparkline
       {:type :stacked
        :points [high med low]
        :size [80 12]
        :gap 2
        ;; Use standard CSS colors - oklch with CSS vars doesn't work in SVG fill
        :colors ["#f87171"    ;; red-400 for high priority
                 "#fbbf24"    ;; amber-400 for medium
                 "#4ade80"]   ;; green-400 for low
        :labels ["High" "Medium" "Low"]
        :format "%label: %point (%percent)"}])))

(defn render-columns [state {:keys [tasks error? loading?]}]
  (let [status->tasks (group-by :task/status tasks)
        task (first (filterv (comp #{(-> state :transient :dragging :value)} :task/id) tasks))
        available? (and (not error?) (not loading?))
        stats (:stats state)]
    (into [:div.flex.gap-16.swimlane]
          (for [column (:columns state)]
            (let [status (:column/status column)
                  status-key (keyword (name status))
                  adding? (get-in state [:transient status-key :add?])
                  column-tasks (status->tasks status)]
              [e/column {:class (name status)}
               [:div.flex.flex-col.gap-2.mb-2
                [:h2.text-2xl
                 (if available?
                   (str (:column/title column) " ("
                        (count column-tasks)
                        (when-let [n (:column/limit column)]
                          (str "/" n))
                        ")")
                   (:column/title column))]
                (when (and available? (seq column-tasks))
                  (column-sparkline status stats column-tasks))]
               (some->> (get-in state [:transient status-key :error])
                        (render-error column))
               (if (not available?)
                 [e/column-body {}
                  (when (and loading? (= column (first (:columns state))))
                    [:span.loading.loading-spinner.loading-xl])]
                 (cond-> [e/column-body
                          {:on {:drop (concat [[:actions/prevent-default]]
                                              (get-drop-action column status->tasks task))}}]
                   :then
                   (into
                    (cond->> column-tasks
                      :then (sort-by (or (:column/sort-by column) task-sort-k))
                      (= :desc (:column/sort-order column)) reverse
                      :then (mapv #(render-task state %))))

                   (and (:column/add-new? column) (not adding?))
                   (conj [::e/button.z-1.mt-auto
                          {:on {:click [[:actions/open-new-task-form (:column/status column)]]}}
                          [e/icon {::e/size :w-4} (icons/icon :phosphor.regular/plus)]
                          "Add task"])

                   adding?
                   (conj (render-task-form (:column/status column)))))])))))

(defn render-modal [{:keys [error?]}]
  (when error?
    [e/modal
     [:h2.text-lg.mb-4 "Failed to load tasks"]
     [e/button {::e/actions [[:actions/load-tasks]]}
      "Try again"]]))

;; =============================================================================
;; Edit Modal
;; =============================================================================

(defn edit-modal [state]
  ;; Only show modal when editing AND not inline editing
  (when (and (:ui/editing-task state)
             (not (:ui/inline-editing-id state)))
    (let [task (:ui/editing-task state)]
      [:dialog.modal.modal-open
     [:div.modal-box
      ;; Header with title and close button
      [:div.flex.justify-between.items-center.mb-4
       [:h3.font-bold.text-lg "Edit Task"]
       [:button.btn.btn-sm.btn-circle.btn-ghost
        {:type "button" :on {:click [[:actions/close-modal]]}}
        [e/icon {::e/size :w-4} (icons/icon :phosphor.regular/x)]]]

      ;; Title field
      [:fieldset.fieldset
       [:legend.fieldset-legend "Title"]
       [:input.input.w-full
        {:type "text"
         :value (or (:task/title task) "")
         :on {:input [[:actions/update-editing :task/title :event/target.value]]}}]]

      ;; Description field
      [:fieldset.fieldset.mt-4
       [:legend.fieldset-legend "Description"]
       [:textarea.textarea.w-full
        {:value (or (:task/description task) "")
         :rows 4
         :on {:input [[:actions/update-editing :task/description :event/target.value]]}}]]

      ;; Priority select
      [:fieldset.fieldset.mt-4
       [:legend.fieldset-legend "Priority"]
       [:select.select.w-full
        {:value (format-keyword (:task/priority task))
         :on {:change [[:actions/update-editing :task/priority :event/target.value.keyword]]}}
        [:option {:value "priority/high"} "High"]
        [:option {:value "priority/medium"} "Medium"]
        [:option {:value "priority/low"} "Low"]]]

      ;; Status select
      [:fieldset.fieldset.mt-4
       [:legend.fieldset-legend "Status"]
       [:select.select.w-full
        {:value (format-keyword (:task/status task))
         :on {:change [[:actions/update-editing :task/status :event/target.value.keyword]]}}
        [:option {:value "status/open"} "Open"]
        [:option {:value "status/in-progress"} "In Progress"]
        [:option {:value "status/closed"} "Closed"]]]

      ;; Action buttons
      [:div.modal-action
       [::e/button.btn-ghost {:type "button" :on {:click [[:actions/close-modal]]}} "Cancel"]
       [::e/button.btn-primary {:type "button" :on {:click [[:actions/save-task]]}} "Save"]]]

     ;; Backdrop to close modal
     [:div.modal-backdrop {:on {:click [[:actions/close-modal]]}}]])))

;; =============================================================================
;; History Timeline (Scrollable Card List)
;; =============================================================================

(defn format-date [inst]
  (when inst
    #?(:cljs (.toLocaleString inst)
       :clj (str inst))))

(defn diff-fields
  "Find which fields changed between two snapshots."
  [prev current]
  (when (and prev current)
    (reduce-kv
     (fn [acc k v]
       (if (and (not= k :history/timestamp)
                (not= v (get prev k)))
         (conj acc k)
         acc))
     #{}
     current)))

(defn change-badge
  "Render a colored outline badge showing what field changed."
  [field-key]
  (let [[field-name badge-color] (case field-key
                                   :task/title ["title" "badge-primary"]
                                   :task/description ["description" "badge-secondary"]
                                   :task/status ["status" "badge-accent"]
                                   :task/priority ["priority" "badge-warning"]
                                   :task/tags ["tags" "badge-info"]
                                   [(name field-key) "badge-neutral"])]
    [:span.badge.badge-xs.badge-outline {:replicant/key field-key :class [badge-color]} field-name]))

(defn history-snapshot-card
  "Render a single history snapshot as a styled card."
  [{:keys [snapshot prev-snapshot idx total selected?]}]
  (let [changes (diff-fields prev-snapshot snapshot)
        is-creation? (nil? prev-snapshot)
        {:task/keys [title priority description status tags]} snapshot
        version-num (- total idx)]
    [:article.card.shadow-lg.bg-base-100
     {:replicant/key idx
      :class (when selected? [:ring-2 :ring-primary])}
     [:div.card-body.flex.flex-col.gap-3
       ;; Version header with timestamp and change indicators
       [:div.flex.justify-between.items-start
        [:div.flex.flex-col.gap-2
         [:div.flex.items-center.gap-2
          [:span.badge.badge-lg.badge-neutral (str "v" version-num)]
          (when is-creation?
            [:span.badge.badge-outline.badge-success "created"])]
         (when (and (not is-creation?) (seq changes))
           [:div.flex.flex-wrap.gap-1
            (for [field changes]
              (change-badge field))])]
        [:span.text-sm.opacity-60 (format-date (:history/timestamp snapshot))]]

       ;; Tags (same as task card)
       (when (seq tags)
         (into [:ul.flex.flex-wrap.gap-2]
               (for [t tags]
                 [:li.badge.badge-soft {:replicant/key t} (name t)])))

       ;; Title with priority icon (same as task card)
       [e/card-title
        (when (= :priority/high priority)
          [e/icon {:class [:text-error]} (icons/icon :phosphor.regular/fire)])
        (when (= :priority/low priority)
          [e/icon {:class [:opacity-50]} (icons/icon :phosphor.regular/tray-arrow-down)])
        [:span {:class (when (contains? changes :task/title) [:text-primary :font-bold])} title]]

       ;; Status badge - highlight changes with colored outline
       [:div.flex.gap-2
        [:span.badge
         {:class (if (contains? changes :task/status)
                   [:badge-outline :badge-accent]
                   [:badge-ghost])}
         (format-keyword status)]
        [:span.badge
         {:class (if (contains? changes :task/priority)
                   [:badge-outline :badge-warning]
                   [:badge-ghost])}
         (format-keyword priority)]]

       ;; Description (same as card-details)
       (when description
         [e/card-details {:class (when (contains? changes :task/description) :text-secondary)}
          [:p description]])]]))

(defn history-carousel-item
  "Single carousel item wrapping a history card.
   Uses reusable components for consistent design."
  [{:keys [snapshot prev-snapshot idx total]}]
  (let [changes (diff-fields prev-snapshot snapshot)
        is-creation? (nil? prev-snapshot)
        {:task/keys [title priority description status tags]} snapshot
        version-num (- total idx)
        slide-id (str "history-slide-" idx)
        prev-id (str "history-slide-" (dec idx))
        next-id (str "history-slide-" (inc idx))
        has-prev? (pos? idx)
        has-next? (< idx (dec total))]
    [:div.carousel-item.relative.w-full
     {:id slide-id
      :replicant/key idx
      :style {:flex "0 0 100%"
              :min-width "100%"
              :scroll-snap-align "start"}}

     ;; Content with padding for arrows + visible background for card contrast
     [:div.w-full.py-6.px-12.flex.flex-col.items-center.gap-4.rounded-box
      {:style {:background-color "oklch(0.92 0.01 240)"}}

      ;; HEADER: Version + timestamp
      [:div.flex.flex-col.items-center.gap-1
       [::e/version-badge {::e/version version-num ::e/is-creation? is-creation?}]
       [:time.text-xs.opacity-50 (format-date (:history/timestamp snapshot))]]

      ;; CHANGES (if any)
      (when (and (not is-creation?) (seq changes))
        [:div.flex.items-center.gap-2
         [::e/meta-label "Changed:"]
         (for [field changes]
           (change-badge field))])

      ;; FLOATING CARD - shadow makes it pop against bg-base-200
      [:div.w-full.max-w-xs
       [e/card {:class [:shadow-xl]}
        (when (seq tags)
          (into [e/badges]
                (for [t tags]
                  [e/badge {:replicant/key t} (name t)])))
        [e/card-title
         (when (= :priority/high priority)
           [e/icon {:class [:text-error]} (icons/icon :phosphor.regular/fire)])
         (when (= :priority/low priority)
           [e/icon {:class [:opacity-50]} (icons/icon :phosphor.regular/tray-arrow-down)])
         title]
        (when description
          [e/card-details
           [:p description]])]]

      ;; FOOTER: Status + Priority using property-display
      [:div.flex.gap-8
       [::e/property-display {::e/label "Status"
                              ::e/value (format-keyword status)
                              ::e/highlight? (contains? changes :task/status)}]
       [::e/property-display {::e/label "Priority"
                              ::e/value (format-keyword priority)
                              ::e/highlight? (contains? changes :task/priority)}]]]

     ;; ARROWS using carousel-arrow component
     (when has-prev?
       [::e/carousel-arrow {::e/direction :prev ::e/target-id prev-id}])
     (when has-next?
       [::e/carousel-arrow {::e/direction :next ::e/target-id next-id}])]))

(defn history-timeline [state]
  (when-let [history (:ui/task-history state)]
    (let [total (count history)
          history-items (map-indexed
                         (fn [idx snapshot]
                           {:snapshot snapshot
                            :prev-snapshot (get history (inc idx))
                            :idx idx
                            :total total})
                         history)]
      [:dialog.modal.modal-open
       [:div.modal-box.max-w-3xl.w-full
        ;; Header - close button on LEFT
        [:div.flex.items-center.gap-4.mb-4
         [:button.btn.btn-sm.btn-circle.btn-ghost
          {:type "button" :on {:click [[:actions/close-modal]]}}
          [e/icon {::e/size :w-4} (icons/icon :phosphor.regular/x)]]
         [:h3.text-lg.font-bold.flex.items-center.gap-2
          [e/icon {::e/size :w-6} (icons/icon :phosphor.regular/clock-counter-clockwise)]
          (str "Version History (" total " versions)")]]

        ;; DaisyUI Carousel - horizontal scroll with snap
        [:div.carousel.w-full.rounded-box.bg-base-200.flex-nowrap
         {:style {:display "flex"
                  :overflow-x "auto"
                  :scroll-snap-type "x mandatory"
                  :scroll-behavior "smooth"}}
         (for [item history-items]
           (history-carousel-item item))]

        ;; Dot navigation - more spacing from carousel
        [:div.flex.w-full.justify-center.gap-1.py-6
         (for [idx (range total)]
           [:a.btn.btn-xs.btn-circle
            {:replicant/key idx
             :href (str "#history-slide-" idx)}
            (- total idx)])]]

       [:div.modal-backdrop {:on {:click [[:actions/close-modal]]}}]])))

(defn render-header [state]
  [:header.flex.justify-between.items-center.mb-6
   [:h1.text-3xl.font-bold "Kanban Board"]
   [::e/connection-status
    {::e/status (:connection-status state)
     ::e/on-reconnect [[:actions/connect]]}]])

(defn render-app [state]
  (let [tasks (task/get-tasks state)]
    [:main.m-8
     (render-header state)
     (render-modal tasks)
     (render-columns state tasks)
     (edit-modal state)
     (history-timeline state)]))
