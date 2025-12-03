(ns foton.schema
  "Malli schemas for Foton primitives - Figma-compatible UI data"
  (:require [malli.core :as m]
            [malli.error :as me]))

;; -----------------------------------------------------------------------------
;; Value Primitives
;; -----------------------------------------------------------------------------

(def Color
  "Color can be:
   - keyword: :card, :primary (looks up in theme)
   - vector path: [:status :good] (nested theme lookup)
   - hex string: \"#1e293b\" (raw CSS)"
  [:or
   :keyword
   [:vector :keyword]
   [:re #"^#[0-9a-fA-F]{6}$"]])

(def Dimension
  "Dimension can be:
   - keyword: :sm, :md, :lg (theme token)
   - integer: 16, 24 (raw pixels)
   - special: :fill, :hug, :auto (layout sizing)"
  [:or
   :keyword
   :int
   [:enum :fill :hug :auto]])

(def DimensionOrVector
  "Single dimension or [top right bottom left] / [vertical horizontal]"
  [:or
   Dimension
   [:vector Dimension]])

;; -----------------------------------------------------------------------------
;; Frame Schema (Figma 1:1)
;; -----------------------------------------------------------------------------

(def FrameAttrs
  "Frame attributes - maps directly to Figma frame properties"
  [:map
   ;; Appearance
   [:fill {:optional true} Color]
   [:stroke {:optional true} [:or Color [:map
                                         [:color Color]
                                         [:width {:optional true} :int]]]]
   [:radius {:optional true} DimensionOrVector]
   [:opacity {:optional true} [:and :double [:>= 0] [:<= 1]]]

   ;; Layout
   [:direction {:optional true} [:enum :horizontal :vertical]]
   [:gap {:optional true} Dimension]
   [:padding {:optional true} DimensionOrVector]
   [:align {:optional true} [:enum :start :center :end :stretch]]
   [:justify {:optional true} [:enum :start :center :end :space-between]]

   ;; Sizing
   [:width {:optional true} Dimension]
   [:height {:optional true} Dimension]

   ;; Effects
   [:effects {:optional true} [:vector [:map
                                        [:type [:enum :shadow :blur]]
                                        [:blur {:optional true} :int]
                                        [:spread {:optional true} :int]
                                        [:color {:optional true} Color]]]]

   ;; Foton-only (not in Figma)
   [:on {:optional true} [:map-of :keyword :any]]
   [:transition {:optional true} [:enum :default :fast :slow :none]]])

;; -----------------------------------------------------------------------------
;; Text Schema (Figma 1:1)
;; -----------------------------------------------------------------------------

(def TextAttrs
  "Text attributes - maps directly to Figma text properties"
  [:map
   ;; Typography
   [:size {:optional true} [:or :int :keyword]]
   [:weight {:optional true} [:or :int [:enum :normal :medium :semibold :bold]]]
   [:line-height {:optional true} [:or :number :int]]
   [:tracking {:optional true} :number]
   [:text-align {:optional true} [:enum :left :center :right]]

   ;; Appearance
   [:color {:optional true} Color]

   ;; Shortcut for preset styles
   [:style {:optional true} :keyword]

   ;; Foton-only
   [:on {:optional true} [:map-of :keyword :any]]])

;; -----------------------------------------------------------------------------
;; Icon Schema
;; -----------------------------------------------------------------------------

(def IconAttrs
  "Icon attributes"
  [:map
   [:name {:optional true} :keyword]
   [:path {:optional true} :string]
   [:size {:optional true} Dimension]
   [:color {:optional true} Color]
   [:on {:optional true} [:map-of :keyword :any]]])

;; -----------------------------------------------------------------------------
;; Validation Helpers
;; -----------------------------------------------------------------------------

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
