(ns clay.client
  "Clay client integration with Rheon.

   This module connects a renderer (Canvas2D or HTML) to Rheon wires:
   - Emits viewport/pointer/scroll streams to server
   - Handles text measurement requests from server
   - Watches render commands signal and renders to element

   Usage:
     ;; Canvas2D renderer (default) - pass a canvas element
     (def client (create-client canvas-el {:url \"ws://localhost:8080\"}))
     (def client (create-client canvas-el {:url \"ws://localhost:8080\" :renderer :canvas2d}))

     ;; HTML renderer - pass a root div element
     (def client (create-client div-el {:url \"ws://localhost:8080\" :renderer :html}))

     (start! client)
     (stop! client)"
  (:require [rheon.core :as r]
            [clay.renderer.canvas2d :as canvas-renderer]
            [clay.renderer.html :as html-renderer]
            [clay.wires :as wires]))

;; ============================================================================
;; CLIENT STATE
;; ============================================================================

(defrecord ClayClient
  [conn                    ; Rheon connection
   element                 ; Canvas or root div element
   renderer                ; Renderer instance (canvas2d or html)
   renderer-type           ; :canvas2d or :html
   measure-fn              ; Text measurement function
   viewport-wire           ; Stream: viewport dimensions to server
   pointer-wire            ; Stream: pointer state to server
   scroll-wire             ; Stream: scroll events to server
   measure-text-wire       ; Discrete: text measurement (we respond)
   render-commands-wire    ; Signal: render commands from server
   subscriptions           ; Active subscriptions for cleanup
   options])               ; Client options

(defn- create-wires
  "Create all wire instances for a connection."
  [conn]
  {:viewport (r/wire conn wires/viewport-wire)
   :pointer (r/wire conn wires/pointer-wire)
   :scroll (r/wire conn wires/scroll-wire)
   :measure-text (r/wire conn wires/measure-text-wire)
   :render-commands (r/wire conn wires/render-commands-wire)})

;; ============================================================================
;; VIEWPORT HANDLING
;; ============================================================================

(defn- get-viewport
  "Get current viewport dimensions."
  []
  {:width (.-innerWidth js/window)
   :height (.-innerHeight js/window)})

(defn- setup-viewport-emitter!
  "Setup resize listener to emit viewport changes."
  [viewport-wire]
  ;; Emit initial viewport
  (r/emit! viewport-wire (get-viewport))

  ;; Listen for resize events
  (let [handler (fn [_]
                  (r/emit! viewport-wire (get-viewport)))]
    (.addEventListener js/window "resize" handler)
    ;; Return cleanup function
    (fn [] (.removeEventListener js/window "resize" handler))))

;; ============================================================================
;; POINTER HANDLING
;; ============================================================================

(defn- setup-pointer-emitter!
  "Setup pointer event listeners to emit pointer state."
  [canvas pointer-wire]
  (let [emit-pointer! (fn [event state]
                        (let [rect (.getBoundingClientRect canvas)
                              x (- (.-clientX event) (.-left rect))
                              y (- (.-clientY event) (.-top rect))]
                          (r/emit! pointer-wire {:x x :y y :state state})))
        handlers {:mousemove (fn [e] (emit-pointer! e :hover))
                  :mousedown (fn [e] (emit-pointer! e :down))
                  :mouseup (fn [e] (emit-pointer! e :up))
                  :mouseleave (fn [_] (r/emit! pointer-wire {:x -1 :y -1 :state :none}))}]
    ;; Attach handlers
    (doseq [[event handler] handlers]
      (.addEventListener canvas (name event) handler))
    ;; Return cleanup function
    (fn []
      (doseq [[event handler] handlers]
        (.removeEventListener canvas (name event) handler)))))

;; ============================================================================
;; SCROLL HANDLING
;; ============================================================================

(defn- setup-scroll-emitter!
  "Setup scroll event listener to emit scroll state."
  [canvas scroll-wire]
  (let [last-time (atom (.now js/Date))
        handler (fn [e]
                  (let [now (.now js/Date)
                        dt (- now @last-time)]
                    (reset! last-time now)
                    (r/emit! scroll-wire
                             {:x (.-deltaX e)
                              :y (.-deltaY e)
                              :delta-time (/ dt 1000)})))]
    (.addEventListener canvas "wheel" handler #js {:passive true})
    (fn [] (.removeEventListener canvas "wheel" handler))))

;; ============================================================================
;; TEXT MEASUREMENT
;; ============================================================================

(defn- setup-measure-text-handler!
  "Setup handler for text measurement requests from server."
  [measure-wire measure-fn]
  (r/reply! measure-wire
            (fn [{:keys [text font-id font-size letter-spacing]}]
              (let [config {:font-id font-id
                            :font-size font-size
                            :letter-spacing letter-spacing}
                    measured (measure-fn text config)]
                {:width (:width measured)
                 :height (:height measured)
                 :min-width (:width measured)}))))

;; ============================================================================
;; RENDER COMMANDS
;; ============================================================================

(defn- setup-render-watcher!
  "Setup watcher for render commands signal."
  [render-wire renderer renderer-type]
  (r/watch render-wire
           (fn [commands]
             (when (seq commands)
               (case renderer-type
                 :html (html-renderer/render! renderer commands)
                 :canvas2d (canvas-renderer/render! renderer commands))))))

;; ============================================================================
;; CANVAS SETUP
;; ============================================================================

(defn- setup-canvas!
  "Configure canvas element for high-DPI rendering."
  [canvas]
  (let [dpr (or (.-devicePixelRatio js/window) 1)
        rect (.getBoundingClientRect canvas)
        width (.-width rect)
        height (.-height rect)]
    ;; Set canvas size for high-DPI
    (set! (.-width canvas) (* width dpr))
    (set! (.-height canvas) (* height dpr))
    ;; Scale context
    (let [ctx (.getContext canvas "2d")]
      (.scale ctx dpr dpr))
    ;; Set CSS size
    (set! (.. canvas -style -width) (str width "px"))
    (set! (.. canvas -style -height) (str height "px"))
    canvas))

;; ============================================================================
;; CLIENT LIFECYCLE
;; ============================================================================

(defn create-client
  "Create a Clay client instance.

   Parameters:
   - element: HTML element (Canvas for :canvas2d, Div for :html) or ID string
   - opts: Map with:
           :renderer  - Renderer type (:canvas2d or :html, defaults to :canvas2d)
           :transport - Rheon transport (:ws-client, :mem)
           :url       - WebSocket URL (for :ws-client)
           :hub       - Rheon hub (for :mem transport)
           :fonts     - Font family mapping (optional)

   Returns ClayClient record."
  [element opts]
  (let [renderer-type (get opts :renderer :canvas2d)
        el (if (string? element)
             (.getElementById js/document element)
             element)
        fonts (or (:fonts opts) {})
        ;; Create appropriate renderer based on type
        [renderer measure-fn]
        (case renderer-type
          :html
          [(html-renderer/create-renderer el fonts)
           (html-renderer/create-measure-fn fonts)]

          :canvas2d
          (let [_ (setup-canvas! el)
                r (canvas-renderer/create-renderer el fonts)]
            [r (:measure-fn r)]))

        conn (r/connection (merge {:transport :ws-client} opts))
        wires (create-wires conn)]
    (map->ClayClient
     {:conn conn
      :element el
      :renderer renderer
      :renderer-type renderer-type
      :measure-fn measure-fn
      :viewport-wire (:viewport wires)
      :pointer-wire (:pointer wires)
      :scroll-wire (:scroll wires)
      :measure-text-wire (:measure-text wires)
      :render-commands-wire (:render-commands wires)
      :subscriptions (atom [])
      :options opts})))

(defn start!
  "Start the Clay client.

   Sets up all event listeners and wire connections."
  [client]
  (let [{:keys [element renderer renderer-type measure-fn
                viewport-wire pointer-wire scroll-wire
                measure-text-wire render-commands-wire subscriptions]} client]

    ;; Setup emitters
    (let [cleanup-viewport (setup-viewport-emitter! viewport-wire)
          cleanup-pointer (setup-pointer-emitter! element pointer-wire)
          cleanup-scroll (setup-scroll-emitter! element scroll-wire)

          ;; Setup handlers
          measure-sub (setup-measure-text-handler! measure-text-wire measure-fn)
          render-sub (setup-render-watcher! render-commands-wire renderer renderer-type)]

      ;; Store all cleanups
      (reset! subscriptions
              [cleanup-viewport cleanup-pointer cleanup-scroll
               measure-sub render-sub]))

    client))

(defn stop!
  "Stop the Clay client.

   Removes all event listeners and closes the connection."
  [client]
  (let [{:keys [conn subscriptions]} client]
    ;; Run all cleanups
    (doseq [cleanup-or-sub @subscriptions]
      (if (fn? cleanup-or-sub)
        (cleanup-or-sub)  ; Cleanup function
        (r/unsubscribe! cleanup-or-sub)))  ; Rheon subscription
    (reset! subscriptions [])

    ;; Close connection
    (r/close! conn)
    client))

;; ============================================================================
;; CONVENIENCE API
;; ============================================================================

(defn resize!
  "Handle element resize.

   Call this when the container changes size."
  [client]
  (let [{:keys [element renderer-type viewport-wire]} client]
    ;; Only setup-canvas for canvas2d renderer
    (when (= renderer-type :canvas2d)
      (setup-canvas! element))
    (r/emit! viewport-wire (get-viewport))))

;; ============================================================================
;; MEMORY TRANSPORT HELPERS (for testing)
;; ============================================================================

(defn create-test-client
  "Create a client with memory transport for testing.

   Parameters:
   - canvas: Canvas element
   - hub: Rheon hub from server's create-test-server

   Returns ClayClient."
  [canvas hub]
  (create-client canvas {:transport :mem :hub hub}))
