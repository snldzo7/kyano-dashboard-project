(ns rheon.transport.ws-client
  "WebSocket client transport for Rheon (Clojure JVM).

   Mirrors the ClojureScript client transport API exactly.
   Uses Java 11+ WebSocket API with Transit encoding by default.

   Usage:
     (def conn (connection {:transport :ws-client :url \"ws://localhost:8084\"}))
     (def mouse (r/stream :mouse conn))
     (r/emit! mouse {:x 100 :y 200})"
  (:require [rheon.protocols :as p]
            [rheon.encoder :as enc]
            [missionary.core :as m])
  (:import [java.net URI]
           [java.net.http HttpClient WebSocket WebSocket$Listener]))

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
;; Forward Declarations
;; =============================================================================

(declare send-or-buffer!)
(declare connect!)

;; =============================================================================
;; Subscription Record
;; =============================================================================

(defrecord Subscription [wire-id type unsub-fn]
  p/ISubscription
  (unsubscribe! [_]
    (when unsub-fn (unsub-fn))))

;; =============================================================================
;; Message Buffering
;; =============================================================================

(def ^:private max-buffer-size 1000)

;; =============================================================================
;; Stream Wire
;; =============================================================================

(defrecord StreamWire [id conn ref listeners seq-counter last-seq-received flow-callbacks]
  p/IEmit
  (emit! [_ data]
    (let [seq-num (swap! seq-counter inc)
          enriched-data (assoc data :rheon/seq seq-num)]
      ;; Notify flow callbacks (for m/observe consumers)
      (doseq [cb @flow-callbacks]
        (cb enriched-data))
      (send-or-buffer! conn {:op :emit :wire id :seq seq-num :data data})))

  p/IListen
  (listen [_ handler]
    (swap! listeners conj handler)
    (->Subscription id :stream
                    #(swap! listeners (fn [ls] (vec (remove #{handler} ls))))))

  p/IFlow
  (->flow [_]
    ;; Returns a Missionary discrete flow that emits on each emit!
    (m/observe
     (fn [emit-fn]
       ;; Register callback
       (swap! flow-callbacks conj emit-fn)
       ;; Return cleanup function
       (fn []
         (swap! flow-callbacks disj emit-fn))))))

;; =============================================================================
;; Discrete Wire
;; =============================================================================

(defrecord DiscreteWire [id conn ref handler pending request-callbacks]
  p/ISend
  (send! [_ data opts]
    (let [req-id (str (java.util.UUID/randomUUID))
          {:keys [on-reply on-error timeout-ms]} opts
          msg {:op :send :wire id :req-id req-id :data data}]
      ;; Store pending request before sending
      (swap! pending assoc req-id {:on-reply on-reply :on-error on-error})
      ;; Try to send (will buffer if disconnected)
      (let [sent? (send-or-buffer! conn msg)]
        ;; Handle timeout
        (when timeout-ms
          (future
            (Thread/sleep timeout-ms)
            (when-let [p (get @pending req-id)]
              (swap! pending dissoc req-id)
              (when-let [on-err (:on-error p)]
                (on-err {:error (if sent? "Request timeout" "Request timeout (was buffered)")}))))))))

  p/IReply
  (reply! [_ h]
    (reset! handler h)
    (->Subscription id :discrete #(reset! handler nil)))

  p/IFlow
  (->flow [_]
    ;; Returns a Missionary discrete flow that emits on each incoming request
    (m/observe
     (fn [emit-fn]
       ;; Register callback
       (swap! request-callbacks conj emit-fn)
       ;; Return cleanup function
       (fn []
         (swap! request-callbacks disj emit-fn))))))

;; =============================================================================
;; Signal Wire
;; =============================================================================

(defrecord SignalWire [id conn ref value watchers]
  p/ISignal
  (signal! [_ v]
    (reset! value v)
    ;; Notify local watchers
    (doseq [w @watchers] (w v))
    ;; Send to server (will buffer if disconnected)
    (send-or-buffer! conn {:op :signal :wire id :data v}))

  p/IWatch
  (watch [_ handler]
    (swap! watchers conj handler)
    ;; Call immediately with current value
    (handler @value)
    ;; Also request current value from server
    (when (:connected? @(:state conn))
      (send-or-buffer! conn {:op :watch :wire id}))
    (->Subscription id :signal
                    #(swap! watchers (fn [ws] (vec (remove #{handler} ws))))))

  p/IFlow
  (->flow [_]
    ;; Returns a Missionary continuous flow that tracks the signal value.
    ;; m/watch is Missionary's built-in atom observation - perfect for signals.
    ;; Emits current value immediately, then on each change.
    (m/watch value)))

;; =============================================================================
;; IWire Implementation - Get wire-ref from live wire
;; =============================================================================

(extend-type StreamWire
  p/IWire
  (wire-ref [wire]
    (:ref wire)))

(extend-type DiscreteWire
  p/IWire
  (wire-ref [wire]
    (:ref wire)))

(extend-type SignalWire
  p/IWire
  (wire-ref [wire]
    (:ref wire)))

;; =============================================================================
;; Connection Record
;; =============================================================================

(defrecord WSClientConnection [state wires pending-buffer url opts encoder]
  p/IConnection
  (stream [conn ref]
    (let [wire-id (:wire-id ref)]
      (if-let [existing (get @wires wire-id)]
        existing
        (let [wire (->StreamWire wire-id conn ref (atom []) (atom 0) (atom 0) (atom #{}))]
          (swap! wires assoc wire-id wire)
          wire))))

  (discrete [conn ref]
    (let [wire-id (:wire-id ref)]
      (if-let [existing (get @wires wire-id)]
        existing
        (let [wire (->DiscreteWire wire-id conn ref (atom nil) (atom {}) (atom #{}))]
          (swap! wires assoc wire-id wire)
          wire))))

  (signal [conn ref]
    (let [wire-id (:wire-id ref)
          initial-value (:initial ref)]
      (if-let [existing (get @wires wire-id)]
        existing
        (let [wire (->SignalWire wire-id conn ref (atom initial-value) (atom []))]
          (swap! wires assoc wire-id wire)
          wire))))

  p/ICloseable
  (close! [_]
    (swap! state assoc :closed? true)
    (when-let [^WebSocket ws (:ws @state)]
      (try
        (.sendClose ws WebSocket/NORMAL_CLOSURE "closing")
        (catch Exception _)))
    (reset! state {:ws nil :connected? false :reconnect-attempts 0 :closed? true})
    (reset! wires {})
    (reset! pending-buffer [])))

;; =============================================================================
;; Message Handling
;; =============================================================================

(defn- handle-message! [conn msg]
  (let [{:keys [op wire data req-id seq]} msg
        wires (:wires conn)]
    (case op
      ;; Stream emission from server
      :emit
      (when-let [w (get @wires wire)]
        ;; Track sequence numbers for gap detection
        (when seq
          (let [last-seq @(:last-seq-received w)
                expected (inc last-seq)
                gap (when (and (pos? last-seq) (> seq expected))
                      (- seq expected))]
            (when gap
              (println "Rheon: Sequence gap detected on" wire
                       "- expected" expected "got" seq
                       "- missed" gap "messages"))
            (reset! (:last-seq-received w) seq)))
        ;; Deliver to listeners with sequence metadata
        (let [enriched-data (if seq (assoc data :rheon/seq seq) data)]
          (doseq [listener @(:listeners w)]
            (listener enriched-data))
          ;; Notify flow callbacks (for m/observe consumers)
          (doseq [cb @(:flow-callbacks w)]
            (cb enriched-data))))

      ;; Reply to our request
      :reply
      (when-let [w (get @wires wire)]
        (when-let [pending-req (get @(:pending w) req-id)]
          (swap! (:pending w) dissoc req-id)
          (when-let [on-reply (:on-reply pending-req)]
            (on-reply data))))

      ;; Error response
      :error
      (when-let [w (get @wires wire)]
        (when-let [pending-req (get @(:pending w) req-id)]
          (swap! (:pending w) dissoc req-id)
          (when-let [on-error (:on-error pending-req)]
            (on-error {:error (:error msg)}))))

      ;; Signal value update
      :signal
      (when-let [w (get @wires wire)]
        (reset! (:value w) data)
        (doseq [watcher @(:watchers w)]
          (watcher data)))

      ;; Initial signal value (from watch request)
      :value
      (when-let [w (get @wires wire)]
        (reset! (:value w) data)
        (doseq [watcher @(:watchers w)]
          (watcher data)))

      ;; Incoming request (client can also handle requests)
      :send
      (when-let [w (get @wires wire)]
        ;; Notify flow callbacks about incoming request (for m/observe consumers)
        (doseq [cb @(:request-callbacks w)]
          (cb {:request data :req-id req-id}))
        (when-let [handler @(:handler w)]
          (try
            (let [reply (handler data)]
              (send-or-buffer! conn {:op :reply :wire wire :req-id req-id :data reply}))
            (catch Exception e
              (send-or-buffer! conn {:op :error :wire wire :req-id req-id
                                     :error (.getMessage e)})))))

      ;; Unknown op
      (println "Rheon: Unknown op:" op))))

;; =============================================================================
;; Send/Buffer Helper
;; =============================================================================

(defn- send-or-buffer!
  "Send a message if connected, otherwise buffer it for later.
   Returns true if sent immediately, false if buffered."
  [conn msg]
  (let [{:keys [^WebSocket ws connected?]} @(:state conn)
        encoder (:encoder conn)]
    (if (and ws connected?)
      (do
        (.sendText ws (p/encode encoder msg) true)
        true)
      (let [buffer (:pending-buffer conn)]
        (when (< (count @buffer) max-buffer-size)
          (swap! buffer conj msg)
          (println "Rheon: Buffered message (buffer size:" (count @buffer) ")"))
        false))))

(defn- flush-buffer!
  "Flush all buffered messages. Called on successful reconnection."
  [conn]
  (let [buffer (:pending-buffer conn)
        messages @buffer
        {:keys [^WebSocket ws connected?]} @(:state conn)
        encoder (:encoder conn)]
    (when (and ws connected? (seq messages))
      (println "Rheon: Flushing" (count messages) "buffered messages")
      (doseq [msg messages]
        (.sendText ws (p/encode encoder msg) true))
      (reset! buffer []))))

;; =============================================================================
;; Reconnection with Exponential Backoff
;; =============================================================================

(defn- reconnect-delay
  "Calculate reconnect delay with exponential backoff and jitter."
  [attempt & {:keys [base-ms cap-ms jitter-ms]
              :or {base-ms 1000 cap-ms 30000 jitter-ms 1000}}]
  (let [exp-delay (* base-ms (Math/pow 2 attempt))
        capped-delay (min cap-ms exp-delay)
        jitter (rand-int jitter-ms)]
    (long (+ capped-delay jitter))))

;; =============================================================================
;; WebSocket Connection
;; =============================================================================

(defn- connect!
  "Establish WebSocket connection with reconnection support."
  [conn]
  (let [{:keys [url opts state encoder]} conn
        {:keys [on-connect on-disconnect auto-reconnect?
                reconnect-base-ms reconnect-cap-ms]} opts
        closed? (:closed? @state)
        ;; StringBuilder for accumulating text fragments
        text-buffer (StringBuilder.)]
    (when-not closed?
      (println "Rheon: Connecting to" url)
      (let [client (HttpClient/newHttpClient)
            listener (reify WebSocket$Listener
                       (onOpen [_ ws]
                         (println "Rheon: Connected to" url)
                         (swap! state assoc :ws ws :connected? true :reconnect-attempts 0)
                         (flush-buffer! conn)
                         (when on-connect (on-connect))
                         ;; Request first message - Java WebSocket requires explicit request
                         (.request ws 1)
                         nil)

                       (onText [_ ws data last?]
                         (.append text-buffer data)
                         (when last?
                           (try
                             (let [full-text (.toString text-buffer)]
                               (.setLength text-buffer 0)
                               (handle-message! conn (p/decode encoder full-text)))
                             (catch Exception e
                               (println "Rheon: Decode error:" (.getMessage e)))))
                         (.request ws 1)
                         nil)

                       (onClose [_ _ status-code reason]
                         (let [attempts (:reconnect-attempts @state)
                               delay (reconnect-delay attempts
                                                      :base-ms (or reconnect-base-ms 1000)
                                                      :cap-ms (or reconnect-cap-ms 30000))]
                           (swap! state assoc :ws nil :connected? false)
                           (println "Rheon: Disconnected (status:" status-code "reason:" reason ")")
                           (when on-disconnect (on-disconnect))
                           (when (and auto-reconnect? (not (:closed? @state)))
                             (println "Rheon: Reconnecting in" delay "ms (attempt" (inc attempts) ")")
                             (swap! state update :reconnect-attempts inc)
                             (future
                               (Thread/sleep delay)
                               (connect! conn))))
                         nil)

                       (onError [_ _ error]
                         (println "Rheon: WebSocket error:" (.getMessage error))
                         nil))]
        (-> client
            (.newWebSocketBuilder)
            (.buildAsync (URI. url) listener))))))

;; =============================================================================
;; Connection Factory
;; =============================================================================

(defn connection
  "Create a WebSocket client connection.

   Args:
     opts - Map with:
            :url - WebSocket URL (e.g., \"ws://localhost:8084\")
            :on-connect - Optional callback when connected
            :on-disconnect - Optional callback when disconnected
            :auto-reconnect? - Auto-reconnect on disconnect (default true)
            :reconnect-base-ms - Base reconnect delay (default 1000)
            :reconnect-cap-ms - Max reconnect delay (default 30000)
            :encoding - :json or :msgpack (default :json for browser compatibility)
            :encoder - Custom IEncoder (takes priority over :encoding)

   Returns:
     A WSClientConnection that can be used to create wires.

   Example:
     (def conn (connection {:transport :ws-client :url \"ws://localhost:8084\"}))"
  [opts]
  (let [url (or (:url opts) "ws://localhost:8084")
        auto-reconnect? (get opts :auto-reconnect? true)
        encoder (resolve-encoder opts)
        state (atom {:ws nil :connected? false :reconnect-attempts 0 :closed? false})
        wires (atom {})
        pending-buffer (atom [])
        full-opts (assoc opts :auto-reconnect? auto-reconnect?)
        conn (->WSClientConnection state wires pending-buffer url full-opts encoder)]
    (connect! conn)
    conn))
