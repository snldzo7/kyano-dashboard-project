(ns clay.dsl.normalize
  "Alternative multi-method normalization system using schema-driven dispatch.

  This namespace demonstrates an alternative approach to the individual
  function normalization in clay.dsl. It uses a single multi-method that
  dispatches on [key-spec value-spec] pairs inferred from Malli schemas.

  Comparison:
  - clay.dsl: Individual functions (normalize-size, normalize-padding, etc.)
  - clay.dsl.normalize: Single multi-method with schema-driven dispatch

  Philosophy:
  - Loose schemas define WHAT data looks like (structure, not meaning)
  - Multi-methods define HOW to transform it (meaning emerges from usage)
  - No rigid coupling - schemas and transformations are separate concerns
  - Selection-based system - can query and transform based on structure
  - 100% schema-driven - NO hardcoded logic, schemas are single source of truth"
  (:require [clay.color :as c]
            [clay.schema :as schema]
            [malli.core :as m]
            [hyperfiddle.rcf :refer [tests]]))

#?(:clj (hyperfiddle.rcf/enable!))

;; ============================================================================
;; DISPATCH FUNCTION - Schema-driven type inference
;; ============================================================================

(defn- infer-value-spec
  "Infer the value specification type using Malli schema validation.

  Returns a keyword representing the inferred type.

  Philosophy:
  - Schemas define WHAT already-normalized data looks like
  - Use canonical output schemas (Color, Padding, etc.) to detect normalized values
  - For DSL input values, use basic types - meaning emerges from property key context
  - Property key determines which transformation to apply"
  [value]
  (cond
    ;; === Canonical output schemas (already normalized) ===
    ;; These are specific enough to reliably identify normalized values
    (m/validate schema/Color value) ::color
    (m/validate schema/Padding value) ::padding
    (m/validate schema/Sizing value) ::sizing
    (m/validate schema/SizingAxis value) ::sizing-axis
    (m/validate schema/Vector2 value) ::vector2
    (m/validate schema/CornerRadius value) ::corner-radius
    (m/validate schema/ChildAlignment value) ::child-alignment

    ;; === Basic types ===
    ;; Meaning emerges from property key context
    ;; (e.g., :grow as keyword means different things for :size vs :bg)
    (map? value) ::map
    (vector? value) ::vector
    (keyword? value) ::keyword
    (number? value) ::number
    (boolean? value) ::boolean
    (string? value) ::string
    (fn? value) ::function
    :else ::unknown))

(defn dispatch-normalize
  "Dispatch function for normalize multi-method.

  Takes a property key and value, returns [key-spec value-spec].

  - key-spec: Semantic category of the property (::sizing-prop, ::color-prop, etc.)
  - value-spec: Inferred type from Malli schemas or fallback types

  Example:
    (dispatch-normalize :size [:grow :fit]) => [::sizing-prop ::vector]
    (dispatch-normalize :bg :red-500) => [::color-prop ::keyword]"
  [k v]
  (let [key-spec (case k
                   ;; Sizing
                   (:size :sizing) ::sizing-prop
                   (:width :height) ::dimension-prop

                   ;; Spacing
                   (:padding :pad) ::padding-prop
                   (:gap :child-gap) ::gap-prop
                   (:radius :corner-radius) ::radius-prop

                   ;; Alignment
                   :align ::align-prop

                   ;; Colors
                   (:color :bg :background) ::color-prop

                   ;; Border
                   :border ::border-prop

                   ;; Position
                   :floating ::floating-prop

                   ;; Behavior
                   :scroll ::scroll-prop
                   :constrain ::constrain-prop
                   :wrap ::wrap-prop

                   ;; Image
                   (:image :img) ::image-prop

                   ;; Default
                   ::unknown-prop)
        value-spec (infer-value-spec v)]
    [key-spec value-spec]))

;; ============================================================================
;; MULTI-METHOD
;; ============================================================================

(defmulti normalize
  "Normalize property value based on key and value type.

  Dispatch: [key-spec value-spec]
  Returns: normalized value according to canonical schema

  Usage:
    (normalize :size [:grow :fit])
    (normalize :padding 16)
    (normalize :color :red-500)"
  dispatch-normalize)

;; ============================================================================
;; DIMENSION NORMALIZATION (width, height)
;; Schema-driven: Uses Malli parser to extract structured data
;; ============================================================================

