(ns demo.pages.layout
  "Layout showcase page.
   Spacing, flexbox, positioning, and responsive patterns."
  (:require [demo.pages.shared :as shared]))

;; =============================================================================
;; Spacing Scale
;; =============================================================================

(defn spacing-section
  "Spacing scale showcase."
  []
  (shared/section-card
   "Spacing Scale"
   "Consistent spacing tokens based on 4px base unit."
   [:foton.css/frame {:direction :vertical :gap :lg}
    ;; Visual scale
    [:foton.css/frame {:direction :horizontal :gap :md :wrap :wrap :align :end}
     (for [[key px] [[:xs 4] [:sm 8] [:md 16] [:lg 24] [:xl 32] [:2xl 48]]]
       ^{:key key}
       [:foton.css/frame {:direction :vertical :gap :xs :align :center}
        [:foton.css/frame {:fill :primary :width px :height 48 :radius :sm}]
        [:foton.css/text {:preset :caption2 :weight 600 :color [:text :primary] :family :mono}
         (str ":" (name key))]
        [:foton.css/text {:preset :caption2 :color [:text :muted]} (str px "px")]])]

    ;; Usage example
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "Usage")
     [:foton.css/frame {:fill :surface :radius :md :padding :md}
      [:foton.css/text {:preset :caption1 :color [:text :secondary] :family :mono}
       ":padding :md  ;; 16px\n:gap :sm      ;; 8px\n:margin :lg   ;; 24px"]]]]))

;; =============================================================================
;; Border Radius
;; =============================================================================

(defn radius-section
  "Border radius tokens."
  []
  (shared/section-card
   "Border Radius"
   "Concentric radius scale for nested elements."
   [:foton.css/frame {:direction :horizontal :gap :lg :wrap :wrap}
    (for [[key px] [[:none 0] [:xs 4] [:sm 8] [:md 12] [:lg 16] [:xl 20] [:2xl 24] [:full "∞"]]]
      ^{:key key}
      [:foton.css/frame {:direction :vertical :gap :xs :align :center}
       [:foton.css/frame {:fill :primary :width 48 :height 48 :radius key}]
       [:foton.css/text {:preset :caption2 :weight 500 :color [:text :primary] :family :mono}
        (str ":" (name key))]
       [:foton.css/text {:preset :caption2 :color [:text :muted]} (str px (when (number? px) "px"))]])]))

;; =============================================================================
;; Flexbox Layout
;; =============================================================================

(defn flexbox-section
  "Flexbox layout patterns."
  []
  (shared/section-card
   "Flexbox Layout"
   "Frame uses flex by default with direction, alignment, and gap."
   [:foton.css/frame {:direction :vertical :gap :lg}
    ;; Row layout
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "Row with Gap")
     [:foton.css/frame {:fill :surface :radius :md :padding :md :direction :horizontal :gap :md}
      (for [i (range 4)]
        ^{:key i}
        [:foton.css/frame {:fill :primary :radius :sm :padding :sm :grow 1 :align :center :justify :center}
         [:foton.css/text {:preset :caption1 :color :white} (str "Item " (inc i))]])]]

    ;; Justify content
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "Justify Content")
     [:foton.css/frame {:direction :vertical :gap :xs}
      (for [[justify label] [[:start "start"] [:center "center"] [:end "end"] [:space-between "space-between"]]]
        ^{:key justify}
        [:foton.css/frame {:fill :surface :radius :md :padding :sm :direction :horizontal :justify justify}
         (for [i (range 3)]
           ^{:key i}
           [:foton.css/frame {:fill :primary :radius :sm :padding :xs :width 40 :height 24}])
         [:foton.css/text {:preset :caption2 :color [:text :muted] :family :mono :position :absolute :right 8}
          (str ":" label)]])]]

    ;; Align items
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "Align Items")
     [:foton.css/frame {:direction :horizontal :gap :md}
      (for [[align label] [[:start "start"] [:center "center"] [:end "end"]]]
        ^{:key align}
        [:foton.css/frame {:direction :vertical :gap :xs :align :center}
         [:foton.css/frame {:fill :surface :radius :md :padding :sm :direction :horizontal :gap :xs :align align :height 80}
          [:foton.css/frame {:fill :primary :radius :sm :width 24 :height 24}]
          [:foton.css/frame {:fill :primary :radius :sm :width 24 :height 40}]
          [:foton.css/frame {:fill :primary :radius :sm :width 24 :height 32}]]
         [:foton.css/text {:preset :caption2 :color [:text :muted] :family :mono} (str ":" label)]])]]]))

