(ns mouse-tracker-client
  "Example: Mouse Tracker Client using Rheon Sente Gauge

   This demonstrates how to build a ClojureScript client that
   connects to the Rheon mouse tracker server.

   Key features:
   - Connect to Rheon Sente server
   - Send mouse coordinates with request/reply pattern
   - Receive clock synchronization data from server

   This replaces direct Sente usage with Rheon's unified API."
  (:require [rheon.protocols :as p]
            [rheon.gauge.sente-client :as sente-client]
            [applied-science.js-interop :as j]))

;; =============================================================================
;; Application State
;; =============================================================================

(defonce !app-state
  (atom {:coords {:x 0 :y 0}
         :clock-gap nil
         :server-time nil
         :connected? false}))

(defonce !connection (atom nil))

;; =============================================================================
;; Connection Management
;; =============================================================================

(defn connect!
  "Connect to the Rheon mouse server.

   Options:
   - :host - Server host (default: localhost:9092)
   - :path - WebSocket path (default: /chsk)"
  ([] (connect! {}))
  ([opts]
   (when @!connection
     (p/gauge-close! sente-client/gauge @!connection)
     (reset! !connection nil))

   (let [uri (str "ws://" (or (:host opts) "localhost:9092") "/chsk")
         conn (p/gauge-connect! sente-client/gauge uri opts)]

     ;; Subscribe to welcome messages from server
     (p/on-message conn :welcome
                   (fn [data]
                     (println "Welcome from server:" data)
                     (swap! !app-state assoc :connected? true)))

     ;; Subscribe to notifications
     (p/on-message conn :notification
                   (fn [data]
                     (println "Notification:" data)))

     (reset! !connection conn)
     (println "Connected to Rheon server!")
     conn)))

(defn disconnect!
  "Disconnect from the server."
  []
  (when @!connection
    (p/gauge-close! sente-client/gauge @!connection)
    (reset! !connection nil)
    (swap! !app-state assoc :connected? false)
    (println "Disconnected")))

;; =============================================================================
;; Mouse Tracking with Clock Sync
;; =============================================================================

(defn send-coords!
  "Send mouse coordinates to server and receive clock sync reply.

   The server will respond with:
   {:x x :y y :server-time <timestamp> :clock-gap <ms>}"
  [coords]
  (when @!connection
    (let [client-time (.now js/Date)
          payload (assoc coords :client-time client-time)]

      ;; Use request! for bidirectional communication
      (p/request! @!connection :mouse payload
                  {:timeout-ms 5000
                   :on-reply (fn [reply]
                               (when (map? reply)
                                 (let [{:keys [server-time clock-gap]} reply]
                                   (swap! !app-state assoc
                                          :server-time server-time
                                          :clock-gap clock-gap)
                                   (println "Clock gap:" clock-gap "ms"))))}))))

;; =============================================================================
;; Mouse Event Handler
;; =============================================================================

(defonce !last-send-time (atom 0))

(defn handle-mousemove!
  "Handle mouse move events with throttling."
  [e]
  (let [x (j/get e :clientX)
        y (j/get e :clientY)
        now (.now js/Date)]

    ;; Always update local state
    (swap! !app-state assoc :coords {:x x :y y})

    ;; Throttle network sends to every 50ms
    (when (> (- now @!last-send-time) 50)
      (reset! !last-send-time now)
      (send-coords! {:x x :y y}))))

(defn start-tracking!
  "Start mouse tracking."
  []
  (j/call js/document :addEventListener "mousemove" handle-mousemove!)
  (println "Mouse tracking started"))

(defn stop-tracking!
  "Stop mouse tracking."
  []
  (j/call js/document :removeEventListener "mousemove" handle-mousemove!)
  (println "Mouse tracking stopped"))

;; =============================================================================
;; Initialization
;; =============================================================================

(defn init!
  "Initialize the mouse tracker client."
  []
  (println "Initializing Rheon Mouse Tracker Client...")
  (connect!)
  (start-tracking!)
  (println "Ready!"))

(defn cleanup!
  "Clean up resources."
  []
  (stop-tracking!)
  (disconnect!))

;; =============================================================================
;; REPL Helpers
;; =============================================================================

(comment
  ;; Initialize
  (init!)

  ;; Connect to different server
  (connect! {:host "localhost:8080"})

  ;; Check state
  @!app-state
  @!connection

  ;; Manual send
  (send-coords! {:x 100 :y 200})

  ;; Cleanup
  (cleanup!)
  )
