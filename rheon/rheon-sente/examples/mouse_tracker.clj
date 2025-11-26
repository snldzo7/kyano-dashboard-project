(ns mouse-tracker
  "Example: Mouse Tracker using Rheon Sente Gauge

   This demonstrates how to build the v4 mouse tracker pattern
   using the Rheon abstraction layer instead of direct Sente calls.

   Key features:
   - Server-side request handler for :mouse wire
   - Bidirectional clock sync (client sends timestamp, server replies with gap)
   - Multiple client support with broadcast capability

   To run:
     (require '[mouse-tracker :as tracker])
     (tracker/start-server!)

   Then connect from ClojureScript using rheon.gauge.sente-client"
  (:require [rheon.protocols :as p]
            [rheon.gauge.sente :as sente-gauge]))

;; =============================================================================
;; Server State
;; =============================================================================

(defonce server (atom nil))
(defonce client-count (atom 0))

;; =============================================================================
;; Mouse Request Handler
;; =============================================================================

(defn handle-mouse-coords!
  "Handle incoming mouse coordinates with clock synchronization.

   Client sends: {:x 100 :y 200 :client-time 1234567890}
   Server replies: {:x 100 :y 200 :server-time 1234567900 :clock-gap 10}"
  [data reply!]
  (let [{:keys [x y client-time]} data
        server-time (System/currentTimeMillis)
        clock-gap (when client-time (- server-time client-time))]

    ;; Log the coordinates
    (println (format "Mouse from client: x=%d y=%d (gap: %s ms)"
                     x y (or clock-gap "N/A")))

    ;; Send reply with server timestamp and calculated gap
    (reply! {:x x
             :y y
             :server-time server-time
             :clock-gap clock-gap})))

;; =============================================================================
;; Client Connection Handler
;; =============================================================================

(defn on-client-connected!
  "Called when a new client connects.
   The client-conn implements IConnection and can be used for direct messaging."
  [client-conn]
  (swap! client-count inc)
  (println (format "Client connected! UID: %s (total: %d)"
                   (:uid client-conn) @client-count))

  ;; Example: Send welcome message to this specific client
  (p/send! client-conn :welcome {:msg "Connected to Rheon Mouse Server"
                                  :server-time (System/currentTimeMillis)}))

(defn on-client-disconnected!
  "Called when a client disconnects."
  [client-conn]
  (swap! client-count dec)
  (println (format "Client disconnected! UID: %s (total: %d)"
                   (:uid client-conn) @client-count)))

;; =============================================================================
;; Server Lifecycle
;; =============================================================================

(defn start-server!
  "Start the mouse tracker server.

   Options:
   - :port - Server port (default: 9092)"
  ([] (start-server! {}))
  ([opts]
   (when @server
     (println "Stopping existing server...")
     (.close @server)
     (reset! server nil))

   (println "Starting Rheon Mouse Server...")
   (let [port (or (:port opts) 9092)
         srv (p/gauge-listen! sente-gauge/gauge {:port port})]

     ;; Register client connection handler
     (p/on-client srv on-client-connected!)

     ;; Register disconnect handler
     (sente-gauge/on-disconnect srv on-client-disconnected!)

     ;; Register request handler for :mouse wire
     ;; This handles the bidirectional request/reply pattern
     (p/on-request srv :mouse handle-mouse-coords!)

     ;; Example: Also listen for simple messages (no reply expected)
     (p/on-message srv :ping
                   (fn [data]
                     (println "Received ping:" data)))

     (reset! server srv)
     (println (format "Rheon Mouse Server running on port %d" port))
     srv)))

(defn stop-server!
  "Stop the mouse tracker server."
  []
  (when @server
    (println "Stopping Rheon Mouse Server...")
    (.close @server)
    (reset! server nil)
    (reset! client-count 0)
    (println "Server stopped")))

(defn broadcast-to-all!
  "Broadcast a message to all connected clients."
  [wire data]
  (when @server
    (p/send! @server wire data)))

;; =============================================================================
;; REPL Helpers
;; =============================================================================

(comment
  ;; Start server
  (start-server!)

  ;; Start on custom port
  (start-server! {:port 8080})

  ;; Stop server
  (stop-server!)

  ;; Check connected clients
  @client-count
  (sente-gauge/connected-count @server)
  (sente-gauge/connected-uids @server)

  ;; Broadcast to all clients
  (broadcast-to-all! :notification {:msg "Server announcement!"})

  ;; Server state
  @server
  )
