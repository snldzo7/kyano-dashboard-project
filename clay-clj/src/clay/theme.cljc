(ns clay.theme
  "Theme system using Specter navigators for tree transformations"
  (:require [com.rpl.specter :as sp]
            [clay.color :as c]
            [hyperfiddle.rcf :refer [tests]]))

#?(:clj (hyperfiddle.rcf/enable!))

;; ============================================================================
;; SPECTER PATHS
;; ============================================================================

(def ALL-ELEMENTS
  "Navigate to all elements in tree (depth-first)"
  (sp/recursive-path [] p
    (sp/if-path :children
      (sp/stay-then-continue :children sp/ALL p)
      sp/STAY)))

(def ALL-PROPS
  "Navigate to props of all elements"
  [ALL-ELEMENTS (sp/must :props)])

(def COLOR-PROPS
  "Navigate to color properties in all elements"
  [ALL-ELEMENTS (sp/submap [:bg :color :border-color])])

(def RADIUS-PROPS
  "Navigate to radius properties in all elements"
  [ALL-ELEMENTS (sp/must :radius)])

;; ============================================================================
;; BUILT-IN THEMES
;; ============================================================================

(def light-theme
  "Light theme tokens"
  {;; Surfaces
   :surface-primary :slate-50
   :surface-secondary :slate-100
   :surface-tertiary :slate-200
   :surface-inverse :slate-900

   ;; Text
   :text-primary :slate-900
   :text-secondary :slate-600
   :text-tertiary :slate-500
   :text-inverse :slate-50
   :text-muted :slate-400

   ;; Borders
   :border-default :slate-200
   :border-strong :slate-300
   :border-muted :slate-100

   ;; Accent colors
   :accent :blue-500
   :accent-hover :blue-600
   :accent-muted :blue-100

   ;; Semantic colors
   :success :emerald-500
   :warning :amber-500
   :error :red-500
   :info :sky-500

   ;; Radius scale
   :radius-none 0
   :radius-sm 4
   :radius-md 8
   :radius-lg 12
   :radius-xl 16
   :radius-full 9999

   ;; Shadow (for future use)
   :shadow-sm {:blur 4 :spread 0 :color [:black 0.1]}
   :shadow-md {:blur 8 :spread 0 :color [:black 0.15]}
   :shadow-lg {:blur 16 :spread 0 :color [:black 0.2]}})

(def dark-theme
  "Dark theme tokens"
  {;; Surfaces
   :surface-primary :slate-900
   :surface-secondary :slate-800
   :surface-tertiary :slate-700
   :surface-inverse :slate-50

   ;; Text
   :text-primary :slate-50
   :text-secondary :slate-300
   :text-tertiary :slate-400
   :text-inverse :slate-900
   :text-muted :slate-500

   ;; Borders
   :border-default :slate-700
   :border-strong :slate-600
   :border-muted :slate-800

   ;; Accent colors
   :accent :blue-400
   :accent-hover :blue-300
   :accent-muted :blue-900

   ;; Semantic colors
   :success :emerald-400
   :warning :amber-400
   :error :red-400
   :info :sky-400

   ;; Radius scale (slightly larger in dark mode)
   :radius-none 0
   :radius-sm 6
   :radius-md 10
   :radius-lg 14
   :radius-xl 18
   :radius-full 9999

   ;; Shadow
   :shadow-sm {:blur 4 :spread 0 :color [:black 0.3]}
   :shadow-md {:blur 8 :spread 0 :color [:black 0.4]}
   :shadow-lg {:blur 16 :spread 0 :color [:black 0.5]}})

(def high-contrast-theme
  "High contrast theme for accessibility"
  {;; Surfaces
   :surface-primary :white
   :surface-secondary :gray-100
   :surface-tertiary :gray-200
   :surface-inverse :black

   ;; Text
   :text-primary :black
   :text-secondary :gray-900
   :text-tertiary :gray-800
   :text-inverse :white
   :text-muted :gray-700

   ;; Borders
   :border-default :black
   :border-strong :black
   :border-muted :gray-600

   ;; Accent colors
   :accent :blue-700
   :accent-hover :blue-900
   :accent-muted :blue-200

   ;; Semantic colors
   :success :green-700
   :warning :yellow-700
   :error :red-700
   :info :blue-600

   ;; Radius scale
   :radius-none 0
   :radius-sm 4
   :radius-md 8
   :radius-lg 12
   :radius-xl 16
   :radius-full 9999})

;; ============================================================================
;; THEME APPLICATION
;; ============================================================================

(defn resolve-token
  "Resolve a single token value. Returns original if not a token."
  [tokens value]
  (if (keyword? value)
    (get tokens value value)
    value))

(defn apply-theme
  "Apply theme tokens to a UI tree. Resolves semantic tokens to actual values."
  [tree tokens]
  (sp/transform
    [ALL-ELEMENTS sp/MAP-VALS]
    (fn [v]
      (if (map? v)
        ;; Transform property maps
        (reduce-kv
          (fn [m k val]
            (assoc m k (resolve-token tokens val)))
          {}
          v)
        ;; Resolve direct values
        (resolve-token tokens v)))
    tree))

