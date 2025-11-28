(ns mouse-tracker-ui
  "ClojureScript frontend for mouse tracker demo.

   Uses Rheon for wire communication - SAME API as server!
   Uses Replicant for UI, Garden for CSS."
  (:require [rheon.core :as r]
            [replicant.dom :as d]
            [garden.core :refer [css]]
            [garden.stylesheet :refer [at-keyframes]]))

;; =============================================================================
;; Rheon Connection and Wires
;; =============================================================================

(defonce conn (r/connection {:transport :ws
                             :url "ws://localhost:8084"}))

;; Create wires - SAME API as server!
;; Client → Server
(defonce mouse-wire (r/stream :mouse conn))           ;; Stream: client emits mouse position
(defonce presence-wire (r/signal :presence conn {:users {}}))  ;; Signal: client sets name/color

;; Server → Client
(defonce heartbeat-wire (r/stream :heartbeat conn))   ;; Stream: server emits heartbeats
(defonce status-wire (r/signal :status conn {:state :connecting}))  ;; Signal: server status

;; Bidirectional
(defonce clock-wire (r/discrete :clock conn))         ;; Discrete: clock sync request/response

;; =============================================================================
;; UI State (local to UI, not network state)
;; =============================================================================

(defonce state
  (atom {:mouse {:x 0 :y 0}
         :clock {:client-time nil :server-time nil :gap nil :rtt nil}
         :status {:state :connecting :port nil}
         :heartbeat {:server-time nil :tick nil :uptime-sec nil}
         :presence {:name "" :color "#00d9ff"}
         :connected? false
         :events-sent 0
         :log []
         ;; Phase 1: Connection Health metrics
         :connection-health {:reconnect-attempts 0
                             :last-reconnect-delay nil
                             :buffer-size 0
                             :buffer-max 1000
                             :messages-flushed 0
                             :total-reconnects 0}
         ;; Phase 2: Flow Control metrics
         :flow-control {:backpressure-mode :none  ;; :none, :sample, :buffer, :latest
                        :messages-received 0
                        :messages-processed 0
                        :messages-dropped 0
                        :last-seq nil
                        :gaps-detected 0
                        :sample-interval-ms 16}}))

;; =============================================================================
;; Garden CSS Styles
;; =============================================================================

