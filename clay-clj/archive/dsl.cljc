(ns clay.dsl
  "High-level DSL for building Clay layouts.

   This namespace provides a Rheon-style DSL that normalizes shorthand props
   and builds layouts using clay.layout.core.

   DSL Shorthand Examples:
   - :size [:grow :fit]        → {:width {:type :grow} :height {:type :fit}}
   - :width [:% 55]            → {:width {:type :percent :value 0.55}}
   - :padding 16               → {:top 16 :right 16 :bottom 16 :left 16}
   - :gap 8                    → {:child-gap 8}
   - :bg :red-500              → resolved color
   - :direction :horizontal    → {:layout-direction :left-to-right}
   - :align :center            → {:x :center :y :center}

   Usage:
   (-> (begin-layout viewport)
       (box {:size :grow :padding 16 :gap 8 :bg :dark}
         (text \"Hello\" {:font-size 24 :color :white}))
       (end-layout measure-fn))"
  (:require [clay.layout.core :as layout]
            [clay.dsl.normalize :as normalize]))

;; ============================================================================
;; NORMALIZATION
;; ============================================================================

(defn- normalize-layout-config
  "Normalize DSL props to layout config format."
  [props]
  (let [normalized (normalize/normalize-props props)
        ;; Map DSL keys to layout config keys
        layout-direction (case (:direction props)
                           :horizontal :left-to-right
                           :vertical :top-to-bottom
                           :row :left-to-right
                           :col :top-to-bottom
                           :column :top-to-bottom
                           (:layout-direction props)
                           (:direction normalized)
                           nil)]
    (cond-> {}
      ;; Sizing
      (:size normalized)
      (assoc :sizing (:size normalized))

      (:width normalized)
      (assoc-in [:sizing :width] (:width normalized))

      (:height normalized)
      (assoc-in [:sizing :height] (:height normalized))

      ;; Padding
      (or (:padding normalized) (:pad normalized))
      (assoc :padding (or (:padding normalized) (:pad normalized)))

      ;; Gap
      (or (:gap normalized) (:child-gap normalized))
      (assoc :child-gap (or (:gap normalized) (:child-gap normalized)))

      ;; Layout direction
      layout-direction
      (assoc :layout-direction layout-direction)

      ;; Child alignment
      (:align normalized)
      (assoc :child-alignment (:align normalized)))))

(defn- extract-configs
  "Extract configuration types (bg, border, etc.) from props."
  [props]
  (let [normalized (normalize/normalize-props props)]
    (cond-> []
      ;; Background
      (or (:bg normalized) (:background normalized))
      (conj {:type :background
             :config {:color (or (:bg normalized) (:background normalized))
                      :corner-radius (or (:radius normalized) (:corner-radius normalized)
                                         {:top-left 0 :top-right 0
                                          :bottom-left 0 :bottom-right 0})}})

      ;; Border
      (:border normalized)
      (conj {:type :border :config (:border normalized)})

      ;; Clip/Scroll
      (:scroll normalized)
      (conj {:type :clip :config {:vertical (= (:direction (:scroll normalized)) :vertical)
                                   :horizontal (= (:direction (:scroll normalized)) :horizontal)}})

      ;; Floating
      (:floating normalized)
      (conj {:type :floating :config (:floating normalized)}))))

;; ============================================================================
;; LAYOUT FUNCTIONS
;; ============================================================================

(defn begin-layout
  "Initialize layout state for a new frame.

   Parameters:
   - viewport: {:width number :height number}

   Returns layout state map."
  [viewport]
  (layout/begin-layout viewport))

(defn end-layout
  "Complete the layout calculation and generate render commands.

   Parameters:
   - state: Layout state
   - measure-fn: Text measurement function

   Returns state with :render-commands populated."
  ([state] (layout/end-layout state))
  ([state measure-fn] (layout/end-layout state measure-fn))
  ([state measure-fn scroll-positions] (layout/end-layout state measure-fn scroll-positions)))

(defn get-render-commands
  "Extract render commands from completed layout state."
  [state]
  (layout/get-render-commands state))

;; ============================================================================
;; ELEMENT DSL
;; ============================================================================

(defn box
  "Create a container element with DSL shorthand.

   Props shorthand:
   - :size [:grow :fit] or :grow or [100 200]
   - :width :grow or 100 or [:% 55] or [:grow 0 500]
   - :height :fit or [:fixed 100]
   - :padding 16 or [16 8] or [16 8 16 8]
   - :gap 8
   - :direction :horizontal or :vertical
   - :align :center or [:left :top] or {:x :center :y :top}
   - :bg :red-500 or {:r 255 :g 0 :b 0 :a 255}
   - :border {:color :red :width 2 :radius 8}
   - :radius 8 or [8 8 0 0]

   Usage:
   (-> state
       (box {:size :grow :padding 16 :bg :dark})
       close)

   Or with body:
   (box state {:size :grow}
     (fn [s]
       (-> s
           (text \"Hello\" {:font-size 24})
           close)))"
  ([state props]
   (let [layout-config (normalize-layout-config props)
         configs (extract-configs props)]
     (reduce
      (fn [s {:keys [type config]}]
        (layout/configure-element s type config))
      (layout/open-element state layout-config)
      configs)))
  ([state props body-fn]
   (-> state
       (box props)
       body-fn
       layout/close-element)))

(defn text
  "Create a text element.

   Props:
   - :font-id 0
   - :font-size 16
   - :color :white or {:r 255 :g 255 :b 255 :a 255}
   - :line-height 24
   - :letter-spacing 0
   - :wrap :words or :none

   Usage:
   (text state \"Hello World\" {:font-size 24 :color :white} measure-fn)"
  [state content props measure-fn]
  (let [normalized (normalize/normalize-props props)
        text-config {:font-id (or (:font-id normalized) (:font normalized) 0)
                     :font-size (or (:font-size normalized) 16)
                     :text-color (or (:text-color normalized) (:color normalized))
                     :letter-spacing (:letter-spacing normalized)
                     :line-height (:line-height normalized)
                     :wrap-mode (or (:text-wrap normalized) (:wrap normalized))}
        measured (measure-fn content text-config)]
    (layout/open-text-element state content text-config measured)))

(defn close
  "Close the current element."
  [state]
  (layout/close-element state))

;; ============================================================================
;; CONVENIENCE MACROS / HELPERS
;; ============================================================================

(defn row
  "Horizontal container shorthand.

   Equivalent to (box state (merge {:direction :horizontal} props))"
  ([state] (row state {}))
  ([state props]
   (box state (merge {:direction :horizontal} props)))
  ([state props body-fn]
   (box state (merge {:direction :horizontal} props) body-fn)))

(defn col
  "Vertical container shorthand.

   Equivalent to (box state (merge {:direction :vertical} props))"
  ([state] (col state {}))
  ([state props]
   (box state (merge {:direction :vertical} props)))
  ([state props body-fn]
   (box state (merge {:direction :vertical} props) body-fn)))

(defn spacer
  "Flexible spacer element that grows to fill space."
  ([state] (spacer state {}))
  ([state props]
   (-> state
       (box (merge {:size :grow} props))
       close)))

(defn fixed-spacer
  "Fixed-size spacer element."
  [state size]
  (-> state
      (box {:size size})
      close))

;; ============================================================================
;; SCROLL CONTAINERS
;; ============================================================================

(defn scroll-box
  "Scrollable container.

   Props:
   - :direction :vertical or :horizontal (default :vertical)
   - All other box props

   Usage:
   (scroll-box state {:size :grow :direction :vertical}
     (fn [s] (-> s (text \"Content...\" {...} measure-fn) close)))"
  ([state props]
   (let [scroll-direction (or (:scroll-direction props) :vertical)]
     (-> state
         (box (dissoc props :scroll-direction))
         (layout/configure-element :clip {:vertical (= scroll-direction :vertical)
                                          :horizontal (= scroll-direction :horizontal)}))))
  ([state props body-fn]
   (-> state
       (scroll-box props)
       body-fn
       close)))

;; ============================================================================
;; LAYOUT UTILITIES
;; ============================================================================

(defn with-border
  "Add border to current element."
  [state border-props]
  (let [normalized (normalize/normalize :border border-props)]
    (layout/configure-element state :border normalized)))

(defn with-background
  "Add background to current element."
  [state bg-props]
  (let [color (if (map? bg-props)
                (or (:color bg-props) bg-props)
                (normalize/normalize :color bg-props))
        radius (when (map? bg-props)
                 (normalize/normalize :radius (:radius bg-props)))]
    (layout/configure-element state :background
                              {:color color
                               :corner-radius (or radius {:top-left 0 :top-right 0
                                                           :bottom-left 0 :bottom-right 0})})))

(defn with-clip
  "Add clipping to current element for scroll container."
  [state direction]
  (layout/configure-element state :clip
                            {:vertical (or (= direction :vertical) (= direction :both))
                             :horizontal (or (= direction :horizontal) (= direction :both))}))