(defn apply-to-colors
  "Apply a transformation function to all color properties in tree"
  [tree transform-fn]
  (sp/transform
    [ALL-ELEMENTS (sp/submap [:bg :color :border-color]) sp/MAP-VALS]
    (fn [color]
      (when color
        (transform-fn color)))
    tree))

;; ============================================================================
;; COLOR MODES
;; ============================================================================

(defn apply-color-mode
  "Apply a color mode transformation to entire tree.
   Modes: :dark, :protanopia, :deuteranopia, :tritanopia, :grayscale"
  [tree mode]
  (let [transform-fn (case mode
                       :dark c/dark-mode
                       :protanopia c/protanopia
                       :deuteranopia c/deuteranopia
                       :tritanopia c/tritanopia
                       :grayscale c/grayscale
                       identity)]
    (apply-to-colors tree
      (fn [color]
        (-> color
            c/resolve-color
            transform-fn)))))

(defn apply-dark-mode
  "Convenience function for dark mode"
  [tree]
  (apply-color-mode tree :dark))

(defn apply-protanopia
  "Convenience function for protanopia simulation"
  [tree]
  (apply-color-mode tree :protanopia))

(defn apply-deuteranopia
  "Convenience function for deuteranopia simulation"
  [tree]
  (apply-color-mode tree :deuteranopia))

(defn apply-tritanopia
  "Convenience function for tritanopia simulation"
  [tree]
  (apply-color-mode tree :tritanopia))

;; ============================================================================
;; UTILITY FUNCTIONS
;; ============================================================================

(defn merge-themes
  "Merge multiple themes, later themes override earlier ones"
  [& themes]
  (apply merge themes))

(defn scale-radii
  "Scale all radius values in tree by a factor"
  [tree factor]
  (sp/transform
    [ALL-ELEMENTS (sp/must :radius)]
    (fn [r]
      (if (number? r)
        (* r factor)
        (mapv #(* % factor) r)))
    tree))

(defn adjust-contrast
  "Adjust contrast of all colors in tree.
   factor > 1 increases contrast, < 1 decreases"
  [tree factor]
  (apply-to-colors tree
    (fn [color]
      (let [{:keys [r g b a]} (c/resolve-color color)
            ;; Adjust towards/away from mid-gray
            adjust (fn [v]
                     (let [mid 128
                           diff (- v mid)
                           new-v (+ mid (* diff factor))]
                       (c/clamp (Math/round new-v) 0 255)))]
        {:r (adjust r) :g (adjust g) :b (adjust b) :a a}))))

(defn tint-colors
  "Apply a tint color to all colors in tree"
  [tree tint-color amount]
  (let [tint (c/resolve-color tint-color)]
    (apply-to-colors tree
      (fn [color]
        (c/mix (c/resolve-color color) tint amount)))))

(defn desaturate
  "Reduce saturation of all colors by amount (0-1)"
  [tree amount]
  (apply-to-colors tree
    (fn [color]
      (let [resolved (c/resolve-color color)
            gray (c/grayscale resolved)]
        (c/mix resolved gray amount)))))

;; ============================================================================
;; COMPONENT-SPECIFIC THEMES
;; ============================================================================

(defn theme-buttons
  "Apply button-specific styling transformations"
  [tree {:keys [rounded? large?]}]
  (let [radius-transform (if rounded?
                           (fn [_] 9999)
                           identity)
        size-transform (if large?
                         (fn [pad] (if (number? pad) (* pad 1.5) pad))
                         identity)]
    (-> tree
        (sp/transform
          [ALL-ELEMENTS (sp/pred #(= :button (:type %))) (sp/must :radius)]
          radius-transform)
        (sp/transform
          [ALL-ELEMENTS (sp/pred #(= :button (:type %))) (sp/must :pad)]
          size-transform))))

;; ============================================================================
;; RCF TESTS
;; ============================================================================

#?(:clj
   (tests
    "resolve-token - known token"
    (resolve-token light-theme :surface-primary) := :slate-50

    "resolve-token - unknown token"
    (resolve-token light-theme :unknown) := :unknown

    "resolve-token - non-keyword"
    (resolve-token light-theme 42) := 42

    "apply-theme resolves tokens"
    (let [tree {:id :card
                :bg :surface-primary
                :radius :radius-md
                :children []}
          result (apply-theme tree light-theme)]
      (:bg result) := :slate-50
      (:radius result) := 8)

    "merge-themes"
    (let [custom {:accent :purple-500}
          merged (merge-themes light-theme custom)]
      (:accent merged) := :purple-500
      (:surface-primary merged) := :slate-50)

    "ALL-ELEMENTS path works"
    (let [tree {:id :root
                :children [{:id :child1
                            :children [{:id :grandchild}]}
                           {:id :child2}]}
          ids (sp/select [ALL-ELEMENTS :id] tree)]
      (count ids) := 4)

    "apply-to-colors transforms colors"
    (let [tree {:id :test
                :bg :red-500
                :children []}
          result (apply-to-colors tree c/resolve-color)]
      (map? (:bg result)) := true
      (:r (:bg result)) := 239)))
