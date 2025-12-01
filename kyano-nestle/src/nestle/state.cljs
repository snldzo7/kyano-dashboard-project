(ns nestle.state
  "Application state management"
  (:require [nestle.data.observations :as obs]
            [nestle.data.derived :as derived]
            [nestle.data.stakeholders :as stakeholders]))

;; -----------------------------------------------------------------------------
;; App State Atom
;; -----------------------------------------------------------------------------

(defonce !app-state
  (atom {:view :dashboard

         ;; Core data
         :observations obs/observations
         :derived derived/derived
         :historical-observations obs/historical-observations
         :sparkline-data obs/sparkline-data
         :stakeholders stakeholders/stakeholders
         :historical-decisions stakeholders/historical-decisions

         ;; Lineage view state
         :selected-node nil
         :selected-kpi nil

         ;; Scenario simulator state
         :scenario-observations nil    ;; nil = not in scenario mode
         :scenario-derived nil
         :scenario-name ""
         :active-category :supply      ;; Active tab in scenario view
         :participants []              ;; Selected participant IDs for decision recording
         :decisions []                 ;; Recorded decisions in current session

         ;; Time travel state
         :time-cursor nil              ;; nil = "now", otherwise ISO date string
         :as-of-observations nil       ;; Observations as of time-cursor
         :as-of-derived nil            ;; Derived values as of time-cursor
         :selected-decision nil        ;; Selected decision on timeline
         :selected-fact :supplier-otif-dist ;; For fact evolution panel

         ;; Collaborative decision room state
         :active-cpg-stakeholders #{:demand-planner}  ;; Mandatory stakeholder
         :active-retail-stakeholders #{:category-manager :replenishment-planner}  ;; Mandatory stakeholders
         :selected-complexity-scenario nil
         }))

;; -----------------------------------------------------------------------------
;; State accessors
;; -----------------------------------------------------------------------------

(defn get-view []
  (:view @!app-state))

(defn get-observations []
  (:observations @!app-state))

(defn get-derived []
  (:derived @!app-state))

(defn get-sparkline-data [k]
  (get-in @!app-state [:sparkline-data k]))

;; -----------------------------------------------------------------------------
;; State mutators
;; -----------------------------------------------------------------------------

(defn set-view! [view]
  (swap! !app-state assoc :view view))

(defn select-node! [node-id]
  (swap! !app-state assoc :selected-node node-id))

(defn select-kpi! [kpi-id]
  (swap! !app-state assoc :selected-kpi kpi-id))

(defn set-time-cursor! [time]
  (swap! !app-state assoc :time-cursor time))

;; -----------------------------------------------------------------------------
;; Scenario simulator mutators
;; -----------------------------------------------------------------------------

(defn start-scenario!
  "Enter scenario mode - copy current observations to scenario-observations"
  []
  (swap! !app-state
         (fn [state]
           (assoc state
                  :scenario-observations (:observations state)
                  :scenario-derived (:derived state)
                  :scenario-name ""
                  :participants []))))

(defn exit-scenario!
  "Exit scenario mode without applying changes"
  []
  (swap! !app-state assoc
         :scenario-observations nil
         :scenario-derived nil
         :scenario-name ""
         :participants []))

(defn update-scenario-observation!
  "Update a single observation in scenario mode"
  [obs-key new-value]
  (swap! !app-state assoc-in [:scenario-observations obs-key :value] new-value))

(defn set-scenario-derived!
  "Update derived values in scenario mode (called after recalculation)"
  [new-derived]
  (swap! !app-state assoc :scenario-derived new-derived))

(defn reset-scenario!
  "Reset scenario observations to current baseline"
  []
  (swap! !app-state
         (fn [state]
           (assoc state
                  :scenario-observations (:observations state)
                  :scenario-derived (:derived state)))))

(defn apply-scenario!
  "Apply scenario changes to actual observations"
  []
  (swap! !app-state
         (fn [state]
           (assoc state
                  :observations (:scenario-observations state)
                  :derived (:scenario-derived state)
                  :scenario-observations nil
                  :scenario-derived nil
                  :scenario-name ""
                  :participants []))))

(defn set-scenario-name! [name]
  (swap! !app-state assoc :scenario-name name))

(defn set-active-category! [category]
  (swap! !app-state assoc :active-category category))

(defn toggle-participant! [participant-id]
  (swap! !app-state update :participants
         (fn [ps]
           (if (some #{participant-id} ps)
             (vec (remove #{participant-id} ps))
             (conj ps participant-id)))))

(defn record-decision!
  "Record a decision with current facts and participants"
  []
  (let [state @!app-state
        decision {:id (random-uuid)
                  :timestamp (js/Date.)
                  :name (:scenario-name state)
                  :participants (:participants state)
                  :baseline-observations (:observations state)
                  :scenario-observations (:scenario-observations state)
                  :baseline-derived (:derived state)
                  :scenario-derived (:scenario-derived state)}]
    (swap! !app-state update :decisions conj decision)))

;; -----------------------------------------------------------------------------
;; Time travel mutators
;; -----------------------------------------------------------------------------

(defn set-as-of-state!
  "Set the as-of observations and derived values"
  [observations derived]
  (swap! !app-state assoc
         :as-of-observations observations
         :as-of-derived derived))

(defn clear-time-travel!
  "Reset time travel to 'now'"
  []
  (swap! !app-state assoc
         :time-cursor nil
         :as-of-observations nil
         :as-of-derived nil
         :selected-decision nil))

(defn select-decision! [decision-id]
  (swap! !app-state assoc :selected-decision decision-id))

(defn set-selected-fact! [fact-key]
  (swap! !app-state assoc :selected-fact fact-key))

;; -----------------------------------------------------------------------------
;; Collaborative Decision Room mutators
;; -----------------------------------------------------------------------------

(defn toggle-cpg-stakeholder! [stakeholder-key]
  (swap! !app-state update :active-cpg-stakeholders
         (fn [s]
           (if (contains? s stakeholder-key)
             (disj s stakeholder-key)
             (conj s stakeholder-key)))))

(defn toggle-retail-stakeholder! [stakeholder-key]
  (swap! !app-state update :active-retail-stakeholders
         (fn [s]
           (if (contains? s stakeholder-key)
             (disj s stakeholder-key)
             (conj s stakeholder-key)))))

(defn set-selected-complexity-scenario! [scenario-key]
  (swap! !app-state assoc :selected-complexity-scenario scenario-key))

;; -----------------------------------------------------------------------------
;; Computed state helpers
;; -----------------------------------------------------------------------------

(defn in-scenario-mode? []
  (some? (:scenario-observations @!app-state)))

(defn in-time-travel-mode? []
  (some? (:time-cursor @!app-state)))

(defn get-effective-observations
  "Get observations for current mode (scenario, time-travel, or baseline)"
  []
  (let [state @!app-state]
    (cond
      (:scenario-observations state) (:scenario-observations state)
      (:as-of-observations state) (:as-of-observations state)
      :else (:observations state))))

(defn get-effective-derived
  "Get derived values for current mode"
  []
  (let [state @!app-state]
    (cond
      (:scenario-derived state) (:scenario-derived state)
      (:as-of-derived state) (:as-of-derived state)
      :else (:derived state))))
