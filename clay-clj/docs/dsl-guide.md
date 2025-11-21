# Clay-CLJ DSL Guide

A reactive, Clojure-native DSL for declarative UI layout, inspired by Clay.

## Reactive vs Linear

**Clay-CLJ is fundamentally different from Clay (C).** Clay runs a linear pipeline once per frame. Clay-CLJ maintains a reactive computation graph that automatically recomputes when inputs change.

### Clay (C) - Linear Pipeline

```text
BeginLayout() → Measure → Layout → Render → EndLayout() → (repeat next frame)
```

You explicitly call layout functions each frame. State is managed imperatively.

### Clay-CLJ - Reactive DAG

```text
pointer-flow ──────┐
                   │
dimensions-flow ───┼──► layout-flow ──► render-commands-flow
                   │         │                    │
scroll-flow ───────┤         │                    ▼
                   │         ▼              (to renderer)
app-tree-flow ─────┘   hovered-ids-flow
                            │
                            └──────► (feedback to app)
```

**Key differences:**

- **No frame loop** - Layout recomputes automatically when ANY input flow changes
- **Parallel computation** - Independent subtrees compute concurrently
- **Distributed** - Layout can run on server, rendering on client (CLJ-CLJS)
- **Always live** - No "begin/end" - the UI is a continuous reactive signal

## DSL Syntax

### Element Structure

```clojure
[:id {props} [children]]
```

- **id**: Keyword identifying the element (`:sidebar`, `:button`, etc.)
- **props**: Single map containing all properties (visual + layout)
- **children**: Vector of child elements

### Text Elements

```clojure
[:text "content" {text-props}]
```

Text is a special case with content as second element.

### Element IDs

IDs are used for:

- Referencing elements for floating attachment
- Tracking hover/click state
- Caching and reconciliation

```clojure
;; Simple keyword ID
[:sidebar {...} [...]]

;; Indexed IDs (for list items)
[(keyword (str "item-" i)) {...} [...]]

;; Or use a vector for compound IDs
[[:list-item i] {...} [...]]
```

Compound IDs are hashed together to create unique element IDs.

---

## Shorthand Quick Reference

All properties support concise vector notation:

| Property   | Shorthand                          | Expanded                                    |
| ---------- | ---------------------------------- | ------------------------------------------- |
| `:size`    | `[300 :grow]`                      | `[[:fixed 300] [:grow 0]]`                  |
| `:size`    | `[:% 0.5 :fit]`                    | `[[:percent 0.5] [:fit 0]]`                 |
| `:pad`     | `16`                               | `{:left 16 :right 16 :top 16 :bottom 16}`   |
| `:pad`     | `[16 8]`                           | `{:left 16 :right 16 :top 8 :bottom 8}`     |
| `:align`   | `:center`                          | `{:x :center :y :center}`                   |
| `:bg`      | `:red`                             | `{:r 255 :g 0 :b 0 :a 255}`                 |
| `:bg`      | `"#FF0000"`                        | `{:r 255 :g 0 :b 0 :a 255}`                 |
| `:radius`  | `8`                                | `{:top-left 8 ...}`                         |
| `:border`  | `[:red 1]`                         | `{:color {...} :width {...}}`               |
| `:float`   | `{:to :parent :at [...] ...}`      | `{:attach-to :parent :attach-points {...}}` |
| `:scroll`  | `:y`                               | `{:axis :y :momentum true}`                 |
| `:id`      | `:my-id`                           | `{:id :my-id :id-hash 123456}`              |
| `:id`      | `[:item i]`                        | `{:id [:item i] :id-hash 789012}`           |
| `:clip`    | `true`                             | `{:x true :y true}`                         |
| `:image`   | `["url" 16/9]`                     | `{:src "url" :aspect 16/9}`                 |

---

## Property Reference

### Sizing

Controls element dimensions. Format: `[width height]`

| Syntax              | Meaning                        |
| ------------------- | ------------------------------ |
| `:grow`             | Expand to fill available space |
| `[:grow min]`       | Grow with minimum constraint   |
| `[:grow min max]`   | Grow with min and max          |
| `:fit`              | Shrink to content              |
| `[:fit min max]`    | Fit with constraints           |
| `300`               | Fixed pixel size (number)      |
| `[:fixed value]`    | Fixed pixel size (explicit)    |
| `[:percent 0.5]`    | Percentage of parent           |
| `[:% 0.5]`          | Percentage shorthand           |

**Examples:**

```clojure
:size [:grow :grow]                    ; fill both axes
:size :grow                            ; fill both axes
:size [[:fixed 300] :grow]             ; fixed width, grow height
:size [300 :grow]                      ; fixed width, grow height
:size [[:grow 100 500] [:fixed 60]]    ; constrained grow, fixed height
:size [[:percent 0.5] :fit]            ; 50% width, fit height
:size [[:% 0.5] :fit]                  ; 50% width, fit height
```

### Direction

```clojure
:dir :row    ; left to right (default)
:dir :col    ; top to bottom
```

### Padding

| Syntax         | Meaning                     |
| -------------- | --------------------------- |
| `16`         | All sides                   |
| `[16 8]`     | `[horizontal vertical]`   |
| `[16 8 4 2]` | `[left right top bottom]` |

### Gap

Space between children:

```clojure
:gap 16
```

### Alignment

| Syntax              | Meaning                      |
| ------------------- | ---------------------------- |
| `:center`         | Both axes centered           |
| `[:center :top]`  | `[x y]` alignment          |
| `[:left :bottom]` | Left-aligned, bottom-aligned |

X values: `:left`, `:center`, `:right`
Y values: `:top`, `:center`, `:bottom`

### Colors

Colors can be specified in multiple formats. All resolve to canonical `{:r :g :b :a}` maps.

#### Tailwind Colors (Recommended)

Use Tailwind color keywords directly - no imports needed:

```clojure
:red-500            ; Tailwind red
:blue-300           ; Tailwind blue
:slate-100          ; Tailwind slate
:emerald-600        ; Tailwind emerald
:amber-400          ; Tailwind amber
:white              ; Tailwind white
:black              ; Tailwind black
```

Full palette: `slate`, `gray`, `zinc`, `neutral`, `stone`, `red`, `orange`, `amber`, `yellow`, `lime`, `green`, `emerald`, `teal`, `cyan`, `sky`, `blue`, `indigo`, `violet`, `purple`, `fuchsia`, `pink`, `rose` (shades: 50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 950)

#### HTML Named Colors

```clojure
:coral              ; HTML coral
:tomato             ; HTML tomato
:salmon             ; HTML salmon
:indian-red         ; HTML indian red
:lime-green         ; HTML lime green
```

140+ HTML named colors supported.

#### RGBA Vectors

```clojure
[255 0 0]           ; RGB (alpha defaults to 255)
[255 0 0 128]       ; RGBA with alpha
```

#### Hex Strings

```clojure
"#FF0000"           ; 6-digit hex (RGB)
"#FF000080"         ; 8-digit hex (RGBA)
"#F00"              ; 3-digit shorthand
```

#### Color Functions

Use `clay.color` functions for any color space:

