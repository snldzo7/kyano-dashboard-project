(ns foton.primitives
  "Foton primitives - Frame, Text, Icon, Input, etc.

   Each primitive is a render function that takes attrs and children
   and returns hiccup. No multimethod dispatch - direct functions.

   Usage:
     (render-frame {:fill :primary :padding :md} [child1 child2])
     (render-text {:size 14 :color [:text :primary]} [\"Hello\"])
     (render-icon {:name :check :color [:status :good]})"
  (:require [clojure.string :as str]
            [foton.css :as css]
            [foton.theme :as theme]
            [foton.vary :as vary]))

;; =============================================================================
;; Data Expansion (pure transformations)
;; =============================================================================

(defn expand-text-attrs
  "Expand :preset to full typography attrs."
  [{:keys [preset] :as attrs}]
  (if preset
    (merge (theme/typography preset) (dissoc attrs :preset))
    attrs))

;; =============================================================================
;; Attr Key Sets - separate Foton attrs from user pass-through
;; =============================================================================

(def ^:private frame-keys
  #{:fill :radius :padding :margin :direction :gap :align :justify
    :width :height :min-width :max-width :min-height :max-height
    :grow :shrink :basis :wrap :cursor :opacity :border
    :transition :transform :scale
    ;; Semantic transform properties
    :translate-x :translate-y :rotate :skew-x :skew-y
    ;; Figma properties
    :stroke :effects :shadow :blend
    ;; Overflow & scrolling
    :overflow :overflow-x :overflow-y :clip
    :scroll-behavior :scrollbar :scroll-snap :scroll-snap-type
    ;; Masking & clipping
    :mask :clip-path
    ;; Positioning (P0 Figma)
    :position :top :left :right :bottom :z :visible
    ;; Gradient (P0 Figma)
    :gradient
    ;; Background Image (P0 Figma)
    :background-image :background-size :background-position
    ;; Variant system
    :vary :current-variant})

(def ^:private text-keys
  #{;; Typography
    :preset :family :size :weight :style
    ;; Color & opacity
    :color :opacity
    ;; Spacing
    :tracking :line-height :paragraph-spacing
    ;; Alignment
    :text-align :valign
    ;; Decoration
    :decoration :transform
    ;; Truncation & overflow
    :truncate :max-lines :wrap :break
    ;; Selection
    :selectable})

