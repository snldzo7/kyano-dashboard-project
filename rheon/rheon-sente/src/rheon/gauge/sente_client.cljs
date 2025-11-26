(ns rheon.gauge.sente-client
  "Rheon Sente gauge - WebSocket communication via Sente.

   Client-side implementation (ClojureScript).

   This gauge provides real-time bidirectional communication using
   Sente's channel sockets from the browser.

   Features:
   - WebSocket with AJAX fallback
   - Request/reply pattern with callbacks
   - Automatic reconnection
   - Connection state events

   Usage:
     (require '[rheon.core :as r])
     (r/register-gauge! :sente rheon.gauge.sente-client/gauge)

     ;; Connect to server
     (def conn (r/connect! \"ws://localhost:9092/chsk\" {:gauge :sente}))

     ;; Send message
     (r/send! conn :mouse {:x 100 :y 200})

     ;; Request with reply
     (r/request! conn :ping {} 5000
       (fn [response] (println \"Pong:\" response)))

     ;; Subscribe to messages
     (r/on-message conn :notification
       (fn [data] (println \"Got:\" data)))"
  (:require [rheon.protocols :as p]
            [taoensso.sente :as sente]
            [clojure.string :as str]))

;; =============================================================================
;; Sente Client State
;; =============================================================================

(defrecord SenteClientState
    [chsk                 ;; Sente channel socket
     ch-recv              ;; Channel for receiving events
     send-fn              ;; Function to send messages
     state                ;; Atom with connection state
     router               ;; Router stop function
     message-handlers     ;; atom: {wire -> [handlers]}
     request-handlers     ;; atom: {wire -> handler}
     state-handlers       ;; atom: [handler-fns] for connection state changes
     wire-config])        ;; Wire configuration

;; =============================================================================
;; Event Router
;; =============================================================================

(defn- make-event-handler
  "Create the event handler for Sente client events."
  [client-state]
  (fn [{:keys [id ?data] :as event}]
    (let [message-handlers @(:message-handlers client-state)
          request-handlers @(:request-handlers client-state)
          state-handlers @(:state-handlers client-state)]

      (case id
        ;; Connection state change
        :chsk/state
        (let [[old-state new-state] ?data]
          (doseq [handler state-handlers]
            (try
              (handler {:old old-state :new new-state :open? (:open? new-state)})
              (catch :default e
                (println "Error in state handler:" e)))))

        ;; Handshake complete
        :chsk/handshake
        (let [[?uid ?csrf-token ?handshake-data] ?data]
          (println "Sente handshake complete. UID:" ?uid))

        ;; Received message from server
        :chsk/recv
        (let [[event-id event-data] ?data
              wire-name (name event-id)
              wire (keyword wire-name)
              handlers (get message-handlers wire)]
          (doseq [handler handlers]
            (try
              (handler event-data)
              (catch :default e
                (println "Error in message handler for" wire ":" e)))))

        ;; Ignore ping
        :chsk/ws-ping nil

        ;; Other events - try to dispatch by wire
        (when (keyword? id)
          (let [wire (keyword (name id))
                handlers (get message-handlers wire)]
            (doseq [handler handlers]
              (try
                (handler ?data)
                (catch :default e
                  (println "Error in message handler for" wire ":" e))))))))))

(defn- start-router!
  "Start the Sente event router."
  [client-state]
  (sente/start-client-chsk-router! (:ch-recv client-state)
                                   (make-event-handler client-state)))

;; =============================================================================
;; URI Parsing
;; =============================================================================

(defn- parse-sente-uri
  "Parse a Sente URI into host, port, and path.
   Format: ws://host:port/path or sente://host:port/path"
  [uri]
  (let [;; Remove protocol prefix
        without-protocol (-> uri
                             (str/replace #"^ws://" "")
                             (str/replace #"^wss://" "")
                             (str/replace #"^sente://" "")
                             (str/replace #"^http://" "")
                             (str/replace #"^https://" ""))
        ;; Split host:port from path
        [host-port path] (str/split without-protocol #"/" 2)
        ;; Split host from port
        [host port-str] (str/split host-port #":")
        port (when port-str (js/parseInt port-str 10))]
    {:host (or host "localhost")
     :port (or port 9092)
     :path (str "/" (or path "chsk"))}))

;; =============================================================================
;; Protocol Implementation
;; =============================================================================

(extend-type SenteClientState
  p/IConnection
  (send! [this wire data]
    (let [send-fn (:send-fn this)]
      (send-fn [(keyword "rheon" (name wire)) data])))

  (request! [this wire data opts]
    (let [send-fn (:send-fn this)
          {:keys [timeout-ms on-reply]} opts]
      (send-fn [(keyword "rheon" (name wire)) data]
               (or timeout-ms 5000)
               (fn [reply]
                 (when on-reply
                   (if (sente/cb-success? reply)
                     (on-reply reply)
                     (on-reply {:error :timeout})))))))

  (on-message [this wire handler]
    (swap! (:message-handlers this) update wire (fnil conj []) handler)
    {:wire wire :handler handler :state this :type :message})

  (on-request [this wire handler]
    ;; Client-side can register request handlers for server-initiated requests
    ;; This is less common but supported
    (swap! (:request-handlers this) assoc wire handler)
    {:wire wire :type :request :state this})

  (unsubscribe! [_ subscription]
    (let [client-state (:state subscription)]
      (case (:type subscription)
        :request
        (swap! (:request-handlers client-state) dissoc (:wire subscription))

        ;; Default: message subscription
        (swap! (:message-handlers client-state) update (:wire subscription)
               (fn [handlers] (vec (remove #{(:handler subscription)} handlers)))))))

  (configure! [this wire opts]
    (assoc-in this [:wire-config wire] opts)))

;; =============================================================================
;; Closeable Protocol
;; =============================================================================

;; ClojureScript doesn't have java.io.Closeable, so we use a custom approach
(defn close!
  "Close the Sente client connection."
  [client-state]
  ;; Stop router
  (when-let [stop-fn (:router client-state)]
    (stop-fn))
  ;; Disconnect channel socket
  (when-let [chsk (:chsk client-state)]
    (sente/chsk-disconnect! chsk)))

;; =============================================================================
;; Gauge Implementation
;; =============================================================================

(defrecord SenteClientGauge []
  p/IGauge
  (gauge-connect! [_ uri opts]
    (let [{:keys [host port path]} (parse-sente-uri uri)
          host-with-port (str host ":" port)

          ;; Create Sente channel socket client
          {:keys [chsk ch-recv send-fn state]}
          (sente/make-channel-socket-client!
           path
           nil  ;; CSRF token (nil for no CSRF)
           {:type :auto
            :host host-with-port
            :wrap-recv-evs? false
            :packer :edn})

          ;; Create client state
          client-state (map->SenteClientState
                        {:chsk chsk
                         :ch-recv ch-recv
                         :send-fn send-fn
                         :state state
                         :message-handlers (atom {})
                         :request-handlers (atom {})
                         :state-handlers (atom [])
                         :wire-config {}})

          ;; Start event router
          router-stop-fn (start-router! client-state)

          ;; Update with router
          final-state (assoc client-state :router router-stop-fn)]

      (println "Rheon Sente client connecting to" host-with-port path)
      final-state))

  (gauge-listen! [_ opts]
    ;; Client-side gauge doesn't support listen - that's for server
    (throw (js/Error. "Use rheon.gauge.sente for server-side listen!")))

  (gauge-send! [_ state wire data _opts]
    (p/send! state wire data))

  (gauge-request! [_ state wire data opts]
    (p/request! state wire data opts))

  (gauge-subscribe! [_ state wire handler]
    (p/on-message state wire handler))

  (gauge-on-request! [_ state wire handler]
    (p/on-request state wire handler))

  (gauge-unsubscribe! [_ subscription]
    (p/unsubscribe! (:state subscription) subscription))

  (gauge-close! [_ state]
    (close! state))

  p/IGaugeInfo
  (gauge-name [_] :sente)
  (gauge-description [_] "Sente WebSocket gauge for real-time bidirectional communication (client)")
  (gauge-requires [_] [["com.taoensso/sente" "1.19.2"]])
  (gauge-options [_] {:host "Server host:port"
                      :path "WebSocket path (default: /chsk)"})
  (gauge-capabilities [_] #{:send :request :subscribe}))

;; =============================================================================
;; Public API
;; =============================================================================

(def gauge
  "Sente client gauge instance for registration with Rheon."
  (->SenteClientGauge))

;; =============================================================================
;; Convenience Functions
;; =============================================================================

(defn connected?
  "Check if the client is connected."
  [client-state]
  (:open? @(:state client-state)))

(defn on-state-change
  "Register a handler for connection state changes.
   Handler receives {:old old-state :new new-state :open? boolean}"
  [client-state handler]
  (swap! (:state-handlers client-state) conj handler))

(defn connection-state
  "Get the current connection state."
  [client-state]
  @(:state client-state))
