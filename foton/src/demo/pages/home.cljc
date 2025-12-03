(ns demo.pages.home
  "Home page with overview of Foton capabilities."
  (:require [demo.pages.shared :as shared]))

;; =============================================================================
;; Hero Section
;; =============================================================================

(defn hero-section
  "Hero section with title and description."
  []
  [:foton.css/frame {:direction :vertical
                     :gap :xl
                     :align :center
                     :padding {:v :2xl :h :lg}}
   ;; Logo with gradient background
   [:foton.css/frame {:gradient {:type :linear
                                 :angle 135
                                 :stops [{:color "#3b82f6" :position 0}
                                         {:color "#8b5cf6" :position 100}]}
                      :radius :2xl
                      :width 96
                      :height 96
                      :align :center
                      :justify :center
                      :shadow :lg}
    [:foton.css/text {:preset :large-title :color :white} "F"]]

   ;; Title and subtitle
   [:foton.css/frame {:direction :vertical :gap :md :align :center :max-width 700}
    [:foton.css/text {:preset :hero :color [:text :primary] :text-align :center}
     "Build Beautiful UIs"]
    [:foton.css/text {:preset :body :color [:text :secondary] :text-align :center}
     "A Figma-inspired design system for ClojureScript. Semantic tokens, "
     "declarative components, and smooth animations out of the box."]]

   ;; CTA Buttons
   [:foton.css/frame {:direction :horizontal :gap :md :align :center}
    [:foton/button {:variant :primary :size :lg}
     [:foton.css/text {:preset :body-emphasis :color :white} "Get Started"]
     [:foton.css/icon {:name :arrow-right :color :white :size :sm}]]
    [:foton/button {:variant :outline :size :lg}
     [:foton.css/icon {:name :github :size :sm}]
     [:foton.css/text {:preset :body-emphasis} "GitHub"]]]])

;; =============================================================================
;; Feature Cards - Using CSS Grid-like fixed widths
;; =============================================================================

(def feature-colors
  "Distinct colors for each feature card"
  {:colors   {:bg "#3b82f6" :accent "#60a5fa"}   ; Blue
   :typography {:bg "#8b5cf6" :accent "#a78bfa"} ; Purple
   :primitives {:bg "#10b981" :accent "#34d399"} ; Emerald
   :effects  {:bg "#f59e0b" :accent "#fbbf24"}   ; Amber
   :layout   {:bg "#ec4899" :accent "#f472b6"}}) ; Pink

(defn feature-card
  "Feature highlight card with distinct color."
  [{:keys [icon title description page-id color-key]}]
  (let [{:keys [bg]} (get feature-colors color-key)]
    [:ui/a {:ui/location {:location/page-id page-id}}
     [:foton/pop {:distance 6 :scale 1.02}
      [:foton.css/frame {:fill :card
                         :radius :xl
                         :padding :lg
                         :direction :vertical
                         :gap :md
                         :width 280
                         :shadow :sm
                         :stroke {:width 1 :color :border}
                         :cursor :pointer}
       ;; Icon with colored background
       [:foton.css/frame {:fill bg
                          :radius :lg
                          :width 52
                          :height 52
                          :align :center
                          :justify :center}
        [:foton.css/icon {:name icon :color :white :size :md}]]
       ;; Title
       [:foton.css/text {:preset :headline :color [:text :primary]} title]
       ;; Description
       [:foton.css/text {:preset :footnote :color [:text :secondary]} description]
       ;; Link
       [:foton.css/frame {:direction :horizontal :gap :xs :align :center}
        [:foton.css/text {:preset :label :color bg} "Explore"]
        [:foton.css/icon {:name :arrow-right :color bg :size :xs}]]]]]))

(defn features-section
  "Feature cards in responsive grid."
  []
  [:foton.css/frame {:direction :horizontal
                     :gap :lg
                     :wrap :wrap
                     :justify :center
                     :padding {:v :lg}}
   (feature-card {:icon :palette
                  :title "Colors"
                  :description "Semantic color tokens with themes, gradients, and status colors."
                  :page-id :pages/colors
                  :color-key :colors})
   (feature-card {:icon :font
                  :title "Typography"
                  :description "Type scale, font families, weights, and text styles."
                  :page-id :pages/typography
                  :color-key :typography})
   (feature-card {:icon :cube
                  :title "Primitives"
                  :description "Frame, Text, Icon, Input, Button, and more."
                  :page-id :pages/primitives
                  :color-key :primitives})
   (feature-card {:icon :wand-magic-sparkles
                  :title "Effects"
                  :description "Hover, press, animations, and micro-interactions."
                  :page-id :pages/effects
                  :color-key :effects})
   (feature-card {:icon :table-columns
                  :title "Layout"
                  :description "Flexbox, spacing, positioning, and responsive."
                  :page-id :pages/layout
                  :color-key :layout})])

;; =============================================================================
;; Stats Section with better visual design
;; =============================================================================

