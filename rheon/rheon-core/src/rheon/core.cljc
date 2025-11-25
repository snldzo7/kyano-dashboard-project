(ns rheon.core
  "Rheon - Network Wire Abstraction

   Main public API for Rheon. Import this namespace to use Rheon.

   Quick Start:
     ;; Server
     (def server (listen! {:port 9092}))
     (on-client server
       (fn [conn]
         (on-request conn :mouse
           (fn [{:keys [x y t]} reply!]
             (reply! {:gap (- (System/currentTimeMillis) t)})))))

     ;; Client
     (def conn (connect! \"mem://localhost:9092\"))
     (request! conn :mouse {:x 100 :y 200 :t (System/currentTimeMillis)}
               {:on-reply (fn [{:keys [gap]}] (println \"Gap:\" gap))})"
  (:require [rheon.protocols :as p]
            [rheon.defaults :as defaults]
            [rheon.conn :as conn]
            [rheon.signal :as sig]
            [rheon.gauge.mem :as mem-gauge]))

;; =============================================================================
;; Gauge Registry
;; =============================================================================

;; Registry of available gauges by keyword
(defonce ^:private gauges (atom {:mem mem-gauge/gauge}))

(defn register-gauge!
  "Register a gauge implementation.

   Args:
     k     - Keyword identifier (e.g., :sente, :kafka)
     gauge - IGauge implementation"
  [k gauge]
  (swap! gauges assoc k gauge))

(defn available-gauges
  "Return set of available gauge keywords."
  []
  (set (keys @gauges)))

(defn get-gauge
  "Get gauge by keyword. Throws if not found."
  [k]
  (or (get @gauges k)
      (throw (ex-info (str "Unknown gauge: " k
                           ". Available: " (available-gauges))
                      {:gauge k :available (available-gauges)}))))

;; =============================================================================
;; Connection Functions
;; =============================================================================

(defn connect!
  "Connect to a Rheon server.

   Args:
     uri  - Connection URI (e.g., \"mem://localhost:9092\")
     opts - Optional map:
            :gauge - Gauge to use (default: from URI or defaults/gauge)

   Returns:
     A Connection that can be used with send!, request!, etc.
     Implements Closeable for use with with-open.

   Example:
     (def conn (connect! \"mem://localhost:9092\"))
     (with-open [conn (connect! \"mem://localhost:9092\")]
       (send! conn :mouse {:x 100 :y 200}))"
  ([uri] (connect! uri {}))
  ([uri opts]
   (let [;; Determine gauge from opts, URI scheme, or default
         gauge-key (or (:gauge opts)
                       (cond
                         (clojure.string/starts-with? uri "mem://") :mem
                         (clojure.string/starts-with? uri "ws://") :sente
                         (clojure.string/starts-with? uri "wss://") :sente
                         (clojure.string/starts-with? uri "kafka://") :kafka
                         (clojure.string/starts-with? uri "mqtt://") :mqtt
                         :else defaults/gauge))
         gauge (get-gauge gauge-key)
         gauge-state (p/gauge-connect! gauge uri opts)]
     (conn/make-connection gauge gauge-state opts))))

(defn listen!
  "Start a Rheon server.

   Args:
     opts - Map with:
            :port  - Port to listen on (required)
            :gauge - Gauge to use (default: defaults/gauge)

   Returns:
     A Server that can be used with on-client, on-message, etc.

   Example:
     (def server (listen! {:port 9092}))
     (on-client server (fn [conn] ...))"
  [opts]
  (let [gauge-key (or (:gauge opts) defaults/gauge)
        gauge (get-gauge gauge-key)
        gauge-state (p/gauge-listen! gauge opts)]
    (conn/make-server gauge gauge-state opts)))

;; =============================================================================
;; Wire Operations (re-exported from protocols)
;; =============================================================================

(defn send!
  "Send data on a wire.

   Args:
     conn - Connection or Server
     wire - Wire keyword (e.g., :mouse, :status)
     data - Data to send (will be serialized by gauge)
     opts - Optional map with :on-ack, :on-error callbacks

   Example:
     (send! conn :mouse {:x 100 :y 200})"
  ([conn wire data]
   (p/send! conn wire data))
  ([conn wire data opts]
   (p/send! conn wire data opts)))

(defn request!
  "Send a request and receive a reply.

   Args:
     conn - Connection
     wire - Wire keyword
     data - Request data
     opts - Map with:
            :on-reply   - (fn [reply] ...) called with response
            :on-error   - (fn [error] ...) called on failure
            :timeout-ms - Timeout in milliseconds (default: 5000)

   Example:
     (request! conn :clock {:t (now)}
               {:on-reply (fn [{:keys [gap]}] (println \"Gap:\" gap))})"
  [conn wire data opts]
  (p/request! conn wire data opts))

(defn on-message
  "Subscribe to messages on a wire.

   Args:
     conn    - Connection or Server
     wire    - Wire keyword
     handler - (fn [data] ...) called for each message

   Returns:
     Subscription that can be passed to unsubscribe!

   Example:
     (on-message conn :mouse
       (fn [{:keys [x y]}]
         (println \"Mouse at\" x y)))"
  [conn wire handler]
  (p/on-message conn wire handler))

(defn on-request
  "Handle requests on a wire.

   Args:
     conn    - Connection or Server
     wire    - Wire keyword
     handler - (fn [data reply!] ...) where reply! sends response

   Returns:
     Subscription that can be passed to unsubscribe!

   Example:
     (on-request conn :clock
       (fn [{:keys [t]} reply!]
         (reply! {:gap (- (now) t)})))"
  [conn wire handler]
  (p/on-request conn wire handler))

(defn on-client
  "Register handler for new client connections.

   Args:
     server  - Server from listen!
     handler - (fn [conn] ...) called when client connects

   Example:
     (on-client server
       (fn [conn]
         (on-request conn :ping
           (fn [_ reply!] (reply! :pong)))))"
  [server handler]
  ;; Wrap the handler to convert gauge-state to Connection
  (let [gauge (:gauge server)
        wrapped-handler (fn [client-gauge-state]
                          (let [client-conn (conn/make-connection gauge client-gauge-state {})]
                            (handler client-conn)))]
    (p/on-client server wrapped-handler)))

(defn unsubscribe!
  "Remove a subscription.

   Args:
     conn         - Connection or Server
     subscription - Value returned by on-message or on-request"
  [conn subscription]
  (p/unsubscribe! conn subscription))

(defn configure!
  "Configure wire-specific options.

   Args:
     conn - Connection
     wire - Wire keyword
     opts - Map with:
            :mode        - :discrete, :continuous, or :signal
            :buffer-size - Buffer size for messages
            :throttle-ms - Throttle outgoing messages
            :drop-policy - :oldest, :newest, or :block

   Example:
     (configure! conn :mouse {:mode :continuous :throttle-ms 50})"
  [conn wire opts]
  (p/configure! conn wire opts))

;; =============================================================================
;; Signal
;; =============================================================================

(defn signal
  "Create a Signal - a wire that behaves like an atom.

   The signal subscribes to the wire and updates when messages arrive.
   Supports @signal for current value and add-watch for reactivity.

   Args:
     conn          - Connection
     wire          - Wire keyword
     initial-value - Value before first message

   Returns:
     Signal implementing IDeref and IWatchable

   Example:
     (def status (signal conn :status :disconnected))
     @status                              ;; => :disconnected
     (add-watch status :ui render!)
     ;; After server sends :connected on :status wire:
     @status                              ;; => :connected"
  [conn wire initial-value]
  (sig/make-signal conn wire initial-value))

;; =============================================================================
;; Gauge Discovery
;; =============================================================================

(defn gauge-info
  "Get information about a gauge.

   Returns map with :name, :description, :requires, :options
   or nil if gauge doesn't implement IGaugeInfo."
  [gauge-key]
  (when-let [gauge (get @gauges gauge-key)]
    (when (satisfies? p/IGaugeInfo gauge)
      {:name (p/gauge-name gauge)
       :description (p/gauge-description gauge)
       :requires (p/gauge-requires gauge)
       :options (p/gauge-options gauge)})))
