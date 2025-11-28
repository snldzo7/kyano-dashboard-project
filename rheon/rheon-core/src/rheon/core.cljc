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
                        :ws-client ws-client/connection})
               #?(:cljs {:ws-client ws-client/connection}))))

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
  (p/stream conn wire-id))

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
  (p/discrete conn wire-id))

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
  (p/signal conn wire-id initial-value))

;; =============================================================================
;; Stream Operations
;; =============================================================================

(defn emit!
  "Emit data onto a stream. Fire and forget.

   Args:
     wire - StreamWire from `stream`
     data - Data to emit

   Returns:
     nil (immediately, async).

   Example:
     (emit! mouse {:x 100 :y 200})"
  [wire data]
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

   Args:
     wire - DiscreteWire from `discrete`
     data - Request data
     opts - Map with:
            :timeout-ms - Timeout in milliseconds
            :on-reply   - (fn [reply] ...) called with response
            :on-error   - (fn [error] ...) called on failure

   Returns:
     nil.

   Example:
     (send! clock {:t (now)}
       {:timeout-ms 5000
        :on-reply (fn [{:keys [gap]}] (println \"Gap:\" gap))})"
  [wire data opts]
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

   Args:
     wire  - SignalWire from `signal`
     value - New value

   Returns:
     nil.

   Example:
     (signal! status {:state :running :uptime 3600})"
  [wire value]
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
