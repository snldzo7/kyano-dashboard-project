(ns demo.pages.shared
  "Shared components for demo pages.
   Navigation, layout, and common UI elements.")

;; =============================================================================
;; Navigation Items
;; =============================================================================

(def nav-items
  "Navigation menu items."
  [{:id :pages/frontpage :label "Home" :icon :house}
   {:id :pages/colors :label "Colors" :icon :palette}
   {:id :pages/typography :label "Typography" :icon :font}
   {:id :pages/primitives :label "Primitives" :icon :cube}
   {:id :pages/effects :label "Effects" :icon :wand-magic-sparkles}
   {:id :pages/layout :label "Layout" :icon :table-columns}])

;; =============================================================================
;; Navigation Bar
;; =============================================================================

(defn nav-link
  "Single navigation link."
  [{:keys [id label icon]} current-page]
  (let [active? (= current-page id)]
    [:ui/a {:ui/location {:location/page-id id}}
     [:foton.css/frame {:fill (if active? :primary :transparent)
                        :padding :sm
                        :radius :md
                        :gap :sm
                        :direction :horizontal
                        :align :center
                        :vary {:hovered {:fill (if active? :primary :surface)}}
                        :transition {:duration 150 :easing :ease-out}
                        :cursor :pointer}
      [:foton.css/icon {:name icon
                        :size :sm
                        :color (if active? :white [:text :secondary])}]
      [:foton.css/text {:preset :label
                        :color (if active? :white [:text :primary])}
       label]]]))

(defn nav-bar
  "Top navigation bar with logo and links."
  [current-page theme-id]
  [:foton.css/frame {:direction :horizontal
                     :justify :space-between
                     :align :center
                     :padding {:v 12 :h 20}
                     :fill :card
                     :radius :lg
                     :shadow :sm}
   ;; Logo
   [:foton.css/frame {:direction :horizontal :gap :sm :align :center}
    [:foton.css/frame {:fill :primary
                       :radius :md
                       :width 32
                       :height 32
                       :align :center
                       :justify :center}
     [:foton.css/text {:preset :callout :weight 700 :color :white} "F"]]
    [:foton.css/text {:preset :headline :color [:text :primary]} "Foton"]]

   ;; Nav Links
   [:foton.css/frame {:direction :horizontal :gap :sm :align :center}
    (for [{:keys [id] :as item} nav-items]
      ^{:key id}
      (nav-link item current-page))]

   ;; Theme Switcher
   [:foton.css/frame {:direction :horizontal :gap :xs :align :center}
    [:foton.css/frame {:fill :surface :radius :full :padding :xs :direction :horizontal :gap :xs}
     [:foton.css/frame {:fill (if (= theme-id :dark) :primary :transparent)
                        :radius :full
                        :width 28
                        :height 28
                        :align :center
                        :justify :center
                        :cursor :pointer
                        :vary {:hovered {:fill (if (= theme-id :dark) :primary :elevated)}}
                        :transition 150
                        :on {:click [[:ui/set-theme :dark]]}}
      [:foton.css/icon {:name :moon :size :xs :color (if (= theme-id :dark) :white [:text :muted])}]]
     [:foton.css/frame {:fill (if (= theme-id :light) :primary :transparent)
                        :radius :full
                        :width 28
                        :height 28
                        :align :center
                        :justify :center
                        :cursor :pointer
                        :vary {:hovered {:fill (if (= theme-id :light) :primary :elevated)}}
                        :transition 150
                        :on {:click [[:ui/set-theme :light]]}}
      [:foton.css/icon {:name :sun :size :xs :color (if (= theme-id :light) :white [:text :muted])}]]
     [:foton.css/frame {:fill (if (= theme-id :nord) :primary :transparent)
                        :radius :full
                        :width 28
                        :height 28
                        :align :center
                        :justify :center
                        :cursor :pointer
                        :vary {:hovered {:fill (if (= theme-id :nord) :primary :elevated)}}
                        :transition 150
                        :on {:click [[:ui/set-theme :nord]]}}
      [:foton.css/icon {:name :snowflake :size :xs :color (if (= theme-id :nord) :white [:text :muted])}]]]]])

;; =============================================================================
;; Page Layout
;; =============================================================================

(defn page-layout
  "Standard page layout with navigation and content area."
  [current-page theme-id & children]
  [:foton.css/frame {:fill :background
                     :direction :vertical
                     :min-height :viewport
                     :padding :lg
                     :gap :lg}
   ;; Navigation
   (nav-bar current-page theme-id)

   ;; Page content with fade transition
   [:foton/fade-in {:duration 200}
    (into [:foton.css/frame {:direction :vertical :gap :lg :grow 1}]
          children)]])

;; =============================================================================
;; Section Components
;; =============================================================================

(defn section-card
  "Card container for a demo section."
  [title description & children]
  [:foton.css/frame {:fill :card
                     :radius :lg
                     :padding :lg
                     :direction :vertical
                     :gap :md
                     :shadow :sm}
   [:foton.css/frame {:direction :vertical :gap :xs}
    [:foton.css/text {:preset :headline :color [:text :primary]} title]
    (when description
      [:foton.css/text {:preset :footnote :color [:text :secondary]} description])]
   (into [:foton.css/frame {:direction :vertical :gap :md}] children)])

(defn section-title
  "Section title with optional subtitle."
  ([title] (section-title title nil))
  ([title subtitle]
   [:foton.css/frame {:direction :vertical :gap :xs}
    [:foton.css/text {:preset :label-sm :color [:text :secondary]} title]
    (when subtitle
      [:foton.css/text {:preset :caption2 :color [:text :muted]} subtitle])]))

(defn demo-row
  "Horizontal row of demo items with wrapping."
  [& children]
  (into [:foton.css/frame {:direction :horizontal :gap :md :wrap :wrap :align :center}]
        children))

(defn demo-item
  "Individual demo item with label."
  [label & children]
  [:foton.css/frame {:direction :vertical :gap :xs :align :center}
   (into [:foton.css/frame {}] children)
   [:foton.css/text {:preset :caption2 :color [:text :muted]} label]])
