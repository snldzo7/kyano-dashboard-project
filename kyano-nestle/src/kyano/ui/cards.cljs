(ns kyano.ui.cards
  "Generic card components - Card, StatCard, MetricCard, PoolCard, KPICard"
  (:require [kyano.ui.primitives :as p]
            [kyano.ui.charts :as ch]))

;; -----------------------------------------------------------------------------
;; Status border/background utilities
;; -----------------------------------------------------------------------------

(def status-borders
  {:good    "border-l-4 border-l-emerald-500"
   :warning "border-l-4 border-l-amber-500"
   :danger  "border-l-4 border-l-red-500"
   :neutral "border-l-4 border-l-slate-500"
   :info    "border-l-4 border-l-blue-500"})

(def status-backgrounds
  {:good    "bg-emerald-500/5"
   :warning "bg-amber-500/5"
   :danger  "bg-red-500/5"
   :neutral "bg-slate-500/5"
   :info    "bg-blue-500/5"})

;; -----------------------------------------------------------------------------
;; Card (base)
;; -----------------------------------------------------------------------------

(defn card
  "Base card component"
  [{:keys [header footer status padding class]} & children]
  (let [padding-class (case (or padding :md)
                        :none ""
                        :sm "p-3"
                        :md "p-4"
                        :lg "p-6"
                        "p-4")]
    [:div {:class (p/classes
                   "card rounded-xl"
                   (when status (status-borders status))
                   (when status (status-backgrounds status))
                   class)}
     (when header
       [:div {:class "px-4 py-3 border-b border-slate-700/50"} header])
     (into [:div {:class padding-class}] children)
     (when footer
       [:div {:class "px-4 py-3 border-t border-slate-700/50"} footer])]))

;; -----------------------------------------------------------------------------
;; StatCard
;; -----------------------------------------------------------------------------

(defn stat-card
  "Simple statistic display card"
  [{:keys [label value delta trend icon class]}]
  (let [trend-class (case trend
                      :up "text-emerald-400"
                      :down "text-red-400"
                      :neutral "text-slate-400"
                      "text-slate-400")
        trend-icon (case trend
                     :up "↑"
                     :down "↓"
                     nil)]
    (card {:class class :padding :md}
          [:div {:class "flex justify-between items-start"}
           [:div
            [:div {:class "text-sm text-slate-400 mb-1"} label]
            [:div {:class "text-2xl font-bold text-white"} value]
            (when delta
              [:div {:class (p/classes "text-sm mt-1" trend-class)}
               (when trend-icon [:span {:class "mr-1"} trend-icon])
               delta])]
           (when icon
             [:div {:class "text-2xl opacity-50"} icon])])))

;; -----------------------------------------------------------------------------
;; MetricCard (KPI card with sparkline support)
;; -----------------------------------------------------------------------------

(defn metric-card
  "KPI metric card with optional chart/sparkline"
  [{:keys [title value subtitle chart status badge-props trend delta actions class]}]
  (card {:status status :class class :padding :lg}
        ;; Header row
        [:div {:class "flex justify-between items-start mb-3"}
         [:div
          [:div {:class "text-sm text-slate-400"} title]
          (when badge-props
            [:div {:class "mt-1"} (p/badge badge-props)])]
         (when actions actions)]
        ;; Value row
        [:div {:class "flex justify-between items-end"}
         [:div
          [:div {:class "text-3xl font-bold text-white"} value]
          (when (or subtitle delta)
            [:div {:class "mt-1"}
             (when subtitle [:span {:class "text-sm text-slate-400"} subtitle])
             (when delta
               [:span {:class (p/classes
                               "text-sm ml-2"
                               (case trend
                                 :up "text-emerald-400"
                                 :down "text-red-400"
                                 "text-slate-400"))}
                (when (= trend :up) "↑")
                (when (= trend :down) "↓")
                delta])])]
         (when chart
           [:div {:class "w-24 h-12"} chart])]))

;; -----------------------------------------------------------------------------
;; PoolCard (Inventory pool card with inflows/outflows)
;; -----------------------------------------------------------------------------