(defmethod normalize [::dimension-prop ::keyword]
  [_ value]
  ;; Schema: DslDimensionKeyword defines valid keywords (:grow, :fit)
  ;; Transformation: Create SizingAxis with type
  {:type value})

(defmethod normalize [::dimension-prop ::number]
  [_ value]
  ;; Schema: DslDimensionFixed defines numeric dimensions
  ;; Transformation: Create SizingAxis with :fixed type
  {:type :fixed :value value})

(defmethod normalize [::dimension-prop ::vector]
  [_ value]
  (cond
    ;; Percentage: [:% 50] or [:percent 0.5]
    ;; Schema: DslDimensionPercent → {:type :percent :value normalized}
    (m/validate schema/DslDimensionPercent value)
    (let [[_ val] (m/parse schema/DslDimensionPercent value)]
      {:type :percent :value (double (if (> val 1) (/ val 100) val))})

    ;; Constrained with no args: [:grow] or [:fit]
    ;; Schema: DslDimensionConstrainedNoArgs → {:type type-kw}
    (m/validate schema/DslDimensionConstrainedNoArgs value)
    (let [[type-kw] (m/parse schema/DslDimensionConstrainedNoArgs value)]
      {:type type-kw})

    ;; Constrained with min only: [:grow 100]
    ;; Schema: DslDimensionConstrainedMin → {:type type-kw :min min-val}
    (m/validate schema/DslDimensionConstrainedMin value)
    (let [[type-kw min-val] (m/parse schema/DslDimensionConstrainedMin value)]
      {:type type-kw :min min-val})

    ;; Constrained with min and max: [:grow 100 500]
    ;; Schema: DslDimensionConstrainedMinMax → {:type type-kw :min min-val :max max-val}
    (m/validate schema/DslDimensionConstrainedMinMax value)
    (let [[type-kw min-val max-val] (m/parse schema/DslDimensionConstrainedMinMax value)]
      {:type type-kw :min min-val :max max-val})

    ;; Constrained with map: [:grow {:min 100 :max 500}]
    ;; Schema: DslDimensionConstrainedMap → merge {:type type-kw} with map
    (m/validate schema/DslDimensionConstrainedMap value)
    (let [[type-kw constraints] (m/parse schema/DslDimensionConstrainedMap value)]
      (merge {:type type-kw} constraints))

    ;; Explicit fixed: [:fixed 300]
    ;; Schema: DslDimensionExplicitFixed → {:type :fixed :value val}
    (m/validate schema/DslDimensionExplicitFixed value)
    (let [[_ val] (m/parse schema/DslDimensionExplicitFixed value)]
      {:type :fixed :value val})

    :else value))

(defmethod normalize [::dimension-prop ::map]
  [_ value]
  value) ; Already normalized

(defmethod normalize [::dimension-prop ::sizing-axis]
  [_ value]
  value) ; Already normalized SizingAxis

;; ============================================================================
;; SIZING NORMALIZATION (size property)
;; Implicit schema: DslSizing → Sizing
;; ============================================================================

(defmethod normalize [::sizing-prop ::keyword]
  [_ value]
  {:width (normalize :width value)
   :height (normalize :height value)})

(defmethod normalize [::sizing-prop ::number]
  [_ value]
  {:width (normalize :width value)
   :height (normalize :height value)})

(defmethod normalize [::sizing-prop ::vector]
  [_ value]
  (let [[w h] value]
    {:width (normalize :width w)
     :height (normalize :height h)}))

(defmethod normalize [::sizing-prop ::map]
  [_ value]
  value) ; Already normalized

(defmethod normalize [::sizing-prop ::sizing]
  [_ value]
  value) ; Already normalized Sizing

;; ============================================================================
;; PADDING NORMALIZATION
;; Schema-driven: Uses Malli parser to extract structured data
;; ============================================================================

(defmethod normalize [::padding-prop ::number]
  [_ value]
  ;; Schema: DslPaddingUniform → all sides
  (when (m/validate schema/DslPaddingUniform value)
    {:top value :right value :bottom value :left value}))

