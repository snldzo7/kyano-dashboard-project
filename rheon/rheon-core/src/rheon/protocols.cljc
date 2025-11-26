(ns rheon.protocols
  "Rheon protocols - minimal additions to Clojure's standard protocols.

   We reuse:
   - clojure.lang.IDeref     - @signal dereferencing
   - clojure.lang.IWatchable - add-watch/remove-watch on signals
   - java.io.Closeable       - with-open for connections
   - clojure.lang.ILookup    - (:wire conn) access

   We add:
   - IGauge - Transport abstraction (sente, kafka, mqtt, mem)
   - IWire  - Wire operations (send!, on-message, etc.)")

;; =============================================================================
;; IGauge - Transport Abstraction
;; =============================================================================
;; Like wire gauge in electrical engineering - determines capacity/characteristics

(defprotocol IGauge
  "Transport abstraction - pluggable backends for network communication.

   Each gauge implementation handles the actual transport:
   - :mem   - In-memory (testing)
   - :sente - WebSocket via Sente
   - :kafka - Apache Kafka
   - :mqtt  - MQTT protocol"

  (gauge-connect! [gauge uri opts]
    "Establish a connection using this gauge.
     Returns a connection state map that will be stored in the Connection.")

  (gauge-listen! [gauge opts]
    "Start a server listening for connections.
     Returns a server state map.")

  (gauge-send! [gauge conn-state wire data opts]
    "Send data on the specified wire through this gauge.
     conn-state is the map returned by gauge-connect!
     wire is a keyword like :mouse
     data is the payload (will be serialized by gauge)
     opts may include :on-ack, :on-error, etc.")

  (gauge-request! [gauge conn-state wire data opts]
    "Send a request and expect a reply.
     opts must include :on-reply callback and may include :timeout-ms")

  (gauge-subscribe! [gauge conn-state wire handler]
    "Subscribe to messages on a wire.
     handler is (fn [data] ...) called for each message.
     Returns a subscription that can be passed to gauge-unsubscribe!")

  (gauge-on-request! [gauge conn-state wire handler]
    "Register a request handler for a wire.
     handler is (fn [data reply!] ...) where reply! sends the response.
     Returns a subscription that can be passed to gauge-unsubscribe!")

  (gauge-unsubscribe! [gauge subscription]
    "Remove a subscription created by gauge-subscribe! or gauge-on-request!")

  (gauge-close! [gauge conn-state]
    "Close a connection and release resources."))

;; =============================================================================
;; IGaugeInfo - Gauge Metadata (optional)
;; =============================================================================

(defprotocol IGaugeInfo
  "Optional protocol for gauge metadata and discovery."

  (gauge-name [gauge]
    "Return the gauge's keyword identifier, e.g. :sente, :kafka")

  (gauge-description [gauge]
    "Human-readable description of the gauge")

  (gauge-requires [gauge]
    "Vector of dependency coordinates this gauge requires, e.g.
     [\"com.taoensso/sente\" \"1.19.2\"]")

  (gauge-options [gauge]
    "Map of supported options with their descriptions")

  (gauge-capabilities [gauge]
    "Set of capabilities this gauge supports, e.g.
     #{:bidirectional :broadcast :request-reply :persistent}"))

;; =============================================================================
;; IConnection - Connection Operations
;; =============================================================================

(defprotocol IConnection
  "Operations on a Rheon connection."

  (send! [conn wire data] [conn wire data opts]
    "Send data on a wire through this connection.")

  (request! [conn wire data opts]
    "Send a request and receive a reply via callback.")

  (on-message [conn wire handler]
    "Subscribe to messages on a wire. Returns subscription.")

  (on-request [conn wire handler]
    "Handle requests on a wire. handler is (fn [data reply!] ...). Returns subscription.")

  (unsubscribe! [conn subscription]
    "Remove a subscription.")

  (configure! [conn wire opts]
    "Configure wire-specific options (mode, buffer, throttle, etc.)"))

;; =============================================================================
;; IServer - Server Operations
;; =============================================================================

(defprotocol IServer
  "Operations on a Rheon server."

  (on-client [server handler]
    "Register handler for new client connections.
     handler is (fn [conn] ...) called when a client connects."))