(defn flow-item
  "Single flow item (inflow or outflow)"
  [{:keys [label value subvalue]}]
  [:div {:class "text-center"}
   [:div {:class "text-xs text-slate-500 uppercase tracking-wide"} label]
   [:div {:class "text-lg font-semibold text-white"} value]
   (when subvalue
     [:div {:class "text-xs text-slate-400"} subvalue])])

(defn pool-card
  "Inventory pool card showing inflows → position → outflows"
  [{:keys [title subtitle status position value inflows outflows class]}]
  (card {:status status :class class :padding :lg}
        ;; Header
        [:div {:class "flex justify-between items-center mb-4"}
         [:div
          [:h3 {:class "text-lg font-semibold text-white"} title]
          (when subtitle [:p {:class "text-sm text-slate-400"} subtitle])]
         (p/status-dot {:status status :pulse (= status :danger) :size :lg})]

        ;; Main metrics row
        [:div {:class "flex justify-between items-center mb-6"}
         [:div {:class "text-center"}
          [:div {:class "text-xs text-slate-500 uppercase"} (:label position "Position")]
          [:div {:class "text-2xl font-bold text-white"} (:value position)]]
         [:div {:class "text-center"}
          [:div {:class "text-xs text-slate-500 uppercase"} (:label value "Value")]
          [:div {:class "text-2xl font-bold text-white"} (:value value)]]]

        ;; Flow diagram
        [:div {:class "bg-slate-900/50 rounded-lg p-4"}
         [:div {:class "flex justify-between items-center"}
          ;; Inflows
          [:div {:class "flex-1"}
           [:div {:class "text-xs text-emerald-400 uppercase mb-2"} "Inflows"]
           (for [[idx inflow] (map-indexed vector inflows)]
             ^{:key idx}
             (flow-item inflow))]

          ;; Arrow
          [:div {:class "px-4 text-slate-600"} "→"]

          ;; Position bar
          [:div {:class "flex-1 text-center"}
           [:div {:class "h-8 bg-slate-700 rounded-full overflow-hidden"}
            [:div {:class (p/classes
                           "h-full rounded-full"
                           (case status
                             :good "bg-emerald-500"
                             :warning "bg-amber-500"
                             :danger "bg-red-500"
                             "bg-slate-500"))
                   :style {:width "65%"}}]]]

          ;; Arrow
          [:div {:class "px-4 text-slate-600"} "→"]

          ;; Outflows
          [:div {:class "flex-1"}
           [:div {:class "text-xs text-red-400 uppercase mb-2"} "Outflows"]
           (for [[idx outflow] (map-indexed vector outflows)]
             ^{:key idx}
             (flow-item outflow))]]]))

;; -----------------------------------------------------------------------------
;; WhyCard (Root cause explainer)
;; -----------------------------------------------------------------------------

(defn why-card
  "Root cause explanation card"
  [{:keys [title explanation factors actions class]}]
  (card {:class class :padding :lg}
        [:h3 {:class "text-lg font-semibold text-white mb-2"} title]
        (when explanation
          [:p {:class "text-slate-400 mb-4"} explanation])

        ;; Contributing factors
        (when (seq factors)
          [:div {:class "mb-4"}
           [:div {:class "text-xs text-slate-500 uppercase mb-2"} "Contributing Factors"]
           (for [[idx {:keys [label impact direction]}] (map-indexed vector factors)]
             ^{:key idx}
             [:div {:class "flex justify-between py-1 border-b border-slate-700/50 last:border-0"}
              [:span {:class "text-sm text-slate-300"} label]
              [:span {:class (p/classes
                              "text-sm font-medium"
                              (case direction
                                :positive "text-emerald-400"
                                :negative "text-red-400"
                                "text-slate-400"))}
               impact]])])

        ;; Recommended actions
        (when (seq actions)
          [:div
           [:div {:class "text-xs text-slate-500 uppercase mb-2"} "Recommended Actions"]
           [:ul {:class "space-y-1"}
            (for [[idx action] (map-indexed vector actions)]
              ^{:key idx}
              [:li {:class "text-sm text-slate-300 flex items-start gap-2"}
               [:span {:class "text-emerald-400"} "→"]
               action])]])))

