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
   ;; ATOMIC ATTRIBUTES - Color
   ;; -------------------------------------------------------------------------
   :color/r [:int {:min 0 :max 255}]
   :color/g [:int {:min 0 :max 255}]
   :color/b [:int {:min 0 :max 255}]
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
   ;; CANONICAL SCHEMAS - Composed from Atomic Attributes
   ;; -------------------------------------------------------------------------

   ;; Color - RGBA map
   ::Color [:map
            [:r :color/r]
            [:g :color/g]
            [:b :color/b]
            [:a {:optional true} :color/a]]

   ;; Padding - Four sides
   ::Padding [:map
              [:top :spacing/top]
              [:right :spacing/right]
              [:bottom :spacing/bottom]
              [:left :spacing/left]]

   ;; CornerRadius - Four corners
   ::CornerRadius [:map
                   [:top-left :corner/top-left]
                   [:top-right :corner/top-right]
                   [:bottom-left :corner/bottom-left]
                   [:bottom-right :corner/bottom-right]]

   ;; Vector2 - 2D position
   ::Vector2 [:map
              [:x :pos/x]
              [:y :pos/y]]

   ;; ChildAlignment - X and Y alignment
   ::ChildAlignment [:map
                     [:x :align/x]
                     [:y :align/y]]

   ;; SizingAxis - Single axis sizing
   ::SizingAxis [:map
                 [:type :sizing/type]
                 [:value {:optional true} :sizing/value]
                 [:min {:optional true} :sizing/min]
                 [:max {:optional true} :sizing/max]]

   ;; Sizing - Two axis sizing
   ::Sizing [:map
             [:width [:ref ::SizingAxis]]
             [:height [:ref ::SizingAxis]]]

   ;; -------------------------------------------------------------------------
   ;; DSL INPUT SCHEMAS - Dimension (width, height)
   ;; -------------------------------------------------------------------------

   ::DslDimensionKeyword [:enum :grow :fit]

   ::DslDimensionFixed number?

   ::DslDimensionPercent [:tuple [:enum :% :percent] number?]

   ::DslDimensionConstrainedNoArgs [:tuple [:enum :grow :fit]]

   ::DslDimensionConstrainedMin [:tuple [:enum :grow :fit] number?]

   ::DslDimensionConstrainedMinMax [:tuple [:enum :grow :fit] number? number?]

   ::DslDimensionConstrainedMap [:tuple
                                 [:enum :grow :fit]
                                 [:map
                                  [:min {:optional true} number?]
                                  [:max {:optional true} number?]]]

   ::DslDimensionExplicitFixed [:tuple [:enum :fixed] number?]

   ;; -------------------------------------------------------------------------
   ;; DSL INPUT SCHEMAS - Padding
   ;; -------------------------------------------------------------------------

   ::DslPaddingUniform number?

   ::DslPaddingVerticalHorizontal [:tuple number? number?]

   ::DslPaddingFourSides [:tuple number? number? number? number?]

   ;; -------------------------------------------------------------------------
   ;; DSL INPUT SCHEMAS - Radius
   ;; -------------------------------------------------------------------------

   ::DslRadiusUniform number?

   ::DslRadiusFourCorners [:tuple number? number? number? number?]

   ;; -------------------------------------------------------------------------
   ;; DSL INPUT SCHEMAS - Alignment
   ;; -------------------------------------------------------------------------

   ::DslAlignKeyword [:enum :center :left :right :top :bottom]

   ::DslAlignTuple [:tuple
                    [:enum :left :center :right]
                    [:enum :top :center :bottom]]

   ::DslAlignMap [:map
                  [:x [:enum :left :center :right]]
                  [:y [:enum :top :center :bottom]]]

   ;; -------------------------------------------------------------------------
   ;; DSL INPUT SCHEMAS - Border
   ;; -------------------------------------------------------------------------

   ::DslBorderWidth number?

   ::DslBorderTuple2 [:tuple
                      [:or keyword? string? [:tuple number? number? number?]
                           [:tuple number? number? number? number?] fn?]
                      number?]

   ::DslBorderTuple3 [:tuple
                      [:or keyword? string? [:tuple number? number? number?]
                           [:tuple number? number? number? number?] fn?]
                      number?
                      number?]

   ::DslBorderMap [:map
                   [:width number?]
                   [:color {:optional true} [:or keyword? string?
                                             [:tuple number? number? number?]
                                             [:tuple number? number? number? number?] fn?]]
                   [:radius {:optional true} [:or number? [:tuple number? number? number? number?]]]]

   ;; -------------------------------------------------------------------------
   ;; DSL INPUT SCHEMAS - Floating (position)
   ;; -------------------------------------------------------------------------

   ::DslFloatingVector [:tuple number? number?]

   ::DslFloatingMap [:map
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

   ::DslScrollBoolean boolean?

   ::DslScrollKeyword [:enum :vertical :horizontal :both :x :y]

   ::DslScrollMap [:map
                   [:direction {:optional true} [:enum :vertical :horizontal :both :x :y]]
                   [:show-scrollbars {:optional true} boolean?]]

   ;; -------------------------------------------------------------------------
   ;; DSL INPUT SCHEMAS - Wrap
   ;; -------------------------------------------------------------------------

   ::DslWrapKeyword [:enum :words :chars :none]

   ::DslWrapBoolean boolean?

   ;; -------------------------------------------------------------------------
   ;; DSL INPUT SCHEMAS - Image
   ;; -------------------------------------------------------------------------

   ::DslImageString string?

   ::DslImageKeyword keyword?

   ::DslImageFunction fn?

   ::DslImageMap [:map
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

;; Canonical Output Schemas
(def Color [:schema {:registry registry} ::Color])
(def Padding [:schema {:registry registry} ::Padding])
(def Sizing [:schema {:registry registry} ::Sizing])
(def SizingAxis [:schema {:registry registry} ::SizingAxis])
(def Vector2 [:schema {:registry registry} ::Vector2])
(def CornerRadius [:schema {:registry registry} ::CornerRadius])
(def ChildAlignment [:schema {:registry registry} ::ChildAlignment])

;; DSL Input Schemas - Dimension
(def DslDimensionKeyword [:schema {:registry registry} ::DslDimensionKeyword])
(def DslDimensionFixed [:schema {:registry registry} ::DslDimensionFixed])
(def DslDimensionPercent [:schema {:registry registry} ::DslDimensionPercent])
(def DslDimensionConstrainedNoArgs [:schema {:registry registry} ::DslDimensionConstrainedNoArgs])
(def DslDimensionConstrainedMin [:schema {:registry registry} ::DslDimensionConstrainedMin])
(def DslDimensionConstrainedMinMax [:schema {:registry registry} ::DslDimensionConstrainedMinMax])
(def DslDimensionConstrainedMap [:schema {:registry registry} ::DslDimensionConstrainedMap])
(def DslDimensionExplicitFixed [:schema {:registry registry} ::DslDimensionExplicitFixed])

;; DSL Input Schemas - Padding
(def DslPaddingUniform [:schema {:registry registry} ::DslPaddingUniform])
(def DslPaddingVerticalHorizontal [:schema {:registry registry} ::DslPaddingVerticalHorizontal])
(def DslPaddingFourSides [:schema {:registry registry} ::DslPaddingFourSides])

;; DSL Input Schemas - Radius
(def DslRadiusUniform [:schema {:registry registry} ::DslRadiusUniform])
(def DslRadiusFourCorners [:schema {:registry registry} ::DslRadiusFourCorners])

;; DSL Input Schemas - Alignment
(def DslAlignKeyword [:schema {:registry registry} ::DslAlignKeyword])
(def DslAlignTuple [:schema {:registry registry} ::DslAlignTuple])
(def DslAlignMap [:schema {:registry registry} ::DslAlignMap])

;; DSL Input Schemas - Border
(def DslBorderWidth [:schema {:registry registry} ::DslBorderWidth])
(def DslBorderTuple2 [:schema {:registry registry} ::DslBorderTuple2])
(def DslBorderTuple3 [:schema {:registry registry} ::DslBorderTuple3])
(def DslBorderMap [:schema {:registry registry} ::DslBorderMap])

;; DSL Input Schemas - Floating
(def DslFloatingVector [:schema {:registry registry} ::DslFloatingVector])
(def DslFloatingMap [:schema {:registry registry} ::DslFloatingMap])

;; DSL Input Schemas - Scroll
(def DslScrollBoolean [:schema {:registry registry} ::DslScrollBoolean])
(def DslScrollKeyword [:schema {:registry registry} ::DslScrollKeyword])
(def DslScrollMap [:schema {:registry registry} ::DslScrollMap])

;; DSL Input Schemas - Wrap
(def DslWrapKeyword [:schema {:registry registry} ::DslWrapKeyword])
(def DslWrapBoolean [:schema {:registry registry} ::DslWrapBoolean])

;; DSL Input Schemas - Image
(def DslImageString [:schema {:registry registry} ::DslImageString])
(def DslImageKeyword [:schema {:registry registry} ::DslImageKeyword])
(def DslImageFunction [:schema {:registry registry} ::DslImageFunction])
(def DslImageMap [:schema {:registry registry} ::DslImageMap])
