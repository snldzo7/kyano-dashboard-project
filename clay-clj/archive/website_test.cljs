(ns clay.website-test
  "Clay Official Website - Ported from clay-official-website/main.c

   This demonstrates the Rheon DSL for building Clay layouts:
   - :size [:grow :fit] shorthand
   - :width [:% 55] for percentages
   - :padding 16 for uniform padding
   - :bg and :border shorthand
   - row/col helpers for direction"
  (:require [clay.dsl :as ui]
            [clay.layout.text :as text]
            [clay.renderer.canvas2d :as renderer]))

;; ============================================================================
;; COLORS (matching Clay.h official website)
;; ============================================================================

(def colors
  {:light {:r 244 :g 235 :b 230 :a 255}
   :light-hover {:r 224 :g 215 :b 210 :a 255}
   :red {:r 168 :g 66 :b 28 :a 255}
   :red-hover {:r 148 :g 46 :b 8 :a 255}
   :orange {:r 225 :g 138 :b 50 :a 255}
   :blue {:r 111 :g 173 :b 162 :a 255}
   ;; Top border stripe colors
   :top-border-1 {:r 168 :g 66 :b 28 :a 255}
   :top-border-2 {:r 223 :g 110 :b 44 :a 255}
   :top-border-3 {:r 225 :g 138 :b 50 :a 255}
   :top-border-4 {:r 236 :g 189 :b 80 :a 255}
   :top-border-5 {:r 240 :g 213 :b 137 :a 255}
   ;; Blob border colors
   :blob-border-1 {:r 168 :g 66 :b 28 :a 255}
   :blob-border-2 {:r 203 :g 100 :b 44 :a 255}
   :blob-border-3 {:r 225 :g 138 :b 50 :a 255}
   :blob-border-4 {:r 236 :g 159 :b 70 :a 255}
   :blob-border-5 {:r 240 :g 189 :b 100 :a 255}
   ;; Text colors
   :text-dark {:r 61 :g 26 :b 5 :a 255}})

;; ============================================================================
;; STATE
;; ============================================================================

(defonce state
  (atom {:canvas nil
         :ctx nil
         :renderer nil
         :render-commands []
         :scroll-y 0
         :animation-lerp 0
         :last-time 0
         :pointer {:x 0 :y 0 :down false}
         :active-renderer 0
         :debug-mode false}))

;; ============================================================================
;; TEXT MEASUREMENT
;; ============================================================================

(defn create-measure-fn [ctx fonts]
  (fn [text-content config]
    (let [font-str (renderer/build-font-string fonts config)]
      (set! (.-font ctx) font-str)
      (let [metrics (.measureText ctx text-content)
            width (.-width metrics)
            font-size (:font-size config 16)
            ascent (or (.-fontBoundingBoxAscent metrics) (* 0.8 font-size))
            descent (or (.-fontBoundingBoxDescent metrics) (* 0.2 font-size))
            height (+ ascent descent)
            word-data (text/measure-words text-content config
                        (fn [t _c]
                          (let [m (.measureText ctx t)]
                            {:width (.-width m) :height height})))]
        {:width width
         :height height
         :min-width (:min-width word-data)
         :words (:words word-data)}))))

;; ============================================================================
;; COLOR HELPERS
;; ============================================================================

(defn color-lerp [a b amount]
  {:r (+ (:r a) (* (- (:r b) (:r a)) amount))
   :g (+ (:g a) (* (- (:g b) (:g a)) amount))
   :b (+ (:b a) (* (- (:b b) (:b a)) amount))
   :a (+ (:a a) (* (- (:a b) (:a a)) amount))})

;; ============================================================================
;; COMPONENTS using Rheon DSL
;; ============================================================================

(defn landing-blob
  "Feature blob with border and checkmark."
  [state text-content border-color measure-fn]
  (-> state
      ;; Outer blob container
      (ui/row {:width [:grow {:max 480}]
               :height :fit
               :padding 16
               :gap 16
               :align [:left :center]
               :border {:color border-color :width 2}
               :radius 10})
      ;; Checkmark box
      (ui/box {:size [32 32]
               :bg border-color
               :radius 4})
      (ui/close)
      ;; Text
      (ui/text text-content {:font-size 24 :color border-color} measure-fn)
      (ui/close)))

