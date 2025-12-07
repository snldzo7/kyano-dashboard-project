(ns clay.layout.core
  "Core layout engine - port of Clay.h layout functions to Clojure.

   This implements the main layout pipeline:
   1. begin-layout  - Initialize layout state with viewport
   2. open-element  - Push element onto stack (containers)
   3. open-text-element - Create text element with measured dimensions
   4. close-element - Pop element, add to parent's children
   5. end-layout    - Compute positions and generate render commands

   Key differences from Clay.h:
   - Immutable data structures (no arena allocation)
   - Text measurement is async (via callback/Rheon discrete wire)
   - Uses schema-validated data structures"
  (:require [clay.schema :as schema]
            [clay.layout.sizing :as sizing]
            [clay.layout.text :as text]
            [clay.layout.floating :as floating]
            [clay.layout.scroll :as scroll]
            [clay.layout.aspect-ratio :as aspect]))

;; Forward declarations for functions referenced before definition
(declare calculate-positions generate-render-commands)

;; ============================================================================
;; LAYOUT STATE - Replaces Clay_Context
;; ============================================================================

(def default-sizing-axis
  {:type :fit
   :value nil
   :min nil
   :max nil})

(def default-sizing
  {:width default-sizing-axis
   :height default-sizing-axis})

(def default-padding
  {:top 0 :right 0 :bottom 0 :left 0})

(def default-child-alignment
  {:x :left :y :top})

(def default-layout-config
  {:sizing default-sizing
   :padding default-padding
   :child-gap 0
   :child-alignment default-child-alignment
   :layout-direction :left-to-right})

(defn create-root-element
  "Create root element with viewport dimensions."
  [{:keys [width height]}]
  {:id {:id 0 :offset 0 :base-id 0 :string-id 0}
   :type :container
   :dimensions {:width width :height height}
   :min-dimensions {:width 0 :height 0}
   :layout (-> default-layout-config
               (assoc-in [:sizing :width] {:type :fixed :value width})
               (assoc-in [:sizing :height] {:type :fixed :value height}))
   :bounding-box {:x 0 :y 0 :width width :height height}
   :children []
   :configs []})

(defn begin-layout
  "Initialize layout state for a new frame.

   Parameters:
   - viewport: {:width number :height number}

   Returns layout state map."
  [viewport]
  (let [root (create-root-element viewport)]
    {:layout-elements [root]
     :element-stack [0]                     ; Stack of element indices
     :next-id 1                             ; Next element ID
     :viewport viewport
     :render-commands []
     :text-cache {}                         ; Hash-based text measurement cache
     :generation 0}))                       ; For cache expiry

;; ============================================================================
;; ELEMENT ID GENERATION
;; ============================================================================

(defn- generate-element-id
  "Generate a new element ID."
  [state]
  (let [id (:next-id state)]
    [{:id id :offset 0 :base-id id :string-id id}
     (update state :next-id inc)]))

;; ============================================================================
;; ELEMENT TREE BUILDING
;; ============================================================================

