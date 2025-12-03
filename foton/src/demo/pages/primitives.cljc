(ns demo.pages.primitives
  "Primitives showcase page.
   All base components: Frame, Text, Icon, Input, etc."
  (:require [demo.pages.shared :as shared]))

;; =============================================================================
;; Frame Primitive
;; =============================================================================

(defn frame-section
  "Frame primitive showcase."
  []
  (shared/section-card
   "Frame"
   "The foundational layout primitive. Flex-based with sensible defaults."
   [:foton.css/frame {:direction :vertical :gap :lg}
    ;; Basic frames
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "Basic Usage")
     [:foton.css/frame {:direction :horizontal :gap :md :wrap :wrap}
      [:foton.css/frame {:fill :surface :radius :md :padding :md}
       [:foton.css/text {:size 12 :color [:text :primary]} "Default frame"]]
      [:foton.css/frame {:fill :primary :radius :lg :padding :md}
       [:foton.css/text {:size 12 :color :white} "Primary fill"]]
      [:foton.css/frame {:fill :transparent :stroke {:width 1 :color :border} :radius :md :padding :md}
       [:foton.css/text {:size 12 :color [:text :primary]} "With stroke"]]]]

    ;; Direction
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "Direction" ":direction :horizontal | :vertical")
     [:foton.css/frame {:direction :horizontal :gap :md}
      [:foton.css/frame {:fill :surface :radius :md :padding :sm :direction :horizontal :gap :sm}
       [:foton.css/frame {:fill :primary :radius :sm :padding :xs}
        [:foton.css/text {:size 10 :color :white} "1"]]
       [:foton.css/frame {:fill :primary :radius :sm :padding :xs}
        [:foton.css/text {:size 10 :color :white} "2"]]
       [:foton.css/frame {:fill :primary :radius :sm :padding :xs}
        [:foton.css/text {:size 10 :color :white} "3"]]]
      [:foton.css/frame {:fill :surface :radius :md :padding :sm :direction :vertical :gap :sm}
       [:foton.css/frame {:fill :primary :radius :sm :padding :xs}
        [:foton.css/text {:size 10 :color :white} "1"]]
       [:foton.css/frame {:fill :primary :radius :sm :padding :xs}
        [:foton.css/text {:size 10 :color :white} "2"]]
       [:foton.css/frame {:fill :primary :radius :sm :padding :xs}
        [:foton.css/text {:size 10 :color :white} "3"]]]]]

    ;; Alignment
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "Alignment" ":align :center | :justify :center")
     [:foton.css/frame {:direction :horizontal :gap :md}
      [:foton.css/frame {:fill :surface :radius :md :width 100 :height 80 :align :start :justify :start}
       [:foton.css/frame {:fill :primary :radius :sm :padding :xs}
        [:foton.css/text {:size 10 :color :white} "start"]]]
      [:foton.css/frame {:fill :surface :radius :md :width 100 :height 80 :align :center :justify :center}
       [:foton.css/frame {:fill :primary :radius :sm :padding :xs}
        [:foton.css/text {:size 10 :color :white} "center"]]]
      [:foton.css/frame {:fill :surface :radius :md :width 100 :height 80 :align :end :justify :end}
       [:foton.css/frame {:fill :primary :radius :sm :padding :xs}
        [:foton.css/text {:size 10 :color :white} "end"]]]]]]))

;; =============================================================================
;; Text Primitive
;; =============================================================================

(defn text-section
  "Text primitive showcase."
  []
  (shared/section-card
   "Text"
   "Typography primitive with presets and customization."
   [:foton.css/frame {:direction :vertical :gap :lg}
    ;; Presets
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "Presets")
     [:foton.css/frame {:direction :vertical :gap :xs}
      [:foton.css/text {:preset :title :color [:text :primary]} "Title preset"]
      [:foton.css/text {:preset :heading :color [:text :primary]} "Heading preset"]
      [:foton.css/text {:preset :body :color [:text :secondary]} "Body preset for paragraphs"]
      [:foton.css/text {:preset :small :color [:text :muted]} "Small preset for details"]]]

    ;; Custom styles
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "Custom Styling")
     [:foton.css/frame {:direction :horizontal :gap :md :wrap :wrap}
      [:foton.css/text {:size 16 :weight 700 :color :primary} "Bold primary"]
      [:foton.css/text {:size 14 :style :italic :color [:text :secondary]} "Italic text"]
      [:foton.css/text {:size 14 :decoration :underline :color [:text :primary]} "Underlined"]
      [:foton.css/text {:size 12 :transform :uppercase :tracking 0.1 :weight 600 :color [:text :muted]} "Label"]]]]))

