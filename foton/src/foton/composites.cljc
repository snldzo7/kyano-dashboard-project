(ns foton.composites
  "Foton composites - Button, Animations, Draggable, Resizable.

   Composites are higher-level components built from primitives.
   Each composite is a render function that returns hiccup.

   Usage:
     (render-button {:variant :primary :on {:click [...]}} [children])
     (render-fade-in {:duration 300} [children])"
  (:require [foton.css :as css]
            [foton.primitives :as p]))

;; =============================================================================
;; Button Variants
;; =============================================================================

(def button-variants
  "Predefined button variant styles.

   Each variant defines:
   - Base styles (fill, shadow, stroke)
   - Vary animations (hovered, pressed states)"
  {:primary   {:base {:fill :primary :shadow :sm}
               :vary {:hovered {:translate-y -2 :shadow :md}
                      :pressed {:translate-y 0 :scale 0.98 :shadow :sm}}}

   :secondary {:base {:fill :surface :stroke {:width 1 :color :border}}
               :vary {:hovered {:fill :elevated :translate-y -1}
                      :pressed {:translate-y 0 :scale 0.98}}}

   :ghost     {:base {:fill :transparent}
               :vary {:hovered {:fill :surface}
                      :pressed {:fill :elevated :scale 0.98}}}

   :outline   {:base {:fill :transparent :stroke {:width 1 :color :primary}}
               :vary {:hovered {:fill :surface [:stroke :width] 2}
                      :pressed {:scale 0.98}}}})

;; =============================================================================
;; Button Sizes
;; =============================================================================

(def button-sizes
  "Button size presets - generous padding for comfortable touch targets."
  {:xs {:padding-v 6  :padding-h 12 :font-size 12}
   :sm {:padding-v 8  :padding-h 16 :font-size 13}
   :md {:padding-v 12 :padding-h 24 :font-size 14}
   :lg {:padding-v 16 :padding-h 32 :font-size 16}
   :xl {:padding-v 20 :padding-h 40 :font-size 18}})

(defn get-variant [variant-key]
  (get button-variants variant-key (:primary button-variants)))

(defn get-size [size-key]
  (get button-sizes size-key (:md button-sizes)))

;; =============================================================================
;; Button
;; =============================================================================

(defn render-button
  "Render a button with Figma-like variant animations.

   Attrs:
   - :variant   :primary | :secondary | :ghost | :outline
   - :size      :xs | :sm | :md | :lg | :xl
   - :fill      Override base fill color
   - :disabled  Boolean - disables interactions
   - :on        Event handlers {:click [...]}

   Returns hiccup via render-frame"
  [attrs children]
  (let [{:keys [on disabled variant size]
         user-fill :fill
         :or {variant :primary size :md}} attrs
        {:keys [fill shadow stroke]} (:base (get-variant variant))
        vary-map (let [base-vary (:vary (get-variant variant))]
                   (if disabled
                     (assoc base-vary :disabled {:opacity 0.5 :cursor :not-allowed})
                     base-vary))
        {:keys [padding-v padding-h font-size]} (get-size size)]
    (p/render-frame
      (cond-> {:fill (or user-fill fill)
               :cursor (if disabled :not-allowed :pointer)
               :radius :md
               :direction :horizontal
               :gap :sm
               :align :center
               :justify :center
               :vary vary-map
               :transition {:duration 180 :easing :ease-out}
               :on (when-not disabled on)
               :style {:user-select "none"
                       :border "none"
                       :outline "none"
                       :padding (str padding-v "px " padding-h "px")
                       :font-size (str font-size "px")
                       :font-weight 500
                       :line-height 1.2}}
        shadow (assoc :shadow shadow)
        stroke (assoc :stroke stroke))
      children)))

;; =============================================================================
;; Animation Helpers
;; =============================================================================

(defn- easing->css [easing]
  (case easing
    :linear "linear"
    :ease "ease"
    :ease-in "ease-in"
    :ease-out "ease-out"
    :ease-in-out "ease-in-out"
    (if (string? easing) easing "ease-out")))

(defn- animation-str
  [{:keys [duration delay easing fill-mode]
    :or {duration 300 delay 0 easing :ease-out fill-mode "forwards"}}
   keyframes-name]
  (str keyframes-name " " duration "ms " (easing->css easing) " " delay "ms " fill-mode))

;; =============================================================================
;; Fade Animations
;; =============================================================================

(defn render-fade-in
  "Fade in animation wrapper.

   Attrs:
   - :duration   Animation duration in ms (default 300)
   - :delay      Animation delay in ms (default 0)
   - :easing     Easing function (default :ease-out)"
  [attrs children]
  (into [:div {:style {:opacity 0
                       :animation (animation-str attrs "foton-fade-in")}}]
        children))

(defn render-fade-out
  "Fade out animation wrapper."
  [attrs children]
  (into [:div {:style {:opacity 1
                       :animation (animation-str attrs "foton-fade-out")}}]
        children))

;; =============================================================================
;; Slide Animations
;; =============================================================================

