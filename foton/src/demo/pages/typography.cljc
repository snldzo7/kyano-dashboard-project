(ns demo.pages.typography
  "Typography showcase page.
   Displays the complete type system with scales, families, and styles."
  (:require [demo.pages.shared :as shared]))

;; =============================================================================
;; Type Scale
;; =============================================================================

(defn type-scale-section
  "Type scale from display to caption."
  []
  (shared/section-card
   "Type Scale"
   "A harmonious type scale for visual hierarchy."
   [:foton.css/frame {:direction :vertical :gap :lg}
    ;; Display
    [:foton.css/frame {:direction :horizontal :gap :lg :align :baseline}
     [:foton.css/frame {:width 80}
      [:foton.css/text {:size 11 :color [:text :muted] :family :mono} "Display"]]
     [:foton.css/text {:preset :display :color [:text :primary]}
      "The quick brown fox"]]

    ;; Title
    [:foton.css/frame {:direction :horizontal :gap :lg :align :baseline}
     [:foton.css/frame {:width 80}
      [:foton.css/text {:size 11 :color [:text :muted] :family :mono} "Title"]]
     [:foton.css/text {:preset :title :color [:text :primary]}
      "The quick brown fox jumps"]]

    ;; Heading
    [:foton.css/frame {:direction :horizontal :gap :lg :align :baseline}
     [:foton.css/frame {:width 80}
      [:foton.css/text {:size 11 :color [:text :muted] :family :mono} "Heading"]]
     [:foton.css/text {:preset :heading :color [:text :primary]}
      "The quick brown fox jumps over the lazy dog"]]

    ;; Subheading
    [:foton.css/frame {:direction :horizontal :gap :lg :align :baseline}
     [:foton.css/frame {:width 80}
      [:foton.css/text {:size 11 :color [:text :muted] :family :mono} "Subheading"]]
     [:foton.css/text {:preset :subheading :color [:text :primary]}
      "The quick brown fox jumps over the lazy dog"]]

    ;; Body
    [:foton.css/frame {:direction :horizontal :gap :lg :align :baseline}
     [:foton.css/frame {:width 80}
      [:foton.css/text {:size 11 :color [:text :muted] :family :mono} "Body"]]
     [:foton.css/text {:preset :body :color [:text :primary]}
      "The quick brown fox jumps over the lazy dog. Pack my box with five dozen liquor jugs."]]

    ;; Small
    [:foton.css/frame {:direction :horizontal :gap :lg :align :baseline}
     [:foton.css/frame {:width 80}
      [:foton.css/text {:size 11 :color [:text :muted] :family :mono} "Small"]]
     [:foton.css/text {:preset :small :color [:text :primary]}
      "The quick brown fox jumps over the lazy dog. Pack my box with five dozen liquor jugs."]]

    ;; Caption
    [:foton.css/frame {:direction :horizontal :gap :lg :align :baseline}
     [:foton.css/frame {:width 80}
      [:foton.css/text {:size 11 :color [:text :muted] :family :mono} "Caption"]]
     [:foton.css/text {:preset :caption :color [:text :muted]}
      "The quick brown fox jumps over the lazy dog."]]

    ;; Label
    [:foton.css/frame {:direction :horizontal :gap :lg :align :baseline}
     [:foton.css/frame {:width 80}
      [:foton.css/text {:size 11 :color [:text :muted] :family :mono} "Label"]]
     [:foton.css/text {:preset :label :color [:text :secondary]}
      "Label Text"]]]))

;; =============================================================================
;; Font Families
;; =============================================================================

