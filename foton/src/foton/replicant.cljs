(ns foton.replicant
  "Replicant alias registration for Foton CSS primitives"
  (:require [replicant.alias :as alias]
            [foton.core :as core]
            [foton.render.replicant-css]))

(def ^:private primitives [:frame :text :icon :input :textarea :link :image :video :svg])

(defn- make-renderer
  "Create a render function for a primitive that delegates to the multimethod"
  [primitive]
  (fn [attrs children]
    (core/render (keyword "foton" (name primitive)) :replicant-css
                 {:attrs attrs :children children})))

(defn init!
  "Initialize Foton CSS primitives as Replicant aliases.

   Registers aliases as :foton.css/xxx (e.g., :foton.css/frame, :foton.css/text)

   Usage:
     (foton/init!)

   After init, use primitives directly in views:
     [:foton.css/frame {:fill :card} [:foton.css/text {:preset :heading} \"Hello\"]]

   Primitives work with any linking mechanism:
     [:ui/a {:ui/location {...}}
       [:foton.css/frame {:fill :card} \"Click me\"]]"
  []
  (doseq [prim primitives]
    (alias/register! (keyword "foton.css" (name prim))
                     (make-renderer prim))))
