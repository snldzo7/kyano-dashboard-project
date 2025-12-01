(ns nestle.views.lineage
  "Decision Lineage Explorer - Interactive dependency graph visualization
   Matches React implementation exactly. Fully data-driven - no functions in components."
  (:require [kyano.ui.primitives :refer [classes]]
            [nestle.data.dependency-graph :as graph]
            [nestle.state :as state]))

;; -----------------------------------------------------------------------------
;; Value Formatting - Matches React getNodeValue exactly
;; -----------------------------------------------------------------------------

(defn format-currency [v]
  (str "$" (.toFixed (/ v 1000000) 2) "M"))

(defn format-percent [v]
  (str (Math/round (* v 100)) "%"))

(defn format-number [v]
  (.toLocaleString (Math/round v)))

(defn get-node-value
  "Get the computed value for a node from state"
  [node-id]
  (let [app-state @state/!app-state
        derived (:derived app-state)
        obs (:observations app-state)]
    (case node-id
      ;; Level 0 - Outputs
      :total-inventory-value (format-currency (:total-inventory-value derived))
      :service-risk (format-percent (:service-risk derived))
      :cash-impact (str "$" (format-number (:cash-impact derived)))

      ;; Level 1 - Inventory Values
      :fg-inventory-value (format-currency (:fg-inventory-value derived))
      :rpm-inventory-value (format-currency (:rpm-inventory-value derived))
      :fg-inventory-position (format-number (:fg-inventory-position derived))
      :rpm-inventory-position (format-number (:rpm-inventory-position derived))

      ;; Level 2 - Flows
      :fg-inflows (format-number (:fg-inflows derived))
      :fg-outflows (format-number (:fg-outflows derived))
      :rpm-inflows (format-number (:rpm-inflows derived))
      :rpm-outflows (format-number (:rpm-outflows derived))
      :rpm-availability (format-number (:rpm-available derived))
      :demand-distribution (format-number (get-in derived [:demand-distribution :mean]))

      ;; Level 3 - Observations
      :consensus-forecast-qty (format-number (get-in obs [:consensus-forecast-qty :value]))
      :forecast-error-dist (format-percent (get-in obs [:forecast-error-dist :value]))
      :order-variability-cv (format-percent (get-in obs [:order-variability-cv :value]))
      :planned-production-qty (format-number (get-in obs [:planned-production-qty :value]))
      :mfg-adherence-pct (format-percent (get-in obs [:mfg-adherence-pct :value]))
      :rpm-consumption-ratio (str (get-in obs [:rpm-consumption-ratio :value]) "x")
      :fg-opening-stock (format-number (get-in obs [:fg-opening-stock :value]))
      :fg-blocked-stock-proj (format-number (get-in obs [:fg-blocked-stock-proj :value]))
      :rpm-opening-stock (format-number (get-in obs [:rpm-opening-stock :value]))
      :fg-unit-cost (str "$" (get-in obs [:fg-unit-cost :value]))
      :rpm-unit-cost (str "$" (get-in obs [:rpm-unit-cost :value]))
      :open-po-qty-rpm (format-number (get-in obs [:open-po-qty-rpm :value]))
      :supplier-otif-dist (format-percent (get-in obs [:supplier-otif-dist :value]))
      :lead-time-variability (format-percent (get-in obs [:lead-time-variability :value]))
      :material-shelf-life (str (get-in obs [:material-shelf-life :value]) " days")
      :target-cof-pct (format-percent (get-in obs [:target-cof-pct :value]))
      :cost-of-failure-unit (str "$" (get-in obs [:cost-of-failure-unit :value]))
      :holding-cost-rate (format-percent (get-in obs [:holding-cost-rate :value]))

      ;; Default
      "-")))

(defn get-node-source
  "Get the source system for an observation"
  [node-id]
  (let [obs (:observations @state/!app-state)]
    (get-in obs [node-id :source])))

;; -----------------------------------------------------------------------------
;; Node Type Legend - Matches React exactly
;; -----------------------------------------------------------------------------

(defn legend-item [{:keys [bg border label]}]
  [:div {:class "flex items-center gap-2"}
   [:div {:class (classes "w-4 h-4 rounded border-2" bg border)}]
   [:span {:class "text-xs text-slate-400 capitalize"} label]])

