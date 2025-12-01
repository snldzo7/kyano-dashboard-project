(ns nestle.data.observations
  "Static observation data - current state of working capital facts")

;; -----------------------------------------------------------------------------
;; Current Observations (latest values)
;; Each observation has :value :unit :source :label :tx-time
;; -----------------------------------------------------------------------------

(def observations
  {:consensus-forecast-qty
   {:value 55000
    :unit "cases"
    :source "S&OP/IBP"
    :label "Consensus Forecast"
    :tx-time "2025-11-20T09:00:00Z"}

   :forecast-error-dist
   {:value 0.12
    :unit "ratio"
    :source "Analytics"
    :label "Forecast Error"
    :tx-time "2025-11-18T14:30:00Z"}

   :order-variability-cv
   {:value 0.35
    :unit "ratio"
    :source "ERP Sales"
    :label "Order Variability CV"
    :tx-time "2025-11-19T11:00:00Z"}

   :planned-production-qty
   {:value 55000
    :unit "cases"
    :source "SAP/APO"
    :label "Planned Production"
    :tx-time "2025-11-15T08:00:00Z"}

   :mfg-adherence-pct
   {:value 0.92
    :unit "ratio"
    :source "MES"
    :label "Mfg Adherence"
    :tx-time "2025-11-22T16:00:00Z"}

   :production-lot-size
   {:value 5000
    :unit "cases"
    :source "Master Data"
    :label "Production Lot Size"
    :tx-time "2025-01-01T00:00:00Z"}

   :rpm-consumption-ratio
   {:value 2.5
    :unit "units/case"
    :source "PLM/BOM"
    :label "RPM Consumption Ratio"
    :tx-time "2025-01-01T00:00:00Z"}

   :fg-opening-stock
   {:value 42000
    :unit "cases"
    :source "WMS"
    :label "FG Opening Stock"
    :tx-time "2025-11-25T00:00:00Z"}

   :fg-blocked-stock-proj
   {:value 3500
    :unit "cases"
    :source "Quality/WMS"
    :label "FG Blocked Stock"
    :tx-time "2025-11-24T10:00:00Z"}

   :rpm-opening-stock
   {:value 180000
    :unit "units"
    :source "WMS"
    :label "RPM Opening Stock"
    :tx-time "2025-11-25T00:00:00Z"}

   :fg-unit-cost
   {:value 45
    :unit "$/case"
    :source "Finance"
    :label "FG Unit Cost"
    :tx-time "2025-11-01T00:00:00Z"}

   :rpm-unit-cost
   {:value 8.5
    :unit "$/unit"
    :source "Finance"
    :label "RPM Unit Cost"
    :tx-time "2025-11-01T00:00:00Z"}

   :open-po-qty-rpm
   {:value 95000
    :unit "units"
    :source "ERP MM"
    :label "Open PO Qty (RPM)"
    :tx-time "2025-11-20T12:00:00Z"}

   :supplier-otif-dist
   {:value 0.78
    :unit "ratio"
    :source "Vendor Mgmt"
    :label "Supplier OTIF"
    :tx-time "2025-11-23T09:00:00Z"}

   :lead-time-variability
   {:value 3.5
    :unit "days"
    :source "Logistics"
    :label "Lead Time Variability"
    :tx-time "2025-11-22T14:00:00Z"}

   :material-shelf-life
   {:value 180
    :unit "days"
    :source "Master Data"
    :label "Material Shelf Life"
    :tx-time "2025-01-01T00:00:00Z"}

   :target-cof-pct
   {:value 0.95
    :unit "ratio"
    :source "Strategy"
    :label "Target COF %"
    :tx-time "2025-10-01T00:00:00Z"}

   :cost-of-failure-unit
   {:value 125
    :unit "$/case"
    :source "Finance"
    :label "Cost of Failure"
    :tx-time "2025-11-01T00:00:00Z"}

   :holding-cost-rate
   {:value 0.18
    :unit "ratio/year"
    :source "Finance"
    :label "Holding Cost Rate"
    :tx-time "2025-11-01T00:00:00Z"}})

