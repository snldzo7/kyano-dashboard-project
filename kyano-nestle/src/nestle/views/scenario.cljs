(ns nestle.views.scenario
  "Scenario Simulator & Decision Room
   Data-driven: all handlers are action vectors, no functions in components."
  (:require [kyano.ui.primitives :refer [classes]]
            [kyano.ui.forms :as forms]
            [nestle.data.scenario-config :as config]
            [nestle.state :as state]
            [nestle.views.dashboard :as dashboard]))

;; -----------------------------------------------------------------------------
;; Helper Functions (pure formatting, no state)
;; -----------------------------------------------------------------------------

(defn format-currency [v]
  (str "$" (.toFixed (/ v 1000000) 2) "M"))

(defn format-percent [v]
  (str (Math/round (* v 100)) "%"))

(defn format-number [v]
  (.toLocaleString (Math/round v)))

(defn format-value [obs-key v]
  (let [{:keys [format suffix]} (config/get-slider-config obs-key)]
    (case format
      :percent (format-percent v)
      :currency (str "$" (Math/round v))
      :number (str (format-number v) (or suffix ""))
      (str v))))

;; -----------------------------------------------------------------------------
;; KPI Impact Cards
;; -----------------------------------------------------------------------------

(defn kpi-impact-card
  "Shows KPI comparison between baseline and scenario
   Matches React ImpactCard: strikethrough, arrows, target indicator"
  [{:keys [label baseline-value scenario-value format-fn target]}]
  (let [delta (- scenario-value baseline-value)
        delta-pct (when (and baseline-value (not (zero? baseline-value)))
                    (* 100 (/ delta baseline-value)))
        is-good (neg? delta)
        meets-target (when target (<= scenario-value (* target 1.1)))
        value-color (if is-good "text-emerald-400" "text-red-400")]
    [:div {:class (classes "p-4 rounded-lg border"
                           (if meets-target
                             "bg-emerald-950 border-emerald-800"
                             "bg-slate-800 border-slate-700"))}
     [:div {:class "text-xs text-slate-400 mb-2"} label]
     [:div {:class "flex items-end justify-between"}
      [:div
       [:div {:class "text-sm text-slate-500 line-through"} (format-fn baseline-value)]
       [:div {:class (classes "text-2xl font-bold" value-color)}
        (format-fn scenario-value)]]
      (when delta-pct
        [:div {:class (classes "text-right" value-color)}
         [:div {:class "text-lg font-bold"} (if is-good "↓" "↑")]
         [:div {:class "text-sm"} (str (.toFixed (Math/abs delta-pct) 1) "%")]])]
     (when meets-target
       [:div {:class "mt-2 text-xs text-emerald-400 flex items-center gap-1"}
        [:svg {:class "w-3 h-3" :fill "none" :viewBox "0 0 24 24" :stroke "currentColor"}
         [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                 :d "M5 13l4 4L19 7"}]]
        "Within target"])]))

(defn kpi-impact-row
  "Row of KPI impact cards with targets"
  [{:keys [baseline-derived scenario-derived targets]}]
  [:div {:class "grid grid-cols-3 gap-6"}
   (kpi-impact-card
    {:label "Total Inventory $"
     :baseline-value (:total-inventory-value baseline-derived)
     :scenario-value (:total-inventory-value scenario-derived)
     :format-fn format-currency
     :target (get-in targets [:total-inventory-value :value])})
   (kpi-impact-card
    {:label "Service Risk"
     :baseline-value (:service-risk baseline-derived)
     :scenario-value (:service-risk scenario-derived)
     :format-fn format-percent
     :target (get-in targets [:service-risk :value])})
   (kpi-impact-card
    {:label "Cash Impact"
     :baseline-value (:cash-impact baseline-derived)
     :scenario-value (:cash-impact scenario-derived)
     :format-fn #(str "$" (format-number %))
     :target (get-in targets [:cash-impact :value])})])

;; -----------------------------------------------------------------------------
;; Category Tabs - Data-driven
;; -----------------------------------------------------------------------------

(defn category-tabs
  "Tab buttons for observation categories - data-driven
   Matches React: container with bg-slate-900 rounded-lg p-1"
  [{:keys [active-category]}]
  [:div {:class "flex gap-2 bg-slate-900 rounded-lg p-1"}
   (for [cat-key config/category-order]
     (let [{:keys [label]} (get config/categories cat-key)]
       ^{:key cat-key}
       (forms/tab-button
        {:label label
         :active? (= cat-key active-category)
         :on-click [:scenario/set-category cat-key]})))])

;; -----------------------------------------------------------------------------
;; Observation Sliders Panel - Data-driven
;; -----------------------------------------------------------------------------

