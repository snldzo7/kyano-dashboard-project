(ns rheon.transport.ws-client
  "WebSocket client transport for Rheon (ClojureScript browser).

   Mirrors the Clojure server transport API exactly.
   Uses browser WebSocket with Transit JSON encoding by default.

   Usage:
     (def conn (connection {:transport :ws :url \"ws://localhost:8084\"}))
     (def mouse (r/stream :mouse conn))
     (r/emit! mouse {:x 100 :y 200})"
  (:require [rheon.protocols :as p]
            [rheon.encoder :as enc]
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
;; Subscription Record
;; =============================================================================

(defrecord Subscription [wire-id type unsub-fn]
  p/ISubscription
  (unsubscribe! [_]
    (when unsub-fn (unsub-fn))))

;; =============================================================================
;; Message Buffering Helpers
;; =============================================================================

(def ^:private max-buffer-size
  "Maximum number of messages to buffer during disconnect."
  1000)

(defn- send-or-buffer!
  "Send a message if connected, otherwise buffer it for later.
   Returns true if sent immediately, false if buffered."
  [conn msg]
  (let [ws (:ws @(:state conn))
        encoder (:encoder conn)]
    (if (and ws (= 1 (.-readyState ws)))
      (do
        (.send ws (p/encode encoder msg))
        true)
      (let [buffer (:pending-buffer conn)]
        (when (< (count @buffer) max-buffer-size)
          (swap! buffer conj msg)
          (js/console.log "Rheon: Buffered message (buffer size:" (count @buffer) ")"))
        false))))

(defn- flush-buffer!
  "Flush all buffered messages. Called on successful reconnection."
  [conn]
  (let [buffer (:pending-buffer conn)
        messages @buffer
        ws (:ws @(:state conn))
        encoder (:encoder conn)]
    (when (and ws (= 1 (.-readyState ws)) (seq messages))
      (js/console.log "Rheon: Flushing" (count messages) "buffered messages")
      (doseq [msg messages]
        (.send ws (p/encode encoder msg)))
      (reset! buffer []))))

;; =============================================================================
;; Stream Wire
;; =============================================================================

(defrecord StreamWire [id conn listeners seq-counter last-seq-received flow-callbacks]
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

(defrecord DiscreteWire [id conn handler pending request-callbacks]
  p/ISend
  (send! [_ data opts]
    (let [req-id (str (random-uuid))
          {:keys [on-reply on-error timeout-ms]} opts
          msg {:op :send :wire id :req-id req-id :data data}]
      ;; Store pending request before sending
      (swap! pending assoc req-id {:on-reply on-reply :on-error on-error})
      ;; Try to send (will buffer if disconnected)
      (let [sent? (send-or-buffer! conn msg)]
        ;; Handle timeout (applies whether sent or buffered)
        (when timeout-ms
          (js/setTimeout
           (fn []
             (when-let [p (get @pending req-id)]
               (swap! pending dissoc req-id)
               (when-let [on-err (:on-error p)]
                 (on-err {:error (if sent? "Request timeout" "Request timeout (was buffered)")}))))
           timeout-ms)))))

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

(defrecord SignalWire [id conn value watchers]
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
    (let [ws (:ws @(:state conn))
          encoder (:encoder conn)]
      (when (and ws (= 1 (.-readyState ws)))
        (.send ws (p/encode encoder {:op :watch :wire id}))))
    (->Subscription id :signal
                    #(swap! watchers (fn [ws] (vec (remove #{handler} ws))))))

  p/IFlow
  (->flow [_]
    ;; Returns a Missionary continuous flow that tracks the signal value.
    ;; m/watch is Missionary's built-in atom observation - perfect for signals.
    ;; Emits current value immediately, then on each change.
    (m/watch value)))

;; =============================================================================
;; Connection
;; =============================================================================

(defrecord WSConnection [state wires pending-buffer encoder]
  p/IConnection
  (stream [conn wire-id]
    (if-let [existing (get @wires wire-id)]
      existing
      (let [wire (->StreamWire wire-id conn (atom []) (atom 0) (atom 0) (atom #{}))]
        (swap! wires assoc wire-id wire)
        wire)))

  (discrete [conn wire-id]
    (if-let [existing (get @wires wire-id)]
      existing
      (let [wire (->DiscreteWire wire-id conn (atom nil) (atom {}) (atom #{}))]
        (swap! wires assoc wire-id wire)
        wire)))

  (signal [conn wire-id initial-value]
    (if-let [existing (get @wires wire-id)]
      existing
      (let [wire (->SignalWire wire-id conn (atom initial-value) (atom []))]
        (swap! wires assoc wire-id wire)
        wire)))

  p/ICloseable
  (close! [_]
    (when-let [ws (:ws @state)]
      (.close ws))
    (reset! state {:ws nil :connected? false :reconnect-attempts 0})
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
      (if-let [w (get @wires wire)]
        (do
          ;; Track sequence numbers for gap detection
          (when seq
            (let [last-seq @(:last-seq-received w)
                  expected (inc last-seq)
                  gap (when (and (pos? last-seq) (> seq expected))
                        (- seq expected))]
              (when gap
                (js/console.warn "Rheon: Sequence gap detected on" wire
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
        (js/console.warn "Rheon: Wire not found:" (pr-str wire)))

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
            (let [reply (handler data)
                  ws (:ws @(:state conn))
                  encoder (:encoder conn)]
              (when (and ws (= 1 (.-readyState ws)))
                (.send ws (p/encode encoder {:op :reply :wire wire :req-id req-id :data reply}))))
            (catch :default e
              (let [ws (:ws @(:state conn))
                    encoder (:encoder conn)]
                (when (and ws (= 1 (.-readyState ws)))
                  (.send ws (p/encode encoder {:op :error :wire wire :req-id req-id
                                               :error (str e)}))))))))

      ;; Unknown op
      (js/console.warn "Unknown Rheon op:" op))))

;; =============================================================================
;; Reconnection with Exponential Backoff
;; =============================================================================

(defn- reconnect-delay
  "Calculate reconnect delay with exponential backoff and jitter.
   Formula: min(cap, base * 2^attempt) + random(0, jitter)

   Args:
     attempt - Number of reconnection attempts (0-indexed)
     base-ms - Base delay in ms (default 1000)
     cap-ms - Maximum delay cap in ms (default 30000)
     jitter-ms - Maximum jitter to add in ms (default 1000)"
  [attempt & {:keys [base-ms cap-ms jitter-ms]
              :or {base-ms 1000 cap-ms 30000 jitter-ms 1000}}]
  (let [exp-delay (* base-ms (js/Math.pow 2 attempt))
        capped-delay (min cap-ms exp-delay)
        jitter (js/Math.floor (* (js/Math.random) jitter-ms))]
    (+ capped-delay jitter)))

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
            :encoding - :json (default, browser-compatible)
            :encoder - Custom IEncoder (takes priority over :encoding)

   Returns:
     A WSConnection that can be used to create wires.

   Example:
     (def conn (connection {:transport :ws :url \"ws://localhost:8084\"}))"
  [opts]
  (let [url (or (:url opts) "ws://localhost:8084")
        auto-reconnect? (get opts :auto-reconnect? true)
        reconnect-base-ms (get opts :reconnect-base-ms 1000)
        reconnect-cap-ms (get opts :reconnect-cap-ms 30000)
        on-connect (:on-connect opts)
        on-disconnect (:on-disconnect opts)
        encoder (resolve-encoder opts)
        state (atom {:ws nil :connected? false :reconnect-attempts 0})
        wires (atom {})
        pending-buffer (atom [])
        conn (->WSConnection state wires pending-buffer encoder)]

    (letfn [(connect! []
              (let [ws (js/WebSocket. url)]
                (set! (.-onopen ws)
                      (fn [_]
                        ;; Reset reconnect attempts on successful connection
                        (swap! state assoc :ws ws :connected? true :reconnect-attempts 0)
                        (js/console.log "Rheon: Connected to" url)
                        ;; Flush any buffered messages from disconnect
                        (flush-buffer! conn)
                        (when on-connect (on-connect))))

                (set! (.-onclose ws)
                      (fn [_]
                        (let [attempts (:reconnect-attempts @state)
                              delay (reconnect-delay attempts
                                                     :base-ms reconnect-base-ms
                                                     :cap-ms reconnect-cap-ms)]
                          (swap! state assoc :ws nil :connected? false)
                          (js/console.log "Rheon: Disconnected")
                          (when on-disconnect (on-disconnect))
                          (when auto-reconnect?
                            (js/console.log "Rheon: Reconnecting in" delay "ms (attempt" (inc attempts) ")")
                            (swap! state update :reconnect-attempts inc)
                            (js/setTimeout connect! delay)))))

                (set! (.-onerror ws)
                      (fn [e]
                        (js/console.error "Rheon: WebSocket error" e)))

                (set! (.-onmessage ws)
                      (fn [event]
                        (try
                          (handle-message! conn (p/decode encoder (.-data event)))
                          (catch :default e
                            (js/console.error "Rheon: Decode error" e)))))))]
      (connect!)
      conn)))