```clojure
(require '[clay.color :as c])

;; Color spaces
(c/rgb 255 100 50)         ; RGB
(c/hsl 200 80 50)          ; HSL (H: 0-360, S/L: 0-100)
(c/hsv 200 80 80)          ; HSV/HSB
(c/oklch 0.7 0.15 200)     ; OKLCH (perceptually uniform)
(c/lab 50 20 -30)          ; CIE LAB
(c/lch 50 40 200)          ; CIE LCH
(c/hwb 200 10 10)          ; HWB
(c/cmyk 0 100 100 0)       ; CMYK
(c/xyz 41 21 2)            ; CIE XYZ
```

#### Color Manipulation

```clojure
(c/darken :blue-500 0.2)           ; 20% darker
(c/lighten :red-500 0.3)           ; 30% lighter
(c/alpha :black 0.5)               ; Set alpha to 50%
(c/mix :blue-500 :red-500 0.5)     ; 50% blend
(c/invert :white)                  ; Invert color
(c/grayscale :blue-500)            ; Convert to grayscale
```

#### Functional Colors

Colors can be functions that evaluate at render time:

```clojure
;; Hover effect
[:button
 {:bg #(if (contains? (:hovered-ids %) :button)
         :blue-600
         :blue-500)}
 [...]]

;; Pressed state
[:card
 {:bg #(case (get-in % [:pointer :state])
         :pressed :slate-200
         :slate-100)}
 [...]]
```

**Evaluation context available to functional colors:**

```clojure
{:hovered-ids #{:button :sidebar}
 :pointer {:x 100 :y 200 :state :pressed}
 :scroll-offsets {:list {:x 0 :y 150}}
 :frame-time 0.016}
```

**All formats resolve to canonical RGBA:**

```clojure
{:r 255 :g 0 :b 0 :a 255}
```

### Background

```clojure
:bg [240 240 240]
:bg :slate-100
:bg "#F0F0F0"
:background :neutral-50
```

### Themes

Themes are Specter navigators that transform the UI tree. This enables dark mode, accessibility modes, and design tokens without modifying your DSL.

#### Semantic Tokens

Define a design system with semantic names:

```clojure
(def light-theme
  {:surface-primary :slate-50
   :surface-secondary :slate-100
   :text-primary :slate-900
   :text-secondary :slate-600
   :accent :blue-500
   :radius-sm 4
   :radius-md 8})

(def dark-theme
  {:surface-primary :slate-900
   :surface-secondary :slate-800
   :text-primary :slate-50
   :text-secondary :slate-400
   :accent :blue-400
   :radius-sm 6
   :radius-md 12})
```

Use tokens in DSL:

```clojure
[:card {:bg :surface-primary :radius :radius-md}
 [:text "Title" {:color :text-primary}]]
```

#### Theme Application with Specter

```clojure
(require '[com.rpl.specter :as sp])

;; Navigate all elements
(def ALL-ELEMENTS
  (sp/recursive-path [] p
    (sp/if-path :children
      (sp/stay-then-continue :children sp/ALL p)
      sp/STAY)))

;; Resolve tokens
(defn apply-theme [tree tokens]
  (sp/transform
    [ALL-ELEMENTS (sp/submap [:bg :color :border-color :radius])]
    (fn [props]
      (reduce-kv
        (fn [m k v] (assoc m k (get tokens v v)))
        {}
        props))
    tree))

;; Usage
(apply-theme my-ui dark-theme)
```

#### Color Modes

Apply color transformations across the entire tree:

```clojure
(require '[clay.color :as c])

;; Dark mode transformation
(defn apply-dark-mode [tree]
  (sp/transform
    [ALL-ELEMENTS (sp/must :bg)]
    (fn [el] (update el :bg c/dark-mode))
    tree))

;; Colorblind simulations
(defn apply-colorblind [tree mode]
  (let [transform-fn (case mode
                       :protanopia c/protanopia
                       :deuteranopia c/deuteranopia
                       :tritanopia c/tritanopia)]
    (sp/transform
      [ALL-ELEMENTS (sp/must :bg)]
      (fn [el] (update el :bg #(transform-fn (c/resolve-color %))))
      tree)))
```

### Corner Radius

| Syntax        | Meaning                                       |
| ------------- | --------------------------------------------- |
| `8`         | All corners                                   |
| `[8 8 0 0]` | `[topLeft topRight bottomLeft bottomRight]` |

```clojure
:radius 8
:radius [8 8 0 0]   ; rounded top, square bottom
```

### Border

Vector shorthand: `[color width between?]`

| Syntax                              | Meaning                          |
| ----------------------------------- | -------------------------------- |
| `[:red 1]`                          | Color + uniform width            |
| `[:red 1 2]`                        | Color + width + between-children |
| `[[100 100 100] [3 9 6 12]]`        | Color + per-side width [l r t b] |
| `[[100 100 100] [3 9 6 12] 2]`      | + between-children               |

Map form (explicit):

```clojure
:border {:color [200 200 200]
         :width 1}

:border {:color [100 100 100]
         :width [3 9 6 12]    ; [l r t b]
         :between 2}          ; between children
```

Examples:

```clojure
:border [:red 1]                           ; simple red border
:border ["#FF0000" 2]                      ; hex color, 2px
:border [[100 100 100] [3 9 6 12] 2]       ; full control
```

### Image

Image sources can be specified in multiple formats:

| Syntax                    | Meaning                          |
| ------------------------- | -------------------------------- |
| `"https://..."`           | URL (remote image)               |
| `"/path/to/image.png"`    | Local file path                  |
| `"data:image/png;base64,..."` | Data URI (inline)            |
| `image-handle`            | Platform-specific handle/pointer |

```clojure
;; URL
:image "https://example.com/avatar.png"

;; Local path
:image "/assets/icons/menu.svg"

;; Data URI
:image "data:image/svg+xml;base64,PHN2Zy..."

;; Platform handle (for pre-loaded images)
:image my-loaded-texture

;; With aspect ratio
:image {:src "https://example.com/photo.jpg"
        :aspect 16/9}

;; Shorthand with aspect
:image ["https://example.com/photo.jpg" 16/9]
```

#### Image Properties

```clojure
{:image {:src "..."           ; image source (required)
         :aspect 1.0          ; aspect ratio (width/height)
         :fit :cover          ; :cover, :contain, :fill, :none
         :position [:center :center]}}  ; [x y] alignment
```

Fit modes:

- `:cover` - Scale to cover, may crop
- `:contain` - Scale to fit, may letterbox
- `:fill` - Stretch to fill (distorts)
- `:none` - Original size

#### Aspect Ratio (standalone)

```clojure
:aspect 1.0           ; square
:aspect 16/9          ; widescreen
:aspect 4/3           ; standard
:aspect 0.5           ; portrait (1:2)
```

### Floating (Absolute Positioning)

```clojure
:float {:to :parent              ; attach to parent
        :at [:center-top         ; element attach point
             :center-bottom]     ; parent attach point
        :offset [0 8]            ; [x y] offset
        :z 1000                  ; z-index
        :capture :pass           ; pointer capture mode
        :clip-to :parent}        ; clip to attached parent
```

Attach targets (`:to`):

