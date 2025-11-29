(ns rheon.protocols
  "Rheon v2 Protocols - Wire-as-Value Architecture

   Core insight: The network boundary should be invisible to application code.

   Three wire types:
   - Stream   - Continuous flow (emit!/listen)
   - Discrete - Request/response (send!/reply!)
   - Signal   - Current value (signal!/watch)")

;; =============================================================================
;; Connection - Creates typed wires
;; =============================================================================

(defprotocol IConnection
  "Connection to a transport. Creates wires from refs."

  (stream [conn ref]
    "Create or get a Stream wire from a wire-ref.
     ref should be a map with at least :wire-id, optionally :spec and other fields.
     Returns a StreamWire.")

  (discrete [conn ref]
    "Create or get a Discrete wire from a wire-ref.
     ref should be a map with at least :wire-id, optionally :spec and other fields.
     Returns a DiscreteWire.")

  (signal [conn ref]
    "Create or get a Signal wire from a wire-ref.
     ref should be a map with at least :wire-id and :initial, optionally :spec.
     Returns a SignalWire."))

;; =============================================================================
;; Stream Operations - Continuous flow
;; =============================================================================

(defprotocol IEmit
  "Emit values onto a stream."

  (emit! [wire data]
    "Emit data onto the stream. Fire and forget.
     Returns nil immediately (async)."))

(defprotocol IListen
  "Listen to stream emissions."

  (listen [wire handler]
    "Listen to emissions on this stream.
     handler: (fn [data] ...) called for each emitted value.
     Returns a subscription for unsubscribe."))

;; =============================================================================
;; Discrete Operations - Request/Response
;; =============================================================================

(defprotocol ISend
  "Send requests expecting replies."

  (send! [wire data opts]
    "Send a request and expect a reply.
     opts: {:timeout-ms n, :on-reply (fn [reply] ...), :on-error (fn [err] ...)}
     Returns nil."))

(defprotocol IReply
  "Handle requests and return replies."

  (reply! [wire handler]
    "Register a handler for incoming requests.
     handler: (fn [data] reply-value) - return value IS the reply.
     Returns a subscription for unsubscribe."))

;; =============================================================================
;; Signal Operations - Current value
;; =============================================================================

(defprotocol ISignal
  "Set signal values."

  (signal! [wire value]
    "Set the current signal value. All watchers notified.
     Returns nil."))

(defprotocol IWatch
  "Watch signal values."

  (watch [wire handler]
    "Watch this signal's value.
     handler: (fn [value] ...) called immediately with current value,
              then on each signal! call.
     Returns a subscription for unsubscribe."))

;; =============================================================================
;; Lifecycle
;; =============================================================================

(defprotocol ISubscription
  "Subscription management."

  (unsubscribe! [subscription]
    "Cancel the subscription, stop receiving updates."))

(defprotocol ICloseable
  "Resource cleanup."

  (close! [x]
    "Close the connection or wire, release resources."))

;; =============================================================================
;; Flow - Raw Missionary Access
;; =============================================================================

(defprotocol IFlow
  "Access raw Missionary flow for advanced composition.

   This gives power users direct access to Missionary's flow abstraction,
   enabling powerful declarative transformations:

   Example:
     (require '[missionary.core :as m])

     ;; Get raw flow from a stream
     (def mouse-flow (->flow mouse))

     ;; Compose with Missionary operators
     (->> mouse-flow
          (m/eduction (filter #(> (:x %) 100)))
          (m/sample 16)
          (m/reduce (fn [_ v] (render! v)) nil))"

  (->flow [wire]
    "Get the underlying Missionary flow for this wire.

     For Stream:   emits each value passed to emit!
     For Signal:   emits current value, then each value on signal!
     For Discrete: emits each incoming request

     Returns a Missionary continuous/discrete flow depending on wire type."))

;; =============================================================================
;; Wire Reference - Get wire-ref from live wire
;; =============================================================================

(defprotocol IWire
  "Get the wire-ref (data) that describes this live wire.

   Wire-refs are pure data. A live wire is an instantiated wire-ref.
   This protocol lets you extract the original ref from a live wire,
   enabling serialization and inspection.

   Example:
     (def mouse (r/wire conn {:wire-id :mouse :type :stream :spec [:map [:x :int] [:y :int]]}))
     (wire-ref mouse)
     ;; => {:wire-id :mouse :type :stream :spec [:map [:x :int] [:y :int]]}"

  (wire-ref [wire]
    "Get the wire-ref that describes this wire.
     Returns a map with :wire-id, :type, and optional :spec, :initial, :opts, :conn."))

;; =============================================================================
;; Encoder - Wire Format Abstraction (Internal)
;; =============================================================================

(defprotocol IEncoder
  "Wire format encoding/decoding.

   This is an internal protocol used by transports. Users don't need to
   interact with it directly - Transit is used by default.

   If you need a custom encoding format, implement this protocol and pass
   your encoder via the :encoder option when creating a connection.

   Example:
     (defrecord MyEncoder []
       IEncoder
       (encode [_ msg] (my-encode msg))
       (decode [_ data] (my-decode data)))

     (connection {:transport :ws-server
                  :encoder (->MyEncoder)})"

  (encode [encoder msg]
    "Encode a message (Clojure data) to wire format (string or bytes).")

  (decode [encoder data]
    "Decode wire format (string or bytes) to a message (Clojure data)."))
