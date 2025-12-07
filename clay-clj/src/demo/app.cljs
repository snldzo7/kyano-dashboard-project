(ns demo.app
  "Immediate Mode UI - Debug version (no RAF loop)."
  (:require [demo.ui :as ui]
            [clay.hiccup2 :as hiccup]
            [clay.layout :as layout]
            [clay.renderer.html :as html-renderer]
            [clojure.string :as str]))

;; ============================================================================
;; STATE
;; ============================================================================

(defonce state
  (atom {:viewport {:width 800 :height 600}
         :mouse {:x 0 :y 0}}))

;; ============================================================================
;; RENDERER
;; ============================================================================

(defonce renderer (atom nil))

;; ============================================================================
;; TEXT MEASUREMENT
;; ============================================================================

(defn create-measure-fn []
  (let [canvas (js/document.createElement "canvas")
        ctx (.getContext canvas "2d")]
    (fn [text {:keys [font-size font-family]
               :or {font-size 16 font-family "Inter, system-ui, sans-serif"}}]
      (set! (.-font ctx) (str font-size "px " font-family))
      (let [metrics (.measureText ctx (str text))
            width (.-width metrics)
            words (str/split (str text) #"\s+")]
        {:width width
         :height (* font-size 1.2)
         :words (mapv (fn [word]
                        (let [word-text (str word " ")
                              m (.measureText ctx word-text)]
                          {:text word-text :width (.-width m)}))
                      words)}))))

(defonce measure-fn (create-measure-fn))

;; ============================================================================
;; FRAME
;; ============================================================================

(defn frame!
  "Render one frame with debug output."
  []
  (when @renderer
    (let [s @state
          vp (:viewport s)
          _ (js/console.log "[clay] Viewport:" (clj->js vp))

          ;; 1. UI declaration
          hiccup-data (ui/video-demo-layout s)
          _ (js/console.log "[clay] Hiccup:" (clj->js hiccup-data))

          ;; 2. Parse to tree
          tree (hiccup/parse vp hiccup-data measure-fn)
          _ (js/console.log "[clay] Tree root bounding-box:" (clj->js (:bounding-box tree)))
          _ (js/console.log "[clay] Tree root children count:" (count (:children tree)))

          ;; 3. Layout
          t0 (js/performance.now)
          commands (layout/layout tree {:measure-fn measure-fn})
          t1 (js/performance.now)
          _ (js/console.log "[clay] Layout took:" (.toFixed (- t1 t0) 2) "ms")
          _ (js/console.log "[clay] Commands count:" (count commands))
          _ (doseq [cmd commands]
              (let [bb (:bounding-box cmd)
                    id-str (if (keyword? (:id cmd)) (name (:id cmd)) (str (:id cmd)))
                    rd (:render-data cmd)]
                (js/console.log "[cmd]" (name (:command-type cmd)) id-str
                               "x:" (:x bb) "y:" (:y bb) "w:" (:width bb) "h:" (:height bb)
                               "color:" (clj->js (:color rd)))))]

      ;; 4. Render
      (html-renderer/render! @renderer commands))))

;; ============================================================================
;; INPUT
;; ============================================================================

(defn on-resize []
  (swap! state assoc :viewport
         {:width js/window.innerWidth
          :height js/window.innerHeight})
  (frame!))

(defn on-click [_e]
  (js/console.log "[clay] Click - re-rendering")
  (frame!))

;; ============================================================================
;; SETUP
;; ============================================================================

(defn setup! [target]
  (js/console.log "[clay] Initializing...")

  ;; Initialize renderer
  (reset! renderer (html-renderer/create-renderer target))

  ;; Set initial viewport
  (swap! state assoc :viewport
         {:width js/window.innerWidth
          :height js/window.innerHeight})

  ;; Input handlers
  (.addEventListener js/window "resize" on-resize)
  (.addEventListener target "click" on-click)

  ;; Initial render
  (frame!)

  (js/console.log "[clay] Ready - click to re-render"))

;; ============================================================================
;; ENTRY
;; ============================================================================

(defn ^:export init []
  (if-let [target (js/document.getElementById "app")]
    (setup! target)
    (js/console.error "[clay] No #app element!")))

(defn ^:dev/after-load reload! []
  (js/console.log "[clay] Hot reload")
  (frame!))
