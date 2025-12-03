(ns demo.pages.colors
  "Colors showcase page.
   Displays the complete color system with tokens and usage examples."
  (:require [demo.pages.shared :as shared]))

;; =============================================================================
;; Color Swatch Components
;; =============================================================================

(defn color-swatch
  "Individual color swatch with label and value."
  [color-key color-value & {:keys [size] :or {size :md}}]
  (let [sizes {:sm {:w 48 :h 32}
               :md {:w 64 :h 48}
               :lg {:w 80 :h 64}}
        {:keys [w h]} (get sizes size)]
    [:foton.css/frame {:direction :vertical :gap :xs :align :center}
     [:foton/lift {:distance 4 :shadow :md}
      [:foton.css/frame {:fill color-key
                         :width w
                         :height h
                         :radius :md
                         :shadow :sm}]]
     [:foton.css/text {:size 11 :weight 500 :color [:text :primary] :family :mono}
      (if (keyword? color-key) (name color-key) (str color-key))]
     (when color-value
       [:foton.css/text {:size 10 :color [:text :muted] :family :mono} color-value])]))

(defn color-row
  "Row of color swatches with a title."
  [title swatches]
  [:foton.css/frame {:direction :vertical :gap :md}
   (shared/section-title title)
   [:foton.css/frame {:direction :horizontal :gap :lg :wrap :wrap}
    (for [[k v] swatches]
      ^{:key k}
      (color-swatch k v))]])

;; =============================================================================
;; Color Sections
;; =============================================================================

(defn primary-colors-section
  "Primary brand colors."
  []
  (shared/section-card
   "Brand Colors"
   "Primary colors used for interactive elements and emphasis."
   (color-row "Primary"
              [[:primary "#3b82f6"]
               [:secondary "#64748b"]])
   (color-row "Surfaces"
              [[:background "#0f172a"]
               [:surface "#1e293b"]
               [:card "#1e293b"]
               [:elevated "#334155"]])))

(defn text-colors-section
  "Text color hierarchy."
  []
  (shared/section-card
   "Text Colors"
   "Hierarchical text colors for content organization."
   [:foton.css/frame {:direction :horizontal :gap :xl :wrap :wrap}
    [:foton.css/frame {:direction :vertical :gap :sm}
     [:foton.css/frame {:fill :surface :radius :md :padding :md :gap :sm :direction :vertical}
      [:foton.css/text {:size 24 :weight 700 :color [:text :primary]} "Primary Text"]
      [:foton.css/text {:size 16 :color [:text :secondary]} "Secondary text for supporting content"]
      [:foton.css/text {:size 14 :color [:text :muted]} "Muted text for less important info"]]
     [:foton.css/frame {:direction :horizontal :gap :md}
      [:foton.css/frame {:direction :vertical :gap :xs :align :center}
       [:foton.css/frame {:fill [:text :primary] :width 32 :height 32 :radius :full}]
       [:foton.css/text {:size 10 :color [:text :muted] :family :mono} "primary"]]
      [:foton.css/frame {:direction :vertical :gap :xs :align :center}
       [:foton.css/frame {:fill [:text :secondary] :width 32 :height 32 :radius :full}]
       [:foton.css/text {:size 10 :color [:text :muted] :family :mono} "secondary"]]
      [:foton.css/frame {:direction :vertical :gap :xs :align :center}
       [:foton.css/frame {:fill [:text :muted] :width 32 :height 32 :radius :full}]
       [:foton.css/text {:size 10 :color [:text :muted] :family :mono} "muted"]]]]]))

