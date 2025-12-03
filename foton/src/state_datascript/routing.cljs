(ns state-datascript.routing
  "Routing helpers"
  (:require [nexus.registry :as nxr]
            [state-datascript.router :as router]))

(defn find-target-href [e]
  (some-> e .-target
          (.closest "a")
          (.getAttribute "href")))

(defn get-current-location []
  (->> js/location.href
       (router/url->location router/routes)))

(defn get-location-entity [location]
  (into {:db/ident :ui/location
         :location/query-params {}
         :location/hash-params {}
         :location/params {}}
        location))

(defn route-click [e system]
  (let [href (find-target-href e)]
    (when-let [location (router/url->location (:routes system) href)]
      (.preventDefault e)
      (nxr/dispatch system nil [[:actions/navigate location]]))))
