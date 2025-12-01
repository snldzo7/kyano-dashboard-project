(ns kyano.ui.charts
  "Generic chart components - Sparkline, ProgressBar, FlowDiagram"
  (:require [kyano.ui.primitives :refer [classes]]
            [clojure.string :as str]))

;; -----------------------------------------------------------------------------
;; Sparkline (SVG mini line chart) - Matches React exactly
;; Uses viewBox 0 0 100 100 with preserveAspectRatio="none" to stretch
;; Uses polygon for fill area, polyline for stroke line
;; -----------------------------------------------------------------------------

(defn- data->points
  "Convert data to SVG points string (space-separated x,y pairs).
   Uses 100x100 viewBox with Y inverted and 10% padding top/bottom.
   React formula: y = 100 - ((v - min) / range) * 80 - 10"
  [data]
  (when (seq data)
    (let [min-val (apply min data)
          max-val (apply max data)
          range (- max-val min-val)
          range (if (zero? range) 1 range)
          n (count data)]
      (->> data
           (map-indexed
            (fn [i v]
              (let [x (* (/ i (max 1 (dec n))) 100)
                    ;; Match React: 100 - ((v - min) / range) * 80 - 10
                    y (- 100 (+ (* (/ (- v min-val) range) 80) 10))]
                (str x "," y))))
           (str/join " ")))))

(defn sparkline
  "SVG sparkline chart - Matches React implementation exactly

   Props:
   - :data    - Vector of numeric values
   - :color   - Stroke color (CSS color string, e.g. '#34d399')
   - :class   - Additional classes (applied to container)"
  [{:keys [data color class]
    :or {color "#34d399"}}]
  (when (seq data)
    (let [points (data->points data)
          gradient-id (str "sparkline-grad-" (hash color))
          min-val (apply min data)
          max-val (apply max data)
          range (- max-val min-val)
          range (if (zero? range) 1 range)
          last-val (last data)
          ;; Circle position matches React: x=100, y = 100 - ((v - min) / range) * 80 - 10
          circle-y (- 100 (+ (* (/ (- last-val min-val) range) 80) 10))]
      [:svg {:viewBox "0 0 100 100"
             :preserveAspectRatio "none"
             :class (classes "w-full h-full" class)}
       ;; Gradient definition
       [:defs
        [:linearGradient {:id gradient-id :x1 "0%" :y1 "0%" :x2 "0%" :y2 "100%"}
         [:stop {:offset "0%" :stop-color color :stop-opacity "0.3"}]
         [:stop {:offset "100%" :stop-color color :stop-opacity "0"}]]]

       ;; Polygon for gradient fill area (React: points="0,100 ${points} 100,100")
       [:polygon {:points (str "0,100 " points " 100,100")
                  :fill (str "url(#" gradient-id ")")}]

       ;; Polyline for the stroke line
       [:polyline {:points points
                   :fill "none"
                   :stroke color
                   :stroke-width 2
                   :stroke-linecap "round"
                   :stroke-linejoin "round"}]

       ;; End dot (circle at last point)
       [:circle {:cx 100
                 :cy circle-y
                 :r 3
                 :fill color}]])))

;; -----------------------------------------------------------------------------
;; ProgressBar
;; -----------------------------------------------------------------------------

(def progress-variants
  {:default "bg-slate-500"
   :primary "bg-blue-500"
   :success "bg-emerald-500"
   :warning "bg-amber-500"
   :danger  "bg-red-500"
   :info    "bg-cyan-500"})

(defn progress-bar
  "Progress bar component

   Props:
   - :value    - Current value
   - :max      - Maximum value (default 100)
   - :label    - Optional label text
   - :show-value - Boolean, show percentage
   - :variant  - :default :primary :success :warning :danger :info
   - :size     - :sm :md :lg
   - :class    - Additional classes"
  [{:keys [value max label show-value variant size class]
    :or {max 100 variant :primary size :md}}]
  (let [pct (min 100 (* 100 (/ value max)))
        height-class (case size
                       :sm "h-1"
                       :md "h-2"
                       :lg "h-3"
                       "h-2")]
    [:div {:class (classes "w-full" class)}
     (when (or label show-value)
       [:div {:class "flex justify-between text-sm mb-1"}
        (when label [:span {:class "text-slate-400"} label])
        (when show-value [:span {:class "text-slate-300"} (str (Math/round pct) "%")])])
     [:div {:class (classes "w-full bg-slate-700 rounded-full overflow-hidden" height-class)}
      [:div {:class (classes "h-full rounded-full transition-all duration-300" (progress-variants variant))
             :style {:width (str pct "%")}}]]]))