(defn status-colors-section
  "Status and semantic colors."
  []
  (shared/section-card
   "Status Colors"
   "Semantic colors for feedback and state indication."
   [:foton.css/frame {:direction :horizontal :gap :lg :wrap :wrap}
    ;; Success
    [:foton.css/frame {:direction :vertical :gap :sm :align :center}
     [:foton/lift {}
      [:foton.css/frame {:fill [:status :good]
                         :radius :lg
                         :padding :md
                         :width 120
                         :height 80
                         :align :center
                         :justify :center
                         :shadow :sm}
       [:foton.css/icon {:name :check :size :lg :color :white}]]]
     [:foton.css/text {:size 12 :weight 600 :color [:text :primary]} "Success"]
     [:foton.css/text {:size 10 :color [:text :muted] :family :mono} ":status/good"]]

    ;; Warning
    [:foton.css/frame {:direction :vertical :gap :sm :align :center}
     [:foton/lift {}
      [:foton.css/frame {:fill [:status :warning]
                         :radius :lg
                         :padding :md
                         :width 120
                         :height 80
                         :align :center
                         :justify :center
                         :shadow :sm}
       [:foton.css/icon {:name :warning :size :lg :color :white}]]]
     [:foton.css/text {:size 12 :weight 600 :color [:text :primary]} "Warning"]
     [:foton.css/text {:size 10 :color [:text :muted] :family :mono} ":status/warning"]]

    ;; Error
    [:foton.css/frame {:direction :vertical :gap :sm :align :center}
     [:foton/lift {}
      [:foton.css/frame {:fill [:status :bad]
                         :radius :lg
                         :padding :md
                         :width 120
                         :height 80
                         :align :center
                         :justify :center
                         :shadow :sm}
       [:foton.css/icon {:name :x :size :lg :color :white}]]]
     [:foton.css/text {:size 12 :weight 600 :color [:text :primary]} "Error"]
     [:foton.css/text {:size 10 :color [:text :muted] :family :mono} ":status/bad"]]

    ;; Info
    [:foton.css/frame {:direction :vertical :gap :sm :align :center}
     [:foton/lift {}
      [:foton.css/frame {:fill [:status :info]
                         :radius :lg
                         :padding :md
                         :width 120
                         :height 80
                         :align :center
                         :justify :center
                         :shadow :sm}
       [:foton.css/icon {:name :info :size :lg :color :white}]]]
     [:foton.css/text {:size 12 :weight 600 :color [:text :primary]} "Info"]
     [:foton.css/text {:size 10 :color [:text :muted] :family :mono} ":status/info"]]]))

(defn gradients-section
  "Gradient examples."
  []
  (shared/section-card
   "Gradients"
   "Linear, radial, and conic gradients using color tokens."
   [:foton.css/frame {:direction :horizontal :gap :lg :wrap :wrap}
    ;; Linear
    [:foton.css/frame {:direction :vertical :gap :sm}
     [:foton/pop {}
      [:foton.css/frame {:gradient {:type :linear
                                    :angle 135
                                    :stops [{:color :primary :position 0}
                                            {:color [:status :info] :position 100}]}
                         :radius :lg
                         :width 160
                         :height 100
                         :shadow :md}]]
     [:foton.css/text {:size 12 :weight 500 :color [:text :primary]} "Linear 135°"]]

    ;; Radial
    [:foton.css/frame {:direction :vertical :gap :sm}
     [:foton/pop {}
      [:foton.css/frame {:gradient {:type :radial
                                    :stops [{:color [:status :good] :position 0}
                                            {:color :background :position 100}]}
                         :radius :lg
                         :width 160
                         :height 100
                         :shadow :md}]]
     [:foton.css/text {:size 12 :weight 500 :color [:text :primary]} "Radial"]]

    ;; Conic
    [:foton.css/frame {:direction :vertical :gap :sm}
     [:foton/pop {}
      [:foton.css/frame {:gradient {:type :conic
                                    :angle 0
                                    :stops [{:color :primary :position 0}
                                            {:color [:status :good] :position 33}
                                            {:color [:status :warning] :position 66}
                                            {:color :primary :position 100}]}
                         :radius :full
                         :width 100
                         :height 100
                         :shadow :md}]]
     [:foton.css/text {:size 12 :weight 500 :color [:text :primary]} "Conic"]]

    ;; Sunset
    [:foton.css/frame {:direction :vertical :gap :sm}
     [:foton/pop {}
      [:foton.css/frame {:gradient {:type :linear
                                    :angle 180
                                    :stops [{:color [:status :warning] :position 0}
                                            {:color [:status :bad] :position 50}
                                            {:color :primary :position 100}]}
                         :radius :lg
                         :width 160
                         :height 100
                         :shadow :md}]]
     [:foton.css/text {:size 12 :weight 500 :color [:text :primary]} "Sunset"]]]))