- `:none` - disable floating (default)
- `:parent` - attach to parent element
- `:root` - attach to layout root (absolute positioning)
- `:element-id` - attach to element with specific ID (requires `:parent-id`)

```clojure
;; Attach to specific element by ID
:float {:to :element-id
        :parent-id :my-target-element
        :at [:center-top :center-bottom]
        :offset [0 8]}

;; Absolute positioning (attach to root)
:float {:to :root
        :offset [100 100]}
```

Attach points (`:at`):

- `:left-top`, `:center-top`, `:right-top`
- `:left-center`, `:center-center`, `:right-center`
- `:left-bottom`, `:center-bottom`, `:right-bottom`

Capture modes (`:capture`):

- `:capture` - capture pointer events (default)
- `:pass` - pass through to elements underneath

Clip modes (`:clip-to`):

- `:none` - no clipping (default)
- `:parent` - clip to attached parent's bounds

### Scroll

```clojure
:scroll :y       ; vertical scroll
:scroll :x       ; horizontal scroll
:scroll :both    ; both axes
```

Full scroll configuration:

```clojure
:scroll {:axis :y                    ; :x, :y, or :both
         :momentum true              ; enable momentum scrolling
         :initial [0 0]}             ; initial scroll offset [x y]
```

### Clip

Clip content to element bounds (used with scroll):

```clojure
:clip true                           ; clip both axes
:clip :x                             ; clip horizontal only
:clip :y                             ; clip vertical only
:clip {:x true :y false}             ; explicit
```

### Text Properties

Full text configuration for `:text` elements:

```clojure
[:text "Hello World"
 {:size 16                           ; font size (required)
  :color [0 0 0]                     ; text color (required)
  :font 0                            ; font ID
  :weight :normal                    ; :normal, :bold, :light
  :spacing 0                         ; letter spacing
  :line-height 20                    ; line height in pixels
  :align :left                       ; :left, :center, :right
  :wrap :words                       ; :words, :newlines, :none
  :hash-string true}]                ; include string in ID hash
```

Wrap modes:

- `:words` - Wrap at word boundaries (default)
- `:newlines` - Only wrap at explicit newlines
- `:none` - No wrapping, single line

### Custom Elements

For custom rendering (platform-specific):

```clojure
:custom {:type :my-widget
         :data {:any "data"}}
```

The renderer receives this data and handles custom drawing.

### User Data

Attach arbitrary data to elements:

```clojure
:data {:custom "anything"
       :on-click #(println "clicked")
       :state atom-ref}
```

### Pointer Interaction

Layout is pure data-in, data-out. Hover state follows the immediate-mode pattern with a 1-frame delay:

**Frame N:**

1. Build element tree using `hovered-ids` from frame N-1
2. Call `(layout input)` → returns render commands + NEW `hovered-ids`
3. Render the commands

**Frame N+1:**

1. Use the NEW `hovered-ids` to build element tree
2. ... repeat

```clojure
;; Layout input/output
(def input
  {:tree (my-ui hovered-ids)     ; pass previous frame's hover state
   :dimensions {:width 800 :height 600}
   :pointer {:position {:x 100 :y 200}
             :state :released}})

(def output (layout input))
;; => {:render-commands [...]
;;     :hovered-ids #{:button :sidebar}  ; computed from THIS frame
;;     :scroll-data {...}
;;     :element-data {...}}

;; Your render function uses hovered-ids from previous frame
(defn my-button [id hovered-ids]
  [id
   {:bg (if (contains? hovered-ids id)
          [80 120 200]    ; hovered
          [100 140 220])} ; normal
   [[:text "Click me" {:size 14 :color [255 255 255]}]]])
```

**How hover is computed internally:**

After layout computes bounding boxes, the engine:

1. Does DFS through element tree (highest z-index first)
2. Checks if pointer is inside each element's bounding box
3. Respects clipping bounds
4. Respects `pointer-capture-mode` for floating elements
5. Returns set of all hovered element IDs

```clojure
;; Internal hover computation (simplified)
(defn compute-hovered-ids [positioned-tree pointer-pos]
  (let [hovered (atom #{})]
    (dfs-walk positioned-tree
      (fn [element]
        (when (point-in-box? pointer-pos (:bounding-box element))
          (swap! hovered conj (:id element)))))
    @hovered))
```

### Visibility

Conditionally show/hide elements:

```clojure
:visible true                        ; default
:visible false                       ; hidden but reserves space
:visible :collapse                   ; hidden and collapses space
```

---

## Example 1: Basic Sidebar Layout

### Clay C

```c
CLAY(CLAY_ID("OuterContainer"), {
    .layout = {
        .layoutDirection = CLAY_LEFT_TO_RIGHT,
        .sizing = { CLAY_SIZING_GROW(0), CLAY_SIZING_GROW(0) },
        .padding = CLAY_PADDING_ALL(16),
        .childGap = 16
    },
    .backgroundColor = { 250, 250, 255, 255 }
}) {
    CLAY(CLAY_ID("Sidebar"), {
        .layout = {
            .layoutDirection = CLAY_TOP_TO_BOTTOM,
            .sizing = { CLAY_SIZING_FIXED(300), CLAY_SIZING_GROW(0) },
            .padding = CLAY_PADDING_ALL(16),
            .childGap = 8
        },
        .backgroundColor = { 224, 215, 210, 255 }
    }) {
        CLAY_TEXT(CLAY_STRING("Menu"), CLAY_TEXT_CONFIG({
            .fontSize = 24,
            .textColor = { 0, 0, 0, 255 }
        }));
    }

    CLAY(CLAY_ID("MainContent"), {
        .layout = {
            .layoutDirection = CLAY_TOP_TO_BOTTOM,
            .sizing = { CLAY_SIZING_GROW(0), CLAY_SIZING_GROW(0) },
            .padding = CLAY_PADDING_ALL(16),
            .childGap = 16
        }
    }) {
        CLAY_TEXT(CLAY_STRING("Welcome"), CLAY_TEXT_CONFIG({
            .fontSize = 32,
            .textColor = { 0, 0, 0, 255 }
        }));
    }
}
```

### DSL

```clojure
[:outer-container
 {:bg [250 250 255]
  :dir :row
  :size [:grow :grow]
  :pad 16
  :gap 16}
 [
  [:sidebar
   {:bg [224 215 210]
    :dir :col
    :size [[:fixed 300] :grow]
    :pad 16
    :gap 8}
   [
    [:text "Menu" {:size 24 :color [0 0 0]}]
   ]]

  [:main-content
   {:dir :col
    :size [:grow :grow]
    :pad 16
    :gap 16}
   [
    [:text "Welcome" {:size 32 :color [0 0 0]}]
   ]]
 ]]
```

### Parsed (Canonical Form)

