(ns nestle.data.stakeholders
  "Stakeholder definitions for RACI and collaboration")

;; -----------------------------------------------------------------------------
;; CPG Manufacturing Stakeholders
;; -----------------------------------------------------------------------------

(def cpg-stakeholders
  {:demand-planner
   {:id :demand-planner
    :name "Demand Planner"
    :icon :chart
    :org :cpg
    :color "#3b82f6"
    :kpis ["Forecast Accuracy" "Bias" "Promo Uplift"]
    :expertise ["Demand Sensing" "Statistical Forecasting" "Promo Planning"]}

   :inventory-planner
   {:id :inventory-planner
    :name "Inventory Planner"
    :icon :package
    :org :cpg
    :color "#10b981"
    :kpis ["Service Level (COF %)" "Safety Stock" "Inventory Turns"]
    :expertise ["Safety Stock" "Inventory Optimization" "Service Level"]}

   :production-scheduler
   {:id :production-scheduler
    :name "Production Scheduler"
    :icon :factory
    :org :cpg
    :color "#f59e0b"
    :kpis ["Schedule Adherence" "Lot Efficiency" "Changeover Time"]
    :expertise ["Production Scheduling" "Capacity Planning" "Lot Sizing"]}

   :supply-planner
   {:id :supply-planner
    :name "Supply Planner"
    :icon :truck
    :org :cpg
    :color "#8b5cf6"
    :kpis ["Supplier OTIF" "Lead Time Variability" "Material Availability"]
    :expertise ["Supplier Management" "Lead Time" "Risk Mitigation"]}

   :sales-kam
   {:id :sales-kam
    :name "Sales/KAM"
    :icon :handshake
    :org :cpg
    :color "#ec4899"
    :kpis ["Promo Calendar" "Order Variability" "Fill Rate"]
    :expertise ["Customer Relations" "Demand Shaping" "Allocation"]}})

;; -----------------------------------------------------------------------------
;; Retail Partner Stakeholders
;; -----------------------------------------------------------------------------

(def retail-stakeholders
  {:category-manager
   {:id :category-manager
    :name "Category Manager"
    :icon :shopping
    :org :retail
    :color "#06b6d4"
    :kpis ["Category Growth" "Promo ROI" "OSA"]
    :expertise ["Category Strategy" "Assortment" "Pricing"]}

   :replenishment-planner
   {:id :replenishment-planner
    :name "Replenishment Planner"
    :icon :clipboard
    :org :retail
    :color "#84cc16"
    :kpis ["DC Service" "Order Stability" "Store OSA"]
    :expertise ["Store Replenishment" "Order Planning" "DC Operations"]}

   :retail-demand-planner
   {:id :retail-demand-planner
    :name "Retail Demand Planner"
    :icon :trending
    :org :retail
    :color "#f97316"
    :kpis ["POS Forecast" "Baseline Accuracy" "Uplift Accuracy"]
    :expertise ["POS Analytics" "Demand Forecasting" "Seasonality"]}

   :buyer-procurement
   {:id :buyer-procurement
    :name "Buyer/Procurement"
    :icon :briefcase
    :org :retail
    :color "#a855f7"
    :kpis ["MOQ Compliance" "Fill-Rate" "Lead Time"]
    :expertise ["Vendor Management" "Order Optimization" "Cost Control"]}

   :dc-operations
   {:id :dc-operations
    :name "DC Operations"
    :icon :warehouse
    :org :retail
    :color "#64748b"
    :kpis ["Inbound Capacity" "Slotting" "Pick Rate"]
    :expertise ["Warehouse Operations" "Capacity Planning" "Labor"]}})

;; -----------------------------------------------------------------------------
;; All stakeholders
;; -----------------------------------------------------------------------------

(def stakeholders
  (merge cpg-stakeholders retail-stakeholders))

;; -----------------------------------------------------------------------------
;; Historical Decisions
;; -----------------------------------------------------------------------------

(def historical-decisions
  [{:id "decision-2025-09-15-001"
    :timestamp "2025-09-15T16:30:00Z"
    :scenario "Q4 Initial Planning"
    :participants [:sop :supply-chain :finance]
    :facts-known-at {:supplier-otif-dist 0.91 :forecast-error-dist 0.05 :consensus-forecast-qty 50000}
    :outcome "Approved Q4 production plan at 50,000 cases"
    :status :executed}

   {:id "decision-2025-10-15-001"
    :timestamp "2025-10-15T15:45:00Z"
    :scenario "Mid-Q4 Adjustment"
    :participants [:sop :procurement :analytics]
    :facts-known-at {:supplier-otif-dist 0.82 :forecast-error-dist 0.10 :consensus-forecast-qty 55000}
    :outcome "Increased forecast to 55K, flagged supplier risk"
    :status :executed}

   {:id "decision-2025-11-01-001"
    :timestamp "2025-11-01T11:00:00Z"
    :scenario "Supplier Risk Escalation"
    :participants [:procurement :vendor-mgmt :sop]
    :facts-known-at {:supplier-otif-dist 0.78 :forecast-error-dist 0.12 :open-po-qty-rpm 95000}
    :outcome "Initiated alternate supplier qualification"
    :status :in-progress}])