(defn usage-example-section
  "Real-world color usage examples."
  []
  (shared/section-card
   "Usage Examples"
   "How colors work in real components."
   [:foton.css/frame {:direction :horizontal :gap :lg :wrap :wrap}
    ;; Alert examples
    [:foton.css/frame {:direction :vertical :gap :sm}
     [:foton.css/frame {:fill [:status :good]
                        :fill-opacity 0.1
                        :stroke {:width 1 :color [:status :good]}
                        :radius :md
                        :padding :md
                        :direction :horizontal
                        :gap :sm
                        :align :center}
      [:foton.css/icon {:name :check :color [:status :good] :size :sm}]
      [:foton.css/text {:size 13 :color [:status :good]} "Changes saved successfully"]]
     [:foton.css/text {:size 10 :color [:text :muted]} "Success alert"]]

    [:foton.css/frame {:direction :vertical :gap :sm}
     [:foton.css/frame {:fill [:status :warning]
                        :fill-opacity 0.1
                        :stroke {:width 1 :color [:status :warning]}
                        :radius :md
                        :padding :md
                        :direction :horizontal
                        :gap :sm
                        :align :center}
      [:foton.css/icon {:name :warning :color [:status :warning] :size :sm}]
      [:foton.css/text {:size 13 :color [:status :warning]} "Unsaved changes"]]
     [:foton.css/text {:size 10 :color [:text :muted]} "Warning alert"]]

    [:foton.css/frame {:direction :vertical :gap :sm}
     [:foton.css/frame {:fill [:status :bad]
                        :fill-opacity 0.1
                        :stroke {:width 1 :color [:status :bad]}
                        :radius :md
                        :padding :md
                        :direction :horizontal
                        :gap :sm
                        :align :center}
      [:foton.css/icon {:name :x :color [:status :bad] :size :sm}]
      [:foton.css/text {:size 13 :color [:status :bad]} "Failed to connect"]]
     [:foton.css/text {:size 10 :color [:text :muted]} "Error alert"]]]))

;; =============================================================================
;; Main Page
;; =============================================================================

(defn render
  "Render the colors page."
  [current-page theme-id]
  (shared/page-layout
   current-page theme-id

   ;; Page header
   [:foton/slide-up {:duration 300}
    [:foton.css/frame {:direction :vertical :gap :xs}
     [:foton.css/text {:size 32 :weight 700 :color [:text :primary]} "Colors"]
     [:foton.css/text {:size 16 :color [:text :secondary]}
      "A semantic color system with tokens for consistent theming."]]]

   ;; Content grid
   [:foton.css/frame {:direction :horizontal :gap :lg :wrap :wrap}
    [:foton.css/frame {:grow 1 :min-width 320}
     [:foton/slide-up {:duration 400 :delay 100}
      (primary-colors-section)]]
    [:foton.css/frame {:grow 1 :min-width 320}
     [:foton/slide-up {:duration 400 :delay 150}
      (text-colors-section)]]]

   [:foton/slide-up {:duration 400 :delay 200}
    (status-colors-section)]

   [:foton/slide-up {:duration 400 :delay 250}
    (gradients-section)]

   [:foton/slide-up {:duration 400 :delay 300}
    (usage-example-section)]))