(defn header-link [state text-content measure-fn]
  (-> state
      (ui/box {:size :fit :padding 8})
      (ui/text text-content {:font-size 24 :color (:text-dark colors)} measure-fn)
      (ui/close)))

(defn header-button [state text-content measure-fn]
  (-> state
      (ui/box {:size :fit
               :padding [6 16]
               :bg (:light colors)
               :border {:color (:red colors) :width 2}
               :radius 10})
      (ui/text text-content {:font-size 24 :color (:text-dark colors)} measure-fn)
      (ui/close)))

;; ============================================================================
;; PAGE SECTIONS
;; ============================================================================

(defn header-bar [state measure-fn mobile?]
  (-> state
      ;; Header row
      (ui/row {:width :grow
               :height 50
               :padding [0 32]
               :gap 16
               :align [:left :center]})
      ;; Logo
      (ui/text "Clay" {:font-size 24 :color (:text-dark colors)} measure-fn)
      ;; Spacer
      (ui/spacer)
      ;; Links (desktop only)
      (as-> s (if mobile? s
                (-> s
                    (header-link "Examples" measure-fn)
                    (header-link "Docs" measure-fn))))
      ;; Buttons
      (header-button "Discord" measure-fn)
      (header-button "Github" measure-fn)
      (ui/close)))

(defn top-border-stripe [state]
  (reduce
   (fn [s color-key]
     (-> s
         (ui/box {:width :grow :height 4 :bg (get colors color-key)})
         (ui/close)))
   state
   [:top-border-5 :top-border-4 :top-border-3 :top-border-2 :top-border-1]))

(defn landing-page-desktop [state measure-fn vh]
  (-> state
      ;; Outer container
      (ui/box {:width :grow
               :height [:fit {:min (- vh 70)}]
               :padding 50
               :align [:left :center]})
      ;; Inner with red borders
      (ui/row {:size :grow
               :padding 32
               :gap 32
               :align [:left :center]
               :border {:color (:red colors) :width {:left 2 :right 2 :top 0 :bottom 0}}})
      ;; Left text column (55%)
      (ui/col {:width [:% 55]
               :height :fit
               :gap 8})
      (ui/text "Clay is a flex-box style UI auto layout library in C, with declarative syntax and microsecond performance."
               {:font-size 56 :color (:red colors)} measure-fn)
      ;; Spacer
      (ui/fixed-spacer [0 32])
      (ui/text "Clay is laying out this webpage right now!"
               {:font-size 36 :color (:orange colors)} measure-fn)
      (ui/close) ;; left col
      ;; Right blobs column (45%)
      (ui/col {:width [:% 45]
               :height :fit
               :gap 16
               :align [:center :top]})
      (landing-blob "High performance" (:blob-border-5 colors) measure-fn)
      (landing-blob "Flexbox-style responsive layout" (:blob-border-4 colors) measure-fn)
      (landing-blob "Declarative syntax" (:blob-border-3 colors) measure-fn)
      (landing-blob "Single .h file for C/C++" (:blob-border-2 colors) measure-fn)
      (landing-blob "Compile to 15kb .wasm" (:blob-border-1 colors) measure-fn)
      (ui/close) ;; right col
      (ui/close) ;; inner
      (ui/close))) ;; outer

(defn feature-blocks-desktop [state measure-fn]
  (-> state
      ;; Outer
      (ui/box {:width :grow :height :fit})
      ;; Inner row
      (ui/row {:width :grow
               :height :fit
               :align [:left :center]
               :border {:color (:red colors) :width {:between-children 2}}})
      ;; Left block (50%)
      (ui/col {:width [:% 50]
               :height :fit
               :padding [32 50]
               :gap 8
               :align [:left :center]})
      ;; #include badge
      (ui/box {:size :fit :padding [4 8] :bg (:red colors) :radius 8})
      (ui/text "#include clay.h" {:font-size 24 :color (:light colors)} measure-fn)
      (ui/close)
      (ui/text "~2000 lines of C99." {:font-size 24 :color (:red colors)} measure-fn)
      (ui/text "Zero dependencies, including no C standard library."
               {:font-size 24 :color (:red colors)} measure-fn)
      (ui/close) ;; left block
      ;; Right block (50%)
      (ui/col {:width [:% 50]
               :height :fit
               :padding [32 50]
               :gap 8
               :align [:left :center]})
      (ui/text "Renderer agnostic." {:font-size 24 :color (:orange colors)} measure-fn)
      (ui/text "Layout with clay, then render with Raylib, WebGL Canvas or even as HTML."
               {:font-size 24 :color (:red colors)} measure-fn)
      (ui/text "Flexible output for easy compositing in your custom engine or environment."
               {:font-size 24 :color (:red colors)} measure-fn)
      (ui/close) ;; right block
      (ui/close) ;; inner row
      (ui/close))) ;; outer