```clojure
{:id :outer-container
 :id-hash 2847593821
 :background-color {:r 250 :g 250 :b 255 :a 255}
 :layout {:direction :left-to-right
          :sizing {:width {:type :grow :min 0}
                   :height {:type :grow :min 0}}
          :padding {:left 16 :right 16 :top 16 :bottom 16}
          :child-gap 16
          :child-alignment {:x :left :y :top}}
 :children
 [{:id :sidebar
   :id-hash 9283745123
   :background-color {:r 224 :g 215 :b 210 :a 255}
   :layout {:direction :top-to-bottom
            :sizing {:width {:type :fixed :value 300}
                     :height {:type :grow :min 0}}
            :padding {:left 16 :right 16 :top 16 :bottom 16}
            :child-gap 8
            :child-alignment {:x :left :y :top}}
   :children
   [{:id :text-0
     :id-hash 1293847561
     :type :text
     :content "Menu"
     :text-config {:font-size 24
                   :text-color {:r 0 :g 0 :b 0 :a 255}
                   :wrap-mode :words}}]}

  {:id :main-content
   :id-hash 8374651923
   :layout {:direction :top-to-bottom
            :sizing {:width {:type :grow :min 0}
                     :height {:type :grow :min 0}}
            :padding {:left 16 :right 16 :top 16 :bottom 16}
            :child-gap 16
            :child-alignment {:x :left :y :top}}
   :children
   [{:id :text-1
     :id-hash 7364518273
     :type :text
     :content "Welcome"
     :text-config {:font-size 32
                   :text-color {:r 0 :g 0 :b 0 :a 255}
                   :wrap-mode :words}}]}]}
```

---

## Example 2: Button with Floating Tooltip

### Clay C

```c
CLAY(CLAY_ID("Button"), {
    .layout = { .padding = CLAY_PADDING_ALL(16) },
    .backgroundColor = Clay_Hovered() ? (Clay_Color){80, 120, 200, 255} : (Clay_Color){100, 140, 220, 255},
    .cornerRadius = CLAY_CORNER_RADIUS(6)
}) {
    CLAY_TEXT(CLAY_STRING("Hover me"), CLAY_TEXT_CONFIG({
        .fontSize = 16,
        .textColor = { 255, 255, 255, 255 }
    }));

    if (Clay_Hovered()) {
        CLAY(CLAY_ID("Tooltip"), {
            .floating = {
                .attachTo = CLAY_ATTACH_TO_PARENT,
                .attachPoints = {
                    .element = CLAY_ATTACH_POINT_CENTER_TOP,
                    .parent = CLAY_ATTACH_POINT_CENTER_BOTTOM
                },
                .offset = { 0, 8 },
                .zIndex = 1000,
                .pointerCaptureMode = CLAY_POINTER_CAPTURE_MODE_PASSTHROUGH
            },
            .layout = { .padding = CLAY_PADDING_ALL(8) },
            .backgroundColor = { 50, 50, 50, 255 },
            .cornerRadius = CLAY_CORNER_RADIUS(4)
        }) {
            CLAY_TEXT(CLAY_STRING("This is a tooltip"), CLAY_TEXT_CONFIG({
                .fontSize = 12,
                .textColor = { 255, 255, 255, 255 }
            }));
        }
    }
}
```

### DSL

```clojure
;; As a function with hover state passed in
(defn button-with-tooltip [hovered?]
  [:button
   {:bg (if hovered? [80 120 200] [100 140 220])
    :radius 6
    :pad 16}
   [
    [:text "Hover me" {:size 16 :color [255 255 255]}]

    (when hovered?
      [:tooltip
       {:bg [50 50 50]
        :radius 4
        :pad 8
        :float {:to :parent
                :at [:center-top :center-bottom]
                :offset [0 8]
                :z 1000
                :capture :pass}}
       [
        [:text "This is a tooltip" {:size 12 :color [255 255 255]}]
       ]])
   ]])
```

### Parsed (Canonical Form, hovered? = true)

```clojure
{:id :button
 :id-hash 8273645192
 :background-color {:r 80 :g 120 :b 200 :a 255}
 :corner-radius {:top-left 6 :top-right 6 :bottom-left 6 :bottom-right 6}
 :layout {:padding {:left 16 :right 16 :top 16 :bottom 16}
          :child-alignment {:x :left :y :top}}
 :children
 [{:id :text-0
   :id-hash 9182736450
   :type :text
   :content "Hover me"
   :text-config {:font-size 16
                 :text-color {:r 255 :g 255 :b 255 :a 255}
                 :wrap-mode :words}}

  {:id :tooltip
   :id-hash 1928374651
   :background-color {:r 50 :g 50 :b 50 :a 255}
   :corner-radius {:top-left 4 :top-right 4 :bottom-left 4 :bottom-right 4}
   :layout {:padding {:left 8 :right 8 :top 8 :bottom 8}
            :child-alignment {:x :left :y :top}}
   :floating {:attach-to :parent
              :attach-points {:element :center-top
                              :parent :center-bottom}
              :offset {:x 0 :y 8}
              :z-index 1000
              :pointer-capture-mode :passthrough}
   :children
   [{:id :text-1
     :id-hash 8374651928
     :type :text
     :content "This is a tooltip"
     :text-config {:font-size 12
                   :text-color {:r 255 :g 255 :b 255 :a 255}
                   :wrap-mode :words}}]}]}
```

---

## Example 3: Scrollable List

### Clay C

```c
CLAY(CLAY_ID("ListContainer"), {
    .layout = {
        .sizing = { CLAY_SIZING_GROW(0), CLAY_SIZING_FIXED(400) }
    },
    .clip = {
        .vertical = true,
        .childOffset = Clay_GetScrollOffset()
    },
    .backgroundColor = { 240, 240, 240, 255 }
}) {
    for (int i = 0; i < 10; i++) {
        CLAY(CLAY_IDI("ListItem", i), {
            .layout = {
                .padding = CLAY_PADDING_ALL(12),
                .sizing = { CLAY_SIZING_GROW(0), CLAY_SIZING_FIXED(50) }
            },
            .backgroundColor = Clay_Hovered() ? (Clay_Color){200, 200, 200, 255} : (Clay_Color){220, 220, 220, 255}
        }) {
            CLAY_TEXT(items[i], textConfig);
        }
    }
}
```

### DSL

```clojure
(defn scrollable-list [items hovered-index]
  [:list-container
   {:bg [240 240 240]
    :size [:grow [:fixed 400]]
    :scroll :y}
   (vec
    (for [[i item] (map-indexed vector items)]
      [(keyword (str "list-item-" i))
       {:bg (if (= hovered-index i) [200 200 200] [220 220 220])
        :size [:grow [:fixed 50]]
        :pad 12}
       [
        [:text item {:size 14 :color [0 0 0]}]
       ]]))])
```

### Parsed (Canonical Form)

```clojure
{:id :list-container
 :id-hash 9182736451
 :background-color {:r 240 :g 240 :b 240 :a 255}
 :layout {:sizing {:width {:type :grow :min 0}
                   :height {:type :fixed :value 400}}
          :child-alignment {:x :left :y :top}}
 :clip {:vertical true
        :horizontal false}
 :children
 [{:id :list-item-0
   :id-hash 1827364591
   :background-color {:r 220 :g 220 :b 220 :a 255}
   :layout {:sizing {:width {:type :grow :min 0}
                     :height {:type :fixed :value 50}}
            :padding {:left 12 :right 12 :top 12 :bottom 12}
            :child-alignment {:x :left :y :top}}
   :children
   [{:id :text-0
     :type :text
     :content "Item 1"
     :text-config {:font-size 14
                   :text-color {:r 0 :g 0 :b 0 :a 255}
                   :wrap-mode :words}}]}

  {:id :list-item-1
   :id-hash 2736451928
   :background-color {:r 220 :g 220 :b 220 :a 255}
   :layout {:sizing {:width {:type :grow :min 0}
                     :height {:type :fixed :value 50}}
            :padding {:left 12 :right 12 :top 12 :bottom 12}
            :child-alignment {:x :left :y :top}}
   :children
   [{:id :text-1
     :type :text
     :content "Item 2"
     :text-config {:font-size 14
                   :text-color {:r 0 :g 0 :b 0 :a 255}
                   :wrap-mode :words}}]}

  ;; ... more items
  ]}
```

