(ns state-datascript.effects
  "Nexus effect registrations"
  (:require [datascript.core :as ds]
            [nexus.registry :as nxr]
            [state-datascript.router :as router]))

(defn register-effects! []
  (nxr/register-effect! :effects/update-url
    (fn [_ {:keys [routes]} new-location old-location]
      (if (router/essentially-same? new-location old-location)
        (.replaceState js/history nil "" (router/location->url routes new-location))
        (.pushState js/history nil "" (router/location->url routes new-location)))))

  (nxr/register-effect! :db/transact
    ^:nexus/batch
    (fn [_ {:keys [conn]} txes]
      (ds/transact! conn (apply concat (map first txes))))))