(defmethod normalize [::padding-prop ::vector]
  [_ value]
  (cond
    ;; Vertical/horizontal: [16 8] → [vertical horizontal]
    ;; Schema: DslPaddingVerticalHorizontal
    (m/validate schema/DslPaddingVerticalHorizontal value)
    (let [[v h] (m/parse schema/DslPaddingVerticalHorizontal value)]
      {:top v :right h :bottom v :left h})

    ;; Four sides: [10 20 30 40] → [top right bottom left]
    ;; Schema: DslPaddingFourSides
    (m/validate schema/DslPaddingFourSides value)
    (let [[t r b l] (m/parse schema/DslPaddingFourSides value)]
      {:top t :right r :bottom b :left l})

    :else {:top 0 :right 0 :bottom 0 :left 0}))

(defmethod normalize [::padding-prop ::map]
  [_ value]
  value) ; Already normalized

(defmethod normalize [::padding-prop ::padding]
  [_ value]
  value) ; Already normalized Padding

;; ============================================================================
;; GAP NORMALIZATION (simple number pass-through)
;; ============================================================================

(defmethod normalize [::gap-prop ::number]
  [_ value]
  value)

;; ============================================================================
;; RADIUS NORMALIZATION
;; Schema-driven: Uses Malli parser to extract structured data
;; ============================================================================

(defmethod normalize [::radius-prop ::number]
  [_ value]
  ;; Schema: DslRadiusUniform → all corners
  (when (m/validate schema/DslRadiusUniform value)
    {:top-left value :top-right value
     :bottom-left value :bottom-right value}))

(defmethod normalize [::radius-prop ::vector]
  [_ value]
  ;; Schema: DslRadiusFourCorners → [tl tr bl br]
  (when (m/validate schema/DslRadiusFourCorners value)
    (let [[tl tr bl br] (m/parse schema/DslRadiusFourCorners value)]
      {:top-left tl :top-right tr
       :bottom-left bl :bottom-right br})))

(defmethod normalize [::radius-prop ::map]
  [_ value]
  value) ; Already normalized

(defmethod normalize [::radius-prop ::corner-radius]
  [_ value]
  value)

;; ============================================================================
;; ALIGNMENT NORMALIZATION
;; Schema-driven: Uses Malli parser to extract structured data
;; ============================================================================

(defmethod normalize [::align-prop ::keyword]
  [_ value]
  ;; Schema: DslAlignKeyword → keyword shortcuts with semantic mapping
  (when (m/validate schema/DslAlignKeyword value)
    (case value
      :center {:x :center :y :center}
      :left {:x :left :y :center}
      :right {:x :right :y :center}
      :top {:x :center :y :top}
      :bottom {:x :center :y :bottom}
      {:x :left :y :top})))

(defmethod normalize [::align-prop ::vector]
  [_ value]
  ;; Schema: DslAlignTuple → [x y]
  (when (m/validate schema/DslAlignTuple value)
    (let [[x y] (m/parse schema/DslAlignTuple value)]
      {:x x :y y})))

(defmethod normalize [::align-prop ::map]
  [_ value]
  ;; Schema: DslAlignMap → already explicit map form
  (when (m/validate schema/DslAlignMap value)
    value))

(defmethod normalize [::align-prop ::child-alignment]
  [_ value]
  value) ; Already normalized ChildAlignment

;; ============================================================================
;; COLOR NORMALIZATION
;; Implicit schema: DslColor → Color
;; ============================================================================

(defmethod normalize [::color-prop ::keyword]
  [_ value]
  (c/resolve-color value))

(defmethod normalize [::color-prop ::vector]
  [_ value]
  (c/resolve-color value))

(defmethod normalize [::color-prop ::string]
  [_ value]
  (c/resolve-color value))

(defmethod normalize [::color-prop ::function]
  [_ value]
  value) ; Keep functions for deferred evaluation

(defmethod normalize [::color-prop ::map]
  [_ value]
  value) ; Already normalized

(defmethod normalize [::color-prop ::color]
  [_ value]
  value) ; Already normalized Color

;; ============================================================================
;; BORDER NORMALIZATION
;; Schema-driven: Uses Malli parser for nested normalization
;; ============================================================================

(defmethod normalize [::border-prop ::number]
  [_ value]
  ;; Schema: DslBorderWidth → width only
  ;; Validation: Schema defines structure, transformation is unconditional
  {:width value :color nil :radius nil})