;; =============================================================================
;; Positioning
;; =============================================================================

(defn positioning-section
  "Absolute and relative positioning."
  []
  (shared/section-card
   "Positioning"
   "Absolute positioning with :position, :top, :left, :right, :bottom, :z."
   [:foton.css/frame {:direction :horizontal :gap :lg :wrap :wrap}
    ;; Stacking example
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "Z-Index Stacking")
     [:foton.css/frame {:position :relative :width 180 :height 120 :fill :surface :radius :md}
      [:foton.css/frame {:position :absolute :top 10 :left 10 :z 1
                         :fill :primary :radius :md :padding :sm :width 80 :height 50}
       [:foton.css/text {:preset :caption2 :color :white} "z: 1"]]
      [:foton.css/frame {:position :absolute :top 30 :left 40 :z 2
                         :fill [:status :warning] :radius :md :padding :sm :width 80 :height 50}
       [:foton.css/text {:preset :caption2 :color :white} "z: 2"]]
      [:foton.css/frame {:position :absolute :top 50 :left 70 :z 3
                         :fill [:status :good] :radius :md :padding :sm :width 80 :height 50}
       [:foton.css/text {:preset :caption2 :color :white} "z: 3"]]]]

    ;; Corner positioning
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "Corner Placement")
     [:foton.css/frame {:position :relative :width 180 :height 120 :fill :surface :radius :md}
      [:foton.css/frame {:position :absolute :top 8 :left 8
                         :fill :primary :radius :full :width 24 :height 24 :align :center :justify :center}
       [:foton.css/text {:preset :caption2 :color :white} "TL"]]
      [:foton.css/frame {:position :absolute :top 8 :right 8
                         :fill :primary :radius :full :width 24 :height 24 :align :center :justify :center}
       [:foton.css/text {:preset :caption2 :color :white} "TR"]]
      [:foton.css/frame {:position :absolute :bottom 8 :left 8
                         :fill :primary :radius :full :width 24 :height 24 :align :center :justify :center}
       [:foton.css/text {:preset :caption2 :color :white} "BL"]]
      [:foton.css/frame {:position :absolute :bottom 8 :right 8
                         :fill :primary :radius :full :width 24 :height 24 :align :center :justify :center}
       [:foton.css/text {:preset :caption2 :color :white} "BR"]]]]]))

;; =============================================================================
;; Responsive Patterns
;; =============================================================================

(defn responsive-section
  "Responsive layout patterns."
  []
  (shared/section-card
   "Responsive Patterns"
   "Common layouts that adapt to container width using :wrap."
   [:foton.css/frame {:direction :vertical :gap :lg}
    ;; Card grid
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "Wrapping Cards")
     [:foton.css/frame {:direction :horizontal :gap :md :wrap :wrap}
      (for [i (range 6)]
        ^{:key i}
        [:foton/lift {}
         [:foton.css/frame {:fill :surface :radius :lg :padding :md :min-width 140 :grow 1 :shadow :sm}
          [:foton.css/frame {:direction :vertical :gap :xs}
           [:foton.css/frame {:fill :primary :radius :md :width 32 :height 32 :align :center :justify :center}
            [:foton.css/text {:preset :label :color :white} (inc i)]]
           [:foton.css/text {:preset :caption1 :weight 500 :color [:text :primary]} (str "Card " (inc i))]
           [:foton.css/text {:preset :caption2 :color [:text :muted]} "Auto-wrapping"]]]])]]

    ;; Sidebar layout
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "Sidebar + Content")
     [:foton.css/frame {:direction :horizontal :gap :md :wrap :wrap}
      [:foton.css/frame {:fill :surface :radius :md :padding :md :min-width 120 :shrink 0}
       [:foton.css/text {:preset :caption1 :weight 500 :color [:text :secondary]} "Sidebar"]]
      [:foton.css/frame {:fill :surface :radius :md :padding :md :grow 1 :min-width 200}
       [:foton.css/text {:preset :caption1 :weight 500 :color [:text :secondary]} "Main Content (grows)"]]]]]))

