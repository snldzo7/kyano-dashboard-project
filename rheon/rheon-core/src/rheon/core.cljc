(ns rheon.core
  "Rheon v2 - Wire-as-Value Architecture

   Core insight: The network boundary should be invisible to application code.

   Quick Start:
     ;; Create connection
     (def conn (connection {:transport :mem}))

     ;; Stream - continuous flow
     (def mouse (stream :mouse conn))
     (emit! mouse {:x 100 :y 200})
     (listen mouse (fn [data] (render! data)))

     ;; Discrete - request/response
     (def clock (discrete :clock conn))
     (reply! clock (fn [data] {:time (now)}))
     (send! clock {:t (now)} {:on-reply println})

     ;; Signal - current value
     (def status (signal :status conn :starting))
     (signal! status :running)
     (watch status (fn [v] (update-ui! v)))"
  (:require [rheon.protocols :as p]
            [rheon.spec :as spec]
            [rheon.transport.mem :as mem]
            #?(:clj [rheon.transport.ws-server :as ws-server])
            #?(:clj [rheon.transport.ws-client :as ws-client])
            #?(:cljs [rheon.transport.ws-client :as ws-client])))

;; =============================================================================
;; Transport Registry
;; =============================================================================

(defonce ^:private transports
  (atom (merge {:mem mem/connection}
               #?(:clj {:ws-server ws-server/connection
                        :ws-client ws-client/connection
                        :ws ws-client/connection})  ;; :ws alias for client
               #?(:cljs {:ws-client ws-client/connection
                         :ws ws-client/connection}))))  ;; :ws alias for browser

(defn register-transport!
  "Register a transport connection factory.

   Args:
     transport-key - Keyword identifier (e.g., :sente, :kafka)
     factory       - Function that creates a connection"
  [transport-key factory]
  (swap! transports assoc transport-key factory))

;; =============================================================================
;; Hub (Memory Transport Only)
;; =============================================================================

