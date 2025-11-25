(ns rheon.gauge.mem
  "In-memory gauge for testing - no network dependencies.

   This gauge uses atoms to simulate network communication within
   a single process. Perfect for:
   - Unit tests
   - REPL exploration
   - Development without running servers

   Usage:
     (def server (listen! {:port 9092 :gauge :mem}))
     (def client (connect! \"mem://localhost:9092\" {:gauge :mem}))"
  (:require [rheon.protocols :as p]
            [clojure.string :as str]))

;; =============================================================================
;; Global Registry (simulates network)
;; =============================================================================
;; Maps port -> server state

(defonce ^:private servers (atom {}))

(defn- get-server
  "Get server by port (from URI like 'mem://localhost:9092')"
  [port]
  (get @servers port))

;; =============================================================================
;; Subscription Management
;; =============================================================================

(defn- make-subscription
  "Create a subscription record"
  [type wire handler]
  {:id (random-uuid)
   :type type  ;; :message or :request
   :wire wire
   :handler handler})

;; =============================================================================
;; In-Memory Server State
;; =============================================================================

(defrecord MemServerState [port
                           clients          ;; atom: set of client-ids
                           subscriptions    ;; atom: {wire -> [subscription]}
                           client-handlers  ;; atom: [(fn [conn] ...)]
                           closed?])        ;; atom: boolean

(defn- make-server-state [port]
  (->MemServerState port
                    (atom #{})
                    (atom {})
                    (atom [])
                    (atom false)))

;; =============================================================================
;; In-Memory Client State
;; =============================================================================

(defrecord MemClientState [id
                           port
                           subscriptions    ;; atom: {wire -> [subscription]}
                           request-handlers ;; atom: {wire -> handler}
                           reply-callbacks  ;; atom: {request-id -> callback}
                           closed?])        ;; atom: boolean

(defn- make-client-state [port]
  (->MemClientState (random-uuid)
                    port
                    (atom {})
                    (atom {})
                    (atom {})
                    (atom false)))

;; =============================================================================
;; Message Delivery
;; =============================================================================

(defn- deliver-to-subscribers
  "Deliver message to all subscribers on a wire"
  [subscriptions-atom wire data]
  (when-let [subs (get @subscriptions-atom wire)]
    (doseq [{:keys [handler type]} subs]
      (when (= type :message)
        (try
          (handler data)
          (catch #?(:clj Exception :cljs :default) e
            (println "Error in message handler:" e)))))))

(defn- deliver-request
  "Deliver request to handler and get reply"
  [subscriptions-atom wire data reply-fn]
  (when-let [subs (get @subscriptions-atom wire)]
    (when-let [{:keys [handler]} (first (filter #(= (:type %) :request) subs))]
      (try
        (handler data reply-fn)
        (catch #?(:clj Exception :cljs :default) e
          (println "Error in request handler:" e))))))

;; =============================================================================
;; MemGauge Implementation
;; =============================================================================

(defrecord MemGauge []
  p/IGauge

  (gauge-connect! [_ uri _opts]
    ;; Parse port from URI like "mem://localhost:9092"
    (let [port (-> uri
                   (str/replace #"mem://[^:]+:" "")
                   (str/replace #"/" "")
                   #?(:clj Integer/parseInt :cljs js/parseInt))
          server (get-server port)
          client-state (make-client-state port)]

      (when-not server
        (throw (ex-info "No server listening on port" {:port port})))

      ;; Register client with server
      (swap! (:clients server) conj (:id client-state))

      ;; Notify server of new client (call handlers)
      ;; Pass raw client-state - the public API wraps it in Connection
      (doseq [handler @(:client-handlers server)]
        (handler client-state))

      client-state))

  (gauge-listen! [_ opts]
    (let [port (:port opts)
          state (make-server-state port)]
      ;; Register server
      (swap! servers assoc port state)
      state))

  (gauge-send! [_ conn-state wire data _opts]
    (when-not @(:closed? conn-state)
      (let [port (:port conn-state)
            server (get-server port)]
        (when server
          ;; Deliver to server's subscribers on this wire
          (deliver-to-subscribers (:subscriptions server) wire data)))))

  (gauge-request! [_ conn-state wire data opts]
    (when-not @(:closed? conn-state)
      (let [port (:port conn-state)
            server (get-server port)
            {:keys [on-reply]} opts]
        (when server
          ;; Create reply function that calls the callback
          (let [reply-fn (fn [reply-data]
                           (when on-reply
                             (on-reply reply-data)))]
            ;; Deliver request to server's request handlers
            (deliver-request (:subscriptions server) wire data reply-fn))))))

  (gauge-subscribe! [_ conn-state wire handler]
    (let [sub (make-subscription :message wire handler)]
      (swap! (:subscriptions conn-state) update wire (fnil conj []) sub)
      sub))

  (gauge-on-request! [_ conn-state wire handler]
    (let [sub (make-subscription :request wire handler)]
      (swap! (:subscriptions conn-state) update wire (fnil conj []) sub)
      sub))

  (gauge-unsubscribe! [_ _subscription]
    ;; TODO: Implement proper unsubscribe
    ;; Subscription should contain conn-state reference
    nil)

  (gauge-close! [_ conn-state]
    (reset! (:closed? conn-state) true)
    (let [port (:port conn-state)
          server (get-server port)]
      (when server
        (swap! (:clients server) disj (:id conn-state))))))

;; Server-specific operations (extend MemServerState)
(extend-type MemServerState
  p/IServer
  (on-client [server handler]
    (swap! (:client-handlers server) conj handler)
    ;; Return a subscription-like thing for consistency
    {:type :client-handler :handler handler}))

;; =============================================================================
;; Gauge Instance
;; =============================================================================

(def gauge
  "Singleton in-memory gauge instance"
  (->MemGauge))

;; =============================================================================
;; IGaugeInfo Implementation
;; =============================================================================

(extend-type MemGauge
  p/IGaugeInfo
  (gauge-name [_] :mem)
  (gauge-description [_] "In-memory gauge for testing - no network dependencies")
  (gauge-requires [_] [])
  (gauge-options [_]
    {:port "Port number to listen on or connect to"}))

;; =============================================================================
;; Cleanup Utilities
;; =============================================================================

(defn reset-servers!
  "Clear all registered servers. Useful for test cleanup."
  []
  (reset! servers {}))

(defn server-count
  "Return count of registered servers."
  []
  (count @servers))