;; -----------------------------------------------------------------------------
;; SegmentedBar (multiple segments)
;; -----------------------------------------------------------------------------

(defn segmented-bar
  "Bar with multiple colored segments

   Props:
   - :segments - Vector of {:value :color :label}
   - :total    - Total value (optional, calculated from segments if nil)
   - :size     - :sm :md :lg
   - :class    - Additional classes"
  [{:keys [segments total size class]
    :or {size :md}}]
  (let [calc-total (or total (reduce + (map :value segments)))
        height-class (case size
                       :sm "h-2"
                       :md "h-3"
                       :lg "h-4"
                       "h-3")]
    [:div {:class (classes "w-full" class)}
     [:div {:class (classes "w-full bg-slate-700 rounded-full overflow-hidden flex" height-class)}
      (for [[idx {:keys [value color]}] (map-indexed vector segments)]
        (let [pct (* 100 (/ value calc-total))]
          ^{:key idx}
          [:div {:class "h-full first:rounded-l-full last:rounded-r-full"
                 :style {:width (str pct "%")
                         :background-color color}}]))]]))

;; -----------------------------------------------------------------------------
;; Gauge (radial progress)
;; -----------------------------------------------------------------------------

(defn gauge
  "Radial gauge/progress indicator

   Props:
   - :value   - Current value (0-100)
   - :size    - Diameter in pixels (default 80)
   - :stroke  - Stroke width (default 8)
   - :color   - Progress color
   - :label   - Center label
   - :class   - Additional classes"
  [{:keys [value size stroke color label class]
    :or {size 80 stroke 8 color "#10b981"}}]
  (let [radius (/ (- size stroke) 2)
        circumference (* 2 Math/PI radius)
        offset (* circumference (- 1 (/ (min 100 (max 0 value)) 100)))]
    [:div {:class (classes "relative inline-flex items-center justify-center" class)}
     [:svg {:width size :height size :class "-rotate-90"}
      ;; Background circle
      [:circle {:cx (/ size 2)
                :cy (/ size 2)
                :r radius
                :fill "none"
                :stroke "#334155"
                :stroke-width stroke}]
      ;; Progress circle
      [:circle {:cx (/ size 2)
                :cy (/ size 2)
                :r radius
                :fill "none"
                :stroke color
                :stroke-width stroke
                :stroke-linecap "round"
                :stroke-dasharray circumference
                :stroke-dashoffset offset
                :class "transition-all duration-500"}]]
     ;; Center label
     (when label
       [:div {:class "absolute inset-0 flex items-center justify-center"}
        [:span {:class "text-sm font-semibold text-white"} label]])]))

;; -----------------------------------------------------------------------------
;; MiniChart (bar chart variant)
;; -----------------------------------------------------------------------------

(defn mini-bars
  "Mini bar chart

   Props:
   - :data   - Vector of numeric values
   - :width  - Total width (default 64)
   - :height - Max height (default 24)
   - :color  - Bar color
   - :gap    - Gap between bars (default 2)
   - :class  - Additional classes"
  [{:keys [data width height color gap class]
    :or {width 64 height 24 color "#10b981" gap 2}}]
  (when (seq data)
    (let [max-val (apply max data)
          n (count data)
          bar-width (/ (- width (* (dec n) gap)) n)]
      [:svg {:width width :height height :class class}
       (for [[idx v] (map-indexed vector data)]
         (let [bar-height (* height (/ v max-val))
               x (* idx (+ bar-width gap))
               y (- height bar-height)]
           ^{:key idx}
           [:rect {:x x
                   :y y
                   :width bar-width
                   :height bar-height
                   :fill color
                   :rx 1}]))])))
