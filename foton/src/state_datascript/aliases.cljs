(ns state-datascript.aliases
  "Replicant alias registrations"
  (:require [replicant.alias :as alias]
            [state-datascript.router :as router]))

(defn routing-anchor [attrs children]
  (let [routes (-> attrs :replicant/alias-data :routes)]
    (into [:a (cond-> attrs
                (:ui/location attrs)
                (assoc :href (router/location->url routes
                                                   (:ui/location attrs))))]
          children)))

(defn register-aliases! []
  (alias/register! :ui/a routing-anchor))
