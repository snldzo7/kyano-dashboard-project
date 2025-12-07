(ns clay.pointer
  "Pointer/hover state management using Specter.

   Minimal API - just update hover state and query hovered elements.
   All the heavy lifting is done by navigators in tree.cljc."
  (:require [com.rpl.specter :as s]
            [clay.tree :as t]
            [clay.layout :as layout]))

;; ============================================================================
;; HOVER STATE
;; ============================================================================

(defn update-hover-state
  "Update hover state for all nodes based on pointer position.

   Adds :_hovered? key to nodes that contain the pointer.
   Returns updated tree."
  [tree pointer-pos]
  (let [hit-ids (set (map :id (layout/get-elements-at-point tree pointer-pos)))]
    (s/transform [t/TREE-NODES]
                 #(assoc % :_hovered? (contains? hit-ids (:id %)))
                 tree)))

(defn get-hovered
  "Get all currently hovered nodes."
  [tree]
  (s/select [t/TREE-NODES (s/pred :_hovered?)] tree))

(defn get-topmost-hovered
  "Get the topmost (deepest in tree) hovered element."
  [tree]
  (first (get-hovered tree)))

;; ============================================================================
;; CLICK/TAP DETECTION
;; ============================================================================

(defn get-elements-at-point
  "Get all elements at a point, deepest first.
   Alias for layout/get-elements-at-point."
  [tree point]
  (layout/get-elements-at-point tree point))

(defn get-clickable-at-point
  "Get the topmost clickable element at a point.
   An element is clickable if it has :on-click in :configs."
  [tree point]
  (first (filter #(get-in % [:configs :on-click])
                 (get-elements-at-point tree point))))

;; ============================================================================
;; POINTER EVENT HANDLING
;; ============================================================================

(defn handle-pointer-move
  "Handle pointer move event. Returns updated tree with hover state."
  [tree {:keys [x y]}]
  (update-hover-state tree {:x x :y y}))

(defn handle-pointer-down
  "Handle pointer down event. Returns element that was clicked, if any."
  [tree {:keys [x y]}]
  (first (get-elements-at-point tree {:x x :y y})))