;; -----------------------------------------------------------------------------
;; KPICard (Interactive KPI card matching React styling)
;; -----------------------------------------------------------------------------

(def kpi-status-colors
  {:good    {:bg "bg-emerald-950" :border "border-emerald-600" :text "text-emerald-400" :glow "shadow-emerald-500/20"}
   :warning {:bg "bg-amber-950" :border "border-amber-600" :text "text-amber-400" :glow "shadow-amber-500/20"}
   :danger  {:bg "bg-red-950" :border "border-red-600" :text "text-red-400" :glow "shadow-red-500/20"}})

(defn kpi-card
  "Interactive KPI card with status, icon, target, variance, sparkline - data-driven

   Props:
   - :id        - KPI identifier
   - :label     - Display label
   - :value     - Current value (formatted string)
   - :target    - Target value (formatted string)
   - :variance  - Variance from target (number, e.g. 0.15 for 15%)
   - :status    - :good :warning :danger
   - :icon-path - SVG path string for icon
   - :trend     - Vector of trend data for sparkline
   - :selected? - Boolean, if this card is selected
   - :on-click  - Action vector to dispatch on click"
  [{:keys [label value target variance status icon-path trend selected? on-click risk-direction]}]
  (let [colors (get kpi-status-colors status (kpi-status-colors :good))
        variance-positive? (pos? variance)
        variance-bad? (if (= risk-direction :high) variance-positive? (not variance-positive?))]
    [:button {:class (p/classes
                      "relative group text-left p-6 rounded-2xl border-2 transition-all duration-300"
                      (:bg colors)
                      (:border colors)
                      (if selected?
                        (str "ring-2 ring-offset-2 ring-offset-slate-950 ring-cyan-500 shadow-lg " (:glow colors))
                        "hover:scale-[1.02]"))
              :on {:click on-click}}
     ;; Status dot top-right
     [:div {:class (p/classes
                    "absolute top-4 right-4 w-3 h-3 rounded-full"
                    (case status
                      :good "bg-emerald-500"
                      :warning "bg-amber-500 animate-pulse"
                      :danger "bg-red-500 animate-pulse"
                      "bg-slate-500"))}]

     ;; Icon
     [:div {:class (p/classes "w-12 h-12 rounded-xl bg-slate-800 flex items-center justify-center mb-4" (:text colors))}
      [:svg {:class "w-6 h-6" :fill "none" :viewBox "0 0 24 24" :stroke "currentColor"}
       [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "1.5" :d icon-path}]]]

     ;; Label
     [:div {:class "text-sm text-slate-400 mb-1"} label]

     ;; Value
     [:div {:class (p/classes "text-3xl font-bold tracking-tight mb-2" (:text colors))} value]

     ;; Target + Variance
     [:div {:class "flex items-center gap-3 text-sm"}
      [:span {:class "text-slate-500"} (str "Target: " target)]
      [:span {:class (p/classes "font-medium" (if variance-bad? "text-red-400" "text-emerald-400"))}
       (if variance-positive? "▲" "▼")
       " "
       (.toFixed (* (js/Math.abs variance) 100) 1)
       "%"]]

     ;; Sparkline (uses viewBox 100x100 with preserveAspectRatio=none, stretches to fill container)
     (when (seq trend)
       [:div {:class "mt-4 h-12"}
        (ch/sparkline {:data trend
                       :color (case status
                                :good "#34d399"
                                :warning "#fbbf24"
                                :danger "#f87171"
                                "#94a3b8")})])

     ;; Hover hint
     [:div {:class "absolute bottom-4 right-4 text-xs text-slate-600 opacity-0 group-hover:opacity-100 transition-opacity"}
      "Click to analyze →"]]))

;; -----------------------------------------------------------------------------
;; InventoryPoolCard (Matching React styling)
;; -----------------------------------------------------------------------------

