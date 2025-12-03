(ns foton.vary
  "Variant system for Foton components.

   Animation = Interpolation between variants.

   Each variant is a property snapshot. When state changes,
   the system interpolates from current to target variant
   using the specified transition.

   Specter Path Support:
   Variant keys can be Specter paths (vectors) to target nested properties:
   {:hovered {:fill :elevated
              [:stroke :width] 2}}"
  (:require [clojure.string :as str]
            [com.rpl.specter :as sp]
            [foton.css :as css]))

;; =============================================================================
;; Semantic Transform Properties
;; =============================================================================

(def transform-properties
  "Transform properties that can be varied."
  #{:translate-x :translate-y :scale :rotate :skew-x :skew-y})

(defn extract-transforms
  "Extract transform properties from an attribute map."
  [attrs]
  (select-keys attrs transform-properties))

(defn has-transforms?
  "Check if attrs contain any transform properties."
  [attrs]
  (some transform-properties (keys attrs)))

;; =============================================================================
;; Specter Path Support
;; =============================================================================

(defn apply-variant
  "Apply variant changes to base attrs.

   Supports both simple keys and Specter paths:
   - :fill           -> simple assoc
   - [:stroke :width] -> Specter path navigation"
  [base variant-map]
  (reduce-kv
    (fn [acc k v]
      (if (vector? k)
        (sp/setval k v acc)
        (assoc acc k v)))
    base
    variant-map))

(defn split-variant-keys
  "Split variant map into simple keys and Specter paths.
   Returns {:simple {...} :paths {...}}"
  [variant-map]
  (reduce-kv
    (fn [acc k v]
      (if (vector? k)
        (update acc :paths assoc k v)
        (update acc :simple assoc k v)))
    {:simple {} :paths {}}
    variant-map))

(defn variant->css-style
  "Convert variant attributes to CSS style map."
  [variant-attrs]
  (let [{:keys [simple paths]} (split-variant-keys variant-attrs)
        transform (css/build-transform simple)
        non-transform-attrs (apply dissoc simple transform-properties)
        ;; Process simple keys
        simple-styles (reduce-kv
                        (fn [acc k v]
                          (if-let [css-prop (css/attr->css-property k)]
                            (if-let [css-val (css/attr->css-value k v)]
                              (assoc acc css-prop css-val)
                              acc)
                            acc))
                        {}
                        non-transform-attrs)
        ;; Process Specter paths
        path-styles (reduce-kv
                      (fn [acc path v]
                        (if-let [css-prop (css/path->css-property path)]
                          (if-let [css-val (css/path->css-value path v)]
                            (assoc acc css-prop css-val)
                            acc)
                          acc))
                      {}
                      paths)]
    (cond-> {}
      transform (assoc :transform transform)
      :always (merge simple-styles path-styles))))

;; =============================================================================
;; State -> Pseudo-class Mapping
;; =============================================================================

(def state->pseudo
  {:hovered ":hover"
   :pressed ":active"
   :focused ":focus"
   :disabled ":disabled"})

;; =============================================================================
;; Variant Processing
;; =============================================================================

(defn process-vary
  "Process :vary attribute and generate CSS variation data.

   vary-map: {:hovered {:fill :elevated} :pressed {:scale 0.98}}
   target: ignored (only replicant-css supported)
   base-attrs: the component's base attributes

   Returns map of state -> {:pseudo-class :style}"
  [vary-map _target _base-attrs]
  (when vary-map
    (reduce-kv
      (fn [acc state-type state-attrs]
        (if-let [pseudo (get state->pseudo state-type)]
          (assoc acc state-type {:pseudo-class pseudo
                                 :style (variant->css-style state-attrs)})
          acc))
      {}
      vary-map)))

;; =============================================================================
;; CSS Generation
;; =============================================================================

(defn generate-pseudo-css
  "Generate CSS rules for all pseudo-class variations.

   class-name: unique class for this element
   variations: map of state-type -> {:pseudo-class :style}

   Returns CSS string like:
   .fv-123:hover{background-color:#334155}
   .fv-123:active{transform:scale(0.98)}"
  [class-name variations]
  (->> variations
       (map (fn [[_state-type {:keys [pseudo-class style]}]]
              (when (and pseudo-class (seq style))
                (str "." class-name pseudo-class "{" (css/style->css-string style) "}"))))
       (remove nil?)
       (str/join "\n")))
