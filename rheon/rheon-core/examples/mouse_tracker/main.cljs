(ns mouse-tracker.main
  "Entry point for mouse tracker demo.

   Wire setup, state management, and initialization.
   Connects to real Clojure server via WebSocket.

   Architecture: Wire-refs ARE data - shared between client and server.
   We require the same wire definitions and instantiate them with r/wire."
  (:require [rheon.core :as r]
            [replicant.dom :as d]
            [ui.styles :as styles]
            [ui.charts :as charts]
            [mouse-tracker.state :as s]
            [mouse-tracker.handlers :as h]
            [mouse-tracker.views :as v]
            [mouse-tracker.wires :as wires]))

;; =============================================================================
;; Rheon Connection (WebSocket to real server)
;; =============================================================================

(defonce conn
  (r/connection {:transport :ws
                 :url "ws://localhost:8084"
                 :on-connect #(js/console.log "Connected to server!")
                 :on-disconnect #(js/console.log "Disconnected from server")}))

;; =============================================================================
;; Wire Instances (created from shared wire-refs with Malli specs)
;; =============================================================================

(defonce mouse-wire (r/wire conn wires/mouse-ref))
(defonce heartbeat-wire (r/wire conn wires/heartbeat-ref))
(defonce clock-wire (r/wire conn wires/clock-ref))
(defonce status-wire (r/wire conn wires/status-ref))
(defonce presence-wire (r/wire conn wires/presence-ref))

;; =============================================================================
;; Application State
;; =============================================================================

(defonce state (atom s/initial-state))

;; =============================================================================
;; Wire Operations (Effectful)
;; =============================================================================

(defonce last-emit (atom 0))
(def emit-throttle-ms 16)

(defn emit-mouse!
  "Emit mouse position to wire (throttled to ~60fps)."
  [x y]
  (let [now (js/Date.now)]
    (when (> (- now @last-emit) emit-throttle-ms)
      (reset! last-emit now)
      (try
        (r/emit! mouse-wire {:x x :y y :t now})
        true
        (catch :default e
          (js/console.error "emit-mouse! error:" e)
          false)))))

(defn send-clock-sync!
  "Send clock sync request via discrete wire."
  []
  (let [client-time (js/Date.now)]
    (r/send! clock-wire
             {:client-time client-time}
             {:on-reply (fn [reply]
                          (let [now (js/Date.now)
                                server-time (:server-time reply)
                                gap (:gap reply)
                                rtt (- now client-time)]
                            (swap! state h/handle
                                   {:type :clock-synced
                                    :client-time client-time
                                    :server-time server-time
                                    :gap gap
                                    :rtt rtt})))
              :on-error (fn [err]
                          (swap! state h/handle
                                 {:type :clock-sync-error
                                  :error (:error err)}))
              :timeout-ms 5000})
    (swap! state h/handle {:type :clock-sync-requested})))

(defn update-presence!
  "Update presence via signal wire."
  [name color]
  (let [client-id (str (random-uuid))
        presence-data {:users {client-id {:name name :color color :updated (js/Date.now)}}}]
    (r/signal! presence-wire presence-data)
    (swap! state h/handle {:type :presence-updated :name name :color color})))

(defn simulate-disconnect!
  "Close WebSocket to trigger reconnection."
  []
  (swap! state s/add-log :signal "Simulating disconnect...")
  (when-let [ws (:ws @(:state conn))]
    (.close ws)))

;; =============================================================================
;; Event Handler (Replicant data-driven dispatch)
;; =============================================================================

(defn event-handler
  "Handle all UI actions. Actions are data vectors.
   Receives Replicant context with js-event and node."
  [{:replicant/keys [js-event node]} actions]
  (doseq [[action-type & args] actions]
    (try
      (case action-type
        ;; Mouse tracking - extract coords from DOM event
        :mouse-moved
        (when (and js-event node)
          (let [rect (.getBoundingClientRect node)
                x (js/Math.round (- (.-clientX js-event) (.-left rect)))
                y (js/Math.round (- (.-clientY js-event) (.-top rect)))
                emitted? (emit-mouse! x y)]
            (swap! state h/handle {:type :mouse-moved :x x :y y :emitted? emitted?})))

        ;; Clock sync
        :request-clock-sync
        (send-clock-sync!)

        ;; Presence updates
        :update-presence
        (let [{:keys [name color]} (:presence @state)]
          (update-presence! name color))

        :presence-name-changed
        (when js-event
          (swap! state h/handle {:type :presence-name-changed
                                 :name (.. js-event -target -value)}))

        :presence-color-changed
        (when js-event
          (swap! state h/handle {:type :presence-color-changed
                                 :color (.. js-event -target -value)}))

        ;; Flow control
        :set-backpressure-mode
        (swap! state h/handle {:type :backpressure-mode-changed
                               :mode (first args)})

        ;; Connection testing
        :simulate-disconnect
        (simulate-disconnect!)

        ;; Default: pass to pure handler
        (js/console.warn "Unknown action:" action-type))
      (catch :default e
        (js/console.error "Event handler error:" e action-type)))))

;; =============================================================================
;; Rendering
;; =============================================================================

(defonce root-el (atom nil))

(defn render! []
  (when @root-el
    (d/render @root-el (v/ui @state))))

;; =============================================================================
;; Wire Listeners
;; =============================================================================

(defn setup-wires! []
  ;; Heartbeat stream with flow control metrics
  (let [heartbeat-handler
        (fn [heartbeat]
          (try
            ;; Track messages received
            (swap! state h/handle {:type :message-received})

            ;; Extract sequence number for gap detection
            (let [seq-num (:rheon/seq heartbeat)
                  last-seq (get-in @state [:flow-control :last-seq])]
              (when (and seq-num last-seq (> seq-num (inc last-seq)))
                (let [gap (- seq-num (inc last-seq))]
                  (swap! state h/handle {:type :gaps-detected :gap-count gap})
                  (js/console.warn "Sequence gap:" gap "messages missed")))
              (when seq-num
                (swap! state h/handle {:type :sequence-updated :seq-num seq-num})))

            ;; Track messages processed
            (swap! state h/handle {:type :message-processed})

            ;; Update heartbeat display
            (let [server-time (or (:server-time heartbeat) (get heartbeat "server-time"))
                  tick (or (:tick heartbeat) (get heartbeat "tick"))
                  uptime-sec (or (:uptime-sec heartbeat) (get heartbeat "uptime-sec"))]
              (swap! state h/handle {:type :heartbeat-received
                                     :server-time server-time
                                     :tick tick
                                     :uptime-sec uptime-sec}))
            (catch :default e
              (js/console.error "Heartbeat listener error:" e))))

        backpressure-mode (get-in @state [:flow-control :backpressure-mode] :none)
        bp-opts (case backpressure-mode
                  :sample {:backpressure :sample :interval-ms 16}
                  :buffer {:backpressure :buffer :size 10 :interval-ms 100}
                  :latest {:backpressure :latest}
                  nil)]

    (if bp-opts
      (r/listen heartbeat-wire heartbeat-handler bp-opts)
      (r/listen heartbeat-wire heartbeat-handler)))

  ;; Status signal
  (r/watch status-wire
           (fn [status]
             (try
               (swap! state h/handle {:type :status-changed :status status})
               (catch :default e
                 (js/console.error "Status watcher error:" e))))))

(defn setup-connection-watch! []
  ;; Check initial connection state (connection may already be established)
  (let [initial-state @(:state conn)]
    (when (:connected? initial-state)
      (swap! state h/handle {:type :connected :attempts 0})))

  ;; Watch for future connection state changes
  (add-watch (:state conn) :connection
             (fn [_ _ old-conn-state new-conn-state]
               (let [was-connected? (:connected? old-conn-state)
                     now-connected? (:connected? new-conn-state)
                     attempts (:reconnect-attempts new-conn-state 0)
                     buffer-size (count @(:pending-buffer conn))]
                 ;; Update connection health
                 (swap! state h/handle {:type :connection-health-updated
                                        :reconnect-attempts attempts
                                        :buffer-size buffer-size})

                 ;; Connection state changes
                 (cond
                   (and (not was-connected?) now-connected?)
                   (do
                     (swap! state h/handle {:type :connected :attempts attempts})
                     (when (pos? buffer-size)
                       (swap! state h/handle {:type :messages-flushed :count buffer-size})))

                   (and was-connected? (not now-connected?))
                   (swap! state h/handle {:type :disconnected})

                   (and (not now-connected?) (pos? attempts))
                   (swap! state h/handle {:type :reconnecting :attempt attempts}))))))

;; =============================================================================
;; Init
;; =============================================================================

(defn ^:export init! []
  (js/console.log "Rheon Mouse Tracker - WebSocket Client!")
  (js/console.log "Connecting to ws://localhost:8084...")

  (try
    ;; Register global event handler (Replicant data-driven dispatch)
    (d/set-dispatch! event-handler)
    (js/console.log "Event handler registered")

    ;; Inject styles
    (styles/inject!)
    (js/console.log "Styles injected")

    ;; Get root element
    (reset! root-el (.getElementById js/document "app"))
    (js/console.log "Root element:" @root-el)

    ;; Initial render
    (render!)
    (js/console.log "Initial render complete")

    ;; Watch state and re-render
    (add-watch state :render
               (fn [_ _ old-state new-state]
                 (when (not= old-state new-state)
                   (try
                     (render!)
                     (catch :default e
                       (js/console.error "Render error:" e))))))
    (js/console.log "State watch added")

    ;; Watch state for chart updates (separate from render)
    (add-watch state :chart-updates
               (fn [_ _ old-state new-state]
                 (when (not= (:mouse-history old-state)
                             (:mouse-history new-state))
                   (charts/update-speed-chart! (s/get-speed-data new-state)))))
    (js/console.log "Chart watch added")

    ;; Setup wire listeners
    (setup-wires!)
    (js/console.log "Wire listeners setup")

    ;; Setup connection watch
    (setup-connection-watch!)
    (js/console.log "Connection watch added")

    ;; Periodically update buffer size while disconnected
    (js/setInterval
     (fn []
       (when-not (:connected? @(:state conn))
         (swap! state h/handle {:type :connection-health-updated
                                :buffer-size (count @(:pending-buffer conn))})))
     500)

    (swap! state s/add-log :signal "UI initialized - connecting to server...")

    (catch :default e
      (js/console.error "Init error:" e))))
