(ns clay.tree
  "Specter navigators and utilities for nested Clay UI trees.

   Tree Structure:
   {:type :container | :text
    :id keyword-or-int
    :layout {:sizing :padding :child-gap :layout-direction :child-alignment}
    :configs {:background :border :clip :floating}
    :dimensions {:width :height}       ; Set by sizing passes
    :bounding-box {:x :y :width :height}  ; Set by position pass
    :children [...]}                   ; Nested children (containers only)

   For :text nodes:
   {:type :text
    :text-content \"string\"
    :text-config {:font-id :font-size :text-color ...}
    :measured {:width :height :words [...]}
    :wrapped-lines [...]}              ; Set by text wrapping pass"
  (:require [com.rpl.specter :as s :refer [recursive-path ALL STAY
                                            stay-then-continue continue-then-stay
                                            if-path pred collect-one putval
                                            view multi-path]]))

;; ============================================================================
;; TREE NAVIGATORS
;; ============================================================================

(def CHILDREN
  "Navigate to children vector of a container node."
  [:children ALL])

(def TREE-NODES
  "Navigate to ALL nodes in tree (pre-order DFS).
   Parent visited before children."
  (recursive-path [] p
    (if-path #(and (map? %) (contains? % :type))
      (stay-then-continue [:children ALL p])
      STAY)))

(def TREE-NODES-POST
  "Navigate to ALL nodes in tree (post-order DFS).
   Children visited before parent - needed for bottom-up sizing."
  (recursive-path [] p
    (if-path #(and (map? %) (contains? % :type))
      (continue-then-stay [:children ALL p])
      STAY)))

(def CONTAINERS
  "Navigate to all container nodes (pre-order)."
  (recursive-path [] p
    (if-path #(= :container (:type %))
      (stay-then-continue [:children ALL p])
      s/STOP)))

(def CONTAINERS-POST
  "Navigate to all container nodes (post-order)."
  (recursive-path [] p
    (if-path #(= :container (:type %))
      (continue-then-stay [:children ALL p])
      s/STOP)))

(def TEXT-NODES
  "Navigate to all text nodes."
  (recursive-path [] p
    (if-path #(= :text (:type %))
      STAY
      (if-path #(= :container (:type %))
        [:children ALL p]
        s/STOP))))

(def LEAVES
  "Navigate to all leaf nodes (text or empty containers)."
  (recursive-path [] p
    (if-path #(or (= :text (:type %))
                  (and (= :container (:type %))
                       (empty? (:children %))))
      STAY
      [:children ALL p])))

;; ============================================================================
;; PREDICATE NAVIGATORS
;; ============================================================================

(defn with-sizing-type
  "Navigate to nodes with specific sizing type on axis."
  [sizing-type x-axis?]
  (pred #(= sizing-type
            (get-in % [:layout :sizing (if x-axis? :width :height) :type]))))

