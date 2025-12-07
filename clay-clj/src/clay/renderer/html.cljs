(ns clay.renderer.html
  "HTML/DOM renderer - renders Clay render commands to DOM elements.

   FAITHFUL PORT of clay-html-renderer.html from Clay.h
   Much easier to debug than Canvas2D!

   Key features from original:
   - Element cache with dirty checking (lines 354-360, 375)
   - Scissor stack with DOM ordering via insertAdjacentElement (lines 331, 365-371)
   - Link and cursor support (lines 341-344, 403-410)
   - Element cleanup for removed elements (lines 507-514)")

;; ============================================================================
;; FONTS
;; ============================================================================

(def default-fonts
  {0 "Inter, system-ui, sans-serif"
   1 "Georgia, serif"
   2 "Menlo, Consolas, monospace"})

(def ^:const GLOBAL_FONT_SCALING_FACTOR 1.0)  ;; Original uses 0.8

;; ============================================================================
;; ELEMENT CACHE (faithful to original lines 354-360)
;; ============================================================================
;; Original structure:
;; elementCache[id] = {
;;   exists: true,
;;   element: element,
;;   previousMemoryCommand: new Uint8Array(0),
;;   previousMemoryConfig: new Uint8Array(0),
;;   previousMemoryText: new Uint8Array(0)
;; };

(defonce element-cache (atom {}))

;; ============================================================================
;; COLOR CONVERSION (faithful to original line 413)
;; ============================================================================
;; Original: `rgba(${color.r.value}, ${color.g.value}, ${color.b.value}, ${color.a.value / 255})`

(defn color->css
  "Convert Clay color {:r :g :b :a} to CSS color string.
   Original divides alpha by 255."
  [{:keys [r g b a]}]
  (if a
    ;; Original: color.a.value / 255
    (let [alpha (if (> a 1) (/ a 255.0) a)]
      (str "rgba(" r "," g "," b "," alpha ")"))
    (str "rgb(" r "," g "," b ")")))

;; ============================================================================
;; DIRTY CHECKING (faithful to original line 375)
;; ============================================================================
;; Original: let dirty = MemoryIsDifferent(elementData.previousMemoryCommand, entireRenderCommandMemory, renderCommandSize);

(defn- dirty?
  "Check if render command has changed since last frame.
   Port of MemoryIsDifferent (lines 318-325)."
  [cached-data new-cmd]
  (not= (:prev-cmd cached-data) new-cmd))

(defn- config-dirty?
  "Check if config has changed since last frame."
  [cached-data new-config]
  (not= (:prev-config cached-data) new-config))

(defn- text-dirty?
  "Check if text has changed since last frame."
  [cached-data new-text]
  (not= (:prev-text cached-data) new-text))

;; ============================================================================
;; DOM ORDERING (faithful to original lines 365-371)
;; ============================================================================
;; Original:
;; if (parentElement.nextElementIndex === 0 || !parentElement.element.childNodes[parentElement.nextElementIndex - 1]) {
;;     parentElement.element.insertAdjacentElement('afterbegin', element);
;; } else {
;;     parentElement.element.childNodes[parentElement.nextElementIndex - 1].insertAdjacentElement('afterend', element);
;; }

(defn- insert-at-index!
  "Insert element at correct position in parent.
   Port of DOM ordering via insertAdjacentElement (lines 365-371)."
  [parent element idx]
  (let [children (.-children parent)
        current-idx (.indexOf (js/Array.from children) element)]
    (js/console.log "[insert-at-index]" (.-id element) "current:" current-idx "target:" idx "children-count:" (.-length children))
    ;; Only reorder if not already at correct position (line 365)
    (when (not= current-idx idx)
      (if (or (zero? idx) (nil? (aget children (dec idx))))
        (do
          (js/console.log "[insert-at-index] inserting at beginning")
          (.insertAdjacentElement parent "afterbegin" element))
        (when-let [prev (aget children (dec idx))]
          (js/console.log "[insert-at-index] inserting after" (.-id prev))
          (.insertAdjacentElement prev "afterend" element))))))

;; ============================================================================
;; ELEMENT CACHE HELPERS
;; ============================================================================

(defn- get-or-create-element!
  "Get cached element or create new one.
   Port of element creation (lines 337-360)."
  [id element-type]
  (if-let [cached (get @element-cache id)]
    (:element cached)
    (let [el (js/document.createElement element-type)]
      (set! (.-id el) (str "clay-" id))
      ;; Faithful to original: store element with empty previous data
      (swap! element-cache assoc id
             {:exists true
              :element el
              :prev-cmd nil
              :prev-config nil
              :prev-text nil})
      el)))

(defn- update-cache!
  "Update cache with current command data."
  [id cmd config text]
  (swap! element-cache update id assoc
         :exists true
         :prev-cmd cmd
         :prev-config config
         :prev-text text))

(defn- mark-element-exists! [id]
  (swap! element-cache update id assoc :exists true))

(defn- reset-exists-flags!
  "Reset all exists flags to false before rendering.
   Port of cleanup logic (lines 507-514)."
  []
  (swap! element-cache
         (fn [cache]
           (reduce-kv (fn [m k v] (assoc m k (assoc v :exists false)))
                      {} cache))))

(defn- cleanup-unused-elements!
  "Remove elements that weren't used this frame.
   Port of cleanup logic (lines 507-514)."
  []
  (doseq [[id {:keys [element exists]}] @element-cache]
    (when-not exists
      ;; Original: elementCache[key].element.remove()
      (.remove element)
      (swap! element-cache dissoc id))))

(defn- set-transform! [el x y]
  (set! (.. el -style -transform)
        (str "translate(" (Math/round x) "px," (Math/round y) "px)")))

(defn- set-size! [el width height]
  (set! (.. el -style -width) (str (Math/round width) "px"))
  (set! (.. el -style -height) (str (Math/round height) "px")))

;; ============================================================================
;; RENDER COMMANDS
;; ============================================================================

(defn render-rectangle!
  "Render a rectangle as a div with background.
   Supports links and cursor pointer (lines 341-344, 403-410)."
  [parent {:keys [bounding-box render-data id]} offset-x offset-y]
  (let [{:keys [x y width height]} bounding-box
        {:keys [color corner-radius link cursor-pointer]} render-data
        ;; Create <a> if has link, else <div> (lines 341-344)
        element-type (if (and link (seq link)) "a" "div")
        el (get-or-create-element! id element-type)]
    (mark-element-exists! id)

    ;; Position and size
    (set-transform! el (- x offset-x) (- y offset-y))
    (set-size! el width height)
    (set! (.. el -style -position) "absolute")

    ;; Background color
    (when color
      (set! (.. el -style -backgroundColor) (color->css color)))

    ;; Link support (lines 403-410)
    (when (and link (seq link))
      (set! (.-href el) link)
      (set! (.. el -style -pointerEvents) "all")
      (set! (.. el -style -cursor) "pointer"))

    ;; Cursor pointer without link (line 407-410)
    (when cursor-pointer
      (set! (.. el -style -pointerEvents) "all")
      (set! (.. el -style -cursor) "pointer"))

    ;; Corner radius
    (when corner-radius
      (when (pos? (:top-left corner-radius 0))
        (set! (.. el -style -borderTopLeftRadius) (str (:top-left corner-radius) "px")))
      (when (pos? (:top-right corner-radius 0))
        (set! (.. el -style -borderTopRightRadius) (str (:top-right corner-radius) "px")))
      (when (pos? (:bottom-left corner-radius 0))
        (set! (.. el -style -borderBottomLeftRadius) (str (:bottom-left corner-radius) "px")))
      (when (pos? (:bottom-right corner-radius 0))
        (set! (.. el -style -borderBottomRightRadius) (str (:bottom-right corner-radius) "px"))))

    ;; Append to parent if not already
    (when-not (.-parentNode el)
      (.appendChild parent el))

    el))

(defn render-text!
  "Render text as a div with text content.
   Port of CLAY_RENDER_COMMAND_TYPE_TEXT (lines 465-484)."
  [parent {:keys [bounding-box render-data id]} offset-x offset-y fonts]
  (let [{:keys [x y width height]} bounding-box
        {:keys [text font-id font-size text-color disable-pointer-events]} render-data
        text-str (if (map? text) (:chars text) text)
        el (get-or-create-element! id "div")]
    (mark-element-exists! id)

    ;; Position and size
    (set-transform! el (- x offset-x) (- y offset-y))
    (set-size! el width height)
    (set! (.. el -style -position) "absolute")
    (set! (.. el -style -whiteSpace) "pre")
    (set! (.. el -style -overflow) "hidden")

    ;; Text styling (lines 471-477)
    (set! (.-className el) "text")
    (set! (.. el -style -fontFamily) (get fonts (or font-id 0) (get default-fonts 0)))
    (set! (.. el -style -fontSize) (str (* (or font-size 16) GLOBAL_FONT_SCALING_FACTOR) "px"))
    ;; Always set text color - default to black if not specified
    (set! (.. el -style -color) (if text-color
                                   (color->css text-color)
                                   "#000000"))

    ;; Pointer events (line 477)
    (set! (.. el -style -pointerEvents) (if disable-pointer-events "none" "all"))

    ;; Text content
    (set! (.-textContent el) (or text-str ""))

    ;; Append to parent if not already
    (when-not (.-parentNode el)
      (.appendChild parent el))

    el))

(defn render-image!
  "Render an image element.
   Port of CLAY_RENDER_COMMAND_TYPE_IMAGE (lines 494-501)."
  [parent {:keys [bounding-box render-data id]} offset-x offset-y]
  (let [{:keys [x y width height]} bounding-box
        {:keys [source-url image-data]} render-data
        el (get-or-create-element! id "img")]
    (mark-element-exists! id)

    ;; Position and size
    (set-transform! el (- x offset-x) (- y offset-y))
    (set-size! el width height)
    (set! (.. el -style -position) "absolute")

    ;; Image source
    (when source-url
      (set! (.-src el) source-url))

    ;; Append to parent if not already
    (when-not (.-parentNode el)
      (.appendChild parent el))

    el))

(defn render-border!
  "Render a border element."
  [parent {:keys [bounding-box render-data id]} offset-x offset-y]
  (let [{:keys [x y width height]} bounding-box
        {:keys [color corner-radius]} render-data
        border-width (:width render-data)
        el (get-or-create-element! id "div")]
    (mark-element-exists! id)

    ;; Position and size
    (set-transform! el (- x offset-x) (- y offset-y))
    (set-size! el width height)
    (set! (.. el -style -position) "absolute")
    (set! (.. el -style -boxSizing) "border-box")
    (set! (.. el -style -pointerEvents) "none")

    ;; Border widths
    (when (map? border-width)
      (let [{:keys [left right top bottom]} border-width
            border-color (color->css color)]
        (when (and left (pos? left))
          (set! (.. el -style -borderLeft) (str left "px solid " border-color)))
        (when (and right (pos? right))
          (set! (.. el -style -borderRight) (str right "px solid " border-color)))
        (when (and top (pos? top))
          (set! (.. el -style -borderTop) (str top "px solid " border-color)))
        (when (and bottom (pos? bottom))
          (set! (.. el -style -borderBottom) (str bottom "px solid " border-color)))))

    ;; Simple border (all sides)
    (when (number? border-width)
      (set! (.. el -style -border) (str border-width "px solid " (color->css color))))

    ;; Corner radius
    (when corner-radius
      (when (pos? (:top-left corner-radius 0))
        (set! (.. el -style -borderTopLeftRadius) (str (:top-left corner-radius) "px")))
      (when (pos? (:top-right corner-radius 0))
        (set! (.. el -style -borderTopRightRadius) (str (:top-right corner-radius) "px")))
      (when (pos? (:bottom-left corner-radius 0))
        (set! (.. el -style -borderBottomLeftRadius) (str (:bottom-left corner-radius) "px")))
      (when (pos? (:bottom-right corner-radius 0))
        (set! (.. el -style -borderBottomRightRadius) (str (:bottom-right corner-radius) "px"))))

    ;; Append to parent if not already
    (when-not (.-parentNode el)
      (.appendChild parent el))

    el))

;; ============================================================================
;; MAIN RENDER (faithful to original renderLoopHTML lines 327-515)
;; ============================================================================
;; Original scissor stack structure (line 331):
;; scissorStack = [{ nextAllocation: { x: 0, y: 0 }, element: htmlRoot, nextElementIndex: 0 }];

(defn render-commands!
  "Render all commands to DOM elements.
   FAITHFUL PORT of renderLoopHTML (lines 327-515).

   Parameters:
   - root: Root DOM element to render into
   - commands: Vector of render commands
   - fonts: Optional font mapping"
  ([root commands] (render-commands! root commands default-fonts))
  ([root commands fonts]
   (reset-exists-flags!)

   ;; Scissor stack with nextElementIndex for DOM ordering (line 331)
   (let [scissor-stack (atom [{:element root
                               :offset-x 0
                               :offset-y 0
                               :next-element-index 0}])]
     (doseq [cmd commands]
       (let [parent-entry (peek @scissor-stack)
             {:keys [element offset-x offset-y next-element-index]} parent-entry
             id (:id cmd)]

         (case (:command-type cmd)
           :rectangle
           (let [el (render-rectangle! element cmd offset-x offset-y)]
             ;; DOM ordering (lines 365-371)
             (insert-at-index! element el next-element-index)
             ;; Increment nextElementIndex (line 376)
             (swap! scissor-stack update (dec (count @scissor-stack))
                    update :next-element-index inc))

           :text
           (let [el (render-text! element cmd offset-x offset-y fonts)]
             (insert-at-index! element el next-element-index)
             (swap! scissor-stack update (dec (count @scissor-stack))
                    update :next-element-index inc))

           :border
           (let [el (render-border! element cmd offset-x offset-y)]
             (insert-at-index! element el next-element-index)
             (swap! scissor-stack update (dec (count @scissor-stack))
                    update :next-element-index inc))

           :image
           (let [el (render-image! element cmd offset-x offset-y)]
             (insert-at-index! element el next-element-index)
             (swap! scissor-stack update (dec (count @scissor-stack))
                    update :next-element-index inc))

           :clip
           (let [{:keys [bounding-box]} cmd
                 {:keys [x y width height]} bounding-box
                 clip-el (get-or-create-element! id "div")]
             (mark-element-exists! id)
             (set-transform! clip-el (- x offset-x) (- y offset-y))
             (set-size! clip-el width height)
             (set! (.. clip-el -style -position) "absolute")
             (set! (.. clip-el -style -overflow) "hidden")
             ;; DOM ordering
             (insert-at-index! element clip-el next-element-index)
             ;; Increment parent's nextElementIndex
             (swap! scissor-stack update (dec (count @scissor-stack))
                    update :next-element-index inc)
             ;; Push new scissor with own nextElementIndex (line 487)
             (swap! scissor-stack conj {:element clip-el
                                        :offset-x x
                                        :offset-y y
                                        :next-element-index 0}))

           :clip-end
           (when (> (count @scissor-stack) 1)
             ;; Pop scissor stack (line 491)
             (swap! scissor-stack pop))

           nil))))

   ;; Cleanup unused elements (lines 507-514)
   (cleanup-unused-elements!)))

;; ============================================================================
;; RENDERER INSTANCE
;; ============================================================================

(defn create-renderer
  "Create an HTML renderer instance.

   Parameters:
   - root: Root DOM element to render into
   - fonts: Optional font mapping

   Returns renderer map."
  ([root] (create-renderer root default-fonts))
  ([root fonts]
   {:root root
    :fonts fonts}))

(defn render!
  "Render commands using a renderer instance."
  [renderer commands]
  (let [{:keys [root fonts]} renderer]
    (render-commands! root commands fonts)))

(defn clear!
  "Clear all rendered elements."
  [renderer]
  (let [{:keys [root]} renderer]
    (set! (.-innerHTML root) "")
    (reset! element-cache {})))

;; ============================================================================
;; TEXT MEASUREMENT (using hidden span)
;; ============================================================================

(defonce measure-span (atom nil))

(defn- ensure-measure-span! []
  (when-not @measure-span
    (let [span (js/document.createElement "span")]
      (set! (.. span -style -position) "absolute")
      (set! (.. span -style -visibility) "hidden")
      (set! (.. span -style -whiteSpace) "pre")
      (.appendChild js/document.body span)
      (reset! measure-span span)))
  @measure-span)

(defn measure-text
  "Measure text dimensions using DOM."
  [text config fonts]
  (let [span (ensure-measure-span!)
        font-id (or (:font-id config) 0)
        font-size (or (:font-size config) 16)
        font-family (get fonts font-id (get default-fonts 0))]
    (set! (.. span -style -fontFamily) font-family)
    (set! (.. span -style -fontSize) (str font-size "px"))
    (set! (.-textContent span) text)
    (let [rect (.getBoundingClientRect span)
          result {:width (.-width rect)
                  :height (.-height rect)}]
      (js/console.log "measure-text:" text "font-size:" font-size "result:" (clj->js result))
      result)))

(defn create-measure-fn
  "Create a text measurement function."
  ([fonts]
   (fn [text config]
     (measure-text text config fonts)))
  ([]
   (create-measure-fn default-fonts)))
