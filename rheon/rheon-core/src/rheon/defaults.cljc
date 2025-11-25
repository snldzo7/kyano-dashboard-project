(ns rheon.defaults
  "Rheon defaults - discoverable, overridable configuration.

   All defaults are Vars that can be:
   1. Read directly: rheon.defaults/gauge
   2. Changed globally: (alter-var-root #'rheon.defaults/gauge (constantly :kafka))
   3. Overridden per-connection or per-wire

   Default cascade: wire config > connection config > global defaults")

;; =============================================================================
;; Transport Defaults
;; =============================================================================

(def ^:dynamic gauge
  "Default gauge (transport backend).
   :mem is built-in, others require additional dependencies.

   Available gauges:
   - :mem   - In-memory (built-in, for testing)
   - :sente - WebSocket via Sente (requires rheon-sente)
   - :kafka - Apache Kafka (requires rheon-kafka)
   - :mqtt  - MQTT protocol (requires rheon-mqtt)"
  :mem)

;; =============================================================================
;; Wire Mode Defaults
;; =============================================================================

(def ^:dynamic mode
  "Default wire mode - how messages are delivered.

   Modes:
   - :discrete   - All messages delivered in order (default)
   - :continuous - May drop messages if overwhelmed
   - :signal     - Only latest value matters"
  :discrete)

;; =============================================================================
;; Buffer & Flow Control
;; =============================================================================

(def ^:dynamic buffer-size
  "Default buffer size for wire message queues."
  100)

(def ^:dynamic drop-policy
  "What to do when buffer is full.
   - :oldest - Drop oldest messages
   - :newest - Drop newest messages (incoming)
   - :block  - Block sender until space available"
  :oldest)

;; =============================================================================
;; Timing Defaults
;; =============================================================================

(def ^:dynamic timeout-ms
  "Default timeout for request/reply operations (milliseconds)."
  5000)

(def ^:dynamic throttle-ms
  "Default throttle interval for outgoing messages (milliseconds).
   nil means no throttling."
  nil)

(def ^:dynamic reconnect-delay-ms
  "Initial delay before reconnection attempt (milliseconds)."
  1000)

(def ^:dynamic max-reconnect-delay-ms
  "Maximum delay between reconnection attempts (milliseconds)."
  30000)

(def ^:dynamic heartbeat-interval-ms
  "Interval between heartbeat messages (milliseconds).
   nil means use gauge default."
  nil)

;; =============================================================================
;; Helper Functions
;; =============================================================================

(defn get-default
  "Get a default value by keyword.
   Returns nil if not found."
  [k]
  (case k
    :gauge gauge
    :mode mode
    :buffer-size buffer-size
    :drop-policy drop-policy
    :timeout-ms timeout-ms
    :throttle-ms throttle-ms
    :reconnect-delay-ms reconnect-delay-ms
    :max-reconnect-delay-ms max-reconnect-delay-ms
    :heartbeat-interval-ms heartbeat-interval-ms
    nil))

(defn merge-with-defaults
  "Merge options map with defaults, options taking precedence."
  [opts]
  (merge {:gauge gauge
          :mode mode
          :buffer-size buffer-size
          :drop-policy drop-policy
          :timeout-ms timeout-ms
          :throttle-ms throttle-ms
          :reconnect-delay-ms reconnect-delay-ms
          :max-reconnect-delay-ms max-reconnect-delay-ms
          :heartbeat-interval-ms heartbeat-interval-ms}
         opts))

(defn all-defaults
  "Return a map of all current default values."
  []
  {:gauge gauge
   :mode mode
   :buffer-size buffer-size
   :drop-policy drop-policy
   :timeout-ms timeout-ms
   :throttle-ms throttle-ms
   :reconnect-delay-ms reconnect-delay-ms
   :max-reconnect-delay-ms max-reconnect-delay-ms
   :heartbeat-interval-ms heartbeat-interval-ms})