(defn font-families-section
  "Font family showcase."
  []
  (shared/section-card
   "Font Families"
   "Three font families for different purposes."
   [:foton.css/frame {:direction :vertical :gap :xl}
    ;; Sans-serif
    [:foton.css/frame {:fill :surface :radius :lg :padding :lg :direction :vertical :gap :md}
     [:foton.css/frame {:direction :horizontal :justify :space-between :align :center}
      [:foton.css/text {:size 12 :weight 600 :color [:text :secondary] :transform :uppercase :tracking 0.05}
       "Sans-Serif"]
      [:foton.css/text {:size 11 :color [:text :muted] :family :mono} ":family :sans"]]
     [:foton.css/text {:size 32 :weight 300 :family :sans :color [:text :primary]}
      "Inter — Modern & Clean"]
     [:foton.css/text {:size 16 :family :sans :color [:text :secondary]}
      "The primary font for UI text. Optimized for screen legibility at all sizes. "
      "Supports variable font weights from 100 to 900."]]

    ;; Serif
    [:foton.css/frame {:fill :surface :radius :lg :padding :lg :direction :vertical :gap :md}
     [:foton.css/frame {:direction :horizontal :justify :space-between :align :center}
      [:foton.css/text {:size 12 :weight 600 :color [:text :secondary] :transform :uppercase :tracking 0.05}
       "Serif"]
      [:foton.css/text {:size 11 :color [:text :muted] :family :mono} ":family :serif"]]
     [:foton.css/text {:size 32 :weight 400 :family :serif :color [:text :primary]}
      "Source Serif Pro — Editorial"]
     [:foton.css/text {:size 16 :family :serif :color [:text :secondary]}
      "For long-form content and editorial contexts. "
      "Classic proportions with modern refinements."]]

    ;; Monospace
    [:foton.css/frame {:fill :surface :radius :lg :padding :lg :direction :vertical :gap :md}
     [:foton.css/frame {:direction :horizontal :justify :space-between :align :center}
      [:foton.css/text {:size 12 :weight 600 :color [:text :secondary] :transform :uppercase :tracking 0.05}
       "Monospace"]
      [:foton.css/text {:size 11 :color [:text :muted] :family :mono} ":family :mono"]]
     [:foton.css/text {:size 28 :weight 400 :family :mono :color [:text :primary]}
      "JetBrains Mono — Code"]
     [:foton.css/text {:size 16 :family :mono :color [:text :secondary]}
      "(defn hello [] \"world\")"]]]))

;; =============================================================================
;; Font Weights
;; =============================================================================

(defn font-weights-section
  "Font weight scale."
  []
  (shared/section-card
   "Font Weights"
   "Available weights for the sans-serif family."
   [:foton.css/frame {:direction :horizontal :gap :lg :wrap :wrap}
    (for [[weight label] [[400 "Regular"]
                          [500 "Medium"]
                          [600 "Semibold"]
                          [700 "Bold"]]]
      ^{:key weight}
      [:foton.css/frame {:direction :vertical :gap :xs :align :center}
       [:foton.css/frame {:fill :surface
                          :radius :md
                          :padding :md
                          :width 100
                          :height 80
                          :align :center
                          :justify :center}
        [:foton.css/text {:size 24 :weight weight :color [:text :primary]} "Aa"]]
       [:foton.css/text {:size 12 :weight 600 :color [:text :primary]} label]
       [:foton.css/text {:size 11 :color [:text :muted] :family :mono} (str weight)]])]))

;; =============================================================================
;; Text Styles
;; =============================================================================

