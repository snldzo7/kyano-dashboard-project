(ns rheon.gauge.sente
  "Rheon Sente gauge - WebSocket communication via Sente.

   Server-side implementation (Clojure).

   This gauge provides real-time bidirectional communication using
   Sente's channel sockets with http-kit as the server adapter.

   Features:
   - WebSocket with AJAX fallback
   - Request/reply pattern with ?reply-fn
   - Client connection/disconnection events
   - Broadcast to all connected clients

   Usage:
     (require '[rheon.core :as r])
     (r/register-gauge! :sente rheon.gauge.sente/gauge)

     ;; Server
     (def server (r/listen! {:port 9092 :gauge :sente}))
     (r/on-client server (fn [conn] ...))
     (r/on-request server :ping (fn [data reply!] (reply! :pong)))

     ;; Send to specific client
     (r/send! client-conn :notification {:msg \"hello\"})

     ;; Broadcast to all
     (r/send! server :broadcast {:msg \"to-all\"})"
  (:require [rheon.protocols :as p]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter]]
            [org.httpkit.server :as http]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]])
  (:import [java.io Closeable]))

;; =============================================================================
;; Forward declarations for protocol implementations
;; =============================================================================

(declare server-send! server-on-message server-on-request server-unsubscribe!)

;; =============================================================================
;; Sente Server State
;; =============================================================================

(defrecord SenteServerState
    [port
     ch-recv              ;; Channel for receiving events
     send-fn              ;; Function to send messages
     connected-uids       ;; Atom of connected user IDs
     ajax-post-fn         ;; Ring handler for POST
     ajax-get-fn          ;; Ring handler for GET/WebSocket
     http-server          ;; http-kit server instance
     router               ;; Sente event router stop-fn
     message-handlers     ;; atom: {wire -> [handlers]}
     request-handlers     ;; atom: {wire -> handler}
     client-handlers      ;; atom: [handler-fns]
     disconnect-handlers  ;; atom: [handler-fns]
     wire-config]         ;; atom: Wire configuration

  Closeable
  (close [_]
    ;; Stop router
    (when router
      (router))
    ;; Stop HTTP server
    (when http-server
      (http-server)))

  p/IConnection
  (send! [this wire data]
    (server-send! this wire data))

  (send! [this wire data _opts]
    (server-send! this wire data))

  (request! [_ _ _ _]
    (throw (ex-info "Server cannot make requests. Use send! to broadcast." {})))

  (on-message [this wire handler]
    (server-on-message this wire handler))

  (on-request [this wire handler]
    (server-on-request this wire handler))

  (unsubscribe! [_ subscription]
    (server-unsubscribe! subscription))

  (configure! [_ wire opts]
    (swap! wire-config assoc wire opts))

  p/IServer
  (on-client [_ handler]
    (swap! client-handlers conj handler)))

(defrecord SenteClientState
    [uid                  ;; Client's unique ID
     server-state         ;; Reference to server state
     client-wire-config]  ;; atom: Wire configuration for this client

  p/IConnection
  (send! [_ wire data]
    (let [sfn (:send-fn server-state)]
      (sfn uid [(keyword "rheon" (name wire)) data])))

  (send! [this wire data _opts]
    (p/send! this wire data))

  (request! [_ _ _ _]
    (throw (ex-info "Cannot make request from server to client. Use send! instead." {})))

  (on-message [_ wire handler]
    (server-on-message server-state wire handler))

  (on-request [_ wire handler]
    (server-on-request server-state wire handler))

  (unsubscribe! [_ subscription]
    (server-unsubscribe! subscription))

  (configure! [_ wire opts]
    (swap! client-wire-config assoc wire opts)))

;; =============================================================================
;; Protocol Implementation Functions
;; =============================================================================

(defn- server-send!
  "Send message to all connected clients."
  [server wire data]
  (let [uids (:any @(:connected-uids server))]
    (doseq [uid uids]
      ((:send-fn server) uid [(keyword "rheon" (name wire)) data]))))

(defn- server-on-message
  "Subscribe to messages on a wire."
  [server wire handler]
  (swap! (:message-handlers server) update wire (fnil conj []) handler)
  {:wire wire :handler handler :state server :type :message})

(defn- server-on-request
  "Register request handler for a wire."
  [server wire handler]
  (swap! (:request-handlers server) assoc wire handler)
  {:wire wire :type :request :state server})

(defn- server-unsubscribe!
  "Unsubscribe from a wire."
  [subscription]
  (let [server (:state subscription)]
    (case (:type subscription)
      :request
      (swap! (:request-handlers server) dissoc (:wire subscription))

      ;; Default: message subscription
      (swap! (:message-handlers server) update (:wire subscription)
             (fn [handlers] (vec (remove #{(:handler subscription)} handlers)))))))

;; =============================================================================
;; Event Router
;; =============================================================================

(defn- make-event-handler
  "Create the multimethod-style event handler for Sente events."
  [server-state]
  (fn [{:keys [id ?data ?reply-fn uid]}]
    (let [message-handlers @(:message-handlers server-state)
          request-handlers @(:request-handlers server-state)
          client-handlers @(:client-handlers server-state)
          disconnect-handlers @(:disconnect-handlers server-state)]

      (case id
        ;; Client connected
        :chsk/uidport-open
        (let [client-state (->SenteClientState uid server-state (atom {}))]
          (doseq [handler client-handlers]
            (try
              (handler client-state)
              (catch Exception e
                (println "Error in client handler:" (.getMessage e))))))

        ;; Client disconnected
        :chsk/uidport-close
        (let [client-state (->SenteClientState uid server-state (atom {}))]
          (doseq [handler disconnect-handlers]
            (try
              (handler client-state)
              (catch Exception e
                (println "Error in disconnect handler:" (.getMessage e))))))

        ;; Ignore ping
        :chsk/ws-ping nil

        ;; Application events - dispatch by wire
        (let [wire (keyword (name id))]
          ;; Check if this is a request (has reply-fn) or message
          (if ?reply-fn
            ;; Request - call registered request handler
            (when-let [handler (get request-handlers wire)]
              (try
                (handler ?data
                         (fn [response]
                           (?reply-fn response)))
                (catch Exception e
                  (println "Error in request handler for" wire ":" (.getMessage e))
                  (?reply-fn {:error (.getMessage e)}))))

            ;; Message - call all subscribed handlers
            (when-let [handlers (get message-handlers wire)]
              (doseq [handler handlers]
                (try
                  (handler ?data)
                  (catch Exception e
                    (println "Error in message handler for" wire ":" (.getMessage e))))))))))))

(defn- start-router!
  "Start the Sente event router."
  [server-state]
  (let [handler (make-event-handler server-state)]
    (sente/start-server-chsk-router! (:ch-recv server-state) handler)))

;; =============================================================================
;; Ring Handler
;; =============================================================================

(defn- make-ring-handler
  "Create Ring handler for Sente endpoints."
  [server-state chsk-path]
  (fn [req]
    (if (= (:uri req) chsk-path)
      (case (:request-method req)
        :get ((:ajax-get-fn server-state) req)
        :post ((:ajax-post-fn server-state) req)
        {:status 405 :body "Method not allowed"})
      {:status 200
       :headers {"Content-Type" "text/plain"
                 "Access-Control-Allow-Origin" "*"}
       :body (str "Rheon Sente server. Connect to " chsk-path " for WebSocket.")})))

(defn- make-app
  "Create Ring app with middleware."
  [server-state chsk-path]
  (-> (make-ring-handler server-state chsk-path)
      wrap-keyword-params
      wrap-params))

;; =============================================================================
;; Gauge Implementation
;; =============================================================================

(defrecord SenteGauge []
  p/IGauge
  (gauge-connect! [_ uri _opts]
    (throw (ex-info "Use rheon.gauge.sente-client for client connections" {:uri uri})))

  (gauge-listen! [_ opts]
    (let [port (or (:port opts) 9092)
          chsk-path (or (:path opts) "/chsk")
          user-id-fn (or (:user-id-fn opts)
                         (fn [_req] (str (java.util.UUID/randomUUID))))

          ;; Create Sente channel socket
          {:keys [ch-recv send-fn connected-uids
                  ajax-post-fn ajax-get-or-ws-handshake-fn]}
          (sente/make-channel-socket-server!
           (get-sch-adapter)
           {:user-id-fn user-id-fn
            :csrf-token-fn nil
            :packer :edn})

          ;; Create server state (without http-server and router initially)
          server-state (map->SenteServerState
                        {:port port
                         :ch-recv ch-recv
                         :send-fn send-fn
                         :connected-uids connected-uids
                         :ajax-post-fn ajax-post-fn
                         :ajax-get-fn ajax-get-or-ws-handshake-fn
                         :message-handlers (atom {})
                         :request-handlers (atom {})
                         :client-handlers (atom [])
                         :disconnect-handlers (atom [])
                         :wire-config (atom {})})

          ;; Create Ring app
          app (make-app server-state chsk-path)

          ;; Start HTTP server
          http-server (http/run-server app {:port port})

          ;; Start event router
          router-stop-fn (start-router! server-state)

          ;; Return updated state with server and router
          final-state (assoc server-state
                             :http-server http-server
                             :router router-stop-fn)]

      (println (str "Rheon Sente server started on port " port " at " chsk-path))
      final-state))

  (gauge-send! [_ state wire data _opts]
    (p/send! state wire data))

  (gauge-request! [_ state wire data opts]
    (p/request! state wire data opts))

  (gauge-subscribe! [_ state wire handler]
    (p/on-message state wire handler))

  (gauge-on-request! [_ state wire handler]
    (p/on-request state wire handler))

  (gauge-unsubscribe! [_ subscription]
    (server-unsubscribe! subscription))

  (gauge-close! [_ state]
    (.close ^Closeable state))

  p/IGaugeInfo
  (gauge-name [_] :sente)
  (gauge-description [_] "Sente WebSocket gauge for real-time bidirectional communication")
  (gauge-capabilities [_] #{:send :request :subscribe :on-request :broadcast})
  (gauge-requires [_] [["com.taoensso/sente" "1.19.2"]
                       ["http-kit/http-kit" "2.7.0"]])
  (gauge-options [_] {:port "Server port (default: 9092)"
                      :path "WebSocket path (default: /chsk)"
                      :user-id-fn "Function to extract user ID from request"}))

;; =============================================================================
;; Public API
;; =============================================================================

(def gauge
  "Sente gauge instance for registration with Rheon."
  (->SenteGauge))

;; =============================================================================
;; Convenience Functions
;; =============================================================================

(defn connected-count
  "Get the number of connected clients."
  [server-state]
  (count (:any @(:connected-uids server-state))))

(defn connected-uids
  "Get set of connected client UIDs."
  [server-state]
  (:any @(:connected-uids server-state)))

(defn send-to-client!
  "Send a message to a specific client by UID."
  [server-state uid wire data]
  ((:send-fn server-state) uid [(keyword "rheon" (name wire)) data]))

(defn on-disconnect
  "Register handler for client disconnections.
   handler is (fn [client-state] ...) called when client disconnects."
  [server-state handler]
  (swap! (:disconnect-handlers server-state) conj handler))

(defn broadcast!
  "Broadcast message to all connected clients."
  [server-state wire data]
  (p/send! server-state wire data))
