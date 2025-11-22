# Kyano Dashboard - Clay DSL Implementation

**Complete rewrite of React dashboard using Clay's data-driven, element-based DSL.**

This document recreates the exact visual design of the Kyano Decision Intelligence Dashboard using Clay's native semantics: element trees, flat property maps, immediate-mode interactivity, and pure data composition.

---

## Table of Contents

1. [Data Definitions](#data-definitions)
2. [Atoms](#atoms) - Basic Elements
3. [Molecules](#molecules) - Simple Compositions
4. [Organisms](#organisms) - Complex Sections
5. [Templates](#templates) - Layout Structures
6. [Page](#page) - Complete Dashboard
7. [Context & State](#context--state)
8. [Interactivity Patterns](#interactivity-patterns)

---

## Data Definitions

All static data structures used throughout the dashboard.

```clojure
;; ============================================================================
;; TIMELINE DATA
;; ============================================================================

(def historical-data
  [{:date "2024-09-10"
    :otif 94.5
    :status :normal
    :event "Baseline operations"
    :status-color :green-500
    :orders 1847
    :on-time 1745
    :delayed 102
    :risk "Low"}

   {:date "2024-09-11"
    :otif 94.2
    :status :normal
    :event "Baseline operations"
    :status-color :green-500
    :orders 1893
    :on-time 1783
    :delayed 110
    :risk "Low"}

   {:date "2024-09-12"
    :otif 93.8
    :status :normal
    :event "Baseline operations"
    :status-color :green-500
    :orders 1826
    :on-time 1713
    :delayed 113
    :risk "Low"}

   {:date "2024-09-13"
    :otif 92.1
    :status :warning
    :event "Minor delay in Nordic region"
    :status-color :yellow-500
    :orders 1954
    :on-time 1800
    :delayed 154
    :risk "Medium"}

   {:date "2024-09-14"
    :otif 89.5
    :status :alert
    :event "Demand spike detected: Norway +35%"
    :status-color :orange-500
    :orders 2134
    :on-time 1910
    :delayed 224
    :risk "High"}

   {:date "2024-09-15"
    :otif 87.2
    :status :critical
    :event "Stockout risk: 1.2 days"
    :status-color :red-500
    :orders 2298
    :on-time 2004
    :delayed 294
    :risk "Critical"}

   {:date "2024-09-16"
    :otif 86.8
    :status :critical
    :event "Critical: OTIF dropping"
    :status-color :red-500
    :orders 2367
    :on-time 2054
    :delayed 313
    :risk "Critical"}

   {:date "2024-09-17"
    :otif 88.5
    :status :recovering
    :event "DECISION: Transfer 13K cases approved"
    :status-color :blue-500
    :orders 2189
    :on-time 1937
    :delayed 252
    :risk "Medium"}

   {:date "2024-09-18"
    :otif 92.3
    :status :recovering
    :event "Transfer in transit"
    :status-color :blue-500
    :orders 2045
    :on-time 1888
    :delayed 157
    :risk "Medium"}

   {:date "2024-09-19"
    :otif 95.8
    :status :recovered
    :event "Transfer delivered"
    :status-color :green-500
    :orders 1967
    :on-time 1884
    :delayed 83
    :risk "Low"}

   {:date "2024-09-20"
    :otif 98.0
    :status :excellent
    :event "OTIF restored, €26K saved"
    :status-color :green-600
    :orders 1823
    :on-time 1787
    :delayed 36
    :risk "Very Low"}

   {:date "2024-09-21"
    :otif 97.2
    :status :excellent
    :event "Stabilized operations"
    :status-color :green-600
    :orders 1891
    :on-time 1838
    :delayed 53
    :risk "Very Low"}

   {:date "2024-09-22"
    :otif 96.5
    :status :normal
    :event "Normal operations"
    :status-color :green-500
    :orders 1856
    :on-time 1791
    :delayed 65
    :risk "Low"}])

(def optimized-data
  [{:date "2024-09-10" :otif 94.5 :status :normal :event "Baseline operations"
    :status-color :green-500 :orders 1847 :on-time 1745 :delayed 102 :risk "Low"}
   {:date "2024-09-11" :otif 94.2 :status :normal :event "Baseline operations"
    :status-color :green-500 :orders 1893 :on-time 1783 :delayed 110 :risk "Low"}
   {:date "2024-09-12" :otif 93.8 :status :normal :event "Baseline operations"
    :status-color :green-500 :orders 1826 :on-time 1713 :delayed 113 :risk "Low"}
   {:date "2024-09-13" :otif 92.1 :status :warning :event "Minor delay detected"
    :status-color :yellow-500 :orders 1954 :on-time 1800 :delayed 154 :risk "Medium"}
   {:date "2024-09-14" :otif 89.5 :status :alert :event "Early warning triggered"
    :status-color :orange-500 :orders 2134 :on-time 1910 :delayed 224 :risk "High"}
   {:date "2024-09-15" :otif 90.2 :status :recovering :event "OPTIMIZED: Proactive transfer"
    :status-color :blue-500 :orders 2298 :on-time 2073 :delayed 225 :risk "Medium"}
   {:date "2024-09-16" :otif 93.5 :status :recovering :event "Transfer in progress"
    :status-color :blue-500 :orders 2367 :on-time 2213 :delayed 154 :risk "Low"}
   {:date "2024-09-17" :otif 96.8 :status :excellent :event "Crisis averted"
    :status-color :green-600 :orders 2189 :on-time 2119 :delayed 70 :risk "Very Low"}
   {:date "2024-09-18" :otif 97.5 :status :excellent :event "High performance"
    :status-color :green-600 :orders 2045 :on-time 1994 :delayed 51 :risk "Very Low"}
   {:date "2024-09-19" :otif 97.1 :status :excellent :event "Operations normalized"
    :status-color :green-600 :orders 1967 :on-time 1910 :delayed 57 :risk "Very Low"}
   {:date "2024-09-20" :otif 98.2 :status :excellent :event "Optimal performance"
    :status-color :green-600 :orders 1823 :on-time 1790 :delayed 33 :risk "Very Low"}
   {:date "2024-09-21" :otif 97.6 :status :excellent :event "Sustained excellence"
    :status-color :green-600 :orders 1891 :on-time 1846 :delayed 45 :risk "Very Low"}
   {:date "2024-09-22" :otif 97.2 :status :excellent :event "Normal operations"
    :status-color :green-600 :orders 1856 :on-time 1804 :delayed 52 :risk "Low"}])

;; ============================================================================
;; TEAM DATA
;; ============================================================================

(def team-members
  [{:id :sarah :name "Sarah Chen" :role "Supply Chain Director" :color :blue-500}
   {:id :marcus :name "Marcus Johnson" :role "Finance Manager" :color :green-500}
   {:id :emma :name "Emma Larsen" :role "Logistics Coordinator" :color :yellow-500}
   {:id :david :name "David Park" :role "Demand Planner" :color :purple-500}
   {:id :lisa :name "Lisa Anderson" :role "Customer Success" :color :pink-500}])

;; ============================================================================
;; SCENARIO DATA
;; ============================================================================

(def decision-scenarios
  [{:title "Emergency Transfer"
    :badge "✓ Chosen"
    :badge-color :green-600
    :cost "€5,080"
    :time "36h"
    :otif "+11.2%"
    :roi "5.2x"
    :roi-color :green-500
    :why "Fastest response with highest ROI. Leverages Denmark warehouse (240km). Risk: 12% (Low)"
    :bg-gradient [:from :green-700 :to :green-800]}

   {:title "Expedited Production"
    :badge "Alternative"
    :badge-color :slate-600
    :cost "€8,920"
    :time "72h"
    :otif "+8.5%"
    :roi "2.9x"
    :roi-color :slate-300
    :why "Higher cost, longer lead time. Risk: 28% (Medium). Production capacity constraint."
    :bg-gradient [:from :slate-700 :to :slate-800]}

   {:title "Do Nothing"
    :badge "Rejected"
    :badge-color :slate-600
    :cost "€0"
    :time "—"
    :otif "-7.8%"
    :roi "-€31K"
    :roi-color :red-500
    :why "Zero cost but severe consequences. Lost revenue + penalties + damaged relationships. Risk: 89% (Critical)"
    :bg-gradient [:from :slate-700 :to :slate-800]}])

(def comparison-metrics
  [{:label "Cost Efficiency"
    :bars [{:text "Emergency: €5,080" :width 0.85 :winner true}
           {:text "Expedited: €8,920" :width 0.50}
           {:text "Wait: €0" :width 0.95}]}

   {:label "ROI Multiple"
    :bars [{:text "5.2x" :width 1.0 :winner true}
           {:text "2.9x" :width 0.56}
           {:text "—" :width 0}]}

   {:label "Speed"
    :bars [{:text "36 hours" :width 1.0 :winner true}
           {:text "72 hours" :width 0.50}
           {:text "∞" :width 0.20}]}

   {:label "Risk Level"
    :bars [{:text "12% Low" :width 0.12 :winner true :color :green-600}
           {:text "28% Med" :width 0.28 :color :yellow-600}
           {:text "89% Critical" :width 0.89 :color :red-600}]}

   {:label "OTIF Improvement"
    :bars [{:text "+11.2%" :width 1.0 :winner true}
           {:text "+8.5%" :width 0.76}
           {:text "-7.8%" :width 0.30 :color :red-600}]}

   {:label "Value Created"
    :bars [{:text "€26,400" :width 1.0 :winner true}
           {:text "€16,850" :width 0.62}
           {:text "-€31,000" :width 0.15 :color :red-600}]}])

(def similar-cases
  [{:id "NORD-2024-06-15"
    :desc "Sweden summer demand surge"
    :decision "Emergency transfer + Air freight"
    :otif "96.8%"
    :cost "€7,200"
    :value "€31,500"
    :similarity "94%"}

   {:id "NORD-2023-12-03"
    :desc "Denmark holiday spike"
    :decision "Regional inventory rebalancing"
    :otif "98.1%"
    :cost "€3,840"
    :value "€28,900"
    :similarity "92%"}

   {:id "NORD-2023-11-08"
    :desc "Norway stockout risk"
    :decision "Emergency transfer"
    :otif "97.2%"
    :cost "€5,120"
    :value "€22,400"
    :similarity "88%"}])
```

---

## Atoms

Basic building blocks - individual UI elements with no children or minimal text children.

### Text Elements

```clojure
(defn text-atom
  "Generic text element with size and color variants."
  [id content {:keys [size color weight transform]
               :or {size 14 color :slate-100 weight :normal}}]
  [id content {:size size
               :color color
               :weight weight
               :text-transform transform}])

(defn heading-1 [id text]
  (text-atom id text {:size 24 :weight :bold}))

(defn heading-2 [id text]
  (text-atom id text {:size 18 :weight :semibold}))

(defn heading-3 [id text]
  (text-atom id text {:size 16 :weight :semibold}))

(defn label-text [id text]
  (text-atom id text {:size 11 :color :slate-400 :transform :uppercase}))

(defn body-text [id text]
  (text-atom id text {:size 14 :color :slate-100}))

(defn small-text [id text]
  (text-atom id text {:size 12 :color :slate-400}))

(defn tiny-text [id text]
  (text-atom id text {:size 11 :color :slate-500}))
```

### Buttons

```clojure
(defn button
  "Interactive button with hover state."
  [id label {:keys [variant on-click disabled?]} ctx]
  (let [base-colors {:primary {:normal :blue-600 :hover :blue-700}
                     :success {:normal :green-600 :hover :green-700}
                     :warning {:normal :yellow-600 :hover :yellow-700}
                     :danger {:normal :red-600 :hover :red-700}
                     :secondary {:normal :slate-600 :hover :slate-500}}
        colors (get base-colors variant :primary)
        hovered? (contains? (:hovered-ids ctx) id)
        bg-color (if hovered? (:hover colors) (:normal colors))]
    [id
     {:bg (if disabled? :slate-600 bg-color)
      :pad [6 12]
      :radius 4
      :size [:fit :fit]
      :align {:y :center}
      :data {:on-click (when-not disabled? on-click)
             :cursor (if disabled? :not-allowed :pointer)}}
     [[(keyword (str (name id) "-text"))
       label
       {:size 12
        :color (if disabled? :slate-400 :white)
        :weight :semibold}]]]))

(defn icon-button
  "Button with emoji/icon."
  [id icon label variant on-click ctx]
  (button id (str icon " " label) {:variant variant :on-click on-click} ctx))
```

### Badges

```clojure
(defn badge
  "Small status or category badge."
  [id text bg-color]
  [id
   {:bg bg-color
    :pad [4 8]
    :radius 4
    :size [:fit :fit]}
   [[(keyword (str (name id) "-text"))
     text
     {:size 11 :weight :bold :color :white}]]])

(defn status-badge
  "Dynamic status badge with color."
  [id status color]
  (badge id status color))
```

### Range Slider

```clojure
(defn range-slider
  "Horizontal slider input."
  [id {:keys [min max value on-change step]} ctx]
  [:input-wrapper
   {:dir :col :gap 4 :size [:grow :fit]}
   [[:slider-track
     {:bg :slate-700
      :radius 8
      :size [:grow [:fixed 8]]
      :custom :range-input
      :data {:type :range
             :min min
             :max max
             :value value
             :step (or step 1)
             :on-change on-change}}]]])
```

### Metric Display

```clojure
(defn large-metric
  "Big number display for key metrics."
  [id value color]
  [id
   {:size [:fit :fit]}
   [[(keyword (str (name id) "-value"))
     (str value)
     {:size 36 :weight :extrabold :color (or color :slate-100)}]]])

(defn huge-metric
  "Massive number for primary metrics."
  [id value color]
  [id
   {:size [:fit :fit]}
   [[(keyword (str (name id) "-value"))
     (str value)
     {:size 72 :weight :black :color (or color :slate-100)}]]])
```

### Avatar Circle

```clojure
(defn avatar-circle
  "Circular avatar with initials."
  [id initials bg-color]
  [id
   {:size [[:fixed 36] [:fixed 36]]
    :radius 18
    :bg [:gradient :135deg bg-color (c/darken bg-color 0.1)]
    :align {:x :center :y :center}}
   [[(keyword (str (name id) "-initials"))
     initials
     {:size 14 :weight :bold :color :white}]]])
```

---

## Molecules

Simple compositions of atoms - cards, controls, list items.

### Metric Card

```clojure
(defn metric-card
  "Card displaying a single metric with label and subtitle."
  [id label value subtitle & [{:keys [value-color]}]]
  [id
   {:bg :slate-800
    :radius 8
    :border [:slate-700 1]
    :pad 20
    :dir :col
    :gap 8
    :size [:grow :fit]}
   [[(label-text (keyword (str (name id) "-label")) label)]
    [(large-metric (keyword (str (name id) "-value")) value value-color)]
    [(tiny-text (keyword (str (name id) "-subtitle")) subtitle)]]])
```

### Timeline Control

```clojure
(defn timeline-control
  "Slider with play/jump buttons and date labels."
  [current-idx max-idx is-playing? on-change on-play on-jump ctx]
  (let [current-data (nth historical-data current-idx)
        status-text (-> (:status current-data) name clojure.string/capitalize)]
    [:timeline-control
     {:dir :col
      :gap 12
      :pad [16 24]
      :bg :slate-800
      :border [:bottom :slate-700 1]}

     ;; Header with date, status, and buttons
     [[:timeline-header
       {:dir :row :align {:x :space-between :y :center} :size [:grow :fit]}

       [[:date-info
         {:dir :row :gap 8 :align {:y :center}}
         [[(body-text :current-date (:date current-data))
           " | Status: "]
          [(text-atom :status-text status-text
             {:size 14 :weight :semibold :color (:status-color current-data)})]
          [(small-text :event-text (str " | " (:event current-data)))]]]]

       [:button-group
        {:dir :row :gap 8}
        [[(icon-button :btn-play "▶"
            (if is-playing? "Playing..." "Play Timeline")
            :primary on-play ctx)]
         [(icon-button :btn-jump "⚡" "Jump to Decision"
            :success on-jump ctx)]]]]]

     ;; Slider
     [[:slider-container
       {:size [:grow :fit]}
       [(range-slider :timeline-slider
          {:min 0 :max max-idx :value current-idx :on-change on-change}
          ctx)]]]

     ;; Date labels
     [[:date-labels
       {:dir :row :align {:x :space-between :y :center} :size [:grow :fit]}
       [[(tiny-text :label-start "Sep 10")]
        [(tiny-text :label-mid "Sep 16 (Decision Point)")]
        [(tiny-text :label-end "Sep 22 (Today)")]]]]]))
```

### Team Member Card

```clojure
(defn team-member-card
  "Draggable team member card with avatar, name, and role."
  [member is-selected? ctx]
  (let [{:keys [id name role color]} member
        card-id (keyword (str "member-" (clojure.core/name id)))]
    [card-id
     {:dir :row
      :gap 8
      :pad 8
      :radius 4
      :border (if is-selected? [:blue-500 2] [:transparent 2])
      :bg (if is-selected? :blue-900 :slate-900)
      :size [:grow :fit]
      :align {:y :center}
      :data {:draggable true
             :drag-data {:type :team-member :member member}}}

     [[(avatar-circle (keyword (str "avatar-" (name id)))
         (clojure.string/join (map first (clojure.string/split name #" ")))
         color)]

      [:member-info
       {:dir :col :gap 2 :size [:grow :fit]}
       [[(text-atom :name-text name {:size 14 :weight :semibold})]
        [(small-text :role-text role)]]]]]))
```

### Activity Log Entry

```clojure
(defn activity-log-entry
  "Single timeline entry with dot, user, time, and action."
  [idx entry is-last?]
  (let [{:keys [user time action]} entry
        entry-id (keyword (str "log-entry-" idx))]
    [entry-id
     {:dir :row :gap 0 :size [:grow :fit] :pad [0 0 12 0]}

     ;; Timeline line and dot
     [[:timeline-marker
       {:size [[:fixed 32] :fit] :align {:x :center} :dir :col}
       [[:dot
         {:size [[:fixed 16] [:fixed 16]]
          :radius 8
          :bg :blue-600
          :border [:slate-900 2]}]

        (when-not is-last?
          [:line
           {:size [[:fixed 2] :grow]
            :bg :slate-600}])]]]

     ;; Content
     [[:entry-content
       {:bg :slate-900 :radius 4 :pad 12 :size [:grow :fit] :dir :col :gap 4}
       [[:header
         {:dir :row :align {:x :space-between :y :center}}
         [[(text-atom :user-text user {:size 14 :weight :semibold})]
          [(tiny-text :time-text time)]]]

        [(small-text :action-text action)]]]]]))
```

### Tab Button

```clojure
(defn tab-button
  "Single tab in navigation strip."
  [id label active? on-click ctx]
  (let [hovered? (contains? (:hovered-ids ctx) id)]
    [id
     {:pad [12 20]
      :bg (if active? :slate-700 :transparent)
      :border [:bottom (if active? :blue-500 :transparent) 2]
      :size [:fit :fit]
      :data {:on-click on-click}}
     [[(text-atom (keyword (str (name id) "-text")) label
         {:size 14
          :weight :semibold
          :color (cond
                   active? :slate-100
                   hovered? :slate-200
                   :else :slate-400)})]]]))
```

### Scenario Card

```clojure
(defn scenario-card
  "Decision scenario with badge, metrics, and reasoning."
  [scenario ctx]
  (let [{:keys [title badge badge-color cost time otif roi roi-color why bg-gradient]} scenario
        card-id (keyword (str "scenario-" (clojure.string/lower-case (clojure.string/replace title #" " "-"))))]
    [card-id
     {:bg [:gradient :to-br (first bg-gradient) (second bg-gradient)]
      :radius 8
      :border [:slate-600 1]
      :pad 20
      :dir :col
      :gap 16
      :size [:grow :fit]}

     ;; Header with badge
     [[:header
       {:dir :row :align {:x :space-between :y :flex-start}}
       [[(heading-3 :title text title)]
        [(badge :badge-elem badge badge-color)]]]]

     ;; Metrics grid (2x2)
     [[:metrics-grid
       {:dir :col :gap 12}
       [[:row-1
         {:dir :row :gap 12}
         [[:metric-cost
           {:dir :col :gap 4 :size [:grow :fit]}
           [[(label-text :cost-label "COST")]
            [(text-atom :cost-value cost {:size 24 :weight :bold})]]]

          [:metric-time
           {:dir :col :gap 4 :size [:grow :fit]}
           [[(label-text :time-label "TIME")]
            [(text-atom :time-value time {:size 24 :weight :bold})]]]]]

        [:row-2
         {:dir :row :gap 12}
         [[:metric-otif
           {:dir :col :gap 4 :size [:grow :fit]}
           [[(label-text :otif-label "OTIF IMPACT")]
            [(text-atom :otif-value otif {:size 24 :weight :bold})]]]

          [:metric-roi
           {:dir :col :gap 4 :size [:grow :fit]}
           [[(label-text :roi-label "ROI")]
            [(text-atom :roi-value roi {:size 24 :weight :bold :color roi-color})]]]]]]]

     ;; Reasoning box
     [[:reasoning
       {:bg [0 0 0 51]  ; rgba(0,0,0,0.2)
        :radius 4
        :pad 12}
       [[(text-atom :why-label "Why: " {:size 12 :weight :bold})]
        [(text-atom :why-text why {:size 12 :color :slate-300})]]]]]))
```

---

## Organisms

Complex sections combining multiple molecules and atoms.

### Header Bar

```clojure
(defn header-bar
  "Top dashboard header with title and current OTIF."
  [current-data]
  [:header
   {:bg [:gradient :to-br :slate-900 :slate-800]
    :border [:bottom :slate-700 2]
    :pad [16 24]
    :dir :row
    :align {:x :space-between :y :center}
    :size [:grow :fit]}

   [[:title-section
     {:dir :col :gap 4}
     [[(heading-1 :app-title "Kyano Decision Intelligence")]
      [(small-text :app-subtitle "Supply Chain OTIF Performance & Decision Support")]]]

    [:otif-display
     {:dir :col :gap 4 :align {:x :right}}
     [[(text-atom :otif-value (str (:otif current-data) "%")
         {:size 32 :weight :extrabold :color (:status-color current-data)})]
      [(label-text :otif-label "Current OTIF")]]]]])
```

### Tab Navigation

```clojure
(defn tab-navigation
  "Horizontal tab strip with 6 tabs."
  [active-tab on-change ctx]
  (let [tabs [{:id :overview :label "Overview"}
              {:id :decision-history :label "Decision History"}
              {:id :decision-room :label "Decision Room"}
              {:id :what-if :label "What-If Scenarios"}
              {:id :retailer-collab :label "Retailer Collaboration"}
              {:id :similar-cases :label "Similar Cases"}]]
    [:tab-strip
     {:bg :slate-800
      :border [:bottom :slate-700 1]
      :pad [0 24]
      :dir :row
      :gap 4
      :size [:grow :fit]}

     (mapv (fn [{:keys [id label]}]
             (tab-button id label (= id active-tab) #(on-change id) ctx))
           tabs)]))
```

### OTIF Chart

```clojure
(defn otif-chart
  "Line chart showing OTIF performance with hover tooltip."
  [data current-idx show-optimized? title ctx]
  (let [visible-data (take (inc current-idx) data)
        hover-point (:chart-hover-point ctx)]
    [:chart-container
     {:bg :slate-800
      :radius 8
      :border [:slate-700 1]
      :pad 20
      :dir :col
      :gap 16
      :size [:grow :fit]}

     ;; Title
     [[:chart-header
       {:dir :row :align {:x :space-between :y :center}}
       [[(heading-2 :chart-title title)]

        (when show-optimized?
          [:info-badge
           {:bg :slate-700
            :pad [4 8]
            :radius 4
            :size [:fit :fit]
            :data {:on-click :show-optimization-info}}
           [[(text-atom :info-text "ℹ️ What if?"
               {:size 12 :color :slate-300})]]])]]]

     ;; Canvas chart (custom widget)
     [[:canvas-chart
       {:custom :line-chart
        :size [[:fixed 800] [:fixed 300]]
        :data {:points visible-data
               :x-accessor :date
               :y-accessor :otif
               :y-range [80 100]
               :point-color-accessor :status-color
               :show-grid true
               :grid-lines 4
               :show-actual-faded show-optimized?
               :actual-data (when show-optimized?
                             (take (inc current-idx) historical-data))}
        :on-hover :update-chart-hover}]]

     ;; Hover tooltip
     (when hover-point
       [:tooltip
        {:float {:to :parent
                 :at [:left-top :left-top]
                 :offset [(:x hover-point) (:y hover-point)]
                 :z 50}
         :bg :slate-800
         :border [:blue-500 2]
         :radius 8
         :pad 12
         :size [[:fixed 200] :fit]}
        [[:tooltip-content
          {:dir :col :gap 4}
          [[(text-atom :tooltip-date (:date (:data hover-point))
              {:size 14 :weight :bold :color :blue-400})]

           [:metric-row {:dir :row :align {:x :space-between}}
            [[(small-text :otif-label-tt "OTIF:")]
             [(text-atom :otif-value-tt (str (:otif (:data hover-point)) "%")
                {:size 12 :weight :bold :color (:status-color (:data hover-point))})]]]

           [:metric-row-2 {:dir :row :align {:x :space-between}}
            [[(small-text :orders-label "Orders:")]
             [(text-atom :orders-value (str (:orders (:data hover-point)))
                {:size 12 :weight :bold})]]]

           [:metric-row-3 {:dir :row :align {:x :space-between}}
            [[(small-text :ontime-label "On Time:")]
             [(text-atom :ontime-value (str (:on-time (:data hover-point)))
                {:size 12 :weight :bold :color :green-500})]]]

           [:metric-row-4 {:dir :row :align {:x :space-between}}
            [[(small-text :delayed-label "Delayed:")]
             [(text-atom :delayed-value (str (:delayed (:data hover-point)))
                {:size 12 :weight :bold :color :red-500})]]]

           [:metric-row-5 {:dir :row :align {:x :space-between}}
            [[(small-text :risk-label "Risk:")]
             [(text-atom :risk-value (:risk (:data hover-point))
                {:size 12 :weight :bold})]]]

           [:divider {:size [:grow [:fixed 1]] :bg :slate-600}]

           [(tiny-text :event-desc (:event (:data hover-point)))]]]])])

     ;; Optimization impact message
     (when show-optimized?
       [:impact-message
        {:bg [34 197 94 51]  ; green-500 with alpha
         :border [:left :green-500 4]
         :radius 4
         :pad 12}
        [[(text-atom :impact-title "Optimization Impact: "
            {:size 14 :weight :bold :color :green-500})]
         [(text-atom :impact-desc
            "Earlier intervention on Sep 15 would have prevented OTIF from dropping below 90%, saving an additional €8,200 and improving customer satisfaction by 4.2%."
            {:size 14 :color :slate-300})]]])]))
```

### Comparison Chart

```clojure
(defn comparison-chart
  "Horizontal bar chart comparing 3 scenarios across 6 metrics."
  [metrics]
  [:comparison-chart
   {:bg :slate-900
    :radius 8
    :border [:slate-700 1]
    :pad 24
    :dir :col
    :gap 16
    :size [:grow :fit]}

   [[(heading-2 :chart-title "📊 Scenario Comparison Analysis - Why Emergency Transfer Won")]

    [:metrics-list
     {:dir :col :gap 16}
     (mapv (fn [idx metric]
             (let [{:keys [label bars]} metric
                   row-id (keyword (str "metric-row-" idx))]
               [row-id
                {:dir :row :gap 12 :align {:y :center}}

                ;; Label column
                [[:label-col
                  {:size [[:% 0.16] :fit]}
                  [[(text-atom :metric-label label
                      {:size 14 :weight :semibold :color :slate-400})]]]

                 ;; Bars column
                 [:bars-col
                  {:size [[:% 0.84] :fit] :dir :row :gap 8}
                  (mapv-indexed
                    (fn [bar-idx bar]
                      (let [{:keys [text width winner color]} bar
                            bar-id (keyword (str "bar-" idx "-" bar-idx))
                            hovered? (contains? (:hovered-ids ctx) bar-id)
                            bg-color (cond
                                      winner [:gradient :to-r :green-600 :green-700]
                                      color color
                                      :else :slate-600)]
                        [bar-id
                         {:size [[:% width] [:fixed 32]]
                          :bg bg-color
                          :border (when winner [:green-400 2])
                          :radius 4
                          :align {:x :center :y :center}
                          :transform (when hovered? {:scale 1.05})
                          :data {:cursor :pointer}}
                         [[(text-atom :bar-text text
                             {:size 11 :weight :bold :color :white})]]]))
                    bars)]]]))
           (range)
           metrics)]]

    ;; Rationale box
    [:rationale
     {:bg [34 197 94 51]
      :border [:left :green-500 4]
      :radius 4
      :pad 16}
     [[(text-atom :rationale-title "Decision Rationale: "
         {:size 14 :weight :bold :color :green-500})]
      [(text-atom :rationale-text
         "Emergency Transfer achieved optimal balance: lowest risk (12%), fastest resolution (36h), and highest ROI (5.2x). The proximity advantage of Denmark warehouse (240km) provided decisive logistics efficiency."
         {:size 14 :color :slate-300})]]]]))
```

### Decision Room Workspace

```clojure
(defn decision-room-workspace
  "Collaborative workspace with participants, activity log, and actions."
  [state ctx]
  (let [{:keys [room-participants activity-log voting-status]} state]
    [:workspace
     {:dir :col :gap 20 :size [:grow :grow] :scroll :y}

     ;; Drop zone for participants
     [[:participants-zone
       {:border [:dashed :slate-600 2]
        :radius 8
        :pad 20
        :size [:grow :fit]
        :min-height 120
        :bg (when (:drag-over? ctx) [59 130 246 25])  ; blue-500 alpha
        :data {:drop-zone true
               :accepts :team-member
               :on-drop :add-participant}}
       [[(label-text :zone-title "👥 Decision Room Participants")]

        (if (empty? room-participants)
          [:empty-state
           {:align {:x :center :y :center} :pad [32 0]}
           [[(text-atom :empty-text "Drag team members here to add them to the decision room"
               {:size 14 :color :slate-500})]]]

          [:participants-list
           {:dir :row :gap 12 :wrap :wrap}
           (mapv (fn [member]
                   (let [{:keys [id name role color]} member
                         chip-id (keyword (str "participant-" (clojure.core/name id)))]
                     [chip-id
                      {:dir :row :gap 8 :pad 8 :bg :slate-700
                       :radius 4 :border [:blue-500 1] :align {:y :center}}
                      [[(avatar-circle (keyword (str "p-avatar-" (name id)))
                          (clojure.string/join (map first (clojure.string/split name #" ")))
                          color)]

                       [:member-details
                        {:dir :col :gap 2}
                        [[(text-atom :p-name name {:size 14 :weight :semibold})]
                         [(small-text :p-role role)]]]

                       [:remove-btn
                        {:bg :slate-600
                         :pad [4 8]
                         :radius 4
                         :size [:fit :fit]
                         :data {:on-click #((:remove-participant ctx) id)}}
                        [[(text-atom :remove-icon "×"
                            {:size 16 :weight :bold :color :white})]]]]]))
                 room-participants)])]]

      ;; Activity timeline
      [:activity-log
       {:bg :slate-800
        :radius 8
        :border [:slate-700 1]
        :pad 20
        :size [:grow [:fixed 320]]
        :scroll :y}
       [[(heading-3 :log-title "📝 Decision Activity Timeline")]

        [:log-entries
         {:dir :col :gap 12}
         (mapv-indexed
           (fn [idx entry]
             (activity-log-entry idx entry (= idx (dec (count activity-log)))))
           activity-log)]]]

      ;; Action buttons
      [:actions-panel
       {:bg :slate-800
        :radius 8
        :border [:slate-700 1]
        :pad 20
        :dir :col
        :gap 12}
       [[(heading-3 :actions-title "⚡ Collaborative Actions")]

        [:button-row
         {:dir :row :gap 12 :wrap :wrap}
         [[(button :btn-vote "🗳️ Start Vote"
             {:variant :primary :on-click :start-voting
              :disabled? (empty? room-participants)} ctx)]

          [(button :btn-approve "✓ Request Approvals"
             {:variant :warning :on-click :request-approvals
              :disabled? (empty? room-participants)} ctx)]

          [(button :btn-finalize "🎯 Finalize"
             {:variant :success :on-click :finalize-decision
              :disabled? (empty? room-participants)} ctx)]]]

        (when voting-status
          [:voting-status-box
           {:bg :slate-900 :radius 4 :pad 12}
           (if (= voting-status :in-progress)
             [[(text-atom :status-title "Voting in Progress..."
                 {:size 14 :weight :bold})]
              [(small-text :status-desc "Waiting for team members to cast their votes.")]]

             [[(text-atom :status-complete "✓ Complete"
                 {:size 14 :weight :bold :color :green-500})]
              [(small-text :status-result "Unanimous for Emergency Transfer")]])])]]]))
```

### Manufacturer/Retailer Panels

```clojure
(defn collab-panels
  "Side-by-side manufacturer and retailer views."
  []
  [:collab-grid
   {:dir :row :gap 20 :size [:grow :fit]}

   ;; Manufacturer panel
   [[:manufacturer-panel
     {:bg :slate-800
      :radius 8
      :border [:slate-700 1]
      :pad 20
      :size [:grow :fit]
      :dir :col
      :gap 20}
     [[:header
       {:dir :row :gap 12 :align {:y :center} :border [:bottom :slate-700 2] :pad [0 0 16 0]}
       [[:icon-box
         {:size [[:fixed 48] [:fixed 48]]
          :bg [:gradient :to-br :blue-600 :blue-700]
          :radius 8
          :align {:x :center :y :center}}
         [[(text-atom :icon "🏭" {:size 24})]]]

        [:title-col
         {:dir :col :gap 2}
         [[(heading-2 :title "Manufacturer View")]
          [(tiny-text :subtitle "Nordic Beverages Co.")]]]]]

      ;; Metrics grid
      [:metrics
       {:dir :row :gap 12 :wrap :wrap}
       [[(metric-card :m-capacity "Production Capacity" "87%" "")]
        [(metric-card :m-inventory "Available Inventory" "18.4K" "")]
        [(metric-card :m-leadtime "Lead Time" "36h" "")]
        [(metric-card :m-otif "OTIF Current" "96.5%" "" {:value-color :green-500})]]]

      ;; Constraints
      [:constraints
       {:dir :col :gap 8}
       [[(label-text :constraints-title "Supply Constraints")]

        [:constraint-1
         {:bg :slate-900 :pad 12 :radius 4}
         [[(text-atom :c1-icon "⚠️ " {:size 14 :weight :bold :color :yellow-500})]
          [(text-atom :c1-text "Denmark Warehouse: 13.2K cases available, 36h transfer time"
             {:size 14})]]]

        [:constraint-2
         {:bg :slate-900 :pad 12 :radius 4}
         [[(text-atom :c2-icon "ℹ️ " {:size 14 :weight :bold :color :blue-400})]
          [(text-atom :c2-text "Production: Next batch in 72h, capacity at 87%"
             {:size 14})]]]

        [:constraint-3
         {:bg :slate-900 :pad 12 :radius 4}
         [[(text-atom :c3-icon "✓ " {:size 14 :weight :bold :color :green-500})]
          [(text-atom :c3-text "Flexibility: Can expedite for priority customers"
             {:size 14})]]]]]]

    ;; Retailer panel
    [:retailer-panel
     {:bg :slate-800
      :radius 8
      :border [:slate-700 1]
      :pad 20
      :size [:grow :fit]
      :dir :col
      :gap 20}
     [[:header
       {:dir :row :gap 12 :align {:y :center} :border [:bottom :slate-700 2] :pad [0 0 16 0]}
       [[:icon-box
         {:size [[:fixed 48] [:fixed 48]]
          :bg [:gradient :to-br :green-600 :green-700]
          :radius 8
          :align {:x :center :y :center}}
         [[(text-atom :icon "🏪" {:size 24})]]]

        [:title-col
         {:dir :col :gap 2}
         [[(heading-2 :title "Retailer View")]
          [(tiny-text :subtitle "SuperMart Norway")]]]]]

      ;; Metrics grid
      [:metrics
       {:dir :row :gap 12 :wrap :wrap}
       [[(metric-card :r-stock "Current Stock" "2.1K" "" {:value-color :red-500})]
        [(metric-card :r-demand "Daily Demand" "1.8K" "")]
        [(metric-card :r-dos "Days of Supply" "1.2" "" {:value-color :red-500})]
        [(metric-card :r-promo "Promo Impact" "+35%" "" {:value-color :yellow-500})]]]

      ;; Demand insights
      [:insights
       {:dir :col :gap 8}
       [[(label-text :insights-title "Demand Insights")]

        [:insight-1
         {:bg :slate-900 :pad 12 :radius 4}
         [[(text-atom :i1-icon "🔥 " {:size 14 :weight :bold :color :red-500})]
          [(text-atom :i1-text "Urgent: Stockout risk in 1.2 days"
             {:size 14})]]]

        [:insight-2
         {:bg :slate-900 :pad 12 :radius 4}
         [[(text-atom :i2-icon "📈 " {:size 14 :weight :bold :color :yellow-500})]
          [(text-atom :i2-text "Trend: Demand +35% (sports event)"
             {:size 14})]]]

        [:insight-3
         {:bg :slate-900 :pad 12 :radius 4}
         [[(text-atom :i3-icon "💰 " {:size 14 :weight :bold :color :green-500})]
          [(text-atom :i3-text "Opportunity: €42K revenue if supply maintained"
             {:size 14})]]]]]]]]))
```

### Confidence Modal

```clojure
(defn confidence-modal
  "Full-screen modal explaining confidence score."
  [on-close ctx]
  [:modal-overlay
   {:float {:to :root :at [:left-top :left-top] :offset [0 0] :z 100}
    :size [:grow :grow]
    :bg [0 0 0 204]  ; rgba(0,0,0,0.8)
    :align {:x :center :y :center}
    :data {:on-click on-close}}  ; Click overlay to close

   [[:modal-content
     {:bg :slate-800
      :radius 12
      :border [:slate-700 2]
      :pad 32
      :size [[:fixed 672] [:% 0.9]]
      :scroll :y
      :data {:on-click :stop-propagation}}  ; Don't close when clicking content

     [;; Close button
      [:close-btn
       {:float {:to :parent :at [:right-top :right-top] :offset [-16 16] :z 1}
        :size [[:fixed 32] [:fixed 32]]
        :radius 16
        :bg :slate-700
        :align {:x :center :y :center}
        :data {:on-click on-close}}
       [[(text-atom :close-icon "×" {:size 20 :weight :bold})]]]

      ;; Header
      [:modal-header
       {:dir :col :gap 8 :size [:grow :fit] :pad [0 0 24 0]}
       [[(heading-1 :modal-title "Understanding Confidence Score")]
        [(small-text :modal-subtitle "How we calculate 87% confidence")]]]

      ;; Big confidence display
      [:confidence-display
       {:bg [:gradient :to-r :blue-600 :blue-700]
        :radius 8
        :pad 32
        :align {:x :center :y :center}
        :size [:grow :fit]}
       [[(huge-metric :confidence-value "87%" :white)]
        [(text-atom :confidence-label "Prediction Confidence"
           {:size 14 :color :white})]]]

      ;; Data sources
      [:data-sources
       {:dir :col :gap 12 :pad [24 0]}
       [[(heading-2 :sources-title "📊 Data Sources")]

        [:source-1
         {:dir :row :gap 12 :pad 12 :bg :slate-900 :radius 4 :align {:y :center}}
         [[:icon-circle
           {:size [[:fixed 40] [:fixed 40]]
            :radius 20
            :bg :slate-700
            :align {:x :center :y :center}}
           [[(text-atom :s1-num "47" {:size 14 :weight :bold :color :blue-400})]]]

          [:text-col
           {:dir :col :gap 2 :size [:grow :fit]}
           [[(text-atom :s1-title "Similar Historical Scenarios"
               {:size 14 :weight :bold})]
            [(small-text :s1-desc "Matched cases from past 18 months with 85%+ similarity")]]]]]

        [:source-2
         {:dir :row :gap 12 :pad 12 :bg :slate-900 :radius 4 :align {:y :center}}
         [[:icon-circle
           {:size [[:fixed 40] [:fixed 40]]
            :radius 20
            :bg :slate-700
            :align {:x :center :y :center}}
           [[(text-atom :s2-num "94%" {:size 14 :weight :bold :color :blue-400})]]]

          [:text-col
           {:dir :col :gap 2 :size [:grow :fit]}
           [[(text-atom :s2-title "Model Accuracy"
               {:size 14 :weight :bold})]
            [(small-text :s2-desc "Cross-validated on Nordic region supply chain events")]]]]]

        [:source-3
         {:dir :row :gap 12 :pad 12 :bg :slate-900 :radius 4 :align {:y :center}}
         [[:icon-circle
           {:size [[:fixed 40] [:fixed 40]]
            :radius 20
            :bg :slate-700
            :align {:x :center :y :center}}
           [[(text-atom :s3-num "12" {:size 14 :weight :bold :color :blue-400})]]]

          [:text-col
           {:dir :col :gap 2 :size [:grow :fit]}
           [[(text-atom :s3-title "Key Variables Analyzed"
               {:size 14 :weight :bold})]
            [(small-text :s3-desc "Demand volatility, inventory, lead times, regional factors")]]]]]]]

      ;; Confidence breakdown
      [:breakdown
       {:dir :col :gap 12 :pad [24 0]}
       [[(heading-2 :breakdown-title "🎯 Confidence Breakdown")]

        (mapv (fn [{:keys [label value color]}]
                (let [bar-id (keyword (str "breakdown-" (clojure.string/lower-case label)))]
                  [bar-id
                   {:dir :col :gap 4}
                   [[:bar-header
                     {:dir :row :align {:x :space-between :y :center}}
                     [[(text-atom :bar-label label {:size 14})]
                      [(text-atom :bar-value (str value "%") {:size 14 :weight :bold})]]]

                    [:bar-track
                     {:size [:grow [:fixed 8]]
                      :bg :slate-700
                      :radius 4}
                     [[:bar-fill
                       {:size [[:% (/ value 100.0)] :grow]
                        :bg [:gradient :to-r color (c/darken color 0.1)]
                        :radius 4}]]]]]))
              [{:label "Scenario Similarity" :value 92 :color :green-600}
               {:label "Success Rate" :value 94 :color :green-600}
               {:label "Data Quality" :value 89 :color :green-600}
               {:label "External Stability" :value 78 :color :yellow-600}])]]

      ;; Uncertainty factors
      [:uncertainty
       {:pad [24 0]}
       [[(heading-2 :unc-title "⚠️ Uncertainty Factors")]

        [:unc-box
         {:bg [239 68 68 51]  ; red-500 alpha
          :border [:left :red-500 4]
          :radius 4
          :pad 16}
         [[(text-atom :unc-item-1 "• Weather in transit route (±8% impact)"
             {:size 14 :color :slate-300})]
          [(text-atom :unc-item-2 "• Warehouse staff availability (±5%)"
             {:size 14 :color :slate-300})]
          [(text-atom :unc-item-3 "• Competing priority orders (±4%)"
             {:size 14 :color :slate-300})]]]]]

      ;; Interpretation
      [:interpretation
       {:bg [59 130 246 51]  ; blue-500 alpha
        :border [:left :blue-500 4]
        :radius 4
        :pad 16}
       [[(text-atom :interp-label "Interpretation: "
           {:size 14 :weight :bold :color :blue-400})]
        [(text-atom :interp-text
           "87% confidence indicates high reliability based on substantial historical evidence and proven patterns."
           {:size 14 :color :slate-300})]]]]]]])
```

---

## Templates

Layout structures that organize organisms and molecules.

### Three-Column Layout

```clojure
(defn three-column-layout
  "2/3 left content + 1/3 right sidebar."
  [left-content right-content]
  [:three-col-grid
   {:dir :row
    :gap 20
    :pad 24
    :size [:grow :grow]}

   [[:left-section
     {:size [[:% 0.66] :grow]
      :dir :col
      :gap 20}
     left-content]

    [:right-section
     {:size [[:% 0.33] :grow]
      :dir :col
      :gap 16}
     right-content]]])
```

### Four-Column with Sidebar

```clojure
(defn four-column-sidebar-layout
  "Sidebar (1/4) + Main content (3/4)."
  [sidebar-content main-content]
  [:four-col-layout
   {:dir :row
    :gap 20
    :pad 24
    :size [:grow :grow]}

   [[:sidebar
     {:size [[:% 0.25] :grow]
      :dir :col
      :scroll :y}
     sidebar-content]

    [:main-area
     {:size [[:% 0.75] :grow]
      :dir :col
      :gap 20
      :scroll :y}
     main-content]]])
```

### Centered Content

```clojure
(defn centered-content-layout
  "Max-width centered container."
  [content]
  [:centered-wrapper
   {:size [:grow :grow]
    :align {:x :center}
    :pad 24
    :scroll :y}

   [[:content-container
     {:size [[:fixed 896] :fit]  ; max-w-4xl = 896px
      :dir :col}
     content]]])
```

### Full-Width Content

```clojure
(defn full-width-layout
  "Simple full-width scrollable container."
  [content]
  [:full-width-wrapper
   {:size [:grow :grow]
    :pad 24
    :scroll :y
    :dir :col
    :gap 24}
   content])
```

---

## Page

Complete dashboard assembly with all tabs.

```clojure
(defn kyano-dashboard
  "Complete Kyano Decision Intelligence Dashboard."
  [state ctx]
  (let [{:keys [active-tab
                current-index
                is-playing?
                show-confidence-modal?
                room-participants
                activity-log
                voting-status
                demand
                leadtime
                capacity]} state

        current-data (nth historical-data current-index)

        ;; Calculate projected OTIF for what-if
        projected-otif (let [base 94.2
                            demand-impact (* demand -0.15)
                            leadtime-impact (* leadtime -0.25)
                            capacity-impact (* capacity 0.18)]
                        (max 75 (min 100 (+ base demand-impact
                                           leadtime-impact capacity-impact))))]

    [:dashboard
     {:size [:grow :grow]
      :bg [:gradient :to-br :slate-900 :slate-800]
      :color :slate-100
      :dir :col}

     [;; Header
      [(header-bar current-data)]

      ;; Timeline control
      [(timeline-control
         current-index
         (dec (count historical-data))
         is-playing?
         (:on-timeline-change ctx)
         (:on-play-timeline ctx)
         (:on-jump-to-decision ctx)
         ctx)]

      ;; Tab navigation
      [(tab-navigation active-tab (:on-tab-change ctx) ctx)]

      ;; Tab content
      [:tab-content
       {:size [:grow :grow]
        :scroll :y}

       [(case active-tab

          ;; OVERVIEW TAB
          :overview
          (three-column-layout
            ;; Left: Charts
            [[:chart-actual
              (otif-chart historical-data current-index false
                "OTIF Performance Timeline - Actual" ctx)]

             [:chart-optimized
              (otif-chart optimized-data current-index true
                "OTIF Performance Timeline - Optimized Scenario" ctx)]]

            ;; Right: Metrics
            [[(metric-card :orders-at-risk
                "Orders at Risk"
                (case (:status current-data)
                  (:critical) "47"
                  (:alert) "23"
                  "8")
                "Within next 48 hours")]

             [(metric-card :value-at-stake
                "Value at Stake"
                (case (:status current-data)
                  (:critical) "€31K"
                  (:alert) "€18K"
                  "€5K")
                "Potential revenue impact")]

             (when (>= current-index 9)
               [:decision-value
                (metric-card :decision-value-created
                  "Decision Value Created"
                  "€26,400"
                  "Emergency transfer ROI: 5.2x"
                  {:value-color :green-500})])])

          ;; DECISION HISTORY TAB
          :decision-history
          (full-width-layout
            [[:decision-header
              {:bg [:gradient :to-r :slate-800 :slate-900]
               :radius 8
               :border [:slate-700 1]
               :pad 20
               :dir :col
               :gap 16}
              [[(heading-2 :dec-title "Critical Decision: Norway Demand Spike")]
               [(small-text :dec-meta "September 17, 2024 | Decision made in 6.2 hours | AI-Assisted")]

               [:situation-box
                {:bg [239 68 68 51]
                 :border [:left :red-500 4]
                 :radius 4
                 :pad 16}
                [[(text-atom :sit-label "Situation: " {:size 14 :weight :bold :color :red-400})]
                 [(text-atom :sit-text
                    "Unexpected demand spike in Norway (+35%), stockout risk in 1.2 days, OTIF dropping from 94.5% to 86.8%"
                    {:size 14})]]]]]

             ;; 3 scenario cards
             [:scenarios-row
              {:dir :row :gap 16}
              (mapv #(scenario-card % ctx) decision-scenarios)]

             ;; Comparison chart
             [(comparison-chart comparison-metrics)]])

          ;; DECISION ROOM TAB
          :decision-room
          (four-column-sidebar-layout
            ;; Sidebar: Team members
            [[:team-header
              (label-text :team-title "Available Team Members")]

             [:team-list
              {:dir :col :gap 8}
              (mapv (fn [member]
                      (team-member-card member
                        (some #(= (:id %) (:id member)) room-participants)
                        ctx))
                    team-members)]

             [:hint-box
              {:bg [59 130 246 51]
               :radius 4
               :pad 12}
              [[(text-atom :hint-text
                  "💡 Drag team members into the decision room to start collaborating"
                  {:size 12 :color :slate-400})]]]]

            ;; Main: Workspace
            [[(decision-room-workspace state ctx)]])

          ;; WHAT-IF TAB
          :what-if
          (centered-content-layout
            [[:what-if-header
              {:bg :slate-800
               :radius 8
               :border [:slate-700 1]
               :pad 24
               :dir :col
               :gap 24}

              [;; Title with confidence badge
               [:title-row
                {:dir :row :align {:x :space-between :y :center}}
                [[(heading-2 :wif-title "What-If Scenario Builder")]

                 [:confidence-badge
                  {:bg :slate-700
                   :pad [6 12]
                   :radius 4
                   :size [:fit :fit]
                   :data {:on-click :show-confidence-modal}}
                  [[(text-atom :conf-text "Confidence: 87% | 47 scenarios"
                      {:size 12 :color :slate-300})]]]]]

               ;; Demand slider
               [:demand-control
                {:dir :col :gap 8}
                [[(text-atom :demand-label (str "Demand Change: "
                                                (if (> demand 0) "+" "") demand "%")
                    {:size 14 :weight :semibold})]

                 [(range-slider :demand-slider
                    {:min -30 :max 50 :step 5 :value demand
                     :on-change (:on-demand-change ctx)}
                    ctx)]

                 [:slider-labels
                  {:dir :row :align {:x :space-between}}
                  [[(tiny-text :demand-min "-30%")]
                   [(tiny-text :demand-base "Baseline")]
                   [(tiny-text :demand-max "+50%")]]]]]

               ;; Leadtime slider
               [:leadtime-control
                {:dir :col :gap 8}
                [[(text-atom :leadtime-label (str "Lead Time Change: "
                                                  (if (> leadtime 0) "+" "") leadtime " days")
                    {:size 14 :weight :semibold})]

                 [(range-slider :leadtime-slider
                    {:min -3 :max 5 :step 1 :value leadtime
                     :on-change (:on-leadtime-change ctx)}
                    ctx)]

                 [:slider-labels
                  {:dir :row :align {:x :space-between}}
                  [[(tiny-text :lt-min "-3 days")]
                   [(tiny-text :lt-base "Baseline")]
                   [(tiny-text :lt-max "+5 days")]]]]]

               ;; Capacity slider
               [:capacity-control
                {:dir :col :gap 8}
                [[(text-atom :capacity-label (str "Capacity Change: "
                                                  (if (> capacity 0) "+" "") capacity "%")
                    {:size 14 :weight :semibold})]

                 [(range-slider :capacity-slider
                    {:min -20 :max 30 :step 5 :value capacity
                     :on-change (:on-capacity-change ctx)}
                    ctx)]

                 [:slider-labels
                  {:dir :row :align {:x :space-between}}
                  [[(tiny-text :cap-min "-20%")]
                   [(tiny-text :cap-base "Baseline")]
                   [(tiny-text :cap-max "+30%")]]]]]

               ;; Result display
               [:result-row
                {:dir :row :gap 12}
                [[:projected-otif
                  {:bg [:gradient :to-r :blue-600 :blue-700]
                   :radius 8
                   :pad 32
                   :size [[:% 0.66] :fit]
                   :align {:x :center :y :center}
                   :dir :col
                   :gap 8}
                  [[(small-text :proj-label "Projected OTIF Performance")]
                   [(huge-metric :proj-value (format "%.1f%%" projected-otif) :white)]
                   [(text-atom :proj-delta
                      (let [delta (- projected-otif 94.2)]
                        (cond
                          (< (Math/abs delta) 0.1) "Baseline"
                          (> delta 0) (format "↗ +%.1f%% vs baseline" delta)
                          :else (format "↘ %.1f%% vs baseline" delta)))
                      {:size 14 :color :white})]]]

                 [:reset-col
                  {:size [[:% 0.33] :fit] :dir :col :gap 12}
                  [[(button :btn-reset "↺ Reset"
                      {:variant :secondary
                       :on-click (:on-reset-what-if ctx)}
                      ctx)]]]]]

               ;; AI Recommendation
               [:ai-rec
                {:bg :slate-900 :radius 4 :pad 20 :dir :col :gap 12}
                [[(text-atom :ai-title "🤖 AI Recommendation"
                    {:size 14 :weight :semibold :color :blue-400})]
                 [(text-atom :ai-text
                    (cond
                      (< projected-otif 90)
                      "Consider increasing capacity or reducing lead times to maintain service levels."

                      (> projected-otif 97)
                      "Scenario shows strong performance. Consider cost optimization opportunities."

                      :else
                      "Projected performance within acceptable range. Monitor closely for early warning signals.")
                    {:size 14 :color :slate-300})]]]]]])

          ;; RETAILER COLLAB TAB
          :retailer-collab
          (full-width-layout
            [[:collab-header
              {:bg :slate-800
               :radius 8
               :border [:slate-700 1]
               :pad 20
               :dir :col
               :gap 8}
              [[(heading-2 :collab-title "🤝 Joint Decision Room: Manufacturer ↔ Retailer")]
               [(small-text :collab-subtitle "Collaborative planning with shared visibility")]]]

             ;; Panels
             [(collab-panels)]

             ;; Shared insights
             [:shared-insights
              {:bg :slate-900
               :radius 8
               :border [:slate-700 1]
               :pad 24
               :dir :col
               :gap 20}
              [[(heading-2 :insights-title "🎯 Shared Decision Insights")]

               [:insights-list
                {:dir :col :gap 12}
                [[:insight-aligned
                  {:dir :row :gap 12 :pad 12 :bg :slate-800 :radius 4 :align {:y :flex-start}}
                  [[:icon-circle
                    {:size [[:fixed 32] [:fixed 32]]
                     :radius 16
                     :bg [:gradient :to-br :green-600 :green-700]
                     :align {:x :center :y :center}}
                    [[(text-atom :icon-aligned "✓" {:size 16 :color :white})]]]

                   [:text-col
                    {:dir :col :gap 4 :size [:grow :fit]}
                    [[(text-atom :aligned-title "Aligned: Emergency Transfer"
                        {:size 14 :weight :bold :color :green-500})]
                     [(small-text :aligned-desc
                        "Win-win: Manufacturer saves €26.4K, Retailer maintains availability. ROI: 5.2x")]]]]]

                 [:insight-forecast
                  {:dir :row :gap 12 :pad 12 :bg :slate-800 :radius 4 :align {:y :flex-start}}
                  [[:icon-circle
                    {:size [[:fixed 32] [:fixed 32]]
                     :radius 16
                     :bg [:gradient :to-br :blue-600 :blue-700]
                     :align {:x :center :y :center}}
                    [[(text-atom :icon-forecast "📊" {:size 16 :color :white})]]]

                   [:text-col
                    {:dir :col :gap 4 :size [:grow :fit]}
                    [[(text-atom :forecast-title "Joint Forecast: 94.2%"
                        {:size 14 :weight :bold})]
                     [(small-text :forecast-desc
                        "Combined data improves accuracy by 12% vs. standalone")]]]]]

                 [:insight-timeline
                  {:dir :row :gap 12 :pad 12 :bg :slate-800 :radius 4 :align {:y :flex-start}}
                  [[:icon-circle
                    {:size [[:fixed 32] [:fixed 32]]
                     :radius 16
                     :bg [:gradient :to-br :yellow-600 :yellow-700]
                     :align {:x :center :y :center}}
                    [[(text-atom :icon-timeline "⚡" {:size 16 :color :white})]]]

                   [:text-col
                    {:dir :col :gap 4 :size [:grow :fit]}
                    [[(text-atom :timeline-title "Timeline Agreement"
                        {:size 14 :weight :bold})]
                     [(small-text :timeline-desc
                        "Manufacturer: 36h delivery | Retailer: Promotional extension")]]]]]]]

               ;; Action buttons
               [:actions
                {:dir :row :gap 12 :wrap :wrap}
                [[(button :btn-both-approve "✓ Both Approve"
                    {:variant :success :on-click (:on-approve-joint ctx)}
                    ctx)]
                 [(button :btn-negotiate "💬 Negotiate"
                    {:variant :warning :on-click (:on-negotiate ctx)}
                    ctx)]
                 [(button :btn-contract "📄 Contract"
                    {:variant :secondary :on-click (:on-view-contract ctx)}
                    ctx)]]]]]]])

          ;; SIMILAR CASES TAB
          :similar-cases
          (full-width-layout
            [[:similar-header
              {:bg [:gradient :to-r :blue-600 :blue-700]
               :radius 8
               :border [:blue-500 1]
               :pad 20
               :dir :col
               :gap 8}
              [[(heading-2 :similar-title "AI-Powered Case Matching")]
               [(text-atom :similar-desc
                  "Found 47 similar situations from historical data. Top 3 matches shown below."
                  {:size 14 :color :white})]]]

             ;; Case list
             [:cases-list
              {:dir :col :gap 16}
              (mapv (fn [case-data]
                      (let [{:keys [id desc decision otif cost value similarity]} case-data
                            case-id (keyword (str "case-" (clojure.string/replace id #"[^a-z0-9]" "-")))]
                        [case-id
                         {:bg :slate-800
                          :radius 8
                          :border [:slate-700 1]
                          :pad 20
                          :dir :row
                          :gap 20
                          :align {:y :center}}

                         [[:case-id-col
                           {:size [[:% 0.25] :fit] :dir :col :gap 8}
                           [[(label-text :case-id-label "CASE ID")]
                            [(text-atom :case-id-text id {:size 16 :weight :bold})]
                            [(tiny-text :case-desc-text desc)]]]

                          [:case-decision-col
                           {:size [[:% 0.50] :fit] :dir :col :gap 8}
                           [[(label-text :decision-label "DECISION & OUTCOME")]
                            [(text-atom :decision-text decision {:size 14 :weight :semibold})]

                            [:metrics-row
                             {:dir :row :gap 16}
                             [[(text-atom :otif-metric (str "OTIF: " otif)
                                 {:size 12})]
                              [(text-atom :cost-metric (str "Cost: " cost)
                                 {:size 12})]
                              [(text-atom :value-metric (str "Value: " value)
                                 {:size 12 :color :green-500 :weight :bold})]]]]]

                          [:similarity-col
                           {:size [[:% 0.25] :fit] :align {:x :right} :dir :col :gap 4}
                           [[(label-text :similarity-label "SIMILARITY")]
                            [(text-atom :similarity-value similarity
                               {:size 36 :weight :extrabold :color :blue-500})]]]]]))
                    similar-cases)]

             ;; Learning pattern box
             [:learning-pattern
              {:bg [:gradient :to-r :blue-600 :blue-700]
               :radius 8
               :pad 20
               :align {:x :center}
               :dir :col
               :gap 8}
              [[(text-atom :pattern-title "Learning Pattern: "
                  {:size 14 :weight :bold :color :white})]
               [(text-atom :pattern-text
                  "Nordic region transfer operations show 6.2x average ROI across 47 cases"
                  {:size 14 :color :white})]
               [(text-atom :pattern-stats
                  "Success rate: 94% | Average recovery time: 52 hours | Median cost: €4,600"
                  {:size 14 :color :white})]]]]])]]

      ;; Modal overlay (conditional)
      (when show-confidence-modal?
        [(confidence-modal (:on-close-modal ctx) ctx)])]]))
```

---

## Context & State

### State Structure

The dashboard requires the following state:

```clojure
{;; Navigation
 :active-tab :overview  ; :overview | :decision-history | :decision-room | :what-if | :retailer-collab | :similar-cases

 ;; Timeline
 :current-index 12      ; Index into historical-data (0-12)
 :is-playing? false     ; Animation state

 ;; Decision Room
 :room-participants []  ; Vector of team member maps
 :activity-log [{:user "System" :time "2 mins ago" :action "..."}]
 :voting-status nil     ; nil | :in-progress | :complete
 :dragged-member nil    ; Currently dragged member or nil

 ;; What-If
 :demand 0              ; -30 to 50
 :leadtime 0            ; -3 to 5
 :capacity 0            ; -20 to 30

 ;; Modal
 :show-confidence-modal? false

 ;; Computed (from state)
 :projected-otif 94.2}  ; Calculated from what-if params
```

### Context Structure

Context provides dynamic values and callbacks:

```clojure
{;; Hover state (updated each frame)
 :hovered-ids #{:btn-play :sidebar}  ; Set of hovered element IDs

 ;; Chart hover
 :chart-hover-point {:x 150 :y 200 :data {...}}  ; nil or point data

 ;; Drag state
 :drag-over? false      ; Is drop zone being hovered during drag?

 ;; Callbacks
 :on-timeline-change (fn [new-idx] ...)
 :on-play-timeline (fn [] ...)
 :on-jump-to-decision (fn [] ...)  ; Sets index to 7
 :on-tab-change (fn [tab-id] ...)

 :add-participant (fn [member] ...)
 :remove-participant (fn [member-id] ...)
 :start-voting (fn [] ...)
 :request-approvals (fn [] ...)
 :finalize-decision (fn [] ...)

 :on-demand-change (fn [val] ...)
 :on-leadtime-change (fn [val] ...)
 :on-capacity-change (fn [val] ...)
 :on-reset-what-if (fn [] ...)

 :show-confidence-modal (fn [] ...)
 :on-close-modal (fn [] ...)

 :on-approve-joint (fn [] ...)
 :on-negotiate (fn [] ...)
 :on-view-contract (fn [] ...)}
```

---

## Interactivity Patterns

### Immediate-Mode Hover

Clay uses immediate-mode rendering with 1-frame hover delay:

```clojure
;; Frame N: Render with previous frame's hover state
(defn render-frame [state hovered-ids]
  (kyano-dashboard state {:hovered-ids hovered-ids}))

;; Frame N+1: Use new hover IDs from engine
(let [result (layout-engine {:tree (render-frame state prev-hover)
                             :pointer pointer-state})]
  (render-frame state (:hovered-ids result)))
```

### Button Hover Example

```clojure
(defn button [id label on-click ctx]
  (let [hovered? (contains? (:hovered-ids ctx) id)]
    [id
     {:bg (if hovered? :blue-700 :blue-600)  ; Change color on hover
      :data {:on-click on-click}}
     [...]]))
```

### Conditional Rendering

```clojure
;; Only show if condition true
(when (>= current-index 9)
  [:decision-value-card {...}])

;; Show different content based on state
(if (empty? participants)
  [:empty-state {...}]
  [:participant-list {...}])

;; Case dispatch for tabs
(case active-tab
  :overview (overview-content)
  :decision-room (decision-room-content)
  ...)
```

### Drag & Drop

```clojure
;; Draggable source
[id
 {:data {:draggable true
         :drag-data {:type :team-member :member member}}}
 ...]

;; Drop target
[id
 {:data {:drop-zone true
         :accepts :team-member
         :on-drop :add-participant}
  :bg (when (:drag-over? ctx) :blue-500)}  ; Visual feedback
 ...]
```

### Modal Overlay

```clojure
;; Full-screen overlay with click-to-close
[:modal-overlay
 {:float {:to :root :z 100}
  :size [:grow :grow]
  :bg [0 0 0 204]  ; Semi-transparent black
  :data {:on-click on-close}}  ; Click overlay to close

 [[:modal-content
   {:data {:on-click :stop-propagation}}  ; Prevent close when clicking content
   ...]]]
```

### Dynamic Lists

```clojure
;; Map over data with indexed IDs
[:participant-list
 {:dir :col :gap 8}
 (mapv (fn [member]
         (team-member-card member ...))
       team-members)]

;; Indexed IDs for similar items
(mapv-indexed
  (fn [idx entry]
    [(keyword (str "log-entry-" idx))
     {...}
     [...]])
  activity-log)
```

---

## Notes

### React → DSL Translation Summary

| React Concept | Clay DSL Equivalent |
|---------------|-------------------|
| `<div>` | `[:id {...} [...]]` |
| `className="..."` | Properties in `{...}` map |
| `useState` | State passed as parameter |
| `onClick={...}` | `:data {:on-click ...}` |
| `onMouseEnter` | Automatic via `:hovered-ids` |
| Conditional `&&` | `(when cond [...])` |
| Ternary `? :` | `(if cond [...] [...])` |
| `map()` | `(mapv ...)` |
| Props drilling | Function parameters |
| CSS-in-JS | Flat property map |
| Flexbox | `:dir :row/:col` + `:align` + `:gap` |
| Grid | Row with sized children |
| Absolute position | `:float {:to ...}` |
| z-index | `:float {:z ...}` |
| Scroll container | `:scroll :y/:x/:both` |
| Portal | `:float {:to :root}` |

### Canvas Chart

The OTIF chart uses a custom canvas widget:

```clojure
{:custom :line-chart
 :size [800 300]
 :data {:points data-vec
        :x-accessor :date
        :y-accessor :otif
        ...}
 :on-hover :update-chart-hover}
```

Platform-specific rendering code would handle the actual canvas drawing, grid lines, points, and tooltip positioning.

### Color System

All Tailwind colors are available via keywords:

```clojure
:bg :slate-800
:color :blue-500
:border [:green-600 2]
```

Gradients use vector notation:

```clojure
:bg [:gradient :to-br :slate-900 :slate-800]
:bg [:gradient :to-r :blue-600 :blue-700]
```

RGBA colors as vectors:

```clojure
:bg [0 0 0 204]        ; rgba(0, 0, 0, 0.8)
:bg [59 130 246 51]    ; blue-500 with alpha
```

---

**End of Kyano Dashboard DSL Specification**

This complete specification recreates the entire React dashboard using Clay's native data-driven semantics. Every visual element, layout, interaction, and state management pattern has been translated to pure Clojure data structures following atomic design principles.