(defn declarative-syntax-page [state measure-fn vh]
  (-> state
      ;; Outer
      (ui/box {:width :grow
               :height [:fit {:min (- vh 50)}]
               :padding 50
               :align [:left :center]})
      ;; Inner
      (ui/row {:size :grow
               :padding 32
               :gap 32
               :align [:left :center]
               :border {:color (:red colors) :width {:left 2 :right 2 :top 0 :bottom 0}}})
      ;; Left text (50%)
      (ui/col {:width [:% 50]
               :height :fit
               :gap 8})
      (ui/text "Declarative Syntax" {:font-size 52 :color (:red colors)} measure-fn)
      (ui/fixed-spacer [0 16])
      (ui/text "Flexible and readable declarative syntax with nested UI element hierarchies."
               {:font-size 28 :color (:red colors)} measure-fn)
      (ui/text "Mix elements with standard C code like loops, conditionals and functions."
               {:font-size 28 :color (:red colors)} measure-fn)
      (ui/text "Create your own library of re-usable components from UI primitives like text, images and rectangles."
               {:font-size 28 :color (:red colors)} measure-fn)
      (ui/close) ;; left col
      ;; Right image placeholder (50%)
      (ui/box {:width [:% 50]
               :height :fit
               :align :center})
      (ui/box {:width [:grow {:max 400}]
               :height 350
               :bg {:r 200 :g 200 :b 200 :a 255}
               :radius 8})
      (ui/close)
      (ui/close)
      (ui/close) ;; inner
      (ui/close))) ;; outer

(defn high-performance-page [state measure-fn vh lerp-value]
  (-> state
      ;; Outer (red bg)
      (ui/row {:width :grow
               :height [:fit {:min (- vh 50)}]
               :padding [32 82]
               :gap 64
               :align [:left :center]
               :bg (:red colors)})
      ;; Left text (50%)
      (ui/col {:width [:% 50]
               :height :fit
               :gap 8})
      (ui/text "High Performance" {:font-size 52 :color (:light colors)} measure-fn)
      (ui/fixed-spacer [0 16])
      (ui/text "Fast enough to recompute your entire UI every frame."
               {:font-size 28 :color (:light colors)} measure-fn)
      (ui/text "Small memory footprint (3.5mb default) with static allocation & reuse. No malloc / free."
               {:font-size 28 :color (:light colors)} measure-fn)
      (ui/text "Simplify animations and reactive UI design by avoiding the standard performance hacks."
               {:font-size 28 :color (:light colors)} measure-fn)
      (ui/close) ;; left col
      ;; Right animation (50%)
      (ui/box {:width [:% 50]
               :height :fit
               :align :center})
      ;; Animation container
      (ui/row {:width :grow
               :height 400
               :border {:color (:light colors) :width 2}})
      ;; Left animated box
      (ui/box {:width [:% (+ 0.3 (* 0.4 (Math/abs lerp-value)))]
               :height :grow
               :padding 32
               :align [:left :center]
               :bg (color-lerp (:red colors) (:orange colors) (Math/abs lerp-value))})
      (ui/text "Lorem ipsum dolor sit amet, consectetur adipiscing elit."
               {:font-size 24 :color (:light colors)} measure-fn)
      (ui/close)
      ;; Right animated box
      (ui/box {:width :grow
               :height :grow
               :padding 32
               :align [:left :center]
               :bg (color-lerp (:orange colors) (:red colors) (Math/abs lerp-value))})
      (ui/text "Lorem ipsum dolor sit amet, consectetur adipiscing elit."
               {:font-size 24 :color (:light colors)} measure-fn)
      (ui/close)
      (ui/close) ;; animation container
      (ui/close) ;; right box
      (ui/close))) ;; outer

