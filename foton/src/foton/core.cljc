(ns foton.core
  "Foton core - renderer-agnostic multimethod dispatch and data expansion"
  (:require [foton.theme :as theme]))

;; -----------------------------------------------------------------------------
;; Multimethod Dispatch
;; -----------------------------------------------------------------------------

(defmulti render
  "Render a Foton primitive to a target format.
   Dispatch on [primitive-type target] vector.

   Examples:
     (render :frame :replicant-tw {:attrs {...} :children [...]})
     (render :text :figma {:attrs {...} :children [...]})"
  (fn [primitive target _ctx] [primitive target]))

(defmethod render :default [primitive target _ctx]
  (throw (ex-info (str "No render implementation for " [primitive target])
                  {:primitive primitive :target target})))

;; -----------------------------------------------------------------------------
;; Data Expansion (pure transformations)
;; -----------------------------------------------------------------------------

(defn expand-text-attrs
  "Expand :preset to full typography attrs. Pure data transformation."
  [{:keys [preset] :as attrs}]
  (if preset
    (merge (theme/typography preset) (dissoc attrs :preset))
    attrs))

(defn expand-frame-attrs
  "Expand frame attrs. Currently a pass-through but extensible."
  [attrs]
  attrs)

(defn expand-icon-attrs
  "Expand icon attrs. Currently a pass-through but extensible."
  [attrs]
  attrs)