(defn inventory-pool-card
  "Inventory pool card matching React styling exactly

   Props:
   - :title    - Pool name (e.g. 'Finished Goods')
   - :position - Current inventory position (number)
   - :value    - Inventory value in dollars (number)
   - :inflows  - Inflow quantity (number)
   - :outflows - Outflow quantity (number)
   - :unit     - Unit label (e.g. 'cases', 'units')"
  [{:keys [title position value inflows outflows unit]}]
  [:div {:class "bg-slate-900 rounded-xl border border-slate-800 p-5"}
   ;; Header
   [:div {:class "flex items-center justify-between mb-4"}
    [:h3 {:class "font-medium text-white"} title]
    [:span {:class "text-lg font-bold text-cyan-400"}
     (str "$" (.toFixed (/ value 1000000) 2) "M")]]

   ;; Flow diagram
   [:div {:class "flex items-center gap-4 mb-4"}
    ;; Inflows
    [:div {:class "flex-1 text-center p-3 bg-emerald-950 rounded-lg border border-emerald-800"}
     [:div {:class "text-xs text-emerald-400 mb-1"} "Inflows"]
     [:div {:class "text-lg font-semibold text-emerald-300"} (str "+" (.toLocaleString inflows))]
     [:div {:class "text-xs text-slate-500"} unit]]

    ;; Arrow
    [:div {:class "text-2xl text-slate-600"} "→"]

    ;; Position
    [:div {:class "flex-1 text-center p-3 bg-slate-800 rounded-lg border border-slate-700"}
     [:div {:class "text-xs text-slate-400 mb-1"} "Position"]
     [:div {:class "text-lg font-semibold text-white"} (.toLocaleString position)]
     [:div {:class "text-xs text-slate-500"} unit]]

    ;; Arrow
    [:div {:class "text-2xl text-slate-600"} "→"]

    ;; Outflows
    [:div {:class "flex-1 text-center p-3 bg-red-950 rounded-lg border border-red-800"}
     [:div {:class "text-xs text-red-400 mb-1"} "Outflows"]
     [:div {:class "text-lg font-semibold text-red-300"} (str "-" (.toLocaleString (js/Math.round outflows)))]
     [:div {:class "text-xs text-slate-500"} unit]]]

   ;; Progress bar
   [:div {:class "h-2 bg-slate-800 rounded-full overflow-hidden"}
    [:div {:class "h-full bg-gradient-to-r from-emerald-500 to-cyan-500 rounded-full transition-all duration-500"
           :style {:width (str (min 100 (* (/ position (+ inflows position)) 100)) "%")}}]]])

;; -----------------------------------------------------------------------------
;; Simple StatCard (Matching React styling)
;; -----------------------------------------------------------------------------

(defn simple-stat-card
  "Simple stat card matching React styling

   Props:
   - :label  - Stat label
   - :value  - Stat value (string)
   - :unit   - Optional unit
   - :status - Optional :good, :warning, or :neutral (default)"
  [{:keys [label value unit status]}]
  (let [status-colors {:good "text-emerald-400"
                       :warning "text-amber-400"
                       :neutral "text-slate-300"}]
    [:div {:class "bg-slate-900 rounded-lg border border-slate-800 p-4"}
     [:div {:class "text-xs text-slate-500 mb-1"} label]
     [:div {:class (p/classes "text-xl font-semibold"
                              (get status-colors status "text-slate-300"))}
      value]
     (when unit
       [:div {:class "text-xs text-slate-600"} unit])]))

;; -----------------------------------------------------------------------------
;; WhyExplainer (Root cause analysis panel)
;; -----------------------------------------------------------------------------

