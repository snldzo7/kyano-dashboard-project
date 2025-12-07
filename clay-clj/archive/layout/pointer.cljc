(ns clay.layout.pointer
  "Pointer/hit testing system - faithful port of Clay.h pointer handling.

   In Clay.h, pointer handling works as follows:
   1. Clay_SetPointerState(position, isPointerDown) called each frame
   2. DFS traversal of elements (back-to-front for z-order)
   3. For each element, check if pointer is inside bounding box
   4. Respects clip/scroll container bounds
   5. Builds pointerOverIds array
   6. Manages state transitions: pressed_this_frame -> pressed -> released_this_frame -> released

   This module provides:
   - point-inside-rect? : AABB hit test
   - set-pointer-state  : Update pointer and calculate hovered elements
   - pointer-over?      : Check if element is under pointer
   - get-pointer-over-ids : Get all elements under pointer
   - get-pointer-data   : Get current pointer state")

;; ============================================================================
;; POINTER STATE (matches Clay_PointerDataInteractionState)
;; ============================================================================

(def pointer-states
  "Pointer interaction states matching Clay.h"
  {:pressed-this-frame 0  ; Left mouse click or touch occurred this frame
   :pressed 1             ; Button held down from previous frame
   :released-this-frame 2 ; Button released this frame
   :released 3})          ; Button not pressed (default)

(defn create-pointer-data
  "Create initial pointer data structure matching Clay_PointerData."
  []
  {:position {:x 0 :y 0}
   :state :released})

;; ============================================================================
;; HIT TESTING (matches Clay__PointIsInsideRect)
;; ============================================================================

(defn point-inside-rect?
  "Check if a point is inside a bounding box.
   Port of Clay__PointIsInsideRect (line 1715-1716):
   point.x >= rect.x && point.x <= rect.x + rect.width &&
   point.y >= rect.y && point.y <= rect.y + rect.height"
  [point rect]
  (let [px (:x point)
        py (:y point)
        rx (:x rect)
        ry (:y rect)
        rw (:width rect)
        rh (:height rect)]
    (and (>= px rx)
         (<= px (+ rx rw))
         (>= py ry)
         (<= py (+ ry rh)))))

;; ============================================================================
;; ELEMENT QUERIES
;; ============================================================================

(defn- get-element [state idx]
  (get-in state [:layout-elements idx]))

(defn- get-element-id [element]
  (get-in element [:id :id]))

(defn- get-clip-element-id
  "Get the clip/scroll parent element ID for an element, if any."
  [_state element]
  ;; For now, check if element has a clip parent recorded
  ;; This would be set during layout if element is inside a scroll container
  (get element :clip-element-id))

;; ============================================================================
;; DFS TRAVERSAL FOR HIT TESTING
;; Port of Clay_SetPointerState traversal (lines 3978-4022)
;; ============================================================================

(defn- collect-elements-under-point
  "DFS traversal to find all elements under a point.
   Traverses back-to-front (highest z-index first).
   Respects clip container bounds.

   Port of Clay_SetPointerState DFS logic."
  [state position]
  (let [elements (:layout-elements state)
        n (count elements)]
    (when (pos? n)
      (loop [stack [0]  ; Start with root
             visited #{}
             result []]
        (if (empty? stack)
          result
          (let [idx (peek stack)
                element (get-element state idx)]
            (if (or (nil? element) (contains? visited idx))
              (recur (pop stack) visited result)
              (let [box (:bounding-box element)
                    inside? (and box (point-inside-rect? position box))

                    ;; Check clip parent bounds if element is in a scroll container
                    clip-id (get-clip-element-id state element)
                    clip-ok? (if clip-id
                               (let [clip-el (some #(when (= clip-id (get-element-id %)) %)
                                                   elements)
                                     clip-box (when clip-el (:bounding-box clip-el))]
                                 (or (nil? clip-box)
                                     (point-inside-rect? position clip-box)))
                               true)

                    hit? (and inside? clip-ok?)

                    ;; Add children in reverse order (so last child is checked first)
                    children (vec (reverse (:children element [])))
                    new-stack (into (pop stack) children)
                    new-visited (conj visited idx)
                    new-result (if hit?
                                 (conj result {:element-id (get-element-id element)
                                               :element-idx idx})
                                 result)]
                (recur new-stack new-visited new-result)))))))))

;; ============================================================================
;; POINTER STATE MACHINE
;; Port of Clay_SetPointerState state transitions (lines 4025-4037)
;; ============================================================================

(defn- update-pointer-state
  "Update pointer state based on current button state.
   Port of Clay.h state machine:
   - If down and was pressed_this_frame -> pressed
   - If down and not pressed -> pressed_this_frame
   - If up and was released_this_frame -> released
   - If up and not released -> released_this_frame"
  [current-state is-pointer-down?]
  (if is-pointer-down?
    (case current-state
      :pressed-this-frame :pressed
      :pressed :pressed
      :pressed-this-frame)  ; Any other state -> pressed-this-frame
    (case current-state
      :released-this-frame :released
      :released :released
      :released-this-frame)))  ; Any other state -> released-this-frame

;; ============================================================================
;; PUBLIC API
;; ============================================================================

(defn set-pointer-state
  "Update pointer position and calculate hovered elements.
   Port of Clay_SetPointerState.

   Parameters:
   - state: Layout state (after end-layout)
   - position: {:x :y} pointer position
   - is-pointer-down?: Boolean, true if mouse button/touch is down

   Returns updated state with:
   - :pointer-data updated
   - :pointer-over-ids populated"
  [state position is-pointer-down?]
  (let [current-pointer-data (get state :pointer-data (create-pointer-data))
        new-state (update-pointer-state (:state current-pointer-data) is-pointer-down?)

        ;; Find all elements under pointer
        elements-under-point (collect-elements-under-point state position)

        ;; Extract just the element IDs
        pointer-over-ids (mapv :element-id elements-under-point)]

    (assoc state
           :pointer-data {:position position
                          :state new-state}
           :pointer-over-ids pointer-over-ids)))

(defn pointer-over?
  "Check if an element ID is under the pointer.
   Port of Clay_PointerOver.

   Parameters:
   - state: Layout state with pointer info
   - element-id: Element ID to check

   Returns true if element is under pointer."
  [state element-id]
  (some #(= element-id %) (:pointer-over-ids state [])))

(defn get-pointer-over-ids
  "Get all element IDs under the pointer.
   Port of Clay_GetPointerOverIds.

   Returns vector of element IDs (front-to-back order)."
  [state]
  (:pointer-over-ids state []))

(defn get-pointer-data
  "Get current pointer data.

   Returns {:position {:x :y} :state keyword}"
  [state]
  (get state :pointer-data (create-pointer-data)))

(defn pressed-this-frame?
  "Check if pointer was pressed this frame."
  [state]
  (= :pressed-this-frame (:state (get-pointer-data state))))

(defn released-this-frame?
  "Check if pointer was released this frame."
  [state]
  (= :released-this-frame (:state (get-pointer-data state))))

(defn pressed?
  "Check if pointer is currently pressed (held down)."
  [state]
  (#{:pressed :pressed-this-frame} (:state (get-pointer-data state))))

;; ============================================================================
;; ELEMENT QUERY BY POINT
;; ============================================================================

(defn get-element-at-point
  "Get the topmost element at a given point.
   Returns element ID or nil."
  [state position]
  (first (mapv :element-id (collect-elements-under-point state position))))

(defn get-elements-at-point
  "Get all elements at a given point, front-to-back order.
   Returns vector of {:element-id :element-idx}."
  [state position]
  (collect-elements-under-point state position))