(defmethod normalize [::border-prop ::vector]
  [_ value]
  ;; Schema: DslBorderTuple2 or DslBorderTuple3
  (cond
    ;; Two-element tuple: [color width]
    (m/validate schema/DslBorderTuple2 value)
    (let [[color-val width-val] (m/parse schema/DslBorderTuple2 value)]
      {:width width-val
       :color (normalize :color color-val)
       :radius nil})

    ;; Three-element tuple: [color width radius]
    (m/validate schema/DslBorderTuple3 value)
    (let [[color-val width-val radius-val] (m/parse schema/DslBorderTuple3 value)]
      {:width width-val
       :color (normalize :color color-val)
       :radius radius-val})

    :else nil))

(defmethod normalize [::border-prop ::map]
  [_ value]
  ;; Schema: DslBorderMap defines structure
  ;; Transformation: Normalize nested color and radius
  (let [color-val (:color value)
        radius-val (:radius value)]
    {:width (:width value 1)
     :color (if color-val
              (normalize :color color-val)
              nil)
     :radius (if radius-val
               (normalize :radius radius-val)
               nil)}))
;; ============================================================================
;; VECTOR2 PROPERTIES (floating position)
;; Schema-driven: Uses Malli parser to extract structured data
;; ============================================================================

(defmethod normalize [::floating-prop ::vector]
  [_ value]
  ;; Schema: DslFloatingVector → [x y] offset
  (when (m/validate schema/DslFloatingVector value)
    (let [[x y] (m/parse schema/DslFloatingVector value)]
      {:x x :y y})))

(defmethod normalize [::floating-prop ::map]
  [_ value]
  ;; Schema: DslFloatingMap → full config with defaults
  (when (m/validate schema/DslFloatingMap value)
    {:to (:to value :none)
     :at (:at value nil)
     :offset (:offset value [0 0])
     :z (:z value nil)}))

(defmethod normalize [::floating-prop ::vector2]
  [_ value]
  value) ; Already normalized

;; ============================================================================
;; SCROLL NORMALIZATION
;; Schema-driven: Uses Malli parser to extract structured data
;; ============================================================================

(defmethod normalize [::scroll-prop ::boolean]
  [_ value]
  ;; Schema: DslScrollBoolean → boolean shortcut
  (when (m/validate schema/DslScrollBoolean value)
    {:direction :vertical :show-scrollbars true}))

(defmethod normalize [::scroll-prop ::keyword]
  [_ value]
  ;; Schema: DslScrollKeyword → axis keyword
  (when (m/validate schema/DslScrollKeyword value)
    {:direction value :show-scrollbars true}))

(defmethod normalize [::scroll-prop ::map]
  [_ value]
  ;; Schema: DslScrollMap → explicit config with defaults
  (when (m/validate schema/DslScrollMap value)
    {:direction (:direction value :vertical)
     :show-scrollbars (:show-scrollbars value true)}))

;; ============================================================================
;; CONSTRAIN NORMALIZATION
;; ============================================================================

(defmethod normalize [::constrain-prop ::map]
  [_ value]
  (when value
    (if (or (contains? value :min-width)
            (contains? value :max-width)
            (contains? value :min-height)
            (contains? value :max-height))
      value ; Already normalized
      (cond-> {}
        (:min-width value) (assoc :min-width (:min-width value))
        (:max-width value) (assoc :max-width (:max-width value))
        (:min-height value) (assoc :min-height (:min-height value))
        (:max-height value) (assoc :max-height (:max-height value))))))

;; ============================================================================
;; WRAP NORMALIZATION
;; Schema-driven: Uses Malli parser to extract structured data
;; ============================================================================

(defmethod normalize [::wrap-prop ::keyword]
  [_ value]
  ;; Schema: DslWrapKeyword → keyword form
  (when (m/validate schema/DslWrapKeyword value)
    value))

(defmethod normalize [::wrap-prop ::boolean]
  [_ value]
  ;; Schema: DslWrapBoolean → true = :words, false = :none
  (when (m/validate schema/DslWrapBoolean value)
    (if value :words :none)))

;; ============================================================================
;; IMAGE NORMALIZATION
;; Schema-driven: Uses Malli parser to extract structured data
;; ============================================================================

(defmethod normalize [::image-prop ::string]
  [_ value]
  ;; Schema: DslImageString → URL shorthand
  (when (m/validate schema/DslImageString value)
    {:src value :aspect nil :fit :contain}))

(defmethod normalize [::image-prop ::keyword]
  [_ value]
  ;; Schema: DslImageKeyword → named reference
  (when (m/validate schema/DslImageKeyword value)
    {:src value :aspect nil :fit :contain}))

