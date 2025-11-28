(ns rheon.transport.mem
  "In-memory transport for Rheon v2.

   Perfect for:
   - Testing
   - Same-process communication
   - Local development

   Supports two modes:
   1. Standalone: Single connection, local-only (default)
   2. Hub: Multiple connections share state (like WS)

   Usage:
     ;; Standalone (single participant)
     (def conn (connection))

     ;; Hub mode (multi-participant, like WS)
     (def hub (create-hub))
     (def server (connection {:hub hub}))
     (def client (connection {:hub hub}))
     ;; Now emit on server reaches client listeners!"
  (:require [rheon.protocols :as p]
            [missionary.core :as m]))

;; =============================================================================
;; Hub - Shared state for multi-participant communication
;; =============================================================================

(defrecord MemHub [wires])

(defn create-hub
  "Create a memory hub for multi-participant communication.

   Multiple connections can attach to a hub, sharing wire state.
   This makes memory transport behave exactly like WebSocket:
   - emit on one connection reaches all listeners on all connections
   - signal updates are visible to all watchers across connections

   Example:
     (def hub (create-hub))
     (def server (connection {:hub hub}))
     (def client (connection {:hub hub}))

     (def mouse-server (stream :mouse server))
     (def mouse-client (stream :mouse client))

     (listen mouse-client (fn [data] (println \"Client got:\" data)))
     (emit! mouse-server {:x 100})  ;; Client receives this!"
  []
  (->MemHub (atom {})))

;; =============================================================================
;; Wire Records (defined here to keep extend-type in same namespace)
;; =============================================================================

(defrecord StreamWire [id hub wire-type listeners seq-counter last-seq-received flow-callbacks])
;; flow-callbacks: atom #{emit-fn ...} - functions to call when emitting (for m/observe)

(defrecord DiscreteWire [id hub wire-type handler request-callbacks])
;; request-callbacks: atom #{fn ...} - functions to call on incoming requests (for m/observe)

(defrecord SignalWire [id hub wire-type value watchers])
;; value atom is directly watchable via m/watch

(defrecord Subscription [wire handler type])

;; =============================================================================
;; Wire Constructors
;; =============================================================================

(defn make-stream-wire [id hub]
  (->StreamWire id hub :stream (atom []) (atom 0) (atom 0) (atom #{})))

(defn make-discrete-wire [id hub]
  (->DiscreteWire id hub :discrete (atom nil) (atom #{})))

(defn make-signal-wire [id hub initial-value]
  (->SignalWire id hub :signal (atom initial-value) (atom [])))

;; =============================================================================
;; MemConnection - In-memory connection
;; =============================================================================

(defrecord MemConnection [hub closed?])
;; hub: MemHub (always present - standalone connections get their own hub)
;; closed?: atom boolean

(defn- get-or-create-wire!
  "Get existing wire or create new one. Throws if type mismatch."
  [conn wire-id wire-type constructor]
  (let [hub (:hub conn)
        wires-atom (:wires hub)]
    (if-let [existing (get @wires-atom wire-id)]
      ;; Check type matches
      (let [existing-type (:wire-type existing)]
        (if (= existing-type wire-type)
          existing
          (throw (ex-info (str "Wire " wire-id " already exists as " existing-type
                               ", cannot create as " wire-type)
                          {:wire-id wire-id
                           :existing-type existing-type
                           :requested-type wire-type}))))
      ;; Create new wire
      (let [new-wire (constructor)]
        (swap! wires-atom assoc wire-id new-wire)
        new-wire))))

;; =============================================================================
;; IConnection Implementation
;; =============================================================================

(extend-type MemConnection
  p/IConnection
  (stream [conn wire-id]
    (get-or-create-wire! conn wire-id :stream
                         #(make-stream-wire wire-id (:hub conn))))

  (discrete [conn wire-id]
    (get-or-create-wire! conn wire-id :discrete
                         #(make-discrete-wire wire-id (:hub conn))))

  (signal [conn wire-id initial-value]
    (get-or-create-wire! conn wire-id :signal
                         #(make-signal-wire wire-id (:hub conn) initial-value)))

  p/ICloseable
  (close! [conn]
    (reset! (:closed? conn) true)
    ;; Only clear wires if this is a standalone connection (owns its hub)
    ;; For shared hubs, closing one connection shouldn't affect others
    (reset! (:wires (:hub conn)) {})))

;; =============================================================================
;; StreamWire Protocol Implementations
;; =============================================================================

(extend-type StreamWire
  p/IEmit
  (emit! [wire data]
    (let [seq-num (swap! (:seq-counter wire) inc)
          enriched-data (assoc data :rheon/seq seq-num)]
      ;; Notify ALL listeners (from all connections sharing this hub)
      (doseq [handler @(:listeners wire)]
        (handler enriched-data))
      ;; Notify flow callbacks (for m/observe consumers)
      (doseq [cb @(:flow-callbacks wire)]
        (cb enriched-data)))
    nil)

  p/IListen
  (listen [wire handler]
    (swap! (:listeners wire) conj handler)
    (->Subscription wire handler :listen))

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
    (let [{:keys [on-reply on-error]} opts
          handler @(:handler wire)]
      ;; Notify flow callbacks about incoming request (for m/observe consumers)
      (doseq [cb @(:request-callbacks wire)]
        (cb {:request data :opts opts}))
      (if handler
        ;; Handler returns the reply directly
        (try
          (let [reply (handler data)]
            (when on-reply
              (on-reply reply)))
          (catch #?(:clj Exception :cljs js/Error) e
            (if on-error
              (on-error e)
              (throw e))))
        ;; No handler registered
        (when on-error
          (on-error (ex-info "No handler registered for discrete wire"
                             {:wire-id (:id wire)})))))
    nil)

  p/IReply
  (reply! [wire handler]
    (reset! (:handler wire) handler)
    (->Subscription wire handler :reply))

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
    (reset! (:value wire) value)
    ;; Notify ALL watchers (from all connections sharing this hub)
    (doseq [handler @(:watchers wire)]
      (handler value))
    nil)

  p/IWatch
  (watch [wire handler]
    ;; Call immediately with current value
    (handler @(:value wire))
    ;; Add to watchers
    (swap! (:watchers wire) conj handler)
    (->Subscription wire handler :watch))

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
    (let [{:keys [wire handler type]} sub]
      (case type
        :listen (swap! (:listeners wire)
                       (fn [handlers] (vec (remove #{handler} handlers))))
        :reply  (reset! (:handler wire) nil)
        :watch  (swap! (:watchers wire)
                       (fn [handlers] (vec (remove #{handler} handlers))))))
    nil))

;; =============================================================================
;; Connection Factory
;; =============================================================================

(defn connection
  "Create an in-memory connection.

   Args:
     opts - Optional map with:
            :hub - MemHub for multi-participant mode (optional)

   Without :hub (standalone mode):
     Connection has isolated wire state. Emit only reaches
     listeners on the same connection. Good for simple testing.

   With :hub (multi-participant mode):
     All connections sharing a hub see the same wires.
     Emit reaches listeners on ALL connections attached to the hub.
     This matches WebSocket transport semantics exactly.

   Examples:
     ;; Standalone - isolated
     (def conn (connection))

     ;; Multi-participant - shared state
     (def hub (create-hub))
     (def server (connection {:hub hub}))
     (def client (connection {:hub hub}))"
  ([] (connection {}))
  ([opts]
   (let [hub (or (:hub opts) (create-hub))]
     (->MemConnection hub (atom false)))))