(def ^:private icon-keys
  #{:name :size :color})

(def ^:private input-keys
  #{:size :radius :disabled :type :placeholder :value :name})

(def ^:private textarea-keys
  #{:size :radius :disabled :placeholder :value :name :rows})

(def ^:private link-keys
  #{:color :underline :href :target})

(def ^:private image-keys
  #{:radius :fit :width :height :src :alt})

(def ^:private video-keys
  #{:radius :width :height :src :poster :controls :autoplay :loop :muted})

(def ^:private svg-keys
  #{:size :color :width :height :viewBox :fill :stroke})

(defn- user-attrs
  "Extract user attrs (on, class, id, etc.) that should pass through to HTML"
  [attrs foton-keys]
  (apply dissoc attrs foton-keys))

;; =============================================================================
;; Icon Mapping
;; =============================================================================

;; Brand icons that require fa-brands prefix
(def ^:private brand-icons
  #{:github :twitter :linkedin :discord :facebook :instagram :youtube :tiktok
    :apple :google :microsoft :amazon :spotify :slack :figma :dribbble
    :behance :codepen :npm :node :python :java :php :rust :golang
    :docker :kubernetes :aws :react :vue :angular :svelte :bootstrap
    :sass :css3 :html5 :js :wordpress :shopify :stripe :paypal})

;; Shorthand aliases for common icons (optional convenience)
(def ^:private icon-aliases
  {:x "xmark"
   :close "xmark"
   :warning "triangle-exclamation"
   :info "circle-info"
   :settings "gear"
   :edit "pen"
   :delete "trash"
   :add "plus"
   :remove "minus"
   :expand "expand"
   :collapse "compress"
   :external "arrow-up-right-from-square"
   :link "link"
   :unlink "link-slash"})

(defn- resolve-icon-class
  "Resolve icon name to FontAwesome class.
   Supports:
   - Keywords: :check, :arrow-right, :github
   - Strings: \"fa-check\", \"check\"
   - With style prefix: \"solid/check\", \"brands/github\""
  [icon-name]
  (let [name-str (if (keyword? icon-name) (name icon-name) (str icon-name))
        ;; Check for explicit style prefix (e.g., \"solid/check\" or \"brands/github\")
        [style icon] (if (str/includes? name-str "/")
                       (str/split name-str #"/" 2)
                       [nil name-str])
        ;; Resolve aliases
        resolved-icon (get icon-aliases (keyword icon) icon)
        ;; Determine prefix
        prefix (cond
                 style (str "fa-" style)
                 (contains? brand-icons (keyword resolved-icon)) "fa-brands"
                 :else "fa-solid")
        ;; Build class name
        fa-class (if (str/starts-with? resolved-icon "fa-")
                   resolved-icon
                   (str "fa-" resolved-icon))]
    [prefix fa-class]))

;; =============================================================================
;; Render Functions
;; =============================================================================

(defn render-frame
  "Render a frame (div with flexbox layout).

   Attrs:
   - :fill          Color token (:primary, [:status :good])
   - :radius        Radius token (:md, 8, [8 8 0 0])
   - :padding       Spacing token (:md, 16)
   - :margin        Spacing token
   - :gap           Spacing token
   - :direction     :horizontal | :vertical
   - :align         :start | :center | :end | :stretch | :baseline
   - :justify       :start | :center | :end | :space-between | :space-around
   - :width         :fill | :hug | :viewport | number | string
   - :height        :fill | :hug | :viewport | number | string
   - :shadow        Shadow preset (:sm, :md, :lg)
   - :stroke        {:width 1 :color :border :position :inside}
   - :effects       Vector of effect maps
   - :vary          {:hovered {...} :pressed {...}}
   - :transition    Number (ms) or {:duration :easing :properties}
   - :on            Event handlers {:click [...]}

   Returns hiccup [:div {:style ...} children...]"
  [attrs children]
  (let [attrs (or attrs {})
        vary-map (:vary attrs)
        transition (:transition attrs)
        ;; If vary is present, process variations
        variations (when vary-map
                     (vary/process-vary vary-map :replicant-css attrs))
        ;; Generate unique class for pseudo-class CSS
        class-name (when (seq variations)
                     (css/gen-class-name attrs))
        ;; Generate pseudo-class CSS rules
        pseudo-css (when class-name
                     (vary/generate-pseudo-css class-name variations))
        ;; Add transition if vary is present (default 200ms)
        attrs-with-transition (if (and vary-map (not transition))
                                (assoc attrs :transition 200)
                                attrs)
        style (css/frame-style attrs-with-transition)
        pass-through (user-attrs attrs frame-keys)
        ;; Add generated class to existing classes
        final-attrs (cond-> (merge pass-through {:style style})
                      class-name (update :class #(if % (str % " " class-name) class-name)))]
    (if pseudo-css
      (into [:div final-attrs [:style pseudo-css]] children)
      (into [:div final-attrs] children))))

(defn render-text
  "Render text (span with typography).

   Attrs:
   - :preset        Typography preset (:title, :heading, :body, :small)
   - :size          Font size (number or string)
   - :weight        Font weight (400, 500, 600, 700)
   - :color         Color token (:primary, [:text :secondary])
   - :text-align    :left | :center | :right
   - :tracking      Letter spacing in em
   - :line-height   Line height multiplier

   Returns hiccup [:span {:style ...} children...]"
  [attrs children]
  (let [attrs (or attrs {})
        style (css/text-style (expand-text-attrs attrs))
        pass-through (user-attrs attrs text-keys)]
    (into [:span (merge pass-through {:style style})]
          children)))

(defn render-icon
  "Render an icon (FontAwesome).

   Attrs:
   - :name   Icon name - supports any FontAwesome icon:
             Keywords: :check, :arrow-right, :github, :heart
             Strings: \"fa-check\", \"check\"
             With style: \"solid/check\", \"brands/github\", \"regular/heart\"
   - :size   Size preset (:xs, :sm, :md, :lg, :xl)
   - :color  Color token

   Returns hiccup [:i {:class \"fa-solid fa-...\" :style ...}]"
  [attrs]
  (let [attrs (or attrs {})
        style (css/icon-style attrs)
        icon-name (:name attrs)
        [prefix fa-class] (if icon-name
                            (resolve-icon-class icon-name)
                            ["fa-solid" "fa-circle"])
        pass-through (user-attrs attrs icon-keys)]
    [:i (merge pass-through {:class (str prefix " " fa-class) :style style})]))

(defn render-input
  "Render an input field.

   Attrs:
   - :type         Input type (text, password, email, etc.)
   - :placeholder  Placeholder text
   - :value        Current value
   - :name         Field name
   - :size         Size preset (:sm, :md, :lg)
   - :radius       Border radius token
   - :disabled     Boolean

   Returns hiccup [:input {:style ...}]"
  [attrs]
  (let [attrs (or attrs {})
        style (css/input-style attrs)
        pass-through (user-attrs attrs input-keys)]
    [:input (merge pass-through
                   (cond-> {:type (or (:type attrs) "text") :style style}
                     (:placeholder attrs) (assoc :placeholder (:placeholder attrs))
                     (:value attrs) (assoc :value (:value attrs))
                     (:name attrs) (assoc :name (:name attrs))
                     (:disabled attrs) (assoc :disabled true)))]))

(defn render-textarea
  "Render a textarea.

   Attrs:
   - :placeholder  Placeholder text
   - :value        Current value
   - :name         Field name
   - :rows         Number of rows
   - :size         Size preset (:sm, :md, :lg)
   - :radius       Border radius token
   - :disabled     Boolean

   Returns hiccup [:textarea {:style ...}]"
  [attrs]
  (let [attrs (or attrs {})
        style (css/textarea-style attrs)
        pass-through (user-attrs attrs textarea-keys)]
    [:textarea (merge pass-through
                      (cond-> {:style style}
                        (:placeholder attrs) (assoc :placeholder (:placeholder attrs))
                        (:value attrs) (assoc :value (:value attrs))
                        (:name attrs) (assoc :name (:name attrs))
                        (:rows attrs) (assoc :rows (:rows attrs))
                        (:disabled attrs) (assoc :disabled true)))]))

(defn render-link
  "Render a link.

   Attrs:
   - :href       URL
   - :target     Target (_blank, _self, etc.)
   - :color      Color token
   - :underline  :none to remove underline

   Returns hiccup [:a {:style ...} children...]"
  [attrs children]
  (let [attrs (or attrs {})
        style (css/link-style attrs)
        pass-through (user-attrs attrs link-keys)]
    (into [:a (merge pass-through
                     (cond-> {:style style}
                       (:href attrs) (assoc :href (:href attrs))
                       (:target attrs) (assoc :target (:target attrs))))]
          children)))

(defn render-image
  "Render an image.

   Attrs:
   - :src      Image URL
   - :alt      Alt text
   - :width    Width (:fill, number)
   - :height   Height (:fill, number)
   - :radius   Border radius token
   - :fit      Object fit (:cover, :contain, :fill)

   Returns hiccup [:img {:style ...}]"
  [attrs]
  (let [attrs (or attrs {})
        style (css/image-style attrs)
        pass-through (user-attrs attrs image-keys)]
    [:img (merge pass-through
                 (cond-> {:style style}
                   (:src attrs) (assoc :src (:src attrs))
                   (:alt attrs) (assoc :alt (:alt attrs))))]))

(defn render-video
  "Render a video.

   Attrs:
   - :src       Video URL
   - :poster    Poster image URL
   - :width     Width
   - :height    Height
   - :radius    Border radius token
   - :controls  Show controls (boolean)
   - :autoplay  Auto-play (boolean)
   - :loop      Loop video (boolean)
   - :muted     Muted (boolean)

   Returns hiccup [:video {:style ...}]"
  [attrs]
  (let [attrs (or attrs {})
        style (css/video-style attrs)
        pass-through (user-attrs attrs video-keys)]
    [:video (merge pass-through
                   (cond-> {:style style}
                     (:src attrs) (assoc :src (:src attrs))
                     (:poster attrs) (assoc :poster (:poster attrs))
                     (:controls attrs) (assoc :controls true)
                     (:autoplay attrs) (assoc :autoplay true)
                     (:loop attrs) (assoc :loop true)
                     (:muted attrs) (assoc :muted true)))]))

(defn render-svg
  "Render an SVG element.

   Attrs:
   - :viewBox  SVG viewBox
   - :size     Size preset (:xs, :sm, :md, :lg, :xl)
   - :width    Width (number)
   - :height   Height (number)
   - :color    Color token (sets currentColor)
   - :fill     Fill color
   - :stroke   Stroke color

   Returns hiccup [:svg {:style ...} children...]"
  [attrs children]
  (let [attrs (or attrs {})
        style (css/svg-style attrs)
        pass-through (user-attrs attrs svg-keys)]
    (into [:svg (merge pass-through
                       (cond-> {:style style}
                         (:viewBox attrs) (assoc :viewBox (:viewBox attrs))
                         (:fill attrs) (assoc :fill (if (keyword? (:fill attrs)) "currentColor" (:fill attrs)))
                         (:stroke attrs) (assoc :stroke (:stroke attrs))))]
          children)))