(def styles
  (css
   (at-keyframes :pulse
     [:0% :100% {:opacity 1}]
     [:50% {:opacity 0.5}])

   [:* {:margin 0 :padding 0 :box-sizing :border-box}]

   [:body
    {:font-family "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif"
     :background "linear-gradient(135deg, #1a1a2e 0%, #16213e 100%)"
     :min-height "100vh"
     :color "#fff"}]

   [:.container
    {:max-width "1200px"
     :margin "0 auto"
     :padding "2rem"}]

   [:h1
    {:text-align :center
     :margin-bottom "2rem"
     :font-size "2.5rem"
     :background "linear-gradient(90deg, #00d9ff, #00ff88)"
     :-webkit-background-clip :text
     :-webkit-text-fill-color :transparent}]

   [:.grid
    {:display :grid
     :grid-template-columns "repeat(auto-fit, minmax(300px, 1fr))"
     :gap "1.5rem"}]

   [:.card
    {:background "rgba(255, 255, 255, 0.05)"
     :border-radius "12px"
     :padding "1.5rem"
     :border "1px solid rgba(255, 255, 255, 0.1)"}]

   [:.card-title
    {:font-size "1.2rem"
     :margin-bottom "1rem"
     :display :flex
     :align-items :center
     :gap "0.5rem"}]

   [:.badge
    {:font-size "0.7rem"
     :padding "0.2rem 0.5rem"
     :border-radius "4px"
     :text-transform :uppercase}]

   [:.badge-stream {:background "#00d9ff" :color "#000"}]
   [:.badge-discrete {:background "#ff6b6b" :color "#000"}]
   [:.badge-signal {:background "#00ff88" :color "#000"}]

   [:#mouse-area
    {:width "100%"
     :height "200px"
     :background "rgba(0, 217, 255, 0.1)"
     :border-radius "8px"
     :border "2px dashed rgba(0, 217, 255, 0.3)"
     :display :flex
     :align-items :center
     :justify-content :center
     :cursor :crosshair
     :position :relative
     :overflow :hidden}]

   [:.hint {:color "rgba(255, 255, 255, 0.5)"}]

   [:#mouse-coords
    {:font-family :monospace
     :font-size "1.5rem"
     :color "#00d9ff"
     :margin-top "1rem"
     :text-align :center}]

   [:.stat
    {:display :flex
     :justify-content :space-between
     :padding "0.5rem 0"
     :border-bottom "1px solid rgba(255, 255, 255, 0.1)"}]
   [:.stat:last-child {:border-bottom :none}]

   [:.stat-label {:color "rgba(255, 255, 255, 0.6)"}]
   [:.stat-value {:font-family :monospace}]

   [:.status-dot
    {:display :inline-block
     :width "10px"
     :height "10px"
     :border-radius "50%"
     :margin-right "0.5rem"}]

   [:.status-dot.connected {:background "#00ff88"}]
   [:.status-dot.disconnected {:background "#ff6b6b"}]
   [:.status-dot.connecting {:background "#ffaa00" :animation "pulse 1s infinite"}]

   [:button
    {:background "linear-gradient(90deg, #00d9ff, #00ff88)"
     :border :none
     :padding "0.75rem 1.5rem"
     :border-radius "8px"
     :font-weight :bold
     :cursor :pointer
     :color "#000"
     :transition "transform 0.2s"}]
   [:button:hover {:transform "scale(1.05)"}]
   [:button:disabled {:opacity 0.5 :cursor :not-allowed :transform :none}]

   [:input
    {:background "rgba(255, 255, 255, 0.1)"
     :border "1px solid rgba(255, 255, 255, 0.2)"
     :border-radius "6px"
     :padding "0.5rem 0.75rem"
     :color "#fff"
     :font-size "0.9rem"
     :width "100%"}]
   [:input:focus
    {:outline :none
     :border-color "#00d9ff"}]

   [:.input-group
    {:display :flex
     :gap "0.5rem"
     :align-items :center
     :margin-bottom "0.75rem"}]

   [:.input-label
    {:color "rgba(255, 255, 255, 0.6)"
     :font-size "0.85rem"
     :min-width "60px"}]

   [:.color-preview
    {:width "24px"
     :height "24px"
     :border-radius "4px"
     :border "1px solid rgba(255, 255, 255, 0.2)"}]

   [:#event-log
    {:height "150px"
     :overflow-y :auto
     :font-family :monospace
     :font-size "0.85rem"
     :background "rgba(0, 0, 0, 0.3)"
     :border-radius "8px"
     :padding "0.5rem"}]

   [:.log-entry
    {:padding "0.25rem 0"
     :border-bottom "1px solid rgba(255, 255, 255, 0.05)"}]

   [:.log-time {:color "rgba(255, 255, 255, 0.4)"}]
   [:.log-emit {:color "#00d9ff"}]
   [:.log-discrete {:color "#ff6b6b"}]
   [:.log-signal {:color "#00ff88"}]

   [:.full-width {:grid-column "1 / -1"}]

   [:.btn-container {:margin-top "1rem" :text-align :center}]

   ;; Phase 1: Connection Health styles
   [:.badge-health {:background "#ffaa00" :color "#000"}]

   [:.progress-bar
    {:height "8px"
     :background "rgba(255, 255, 255, 0.1)"
     :border-radius "4px"
     :overflow :hidden
     :margin-top "0.5rem"}]

   [:.progress-fill
    {:height "100%"
     :border-radius "4px"
     :transition "width 0.3s ease"}]

   [:.progress-fill.buffer {:background "linear-gradient(90deg, #ffaa00, #ff6b6b)"}]
   [:.progress-fill.backoff {:background "linear-gradient(90deg, #00d9ff, #00ff88)"}]

   [:.health-metric
    {:display :flex
     :flex-direction :column
     :padding "0.75rem 0"
     :border-bottom "1px solid rgba(255, 255, 255, 0.1)"}]
   [:.health-metric:last-child {:border-bottom :none}]

   [:.metric-header
    {:display :flex
     :justify-content :space-between
     :align-items :center}]

   [:.metric-label {:color "rgba(255, 255, 255, 0.6)" :font-size "0.9rem"}]
   [:.metric-value {:font-family :monospace :font-size "0.9rem"}]

   [:.backoff-steps
    {:display :flex
     :gap "4px"
     :margin-top "0.5rem"}]

   [:.backoff-step
    {:width "20px"
     :height "20px"
     :border-radius "4px"
     :display :flex
     :align-items :center
     :justify-content :center
     :font-size "0.6rem"
     :font-family :monospace}]

   [:.backoff-step.active {:background "#00d9ff" :color "#000"}]
   [:.backoff-step.pending {:background "rgba(255, 255, 255, 0.1)" :color "rgba(255, 255, 255, 0.4)"}]
   [:.backoff-step.done {:background "#00ff88" :color "#000"}]

   [:.test-btn
    {:background "linear-gradient(90deg, #ff6b6b, #ffaa00)"
     :font-size "0.8rem"
     :padding "0.5rem 1rem"}]

   ;; Phase 2: Flow Control styles
   [:.badge-flow {:background "#9b59b6" :color "#fff"}]

   [:.mode-selector
    {:display :flex
     :gap "0.5rem"
     :flex-wrap :wrap
     :margin-bottom "1rem"}]

   [:.mode-btn
    {:background "rgba(255, 255, 255, 0.1)"
     :border "1px solid rgba(255, 255, 255, 0.2)"
     :padding "0.4rem 0.8rem"
     :border-radius "6px"
     :font-size "0.8rem"
     :cursor :pointer
     :transition "all 0.2s"
     :color "rgba(255, 255, 255, 0.7)"}]

   [:.mode-btn:hover
    {:background "rgba(255, 255, 255, 0.15)"
     :color "#fff"}]

   [:.mode-btn.active
    {:background "linear-gradient(90deg, #9b59b6, #8e44ad)"
     :border-color "#9b59b6"
     :color "#fff"}]

   [:.seq-display
    {:font-family :monospace
     :font-size "1.2rem"
     :text-align :center
     :padding "0.75rem"
     :background "rgba(155, 89, 182, 0.2)"
     :border-radius "8px"
     :margin-bottom "1rem"}]

   [:.seq-gap-warning
    {:color "#ff6b6b"
     :font-size "0.8rem"
     :text-align :center
     :padding "0.5rem"
     :background "rgba(255, 107, 107, 0.1)"
     :border-radius "4px"
     :margin-top "0.5rem"}]

   [:.flow-stats
    {:display :grid
     :grid-template-columns "1fr 1fr"
     :gap "0.5rem"}]

   [:.flow-stat
    {:text-align :center
     :padding "0.5rem"
     :background "rgba(255, 255, 255, 0.05)"
     :border-radius "6px"}]

   [:.flow-stat-value
    {:font-family :monospace
     :font-size "1.1rem"
     :color "#9b59b6"}]

   [:.flow-stat-label
    {:font-size "0.7rem"
     :color "rgba(255, 255, 255, 0.5)"
     :text-transform :uppercase}]))

