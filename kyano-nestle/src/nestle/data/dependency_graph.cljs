(ns nestle.data.dependency-graph
  "Dependency graph structure for lineage visualization - matches React exactly")

;; -----------------------------------------------------------------------------
;; Dependency Graph Definition
;; Matches React's dependencyGraph exactly
;; -----------------------------------------------------------------------------

(def dependency-graph
  {;; Level 0 - Outputs
   :total-inventory-value
   {:level 0
    :label "Total Inventory Value"
    :type :output
    :dependencies [:fg-inventory-value :rpm-inventory-value]
    :formula "fg_inventory_value + rpm_inventory_value"}

   :service-risk
   {:level 0
    :label "Service Risk"
    :type :output
    :dependencies [:fg-inventory-position :demand-distribution :target-cof-pct]
    :formula "P(FG_available < Demand) | target_cof"}

   :cash-impact
   {:level 0
    :label "Cash Impact"
    :type :output
    :dependencies [:total-inventory-value :holding-cost-rate :service-risk :cost-of-failure-unit :consensus-forecast-qty]
    :formula "holding_cost + stockout_exposure"}

   ;; Level 1 - Inventory Pools
   :fg-inventory-value
   {:level 1
    :label "FG Inventory Value"
    :type :derived
    :dependencies [:fg-inventory-position :fg-unit-cost]
    :formula "fg_position x fg_unit_cost"}

   :rpm-inventory-value
   {:level 1
    :label "RPM Inventory Value"
    :type :derived
    :dependencies [:rpm-inventory-position :rpm-unit-cost]
    :formula "rpm_position x rpm_unit_cost"}

   :fg-inventory-position
   {:level 1
    :label "FG Inventory Position"
    :type :derived
    :dependencies [:fg-opening-stock :fg-inflows :fg-outflows :fg-blocked-stock-proj]
    :formula "opening + inflows - outflows - blocked"}

   :rpm-inventory-position
   {:level 1
    :label "RPM Inventory Position"
    :type :derived
    :dependencies [:rpm-opening-stock :rpm-inflows :rpm-outflows]
    :formula "opening + inflows - outflows"}

   ;; Level 2 - Flow Components
   :fg-inflows
   {:level 2
    :label "FG Inflows (Production)"
    :type :derived
    :dependencies [:planned-production-qty :mfg-adherence-pct :rpm-availability]
    :formula "min(plan x adherence, rpm_avail / bom)"}

   :fg-outflows
   {:level 2
    :label "FG Outflows (Demand)"
    :type :derived
    :dependencies [:demand-distribution]
    :formula "demand_mean"}

   :rpm-inflows
   {:level 2
    :label "RPM Inflows (Supplier)"
    :type :derived
    :dependencies [:open-po-qty-rpm :supplier-otif-dist]
    :formula "open_po x otif"}

   :rpm-outflows
   {:level 2
    :label "RPM Outflows (Consumption)"
    :type :derived
    :dependencies [:fg-inflows :rpm-consumption-ratio]
    :formula "fg_inflows x bom_ratio"}

   :rpm-availability
   {:level 2
    :label "RPM Availability"
    :type :derived
    :dependencies [:rpm-opening-stock :rpm-inflows]
    :formula "rpm_opening + rpm_inflows"}

   :demand-distribution
   {:level 2
    :label "Demand Distribution"
    :type :derived
    :dependencies [:consensus-forecast-qty :forecast-error-dist :order-variability-cv]
    :formula "N(forecast x (1 + error), forecast x cv)"}

   ;; Level 3 - Observations (leaf nodes)
   :consensus-forecast-qty
   {:level 3 :label "Consensus Forecast" :type :observation :dependencies []}

   :forecast-error-dist
   {:level 3 :label "Forecast Error" :type :observation :dependencies []}

   :order-variability-cv
   {:level 3 :label "Order Variability CV" :type :observation :dependencies []}

   :planned-production-qty
   {:level 3 :label "Planned Production" :type :observation :dependencies []}

   :mfg-adherence-pct
   {:level 3 :label "Mfg Adherence" :type :observation :dependencies []}

   :rpm-consumption-ratio
   {:level 3 :label "RPM Consumption Ratio" :type :observation :dependencies []}

   :fg-opening-stock
   {:level 3 :label "FG Opening Stock" :type :observation :dependencies []}

   :fg-blocked-stock-proj
   {:level 3 :label "FG Blocked Stock" :type :observation :dependencies []}

   :rpm-opening-stock
   {:level 3 :label "RPM Opening Stock" :type :observation :dependencies []}

   :fg-unit-cost
   {:level 3 :label "FG Unit Cost" :type :observation :dependencies []}

   :rpm-unit-cost
   {:level 3 :label "RPM Unit Cost" :type :observation :dependencies []}

   :open-po-qty-rpm
   {:level 3 :label "Open PO Qty (RPM)" :type :observation :dependencies []}

   :supplier-otif-dist
   {:level 3 :label "Supplier OTIF" :type :observation :dependencies []}

   :lead-time-variability
   {:level 3 :label "Lead Time Variability" :type :observation :dependencies []}

   :material-shelf-life
   {:level 3 :label "Material Shelf Life" :type :observation :dependencies []}

   :target-cof-pct
   {:level 3 :label "Target COF %" :type :observation :dependencies []}

   :cost-of-failure-unit
   {:level 3 :label "Cost of Failure" :type :observation :dependencies []}

   :holding-cost-rate
   {:level 3 :label "Holding Cost Rate" :type :observation :dependencies []}})

