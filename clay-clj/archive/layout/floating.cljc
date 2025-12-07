(ns clay.layout.floating
  "Floating elements system - port of Clay.h floating functionality.

   Floating elements 'float' above other elements in z-order, don't affect
   sibling/parent layout, and can be positioned relative to:
   - Their parent element (:parent)
   - A specific element by ID (:element)
   - The root/viewport (:root)

   Attach points control where the floating element anchors:
   - 9 positions in a 3x3 grid (left/center/right x top/center/bottom)
   - Both the floating element and its parent have attach points
   - The floating element's attach point aligns to the parent's attach point")

;; ============================================================================
;; ATTACH POINT CALCULATIONS
;; ============================================================================

(def attach-points
  "Valid attach point keywords"
  #{:left-top :left-center :left-bottom
    :center-top :center-center :center-bottom
    :right-top :right-center :right-bottom})

(defn- get-attach-x
  "Get the X coordinate for an attach point on a bounding box."
  [attach-point {:keys [x width]}]
  (case attach-point
    (:left-top :left-center :left-bottom) x
    (:center-top :center-center :center-bottom) (+ x (/ width 2))
    (:right-top :right-center :right-bottom) (+ x width)
    x))

(defn- get-attach-y
  "Get the Y coordinate for an attach point on a bounding box."
  [attach-point {:keys [y height]}]
  (case attach-point
    (:left-top :center-top :right-top) y
    (:left-center :center-center :right-center) (+ y (/ height 2))
    (:left-bottom :center-bottom :right-bottom) (+ y height)
    y))

(defn- calculate-element-offset-x
  "Calculate the X offset to apply to floating element based on its attach point."
  [attach-point width]
  (case attach-point
    (:left-top :left-center :left-bottom) 0
    (:center-top :center-center :center-bottom) (- (/ width 2))
    (:right-top :right-center :right-bottom) (- width)
    0))

(defn- calculate-element-offset-y
  "Calculate the Y offset to apply to floating element based on its attach point."
  [attach-point height]
  (case attach-point
    (:left-top :center-top :right-top) 0
    (:left-center :center-center :right-center) (- (/ height 2))
    (:left-bottom :center-bottom :right-bottom) (- height)
    0))

;; ============================================================================
;; FLOATING POSITION CALCULATION
;; ============================================================================

(defn calculate-floating-position
  "Calculate the final position for a floating element.

   Parameters:
   - floating-config: {:attach-to :parent/:element/:root
                       :attach-points {:element :left-top :parent :right-top}
                       :offset {:x 0 :y 0}
                       :parent-id uint32 (for :element mode)}
   - floating-dimensions: {:width :height} of the floating element
   - parent-bounding-box: {:x :y :width :height} of the parent
   - root-bounding-box: {:x :y :width :height} of the root (viewport)

   Returns {:x :y} position for the floating element."
  [floating-config floating-dimensions parent-bounding-box root-bounding-box]
  (let [{:keys [attach-to attach-points offset]} floating-config
        {:keys [element parent]} (or attach-points {:element :left-top :parent :left-top})
        offset-x (get offset :x 0)
        offset-y (get offset :y 0)

        ;; Determine which bounding box to use based on attach-to mode
        target-box (case attach-to
                     :root root-bounding-box
                     ;; :element uses the looked-up parent's bounding box (passed in)
                     ;; :parent uses the parent's bounding box
                     parent-bounding-box)

        ;; Get the target attach position on parent
        target-x (get-attach-x parent target-box)
        target-y (get-attach-y parent target-box)

        ;; Apply element's attach point offset
        element-offset-x (calculate-element-offset-x element (:width floating-dimensions))
        element-offset-y (calculate-element-offset-y element (:height floating-dimensions))

        ;; Final position
        final-x (+ target-x element-offset-x offset-x)
        final-y (+ target-y element-offset-y offset-y)]

    {:x final-x :y final-y}))

;; ============================================================================
;; FLOATING ELEMENT TRACKING
;; ============================================================================

