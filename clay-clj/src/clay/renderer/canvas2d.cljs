(ns clay.renderer.canvas2d
  "Canvas2D renderer - renders Clay render commands to HTML5 Canvas.

   This module provides:
   - render-command!     : Render a single command to canvas
   - render-commands!    : Render all commands from layout
   - measure-text        : Measure text dimensions for layout
   - create-renderer     : Create a renderer instance

   Usage:
   (let [renderer (create-renderer canvas-element fonts)]
     (render-commands! renderer render-commands))")

;; ============================================================================
;; FONTS
;; ============================================================================

(def default-fonts
  "Default font family mapping by font-id."
  {0 "Inter, system-ui, sans-serif"
   1 "Georgia, serif"
   2 "Menlo, Consolas, monospace"})

(defn get-font-family
  "Get font family string for font-id."
  [fonts font-id]
  (get fonts font-id (get default-fonts 0)))

(defn build-font-string
  "Build CSS font string from config."
  [fonts {:keys [font-id font-size]}]
  (str (or font-size 16) "px " (get-font-family fonts (or font-id 0))))

;; ============================================================================
;; COLOR CONVERSION
;; ============================================================================

(defn color->css
  "Convert Clay color {:r :g :b :a} to CSS color string.
   Alpha can be 0-255 (Clay.h/color module style) or 0-1 (CSS style).
   Normalizes to CSS 0-1 range automatically."
  [{:keys [r g b a]}]
  (if a
    ;; Normalize alpha: if > 1, assume 0-255 range and convert to 0-1
    (let [alpha (if (> a 1) (/ a 255.0) a)]
      (str "rgba(" r "," g "," b "," alpha ")"))
    (str "rgb(" r "," g "," b ")")))

;; ============================================================================
;; TEXT MEASUREMENT
;; ============================================================================

(defn measure-text
  "Measure text dimensions using Canvas2D.

   Parameters:
   - ctx: Canvas 2D context
   - text: String to measure
   - config: {:font-id :font-size :letter-spacing}
   - fonts: Font family mapping

   Returns {:width :height}"
  [ctx text config fonts]
  (let [font-str (build-font-string fonts config)]
    (set! (.-font ctx) font-str)
    (let [metrics (.measureText ctx text)
          width (.-width metrics)
          ;; Use font bounding box for accurate height
          ascent (or (.-fontBoundingBoxAscent metrics)
                     (.-actualBoundingBoxAscent metrics)
                     (* 0.8 (:font-size config 16)))
          descent (or (.-fontBoundingBoxDescent metrics)
                      (.-actualBoundingBoxDescent metrics)
                      (* 0.2 (:font-size config 16)))
          height (+ ascent descent)]
      {:width width
       :height height
       :ascent ascent
       :descent descent})))

(defn create-measure-fn
  "Create a text measurement function for use with layout engine.

   Parameters:
   - ctx: Canvas 2D context
   - fonts: Font family mapping

   Returns function (text config) -> {:width :height}"
  [ctx fonts]
  (fn [text config]
    (measure-text ctx text config fonts)))

;; ============================================================================
;; RENDER HELPERS
;; ============================================================================

(defn- set-fill-style! [ctx color]
  (set! (.-fillStyle ctx) (color->css color)))

(defn- draw-rounded-rect!
  "Draw a rounded rectangle path."
  [ctx x y width height corner-radius]
  (let [{:keys [top-left top-right bottom-left bottom-right]} corner-radius
        tl (or top-left 0)
        tr (or top-right 0)
        bl (or bottom-left 0)
        br (or bottom-right 0)]
    (.beginPath ctx)
    (.moveTo ctx (+ x tl) y)
    (.lineTo ctx (- (+ x width) tr) y)
    (when (pos? tr)
      (.arcTo ctx (+ x width) y (+ x width) (+ y tr) tr))
    (.lineTo ctx (+ x width) (- (+ y height) br))
    (when (pos? br)
      (.arcTo ctx (+ x width) (+ y height) (- (+ x width) br) (+ y height) br))
    (.lineTo ctx (+ x bl) (+ y height))
    (when (pos? bl)
      (.arcTo ctx x (+ y height) x (- (+ y height) bl) bl))
    (.lineTo ctx x (+ y tl))
    (when (pos? tl)
      (.arcTo ctx x y (+ x tl) y tl))
    (.closePath ctx)))

;; ============================================================================
;; RENDER COMMANDS
;; ============================================================================

(defn render-rectangle!
  "Render a rectangle command."
  [ctx {:keys [bounding-box render-data]} _fonts]
  (let [{:keys [x y width height]} bounding-box
        {:keys [color corner-radius]} render-data]
    (when color
      (set-fill-style! ctx color)
      (if (and corner-radius
               (or (pos? (:top-left corner-radius 0))
                   (pos? (:top-right corner-radius 0))
                   (pos? (:bottom-left corner-radius 0))
                   (pos? (:bottom-right corner-radius 0))))
        (do
          (draw-rounded-rect! ctx x y width height corner-radius)
          (.fill ctx))
        (.fillRect ctx x y width height)))))

(defn render-text!
  "Render a text command."
  [ctx {:keys [bounding-box render-data]} fonts]
  (let [{:keys [x y]} bounding-box
        {:keys [text font-id font-size text-color]} render-data
        text-str (if (map? text) (:chars text) text)
        font-str (build-font-string fonts {:font-id font-id :font-size font-size})]
    (set! (.-font ctx) font-str)
    (set-fill-style! ctx (or text-color {:r 0 :g 0 :b 0 :a 1}))
    (set! (.-textBaseline ctx) "top")
    (.fillText ctx text-str x y)))

(defn render-border!
  "Render a border command.
   Port of Clay.h border rendering with proper corner arc support."
  [ctx {:keys [bounding-box render-data]} _fonts]
  (let [{:keys [x y]} bounding-box
        box-width (:width bounding-box)
        box-height (:height bounding-box)
        {:keys [color corner-radius]} render-data
        border-width (:width render-data)
        {:keys [left right top bottom]} (or border-width {:left 1 :right 1 :top 1 :bottom 1})
        tl (or (:top-left corner-radius) 0)
        tr (or (:top-right corner-radius) 0)
        bl (or (:bottom-left corner-radius) 0)
        br (or (:bottom-right corner-radius) 0)]
    (when color
      (set-fill-style! ctx color)
      ;; Top border with corner arcs
      (when (pos? top)
        ;; Top-left corner arc
        (when (pos? tl)
          (.beginPath ctx)
          (.arc ctx (+ x tl) (+ y tl) tl Math/PI (* 1.5 Math/PI) false)
          (.arc ctx (+ x tl) (+ y tl) (- tl top) (* 1.5 Math/PI) Math/PI true)
          (.closePath ctx)
          (.fill ctx))
        ;; Top straight edge
        (.fillRect ctx (+ x tl) y (- box-width tl tr) top)
        ;; Top-right corner arc
        (when (pos? tr)
          (.beginPath ctx)
          (.arc ctx (- (+ x box-width) tr) (+ y tr) tr (* 1.5 Math/PI) 0 false)
          (.arc ctx (- (+ x box-width) tr) (+ y tr) (- tr top) 0 (* 1.5 Math/PI) true)
          (.closePath ctx)
          (.fill ctx)))
      ;; Right border
      (when (pos? right)
        (.fillRect ctx (- (+ x box-width) right) (+ y tr)
                   right (- box-height tr br)))
      ;; Bottom border with corner arcs
      (when (pos? bottom)
        ;; Bottom-right corner arc
        (when (pos? br)
          (.beginPath ctx)
          (.arc ctx (- (+ x box-width) br) (- (+ y box-height) br) br 0 (* 0.5 Math/PI) false)
          (.arc ctx (- (+ x box-width) br) (- (+ y box-height) br) (- br bottom) (* 0.5 Math/PI) 0 true)
          (.closePath ctx)
          (.fill ctx))
        ;; Bottom straight edge
        (.fillRect ctx (+ x bl) (- (+ y box-height) bottom) (- box-width bl br) bottom)
        ;; Bottom-left corner arc
        (when (pos? bl)
          (.beginPath ctx)
          (.arc ctx (+ x bl) (- (+ y box-height) bl) bl (* 0.5 Math/PI) Math/PI false)
          (.arc ctx (+ x bl) (- (+ y box-height) bl) (- bl bottom) Math/PI (* 0.5 Math/PI) true)
          (.closePath ctx)
          (.fill ctx)))
      ;; Left border
      (when (pos? left)
        (.fillRect ctx x (+ y tl) left (- box-height tl bl))))))

(defn render-clip-start!
  "Begin a clipping region."
  [ctx {:keys [bounding-box]} _fonts]
  (let [{:keys [x y width height]} bounding-box]
    (.save ctx)
    (.beginPath ctx)
    (.rect ctx x y width height)
    (.clip ctx)))

(defn render-clip-end!
  "End a clipping region."
  [ctx _cmd _fonts]
  (.restore ctx))

(defn render-image!
  "Render an image command."
  [ctx {:keys [bounding-box render-data]} _fonts]
  (let [{:keys [x y width height]} bounding-box
        {:keys [image-data]} render-data]
    ;; image-data should be an Image object
    (when image-data
      (.drawImage ctx image-data x y width height))))

(defn render-custom!
  "Render a custom command by calling user-provided callback.
   Port of Clay.h CLAY_RENDER_COMMAND_TYPE_CUSTOM handling."
  [ctx {:keys [bounding-box render-data id]} _fonts custom-render-fn]
  (when custom-render-fn
    (custom-render-fn ctx bounding-box render-data id)))

(defn render-command!
  "Render a single render command.

   Parameters:
   - ctx: Canvas 2D context
   - cmd: Render command map
   - fonts: Font family mapping
   - custom-render-fn: Optional callback for :custom commands"
  ([ctx cmd fonts] (render-command! ctx cmd fonts nil))
  ([ctx cmd fonts custom-render-fn]
   (case (:command-type cmd)
     :rectangle (render-rectangle! ctx cmd fonts)
     :text (render-text! ctx cmd fonts)
     :border (render-border! ctx cmd fonts)
     :clip (render-clip-start! ctx cmd fonts)
     :clip-end (render-clip-end! ctx cmd fonts)
     :image (render-image! ctx cmd fonts)
     :custom (render-custom! ctx cmd fonts custom-render-fn)
     nil)))

;; ============================================================================
;; MAIN RENDER FUNCTION
;; ============================================================================

(defn render-commands!
  "Render all commands to canvas.

   Parameters:
   - ctx: Canvas 2D context
   - commands: Vector of render commands
   - fonts: Font family mapping (optional, uses defaults)"
  ([ctx commands] (render-commands! ctx commands default-fonts))
  ([ctx commands fonts]
   (doseq [cmd commands]
     (render-command! ctx cmd fonts))))

(defn clear!
  "Clear the canvas."
  [ctx width height]
  (.clearRect ctx 0 0 width height))

;; ============================================================================
;; RENDERER INSTANCE
;; ============================================================================

(defn create-renderer
  "Create a renderer instance bound to a canvas element.

   Parameters:
   - canvas: HTML Canvas element
   - fonts: Optional font family mapping

   Returns renderer map with:
   - :ctx - Canvas 2D context
   - :canvas - Canvas element
   - :fonts - Font mapping
   - :measure-fn - Text measurement function"
  ([canvas] (create-renderer canvas default-fonts))
  ([canvas fonts]
   (let [ctx (.getContext canvas "2d")]
     {:ctx ctx
      :canvas canvas
      :fonts fonts
      :measure-fn (create-measure-fn ctx fonts)})))

(defn render!
  "Render commands using a renderer instance.

   Parameters:
   - renderer: Renderer map from create-renderer
   - commands: Vector of render commands
   - opts: Optional {:clear? true}"
  ([renderer commands] (render! renderer commands {:clear? true}))
  ([renderer commands {:keys [clear?]}]
   (let [{:keys [ctx canvas fonts]} renderer]
     (when clear?
       (clear! ctx (.-width canvas) (.-height canvas)))
     (render-commands! ctx commands fonts))))

;; ============================================================================
;; EVENT HELPERS
;; ============================================================================

(defn get-canvas-pointer-position
  "Get pointer position relative to canvas.

   Parameters:
   - canvas: Canvas element
   - event: Mouse or touch event

   Returns {:x :y}"
  [canvas event]
  (let [rect (.getBoundingClientRect canvas)
        client-x (or (.-clientX event)
                     (when-let [touches (.-touches event)]
                       (.-clientX (aget touches 0))))
        client-y (or (.-clientY event)
                     (when-let [touches (.-touches event)]
                       (.-clientY (aget touches 0))))]
    {:x (- client-x (.-left rect))
     :y (- client-y (.-top rect))}))

(defn setup-pointer-events!
  "Setup pointer event handlers on canvas.

   Parameters:
   - canvas: Canvas element
   - on-pointer: Function (event-type position) -> nil

   event-type is one of: :move :down :up"
  [canvas on-pointer]
  (let [get-pos #(get-canvas-pointer-position canvas %)]
    (.addEventListener canvas "mousemove"
      (fn [e] (on-pointer :move (get-pos e))))
    (.addEventListener canvas "mousedown"
      (fn [e] (on-pointer :down (get-pos e))))
    (.addEventListener canvas "mouseup"
      (fn [e] (on-pointer :up (get-pos e))))
    ;; Touch events
    (.addEventListener canvas "touchstart"
      (fn [e]
        (.preventDefault e)
        (on-pointer :down (get-pos e))))
    (.addEventListener canvas "touchmove"
      (fn [e]
        (.preventDefault e)
        (on-pointer :move (get-pos e))))
    (.addEventListener canvas "touchend"
      (fn [e]
        (.preventDefault e)
        (on-pointer :up (get-pos e))))))
