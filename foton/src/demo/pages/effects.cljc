(ns demo.pages.effects
  "Effects showcase page.
   Interactive effects, shadows, and animations."
  (:require [demo.pages.shared :as shared]))

;; =============================================================================
;; Interactive Effects
;; =============================================================================

(defn- effect-card
  "Demo card for an interactive effect."
  [effect-type attrs icon label description]
  [:foton.css/frame {:direction :vertical :gap :sm :align :center}
   [effect-type attrs
    [:foton.css/frame {:fill :surface
                       :radius :lg
                       :padding :lg
                       :width 120
                       :height 100
                       :align :center
                       :justify :center
                       :shadow :sm}
     [:foton.css/frame {:direction :vertical :gap :xs :align :center}
      [:foton.css/icon {:name icon :size :lg :color :primary}]
      [:foton.css/text {:size 13 :weight 600 :color [:text :primary]} label]]]]
   [:foton.css/text {:size 11 :color [:text :muted] :text-align :center :max-width 100} description]])

(defn interactive-effects-section
  "Hover and press effects."
  []
  (shared/section-card
   "Interactive Effects"
   "Hover over each card to see the effect in action."
   [:foton.css/frame {:direction :horizontal :gap :xl :wrap :wrap :justify :center}
    (effect-card :foton/lift {:distance 8 :shadow :xl}
                 :arrow-up "Lift" "Raises with shadow")
    (effect-card :foton/sink {:distance 3}
                 :arrow-down "Sink" "Presses down")
    (effect-card :foton/grow {:scale 1.08}
                 :plus "Grow" "Scales up")
    (effect-card :foton/shrink {:scale 0.92}
                 :minus "Shrink" "Scales down")
    (effect-card :foton/tilt {:angle 5}
                 :arrow-right "Tilt" "Rotates slightly")
    (effect-card :foton/pop {:distance 10 :scale 1.03}
                 :sparkles "Pop" "Lift + scale + shadow")
    [:foton.css/frame {:direction :vertical :gap :sm :align :center}
     [:foton/glow {:intensity :lg}
      [:foton.css/frame {:fill :primary
                         :radius :lg
                         :padding :lg
                         :width 120
                         :height 100
                         :align :center
                         :justify :center}
       [:foton.css/frame {:direction :vertical :gap :xs :align :center}
        [:foton.css/icon {:name :sun :size :lg :color :white}]
        [:foton.css/text {:size 13 :weight 600 :color :white} "Glow"]]]]
     [:foton.css/text {:size 11 :color [:text :muted] :text-align :center} "Colored shadow"]]
    [:foton.css/frame {:direction :vertical :gap :sm :align :center}
     [:foton/press {}
      [:foton.css/frame {:fill :surface
                         :radius :lg
                         :padding :lg
                         :width 120
                         :height 100
                         :align :center
                         :justify :center
                         :shadow :sm}
       [:foton.css/frame {:direction :vertical :gap :xs :align :center}
        [:foton.css/icon {:name :hand-pointer :size :lg :color [:status :info]}]
        [:foton.css/text {:size 13 :weight 600 :color [:text :primary]} "Press"]]]]
     [:foton.css/text {:size 11 :color [:text :muted] :text-align :center} "Click to see"]]]))

;; =============================================================================
;; Trigger Options
;; =============================================================================

(defn trigger-options-section
  "Different trigger states."
  []
  (shared/section-card
   "Trigger Options"
   "Effects can be triggered by different states."
   [:foton.css/frame {:direction :horizontal :gap :xl :wrap :wrap}
    ;; Hover trigger
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "Hover" ":trigger :hovered")
     [:foton/lift {:trigger :hovered :distance 6}
      [:foton.css/frame {:fill :surface :radius :md :padding :md :shadow :sm}
       [:foton.css/text {:size 13 :color [:text :primary]} "Hover me"]]]]

    ;; Focus trigger (needs focusable element)
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "Focus" ":trigger :focused")
     [:foton.css/frame {:fill :surface
                        :radius :md
                        :padding :md
                        :shadow :sm
                        :vary {:focused {:shadow :lg :stroke {:width 2 :color :primary}}}
                        :transition 150}
      [:foton.css/input {:placeholder "Focus me..." :size :md}]]]

    ;; Press trigger
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "Press" ":trigger :pressed")
     [:foton/press {:trigger :pressed}
      [:foton.css/frame {:fill :primary :radius :md :padding :md}
       [:foton.css/text {:size 13 :color :white} "Press me"]]]]]))

;; =============================================================================
;; Shadow System
;; =============================================================================

