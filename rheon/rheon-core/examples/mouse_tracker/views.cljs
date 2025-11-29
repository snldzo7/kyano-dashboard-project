(ns mouse-tracker.views
  "Page-level view components for mouse tracker demo.
   Composes UI components into feature cards using Tailwind CSS."
  (:require [ui.components :as c]
            [ui.charts :as charts]
            [mouse-tracker.state :as s]))

;; =============================================================================
;; Mouse Tracking Card
;; =============================================================================

(defn mouse-card
  "Mouse position tracking card with interactive area, speed display, and chart."
  [state dispatch!]
  (let [{:keys [x y]} (:mouse state)
        events-sent (:events-sent state)
        speed (s/current-speed state)]
    (c/card {:title "Mouse Position" :wire-type :stream}
      (c/mouse-area (fn [e]
                      (let [rect (.getBoundingClientRect (.-currentTarget e))
                            mx (js/Math.round (- (.-clientX e) (.-left rect)))
                            my (js/Math.round (- (.-clientY e) (.-top rect)))]
                        (dispatch! {:type :mouse-moved :x mx :y my}))))
      (c/coords-display x y)
      [:div {:class "flex gap-4 justify-between"}
       (c/stat "Events Sent" events-sent)
       (c/stat "Speed" (str (.toFixed (or speed 0) 0) " px/s"))]
      ;; Speed chart - init on mount, updates via atom watch in main.cljs
      [:div {:class "mt-4"}
       [:div {:class "text-sm text-white/60 mb-2"} "Speed History"]
       [:div {:id "speed-chart"
              :class "w-full"
              :style {:height "120px"}
              :replicant/on-mount
              (fn [{:replicant/keys [node]}]
                (charts/init-speed-chart! node))}]])))

;; =============================================================================
;; Clock Sync Card
;; =============================================================================

(defn clock-card
  "Clock synchronization status card."
  [{:keys [clock]} dispatch!]
  (let [{:keys [client-time server-time gap rtt]} clock]
    (c/card {:title "Clock Sync" :wire-type :discrete}
      (c/stat "Client Time" (if client-time (str client-time "ms") "--"))
      (c/stat "Server Time" (if server-time (str server-time "ms") "--"))
      (c/stat "Gap" (if gap (str gap "ms") "--"))
      (c/stat "RTT" (if rtt (str rtt "ms") "--"))
      (c/btn-container
        (c/button {:on-click #(dispatch! {:type :request-clock-sync})}
                  "Sync Clock")))))

;; =============================================================================
;; Server Status Card
;; =============================================================================

(defn status-card
  "Server connection status card."
  [{:keys [status connected?]}]
  (let [{:keys [state port]} status
        status-key (cond
                     connected? :connected
                     (= state :connecting) :connecting
                     :else :disconnected)]
    (c/card {:title "Server Status" :wire-type :signal}
      (c/stat "Connection" (c/status-display status-key (name status-key)))
      (c/stat "State" (if state (name state) "--"))
      (c/stat "Port" (or port "--")))))

;; =============================================================================
;; Heartbeat Card
;; =============================================================================

(defn heartbeat-card
  "Server heartbeat status card."
  [{:keys [heartbeat]}]
  (let [{:keys [server-time tick uptime-sec]} heartbeat]
    (c/card {:title "Heartbeat" :wire-type :signal}
      (c/stat "Server Time" (or server-time "--"))
      (c/stat "Tick" (or tick "--"))
      (c/stat "Uptime" (if uptime-sec (str uptime-sec "s") "--")))))

;; =============================================================================
;; Presence Card
;; =============================================================================

(defn presence-card
  "User presence configuration card."
  [{:keys [presence]} dispatch!]
  (let [{:keys [name color]} presence]
    (c/card {:title "Presence" :wire-type :signal}
      (c/input-group
        {:label "Name"
         :value name
         :on-change (fn [e]
                      (dispatch! {:type :presence-name-changed
                                  :name (.. e -target -value)}))})
      (c/input-group
        {:label "Color"
         :type "color"
         :value color
         :on-change (fn [e]
                      (dispatch! {:type :presence-color-changed
                                  :color (.. e -target -value)}))
         :preview (c/color-preview color)})
      (c/btn-container
        (c/button {:on-click #(dispatch! {:type :update-presence
                                          :name name
                                          :color color})}
                  "Update Presence")))))

;; =============================================================================
;; Connection Health Card
;; =============================================================================

(defn connection-health-card
  "Connection health metrics card."
  [{:keys [connection-health connected?]}]
  (let [{:keys [reconnect-attempts buffer-size buffer-max
                messages-flushed total-reconnects]} connection-health]
    (c/card {:title "Connection Health" :wire-type :health}
      (c/metric {:label "Buffer"
                 :value (str (or buffer-size 0) "/" (or buffer-max 1000))
                 :progress {:percent (if (and buffer-max (pos? buffer-max))
                                       (* 100 (/ (or buffer-size 0) buffer-max))
                                       0)
                            :class "buffer"}})
      (c/stat "Reconnect Attempts" (or reconnect-attempts 0))
      (c/stat "Total Reconnects" (or total-reconnects 0))
      (c/stat "Messages Flushed" (or messages-flushed 0))
      [:div {:class "mt-4"}
       [:div {:class "text-sm text-white/60 mb-2"} "Backoff Schedule"]
       (c/backoff-steps reconnect-attempts connected?)])))

;; =============================================================================
;; Flow Control Card
;; =============================================================================

(def backpressure-modes
  [{:id :none :label "None" :desc "No flow control"}
   {:id :drop :label "Drop" :desc "Drop excess messages"}
   {:id :buffer :label "Buffer" :desc "Buffer and replay"}
   {:id :sample :label "Sample" :desc "Sample at interval"}])

(defn flow-control-card
  "Flow control configuration and stats card."
  [{:keys [flow-control]} dispatch!]
  (let [{:keys [backpressure-mode messages-received messages-processed
                last-seq gaps-detected]} flow-control]
    (c/card {:title "Flow Control" :wire-type :flow}
      (c/mode-selector backpressure-modes
                       backpressure-mode
                       (fn [mode]
                         (dispatch! {:type :backpressure-mode-changed
                                     :mode mode})))
      (c/seq-display "Last Sequence" last-seq)
      (c/gap-warning gaps-detected)
      [:div {:class "grid grid-cols-2 gap-2"}
       (c/flow-stat (or messages-received 0) "Received")
       (c/flow-stat (or messages-processed 0) "Processed")])))

;; =============================================================================
;; Event Log Card
;; =============================================================================

(defn event-log-card
  "Event log display card."
  [{:keys [log]}]
  (c/card {:title "Event Log" :class "col-span-full"}
    (c/event-log log)))

;; =============================================================================
;; Root UI Component
;; =============================================================================

(defn ui
  "Root UI component. Composes all cards into page layout."
  [state dispatch!]
  [:div {:class "max-w-7xl mx-auto p-8"}
   [:h1 {:class "text-center mb-8 text-3xl font-bold bg-gradient-to-r from-wire-stream to-wire-signal bg-clip-text text-transparent"}
    "Rheon Mouse Tracker"]
   [:div {:class "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6"}
    ;; Row 1: Mouse + Clock + Status
    (mouse-card state dispatch!)
    (clock-card state dispatch!)
    (status-card state)

    ;; Row 2: Heartbeat + Presence + Connection Health
    (heartbeat-card state)
    (presence-card state dispatch!)
    (connection-health-card state)

    ;; Row 3: Flow Control + Event Log
    (flow-control-card state dispatch!)
    (event-log-card state)]])