(defn legend []
  [:div {:class "flex gap-4"}
   (legend-item {:bg "bg-cyan-900" :border "border-cyan-500" :label "output"})
   (legend-item {:bg "bg-violet-900" :border "border-violet-500" :label "derived"})
   (legend-item {:bg "bg-amber-900" :border "border-amber-500" :label "observation"})])

;; -----------------------------------------------------------------------------
;; Node Component - Shows label + value + source (matches React)
;; Data-driven: uses action data, not functions
;; -----------------------------------------------------------------------------

(defn dependency-node
  "Single node in the dependency graph - data-driven"
  [{:keys [node-id selected? ancestor? descendant? dimmed?]}]
  (let [node (get graph/dependency-graph node-id)
        styles (graph/get-node-style node-id false dimmed?)
        value (get-node-value node-id)
        source (get-node-source node-id)
        ring-class (cond
                     selected? "ring-2 ring-white ring-offset-2 ring-offset-slate-900"
                     ancestor? "ring-2 ring-cyan-400 ring-offset-2 ring-offset-slate-900"
                     descendant? "ring-2 ring-violet-400 ring-offset-2 ring-offset-slate-900"
                     :else "")]
    [:button
     {:class (classes "px-4 py-3 rounded-lg border-2 text-left transition-all"
                      "hover:scale-105 cursor-pointer min-w-[120px]"
                      (:bg styles) (:border styles) (:text styles)
                      ring-class
                      (when dimmed? "opacity-40"))
      :on {:click [:lineage/toggle-node node-id]}}
     [:div {:class "text-sm font-medium"} (:label node)]
     [:div {:class "text-lg font-bold"} value]
     (when source
       [:div {:class "text-xs opacity-70"} source])]))

;; -----------------------------------------------------------------------------
;; Level Row Component - gap-3 (not gap-2)
;; -----------------------------------------------------------------------------

(defn level-label [level]
  (case level
    0 "Outputs"
    1 "Inventory Pools"
    2 "Flows"
    3 "Observations"
    (str "Level " level)))

(defn level-row
  "Row of nodes at a specific level - data-driven"
  [{:keys [level nodes selected-id ancestors descendants]}]
  [:div
   [:div {:class "text-xs text-slate-500 mb-3 font-medium"}
    (str "Level " level ": " (level-label level))]
   [:div {:class "flex flex-wrap gap-3"}
    (for [node-id nodes]
      ^{:key node-id}
      (dependency-node
       {:node-id node-id
        :selected? (= node-id selected-id)
        :ancestor? (contains? ancestors node-id)
        :descendant? (contains? descendants node-id)
        :dimmed? (and selected-id
                      (not= node-id selected-id)
                      (not (contains? ancestors node-id))
                      (not (contains? descendants node-id)))}))]])

;; -----------------------------------------------------------------------------
;; Detail Panel - 3-column grid layout (matches React)
;; Data-driven: uses action data, not functions
;; -----------------------------------------------------------------------------

(defn clickable-dep-button
  "Small button for a dependency - data-driven"
  [{:keys [node-id direction]}]
  (let [node (get graph/dependency-graph node-id)
        text-color (if (= direction :upstream) "text-cyan-400" "text-violet-400")]
    [:button
     {:class (classes "px-2 py-1 text-xs rounded bg-slate-800 hover:bg-slate-700" text-color)
      :on {:click [:lineage/select-node node-id]}}
     (:label node)]))