(defn shadows-section
  "Shadow presets showcase."
  []
  (shared/section-card
   "Shadow System"
   "Layered shadows with colored ambient light for depth."
   [:foton.css/frame {:direction :horizontal :gap :xl :wrap :wrap :padding :lg}
    (for [[key label] [[:sm "Small"] [:md "Medium"] [:lg "Large"] [:xl "X-Large"] [:2xl "2X-Large"]]]
      ^{:key key}
      [:foton.css/frame {:direction :vertical :gap :sm :align :center}
       [:foton/lift {:shadow key}
        [:foton.css/frame {:fill :surface
                           :radius :md
                           :padding :md
                           :shadow key
                           :width 80
                           :height 60
                           :align :center
                           :justify :center}
         [:foton.css/text {:size 12 :weight 500 :color [:text :secondary] :family :mono}
          (str ":" (name key))]]]
       [:foton.css/text {:size 11 :color [:text :muted]} label]])

    ;; Inner shadow
    [:foton.css/frame {:direction :vertical :gap :sm :align :center}
     [:foton.css/frame {:fill :surface
                        :radius :md
                        :padding :md
                        :shadow :inner
                        :width 80
                        :height 60
                        :align :center
                        :justify :center}
      [:foton.css/text {:size 12 :weight 500 :color [:text :secondary] :family :mono} ":inner"]]
     [:foton.css/text {:size 11 :color [:text :muted]} "Inset"]]

    ;; Glow
    [:foton.css/frame {:direction :vertical :gap :sm :align :center}
     [:foton.css/frame {:fill :primary
                        :radius :md
                        :padding :md
                        :shadow :glow
                        :width 80
                        :height 60
                        :align :center
                        :justify :center}
      [:foton.css/text {:size 12 :weight 500 :color :white :family :mono} ":glow"]]
     [:foton.css/text {:size 11 :color [:text :muted]} "Glow"]]]))

;; =============================================================================
;; Entrance Animations
;; =============================================================================

(defn entrance-animations-section
  "Fade, slide, and scale animations."
  []
  (shared/section-card
   "Entrance Animations"
   "Animation composites for page transitions and reveal effects."
   [:foton.css/frame {:direction :vertical :gap :lg}
    ;; Fade
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "Fade" "Opacity transition")
     [:foton.css/frame {:direction :horizontal :gap :md}
      [:foton/fade-in {:duration 600}
       [:foton.css/frame {:fill :surface :radius :md :padding :md}
        [:foton.css/text {:size 12 :color [:text :primary]} "Fade In (600ms)"]]]
      [:foton/fade-in {:duration 800 :delay 200}
       [:foton.css/frame {:fill :surface :radius :md :padding :md}
        [:foton.css/text {:size 12 :color [:text :primary]} "Delayed (200ms)"]]]]]

    ;; Slide
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "Slide" "Directional movement")
     [:foton.css/frame {:direction :horizontal :gap :md :wrap :wrap}
      [:foton/slide-up {:duration 500}
       [:foton.css/frame {:fill :primary :radius :md :padding :md}
        [:foton.css/text {:color :white :size 12} "Slide Up"]]]
      [:foton/slide-down {:duration 500 :delay 100}
       [:foton.css/frame {:fill :primary :radius :md :padding :md}
        [:foton.css/text {:color :white :size 12} "Slide Down"]]]
      [:foton/slide-left {:duration 500 :delay 200}
       [:foton.css/frame {:fill :primary :radius :md :padding :md}
        [:foton.css/text {:color :white :size 12} "Slide Left"]]]
      [:foton/slide-right {:duration 500 :delay 300}
       [:foton.css/frame {:fill :primary :radius :md :padding :md}
        [:foton.css/text {:color :white :size 12} "Slide Right"]]]]]

    ;; Scale
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "Scale" "Size transition")
     [:foton.css/frame {:direction :horizontal :gap :md}
      [:foton/scale-in {:duration 500}
       [:foton.css/frame {:fill [:status :good] :radius :md :padding :md}
        [:foton.css/text {:color :white :size 12} "Scale In"]]]
      [:foton/scale-in {:duration 500 :delay 200 :from 0.5}
       [:foton.css/frame {:fill [:status :warning] :radius :md :padding :md}
        [:foton.css/text {:color :white :size 12} "From 0.5"]]]]]]))

;; =============================================================================
;; Easing Functions
;; =============================================================================