;; -----------------------------------------------------------------------------
;; Historical Observations (for time travel)
;; Each key maps to a vector of historical values
;; -----------------------------------------------------------------------------

(def historical-observations
  {:supplier-otif-dist
   [{:value 0.94 :tx-time "2025-09-01T08:00:00Z" :source "Vendor Mgmt" :note "Q3 close - strong performance"}
    {:value 0.91 :tx-time "2025-09-15T09:30:00Z" :source "Vendor Mgmt" :note "Early Q4 - slight decline"}
    {:value 0.87 :tx-time "2025-10-01T10:00:00Z" :source "Vendor Mgmt" :note "ACME capacity issues reported"}
    {:value 0.82 :tx-time "2025-10-15T08:45:00Z" :source "Vendor Mgmt" :note "Continued degradation"}
    {:value 0.78 :tx-time "2025-11-01T09:00:00Z" :source "Vendor Mgmt" :note "Critical level reached"}
    {:value 0.78 :tx-time "2025-11-23T09:00:00Z" :source "Vendor Mgmt" :note "Current state - action required"}]

   :forecast-error-dist
   [{:value 0.03 :tx-time "2025-09-01T08:00:00Z" :source "Analytics" :note "Q3 close - within tolerance"}
    {:value 0.05 :tx-time "2025-09-15T14:00:00Z" :source "Analytics" :note "Slight increase"}
    {:value 0.08 :tx-time "2025-10-01T11:00:00Z" :source "Analytics" :note "Promo uplift underestimated"}
    {:value 0.10 :tx-time "2025-10-15T13:30:00Z" :source "Analytics" :note "Pattern continuing"}
    {:value 0.12 :tx-time "2025-11-01T10:00:00Z" :source "Analytics" :note "Systematic bias identified"}
    {:value 0.12 :tx-time "2025-11-18T14:30:00Z" :source "Analytics" :note "Current state"}]

   :consensus-forecast-qty
   [{:value 48000 :tx-time "2025-09-01T08:00:00Z" :source "S&OP/IBP" :note "Q4 initial forecast"}
    {:value 50000 :tx-time "2025-09-15T16:00:00Z" :source "S&OP/IBP" :note "S&OP cycle revision"}
    {:value 52000 :tx-time "2025-10-01T09:00:00Z" :source "S&OP/IBP" :note "October S&OP"}
    {:value 55000 :tx-time "2025-10-15T15:00:00Z" :source "S&OP/IBP" :note "Holiday demand signal"}
    {:value 55000 :tx-time "2025-11-20T09:00:00Z" :source "S&OP/IBP" :note "Current consensus"}]

   :order-variability-cv
   [{:value 0.25 :tx-time "2025-09-01T08:00:00Z" :source "ERP Sales" :note "Stable Q3 demand"}
    {:value 0.28 :tx-time "2025-09-15T10:00:00Z" :source "ERP Sales" :note "Holiday prep begins"}
    {:value 0.30 :tx-time "2025-10-01T09:00:00Z" :source "ERP Sales" :note "Increased volatility"}
    {:value 0.33 :tx-time "2025-10-15T11:00:00Z" :source "ERP Sales" :note "Pre-holiday spike"}
    {:value 0.35 :tx-time "2025-11-01T08:30:00Z" :source "ERP Sales" :note "Peak variability"}
    {:value 0.35 :tx-time "2025-11-19T11:00:00Z" :source "ERP Sales" :note "Current state"}]

   :planned-production-qty
   [{:value 45000 :tx-time "2025-09-01T06:00:00Z" :source "SAP/APO" :note "Q4 baseline plan"}
    {:value 48000 :tx-time "2025-09-15T07:00:00Z" :source "SAP/APO" :note "First adjustment"}
    {:value 50000 :tx-time "2025-10-01T06:30:00Z" :source "SAP/APO" :note "October plan"}
    {:value 52000 :tx-time "2025-10-15T07:00:00Z" :source "SAP/APO" :note "Demand signal response"}
    {:value 55000 :tx-time "2025-11-01T06:00:00Z" :source "SAP/APO" :note "November target"}
    {:value 55000 :tx-time "2025-11-15T08:00:00Z" :source "SAP/APO" :note "Current plan"}]

   :mfg-adherence-pct
   [{:value 0.96 :tx-time "2025-09-01T16:00:00Z" :source "MES" :note "Q3 close - excellent"}
    {:value 0.95 :tx-time "2025-09-15T15:30:00Z" :source "MES" :note "Slight decline"}
    {:value 0.94 :tx-time "2025-10-01T16:00:00Z" :source "MES" :note "Equipment issues"}
    {:value 0.93 :tx-time "2025-10-15T15:00:00Z" :source "MES" :note "Continued pressure"}
    {:value 0.92 :tx-time "2025-11-01T16:30:00Z" :source "MES" :note "RPM shortages impact"}
    {:value 0.92 :tx-time "2025-11-22T16:00:00Z" :source "MES" :note "Current state"}]

   :fg-opening-stock
   [{:value 38000 :tx-time "2025-09-01T00:00:00Z" :source "WMS" :note "September opening"}
    {:value 40000 :tx-time "2025-09-15T00:00:00Z" :source "WMS" :note "Mid-September"}
    {:value 39000 :tx-time "2025-10-01T00:00:00Z" :source "WMS" :note "October opening"}
    {:value 41000 :tx-time "2025-10-15T00:00:00Z" :source "WMS" :note "Mid-October"}
    {:value 43000 :tx-time "2025-11-01T00:00:00Z" :source "WMS" :note "November opening"}
    {:value 42000 :tx-time "2025-11-25T00:00:00Z" :source "WMS" :note "Current stock"}]

   :fg-blocked-stock-proj
   [{:value 2000 :tx-time "2025-09-01T08:00:00Z" :source "Quality/WMS" :note "Low blocked level"}
    {:value 2200 :tx-time "2025-09-15T09:00:00Z" :source "Quality/WMS" :note "Minor quality hold"}
    {:value 2800 :tx-time "2025-10-01T08:30:00Z" :source "Quality/WMS" :note "Packaging issue batch"}
    {:value 3200 :tx-time "2025-10-15T09:00:00Z" :source "Quality/WMS" :note "Testing in progress"}
    {:value 3500 :tx-time "2025-11-01T08:00:00Z" :source "Quality/WMS" :note "Elevated holds"}
    {:value 3500 :tx-time "2025-11-24T10:00:00Z" :source "Quality/WMS" :note "Current blocked"}]

   :rpm-opening-stock
   [{:value 200000 :tx-time "2025-09-01T00:00:00Z" :source "WMS" :note "September opening"}
    {:value 195000 :tx-time "2025-09-15T00:00:00Z" :source "WMS" :note "Drawing down"}
    {:value 190000 :tx-time "2025-10-01T00:00:00Z" :source "WMS" :note "October opening"}
    {:value 185000 :tx-time "2025-10-15T00:00:00Z" :source "WMS" :note "Supplier delays"}
    {:value 182000 :tx-time "2025-11-01T00:00:00Z" :source "WMS" :note "Critical level"}
    {:value 180000 :tx-time "2025-11-25T00:00:00Z" :source "WMS" :note "Current stock"}]

   :open-po-qty-rpm
   [{:value 120000 :tx-time "2025-09-01T10:00:00Z" :source "ERP MM" :note "Strong order book"}
    {:value 115000 :tx-time "2025-09-15T11:00:00Z" :source "ERP MM" :note "POs receiving"}
    {:value 108000 :tx-time "2025-10-01T10:30:00Z" :source "ERP MM" :note "Supplier constraints"}
    {:value 100000 :tx-time "2025-10-15T11:00:00Z" :source "ERP MM" :note "Reduced orders"}
    {:value 95000 :tx-time "2025-11-01T10:00:00Z" :source "ERP MM" :note "Tight supply"}
    {:value 95000 :tx-time "2025-11-20T12:00:00Z" :source "ERP MM" :note "Current POs"}]

   :lead-time-variability
   [{:value 2.0 :tx-time "2025-09-01T12:00:00Z" :source "Logistics" :note "Stable lead times"}
    {:value 2.3 :tx-time "2025-09-15T13:00:00Z" :source "Logistics" :note "Minor delays"}
    {:value 2.8 :tx-time "2025-10-01T12:30:00Z" :source "Logistics" :note "Carrier issues"}
    {:value 3.2 :tx-time "2025-10-15T13:00:00Z" :source "Logistics" :note "Increased variability"}
    {:value 3.5 :tx-time "2025-11-01T12:00:00Z" :source "Logistics" :note "Peak season impact"}
    {:value 3.5 :tx-time "2025-11-22T14:00:00Z" :source "Logistics" :note "Current state"}]

   :fg-unit-cost
   [{:value 42 :tx-time "2025-09-01T00:00:00Z" :source "Finance" :note "Q3 standard cost"}
    {:value 43 :tx-time "2025-10-01T00:00:00Z" :source "Finance" :note "Minor increase"}
    {:value 44 :tx-time "2025-10-15T00:00:00Z" :source "Finance" :note "Material cost pressure"}
    {:value 45 :tx-time "2025-11-01T00:00:00Z" :source "Finance" :note "Current standard cost"}]

   :rpm-unit-cost
   [{:value 7.5 :tx-time "2025-09-01T00:00:00Z" :source "Finance" :note "Q3 standard cost"}
    {:value 7.8 :tx-time "2025-10-01T00:00:00Z" :source "Finance" :note "Supplier price increase"}
    {:value 8.2 :tx-time "2025-10-15T00:00:00Z" :source "Finance" :note "Expedite premiums"}
    {:value 8.5 :tx-time "2025-11-01T00:00:00Z" :source "Finance" :note "Current cost"}]

   :holding-cost-rate
   [{:value 0.15 :tx-time "2025-09-01T00:00:00Z" :source "Finance" :note "Standard rate"}
    {:value 0.16 :tx-time "2025-10-01T00:00:00Z" :source "Finance" :note "Rate revision"}
    {:value 0.18 :tx-time "2025-11-01T00:00:00Z" :source "Finance" :note "Current rate"}]

   :cost-of-failure-unit
   [{:value 100 :tx-time "2025-09-01T00:00:00Z" :source "Finance" :note "Q3 baseline"}
    {:value 110 :tx-time "2025-10-01T00:00:00Z" :source "Finance" :note "Updated estimate"}
    {:value 125 :tx-time "2025-11-01T00:00:00Z" :source "Finance" :note "Current estimate"}]

   :target-cof-pct
   [{:value 0.97 :tx-time "2025-09-01T00:00:00Z" :source "Strategy" :note "Q3 target"}
    {:value 0.96 :tx-time "2025-10-01T00:00:00Z" :source "Strategy" :note "Revised down"}
    {:value 0.95 :tx-time "2025-10-15T00:00:00Z" :source "Strategy" :note "Current target"}]

   :production-lot-size
   [{:value 5000 :tx-time "2025-01-01T00:00:00Z" :source "Master Data" :note "Standard lot size"}]

   :rpm-consumption-ratio
   [{:value 2.5 :tx-time "2025-01-01T00:00:00Z" :source "PLM/BOM" :note "Standard BOM ratio"}]

   :material-shelf-life
   [{:value 180 :tx-time "2025-01-01T00:00:00Z" :source "Master Data" :note "Standard shelf life"}]})

;; -----------------------------------------------------------------------------
;; Sparkline data (for trends)
;; -----------------------------------------------------------------------------

(def sparkline-data
  {:inventory-value [2.1 2.0 2.15 2.3 2.25 2.4 2.5 2.48]
   :service-risk [0.05 0.08 0.12 0.15 0.18 0.20 0.22 0.23]
   :cash-impact [420 435 450 460 470 475 480 485]
   :supplier-otif [0.94 0.91 0.87 0.82 0.78 0.78]
   :forecast-error [0.03 0.05 0.08 0.10 0.12 0.12]})
