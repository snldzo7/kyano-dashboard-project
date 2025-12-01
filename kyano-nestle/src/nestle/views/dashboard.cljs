(ns nestle.views.dashboard
  "Dashboard view - KPIs, inventory pools, and stats (matching React exactly)"
  (:require [kyano.ui.cards :as c]
            [nestle.state :as state]))

;; -----------------------------------------------------------------------------
;; KPI Definitions (matching React)
;; -----------------------------------------------------------------------------

(def kpi-icons
  {:total-inventory-value "M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"
   :service-risk "M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
   :cash-impact "M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"})

(def targets
  {:total-inventory-value {:value 4250000 :tolerance 0.10}
   :service-risk {:value 0.05 :tolerance 0.02}
   :cash-impact {:value 100000 :tolerance 0.15}})

;; -----------------------------------------------------------------------------
;; KPI Section
;; -----------------------------------------------------------------------------

(defn calculate-status [value target tolerance risk-direction]
  (let [variance (/ (- value target) target)
        abs-variance (js/Math.abs variance)
        is-off-target (> abs-variance tolerance)
        is-warning (and (> abs-variance (* tolerance 0.5)) (not is-off-target))]
    (cond
      is-off-target :danger
      is-warning :warning
      :else :good)))

(defn kpi-section []
  (let [derived (state/get-derived)
        sparklines (:sparkline-data @state/!app-state)
        selected-kpi (:selected-kpi @state/!app-state)

        ;; Total Inventory Value KPI
        inv-value (:total-inventory-value derived)
        inv-target (:value (targets :total-inventory-value))
        inv-tolerance (:tolerance (targets :total-inventory-value))
        inv-variance (/ (- inv-value inv-target) inv-target)
        inv-status (calculate-status inv-value inv-target inv-tolerance :high)

        ;; Service Risk KPI
        risk-value (:service-risk derived)
        risk-target (:value (targets :service-risk))
        risk-tolerance (:tolerance (targets :service-risk))
        risk-variance (/ (- risk-value risk-target) risk-target)
        risk-status (calculate-status risk-value risk-target risk-tolerance :high)

        ;; Cash Impact KPI
        cash-value (:cash-impact derived)
        cash-target (:value (targets :cash-impact))
        cash-tolerance (:tolerance (targets :cash-impact))
        cash-variance (/ (- cash-value cash-target) cash-target)
        cash-status (calculate-status cash-value cash-target cash-tolerance :high)]

    [:div {:class "grid grid-cols-3 gap-6"}
     ;; Total Inventory Value
     (c/kpi-card
      {:label "Total Inventory Value"
       :value (str "$" (.toFixed (/ inv-value 1000000) 2) "M")
       :target (str "$" (.toFixed (/ inv-target 1000000) 2) "M")
       :variance inv-variance
       :status inv-status
       :risk-direction :high
       :icon-path (kpi-icons :total-inventory-value)
       :trend (or (:inventory-value sparklines) [3.8 4.0 4.2 4.1 4.3 4.2 4.4 (/ inv-value 1000000)])
       :selected? (= selected-kpi :total-inventory-value)
       :on-click [:app/toggle-kpi :total-inventory-value]})

     ;; Service Risk
     (c/kpi-card
      {:label "Service Risk"
       :value (str (.toFixed (* risk-value 100) 1) "%")
       :target (str "<" (.toFixed (* risk-target 100) 0) "%")
       :variance risk-variance
       :status risk-status
       :risk-direction :high
       :icon-path (kpi-icons :service-risk)
       :trend (or (:service-risk sparklines) [0.03 0.04 0.05 0.08 0.10 0.12 0.14 risk-value])
       :selected? (= selected-kpi :service-risk)
       :on-click [:app/toggle-kpi :service-risk]})

     ;; Cash Impact
     (c/kpi-card
      {:label "Cash Impact"
       :value (str "$" (.toFixed (/ cash-value 1000) 0) "K")
       :target (str "<$" (.toFixed (/ cash-target 1000) 0) "K")
       :variance cash-variance
       :status cash-status
       :risk-direction :high
       :icon-path (kpi-icons :cash-impact)
       :trend (or (:cash-impact sparklines) [80 85 90 95 105 120 150 (/ cash-value 1000)])
       :selected? (= selected-kpi :cash-impact)
       :on-click [:app/toggle-kpi :cash-impact]})]))

;; -----------------------------------------------------------------------------
;; Inventory Pools Section
;; -----------------------------------------------------------------------------

(defn inventory-pools []
  (let [derived (state/get-derived)]
    [:div {:class "grid grid-cols-2 gap-6"}
     ;; Finished Goods Pool
     (c/inventory-pool-card
      {:title "Finished Goods"
       :position (:fg-inventory-position derived)
       :value (:fg-inventory-value derived)
       :inflows (:fg-inflows derived)
       :outflows (:fg-outflows derived)
       :unit "cases"})

     ;; Raw & Pack Materials Pool
     (c/inventory-pool-card
      {:title "Raw & Pack Materials"
       :position (:rpm-inventory-position derived)
       :value (:rpm-inventory-value derived)
       :inflows (:rpm-inflows derived)
       :outflows (:rpm-outflows derived)
       :unit "units"})]))

;; -----------------------------------------------------------------------------
;; Stats Section
;; -----------------------------------------------------------------------------