(defn open-element
  "Push a new container element onto the stack.

   Parameters:
   - state: Layout state
   - config: Optional layout configuration override

   Returns updated state."
  ([state] (open-element state {}))
  ([state config]
   (let [[element-id state'] (generate-element-id state)
         idx (count (:layout-elements state'))
         element {:id element-id
                  :type :container
                  :dimensions {:width 0 :height 0}
                  :min-dimensions {:width 0 :height 0}
                  :layout (merge default-layout-config config)
                  :bounding-box {:x 0 :y 0 :width 0 :height 0}
                  :children []
                  :configs []}]
     (-> state'
         (update :layout-elements conj element)
         (update :element-stack conj idx)))))

(defn configure-element
  "Apply configuration to the currently open element.

   Parameters:
   - state: Layout state
   - config-type: Keyword like :layout, :border, :background, :floating
   - config: Configuration map

   Returns updated state."
  [state config-type config]
  (let [current-idx (peek (:element-stack state))]
    (update-in state [:layout-elements current-idx :configs]
               conj {:type config-type :config config})))

(defn open-text-element
  "Create a text element with measured dimensions.

   This is the critical function that needs text measurement from the client.
   In Clay.h, this calls Clay__MeasureTextCached immediately.
   In our distributed system, this may be async via Rheon discrete wire.

   Parameters:
   - state: Layout state
   - text: String content
   - text-config: {:font-id :font-size :letter-spacing :line-height :text-color ...}
   - measured: Pre-measured dimensions {:width :height} (from client)

   Returns updated state with text element added to current parent."
  [state text text-config measured]
  (let [[element-id state'] (generate-element-id state)
        idx (count (:layout-elements state'))
        parent-idx (peek (:element-stack state'))

        ;; Calculate line height (0 means use measured height)
        line-height (or (:line-height text-config) 0)
        height (if (pos? line-height)
                 line-height
                 (:height measured))
        _ #?(:cljs (js/console.log "open-text-element:" text "measured-height:" (:height measured) "final-height:" height)
             :clj nil)

        element {:id element-id
                 :type :text
                 :dimensions {:width (:width measured) :height height}
                 :min-dimensions {:width (:min-width measured (:width measured))
                                  :height height}
                 :text-content text
                 :text-config text-config
                 :measured-words (:words measured [])
                 :bounding-box {:x 0 :y 0 :width 0 :height 0}
                 :wrapped-lines nil}]  ; Filled in during layout
    (-> state'
        (update :layout-elements conj element)
        (update-in [:layout-elements parent-idx :children] conj idx))))

(defn close-element
  "Pop the current element from the stack and add it to parent's children.

   Returns updated state."
  [state]
  (let [current-idx (peek (:element-stack state))
        state' (update state :element-stack pop)]
    (if (empty? (:element-stack state'))
      ;; We're closing the root - nothing to add to
      state'
      ;; Add current to parent's children
      (let [parent-idx (peek (:element-stack state'))]
        (update-in state' [:layout-elements parent-idx :children] conj current-idx)))))

;; ============================================================================
;; LAYOUT CALCULATION - Main Pipeline
;; ============================================================================

(defn- get-element [state idx]
  (get-in state [:layout-elements idx]))

(defn- update-element [state idx f & args]
  (apply update-in state [:layout-elements idx] f args))

(defn end-layout
  "Complete the layout calculation and generate render commands.

   This implements Clay__CalculateFinalLayout:
   1. Initialize dimensions (fixed, fit bottom-up)
   2. Size containers along X-axis (percent, grow, compress)
   3. Wrap text elements
   4. Propagate child heights to parents
   5. Size containers along Y-axis
   6. Calculate final positions
   7. Apply scroll offsets
   8. Position floating elements
   9. Generate render commands

   Parameters:
   - state: Layout state
   - measure-fn: Optional function for re-measuring wrapped text lines
   - scroll-positions: Optional map of element-id -> {:x :y} scroll positions

   Returns render commands vector."
  ([state] (end-layout state nil nil))
  ([state measure-fn] (end-layout state measure-fn nil))
  ([state measure-fn scroll-positions]
   (-> state
       ;; Phase 1: X-axis sizing
       (sizing/initialize-dimensions true)
       (sizing/size-along-axis true)

       ;; Phase 2: Apply aspect ratio heights (after X-axis, before text)
       ;; Port of Clay.h lines 2591-2597
       (aspect/apply-all-aspect-heights)

       ;; Phase 3: Text wrapping (adjusts heights based on wrapping)
       (text/wrap-all-text measure-fn)

       ;; Phase 4: Y-axis sizing
       (sizing/initialize-dimensions false)
       (sizing/propagate-heights)
       (sizing/size-along-axis false)

       ;; Phase 5: Apply aspect ratio widths (after Y-axis sizing)
       ;; Port of Clay.h lines 2651-2654
       (aspect/apply-all-aspect-widths)

       ;; Phase 6: Apply scroll offsets BEFORE position calculation
       ;; (Clay.h applies childOffset during DFS position calc)
       (scroll/apply-scroll-positions (or scroll-positions {}))

       ;; Phase 7: Calculate final positions (with scroll offsets applied)
       (calculate-positions)

       ;; Phase 8: Position floating elements (after regular positioning)
       (floating/position-all-floating)

       ;; Phase 9: Generate render commands (sorted by z-index, with clips)
       (generate-render-commands))))

;; ============================================================================
;; POSITION CALCULATION
;; ============================================================================

(defn- calculate-child-positions
  "Calculate positions for all children of an element.
   Port of Clay.h position calculation with scroll offset application."
  [state parent-idx]
  (let [parent (get-element state parent-idx)
        layout (:layout parent)
        {:keys [x y width height]} (:bounding-box parent)
        {:keys [top right bottom left]} (:padding layout)
        ;; Get scroll offset from parent (Clay.h line 3113-3116)
        scroll-offset (scroll/get-scroll-offset parent)
        inner-x (+ x left)
        inner-y (+ y top)
        inner-width (- width left right)
        inner-height (- height top bottom)
        gap (:child-gap layout 0)
        direction (:layout-direction layout)
        child-alignment (:child-alignment layout)
        align-x (get child-alignment :x :left)
        align-y (get child-alignment :y :top)
        horizontal? (= direction :left-to-right)]
    (loop [children (:children parent)
           state state
           cursor-x inner-x
           cursor-y inner-y]
      (if (empty? children)
        state
        (let [child-idx (first children)
              child (get-element state child-idx)
              child-width (get-in child [:dimensions :width])
              child-height (get-in child [:dimensions :height])

              ;; Calculate position based on layout direction and alignment
              [base-x base-y]
              (if horizontal?
                ;; Horizontal layout
                [cursor-x
                 (case align-y
                   :top inner-y
                   :center (+ inner-y (/ (- inner-height child-height) 2))
                   :bottom (- (+ inner-y inner-height) child-height)
                   inner-y)]
                ;; Vertical layout
                [(case align-x
                   :left inner-x
                   :center (+ inner-x (/ (- inner-width child-width) 2))
                   :right (- (+ inner-x inner-width) child-width)
                   inner-x)
                 cursor-y])

              ;; Apply scroll offset to child position (Clay.h line 3113-3116)
              child-x (+ base-x (:x scroll-offset 0))
              child-y (+ base-y (:y scroll-offset 0))

              state' (update-element state child-idx
                                     assoc :bounding-box
                                     {:x child-x
                                      :y child-y
                                      :width child-width
                                      :height child-height})

              ;; Recursively position children's children
              state'' (if (= (:type child) :container)
                        (calculate-child-positions state' child-idx)
                        state')

              ;; Advance cursor
              [next-x next-y]
              (if horizontal?
                [(+ cursor-x child-width gap) cursor-y]
                [cursor-x (+ cursor-y child-height gap)])]
          (recur (rest children) state'' next-x next-y))))))

(defn- calculate-positions
  "Calculate final bounding box positions for all elements."
  [state]
  ;; Root already has bounding box from viewport
  ;; Calculate positions for root's children recursively
  (calculate-child-positions state 0))

;; ============================================================================
;; RENDER COMMAND GENERATION
;; ============================================================================

(defn- generate-between-children-borders
  "Generate betweenChildren border commands between child elements.
   Port of Clay.h lines 3038-3074."
  [state element border-config]
  (let [layout (:layout element)
        children (:children element [])
        border-width (get-in border-config [:config :width :between-children] 0)
        border-color (get-in border-config [:config :color])]
    (when (and (> border-width 0)
               (> (count children) 1)
               border-color
               (> (get border-color :a 0) 0))
      (let [gap (:child-gap layout 0)
            half-gap (/ gap 2)
            padding (:padding layout default-padding)
            horizontal? (= (:layout-direction layout) :left-to-right)
            parent-box (:bounding-box element)]
        (loop [child-indices (rest children)  ; Skip first child
               prev-child-idx (first children)
               offset (if horizontal?
                        (+ (:left padding) (- half-gap))
                        (+ (:top padding) (- half-gap)))
               result []]
          (if (empty? child-indices)
            result
            (let [prev-child (get-element state prev-child-idx)
                  prev-dims (:dimensions prev-child)
                  ;; Position offset at end of previous child
                  new-offset (+ offset
                                (if horizontal?
                                  (+ (:width prev-dims) gap)
                                  (+ (:height prev-dims) gap)))
                  ;; Create border rectangle between children
                  border-box (if horizontal?
                               {:x (+ (:x parent-box) new-offset (- half-gap))
                                :y (:y parent-box)
                                :width border-width
                                :height (:height parent-box)}
                               {:x (:x parent-box)
                                :y (+ (:y parent-box) new-offset (- half-gap))
                                :width (:width parent-box)
                                :height border-width})
                  border-cmd {:bounding-box border-box
                              :command-type :rectangle
                              :render-data {:color border-color}
                              :id (:id element)}]
              (recur (rest child-indices)
                     (first child-indices)
                     new-offset
                     (conj result border-cmd)))))))))

(defn- element->render-commands
  "Generate render commands for a single element."
  [state element]
  (let [{:keys [type bounding-box configs]} element
        base-commands (case type
                        :container
                        ;; Check for background color in configs
                        (let [bg-config (first (filter #(= :background (:type %)) configs))]
                          (when bg-config
                            [{:bounding-box bounding-box
                              :command-type :rectangle
                              :render-data {:color (get-in bg-config [:config :color])
                                            :corner-radius (get-in bg-config [:config :corner-radius]
                                                                   {:top-left 0 :top-right 0
                                                                    :bottom-left 0 :bottom-right 0})}
                              :id (:id element)}]))

                        :text
                        ;; Generate text render commands (one per wrapped line)
                        (let [{:keys [text-content text-config wrapped-lines]} element
                              {:keys [font-id font-size letter-spacing line-height text-color]} text-config
                              lh (or line-height font-size)]
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
                                              :text-color text-color}
                                :id (:id element)})
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
                                            :text-color text-color}
                              :id (:id element)}]))

                        nil)

        ;; Add border commands from configs
        border-config (first (filter #(= :border (:type %)) configs))
        border-commands (when border-config
                          [{:bounding-box bounding-box
                            :command-type :border
                            :render-data (:config border-config)
                            :id (:id element)}])

        ;; Add betweenChildren border commands
        between-children-commands (when border-config
                                    (generate-between-children-borders state element border-config))]
    (concat base-commands border-commands between-children-commands)))

(defn- generate-render-commands
  "Generate all render commands from layout elements.
   Commands are sorted by z-index for proper floating element rendering.
   Clip commands are inserted for scroll containers.

   Returns updated state with :render-commands populated."
  [state]
  (let [commands (mapcat #(element->render-commands state %) (:layout-elements state))
        filtered (vec (remove nil? commands))
        ;; Add clip commands for scroll containers
        with-clips (scroll/generate-clip-commands state filtered)
        sorted (floating/sort-render-commands-by-z state with-clips)]
    (assoc state :render-commands sorted)))

;; ============================================================================
;; CONVENIENCE FUNCTIONS
;; ============================================================================

(defn get-render-commands
  "Extract render commands from completed layout state."
  [state]
  (:render-commands state))

(defn get-scroll-container-data
  "Get scroll container data for an element by ID.

   Parameters:
   - state: Completed layout state
   - element-id: Element ID hash

   Returns scroll container data map or {:found false}."
  [state element-id]
  (scroll/get-scroll-container-data state element-id))

(defn update-scroll-position
  "Update the scroll position for a scroll container.

   Parameters:
   - state: Layout state
   - element-id: Element ID hash
   - delta: {:x :y} scroll delta

   Returns {:state updated-state :new-position {:x :y}}."
  [state element-id delta]
  (scroll/update-scroll-position state element-id delta))

(defn with-element
  "Helper for creating nested element structures.

   Usage:
   (-> state
       (with-element {:layout ...}
         (fn [s] (open-text-element s \"Hello\" {...} {...}))))"
  [state config body-fn]
  (-> state
      (open-element config)
      body-fn
      close-element))
