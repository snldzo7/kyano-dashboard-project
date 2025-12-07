(ns clay.layout.debug
  "Debug mode and helper functions - port of Clay.h debug system.

   This module provides:
   - set-debug-mode-enabled : Toggle debug visualization
   - debug-mode-enabled?    : Check if debug mode is on
   - set-culling-enabled    : Toggle off-screen element culling
   - culling-enabled?       : Check if culling is on
   - reset-text-cache       : Clear text measurement cache
   - generate-debug-overlay : Create debug visualization commands

   Clay.h Debug Mode:
   When enabled, Clay renders colored overlays showing element bounds,
   layout info, and a tree view. Our port generates render commands
   that can be rendered on top of the UI.")

;; ============================================================================
;; DEBUG STATE
;; ============================================================================

(def ^:dynamic *debug-mode* false)
(def ^:dynamic *culling-enabled* false)

(defn set-debug-mode-enabled
  "Enable or disable debug visualization mode.
   Port of Clay_SetDebugModeEnabled.

   When enabled, generate-debug-overlay produces render commands
   showing element bounds, IDs, and layout info."
  [state enabled]
  (assoc state :debug-mode enabled))

(defn debug-mode-enabled?
  "Check if debug mode is enabled.
   Port of Clay_IsDebugModeEnabled."
  [state]
  (get state :debug-mode false))

(defn set-culling-enabled
  "Enable or disable culling of off-screen elements.
   Port of Clay_SetCullingEnabled.

   When enabled, elements outside the viewport are not rendered.
   This improves performance for scrollable content."
  [state enabled]
  (assoc state :culling-enabled enabled))

(defn culling-enabled?
  "Check if culling is enabled."
  [state]
  (get state :culling-enabled false))

;; ============================================================================
;; TEXT CACHE MANAGEMENT
;; ============================================================================

(defn reset-text-cache
  "Clear the text measurement cache.
   Port of Clay_ResetMeasureTextCache.

   Call this when font settings change or to free memory."
  [state]
  (assoc state :text-cache {}))

(defn get-text-cache-stats
  "Get statistics about the text measurement cache."
  [state]
  (let [cache (:text-cache state {})]
    {:entry-count (count cache)
     :generation (:generation state 0)}))

;; ============================================================================
;; ELEMENT ID GENERATION (matches Clay_GetElementId / Clay_GetElementIdWithIndex)
;; ============================================================================

(defn- hash-string
  "Hash a string to a numeric ID.
   Port of Clay's string hashing (used for element ID generation)."
  [s]
  ;; Simple djb2 hash algorithm (matches Clay.h behavior)
  (reduce (fn [hash char]
            (bit-and (+ (bit-shift-left hash 5) hash (int char))
                     0x7FFFFFFF))
          5381
          s))

(defn get-element-id
  "Get element ID from a string identifier.
   Port of Clay_GetElementId.

   Parameters:
   - id-string: String identifier for the element

   Returns element ID map compatible with Clay layout system."
  [id-string]
  (let [hash (hash-string id-string)]
    {:id hash
     :offset 0
     :base-id hash
     :string-id hash}))

(defn get-element-id-with-index
  "Get element ID with index suffix.
   Port of Clay_GetElementIdWithIndex.

   Useful for elements in loops where you need unique IDs.

   Parameters:
   - id-string: Base string identifier
   - index: Numeric index

   Returns element ID with index incorporated into hash."
  [id-string index]
  (let [base-hash (hash-string id-string)
        indexed-hash (hash-string (str id-string "#" index))]
    {:id indexed-hash
     :offset index
     :base-id base-hash
     :string-id indexed-hash}))

;; ============================================================================
;; HOVER CALLBACKS
;; ============================================================================

(defn register-on-hover
  "Register a hover callback for the current element.
   Port of Clay_OnHover.

   The callback will be called when the element is hovered with:
   {:element-id id :pointer-data {:position :state} :user-data data}

   Parameters:
   - state: Layout state
   - callback: Function to call on hover
   - user-data: Optional user data passed to callback

   Returns updated state with callback registered."
  ([state callback] (register-on-hover state callback nil))
  ([state callback user-data]
   (let [current-idx (peek (:element-stack state))]
     (update-in state [:layout-elements current-idx]
                assoc :on-hover {:callback callback
                                 :user-data user-data}))))

(defn process-hover-callbacks
  "Process hover callbacks for all hovered elements.
   Call this after set-pointer-state.

   Returns vector of callback results."
  [state]
  (let [pointer-over-ids (:pointer-over-ids state [])
        pointer-data (:pointer-data state)
        elements (:layout-elements state)]
    (vec
     (for [el elements
           :let [el-id (get-in el [:id :id])
                 hover-config (:on-hover el)]
           :when (and hover-config
                      (some #(= el-id %) pointer-over-ids))]
       (let [{:keys [callback user-data]} hover-config]
         (when callback
           (callback {:element-id el-id
                      :pointer-data pointer-data
                      :user-data user-data})))))))

;; ============================================================================
;; DEBUG OVERLAY GENERATION
;; ============================================================================

(def debug-colors
  "Colors for debug overlay."
  {:bounds {:r 255 :g 0 :b 0 :a 0.3}     ; Red for bounds
   :padding {:r 0 :g 255 :b 0 :a 0.2}    ; Green for padding
   :content {:r 0 :g 0 :b 255 :a 0.2}    ; Blue for content
   :text {:r 255 :g 255 :b 0 :a 0.3}     ; Yellow for text
   :scroll {:r 255 :g 0 :b 255 :a 0.3}   ; Magenta for scroll
   :floating {:r 0 :g 255 :b 255 :a 0.3} ; Cyan for floating
   :hovered {:r 255 :g 128 :b 0 :a 0.5}  ; Orange for hovered
   :border-color {:r 255 :g 255 :b 255 :a 1}}) ; White borders

(defn- element-debug-color
  "Get debug color for an element based on its type and state."
  [element hovered?]
  (cond
    hovered? (:hovered debug-colors)
    (= :text (:type element)) (:text debug-colors)
    (some #(= :floating (:type %)) (:configs element [])) (:floating debug-colors)
    (some #(= :clip (:type %)) (:configs element [])) (:scroll debug-colors)
    :else (:bounds debug-colors)))

(defn- element->debug-commands
  "Generate debug overlay commands for a single element."
  [element hovered?]
  (let [{:keys [bounding-box type id]} element
        color (element-debug-color element hovered?)]
    [{;; Filled rectangle showing bounds
      :bounding-box bounding-box
      :command-type :rectangle
      :render-data {:color color}
      :id id
      :z-index 9998}
     ;; Border outline
     {:bounding-box bounding-box
      :command-type :border
      :render-data {:color (:border-color debug-colors)
                    :width {:left 1 :right 1 :top 1 :bottom 1}}
      :id id
      :z-index 9999}]))

(defn generate-debug-overlay
  "Generate debug visualization render commands.

   Creates colored overlays showing:
   - Element bounds (red)
   - Text elements (yellow)
   - Scroll containers (magenta)
   - Floating elements (cyan)
   - Hovered elements (orange)

   Parameters:
   - state: Layout state (after end-layout)

   Returns vector of debug render commands (high z-index)."
  [state]
  (when (debug-mode-enabled? state)
    (let [pointer-over-ids (set (:pointer-over-ids state []))
          elements (:layout-elements state)]
      (vec
       (mapcat (fn [element]
                 (let [el-id (get-in element [:id :id])
                       hovered? (contains? pointer-over-ids el-id)]
                   (element->debug-commands element hovered?)))
               elements)))))

;; ============================================================================
;; CULLING HELPERS
;; ============================================================================

(defn element-visible?
  "Check if an element is visible within the viewport.
   Used for culling when culling-enabled? is true."
  [element viewport]
  (let [{:keys [x y width height]} (:bounding-box element)
        vw (:width viewport)
        vh (:height viewport)]
    ;; Element is visible if its bounds intersect the viewport
    (and (< x vw)
         (< y vh)
         (> (+ x width) 0)
         (> (+ y height) 0))))

(defn apply-culling
  "Filter render commands to only include visible elements.
   Port of Clay.h culling behavior.

   Parameters:
   - state: Layout state
   - commands: Vector of render commands

   Returns filtered commands (or all commands if culling disabled)."
  [state commands]
  (if (culling-enabled? state)
    (let [viewport (:viewport state)]
      (filterv (fn [cmd]
                 (let [{:keys [x y width height]} (:bounding-box cmd)]
                   (and (< x (:width viewport))
                        (< y (:height viewport))
                        (> (+ x width) 0)
                        (> (+ y height) 0))))
               commands))
    commands))

;; ============================================================================
;; CONVENIENCE - HOVERED CHECK (like Clay_Hovered but for querying)
;; ============================================================================

(defn hovered?
  "Check if an element is currently hovered.
   Unlike Clay_Hovered() which works during element declaration,
   this works after set-pointer-state has been called.

   Parameters:
   - state: Layout state with pointer info
   - element-id: Element ID to check

   Returns true if element is under pointer."
  [state element-id]
  (some #(= element-id %) (:pointer-over-ids state [])))

(defn get-hovered-element
  "Get the topmost hovered element.
   Returns element map or nil."
  [state]
  (when-let [top-id (first (:pointer-over-ids state))]
    (first (filter #(= top-id (get-in % [:id :id]))
                   (:layout-elements state)))))