(defn create-hub
  "Create a memory hub for multi-participant communication.

   Multiple connections can attach to a hub, sharing wire state.
   This makes memory transport behave exactly like WebSocket:
   - emit on one connection reaches all listeners on all connections
   - signal updates are visible to all watchers across connections

   Example:
     (def hub (create-hub))
     (def server (connection {:transport :mem :hub hub}))
     (def client (connection {:transport :mem :hub hub}))

     (def mouse-server (stream :mouse server))
     (def mouse-client (stream :mouse client))

     (listen mouse-client (fn [data] (println \"Client got:\" data)))
     (emit! mouse-server {:x 100})  ;; Client receives this!"
  []
  (mem/create-hub))

;; =============================================================================
;; Connection
;; =============================================================================

(defn connection
  "Create a connection with the specified transport.

   Args:
     opts - Map with:
            :transport - Transport keyword (:mem, :sente, etc.)
            ... transport-specific options

   Returns:
     A Connection that can be used to create wires.

   Examples:
     (def conn (connection {:transport :mem}))
     (def conn (connection {:transport :sente :port 9092}))"
  [opts]
  (let [transport-key (or (:transport opts) :mem)
        factory (get @transports transport-key)]
    (if factory
      (factory opts)
      (throw (ex-info (str "Unknown transport: " transport-key
                           ". Available: " (keys @transports))
                      {:transport transport-key
                       :available (keys @transports)})))))

;; =============================================================================
;; Wire-Ref (The Primitive)
;; =============================================================================

(defn wire
  "Instantiate a wire from a wire-ref.

   Wire-refs are data. This function brings them to life.

   Args:
     conn - Connection (optional if ref contains :conn)
     ref  - Wire reference map with:
            :wire-id  - Keyword identifier (required)
            :type     - :stream, :discrete, or :signal (required)
            :spec     - Schema for validation (optional)
            :initial  - Initial value for signals (optional)
            :opts     - Additional options (optional)
            :conn     - Connection ref for auto-connect (optional)

   Returns:
     A live wire (StreamWire, DiscreteWire, or SignalWire).

   Examples:
     ;; Basic usage
     (wire conn {:wire-id :mouse :type :stream})

     ;; With spec
     (wire conn {:wire-id :mouse :type :stream :spec [:map [:x :int] [:y :int]]})

     ;; Signal with initial value
     (wire conn {:wire-id :status :type :signal :initial {:state :starting}})

     ;; Auto-connect (when ref has :conn)
     (wire {:wire-id :mouse :type :stream :conn {:transport :ws-client :url \"ws://...\"}})"
  ([ref]
   ;; No conn provided - ref must have :conn
   (if-let [conn-ref (:conn ref)]
     (wire (connection conn-ref) ref)
     (throw (ex-info "Wire ref must have :conn when no connection provided"
                     {:ref ref}))))
  ([conn ref]
   (spec/validate-wire-ref! ref)
   (let [{:keys [type]} ref]
     (case type
       :stream   (p/stream conn ref)
       :discrete (p/discrete conn ref)
       :signal   (p/signal conn ref)
       (throw (ex-info (str "Unknown wire type: " type)
                       {:ref ref :type type}))))))

(defn wire-ref
  "Get the wire-ref (data) that describes a live wire.

   This is the inverse of `wire`: given a live wire, returns the
   pure data that describes it.

   Args:
     wire - A live wire (StreamWire, DiscreteWire, or SignalWire)

   Returns:
     A map with :wire-id, :type, and optional :spec, :initial, :opts, :conn.

   Example:
     (def mouse (wire conn {:wire-id :mouse :type :stream :spec [:map [:x :int]]}))
     (wire-ref mouse)
     ;; => {:wire-id :mouse :type :stream :spec [:map [:x :int]]}"
  [wire]
  (p/wire-ref wire))

;; =============================================================================
;; Wire Creation (delegated to connection)
;; =============================================================================

(defn stream
  "Create or get a Stream wire for continuous flow.

   Args:
     wire-id - Keyword identifier for the wire
     conn    - Connection from `connection`

   Returns:
     A StreamWire supporting emit!/listen.

   Example:
     (def mouse (stream :mouse conn))
     (emit! mouse {:x 100 :y 200})"
  [wire-id conn]
  (p/stream conn {:wire-id wire-id :type :stream}))

(defn discrete
  "Create or get a Discrete wire for request/response.

   Args:
     wire-id - Keyword identifier for the wire
     conn    - Connection from `connection`

   Returns:
     A DiscreteWire supporting send!/reply!.

   Example:
     (def clock (discrete :clock conn))
     (reply! clock (fn [data] {:time (now)}))"
  [wire-id conn]
  (p/discrete conn {:wire-id wire-id :type :discrete}))

(defn signal
  "Create or get a Signal wire with initial value.

   Args:
     wire-id       - Keyword identifier for the wire
     conn          - Connection from `connection`
     initial-value - Initial value of the signal

   Returns:
     A SignalWire supporting signal!/watch.

   Example:
     (def status (signal :status conn :starting))
     (signal! status :running)"
  [wire-id conn initial-value]
  (p/signal conn {:wire-id wire-id :type :signal :initial initial-value}))

;; =============================================================================
;; Stream Operations
;; =============================================================================

(defn emit!
  "Emit data onto a stream. Fire and forget.

   If the wire was created with a :spec, validates data before emitting.

   Args:
     wire - StreamWire from `stream` or `wire`
     data - Data to emit

   Returns:
     nil (immediately, async).

   Throws:
     ExceptionInfo if data fails spec validation.

   Example:
     (emit! mouse {:x 100 :y 200})"
  [wire data]
  ;; Validate against wire's spec (if any)
  (spec/validate-wire-data! (p/wire-ref wire) :emit data)
  (p/emit! wire data))

;; =============================================================================
;; Backpressure Strategies
;; =============================================================================

(defn- make-sampled-handler
  "Wrap handler to sample at most once per interval-ms.
   Drops intermediate values, keeping only the latest."
  [handler interval-ms]
  (let [last-value (atom nil)
        scheduled? (atom false)]
    (fn [data]
      (reset! last-value data)
      (when-not @scheduled?
        (reset! scheduled? true)
        #?(:clj  (future
                   (Thread/sleep interval-ms)
                   (reset! scheduled? false)
                   (handler @last-value))
           :cljs (js/setTimeout
                  (fn []
                    (reset! scheduled? false)
                    (handler @last-value))
                  interval-ms))))))

(defn- make-buffered-handler
  "Wrap handler to buffer values and flush on interval.
   Calls handler with each buffered value."
  [handler size interval-ms]
  (let [buffer (atom [])
        flush! (fn []
                 (let [items @buffer]
                   (reset! buffer [])
                   (doseq [item items]
                     (handler item))))
        ;; Capture interval for CLJS use (unused in CLJ)
        _flush-interval interval-ms]
    (fn [data]
      (swap! buffer (fn [buf]
                      (let [new-buf (conj buf data)]
                        (if (>= (count new-buf) size)
                          (do (flush!) [])
                          new-buf))))
      ;; Also flush on interval (CLJS only)
      #?(:clj  nil
         :cljs (when (= 1 (count @buffer))
                 (js/setTimeout flush! _flush-interval))))))

(defn- make-latest-handler
  "Wrap handler to keep only the latest value (drop all but most recent).
   Processes on next tick to batch rapid updates."
  [handler]
  (let [latest (atom nil)
        scheduled? (atom false)]
    (fn [data]
      (reset! latest data)
      (when-not @scheduled?
        (reset! scheduled? true)
        #?(:clj  (future
                   (Thread/sleep 0)
                   (reset! scheduled? false)
                   (handler @latest))
           :cljs (js/queueMicrotask
                  (fn []
                    (reset! scheduled? false)
                    (handler @latest))))))))

(defn- wrap-with-backpressure
  "Apply backpressure strategy to handler based on opts."
  [handler opts]
  (case (:backpressure opts)
    :sample (make-sampled-handler handler (or (:interval-ms opts) 16))
    :buffer (make-buffered-handler handler
                                   (or (:size opts) 10)
                                   (or (:interval-ms opts) 100))
    :latest (make-latest-handler handler)
    ;; No backpressure - pass through
    handler))

(defn listen
  "Listen to emissions on a stream.

   Args:
     wire    - StreamWire from `stream`
     handler - (fn [data] ...) called for each emitted value
     opts    - Optional map with:
               :backpressure - Strategy (:sample, :buffer, :latest)
               :interval-ms  - Sampling/flush interval (default 16ms for sample, 100ms for buffer)
               :size         - Buffer size (default 10, only for :buffer)

   Returns:
     A subscription for unsubscribe!.

   Examples:
     ;; Basic - every event
     (listen mouse (fn [{:keys [x y]}] (render! x y)))

     ;; Sample at 60fps (16ms)
     (listen mouse handler {:backpressure :sample :interval-ms 16})

     ;; Buffer up to 10 items, flush every 100ms
     (listen mouse handler {:backpressure :buffer :size 10 :interval-ms 100})

     ;; Keep only latest value (batch rapid updates)
     (listen mouse handler {:backpressure :latest})"
  ([wire handler]
   (p/listen wire handler))
  ([wire handler opts]
   (let [wrapped-handler (wrap-with-backpressure handler opts)]
     (p/listen wire wrapped-handler))))

;; =============================================================================
;; Discrete Operations
;; =============================================================================

(defn send!
  "Send a request and expect a reply.

   If the wire was created with a :spec containing :request,
   validates data before sending.

   Args:
     wire - DiscreteWire from `discrete` or `wire`
     data - Request data
     opts - Map with:
            :timeout-ms - Timeout in milliseconds
            :on-reply   - (fn [reply] ...) called with response
            :on-error   - (fn [error] ...) called on failure

   Returns:
     nil.

   Throws:
     ExceptionInfo if data fails spec validation.

   Example:
     (send! clock {:t (now)}
       {:timeout-ms 5000
        :on-reply (fn [{:keys [gap]}] (println \"Gap:\" gap))})"
  [wire data opts]
  ;; Validate request data against wire's spec (if any)
  (spec/validate-wire-data! (p/wire-ref wire) :send data)
  (p/send! wire data opts))

(defn reply!
  "Register a handler for incoming requests.
   Handler return value IS the reply.

   Args:
     wire    - DiscreteWire from `discrete`
     handler - (fn [data] reply-value)

   Returns:
     A subscription for unsubscribe!.

   Example:
     (reply! clock
       (fn [{:keys [client-time]}]
         {:server-time (now)
          :gap (- (now) client-time)}))"
  [wire handler]
  (p/reply! wire handler))

;; =============================================================================
;; Signal Operations
;; =============================================================================

(defn signal!
  "Set the current signal value. All watchers notified.

   If the wire was created with a :spec, validates value before setting.

   Args:
     wire  - SignalWire from `signal` or `wire`
     value - New value

   Returns:
     nil.

   Throws:
     ExceptionInfo if value fails spec validation.

   Example:
     (signal! status {:state :running :uptime 3600})"
  [wire value]
  ;; Validate value against wire's spec (if any)
  (spec/validate-wire-data! (p/wire-ref wire) :signal value)
  (p/signal! wire value))

(defn watch
  "Watch a signal's value.
   Handler called immediately with current value, then on each signal!.

   Args:
     wire    - SignalWire from `signal`
     handler - (fn [value] ...) called with current and future values

   Returns:
     A subscription for unsubscribe!.

   Example:
     (watch status (fn [{:keys [state]}] (update-ui! state)))"
  [wire handler]
  (p/watch wire handler))

;; =============================================================================
;; Lifecycle
;; =============================================================================

(defn unsubscribe!
  "Cancel a subscription, stop receiving updates.

   Args:
     subscription - Value returned by listen, reply!, or watch

   Example:
     (def sub (listen mouse handler))
     (unsubscribe! sub)"
  [subscription]
  (p/unsubscribe! subscription))

(defn close!
  "Close a connection, release resources.

   Args:
     conn - Connection from `connection`

   Example:
     (close! conn)"
  [conn]
  (p/close! conn))

;; =============================================================================
;; Raw Missionary Flow Access
;; =============================================================================

(defn ->flow
  "Get the raw Missionary flow from a wire.

   This exposes the underlying Missionary flow abstraction for advanced users
   who want to use Missionary's powerful flow composition operators.

   Args:
     wire - Any wire type (StreamWire, DiscreteWire, SignalWire)

   Returns:
     For Stream:   Missionary discrete flow that emits on each emit!
     For Discrete: Missionary discrete flow that emits on each incoming request
     For Signal:   Missionary continuous flow that tracks the signal value

   Examples:
     (require '[missionary.core :as m])

     ;; Stream: compose with Missionary operators
     (def mouse-flow (->flow mouse))
     (->> mouse-flow
          (m/eduction (filter #(> (:x %) 100)))
          (m/reduce (fn [_ v] (render! v)) nil))

     ;; Signal: sample at interval
     (def status-flow (->flow status))
     (m/reduce (fn [_ v] (println v)) nil (m/sample 1000 status-flow))"
  [wire]
  (p/->flow wire))
