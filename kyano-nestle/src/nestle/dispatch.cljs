(ns nestle.dispatch
  "Data-driven action dispatch system for Replicant.
   All UI interactions are expressed as data (action tuples),
   routed through a single dispatcher."
  (:require [clojure.walk :as walk]
            [replicant.dom :as r]
            [nestle.state :as state]
            [nestle.data.derived :as derived-calc]))

;; -----------------------------------------------------------------------------
;; Event Value Interpolation
;; -----------------------------------------------------------------------------

(defn interpolate-event-values
  "Replace event placeholders with actual values from the DOM event.
   Supports:
   - :event/target.value - input value
   - :event/target.checked - checkbox checked state
   - :event/key - keyboard key
   - :event/prevent-default - marker to call preventDefault"
  [dom-event actions]
  (walk/postwalk
   (fn [x]
     (case x
       :event/target.value (.. dom-event -target -value)
       :event/target.checked (.. dom-event -target -checked)
       :event/key (.-key dom-event)
       x))
   actions))

;; -----------------------------------------------------------------------------
;; Action Execution
;; -----------------------------------------------------------------------------

(defmulti execute-action!
  "Execute a single action. Dispatch on the action keyword (first element)."
  (fn [[action-type & _args]] action-type))

;; --- Navigation Actions ---

(defmethod execute-action! :app/set-view
  [[_ view-id]]
  (state/set-view! view-id))

(defmethod execute-action! :app/select-kpi
  [[_ kpi-id]]
  (state/select-kpi! kpi-id))

(defmethod execute-action! :app/toggle-kpi
  [[_ kpi-id]]
  (let [current (:selected-kpi @state/!app-state)]
    (state/select-kpi! (if (= current kpi-id) nil kpi-id))))

;; --- Lineage Actions ---

(defmethod execute-action! :lineage/select-node
  [[_ node-id]]
  (state/select-node! node-id))

(defmethod execute-action! :lineage/toggle-node
  [[_ node-id]]
  (let [current (:selected-node @state/!app-state)]
    (state/select-node! (if (= current node-id) nil node-id))))

;; --- Scenario Actions ---

(defmethod execute-action! :scenario/start
  [_]
  (state/start-scenario!))

(defmethod execute-action! :scenario/reset
  [_]
  (state/reset-scenario!))

(defmethod execute-action! :scenario/apply
  [_]
  (state/apply-scenario!))

(defmethod execute-action! :scenario/set-category
  [[_ category]]
  (state/set-active-category! category))

(defmethod execute-action! :scenario/update-observation
  [[_ obs-key new-value]]
  (let [parsed-value (if (string? new-value)
                       (js/parseFloat new-value)
                       new-value)]
    (state/update-scenario-observation! obs-key parsed-value)
    ;; Recalculate derived values
    (let [updated-obs (assoc-in (:scenario-observations @state/!app-state)
                                [obs-key :value] parsed-value)
          new-derived (derived-calc/calculate-derived updated-obs)]
      (state/set-scenario-derived! new-derived))))

(defmethod execute-action! :scenario/set-name
  [[_ name]]
  (state/set-scenario-name! name))

(defmethod execute-action! :scenario/toggle-participant
  [[_ participant-id]]
  (state/toggle-participant! participant-id))

(defmethod execute-action! :scenario/record-decision
  [_]
  (state/record-decision!))

;; --- Time Travel Actions ---