(defn detail-panel
  "Shows details of selected node - data-driven"
  [{:keys [node-id]}]
  (when node-id
    (let [node (get graph/dependency-graph node-id)
          deps (:dependencies node)
          descendants (graph/get-descendants node-id)
          shown-descendants (take 5 descendants)
          remaining-count (- (count descendants) 5)
          value (get-node-value node-id)
          source (get-node-source node-id)
          type-color (case (:type node)
                       :output "text-cyan-400"
                       :derived "text-violet-400"
                       :observation "text-amber-400"
                       "text-slate-400")]
      [:div {:class "bg-slate-900 rounded-xl border border-cyan-700 p-4"}
       ;; Header with close button
       [:div {:class "flex items-center justify-between mb-3"}
        [:div
         [:h3 {:class "text-lg font-semibold text-white"} (:label node)]
         [:div {:class "flex items-center gap-2 mt-1"}
          [:span {:class (classes "text-xs font-medium uppercase" type-color)}
           (name (:type node))]
          [:span {:class "text-xs text-slate-500"} (str "Level " (:level node))]]]
        [:button
         {:class "text-slate-400 hover:text-white transition-colors"
          :on {:click [:lineage/select-node nil]}}
         [:svg {:class "w-5 h-5" :fill "none" :viewBox "0 0 24 24" :stroke "currentColor"}
          [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                  :d "M6 18L18 6M6 6l12 12"}]]]]

       ;; 3-column grid: Current Value | Formula | Source
       [:div {:class "grid grid-cols-3 gap-4"}
        [:div {:class "bg-slate-800 rounded-lg p-3"}
         [:div {:class "text-xs text-slate-500 mb-1"} "Current Value"]
         [:div {:class "text-xl font-bold text-white"} value]]

        [:div {:class "bg-slate-800 rounded-lg p-3"}
         [:div {:class "text-xs text-slate-500 mb-1"} "Formula"]
         [:code {:class "text-sm text-slate-300 font-mono"}
          (or (:formula node) "Observed")]]

        [:div {:class "bg-slate-800 rounded-lg p-3"}
         [:div {:class "text-xs text-slate-500 mb-1"} "Source"]
         [:div {:class "text-sm text-slate-300"}
          (or source "Calculated")]]]

       ;; Dependencies section
       [:div {:class "mt-4 pt-4 border-t border-slate-800"}
        [:div {:class "grid grid-cols-2 gap-4"}
         [:div
          [:div {:class "text-xs text-slate-500 mb-2"}
           (str "Depends On (" (count deps) " nodes)")]
          (if (seq deps)
            [:div {:class "flex flex-wrap gap-1"}
             (for [dep-id deps]
               ^{:key dep-id}
               (clickable-dep-button {:node-id dep-id :direction :upstream}))]
            [:div {:class "text-xs text-slate-600"} "None (leaf node)"])]

         [:div
          [:div {:class "text-xs text-slate-500 mb-2"}
           (str "Affects (" (count descendants) " nodes)")]
          (if (seq descendants)
            [:div {:class "flex flex-wrap gap-1"}
             (for [desc-id shown-descendants]
               ^{:key desc-id}
               (clickable-dep-button {:node-id desc-id :direction :downstream}))
             (when (pos? remaining-count)
               [:span {:class "text-xs text-slate-500"}
                (str "+" remaining-count " more")])]
            [:div {:class "text-xs text-slate-600"} "None (output node)"])]]]])))

;; -----------------------------------------------------------------------------
;; Main Lineage View
;; -----------------------------------------------------------------------------

(defn lineage-view
  "Main lineage explorer view - data-driven"
  []
  (let [app-state @state/!app-state
        selected-id (:selected-node app-state)
        ancestors (when selected-id (set (graph/get-ancestors selected-id)))
        descendants (when selected-id (set (graph/get-descendants selected-id)))]
    [:div {:class "space-y-6"}
     ;; Header
     [:div {:class "flex items-center justify-between"}
      [:div
       [:h2 {:class "text-xl font-semibold text-white"} "Decision Lineage Explorer"]
       [:p {:class "text-slate-400 text-sm mt-1"}
        "Click any node to trace upstream dependencies and downstream impacts"]]
      (legend)]

     ;; Detail panel (if node selected)
     (detail-panel {:node-id selected-id})

     ;; Node Grid by Level
     [:div {:class "bg-slate-900 rounded-2xl border border-slate-800 p-8 space-y-8"}
      (level-row {:level 0
                  :nodes (graph/level-0-nodes)
                  :selected-id selected-id
                  :ancestors ancestors
                  :descendants descendants})

      (level-row {:level 1
                  :nodes (graph/level-1-nodes)
                  :selected-id selected-id
                  :ancestors ancestors
                  :descendants descendants})

      (level-row {:level 2
                  :nodes (graph/level-2-nodes)
                  :selected-id selected-id
                  :ancestors ancestors
                  :descendants descendants})

      (level-row {:level 3
                  :nodes (graph/level-3-nodes)
                  :selected-id selected-id
                  :ancestors ancestors
                  :descendants descendants})]]))