(defn stats-section []
  (let [obs (state/get-observations)
        derived (state/get-derived)]
    [:div {:class "grid grid-cols-4 gap-4"}
     (c/simple-stat-card
      {:label "Demand Mean"
       :value (.toLocaleString (get-in derived [:demand-distribution :mean]))
       :unit "cases"})

     (c/simple-stat-card
      {:label "Production Capacity"
       :value (.toLocaleString (:fg-inflows derived))
       :unit "cases"})

     (c/simple-stat-card
      {:label "Supplier OTIF"
       :value (str (.toFixed (* (get-in obs [:supplier-otif-dist :value]) 100) 0) "%")
       :status (if (< (get-in obs [:supplier-otif-dist :value]) 0.85) :warning :good)})

     (c/simple-stat-card
      {:label "Forecast Error"
       :value (str "+" (.toFixed (* (get-in obs [:forecast-error-dist :value]) 100) 0) "%")
       :status (if (> (get-in obs [:forecast-error-dist :value]) 0.05) :warning :good)})]))

;; -----------------------------------------------------------------------------
;; WhyExplainer Section
;; -----------------------------------------------------------------------------

(defn get-kpi-explanation [kpi-id derived obs]
  (case kpi-id
    :total-inventory-value
    {:title "Total Inventory Value Analysis"
     :summary (str "Inventory value of $" (.toFixed (/ (:total-inventory-value derived) 1000000) 2)
                   "M is above target due to combination of supply and demand factors.")
     :root-causes [{:contribution 55
                    :title "FG Inventory Buildup"
                    :detail (str "FG position at " (.toLocaleString (:fg-inventory-position derived)) " cases")
                    :source "WMS"
                    :recorded "Nov 25, 2025"}
                   {:contribution 30
                    :title "RPM Excess Stock"
                    :detail (str "RPM position at " (.toLocaleString (:rpm-inventory-position derived)) " units")
                    :source "WMS"
                    :recorded "Nov 25, 2025"}
                   {:contribution 15
                    :title "Unit Cost Pressure"
                    :detail (str "FG unit cost at $" (get-in obs [:fg-unit-cost :value]) "/case")
                    :source "Finance"
                    :recorded "Nov 1, 2025"}]
     :actions [{:text "Accelerate sales promotions" :owner "Sales"}
               {:text "Reduce production plan" :owner "S&OP"}
               {:text "Defer incoming POs" :owner "Procurement"}]}

    :service-risk
    {:title "Service Risk Analysis"
     :summary (str "Service risk at " (.toFixed (* (:service-risk derived) 100) 1)
                   "% indicates high probability of missing customer fulfillment targets.")
     :root-causes [{:contribution 70
                    :title "Supply Constraint"
                    :detail (str "Supplier OTIF at " (.toFixed (* (get-in obs [:supplier-otif-dist :value]) 100) 0) "% (target: 92%)")
                    :source "Vendor Mgmt"
                    :recorded "Nov 23, 2025"
                    :supplier "ACME Corp"}
                   {:contribution 30
                    :title "Demand Spike"
                    :detail (str "Forecast error running +" (.toFixed (* (get-in obs [:forecast-error-dist :value]) 100) 0) "%")
                    :source "Analytics"
                    :recorded "Nov 18, 2025"}]
     :actions [{:text "Expedite open POs with ACME Corp" :owner "Procurement"}
               {:text "Qualify alternate supplier for Component X" :owner "Procurement"}
               {:text "Review promo forecast methodology" :owner "S&OP"}]}

    :cash-impact
    {:title "Cash Impact Analysis"
     :summary (str "Net cash exposure of $" (.toFixed (/ (:cash-impact derived) 1000) 0)
                   "K driven by holding costs and stockout risk.")
     :root-causes [{:contribution 60
                    :title "Holding Costs"
                    :detail (str "$" (.toFixed (/ (:holding-cost derived) 1000) 0) "K monthly holding cost")
                    :source "Finance"
                    :recorded "Nov 1, 2025"}
                   {:contribution 40
                    :title "Stockout Exposure"
                    :detail (str "$" (.toFixed (/ (:stockout-cost derived) 1000) 0) "K potential stockout cost")
                    :source "Finance"
                    :recorded "Nov 1, 2025"}]
     :actions [{:text "Reduce safety stock levels" :owner "S&OP"}
               {:text "Negotiate consignment terms" :owner "Procurement"}
               {:text "Review cost of failure assumptions" :owner "Finance"}]}

    nil))

(defn why-explainer-section []
  (let [selected-kpi (:selected-kpi @state/!app-state)
        derived (state/get-derived)
        obs (state/get-observations)]
    (when selected-kpi
      (when-let [explanation (get-kpi-explanation selected-kpi derived obs)]
        (c/why-explainer
         (assoc explanation :on-simulate [[:scenario/start] [:app/set-view :scenario]]))))))

;; -----------------------------------------------------------------------------
;; Main Dashboard View
;; -----------------------------------------------------------------------------

(defn dashboard-view []
  [:div {:class "space-y-8"}
   ;; KPIs
   (kpi-section)

   ;; WhyExplainer (shown when KPI selected)
   (why-explainer-section)

   ;; Inventory Pools
   (inventory-pools)

   ;; Stats
   (stats-section)])
