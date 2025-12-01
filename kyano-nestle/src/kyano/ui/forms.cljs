(ns kyano.ui.forms
  "Generic form components - Range slider, Input, Select
   All components are data-driven - handlers are action vectors, not functions."
  (:require [kyano.ui.primitives :refer [classes]]))

;; -----------------------------------------------------------------------------
;; Range Slider Component
;; Matches React implementation with custom styling
;; -----------------------------------------------------------------------------

(defn range-slider
  "Range slider with value display and optional baseline comparison
   Matches React styling exactly - uses accent-cyan-500 consistently

   Props:
   - :value        - Current value
   - :min          - Minimum value
   - :max          - Maximum value
   - :step         - Step increment
   - :label        - Label text
   - :baseline     - Optional baseline value for comparison
   - :format-fn    - Function to format display value (default: str)
   - :on-change    - Action vector to dispatch on change (value interpolated via :event/target.value)
   - :disabled?    - Whether slider is disabled
   - :class        - Additional classes"
  [{:keys [value min max step label baseline format-fn on-change disabled? class]
    :or {format-fn str step 1}}]
  (let [changed? (and baseline (not= value baseline))]
    ;; Container with bg-cyan-950 + border when changed (matches React exactly)
    [:div {:class (classes "p-4 rounded-lg transition-all"
                           (if changed? "bg-cyan-950 border border-cyan-800" "bg-slate-800")
                           class)}
     ;; Label and values row
     [:div {:class "flex items-center justify-between"}
      [:span {:class "text-sm text-slate-300 font-medium"} label]
      [:div {:class "flex items-center gap-2"}
       [:span {:class (classes "text-sm font-semibold"
                               (if changed? "text-white" "text-slate-400"))}
        (format-fn value)]
       (when (and baseline changed?)
         [:span {:class "text-xs text-slate-500"}
          (str "\u2190 baseline: " (format-fn baseline))])]]

     ;; Slider track with custom styling - accent-cyan-500 always
     [:div {:class "relative"}
      [:input
       {:type "range"
        :min min
        :max max
        :step step
        :value value
        :disabled disabled?
        :class (classes "w-full h-2 rounded-lg appearance-none cursor-pointer"
                        "bg-slate-700 accent-cyan-500"
                        "[&::-webkit-slider-thumb]:appearance-none"
                        "[&::-webkit-slider-thumb]:w-4"
                        "[&::-webkit-slider-thumb]:h-4"
                        "[&::-webkit-slider-thumb]:rounded-full"
                        "[&::-webkit-slider-thumb]:bg-white"
                        "[&::-webkit-slider-thumb]:shadow-lg"
                        "[&::-webkit-slider-thumb]:cursor-pointer"
                        (when disabled? "opacity-50 cursor-not-allowed"))
        :on {:change on-change}}]]]))

;; -----------------------------------------------------------------------------
;; Text Input Component
;; -----------------------------------------------------------------------------

(defn text-input
  "Styled text input - data-driven

   Props:
   - :value       - Current value
   - :placeholder - Placeholder text
   - :label       - Optional label
   - :on-change   - Action vector to dispatch on change (value via :event/target.value)
   - :disabled?   - Whether input is disabled
   - :class       - Additional classes"
  [{:keys [value placeholder label on-change disabled? class]}]
  [:div {:class (classes "space-y-1" class)}
   (when label
     [:label {:class "text-sm text-slate-400"} label])
   [:input
    {:type "text"
     :value (or value "")
     :placeholder placeholder
     :disabled disabled?
     :class (classes "w-full px-3 py-2 rounded-lg"
                     "bg-slate-800 border border-slate-600"
                     "text-white placeholder-slate-500"
                     "focus:outline-none focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500"
                     "transition-colors"
                     (when disabled? "opacity-50 cursor-not-allowed"))
     :on {:change on-change}}]])

;; -----------------------------------------------------------------------------
;; Button Component
;; -----------------------------------------------------------------------------

(def button-variants
  {:primary "bg-cyan-600 hover:bg-cyan-500 text-white"
   :secondary "bg-slate-700 hover:bg-slate-600 text-white"
   :success "bg-emerald-600 hover:bg-emerald-500 text-white"
   :danger "bg-red-600 hover:bg-red-500 text-white"
   :ghost "bg-transparent hover:bg-slate-800 text-slate-300"
   :outline "bg-transparent border border-slate-600 hover:border-slate-500 text-slate-300"})

(defn button
  "Styled button component - data-driven

   Props:
   - :variant   - :primary :secondary :success :danger :ghost :outline
   - :size      - :sm :md :lg
   - :disabled? - Whether button is disabled
   - :on-click  - Action vector to dispatch on click
   - :class     - Additional classes
   - :children  - Button content"
  [{:keys [variant size disabled? on-click class children]
    :or {variant :primary size :md}}]
  (let [size-class (case size
                     :sm "px-2 py-1 text-xs"
                     :md "px-4 py-2 text-sm"
                     :lg "px-6 py-3 text-base"
                     "px-4 py-2 text-sm")]
    [:button
     {:class (classes "rounded-lg font-medium transition-colors"
                      size-class
                      (get button-variants variant (:primary button-variants))
                      (when disabled? "opacity-50 cursor-not-allowed")
                      class)
      :disabled disabled?
      :on {:click on-click}}
     children]))

;; -----------------------------------------------------------------------------
;; Tab Button Component
;; -----------------------------------------------------------------------------

(defn tab-button
  "Tab button for category selection - data-driven
   Matches React: active = bg-slate-700, inactive = text-slate-400

   Props:
   - :label     - Tab label
   - :active?   - Whether tab is active
   - :on-click  - Action vector to dispatch on click
   - :class     - Additional classes"
  [{:keys [label active? on-click class]}]
  [:button
   {:class (classes "flex-1 px-4 py-2 rounded-md text-sm font-medium transition-all"
                    (if active?
                      "bg-slate-700 text-white"
                      "text-slate-400 hover:text-white")
                    class)
    :on {:click on-click}}
   label])

;; -----------------------------------------------------------------------------
;; Avatar/Participant Selector
;; -----------------------------------------------------------------------------

(defn participant-avatar
  "Clickable participant avatar - data-driven

   Props:
   - :initials  - 2-letter initials
   - :name      - Full name (for tooltip)
   - :color     - Background color class
   - :selected? - Whether participant is selected
   - :on-click  - Action vector to dispatch on click"
  [{:keys [initials name color selected? on-click]}]
  [:button
   {:class (classes "w-10 h-10 rounded-full flex items-center justify-center"
                    "text-sm font-semibold text-white transition-all"
                    color
                    (if selected?
                      "ring-2 ring-white ring-offset-2 ring-offset-slate-900 scale-110"
                      "opacity-60 hover:opacity-100"))
    :title name
    :on {:click on-click}}
   initials])