---

## Example 4: Dropdown Menu

### Clay C

```c
CLAY(CLAY_ID("FileButton"), {
    .layout = { .padding = { 16, 16, 8, 8 } },
    .backgroundColor = { 140, 140, 140, 255 }
}) {
    CLAY_TEXT(CLAY_STRING("File"), buttonTextConfig);

    bool menuVisible = Clay_PointerOver(Clay_GetElementId(CLAY_STRING("FileButton")))
                    || Clay_PointerOver(Clay_GetElementId(CLAY_STRING("FileMenu")));

    if (menuVisible) {
        CLAY(CLAY_ID("FileMenu"), {
            .floating = {
                .attachTo = CLAY_ATTACH_TO_PARENT,
                .attachPoints = {
                    .element = CLAY_ATTACH_POINT_LEFT_TOP,
                    .parent = CLAY_ATTACH_POINT_LEFT_BOTTOM
                },
                .offset = { 0, 4 },
                .zIndex = 100
            },
            .layout = {
                .layoutDirection = CLAY_TOP_TO_BOTTOM,
                .sizing = { CLAY_SIZING_FIXED(150), CLAY_SIZING_FIT(0) }
            },
            .backgroundColor = { 60, 60, 60, 255 }
        }) {
            for (int i = 0; i < 3; i++) {
                CLAY(CLAY_IDI("MenuItem", i), {
                    .layout = {
                        .padding = CLAY_PADDING_ALL(12),
                        .sizing = { CLAY_SIZING_GROW(0), CLAY_SIZING_FIT(0) }
                    },
                    .backgroundColor = Clay_Hovered() ? (Clay_Color){80, 80, 80, 255} : (Clay_Color){60, 60, 60, 255}
                }) {
                    CLAY_TEXT(menuItems[i], menuItemConfig);
                }
            }
        }
    }
}
```

### DSL

```clojure
(defn file-dropdown [menu-visible? menu-items hovered-item]
  [:file-button
   {:bg [140 140 140]
    :pad [16 16 8 8]}
   [
    [:text "File" {:size 14 :color [255 255 255]}]

    (when menu-visible?
      [:file-menu
       {:bg [60 60 60]
        :dir :col
        :size [[:fixed 150] :fit]
        :float {:to :parent
                :at [:left-top :left-bottom]
                :offset [0 4]
                :z 100}}
       (vec
        (for [[i item] (map-indexed vector menu-items)]
          [(keyword (str "menu-item-" i))
           {:bg (if (= hovered-item i) [80 80 80] [60 60 60])
            :size [:grow :fit]
            :pad 12}
           [
            [:text item {:size 12 :color [255 255 255]}]
           ]]))])
   ]])
```

### Parsed (Canonical Form)

```clojure
{:id :file-button
 :id-hash 7364518293
 :background-color {:r 140 :g 140 :b 140 :a 255}
 :layout {:padding {:left 16 :right 16 :top 8 :bottom 8}
          :child-alignment {:x :left :y :top}}
 :children
 [{:id :text-0
   :type :text
   :content "File"
   :text-config {:font-size 14
                 :text-color {:r 255 :g 255 :b 255 :a 255}
                 :wrap-mode :words}}

  {:id :file-menu
   :id-hash 9283746152
   :background-color {:r 60 :g 60 :b 60 :a 255}
   :layout {:direction :top-to-bottom
            :sizing {:width {:type :fixed :value 150}
                     :height {:type :fit :min 0}}
            :child-alignment {:x :left :y :top}}
   :floating {:attach-to :parent
              :attach-points {:element :left-top
                              :parent :left-bottom}
              :offset {:x 0 :y 4}
              :z-index 100
              :pointer-capture-mode :capture}
   :children
   [{:id :menu-item-0
     :id-hash 1928374650
     :background-color {:r 60 :g 60 :b 60 :a 255}
     :layout {:sizing {:width {:type :grow :min 0}
                       :height {:type :fit :min 0}}
              :padding {:left 12 :right 12 :top 12 :bottom 12}
              :child-alignment {:x :left :y :top}}
     :children
     [{:id :text-1
       :type :text
       :content "New"
       :text-config {:font-size 12
                     :text-color {:r 255 :g 255 :b 255 :a 255}
                     :wrap-mode :words}}]}

    {:id :menu-item-1
     :id-hash 2837465192
     :background-color {:r 60 :g 60 :b 60 :a 255}
     :layout {:sizing {:width {:type :grow :min 0}
                       :height {:type :fit :min 0}}
              :padding {:left 12 :right 12 :top 12 :bottom 12}
              :child-alignment {:x :left :y :top}}
     :children
     [{:id :text-2
       :type :text
       :content "Open"
       :text-config {:font-size 12
                     :text-color {:r 255 :g 255 :b 255 :a 255}
                     :wrap-mode :words}}]}

    {:id :menu-item-2
     :id-hash 3746519283
     :background-color {:r 60 :g 60 :b 60 :a 255}
     :layout {:sizing {:width {:type :grow :min 0}
                       :height {:type :fit :min 0}}
              :padding {:left 12 :right 12 :top 12 :bottom 12}
              :child-alignment {:x :left :y :top}}
     :children
     [{:id :text-3
       :type :text
       :content "Save"
       :text-config {:font-size 12
                     :text-color {:r 255 :g 255 :b 255 :a 255}
                     :wrap-mode :words}}]}]}]}
```

---

## Example 5: Profile Card with Image

### Clay C

```c
CLAY(CLAY_ID("ProfileCard"), {
    .layout = {
        .layoutDirection = CLAY_TOP_TO_BOTTOM,
        .sizing = { CLAY_SIZING_FIXED(200), CLAY_SIZING_FIT(0) },
        .padding = CLAY_PADDING_ALL(16),
        .childGap = 12,
        .childAlignment = { .x = CLAY_ALIGN_X_CENTER }
    },
    .backgroundColor = { 255, 255, 255, 255 },
    .cornerRadius = CLAY_CORNER_RADIUS(8),
    .border = {
        .color = { 200, 200, 200, 255 },
        .width = CLAY_BORDER_OUTSIDE(1)
    }
}) {
    CLAY(CLAY_ID("Avatar"), {
        .layout = {
            .sizing = { CLAY_SIZING_FIXED(80), CLAY_SIZING_FIXED(80) }
        },
        .cornerRadius = CLAY_CORNER_RADIUS(40),
        .image = { .imageData = avatarImage }
    }) {}

    CLAY_TEXT(CLAY_STRING("John Doe"), nameConfig);
    CLAY_TEXT(CLAY_STRING("Developer"), roleConfig);
}
```