(defn easing-section
  "Different easing curves."
  []
  (shared/section-card
   "Easing Functions"
   "Available timing functions for animations."
   [:foton.css/frame {:direction :horizontal :gap :lg :wrap :wrap}
    (for [[key label] [[:linear "Linear"]
                       [:ease "Ease"]
                       [:ease-in "Ease In"]
                       [:ease-out "Ease Out"]
                       [:ease-in-out "Ease In-Out"]
                       [:spring "Spring"]
                       [:bounce "Bounce"]]]
      ^{:key key}
      [:foton.css/frame {:direction :vertical :gap :sm :align :center}
       [:foton.css/frame {:fill :surface
                          :radius :md
                          :padding :md
                          :width 100
                          :height 60
                          :align :center
                          :justify :center
                          :vary {:hovered {:fill :primary :translate-y -4}}
                          :transition {:duration 300 :easing key}
                          :cursor :pointer}
        [:foton.css/text {:size 12 :weight 500 :color [:text :primary]
                          :vary {:hovered {:color :white}}} label]]
       [:foton.css/text {:size 10 :color [:text :muted] :family :mono}
        (str ":" (name key))]])]))

;; =============================================================================
;; Combining Effects
;; =============================================================================

(defn combined-effects-section
  "Combining multiple effects."
  []
  (shared/section-card
   "Combining Effects"
   "Effects can be combined for richer interactions."
   [:foton.css/frame {:direction :horizontal :gap :lg :wrap :wrap}
    ;; Card with multiple states
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "Card Interaction")
     [:foton.css/frame {:fill :surface
                        :radius :lg
                        :padding :lg
                        :shadow :sm
                        :direction :vertical
                        :gap :sm
                        :width 200
                        :vary {:hovered {:translate-y -4 :shadow :lg}
                               :pressed {:translate-y 0 :scale 0.98 :shadow :sm}}
                        :transition {:duration 200 :easing :spring}
                        :cursor :pointer}
      [:foton.css/frame {:fill :primary :radius :md :padding :sm :width 48 :height 48 :align :center :justify :center}
       [:foton.css/icon {:name :sparkles :color :white :size :md}]]
      [:foton.css/text {:size 14 :weight 600 :color [:text :primary]} "Interactive Card"]
      [:foton.css/text {:size 12 :color [:text :secondary]} "Hover and click me"]]]

    ;; Button with glow
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "Glowing Button")
     [:foton.css/frame {:fill :primary
                        :radius :lg
                        :padding {:v 12 :h 24}
                        :shadow :sm
                        :direction :horizontal
                        :gap :sm
                        :align :center
                        :vary {:hovered {:shadow :glow :translate-y -2}
                               :pressed {:shadow :sm :translate-y 0 :scale 0.98}}
                        :transition {:duration 200 :easing :ease-out}
                        :cursor :pointer}
      [:foton.css/icon {:name :bolt :color :white :size :sm}]
      [:foton.css/text {:size 14 :weight 600 :color :white} "Activate"]]]

    ;; Tilt + scale
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "Playful Element")
     [:foton.css/frame {:fill [:status :warning]
                        :radius :full
                        :width 80
                        :height 80
                        :align :center
                        :justify :center
                        :shadow :md
                        :vary {:hovered {:rotate 15 :scale 1.1}
                               :pressed {:rotate -5 :scale 0.9}}
                        :transition {:duration 200 :easing :spring}
                        :cursor :pointer}
      [:foton.css/icon {:name :star :color :white :size :lg}]]]]))

;; =============================================================================
;; Main Page
;; =============================================================================

(defn render
  "Render the effects page."
  [current-page theme-id]
  (shared/page-layout
   current-page theme-id

   ;; Page header
   [:foton/slide-up {:duration 300}
    [:foton.css/frame {:direction :vertical :gap :xs}
     [:foton.css/text {:size 32 :weight 700 :color [:text :primary]} "Effects"]
     [:foton.css/text {:size 16 :color [:text :secondary]}
      "Interactive effects, shadows, and animations for engaging UI."]]]

   ;; Interactive effects
   [:foton/slide-up {:duration 400 :delay 100}
    (interactive-effects-section)]

   ;; Trigger options
   [:foton/slide-up {:duration 400 :delay 150}
    (trigger-options-section)]

   ;; Shadows
   [:foton/slide-up {:duration 400 :delay 200}
    (shadows-section)]

   ;; Entrance animations
   [:foton/slide-up {:duration 400 :delay 250}
    (entrance-animations-section)]

   ;; Easing
   [:foton/slide-up {:duration 400 :delay 300}
    (easing-section)]

   ;; Combined
   [:foton/slide-up {:duration 400 :delay 350}
    (combined-effects-section)]))
