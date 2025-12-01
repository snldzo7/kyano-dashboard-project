(ns nestle.data.collaborative
  "CPG x Retail Collaborative Stakeholder Ecosystem Data
   Based on Sandra Mitchell's complexity theory for dual-organization systems")

;; -----------------------------------------------------------------------------
;; Stakeholder Ecosystem - CPG Manufacturing
;; -----------------------------------------------------------------------------

(def cpg-roles
  {:demand-planner
   {:name "Demand Planner"
    :avatar "📊"
    :kpis ["Forecast Accuracy (MAPE)" "Promo Uplift Accuracy" "Forecast Bias" "Weekly Error Distribution"]
    :risks ["Promo timing misalignment" "Baseline drift"]
    :controls ["S&OP consensus" "Promotional forecasts"]
    :expertise ["Historical patterns" "Seasonality" "Market trends"]
    :metadata [:consensus-forecast-qty :forecast-error-dist]}

   :inventory-planner
   {:name "Inventory Planner"
    :avatar "📦"
    :kpis ["Service Level (COF %)" "Safety Stock Attainment" "Inventory Turns" "Landing P10/P50/P90"]
    :risks ["Stockout cascade" "Obsolescence"]
    :controls ["Safety stock policy" "Reorder points"]
    :expertise ["Risk modeling" "Service levels" "Cost optimization"]
    :metadata [:fg-opening-stock :target-cof-pct :holding-cost-rate]}

   :production-scheduler
   {:name "Production Scheduler"
    :avatar "🏭"
    :kpis ["Schedule Adherence" "Lot Size Efficiency" "Changeover Compliance" "FG Availability vs Plan"]
    :risks ["Production bottlenecks" "Quality issues"]
    :controls ["Production schedule" "Lot sizing" "Changeovers"]
    :expertise ["Manufacturing constraints" "Capacity planning"]
    :metadata [:planned-production-qty :mfg-adherence-pct :production-lot-size]}

   :supply-planner
   {:name "Supply Planner (RPM)"
    :avatar "🚚"
    :kpis ["Supplier OTIF Distribution" "Lead Time Variability" "RPM Stockouts Risk" "MOQ & Shelf-Life Loss"]
    :risks ["Supplier disruption" "Material shortage"]
    :controls ["Purchase orders" "Supplier relationships"]
    :expertise ["Supplier performance" "Lead time management"]
    :metadata [:open-po-qty-rpm :supplier-otif-dist :lead-time-variability]}

   :sales-kam
   {:name "Sales/KAM"
    :avatar "🤝"
    :kpis ["Promo Calendar Accuracy" "Customer Order Variability CV" "Trade Terms" "Fill Rate at Retailer DC"]
    :risks ["Customer disputes" "Lost sales"]
    :controls ["Customer negotiations" "Trade promotions"]
    :expertise ["Customer relationships" "Market dynamics"]
    :metadata [:order-variability-cv :cost-of-failure-unit]}})

;; -----------------------------------------------------------------------------
;; Stakeholder Ecosystem - Retail Partner
;; -----------------------------------------------------------------------------

(def retail-roles
  {:category-manager
   {:name "Category Manager"
    :avatar "🛍️"
    :mandatory? true
    :kpis ["Category Growth" "Promo ROI" "Price Elasticity Impact" "Assortment Availability" "OSA"]
    :risks ["Category decline" "Competitive pressure"]
    :controls ["Category strategy" "Promo calendar" "Assortment"]
    :expertise ["Consumer behavior" "Category dynamics"]
    :retail-metadata [:category-growth-rate :promo-roi :osa-target]}

   :replenishment-planner
   {:name "Replenishment Planner"
    :avatar "📋"
    :mandatory? true
    :kpis ["DC Service Level" "Order Frequency & Stability" "Safety Stock vs Policy" "Store-Level OSA"]
    :risks ["Stock-outs" "Overstock"]
    :controls ["Replenishment parameters" "Order timing"]
    :expertise ["Demand sensing" "Inventory optimization"]
    :retail-metadata [:dc-service-level :replenishment-frequency :store-osa]}

   :retail-demand-planner
   {:name "Retail Demand Planner"
    :avatar "📈"
    :kpis ["POS Forecast Accuracy" "Baseline vs Uplift Accuracy" "Substitution Effect" "Negative Sales Flags"]
    :risks ["Forecast bias" "Demand distortion"]
    :controls ["POS forecasts" "Demand shaping"]
    :expertise ["Store-level patterns" "Consumer demand"]
    :retail-metadata [:pos-forecast-accuracy :baseline-uplift-split :substitution-rate]}

   :buyer-procurement
   {:name "Buyer/Procurement"
    :avatar "💼"
    :kpis ["MOQ Compliance" "Delivery Window Compliance" "Fill-Rate from Manufacturer" "Cost/Margin Leakage"]
    :risks ["Supply disruption" "Cost inflation"]
    :controls ["Purchase orders" "Supplier contracts"]
    :expertise ["Supplier negotiation" "Cost management"]
    :retail-metadata [:moq-compliance :delivery-window-adherence :fill-rate-cpg]}

   :dc-operations
   {:name "DC Operations"
    :avatar "🏗️"
    :kpis ["Inbound Capacity" "Slotting Constraints" "Truck Fill/Route Efficiency" "Pick Rate/Delay Risk"]
    :risks ["Capacity overflow" "Operational delays"]
    :controls ["DC scheduling" "Slotting decisions"]
    :expertise ["Warehouse operations" "Logistics"]
    :retail-metadata [:inbound-capacity-util :slotting-efficiency :pick-rate]}})

;; -----------------------------------------------------------------------------
;; Stakeholder Ecosystem Combined
;; -----------------------------------------------------------------------------

(def stakeholder-ecosystem
  {:cpg {:title "CPG Manufacturing"
         :color "#0ea5e9"
         :roles cpg-roles}
   :retail {:title "Retail Partner"
            :color "#10b981"
            :roles retail-roles}})

;; -----------------------------------------------------------------------------
;; Global KPIs (Cross-Organization Success Metrics)
;; -----------------------------------------------------------------------------

(def global-kpis
  {:joint-forecast-accuracy
   {:name "Joint Forecast Accuracy"
    :target 0.85
    :current 0.78
    :trend :improving
    :description "Combined CPG + Retail prediction accuracy"
    :impacted-by [:demand-planner :retail-demand-planner :category-manager]}

   :retailer-dc-service
   {:name "Service Level at Retailer DC"
    :target 0.98
    :current 0.96
    :trend :stable
    :description "Fill rate from CPG to retail distribution"
    :impacted-by [:inventory-planner :replenishment-planner :supply-planner]}

   :on-shelf-availability
   {:name "On-Shelf Availability (OSA)"
    :target 0.97
    :current 0.94
    :trend :declining
    :description "Product availability at store level"
    :impacted-by [:category-manager :dc-operations :replenishment-planner]}

   :inventory-risk-corridor
   {:name "Joint Inventory Risk Corridor"
    :p10 38.2
    :p50 42.5
    :p90 48.1
    :description "Combined CPG + Retail inventory landing positions"
    :impacted-by [:inventory-planner :replenishment-planner :production-scheduler]}

   :total-cost-to-serve
   {:name "Total Cost to Serve (TCTS)"
    :target 12.5
    :current 14.2
    :trend :concerning
    :description "End-to-end value chain cost efficiency (%)"
    :impacted-by [:supply-planner :buyer-procurement :dc-operations]}})

;; -----------------------------------------------------------------------------
;; Global KPI Formula Functions
;; -----------------------------------------------------------------------------

(defn calculate-joint-forecast-accuracy [obs]
  (let [cpg-accuracy (- 1 (get-in obs [:forecast-error-dist :value] 0.12))
        retail-assumed-accuracy 0.82]
    (/ (+ cpg-accuracy retail-assumed-accuracy) 2)))

(defn calculate-retailer-dc-service [obs]
  (* (get-in obs [:supplier-otif-dist :value] 0.78)
     (get-in obs [:mfg-adherence-pct :value] 0.92)
     0.98))

(defn calculate-on-shelf-availability [obs]
  (let [dc-service (* (get-in obs [:supplier-otif-dist :value] 0.78) 0.98)
        retail-execution 0.96]
    (* dc-service retail-execution)))

(defn calculate-inventory-risk-corridor [obs]
  (let [fg (get-in obs [:fg-opening-stock :value] 45000)
        retail-assumed (* fg 0.15)]
    {:p10 (* (+ fg retail-assumed) 0.9)
     :p50 (+ fg retail-assumed)
     :p90 (* (+ fg retail-assumed) 1.14)}))

(defn calculate-total-cost-to-serve [obs]
  (let [holding-cost (* (get-in obs [:holding-cost-rate :value] 0.18) 100)
        service-failure-cost (* (- 1 (get-in obs [:supplier-otif-dist :value] 0.78)) 20)]
    (+ holding-cost service-failure-cost)))

(defn calculate-global-kpis [obs]
  {:joint-forecast-accuracy (calculate-joint-forecast-accuracy obs)
   :retailer-dc-service (calculate-retailer-dc-service obs)
   :on-shelf-availability (calculate-on-shelf-availability obs)
   :inventory-risk-corridor (calculate-inventory-risk-corridor obs)
   :total-cost-to-serve (calculate-total-cost-to-serve obs)})

;; -----------------------------------------------------------------------------
;; Complexity Scenarios (Causation Chains)
;; -----------------------------------------------------------------------------

(def complexity-scenarios
  {:promo-cascade
   {:name "Promotional Cascade Effect"
    :trigger :category-manager
    :description "Retail promo without CPG coordination causes supply chain stress"
    :causation-chain
    [{:node :promo-plan :type :retail-decision :owner :category-manager :label "Retail Promo Launch"}
     {:node :pos-spike :type :market-signal :caused-by :promo-plan :impact 2.3 :label "POS Demand Spike +230%"}
     {:node :dc-strain :type :operational-stress :caused-by :pos-spike :impact 1.8 :label "DC Capacity Strain"}
     {:node :cpg-emergency-production :type :cpg-response :caused-by :dc-strain :impact 1.5 :label "Emergency Production Run"}
     {:node :supplier-pressure :type :upstream-stress :caused-by :cpg-emergency-production :impact 1.2 :label "Supplier Lead Time Pressure"}]
    :global-kpi-impact
    {:joint-forecast-accuracy -0.12
     :retailer-dc-service -0.08
     :total-cost-to-serve 2.1}}

   :supply-disruption
   {:name "Supplier Disruption Impact"
    :trigger :supply-planner
    :description "Upstream supplier issues cascade to retail shelf availability"
    :causation-chain
    [{:node :supplier-delay :type :external-event :owner :supply-planner :label "Supplier OTIF Degradation"}
     {:node :rpm-shortage :type :material-risk :caused-by :supplier-delay :impact 0.78 :label "RPM Shortage Risk"}
     {:node :production-constraint :type :manufacturing-impact :caused-by :rpm-shortage :impact 0.85 :label "Production Constrained"}
     {:node :fg-stockout :type :service-risk :caused-by :production-constraint :impact 0.45 :label "FG Stockout Risk"}
     {:node :retailer-osa-decline :type :retail-impact :caused-by :fg-stockout :impact 0.88 :label "Store OSA Decline"}]
    :global-kpi-impact
    {:retailer-dc-service -0.15
     :on-shelf-availability -0.09
     :total-cost-to-serve 3.5}}

   :forecast-misalignment
   {:name "CPG-Retail Forecast Misalignment"
    :trigger :demand-planner
    :description "Siloed forecasting creates inefficiency and service risk"
    :causation-chain
    [{:node :cpg-forecast :type :cpg-decision :owner :demand-planner :label "CPG Forecast Set"}
     {:node :retail-forecast :type :retail-decision :owner :retail-demand-planner :label "Retail POS Forecast"}
     {:node :forecast-gap :type :planning-conflict :caused-by [:cpg-forecast :retail-forecast] :impact 1.25 :label "Forecast Gap Detected"}
     {:node :inventory-mismatch :type :operational-inefficiency :caused-by :forecast-gap :impact 1.15 :label "Inventory Position Mismatch"}
     {:node :service-degradation :type :performance-impact :caused-by :inventory-mismatch :impact 0.92 :label "Service Level Degradation"}]
    :global-kpi-impact
    {:joint-forecast-accuracy -0.18
     :inventory-risk-corridor :widened
     :total-cost-to-serve 1.8}}})