(defn renderer-page [state measure-fn vh active-renderer]
  (-> state
      ;; Outer
      (ui/box {:width :grow
               :height [:fit {:min (- vh 50)}]
               :padding 50
               :align [:left :center]})
      ;; Inner
      (ui/row {:size :grow
               :padding 32
               :gap 32
               :align [:left :center]
               :border {:color (:red colors) :width {:left 2 :right 2 :top 0 :bottom 0}}})
      ;; Left text (50%)
      (ui/col {:width [:% 50]
               :height :fit
               :gap 8})
      (ui/text "Renderer & Platform Agnostic" {:font-size 52 :color (:red colors)} measure-fn)
      (ui/fixed-spacer [0 16])
      (ui/text "Clay outputs a sorted array of primitive render commands, such as RECTANGLE, TEXT or IMAGE."
               {:font-size 28 :color (:red colors)} measure-fn)
      (ui/text "Write your own renderer in a few hundred lines of code, or use the provided examples for Raylib, WebGL canvas and more."
               {:font-size 28 :color (:red colors)} measure-fn)
      (ui/text "There's even an HTML renderer - you're looking at it right now!"
               {:font-size 28 :color (:red colors)} measure-fn)
      (ui/close) ;; left col
      ;; Right buttons (50%)
      (ui/col {:width [:% 50]
               :height :fit
               :gap 16
               :align [:center :top]})
      (ui/text "Try changing renderer!" {:font-size 36 :color (:orange colors)} measure-fn)
      (ui/fixed-spacer [0 32])
      ;; HTML Renderer button
      (ui/box {:width 300
               :height :fit
               :padding 16
               :bg (if (= active-renderer 0) (:red colors) (:light colors))
               :border {:color (:red colors)
                        :width (if (= active-renderer 0) 0 2)}
               :radius 10})
      (ui/text "HTML Renderer"
               {:font-size 28 :color (if (= active-renderer 0) (:light colors) (:red colors))}
               measure-fn)
      (ui/close)
      ;; Canvas Renderer button
      (ui/box {:width 300
               :height :fit
               :padding 16
               :bg (if (= active-renderer 1) (:red colors) (:light colors))
               :border {:color (:red colors)
                        :width (if (= active-renderer 1) 0 2)}
               :radius 10})
      (ui/text "Canvas Renderer"
               {:font-size 28 :color (if (= active-renderer 1) (:light colors) (:red colors))}
               measure-fn)
      (ui/close)
      (ui/close) ;; right col
      (ui/close) ;; inner
      (ui/close))) ;; outer

(defn debugger-page [state measure-fn vh]
  (-> state
      ;; Outer (red bg)
      (ui/row {:width :grow
               :height [:fit {:min (- vh 50)}]
               :padding [32 82]
               :gap 64
               :align [:left :center]
               :bg (:red colors)})
      ;; Left text (50%)
      (ui/col {:width [:% 50]
               :height :fit
               :gap 8})
      (ui/text "Integrated Debug Tools" {:font-size 52 :color (:light colors)} measure-fn)
      (ui/fixed-spacer [0 16])
      (ui/text "Clay includes built in \"Chrome Inspector\"-style debug tooling."
               {:font-size 28 :color (:light colors)} measure-fn)
      (ui/text "View your layout hierarchy and config in real time."
               {:font-size 28 :color (:light colors)} measure-fn)
      (ui/fixed-spacer [0 32])
      (ui/text "Press the \"d\" key to try it out now!"
               {:font-size 32 :color (:orange colors)} measure-fn)
      (ui/close) ;; left col
      ;; Right image placeholder (50%)
      (ui/box {:width [:% 50]
               :height :fit
               :align :center})
      (ui/box {:width [:grow {:max 400}]
               :height 350
               :bg {:r 180 :g 180 :b 180 :a 255}
               :radius 8})
      (ui/close)
      (ui/close)
      (ui/close))) ;; outer