;; =============================================================================
;; Logging (with stable IDs for Replicant)
;; =============================================================================

(defonce log-counter (atom 0))

(defn log! [log-type msg]
  (let [id (swap! log-counter inc)]
    (swap! state update :log
           (fn [logs]
             (let [time (.toLocaleTimeString (js/Date.))
                   entry {:id id :time time :type log-type :msg msg}]
               (vec (take 50 (cons entry logs))))))))

;; =============================================================================
;; Rheon Wire Operations - USING THE RHEON API!
;; =============================================================================

;; Throttle state for mouse events
(defonce last-emit (atom 0))
(def emit-throttle-ms 16) ;; ~60fps

(defn emit-mouse! [x y]
  ;; Use Rheon API to emit mouse position (throttled)
  ;; NOTE: events-sent is updated in event-handler to batch with mouse update
  (let [now (js/Date.now)]
    (when (> (- now @last-emit) emit-throttle-ms)
      (reset! last-emit now)
      (try
        (r/emit! mouse-wire {:x x :y y :t now})
        true  ;; Return true if emitted
        (catch :default e
          (js/console.error "emit-mouse! error:" e)
          false)))))

(defn send-clock-sync! []
  (let [client-time (js/Date.now)]
    ;; Use Rheon API for request/response
    (r/send! clock-wire
             {:client-time client-time}
             {:on-reply (fn [reply]
                          (let [now (js/Date.now)
                                server-time (:server-time reply)
                                gap (:gap reply)
                                rtt (- now client-time)]
                            (swap! state assoc :clock
                                   {:client-time client-time
                                    :server-time server-time
                                    :gap gap
                                    :rtt rtt})
                            (log! :discrete (str "Clock sync: gap=" gap "ms, RTT=" rtt "ms"))))
              :on-error (fn [err]
                          (log! :discrete (str "Clock sync error: " (:error err))))
              :timeout-ms 5000}))
  (log! :discrete "Sent clock sync request"))