(defmethod execute-action! :time-travel/set-cursor
  [[_ time-cursor]]
  (state/set-time-cursor! time-cursor)
  ;; Recalculate as-of state
  (let [app-state @state/!app-state
        historical-obs (:historical-observations app-state)
        now-obs (:observations app-state)]
    (when time-cursor
      (let [as-of-obs (reduce-kv
                       (fn [state obs-key history]
                         (let [as-of-ms (.getTime (js/Date. time-cursor))
                               valid-facts (filter #(<= (.getTime (js/Date. (:tx-time %))) as-of-ms) history)]
                           (if-let [fact (last valid-facts)]
                             (assoc state obs-key
                                    {:value (:value fact)
                                     :unit (get-in now-obs [obs-key :unit] "")
                                     :source (:source fact)
                                     :label (get-in now-obs [obs-key :label] (name obs-key))
                                     :tx-time (:tx-time fact)
                                     :note (:note fact)})
                             state)))
                       now-obs
                       historical-obs)
            as-of-derived (derived-calc/calculate-derived as-of-obs)]
        (state/set-as-of-state! as-of-obs as-of-derived)))))

(defmethod execute-action! :time-travel/set-cursor-from-ms
  [[_ ms-value]]
  (let [new-date (js/Date. (js/parseFloat ms-value))]
    (execute-action! [:time-travel/set-cursor (.toISOString new-date)])))

(defmethod execute-action! :time-travel/jump-to-now
  [_]
  (state/clear-time-travel!))

(defmethod execute-action! :time-travel/select-decision
  [[_ decision-id]]
  (state/select-decision! decision-id))

(defmethod execute-action! :time-travel/set-selected-fact
  [[_ fact-key]]
  (state/set-selected-fact! (if (string? fact-key) (keyword fact-key) fact-key)))

;; --- Collaborative Room Actions ---

(defmethod execute-action! :collab/toggle-cpg-stakeholder
  [[_ stakeholder-key]]
  (state/toggle-cpg-stakeholder! stakeholder-key))

(defmethod execute-action! :collab/toggle-retail-stakeholder
  [[_ stakeholder-key]]
  (state/toggle-retail-stakeholder! stakeholder-key))

(defmethod execute-action! :collab/set-scenario
  [[_ scenario-key]]
  (state/set-selected-complexity-scenario! scenario-key))

(defmethod execute-action! :collab/toggle-scenario
  [[_ scenario-key]]
  (let [current (:selected-complexity-scenario @state/!app-state)]
    (state/set-selected-complexity-scenario! (if (= current scenario-key) nil scenario-key))))

;; --- DOM Actions (imperative) ---

(defmethod execute-action! :dom/prevent-default
  [[_ dom-event]]
  (when dom-event
    (.preventDefault dom-event)))

(defmethod execute-action! :dom/stop-propagation
  [[_ dom-event]]
  (when dom-event
    (.stopPropagation dom-event)))

;; --- Fallback ---

(defmethod execute-action! :default
  [[action-type & args]]
  (js/console.warn "Unknown action:" (str action-type) (clj->js args)))

;; -----------------------------------------------------------------------------
;; Main Dispatcher
;; -----------------------------------------------------------------------------

(defn execute-actions!
  "Execute a sequence of actions, passing the DOM event for interpolation."
  [dom-event actions]
  (let [interpolated (interpolate-event-values dom-event actions)]
    (if (and (vector? interpolated) (keyword? (first interpolated)))
      ;; Single action
      (execute-action! interpolated)
      ;; Multiple actions
      (doseq [action interpolated]
        (when (and (vector? action) (keyword? (first action)))
          (execute-action! action))))))

(defn dispatch!
  "Global dispatcher called by Replicant for all data handlers.
   event-data contains :replicant/trigger, :replicant/dom-event, :replicant/node
   handler-data is the action data from the view."
  [event-data handler-data]
  (when (= :replicant.trigger/dom-event (:replicant/trigger event-data))
    (let [dom-event (:replicant/dom-event event-data)]
      (execute-actions! dom-event handler-data))))

;; -----------------------------------------------------------------------------
;; Initialize Dispatcher
;; -----------------------------------------------------------------------------

(defn init!
  "Register the global dispatcher with Replicant. Call once at app startup."
  []
  (r/set-dispatch! dispatch!)
  (println "Action dispatcher initialized"))