(defn render-slide-up
  "Slide up animation wrapper.

   Attrs:
   - :distance   Slide distance in px (default 20)
   - :duration   Animation duration in ms
   - :delay      Animation delay in ms
   - :easing     Easing function"
  [attrs children]
  (let [{:keys [distance] :or {distance 20}} attrs]
    (into [:div {:style {:transform (str "translateY(" distance "px)")
                         :animation (animation-str attrs "foton-slide-up")
                         "--foton-slide-distance" (str distance "px")}}]
          children)))

(defn render-slide-down
  "Slide down animation wrapper."
  [attrs children]
  (let [{:keys [distance] :or {distance 20}} attrs]
    (into [:div {:style {:transform (str "translateY(-" distance "px)")
                         :animation (animation-str attrs "foton-slide-down")
                         "--foton-slide-distance" (str distance "px")}}]
          children)))

(defn render-slide-left
  "Slide left animation wrapper."
  [attrs children]
  (let [{:keys [distance] :or {distance 20}} attrs]
    (into [:div {:style {:transform (str "translateX(" distance "px)")
                         :animation (animation-str attrs "foton-slide-left")
                         "--foton-slide-distance" (str distance "px")}}]
          children)))

(defn render-slide-right
  "Slide right animation wrapper."
  [attrs children]
  (let [{:keys [distance] :or {distance 20}} attrs]
    (into [:div {:style {:transform (str "translateX(-" distance "px)")
                         :animation (animation-str attrs "foton-slide-right")
                         "--foton-slide-distance" (str distance "px")}}]
          children)))

;; =============================================================================
;; Scale Animations
;; =============================================================================

(defn render-scale-in
  "Scale in animation wrapper.

   Attrs:
   - :from       Initial scale (default 0.8)"
  [attrs children]
  (let [{:keys [from] :or {from 0.8}} attrs]
    (into [:div {:style {:transform (str "scale(" from ")")
                         :animation (animation-str attrs "foton-scale-in")
                         "--foton-scale-from" (str from)}}]
          children)))

(defn render-scale-out
  "Scale out animation wrapper.

   Attrs:
   - :to         Target scale (default 0.8)"
  [attrs children]
  (let [{:keys [to] :or {to 0.8}} attrs]
    (into [:div {:style {:transform "scale(1)"
                         :animation (animation-str attrs "foton-scale-out")
                         "--foton-scale-to" (str to)}}]
          children)))

;; =============================================================================
;; Interactive Effects (Lift, Sink, Tilt, etc.)
;; =============================================================================
;;
;; All effects support a :trigger option:
;;   :hovered  - activates on mouse hover (default for most)
;;   :pressed  - activates on click/active
;;   :focused  - activates on keyboard focus
;;   :disabled - activates when disabled

(defn render-lift
  "Lift effect - raises element with enhanced shadow.

   Attrs:
   - :trigger    :hovered | :pressed | :focused (default :hovered)
   - :distance   Lift distance in px (default 4)
   - :shadow     Shadow on activation (default :lg)
   - :duration   Transition duration in ms (default 200)"
  [attrs children]
  (let [{:keys [trigger distance shadow duration]
         :or {trigger :hovered distance 4 shadow :lg duration 200}} attrs]
    (p/render-frame
      {:vary {trigger {:translate-y (- distance) :shadow shadow}}
       :transition {:duration duration :easing :ease-out}
       :cursor :pointer}
      children)))

(defn render-sink
  "Sink effect - presses element down with inner shadow.

   Attrs:
   - :trigger    :hovered | :pressed | :focused (default :hovered)
   - :distance   Sink distance in px (default 2)
   - :duration   Transition duration in ms (default 150)"
  [attrs children]
  (let [{:keys [trigger distance duration]
         :or {trigger :hovered distance 2 duration 150}} attrs]
    (p/render-frame
      {:vary {trigger {:translate-y distance :shadow :inner}}
       :transition {:duration duration :easing :ease-out}
       :cursor :pointer}
      children)))

(defn render-grow
  "Grow effect - scales element up.

   Attrs:
   - :trigger    :hovered | :pressed | :focused (default :hovered)
   - :scale      Scale factor (default 1.05)
   - :duration   Transition duration in ms (default 200)"
  [attrs children]
  (let [{:keys [trigger scale duration]
         :or {trigger :hovered scale 1.05 duration 200}} attrs]
    (p/render-frame
      {:vary {trigger {:scale scale}}
       :transition {:duration duration :easing :ease-out}
       :cursor :pointer}
      children)))

(defn render-shrink
  "Shrink effect - scales element down.

   Attrs:
   - :trigger    :hovered | :pressed | :focused (default :hovered)
   - :scale      Scale factor (default 0.95)
   - :duration   Transition duration in ms (default 150)"
  [attrs children]
  (let [{:keys [trigger scale duration]
         :or {trigger :hovered scale 0.95 duration 150}} attrs]
    (p/render-frame
      {:vary {trigger {:scale scale}}
       :transition {:duration duration :easing :ease-out}
       :cursor :pointer}
      children)))

