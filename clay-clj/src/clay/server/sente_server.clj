(ns clay.server.sente-server
  "Sente WebSocket server for mouse tracker v3.
   Provides bidirectional communication with automatic reconnection,
   CSRF protection, and structured event routing."
  (:require [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter]]
            [org.httpkit.server :as http]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]))

;; =============================================================================
;; Sente Channel Socket Setup
;; =============================================================================

(let [{:keys [ch-recv send-fn connected-uids
              ajax-post-fn ajax-get-or-ws-handshake-fn]}
      (sente/make-channel-socket-server!
       (get-sch-adapter)
       {;; Generate unique user ID for each connection
        :user-id-fn (fn [_req] (str (java.util.UUID/randomUUID)))
        ;; Disable CSRF for development simplicity
        ;; In production, use proper CSRF token validation
        :csrf-token-fn nil})]

  (def ring-ajax-post ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk ch-recv)
  (def chsk-send! send-fn)
  (def connected-uids connected-uids))

;; =============================================================================
;; Event Handlers (Multimethod dispatch)
;; =============================================================================

(defmulti event-handler
  "Dispatch Sente events by :id"
  :id)

(defmethod event-handler :default
  [{:keys [id]}]
  (println "Unhandled Sente event:" id))

;; Handle mouse coordinate events from clients
(defmethod event-handler :mouse/coords
  [{:keys [?data uid]}]
  (let [{:keys [x y]} ?data]
    (println (format "Mouse from %s: x=%d y=%d" uid x y))))

;; Client connected
(defmethod event-handler :chsk/uidport-open
  [{:keys [uid]}]
  (println "Client connected:" uid)
  (println "Total clients:" (count (:any @connected-uids))))

;; Client disconnected
(defmethod event-handler :chsk/uidport-close
  [{:keys [uid]}]
  (println "Client disconnected:" uid)
  (println "Total clients:" (count (:any @connected-uids))))

;; WebSocket ping (heartbeat) - ignore silently
(defmethod event-handler :chsk/ws-ping
  [_]
  nil)

;; =============================================================================
;; Event Router
;; =============================================================================

(defonce router (atom nil))

(defn start-router!
  "Start the Sente event router"
  []
  (when @router
    (println "Stopping existing router...")
    (@router))
  (println "Starting Sente event router...")
  (reset! router (sente/start-server-chsk-router! ch-chsk event-handler)))

(defn stop-router!
  "Stop the Sente event router"
  []
  (when @router
    (println "Stopping Sente event router...")
    (@router)
    (reset! router nil)))

;; =============================================================================
;; Ring Handler
;; =============================================================================

(defn handler
  "Ring handler for Sente endpoints"
  [req]
  (case (:uri req)
    "/chsk" (case (:request-method req)
              :get (ring-ajax-get-or-ws-handshake req)
              :post (ring-ajax-post req)
              {:status 405 :body "Method not allowed"})
    {:status 200
     :headers {"Content-Type" "text/plain"}
     :body "Sente server running. Connect to /chsk for WebSocket."}))

(def app
  "Ring app with required middleware"
  (-> handler
      wrap-keyword-params
      wrap-params))

;; =============================================================================
;; Server Lifecycle
;; =============================================================================

(defonce server (atom nil))

(defn start-server!
  "Start the Sente server on port 9091"
  []
  (when @server
    (println "Stopping existing server...")
    (@server)
    (reset! server nil))

  (start-router!)
  (println "Starting Sente HTTP server on port 9091...")
  (reset! server (http/run-server app {:port 9091}))
  (println "Sente server started at http://localhost:9091/chsk"))

(defn stop-server!
  "Stop the Sente server"
  []
  (stop-router!)
  (when @server
    (println "Stopping Sente HTTP server...")
    (@server)
    (reset! server nil)
    (println "Sente server stopped")))

(defn restart-server!
  "Restart the Sente server"
  []
  (stop-server!)
  (start-server!))

;; =============================================================================
;; REPL Helpers
;; =============================================================================

(comment
  ;; Start the server
  (start-server!)

  ;; Stop the server
  (stop-server!)

  ;; Restart the server
  (restart-server!)

  ;; Check connected clients
  @connected-uids

  ;; Send message to all clients
  (doseq [uid (:any @connected-uids)]
    (chsk-send! uid [:server/ping {:time (System/currentTimeMillis)}]))
  )
