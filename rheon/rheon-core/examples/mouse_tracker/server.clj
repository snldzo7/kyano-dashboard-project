(ns mouse-tracker.server
  "Rheon Mouse Tracker Backend Server.

   Real Clojure JVM server with WebSocket transport.
   Uses wire-refs with Malli specs for validation.

   Architecture: Wire-refs ARE data - shared between client and server.
   We require the same wire definitions from the shared namespace."
  (:require [rheon.core :as r]
            [mouse-tracker.wires :as wires])
  (:import [java.util.concurrent Executors TimeUnit]))

;; =============================================================================
;; Server State
;; =============================================================================

(defonce server-state (atom {:tick 0
                             :start-time (System/currentTimeMillis)
                             :clients 0}))

;; =============================================================================
;; Server Connection
;; =============================================================================

(defonce conn (atom nil))

(defn get-conn []
  (when-not @conn
    (reset! conn (r/connection {:transport :ws-server :port 8084})))
  @conn)

;; =============================================================================
;; Wire Instances (created from shared wire-refs)
;; =============================================================================

(defn mouse-wire []
  (r/wire (get-conn) wires/mouse-ref))

(defn heartbeat-wire []
  (r/wire (get-conn) wires/heartbeat-ref))

(defn clock-wire []
  (r/wire (get-conn) wires/clock-ref))

(defn status-wire []
  (r/wire (get-conn) wires/status-ref))

(defn presence-wire []
  (r/wire (get-conn) wires/presence-ref))

;; =============================================================================
;; Heartbeat Emitter
;; =============================================================================

(defonce heartbeat-executor (atom nil))

(defn emit-heartbeat! []
  (let [tick (swap! server-state update :tick inc)
        now (System/currentTimeMillis)
        start (:start-time @server-state)
        uptime-sec (/ (- now start) 1000.0)]
    (try
      (r/emit! (heartbeat-wire)
               {:server-time now
                :tick (:tick @server-state)
                :uptime-sec (format "%.1f" uptime-sec)})
      (catch Exception e
        (println "Heartbeat error:" (.getMessage e))))))

(defn start-heartbeat! []
  (when-not @heartbeat-executor
    (println "Starting heartbeat emitter...")
    (let [executor (Executors/newSingleThreadScheduledExecutor)]
      (reset! heartbeat-executor executor)
      (.scheduleAtFixedRate executor
                            ^Runnable emit-heartbeat!
                            1000  ;; initial delay
                            1000  ;; period
                            TimeUnit/MILLISECONDS))))

(defn stop-heartbeat! []
  (when-let [executor @heartbeat-executor]
    (println "Stopping heartbeat emitter...")
    (.shutdown executor)
    (reset! heartbeat-executor nil)))

;; =============================================================================
;; Clock Sync Handler
;; =============================================================================

(defn setup-clock-handler! []
  (println "Setting up clock sync handler...")
  (r/reply! (clock-wire)
            (fn [{:keys [client-time]}]
              (let [server-time (System/currentTimeMillis)
                    gap (- server-time (or client-time 0))]
                (println "Clock sync request: client-time=" client-time "gap=" gap)
                {:server-time server-time
                 :client-time client-time
                 :gap gap}))))

;; =============================================================================
;; Mouse Listener (echo back to all clients)
;; =============================================================================

(defn setup-mouse-listener! []
  (println "Setting up mouse listener...")
  (r/listen (mouse-wire)
            (fn [data]
              (println "Mouse:" (:x data) "," (:y data)))))

;; =============================================================================
;; Server Lifecycle
;; =============================================================================

(defn start! []
  (println "")
  (println "╔═══════════════════════════════════════════════════════════════╗")
  (println "║           Rheon Mouse Tracker Server v2.0                     ║")
  (println "║                                                               ║")
  (println "║   WebSocket: ws://localhost:8084                              ║")
  (println "║   Wires: :heartbeat (stream), :clock (discrete), :status      ║")
  (println "║   Using: Wire-refs with Malli specs for validation            ║")
  (println "╚═══════════════════════════════════════════════════════════════╝")
  (println "")

  ;; Initialize connection
  (get-conn)
  (println "WebSocket server started on port 8084")

  ;; Set status to connected
  (r/signal! (status-wire) {:state :connected})
  (println "Status signal set to :connected")

  ;; Setup handlers
  (setup-clock-handler!)
  (setup-mouse-listener!)

  ;; Start heartbeat
  (start-heartbeat!)

  (println "")
  (println "Server ready. Press Ctrl+C to stop.")
  (println ""))

(defn stop! []
  (println "Shutting down server...")
  (stop-heartbeat!)
  (when @conn
    (r/signal! (status-wire) {:state :disconnected})
    (r/close! @conn)
    (reset! conn nil))
  (println "Server stopped."))

;; =============================================================================
;; Main Entry Point
;; =============================================================================

(defn -main [& _args]
  (start!)

  ;; Keep server running
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. ^Runnable stop!))

  ;; Block forever
  @(promise))
