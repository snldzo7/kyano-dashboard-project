(ns nestle.data.scenario-config
  "Scenario configuration - categories, sliders, action implications")

;; -----------------------------------------------------------------------------
;; Observation Categories
;; Groups observations by functional area for the scenario simulator
;; -----------------------------------------------------------------------------

(def categories
  {:demand {:label "Demand"
            :icon "chart-line"
            :observations [:consensus-forecast-qty :forecast-error-dist :order-variability-cv]}
   :supply {:label "Supply"
            :icon "truck"
            :observations [:supplier-otif-dist :open-po-qty-rpm :lead-time-variability]}
   :production {:label "Production"
                :icon "factory"
                :observations [:planned-production-qty :mfg-adherence-pct]}
   :policy {:label "Policy"
            :icon "sliders"
            :observations [:target-cof-pct :holding-cost-rate :cost-of-failure-unit]}})

(def category-order [:supply :demand :production :policy])

;; -----------------------------------------------------------------------------
;; Slider Configurations
;; Min, max, step values for each adjustable observation
;; -----------------------------------------------------------------------------

(def slider-config
  {;; Demand (matches React exactly)
   :consensus-forecast-qty {:min 30000 :max 70000 :step 1000
                            :format :number :suffix " units"}
   :forecast-error-dist {:min 0.01 :max 0.20 :step 0.01
                         :format :percent}
   :order-variability-cv {:min 0.10 :max 0.60 :step 0.01
                          :format :percent}

   ;; Supply (matches React exactly)
   :supplier-otif-dist {:min 0.50 :max 1.00 :step 0.01
                        :format :percent}
   :open-po-qty-rpm {:min 50000 :max 150000 :step 5000
                     :format :number :suffix " units"}
   :lead-time-variability {:min 1 :max 7 :step 0.5
                           :format :number :suffix " days"}

   ;; Production (matches React exactly)
   :planned-production-qty {:min 30000 :max 70000 :step 1000
                            :format :number :suffix " units"}
   :mfg-adherence-pct {:min 0.70 :max 1.00 :step 0.01
                       :format :percent}

   ;; Policy (matches React exactly)
   :target-cof-pct {:min 0.85 :max 0.99 :step 0.01
                    :format :percent}
   :holding-cost-rate {:min 0.10 :max 0.30 :step 0.01
                       :format :percent}
   :cost-of-failure-unit {:min 50 :max 200 :step 5
                          :format :currency}})

;; -----------------------------------------------------------------------------
;; Action Implications
;; What actions should be taken when observations change
;; -----------------------------------------------------------------------------

(def action-implications
  {:supplier-otif-dist
   [{:action "Expedite open POs with suppliers"
     :owner "Procurement"
     :urgency :high
     :threshold-below 0.80}
    {:action "Qualify alternate suppliers"
     :owner "Procurement"
     :urgency :medium
     :threshold-below 0.85}
    {:action "Review supplier scorecards"
     :owner "Procurement"
     :urgency :low
     :threshold-below 0.90}]

   :forecast-error-dist
   [{:action "Review forecast methodology"
     :owner "S&OP"
     :urgency :high
     :threshold-above 0.15}
    {:action "Add demand sensing signals"
     :owner "Analytics"
     :urgency :medium
     :threshold-above 0.10}
    {:action "Increase safety stock buffer"
     :owner "Supply Planning"
     :urgency :medium
     :threshold-above 0.10}]

   :mfg-adherence-pct
   [{:action "Review production schedule"
     :owner "Manufacturing"
     :urgency :high
     :threshold-below 0.85}
    {:action "Investigate capacity constraints"
     :owner "Manufacturing"
     :urgency :medium
     :threshold-below 0.90}]

   :consensus-forecast-qty
   [{:action "Align production capacity"
     :owner "Manufacturing"
     :urgency :high
     :change-pct 0.20}
    {:action "Review material requirements"
     :owner "Supply Planning"
     :urgency :medium
     :change-pct 0.10}]

   :open-po-qty-rpm
   [{:action "Release additional POs"
     :owner "Procurement"
     :urgency :high
     :threshold-below 70000}
    {:action "Expedite in-transit orders"
     :owner "Logistics"
     :urgency :medium
     :threshold-below 80000}]})

;; -----------------------------------------------------------------------------
;; Stakeholder Participants for Decision Recording
;; -----------------------------------------------------------------------------

(def participant-roles
  [{:id :supply-chain-dir
    :name "Supply Chain Director"
    :initials "SC"
    :color "bg-blue-600"}
   {:id :procurement-mgr
    :name "Procurement Manager"
    :initials "PM"
    :color "bg-emerald-600"}
   {:id :manufacturing-head
    :name "Manufacturing Head"
    :initials "MH"
    :color "bg-violet-600"}
   {:id :finance-controller
    :name "Finance Controller"
    :initials "FC"
    :color "bg-amber-600"}
   {:id :sales-director
    :name "Sales Director"
    :initials "SD"
    :color "bg-cyan-600"}
   {:id :sop-lead
    :name "S&OP Lead"
    :initials "SL"
    :color "bg-rose-600"}])

;; -----------------------------------------------------------------------------
;; Helper Functions
;; -----------------------------------------------------------------------------

(defn get-slider-config [obs-key]
  (get slider-config obs-key))

(defn get-observations-for-category [category-key]
  (get-in categories [category-key :observations]))

(defn get-applicable-actions
  "Get actions that apply based on current vs baseline values"
  [obs-key current-value baseline-value]
  (let [actions (get action-implications obs-key [])
        change-pct (when (and baseline-value (not (zero? baseline-value)))
                     (Math/abs (/ (- current-value baseline-value) baseline-value)))]
    (filter
     (fn [{:keys [threshold-below threshold-above change-pct-threshold]}]
       (or (and threshold-below (< current-value threshold-below))
           (and threshold-above (> current-value threshold-above))
           (and change-pct-threshold change-pct (> change-pct change-pct-threshold))))
     actions)))

(defn format-value
  "Format a value according to slider config"
  [obs-key value]
  (let [{:keys [format suffix]} (get slider-config obs-key)]
    (case format
      :percent (str (Math/round (* value 100)) "%")
      :currency (str "$" (.toLocaleString value))
      :number (str (.toLocaleString (Math/round value)) (or suffix ""))
      (str value))))
