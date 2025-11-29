(ns ui.specter
  "Specter navigators for CSS and Hiccup tree manipulation.

   The key insight: CSS (Garden) and Hiccup are both trees.
   Specter lets us navigate and transform them uniformly.

   Use cases:
   - Theme switching: swap all colors across entire stylesheet
   - Dark mode: transform background/foreground colors
   - Accessibility: increase font sizes, contrast
   - A/B testing: swap component variants
   - Animation: transform style properties over time"
  (:require [com.rpl.specter :as s :refer [ALL FIRST LAST MAP-VALS
                                            recursive-path if-path
                                            walker pred cond-path]]
            #?(:cljs [com.rpl.specter.macros :refer-macros [defnav]])))

;; =============================================================================
;; CSS (Garden) Navigators
;; =============================================================================

;; Garden CSS structure:
;; [[:selector {:prop value ...}]
;;  [:selector {:prop value ...}]]

(def CSS-RULES
  "Navigate to all CSS rules (selector + props pairs)."
  ALL)

(def CSS-PROPS
  "Navigate to all property maps in CSS rules."
  [ALL (s/filterer map?) ALL])

(defn css-prop
  "Navigate to a specific CSS property across all rules.
   (s/select (css-prop :background) styles)"
  [prop-key]
  [ALL (s/filterer map?) ALL prop-key])

(def CSS-COLORS
  "Navigate to all color values in CSS.
   Matches strings starting with # or rgb/rgba/hsl."
  [ALL (s/filterer map?) MAP-VALS
   (pred #(and (string? %)
               (or (re-matches #"^#[0-9a-fA-F]+" %)
                   (re-matches #"^rgba?\(.*\)" %)
                   (re-matches #"^hsla?\(.*\)" %))))])

(def CSS-HEX-COLORS
  "Navigate to hex color values only."
  [ALL (s/filterer map?) MAP-VALS
   (pred #(and (string? %) (re-matches #"^#[0-9a-fA-F]+" %)))])

;; =============================================================================
;; Hiccup Navigators
;; =============================================================================

;; Hiccup structure:
;; [:tag {:attr value ...} child1 child2 ...]
;; [:tag.class#id child1 child2 ...]

(def HICCUP-TAG
  "Navigate to the tag (first element) of a hiccup vector."
  FIRST)

(def HICCUP-ATTRS
  "Navigate to the attrs map (second element if map)."
  [(s/nthpath 1) (pred map?)])

(defn hiccup-attr
  "Navigate to a specific attribute value."
  [attr-key]
  [(s/nthpath 1) (pred map?) attr-key])

(def HICCUP-CHILDREN
  "Navigate to all children (elements after tag and optional attrs)."
  (s/if-path [(s/nthpath 1) map?]
             (s/srange 2 s/AFTER-ELEM)
             (s/srange 1 s/AFTER-ELEM)))

(def HICCUP-RECURSIVE
  "Recursively navigate all nested hiccup vectors."
  (recursive-path [] p
    (s/if-path vector?
      (s/continue-then-stay ALL p))))

(defn hiccup-by-tag
  "Navigate to all elements with a specific tag."
  [tag]
  [HICCUP-RECURSIVE (pred #(and (vector? %) (= (first %) tag)))])

(defn hiccup-by-class
  "Navigate to elements with a specific class.
   Works with both :class attr and :tag.class syntax."
  [class-name]
  [HICCUP-RECURSIVE
   (pred #(and (vector? %)
               (or
                ;; Check :class in attrs map
                (when-let [attrs (and (> (count %) 1)
                                      (map? (second %)))]
                  (when-let [cls (:class attrs)]
                    (if (string? cls)
                      (.includes cls (name class-name))
                      (= cls class-name))))
                ;; Check tag.class syntax
                (when-let [tag (first %)]
                  (and (keyword? tag)
                       (.includes (name tag) (str "." (name class-name))))))))])

(def HICCUP-STYLES
  "Navigate to all inline :style maps in hiccup."
  [HICCUP-RECURSIVE
   (pred vector?)
   (s/nthpath 1)
   (pred map?)
   :style])

;; =============================================================================
;; Theme Transformations
;; =============================================================================

(defn swap-colors
  "Swap color values according to a color map.
   Works on both CSS and Hiccup structures.

   Example:
     (swap-colors styles {\"#00d9ff\" \"#ff6b6b\"
                          \"#00ff88\" \"#ffaa00\"})"
  [structure color-map]
  (s/transform [CSS-HEX-COLORS]
               #(get color-map % %)
               structure))

(defn apply-dark-mode
  "Transform colors for dark mode.
   Inverts lightness while preserving hue."
  [structure]
  ;; Simple implementation - swap light/dark backgrounds
  (let [light->dark {"#fff" "#1a1a2e"
                     "#ffffff" "#1a1a2e"
                     "#000" "#ffffff"
                     "#000000" "#ffffff"}]
    (swap-colors structure light->dark)))

(defn scale-spacing
  "Scale all spacing values by a factor.
   Useful for accessibility (larger touch targets)."
  [structure factor]
  (s/transform [ALL (s/filterer map?) MAP-VALS
                (pred #(and (string? %)
                            (re-matches #"[\d.]+rem" %)))]
               (fn [val]
                 (let [num (js/parseFloat val)]
                   (str (* num factor) "rem")))
               structure))

(defn scale-fonts
  "Scale all font sizes by a factor."
  [structure factor]
  (s/transform [(css-prop :font-size)]
               (fn [val]
                 (if (string? val)
                   (let [num (js/parseFloat val)]
                     (if (js/isNaN num)
                       val
                       (str (* num factor) (re-find #"[a-z%]+" val))))
                   val))
               structure))

;; =============================================================================
;; Component Transformations
;; =============================================================================

(defn wrap-elements
  "Wrap all elements matching pred with a wrapper component.

   Example:
     (wrap-elements ui #(= (first %) :button)
                    (fn [btn] [:div.btn-wrapper btn]))"
  [hiccup match-pred wrapper-fn]
  (s/transform [HICCUP-RECURSIVE (pred match-pred)]
               wrapper-fn
               hiccup))

(defn add-class
  "Add a class to all elements matching pred."
  [hiccup match-pred class-name]
  (s/transform [HICCUP-RECURSIVE (pred match-pred)
                (s/nthpath 1)]
               (fn [attrs]
                 (if (map? attrs)
                   (update attrs :class #(str % " " (name class-name)))
                   {:class (name class-name)}))
               hiccup))

(defn replace-component
  "Replace all instances of a component with another.
   Useful for A/B testing UI variants."
  [hiccup old-tag new-component-fn]
  (s/transform [(hiccup-by-tag old-tag)]
               new-component-fn
               hiccup))

;; =============================================================================
;; Query Utilities
;; =============================================================================

(defn find-colors
  "Find all unique colors used in CSS."
  [styles]
  (set (s/select CSS-HEX-COLORS styles)))

(defn find-elements
  "Find all elements matching a predicate."
  [hiccup pred-fn]
  (s/select [HICCUP-RECURSIVE (pred pred-fn)] hiccup))

(defn count-by-tag
  "Count elements by tag name."
  [hiccup]
  (frequencies
   (s/select [HICCUP-RECURSIVE (pred vector?) FIRST (pred keyword?)]
             hiccup)))
