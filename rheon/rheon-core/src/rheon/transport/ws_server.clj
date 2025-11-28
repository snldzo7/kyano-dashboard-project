(ns rheon.transport.ws-server
  "WebSocket server transport for Rheon v2 (Clojure JVM).

   Uses Missionary internally for backpressure and flow semantics.
   Transit encoding by default, user can provide custom IEncoder.
   User API remains callback-based (no leakage).

   Server:
     (def conn (connection {:transport :ws-server :port 8080}))
     (def conn (connection {:transport :ws-server :port 8080 :encoding :json}))
     (def conn (connection {:transport :ws-server :port 8080 :encoder my-encoder}))

   Any WebSocket client can connect and communicate."
  (:require [rheon.protocols :as p]
            [rheon.encoder :as enc]
            [org.httpkit.server :as http]
            [missionary.core :as m]))

;; =============================================================================
;; Encoder Resolution
;; =============================================================================

(defn- resolve-encoder
  "Resolve encoder from options. Priority: :encoder > :encoding > default."
  [opts]
  (or (:encoder opts)
      (when-let [fmt (:encoding opts)]
        (enc/encoder-for-format fmt))
      (enc/default-encoder)))

;; =============================================================================
;; Records
;; =============================================================================

(defrecord WSConnection [wires clients closed? server encoder])
;; wires: atom {wire-id -> wire}
;; clients: atom #{http-kit-channel}
;; closed?: atom boolean
;; server: http-kit server stop-fn
;; encoder: IEncoder implementation for wire format

(defrecord StreamWire [id conn wire-type listeners seq-counter last-seq-received flow-callbacks])
;; listeners: atom [handler-fn ...] for broadcast pub/sub
;; seq-counter: atom long - sequence number for ordering detection
;; last-seq-received: atom long - track incoming sequences for gap detection
;; flow-callbacks: atom #{fn ...} - functions to call when emitting (for m/observe)

(defrecord DiscreteWire [id conn wire-type handler pending request-callbacks])
;; handler: atom handler-fn
;; pending: atom {req-id -> {:on-reply fn :on-error fn}}
;; request-callbacks: atom #{fn ...} - functions to call on incoming requests (for m/observe)

(defrecord SignalWire [id conn wire-type value])
;; value: atom current-value (m/watch handles subscriptions automatically)

(defrecord Subscription [wire handler type cancel-fn])
;; cancel-fn: optional Missionary cancellation function

;; =============================================================================
;; Wire Constructors
;; =============================================================================

