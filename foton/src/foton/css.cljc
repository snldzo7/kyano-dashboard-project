(ns foton.css
  "Consolidated CSS helpers for Foton.

   All CSS conversion functions in one place.
   Used by primitives.cljc, composites.cljc, and vary.cljc."
  (:require [clojure.string :as str]
            [foton.theme :as theme]))

;; =============================================================================
;; Color
;; =============================================================================

(defn color->css
  "Convert color token to CSS color value."
  [color]
  (cond
    (string? color) color
    (keyword? color) (theme/resolve-color color)
    (vector? color) (theme/resolve-color color)
    :else nil))

;; =============================================================================
;; Spacing
;; =============================================================================

(defn spacing->px
  "Convert spacing token to CSS px value."
  [v]
  (cond
    (number? v) (str v "px")
    (keyword? v) (str (theme/resolve-spacing v) "px")
    :else nil))

;; =============================================================================
;; Radius
;; =============================================================================

(defn radius->px
  "Convert radius token to CSS value.
   Supports: keyword, number, vector [tl tr br bl], map {:top-left ...}"
  [v]
  (cond
    (number? v) (str v "px")
    (keyword? v) (str (theme/resolve-radius v) "px")
    ;; Individual corners: [top-left top-right bottom-right bottom-left]
    (vector? v) (str/join " " (map #(str % "px") v))
    ;; Named corners: {:top-left 8 :top-right 8}
    (map? v) (str (get v :top-left 0) "px "
                  (get v :top-right 0) "px "
                  (get v :bottom-right 0) "px "
                  (get v :bottom-left 0) "px")
    :else nil))

;; =============================================================================
;; Size
;; =============================================================================

(defn size->css
  "Convert size token to CSS value.
   Supports: :fill, :hug, :viewport, number, string"
  [v]
  (cond
    (= v :fill) "100%"
    (= v :hug) "fit-content"
    (keyword? v) (theme/resolve-size v)
    (number? v) (str v "px")
    (string? v) v
    :else nil))

;; =============================================================================
;; Easing
;; =============================================================================

(defn easing->css
  "Convert easing token to CSS timing function."
  [easing]
  (cond
    (keyword? easing) (theme/resolve-easing easing)
    (string? easing) easing
    :else "ease-out"))

;; =============================================================================
;; Shadow / Effects
;; =============================================================================

(defn- parse-hex [s]
  #?(:clj (Integer/parseInt s 16)
     :cljs (js/parseInt s 16)))

(defn- effect->shadow-css
  "Convert single effect map to CSS shadow string."
  [{:keys [type x y blur spread color opacity]
    :or {x 0 y 0 blur 0 spread 0 opacity 0.1}}]
  (let [color-val (or (color->css color) "#000000")
        ;; Parse hex color and add alpha
        rgba (if (and (string? color-val) (str/starts-with? color-val "#"))
               (let [hex (subs color-val 1)
                     r (parse-hex (subs hex 0 2))
                     g (parse-hex (subs hex 2 4))
                     b (parse-hex (subs hex 4 6))]
                 (str "rgba(" r "," g "," b "," opacity ")"))
               color-val)]
    (case type
      :drop-shadow (str x "px " y "px " blur "px " spread "px " rgba)
      :inner-shadow (str "inset " x "px " y "px " blur "px " spread "px " rgba)
      nil)))

(defn shadow->css
  "Convert shadow preset or effect list to CSS box-shadow string."
  [shadow]
  (when-let [effects (theme/resolve-shadow shadow)]
    (when (seq effects)
      (->> effects
           (filter #(#{:drop-shadow :inner-shadow} (:type %)))
           (map effect->shadow-css)
           (remove nil?)
           (str/join ", ")))))

(defn effects->css
  "Convert vector of effects to CSS style map.
   Returns {:box-shadow \"...\" :filter \"...\" :backdrop-filter \"...\"}."
  [effects]
  (when (seq effects)
    (let [shadows (filter #(#{:drop-shadow :inner-shadow} (:type %)) effects)
          filters (filter #(= :layer-blur (:type %)) effects)
          bg-blurs (filter #(= :background-blur (:type %)) effects)]
      (cond-> {}
        (seq shadows) (assoc :box-shadow (str/join ", " (map effect->shadow-css shadows)))
        (seq filters) (assoc :filter (str/join " " (map #(str "blur(" (:blur %) "px)") filters)))
        (seq bg-blurs) (assoc :backdrop-filter (str/join " " (map #(str "blur(" (:blur %) "px)") bg-blurs)))))))

(defn resolve-effects
  "Resolve effects - keyword preset or vector of effect maps."
  [effects]
  (cond
    (keyword? effects) (theme/resolve-shadow effects)
    (vector? effects) effects
    (map? effects) [effects]
    :else nil))

;; =============================================================================
;; Stroke
;; =============================================================================

(defn stroke->css
  "Convert Figma-style stroke to CSS.
   {:color :border :width 1 :position :inside :style :solid}

   Positions:
   - :inside  -> border with box-sizing: border-box (default)
   - :outside -> outline
   - :center  -> border with adjusted box-sizing"
  [stroke]
  (when (map? stroke)
    (let [{:keys [color width position style]
           :or {width 1 position :inside style :solid}} stroke
          color-css (color->css (or color :border))
          style-css (case style
                      :dashed "dashed"
                      :dotted "dotted"
                      :solid "solid"
                      "solid")]
      (case position
        :outside {:outline (str width "px " style-css " " color-css)
                  :outline-offset "0"}
        :center  {:border (str width "px " style-css " " color-css)
                  :margin (str "-" (/ width 2) "px")}
        ;; :inside (default)
        {:border (str width "px " style-css " " color-css)
         :box-sizing "border-box"}))))

;; =============================================================================
;; Transform
;; =============================================================================

(def transform-properties
  "Backend-agnostic transform properties."
  #{:translate-x :translate-y :scale :rotate :skew-x :skew-y})

(defn build-transform
  "Build CSS transform string from semantic transform properties."
  [{:keys [translate-x translate-y scale rotate skew-x skew-y transform]}]
  (let [parts (cond-> []
                translate-x (conj (str "translateX(" translate-x "px)"))
                translate-y (conj (str "translateY(" translate-y "px)"))
                scale (conj (str "scale(" scale ")"))
                rotate (conj (str "rotate(" rotate "deg)"))
                skew-x (conj (str "skewX(" skew-x "deg)"))
                skew-y (conj (str "skewY(" skew-y "deg)")))]
    (cond
      (seq parts) (str/join " " parts)
      transform transform
      :else nil)))

;; =============================================================================
;; Transition
;; =============================================================================

(defn- prop->css-property
  "Map Foton property name to CSS property name."
  [prop]
  (case prop
    :fill "background-color"
    :color "color"
    :opacity "opacity"
    :transform "transform"
    :radius "border-radius"
    :width "width"
    :height "height"
    :padding "padding"
    :margin "margin"
    :border "border"
    :shadow "box-shadow"
    (name prop)))

(defn build-transition
  "Build CSS transition string from transition spec.
   {:duration 300 :easing :ease-out :properties [:fill :transform]}
   => 'background-color 300ms ease-out, transform 300ms ease-out'"
  [transition]
  (cond
    ;; Simple number = transition all
    (number? transition)
    (str "all " transition "ms ease-out")

    ;; Map with options
    (map? transition)
    (let [{:keys [duration easing properties delay]
           :or {duration 200 easing :ease-out delay 0}} transition
          timing (str duration "ms " (easing->css easing)
                      (when (pos? delay) (str " " delay "ms")))]
      (if properties
        (->> properties
             (map #(str (prop->css-property %) " " timing))
             (str/join ", "))
        (str "all " timing)))

    :else nil))

;; =============================================================================
;; Gradient
;; =============================================================================

(defn gradient->css
  "Convert gradient spec to CSS.
   {:type :linear :angle 90 :stops [{:color :primary :position 0} {:color :secondary :position 100}]}
   {:type :radial :stops [...]}
   {:type :conic :angle 0 :stops [...]}"
  [{:keys [type angle stops]}]
  (when (seq stops)
    (let [stop-css (->> stops
                        (map (fn [{:keys [color position]}]
                               (str (color->css color) " " position "%")))
                        (str/join ", "))]
      (case type
        :linear (str "linear-gradient(" (or angle 180) "deg, " stop-css ")")
        :radial (str "radial-gradient(circle, " stop-css ")")
        :conic (str "conic-gradient(from " (or angle 0) "deg, " stop-css ")")
        nil))))

;; =============================================================================
;; Blend Mode
;; =============================================================================

(defn blend->css
  "Convert blend mode keyword to CSS mix-blend-mode value."
  [blend]
  (when blend
    (name blend)))

;; =============================================================================
;; Frame Style Builder
;; =============================================================================

(defn frame-style
  "Build CSS style map from frame attributes.

   Sane defaults:
   - flex-direction: column (vertical by default)
   - align-items: stretch
   - box-sizing: border-box"
  [{:keys [fill radius padding margin direction gap align justify
           width height min-width max-width min-height max-height
           grow shrink basis wrap overflow overflow-x overflow-y clip
           scroll-behavior scrollbar scroll-snap scroll-snap-type
           mask clip-path
           cursor opacity border transition
           ;; Positioning (P0 Figma)
           position top left right bottom z visible
           ;; Gradient (P0 Figma)
           gradient
           ;; Background Image (P0 Figma)
           background-image background-size background-position
           ;; Figma properties
           stroke effects shadow blend]
    :as attrs}]
  (let [computed-transform (build-transform attrs)
        resolved-effects (or (resolve-effects effects) (resolve-effects shadow))
        effects-css (effects->css resolved-effects)
        stroke-css (stroke->css stroke)]
    (cond-> {:display "flex"
             :flex-direction "column"  ; Vertical by default (most common)
             :align-items "stretch"    ; Fill container width by default
             :box-sizing "border-box"}
      ;; Background & Border
      fill (assoc :background-color (color->css fill))
      gradient (assoc :background (gradient->css gradient))
      background-image (assoc :background-image (str "url(" background-image ")"))
      background-size (assoc :background-size (if (keyword? background-size)
                                                 (name background-size)
                                                 background-size))
      background-position (assoc :background-position (if (keyword? background-position)
                                                         (name background-position)
                                                         background-position))
      radius (assoc :border-radius (radius->px radius))
      border (assoc :border (str "1px solid " (color->css border)))

      ;; Figma stroke (overrides simple border)
      stroke-css (merge stroke-css)

      ;; Figma effects (shadows, blurs)
      effects-css (merge effects-css)

      ;; Blend mode
      blend (assoc :mix-blend-mode (blend->css blend))

      ;; Spacing
      padding (assoc :padding (spacing->px padding))
      margin (assoc :margin (spacing->px margin))
      gap (assoc :gap (spacing->px gap))

      ;; Direction & Wrap
      (= direction :horizontal) (assoc :flex-direction "row")
      (= direction :vertical) (assoc :flex-direction "column")
      (= wrap :wrap) (assoc :flex-wrap "wrap")
      (= wrap :nowrap) (assoc :flex-wrap "nowrap")

      ;; Alignment
      (= align :start) (assoc :align-items "flex-start")
      (= align :center) (assoc :align-items "center")
      (= align :end) (assoc :align-items "flex-end")
      (= align :stretch) (assoc :align-items "stretch")
      (= align :baseline) (assoc :align-items "baseline")

      ;; Justify
      (= justify :start) (assoc :justify-content "flex-start")
      (= justify :center) (assoc :justify-content "center")
      (= justify :end) (assoc :justify-content "flex-end")
      (= justify :space-between) (assoc :justify-content "space-between")
      (= justify :space-around) (assoc :justify-content "space-around")
      (= justify :space-evenly) (assoc :justify-content "space-evenly")

      ;; Sizing
      width (assoc :width (size->css width))
      height (assoc :height (size->css height))

      ;; Constraints
      min-width (assoc :min-width (size->css min-width))
      max-width (assoc :max-width (size->css max-width))
      min-height (assoc :min-height (size->css min-height))
      max-height (assoc :max-height (size->css max-height))

      ;; Flex Item
      grow (assoc :flex-grow grow)
      shrink (assoc :flex-shrink shrink)
      basis (assoc :flex-basis (if (number? basis) (str basis "px") basis))

      ;; Overflow & Clipping
      (= overflow :hidden) (assoc :overflow "hidden")
      (= overflow :scroll) (assoc :overflow "auto")
      (= overflow :auto) (assoc :overflow "auto")
      (= overflow :visible) (assoc :overflow "visible")
      clip (assoc :overflow "hidden")

      ;; Directional overflow
      overflow-x (assoc :overflow-x (name overflow-x))
      overflow-y (assoc :overflow-y (name overflow-y))

      ;; Scroll behavior
      (= scroll-behavior :smooth) (assoc :scroll-behavior "smooth")
      (= scroll-behavior :auto) (assoc :scroll-behavior "auto")

      ;; Scrollbar styling
      (= scrollbar :hidden) (merge {:-webkit-scrollbar-width "none"
                                    :scrollbar-width "none"
                                    :-ms-overflow-style "none"})
      (= scrollbar :thin) (assoc :scrollbar-width "thin")

      ;; Scroll snap (on children)
      (= scroll-snap :start) (assoc :scroll-snap-align "start")
      (= scroll-snap :center) (assoc :scroll-snap-align "center")
      (= scroll-snap :end) (assoc :scroll-snap-align "end")

      ;; Scroll snap type (on container)
      (= scroll-snap-type :x) (assoc :scroll-snap-type "x mandatory")
      (= scroll-snap-type :y) (assoc :scroll-snap-type "y mandatory")
      (= scroll-snap-type :both) (assoc :scroll-snap-type "both mandatory")

      ;; Masking & Clip path
      mask (assoc :mask-image (if (keyword? mask)
                                (case mask
                                  :fade-bottom "linear-gradient(to bottom, black 80%, transparent)"
                                  :fade-top "linear-gradient(to top, black 80%, transparent)"
                                  :fade-edges "linear-gradient(to right, transparent, black 10%, black 90%, transparent)"
                                  (name mask))
                                mask))
      clip-path (assoc :clip-path (if (keyword? clip-path)
                                    (case clip-path
                                      :circle "circle(50%)"
                                      :ellipse "ellipse(50% 50%)"
                                      :inset "inset(0)"
                                      (name clip-path))
                                    clip-path))

      ;; Transform
      computed-transform (assoc :transform computed-transform)

      ;; Cursor
      cursor (assoc :cursor (name cursor))

      ;; Opacity
      opacity (assoc :opacity opacity)

      ;; Transition
      transition (assoc :transition (build-transition transition))

      ;; Positioning (P0 Figma)
      position (assoc :position (name position))
      top (assoc :top (size->css top))
      left (assoc :left (size->css left))
      right (assoc :right (size->css right))
      bottom (assoc :bottom (size->css bottom))
      z (assoc :z-index z)
      (= visible false) (assoc :display "none"))))

;; =============================================================================
;; Text Style Builder
;; =============================================================================

(defn text-style
  "Build CSS style map from text attributes.

   Sane defaults:
   - font-family: Inter (sans)
   - color: [:text :primary]
   - line-height: 1.5 (readable)

   Figma-matching text properties:
   - :family          Font family (:sans, :serif, :mono, or string)
   - :size            Font size (number px or string)
   - :weight          Font weight (400, 500, 600, 700)
   - :style           Font style (:normal, :italic)
   - :color           Color token
   - :opacity         Opacity (0-1)
   - :tracking        Letter spacing in em
   - :line-height     Line height multiplier
   - :paragraph-spacing  Margin bottom for paragraphs
   - :text-align      :left | :center | :right | :justify
   - :valign          Vertical align (:top, :middle, :bottom, :baseline)
   - :decoration      :none | :underline | :line-through | :overline
   - :transform       :none | :uppercase | :lowercase | :capitalize
   - :truncate        Boolean - single line ellipsis
   - :max-lines       Number - multi-line truncation (line-clamp)
   - :wrap            :normal | :nowrap | :pre | :pre-wrap | :pre-line
   - :break           :normal | :break-all | :break-word | :keep-all
   - :selectable      Boolean - user-select"
  [{:keys [family size weight style color opacity
           tracking line-height paragraph-spacing
           text-align valign decoration transform
           truncate max-lines wrap break selectable]}]
  (let [family-css (or (theme/resolve-font family) (theme/resolve-font :sans))
        color-css (or (color->css color) (color->css [:text :primary]))]
    (cond-> {:font-family family-css
             :color color-css
             :line-height (or line-height 1.5)}
      ;; Typography overrides
      size (assoc :font-size (if (number? size) (str size "px") size))
      weight (assoc :font-weight weight)
      style (assoc :font-style (name style))

      ;; Color & opacity
      color (assoc :color (color->css color))
      opacity (assoc :opacity opacity)

      ;; Spacing
      tracking (assoc :letter-spacing (str tracking "em"))
      line-height (assoc :line-height line-height)
      paragraph-spacing (assoc :margin-bottom (spacing->px paragraph-spacing))

      ;; Alignment
      text-align (assoc :text-align (name text-align))
      valign (assoc :vertical-align (name valign))

      ;; Decoration
      decoration (assoc :text-decoration (name decoration))
      transform (assoc :text-transform (name transform))

      ;; Truncation - single line
      truncate (merge {:overflow "hidden"
                       :text-overflow "ellipsis"
                       :white-space "nowrap"})

      ;; Truncation - multi-line (line-clamp)
      max-lines (merge {:overflow "hidden"
                        :display "-webkit-box"
                        :-webkit-line-clamp max-lines
                        :-webkit-box-orient "vertical"})

      ;; Wrap & break
      wrap (assoc :white-space (name wrap))
      break (assoc :word-break (name break))

      ;; Selection
      (= selectable false) (assoc :user-select "none")
      (= selectable true) (assoc :user-select "text"))))

;; =============================================================================
;; Icon Style Builder
;; =============================================================================

(defn icon-style
  "Build CSS style map from icon attributes.

   Sane defaults:
   - size: :md (24px)
   - color: [:text :primary]"
  [{:keys [size color]}]
  (let [px (case size :xs 16 :sm 20 :md 24 :lg 32 :xl 40 24)
        color-css (or (color->css color) (color->css [:text :primary]))]
    {:display "flex"
     :align-items "center"
     :justify-content "center"
     :width (str px "px")
     :height (str px "px")
     :font-size (str px "px")
     :line-height "1"
     :color color-css}))

;; =============================================================================
;; Input Style Builder
;; =============================================================================

(defn input-style
  "Build CSS style map from input attributes."
  [{:keys [size radius disabled]}]
  (let [px (case size :sm 8 :lg 16 12)]
    (cond-> {:background-color (color->css :surface)
             :border (str "1px solid " (color->css [:border :default]))
             :color (color->css [:text :primary])
             :padding (str px "px")
             :border-radius (radius->px (or radius :md))
             :outline "none"}
      disabled (assoc :opacity 0.5 :cursor "not-allowed"))))

;; =============================================================================
;; Textarea Style Builder
;; =============================================================================

(defn textarea-style
  "Build CSS style map from textarea attributes."
  [{:keys [size radius disabled]}]
  (let [px (case size :sm 8 :lg 16 12)]
    (cond-> {:background-color (color->css :surface)
             :border (str "1px solid " (color->css [:border :default]))
             :color (color->css [:text :primary])
             :padding (str px "px")
             :border-radius (radius->px (or radius :md))
             :outline "none"
             :resize "none"}
      disabled (assoc :opacity 0.5 :cursor "not-allowed"))))

;; =============================================================================
;; Link Style Builder
;; =============================================================================

(defn link-style
  "Build CSS style map from link attributes."
  [{:keys [color underline]}]
  (cond-> {:color (color->css (or color [:text :primary]))}
    (= underline :none) (assoc :text-decoration "none")
    (not= underline :none) (assoc :text-decoration "underline")))

;; =============================================================================
;; Image Style Builder
;; =============================================================================

(defn image-style
  "Build CSS style map from image attributes."
  [{:keys [radius fit width height]}]
  (cond-> {}
    radius (assoc :border-radius (radius->px radius))
    (= fit :cover) (assoc :object-fit "cover")
    (= fit :contain) (assoc :object-fit "contain")
    (= fit :fill) (assoc :object-fit "fill")
    width (assoc :width (size->css width))
    height (assoc :height (size->css height))))

;; =============================================================================
;; Video Style Builder
;; =============================================================================

(defn video-style
  "Build CSS style map from video attributes."
  [{:keys [radius width height]}]
  (cond-> {}
    radius (assoc :border-radius (radius->px radius))
    width (assoc :width (size->css width))
    height (assoc :height (size->css height))))

;; =============================================================================
;; SVG Style Builder
;; =============================================================================

(defn svg-style
  "Build CSS style map from svg attributes."
  [{:keys [size color width height]}]
  (let [px (case size :xs 16 :sm 20 :md 24 :lg 32 :xl 40 nil)]
    (cond-> {}
      px (assoc :width (str px "px") :height (str px "px"))
      color (assoc :color (color->css color))
      (number? width) (assoc :width (str width "px"))
      (number? height) (assoc :height (str height "px")))))

;; =============================================================================
;; Vary CSS Helpers
;; =============================================================================

(defn attr->css-property
  "Map Foton attribute key to CSS property name."
  [attr-key]
  (case attr-key
    :fill "background-color"
    :color "color"
    :border "border-color"
    :radius "border-radius"
    :padding "padding"
    :margin "margin"
    :gap "gap"
    :opacity "opacity"
    :width "width"
    :height "height"
    :max-height "max-height"
    :min-height "min-height"
    :max-width "max-width"
    :min-width "min-width"
    :cursor "cursor"
    :shadow "box-shadow"
    :blend "mix-blend-mode"
    nil))

(defn attr->css-value
  "Convert Foton attribute value to CSS value."
  [attr-key attr-val]
  (case attr-key
    :fill (color->css attr-val)
    :color (color->css attr-val)
    :border (color->css attr-val)
    :radius (radius->px attr-val)
    :padding (spacing->px attr-val)
    :margin (spacing->px attr-val)
    :gap (spacing->px attr-val)
    :opacity attr-val
    :cursor (if (keyword? attr-val) (name attr-val) attr-val)
    :shadow (shadow->css attr-val)
    :blend (if (keyword? attr-val) (name attr-val) attr-val)
    ;; Size values
    (:width :height :max-height :min-height :max-width :min-width)
    (size->css attr-val)
    ;; Default
    (cond
      (keyword? attr-val) (name attr-val)
      :else attr-val)))

(defn style->css-string
  "Convert CSS style map to CSS string."
  [style-map]
  (->> style-map
       (map (fn [[k v]] (str (name k) ":" v)))
       (str/join ";")))

(defn gen-class-name
  "Generate unique class name from attributes hash."
  [attrs]
  (str "fv-" (Math/abs (hash attrs))))

;; =============================================================================
;; Specter Path Support (for vary.cljc)
;; =============================================================================

(defn path->css-property
  "Convert Specter path to CSS property name."
  [path]
  (cond
    (= path [:stroke :width]) "border-width"
    (= path [:stroke :color]) "border-color"
    :else nil))

(defn path->css-value
  "Convert Specter path value to CSS value."
  [path value]
  (cond
    (= path [:stroke :width]) (str value "px")
    (= path [:stroke :color]) (color->css value)
    :else nil))