(defmethod normalize [::image-prop ::function]
  [_ value]
  ;; Schema: DslImageFunction → dynamic image
  (when (m/validate schema/DslImageFunction value)
    {:src value :aspect nil :fit :contain}))

(defmethod normalize [::image-prop ::map]
  [_ value]
  ;; Schema: DslImageMap → explicit config with defaults
  (when (m/validate schema/DslImageMap value)
    {:src (:src value)
     :aspect (:aspect value nil)
     :fit (:fit value :contain)
     :position (:position value nil)}))

;; ============================================================================
;; DEFAULT & UNKNOWN
;; ============================================================================

(defmethod normalize :default
  [k v]
  v) ; Pass through unknown properties

;; ============================================================================
;; PUBLIC API
;; ============================================================================

(defn normalize-props
  "Normalize all properties in a props map using multi-method dispatch.

  This is the alternative to clay.dsl/normalize-props. It uses a single
  multi-method that dispatches on [key-spec value-spec] pairs.

  Usage:
    (normalize-props {:size [:grow :fit]
                      :padding 16
                      :bg :red-500})

  Returns: props map with all values normalized to canonical schema"
  [props]
  (reduce-kv
    (fn [acc k v]
      (let [normalized (normalize k v)]
        (if (some? normalized)
          (assoc acc k normalized)
          acc)))
    {}
    props))

;; ============================================================================
;; RCF TESTS - VERIFY MULTI-METHOD DISPATCH
;; ============================================================================