;; =============================================================================
;; Icon Primitive
;; =============================================================================

(defn icon-section
  "Icon primitive showcase."
  []
  (shared/section-card
   "Icon"
   "FontAwesome icons with size and color tokens."
   [:foton.css/frame {:direction :vertical :gap :lg}
    ;; Sizes
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "Sizes")
     [:foton.css/frame {:direction :horizontal :gap :lg :align :end}
      (for [size [:xs :sm :md :lg :xl]]
        ^{:key size}
        [:foton.css/frame {:direction :vertical :gap :xs :align :center}
         [:foton.css/icon {:name :star :size size :color :primary}]
         [:foton.css/text {:size 10 :color [:text :muted] :family :mono} (str ":" (name size))]])]]

    ;; Common icons
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "Common Icons")
     [:foton.css/frame {:direction :horizontal :gap :md :wrap :wrap}
      (for [[icon color] [[:check [:status :good]]
                          [:x [:status :bad]]
                          [:warning [:status :warning]]
                          [:info [:status :info]]
                          [:home :primary]
                          [:settings [:text :secondary]]
                          [:search [:text :secondary]]
                          [:edit [:text :secondary]]
                          [:trash [:status :bad]]
                          [:plus [:status :good]]
                          [:arrow-up :primary]
                          [:arrow-down :primary]]]
        ^{:key icon}
        [:foton.css/frame {:direction :vertical :gap :xs :align :center}
         [:foton.css/frame {:fill :surface :radius :md :padding :sm :width 40 :height 40 :align :center :justify :center}
          [:foton.css/icon {:name icon :size :md :color color}]]
         [:foton.css/text {:size 9 :color [:text :muted]} (name icon)]])]]]))

;; =============================================================================
;; Input Primitives
;; =============================================================================

(defn input-section
  "Input primitives showcase."
  []
  (shared/section-card
   "Input & Textarea"
   "Form input primitives with sizes and states."
   [:foton.css/frame {:direction :vertical :gap :lg}
    ;; Sizes
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "Input Sizes")
     [:foton.css/frame {:direction :horizontal :gap :md :wrap :wrap :align :end}
      [:foton.css/frame {:direction :vertical :gap :xs}
       [:foton.css/input {:placeholder "Small input" :size :sm}]
       [:foton.css/text {:size 10 :color [:text :muted]} ":size :sm"]]
      [:foton.css/frame {:direction :vertical :gap :xs}
       [:foton.css/input {:placeholder "Medium input" :size :md}]
       [:foton.css/text {:size 10 :color [:text :muted]} ":size :md"]]
      [:foton.css/frame {:direction :vertical :gap :xs}
       [:foton.css/input {:placeholder "Large input" :size :lg}]
       [:foton.css/text {:size 10 :color [:text :muted]} ":size :lg"]]]]

    ;; States
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "States")
     [:foton.css/frame {:direction :horizontal :gap :md :wrap :wrap}
      [:foton.css/frame {:direction :vertical :gap :xs}
       [:foton.css/input {:placeholder "Normal input"}]
       [:foton.css/text {:size 10 :color [:text :muted]} "Normal"]]
      [:foton.css/frame {:direction :vertical :gap :xs}
       [:foton.css/input {:placeholder "Disabled" :disabled true}]
       [:foton.css/text {:size 10 :color [:text :muted]} ":disabled true"]]]]

    ;; Textarea
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "Textarea")
     [:foton.css/frame {:max-width 300}
      [:foton.css/textarea {:placeholder "Write something..." :rows 3}]]]]))

;; =============================================================================
;; Button Composite
;; =============================================================================