(defn stat-item
  "Single stat with colored accent."
  [value label color]
  [:foton.css/frame {:direction :vertical :gap :xs :align :center :padding {:h :lg}}
   [:foton.css/text {:preset :large-title :color color} value]
   [:foton.css/text {:preset :label :color [:text :muted]} label]])

(defn stats-section
  "Quick stats row with gradient background."
  []
  [:foton.css/frame {:fill :card
                     :radius :2xl
                     :padding :xl
                     :direction :horizontal
                     :justify :space-around
                     :wrap :wrap
                     :gap :xl
                     :shadow :md
                     :stroke {:width 1 :color :border}}
   (stat-item "9" "Primitives" "#3b82f6")
   (stat-item "11" "Composites" "#8b5cf6")
   (stat-item "3" "Themes" "#10b981")
   (stat-item "8" "Effects" "#f59e0b")
   (stat-item "∞" "Possibilities" "#ec4899")])

;; =============================================================================
;; Code Example with better styling
;; =============================================================================

(defn code-example-section
  "Show a code example."
  []
  [:foton.css/frame {:direction :horizontal
                     :gap :xl
                     :wrap :wrap
                     :align :stretch}
   ;; Code block
   [:foton.css/frame {:fill :surface
                      :radius :xl
                      :padding :lg
                      :grow 1
                      :min-width 340
                      :stroke {:width 1 :color :border}}
    [:foton.css/frame {:direction :vertical :gap :md}
     [:foton.css/frame {:direction :horizontal :gap :sm :align :center}
      [:foton.css/frame {:fill "#ef4444" :radius :full :width 12 :height 12}]
      [:foton.css/frame {:fill "#f59e0b" :radius :full :width 12 :height 12}]
      [:foton.css/frame {:fill "#22c55e" :radius :full :width 12 :height 12}]]
     [:foton.css/text {:preset :footnote :family :mono :color [:text :secondary]}
      "[:foton.css/frame\n"
      " {:fill :card\n"
      "  :radius :lg\n"
      "  :padding :md\n"
      "  :shadow :sm\n"
      "  :vary {:hovered {:shadow :lg}}}\n"
      " [:foton.css/text {:size 24 :weight 700}\n"
      "  \"Hello, Foton!\"]]"]]]

   ;; Result preview
   [:foton.css/frame {:direction :vertical
                      :gap :md
                      :align :center
                      :justify :center
                      :grow 1
                      :min-width 280}
    [:foton.css/text {:preset :label-sm :color [:text :muted]}
     "Live Preview"]
    [:foton/lift {:distance 8}
     [:foton.css/frame {:fill :card
                        :radius :xl
                        :padding :lg
                        :shadow :md
                        :stroke {:width 1 :color :border}
                        :align :center}
      [:foton.css/text {:preset :title2 :color [:text :primary]} "Hello, Foton!"]]]]])

;; =============================================================================
;; Themes Preview with actual theme colors
;; =============================================================================

(defn theme-card
  "Theme preview card."
  [id label bg-color primary-color surface-color]
  [:foton/lift {:distance 4}
   [:foton.css/frame {:fill bg-color
                      :radius :xl
                      :padding :lg
                      :width 180
                      :direction :vertical
                      :gap :sm
                      :shadow :md
                      :stroke {:width 1 :color :border}}
    ;; Preview bars
    [:foton.css/frame {:fill primary-color :radius :md :height 8}]
    [:foton.css/frame {:fill surface-color :radius :md :height 24}]
    [:foton.css/frame {:direction :horizontal :gap :sm}
     [:foton.css/frame {:fill surface-color :radius :sm :height 16 :grow 1}]
     [:foton.css/frame {:fill surface-color :radius :sm :height 16 :grow 1}]]
    ;; Label
    [:foton.css/text {:preset :label
                      :color (if (= id :light) "#1e293b" "#f8fafc")
                      :text-align :center}
     label]]])

(defn themes-section
  "Theme previews."
  []
  [:foton.css/frame {:direction :vertical :gap :lg :align :center}
   [:foton.css/frame {:direction :vertical :gap :sm :align :center}
    [:foton.css/text {:preset :title1 :color [:text :primary]} "Built-in Themes"]
    [:foton.css/text {:preset :callout :color [:text :secondary]} "Switch themes with a single token change"]]
   [:foton.css/frame {:direction :horizontal :gap :lg :wrap :wrap :justify :center}
    (theme-card :dark "Dark" "#0f172a" "#3b82f6" "#1e293b")
    (theme-card :light "Light" "#ffffff" "#2563eb" "#f1f5f9")
    (theme-card :nord "Nord" "#2e3440" "#88c0d0" "#3b4252")]])

;; =============================================================================
;; Main Page
;; =============================================================================

(defn render
  "Render the home page."
  [current-page theme-id]
  (shared/page-layout
   current-page theme-id

   ;; Hero
   (hero-section)

   ;; Features
   (features-section)

   ;; Stats
   (stats-section)

   ;; Code example
   (code-example-section)

   ;; Themes
   (themes-section)))
