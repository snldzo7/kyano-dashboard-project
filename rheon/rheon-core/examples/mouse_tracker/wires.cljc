(ns mouse-tracker.wires
  "Shared Wire-Ref Definitions for Mouse Tracker.

   These are PURE DATA - no runtime, no side effects.
   Both client and server require this namespace and instantiate
   the same wire definitions with `r/wire`.

   This is the core insight: wire-refs are data that describes
   the network boundary. The same definitions work everywhere.")

;; =============================================================================
;; Stream Wire-Refs (continuous flow)
;; =============================================================================

(def mouse-ref
  "Mouse position stream - client -> server."
  {:wire-id :mouse
   :type :stream
   :spec [:map
          [:x :int]
          [:y :int]
          [:t {:optional true} :int]]})

(def heartbeat-ref
  "Heartbeat stream - server -> client."
  {:wire-id :heartbeat
   :type :stream
   :spec [:map
          [:server-time :int]
          [:tick :int]
          [:uptime-sec :string]]})

;; =============================================================================
;; Discrete Wire-Refs (request/response)
;; =============================================================================

(def clock-ref
  "Clock sync discrete - bidirectional request/reply."
  {:wire-id :clock
   :type :discrete
   :spec {:request [:map [:client-time :int]]
          :reply [:map
                  [:server-time :int]
                  [:client-time :int]
                  [:gap :int]]}})

;; =============================================================================
;; Signal Wire-Refs (current value)
;; =============================================================================

(def status-ref
  "Connection status signal - server -> client."
  {:wire-id :status
   :type :signal
   :initial {:state :starting}
   :spec [:map
          [:state [:enum :starting :connected :disconnected]]]})

(def presence-ref
  "User presence signal - shared state."
  {:wire-id :presence
   :type :signal
   :initial {:users {}}
   :spec [:map
          [:users [:map-of :string
                   [:map
                    [:name :string]
                    [:color :string]
                    [:updated :int]]]]]})

;; =============================================================================
;; All Wire-Refs (for iteration/introspection)
;; =============================================================================

(def all-refs
  "All wire-refs for this application."
  [mouse-ref heartbeat-ref clock-ref status-ref presence-ref])