(defn render-tilt
  "Tilt effect - rotates element slightly.

   Attrs:
   - :trigger    :hovered | :pressed | :focused (default :hovered)
   - :angle      Rotation angle in degrees (default 3)
   - :duration   Transition duration in ms (default 200)"
  [attrs children]
  (let [{:keys [trigger angle duration]
         :or {trigger :hovered angle 3 duration 200}} attrs]
    (p/render-frame
      {:vary {trigger {:rotate angle}}
       :transition {:duration duration :easing :spring}
       :cursor :pointer}
      children)))

(defn render-glow
  "Glow effect - adds glowing shadow.

   Attrs:
   - :trigger    :hovered | :pressed | :focused (default :hovered)
   - :intensity  :sm | :md | :lg (default :md)
   - :duration   Transition duration in ms (default 250)"
  [attrs children]
  (let [{:keys [trigger intensity duration]
         :or {trigger :hovered intensity :md duration 250}} attrs
        shadow-key (case intensity
                     :sm :glow
                     :lg :glow-lg
                     :glow)]
    (p/render-frame
      {:vary {trigger {:shadow shadow-key}}
       :transition {:duration duration :easing :ease-out}
       :cursor :pointer}
      children)))

(defn render-pop
  "Pop effect - combines lift + scale for a bouncy pop.

   Attrs:
   - :trigger    :hovered | :pressed | :focused (default :hovered)
   - :distance   Lift distance in px (default 6)
   - :scale      Scale factor (default 1.02)
   - :duration   Transition duration in ms (default 200)"
  [attrs children]
  (let [{:keys [trigger distance scale duration]
         :or {trigger :hovered distance 6 scale 1.02 duration 200}} attrs]
    (p/render-frame
      {:vary {trigger {:translate-y (- distance) :scale scale :shadow :lg}
              :pressed {:translate-y 0 :scale 0.98 :shadow :sm}}
       :transition {:duration duration :easing :spring}
       :cursor :pointer}
      children)))

(defn render-press
  "Press effect - shrinks + sinks for tactile feedback.

   Attrs:
   - :trigger    :hovered | :pressed | :focused (default :pressed)
   - :duration   Transition duration in ms (default 100)"
  [attrs children]
  (let [{:keys [trigger duration]
         :or {trigger :pressed duration 100}} attrs]
    (p/render-frame
      {:vary {trigger {:translate-y 2 :scale 0.97 :shadow :inner}}
       :transition {:duration duration :easing :ease-out}
       :cursor :pointer}
      children)))

;; =============================================================================
;; Draggable
;; =============================================================================

(defn render-draggable
  "Draggable wrapper for drag-and-drop.

   Attrs:
   - :entity-id   Entity ID for drag events
   - :x           X position
   - :y           Y position
   - :dragging?   Boolean - currently dragging"
  [attrs children]
  (let [{:keys [entity-id x y dragging?]} attrs
        style (cond-> {:position "absolute"
                       :cursor (if dragging? "grabbing" "grab")
                       :user-select "none"}
                x (assoc :left (if (number? x) (str x "px") x))
                y (assoc :top (if (number? y) (str y "px") y)))]
    (p/render-frame
      {:style style
       :on (when entity-id
             {:mousedown [[:foton/drag-start entity-id]]})}
      children)))

;; =============================================================================
;; Resizable
;; =============================================================================

(def ^:private handle-size 12)

(defn- handle-style [position]
  (let [base {:position "absolute"
              :width (str handle-size "px")
              :height (str handle-size "px")
              :background "transparent"
              :z-index 10}
        positions {:se {:bottom "0" :right "0" :cursor "se-resize"}
                   :sw {:bottom "0" :left "0" :cursor "sw-resize"}
                   :ne {:top "0" :right "0" :cursor "ne-resize"}
                   :nw {:top "0" :left "0" :cursor "nw-resize"}
                   :e {:top "50%" :right "0" :cursor "e-resize" :transform "translateY(-50%)"}
                   :w {:top "50%" :left "0" :cursor "w-resize" :transform "translateY(-50%)"}
                   :n {:top "0" :left "50%" :cursor "n-resize" :transform "translateX(-50%)"}
                   :s {:bottom "0" :left "50%" :cursor "s-resize" :transform "translateX(-50%)"}}]
    (merge base (get positions position))))

(defn- resize-handle [entity-id position]
  (p/render-frame
    {:style (handle-style position)
     :on {:mousedown [[:foton/resize-start entity-id position]]}}
    []))

(defn render-resizable
  "Resizable wrapper with resize handles.

   Attrs:
   - :entity-id   Entity ID for resize events
   - :width       Current width
   - :height      Current height
   - :handles     Set of handle positions (default #{:se})"
  [attrs children]
  (let [{:keys [entity-id width height handles]
         :or {handles #{:se}}} attrs
        container-style (cond-> {:position "relative" :display "inline-block"}
                          width (assoc :width (if (number? width) (str width "px") width))
                          height (assoc :height (if (number? height) (str height "px") height)))]
    (p/render-frame
      {:style container-style}
      (concat children
              (when entity-id
                (map #(resize-handle entity-id %) handles))))))