(defn why-explainer
  "Root cause explainer panel that appears when a KPI card is clicked - data-driven

   Props:
   - :title      - Title of the analysis
   - :summary    - Summary text
   - :root-causes - Vector of {:contribution :title :detail :source :supplier?}
   - :actions    - Vector of {:text :owner}
   - :on-simulate - Action vector to dispatch on simulate button click"
  [{:keys [title summary root-causes actions on-simulate]}]
  [:div {:class "bg-slate-900 rounded-2xl border border-slate-800 overflow-hidden"}
   ;; Header
   [:div {:class "px-6 py-4 border-b border-slate-800 bg-red-950"}
    [:div {:class "flex items-center gap-3"}
     [:div {:class "w-8 h-8 rounded-lg bg-red-900 flex items-center justify-center"}
      [:svg {:class "w-5 h-5 text-red-400" :fill "none" :viewBox "0 0 24 24" :stroke "currentColor"}
       [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
               :d "M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"}]]]
     [:div
      [:h3 {:class "font-semibold text-white"} title]
      [:p {:class "text-sm text-slate-400"} summary]]]]

   ;; Body
   [:div {:class "p-6 grid grid-cols-2 gap-6"}
    ;; Left: Root Cause Analysis
    [:div
     [:h4 {:class "text-sm font-medium text-slate-300 mb-4 flex items-center gap-2"}
      [:svg {:class "w-4 h-4 text-slate-500" :fill "none" :viewBox "0 0 24 24" :stroke "currentColor"}
       [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
               :d "M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"}]]
      "Root Cause Analysis"]
     [:div {:class "space-y-4"}
      (for [[idx cause] (map-indexed vector root-causes)]
        ^{:key idx}
        [:div {:class "relative pl-4"}
         [:div {:class "absolute left-0 top-1 w-1 bg-gradient-to-b from-red-500 to-transparent rounded-full"
                :style {:height "80%"}}]
         [:div {:class "flex items-center gap-2 mb-1"}
          [:span {:class "text-xs font-bold text-red-400 bg-red-900 px-2 py-0.5 rounded-full"}
           (str (:contribution cause) "%")]
          [:span {:class "text-sm font-medium text-white"} (:title cause)]]
         [:p {:class "text-sm text-slate-400 mb-1"} (:detail cause)]
         [:div {:class "flex items-center gap-2 text-xs text-slate-500"}
          [:span (str "Source: " (:source cause))]
          [:span "•"]
          [:span (str "Recorded: " (:recorded cause))]]
         (when (:supplier cause)
           [:div {:class "mt-2 text-xs text-amber-400 bg-amber-900 px-2 py-1 rounded inline-block"}
            (str "Affected Supplier: " (:supplier cause))])])]]

    ;; Right: Recommended Actions
    [:div
     [:h4 {:class "text-sm font-medium text-slate-300 mb-4 flex items-center gap-2"}
      [:svg {:class "w-4 h-4 text-slate-500" :fill "none" :viewBox "0 0 24 24" :stroke "currentColor"}
       [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
               :d "M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4"}]]
      "Recommended Actions"]
     [:div {:class "space-y-3"}
      (for [[idx action] (map-indexed vector actions)]
        ^{:key idx}
        [:div {:class "flex items-center gap-3 p-3 bg-slate-800 rounded-lg border border-slate-700"}
         [:div {:class "w-6 h-6 rounded-full bg-cyan-900 flex items-center justify-center text-cyan-400 text-xs font-bold"}
          (inc idx)]
         [:div {:class "flex-1"}
          [:p {:class "text-sm text-white"} (:text action)]
          [:p {:class "text-xs text-slate-500"} (str "Owner: " (:owner action))]]])]

     ;; Simulate Fix button
     [:div {:class "mt-6 flex gap-3"}
      [:button {:class "flex-1 px-4 py-2 bg-cyan-600 text-white rounded-lg text-sm font-medium hover:bg-cyan-700 transition-colors flex items-center justify-center gap-2"
                :on {:click on-simulate}}
       [:svg {:class "w-4 h-4" :fill "none" :viewBox "0 0 24 24" :stroke "currentColor"}
        [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                :d "M7 21a4 4 0 01-4-4V5a2 2 0 012-2h4a2 2 0 012 2v12a4 4 0 01-4 4zm0 0h12a2 2 0 002-2v-4a2 2 0 00-2-2h-2.343M11 7.343l1.657-1.657a2 2 0 012.828 0l2.829 2.829a2 2 0 010 2.828l-8.486 8.485M7 17h.01"}]]
       "Simulate Fix"]]]]])
