(ns mouse-tracker
  "Interactive mouse tracker demo using Rheon WebSocket transport.

   Demonstrates all three wire types in BOTH directions:

   Client → Server:
   - Stream: Mouse position (client emits, server listens)
   - Signal: User presence (client sets name/color)

   Server → Client:
   - Stream: Server heartbeat (server emits every second)
   - Signal: Connection status (server sets, clients watch)

   Bidirectional:
   - Discrete: Clock sync request/response

   Run: clj -A:dev -M -m mouse-tracker
   Then open: http://localhost:8085 (run: npm run watch)"
  (:require [rheon.core :as r]))

(defonce state (atom nil))

;; =============================================================================
;; Server
;; =============================================================================

(defn start-heartbeat!
  "Start a background thread that emits heartbeats every second."
  [heartbeat-wire]
  (future
    (loop [tick 0]
      (when-not (:stopped? @state)
        (try
          (r/emit! heartbeat-wire
                   {:server-time (System/currentTimeMillis)
                    :tick tick
                    :uptime-sec tick})
          (catch Exception e
            (println "Heartbeat error:" (.getMessage e))))
        (Thread/sleep 1000)
        (recur (inc tick))))))

(defn start!
  ([] (start! {:port 8084}))
  ([opts]
   (let [port (or (:port opts) 8084)
         ;; Create Rheon connection with JSON encoding for browser compatibility
         conn (r/connection {:transport :ws :port port :encoding :json})

         ;; ===== Client → Server Wires =====
         ;; Stream: Mouse position (client emits)
         mouse (r/stream :mouse conn)
         ;; Signal: User presence (client sets their name/color)
         presence (r/signal :presence conn {:users {}})

         ;; ===== Server → Client Wires =====
         ;; Stream: Server heartbeat (server emits every second)
         heartbeat (r/stream :heartbeat conn)
         ;; Signal: Server status (server sets)
         status (r/signal :status conn {:state :starting :clients 0})

         ;; ===== Bidirectional =====
         ;; Discrete: Clock sync (client sends request, server replies)
         clock (r/discrete :clock conn)

         ;; Set up handlers - pure application logic
         _ (r/listen mouse (fn [{:keys [x y]}]
                             (println "Mouse:" x y)))

         ;; Watch for presence updates from clients
         _ (r/watch presence (fn [p]
                               (println "Presence updated:" (pr-str p))))

         _ (r/reply! clock (fn [data]
                            (let [client-time (or (:client-time data)
                                                  (get data "client-time"))
                                  server-time (System/currentTimeMillis)]
                              {:server-time server-time
                               :gap (- server-time client-time)})))]

     ;; Store state first (heartbeat checks :stopped?)
     (reset! state {:conn conn
                    :mouse mouse
                    :clock clock
                    :status status
                    :heartbeat heartbeat
                    :presence presence
                    :stopped? false})

     ;; Start the heartbeat emitter (Server → Client stream)
     (start-heartbeat! heartbeat)

     ;; Update status to running
     (r/signal! status {:state :running :clients 0 :port port})

     (println)
     (println "========================================")
     (println "  Rheon Mouse Tracker Demo")
     (println "========================================")
     (println)
     (println (str "  WebSocket:      ws://localhost:" port))
     (println "  CLJS Frontend:  http://localhost:8085 (run: npm run watch)")
     (println)
     (println "  Wire types demonstrated:")
     (println)
     (println "  Client → Server:")
     (println "    - :mouse    (Stream)   - real-time cursor position")
     (println "    - :presence (Signal)   - user name/color")
     (println)
     (println "  Server → Client:")
     (println "    - :heartbeat (Stream)  - server tick every second")
     (println "    - :status    (Signal)  - connection state")
     (println)
     (println "  Bidirectional:")
     (println "    - :clock    (Discrete) - clock sync request/response")
     (println)

     @state)))

(defn stop! []
  (when-let [s @state]
    ;; Stop heartbeat thread first
    (swap! state assoc :stopped? true)
    (Thread/sleep 100) ;; Give heartbeat thread time to exit
    (when-let [conn (:conn s)]
      (r/close! conn))
    (reset! state nil)
    (println "Server stopped")))

(defn -main [& _args]
  (start!)
  @(promise))

(comment
  (start!)
  (stop!))
