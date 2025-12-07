(ns clay.website-html-test
  "Clay Website Test - HTML Renderer version for easy debugging"
  (:require [clay.dsl :as ui]
            [clay.layout.text :as text]
            [clay.renderer.html :as renderer]))

;; ============================================================================
;; COLORS
;; ============================================================================

(def colors
  {:light {:r 244 :g 235 :b 230 :a 255}
   :red {:r 168 :g 66 :b 28 :a 255}
   :orange {:r 225 :g 138 :b 50 :a 255}
   :text-dark {:r 61 :g 26 :b 5 :a 255}
   :blob-border-1 {:r 168 :g 66 :b 28 :a 255}
   :blob-border-2 {:r 203 :g 100 :b 44 :a 255}
   :blob-border-3 {:r 225 :g 138 :b 50 :a 255}})

;; ============================================================================
;; STATE
;; ============================================================================

(defonce state (atom {:root nil :renderer nil}))

;; ============================================================================
;; SIMPLE LAYOUT for debugging
;; ============================================================================

(defn create-test-layout [viewport measure-fn]
  (-> (ui/begin-layout viewport)
      ;; Root container with background
      (ui/col {:size :grow :bg (:light colors) :padding 20 :gap 16})

      ;; Header
      (ui/row {:width :grow :height 50 :bg (:red colors) :padding [0 16] :align [:left :center]})
      (ui/text "Clay Website - HTML Renderer Test" {:font-size 24 :color {:r 255 :g 255 :b 255 :a 255}} measure-fn)
      (ui/close)

      ;; Content row
      (ui/row {:width :grow :height :fit :gap 16})

      ;; Left column
      (ui/col {:width [:% 50] :height :fit :gap 8 :padding 16 :bg {:r 255 :g 255 :b 255 :a 255} :radius 8})
      (ui/text "Left Column" {:font-size 20 :color (:text-dark colors)} measure-fn)
      (ui/text "This is some text content that should wrap properly within the container."
               {:font-size 16 :color (:text-dark colors)} measure-fn)
      (ui/close)

      ;; Right column
      (ui/col {:width [:% 50] :height :fit :gap 8 :padding 16 :bg {:r 255 :g 255 :b 255 :a 255} :radius 8})
      (ui/text "Right Column" {:font-size 20 :color (:text-dark colors)} measure-fn)
      (ui/text "Another block of text to test the layout system."
               {:font-size 16 :color (:text-dark colors)} measure-fn)
      (ui/close)

      (ui/close) ;; content row

      ;; Feature boxes
      (ui/row {:width :grow :height :fit :gap 8})
      (ui/box {:width :grow :height 100 :bg (:blob-border-1 colors) :radius 8 :padding 16})
      (ui/text "Box 1" {:font-size 16 :color {:r 255 :g 255 :b 255 :a 255}} measure-fn)
      (ui/close)
      (ui/box {:width :grow :height 100 :bg (:blob-border-2 colors) :radius 8 :padding 16})
      (ui/text "Box 2" {:font-size 16 :color {:r 255 :g 255 :b 255 :a 255}} measure-fn)
      (ui/close)
      (ui/box {:width :grow :height 100 :bg (:blob-border-3 colors) :radius 8 :padding 16})
      (ui/text "Box 3" {:font-size 16 :color {:r 255 :g 255 :b 255 :a 255}} measure-fn)
      (ui/close)
      (ui/close) ;; feature row

      (ui/close) ;; root
      (ui/end-layout measure-fn)))

;; ============================================================================
;; INIT
;; ============================================================================

(defn ^:export init! []
  (js/console.log "Clay HTML Renderer Test - Starting")

  (let [root (js/document.getElementById "root")]
    (if root
      (do
        ;; Setup root styles
        (set! (.. root -style -position) "relative")
        (set! (.. root -style -width) "100%")
        (set! (.. root -style -height) "100%")
        (set! (.. root -style -overflow) "hidden")

        (let [r (renderer/create-renderer root)
              measure-fn (renderer/create-measure-fn)
              viewport {:width (.-innerWidth js/window)
                        :height (.-innerHeight js/window)}
              _ (js/console.log "Viewport:" (clj->js viewport))
              result (create-test-layout viewport measure-fn)
              commands (ui/get-render-commands result)]

          (js/console.log "Layout complete:" (count commands) "commands")
          (js/console.log "First 5 commands:" (clj->js (take 5 commands)))

          (swap! state assoc :root root :renderer r)

          ;; Render!
          (renderer/render! r commands)
          (js/console.log "Render complete!")))

      (js/console.error "No root element found!"))))

(defn ^:export reload! []
  (js/console.log "Hot reload...")
  (when-let [root (:root @state)]
    (renderer/clear! (:renderer @state))
    (let [measure-fn (renderer/create-measure-fn)
          viewport {:width (.-innerWidth js/window)
                    :height (.-innerHeight js/window)}
          result (create-test-layout viewport measure-fn)
          commands (ui/get-render-commands result)]
      (renderer/render! (:renderer @state) commands))))