### DSL

```clojure
[:profile-card
 {:bg [255 255 255]
  :radius 8
  :border {:color [200 200 200] :width 1}
  :dir :col
  :size [[:fixed 200] :fit]
  :pad 16
  :gap 12
  :align [:center :top]}
 [
  [:avatar
   {:radius 40
    :image avatar-image
    :size [[:fixed 80] [:fixed 80]]}
   []]

  [:text "John Doe" {:size 18 :color [0 0 0] :weight :bold}]
  [:text "Developer" {:size 14 :color [100 100 100]}]
 ]]
```

### Parsed (Canonical Form)

```clojure
{:id :profile-card
 :id-hash 8374651928
 :background-color {:r 255 :g 255 :b 255 :a 255}
 :corner-radius {:top-left 8 :top-right 8 :bottom-left 8 :bottom-right 8}
 :border {:color {:r 200 :g 200 :b 200 :a 255}
          :width {:left 1 :right 1 :top 1 :bottom 1}}
 :layout {:direction :top-to-bottom
          :sizing {:width {:type :fixed :value 200}
                   :height {:type :fit :min 0}}
          :padding {:left 16 :right 16 :top 16 :bottom 16}
          :child-gap 12
          :child-alignment {:x :center :y :top}}
 :children
 [{:id :avatar
   :id-hash 9182736451
   :corner-radius {:top-left 40 :top-right 40 :bottom-left 40 :bottom-right 40}
   :image {:image-data avatar-image}
   :layout {:sizing {:width {:type :fixed :value 80}
                     :height {:type :fixed :value 80}}
            :child-alignment {:x :left :y :top}}
   :children []}

  {:id :text-0
   :id-hash 1827364591
   :type :text
   :content "John Doe"
   :text-config {:font-size 18
                 :text-color {:r 0 :g 0 :b 0 :a 255}
                 :font-weight :bold
                 :wrap-mode :words}}

  {:id :text-1
   :id-hash 2736451928
   :type :text
   :content "Developer"
   :text-config {:font-size 14
                 :text-color {:r 100 :g 100 :b 100 :a 255}
                 :wrap-mode :words}}]}
```

---

## Example 6: Complex Borders

### Clay C

```c
CLAY(CLAY_ID("BorderDemo"), {
    .layout = {
        .padding = CLAY_PADDING_ALL(20),
        .layoutDirection = CLAY_TOP_TO_BOTTOM,
        .childGap = 10
    },
    .backgroundColor = { 255, 255, 255, 255 },
    .border = {
        .color = { 120, 140, 255, 255 },
        .width = {
            .left = 3, .right = 9, .top = 6, .bottom = 12,
            .betweenChildren = 2
        }
    }
}) {
    CLAY_TEXT(CLAY_STRING("Item 1"), textConfig);
    CLAY_TEXT(CLAY_STRING("Item 2"), textConfig);
    CLAY_TEXT(CLAY_STRING("Item 3"), textConfig);
}
```

### DSL

```clojure
[:border-demo
 {:bg [255 255 255]
  :border {:color [120 140 255]
           :width [3 9 6 12]
           :between 2}
  :dir :col
  :pad 20
  :gap 10}
 [
  [:text "Item 1" {:size 14 :color [0 0 0]}]
  [:text "Item 2" {:size 14 :color [0 0 0]}]
  [:text "Item 3" {:size 14 :color [0 0 0]}]
 ]]
```

### Parsed (Canonical Form)

```clojure
{:id :border-demo
 :id-hash 7364518293
 :background-color {:r 255 :g 255 :b 255 :a 255}
 :border {:color {:r 120 :g 140 :b 255 :a 255}
          :width {:left 3 :right 9 :top 6 :bottom 12
                  :between-children 2}}
 :layout {:direction :top-to-bottom
          :padding {:left 20 :right 20 :top 20 :bottom 20}
          :child-gap 10
          :child-alignment {:x :left :y :top}}
 :children
 [{:id :text-0
   :type :text
   :content "Item 1"
   :text-config {:font-size 14
                 :text-color {:r 0 :g 0 :b 0 :a 255}
                 :wrap-mode :words}}

  {:id :text-1
   :type :text
   :content "Item 2"
   :text-config {:font-size 14
                 :text-color {:r 0 :g 0 :b 0 :a 255}
                 :wrap-mode :words}}

  {:id :text-2
   :type :text
   :content "Item 3"
   :text-config {:font-size 14
                 :text-color {:r 0 :g 0 :b 0 :a 255}
                 :wrap-mode :words}}]}
```

---

## Example 7: Text with Wrapping

### Clay C

```c
CLAY(CLAY_ID("TextContainer"), {
    .layout = {
        .sizing = { CLAY_SIZING_FIXED(300), CLAY_SIZING_FIT(0) },
        .padding = CLAY_PADDING_ALL(16)
    },
    .backgroundColor = { 245, 245, 245, 255 }
}) {
    CLAY_TEXT(
        CLAY_STRING("This is a long text that will wrap to multiple lines based on the container width."),
        CLAY_TEXT_CONFIG({
            .fontSize = 14,
            .textColor = { 50, 50, 50, 255 },
            .lineHeight = 20,
            .wrapMode = CLAY_TEXT_WRAP_WORDS
        })
    );
}
```

### DSL

```clojure
[:text-container
 {:bg [245 245 245]
  :size [[:fixed 300] :fit]
  :pad 16}
 [
  [:text "This is a long text that will wrap to multiple lines based on the container width."
   {:size 14
    :color [50 50 50]
    :line-height 20
    :wrap :words}]
 ]]
```

### Parsed (Canonical Form)

```clojure
{:id :text-container
 :id-hash 9182736451
 :background-color {:r 245 :g 245 :b 245 :a 255}
 :layout {:sizing {:width {:type :fixed :value 300}
                   :height {:type :fit :min 0}}
          :padding {:left 16 :right 16 :top 16 :bottom 16}
          :child-alignment {:x :left :y :top}}
 :children
 [{:id :text-0
   :id-hash 1827364591
   :type :text
   :content "This is a long text that will wrap to multiple lines based on the container width."
   :text-config {:font-size 14
                 :text-color {:r 50 :g 50 :b 50 :a 255}
                 :line-height 20
                 :wrap-mode :words}}]}
```

---

## Example 8: Constrained Sizing

### Clay C

```c
CLAY(CLAY_ID("ConstrainedBox"), {
    .layout = {
        .sizing = {
            CLAY_SIZING_GROW(100, 500),  // min 100, max 500
            CLAY_SIZING_FIT(50, 200)     // min 50, max 200
        },
        .padding = CLAY_PADDING_ALL(16)
    },
    .backgroundColor = { 200, 220, 255, 255 }
}) {
    CLAY_TEXT(CLAY_STRING("Constrained content"), textConfig);
}
```

### DSL

```clojure
[:constrained-box
 {:bg [200 220 255]
  :size [[:grow 100 500] [:fit 50 200]]
  :pad 16}
 [
  [:text "Constrained content" {:size 14 :color [0 0 0]}]
 ]]
```

### Parsed (Canonical Form)