#?(:clj
   (tests
    "Dispatch function returns correct values"
    (dispatch-normalize :width :grow) := [::dimension-prop ::keyword]
    (dispatch-normalize :padding 16) := [::padding-prop ::number]
    (dispatch-normalize :size [:grow :fit]) := [::sizing-prop ::vector]

    "Multi-method dimension normalization"
    (normalize :width :grow) := {:type :grow}
    (normalize :width 300) := {:type :fixed :value 300}
    (normalize :width [:grow 100 500]) := {:type :grow :min 100 :max 500}
    (normalize :width [:% 50]) := {:type :percent :value 0.5}

    "Multi-method dimension - constrained no args"
    (normalize :width [:grow]) := {:type :grow}
    (normalize :width [:fit]) := {:type :fit}

    "Multi-method dimension - constrained min only"
    (normalize :width [:grow 100]) := {:type :grow :min 100}
    (normalize :height [:fit 50]) := {:type :fit :min 50}

    "Multi-method dimension - constrained with map"
    (normalize :width [:grow {:max 500}]) := {:type :grow :max 500}
    (normalize :width [:grow {:min 100 :max 500}]) := {:type :grow :min 100 :max 500}
    (normalize :height [:fit {:min 50 :max 200}]) := {:type :fit :min 50 :max 200}

    "Multi-method sizing normalization"
    (normalize :size :grow) := {:width {:type :grow} :height {:type :grow}}
    (normalize :size [:grow :fit]) := {:width {:type :grow} :height {:type :fit}}
    (normalize :size [300 400]) := {:width {:type :fixed :value 300}
                                     :height {:type :fixed :value 400}}

    "Multi-method padding normalization"
    (normalize :padding 16) := {:top 16 :right 16 :bottom 16 :left 16}
    (normalize :padding [16 8]) := {:top 16 :right 8 :bottom 16 :left 8}
    (normalize :padding [10 20 30 40]) := {:top 10 :right 20 :bottom 30 :left 40}

    "Multi-method radius normalization"
    (normalize :radius 8) := {:top-left 8 :top-right 8
                               :bottom-left 8 :bottom-right 8}
    (normalize :radius [8 8 0 0]) := {:top-left 8 :top-right 8
                                       :bottom-left 0 :bottom-right 0}

    "Multi-method align normalization"
    (normalize :align :center) := {:x :center :y :center}
    (normalize :align [:left :bottom]) := {:x :left :y :bottom}

    "Multi-method align - map form"
    (normalize :align {:x :left :y :top}) := {:x :left :y :top}
    (normalize :align {:x :center :y :bottom}) := {:x :center :y :bottom}

    "Multi-method color normalization"
    (def red-color (normalize :bg :red-500))
    (:r red-color) := 251
    (def rgb-color (normalize :color [255 0 0]))
    (:r rgb-color) := 255

    "Multi-method border normalization"
    (normalize :border 2) := {:width 2 :color nil :radius nil}
    (def test-border {:width 3 :color :blue-500})
    (def normalized-border (normalize :border test-border))
    (:width normalized-border) := 3
    (map? (:color normalized-border)) := true

    "Multi-method border - tuple with color and width"
    (def border-tuple2 (normalize :border [:red-500 2]))
    (:width border-tuple2) := 2
    (map? (:color border-tuple2)) := true
    (def border-rgb (normalize :border [[255 0 0] 3]))
    (:width border-rgb) := 3

    "Multi-method border - tuple with color, width, and radius"
    (def border-tuple3 (normalize :border [:red-500 2 8]))
    (:width border-tuple3) := 2
    (:radius border-tuple3) := 8
    (map? (:color border-tuple3)) := true
    (def border-tuple3-blue (normalize :border [:blue-300 3 4]))
    (:width border-tuple3-blue) := 3
    (:radius border-tuple3-blue) := 4

    "Multi-method floating normalization"
    (normalize :floating [100 200]) := {:x 100 :y 200}

    "Multi-method floating - map form"
    (normalize :floating {:to :parent :offset [5 10]}) := {:to :parent :at nil :offset [5 10] :z nil}
    (normalize :floating {:at [:left-top :right-bottom] :z 10}) := {:to :none :at [:left-top :right-bottom] :offset [0 0] :z 10}
    (normalize :floating {:to :root}) := {:to :root :at nil :offset [0 0] :z nil}

    "Multi-method scroll normalization"
    (normalize :scroll true) := {:direction :vertical :show-scrollbars true}
    (normalize :scroll :horizontal) := {:direction :horizontal :show-scrollbars true}

    "Multi-method scroll - map form"
    (normalize :scroll {:direction :y :show-scrollbars false}) := {:direction :y :show-scrollbars false}
    (normalize :scroll {:direction :both}) := {:direction :both :show-scrollbars true}
    (normalize :scroll {:show-scrollbars false}) := {:direction :vertical :show-scrollbars false}

    "Multi-method wrap normalization"
    (normalize :wrap true) := :words
    (normalize :wrap :none) := :none

    "Multi-method image normalization"
    (normalize :image "photo.jpg") := {:src "photo.jpg" :aspect nil :fit :contain}

    "Multi-method image - keyword form"
    (normalize :image :my-image) := {:src :my-image :aspect nil :fit :contain}
    (normalize :image :logo) := {:src :logo :aspect nil :fit :contain}

    "Multi-method image - function form"
    (def img-fn (fn [_] "computed.png"))
    (normalize :image img-fn) := {:src img-fn :aspect nil :fit :contain}

    "Multi-method image - map form"
    (normalize :image {:src "img.png" :aspect 1.5}) := {:src "img.png" :aspect 1.5 :fit :contain :position nil}
    (normalize :image {:src :logo :fit :cover}) := {:src :logo :aspect nil :fit :cover :position nil}
    (normalize :image {:src "banner.jpg" :aspect 2.0 :fit :fill :position [:center :top]}) := {:src "banner.jpg" :aspect 2.0 :fit :fill :position [:center :top]}

    "Full props normalization"
    (def test-props {:size [:grow :fit]
                     :padding 16
                     :bg :red-500
                     :radius 8
                     :align :center})
    (def normalized (normalize-props test-props))
    (get-in normalized [:size :width :type]) := :grow
    (get-in normalized [:padding :top]) := 16
    (get-in normalized [:radius :top-left]) := 8
    (get-in normalized [:align :x]) := :center

    "Dispatch function tests"
    (dispatch-normalize :size [:grow :fit]) := [::sizing-prop ::vector]
    (dispatch-normalize :padding 16) := [::padding-prop ::number]
    (dispatch-normalize :bg :red-500) := [::color-prop ::keyword]
    (dispatch-normalize :border 2) := [::border-prop ::number]

    "Infer value spec tests"
    (infer-value-spec :grow) := ::keyword
    (infer-value-spec 300) := ::number
    (infer-value-spec [255 0 0]) := ::vector
    (infer-value-spec {:r 255 :g 0 :b 0}) := ::color
    (infer-value-spec {:top 16 :left 16 :right 16 :bottom 16}) := ::padding))