(defn text-styles-section
  "Text decoration and transformation."
  []
  (shared/section-card
   "Text Styles"
   "Decoration, transformation, and spacing options."
   [:foton.css/frame {:direction :vertical :gap :lg}
    ;; Decorations
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "Decorations")
     [:foton.css/frame {:direction :horizontal :gap :xl :wrap :wrap}
      [:foton.css/frame {:direction :vertical :gap :xs}
       [:foton.css/text {:size 16 :color [:text :primary]} "Normal text"]
       [:foton.css/text {:size 11 :color [:text :muted] :family :mono} "default"]]
      [:foton.css/frame {:direction :vertical :gap :xs}
       [:foton.css/text {:size 16 :style :italic :color [:text :primary]} "Italic text"]
       [:foton.css/text {:size 11 :color [:text :muted] :family :mono} ":style :italic"]]
      [:foton.css/frame {:direction :vertical :gap :xs}
       [:foton.css/text {:size 16 :decoration :underline :color [:text :primary]} "Underlined"]
       [:foton.css/text {:size 11 :color [:text :muted] :family :mono} ":decoration :underline"]]
      [:foton.css/frame {:direction :vertical :gap :xs}
       [:foton.css/text {:size 16 :decoration :line-through :color [:text :muted]} "Strikethrough"]
       [:foton.css/text {:size 11 :color [:text :muted] :family :mono} ":decoration :line-through"]]]]

    ;; Transforms
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "Transformations")
     [:foton.css/frame {:direction :horizontal :gap :xl :wrap :wrap}
      [:foton.css/frame {:direction :vertical :gap :xs}
       [:foton.css/text {:size 14 :transform :uppercase :tracking 0.08 :weight 600 :color [:text :primary]}
        "uppercase"]
       [:foton.css/text {:size 11 :color [:text :muted] :family :mono} ":transform :uppercase"]]
      [:foton.css/frame {:direction :vertical :gap :xs}
       [:foton.css/text {:size 14 :transform :capitalize :color [:text :primary]}
        "capitalize text"]
       [:foton.css/text {:size 11 :color [:text :muted] :family :mono} ":transform :capitalize"]]
      [:foton.css/frame {:direction :vertical :gap :xs}
       [:foton.css/text {:size 14 :transform :lowercase :color [:text :primary]}
        "LOWERCASE"]
       [:foton.css/text {:size 11 :color [:text :muted] :family :mono} ":transform :lowercase"]]]]

    ;; Tracking
    [:foton.css/frame {:direction :vertical :gap :sm}
     (shared/section-title "Letter Spacing (Tracking)")
     [:foton.css/frame {:direction :horizontal :gap :xl :wrap :wrap}
      [:foton.css/frame {:direction :vertical :gap :xs}
       [:foton.css/text {:size 14 :tracking -0.02 :color [:text :primary]} "Tight tracking"]
       [:foton.css/text {:size 11 :color [:text :muted] :family :mono} ":tracking -0.02"]]
      [:foton.css/frame {:direction :vertical :gap :xs}
       [:foton.css/text {:size 14 :tracking 0 :color [:text :primary]} "Normal tracking"]
       [:foton.css/text {:size 11 :color [:text :muted] :family :mono} ":tracking 0"]]
      [:foton.css/frame {:direction :vertical :gap :xs}
       [:foton.css/text {:size 14 :tracking 0.1 :color [:text :primary]} "Wide tracking"]
       [:foton.css/text {:size 11 :color [:text :muted] :family :mono} ":tracking 0.1"]]]]]))

;; =============================================================================
;; Text Overflow
;; =============================================================================

(defn text-overflow-section
  "Truncation and line clamping."
  []
  (shared/section-card
   "Text Overflow"
   "Handling long text content."
   [:foton.css/frame {:direction :horizontal :gap :lg :wrap :wrap}
    ;; Single line truncate
    [:foton.css/frame {:direction :vertical :gap :sm :min-width 200 :max-width 280}
     (shared/section-title "Single Line Truncate")
     [:foton.css/frame {:fill :surface :radius :md :padding :md}
      [:foton.css/text {:size 14 :truncate true :color [:text :primary]}
       "This is a very long text that will be truncated with an ellipsis at the end because it exceeds the container width."]]
     [:foton.css/text {:size 11 :color [:text :muted] :family :mono} ":truncate true"]]

    ;; Multi-line clamp
    [:foton.css/frame {:direction :vertical :gap :sm :min-width 200 :max-width 280}
     (shared/section-title "Multi-line Clamp")
     [:foton.css/frame {:fill :surface :radius :md :padding :md}
      [:foton.css/text {:size 14 :max-lines 2 :color [:text :primary]}
       "This text will be clamped to exactly two lines. Any additional content beyond the second line will be hidden and replaced with an ellipsis to indicate truncation."]]
     [:foton.css/text {:size 11 :color [:text :muted] :family :mono} ":max-lines 2"]]

    ;; Three lines
    [:foton.css/frame {:direction :vertical :gap :sm :min-width 200 :max-width 280}
     (shared/section-title "Three Lines")
     [:foton.css/frame {:fill :surface :radius :md :padding :md}
      [:foton.css/text {:size 14 :max-lines 3 :color [:text :primary]}
       "Here's a longer paragraph that demonstrates three-line clamping. This is useful for card descriptions or preview text where you want to show a consistent amount of content regardless of the actual length of the source text."]]
     [:foton.css/text {:size 11 :color [:text :muted] :family :mono} ":max-lines 3"]]]))

