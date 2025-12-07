(ns clay.layout.aspect-ratio
  "Aspect ratio constraint system - faithful port of Clay.h aspect ratio handling.

   In Clay.h, aspect ratio works as follows:
   1. Config: aspectRatio float = width / height
   2. Clay__UpdateAspectRatioBox (line 1797-1813):
      - If width=0 and height set: width = height * aspectRatio
      - If height=0 and width set: height = width * (1/aspectRatio)
   3. After X-axis sizing (line 2591-2597):
      - height = (1 / aspectRatio) * width
      - Set max height to constrain Y-axis sizing
   4. After Y-axis sizing (line 2651-2654):
      - width = aspectRatio * height

   This module provides:
   - aspect-ratio-config? : Check if element has aspect ratio config
   - get-aspect-ratio : Get the aspect ratio value
   - update-aspect-ratio-box : Initial dimension calculation
   - apply-aspect-height : After X-axis, calculate height from width
   - apply-aspect-width : After Y-axis, calculate width from height")

;; ============================================================================
;; ASPECT RATIO CONFIGURATION
;; ============================================================================

(defn aspect-ratio-config?
  "Check if an element has aspect ratio configuration."
  [element]
  (some #(= :aspect-ratio (:type %)) (:configs element)))

(defn get-aspect-ratio
  "Get the aspect ratio value (width/height) for an element.
   Returns nil if no aspect ratio config."
  [element]
  (some (fn [cfg]
          (when (= :aspect-ratio (:type cfg))
            (get-in cfg [:config :ratio])))
        (:configs element)))

;; ============================================================================
;; UPDATE ASPECT RATIO BOX
;; Port of Clay__UpdateAspectRatioBox (lines 1797-1813)
;; Called when opening an element
;; ============================================================================

(defn update-aspect-ratio-box
  "Update element dimensions based on aspect ratio.
   Port of Clay__UpdateAspectRatioBox.

   If width=0 and height set: width = height * aspectRatio
   If height=0 and width set: height = width * (1/aspectRatio)"
  [element]
  (if-let [ratio (get-aspect-ratio element)]
    (if (zero? ratio)
      element
      (let [width (get-in element [:dimensions :width] 0)
            height (get-in element [:dimensions :height] 0)]
        (cond
          ;; Width is 0, height is set -> calculate width
          (and (zero? width) (pos? height))
          (assoc-in element [:dimensions :width] (* height ratio))

          ;; Height is 0, width is set -> calculate height
          (and (pos? width) (zero? height))
          (assoc-in element [:dimensions :height] (/ width ratio))

          ;; Both set or both zero -> no change
          :else element)))
    element))

;; ============================================================================
;; APPLY ASPECT HEIGHT (After X-axis sizing)
;; Port of lines 2591-2597
;; ============================================================================

(defn apply-aspect-height
  "After X-axis sizing, calculate height from width.
   height = (1 / aspectRatio) * width

   Only applies when width is set (> 0). Sets both min and max to lock
   the height during Y-axis sizing so :fit doesn't override it."
  [element]
  (if-let [ratio (get-aspect-ratio element)]
    (let [width (get-in element [:dimensions :width] 0)]
      (if (or (zero? ratio) (zero? width))
        element  ; Skip if no ratio or width not yet determined
        (let [new-height (double (/ width ratio))]
          (-> element
              (assoc-in [:dimensions :height] new-height)
              ;; Set BOTH min and max to lock the height during Y-axis sizing
              ;; This prevents :fit from overriding with children content size
              (assoc-in [:layout :sizing :height :min] new-height)
              (assoc-in [:layout :sizing :height :max] new-height)))))
    element))

;; ============================================================================
;; APPLY ASPECT WIDTH (After Y-axis sizing)
;; Port of lines 2651-2654
;; ============================================================================

(defn apply-aspect-width
  "After Y-axis sizing, calculate width from height.
   width = aspectRatio * height

   Only applies when height is set (> 0)."
  [element]
  (if-let [ratio (get-aspect-ratio element)]
    (let [height (get-in element [:dimensions :height] 0)]
      (if (or (zero? ratio) (zero? height))
        element  ; Skip if no ratio or height not yet determined
        (let [new-width (double (* ratio height))]
          (assoc-in element [:dimensions :width] new-width))))
    element))

;; ============================================================================
;; STATE-LEVEL FUNCTIONS
;; ============================================================================

(defn- get-element [state idx]
  (get-in state [:layout-elements idx]))

(defn- update-element [state idx f]
  (update-in state [:layout-elements idx] f))

(defn collect-aspect-ratio-elements
  "Collect indices of all elements with aspect ratio config."
  [state]
  (->> (:layout-elements state)
       (map-indexed (fn [idx el] (when (aspect-ratio-config? el) idx)))
       (remove nil?)
       vec))

(defn apply-all-aspect-heights
  "Apply aspect ratio height calculation to all aspect ratio elements.
   Called after X-axis sizing, before Y-axis sizing.
   Port of lines 2591-2597."
  [state]
  (reduce (fn [s idx]
            (update-element s idx apply-aspect-height))
          state
          (collect-aspect-ratio-elements state)))

(defn apply-all-aspect-widths
  "Apply aspect ratio width calculation to all aspect ratio elements.
   Called after Y-axis sizing.
   Port of lines 2651-2654."
  [state]
  (reduce (fn [s idx]
            (update-element s idx apply-aspect-width))
          state
          (collect-aspect-ratio-elements state)))
