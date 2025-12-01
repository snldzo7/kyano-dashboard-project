(ns nestle.views.time-travel
  "Time Travel - Decision Lineage Explorer
   Data-driven: all handlers are action vectors, no functions in components."
  (:require [clojure.string :as str]
            [kyano.ui.primitives :refer [classes]]
            [kyano.ui.forms :as forms]
            [nestle.state :as state]))

;; -----------------------------------------------------------------------------
;; Helper Functions (pure formatting, no state)
;; -----------------------------------------------------------------------------

(defn format-date [date-str]
  (when date-str
    (let [d (js/Date. date-str)]
      (.toLocaleDateString d "en-US" #js {:month "short" :day "numeric"}))))

(defn format-datetime [date-str]
  (when date-str
    (let [d (js/Date. date-str)]
      (str (.toLocaleDateString d "en-US" #js {:month "short" :day "numeric" :year "numeric"})
           " "
           (.toLocaleTimeString d "en-US" #js {:hour "2-digit" :minute "2-digit"})))))

(defn format-currency [v]
  (str "$" (.toFixed (/ v 1000000) 2) "M"))

(defn format-percent [v]
  (str (Math/round (* v 100)) "%"))

(defn format-number [v]
  (.toLocaleString (Math/round v)))

(defn get-change-pct [current baseline]
  (when (and baseline (not (zero? baseline)))
    (Math/round (* 100 (/ (- current baseline) (Math/abs baseline))))))

;; -----------------------------------------------------------------------------
;; Timeline Constants
;; -----------------------------------------------------------------------------

(def timeline-range
  {:start "2025-09-01"
   :end "2025-12-01"})

(defn date-to-pct [date-str]
  (let [start-ms (.getTime (js/Date. (:start timeline-range)))
        end-ms (.getTime (js/Date. (:end timeline-range)))
        date-ms (.getTime (js/Date. date-str))
        range (- end-ms start-ms)]
    (* 100 (/ (- date-ms start-ms) range))))

;; -----------------------------------------------------------------------------
;; Timeline Component - Data-driven
;; -----------------------------------------------------------------------------

(defn timeline-slider
  "Main timeline slider with decision markers - data-driven"
  [{:keys [time-cursor decisions selected-decision-id selected-fact historical-observations]}]
  (let [start-ms (.getTime (js/Date. (:start timeline-range)))
        end-ms (.getTime (js/Date. (:end timeline-range)))
        current-ms (if time-cursor (.getTime (js/Date. time-cursor)) end-ms)
        current-pct (if time-cursor (date-to-pct time-cursor) 100)
        ;; Fact history dots for selected fact
        fact-history (get historical-observations selected-fact [])]
    [:div {:class "bg-slate-900 rounded-xl border border-slate-800 p-6 space-y-4"}
     ;; Timeline track with multiple layers (h-24, top-8 matches React)
     [:div {:class "relative h-24 mb-4"}
      ;; Background track
      [:div {:class "absolute top-8 left-0 right-0 h-1 bg-slate-700 rounded-full"}]

      ;; Fact history dots (violet) - on timeline
      (for [[idx {:keys [tx-time]}] (map-indexed vector fact-history)]
        (let [pct (date-to-pct tx-time)]
          ^{:key (str "fact-" idx)}
          [:div {:class "absolute top-12 w-2 h-2 bg-violet-500 rounded-full transform -translate-x-1/2"
                 :style {:left (str pct "%")}}]))

      ;; Decision markers with labels (D1, D2, D3) - data-driven clicks
      (for [[idx {:keys [id timestamp scenario]}] (map-indexed vector decisions)]
        (let [pct (date-to-pct timestamp)
              is-selected (= id selected-decision-id)]
          ^{:key id}
          [:button
           {:class (classes "absolute top-5 w-6 h-6 rounded-full border-2 transform -translate-x-1/2 z-10"
                           "flex items-center justify-center text-xs font-bold transition-all"
                           (if is-selected
                             "bg-amber-500 border-amber-300 scale-125"
                             "bg-slate-700 border-slate-500 hover:bg-slate-600"))
            :style {:left (str pct "%")}
            :title scenario
            :on {:click [[:time-travel/set-cursor timestamp]
                         [:time-travel/select-decision id]]}}
           (str "D" (inc idx))]))

      ;; Current position indicator (w-4 h-4, top-6 matches React)
      [:div {:class "absolute top-6 w-4 h-4 bg-cyan-500 rounded-full border-2 border-white transform -translate-x-1/2 z-20"
             :style {:left (str current-pct "%")}}]

      ;; Time labels at top-16 position (matches React) - inside h-24 relative div
      [:div {:class "absolute top-16 left-0 text-xs text-slate-500"} "Sep 1"]
      [:div {:class "absolute top-16 left-1/4 text-xs text-slate-500 transform -translate-x-1/2"} "Oct 1"]
      [:div {:class "absolute top-16 left-1/2 text-xs text-slate-500 transform -translate-x-1/2"} "Oct 15"]
      [:div {:class "absolute top-16 left-3/4 text-xs text-slate-500 transform -translate-x-1/2"} "Nov 1"]
      [:div {:class "absolute top-16 right-0 text-xs text-slate-500"} "Now"]]

     ;; Slider input - uses millisecond timestamps, dispatches action data
     [:input
      {:type "range"
       :min start-ms
       :max end-ms
       :value current-ms
       :class "w-full h-2 bg-slate-700 rounded-lg appearance-none cursor-pointer accent-cyan-500"
       :on {:change [:time-travel/set-cursor-from-ms :event/target.value]}}]

     ;; Decision quick-jump buttons - data-driven
     [:div {:class "flex flex-wrap gap-2"}
      (for [{:keys [id scenario timestamp]} decisions]
        ^{:key id}
        (forms/button
         {:variant (if (= id selected-decision-id) :primary :outline)
          :size :sm
          :on-click [[:time-travel/set-cursor timestamp]
                     [:time-travel/select-decision id]]
          :children (str scenario " (" (format-date timestamp) ")")}))
      (forms/button
       {:variant :primary
        :size :sm
        :on-click [:time-travel/jump-to-now]
        :children "Jump to Now"})]]))

;; -----------------------------------------------------------------------------
;; KPI Comparison Panel
;; -----------------------------------------------------------------------------

(defn kpi-comparison-row
  "Single KPI comparison: then vs now"
  [{:keys [label then-value now-value format-fn worse-when-higher?]}]
  (let [change-pct (get-change-pct now-value then-value)
        is-worse (when change-pct
                   (if worse-when-higher?
                     (pos? change-pct)
                     (neg? change-pct)))]
    [:div {:class "flex items-center justify-between py-2 border-b border-slate-700 last:border-0"}
     [:span {:class "text-sm text-slate-300"} label]
     [:div {:class "flex items-center gap-3"}
      [:span {:class "text-sm text-slate-500"} (format-fn then-value)]
      [:span {:class "text-slate-600"} "\u2192"]
      [:span {:class (classes "text-sm font-semibold"
                              (cond
                                (nil? change-pct) "text-white"
                                is-worse "text-red-400"
                                :else "text-emerald-400"))}
       (format-fn now-value)]
      (when change-pct
        [:span {:class (classes "text-xs"
                                (if is-worse "text-red-400" "text-emerald-400"))}
         (str (if (pos? change-pct) "+" "") change-pct "%")])]]))

(defn kpi-comparison-panel
  "Panel showing KPIs then vs now"
  [{:keys [then-derived now-derived time-cursor]}]
  [:div {:class "bg-slate-800/50 rounded-2xl border border-slate-700 p-6"}
   [:h3 {:class "text-lg font-semibold text-white mb-4"}
    (if time-cursor
      (str "KPIs: " (format-date time-cursor) " vs Now")
      "Current KPIs")]
   [:div {:class "space-y-1"}
    (kpi-comparison-row
     {:label "Inventory Value"
      :then-value (:total-inventory-value then-derived)
      :now-value (:total-inventory-value now-derived)
      :format-fn format-currency
      :worse-when-higher? true})
    (kpi-comparison-row
     {:label "Service Risk"
      :then-value (:service-risk then-derived)
      :now-value (:service-risk now-derived)
      :format-fn format-percent
      :worse-when-higher? true})
    (kpi-comparison-row
     {:label "Cash Impact"
      :then-value (:cash-impact then-derived)
      :now-value (:cash-impact now-derived)
      :format-fn #(str "$" (format-number %))
      :worse-when-higher? true})]])

;; -----------------------------------------------------------------------------
;; Facts Known Panel
;; -----------------------------------------------------------------------------

(defn facts-known-panel
  "Panel showing facts known as of a date - matches React with cards not chips"
  [{:keys [as-of-observations current-observations time-cursor]}]
  (let [key-observations [:supplier-otif-dist :forecast-error-dist
                          :consensus-forecast-qty :planned-production-qty
                          :mfg-adherence-pct :open-po-qty-rpm]]
    [:div {:class "bg-slate-900 rounded-xl border border-slate-800 p-6"}
     [:h3 {:class "text-sm font-medium text-slate-300 mb-4"}
      (str "Facts Known As-Of " (format-datetime time-cursor))]
     [:div {:class "space-y-3"}
      (for [obs-key key-observations]
        (let [then-fact (get as-of-observations obs-key)
              now-fact (get current-observations obs-key)]
          (when then-fact
            ^{:key obs-key}
            [:div {:class "flex items-center justify-between p-3 bg-slate-800 rounded-lg"}
             [:div
              [:div {:class "text-sm text-white"} (:label then-fact)]
              [:div {:class "text-xs text-slate-500"} (:source then-fact)]]
             [:div {:class "text-right"}
              [:div {:class "text-lg font-bold text-amber-400"}
               (let [v (:value then-fact)]
                 (cond
                   (and (number? v) (< v 1) (> v 0)) (format-percent v)
                   (number? v) (format-number v)
                   :else (str v)))]
              [:div {:class "text-xs text-slate-500"}
               (str "Now: "
                    (let [v (:value now-fact)]
                      (cond
                        (and (number? v) (< v 1) (> v 0)) (format-percent v)
                        (number? v) (format-number v)
                        :else (str v))))]]])))]]))

;; -----------------------------------------------------------------------------
;; Decision Context Panel
;; -----------------------------------------------------------------------------

(defn empty-decisions-panel
  "Empty state when no decision is selected - matches React"
  []
  [:div {:class "bg-slate-900 rounded-xl border border-slate-800 p-6 text-center"}
   [:div {:class "text-4xl mb-3"} "\uD83D\uDCCB"]
   [:h3 {:class "text-sm font-medium text-slate-300 mb-2"} "No Decision Selected"]
   [:p {:class "text-xs text-slate-500"}
    "Click on a decision marker (D1, D2, D3) on the timeline above to view its context and the facts that were known at that time."]])

(defn decision-context-panel
  "Shows context of selected decision - matches React exactly with amber styling"
  [{:keys [decision decision-index]}]
  (if-not decision
    (empty-decisions-panel)
    [:div {:class "bg-amber-950 rounded-xl border border-amber-700 p-6"}
     ;; Header with badge
     [:div {:class "flex items-center gap-2 mb-4"}
      [:div {:class "w-8 h-8 rounded-full bg-amber-600 flex items-center justify-center text-white font-bold"}
       (str "D" (inc (or decision-index 0)))]
      [:div
       [:h3 {:class "font-medium text-white"} (:scenario decision)]
       [:div {:class "text-xs text-amber-400"} (format-datetime (:timestamp decision))]]]

     [:div {:class "space-y-3"}
      ;; Participants
      (when-let [parts (:participants decision)]
        [:div
         [:div {:class "text-xs text-slate-400 mb-1"} "Participants"]
         [:div {:class "flex gap-1"}
          (for [[idx p] (map-indexed vector parts)]
            ^{:key idx}
            [:span {:class "text-xs bg-slate-800 px-2 py-1 rounded text-slate-300"}
             (if (keyword? p) (name p) p)])]])

      ;; Facts Known at Decision Time
      (when-let [facts (:facts-known-at decision)]
        [:div
         [:div {:class "text-xs text-slate-400 mb-1"} "Facts Known at Decision Time"]
         [:div {:class "grid grid-cols-2 gap-2"}
          (for [[k v] facts]
            ^{:key k}
            [:div {:class "text-xs bg-slate-800 p-2 rounded"}
             [:span {:class "text-slate-400"} (name k) ":"]
             [:span {:class "text-white ml-1 font-medium"}
              (cond
                (and (number? v) (< v 1) (> v 0)) (format-percent v)
                (number? v) (format-number v)
                :else (str v))]])]])

      ;; Outcome
      (when-let [outcome (:outcome decision)]
        [:div
         [:div {:class "text-xs text-slate-400 mb-1"} "Outcome"]
         [:div {:class "text-sm text-white"} outcome]])

      ;; Status badge
      (when-let [status (:status decision)]
        [:div {:class (classes "text-xs px-2 py-1 rounded inline-block"
                               (case status
                                 :executed "bg-emerald-900 text-emerald-400"
                                 :in-progress "bg-amber-900 text-amber-400"
                                 "bg-slate-700 text-slate-400"))}
         (name status)])]]))

;; -----------------------------------------------------------------------------
;; Fact Evolution Panel - Data-driven
;; -----------------------------------------------------------------------------

(defn format-fact-value
  "Format fact value based on the observation key"
  [fact-key value]
  (cond
    (or (str/includes? (name fact-key) "pct")
        (str/includes? (name fact-key) "dist")
        (str/includes? (name fact-key) "cv")
        (str/includes? (name fact-key) "rate"))
    (str (Math/round (* value 100)) "%")

    (or (str/includes? (name fact-key) "cost")
        (str/includes? (name fact-key) "failure"))
    (str "$" value)

    :else (.toLocaleString (Math/round value))))

(defn fact-evolution-panel
  "Shows how a fact evolved over time - data-driven select"
  [{:keys [selected-fact historical-observations time-cursor]}]
  (let [history (get historical-observations selected-fact [])
        ;; Reverse to show newest first like React
        history-reversed (vec (reverse history))
        as-of-ms (if time-cursor
                   (.getTime (js/Date. time-cursor))
                   (.getTime (js/Date.)))]
    [:div {:class "bg-slate-900 rounded-xl border border-slate-800 p-6"}
     [:div {:class "flex items-center justify-between mb-4"}
      [:h3 {:class "text-sm font-medium text-slate-300"} "Fact Evolution Over Time"]
      [:select
       {:class "text-sm bg-slate-800 border border-slate-700 rounded px-2 py-1 text-white"
        :value (name selected-fact)
        :on {:change [:time-travel/set-selected-fact :event/target.value]}}
       [:option {:value "supplier-otif-dist"} "Supplier OTIF"]
       [:option {:value "forecast-error-dist"} "Forecast Error"]
       [:option {:value "consensus-forecast-qty"} "Consensus Forecast"]
       [:option {:value "planned-production-qty"} "Planned Production"]
       [:option {:value "mfg-adherence-pct"} "Mfg Adherence"]
       [:option {:value "fg-opening-stock"} "FG Opening Stock"]
       [:option {:value "rpm-opening-stock"} "RPM Opening Stock"]
       [:option {:value "open-po-qty-rpm"} "Open PO Qty"]]]

     [:div {:class "space-y-2 max-h-64 overflow-y-auto"}
      (for [[idx fact] (map-indexed vector history-reversed)]
        (let [fact-ms (.getTime (js/Date. (:tx-time fact)))
              ;; Is this the "current" fact for the as-of date?
              is-current-as-of (and (<= fact-ms as-of-ms)
                                    (or (zero? idx)
                                        (> (.getTime (js/Date. (:tx-time (get history-reversed (dec idx))))) as-of-ms)))
              is-past (<= fact-ms as-of-ms)]
          ^{:key idx}
          [:div {:class (classes "p-3 rounded-lg border"
                                 (cond
                                   is-current-as-of "bg-cyan-950 border-cyan-700"
                                   is-past "bg-slate-800 border-slate-700"
                                   :else "bg-slate-900 border-slate-800 opacity-50"))}
           [:div {:class "flex items-center justify-between"}
            [:div
             [:div {:class "text-lg font-bold text-white"}
              (format-fact-value selected-fact (:value fact))]
             [:div {:class "text-xs text-slate-500"} (:note fact)]]
            [:div {:class "text-right"}
             [:div {:class "text-xs text-slate-400"} "Recorded"]
             [:div {:class "text-xs text-slate-300"} (format-datetime (:tx-time fact))]]]
           (when is-current-as-of
             [:div {:class "mt-2 text-xs text-cyan-400"} "\u2190 Value known at selected time"])]))]]))

;; -----------------------------------------------------------------------------
;; Example Queries Panel - Data-driven
;; -----------------------------------------------------------------------------

(defn query-card
  "Single query card component - data-driven"
  [{:keys [label on-click]}]
  [:button
   {:class (str "p-4 bg-slate-800 rounded-lg border border-slate-700 "
                "text-left hover:border-cyan-700 transition-all cursor-pointer group")
    :on {:click on-click}}
   [:div {:class "text-sm text-white group-hover:text-cyan-400"} label]
   [:div {:class "text-xs text-slate-500 mt-2 group-hover:text-slate-400"} "Click to query \u2192"]])

(defn example-queries-panel
  "Shows example temporal queries - data-driven"
  []
  [:div {:class "bg-slate-900 rounded-xl border border-slate-800 p-6"}
   [:h3 {:class "text-sm font-medium text-slate-300 mb-4"} "Example Temporal Queries"]
   [:div {:class "grid grid-cols-3 gap-3"}
    (query-card
     {:label "What did we know when we made the Q4 Initial Planning decision?"
      :on-click [:time-travel/set-cursor "2025-09-15"]})
    (query-card
     {:label "What was the Supplier OTIF as we knew it on October 15th?"
      :on-click [:time-travel/set-cursor "2025-10-15"]})
    (query-card
     {:label "How did the forecast evolve from September to November?"
      :on-click [:time-travel/set-selected-fact :consensus-forecast-qty]})]])

;; -----------------------------------------------------------------------------
;; Main Time Travel View - Data-driven
;; -----------------------------------------------------------------------------

(defn time-travel-view
  "Main time travel view - fully data-driven"
  []
  (let [app-state @state/!app-state
        time-cursor (:time-cursor app-state)
        historical-obs (:historical-observations app-state)
        historical-decisions (:historical-decisions app-state)
        selected-decision (:selected-decision app-state)
        selected-fact (:selected-fact app-state)
        now-derived (:derived app-state)
        now-obs (:observations app-state)

        ;; Calculate as-of state based on time cursor
        as-of-obs (or (:as-of-observations app-state) now-obs)
        as-of-derived (or (:as-of-derived app-state) now-derived)

        ;; Find decision by ID and index
        selected-decision-index (when selected-decision
                                  (first (keep-indexed #(when (= (:id %2) selected-decision) %1) historical-decisions)))
        selected-decision-data (when selected-decision
                                 (first (filter #(= (:id %) selected-decision) historical-decisions)))]

    [:div {:class "space-y-6"}
     ;; Header
     [:div
      [:h2 {:class "text-xl font-semibold text-white flex items-center gap-2"}
       [:span "\u23f1"] "Time Travel \u2014 Decision Lineage Explorer"]
      [:p {:class "text-slate-400 text-sm mt-1"}
       "Query the system as it was at any point in time. See what facts were known when decisions were made."]]

     ;; Timeline - all data-driven
     (timeline-slider
      {:time-cursor time-cursor
       :decisions historical-decisions
       :selected-decision-id selected-decision
       :selected-fact selected-fact
       :historical-observations historical-obs})

     ;; Main content: two columns
     [:div {:class "grid grid-cols-2 gap-6"}
      ;; Left column
      [:div {:class "space-y-6"}
       ;; KPI comparison
       (kpi-comparison-panel
        {:then-derived as-of-derived
         :now-derived now-derived
         :time-cursor time-cursor})

       ;; Facts known (cards layout matching React)
       (facts-known-panel
        {:as-of-observations as-of-obs
         :current-observations now-obs
         :time-cursor time-cursor})]

      ;; Right column
      [:div {:class "space-y-6"}
       ;; Decision context (amber styling matching React)
       (decision-context-panel
        {:decision selected-decision-data
         :decision-index selected-decision-index})

       ;; Fact evolution - data-driven
       (fact-evolution-panel
        {:selected-fact selected-fact
         :historical-observations historical-obs
         :time-cursor time-cursor})]]

     ;; Example queries - data-driven
     (example-queries-panel)]))