(defn observation-slider
  "Single observation slider with baseline comparison - data-driven"
  [{:keys [obs-key baseline-observations scenario-observations]}]
  (let [slider-cfg (config/get-slider-config obs-key)
        baseline-value (get-in baseline-observations [obs-key :value])
        current-value (get-in scenario-observations [obs-key :value])
        label (get-in baseline-observations [obs-key :label])]
    (forms/range-slider
     {:value current-value
      :min (:min slider-cfg)
      :max (:max slider-cfg)
      :step (:step slider-cfg)
      :label label
      :baseline baseline-value
      :format-fn #(format-value obs-key %)
      :on-change [:scenario/update-observation obs-key :event/target.value]})))

(defn sliders-panel
  "Panel with all sliders for active category - data-driven"
  [{:keys [active-category baseline-observations scenario-observations]}]
  (let [obs-keys (config/get-observations-for-category active-category)]
    [:div {:class "space-y-6"}
     (for [obs-key obs-keys]
       ^{:key obs-key}
       (observation-slider
        {:obs-key obs-key
         :baseline-observations baseline-observations
         :scenario-observations scenario-observations}))]))

;; -----------------------------------------------------------------------------
;; Required Actions Panel
;; -----------------------------------------------------------------------------

(defn action-card
  "Single action card - matches React styling with dot indicator"
  [{:keys [action owner urgency]}]
  (let [dot-color (case urgency
                    :high "bg-red-500"
                    :medium "bg-amber-500"
                    "bg-slate-500")]
    [:div {:class "flex items-start gap-3 p-3 bg-slate-800 rounded-lg"}
     [:div {:class (classes "w-2 h-2 rounded-full mt-1.5" dot-color)}]
     [:div
      [:div {:class "text-sm text-white"} action]
      [:div {:class "text-xs text-slate-500"} (str "Owner: " owner)]]]))

(defn required-actions-panel
  "Shows actions required based on observation changes
   Matches React: wrapped in container with header"
  [{:keys [baseline-observations scenario-observations]}]
  (let [all-actions
        (for [obs-key (keys config/action-implications)
              :let [baseline (get-in baseline-observations [obs-key :value])
                    current (get-in scenario-observations [obs-key :value])]
              :when (not= current baseline)
              action (config/get-applicable-actions obs-key current baseline)]
          action)
        actions (vec (distinct all-actions))]
    (when (seq actions)
      [:div {:class "bg-slate-900 rounded-xl border border-slate-800 p-6"}
       [:h3 {:class "text-sm font-medium text-slate-300 mb-4"} "Required Actions to Achieve This Scenario"]
       [:div {:class "grid grid-cols-2 gap-3"}
        (for [[idx action] (map-indexed vector actions)]
          ^{:key idx}
          (action-card action))]])))

;; -----------------------------------------------------------------------------
;; Participant Selector - Data-driven
;; -----------------------------------------------------------------------------

(defn participant-selector
  "Grid of participant avatars to select - data-driven"
  [{:keys [selected-ids]}]
  [:div {:class "grid grid-cols-4 gap-2"}
   (for [{:keys [id name initials color]} config/participant-roles]
     ^{:key id}
     (forms/participant-avatar
      {:initials initials
       :name name
       :color color
       :selected? (contains? (set selected-ids) id)
       :on-click [:scenario/toggle-participant id]}))])

;; -----------------------------------------------------------------------------
;; Decision Recording Panel - Data-driven
;; -----------------------------------------------------------------------------

(defn decision-recording-panel
  "Panel for recording decisions - data-driven
   Matches React: bg-slate-900, gradient button"
  [{:keys [scenario-name participants]}]
  [:div {:class "bg-slate-900 rounded-xl border border-slate-800 p-6 space-y-4"}
   [:h3 {:class "text-sm font-medium text-slate-300 mb-4"} "Record Decision"]

   ;; Scenario name input - data-driven
   [:div
    [:label {:class "text-xs text-slate-500 block mb-1"} "Scenario Name"]
    (forms/text-input
     {:value scenario-name
      :placeholder "e.g., Q4 Supplier Risk Mitigation"
      :on-change [:scenario/set-name :event/target.value]})]

   ;; Participant selection - data-driven
   [:div
    [:label {:class "text-xs text-slate-500 block mb-2"} "Participants (RACI)"]
    (participant-selector
     {:selected-ids participants})]

   ;; Record button with gradient - matches React exactly
   [:button
    {:class (classes "w-full py-3 rounded-lg font-semibold transition-all flex items-center justify-center gap-2"
                     (if (or (empty? scenario-name) (empty? participants))
                       "opacity-50 cursor-not-allowed bg-gradient-to-r from-cyan-600 to-blue-600 text-white"
                       "bg-gradient-to-r from-cyan-600 to-blue-600 text-white hover:from-cyan-700 hover:to-blue-700"))
     :disabled (or (empty? scenario-name) (empty? participants))
     :on {:click [:scenario/record-decision]}}
    [:svg {:class "w-5 h-5" :fill "none" :viewBox "0 0 24 24" :stroke "currentColor"}
     [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
             :d "M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"}]]
    "Record Decision"]])

