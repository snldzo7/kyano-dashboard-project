(ns foton.demo
  "Comprehensive demo UI for Foton.

   Showcases all primitives, composites, and themes.
   Uses 100% Foton primitives - no raw CSS.")

;; =============================================================================
;; Navigation Bar
;; =============================================================================

(defn nav-bar
  "Navigation bar component."
  [current-theme]
  [:foton.css/frame {:direction :horizontal
                     :justify :space-between
                     :align :center
                     :padding :md
                     :fill :card
                     :radius :md
                     :shadow :sm}
   ;; Logo / Title
   [:foton.css/frame {:direction :horizontal :gap :sm :align :center}
    [:foton.css/icon {:name :home :color :primary :size :md}]
    [:foton.css/text {:size 18 :weight 700 :color [:text :primary]} "Foton Demo"]]

   ;; Nav Links
   [:foton.css/frame {:direction :horizontal :gap :md :align :center}
    [:ui/a {:ui/location {:location/page-id :pages/frontpage}}
     [:foton.css/frame {:fill :primary :padding :sm :radius :md :cursor :pointer}
      [:foton.css/text {:color :white :size 14} "Home"]]]
    [:ui/a {:ui/location {:location/page-id :pages/composites}}
     [:foton.css/frame {:fill :transparent
                        :padding :sm
                        :radius :md
                        :vary {:hovered {:fill :surface}}
                        :transition 150
                        :cursor :pointer}
      [:foton.css/text {:color [:text :primary] :size 14} "Composites"]]]

    ;; Theme indicator
    [:foton.css/frame {:fill :surface :padding :sm :radius :md}
     [:foton.css/text {:size 12 :color [:text :muted]}
      (str "Theme: " (name current-theme))]]]])

;; =============================================================================
;; Theme Switcher
;; =============================================================================

(defn theme-switcher
  "Theme switcher - segmented control style."
  [current-theme]
  [:foton.css/frame {:direction :horizontal :gap :md :align :center}
   [:foton.css/text {:color [:text :muted] :size 13 :weight 500} "Theme"]
   [:foton.css/frame {:direction :horizontal
                      :fill :surface
                      :radius :lg
                      :padding 4
                      :gap 4}
    ;; Dark button
    [:foton.css/frame {:fill (if (= current-theme :dark) :primary :transparent)
                       :radius :md
                       :padding :sm
                       :cursor :pointer
                       :vary {:hovered {:fill (if (= current-theme :dark) :primary :elevated)}}
                       :transition 150
                       :on {:click [[:ui/set-theme :dark]]}}
     [:foton.css/text {:color (if (= current-theme :dark) :white [:text :primary])
                       :size 13
                       :weight 500}
      "Dark"]]
    ;; Light button
    [:foton.css/frame {:fill (if (= current-theme :light) :primary :transparent)
                       :radius :md
                       :padding :sm
                       :cursor :pointer
                       :vary {:hovered {:fill (if (= current-theme :light) :primary :elevated)}}
                       :transition 150
                       :on {:click [[:ui/set-theme :light]]}}
     [:foton.css/text {:color (if (= current-theme :light) :white [:text :primary])
                       :size 13
                       :weight 500}
      "Light"]]
    ;; Nord button
    [:foton.css/frame {:fill (if (= current-theme :nord) :primary :transparent)
                       :radius :md
                       :padding :sm
                       :cursor :pointer
                       :vary {:hovered {:fill (if (= current-theme :nord) :primary :elevated)}}
                       :transition 150
                       :on {:click [[:ui/set-theme :nord]]}}
     [:foton.css/text {:color (if (= current-theme :nord) :white [:text :primary])
                       :size 13
                       :weight 500}
      "Nord"]]]])

;; =============================================================================
;; Stats Card
;; =============================================================================

(defn stat-card
  "Statistics card with value and change indicator."
  [{:keys [label value change icon]}]
  [:foton.css/frame {:fill :card
                     :radius :lg
                     :padding :md
                     :direction :vertical
                     :gap :sm
                     :grow 1
                     :min-width 150
                     :vary {:hovered {:translate-y -2 :shadow :md}}
                     :transition 200
                     :shadow :sm}
   [:foton.css/frame {:direction :horizontal :justify :space-between :align :center}
    [:foton.css/text {:color [:text :secondary] :size 12} label]
    (when icon
      [:foton.css/icon {:name icon :size :sm :color [:text :muted]}])]
   [:foton.css/text {:size 28 :weight 700 :color [:text :primary]} value]
   (when change
     [:foton.css/frame {:direction :horizontal :gap :xs :align :center}
      [:foton.css/icon {:name (if (pos? change) :arrow-up :arrow-down)
                        :size :xs
                        :color (if (pos? change) [:status :good] [:status :bad])}]
      [:foton.css/text {:size 12 :color (if (pos? change) [:status :good] [:status :bad])}
       (str (when (pos? change) "+") change "%")]])])

;; =============================================================================
;; Forms Section
;; =============================================================================

(defn forms-section
  "Demonstrates input primitives."
  []
  [:foton.css/frame {:fill :card
                     :radius :lg
                     :padding :md
                     :direction :vertical
                     :gap :md}
   [:foton.css/text {:size 16 :weight 600 :color [:text :primary]} "Form Inputs"]

   ;; Text inputs
   [:foton.css/frame {:direction :vertical :gap :sm}
    [:foton.css/text {:size 12 :color [:text :secondary]} "Text Input"]
    [:foton.css/input {:placeholder "Enter your name..." :size :md}]]

   [:foton.css/frame {:direction :vertical :gap :sm}
    [:foton.css/text {:size 12 :color [:text :secondary]} "Search Input"]
    [:foton.css/frame {:direction :horizontal :gap :sm :align :center}
     [:foton.css/icon {:name :search :size :sm :color [:text :muted]}]
     [:foton.css/input {:placeholder "Search..." :size :md :type :search}]]]

   [:foton.css/frame {:direction :vertical :gap :sm}
    [:foton.css/text {:size 12 :color [:text :secondary]} "Disabled Input"]
    [:foton.css/input {:placeholder "Cannot edit" :disabled true}]]

   ;; Textarea
   [:foton.css/frame {:direction :vertical :gap :sm}
    [:foton.css/text {:size 12 :color [:text :secondary]} "Textarea"]
    [:foton.css/textarea {:placeholder "Write your message..." :rows 3}]]])

;; =============================================================================
;; Icons Section
;; =============================================================================

(defn icons-section
  "Demonstrates all available icons."
  []
  [:foton.css/frame {:fill :card
                     :radius :lg
                     :padding :md
                     :direction :vertical
                     :gap :md}
   [:foton.css/text {:size 16 :weight 600 :color [:text :primary]} "Icons"]
   [:foton.css/frame {:direction :horizontal :gap :lg :wrap :wrap :align :center}
    ;; Row 1
    [:foton.css/frame {:direction :vertical :gap :xs :align :center}
     [:foton.css/icon {:name :check :size :md :color [:status :good]}]
     [:foton.css/text {:size 10 :color [:text :muted]} "check"]]
    [:foton.css/frame {:direction :vertical :gap :xs :align :center}
     [:foton.css/icon {:name :x :size :md :color [:status :bad]}]
     [:foton.css/text {:size 10 :color [:text :muted]} "x"]]
    [:foton.css/frame {:direction :vertical :gap :xs :align :center}
     [:foton.css/icon {:name :warning :size :md :color [:status :warning]}]
     [:foton.css/text {:size 10 :color [:text :muted]} "warning"]]
    [:foton.css/frame {:direction :vertical :gap :xs :align :center}
     [:foton.css/icon {:name :info :size :md :color [:status :info]}]
     [:foton.css/text {:size 10 :color [:text :muted]} "info"]]
    [:foton.css/frame {:direction :vertical :gap :xs :align :center}
     [:foton.css/icon {:name :home :size :md :color :primary}]
     [:foton.css/text {:size 10 :color [:text :muted]} "home"]]
    [:foton.css/frame {:direction :vertical :gap :xs :align :center}
     [:foton.css/icon {:name :settings :size :md :color [:text :secondary]}]
     [:foton.css/text {:size 10 :color [:text :muted]} "settings"]]
    [:foton.css/frame {:direction :vertical :gap :xs :align :center}
     [:foton.css/icon {:name :search :size :md :color [:text :secondary]}]
     [:foton.css/text {:size 10 :color [:text :muted]} "search"]]
    [:foton.css/frame {:direction :vertical :gap :xs :align :center}
     [:foton.css/icon {:name :chart :size :md :color :primary}]
     [:foton.css/text {:size 10 :color [:text :muted]} "chart"]]
    [:foton.css/frame {:direction :vertical :gap :xs :align :center}
     [:foton.css/icon {:name :edit :size :md :color [:text :secondary]}]
     [:foton.css/text {:size 10 :color [:text :muted]} "edit"]]
    [:foton.css/frame {:direction :vertical :gap :xs :align :center}
     [:foton.css/icon {:name :trash :size :md :color [:status :bad]}]
     [:foton.css/text {:size 10 :color [:text :muted]} "trash"]]
    [:foton.css/frame {:direction :vertical :gap :xs :align :center}
     [:foton.css/icon {:name :plus :size :md :color [:status :good]}]
     [:foton.css/text {:size 10 :color [:text :muted]} "plus"]]
    [:foton.css/frame {:direction :vertical :gap :xs :align :center}
     [:foton.css/icon {:name :minus :size :md :color [:status :bad]}]
     [:foton.css/text {:size 10 :color [:text :muted]} "minus"]]]])

;; =============================================================================
;; Buttons Section
;; =============================================================================

(defn buttons-section
  "Demonstrates button variants and sizes."
  []
  [:foton.css/frame {:fill :card
                     :radius :lg
                     :padding :md
                     :direction :vertical
                     :gap :md
                     :grow 1
                     :min-width 280}
   [:foton.css/text {:size 16 :weight 600 :color [:text :primary]} "Buttons"]

   ;; Button variants
   [:foton.css/frame {:direction :vertical :gap :sm}
    [:foton.css/text {:size 12 :color [:text :secondary]} "Variants"]
    [:foton.css/frame {:direction :horizontal :gap :sm :wrap :wrap}
     [:foton/button {:variant :primary :on {:click [[:demo/action "primary"]]}}
      [:foton.css/text {:color :white :size 14} "Primary"]]
     [:foton/button {:variant :secondary :on {:click [[:demo/action "secondary"]]}}
      [:foton.css/text {:color [:text :primary] :size 14} "Secondary"]]
     [:foton/button {:variant :ghost :on {:click [[:demo/action "ghost"]]}}
      [:foton.css/text {:color [:text :primary] :size 14} "Ghost"]]
     [:foton/button {:variant :outline :on {:click [[:demo/action "outline"]]}}
      [:foton.css/text {:color :primary :size 14} "Outline"]]]]

   ;; Custom fill colors
   [:foton.css/frame {:direction :vertical :gap :sm}
    [:foton.css/text {:size 12 :color [:text :secondary]} "Custom Colors"]
    [:foton.css/frame {:direction :horizontal :gap :sm :wrap :wrap}
     [:foton/button {:fill [:status :good] :on {:click [[:demo/action "success"]]}}
      [:foton.css/text {:color :white :size 14} "Success"]]
     [:foton/button {:fill [:status :warning] :on {:click [[:demo/action "warning"]]}}
      [:foton.css/text {:color :white :size 14} "Warning"]]
     [:foton/button {:fill [:status :bad] :on {:click [[:demo/action "danger"]]}}
      [:foton.css/text {:color :white :size 14} "Danger"]]]]

   ;; Button sizes
   [:foton.css/frame {:direction :vertical :gap :sm}
    [:foton.css/text {:size 12 :color [:text :secondary]} "Sizes"]
    [:foton.css/frame {:direction :horizontal :gap :sm :align :center :wrap :wrap}
     [:foton/button {:variant :primary :size :xs}
      [:foton.css/text {:color :white} "XS"]]
     [:foton/button {:variant :primary :size :sm}
      [:foton.css/text {:color :white} "SM"]]
     [:foton/button {:variant :primary :size :md}
      [:foton.css/text {:color :white} "MD"]]
     [:foton/button {:variant :primary :size :lg}
      [:foton.css/text {:color :white} "LG"]]
     [:foton/button {:variant :primary :size :xl}
      [:foton.css/text {:color :white} "XL"]]]]

   ;; Disabled state
   [:foton.css/frame {:direction :vertical :gap :sm}
    [:foton.css/text {:size 12 :color [:text :secondary]} "Disabled"]
    [:foton/button {:variant :primary :disabled true}
     [:foton.css/text {:color :white} "Disabled"]]]])

;; =============================================================================
;; Variants Section (Hover Effects)
;; =============================================================================

(defn variants-section
  "Demonstrates Figma-like variant hover effects."
  []
  [:foton.css/frame {:fill :card
                     :radius :lg
                     :padding :md
                     :direction :vertical
                     :gap :md
                     :grow 1
                     :min-width 280}
   [:foton.css/text {:size 16 :weight 600 :color [:text :primary]} "Variants (Hover me!)"]

   ;; Hover effects
   [:foton.css/frame {:direction :horizontal :gap :sm :wrap :wrap}
    ;; Lift on hover
    [:foton.css/frame {:fill :surface
                       :radius :md
                       :padding :md
                       :shadow :sm
                       :vary {:hovered {:fill :elevated :translate-y -4 :shadow :lg}}
                       :transition 200
                       :cursor :pointer}
     [:foton.css/text {:size 12 :color [:text :primary]} "Lift"]]

    ;; Scale on hover
    [:foton.css/frame {:fill :surface
                       :radius :md
                       :padding :md
                       :shadow :sm
                       :vary {:hovered {:scale 1.05 :shadow :md}
                              :pressed {:scale 0.95}}
                       :transition 150
                       :cursor :pointer}
     [:foton.css/text {:size 12 :color [:text :primary]} "Scale"]]

    ;; Color change
    [:foton.css/frame {:fill :surface
                       :radius :md
                       :padding :md
                       :shadow :sm
                       :vary {:hovered {:fill :primary :shadow :md}}
                       :transition 200
                       :cursor :pointer}
     [:foton.css/text {:size 12 :color [:text :primary]} "Color"]]

    ;; Combined effects
    [:foton.css/frame {:fill :primary
                       :radius :md
                       :padding :md
                       :shadow :sm
                       :vary {:hovered {:translate-y -2 :scale 1.02 :shadow :lg}
                              :pressed {:scale 0.98 :shadow :sm}}
                       :transition 150
                       :cursor :pointer}
     [:foton.css/text {:color :white :size 12} "Combined"]]]

   ;; Status indicators
   [:foton.css/text {:size 14 :weight 600 :color [:text :primary]} "Status Indicators"]
   [:foton.css/frame {:direction :vertical :gap :xs}
    [:foton.css/frame {:direction :horizontal :gap :sm :align :center}
     [:foton.css/frame {:fill [:status :good] :radius :full :width 8 :height 8}]
     [:foton.css/text {:size 14 :color [:text :primary]} "System Online"]]
    [:foton.css/frame {:direction :horizontal :gap :sm :align :center}
     [:foton.css/frame {:fill [:status :warning] :radius :full :width 8 :height 8}]
     [:foton.css/text {:size 14 :color [:text :primary]} "3 Warnings"]]
    [:foton.css/frame {:direction :horizontal :gap :sm :align :center}
     [:foton.css/frame {:fill [:status :bad] :radius :full :width 8 :height 8}]
     [:foton.css/text {:size 14 :color [:text :primary]} "1 Error"]]]])

;; =============================================================================
;; Typography Section
;; =============================================================================

(defn typography-section
  "Demonstrates text properties - Figma typography."
  []
  [:foton.css/frame {:fill :card
                     :radius :lg
                     :padding :md
                     :direction :vertical
                     :gap :md
                     :grow 1
                     :min-width 280}
   [:foton.css/text {:size 16 :weight 600 :color [:text :primary]} "Typography"]

   ;; Font families
   [:foton.css/frame {:direction :vertical :gap :xs}
    [:foton.css/text {:size 12 :color [:text :secondary]} "Font Families"]
    [:foton.css/text {:family :sans :size 14 :color [:text :primary]} "Sans-serif (system)"]
    [:foton.css/text {:family :serif :size 14 :color [:text :primary]} "Serif (Georgia)"]
    [:foton.css/text {:family :mono :size 14 :color [:text :primary]} "Monospace (code)"]]

   ;; Text styles
   [:foton.css/frame {:direction :vertical :gap :xs}
    [:foton.css/text {:size 12 :color [:text :secondary]} "Styles & Decoration"]
    [:foton.css/text {:style :italic :size 14 :color [:text :primary]} "Italic text"]
    [:foton.css/text {:decoration :underline :size 14 :color [:text :primary]} "Underlined text"]
    [:foton.css/text {:decoration :line-through :size 14 :color [:text :muted]} "Strikethrough"]]

   ;; Text transform
   [:foton.css/frame {:direction :vertical :gap :xs}
    [:foton.css/text {:size 12 :color [:text :secondary]} "Transform"]
    [:foton.css/text {:transform :uppercase :size 12 :weight 600 :tracking 0.1 :color [:text :primary]} "uppercase label"]
    [:foton.css/text {:transform :capitalize :size 14 :color [:text :primary]} "capitalized text"]]

   ;; Truncation
   [:foton.css/frame {:direction :vertical :gap :xs}
    [:foton.css/text {:size 12 :color [:text :secondary]} "Truncation"]
    [:foton.css/frame {:width 200}
     [:foton.css/text {:truncate true :size 14 :color [:text :primary]}
      "This is a very long text that will be truncated with ellipsis"]]
    [:foton.css/frame {:width 200}
     [:foton.css/text {:max-lines 2 :size 14 :color [:text :primary]}
      "This text will be clamped to two lines maximum. Any additional content will be hidden with an ellipsis at the end."]]]])

;; =============================================================================
;; Scrolling & Masking Section
;; =============================================================================

(defn scrolling-section
  "Demonstrates scrolling and masking capabilities."
  []
  [:foton.css/frame {:fill :card
                     :radius :lg
                     :padding :md
                     :direction :vertical
                     :gap :md
                     :grow 1
                     :min-width 280}
   [:foton.css/text {:size 16 :weight 600 :color [:text :primary]} "Scrolling & Masking"]

   ;; Scrollable container with hidden scrollbar
   [:foton.css/frame {:direction :vertical :gap :xs}
    [:foton.css/text {:size 12 :color [:text :secondary]} "Hidden Scrollbar"]
    [:foton.css/frame {:height 100
                       :overflow-y :scroll
                       :scrollbar :hidden
                       :fill :surface
                       :radius :md
                       :padding :sm}
     (for [i (range 10)]
       ^{:key i}
       [:foton.css/text {:size 14 :color [:text :primary]} (str "Scrollable item " (inc i))])]]

   ;; Fade mask at bottom
   [:foton.css/frame {:direction :vertical :gap :xs}
    [:foton.css/text {:size 12 :color [:text :secondary]} "Fade Mask (bottom)"]
    [:foton.css/frame {:height 80
                       :overflow-y :scroll
                       :scrollbar :hidden
                       :mask :fade-bottom
                       :fill :surface
                       :radius :md
                       :padding :sm}
     (for [i (range 8)]
       ^{:key i}
       [:foton.css/text {:size 14 :color [:text :primary]} (str "Fading item " (inc i))])]]

   ;; Horizontal scroll snap
   [:foton.css/frame {:direction :vertical :gap :xs}
    [:foton.css/text {:size 12 :color [:text :secondary]} "Horizontal Scroll Snap"]
    [:foton.css/frame {:direction :horizontal
                       :gap :sm
                       :overflow-x :scroll
                       :scroll-snap-type :x
                       :scroll-behavior :smooth
                       :scrollbar :thin
                       :padding :xs}
     (for [i (range 6)]
       ^{:key i}
       [:foton.css/frame {:fill :primary
                          :radius :md
                          :padding :md
                          :min-width 120
                          :scroll-snap :center
                          :shrink 0}
        [:foton.css/text {:color :white :size 14 :weight 600} (str "Card " (inc i))]])]]

   ;; Clip path shapes
   [:foton.css/frame {:direction :vertical :gap :xs}
    [:foton.css/text {:size 12 :color [:text :secondary]} "Clip Paths"]
    [:foton.css/frame {:direction :horizontal :gap :md :align :center}
     [:foton.css/frame {:clip-path :circle
                        :fill :primary
                        :width 48
                        :height 48
                        :align :center
                        :justify :center}
      [:foton.css/text {:color :white :size 12} "A"]]
     [:foton.css/frame {:clip-path "polygon(50% 0%, 100% 100%, 0% 100%)"
                        :fill [:status :warning]
                        :width 48
                        :height 48
                        :align :center
                        :justify :center}]
     [:foton.css/frame {:clip-path "polygon(25% 0%, 75% 0%, 100% 50%, 75% 100%, 25% 100%, 0% 50%)"
                        :fill [:status :good]
                        :width 48
                        :height 48}]]]])

;; =============================================================================
;; P0 Features Section (Positioning, Gradients, Backgrounds)
;; =============================================================================

(defn p0-features-section
  "Demonstrates P0 Figma features: positioning, gradients, background images."
  []
  [:foton.css/frame {:fill :card
                     :radius :lg
                     :padding :md
                     :direction :vertical
                     :gap :md
                     :grow 1
                     :min-width 280}
   [:foton.css/text {:size 16 :weight 600 :color [:text :primary]} "P0 Figma Features"]

   ;; Gradients
   [:foton.css/frame {:direction :vertical :gap :xs}
    [:foton.css/text {:size 12 :color [:text :secondary]} "Gradients"]
    [:foton.css/frame {:direction :horizontal :gap :sm :wrap :wrap}
     ;; Linear gradient
     [:foton.css/frame {:gradient {:type :linear
                                   :angle 90
                                   :stops [{:color :primary :position 0}
                                           {:color [:status :info] :position 100}]}
                        :radius :md
                        :padding :md
                        :width 100
                        :height 60
                        :align :center
                        :justify :center}
      [:foton.css/text {:color :white :size 12 :weight 600} "Linear"]]

     ;; Radial gradient
     [:foton.css/frame {:gradient {:type :radial
                                   :stops [{:color :primary :position 0}
                                           {:color :background :position 100}]}
                        :radius :md
                        :padding :md
                        :width 100
                        :height 60
                        :align :center
                        :justify :center}
      [:foton.css/text {:color :white :size 12 :weight 600} "Radial"]]

     ;; Conic gradient
     [:foton.css/frame {:gradient {:type :conic
                                   :angle 0
                                   :stops [{:color [:status :good] :position 0}
                                           {:color [:status :warning] :position 50}
                                           {:color [:status :bad] :position 100}]}
                        :radius :full
                        :width 60
                        :height 60}]]]

   ;; Absolute Positioning & Z-index
   [:foton.css/frame {:direction :vertical :gap :xs}
    [:foton.css/text {:size 12 :color [:text :secondary]} "Positioning & Z-index"]
    [:foton.css/frame {:position :relative
                       :height 100
                       :fill :surface
                       :radius :md}
     ;; Bottom layer
     [:foton.css/frame {:position :absolute
                        :top 10
                        :left 10
                        :z 1
                        :fill :primary
                        :radius :md
                        :padding :sm
                        :width 80
                        :height 60}
      [:foton.css/text {:color :white :size 10} "z: 1"]]

     ;; Middle layer
     [:foton.css/frame {:position :absolute
                        :top 25
                        :left 40
                        :z 2
                        :fill [:status :warning]
                        :radius :md
                        :padding :sm
                        :width 80
                        :height 60}
      [:foton.css/text {:color :white :size 10} "z: 2"]]

     ;; Top layer
     [:foton.css/frame {:position :absolute
                        :top 40
                        :left 70
                        :z 3
                        :fill [:status :good]
                        :radius :md
                        :padding :sm
                        :width 80
                        :height 60}
      [:foton.css/text {:color :white :size 10} "z: 3"]]]]

   ;; Visibility toggle hint
   [:foton.css/frame {:direction :vertical :gap :xs}
    [:foton.css/text {:size 12 :color [:text :secondary]} "Visibility"]
    [:foton.css/frame {:direction :horizontal :gap :sm :align :center}
     [:foton.css/frame {:fill :primary :padding :sm :radius :md}
      [:foton.css/text {:color :white :size 12} "Visible"]]
     [:foton.css/frame {:fill :primary :padding :sm :radius :md :visible false}
      [:foton.css/text {:color :white :size 12} "Hidden"]]
     [:foton.css/text {:size 12 :color [:text :muted]} "(second box is hidden)"]]]])

;; =============================================================================
;; Interactive Effects Section
;; =============================================================================

(defn effects-section
  "Demonstrates interactive effects: lift, sink, grow, shrink, tilt, glow, pop, press."
  []
  [:foton.css/frame {:fill :card
                     :radius :lg
                     :padding :md
                     :direction :vertical
                     :gap :md
                     :grow 1
                     :min-width 280}
   [:foton.css/text {:size 16 :weight 600 :color [:text :primary]} "Interactive Effects"]
   [:foton.css/text {:size 12 :color [:text :muted]} "Hover over each card to see the effect"]

   ;; Effects grid
   [:foton.css/frame {:direction :horizontal :gap :md :wrap :wrap}
    ;; Lift
    [:foton/lift {:distance 6 :shadow :xl}
     [:foton.css/frame {:fill :surface
                        :radius :lg
                        :padding :md
                        :width 100
                        :height 80
                        :align :center
                        :justify :center
                        :shadow :sm}
      [:foton.css/frame {:direction :vertical :gap :xs :align :center}
       [:foton.css/icon {:name :arrow-up :size :md :color :primary}]
       [:foton.css/text {:size 12 :weight 600 :color [:text :primary]} "Lift"]]]]

    ;; Sink
    [:foton/sink {:distance 3}
     [:foton.css/frame {:fill :surface
                        :radius :lg
                        :padding :md
                        :width 100
                        :height 80
                        :align :center
                        :justify :center
                        :shadow :sm}
      [:foton.css/frame {:direction :vertical :gap :xs :align :center}
       [:foton.css/icon {:name :arrow-down :size :md :color :secondary}]
       [:foton.css/text {:size 12 :weight 600 :color [:text :primary]} "Sink"]]]]

    ;; Grow
    [:foton/grow {:scale 1.08}
     [:foton.css/frame {:fill :surface
                        :radius :lg
                        :padding :md
                        :width 100
                        :height 80
                        :align :center
                        :justify :center
                        :shadow :sm}
      [:foton.css/frame {:direction :vertical :gap :xs :align :center}
       [:foton.css/icon {:name :plus :size :md :color [:status :good]}]
       [:foton.css/text {:size 12 :weight 600 :color [:text :primary]} "Grow"]]]]

    ;; Shrink
    [:foton/shrink {:scale 0.92}
     [:foton.css/frame {:fill :surface
                        :radius :lg
                        :padding :md
                        :width 100
                        :height 80
                        :align :center
                        :justify :center
                        :shadow :sm}
      [:foton.css/frame {:direction :vertical :gap :xs :align :center}
       [:foton.css/icon {:name :minus :size :md :color [:status :bad]}]
       [:foton.css/text {:size 12 :weight 600 :color [:text :primary]} "Shrink"]]]]

    ;; Tilt
    [:foton/tilt {:angle 5}
     [:foton.css/frame {:fill :surface
                        :radius :lg
                        :padding :md
                        :width 100
                        :height 80
                        :align :center
                        :justify :center
                        :shadow :sm}
      [:foton.css/frame {:direction :vertical :gap :xs :align :center}
       [:foton.css/icon {:name :arrow-right :size :md :color [:status :warning]}]
       [:foton.css/text {:size 12 :weight 600 :color [:text :primary]} "Tilt"]]]]

    ;; Glow
    [:foton/glow {:intensity :lg}
     [:foton.css/frame {:fill :primary
                        :radius :lg
                        :padding :md
                        :width 100
                        :height 80
                        :align :center
                        :justify :center}
      [:foton.css/frame {:direction :vertical :gap :xs :align :center}
       [:foton.css/icon {:name :info :size :md :color :white}]
       [:foton.css/text {:size 12 :weight 600 :color :white} "Glow"]]]]

    ;; Pop
    [:foton/pop {:distance 8 :scale 1.03}
     [:foton.css/frame {:fill :surface
                        :radius :lg
                        :padding :md
                        :width 100
                        :height 80
                        :align :center
                        :justify :center
                        :shadow :sm}
      [:foton.css/frame {:direction :vertical :gap :xs :align :center}
       [:foton.css/icon {:name :check :size :md :color [:status :good]}]
       [:foton.css/text {:size 12 :weight 600 :color [:text :primary]} "Pop"]]]]

    ;; Press
    [:foton/press {}
     [:foton.css/frame {:fill :surface
                        :radius :lg
                        :padding :md
                        :width 100
                        :height 80
                        :align :center
                        :justify :center
                        :shadow :sm}
      [:foton.css/frame {:direction :vertical :gap :xs :align :center}
       [:foton.css/icon {:name :dot :size :md :color :muted}]
       [:foton.css/text {:size 12 :weight 600 :color [:text :primary]} "Press"]]]]]])

;; =============================================================================
;; Shadows Section
;; =============================================================================

(defn shadows-section
  "Demonstrates shadow presets with colored ambient shadows."
  []
  [:foton.css/frame {:fill :card
                     :radius :lg
                     :padding :md
                     :direction :vertical
                     :gap :md
                     :grow 1
                     :min-width 280}
   [:foton.css/text {:size 16 :weight 600 :color [:text :primary]} "Shadows"]
   [:foton.css/text {:size 12 :color [:text :muted]} "Colored ambient + crisp shadow layers"]

   [:foton.css/frame {:direction :horizontal :gap :lg :wrap :wrap :padding :md}
    ;; sm
    [:foton.css/frame {:fill :surface
                       :radius :md
                       :padding :md
                       :shadow :sm
                       :width 80
                       :height 60
                       :align :center
                       :justify :center}
     [:foton.css/text {:size 11 :weight 500 :color [:text :secondary]} ":sm"]]

    ;; md
    [:foton.css/frame {:fill :surface
                       :radius :md
                       :padding :md
                       :shadow :md
                       :width 80
                       :height 60
                       :align :center
                       :justify :center}
     [:foton.css/text {:size 11 :weight 500 :color [:text :secondary]} ":md"]]

    ;; lg
    [:foton.css/frame {:fill :surface
                       :radius :md
                       :padding :md
                       :shadow :lg
                       :width 80
                       :height 60
                       :align :center
                       :justify :center}
     [:foton.css/text {:size 11 :weight 500 :color [:text :secondary]} ":lg"]]

    ;; xl
    [:foton.css/frame {:fill :surface
                       :radius :md
                       :padding :md
                       :shadow :xl
                       :width 80
                       :height 60
                       :align :center
                       :justify :center}
     [:foton.css/text {:size 11 :weight 500 :color [:text :secondary]} ":xl"]]

    ;; 2xl
    [:foton.css/frame {:fill :surface
                       :radius :md
                       :padding :md
                       :shadow :2xl
                       :width 80
                       :height 60
                       :align :center
                       :justify :center}
     [:foton.css/text {:size 11 :weight 500 :color [:text :secondary]} ":2xl"]]

    ;; glow
    [:foton.css/frame {:fill :primary
                       :radius :md
                       :padding :md
                       :shadow :glow
                       :width 80
                       :height 60
                       :align :center
                       :justify :center}
     [:foton.css/text {:size 11 :weight 500 :color :white} ":glow"]]

    ;; inner
    [:foton.css/frame {:fill :surface
                       :radius :md
                       :padding :md
                       :shadow :inner
                       :width 80
                       :height 60
                       :align :center
                       :justify :center}
     [:foton.css/text {:size 11 :weight 500 :color [:text :secondary]} ":inner"]]]])

;; =============================================================================
;; Main Demo
;; =============================================================================

(defn demo-ui
  "Main demo showing all Foton capabilities with theme switching."
  [current-theme]
  [:foton.css/frame {:fill :background
                     :direction :vertical
                     :gap :lg
                     :padding :lg
                     :min-height :viewport}

   ;; Navigation Bar
   (nav-bar current-theme)

   ;; Header with theme switcher
   [:foton.css/frame {:direction :horizontal
                      :justify :space-between
                      :align :center
                      :wrap :wrap
                      :gap :md}
    [:foton.css/frame {:direction :vertical :gap :xs}
     [:foton.css/text {:size 24 :weight 700 :color [:text :primary]} "Foton Demo"]
     [:foton.css/text {:size 14 :color [:text :secondary]}
      "UI primitives + composites + themes"]]
    (theme-switcher current-theme)]

   ;; Stats Row - responsive with wrap
   [:foton.css/frame {:direction :horizontal :gap :md :wrap :wrap}
    [:foton/fade-in {:duration 300}
     (stat-card {:label "Users" :value "12,847" :change 12 :icon :home})]
    [:foton/fade-in {:duration 400}
     (stat-card {:label "Revenue" :value "$48.2k" :change -3 :icon :chart})]
    [:foton/fade-in {:duration 500}
     (stat-card {:label "Orders" :value "1,284" :change 8 :icon :plus})]
    [:foton/fade-in {:duration 600}
     (stat-card {:label "Conversion" :value "3.2%" :change 0.5 :icon :arrow-up})]]

   ;; Main Content Grid - responsive
   [:foton.css/frame {:direction :horizontal :gap :lg :wrap :wrap}
    ;; Left Panel - Buttons
    (buttons-section)

    ;; Right Panel - Variants
    (variants-section)]

   ;; Bottom Row - Forms & Icons
   [:foton.css/frame {:direction :horizontal :gap :lg :wrap :wrap}
    [:foton.css/frame {:grow 1 :min-width 280}
     (forms-section)]
    [:foton.css/frame {:grow 1 :min-width 280}
     (icons-section)]]

   ;; Typography & Scrolling Row
   [:foton.css/frame {:direction :horizontal :gap :lg :wrap :wrap}
    [:foton.css/frame {:grow 1 :min-width 280}
     (typography-section)]
    [:foton.css/frame {:grow 1 :min-width 280}
     (scrolling-section)]]

   ;; P0 Figma Features Row
   [:foton.css/frame {:direction :horizontal :gap :lg :wrap :wrap}
    [:foton.css/frame {:grow 1 :min-width 280}
     (p0-features-section)]]

   ;; Interactive Effects & Shadows Row
   [:foton.css/frame {:direction :horizontal :gap :lg :wrap :wrap}
    [:foton.css/frame {:grow 1 :min-width 280}
     (effects-section)]
    [:foton.css/frame {:grow 1 :min-width 280}
     (shadows-section)]]])

;; =============================================================================
;; Legacy exports
;; =============================================================================

(defn primitives-demo [] (demo-ui :dark))
(defn composites-demo [] (demo-ui :dark))
