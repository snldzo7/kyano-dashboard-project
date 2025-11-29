(ns mouse-tracker.state
  "State shape and pure update functions for mouse tracker demo.

   All functions are pure - take state, return new state.
   Easy to test, easy to understand."
  (:require [ui.timeseries :as ts]))

;; =============================================================================
;; Initial State
;; =============================================================================

(def initial-state
  {:mouse {:x 0 :y 0}

   ;; Time-series for real-time charts (ring buffer of last 60 positions)
   :mouse-history (ts/ring-buffer 60)

   :clock {:client-time nil
           :server-time nil
           :gap nil
           :rtt nil}

   :status {:state :connecting
            :port nil}

   :heartbeat {:server-time nil
               :tick nil
               :uptime-sec nil}

   :presence {:name ""
              :color "#00d9ff"}

   :connected? false
   :events-sent 0

   :log []

   ;; Connection Health (Phase 1)
   :connection-health {:reconnect-attempts 0
                       :last-reconnect-delay nil
                       :buffer-size 0
                       :buffer-max 1000
                       :messages-flushed 0
                       :total-reconnects 0}

   ;; Flow Control (Phase 2)
   :flow-control {:backpressure-mode :none
                  :messages-received 0
                  :messages-processed 0
                  :messages-dropped 0
                  :last-seq nil
                  :gaps-detected 0
                  :sample-interval-ms 16}})

;; =============================================================================
;; Pure Update Functions - Mouse
;; =============================================================================

(defn update-mouse
  "Update mouse coordinates and push to history for charts."
  [state x y]
  (let [now (js/Date.now)]
    (-> state
        (assoc :mouse {:x x :y y})
        (update :mouse-history ts/rb-push {:t now :x x :y y}))))

(defn inc-events-sent
  "Increment events sent counter."
  [state]
  (update state :events-sent inc))

(defn update-mouse-and-inc
  "Update mouse and increment events in one update."
  [state x y]
  (-> state
      (update-mouse x y)
      inc-events-sent))

(defn get-speed-data
  "Derive speed time-series from mouse history.
   Returns vector of {:t timestamp :v speed-in-px-per-sec}"
  [state]
  (let [positions (ts/rb-values (:mouse-history state))]
    (or (ts/derive-speeds positions) [])))

(defn current-speed
  "Get current mouse speed in px/sec."
  [state]
  (let [speeds (get-speed-data state)]
    (if (seq speeds)
      (:v (last speeds))
      0)))

;; =============================================================================
;; Pure Update Functions - Clock
;; =============================================================================

(defn update-clock
  "Update clock sync data."
  [state {:keys [client-time server-time gap rtt]}]
  (assoc state :clock {:client-time client-time
                       :server-time server-time
                       :gap gap
                       :rtt rtt}))

;; =============================================================================
;; Pure Update Functions - Status
;; =============================================================================

(defn update-status
  "Update server status."
  [state status]
  (assoc state :status status))

(defn set-connected
  "Set connection state."
  [state connected?]
  (assoc state :connected? connected?))

;; =============================================================================
;; Pure Update Functions - Heartbeat
;; =============================================================================

(defn update-heartbeat
  "Update heartbeat data."
  [state {:keys [server-time tick uptime-sec]}]
  (assoc state :heartbeat {:server-time server-time
                           :tick tick
                           :uptime-sec uptime-sec}))

;; =============================================================================
;; Pure Update Functions - Presence
;; =============================================================================

(defn update-presence-name
  "Update presence name."
  [state name]
  (assoc-in state [:presence :name] name))

(defn update-presence-color
  "Update presence color."
  [state color]
  (assoc-in state [:presence :color] color))

(defn update-presence
  "Update both name and color."
  [state name color]
  (assoc state :presence {:name name :color color}))

;; =============================================================================
;; Pure Update Functions - Connection Health
;; =============================================================================

(defn update-connection-health
  "Update connection health metrics."
  [state updates]
  (update state :connection-health merge updates))

(defn inc-total-reconnects
  "Increment total reconnects counter."
  [state]
  (update-in state [:connection-health :total-reconnects] inc))

(defn add-flushed-messages
  "Add to messages flushed count."
  [state count]
  (update-in state [:connection-health :messages-flushed] + count))

;; =============================================================================
;; Pure Update Functions - Flow Control
;; =============================================================================

(defn set-backpressure-mode
  "Set backpressure strategy."
  [state mode]
  (assoc-in state [:flow-control :backpressure-mode] mode))

(defn inc-messages-received
  "Increment messages received."
  [state]
  (update-in state [:flow-control :messages-received] (fnil inc 0)))

(defn inc-messages-processed
  "Increment messages processed."
  [state]
  (update-in state [:flow-control :messages-processed] (fnil inc 0)))

(defn update-last-seq
  "Update last sequence number."
  [state seq-num]
  (assoc-in state [:flow-control :last-seq] seq-num))

(defn add-gaps-detected
  "Add to gaps detected count."
  [state gap-count]
  (update-in state [:flow-control :gaps-detected] (fnil + 0) gap-count))

;; =============================================================================
;; Pure Update Functions - Log
;; =============================================================================

(defonce ^:private log-counter (atom 0))

(defn add-log
  "Add entry to log (max 50 entries)."
  [state log-type msg]
  (let [id (swap! log-counter inc)
        time (.toLocaleTimeString (js/Date.))
        entry {:id id :time time :type log-type :msg msg}]
    (update state :log
            (fn [logs]
              (vec (take 50 (cons entry logs)))))))