(defn recent-decisions-panel
  "Panel showing recent decisions - separate container like React"
  [{:keys [decisions]}]
  [:div {:class "bg-slate-900 rounded-xl border border-slate-800 p-6"}
   [:h3 {:class "text-sm font-medium text-slate-300 mb-4"} "Recent Decisions"]
   (if (seq decisions)
     [:div {:class "space-y-3 max-h-64 overflow-y-auto"}
      (for [decision (reverse decisions)]
        (let [decision-id (:id decision)]
          ^{:key decision-id}
          [:div {:class "p-3 bg-slate-800 rounded-lg border border-slate-700"}
           [:div {:class "text-sm font-medium text-white mb-1"} (:name decision)]
           [:div {:class "text-xs text-slate-500 mb-2"}
            (.toLocaleString (js/Date. (:timestamp decision)))]
           (when (seq (:participants decision))
             [:div {:class "flex flex-wrap gap-1"}
              (for [p-id (:participants decision)]
                (let [participant (first (filter #(= (:id %) p-id) config/participant-roles))]
                  ^{:key p-id}
                  [:span {:class "text-xs bg-slate-700 px-2 py-0.5 rounded text-slate-300"}
                   (or (:name participant) (name p-id))]))])]))]
     ;; Empty state
     [:div {:class "text-center py-6 text-slate-500"}
      [:svg {:class "w-10 h-10 mx-auto mb-2 text-slate-600" :fill "none" :viewBox "0 0 24 24" :stroke "currentColor"}
       [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "1.5"
               :d "M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"}]]
      [:p {:class "text-sm"} "No decisions recorded yet"]])])

;; -----------------------------------------------------------------------------
;; Main Scenario View - Data-driven
;; -----------------------------------------------------------------------------

(defn scenario-view
  "Main scenario simulator view - fully data-driven"
  []
  (let [app-state @state/!app-state
        ;; Initialize scenario mode if not already in it
        _ (when-not (:scenario-observations app-state)
            (state/start-scenario!))
        ;; Get fresh state after potential initialization
        app-state @state/!app-state
        baseline-obs (:observations app-state)
        scenario-obs (:scenario-observations app-state)
        baseline-derived (:derived app-state)
        scenario-derived (or (:scenario-derived app-state) baseline-derived)
        active-category (:active-category app-state)
        scenario-name (:scenario-name app-state)
        participants (:participants app-state)
        decisions (:decisions app-state)]

    [:div {:class "space-y-6"}
     ;; Header with action buttons - data-driven
     [:div {:class "flex items-center justify-between"}
      [:div
       [:h2 {:class "text-xl font-semibold text-white"} "Scenario Simulator & Decision Room"]
       [:p {:class "text-slate-400 text-sm mt-1"}
        "Adjust observations, see impact, and record decisions with stakeholders"]]
      [:div {:class "flex gap-3"}
       (forms/button
        {:variant :outline
         :on-click [:scenario/reset]
         :children "Reset"})
       (forms/button
        {:variant :success
         :on-click [:scenario/apply]
         :children "Apply Changes"})]]

     ;; KPI Impact Summary - matches React container styling
     [:div {:class "bg-slate-900 rounded-xl border border-slate-800 p-6"}
      [:h3 {:class "text-sm font-medium text-slate-300 mb-4"} "Impact on Key Metrics"]
      (kpi-impact-row
       {:baseline-derived baseline-derived
        :scenario-derived scenario-derived
        :targets dashboard/targets})]

     ;; Main content: two columns
     [:div {:class "grid grid-cols-3 gap-6"}
      ;; Left column: Sliders (2/3 width) - matches React exactly
      [:div {:class "col-span-2 space-y-4"}
       ;; Category tabs - data-driven
       (category-tabs
        {:active-category active-category})

       ;; Sliders for active category in container
       [:div {:class "bg-slate-900 rounded-xl border border-slate-800 p-6 space-y-6"}
        (sliders-panel
         {:active-category active-category
          :baseline-observations baseline-obs
          :scenario-observations scenario-obs})]

       ;; Required actions
       (required-actions-panel
        {:baseline-observations baseline-obs
         :scenario-observations scenario-obs})]

      ;; Right column: Decision recording (1/3 width) - two containers like React
      [:div {:class "col-span-1 space-y-4"}
       (decision-recording-panel
        {:scenario-name scenario-name
         :participants participants})
       (recent-decisions-panel
        {:decisions decisions})]]]))
