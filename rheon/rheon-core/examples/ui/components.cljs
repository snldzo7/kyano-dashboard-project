(ns ui.components
  "UI components using Tailwind CSS.
   Pure functions returning Hiccup with Tailwind utility classes.")

;; =============================================================================
;; Badges - Wire type indicators
;; =============================================================================

(def wire-colors
  {:stream   "bg-wire-stream text-black"
   :discrete "bg-wire-discrete text-black"
   :signal   "bg-wire-signal text-black"
   :health   "bg-wire-health text-black"
   :flow     "bg-wire-flow text-white"})

(defn badge
  "Wire type badge."
  [wire-type label]
  [:span {:class (str "text-xs px-2 py-0.5 rounded-full uppercase font-bold "
                      (get wire-colors wire-type "bg-gray-500 text-white"))}
   label])

;; =============================================================================
;; Status Indicators
;; =============================================================================

(defn status-dot
  "Connection status indicator."
  [status]
  (let [color (case status
                :connected "bg-green-500"
                :connecting "bg-yellow-500 animate-pulse"
                :disconnected "bg-red-500"
                "bg-gray-500")]
    [:span {:class (str "inline-block w-2.5 h-2.5 rounded-full mr-2 " color)}]))

(defn status-display
  "Status dot with label."
  [status label]
  [:span.flex.items-center
   (status-dot status)
   label])

;; =============================================================================
;; Stats / Metrics
;; =============================================================================

(defn stat
  "Label + value pair for displaying metrics."
  [label value]
  [:div {:class "flex justify-between py-2 border-b border-white/10 last:border-b-0"}
   [:span {:class "text-white/60"} label]
   [:span {:class "font-mono"} value]])

(defn metric
  "Health metric with optional progress bar."
  [{:keys [label value progress]}]
  [:div {:class "flex flex-col py-3 border-b border-white/10 last:border-b-0"}
   [:div {:class "flex justify-between items-center"}
    [:span {:class "text-white/60 text-sm"} label]
    [:span {:class "font-mono text-sm"} value]]
   (when progress
     [:div {:class "h-2 bg-white/10 rounded-sm overflow-hidden mt-2"}
      [:div {:class (str "h-full rounded-sm transition-all duration-200 "
                         (if (= (:class progress) "backoff")
                           "bg-gradient-to-r from-wire-stream to-wire-signal"
                           "bg-gradient-to-r from-wire-health to-wire-discrete"))
             :style {:width (str (:percent progress 0) "%")}}]])])

(defn flow-stat
  "Flow control stat box."
  [value label]
  [:div {:class "text-center p-2 bg-white/5 rounded-lg"}
   [:div {:class "font-mono text-lg text-wire-flow"} value]
   [:div {:class "text-xs text-white/40 uppercase"} label]])

;; =============================================================================
;; Progress Bars
;; =============================================================================

(defn progress-bar
  "Buffer/progress visualization."
  [value max-val & {:keys [bar-class] :or {bar-class "buffer"}}]
  (let [pct (if (and max-val (pos? max-val))
              (min 100 (* 100 (/ (or value 0) max-val)))
              0)]
    [:div {:class "h-2 bg-white/10 rounded-sm overflow-hidden mt-2"}
     [:div {:class (str "h-full rounded-sm transition-all duration-200 "
                        (if (= bar-class "backoff")
                          "bg-gradient-to-r from-wire-stream to-wire-signal"
                          "bg-gradient-to-r from-wire-health to-wire-discrete"))
            :style {:width (str pct "%")}}]]))

;; =============================================================================
;; Backoff Steps
;; =============================================================================

(defn backoff-delay
  "Calculate delay for a given attempt."
  [attempt]
  (let [base 1000
        cap 30000
        exp-delay (* base (js/Math.pow 2 attempt))]
    (min cap exp-delay)))

(defn backoff-steps
  "Exponential backoff visualization."
  [current-attempt connected? & {:keys [max-steps] :or {max-steps 6}}]
  (into [:div.flex.gap-1.mt-2]
        (for [step (range max-steps)]
          (let [delay-sec (/ (backoff-delay step) 1000)
                status (cond
                         (< step (or current-attempt 0)) :done
                         (= step (or current-attempt 0)) (if connected? :done :active)
                         :else :pending)
                color (case status
                        :done "bg-wire-signal text-black"
                        :active "bg-wire-stream text-black"
                        :pending "bg-white/10 text-white/40")]
            [:div {:replicant/key step
                   :class (str "w-5 h-5 rounded flex items-center justify-center text-[0.6rem] font-mono " color)
                   :title (str delay-sec "s")}
             (str delay-sec "s")]))))

;; =============================================================================
;; Cards
;; =============================================================================

(defn card
  "Card container with optional title and wire type badge."
  [{:keys [title wire-type class]} & children]
  (into [:div {:class (str "bg-white/5 rounded-xl p-6 border border-white/10 hover:border-white/20 transition " class)}
         (when title
           [:h2.text-lg.font-semibold.mb-4.flex.items-center.gap-2
            title " "
            (when wire-type (badge wire-type (name wire-type)))])]
        children))

;; =============================================================================
;; Forms
;; =============================================================================

(defn input-group
  "Label + input with optional preview.
   action: Replicant action vector, e.g. [[:update-name]]"
  [{:keys [label type value action preview]}]
  [:div {:class "flex gap-2 items-center mb-3"}
   [:span {:class "text-white/60 text-sm min-w-[60px]"} label]
   [:input {:class "flex-1 bg-white/5 border border-white/10 rounded-lg px-3 py-2 text-white text-sm font-sans focus:outline-none focus:border-wire-stream focus:ring-2 focus:ring-wire-stream/30"
            :type (or type "text")
            :value (or value "")
            :on {:input action}}]
   (when preview preview)])

(defn color-preview
  "Color swatch preview."
  [color]
  [:div {:class "w-6 h-6 rounded border border-white/10"
         :style {:background-color (or color "#00d9ff")}}])

(defn button
  "Standard button.
   action: Replicant action vector, e.g. [[:submit]]"
  [{:keys [action class disabled]} label]
  [:button {:class (str "bg-gradient-to-r from-wire-stream to-wire-signal text-black font-bold px-6 py-3 rounded-lg cursor-pointer transition hover:scale-105 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:scale-100 " class)
            :disabled disabled
            :on {:click action}}
   label])

(defn btn-container
  "Centered button container."
  [& children]
  (into [:div.mt-4.text-center] children))

;; =============================================================================
;; Mode Selector
;; =============================================================================

(defn mode-selector
  "Multi-choice button group for selecting modes.
   action-type: keyword like :set-mode, will dispatch [[:set-mode id]]"
  [modes current action-type]
  (into [:div.flex.gap-2.flex-wrap.mb-4]
        (for [{:keys [id label desc]} modes]
          [:button {:replicant/key id
                    :class (str "px-3 py-1 rounded-lg text-sm font-sans cursor-pointer transition "
                                (if (= current id)
                                  "bg-gradient-to-r from-wire-flow to-purple-600 border-wire-flow text-white"
                                  "bg-white/10 border border-white/20 text-white/60 hover:bg-white/15 hover:text-white"))
                    :title desc
                    :on {:click [[action-type id]]}}
           label])))

;; =============================================================================
;; Sequence Display
;; =============================================================================

(defn seq-display
  "Monospace sequence number display."
  [label value]
  [:div {:class "font-mono text-lg text-center p-3 bg-purple-500/20 rounded-lg mb-4"}
   [:div {:class "text-xs text-white/50 mb-1"} label]
   [:div {:class "text-wire-flow"} (if value (str "#" value) "--")]])

(defn gap-warning
  "Sequence gap warning message."
  [gaps-count]
  (when (and gaps-count (pos? gaps-count))
    [:div {:class "text-wire-discrete text-sm text-center p-2 bg-red-500/10 rounded mt-2"}
     "Sequence gaps detected: " gaps-count " message(s) missed"]))

;; =============================================================================
;; Mouse Area
;; =============================================================================

(defn mouse-area
  "Interactive mouse tracking area.
   action: Replicant action vector, e.g. [[:mouse-moved]]"
  [action]
  [:div {:class "w-full h-[200px] bg-wire-stream/10 rounded-lg border-2 border-dashed border-wire-stream/30 flex items-center justify-center cursor-crosshair relative overflow-hidden transition hover:border-wire-stream/50 hover:bg-wire-stream/15 touch-none select-none"
         :on {:mousemove action}}
   [:span {:class "text-white/40"} "Move mouse here"]])

(defn coords-display
  "Mouse coordinates display."
  [x y]
  [:div.font-mono.text-xl.text-wire-stream.mt-4.text-center.tabular-nums
   (str "X: " x " Y: " y)])

;; =============================================================================
;; Event Log
;; =============================================================================

(defn log-type-class
  "Get text color for log entry type."
  [log-type]
  (case log-type
    :emit "text-wire-stream"
    :discrete "text-wire-discrete"
    :signal "text-wire-signal"
    ""))

(defn log-entry
  "Single log entry."
  [{:keys [id time type msg]}]
  [:div {:replicant/key id
         :class "py-1 border-b border-white/5"}
   [:span {:class "text-white/40"} (or time "")] " "
   [:span {:class (log-type-class type)}
    (str "[" (if (keyword? type) (name type) (str type)) "]")] " "
   (or msg "")])

(defn event-log
  "Scrolling event log."
  [entries]
  (into [:div {:class "h-[150px] overflow-y-auto font-mono text-sm bg-black/30 rounded-lg p-2"}]
        (map log-entry entries)))
