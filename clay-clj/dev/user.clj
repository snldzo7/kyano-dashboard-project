(ns user
  "REPL utilities for development.

  This namespace is automatically loaded when starting a REPL with the :dev alias.

  Usage:
  - Compile tests: npx shadow-cljs compile clay-test
  - Watch tests: npx shadow-cljs watch clay-test

  The layout engine uses Specter-based navigators for tree transformations.
  See tree.cljc for navigators, layout.cljc for layout passes, hiccup2.cljc for DSL."
  (:require [clay.hiccup2 :as hiccup]
            [clay.layout :as layout]
            [clay.tree :as t]
            [clay.pointer :as pointer]))

;; ============================================================================
;; COLORS
;; ============================================================================

(def colors
  {:light {:r 244 :g 235 :b 230 :a 255}
   :red {:r 168 :g 66 :b 28 :a 255}
   :orange {:r 225 :g 138 :b 50 :a 255}
   :blue {:r 111 :g 173 :b 162 :a 255}
   :white {:r 255 :g 255 :b 255 :a 255}
   :dark {:r 61 :g 26 :b 5 :a 255}})

;; ============================================================================
;; DEMO UI - Pure Hiccup Data
;; ============================================================================

(defn demo-ui
  "Demo UI defined as pure hiccup data.
   This is what users write - clean, declarative, no layout code!"
  []
  [:col {:size :grow :bg (:light colors) :padding 20 :gap 16}

   ;; Header
   [:row {:width :grow :height 50 :bg (:red colors) :padding [0 16] :align [:left :center]}
    [:text "Clay Hiccup DSL Test" {:font-size 24 :color (:white colors)}]]

   ;; Content row with cards
   [:row {:width :grow :height :fit :gap 16}

    ;; Left card
    [:box {:width [:% 50] :bg (:white colors) :radius 8 :padding 16}
     [:col {:gap 8}
      [:text "Left Card" {:font-size 20 :color (:dark colors)}]
      [:text "This is defined using pure Hiccup data!" {:font-size 14 :color (:dark colors)}]
      [:box {:size :fit :padding [8 16] :bg (:red colors) :radius 8}
       [:text "Click Me" {:font-size 14 :color (:white colors)}]]]]

    ;; Right card
    [:box {:width [:% 50] :bg (:white colors) :radius 8 :padding 16}
     [:col {:gap 8}
      [:text "Right Card" {:font-size 20 :color (:dark colors)}]
      [:text "No layout code needed - just data." {:font-size 14 :color (:dark colors)}]
      [:row {:gap 8}
       [:box {:size 40 :bg (:orange colors) :radius 4}]
       [:box {:size 40 :bg (:blue colors) :radius 4}]
       [:box {:size 40 :bg (:red colors) :radius 4}]]]]]

   ;; Feature boxes row
   [:row {:width :grow :height 80 :gap 8}
    [:box {:width :grow :bg (:red colors) :radius 8 :padding 16 :align [:left :center]}
     [:text "Feature 1" {:font-size 16 :color (:white colors)}]]
    [:box {:width :grow :bg (:orange colors) :radius 8 :padding 16 :align [:left :center]}
     [:text "Feature 2" {:font-size 16 :color (:white colors)}]]
    [:box {:width :grow :bg (:blue colors) :radius 8 :padding 16 :align [:left :center]}
     [:text "Feature 3" {:font-size 16 :color (:white colors)}]]]

   ;; Spacer pushes footer to bottom
   [:spacer {}]

   ;; Footer
   [:row {:width :grow :height 40 :bg (:dark colors) :padding [0 16] :align [:center :center]}
    [:text "Built with Clay Hiccup DSL" {:font-size 14 :color (:light colors)}]]])

;; ============================================================================
;; REPL UTILITIES
;; ============================================================================

(defn mock-measure-fn
  "Simple mock text measurement for REPL testing."
  [text config]
  (let [font-size (or (:font-size config) 16)
        char-width (* font-size 0.6)
        width (* (count text) char-width)
        height font-size]
    {:width width
     :height height
     :min-width width
     :words [{:text text :width width}]}))

(defn render-demo
  "Render the demo UI and return commands (for REPL testing)."
  []
  (hiccup/render {:width 800 :height 600} (demo-ui) mock-measure-fn))

(defn inspect-tree
  "Parse the demo UI and return the tree structure (for debugging)."
  []
  (hiccup/render-tree {:width 800 :height 600} (demo-ui) mock-measure-fn))

(comment
  ;; Test the layout in REPL
  (render-demo)

  ;; Inspect the tree structure
  (inspect-tree)

  ;; Parse a simple UI
  (hiccup/parse {:width 400 :height 300}
                [:col {:padding 16}
                 [:text "Hello World"]]
                mock-measure-fn)

  ;; Run full layout pipeline
  (hiccup/render {:width 400 :height 300}
                 [:row {:gap 8}
                  [:box {:size 50 :bg :red}]
                  [:box {:size :grow :bg :blue}]]
                 mock-measure-fn))