(defn collect-floating-elements
  "Collect all floating elements from layout state.
   Returns a vector of {:element-idx :config :z-index} sorted by z-index."
  [state]
  (let [elements (:layout-elements state)]
    (->> elements
         (map-indexed
          (fn [idx element]
            (when-let [floating-config
                       (some (fn [cfg]
                               (when (= :floating (:type cfg))
                                 (:config cfg)))
                             (:configs element))]
              (when (not= :none (:attach-to floating-config :none))
                {:element-idx idx
                 :config floating-config
                 :z-index (get floating-config :z-index 0)}))))
         (remove nil?)
         (sort-by :z-index)
         vec)))

(defn find-element-by-id
  "Find an element in the layout by its ID hash."
  [state target-id]
  (let [elements (:layout-elements state)]
    (first
     (keep-indexed
      (fn [idx element]
        (when (= target-id (get-in element [:id :id]))
          {:idx idx :element element}))
      elements))))

;; ============================================================================
;; POSITION FLOATING ELEMENTS
;; ============================================================================

(defn- get-element [state idx]
  (get-in state [:layout-elements idx]))

(defn- get-parent-idx
  "Find the parent index for an element.
   We traverse layout-elements looking for who has this element as a child."
  [state element-idx]
  (let [elements (:layout-elements state)]
    (first
     (keep-indexed
      (fn [idx element]
        (when (some #{element-idx} (:children element))
          idx))
      elements))))

(defn position-floating-element
  "Position a single floating element based on its config.

   Returns updated state with the element's bounding-box set."
  [state floating-info]
  (let [{:keys [element-idx config]} floating-info
        element (get-element state element-idx)
        dimensions (:dimensions element)
        viewport (:viewport state)
        root-box {:x 0 :y 0 :width (:width viewport) :height (:height viewport)}

        ;; Determine parent bounding box based on attach-to mode
        parent-box
        (case (:attach-to config)
          :root root-box

          :element
          (if-let [target (find-element-by-id state (:parent-id config))]
            (:bounding-box (:element target))
            root-box) ; fallback if not found

          :parent
          (if-let [parent-idx (get-parent-idx state element-idx)]
            (:bounding-box (get-element state parent-idx))
            root-box)

          root-box)

        ;; Calculate position
        position (calculate-floating-position config dimensions parent-box root-box)

        ;; Apply expand if present
        {:keys [x y]} position
        expand (get config :expand {:width 0 :height 0})
        final-box {:x (- x (/ (:width expand 0) 2))
                   :y (- y (/ (:height expand 0) 2))
                   :width (+ (:width dimensions) (:width expand 0))
                   :height (+ (:height dimensions) (:height expand 0))}]

    (assoc-in state [:layout-elements element-idx :bounding-box] final-box)))

(defn position-all-floating
  "Position all floating elements in the layout.
   Should be called after regular position calculation.

   Returns updated state with all floating elements positioned."
  [state]
  (let [floating-elements (collect-floating-elements state)]
    (reduce position-floating-element state floating-elements)))

;; ============================================================================
;; Z-INDEX SORTING FOR RENDER COMMANDS
;; ============================================================================

(defn get-element-z-index
  "Get the z-index for an element (0 if not floating)."
  [element]
  (if-let [floating-config
           (some (fn [cfg]
                   (when (= :floating (:type cfg))
                     (:config cfg)))
                 (:configs element))]
    (get floating-config :z-index 0)
    0))

(defn sort-render-commands-by-z
  "Sort render commands by z-index.
   Commands from floating elements with higher z-index render on top."
  [state commands]
  (let [elements (:layout-elements state)
        id->z-index (into {}
                          (map-indexed
                           (fn [idx element]
                             [(get-in element [:id :id])
                              (get-element-z-index element)])
                           elements))]
    (vec (sort-by (fn [cmd]
                    (get id->z-index (get-in cmd [:id :id]) 0))
                  commands))))
