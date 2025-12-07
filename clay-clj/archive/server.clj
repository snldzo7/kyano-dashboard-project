(ns clay.server
  "Clay server integration with Rheon.

   This module connects the layout engine to Rheon wires:
   - Listens to viewport/pointer streams from clients
   - Handles text measurement requests via discrete wire
   - Broadcasts render commands via signal wire

   Usage:
     (def server (create-server {:port 8080}))
     (start! server ui-fn)
     (stop! server)"
  (:require [rheon.core :as r]
            [clay.hiccup2 :as hiccup]
            [clay.wires :as wires]))

;; ============================================================================
;; SERVER STATE
;; ============================================================================

(defrecord ClayServer
  [conn                    ; Rheon connection
   viewport-wire           ; Stream: viewport dimensions from client
   pointer-wire            ; Stream: pointer state from client
   scroll-wire             ; Stream: scroll events from client
   measure-text-wire       ; Discrete: text measurement request/response
   render-commands-wire    ; Signal: render commands to client
   state                   ; Atom with current layout state
   subscriptions])         ; Active subscriptions for cleanup

(defn- create-wires
  "Create all wire instances for a connection."
  [conn]
  {:viewport (r/wire conn wires/viewport-wire)
   :pointer (r/wire conn wires/pointer-wire)
   :scroll (r/wire conn wires/scroll-wire)
   :measure-text (r/wire conn wires/measure-text-wire)
   :render-commands (r/wire conn wires/render-commands-wire)})

;; ============================================================================
;; TEXT MEASUREMENT
;; ============================================================================

(defn- pending-measurements
  "Atom to track pending text measurements."
  []
  (atom {}))

(defn- request-text-measurement!
  "Request text measurement from client via discrete wire.

   Returns a promise that will be delivered with the measurement result."
  [measure-wire text config pending]
  (let [request-id (random-uuid)
        result-promise (promise)]
    (swap! pending assoc request-id result-promise)
    (r/send! measure-wire
             {:text text
              :font-id (or (:font-id config) 0)
              :font-size (or (:font-size config) 16)
              :letter-spacing (:letter-spacing config)
              :line-height (:line-height config)}
             {:timeout-ms 1000
              :on-reply (fn [response]
                          (swap! pending dissoc request-id)
                          (deliver result-promise response))
              :on-error (fn [error]
                          (swap! pending dissoc request-id)
                          (deliver result-promise {:width 0 :height 0 :error error}))})
    result-promise))

(defn create-measure-fn
  "Create a text measurement function that uses the discrete wire.

   This function is passed to the layout engine for measuring text."
  [measure-wire]
  (let [pending (pending-measurements)]
    (fn [text config]
      ;; Synchronous measurement via promise (blocks until response)
      @(request-text-measurement! measure-wire text config pending))))

;; ============================================================================
;; LAYOUT COMPUTATION (simplified with new pipeline)
;; ============================================================================

(defn compute-layout
  "Compute layout from UI tree and return render commands.

   Parameters:
   - ui-tree: Hiccup UI tree (e.g. [:col {:gap 8} [:text \"Hello\"]])
   - viewport: {:width :height}
   - measure-fn: Text measurement function

   Returns vector of render commands."
  [ui-tree viewport measure-fn]
  (hiccup/render viewport ui-tree measure-fn))

;; ============================================================================
;; SERVER LIFECYCLE
;; ============================================================================

(defn create-server
  "Create a Clay server instance.

   Parameters:
   - opts: Map with:
           :transport - Rheon transport (:ws-server, :mem)
           :port      - WebSocket port (for :ws-server)
           :hub       - Rheon hub (for :mem transport)

   Returns ClayServer record."
  [opts]
  (let [conn (r/connection (merge {:transport :ws-server} opts))
        wires (create-wires conn)]
    (map->ClayServer
     {:conn conn
      :viewport-wire (:viewport wires)
      :pointer-wire (:pointer wires)
      :scroll-wire (:scroll wires)
      :measure-text-wire (:measure-text wires)
      :render-commands-wire (:render-commands wires)
      :state (atom {:viewport {:width 800 :height 600}
                    :pointer {:x 0 :y 0 :state :none}
                    :ui-tree nil})
      :subscriptions (atom [])})))

(defn start!
  "Start the Clay server.

   Parameters:
   - server: ClayServer from create-server
   - ui-fn: Function (state) -> ui-tree that generates the UI

   Listens to viewport/pointer/scroll streams and recomputes layout
   when they change. Broadcasts render commands to clients."
  [server ui-fn]
  (let [{:keys [viewport-wire pointer-wire scroll-wire
                measure-text-wire render-commands-wire
                state subscriptions]} server
        measure-fn (create-measure-fn measure-text-wire)

        recompute! (fn []
                     (let [{:keys [viewport ui-tree]} @state
                           commands (compute-layout ui-tree viewport measure-fn)]
                       (r/signal! render-commands-wire commands)))

        ;; Listen to viewport changes
        viewport-sub (r/listen viewport-wire
                               (fn [viewport]
                                 (swap! state assoc :viewport viewport)
                                 (recompute!)))

        ;; Listen to pointer changes
        pointer-sub (r/listen pointer-wire
                              (fn [pointer]
                                (swap! state assoc :pointer pointer)
                                (recompute!))
                              {:backpressure :sample :interval-ms 16})

        ;; Listen to scroll changes
        scroll-sub (r/listen scroll-wire
                             (fn [scroll]
                               (swap! state update :scroll merge scroll)
                               (recompute!))
                             {:backpressure :sample :interval-ms 16})]

    ;; Store subscriptions for cleanup
    (reset! subscriptions [viewport-sub pointer-sub scroll-sub])

    ;; Initial UI tree computation
    (swap! state assoc :ui-tree (ui-fn @state))
    (recompute!)

    server))

(defn update-ui!
  "Update the UI tree and recompute layout.

   Parameters:
   - server: ClayServer
   - ui-fn: Function (state) -> ui-tree"
  [server ui-fn]
  (let [{:keys [state measure-text-wire render-commands-wire]} server
        measure-fn (create-measure-fn measure-text-wire)]
    (swap! state assoc :ui-tree (ui-fn @state))
    (let [{:keys [viewport ui-tree]} @state
          commands (compute-layout ui-tree viewport measure-fn)]
      (r/signal! render-commands-wire commands))))

(defn stop!
  "Stop the Clay server.

   Unsubscribes from all wires and closes the connection."
  [server]
  (let [{:keys [conn subscriptions]} server]
    ;; Unsubscribe from all wires
    (doseq [sub @(:subscriptions server)]
      (r/unsubscribe! sub))
    (reset! subscriptions [])

    ;; Close connection
    (r/close! conn)
    server))

;; ============================================================================
;; CONVENIENCE API
;; ============================================================================

(defn with-layout
  "Helper for building layout.

   Example:
     (with-layout server
       [:col {:padding 16}
         [:text {:size :lg} \"Hello World\"]])"
  [server ui-tree]
  (update-ui! server (constantly ui-tree)))

;; ============================================================================
;; MEMORY TRANSPORT HELPERS (for testing)
;; ============================================================================

(defn create-test-server
  "Create a server with memory transport for testing.

   Returns {:server :hub} where hub can be used to create test clients."
  []
  (let [hub (r/create-hub)
        server (create-server {:transport :mem :hub hub})]
    {:server server
     :hub hub}))
