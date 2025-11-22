(ns clay.server.mouse-server
  (:require [org.httpkit.server :as http]
            [cognitect.transit :as transit])
  (:import [java.io ByteArrayInputStream]))

(defonce server (atom nil))
(defonce clients (atom #{}))

(defn ws-handler [req]
  (http/with-channel req channel
    (swap! clients conj channel)
    (println "Client connected. Total clients:" (count @clients))

    (http/on-close channel
      (fn [_status]
        (swap! clients disj channel)
        (println "Client disconnected. Total clients:" (count @clients))))

    (http/on-receive channel
      (fn [data]
        (try
          (let [reader (transit/reader (ByteArrayInputStream. (.getBytes data)) :json)
                coords (transit/read reader)
                {:keys [x y]} coords]
            (println (format "Mouse: x=%d y=%d" x y)))
          (catch Exception e
            (println "Error parsing message:" (.getMessage e))))))))

(defn handler [req]
  (if (:websocket? req)
    (ws-handler req)
    {:status 200
     :headers {"Content-Type" "text/plain"}
     :body "WebSocket server running on ws://localhost:9090"}))

(defn start-server! []
  (when @server
    (println "Stopping existing server...")
    (@server)
    (reset! server nil))
  (println "Starting WebSocket server on port 9090...")
  (reset! server (http/run-server handler {:port 9090}))
  (println "Server started at ws://localhost:9090"))

(defn stop-server! []
  (when @server
    (println "Stopping server...")
    (@server)
    (reset! server nil)
    (reset! clients #{})
    (println "Server stopped")))

(comment
  (start-server!)
  (stop-server!)
  @clients)
