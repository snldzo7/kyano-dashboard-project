(ns clay.layout.sizing
  "Sizing algorithm - faithful port of Clay__SizeContainersAlongAxis from clay.h

   Clay sizing types:
   - :fit   - Shrink to content, respects min/max
   - :grow  - Expand to fill available space, shares with other grow elements
   - :percent - Percentage of (parent size - padding - gaps)
   - :fixed - Exact pixel value, respects min/max

   Algorithm (per axis):
   1. BFS traversal from roots
   2. For each parent:
      a. Calculate total padding and child gaps
      b. Sum content size (excluding percent children)
      c. Expand percent children
      d. If along-axis: distribute remaining to grow OR compress if overflow
      e. If off-axis: expand grow to fill")

;; ============================================================================
;; CONSTANTS (matching Clay.h)
;; ============================================================================

(def ^:private EPSILON 0.01)
(def ^:private MAX-FLOAT #?(:clj Float/MAX_VALUE :cljs js/Number.MAX_VALUE))

(defn- float-equal? [a b]
  (let [diff (- a b)]
    (and (< diff EPSILON) (> diff (- EPSILON)))))

;; ============================================================================
;; STATE ACCESSORS
;; ============================================================================

(defn- get-element [state idx]
  (get-in state [:layout-elements idx]))

(defn- get-element-dimension [state idx x-axis?]
  (get-in state [:layout-elements idx :dimensions (if x-axis? :width :height)]))

(defn- set-element-dimension [state idx x-axis? value]
  (assoc-in state [:layout-elements idx :dimensions (if x-axis? :width :height)] value))

(defn- get-sizing [element x-axis?]
  (get-in element [:layout :sizing (if x-axis? :width :height)]))

(defn- get-min-dimension [element x-axis?]
  (get-in element [:min-dimensions (if x-axis? :width :height)] 0))

(defn- get-padding-total [layout x-axis?]
  (let [padding (:padding layout)]
    (if x-axis?
      (+ (get padding :left 0) (get padding :right 0))
      (+ (get padding :top 0) (get padding :bottom 0)))))

(defn- sizing-along-axis? [layout x-axis?]
  (let [dir (:layout-direction layout :left-to-right)]
    (if x-axis?
      (= dir :left-to-right)
      (= dir :top-to-bottom))))

;; ============================================================================
;; COLLECT ELEMENTS (BFS)
;; ============================================================================

(defn- collect-bfs
  "Collect element indices in BFS order starting from index 0."
  [state]
  (loop [queue [0]
         result []]
    (if (empty? queue)
      result
      (let [idx (first queue)
            element (get-element state idx)
            children (when (= :container (:type element))
                       (:children element []))]
        (recur (into (vec (rest queue)) children)
               (conj result idx))))))

;; ============================================================================
;; APPLY MIN/MAX CONSTRAINTS
;; ============================================================================

(defn- apply-min-max
  "Apply min/max constraints from sizing config."
  [value sizing]
  (let [min-val (get sizing :min 0)
        max-val (get sizing :max MAX-FLOAT)]
    (-> value
        (max min-val)
        (min max-val))))

;; ============================================================================
;; SIZE CONTAINERS ALONG AXIS
;; Port of Clay__SizeContainersAlongAxis
;; ============================================================================

(defn size-along-axis
  "Size all elements along the specified axis.
   Faithful port of Clay__SizeContainersAlongAxis from clay.h

   Parameters:
   - state: Layout state with :layout-elements vector
   - x-axis?: true for width, false for height

   Returns updated state with dimensions calculated."
  [state x-axis?]
  (let [bfs-order (collect-bfs state)]
    (reduce
     (fn [state parent-idx]
       (let [parent (get-element state parent-idx)
             layout (:layout parent)
             children (:children parent [])
             child-gap (get layout :child-gap 0)
             parent-size (get-element-dimension state parent-idx x-axis?)
             parent-padding (get-padding-total layout x-axis?)
             sizing-along? (sizing-along-axis? layout x-axis?)]

         (if (or (nil? parent-size) (empty? children))
           state

           ;; First pass: collect info and build resizable buffer
           (let [;; Gather child info
                 child-info
                 (reduce
                  (fn [acc child-idx]
                    (let [child (get-element state child-idx)
                          sizing (get-sizing child x-axis?)
                          sizing-type (:type sizing)
                          child-size (get-element-dimension state child-idx x-axis?)]
                      (-> acc
                          (update :content-size
                                  (fn [cs]
                                    (if sizing-along?
                                      ;; Along axis: sum sizes (exclude percent)
                                      (+ cs (if (= sizing-type :percent) 0 (or child-size 0)))
                                      ;; Off axis: max of sizes
                                      (max cs (or child-size 0)))))
                          (update :total-gaps
                                  (fn [tg]
                                    (if (and sizing-along? (pos? (:child-count acc)))
                                      (+ tg child-gap)
                                      tg)))
                          (update :child-count inc)
                          (update :grow-count
                                  (fn [gc]
                                    (if (and sizing-along? (= sizing-type :grow))
                                      (inc gc)
                                      gc)))
                          ;; Resizable = not percent, not fixed, not text
                          (update :resizable
                                  (fn [r]
                                    (if (and (not= sizing-type :percent)
                                             (not= sizing-type :fixed)
                                             (not= (:type child) :text))
                                      (conj r child-idx)
                                      r))))))
                  {:content-size 0
                   :total-gaps 0
                   :child-count 0
                   :grow-count 0
                   :resizable []}
                  children)

                 total-padding-and-gaps (+ parent-padding (:total-gaps child-info))

                 ;; Second pass: expand percent containers
                 state
                 (reduce
                  (fn [s child-idx]
                    (let [child (get-element s child-idx)
                          sizing (get-sizing child x-axis?)]
                      (if (= :percent (:type sizing))
                        (let [percent-value (get sizing :value 0)
                              ;; Percent values in our DSL are 0-1 (e.g., 0.55 = 55%)
                              ;; If value > 1, assume it's 0-100 scale and convert
                              normalized-percent (if (> percent-value 1)
                                                   (/ percent-value 100.0)
                                                   percent-value)
                              size (* (- parent-size total-padding-and-gaps)
                                      normalized-percent)]
                          (set-element-dimension s child-idx x-axis? size))
                        s)))
                  state
                  children)

                 ;; Recalculate content size after percent expansion
                 inner-content-size
                 (reduce
                  (fn [total child-idx]
                    (let [child-size (get-element-dimension state child-idx x-axis?)]
                      (if sizing-along?
                        (+ total (or child-size 0))
                        (max total (or child-size 0)))))
                  0
                  children)

                 inner-content-with-gaps
                 (if sizing-along?
                   (+ inner-content-size
                      (* child-gap (max 0 (dec (count children)))))
                   inner-content-size)]

             (if sizing-along?
               ;; SIZING ALONG AXIS
               (let [size-to-distribute (- parent-size parent-padding inner-content-with-gaps)]
                 (cond
                   ;; Content too large - compress
                   (< size-to-distribute (- EPSILON))
                   (let [resizable (:resizable child-info)]
                     (if (empty? resizable)
                       state
                       ;; Compress resizable elements proportionally
                       (loop [s state
                              remaining size-to-distribute
                              resizable-set (set resizable)]
                         (if (or (>= remaining (- EPSILON))
                                 (empty? resizable-set))
                           s
                           (let [;; Find largest elements
                                 sizes (map #(get-element-dimension s % x-axis?) resizable-set)
                                 largest (apply max sizes)
                                 width-to-add (/ remaining (count resizable-set))

                                 ;; Compress largest elements
                                 [s' remaining' still-resizable]
                                 (reduce
                                  (fn [[s rem resizable] child-idx]
                                    (let [child-size (get-element-dimension s child-idx x-axis?)
                                          ;; Use sizing min, not min-dimensions (Clay.h uses sizing.min)
                                          sizing (get-sizing (get-element s child-idx) x-axis?)
                                          min-size (get sizing :min 0)]
                                      (if (float-equal? child-size largest)
                                        (let [new-size (+ child-size width-to-add)
                                              clamped (max min-size new-size)
                                              actual-change (- clamped child-size)]
                                          [(set-element-dimension s child-idx x-axis? clamped)
                                           (- rem actual-change)
                                           (if (<= clamped min-size)
                                             (disj resizable child-idx)
                                             resizable)])
                                        [s rem resizable])))
                                  [s remaining resizable-set]
                                  resizable-set)]
                             (recur s' remaining' still-resizable))))))

                   ;; Content too small and has grow containers - expand
                   (and (> size-to-distribute EPSILON)
                        (pos? (:grow-count child-info)))
                   (let [grow-children (filter
                                        (fn [idx]
                                          (= :grow (:type (get-sizing (get-element state idx) x-axis?))))
                                        children)]
                     (if (empty? grow-children)
                       state
                       ;; Distribute evenly to grow elements
                       (loop [s state
                              remaining size-to-distribute
                              grow-set (set grow-children)]
                         (if (or (<= remaining EPSILON)
                                 (empty? grow-set))
                           s
                           (let [;; Find smallest grow elements
                                 sizes (map #(get-element-dimension s % x-axis?) grow-set)
                                 smallest (apply min sizes)
                                 width-to-add (/ remaining (count grow-set))

                                 ;; Expand smallest elements
                                 [s' remaining' still-growing]
                                 (reduce
                                  (fn [[s rem growing] child-idx]
                                    (let [child-size (or (get-element-dimension s child-idx x-axis?) 0)
                                          sizing (get-sizing (get-element s child-idx) x-axis?)
                                          max-size (get sizing :max MAX-FLOAT)]
                                      (if (float-equal? child-size smallest)
                                        (let [new-size (+ child-size width-to-add)
                                              min-size (get sizing :min 0)
                                              clamped (-> new-size (max min-size) (min max-size))
                                              actual-change (- clamped child-size)]
                                          [(set-element-dimension s child-idx x-axis? clamped)
                                           (- rem actual-change)
                                           (if (or (>= clamped max-size) (<= clamped min-size))
                                             (disj growing child-idx)
                                             growing)])
                                        [s rem growing])))
                                  [s remaining grow-set]
                                  grow-set)]
                             (recur s' remaining' still-growing))))))

                   :else state))

               ;; SIZING OFF AXIS (perpendicular)
               ;; Expand grow elements to fill parent
               (let [max-size (- parent-size parent-padding)]
                 (reduce
                  (fn [s child-idx]
                    (let [child (get-element s child-idx)
                          sizing (get-sizing child x-axis?)
                          sizing-type (:type sizing)]
                      (if (= sizing-type :grow)
                        (let [max-sizing (get sizing :max MAX-FLOAT)
                              new-size (min max-size max-sizing)
                              min-size (get-min-dimension child x-axis?)
                              clamped (max min-size new-size)]
                          (set-element-dimension s child-idx x-axis? clamped))
                        s)))
                  state
                  (:resizable child-info))))))))
     state
     bfs-order)))

;; ============================================================================
;; INITIAL SIZING
;; ============================================================================

(defn initialize-dimensions
  "Set initial dimensions for fixed, grow, and fit elements.
   Faithful port of Clay.h initialization - grow elements start at their min size."
  [state x-axis?]
  (let [bfs-order (collect-bfs state)
        bottom-up (reverse bfs-order)
        ;; First: set fixed and grow sizes
        state' (reduce
                (fn [s idx]
                  (let [element (get-element s idx)
                        sizing (get-sizing element x-axis?)
                        sizing-type (:type sizing)]
                    (case sizing-type
                      :fixed
                      (let [value (apply-min-max (:value sizing 0) sizing)]
                        (set-element-dimension s idx x-axis? value))

                      ;; GROW elements start at their min size (Clay.h line 2277-2278)
                      :grow
                      (let [min-size (get sizing :min 0)]
                        (set-element-dimension s idx x-axis? min-size))

                      ;; For text elements, use measured dimensions
                      (if (= :text (:type element))
                        (let [dims (:dimensions element)
                              value (if x-axis? (:width dims) (:height dims))]
                          (if value
                            (set-element-dimension s idx x-axis? value)
                            s))
                        s))))
                state
                bfs-order)]
    ;; Then: calculate fit sizes bottom-up
    (reduce
       (fn [s idx]
         (let [element (get-element s idx)
               sizing (get-sizing element x-axis?)
               sizing-type (:type sizing)]
           (if (= sizing-type :fit)
             (let [layout (:layout element)
                   children (:children element [])
                   padding (get-padding-total layout x-axis?)
                   child-gap (get layout :child-gap 0)
                   sizing-along? (sizing-along-axis? layout x-axis?)

                   content-size
                   (if (empty? children)
                     0
                     (let [child-sizes (map #(or (get-element-dimension s % x-axis?) 0) children)]
                       (if sizing-along?
                         (+ (reduce + child-sizes)
                            (* child-gap (dec (count children))))
                         (apply max child-sizes))))

                   total-size (+ padding content-size)
                   clamped (apply-min-max total-size sizing)]
               (set-element-dimension s idx x-axis? clamped))
             s)))
     state'
     bottom-up)))

;; ============================================================================
;; HEIGHT PROPAGATION
;; ============================================================================

(defn propagate-heights
  "Propagate child heights to parents after text wrapping.
   Port of height propagation from Clay__CalculateFinalLayout."
  [state]
  (let [bfs-order (collect-bfs state)
        bottom-up (reverse bfs-order)]
    (reduce
     (fn [s idx]
       (let [element (get-element s idx)
             layout (:layout element)
             sizing (get-sizing element false)  ; height
             children (:children element [])]

         (if (or (= :text (:type element))
                 (empty? children)
                 (= :fixed (:type sizing))
                 (= :percent (:type sizing)))
           s

           (let [dir (:layout-direction layout :left-to-right)
                 padding-v (get-padding-total layout false)
                 child-gap (get layout :child-gap 0)]

             (if (= dir :left-to-right)
               ;; Horizontal: height = max child height + padding
               (let [max-child-height (apply max
                                             (cons 0 (map #(or (get-element-dimension s % false) 0) children)))
                     new-height (+ max-child-height padding-v)
                     clamped (apply-min-max new-height sizing)]
                 (set-element-dimension s idx false clamped))

               ;; Vertical: height = sum of children + padding + gaps
               (let [sum-heights (reduce + (map #(or (get-element-dimension s % false) 0) children))
                     gaps (* child-gap (max 0 (dec (count children))))
                     new-height (+ sum-heights padding-v gaps)
                     clamped (apply-min-max new-height sizing)]
                 (set-element-dimension s idx false clamped)))))))
     state
     bottom-up)))
