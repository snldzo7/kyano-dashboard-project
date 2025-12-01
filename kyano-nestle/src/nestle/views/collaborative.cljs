(ns nestle.views.collaborative
  "CPG x Retail Collaborative Decision Room
   Data-driven: all handlers are action vectors, no functions in components."
  (:require [clojure.string :as str]
            [kyano.ui.primitives :refer [classes]]
            [nestle.data.collaborative :as collab]
            [nestle.state :as state]))

;; -----------------------------------------------------------------------------
;; Stakeholder Card Component - Data-driven
;; -----------------------------------------------------------------------------

(defn stakeholder-card
  "Single stakeholder card with toggle - data-driven"
  [{:keys [stakeholder-key stakeholder org active? mandatory?]}]
  (let [colors {:cpg {:bg "bg-cyan-950" :border "border-cyan-600" :text "text-cyan-300" :dot "bg-cyan-500"}
                :retail {:bg "bg-emerald-950" :border "border-emerald-600" :text "text-emerald-300" :dot "bg-emerald-500"}}
        c (get colors org)
        action (if (= org :cpg)
                 [:collab/toggle-cpg-stakeholder stakeholder-key]
                 [:collab/toggle-retail-stakeholder stakeholder-key])]
    [:button
     {:class (classes "w-full p-3 rounded-lg border-2 text-left transition-all"
                      (if active? (:bg c) "bg-slate-800")
                      (if active? (:border c) "border-slate-700")
                      (if mandatory? "opacity-100 cursor-not-allowed" "hover:scale-[1.02] cursor-pointer"))
      :disabled mandatory?
      :on (when-not mandatory? {:click action})}
     [:div {:class "flex items-center justify-between mb-2"}
      [:div {:class "flex items-center gap-2"}
       [:span {:class "text-2xl"} (:avatar stakeholder)]
       [:div
        [:div {:class (classes "text-sm font-medium" (:text c))} (:name stakeholder)]
        (when mandatory?
          [:div {:class "text-xs text-amber-400"} "\u2713 Mandatory"])]]
      (when active?
        [:div {:class (classes "w-3 h-3 rounded-full" (:dot c))}])]
     [:div {:class "text-xs text-slate-400"}
      (clojure.string/join " \u2022 " (take 2 (:expertise stakeholder)))]]))

;; -----------------------------------------------------------------------------
;; Global KPI Card Component
;; -----------------------------------------------------------------------------

(defn format-kpi-value [kpi-key value]
  (cond
    (or (clojure.string/includes? (name kpi-key) "accuracy")
        (clojure.string/includes? (name kpi-key) "service")
        (clojure.string/includes? (name kpi-key) "availability"))
    (str (.toFixed (* value 100) 1) "%")

    (clojure.string/includes? (name kpi-key) "cost")
    (str (.toFixed value 1) "%")

    (= kpi-key :inventory-risk-corridor)
    (if (map? value)
      (str "P50: " (.toFixed (/ (:p50 value) 1000) 1) "K")
      (str value))

    :else (.toFixed value 2)))

(defn get-kpi-status-color [kpi current-value]
  (if-not (:target kpi)
    "text-slate-400"
    (let [diff (Math/abs (- current-value (:target kpi)))
          tolerance (* (:target kpi) 0.1)]
      (cond
        (< diff tolerance) "text-emerald-400"
        (< diff (* tolerance 2)) "text-amber-400"
        :else "text-red-400"))))

(defn global-kpi-card
  "Single global KPI card - matches React exactly"
  [{:keys [kpi-key kpi current-value impact-level]}]
  [:div {:class "bg-slate-900 rounded-xl border border-slate-800 p-4"}
   [:div {:class "flex items-center justify-between mb-2"}
    [:div {:class "text-xs text-slate-500"} (:name kpi)]
    (when impact-level
      [:div {:class (classes "text-xs px-2 py-0.5 rounded"
                             (case impact-level
                               :high "bg-red-900 text-red-400"
                               :medium "bg-amber-900 text-amber-400"
                               "bg-emerald-900 text-emerald-400"))}
       (name impact-level)])]
   [:div {:class (classes "text-2xl font-bold mb-1" (get-kpi-status-color kpi current-value))}
    (format-kpi-value kpi-key current-value)]
   (when (:target kpi)
     [:div {:class "text-xs text-slate-500"}
      (str "Target: " (format-kpi-value kpi-key (:target kpi)))])])

