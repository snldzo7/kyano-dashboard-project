(ns demo.routing
  "Demo app routing helpers."
  (:require [nexus.registry :as nxr]
            [replicant-kit.router :as router]
            [demo.routes :as routes]))

(defn find-target-href
  "Find the href of the closest anchor element."
  [e]
  (some-> e .-target
          (.closest "a")
          (.getAttribute "href")))

(defn get-current-location
  "Get location from current browser URL."
  []
  (->> js/location.href
       (router/url->location routes/routes)))

(defn get-location-entity
  "Convert location to DataScript entity format."
  [location]
  (into {:db/ident :ui/location
         :location/query-params {}
         :location/hash-params {}
         :location/params {}}
        location))

(defn route-click
  "Handle click events for client-side routing."
  [e system]
  (let [href (find-target-href e)]
    (when-let [location (router/url->location (:routes system) href)]
      (.preventDefault e)
      (nxr/dispatch system nil [[:actions/navigate location]]))))