(defn button-section
  "Button composite showcase."
  []
  (shared/section-card
   "Button"
   "Composite with variants, sizes, and states."
   [:foton.css/frame {:direction :vertical :gap :lg}
    ;; Variants
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "Variants")
     [:foton.css/frame {:direction :horizontal :gap :sm :wrap :wrap}
      [:foton/button {:variant :primary}
       [:foton.css/text {:color :white} "Primary"]]
      [:foton/button {:variant :secondary}
       [:foton.css/text {:color [:text :primary]} "Secondary"]]
      [:foton/button {:variant :ghost}
       [:foton.css/text {:color [:text :primary]} "Ghost"]]
      [:foton/button {:variant :outline}
       [:foton.css/text {:color :primary} "Outline"]]]]

    ;; Sizes
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "Sizes")
     [:foton.css/frame {:direction :horizontal :gap :sm :wrap :wrap :align :center}
      (for [size [:xs :sm :md :lg :xl]]
        ^{:key size}
        [:foton/button {:variant :primary :size size}
         [:foton.css/text {:color :white} (clojure.string/upper-case (name size))]])]]

    ;; With icons
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "With Icons")
     [:foton.css/frame {:direction :horizontal :gap :sm :wrap :wrap}
      [:foton/button {:variant :primary}
       [:foton.css/frame {:direction :horizontal :gap :sm :align :center}
        [:foton.css/icon {:name :plus :color :white :size :sm}]
        [:foton.css/text {:color :white} "Add Item"]]]
      [:foton/button {:variant :secondary}
       [:foton.css/frame {:direction :horizontal :gap :sm :align :center}
        [:foton.css/icon {:name :download :color [:text :primary] :size :sm}]
        [:foton.css/text {:color [:text :primary]} "Download"]]]
      [:foton/button {:fill [:status :bad]}
       [:foton.css/frame {:direction :horizontal :gap :sm :align :center}
        [:foton.css/icon {:name :trash :color :white :size :sm}]
        [:foton.css/text {:color :white} "Delete"]]]]]

    ;; Disabled
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "Disabled State")
     [:foton/button {:variant :primary :disabled true}
      [:foton.css/text {:color :white} "Disabled"]]]]))

;; =============================================================================
;; Media Primitives
;; =============================================================================

(defn media-section
  "Image, Video, SVG primitives."
  []
  (shared/section-card
   "Media"
   "Image, Video, and SVG primitives."
   [:foton.css/frame {:direction :horizontal :gap :lg :wrap :wrap}
    ;; Image placeholder
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "Image")
     [:foton.css/frame {:fill :surface
                        :radius :lg
                        :width 160
                        :height 100
                        :align :center
                        :justify :center}
      [:foton.css/icon {:name :image :size :xl :color [:text :muted]}]]]

    ;; SVG placeholder
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "SVG")
     [:foton.css/frame {:fill :surface
                        :radius :lg
                        :width 100
                        :height 100
                        :align :center
                        :justify :center}
      [:foton.css/icon {:name :shapes :size :xl :color :primary}]]]]))

;; =============================================================================
;; Link Primitive
;; =============================================================================

(defn link-section
  "Link primitive showcase."
  []
  (shared/section-card
   "Link"
   "Accessible link primitive."
   [:foton.css/frame {:direction :horizontal :gap :lg :wrap :wrap}
    [:foton.css/link {:href "#"}
     [:foton.css/text {:size 14 :color :primary :decoration :underline} "Simple link"]]
    [:foton.css/frame {:direction :horizontal :gap :xs :align :center}
     [:foton.css/link {:href "#"}
      [:foton.css/text {:size 14 :color :primary} "Link with icon"]]
     [:foton.css/icon {:name :external-link :size :xs :color :primary}]]]))

;; =============================================================================
;; Main Page
;; =============================================================================

(defn render
  "Render the primitives page."
  [current-page theme-id]
  (shared/page-layout
   current-page theme-id

   ;; Page header
   [:foton/slide-up {:duration 300}
    [:foton.css/frame {:direction :vertical :gap :xs}
     [:foton.css/text {:size 32 :weight 700 :color [:text :primary]} "Primitives"]
     [:foton.css/text {:size 16 :color [:text :secondary]}
      "Base building blocks for UI composition."]]]

   ;; Two column layout
   [:foton.css/frame {:direction :horizontal :gap :lg :wrap :wrap}
    [:foton.css/frame {:grow 1 :min-width 400}
     [:foton/slide-up {:duration 400 :delay 100}
      (frame-section)]]
    [:foton.css/frame {:grow 1 :min-width 300}
     [:foton/slide-up {:duration 400 :delay 150}
      (text-section)]]]

   [:foton.css/frame {:direction :horizontal :gap :lg :wrap :wrap}
    [:foton.css/frame {:grow 1 :min-width 320}
     [:foton/slide-up {:duration 400 :delay 200}
      (icon-section)]]
    [:foton.css/frame {:grow 1 :min-width 320}
     [:foton/slide-up {:duration 400 :delay 200}
      (input-section)]]]

   [:foton/slide-up {:duration 400 :delay 250}
    (button-section)]

   [:foton.css/frame {:direction :horizontal :gap :lg :wrap :wrap}
    [:foton.css/frame {:grow 1 :min-width 200}
     [:foton/slide-up {:duration 400 :delay 300}
      (media-section)]]
    [:foton.css/frame {:grow 1 :min-width 200}
     [:foton/slide-up {:duration 400 :delay 300}
      (link-section)]]]))