(defn make-stream-wire [id conn]
  (->StreamWire id conn :stream (atom []) (atom 0) (atom 0) (atom #{})))

(defn make-discrete-wire [id conn]
  (->DiscreteWire id conn :discrete (atom nil) (atom {}) (atom #{})))

(defn make-signal-wire [id conn initial-value]
  (->SignalWire id conn :signal (atom initial-value)))

;; =============================================================================
;; Broadcast Helper
;; =============================================================================

(defn broadcast!
  "Send a message to all connected clients."
  [conn msg]
  (let [encoder (:encoder conn)
        data (p/encode encoder msg)
        client-count (count @(:clients conn))]
    (when (= :emit (:op msg))
      (println "WS: Broadcasting emit, wire=" (:wire msg) "to" client-count "clients"))
    (doseq [ch @(:clients conn)]
      (http/send! ch data))))

(defn send-to!
  "Send a message to a specific client using connection's encoder."
  [conn ch msg]
  (let [encoder (:encoder conn)]
    (http/send! ch (p/encode encoder msg))))

;; =============================================================================
;; Message Handlers
;; =============================================================================

(defn handle-emit!
  "Handle incoming stream emit from client."
  [conn wire-id data client-seq]
  (when-let [wire (get @(:wires conn) wire-id)]
    ;; Track sequence numbers for gap detection (if client sends seq)
    (when client-seq
      (let [last-seq @(:last-seq-received wire)
            expected (inc last-seq)
            gap (when (and (pos? last-seq) (> client-seq expected))
                  (- client-seq expected))]
        (when gap
          (println "Rheon: Sequence gap detected on" wire-id
                   "- expected" expected "got" client-seq
                   "- missed" gap "messages"))
        (reset! (:last-seq-received wire) client-seq)))
    ;; Enrich data with sequence number
    (let [enriched-data (if client-seq (assoc data :rheon/seq client-seq) data)]
      ;; Notify all local listeners
      (doseq [handler @(:listeners wire)]
        (handler enriched-data))
      ;; Broadcast to other clients (pass through client seq)
      (broadcast! conn {:op :emit :wire wire-id :seq client-seq :data data}))))

(defn handle-send!
  "Handle incoming discrete request from client."
  [conn ch wire-id req-id data]
  (if-let [wire (get @(:wires conn) wire-id)]
    (do
      ;; Notify flow callbacks about incoming request (for m/observe consumers)
      (doseq [cb @(:request-callbacks wire)]
        (cb {:request data :req-id req-id}))
      (if-let [handler @(:handler wire)]
        (try
          (let [reply (handler data)]
            (send-to! conn ch {:op :reply :wire wire-id :req-id req-id :data reply}))
          (catch Exception e
            (send-to! conn ch {:op :error :wire wire-id :req-id req-id
                               :error (.getMessage e)})))
        (send-to! conn ch {:op :error :wire wire-id :req-id req-id
                           :error "No handler registered"})))
    (send-to! conn ch {:op :error :wire wire-id :req-id req-id
                       :error "Wire not found"})))

(defn handle-reply!
  "Handle incoming reply for a pending discrete request.
   Resolves the Missionary deferred value (m/dfv) which triggers the waiting m/sp."
  [conn wire-id req-id data]
  (when-let [wire (get @(:wires conn) wire-id)]
    (when-let [dfv (get @(:pending wire) req-id)]
      ;; Resolve the deferred value - Missionary will handle the rest
      (dfv data))))

(defn handle-error!
  "Handle incoming error for a pending discrete request."
  [conn wire-id req-id error-msg]
  (when-let [wire (get @(:wires conn) wire-id)]
    (when-let [pending (get @(:pending wire) req-id)]
      (swap! (:pending wire) dissoc req-id)
      (when-let [on-error (:on-error pending)]
        (on-error (ex-info error-msg {:wire wire-id :req-id req-id}))))))

(defn handle-signal!
  "Handle incoming signal update from client."
  [conn wire-id value]
  (when-let [wire (get @(:wires conn) wire-id)]
    ;; Update local value - m/watch automatically notifies all subscribers
    (reset! (:value wire) value)
    ;; Broadcast to other clients
    (broadcast! conn {:op :signal :wire wire-id :data value})))

(defn handle-watch!
  "Handle client requesting to watch a signal - send current value."
  [conn ch wire-id]
  (when-let [wire (get @(:wires conn) wire-id)]
    (send-to! conn ch {:op :value :wire wire-id :data @(:value wire)})))

(defn handle-message!
  "Route incoming WebSocket message to appropriate handler."
  [conn ch msg]
  (let [{:keys [op wire data req-id error seq]} msg]
    (case op
      :emit    (handle-emit! conn wire data seq)
      :send    (handle-send! conn ch wire req-id data)
      :reply   (handle-reply! conn wire req-id data)
      :error   (handle-error! conn wire req-id error)
      :signal  (handle-signal! conn wire data)
      :watch   (handle-watch! conn ch wire)
      nil)))

;; =============================================================================
;; WebSocket Handler
;; =============================================================================

(defn make-ws-handler
  "Create an http-kit WebSocket handler for the connection."
  [conn]
  (let [encoder (:encoder conn)]
    (fn [req]
      (http/as-channel req
        {:on-open
         (fn [ch]
           (println "WS: Client connected")
           (swap! (:clients conn) conj ch))

         :on-close
         (fn [ch _status]
           (println "WS: Client disconnected")
           (swap! (:clients conn) disj ch))

         :on-receive
         (fn [ch data]
           (println "WS: Received" (if (string? data) (subs data 0 (min 100 (count data))) "<binary>"))
           (try
             (let [msg (p/decode encoder data)]
               (println "WS: Decoded ->" (pr-str msg))
               (handle-message! conn ch msg))
             (catch Exception e
               (println "WebSocket decode error:" (.getMessage e))
               (.printStackTrace e))))}))))

;; =============================================================================
;; Wire Management
;; =============================================================================

(defn- get-or-create-wire!
  "Get existing wire or create new one. Throws if type mismatch."
  [conn wire-id wire-type constructor]
  (let [wires-atom (:wires conn)]
    (if-let [existing (get @wires-atom wire-id)]
      (let [existing-type (:wire-type existing)]
        (if (= existing-type wire-type)
          existing
          (throw (ex-info (str "Wire " wire-id " already exists as " existing-type
                               ", cannot create as " wire-type)
                          {:wire-id wire-id
                           :existing-type existing-type
                           :requested-type wire-type}))))
      (let [new-wire (constructor)]
        (swap! wires-atom assoc wire-id new-wire)
        new-wire))))

;; =============================================================================
;; IConnection Implementation
;; =============================================================================

(extend-type WSConnection
  p/IConnection
  (stream [conn wire-id]
    (get-or-create-wire! conn wire-id :stream
                         #(make-stream-wire wire-id conn)))

  (discrete [conn wire-id]
    (get-or-create-wire! conn wire-id :discrete
                         #(make-discrete-wire wire-id conn)))

  (signal [conn wire-id initial-value]
    (get-or-create-wire! conn wire-id :signal
                         #(make-signal-wire wire-id conn initial-value)))

  p/ICloseable
  (close! [conn]
    (reset! (:closed? conn) true)
    ;; Close all client connections
    (doseq [ch @(:clients conn)]
      (http/close ch))
    (reset! (:clients conn) #{})
    (reset! (:wires conn) {})
    ;; Stop HTTP server
    (when-let [stop-fn (:server conn)]
      (stop-fn))
    nil))

;; =============================================================================
;; StreamWire Protocol Implementations
;; =============================================================================

(extend-type StreamWire
  p/IEmit
  (emit! [wire data]
    (let [conn (:conn wire)
          wire-id (:id wire)
          seq-num (swap! (:seq-counter wire) inc)
          enriched-data (assoc data :rheon/seq seq-num)]
      ;; Notify all local listeners with enriched data
      (doseq [handler @(:listeners wire)]
        (handler enriched-data))
      ;; Notify flow callbacks (for m/observe consumers)
      (doseq [cb @(:flow-callbacks wire)]
        (cb enriched-data))
      ;; Broadcast to all clients with sequence number
      (broadcast! conn {:op :emit :wire wire-id :seq seq-num :data data}))
    nil)

  p/IListen
  (listen [wire handler]
    ;; Add handler to listeners
    (swap! (:listeners wire) conj handler)
    (->Subscription wire handler :listen nil))

  p/IFlow
  (->flow [wire]
    ;; Returns a Missionary discrete flow that emits on each emit!
    (m/observe
     (fn [emit-fn]
       ;; Register callback
       (swap! (:flow-callbacks wire) conj emit-fn)
       ;; Return cleanup function
       (fn []
         (swap! (:flow-callbacks wire) disj emit-fn))))))

;; =============================================================================
;; DiscreteWire Protocol Implementations
;; =============================================================================

(extend-type DiscreteWire
  p/ISend
  (send! [wire data opts]
    (let [conn (:conn wire)
          wire-id (:id wire)
          req-id (str (random-uuid))
          {:keys [on-reply on-error timeout-ms]} opts
          timeout (or timeout-ms 10000)
          ;; Create Missionary deferred value for the response
          dfv (m/dfv)]
      ;; Store deferred value (not callbacks) - internal Missionary type
      (when (or on-reply on-error)
        (swap! (:pending wire) assoc req-id dfv))
      ;; Broadcast request
      (broadcast! conn {:op :send :wire wire-id :req-id req-id :data data})
      ;; Use Missionary for async wait with timeout - user sees only callbacks
      ;; Missionary tasks are functions: (task success-cb failure-cb) -> cancel-fn
      (when (or on-reply on-error)
        ((m/sp
           (try
             (let [result (m/? (m/timeout timeout dfv))]
               (swap! (:pending wire) dissoc req-id)
               (when on-reply (on-reply result)))
             (catch Exception _
               (swap! (:pending wire) dissoc req-id)
               (when on-error
                 (on-error (ex-info "Request timeout" {:wire wire-id :timeout-ms timeout}))))))
         (fn [_] nil)   ;; success callback (called when sp completes)
         (fn [_] nil))))  ;; failure callback (shouldn't happen, caught internally)
    nil)

  p/IReply
  (reply! [wire handler]
    (reset! (:handler wire) handler)
    (->Subscription wire handler :reply nil))

  p/IFlow
  (->flow [wire]
    ;; Returns a Missionary discrete flow that emits on each incoming request
    (m/observe
     (fn [emit-fn]
       ;; Register callback
       (swap! (:request-callbacks wire) conj emit-fn)
       ;; Return cleanup function
       (fn []
         (swap! (:request-callbacks wire) disj emit-fn))))))

;; =============================================================================
;; SignalWire Protocol Implementations
;; =============================================================================

(extend-type SignalWire
  p/ISignal
  (signal! [wire value]
    (let [conn (:conn wire)
          wire-id (:id wire)]
      ;; Update local value - m/watch will automatically notify watchers
      (reset! (:value wire) value)
      ;; Broadcast to all clients
      (broadcast! conn {:op :signal :wire wire-id :data value}))
    nil)

  p/IWatch
  (watch [wire handler]
    ;; Use Missionary m/watch internally - user only sees their callback
    ;; m/watch wraps an atom and emits on every change
    ;; Missionary flows/tasks are functions: (task success-cb failure-cb) -> cancel-fn
    (let [flow (m/reduce (fn [_ v] (handler v) nil) nil (m/watch (:value wire)))
          cancel (flow (fn [_] nil) (fn [_] nil))]
      (->Subscription wire handler :watch cancel)))

  p/IFlow
  (->flow [wire]
    ;; Returns a Missionary continuous flow that tracks the signal value.
    ;; m/watch is Missionary's built-in atom observation - perfect for signals.
    ;; Emits current value immediately, then on each change.
    (m/watch (:value wire))))

;; =============================================================================
;; Subscription Implementation
;; =============================================================================

(extend-type Subscription
  p/ISubscription
  (unsubscribe! [sub]
    (let [{:keys [wire handler type cancel-fn]} sub]
      ;; Call Missionary cancel function if present (for m/watch, m/dfv, etc.)
      (when cancel-fn
        (cancel-fn))
      ;; Clean up wire state
      (case type
        :listen (swap! (:listeners wire)
                       (fn [handlers] (vec (remove #{handler} handlers))))
        :reply  (reset! (:handler wire) nil)
        :watch  nil))
    nil))

;; =============================================================================
;; Connection Factory
;; =============================================================================

(defn connection
  "Create a WebSocket server connection.

   Args:
     opts - Map with:
            :port     - HTTP port (default 8080)
            :encoding - :json or :msgpack (default :json for browser compatibility)
            :encoder  - Custom IEncoder (takes priority over :encoding)

   Returns:
     A WSConnection that can be used to create wires.

   Example:
     (def conn (connection {:transport :ws-server :port 8080}))
     (def conn (connection {:transport :ws-server :port 8080 :encoding :msgpack}))
     (def conn (connection {:transport :ws-server :encoder my-encoder}))"
  [opts]
  (let [port (or (:port opts) 8080)
        encoder (resolve-encoder opts)
        conn (->WSConnection (atom {}) (atom #{}) (atom false) nil encoder)
        handler (make-ws-handler conn)
        server (http/run-server handler {:port port})]
    (assoc conn :server server)))
