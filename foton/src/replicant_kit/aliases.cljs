(ns replicant-kit.aliases
  "Replicant alias registrations for routing.

   Usage:
     (register-aliases!)

   Then in views:
     [:ui/a {:ui/location {:location/page-id :pages/home}} \"Home\"]

   Note: Pass routes in alias-data when rendering:
     (r/render el (ui/view) {:alias-data {:routes my-routes}})"
  (:require [replicant.alias :as alias]
            [replicant-kit.router :as router]))

(defn routing-anchor
  "Anchor element that converts :ui/location to href.

   Attrs:
   - :ui/location  Location map to link to
   - Other attrs passed through to :a element

   Requires :routes in :replicant/alias-data."
  [attrs children]
  (let [routes (-> attrs :replicant/alias-data :routes)]
    (into [:a (cond-> attrs
                (:ui/location attrs)
                (assoc :href (router/location->url routes
                                                   (:ui/location attrs))))]
          children)))

(defn register-aliases!
  "Register routing aliases."
  []
  (alias/register! :ui/a routing-anchor))
