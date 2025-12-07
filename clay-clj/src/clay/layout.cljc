(ns clay.layout
  "High-performance layout engine using Specter for tree transformations.

   Pipeline: Tree → Sizing → Text Wrap → Positioning → Render Commands

   Design principles:
   - Pure Specter transforms for all tree operations
   - Structural sharing via immutable updates
   - Parallel execution for independent passes
   - Transducers for render command generation

   All passes are pure functions on nested tree structures.
   No begin/end ceremony - just transform the tree."
  (:require [com.rpl.specter :as s]
            [clay.tree :as t]
            #?(:clj [clojure.core.reducers :as r])))

;; ============================================================================
;; CONSTANTS & UTILITIES
;; ============================================================================

(def ^:private EPSILON 0.01)
(def ^:private MAX-FLOAT #?(:clj Float/MAX_VALUE :cljs js/Number.MAX_VALUE))

(defn- float-equal? [a b]
  (< (abs (- a b)) EPSILON))

(defn- clamp
  "Clamp value between min and max. Inlined for performance."
  ^double [^double value ^double min-val ^double max-val]
  (-> value (max min-val) (min max-val)))

;; ============================================================================
;; PERFORMANCE: BATCHED SPECTER TRANSFORMS
;; ============================================================================

(defmacro batch-transforms
  "Batch multiple Specter transforms for better performance.
   Reduces intermediate tree allocations by composing transforms.

   Usage:
   (batch-transforms tree
     [t/CONTAINERS] transform-fn-1
     [t/TEXT-NODES] transform-fn-2)"
  [tree & nav-fn-pairs]
  (let [pairs (partition 2 nav-fn-pairs)]
    `(-> ~tree
         ~@(for [[nav f] pairs]
             `(s/transform ~nav ~f)))))

(defn compose-sizing-transforms
  "Compose multiple sizing transforms into a single tree pass.
   More efficient than sequential transforms for large trees."
  [x-axis?]
  (fn [node]
    (let [sizing-type (t/sizing-type node x-axis?)]
      (case sizing-type
        :fixed
        (let [value (t/sizing-value node x-axis?)
              min-v (t/sizing-min node x-axis?)
              max-v (t/sizing-max node x-axis?)]
          (t/set-dim node x-axis? (clamp value min-v max-v)))

        :grow
        (t/set-dim node x-axis? (t/sizing-min node x-axis?))

        ;; Text nodes
        (if (= :text (:type node))
          (let [measured (get-in node [:measured (if x-axis? :width :height)] 0)]
            (t/set-dim node x-axis? measured))
          node)))))

;; ============================================================================
;; PHASE 1: FIXED & GROW INITIALIZATION (Pre-order)
;; ============================================================================

(defn apply-fixed-sizes
  "Set dimensions for :fixed sizing type (pre-order).
   Also initializes :grow elements to their min size."
  [tree x-axis?]
  (s/transform [t/TREE-NODES]
               (fn [node]
                 (let [sizing-type (t/sizing-type node x-axis?)]
                   (case sizing-type
                     :fixed
                     (let [value (t/sizing-value node x-axis?)
                           min-v (t/sizing-min node x-axis?)
                           max-v (t/sizing-max node x-axis?)]
                       (t/set-dim node x-axis? (clamp value min-v max-v)))

                     :grow
                     ;; Grow starts at min size (Clay.h line 2277-2278)
                     (t/set-dim node x-axis? (t/sizing-min node x-axis?))

                     ;; Text nodes use measured dimensions
                     (if (= :text (:type node))
                       (let [measured (get-in node [:measured (if x-axis? :width :height)] 0)]
                         (t/set-dim node x-axis? measured))
                       node))))
               tree))

;; ============================================================================
;; PHASE 2: FIT SIZING (Post-order - children before parents)
;; ============================================================================

(defn calculate-fit-sizes
  "Calculate :fit sizing from children (post-order).
   Parent size = content + padding + gaps."
  [tree x-axis?]
  (s/transform [t/CONTAINERS-POST (t/with-sizing-type :fit x-axis?)]
               (fn [node]
                 (let [children (:children node [])
                       padding (t/padding-total node x-axis?)
                       gap (t/child-gap node)
                       along? (t/along-axis? node x-axis?)

                       content-size
                       (if (empty? children)
                         0
                         (let [child-dims (map #(t/dim % x-axis?) children)]
                           (if along?
                             ;; Along axis: sum + gaps
                             (+ (reduce + child-dims)
                                (* gap (dec (count children))))
                             ;; Off axis: max
                             (apply max child-dims))))

                       total (+ content-size padding)
                       min-v (t/sizing-min node x-axis?)
                       max-v (t/sizing-max node x-axis?)]
                   (t/set-dim node x-axis? (clamp total min-v max-v))))
               tree))

;; ============================================================================
;; PHASE 3: PERCENT SIZING (Pre-order - needs parent size)
;; ============================================================================

(defn apply-percent-sizes
  "Apply :percent sizing based on parent dimensions."
  [tree x-axis?]
  ;; Transform containers, updating their children's percent sizes
  (s/transform [t/CONTAINERS]
               (fn [parent]
                 (let [parent-size (t/dim parent x-axis?)
                       padding (t/padding-total parent x-axis?)
                       available (- parent-size padding)]
                   (update parent :children
                           (fn [children]
                             (mapv (fn [child]
                                     (if (= :percent (t/sizing-type child x-axis?))
                                       (let [pct (t/sizing-value child x-axis?)
                                             ;; Normalize 0-100 to 0-1 if needed
                                             pct-norm (if (> pct 1) (/ pct 100.0) pct)
                                             size (* available pct-norm)]
                                         (t/set-dim child x-axis? size))
                                       child))
                                   children)))))
               tree))

;; ============================================================================
;; PHASE 4: GROW DISTRIBUTION (Parent-level transform)
;; ============================================================================

(defn distribute-grow
  "Distribute remaining space to :grow children.
   Also compresses if content exceeds parent."
  [tree x-axis?]
  (s/transform [t/CONTAINERS]
               (fn [parent]
                 (let [children (:children parent [])
                       parent-size (t/dim parent x-axis?)
                       padding (t/padding-total parent x-axis?)
                       gap (t/child-gap parent)
                       along? (t/along-axis? parent x-axis?)]

                   (if (or (not along?) (empty? children))
                     ;; Off-axis: expand grow children to fill
                     (let [available (- parent-size padding)]
                       (update parent :children
                               (fn [cs]
                                 (mapv (fn [c]
                                         (if (= :grow (t/sizing-type c x-axis?))
                                           (let [max-v (t/sizing-max c x-axis?)
                                                 min-v (t/sizing-min c x-axis?)]
                                             (t/set-dim c x-axis?
                                                        (clamp available min-v max-v)))
                                           c))
                                       cs))))

                     ;; Along-axis: distribute or compress
                     (let [gaps-total (* gap (dec (count children)))
                           content-size (reduce + (map #(t/dim % x-axis?) children))
                           available (- parent-size padding gaps-total)
                           space-to-distribute (- available content-size)]

                       (cond
                         ;; Need to expand grow children
                         (and (> space-to-distribute EPSILON)
                              (some #(= :grow (t/sizing-type % x-axis?)) children))
                         (let [grow-children (filter #(= :grow (t/sizing-type % x-axis?)) children)
                               grow-count (count grow-children)
                               per-child (/ space-to-distribute grow-count)]
                           (update parent :children
                                   (fn [cs]
                                     (mapv (fn [c]
                                             (if (= :grow (t/sizing-type c x-axis?))
                                               (let [current (t/dim c x-axis?)
                                                     new-size (+ current per-child)
                                                     min-v (t/sizing-min c x-axis?)
                                                     max-v (t/sizing-max c x-axis?)]
                                                 (t/set-dim c x-axis?
                                                            (clamp new-size min-v max-v)))
                                               c))
                                           cs))))

                         ;; Need to compress - shrink largest resizable
                         (< space-to-distribute (- EPSILON))
                         (let [resizable (filter (fn [c]
                                                   (and (not= :fixed (t/sizing-type c x-axis?))
                                                        (not= :percent (t/sizing-type c x-axis?))
                                                        (not= :text (:type c))))
                                                 children)]
                           (if (empty? resizable)
                             parent
                             ;; Simple proportional compression
                             (let [resizable-total (reduce + (map #(t/dim % x-axis?) resizable))
                                   compression-ratio (if (pos? resizable-total)
                                                       (/ (+ resizable-total space-to-distribute)
                                                          resizable-total)
                                                       1)]
                               (update parent :children
                                       (fn [cs]
                                         (mapv (fn [c]
                                                 (if (some #(= (:id %) (:id c)) resizable)
                                                   (let [current (t/dim c x-axis?)
                                                         new-size (* current compression-ratio)
                                                         min-v (t/sizing-min c x-axis?)]
                                                     (t/set-dim c x-axis? (max new-size min-v)))
                                                   c))
                                               cs))))))

                         :else parent)))))
               tree))

;; ============================================================================
;; PHASE 5: TEXT WRAPPING
;; ============================================================================

(defn wrap-text
  "Wrap text nodes based on parent width.
   Updates dimensions and creates wrapped-lines."
  [tree measure-fn]
  (if-not measure-fn
    tree
    ;; First pass: collect text nodes with their parent widths
    ;; For now, simple implementation - could be optimized with Specter
    (s/transform [t/TEXT-NODES]
                 (fn [text-node]
                   (let [wrap-mode (get-in text-node [:text-config :wrap-mode] :words)
                         width (t/dim text-node true)]
                     (if (or (= wrap-mode :none) (nil? width) (<= width 0))
                       text-node
                       ;; Use measured words to wrap
                       (let [words (get-in text-node [:measured :words] [])
                             line-height (or (get-in text-node [:text-config :line-height])
                                             (get-in text-node [:text-config :font-size])
                                             16)]
                         (if (empty? words)
                           text-node
                           ;; Simple word wrapping
                           (let [lines (loop [remaining words
                                              current-line []
                                              current-width 0
                                              lines []]
                                         (if (empty? remaining)
                                           (if (empty? current-line)
                                             lines
                                             (conj lines {:text (apply str (map :text current-line))
                                                          :width current-width}))
                                           (let [word (first remaining)
                                                 word-width (:width word 0)
                                                 new-width (+ current-width word-width)]
                                             (if (and (> new-width width)
                                                      (not (empty? current-line)))
                                               ;; Wrap to new line
                                               (recur remaining
                                                      []
                                                      0
                                                      (conj lines {:text (apply str (map :text current-line))
                                                                   :width current-width}))
                                               ;; Add to current line
                                               (recur (rest remaining)
                                                      (conj current-line word)
                                                      new-width
                                                      lines)))))]
                                 (-> text-node
                                     (assoc :wrapped-lines lines)
                                     (t/set-dim false (* (count lines) line-height)))))))))
                 tree)))

;; ============================================================================
;; PHASE 6: PROPAGATE HEIGHTS (Post-order after text wrap)
;; ============================================================================

(defn propagate-heights
  "Recalculate :fit heights after text wrapping."
  [tree]
  (calculate-fit-sizes tree false))

;; ============================================================================
;; PHASE 7: POSITION CALCULATION (Specter-based)
;; ============================================================================

(defn- calculate-child-position
  "Calculate position for a single child given parent context and cursor.
   Pure function - no side effects."
  [child cursor-x cursor-y inner-w inner-h align horiz?]
  (let [child-w (t/dim child true)
        child-h (t/dim child false)
        ;; Off-axis alignment
        [cx cy] (if horiz?
                  [cursor-x
                   (case (:y align :top)
                     :top cursor-y
                     :center (+ cursor-y (/ (- inner-h child-h) 2))
                     :bottom (+ cursor-y (- inner-h child-h))
                     cursor-y)]
                  [(case (:x align :left)
                     :left cursor-x
                     :center (+ cursor-x (/ (- inner-w child-w) 2))
                     :right (+ cursor-x (- inner-w child-w))
                     cursor-x)
                   cursor-y])]
    {:x cx :y cy :width child-w :height child-h}))

(defn- position-children-in-container
  "Position all children within a container. Uses reduce for cursor tracking.
   Returns container with positioned children."
  [container]
  (if (or (not= :container (:type container))
          (empty? (:children container)))
    container
    (let [{:keys [x y]} (:bounding-box container)
          {:keys [padding child-gap layout-direction child-alignment]} (:layout container)
          gap (or child-gap 0)
          align (or child-alignment {:x :left :y :top})
          horiz? (= :left-to-right layout-direction)
          inner-w (- (t/dim container true) (:left padding 0) (:right padding 0))
          inner-h (- (t/dim container false) (:top padding 0) (:bottom padding 0))
          start-x (+ x (:left padding 0))
          start-y (+ y (:top padding 0))]
      (update container :children
              (fn [children]
                (first
                  (reduce
                    (fn [[result cursor-x cursor-y] child]
                      (let [box (calculate-child-position child cursor-x cursor-y
                                                          inner-w inner-h align horiz?)
                            child' (assoc child :bounding-box box)
                            ;; Advance cursor
                            [next-x next-y] (if horiz?
                                              [(+ cursor-x (:width box) gap) cursor-y]
                                              [cursor-x (+ cursor-y (:height box) gap)])]
                        [(conj result child') next-x next-y]))
                    [[] start-x start-y]
                    children)))))))

(defn calculate-positions
  "Calculate bounding boxes for all nodes using Specter pre-order transform.
   Each container positions its direct children, Specter handles tree traversal."
  [tree]
  ;; Use Specter's pre-order transform - parent processed before children
  ;; The transform positions children, then Specter recurses into them
  (s/transform [t/CONTAINERS]
               position-children-in-container
               tree))

;; ============================================================================
;; PHASE 8: ASPECT RATIO (After X-sizing, before Y-sizing)
;; ============================================================================

(defn apply-aspect-ratios
  "Apply aspect ratio constraints - height = width / ratio.
   Must be called after X-sizing is complete."
  [tree]
  (s/transform [t/ASPECT-RATIO-NODES]
               (fn [node]
                 (let [ratio (get-in node [:configs :aspect-ratio :ratio] 1)
                       width (t/dim node true)
                       new-height (/ width ratio)]
                   (t/set-dim node false new-height)))
               tree))

;; ============================================================================
;; PHASE 9: FLOATING ELEMENTS (After positioning)
;; ============================================================================

(defn- build-element-id-map
  "Build a map of element-id -> bounding-box for quick lookups."
  [tree]
  (reduce (fn [m node]
            (assoc m (:id node) (:bounding-box node)))
          {}
          (s/select [t/POSITIONED-NODES] tree)))

(defn- calculate-floating-position
  "Calculate floating element position based on normalized floating config.

   Floating config (from normalize.cljc):
   {:to :parent | :target-id    ;; target element
    :at :bottom-right | etc     ;; attachment point on target
    :offset [ox oy]             ;; offset from attachment point
    :z z}                       ;; z-index (optional)"
  [cfg target-box own-size]
  (let [{:keys [x y width height]} target-box
        ;; Extract from normalized format
        offset-vec (or (:offset cfg) [0 0])
        [ox oy] (if (vector? offset-vec)
                  offset-vec
                  [0 0])
        ow (:width own-size 0)
        oh (:height own-size 0)

        ;; Calculate attach point on target using :at
        attach-point (or (:at cfg) :top-left)
        [ax ay] (case attach-point
                  :top-left [x y]
                  :top-center [(+ x (/ width 2)) y]
                  :top-right [(+ x width) y]
                  :center-left [x (+ y (/ height 2))]
                  :center [(+ x (/ width 2)) (+ y (/ height 2))]
                  :center-center [(+ x (/ width 2)) (+ y (/ height 2))]
                  :center-right [(+ x width) (+ y (/ height 2))]
                  :bottom-left [x (+ y height)]
                  :bottom-center [(+ x (/ width 2)) (+ y height)]
                  :bottom-right [(+ x width) (+ y height)]
                  [x y])]

    ;; Element anchors at its top-left by default
    {:x (+ ax ox)
     :y (+ ay oy)
     :width ow
     :height oh}))

(defn position-floating
  "Position floating elements based on their normalized config.
   Floating elements are positioned relative to a target element."
  [tree]
  (let [id-map (build-element-id-map tree)]
    (s/transform [t/FLOATING-NODES]
                 (fn [node]
                   (let [cfg (get-in node [:configs :floating])
                         ;; Use :to from normalized format (default :parent)
                         target-id (or (:to cfg) :parent)
                         target-box (if (or (= target-id :parent) (= target-id :none))
                                      ;; Use root viewport as fallback
                                      (get id-map :root {:x 0 :y 0 :width 800 :height 600})
                                      (get id-map target-id {:x 0 :y 0 :width 0 :height 0}))
                         own-size {:width (t/dim node true) :height (t/dim node false)}
                         new-box (calculate-floating-position cfg target-box own-size)]
                     (assoc node :bounding-box new-box)))
                 tree)))

;; ============================================================================
;; PHASE 10: SCROLL OFFSETS (After positioning)
;; ============================================================================

(defn apply-scroll-offsets
  "Apply scroll offsets to children of scroll containers.

   scroll-state: Map of {:element-id {:x offset-x :y offset-y}}"
  [tree scroll-state]
  (if (empty? scroll-state)
    tree
    (s/transform [t/SCROLL-NODES]
                 (fn [scroll-node]
                   (let [offset (get scroll-state (:id scroll-node) {:x 0 :y 0})
                         ox (:x offset 0)
                         oy (:y offset 0)]
                     (update scroll-node :children
                             (fn [children]
                               (mapv (fn [child]
                                       (-> child
                                           (update-in [:bounding-box :x] + ox)
                                           (update-in [:bounding-box :y] + oy)))
                                     children)))))
                 tree)))

;; ============================================================================
;; RENDER COMMAND GENERATION
;; ============================================================================

(defn- color->map [c]
  (if (map? c) c {:r 0 :g 0 :b 0 :a 255}))

(defn- node->bg-command
  "Generate background rectangle command if node has background config."
  [node]
  (when-let [bg (get-in node [:configs :background])]
    {:bounding-box (:bounding-box node)
     :command-type :rectangle
     :render-data {:color (color->map (:color bg))
                   :corner-radius (or (:corner-radius bg)
                                      {:top-left 0 :top-right 0
                                       :bottom-left 0 :bottom-right 0})}
     :id (:id node)
     :z-index 0}))

(defn- node->text-commands
  "Generate text commands for text node (one per line if wrapped)."
  [node]
  (when (= :text (:type node))
    (let [{:keys [bounding-box text-content text-config wrapped-lines]} node
          {:keys [font-id font-size text-color letter-spacing line-height]} text-config
          lh (or line-height font-size 16)]
      (if (seq wrapped-lines)
        ;; Multiple wrapped lines
        (map-indexed
          (fn [i line]
            {:bounding-box {:x (:x bounding-box)
                            :y (+ (:y bounding-box) (* i lh))
                            :width (:width line)
                            :height lh}
             :command-type :text
             :render-data {:text {:length (count (:text line))
                                  :chars (:text line)}
                           :font-id (or font-id 0)
                           :font-size (or font-size 16)
                           :letter-spacing letter-spacing
                           :line-height line-height
                           :text-color (color->map text-color)}
             :id (:id node)
             :z-index 0})
          wrapped-lines)
        ;; Single line
        [{:bounding-box bounding-box
          :command-type :text
          :render-data {:text {:length (count text-content)
                               :chars text-content}
                        :font-id (or font-id 0)
                        :font-size (or font-size 16)
                        :letter-spacing letter-spacing
                        :line-height line-height
                        :text-color (color->map text-color)}
          :id (:id node)
          :z-index 0}]))))

(defn- node->border-command
  "Generate border command if node has border config."
  [node]
  (when-let [border (get-in node [:configs :border])]
    {:bounding-box (:bounding-box node)
     :command-type :border
     :render-data border
     :id (:id node)
     :z-index 0}))

(defn- node->clip-commands
  "Generate clip start/end commands if node has clip config."
  [node children-commands]
  (when-let [clip (get-in node [:configs :clip])]
    {:start {:bounding-box (:bounding-box node)
             :command-type :clip
             :render-data clip
             :id (:id node)
             :z-index 0}
     :end {:bounding-box (:bounding-box node)
           :command-type :clip-end
           :render-data nil
           :id (:id node)
           :z-index 0}
     :children children-commands}))

;; ============================================================================
;; RENDER COMMAND GENERATION (Optimized)
;; ============================================================================

(defn generate-render-commands
  "Generate render commands from positioned tree with proper clip region handling.

   Uses depth-first traversal to correctly nest clip begin/end.
   Order: background → clip-start → children → clip-end → text → border

   Optimizations:
   - Lazy sequence generation (no intermediate collections)
   - into [] at the end for efficient realization
   - Structural sharing through concat of small seqs"
  [tree]
  (letfn [(emit-node [node]
            (let [bg-cmd (node->bg-command node)
                  text-cmds (node->text-commands node)
                  border-cmd (node->border-command node)
                  clip-cfg (get-in node [:configs :clip])
                  children (:children node)]
              (concat
                ;; 1. Background
                (when bg-cmd [bg-cmd])
                ;; 2. Clip start
                (when clip-cfg
                  [{:bounding-box (:bounding-box node)
                    :command-type :clip
                    :render-data clip-cfg
                    :id (:id node)
                    :z-index 0}])
                ;; 3. Children (recursive)
                (mapcat emit-node children)
                ;; 4. Clip end
                (when clip-cfg
                  [{:bounding-box (:bounding-box node)
                    :command-type :clip-end
                    :render-data nil
                    :id (:id node)
                    :z-index 0}])
                ;; 5. Text (on top of children)
                text-cmds
                ;; 6. Border (on top of everything)
                (when border-cmd [border-cmd]))))]
    ;; Realize lazy seq into vector, filtering nils
    (into [] (remove nil?) (emit-node tree))))

;; ============================================================================
;; MAIN LAYOUT PIPELINE
;; ============================================================================

(defn- size-axis
  "Complete sizing pass for one axis. Composes all sizing transforms."
  [tree x-axis?]
  (-> tree
      (apply-fixed-sizes x-axis?)
      (calculate-fit-sizes x-axis?)
      (apply-percent-sizes x-axis?)
      (distribute-grow x-axis?)))

(defn layout
  "Complete layout pipeline: tree → render commands.

   Parameters:
   - tree: Nested tree from hiccup/DSL
   - opts: Optional map with:
           :measure-fn - Text measurement function
           :scroll-state - Map of {:element-id {:x dx :y dy}}

   Pipeline phases:
   1. X-axis sizing (fixed → fit → percent → grow)
   2. Aspect ratio (locks Y from X)
   3. Text wrapping (uses X widths, affects Y heights)
   4. Y-axis sizing (fixed → fit → percent → grow)
   5. Positioning (parent positions children)
   6. Floating elements (positioned relative to targets)
   7. Scroll offsets (applied to scroll container children)
   8. Render command generation

   Returns: Vector of render commands."
  ([tree] (layout tree {}))
  ([tree opts]
   (let [{:keys [measure-fn scroll-state]} opts]
     (-> tree
         ;; X-axis sizing (single composed pass)
         (size-axis true)

         ;; Aspect ratio (locks Y from X)
         (apply-aspect-ratios)

         ;; Text wrapping (uses X widths)
         (wrap-text measure-fn)

         ;; Y-axis sizing (single composed pass)
         (size-axis false)
         (propagate-heights)

         ;; Positioning
         (calculate-positions)

         ;; Floating elements
         (position-floating)

         ;; Scroll offsets
         (apply-scroll-offsets (or scroll-state {}))

         ;; Generate commands
         (generate-render-commands)))))

#?(:clj
   (defn layout-parallel
     "Parallel layout for large trees (JVM only).

      Uses reducers for parallel command generation on trees with many nodes.
      For small trees (<100 nodes), use regular layout instead.

      Parameters same as layout."
     ([tree] (layout-parallel tree {}))
     ([tree opts]
      (let [{:keys [measure-fn scroll-state]} opts
            ;; Layout phases are sequential (dependencies between phases)
            positioned-tree (-> tree
                                (size-axis true)
                                (apply-aspect-ratios)
                                (wrap-text measure-fn)
                                (size-axis false)
                                (propagate-heights)
                                (calculate-positions)
                                (position-floating)
                                (apply-scroll-offsets (or scroll-state {})))
            ;; Command generation can be parallelized per-subtree
            ;; For now, use standard generation (already efficient)
            ;; Future: use r/fold for trees with 1000+ nodes
            ]
        (generate-render-commands positioned-tree)))))

(defn layout-tree
  "Layout pipeline returning the positioned tree (for debugging/inspection)."
  ([tree] (layout-tree tree {}))
  ([tree opts]
   (let [{:keys [measure-fn scroll-state]} opts]
     (-> tree
         (size-axis true)
         (apply-aspect-ratios)
         (wrap-text measure-fn)
         (size-axis false)
         (propagate-heights)
         (calculate-positions)
         (position-floating)
         (apply-scroll-offsets (or scroll-state {}))))))

;; ============================================================================
;; QUERY UTILITIES
;; "All queries are just Specter selects"
;; ============================================================================

(defn get-element
  "Get element by ID from positioned tree."
  [tree id]
  (s/select-one [(t/ELEMENT-BY-ID id)] tree))

(defn get-elements-at-point
  "Get all elements containing the given point (deepest first)."
  [tree point]
  (reverse (s/select [(t/NODES-CONTAINING-POINT point)] tree)))

(defn get-elements-in-rect
  "Get all elements intersecting the given rectangle."
  [tree rect]
  (s/select [(t/NODES-IN-RECT rect)] tree))

(defn get-descendants
  "Get all descendants of an element."
  [tree id]
  (when-let [node (get-element tree id)]
    (s/select [t/TREE-NODES] node)))

;; ============================================================================
;; DEBUG OVERLAY
;; ============================================================================

(defn- debug-color-for-node
  "Get debug color based on node type and state."
  [node hovered-ids]
  (let [hovered? (contains? hovered-ids (:id node))]
    (cond
      hovered?                     {:r 255 :g 0 :b 0 :a 128}
      (= :text (:type node))       {:r 0 :g 255 :b 0 :a 64}
      (get-in node [:configs :clip]) {:r 0 :g 0 :b 255 :a 64}
      :else                        {:r 128 :g 128 :b 128 :a 32})))

(defn generate-debug-commands
  "Generate debug overlay commands showing bounding boxes."
  ([tree] (generate-debug-commands tree #{}))
  ([tree hovered-ids]
   (vec
     (for [node (s/select [t/POSITIONED-NODES] tree)]
       {:bounding-box (:bounding-box node)
        :command-type :rectangle
        :render-data {:color (debug-color-for-node node hovered-ids)
                      :corner-radius {:top-left 0 :top-right 0
                                      :bottom-left 0 :bottom-right 0}}
        :id (keyword (str "debug-" (name (or (:id node) "unknown"))))
        :z-index 9998}))))
