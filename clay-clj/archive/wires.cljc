(ns clay.wires
  "Rheon wire definitions for Clay distributed UI.

   Wire Types:
   - Stream: Fire & forget, continuous data (viewport, pointer, scroll)
   - Discrete: Request/response (text measurement)
   - Signal: Shared state sync (render commands)

   These wire-refs are pure data - they can be used identically
   on both server (Clojure) and client (ClojureScript)."
  (:require [clay.schema :as schema]))

;; ============================================================================
;; CLIENT -> SERVER STREAMS
;; ============================================================================

(def viewport-wire
  "Stream of viewport dimensions from client to server.

   Emitted on window resize and initial load.
   Server uses this to set layout dimensions."
  {:wire-id :clay/viewport
   :type :stream
   :spec [:map
          [:width :double]
          [:height :double]]})

(def pointer-wire
  "Stream of pointer (mouse/touch) state from client to server.

   Emitted on mousemove, mousedown, mouseup, touchstart, touchmove, touchend.
   Server uses this for hover detection and click handling."
  {:wire-id :clay/pointer
   :type :stream
   :spec [:map
          [:x :double]
          [:y :double]
          [:state [:enum :none :hover :down :up]]]})

(def scroll-wire
  "Stream of scroll events from client to server.

   Emitted on wheel events within scroll containers.
   Server uses this to update scroll positions."
  {:wire-id :clay/scroll
   :type :stream
   :spec [:map
          [:x :double]
          [:y :double]
          [:delta-time :double]
          [:container-id {:optional true} :int]]})

;; ============================================================================
;; SERVER <-> CLIENT DISCRETE
;; ============================================================================

(def measure-text-wire
  "Discrete request/response for text measurement.

   Server sends text + config, client measures using Canvas2D.
   This is the critical blocking call for layout computation."
  {:wire-id :clay/measure-text
   :type :discrete
   :spec {:request [:map
                    [:text :string]
                    [:font-id :int]
                    [:font-size :int]
                    [:letter-spacing {:optional true} :int]
                    [:line-height {:optional true} :int]]
          :response [:map
                     [:width :double]
                     [:height :double]
                     [:min-width {:optional true} :double]
                     [:words {:optional true}
                      [:vector
                       [:map
                        [:text :string]
                        [:width :double]
                        [:is-whitespace :boolean]
                        [:is-newline :boolean]]]]]}})

(def measure-batch-wire
  "Discrete batch text measurement for efficiency.

   Measures multiple text strings in one round-trip.
   Reduces latency for layouts with many text elements."
  {:wire-id :clay/measure-batch
   :type :discrete
   :spec {:request [:vector
                    [:map
                     [:id :int]
                     [:text :string]
                     [:font-id :int]
                     [:font-size :int]
                     [:letter-spacing {:optional true} :int]]]
          :response [:vector
                     [:map
                      [:id :int]
                      [:width :double]
                      [:height :double]
                      [:min-width {:optional true} :double]]]}})

(def get-scroll-container-wire
  "Discrete request to get scroll container state.

   Used to sync scroll positions between server and client."
  {:wire-id :clay/get-scroll-container
   :type :discrete
   :spec {:request [:map
                    [:element-id :int]]
          :response [:map
                     [:scroll-position [:map [:x :double] [:y :double]]]
                     [:container-dimensions [:map [:width :double] [:height :double]]]
                     [:content-dimensions [:map [:width :double] [:height :double]]]
                     [:found :boolean]]}})

;; ============================================================================
;; SERVER -> CLIENT SIGNALS
;; ============================================================================

(def render-commands-wire
  "Signal of render commands from server to client.

   Updated whenever layout changes.
   Client renders these to Canvas2D."
  {:wire-id :clay/render-commands
   :type :signal
   :initial []
   :spec [:vector
          [:map
           [:bounding-box [:map
                           [:x :double]
                           [:y :double]
                           [:width :double]
                           [:height :double]]]
           [:command-type [:enum :none :rectangle :border :text :image :clip :custom]]
           [:render-data :any]
           [:id {:optional true} :any]
           [:z-index {:optional true} :int]]]})

(def ui-tree-wire
  "Signal of the full UI tree for debugging/inspection.

   Optional - can be disabled in production."
  {:wire-id :clay/ui-tree
   :type :signal
   :initial nil
   :spec [:maybe :any]})

;; ============================================================================
;; COMMANDS (CLIENT -> SERVER DISCRETE)
;; ============================================================================

(def command-wire
  "Discrete commands from client to server.

   Used for user interactions that need server handling:
   - Button clicks
   - Form submissions
   - Drag & drop operations"
  {:wire-id :clay/command
   :type :discrete
   :spec {:request [:map
                    [:command/kind :keyword]
                    [:command/target {:optional true} :any]
                    [:command/data {:optional true} :any]]
          :response [:map
                     [:success :boolean]
                     [:error {:optional true} :string]
                     [:data {:optional true} :any]]}})

;; ============================================================================
;; WIRE REGISTRY
;; ============================================================================

(def all-wires
  "All wire definitions for registration."
  {:viewport viewport-wire
   :pointer pointer-wire
   :scroll scroll-wire
   :measure-text measure-text-wire
   :measure-batch measure-batch-wire
   :get-scroll-container get-scroll-container-wire
   :render-commands render-commands-wire
   :ui-tree ui-tree-wire
   :command command-wire})

(defn get-wire
  "Get wire definition by keyword."
  [wire-key]
  (get all-wires wire-key))

;; ============================================================================
;; WIRE TYPE HELPERS
;; ============================================================================

(defn stream?
  "Check if wire is a stream type."
  [wire]
  (= :stream (:type wire)))

(defn discrete?
  "Check if wire is a discrete type."
  [wire]
  (= :discrete (:type wire)))

(defn signal?
  "Check if wire is a signal type."
  [wire]
  (= :signal (:type wire)))

(defn client->server?
  "Check if wire flows from client to server."
  [wire]
  (contains? #{:clay/viewport :clay/pointer :clay/scroll :clay/command}
             (:wire-id wire)))

(defn server->client?
  "Check if wire flows from server to client."
  [wire]
  (contains? #{:clay/render-commands :clay/ui-tree}
             (:wire-id wire)))

(defn bidirectional?
  "Check if wire is bidirectional (discrete request/response)."
  [wire]
  (contains? #{:clay/measure-text :clay/measure-batch :clay/get-scroll-container}
             (:wire-id wire)))