```clojure
{:id :constrained-box
 :id-hash 8374651928
 :background-color {:r 200 :g 220 :b 255 :a 255}
 :layout {:sizing {:width {:type :grow :min 100 :max 500}
                   :height {:type :fit :min 50 :max 200}}
          :padding {:left 16 :right 16 :top 16 :bottom 16}
          :child-alignment {:x :left :y :top}}
 :children
 [{:id :text-0
   :type :text
   :content "Constrained content"
   :text-config {:font-size 14
                 :text-color {:r 0 :g 0 :b 0 :a 255}
                 :wrap-mode :words}}]}
```

---

## Reactive Computation Graph

Unlike linear pipelines that run once per frame, Clay-CLJ maintains a reactive computation graph using Missionary. The layout automatically recomputes when any input changes.

```text
                    ┌─────────────┐
                    │ app-tree-   │  Your UI definition (DSL)
                    │    flow     │  Changes when state changes
                    └──────┬──────┘
                           │
     ┌─────────────┐       │       ┌─────────────┐
     │  pointer-   │       │       │ dimensions- │
     │    flow     │       │       │    flow     │
     └──────┬──────┘       │       └──────┬──────┘
            │              │              │
            └──────────────┼──────────────┘
                           │
                    ┌──────▼──────┐
                    │   m/latest  │  Missionary combines flows
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │   layout    │  Parallel computation
                    │   engine    │  (Specter + Missionary)
                    └──────┬──────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
       ┌──────▼──────┐ ┌───▼────┐ ┌─────▼─────┐
       │   render    │ │hovered │ │  scroll   │
       │  commands   │ │  ids   │ │   data    │
       └──────┬──────┘ └───┬────┘ └─────┬─────┘
              │            │            │
              ▼            └────────────┘
        (to renderer)            │
                                 ▼
                          (feedback to app)
```

**Data transformations within layout:**

```text
DSL Input → parse → Canonical Form → measure text → Sized Tree → position → Render Commands
```

Each transformation is a pure function. Missionary orchestrates parallel execution where possible.

---

## Architecture

Clay-CLJ uses a unified Missionary-based architecture that works identically across all deployment scenarios.

### Why This Matters

| Capability | Clay (C) | Clay-CLJ |
|------------|----------|----------|
| Layout computation | Sequential, single-threaded | Parallel subtrees via Missionary |
| Reactivity | Manual frame loop | Automatic on input change |
| Distribution | Local only | Server layout, client render (CLJ-CLJS) |
| Text measurement | Blocking, synchronous | Batched async tasks |
| Theme/accessibility | Manual color swapping | Specter tree transformations |
| Color system | RGB only | 10+ color spaces, named colors, functions |

### Core Concepts

**Everything is flows.** Renderer state (mouse position, window dimensions, scroll offsets) are Missionary flows. Layout recomputes automatically when any input flow changes - no frame loop needed.

```clojure
(require '[missionary.core :as m])

;; Layout automatically recomputes when inputs change
(def output-flow
  (m/latest
    (fn [tree dims pointer scroll]
      (layout/compute tree dims pointer scroll measure-fn))
    app-tree-flow
    dimensions-flow
    pointer-flow
    scroll-flow))

;; Render loop
(m/reduce #(renderer/render! r (:render-commands %2)) nil output-flow)
```

### Deployment Scenarios

The same layout engine code (`.cljc`) runs in all three scenarios:

```
┌─────────────────────────────────────────────┐
│         User Application (.cljc)            │
└─────────────────────┬───────────────────────┘
                      │
┌─────────────────────▼───────────────────────┐
│         Layout Engine (.cljc)               │
│   Identical code for all deployments        │
└─────────────────────┬───────────────────────┘
                      │
┌─────────────────────▼───────────────────────┐
│      Renderer Adapter (implements IRenderer)│
│                                             │
│   CLJ-CLJ       CLJ-CLJS        CLJS-CLJS   │
│   java2d        wire-server     canvas      │
│   (local)       ↕ websocket     (local)     │
│                 wire-client                 │
└─────────────────────────────────────────────┘
```

#### CLJ-CLJ (Desktop App)

```clojure
(ns my-app.main
  (:require [clay.layout :as layout]
            [clay.renderer.java2d :as renderer]
            [missionary.core :as m]))

(def r (renderer/create window))

(def output-flow
  (m/latest
    (fn [tree dims pointer scroll]
      (layout/compute tree dims pointer scroll
                      #(renderer/measure-text-task r %)))
    app-tree-flow
    (renderer/dimensions-flow r)
    (renderer/pointer-flow r)
    (renderer/scroll-flow r)))

(m/reduce #(renderer/render! r (:render-commands %2)) nil output-flow)
```

#### CLJS-CLJS (Web App)

```clojure
(ns my-app.main
  (:require [clay.layout :as layout]
            [clay.renderer.canvas :as renderer]
            [missionary.core :as m]))

(def r (renderer/create (js/document.getElementById "app")))

;; IDENTICAL PATTERN - only renderer differs
(def output-flow
  (m/latest
    (fn [tree dims pointer scroll]
      (layout/compute tree dims pointer scroll
                      #(renderer/measure-text-task r %)))
    app-tree-flow
    (renderer/dimensions-flow r)
    (renderer/pointer-flow r)
    (renderer/scroll-flow r)))

(m/reduce #(renderer/render! r (:render-commands %2)) nil output-flow)
```

#### CLJ-CLJS (Server Layout, Browser Render)

**Server (CLJ):**

```clojure
(ns my-app.server
  (:require [clay.layout :as layout]
            [clay.renderer.wire :as wire]
            [missionary.core :as m]))

(defn handle-client [ws]
  (let [r (wire/create-server-adapter ws)]

    ;; IDENTICAL PATTERN - wire adapter handles transport
    (def output-flow
      (m/latest
        (fn [tree dims pointer scroll]
          (layout/compute tree dims pointer scroll
                          #(wire/measure-text-task r %)))
        app-tree-flow
        (wire/dimensions-flow r)
        (wire/pointer-flow r)
        (wire/scroll-flow r)))

    (m/reduce #(wire/send-commands! r (:render-commands %2)) nil output-flow)))
```

**Client (CLJS):**

```clojure
(ns my-app.client
  (:require [clay.renderer.wire :as wire]
            [clay.renderer.canvas :as canvas]))

(def ws (js/WebSocket. "ws://localhost:8080"))
(def canvas-renderer (canvas/create (js/document.getElementById "app")))

;; Wire client bridges websocket to canvas renderer
(wire/create-client-adapter ws canvas-renderer)
```

### Renderer Protocol

All renderers implement the same protocol:

```clojure
(defprotocol IRenderer
  ;; Flows FROM renderer
  (pointer-flow [this] "Flow of {:x :y :state}")
  (dimensions-flow [this] "Flow of {:width :height}")
  (scroll-flow [this] "Flow of {element-id {:x :y}}")
  (fonts-flow [this] "Flow of available fonts with metrics")

  ;; Missionary tasks
  (measure-text-task [this items] "Task that measures text batch")

  ;; Commands TO renderer
  (render! [this commands] "Execute render commands")

  ;; Font management
  (register-font! [this id font-data] "Register a font"))
```