(defn with-config
  "Navigate to nodes that have a specific config type."
  [config-type]
  (pred #(get-in % [:configs config-type])))

(defn with-id
  "Navigate to node with specific ID."
  [id]
  (pred #(= id (:id %))))

;; ============================================================================
;; CONFIG-BASED NAVIGATORS
;; "1 function for 100 things" - compose with-config with TREE-NODES
;; ============================================================================

(def FLOATING-NODES
  "Navigate to nodes with :floating config."
  [TREE-NODES (with-config :floating)])

(def SCROLL-NODES
  "Navigate to nodes with :clip config (scrollable containers)."
  [TREE-NODES (with-config :clip)])

(def ASPECT-RATIO-NODES
  "Navigate to nodes with :aspect-ratio config."
  [TREE-NODES (with-config :aspect-ratio)])

(def IMAGE-NODES
  "Navigate to nodes with :image config."
  [TREE-NODES (with-config :image)])

;; ============================================================================
;; GEOMETRY HELPERS
;; ============================================================================

(defn point-in-box?
  "Check if point (px, py) is inside bounding box."
  [px py box]
  (let [bx (:x box 0)
        by (:y box 0)
        bw (:width box 0)
        bh (:height box 0)]
    (and (>= px bx)
         (< px (+ bx bw))
         (>= py by)
         (< py (+ by bh)))))

(defn rects-intersect?
  "Check if two rectangles intersect."
  [{ax :x ay :y aw :width ah :height :or {ax 0 ay 0 aw 0 ah 0}}
   {bx :x by :y bw :width bh :height :or {bx 0 by 0 bw 0 bh 0}}]
  (and (< ax (+ bx bw))
       (< bx (+ ax aw))
       (< ay (+ by bh))
       (< by (+ ay ah))))

;; ============================================================================
;; POSITIONED NODES NAVIGATORS
;; ============================================================================

(def POSITIONED-NODES
  "Navigate to nodes that have a valid bounding-box (after positioning pass)."
  [TREE-NODES (pred #(and (some? (:bounding-box %))
                          (some? (get-in % [:bounding-box :width]))))])

(defn NODES-CONTAINING-POINT
  "Navigate to positioned nodes that contain the given point."
  [{px :x py :y}]
  [POSITIONED-NODES
   (pred #(point-in-box? px py (:bounding-box %)))])

(defn NODES-IN-RECT
  "Navigate to positioned nodes that intersect the given rectangle."
  [rect]
  [POSITIONED-NODES
   (pred #(rects-intersect? rect (:bounding-box %)))])

;; ============================================================================
;; ELEMENT-BY-ID (stops early when found)
;; ============================================================================

(defn ELEMENT-BY-ID
  "Navigate to element with given ID (stops when found)."
  [id]
  (recursive-path [] p
    (if-path #(= id (:id %))
      STAY
      (if-path #(= :container (:type %))
        [:children ALL p]
        s/STOP))))

;; ============================================================================
;; PARENT-AWARE NAVIGATION
;; ============================================================================

(def PARENT-AND-CHILDREN
  "Navigate to [parent child-idx child] tuples for parent-aware transforms."
  [CONTAINERS
   (s/collect-one STAY)  ; Collect parent
   :children
   s/INDEXED-VALS])      ; [idx child] pairs

;; ============================================================================
;; DIMENSION HELPERS
;; ============================================================================

(defn dim
  "Get dimension from node."
  [node x-axis?]
  (get-in node [:dimensions (if x-axis? :width :height)] 0))

(defn set-dim
  "Set dimension on node."
  [node x-axis? value]
  (assoc-in node [:dimensions (if x-axis? :width :height)] value))

(defn sizing-type
  "Get sizing type for axis."
  [node x-axis?]
  (get-in node [:layout :sizing (if x-axis? :width :height) :type] :fit))

(defn sizing-value
  "Get sizing value for axis (for :fixed or :percent)."
  [node x-axis?]
  (get-in node [:layout :sizing (if x-axis? :width :height) :value]))

(defn sizing-min
  "Get sizing min constraint."
  [node x-axis?]
  (get-in node [:layout :sizing (if x-axis? :width :height) :min] 0))

(defn sizing-max
  "Get sizing max constraint."
  [node x-axis?]
  (get-in node [:layout :sizing (if x-axis? :width :height) :max]
          #?(:clj Float/MAX_VALUE :cljs js/Number.MAX_VALUE)))

(defn padding-total
  "Get total padding for axis."
  [node x-axis?]
  (let [p (get-in node [:layout :padding] {})]
    (if x-axis?
      (+ (get p :left 0) (get p :right 0))
      (+ (get p :top 0) (get p :bottom 0)))))

(defn along-axis?
  "Check if layout direction is along the given axis."
  [node x-axis?]
  (let [dir (get-in node [:layout :layout-direction] :left-to-right)]
    (if x-axis?
      (= dir :left-to-right)
      (= dir :top-to-bottom))))

(defn child-gap
  "Get child gap from node."
  [node]
  (get-in node [:layout :child-gap] 0))

;; ============================================================================
;; TREE CONSTRUCTION HELPERS
;; ============================================================================

(def default-layout
  {:sizing {:width {:type :fit}
            :height {:type :fit}}
   :padding {:top 0 :right 0 :bottom 0 :left 0}
   :child-gap 0
   :layout-direction :left-to-right
   :child-alignment {:x :left :y :top}})

(defn container
  "Create a container node."
  [{:keys [id layout configs children]
    :or {layout {} configs {} children []}}]
  {:type :container
   :id (or id (random-uuid))
   :layout (merge default-layout layout)
   :configs configs
   :dimensions {:width nil :height nil}
   :bounding-box {:x 0 :y 0 :width 0 :height 0}
   :children children})

(defn text-node
  "Create a text node."
  [{:keys [id content config measured]
    :or {config {}}}]
  {:type :text
   :id (or id (random-uuid))
   :text-content content
   :text-config config
   :measured measured
   :dimensions {:width (:width measured 0)
                :height (:height measured 0)}
   :bounding-box {:x 0 :y 0 :width 0 :height 0}
   :wrapped-lines nil})

(defn root-container
  "Create root container with viewport dimensions."
  [viewport children]
  {:type :container
   :id :root
   :layout {:sizing {:width {:type :fixed :value (:width viewport)}
                     :height {:type :fixed :value (:height viewport)}}
            :padding {:top 0 :right 0 :bottom 0 :left 0}
            :child-gap 0
            :layout-direction :top-to-bottom
            :child-alignment {:x :left :y :top}}
   :configs {}
   :dimensions {:width (:width viewport)
                :height (:height viewport)}
   :bounding-box {:x 0 :y 0
                  :width (:width viewport)
                  :height (:height viewport)}
   :children children})
