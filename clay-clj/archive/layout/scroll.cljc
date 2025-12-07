(ns clay.layout.scroll
  "Scroll/Clip containers - faithful port of Clay.h clip functionality.

   In Clay.h, scroll containers are implemented via ClipElementConfig:
   - horizontal: clip on X axis
   - vertical: clip on Y axis
   - childOffset: offset applied to all child positions (scroll position)

   Render command flow:
   1. SCISSOR_START emitted when clip element is encountered in DFS
   2. All children rendered (with childOffset applied to positions)
   3. SCISSOR_END emitted after element's borders

   This module provides:
   - get-clip-config: Extract clip config from element
   - apply-child-offset: Apply scroll offset during position calculation
   - generate-scissor-commands: Insert scissor start/end during command generation")

;; ============================================================================
;; CLIP CONFIGURATION (matches Clay_ClipElementConfig)
;; ============================================================================

(defn clip-config?
  "Check if an element has clip/scroll configuration."
  [element]
  (some #(= :scroll (:type %)) (:configs element)))

(defn get-clip-config
  "Get the clip configuration for an element.
   Returns {:horizontal bool :vertical bool :child-offset {:x :y}} or nil."
  [element]
  (some (fn [cfg]
          (when (= :scroll (:type cfg))
            ;; Normalize to Clay.h structure
            (let [config (:config cfg)]
              {:horizontal (get config :horizontal false)
               :vertical (get config :vertical true)
               :child-offset (get config :child-offset {:x 0 :y 0})})))
        (:configs element)))

;; Aliases for backward compatibility with tests
(def scroll-config? clip-config?)
(def get-scroll-config get-clip-config)

;; ============================================================================
;; SCROLL CONTAINER DATA (matches Clay_ScrollContainerData)
;; ============================================================================

(defn build-scroll-container-data
  "Build scroll container data matching Clay_ScrollContainerData.

   Returns:
   {:scroll-position {:x :y}
    :scroll-container-dimensions {:width :height}
    :content-dimensions {:width :height}
    :config {:horizontal :vertical}
    :found true}"
  [element content-dims]
  (let [clip-cfg (get-clip-config element)
        box (:bounding-box element)]
    {:scroll-position (get clip-cfg :child-offset {:x 0 :y 0})
     :scroll-container-dimensions {:width (:width box)
                                   :height (:height box)}
     :content-dimensions content-dims
     :config {:horizontal (:horizontal clip-cfg false)
              :vertical (:vertical clip-cfg true)}
     :found true}))

;; ============================================================================
;; CHILD OFFSET APPLICATION
;; Port of Clay.h line 3113-3116:
;; childPosition.x = currentElementTreeNode->position.x + nextChildOffset.x + scrollOffset.x
;; childPosition.y = currentElementTreeNode->position.y + nextChildOffset.y + scrollOffset.y
;; ============================================================================

(defn- get-element [state idx]
  (get-in state [:layout-elements idx]))

(defn get-scroll-offset
  "Get the scroll offset for an element (from clip config's childOffset).
   This is applied to child positions during layout."
  [element]
  (if-let [clip-cfg (get-clip-config element)]
    (:child-offset clip-cfg {:x 0 :y 0})
    {:x 0 :y 0}))

(defn collect-clip-elements
  "Collect all clip/scroll elements from layout state.
   Returns vector of element indices that have clip configs."
  [state]
  (->> (:layout-elements state)
       (map-indexed (fn [idx el] (when (clip-config? el) idx)))
       (remove nil?)
       vec))

;; ============================================================================
;; SCISSOR COMMAND GENERATION
;; Port of Clay.h render command generation:
;; - SCISSOR_START when CLIP config encountered (line 2853-2861)
;; - SCISSOR_END after element borders (line 3078-3083)
;; ============================================================================

(defn- element-id [element]
  (get-in element [:id :id]))

(defn- make-scissor-start
  "Create SCISSOR_START render command for a clip element."
  [element]
  (let [clip-cfg (get-clip-config element)
        box (:bounding-box element)]
    {:bounding-box box
     :command-type :clip
     :render-data {:horizontal (:horizontal clip-cfg)
                   :vertical (:vertical clip-cfg)}
     :id (:id element)}))

(defn- make-scissor-end
  "Create SCISSOR_END render command for a clip element."
  [element]
  {:bounding-box (:bounding-box element)
   :command-type :clip-end
   :render-data {}
   :id (:id element)})

(defn generate-clip-commands
  "Insert scissor start/end commands around clip element content.

   Clay.h approach:
   - SCISSOR_START after clip element's own background
   - SCISSOR_END after clip element's children and borders

   Parameters:
   - state: Layout state
   - commands: Existing render commands (DFS ordered)

   Returns commands with scissor commands inserted."
  [state commands]
  (let [clip-indices (set (collect-clip-elements state))]
    (if (empty? clip-indices)
      commands
      ;; Build map: element-id -> element for clip elements
      (let [clip-elements (into {}
                                (map (fn [idx]
                                       (let [el (get-element state idx)]
                                         [(element-id el) {:element el :idx idx}]))
                                     clip-indices))

            ;; Build map: clip-element-id -> set of descendant ids
            descendant-map
            (into {}
                  (map (fn [idx]
                         (let [el (get-element state idx)
                               ;; Get all descendants recursively
                               get-descendants
                               (fn get-descendants [e]
                                 (let [children (:children e [])]
                                   (into (set (map #(element-id (get-element state %)) children))
                                         (mapcat #(get-descendants (get-element state %)) children))))]
                           [(element-id el) (get-descendants el)]))
                       clip-indices))]

        ;; Process commands and insert scissor commands
        (loop [result []
               remaining commands
               open-clips []]  ; Stack of open clip element ids
          (if (empty? remaining)
            ;; Close any remaining clips
            (vec (concat result
                         (map (fn [clip-id]
                                (make-scissor-end (:element (get clip-elements clip-id))))
                              (reverse open-clips))))
            (let [cmd (first remaining)
                  cmd-id (get-in cmd [:id :id])
                  rest-cmds (rest remaining)]

              ;; Check if this is a clip element's command - insert SCISSOR_START after it
              (if (contains? clip-elements cmd-id)
                (let [clip-el (:element (get clip-elements cmd-id))]
                  (recur (conj result cmd (make-scissor-start clip-el))
                         rest-cmds
                         (conj open-clips cmd-id)))

                ;; Check if we need to close any clips
                ;; A clip closes when no more of its descendants are in remaining commands
                (let [;; For each open clip, check if any descendants remain
                      still-open
                      (filterv (fn [clip-id]
                                 (let [descendants (get descendant-map clip-id)]
                                   ;; Still open if this command or any remaining is a descendant
                                   (or (contains? descendants cmd-id)
                                       (some #(contains? descendants (get-in % [:id :id])) rest-cmds))))
                               open-clips)

                      ;; Clips to close (those not still open)
                      clips-to-close (filter #(not (contains? (set still-open) %)) open-clips)

                      ;; Generate close commands in reverse order (LIFO)
                      close-cmds (map (fn [clip-id]
                                        (make-scissor-end (:element (get clip-elements clip-id))))
                                      (reverse clips-to-close))]
                  (recur (vec (concat result close-cmds [cmd]))
                         rest-cmds
                         still-open))))))))))

;; ============================================================================
;; SCROLL CONTAINER QUERIES (matches Clay_GetScrollContainerData)
;; ============================================================================

(defn get-scroll-container-data
  "Get scroll container data for an element by ID.
   Port of Clay_GetScrollContainerData.

   Returns scroll container data or {:found false}."
  [state element-id]
  (let [elements (:layout-elements state)]
    (or (first
         (keep-indexed
          (fn [_idx element]
            (when (and (= element-id (get-in element [:id :id]))
                       (clip-config? element))
              (let [;; Calculate content dimensions from children bounds
                    children (:children element [])
                    box (:bounding-box element)
                    content-dims
                    (if (empty? children)
                      {:width (:width box) :height (:height box)}
                      (let [child-bounds
                            (map (fn [child-idx]
                                   (let [c (get-element state child-idx)
                                         cb (:bounding-box c)]
                                     {:right (+ (:x cb) (:width cb))
                                      :bottom (+ (:y cb) (:height cb))}))
                                 children)]
                        {:width (- (apply max (map :right child-bounds)) (:x box))
                         :height (- (apply max (map :bottom child-bounds)) (:y box))}))]
                (build-scroll-container-data element content-dims))))
          elements))
        {:found false})))

;; ============================================================================
;; SCROLL POSITION UPDATE
;; ============================================================================

(defn clamp-scroll-position
  "Clamp scroll position to valid range.

   Parameters:
   - scroll-data: Scroll container data
   - new-position: {:x :y} new position

   Returns clamped {:x :y}."
  [scroll-data new-position]
  (let [{:keys [scroll-container-dimensions content-dimensions config]} scroll-data
        {:keys [horizontal vertical]} config
        max-x (max 0 (- (:width content-dimensions) (:width scroll-container-dimensions)))
        max-y (max 0 (- (:height content-dimensions) (:height scroll-container-dimensions)))
        x (if horizontal
            (-> (:x new-position 0) (max 0) (min max-x))
            0)
        y (if vertical
            (-> (:y new-position 0) (max 0) (min max-y))
            0)]
    {:x x :y y}))

(defn update-scroll-position
  "Update scroll position for a scroll container.

   Parameters:
   - state: Layout state
   - element-id: Element ID
   - delta: {:x :y} scroll delta

   Returns {:state new-state :new-position {:x :y}}."
  [state element-id delta]
  (let [elements (:layout-elements state)
        element-idx (first (keep-indexed
                            (fn [idx el]
                              (when (= element-id (get-in el [:id :id]))
                                idx))
                            elements))]
    (if (and element-idx (clip-config? (get-element state element-idx)))
      (let [scroll-data (get-scroll-container-data state element-id)
            current-pos (:scroll-position scroll-data {:x 0 :y 0})
            new-pos {:x (+ (:x current-pos 0) (:x delta 0))
                     :y (+ (:y current-pos 0) (:y delta 0))}
            clamped-pos (clamp-scroll-position scroll-data new-pos)]
        {:state (assoc-in state [:layout-elements element-idx :scroll-data :scroll-position]
                          clamped-pos)
         :new-position clamped-pos})
      {:state state
       :new-position {:x 0 :y 0}})))

;; ============================================================================
;; APPLY SCROLL POSITIONS (called during layout)
;; ============================================================================

(defn apply-scroll-positions
  "Apply scroll positions to clip elements.
   This sets the child-offset based on external scroll state.

   Parameters:
   - state: Layout state
   - scroll-positions: Map of element-id -> {:x :y}

   Returns updated state."
  [state scroll-positions]
  (if (empty? scroll-positions)
    state
    (reduce
     (fn [s [elem-id scroll-pos]]
       (let [elements (:layout-elements s)
             elem-idx (first (keep-indexed
                              (fn [idx el]
                                (when (= elem-id (get-in el [:id :id])) idx))
                              elements))]
         (if (and elem-idx (clip-config? (get-element s elem-idx)))
           ;; Update the scroll config's child-offset
           (update-in s [:layout-elements elem-idx :configs]
                      (fn [configs]
                        (mapv (fn [cfg]
                                (if (= :scroll (:type cfg))
                                  (assoc-in cfg [:config :child-offset]
                                            {:x (- (:x scroll-pos 0))
                                             :y (- (:y scroll-pos 0))})
                                  cfg))
                              configs)))
           s)))
     state
     scroll-positions)))