(defn update-presence! [name color]
  ;; Use Rheon signal! to update presence (client → server)
  (let [client-id (str (random-uuid))
        presence-data {:users {client-id {:name name :color color :updated (js/Date.now)}}}]
    (r/signal! presence-wire presence-data)
    (swap! state assoc :presence {:name name :color color})
    (log! :signal (str "Updated presence: " name " (" color ")"))))

;; =============================================================================
;; Event Dispatcher (Replicant data-driven events)
;; =============================================================================

(defn event-handler
  [{:replicant/keys [js-event node] :as ctx} actions]
  (doseq [action actions]
    (let [[action-type & _args] action]
      (try
        (case action-type
          :mouse-moved
          (when (and js-event node)
            (let [rect (.getBoundingClientRect node)
                  x (int (- (.-clientX js-event) (.-left rect)))
                  y (int (- (.-clientY js-event) (.-top rect)))]
              ;; Batch state update: mouse position AND events-sent in ONE swap!
              (when (emit-mouse! x y)
                (swap! state #(-> %
                                  (assoc :mouse {:x x :y y})
                                  (update :events-sent inc))))))

          :sync-clock
          (send-clock-sync!)

          :update-presence-name
          (when js-event
            (swap! state assoc-in [:presence :name] (.-value (.-target js-event))))

          :update-presence-color
          (when js-event
            (swap! state assoc-in [:presence :color] (.-value (.-target js-event))))

          :send-presence
          (let [{:keys [name color]} (:presence @state)]
            (update-presence! name color))

          :simulate-disconnect
          (do
            (log! :signal "Simulating disconnect...")
            ;; Close the WebSocket to trigger reconnection
            (when-let [ws (:ws @(:state conn))]
              (.close ws)))

          :set-backpressure-mode
          (let [mode (second action)]
            (swap! state assoc-in [:flow-control :backpressure-mode] mode)
            (log! :signal (str "Backpressure mode: " (name mode))))

          (js/console.warn "Unknown action:" action-type))
        (catch :default e
          (js/console.error "Event handler error:" e)
          (js/console.error "Context:" (pr-str ctx))
          (js/console.error "Action:" (pr-str action)))))))

;; =============================================================================
;; UI Components (Replicant - pure functions returning hiccup)
;; =============================================================================

