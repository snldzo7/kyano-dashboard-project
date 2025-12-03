(ns foton.schema
  "Malli schemas for Foton primitives - Figma-compatible UI data.

   Every attribute is specced. This is the source of truth for
   what Foton accepts."
  (:require [malli.core :as m]
            [malli.error :as me]))

;; =============================================================================
;; Token Types
;; =============================================================================

(def ColorToken
  "Color can be:
   - keyword: :primary, :surface, :muted
   - vector path: [:text :primary], [:status :good]
   - hex string: \"#1e293b\""
  [:or
   :keyword
   [:vector :keyword]
   [:re #"^#[0-9a-fA-F]{6}$"]])

(def SpacingToken
  "Spacing: keyword (:xs :sm :md :lg :xl :2xl) or number (px)"
  [:or
   [:enum :xs :sm :md :lg :xl :2xl]
   :int])

(def RadiusToken
  "Radius: keyword (:none :sm :md :lg :xl :2xl :full) or number (px)"
  [:or
   [:enum :none :sm :md :lg :xl :2xl :full]
   :int])

(def SizeToken
  "Size: keyword (:fill :hug :viewport :viewport-width :full :half :auto)
   or number (px) or string (CSS value)"
  [:or
   [:enum :fill :hug :viewport :viewport-width :full :half :auto]
   :int
   :string])

(def ShadowToken
  "Shadow preset: :none :sm :md :lg :xl :2xl :inner"
  [:enum :none :sm :md :lg :xl :2xl :inner])

(def EasingToken
  "Easing: :linear :ease :ease-in :ease-out :ease-in-out :spring :bounce"
  [:enum :linear :ease :ease-in :ease-out :ease-in-out :spring :bounce])

;; =============================================================================
;; Complex Types
;; =============================================================================

(def StrokeAttrs
  "Figma-style stroke definition"
  [:map
   [:color {:optional true} ColorToken]
   [:width {:optional true} :int]
   [:position {:optional true} [:enum :inside :outside :center]]
   [:style {:optional true} [:enum :solid :dashed :dotted]]])

(def EffectAttrs
  "Single effect definition"
  [:map
   [:type [:enum :drop-shadow :inner-shadow :layer-blur :background-blur]]
   [:x {:optional true} :int]
   [:y {:optional true} :int]
   [:blur {:optional true} :int]
   [:spread {:optional true} :int]
   [:color {:optional true} ColorToken]
   [:opacity {:optional true} :double]])

(def TransitionAttrs
  "Transition definition"
  [:or
   :int  ; Simple duration in ms
   [:map
    [:duration {:optional true} :int]
    [:easing {:optional true} EasingToken]
    [:delay {:optional true} :int]
    [:properties {:optional true} [:vector :keyword]]]])

(def VaryAttrs
  "Variant states for hover/press/focus"
  [:map-of
   [:enum :hovered :pressed :focused :disabled]
   [:map-of :keyword :any]])

(def EventHandlers
  "Event handlers map"
  [:map-of :keyword :any])

;; =============================================================================
;; Frame Schema
;; =============================================================================

(def FrameAttrs
  "Frame (flexbox container) attributes - Figma Auto Layout"
  [:map
   ;; Appearance
   [:fill {:optional true} ColorToken]
   [:stroke {:optional true} [:or ColorToken StrokeAttrs]]
   [:radius {:optional true} [:or RadiusToken [:vector :int]]]
   [:opacity {:optional true} [:and :double [:>= 0] [:<= 1]]]
   [:border {:optional true} ColorToken]

   ;; Effects
   [:shadow {:optional true} ShadowToken]
   [:effects {:optional true} [:vector EffectAttrs]]
   [:blend {:optional true} [:enum :normal :multiply :screen :overlay :darken :lighten]]

   ;; Layout direction
   [:direction {:optional true} [:enum :horizontal :vertical]]
   [:wrap {:optional true} [:enum :wrap :nowrap]]

   ;; Spacing
   [:gap {:optional true} SpacingToken]
   [:padding {:optional true} SpacingToken]
   [:margin {:optional true} SpacingToken]

   ;; Alignment
   [:align {:optional true} [:enum :start :center :end :stretch :baseline]]
   [:justify {:optional true} [:enum :start :center :end :space-between :space-around :space-evenly]]

   ;; Sizing
   [:width {:optional true} SizeToken]
   [:height {:optional true} SizeToken]
   [:min-width {:optional true} SizeToken]
   [:max-width {:optional true} SizeToken]
   [:min-height {:optional true} SizeToken]
   [:max-height {:optional true} SizeToken]

   ;; Flex item
   [:grow {:optional true} :int]
   [:shrink {:optional true} :int]
   [:basis {:optional true} [:or :int :string]]

   ;; Overflow & Scrolling (Figma: Clip content, Scroll behavior)
   [:overflow {:optional true} [:enum :hidden :scroll :visible :auto]]
   [:overflow-x {:optional true} [:enum :hidden :scroll :visible :auto]]
   [:overflow-y {:optional true} [:enum :hidden :scroll :visible :auto]]
   [:clip {:optional true} :boolean]
   [:scroll-behavior {:optional true} [:enum :auto :smooth]]
   [:scrollbar {:optional true} [:enum :auto :hidden :thin]]
   [:scroll-snap {:optional true} [:enum :none :start :center :end]]
   [:scroll-snap-type {:optional true} [:enum :x :y :both :none]]

   ;; Masking & Clipping (Figma: Mask, Clip path)
   [:mask {:optional true} [:or :keyword :string]]
   [:clip-path {:optional true} [:or :keyword :string]]

   ;; Positioning (P0 Figma)
   [:position {:optional true} [:enum :relative :absolute :fixed :sticky]]
   [:top {:optional true} SizeToken]
   [:left {:optional true} SizeToken]
   [:right {:optional true} SizeToken]
   [:bottom {:optional true} SizeToken]
   [:z {:optional true} :int]
   [:visible {:optional true} :boolean]

   ;; Gradient (P0 Figma)
   [:gradient {:optional true}
    [:map
     [:type [:enum :linear :radial :conic]]
     [:angle {:optional true} :int]
     [:stops [:vector
              [:map
               [:color ColorToken]
               [:position :int]]]]]]

   ;; Background Image (P0 Figma)
   [:background-image {:optional true} :string]
   [:background-size {:optional true} [:or [:enum :cover :contain :auto] :string]]
   [:background-position {:optional true} [:or [:enum :center :top :bottom :left :right] :string]]

   ;; Transform
   [:translate-x {:optional true} :int]
   [:translate-y {:optional true} :int]
   [:scale {:optional true} :double]
   [:rotate {:optional true} :int]
   [:skew-x {:optional true} :int]
   [:skew-y {:optional true} :int]
   [:transform {:optional true} :string]

   ;; Interaction
   [:cursor {:optional true} [:enum :pointer :default :move :grab :grabbing :not-allowed]]
   [:transition {:optional true} TransitionAttrs]
   [:vary {:optional true} VaryAttrs]

   ;; Events
   [:on {:optional true} EventHandlers]])

;; =============================================================================
;; Text Token Types
;; =============================================================================

(def FontFamilyToken
  "Font family: preset keyword or CSS string"
  [:or
   [:enum :sans :serif :mono]
   :string])

(def FontStyleToken
  "Font style"
  [:enum :normal :italic])

(def TextDecorationToken
  "Text decoration"
  [:enum :none :underline :line-through :overline])

(def TextTransformToken
  "Text transform / case"
  [:enum :none :uppercase :lowercase :capitalize])

(def WhiteSpaceToken
  "White space / wrap mode"
  [:enum :normal :nowrap :pre :pre-wrap :pre-line])

(def WordBreakToken
  "Word break mode"
  [:enum :normal :break-all :break-word :keep-all])

(def VerticalAlignToken
  "Vertical alignment"
  [:enum :top :middle :bottom :baseline])

;; =============================================================================
;; Text Schema
;; =============================================================================

(def TextAttrs
  "Text (span) attributes - Figma typography"
  [:map
   ;; Typography preset (expands to size/weight/line-height)
   [:preset {:optional true} [:enum :title :heading :body :small :caption :label]]

   ;; Font family & style
   [:family {:optional true} FontFamilyToken]
   [:size {:optional true} [:or :int :string]]
   [:weight {:optional true} [:or :int [:enum 100 200 300 400 500 600 700 800 900]]]
   [:style {:optional true} FontStyleToken]

   ;; Color & opacity
   [:color {:optional true} ColorToken]
   [:opacity {:optional true} [:and :double [:>= 0] [:<= 1]]]

   ;; Spacing
   [:tracking {:optional true} :number]
   [:line-height {:optional true} [:or :number :string]]
   [:paragraph-spacing {:optional true} SpacingToken]

   ;; Alignment
   [:text-align {:optional true} [:enum :left :center :right :justify]]
   [:valign {:optional true} VerticalAlignToken]

   ;; Decoration & transform
   [:decoration {:optional true} TextDecorationToken]
   [:transform {:optional true} TextTransformToken]

   ;; Truncation & overflow
   [:truncate {:optional true} :boolean]
   [:max-lines {:optional true} :int]
   [:wrap {:optional true} WhiteSpaceToken]
   [:break {:optional true} WordBreakToken]

   ;; Selection
   [:selectable {:optional true} :boolean]

   ;; Events
   [:on {:optional true} EventHandlers]])

;; =============================================================================
;; Icon Schema
;; =============================================================================

(def IconAttrs
  "Icon attributes"
  [:map
   [:name {:optional true} [:enum :check :x :arrow-up :arrow-down :arrow-right :arrow-left
                            :dot :warning :info :chart :plus :minus :edit :trash
                            :close :menu :home :settings :search]]
   [:size {:optional true} [:enum :xs :sm :md :lg :xl]]
   [:color {:optional true} ColorToken]

   ;; Events
   [:on {:optional true} EventHandlers]])

;; =============================================================================
;; Input Schema
;; =============================================================================

(def InputAttrs
  "Input field attributes"
  [:map
   [:type {:optional true} [:enum :text :password :email :number :tel :url :search]]
   [:placeholder {:optional true} :string]
   [:value {:optional true} :string]
   [:name {:optional true} :string]
   [:size {:optional true} [:enum :sm :md :lg]]
   [:radius {:optional true} RadiusToken]
   [:disabled {:optional true} :boolean]

   ;; Events
   [:on {:optional true} EventHandlers]])

;; =============================================================================
;; Textarea Schema
;; =============================================================================

(def TextareaAttrs
  "Textarea attributes"
  [:map
   [:placeholder {:optional true} :string]
   [:value {:optional true} :string]
   [:name {:optional true} :string]
   [:rows {:optional true} :int]
   [:size {:optional true} [:enum :sm :md :lg]]
   [:radius {:optional true} RadiusToken]
   [:disabled {:optional true} :boolean]

   ;; Events
   [:on {:optional true} EventHandlers]])

;; =============================================================================
;; Link Schema
;; =============================================================================

(def LinkAttrs
  "Link attributes"
  [:map
   [:href {:optional true} :string]
   [:target {:optional true} [:enum :_blank :_self :_parent :_top]]
   [:color {:optional true} ColorToken]
   [:underline {:optional true} [:enum :none :underline]]

   ;; Events
   [:on {:optional true} EventHandlers]])

;; =============================================================================
;; Image Schema
;; =============================================================================

(def ImageAttrs
  "Image attributes"
  [:map
   [:src {:optional true} :string]
   [:alt {:optional true} :string]
   [:width {:optional true} SizeToken]
   [:height {:optional true} SizeToken]
   [:radius {:optional true} RadiusToken]
   [:fit {:optional true} [:enum :cover :contain :fill]]

   ;; Events
   [:on {:optional true} EventHandlers]])

;; =============================================================================
;; Video Schema
;; =============================================================================

(def VideoAttrs
  "Video attributes"
  [:map
   [:src {:optional true} :string]
   [:poster {:optional true} :string]
   [:width {:optional true} SizeToken]
   [:height {:optional true} SizeToken]
   [:radius {:optional true} RadiusToken]
   [:controls {:optional true} :boolean]
   [:autoplay {:optional true} :boolean]
   [:loop {:optional true} :boolean]
   [:muted {:optional true} :boolean]

   ;; Events
   [:on {:optional true} EventHandlers]])

;; =============================================================================
;; SVG Schema
;; =============================================================================

(def SVGAttrs
  "SVG attributes"
  [:map
   [:viewBox {:optional true} :string]
   [:size {:optional true} [:enum :xs :sm :md :lg :xl]]
   [:width {:optional true} :int]
   [:height {:optional true} :int]
   [:color {:optional true} ColorToken]
   [:fill {:optional true} [:or ColorToken :string]]
   [:stroke {:optional true} [:or ColorToken :string]]])

;; =============================================================================
;; Button Schema (Composite)
;; =============================================================================

(def ButtonAttrs
  "Button composite attributes"
  [:map
   [:variant {:optional true} [:enum :primary :secondary :ghost :outline]]
   [:size {:optional true} [:enum :xs :sm :md :lg :xl]]
   [:fill {:optional true} ColorToken]
   [:disabled {:optional true} :boolean]

   ;; Events
   [:on {:optional true} EventHandlers]])

;; =============================================================================
;; Animation Schemas (Composites)
;; =============================================================================

(def AnimationAttrs
  "Animation wrapper attributes"
  [:map
   [:duration {:optional true} :int]
   [:delay {:optional true} :int]
   [:easing {:optional true} EasingToken]
   [:fill-mode {:optional true} [:enum :forwards :backwards :both :none]]])

(def SlideAttrs
  "Slide animation attributes"
  [:merge AnimationAttrs
   [:map [:distance {:optional true} :int]]])

(def ScaleAttrs
  "Scale animation attributes"
  [:merge AnimationAttrs
   [:map
    [:from {:optional true} :double]
    [:to {:optional true} :double]]])

;; =============================================================================
;; Validation Helpers
;; =============================================================================

(defn valid?
  "Check if data matches schema"
  [schema data]
  (m/validate schema data))

(defn explain
  "Get human-readable error for invalid data"
  [schema data]
  (when-let [error (m/explain schema data)]
    (me/humanize error)))

(defn validate!
  "Validate data, throw on error with human-readable message"
  [schema data]
  (when-not (m/validate schema data)
    (throw (ex-info "Schema validation failed"
                    {:schema schema
                     :data data
                     :errors (explain schema data)})))
  data)