;; =============================================================================
;; Overflow & Scrolling
;; =============================================================================

(defn overflow-section
  "Overflow and scrolling patterns."
  []
  (shared/section-card
   "Overflow & Scrolling"
   "Control content overflow with scroll, hidden, and masking."
   [:foton.css/frame {:direction :horizontal :gap :lg :wrap :wrap}
    ;; Vertical scroll
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "Vertical Scroll")
     [:foton.css/frame {:fill :surface :radius :md :padding :sm :height 100 :overflow-y :scroll :scrollbar :thin}
      (for [i (range 10)]
        ^{:key i}
        [:foton.css/text {:preset :caption1 :color [:text :primary]} (str "Item " (inc i))])]]

    ;; Hidden scrollbar
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "Hidden Scrollbar")
     [:foton.css/frame {:fill :surface :radius :md :padding :sm :height 100 :overflow-y :scroll :scrollbar :hidden}
      (for [i (range 10)]
        ^{:key i}
        [:foton.css/text {:preset :caption1 :color [:text :primary]} (str "Item " (inc i))])]]

    ;; Fade mask
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "Fade Mask")
     [:foton.css/frame {:fill :surface :radius :md :padding :sm :height 100 :overflow-y :scroll :scrollbar :hidden :mask :fade-bottom}
      (for [i (range 10)]
        ^{:key i}
        [:foton.css/text {:preset :caption1 :color [:text :primary]} (str "Item " (inc i))])]]

    ;; Horizontal scroll snap
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "Horizontal Snap")
     [:foton.css/frame {:direction :horizontal :gap :sm :overflow-x :scroll :scroll-snap-type :x :scrollbar :thin :padding :xs}
      (for [i (range 8)]
        ^{:key i}
        [:foton.css/frame {:fill :primary :radius :md :padding :md :min-width 80 :shrink 0 :scroll-snap :center :align :center :justify :center}
         [:foton.css/text {:preset :caption1 :weight 600 :color :white} (inc i)]])]]]))

;; =============================================================================
;; Main Page
;; =============================================================================

(defn render
  "Render the layout page."
  [current-page theme-id]
  (shared/page-layout
   current-page theme-id

   ;; Page header
   [:foton/slide-up {:duration 300}
    [:foton.css/frame {:direction :vertical :gap :xs}
     [:foton.css/text {:preset :title1 :color [:text :primary]} "Layout"]
     [:foton.css/text {:preset :callout :color [:text :secondary]}
      "Spacing, flexbox, positioning, and responsive patterns."]]]

   ;; Two column layout
   [:foton.css/frame {:direction :horizontal :gap :lg :wrap :wrap}
    [:foton.css/frame {:grow 1 :min-width 300}
     [:foton/slide-up {:duration 400 :delay 100}
      (spacing-section)]]
    [:foton.css/frame {:grow 1 :min-width 300}
     [:foton/slide-up {:duration 400 :delay 100}
      (radius-section)]]]

   [:foton/slide-up {:duration 400 :delay 150}
    (flexbox-section)]

   [:foton/slide-up {:duration 400 :delay 200}
    (positioning-section)]

   [:foton/slide-up {:duration 400 :delay 250}
    (responsive-section)]

   [:foton/slide-up {:duration 400 :delay 300}
    (overflow-section)]))
