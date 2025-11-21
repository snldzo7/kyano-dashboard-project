(ns demo.palette
  "Color palette demo UI - showcases all Clay colors with mode switching"
  (:require [clay.color :as c]))

;; ============================================================================
;; COLOR DATA
;; ============================================================================

(def tw-families
  "Tailwind color families in display order"
  [:slate :gray :zinc :neutral :stone
   :red :orange :amber :yellow :lime :green :emerald :teal :cyan
   :sky :blue :indigo :violet :purple :fuchsia :pink :rose])

(def shades
  "Tailwind shade scale"
  [50 100 200 300 400 500 600 700 800 900 950])

(def html-colors-by-category
  "HTML named colors organized by category"
  {:reds [:indian-red :light-coral :salmon :dark-salmon :light-salmon
          :crimson :red :fire-brick :dark-red]
   :pinks [:pink :light-pink :hot-pink :deep-pink :medium-violet-red :pale-violet-red]
   :oranges [:coral :tomato :orange-red :dark-orange :orange]
   :yellows [:gold :yellow :light-yellow :lemon-chiffon :papaya-whip
             :moccasin :peach-puff :pale-goldenrod :khaki :dark-khaki]
   :purples [:lavender :thistle :plum :violet :orchid :fuchsia :magenta
             :medium-orchid :medium-purple :blue-violet :dark-violet
             :dark-orchid :dark-magenta :purple :indigo]
   :greens [:lime :lime-green :pale-green :light-green :spring-green
            :medium-spring-green :medium-sea-green :sea-green :forest-green
            :green :dark-green :yellow-green :olive :dark-olive-green]
   :blues [:aqua :cyan :light-cyan :pale-turquoise :aquamarine :turquoise
           :medium-turquoise :dark-turquoise :steel-blue :light-steel-blue
           :powder-blue :light-blue :sky-blue :light-sky-blue :deep-sky-blue
           :dodger-blue :cornflower-blue :royal-blue :blue :medium-blue
           :dark-blue :navy :midnight-blue]
   :browns [:cornsilk :blanched-almond :bisque :wheat :burly-wood :tan
            :rosy-brown :sandy-brown :goldenrod :peru :chocolate
            :saddle-brown :sienna :brown :maroon]
   :whites [:white :snow :honeydew :mint-cream :azure :alice-blue
            :ghost-white :white-smoke :seashell :beige :old-lace
            :floral-white :ivory :antique-white :linen]
   :grays [:gainsboro :light-gray :silver :dark-gray :gray :dim-gray
           :light-slate-gray :slate-gray :dark-slate-gray :black]})

;; ============================================================================
;; UI COMPONENTS
;; ============================================================================

(defn swatch
  "Single color swatch"
  [color-kw]
  [:box {:id color-kw
         :size [28 28]
         :bg color-kw
         :radius 4}])

(defn family-row
  "Row of swatches for a Tailwind color family"
  [family]
  [:row {:id (keyword (str (name family) "-row"))
         :size [:grow :fit]
         :gap 4
         :align [:left :center]}
   ;; Family label
   [:text (name family) {:size 11
                         :color :slate-500
                         :width 56}]
   ;; Shades
   (mapv #(swatch (keyword (str (name family) "-" %))) shades)])

(defn mode-button
  "Mode selector button"
  [id label active?]
  [:box {:id id
         :size [:fit 32]
         :bg (if active? :slate-600 :slate-800)
         :radius 6
         :pad [12 8]}
   [:text label {:size 12
                 :color (if active? :white :slate-400)}]])

(defn html-category
  "Category of HTML colors"
  [category-name colors]
  [:col {:id (keyword (str "html-" (name category-name)))
         :gap 4}
   [:text (name category-name) {:size 10
                                :color :slate-600}]
   [:row {:gap 3 :wrap true}
    (mapv swatch colors)]])

;; ============================================================================
;; MAIN UI
;; ============================================================================

(defn palette-ui
  "Main palette UI tree. Takes current mode as parameter."
  [current-mode]
  [:col {:id :app
         :size [:grow :grow]
         :bg :slate-950
         :pad 24
         :gap 20}

   ;; === Header ===
   [:row {:id :header
          :size [:grow :fit]
          :gap 24
          :align [:left :center]}

    ;; Title
    [:text "Clay Colors" {:size 28
                          :color :white
                          :font :bold}]

    ;; Mode selector
    [:row {:id :modes
           :gap 6}
     (mode-button :mode-normal "Normal" (= current-mode :normal))
     (mode-button :mode-dark "Dark" (= current-mode :dark))
     (mode-button :mode-grayscale "Gray" (= current-mode :grayscale))
     (mode-button :mode-protanopia "Protanopia" (= current-mode :protanopia))
     (mode-button :mode-deuteranopia "Deuteranopia" (= current-mode :deuteranopia))
     (mode-button :mode-tritanopia "Tritanopia" (= current-mode :tritanopia))]]

   ;; === Tailwind Palette ===
   [:col {:id :tw-section
          :gap 12}

    [:text "Tailwind Colors" {:size 16
                              :color :slate-300}]

    [:col {:id :tw-grid
           :gap 3}
     (mapv family-row tw-families)]]

   ;; === HTML Named Colors ===
   [:col {:id :html-section
          :gap 12}

    [:text "HTML Named Colors" {:size 16
                                :color :slate-300}]

    [:row {:id :html-grid
           :gap 16
           :wrap true}
     (html-category :reds (:reds html-colors-by-category))
     (html-category :oranges (:oranges html-colors-by-category))
     (html-category :yellows (:yellows html-colors-by-category))
     (html-category :greens (:greens html-colors-by-category))
     (html-category :blues (:blues html-colors-by-category))
     (html-category :purples (:purples html-colors-by-category))
     (html-category :pinks (:pinks html-colors-by-category))
     (html-category :browns (:browns html-colors-by-category))
     (html-category :whites (:whites html-colors-by-category))
     (html-category :grays (:grays html-colors-by-category))]]])

;; ============================================================================
;; COLOR MODE APPLICATION
;; ============================================================================

(defn apply-mode
  "Apply color mode transformation to a color keyword"
  [color-kw mode]
  (if (= mode :normal)
    color-kw
    (let [resolved (c/resolve-color color-kw)
          transformed (case mode
                        :dark (c/dark-mode resolved)
                        :grayscale (c/grayscale resolved)
                        :protanopia (c/protanopia resolved)
                        :deuteranopia (c/deuteranopia resolved)
                        :tritanopia (c/tritanopia resolved)
                        resolved)]
      transformed)))

;; ============================================================================
;; DEMO ENTRY POINT
;; ============================================================================

(def initial-state
  {:mode :normal})

(defn demo
  "Generate the demo UI for a given state"
  [{:keys [mode]}]
  (palette-ui mode))

;; Quick test
(comment
  ;; Generate UI tree
  (demo initial-state)

  ;; Count total swatches
  ;; Tailwind: 22 families × 11 shades = 242
  ;; HTML: ~140 colors
  ;; Total: ~382 color swatches

  ;; Test color mode application
  (apply-mode :red-500 :dark)
  (apply-mode :coral :protanopia))