;; -----------------------------------------------------------------------------
;; Complexity Scenario Cards
;; -----------------------------------------------------------------------------

(defn complexity-scenario-card
  "Single complexity scenario card"
  [{:keys [scenario]}]
  (let [impacts (:global-kpi-impact scenario)
        severity (if (some #(and (number? %) (> (Math/abs %) 2)) (vals impacts)) :high :medium)
        colors {:high "bg-red-950 border-red-600 text-red-400"
                :medium "bg-amber-950 border-amber-600 text-amber-400"}]
    [:div {:class (classes "p-4 rounded-lg border-2" (get colors severity))}
     [:div {:class "flex items-center gap-2 mb-2"}
      [:span {:class "text-xl"} "\u26a0\ufe0f"]
      [:div {:class "font-semibold text-sm"} (:name scenario)]]
     [:div {:class "text-xs opacity-90 mb-2"} (:description scenario)]
     [:div {:class "space-y-1"}
      (for [[kpi impact] (take 2 impacts)]
        ^{:key kpi}
        [:div {:class "text-xs flex items-center gap-1"}
         [:span {:class "opacity-75"} (first (clojure.string/split (:name (get collab/global-kpis kpi)) #" ")) ":"]
         [:span {:class "font-mono font-bold"}
          (if (number? impact)
            (str (when (pos? impact) "+") (.toFixed impact 1))
            (str impact))]])]]))

;; -----------------------------------------------------------------------------
;; Causation Chain Visualization
;; -----------------------------------------------------------------------------

(defn get-severity-color
  "Get color classes based on impact severity - matches React"
  [impact]
  (cond
    (nil? impact) {:bg "bg-slate-700" :text "text-white" :border "border-slate-600"}
    (> (Math/abs impact) 1.5) {:bg "bg-red-950" :text "text-red-400" :border "border-red-700"}
    (> (Math/abs impact) 1.0) {:bg "bg-amber-950" :text "text-amber-400" :border "border-amber-700"}
    :else {:bg "bg-slate-800" :text "text-slate-300" :border "border-slate-600"}))

(defn causation-chain-node
  "Single node in causation chain with severity coloring"
  [{:keys [node idx total-nodes]}]
  (let [severity (get-severity-color (:impact node))
        is-last (= idx (dec total-nodes))]
    [:div {:class "relative"}
     ;; Node
     [:div {:class "flex items-center gap-3"}
      [:div {:class (str/join " " ["w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold border-2"
                                    (:bg severity) (:text severity) (:border severity)])}
       (inc idx)]
      [:div {:class (str/join " " ["flex-1 p-3 rounded-lg border"
                                   (:bg severity) (:border severity)])}
       [:div {:class (str/join " " ["text-sm font-medium" (:text severity)])} (:label node)]
       (when (:impact node)
         [:div {:class "text-xs text-slate-400 mt-1"}
          (str "Impact: " (if (pos? (:impact node)) "+" "") (.toFixed (:impact node) 2) "x")])]]
     ;; Arrow to next node
     (when-not is-last
       [:div {:class "flex justify-center py-1"}
        [:svg {:class "w-4 h-6 text-slate-500" :fill "none" :viewBox "0 0 24 24" :stroke "currentColor"}
         [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                 :d "M19 14l-7 7m0 0l-7-7m7 7V3"}]]])]))

(defn causation-chain-view
  "Visualization of causation chain with arrows and severity colors - matches React"
  [{:keys [scenario]}]
  (let [chain (:causation-chain scenario)
        total-nodes (count chain)]
    [:div {:class "bg-slate-800 rounded-lg p-4"}
     [:h4 {:class "text-sm font-semibold text-white mb-3"}
      (str "Causation Chain: " (:name scenario))]
     [:div {:class "space-y-1"}
      (for [[idx node] (map-indexed vector chain)]
        ^{:key idx}
        (causation-chain-node {:node node :idx idx :total-nodes total-nodes}))]]))

;; -----------------------------------------------------------------------------
;; Main Collaborative Decision Room View - Data-driven
;; -----------------------------------------------------------------------------

(defn collaborative-view
  "Main collaborative decision room view - fully data-driven"
  []
  (let [app-state @state/!app-state
        observations (:observations app-state)

        ;; Active stakeholders state
        active-cpg (or (:active-cpg-stakeholders app-state) #{:demand-planner})
        active-retail (or (:active-retail-stakeholders app-state) #{:category-manager :replenishment-planner})

        ;; Calculate global KPIs from observations
        calculated-kpis (collab/calculate-global-kpis observations)

        ;; Calculate impact level for each KPI based on active stakeholders
        calculate-impact-level (fn [kpi]
                                 (let [impacted-by (:impacted-by kpi)
                                       impacting-count (count (filter #(or (contains? active-cpg %)
                                                                           (contains? active-retail %))
                                                                      impacted-by))
                                       ratio (/ impacting-count (count impacted-by))]
                                   (cond
                                     (> ratio 0.7) :high
                                     (> ratio 0.4) :medium
                                     :else :low)))

        selected-scenario (:selected-complexity-scenario app-state)]

    [:div {:class "space-y-6"}
     ;; Header
     [:div
      [:h2 {:class "text-xl font-semibold text-white flex items-center gap-2"}
       "\ud83e\udd1d CPG \u00d7 Retail Collaborative Decision Room"]
      [:p {:class "text-sm text-slate-400 mt-1"}
       "Cross-boundary decision intelligence with complexity-aware scenario planning"]]

     ;; Global KPIs Dashboard
     [:div {:class "bg-slate-900 rounded-xl border border-slate-800 p-6"}
      [:h3 {:class "text-lg font-semibold text-white mb-4"} "Global KPIs (System-Wide Performance)"]
      [:div {:class "grid grid-cols-3 gap-4"}
       (for [[kpi-key kpi] collab/global-kpis]
         ^{:key kpi-key}
         (global-kpi-card
          {:kpi-key kpi-key
           :kpi kpi
           :current-value (or (get calculated-kpis kpi-key) (:current kpi))
           :impact-level (calculate-impact-level kpi)}))]]

     ;; Stakeholder Panels - Two columns - Data-driven
     [:div {:class "grid grid-cols-2 gap-6"}
      ;; CPG Manufacturing
      [:div {:class "bg-slate-900 rounded-xl border border-slate-800 p-6"}
       [:h3 {:class "text-lg font-semibold text-cyan-400 mb-4 flex items-center gap-2"}
        "\ud83c\udfed CPG Manufacturing"]
       [:div {:class "space-y-3"}
        (for [[stakeholder-key stakeholder] collab/cpg-roles]
          ^{:key stakeholder-key}
          (stakeholder-card
           {:stakeholder-key stakeholder-key
            :stakeholder stakeholder
            :org :cpg
            :active? (contains? active-cpg stakeholder-key)
            :mandatory? (:mandatory? stakeholder)}))]]

      ;; Retail Partner
      [:div {:class "bg-slate-900 rounded-xl border border-slate-800 p-6"}
       [:h3 {:class "text-lg font-semibold text-emerald-400 mb-4 flex items-center gap-2"}
        "\ud83d\udecd\ufe0f Retail Partner"]
       [:div {:class "space-y-3"}
        (for [[stakeholder-key stakeholder] collab/retail-roles]
          ^{:key stakeholder-key}
          (stakeholder-card
           {:stakeholder-key stakeholder-key
            :stakeholder stakeholder
            :org :retail
            :active? (contains? active-retail stakeholder-key)
            :mandatory? (:mandatory? stakeholder)}))]]]

     ;; Complexity Scenarios - Data-driven
     [:div {:class "bg-slate-900 rounded-xl border border-slate-800 p-6"}
      [:h3 {:class "text-lg font-semibold text-white mb-4"} "Complexity Scenarios"]
      [:div {:class "grid grid-cols-3 gap-4 mb-6"}
       (for [[scenario-key scenario] collab/complexity-scenarios]
         ^{:key scenario-key}
         (complexity-scenario-card {:scenario-key scenario-key :scenario scenario}))]

      ;; Causation Chain Selector - Data-driven
      [:div {:class "flex gap-2 mb-4"}
       (for [[scenario-key _scenario] collab/complexity-scenarios]
         ^{:key scenario-key}
         [:button
          {:class (classes "px-4 py-2 rounded-lg text-sm font-medium transition-all"
                           (if (= selected-scenario scenario-key)
                             "bg-cyan-600 text-white"
                             "bg-slate-800 text-slate-400 hover:bg-slate-700"))
           :on {:click [:collab/toggle-scenario scenario-key]}}
          "View Chain"])]

      ;; Causation Chain Visualization
      (when selected-scenario
        (causation-chain-view {:scenario (get collab/complexity-scenarios selected-scenario)}))]]))