;; ============================================================================
;; MAIN LAYOUT
;; ============================================================================

(defn create-clay-website-layout [viewport measure-fn lerp-value active-renderer]
  (let [mobile? (< (:width viewport) 750)
        vh (:height viewport)]
    (-> (ui/begin-layout viewport)
        ;; Root container
        (ui/col {:size :grow :bg (:light colors)})
        ;; Header
        (header-bar measure-fn mobile?)
        ;; Top border stripe
        (top-border-stripe)
        ;; Scroll container
        (ui/scroll-box {:size :grow
                        :scroll-direction :vertical
                        :border {:color (:red colors) :width {:between-children 2}}})
        ;; Page sections
        (landing-page-desktop measure-fn vh)
        (feature-blocks-desktop measure-fn)
        (declarative-syntax-page measure-fn vh)
        (high-performance-page measure-fn vh lerp-value)
        (renderer-page measure-fn vh active-renderer)
        (debugger-page measure-fn vh)
        (ui/close) ;; scroll container
        (ui/close) ;; root
        (ui/end-layout measure-fn))))

;; ============================================================================
;; RENDERING
;; ============================================================================

(defn render-to-canvas! []
  (when-let [r (:renderer @state)]
    (let [commands (:render-commands @state)]
      ;; Debug: log first few commands
      (when (zero? (mod (int (:animation-lerp @state 0)) 1))
        (js/console.log "First 3 commands:" (clj->js (take 3 commands))))
      (renderer/render! r commands))))

;; ============================================================================
;; INITIALIZATION
;; ============================================================================

(defn setup-canvas! []
  (let [canvas (js/document.getElementById "canvas")]
    (if canvas
      (do
        (set! (.-width canvas) (.-innerWidth js/window))
        (set! (.-height canvas) (.-innerHeight js/window))
        (let [ctx (.getContext canvas "2d")
              r (renderer/create-renderer canvas)]
          (swap! state assoc :canvas canvas :ctx ctx :renderer r)
          (js/console.log "Canvas initialized:" (.-width canvas) "x" (.-height canvas))
          true))
      (do
        (js/console.warn "No canvas element found")
        false))))

(defn run-layout! []
  (when-let [ctx (:ctx @state)]
    (let [viewport {:width (.-innerWidth js/window)
                    :height (.-innerHeight js/window)}
          measure-fn (create-measure-fn ctx renderer/default-fonts)
          lerp-value (:animation-lerp @state)
          active-renderer (:active-renderer @state)
          result (create-clay-website-layout viewport measure-fn lerp-value active-renderer)
          commands (ui/get-render-commands result)]
      (swap! state assoc :render-commands commands)
      (js/console.log "Layout computed:" (count commands) "render commands")
      commands)))

(defn animation-frame! [timestamp]
  (let [last-time (:last-time @state)
        delta-time (if (zero? last-time) 0 (/ (- timestamp last-time) 1000))
        current-lerp (:animation-lerp @state)
        new-lerp (let [l (+ current-lerp delta-time)]
                   (if (> l 1) (- l 2) l))]
    (swap! state assoc
           :last-time timestamp
           :animation-lerp new-lerp)
    (run-layout!)
    (render-to-canvas!)
    (js/requestAnimationFrame animation-frame!)))

(defn ^:export init! []
  (js/console.log "Clay Website Test - Rheon DSL")

  (if (setup-canvas!)
    (do
      (run-layout!)
      (render-to-canvas!)

      ;; Start animation loop
      (js/requestAnimationFrame animation-frame!)

      ;; Handle resize
      (.addEventListener js/window "resize"
        (fn []
          (setup-canvas!)
          (run-layout!)
          (render-to-canvas!)))

      ;; Handle keyboard
      (.addEventListener js/window "keydown"
        (fn [e]
          (when (= (.-key e) "d")
            (swap! state update :debug-mode not)
            (js/console.log "Debug mode:" (:debug-mode @state))))))
    (js/console.log "No canvas found")))

(defn ^:export reload! []
  (js/console.log "Hot reloading...")
  (when (:canvas @state)
    (run-layout!)
    (render-to-canvas!)))