;; -----------------------------------------------------------------------------
;; Graph Traversal Functions
;; -----------------------------------------------------------------------------

(defn get-ancestors
  "Get all ancestors (upstream dependencies) of a node"
  ([node-id] (get-ancestors node-id #{}))
  ([node-id visited]
   (if (contains? visited node-id)
     []
     (let [visited (conj visited node-id)
           node (get dependency-graph node-id)
           deps (or (:dependencies node) [])]
       (into deps
             (mapcat #(get-ancestors % visited) deps))))))

(defn get-descendants
  "Get all descendants (downstream dependents) of a node"
  [node-id]
  (let [descendants (atom [])]
    (doseq [[id node] dependency-graph]
      (when (some #{node-id} (:dependencies node))
        (swap! descendants conj id)
        (swap! descendants into (get-descendants id))))
    (vec (distinct @descendants))))

;; -----------------------------------------------------------------------------
;; Node Grouping by Level
;; -----------------------------------------------------------------------------

(defn nodes-by-level
  "Get all nodes grouped by their level"
  []
  (group-by (fn [[_ node]] (:level node)) dependency-graph))

(defn level-0-nodes [] (keys (filter (fn [[_ v]] (= 0 (:level v))) dependency-graph)))
(defn level-1-nodes [] (keys (filter (fn [[_ v]] (= 1 (:level v))) dependency-graph)))
(defn level-2-nodes [] (keys (filter (fn [[_ v]] (= 2 (:level v))) dependency-graph)))
(defn level-3-nodes [] (keys (filter (fn [[_ v]] (= 3 (:level v))) dependency-graph)))

;; -----------------------------------------------------------------------------
;; Node Styling Configuration
;; -----------------------------------------------------------------------------

(def type-styles
  {:output {:bg "bg-cyan-900"
            :border "border-cyan-500"
            :text "text-cyan-300"
            :bg-dim "bg-cyan-950"
            :border-dim "border-cyan-800"
            :text-dim "text-cyan-600"}
   :derived {:bg "bg-violet-900"
             :border "border-violet-500"
             :text "text-violet-300"
             :bg-dim "bg-violet-950"
             :border-dim "border-violet-800"
             :text-dim "text-violet-600"}
   :observation {:bg "bg-amber-900"
                 :border "border-amber-500"
                 :text "text-amber-300"
                 :bg-dim "bg-amber-950"
                 :border-dim "border-amber-800"
                 :text-dim "text-amber-600"}})

(defn get-node-style
  "Get styles for a node based on its type and highlight state"
  [node-id highlighted? dimmed?]
  (let [node (get dependency-graph node-id)
        node-type (:type node)
        styles (get type-styles node-type (type-styles :observation))]
    (if dimmed?
      {:bg (:bg-dim styles)
       :border (:border-dim styles)
       :text (:text-dim styles)}
      {:bg (:bg styles)
       :border (:border styles)
       :text (:text styles)})))
