(ns mouse-tracker.handlers
  "Event handlers for mouse tracker demo.

   Uses multimethod dispatch for clean action routing.
   All handlers are pure functions: state + action -> new state."
  (:require [mouse-tracker.state :as s]))

;; =============================================================================
;; Multimethod Dispatch
;; =============================================================================

(defmulti handle
  "Handle an action. Dispatches on :type."
  (fn [_state action] (:type action)))

;; =============================================================================
;; Mouse Actions
;; =============================================================================

(defmethod handle :mouse-moved
  [state {:keys [x y emitted?]}]
  (if emitted?
    (s/update-mouse-and-inc state x y)
    (s/update-mouse state x y)))

;; =============================================================================
;; Clock Actions
;; =============================================================================

(defmethod handle :clock-synced
  [state {:keys [client-time server-time gap rtt]}]
  (-> state
      (s/update-clock {:client-time client-time
                       :server-time server-time
                       :gap gap
                       :rtt rtt})
      (s/add-log :discrete (str "Clock sync: gap=" gap "ms, RTT=" rtt "ms"))))

(defmethod handle :clock-sync-error
  [state {:keys [error]}]
  (s/add-log state :discrete (str "Clock sync error: " error)))

(defmethod handle :clock-sync-requested
  [state _action]
  (s/add-log state :discrete "Sent clock sync request"))

;; =============================================================================
;; Status Actions
;; =============================================================================

(defmethod handle :status-changed
  [state {:keys [status]}]
  (-> state
      (s/update-status status)
      (s/add-log :signal (str "Status: " (pr-str status)))))

(defmethod handle :connected
  [state {:keys [attempts]}]
  (-> state
      (s/set-connected true)
      (s/add-log :signal (str "Reconnected after " (or attempts 0) " attempt(s)"))))

(defmethod handle :disconnected
  [state _action]
  (-> state
      (s/set-connected false)
      (s/add-log :signal "Disconnected - will reconnect with exponential backoff")))

(defmethod handle :reconnecting
  [state {:keys [attempt]}]
  (s/add-log state :signal (str "Reconnect attempt " attempt " - backoff delay active")))

;; =============================================================================
;; Heartbeat Actions
;; =============================================================================

(defmethod handle :heartbeat-received
  [state {:keys [server-time tick uptime-sec]}]
  (s/update-heartbeat state {:server-time server-time
                             :tick tick
                             :uptime-sec uptime-sec}))

;; =============================================================================
;; Presence Actions
;; =============================================================================

(defmethod handle :presence-name-changed
  [state {:keys [name]}]
  (s/update-presence-name state name))

(defmethod handle :presence-color-changed
  [state {:keys [color]}]
  (s/update-presence-color state color))

(defmethod handle :presence-updated
  [state {:keys [name color]}]
  (-> state
      (s/update-presence name color)
      (s/add-log :signal (str "Updated presence: " name " (" color ")"))))

;; =============================================================================
;; Connection Health Actions
;; =============================================================================

(defmethod handle :connection-health-updated
  [state {:keys [reconnect-attempts buffer-size]}]
  (s/update-connection-health state {:reconnect-attempts reconnect-attempts
                                     :buffer-size buffer-size}))

(defmethod handle :messages-flushed
  [state {:keys [count]}]
  (-> state
      (s/add-flushed-messages count)
      (s/inc-total-reconnects)))

;; =============================================================================
;; Flow Control Actions
;; =============================================================================

(defmethod handle :backpressure-mode-changed
  [state {:keys [mode]}]
  (-> state
      (s/set-backpressure-mode mode)
      (s/add-log :signal (str "Backpressure mode: " (name mode)))))

(defmethod handle :message-received
  [state _action]
  (s/inc-messages-received state))

(defmethod handle :message-processed
  [state _action]
  (s/inc-messages-processed state))

(defmethod handle :sequence-updated
  [state {:keys [seq-num]}]
  (s/update-last-seq state seq-num))

(defmethod handle :gaps-detected
  [state {:keys [gap-count]}]
  (s/add-gaps-detected state gap-count))

;; =============================================================================
;; Log Actions
;; =============================================================================

(defmethod handle :log
  [state {:keys [log-type msg]}]
  (s/add-log state log-type msg))

;; =============================================================================
;; Default Handler
;; =============================================================================

(defmethod handle :default
  [state action]
  (js/console.warn "Unknown action type:" (:type action))
  state)
