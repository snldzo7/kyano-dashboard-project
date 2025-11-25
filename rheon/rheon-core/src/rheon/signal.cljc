(ns rheon.signal
  "Rheon Signal type - a wire that behaves like a Clojure atom.

   Signals implement:
   - clojure.lang.IDeref     - @signal returns current value
   - clojure.lang.IWatchable - add-watch/remove-watch work
   - clojure.lang.IRef       - for validator support (future)

   A Signal subscribes to a wire and keeps track of the latest value.
   When the wire receives a message, the signal updates.

   Usage:
     (def status (signal conn :status :disconnected))
     @status              ;; => :disconnected
     (add-watch status :ui (fn [_ _ old new] (render!)))
     ;; Later, when server sends on :status wire:
     @status              ;; => :connected"
  (:require [rheon.protocols :as p]))

;; =============================================================================
;; Signal Type
;; =============================================================================

(deftype Signal [conn           ;; Connection this signal is attached to
                 wire           ;; Wire keyword (:status, :mouse, etc.)
                 ^:volatile-mutable value      ;; Current value
                 watches        ;; atom: {key -> watch-fn}
                 subscription]  ;; Subscription from gauge

  #?@(:clj
      [;; IDeref - @signal
       clojure.lang.IDeref
       (deref [_]
         value)

       ;; IWatchable - add-watch / remove-watch
       clojure.lang.IRef
       (addWatch [this key callback]
         (swap! watches assoc key callback)
         this)
       (removeWatch [this key]
         (swap! watches dissoc key)
         this)
       (getWatches [_]
         @watches)
       (setValidator [_ _]
         (throw (UnsupportedOperationException. "Signals don't support validators")))
       (getValidator [_]
         nil)]

      :cljs
      [;; IDeref - @signal
       IDeref
       (-deref [_]
         value)

       ;; IWatchable - add-watch / remove-watch
       IWatchable
       (-add-watch [this key callback]
         (swap! watches assoc key callback)
         this)
       (-remove-watch [this key]
         (swap! watches dissoc key)
         this)]))

;; =============================================================================
;; Signal Update (called when wire receives message)
;; =============================================================================

(defn- notify-watches
  "Notify all watches of a value change."
  [signal old-val new-val]
  (doseq [[key callback] @(.-watches signal)]
    (try
      (callback key signal old-val new-val)
      (catch #?(:clj Exception :cljs :default) e
        (println "Error in signal watch" key ":" e)))))

(defn- update-signal!
  "Update signal value and notify watches."
  [^Signal signal new-val]
  (let [old-val (.-value signal)]
    (when (not= old-val new-val)
      (set! (.-value signal) new-val)
      (notify-watches signal old-val new-val))))

;; =============================================================================
;; Constructor
;; =============================================================================

(defn make-signal
  "Create a Signal attached to a wire on a connection.

   The signal subscribes to the wire and updates its value when
   messages arrive. The initial-value is used until the first
   message is received.

   Args:
     conn          - Rheon connection
     wire          - Wire keyword (e.g., :status)
     initial-value - Value before any messages arrive

   Returns:
     A Signal that can be deref'd and watched."
  [conn wire initial-value]
  (let [watches (atom {})
        ;; Use atom to hold signal reference for the handler
        signal-ref (atom nil)
        ;; Subscribe to wire - handler will update signal when messages arrive
        subscription (p/on-message conn wire
                                   (fn [data]
                                     (when-let [sig @signal-ref]
                                       (update-signal! sig data))))
        ;; Create signal with subscription already set
        signal (Signal. conn wire initial-value watches subscription)]
    ;; Store signal reference so handler can find it
    (reset! signal-ref signal)
    signal))

;; =============================================================================
;; Cleanup
;; =============================================================================

(defn close-signal!
  "Unsubscribe the signal from its wire.
   After this, the signal will no longer update."
  [^Signal signal]
  (when-let [sub (.-subscription signal)]
    (p/unsubscribe! (.-conn signal) sub)))