(defn mouse-card [{:keys [x y]}]
  [:div.card
   [:h2.card-title "Mouse Tracker " [:span.badge.badge-stream "STREAM"]]
   [:div#mouse-area {:on {:mousemove [[:mouse-moved]]}}
    [:span.hint "Move mouse here"]]
   [:div#mouse-coords (str "X: " x " Y: " y)]])

(defn clock-card [{:keys [client-time server-time gap rtt]}]
  [:div.card
   [:h2.card-title "Clock Sync " [:span.badge.badge-discrete "DISCRETE"]]
   [:div.stat [:span.stat-label "Client Time"] [:span.stat-value (or client-time "--")]]
   [:div.stat [:span.stat-label "Server Time"] [:span.stat-value (or server-time "--")]]
   [:div.stat [:span.stat-label "Clock Gap"] [:span.stat-value (str (or gap "--") " ms")]]
   [:div.stat [:span.stat-label "Round Trip"] [:span.stat-value (str (or rtt "--") " ms")]]
   [:div.btn-container
    [:button {:on {:click [[:sync-clock]]}} "Sync Clock"]]])

(defn status-card [{:keys [connected? status events-sent]}]
  (let [state-val (or (some-> status :state name)
                      (some-> status (get "state") name)
                      "--")
        port-val (or (:port status)
                     (get status "port")
                     "--")]
    [:div.card
     [:h2.card-title "Status " [:span.badge.badge-signal "SIGNAL"]]
     [:div.stat
      [:span.stat-label "Connection"]
      [:span.stat-value
       [:span.status-dot {:class (if connected? "connected" "disconnected")}]
       (if connected? "Connected" "Disconnected")]]
     [:div.stat [:span.stat-label "Server State"] [:span.stat-value state-val]]
     [:div.stat [:span.stat-label "Server Port"] [:span.stat-value port-val]]
     [:div.stat [:span.stat-label "Events Sent"] [:span.stat-value events-sent]]]))

(defn heartbeat-card [{:keys [server-time tick uptime-sec]}]
  [:div.card
   [:h2.card-title "Heartbeat " [:span.badge.badge-stream "STREAM"]]
   [:div.stat [:span.stat-label "Server Time"] [:span.stat-value (or server-time "--")]]
   [:div.stat [:span.stat-label "Tick"] [:span.stat-value (or tick "--")]]
   [:div.stat [:span.stat-label "Uptime"] [:span.stat-value (str (or uptime-sec "--") " sec")]]])

(defn presence-card [{:keys [name color]}]
  [:div.card
   [:h2.card-title "Presence " [:span.badge.badge-signal "SIGNAL"]]
   [:div.input-group
    [:span.input-label "Name"]
    [:input {:type "text"
             :placeholder "Your name"
             :value (or name "")
             :on {:input [[:update-presence-name]]}}]]
   [:div.input-group
    [:span.input-label "Color"]
    [:input {:type "color"
             :value (or color "#00d9ff")
             :on {:input [[:update-presence-color]]}}]
    [:div.color-preview {:style {:background-color (or color "#00d9ff")}}]]
   [:div.btn-container
    [:button {:on {:click [[:send-presence]]}} "Update Presence"]]])

;; =============================================================================
;; Phase 1: Connection Health Card
;; =============================================================================

(defn backoff-delay
  "Calculate what the delay would be for a given attempt (for visualization)."
  [attempt]
  (let [base 1000
        cap 30000
        exp-delay (* base (js/Math.pow 2 attempt))]
    (min cap exp-delay)))

(defn connection-health-card [{:keys [reconnect-attempts last-reconnect-delay
                                      buffer-size buffer-max messages-flushed
                                      total-reconnects]} connected?]
  (let [buffer-pct (if (and buffer-max (> buffer-max 0))
                     (* 100 (/ (or buffer-size 0) buffer-max))
                     0)
        ;; Show backoff progression (attempts 0-5 = delays 1s, 2s, 4s, 8s, 16s, 30s)
        max-steps 6]
    [:div.card
     [:h2.card-title "Connection Health " [:span.badge.badge-health "PHASE 1"]]

     ;; Connection Status
     [:div.health-metric
      [:div.metric-header
       [:span.metric-label "Status"]
       [:span.metric-value
        [:span.status-dot {:class (if connected? "connected" "disconnected")}]
        (if connected? "Connected" "Reconnecting...")]]]

     ;; Reconnect Attempts with Backoff Visualization
     [:div.health-metric
      [:div.metric-header
       [:span.metric-label "Exponential Backoff"]
       [:span.metric-value (str "Attempt " (or reconnect-attempts 0))]]
      (into [:div.backoff-steps]
            (for [step (range max-steps)]
              (let [delay-sec (/ (backoff-delay step) 1000)
                    status (cond
                             (< step (or reconnect-attempts 0)) :done
                             (= step (or reconnect-attempts 0)) (if connected? :done :active)
                             :else :pending)]
                [:div.backoff-step {:replicant/key step
                                    :class (name status)
                                    :title (str delay-sec "s")}
                 (str delay-sec "s")])))]

     ;; Last Reconnect Delay
     (when last-reconnect-delay
       [:div.health-metric
        [:div.metric-header
         [:span.metric-label "Last Delay"]
         [:span.metric-value (str last-reconnect-delay " ms")]]])

     ;; Message Buffer
     [:div.health-metric
      [:div.metric-header
       [:span.metric-label "Message Buffer"]
       [:span.metric-value (str (or buffer-size 0) " / " (or buffer-max 1000))]]
      [:div.progress-bar
       [:div.progress-fill.buffer {:style {:width (str buffer-pct "%")}}]]]

     ;; Stats
     [:div.health-metric
      [:div.metric-header
       [:span.metric-label "Messages Flushed"]
       [:span.metric-value (or messages-flushed 0)]]
      [:div.metric-header {:style {:margin-top "0.5rem"}}
       [:span.metric-label "Total Reconnects"]
       [:span.metric-value (or total-reconnects 0)]]]

     ;; Test Button
     [:div.btn-container
      [:button.test-btn {:on {:click [[:simulate-disconnect]]}}
       "Simulate Disconnect"]]]))

;; =============================================================================
;; Phase 2: Flow Control Card
;; =============================================================================

(defn flow-control-card [{:keys [backpressure-mode messages-received messages-processed
                                  messages-dropped last-seq gaps-detected]}]
  (let [modes [{:id :none :label "None" :desc "All messages"}
               {:id :sample :label "Sample" :desc "60fps"}
               {:id :buffer :label "Buffer" :desc "Batch 10"}
               {:id :latest :label "Latest" :desc "Drop old"}]]
    [:div.card
     [:h2.card-title "Flow Control " [:span.badge.badge-flow "PHASE 2"]]

     ;; Backpressure Mode Selector
     [:div {:style {:margin-bottom "1rem"}}
      [:div {:style {:font-size "0.85rem" :color "rgba(255,255,255,0.6)" :margin-bottom "0.5rem"}}
       "Backpressure Strategy:"]
      (into [:div.mode-selector]
            (for [{:keys [id label desc]} modes]
              [:button.mode-btn {:replicant/key id
                                 :class (when (= backpressure-mode id) "active")
                                 :title desc
                                 :on {:click [[:set-backpressure-mode id]]}}
               label]))]

     ;; Sequence Number Display
     [:div.seq-display
      [:div {:style {:font-size "0.7rem" :color "rgba(255,255,255,0.5)" :margin-bottom "0.25rem"}}
       "LAST SEQUENCE #"]
      [:div {:style {:color "#9b59b6"}}
       (if last-seq (str "#" last-seq) "--")]]

     ;; Gap Warning
     (when (and gaps-detected (pos? gaps-detected))
       [:div.seq-gap-warning
        "Sequence gaps detected: " gaps-detected " message(s) missed"])

     ;; Flow Stats Grid
     [:div.flow-stats
      [:div.flow-stat
       [:div.flow-stat-value (or messages-received 0)]
       [:div.flow-stat-label "Received"]]
      [:div.flow-stat
       [:div.flow-stat-value (or messages-processed 0)]
       [:div.flow-stat-label "Processed"]]
      [:div.flow-stat
       [:div.flow-stat-value (or messages-dropped 0)]
       [:div.flow-stat-label "Dropped"]]
      [:div.flow-stat
       [:div.flow-stat-value
        (if (pos? (or messages-received 0))
          (str (js/Math.round (* 100 (/ (or messages-processed 0) messages-received))) "%")
          "--")]
       [:div.flow-stat-label "Throughput"]]]]))

(defn log-type-class [log-type]
  (case log-type
    :emit "log-emit"
    :discrete "log-discrete"
    :signal "log-signal"
    ""))

(defn event-log-card [logs]
  [:div.card.full-width
   [:h2.card-title "Event Log"]
   (into [:div#event-log]
         (map (fn [{:keys [id time type msg]}]
                [:div.log-entry {:replicant/key id}  ;; Stable ID, not index!
                 [:span.log-time (or time "")] " "
                 [:span {:class (log-type-class type)}
                  (str "[" (if (keyword? type) (name type) (str type)) "]")] " "
                 (or msg "")])
              logs))])

(defn ui [{:keys [mouse clock connected? status heartbeat presence events-sent log connection-health flow-control]}]
  [:div.container
   [:h1 "Rheon Wire Demo"]
   [:div.grid
    (mouse-card mouse)
    (clock-card clock)
    (status-card {:connected? connected? :status status :events-sent events-sent})
    (heartbeat-card heartbeat)
    (presence-card presence)
    (connection-health-card connection-health connected?)
    (flow-control-card flow-control)]
   (event-log-card log)])

;; =============================================================================
;; Rendering
;; =============================================================================

(defonce root-el (atom nil))

(defn render! []
  (when @root-el
    (d/render @root-el (ui @state))))

;; =============================================================================
;; Inject Styles
;; =============================================================================

(defn inject-styles! []
  (let [style-el (.createElement js/document "style")]
    (set! (.-type style-el) "text/css")
    (set! (.-innerHTML style-el) styles)
    (.appendChild (.-head js/document) style-el)))

;; =============================================================================
;; Setup Rheon Wire Listeners
;; =============================================================================

(defn setup-wires! []
  ;; NOTE: We do NOT listen to mouse-wire here!
  ;; The event-handler already updates state.mouse locally.
  ;; Listening to server echoes would cause a feedback loop:
  ;;   event-handler updates state → emit → server broadcasts → listener updates state again
  ;; This causes 2+ renders per mouse move = crash

  ;; Create a handler that tracks flow control metrics
  (let [heartbeat-handler
        (fn [heartbeat]
          (try
            ;; Track messages received (before backpressure)
            (swap! state update-in [:flow-control :messages-received] (fnil inc 0))

            ;; Extract sequence number for gap detection
            (let [seq-num (:rheon/seq heartbeat)
                  last-seq (get-in @state [:flow-control :last-seq])]
              ;; Check for sequence gaps
              (when (and seq-num last-seq (> seq-num (inc last-seq)))
                (let [gap (- seq-num (inc last-seq))]
                  (swap! state update-in [:flow-control :gaps-detected] (fnil + 0) gap)
                  (js/console.warn "Sequence gap:" gap "messages missed")))
              (when seq-num
                (swap! state assoc-in [:flow-control :last-seq] seq-num)))

            ;; Track messages processed (after backpressure)
            (swap! state update-in [:flow-control :messages-processed] (fnil inc 0))

            ;; Update heartbeat display
            (let [server-time (or (:server-time heartbeat)
                                  (get heartbeat "server-time"))
                  tick (or (:tick heartbeat)
                           (get heartbeat "tick"))
                  uptime-sec (or (:uptime-sec heartbeat)
                                 (get heartbeat "uptime-sec"))]
              (swap! state assoc :heartbeat
                     {:server-time server-time
                      :tick tick
                      :uptime-sec uptime-sec}))
            (catch :default e
              (js/console.error "Heartbeat listener error:" e))))

        ;; Get current backpressure mode
        backpressure-mode (get-in @state [:flow-control :backpressure-mode] :none)

        ;; Build backpressure options based on mode
        bp-opts (case backpressure-mode
                  :sample {:backpressure :sample :interval-ms 16}
                  :buffer {:backpressure :buffer :size 10 :interval-ms 100}
                  :latest {:backpressure :latest}
                  nil)]

    ;; Listen to heartbeat stream with optional backpressure
    (if bp-opts
      (r/listen heartbeat-wire heartbeat-handler bp-opts)
      (r/listen heartbeat-wire heartbeat-handler)))

  ;; Watch status signal - Rheon calls us immediately with current value
  ;; and again whenever it changes
  (r/watch status-wire
           (fn [status]
             (try
               (swap! state assoc :status status)
               (log! :signal (str "Status: " (pr-str status)))
               (catch :default e
                 (js/console.error "Status watcher error:" e))))))

;; =============================================================================
;; Init
;; =============================================================================

(defn ^:export init! []
  (js/console.log "Rheon Mouse Tracker UI - Using Rheon CLJS client!")

  (try
    ;; Inject Garden CSS styles
    (inject-styles!)
    (js/console.log "Styles injected")

    ;; Get root element
    (reset! root-el (.getElementById js/document "app"))
    (js/console.log "Root element:" @root-el)

    ;; Register Replicant event dispatcher
    (d/set-dispatch! event-handler)
    (js/console.log "Event dispatcher registered")

    ;; Initial render
    (render!)
    (js/console.log "Initial render complete")

    ;; Watch state and re-render on changes
    (add-watch state :render
               (fn [_ _ old-state new-state]
                 (when (not= old-state new-state)
                   (try
                     (render!)
                     (catch :default e
                       (js/console.error "Render error:" e))))))
    (js/console.log "State watch added")

    ;; Setup Rheon wire listeners
    (setup-wires!)
    (js/console.log "Wires setup")

    ;; Track connection status and health metrics from Rheon
    (add-watch (:state conn) :connection
               (fn [_ _ old-conn-state new-conn-state]
                 (let [was-connected? (:connected? old-conn-state)
                       now-connected? (:connected? new-conn-state)
                       attempts (:reconnect-attempts new-conn-state 0)
                       buffer-size (count @(:pending-buffer conn))]
                   ;; Update connected state
                   (swap! state assoc :connected? now-connected?)

                   ;; Update connection health metrics
                   (swap! state update :connection-health
                          (fn [health]
                            (-> health
                                (assoc :reconnect-attempts attempts)
                                (assoc :buffer-size buffer-size)
                                ;; Track total reconnects
                                (cond-> (and (not was-connected?) now-connected? (> attempts 0))
                                  (update :total-reconnects inc))
                                ;; Track messages flushed (buffer was non-zero, now connected)
                                (cond-> (and (not was-connected?) now-connected?)
                                  (update :messages-flushed + buffer-size)))))

                   ;; Log connection events
                   (cond
                     (and (not was-connected?) now-connected?)
                     (log! :signal (str "Reconnected after " attempts " attempt(s)"))

                     (and was-connected? (not now-connected?))
                     (log! :signal "Disconnected - will reconnect with exponential backoff")

                     (and (not now-connected?) (> attempts 0))
                     (log! :signal (str "Reconnect attempt " attempts " - backoff delay active"))))))
    (js/console.log "Connection watch added")

    ;; Periodically update buffer size while disconnected
    (js/setInterval
     (fn []
       (when-not (:connected? @(:state conn))
         (swap! state assoc-in [:connection-health :buffer-size]
                (count @(:pending-buffer conn)))))
     500)

    (log! :signal "UI initialized with Rheon!")

    (catch :default e
      (js/console.error "Init error:" e))))