;; =============================================================================
;; Paragraph Example
;; =============================================================================

(defn paragraph-example-section
  "Real-world typography example."
  []
  (shared/section-card
   "Article Example"
   "Typography in context."
   [:foton.css/frame {:max-width 640 :direction :vertical :gap :lg}
    [:foton.css/text {:preset :title :color [:text :primary]}
     "The Art of Visual Hierarchy"]

    [:foton.css/frame {:direction :horizontal :gap :md :align :center}
     [:foton.css/frame {:fill :primary :radius :full :width 32 :height 32 :align :center :justify :center}
      [:foton.css/text {:size 12 :weight 600 :color :white} "JD"]]
     [:foton.css/frame {:direction :vertical}
      [:foton.css/text {:size 13 :weight 500 :color [:text :primary]} "Jane Doe"]
      [:foton.css/text {:size 12 :color [:text :muted]} "Dec 3, 2024 · 5 min read"]]]

    [:foton.css/text {:size 16 :line-height 1.7 :color [:text :secondary]}
     "Good typography establishes a clear visual hierarchy, guiding readers through content with purpose. "
     "The careful balance of size, weight, and spacing creates rhythm and flow that makes complex information accessible."]

    [:foton.css/text {:preset :heading :color [:text :primary]}
     "Understanding Scale"]

    [:foton.css/text {:size 16 :line-height 1.7 :color [:text :secondary]}
     "A well-designed type scale provides enough variety for visual interest while maintaining consistency. "
     "Each step in the scale should serve a clear purpose—from commanding headlines to subtle captions."]

    [:foton.css/frame {:fill :surface :radius :md :padding :md :margin {:left 16} :stroke {:width 2 :color :primary :position :inside}}
     [:foton.css/text {:size 15 :style :italic :color [:text :secondary] :line-height 1.6}
      "\"Typography is what language looks like.\""
      [:foton.css/text {:size 13 :style :normal :color [:text :muted]} " — Ellen Lupton"]]]]))

;; =============================================================================
;; Main Page
;; =============================================================================

(defn render
  "Render the typography page."
  [current-page theme-id]
  (shared/page-layout
   current-page theme-id

   ;; Page header
   [:foton/slide-up {:duration 300}
    [:foton.css/frame {:direction :vertical :gap :xs}
     [:foton.css/text {:size 32 :weight 700 :color [:text :primary]} "Typography"]
     [:foton.css/text {:size 16 :color [:text :secondary]}
      "A flexible type system with Inter, Source Serif Pro, and JetBrains Mono."]]]

   ;; Type scale
   [:foton/slide-up {:duration 400 :delay 100}
    (type-scale-section)]

   ;; Font families
   [:foton/slide-up {:duration 400 :delay 150}
    (font-families-section)]

   ;; Two column layout
   [:foton.css/frame {:direction :horizontal :gap :lg :wrap :wrap}
    [:foton.css/frame {:grow 1 :min-width 300}
     [:foton/slide-up {:duration 400 :delay 200}
      (font-weights-section)]]
    [:foton.css/frame {:grow 1 :min-width 300}
     [:foton/slide-up {:duration 400 :delay 200}
      (text-overflow-section)]]]

   ;; Text styles
   [:foton/slide-up {:duration 400 :delay 250}
    (text-styles-section)]

   ;; Article example
   [:foton/slide-up {:duration 400 :delay 300}
    (paragraph-example-section)]))
