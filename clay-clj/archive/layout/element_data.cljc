(ns clay.layout.element-data
  "Element data queries API - port of Clay.h element query functions.

   This module provides:
   - get-element-data    : Get bounding box for element by ID (Clay_GetElementData)
   - get-element-by-id   : Get full element by ID
   - find-elements       : Query elements by predicate
   - get-all-element-ids : Get all element IDs

   Note: Scroll container queries are in clay.layout.scroll
         Pointer queries are in clay.layout.pointer")

;; ============================================================================
;; ELEMENT LOOKUP
;; ============================================================================

(defn- get-element-id [element]
  (get-in element [:id :id]))

(defn get-element-by-id
  "Find an element by its ID.
   Returns the element map or nil if not found."
  [state element-id]
  (first (filter #(= element-id (get-element-id %))
                 (:layout-elements state))))

(defn get-element-index-by-id
  "Find the index of an element by its ID.
   Returns the index or nil if not found."
  [state element-id]
  (first (keep-indexed (fn [idx el]
                         (when (= element-id (get-element-id el)) idx))
                       (:layout-elements state))))

;; ============================================================================
;; ELEMENT DATA QUERY (matches Clay_GetElementData)
;; ============================================================================

(defn get-element-data
  "Get element data (bounding box) for an element by ID.
   Port of Clay_GetElementData.

   Parameters:
   - state: Layout state (after end-layout)
   - element-id: Element ID (numeric hash)

   Returns:
   {:bounding-box {:x :y :width :height}
    :found true/false}"
  [state element-id]
  (if-let [element (get-element-by-id state element-id)]
    {:bounding-box (:bounding-box element)
     :found true}
    {:bounding-box {:x 0 :y 0 :width 0 :height 0}
     :found false}))

(defn get-bounding-box
  "Convenience function to get just the bounding box for an element.
   Returns nil if not found."
  [state element-id]
  (let [data (get-element-data state element-id)]
    (when (:found data)
      (:bounding-box data))))

;; ============================================================================
;; ELEMENT QUERIES
;; ============================================================================

(defn get-all-element-ids
  "Get all element IDs in the layout.
   Returns vector of element IDs."
  [state]
  (mapv get-element-id (:layout-elements state)))

(defn find-elements
  "Find elements matching a predicate.
   Returns vector of elements."
  [state pred]
  (filterv pred (:layout-elements state)))

(defn find-elements-by-type
  "Find elements by type (:container or :text).
   Returns vector of elements."
  [state element-type]
  (find-elements state #(= element-type (:type %))))

(defn find-elements-with-config
  "Find elements that have a specific config type.
   Returns vector of elements."
  [state config-type]
  (find-elements state
                 (fn [el]
                   (some #(= config-type (:type %)) (:configs el)))))

;; ============================================================================
;; ELEMENT TREE QUERIES
;; ============================================================================

(defn get-children
  "Get child elements of an element.
   Returns vector of elements."
  [state element-id]
  (if-let [element (get-element-by-id state element-id)]
    (mapv #(get-in state [:layout-elements %]) (:children element []))
    []))

(defn get-child-ids
  "Get child element IDs of an element.
   Returns vector of element IDs."
  [state element-id]
  (if-let [element (get-element-by-id state element-id)]
    (mapv #(get-element-id (get-in state [:layout-elements %]))
          (:children element []))
    []))

(defn get-parent
  "Get parent element of an element.
   Returns element or nil if root."
  [state element-id]
  (let [elements (:layout-elements state)
        idx (get-element-index-by-id state element-id)]
    (when idx
      (first (filter (fn [el]
                       (some #(= idx %) (:children el [])))
                     elements)))))

(defn get-ancestors
  "Get all ancestor elements of an element (parent, grandparent, etc.).
   Returns vector of elements from immediate parent to root."
  [state element-id]
  (loop [current-id element-id
         ancestors []]
    (if-let [parent (get-parent state current-id)]
      (recur (get-element-id parent) (conj ancestors parent))
      ancestors)))

(defn get-descendants
  "Get all descendant elements of an element.
   Returns vector of elements in depth-first order."
  [state element-id]
  (let [idx (get-element-index-by-id state element-id)]
    (when idx
      (loop [stack [idx]
             result []
             visited #{}]
        (if (empty? stack)
          result
          (let [current-idx (peek stack)
                current (get-in state [:layout-elements current-idx])]
            (if (or (nil? current) (contains? visited current-idx))
              (recur (pop stack) result visited)
              (let [children (:children current [])
                    ;; Add current to result (skip the initial element)
                    new-result (if (= current-idx idx)
                                 result
                                 (conj result current))]
                (recur (into (pop stack) (reverse children))
                       new-result
                       (conj visited current-idx))))))))))

;; ============================================================================
;; ELEMENT INFO HELPERS
;; ============================================================================

(defn get-element-dimensions
  "Get the calculated dimensions of an element.
   Returns {:width :height} or nil if not found."
  [state element-id]
  (when-let [element (get-element-by-id state element-id)]
    (:dimensions element)))

(defn get-element-position
  "Get the position of an element.
   Returns {:x :y} or nil if not found."
  [state element-id]
  (when-let [box (get-bounding-box state element-id)]
    {:x (:x box) :y (:y box)}))

(defn get-element-size
  "Get the size of an element.
   Returns {:width :height} or nil if not found."
  [state element-id]
  (when-let [box (get-bounding-box state element-id)]
    {:width (:width box) :height (:height box)}))

(defn element-contains-point?
  "Check if an element's bounding box contains a point."
  [state element-id point]
  (when-let [box (get-bounding-box state element-id)]
    (let [{:keys [x y width height]} box
          px (:x point)
          py (:y point)]
      (and (>= px x)
           (<= px (+ x width))
           (>= py y)
           (<= py (+ y height))))))
