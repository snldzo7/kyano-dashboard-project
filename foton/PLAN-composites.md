# Foton Composites Redesign Plan

## Problem

The current composite implementation has two critical flaws:

1. **Wraps children in raw `:div` elements** instead of using Foton atoms
2. **Wraps instead of transforms** - adds extra elements instead of modifying children

```clojure
;; WRONG - Current implementation
(defmethod core/render [:foton/fade-in :replicant-css]
  [_ _ {:keys [attrs children]}]
  [:div {:style animation-style}     ;; <-- Raw :div, not Foton atom!
    (into [:<>] children)])          ;; <-- Wraps instead of transforms
```

## Core Principles

### 1. Only Use Foton Atoms

Composites must ONLY emit Foton primitives:
- `:foton.css/frame`
- `:foton.css/text`
- `:foton.css/icon`
- etc.

Never raw HTML: no `:div`, `:span`, `:p` etc.

### 2. Composites are Transformers

Composites should be **pure tree transformers** that modify children using Specter:

```clojure
;; CORRECT - Transform children in place, use only Foton atoms
(defmethod core/render [:foton/fade-in :replicant-css]
  [_ _ {:keys [attrs children]}]
  (let [animation-style (build-animation-style attrs)]
    ;; Transform each child by merging animation style into it
    (mapv #(fs/merge-style % animation-style) children)))
```

Reference pattern from `clay/theme.cljc`:
```clojure
(defn apply-theme [tree tokens]
  (sp/transform [ALL-ELEMENTS sp/MAP-VALS]
    (fn [v] (resolve-token tokens v))
    tree))

---

## Implementation Steps

### 1. Fix Animation Composites

**Current issues:**
- Wraps children in `[:div ...]`
- Uses `[:<>]` (React fragment - not supported in Replicant)
- Uses keyword CSS variables like `:--foton-slide-distance` (invalid)

**Fix:**
- Transform each child by merging animation styles directly
- For keyframe animations, inject animation name + inline styles into child
- Use string keys for CSS custom properties if needed: `{"--foton-slide-distance" "20px"}`

```clojure
(defmethod core/render [:foton/fade-in :replicant-css]
  [_ _ {:keys [attrs children]}]
  (let [style {:animation (str "foton-fade-in "
                               (get attrs :duration 300) "ms "
                               (easing->css (get attrs :easing :ease-out))
                               " forwards")
               :opacity 0}]
    (mapv #(fs/merge-style % style) children)))
```

### 2. Fix Resizable Composite

**Current issue:** Wraps children in raw `:div` with resize handles

**Challenge:** Resizable NEEDS resize handles as siblings to the content.

**Solution:** Transform first child to be the container, inject handles as its children:

```clojure
(defmethod core/render [:foton/resizable :replicant-css]
  [_ _ {:keys [attrs children]}]
  (let [{:keys [entity-id width height handles]} attrs
        ;; Single child: transform it to be container
        ;; Multiple children: wrap in :foton.css/frame
        container (if (= 1 (count children))
                    (first children)
                    [:foton.css/frame {} children])]
    (-> container
        (fs/merge-style {:position "relative"
                         :width width
                         :height height})
        ;; Inject resize handles as children (using Foton frame for handles)
        (inject-resize-handles entity-id handles))))

;; Resize handle uses :foton.css/frame, not raw :div
(defn- resize-handle [entity-id position]
  [:foton.css/frame {:style (handle-style position)
                     :on {:mousedown [[:foton/resize-start entity-id position]]}}])
```

### 3. Fix Draggable Composite

**Current issue:** Wraps in raw absolute-positioned `:div`

**Fix:** Transform children directly - add drag styles and handlers:

```clojure
(defmethod core/render [:foton/draggable :replicant-css]
  [_ _ {:keys [attrs children]}]
  (let [{:keys [entity-id x y dragging?]} attrs
        style {:position "absolute"
               :left (when x (str x "px"))
               :top (when y (str y "px"))
               :cursor (if dragging? "grabbing" "grab")
               :user-select "none"}]
    ;; Transform each child - no wrapping
    (mapv (fn [child]
            (-> child
                (fs/merge-style style)
                (fs/merge-attrs {:data-draggable (str entity-id)
                                 :on {:mousedown [[:foton/drag-start entity-id]]}})))
          children)))
```

No raw HTML needed - we transform the existing Foton elements.

### 4. Composability

Composites must compose correctly when nested:

```clojure
[:foton/fade-in {:duration 300}
  [:foton/slide-up {:distance 20}
    [:foton.css/frame {...} content]]]
```

The inner `slide-up` transforms the frame.
The outer `fade-in` transforms the already-transformed frame.
Result: frame has both slide and fade styles merged.

---

## Special Cases

### When Wrapping IS Needed

Some composites genuinely need container elements (e.g., resize handles as siblings). In these cases:

1. Transform children first
2. If wrapping needed, use `:foton.css/frame` - NEVER raw `:div`
3. Prefer transforming the first child to BE the container if single child

### Injecting CSS Keyframes

Animation keyframes need to be injected into the page once. Options:

1. **Inline via style tag**: Create a style element with keyframes on first use
2. **Separate CSS file**: Include animation keyframes in a static CSS file
3. **Dynamic generation**: Generate keyframes per-animation with unique IDs

Recommendation: Option 2 (separate CSS file) for simplicity.

---

## Files to Modify

1. **foton/composites/animations.cljc** - Rewrite as transformers
2. **foton/composites/resizable.cljc** - Transform first child, inject handles
3. **foton/composites/draggable.cljc** - Transform children with drag styles
4. **foton/specter.cljc** - May need additional helpers
5. **resources/public/css/foton-animations.css** - Create static keyframes file (if needed)

---

## Validation Checklist

- [ ] Only Foton atoms used (`:foton.css/*`) - NO raw HTML (`:div`, `:span`, etc.)
- [ ] No extra wrappers unless structurally required
- [ ] No React fragments `[:<>]` - use vectors directly
- [ ] No keyword CSS variables (`:--foo`) - use string keys
- [ ] Composites compose correctly when nested
- [ ] Single child case handled (transform it, don't wrap)
- [ ] Multiple children case handled (wrap in `:foton.css/frame`)
- [ ] Works with Replicant rendering