### Wire Protocol (CLJ-CLJS)

**CLJS → CLJ (Renderer State):**

```clojure
{:type :state-update
 :pointer {:x 150 :y 200 :state :pressed}
 :dimensions {:width 1200 :height 800}
 :scroll {:list-view {:x 0 :y 150}}}
```

**CLJ → CLJS (Commands):**

```clojure
{:type :render
 :commands [{:type :rectangle :bounding-box {...} ...}
            {:type :text :text "Hello" ...}]}

{:type :measure-text
 :request-id #uuid "..."
 :items [{:text "Hello" :font-id :inter :size 16} ...]}
```

**CLJS → CLJ (Responses):**

```clojure
{:type :measurement-result
 :request-id #uuid "..."
 :results [{:width 45 :height 19} ...]}
```

### Functional Values

Visual properties can be functions that evaluate against a context:

```clojure
;; DSL with functional color
[:button
 {:bg #(if (contains? (:hovered-ids %) :button)
         [80 120 200]
         [100 140 220])
  :pad 16}
 [...]]
```

**Supported functional properties (visual only):**

- `:bg` / `:background`
- `:radius`
- `:border` color
- `:visible`

**NOT functional (affects layout):**

- `:size` - would require re-layout
- `:pad` - affects child positioning
- `:gap` - affects child spacing
- `:dir` - affects layout direction

**Evaluation context:**

```clojure
{:hovered-ids #{:button :sidebar}
 :pointer {:x 100 :y 200 :state :pressed}
 :scroll-offsets {:list {:x 0 :y 150}}
 :frame-time 0.016}
```

Functions are evaluated during render command generation, after layout completes. For CLJ-CLJS, functions must be resolved before serialization.

### Text Measurement

Text measurement uses Missionary tasks for transparent local/remote handling:

```clojure
;; Layout engine collects text via Specter
(def TEXT-ELEMENTS
  (sp/recursive-path [] p
    (sp/if-path :children
      (sp/continue-then-stay :children sp/ALL p)
      sp/STAY)))

;; Batch measurement task
(defn measure-batch [renderer items]
  (m/sp
    (let [results (m/?> (renderer/measure-text-task renderer items))]
      (zipmap (map :cache-key items) results))))
```

**Caching:**

- Cache key: `(hash [text font-id size letter-spacing])`
- Cache location: layout engine side
- Pre-warm common text on app startup

**Latency (CLJ-CLJS):**

- Localhost: ~2-5ms round-trip
- Remote: ~20-50ms round-trip
- Mitigated by batching and caching

### Specter Paths

Pre-compiled paths for efficient tree navigation:

```clojure
;; All descendants
(def CHILDREN
  (sp/recursive-path [] p
    (sp/if-path :children
      (sp/stay-then-continue :children sp/ALL p))))

;; Text elements only
(def TEXT-ELEMENTS
  (sp/comp-paths CHILDREN (sp/pred :text)))

;; Floating elements
(def FLOATING-ELEMENTS
  (sp/comp-paths CHILDREN (sp/pred :floating)))

;; Usage: collect all text for measurement
(sp/select TEXT-ELEMENTS tree)

;; Usage: inject measurements back
(sp/transform [CHILDREN (sp/pred :text)]
              #(assoc % :text-dims (get measurements (:cache-key %)))
              tree)
```

### Parallel Layout

**This is what Clay (C) cannot do.** Clay processes elements sequentially. Clay-CLJ uses Missionary to parallelize independent subtrees.

```clojure
(defn layout-children [children ctx measure-fn]
  (m/sp
    ;; Layout all children in parallel - each subtree computes independently
    (let [tasks (mapv #(layout-element % ctx measure-fn) children)
          results (m/?> (m/join vector tasks))]
      results)))

(defn layout [input measure-fn]
  (m/sp
    (let [;; Phase 1: Collect text (parallel via Specter)
          text-items (m/?> (collect-text (:tree input)))

          ;; Phase 2: Measure batch (single round-trip for CLJ-CLJS)
          measurements (m/?> (measure-fn text-items))

          ;; Phase 3: Compute sizes (parallel per subtree)
          sized (m/?> (compute-sizes (:tree input) measurements))

          ;; Phase 4: Position elements (parallel)
          positioned (m/?> (compute-positions sized))

          ;; Phase 5: Generate commands
          commands (generate-commands positioned)]

      {:render-commands commands
       :hovered-ids (compute-hovered positioned (:pointer input))
       :scroll-data (extract-scroll-data positioned)
       :element-data (extract-element-data positioned)})))
```

### Performance Considerations

1. **Flow debouncing**: High-frequency events (mousemove) debounced to 60fps
2. **Structural sharing**: Persistent data structures for tree diffs
3. **Compiled Specter paths**: Pre-compile from Malli schema shapes
4. **Memoize functional values**: Cache by context hash
5. **Batch measurements**: Single round-trip per layout pass
6. **Font metrics estimation**: Ship basic metrics for instant layout, refine later

### Font Management

```clojure
;; Register fonts on renderer
(renderer/register-font! r :inter (load-font "Inter-Regular.ttf"))
(renderer/register-font! r :roboto (load-font "Roboto-Regular.ttf"))

;; Fonts flow includes basic metrics
(renderer/fonts-flow r)
;; => {:inter {:avg-char-width 8.5 :cap-height 0.7 :line-height 1.2}
;;    :roboto {:avg-char-width 8.2 :cap-height 0.68 :line-height 1.15}}

;; Use in DSL
[:text "Hello" {:size 16 :font :inter :color [0 0 0]}]
```

### Image Loading

Images need dimensions for layout:

```clojure
;; Load image and get dimensions (task)
(def img-dims (m/?> (renderer/load-image-task r "url")))
;; => {:width 800 :height 600}

;; Use in DSL with aspect ratio
[:container {:image {:src "url" :aspect (/ 800 600)}} []]
```

### Error Handling

```clojure
;; Missionary tasks handle errors gracefully
(m/sp
  (try
    (let [result (m/?> (measure-fn items))]
      result)
    (catch Exception e
      ;; Fallback to estimation
      (estimate-text-dims items))))
```

### Reconnection (CLJ-CLJS)

```clojure
;; Wire adapter handles reconnection
(wire/create-server-adapter ws
  {:on-disconnect #(println "Client disconnected")
   :on-reconnect #(do
                    (println "Client reconnected")
                    ;; Request full state resync
                    (wire/request-state! %))})
```

---

## Defaults

When properties are omitted, these defaults apply:

| Property         | Default            |
| ---------------- | ------------------ |
| `:dir`         | `:row`           |
| `:size`        | `[:fit :fit]`    |
| `:pad`         | `0`              |
| `:gap`         | `0`              |
| `:align`       | `[:left :top]`   |
| `:radius`      | `0`              |
| `:bg`          | none (transparent) |
| `:wrap` (text) | `:words`         |

---

## Validation Rules

1. **Sizing values must be non-negative**
2. **Percentages must be 0-1** (`:percent 0.5` not `:percent 50`)
3. **Color values 0-255**
4. **Children must be a vector** (even if empty: `[]`)
5. **IDs must be unique** within the tree
6. **Text content cannot be empty**
