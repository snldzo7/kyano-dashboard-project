(ns clay.schema
  "Malli schemas for Clay DSL using attribute-based modeling.

  Philosophy:
  - Atomic design: Break down to smallest possible attribute units
  - Namespaced keywords: Use qualified keywords for attributes
  - Registry-based: All attributes defined in a central registry
  - Composable: Build complex schemas from atomic attributes
  - Selectable: Support malli.util/select-keys operations

  Schema Types:
  1. Atomic attributes - Primitive units (::color/r, ::spacing/top, etc.)
  2. Canonical schemas - Normalized output structures
  3. DSL input schemas - Various input format variations"
  (:require [malli.core :as m]))

;; ============================================================================
;; REGISTRY - Atomic Attributes and Composite Schemas
;; ============================================================================

(def registry
  {;; -------------------------------------------------------------------------
   ;; PRIMITIVE TYPES - Reusable type constraints
   ;; -------------------------------------------------------------------------
   :color/channel [:int {:min 0 :max 255}]
   :int/non-negative [:int {:min 0}]

   ;; -------------------------------------------------------------------------
   ;; ATOMIC ATTRIBUTES - Color
   ;; -------------------------------------------------------------------------
   :color/r :color/channel
   :color/g :color/channel
   :color/b :color/channel
   :color/a [:double {:min 0.0 :max 1.0}]

   ;; -------------------------------------------------------------------------
   ;; ATOMIC ATTRIBUTES - Spacing (used for padding)
   ;; -------------------------------------------------------------------------
   :spacing/top number?
   :spacing/right number?
   :spacing/bottom number?
   :spacing/left number?

   ;; -------------------------------------------------------------------------
   ;; ATOMIC ATTRIBUTES - Corner Radius
   ;; -------------------------------------------------------------------------
   :corner/top-left number?
   :corner/top-right number?
   :corner/bottom-left number?
   :corner/bottom-right number?

   ;; -------------------------------------------------------------------------
   ;; ATOMIC ATTRIBUTES - Position/Vector
   ;; -------------------------------------------------------------------------
   :pos/x number?
   :pos/y number?

   ;; -------------------------------------------------------------------------
   ;; ATOMIC ATTRIBUTES - Dimensions
   ;; -------------------------------------------------------------------------
   :dim/width number?
   :dim/height number?

   ;; -------------------------------------------------------------------------
   ;; ATOMIC ATTRIBUTES - String
   ;; -------------------------------------------------------------------------
   :string/length :int/non-negative
   :string/chars string?

   ;; -------------------------------------------------------------------------
   ;; ATOMIC ATTRIBUTES - Element ID
   ;; -------------------------------------------------------------------------
   :element-id/id :int/non-negative
   :element-id/offset :int/non-negative
   :element-id/base-id :int/non-negative
   :element-id/string-id :int/non-negative

   ;; -------------------------------------------------------------------------
   ;; ATOMIC ATTRIBUTES - Alignment
   ;; -------------------------------------------------------------------------
   :align/x [:enum :left :center :right]
   :align/y [:enum :top :center :bottom]
   
   ;; -------------------------------------------------------------------------
   ;; ATOMIC ATTRIBUTES - Sizing
   ;; -------------------------------------------------------------------------
   :sizing/type [:enum :grow :fit :fixed :percent]
   :sizing/value number?
   :sizing/min number?
   :sizing/max number?

   ;; -------------------------------------------------------------------------
   ;; ATOMIC ATTRIBUTES - Layout
   ;; -------------------------------------------------------------------------
   :layout/direction [:enum :left-to-right :top-to-bottom]
   :layout/child-gap number?

   ;; -------------------------------------------------------------------------
   ;; ATOMIC ATTRIBUTES - Text
   ;; -------------------------------------------------------------------------
   :text/font-id :int/non-negative
   :text/font-size number?
   :text/letter-spacing number?
   :text/line-height number?
   :text/wrap-mode [:enum :words :chars :none]
   :text/disable-pointer-events boolean?

   ;; -------------------------------------------------------------------------
   ;; ATOMIC ATTRIBUTES - Floating
   ;; -------------------------------------------------------------------------
   :floating/z-index :int/non-negative
   :floating/parent-id :int/non-negative
   :floating/attachment [:enum :left-top :left-center :left-bottom
                                :center-top :center-center :center-bottom
                                :right-top :right-center :right-bottom]

   ;; -------------------------------------------------------------------------
   ;; ATOMIC ATTRIBUTES - Scroll
   ;; -------------------------------------------------------------------------
   :scroll/horizontal boolean?
   :scroll/vertical boolean?

   ;; -------------------------------------------------------------------------
   ;; ATOMIC ATTRIBUTES - Pointer State
   ;; -------------------------------------------------------------------------
   :pointer/state [:enum :pointer-down :hover :none]

   ;; -------------------------------------------------------------------------
   ;; ATOMIC ATTRIBUTES - Render Command Type
   ;; -------------------------------------------------------------------------
   :render/command-type [:enum :none :rectangle :border :text :image :clip :custom]

   ;; -------------------------------------------------------------------------
   ;; ATOMIC ATTRIBUTES - Error
   ;; -------------------------------------------------------------------------
   :error/type [:enum :text-measurement-function-not-provided
                      :arena-capacity-exceeded
                      :elements-capacity-exceeded
                      :text-measurement-capacity-exceeded
                      :duplicate-id
                      :floating-container-parent-not-found
                      :internal-error]
   :error/location string?
   :error/id :int/non-negative

   ;; -------------------------------------------------------------------------
   ;; CANONICAL SCHEMAS - Composed from Atomic Attributes
   ;; -------------------------------------------------------------------------

   ;; Color - RGBA map
   ::color [:map
            [:r :color/r]
            [:g :color/g]
            [:b :color/b]
            [:a {:optional true} :color/a]]

   ;; BoxSpacing - Four-sided spacing (base for Padding and BorderWidth)
   ::box-spacing [:map
                 [:top :spacing/top]
                 [:right :spacing/right]
                 [:bottom :spacing/bottom]
                 [:left :spacing/left]]

   ;; Padding - Four sides (reuses BoxSpacing)
   ::padding [:ref ::box-spacing]

   ;; CornerRadius - Four corners
   ::corner-radius [:map
                   [:top-left :corner/top-left]
                   [:top-right :corner/top-right]
                   [:bottom-left :corner/bottom-left]
                   [:bottom-right :corner/bottom-right]]

   ;; position-2d - 2D position
   ::position-2d [:map
              [:x :pos/x]
              [:y :pos/y]]

   ;; ChildAlignment - X and Y alignment
   ::child-alignment [:map
                     [:x :align/x]
                     [:y :align/y]]

   ;; SizingAxis - Single axis sizing
   ::sizing-axis [:map
                 [:type :sizing/type]
                 [:value {:optional true} :sizing/value]
                 [:min {:optional true} :sizing/min]
                 [:max {:optional true} :sizing/max]]

   ;; Sizing - Two axis sizing
   ::sizing [:map
             [:width [:ref ::sizing-axis]]
             [:height [:ref ::sizing-axis]]]

   ;; Dimensions - Width and height
   ::dimensions [:map
                 [:width :dim/width]
                 [:height :dim/height]]

   ;; BoundingBox - Position and dimensions (flat, reuses pos/* and dim/* attributes)
   ::bounding-box [:map
                  [:x :pos/x]
                  [:y :pos/y]
                  [:width :dim/width]
                  [:height :dim/height]]

   ;; ClayString - Length-prefixed string slice
   ::clay-string [:map
                 [:length :string/length]
                 [:chars :string/chars]]

   ;; ElementId - Unique element identifier
   ::element-id [:map
                [:id :element-id/id]
                [:offset :element-id/offset]
                [:base-id :element-id/base-id]
                [:string-id :element-id/string-id]]

   ;; LayoutConfig - Complete layout configuration
   ::layout-config [:map
                   [:sizing [:ref ::sizing]]
                   [:padding [:ref ::padding]]
                   [:child-gap :layout/child-gap]
                   [:child-alignment [:ref ::child-alignment]]
                   [:layout-direction :layout/direction]]

   ;; TextElementConfig - Text rendering configuration
   ::text-element-config [:map
                        [:text-color [:ref ::color]]
                        [:font-id :text/font-id]
                        [:font-size :text/font-size]
                        [:letter-spacing :text/letter-spacing]
                        [:line-height :text/line-height]
                        [:wrap-mode :text/wrap-mode]
                        [:disable-pointer-events :text/disable-pointer-events]]

   ;; BorderWidth - Four-sided border width (reuses BoxSpacing)
   ::border-width [:ref ::box-spacing]

   ;; FontConfig - Font typography settings
   ::font-config [:map
                 [:font-id :text/font-id]
                 [:font-size :text/font-size]
                 [:letter-spacing :text/letter-spacing]]

   ;; BorderStyle - Border styling (shared by BorderElementConfig and BorderRenderData)
   ::border-style [:map
                  [:color [:ref ::color]]
                  [:width [:ref ::border-width]]
                  [:corner-radius [:ref ::corner-radius]]]

   ;; TextStyle - Text appearance settings
   ::text-style [:map
                [:text-color [:ref ::color]]
                [:font-id :text/font-id]
                [:font-size :text/font-size]
                [:letter-spacing :text/letter-spacing]]

   ;; BorderElementConfig - Border styling configuration (reuses BorderStyle)
   ::border-element-config [:ref ::border-style]

   ;; FloatingAttachPoints - Floating attachment configuration
   ::floating-attach-points [:map
                           [:element :floating/attachment]
                           [:parent :floating/attachment]]

   ;; FloatingElementConfig - Floating element configuration
   ::floating-element-config [:map
                            [:offset [:ref ::position-2d]]
                            [:expand [:ref ::dimensions]]
                            [:z-index :floating/z-index]
                            [:parent-id :floating/parent-id]
                            [:attachment [:ref ::floating-attach-points]]]

   ;; ScrollElementConfig - Scroll container configuration
   ::scroll-element-config [:map
                          [:horizontal :scroll/horizontal]
                          [:vertical :scroll/vertical]]

   ;; ImageElementConfig - Image configuration
   ::image-element-config [:map
                         [:source-dimensions [:ref ::dimensions]]]

   ;; CustomElementConfig - Custom element (opaque pointer)
   ::custom-element-config [:map
                          [:custom-data any?]]

   ;; TextRenderData - Text rendering data
   ::text-render-data [:map
                     [:text [:ref ::clay-string]]
                     [:font-id :text/font-id]
                     [:font-size :text/font-size]
                     [:letter-spacing :text/letter-spacing]
                     [:text-color [:ref ::color]]]

   ;; RectangleRenderData - Rectangle rendering data
   ::rectangle-render-data [:map
                          [:color [:ref ::color]]
                          [:corner-radius [:ref ::corner-radius]]]

   ;; ImageRenderData - Image rendering data
   ::image-render-data [:map
                      [:source-dimensions [:ref ::dimensions]]
                      [:image-data any?]]

   ;; BorderRenderData - Border rendering data (reuses BorderStyle)
   ::border-render-data [:ref ::border-style]

   ;; ClipRenderData - Clip/Scissor rendering data
   ::clip-render-data [:map
                     [:bounding-box [:ref ::bounding-box]]]

   ;; CustomRenderData - Custom rendering data
   ::custom-render-data [:map
                       [:custom-data any?]]

   ;; RenderCommand - Single render command
   ::render-command [:map
                    [:bounding-box [:ref ::bounding-box]]
                    [:command-type :render/command-type]
                    [:render-data [:or
                                   [:ref ::text-render-data]
                                   [:ref ::rectangle-render-data]
                                   [:ref ::image-render-data]
                                   [:ref ::border-render-data]
                                   [:ref ::clip-render-data]
                                   [:ref ::custom-render-data]]]
                    [:id [:ref ::element-id]]
                    [:user-data {:optional true} any?]]

   ;; ScrollContainerData - Scroll container state
   ::scroll-container-data [:map
                          [:scroll-position [:ref ::position-2d]]
                          [:scroll-container-dimensions [:ref ::dimensions]]
                          [:content-dimensions [:ref ::dimensions]]
                          [:config [:ref ::scroll-element-config]]
                          [:found boolean?]]

   ;; ElementData - Element layout data
   ::element-data [:map
                  [:id [:ref ::element-id]]
                  [:layout [:ref ::layout-config]]
                  [:bounding-box [:ref ::bounding-box]]
                  [:children-or-text-content [:or
                                              [:vector [:ref ::element-data]]
                                              [:ref ::clay-string]]]]

   ;; PointerData - Pointer/mouse state
   ::pointer-data [:map
                  [:position [:ref ::position-2d]]
                  [:state :pointer/state]]

   ;; ErrorData - Error information
   ::error-data [:map
                [:error-type :error/type]
                [:error-text [:ref ::clay-string]]
                [:location :error/location]
                [:user-data {:optional true} any?]]

   ;; -------------------------------------------------------------------------
   ;; DSL INPUT SCHEMAS - Dimension (width, height)
   ;; -------------------------------------------------------------------------

   ::dsl-dimension-keyword [:enum :grow :fit]

   ::dsl-dimension-fixed number?

   ::dsl-dimension-percent [:tuple [:enum :% :percent] number?]

   ::dsl-dimension-constrained-no-args [:tuple [:enum :grow :fit]]

   ::dsl-dimension-constrained-min [:tuple [:enum :grow :fit] number?]

   ::dsl-dimension-constrained-minMax [:tuple [:enum :grow :fit] number? number?]

   ::dsl-dimension-constrained-map [:tuple
                                 [:enum :grow :fit]
                                 [:map
                                  [:min {:optional true} number?]
                                  [:max {:optional true} number?]]]

   ::dsl-dimension-explicit-fixed [:tuple [:enum :fixed] number?]

   ;; -------------------------------------------------------------------------
   ;; DSL INPUT SCHEMAS - Padding
   ;; -------------------------------------------------------------------------

   ::dsl-padding-uniform number?

   ::dsl-padding-vertical-horizontal [:tuple number? number?]

   ::dsl-padding-four-sides [:tuple number? number? number? number?]

   ;; -------------------------------------------------------------------------
   ;; DSL INPUT SCHEMAS - Radius
   ;; -------------------------------------------------------------------------

   ::dsl-radius-uniform number?

   ::dsl-radius-four-corners [:tuple number? number? number? number?]

   ;; -------------------------------------------------------------------------
   ;; DSL INPUT SCHEMAS - Alignment
   ;; -------------------------------------------------------------------------

   ::dsl-align-keyword [:enum :center :left :right :top :bottom]

   ::dsl-align-tuple [:tuple
                    [:enum :left :center :right]
                    [:enum :top :center :bottom]]

   ::dsl-align-map [:map
                  [:x [:enum :left :center :right]]
                  [:y [:enum :top :center :bottom]]]

   ;; -------------------------------------------------------------------------
   ;; DSL INPUT SCHEMAS - Border
   ;; -------------------------------------------------------------------------

   ::dsl-border-width number?

   ::dsl-border-tuple2 [:tuple
                      [:or keyword? string? [:tuple number? number? number?]
                           [:tuple number? number? number? number?] fn?]
                      number?]

   ::dsl-border-tuple3 [:tuple
                      [:or keyword? string? [:tuple number? number? number?]
                           [:tuple number? number? number? number?] fn?]
                      number?
                      number?]

   ::dsl-border-map [:map
                   [:width number?]
                   [:color {:optional true} [:or keyword? string?
                                             [:tuple number? number? number?]
                                             [:tuple number? number? number? number?] fn?]]
                   [:radius {:optional true} [:or number? [:tuple number? number? number? number?]]]]

   ;; -------------------------------------------------------------------------
   ;; DSL INPUT SCHEMAS - Floating (position)
   ;; -------------------------------------------------------------------------

   ::dsl-floating-vector [:tuple number? number?]

   ::dsl-floating-map [:map
                     [:to {:optional true} [:enum :none :parent :root]]
                     [:at {:optional true} [:or
                                            [:tuple [:enum :left :center :right]
                                                    [:enum :top :center :bottom]]
                                            [:tuple [:enum :left-top :left-center :left-bottom
                                                           :center-top :center-center :center-bottom
                                                           :right-top :right-center :right-bottom]
                                                    [:enum :left-top :left-center :left-bottom
                                                           :center-top :center-center :center-bottom
                                                           :right-top :right-center :right-bottom]]]]
                     [:offset {:optional true} [:tuple number? number?]]
                     [:z {:optional true} number?]]

   ;; -------------------------------------------------------------------------
   ;; DSL INPUT SCHEMAS - Scroll
   ;; -------------------------------------------------------------------------

   ::dsl-scroll-boolean boolean?

   ::dsl-scroll-keyword [:enum :vertical :horizontal :both :x :y]

   ::dsl-scroll-map [:map
                   [:direction {:optional true} [:enum :vertical :horizontal :both :x :y]]
                   [:show-scrollbars {:optional true} boolean?]]

   ;; -------------------------------------------------------------------------
   ;; DSL INPUT SCHEMAS - Wrap
   ;; -------------------------------------------------------------------------

   ::dsl-wrap-keyword [:enum :words :chars :none]

   ::dsl-wrap-boolean boolean?

   ;; -------------------------------------------------------------------------
   ;; DSL INPUT SCHEMAS - Image
   ;; -------------------------------------------------------------------------

   ::dsl-image-string string?

   ::dsl-image-keyword keyword?

   ::dsl-image-function fn?

   ::dsl-image-map [:map
                  [:src [:or string? keyword? fn?]]
                  [:aspect {:optional true} number?]
                  [:fit {:optional true} [:enum :contain :cover :fill :scale-down]]
                  [:position {:optional true} [:or
                                               [:tuple [:enum :left :center :right]
                                                       [:enum :top :center :bottom]]
                                               keyword?]]]})

;; ============================================================================
;; PUBLIC SCHEMA DEFINITIONS - Using Registry
;; ============================================================================

;; Canonical Output Schemas - Basic
(def Color [:schema {:registry registry} ::color])
(def BoxSpacing [:schema {:registry registry} ::box-spacing])
(def Padding [:schema {:registry registry} ::padding])
(def Sizing [:schema {:registry registry} ::sizing])
(def SizingAxis [:schema {:registry registry} ::sizing-axis])
(def Position2D [:schema {:registry registry} ::position-2d])
(def CornerRadius [:schema {:registry registry} ::corner-radius])
(def ChildAlignment [:schema {:registry registry} ::child-alignment])

;; Canonical Output Schemas - Intermediate/Shared
(def FontConfig [:schema {:registry registry} ::font-config])
(def BorderStyle [:schema {:registry registry} ::border-style])
(def TextStyle [:schema {:registry registry} ::text-style])

;; DSL Input Schemas - Dimension
(def DslDimensionKeyword [:schema {:registry registry} ::dsl-dimension-keyword])
(def DslDimensionFixed [:schema {:registry registry} ::dsl-dimension-fixed])
(def DslDimensionPercent [:schema {:registry registry} ::dsl-dimension-percent])
(def DslDimensionConstrainedNoArgs [:schema {:registry registry} ::dsl-dimension-constrained-no-args])
(def DslDimensionConstrainedMin [:schema {:registry registry} ::dsl-dimension-constrained-min])
(def DslDimensionConstrainedMinMax [:schema {:registry registry} ::dsl-dimension-constrained-minMax])
(def DslDimensionConstrainedMap [:schema {:registry registry} ::dsl-dimension-constrained-map])
(def DslDimensionExplicitFixed [:schema {:registry registry} ::dsl-dimension-explicit-fixed])

;; DSL Input Schemas - Padding
(def DslPaddingUniform [:schema {:registry registry} ::dsl-padding-uniform])
(def DslPaddingVerticalHorizontal [:schema {:registry registry} ::dsl-padding-vertical-horizontal])
(def DslPaddingFourSides [:schema {:registry registry} ::dsl-padding-four-sides])

;; DSL Input Schemas - Radius
(def DslRadiusUniform [:schema {:registry registry} ::dsl-radius-uniform])
(def DslRadiusFourCorners [:schema {:registry registry} ::dsl-radius-four-corners])

;; DSL Input Schemas - Alignment
(def DslAlignKeyword [:schema {:registry registry} ::dsl-align-keyword])
(def DslAlignTuple [:schema {:registry registry} ::dsl-align-tuple])
(def DslAlignMap [:schema {:registry registry} ::dsl-align-map])

;; DSL Input Schemas - Border
(def DslBorderWidth [:schema {:registry registry} ::dsl-border-width])
(def DslBorderTuple2 [:schema {:registry registry} ::dsl-border-tuple2])
(def DslBorderTuple3 [:schema {:registry registry} ::dsl-border-tuple3])
(def DslBorderMap [:schema {:registry registry} ::dsl-border-map])

;; DSL Input Schemas - Floating
(def DslFloatingVector [:schema {:registry registry} ::dsl-floating-vector])
(def DslFloatingMap [:schema {:registry registry} ::dsl-floating-map])

;; DSL Input Schemas - Scroll
(def DslScrollBoolean [:schema {:registry registry} ::dsl-scroll-boolean])
(def DslScrollKeyword [:schema {:registry registry} ::dsl-scroll-keyword])
(def DslScrollMap [:schema {:registry registry} ::dsl-scroll-map])

;; DSL Input Schemas - Wrap
(def DslWrapKeyword [:schema {:registry registry} ::dsl-wrap-keyword])
(def DslWrapBoolean [:schema {:registry registry} ::dsl-wrap-boolean])

;; DSL Input Schemas - Image
(def DslImageString [:schema {:registry registry} ::dsl-image-string])
(def DslImageKeyword [:schema {:registry registry} ::dsl-image-keyword])
(def DslImageFunction [:schema {:registry registry} ::dsl-image-function])
(def DslImageMap [:schema {:registry registry} ::dsl-image-map])

;; Clay Engine Schemas - Basic Structures
(def Dimensions [:schema {:registry registry} ::dimensions])
(def BoundingBox [:schema {:registry registry} ::bounding-box])
(def ClayString [:schema {:registry registry} ::clay-string])
(def ElementId [:schema {:registry registry} ::element-id])

;; Clay Engine Schemas - Configuration
(def LayoutConfig [:schema {:registry registry} ::layout-config])
(def TextElementConfig [:schema {:registry registry} ::text-element-config])
(def BorderWidth [:schema {:registry registry} ::border-width])
(def BorderElementConfig [:schema {:registry registry} ::border-element-config])
(def FloatingAttachPoints [:schema {:registry registry} ::floating-attach-points])
(def FloatingElementConfig [:schema {:registry registry} ::floating-element-config])
(def ScrollElementConfig [:schema {:registry registry} ::scroll-element-config])
(def ImageElementConfig [:schema {:registry registry} ::image-element-config])
(def CustomElementConfig [:schema {:registry registry} ::custom-element-config])

;; Clay Engine Schemas - Render Data
(def TextRenderData [:schema {:registry registry} ::text-render-data])
(def RectangleRenderData [:schema {:registry registry} ::rectangle-render-data])
(def ImageRenderData [:schema {:registry registry} ::image-render-data])
(def BorderRenderData [:schema {:registry registry} ::border-render-data])
(def ClipRenderData [:schema {:registry registry} ::clip-render-data])
(def CustomRenderData [:schema {:registry registry} ::custom-render-data])
(def RenderCommand [:schema {:registry registry} ::render-command])

;; Clay Engine Schemas - Runtime Data
(def ScrollContainerData [:schema {:registry registry} ::scroll-container-data])
(def ElementData [:schema {:registry registry} ::element-data])
(def PointerData [:schema {:registry registry} ::pointer-data])

;; Clay Engine Schemas - Error Handling
(def ErrorData [:schema {:registry registry} ::error-data])
