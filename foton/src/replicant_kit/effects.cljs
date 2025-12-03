(ns replicant-kit.effects
  "Nexus effect registrations for routing and DataScript.

   Usage:
     (register-effects!)

   System map must contain:
   - :routes  silk routes for URL generation
   - :conn    DataScript connection"
  (:require [datascript.core :as ds]
            [nexus.registry :as nxr]
            [replicant-kit.router :as router]))

(defn register-effects!
  "Register common effects."
  []
  ;; URL update effect (push/replace state)
  (nxr/register-effect! :effects/update-url
    (fn [_ {:keys [routes]} new-location old-location]
      (if (router/essentially-same? new-location old-location)
        (.replaceState js/history nil "" (router/location->url routes new-location))
        (.pushState js/history nil "" (router/location->url routes new-location)))))

  ;; DataScript transact effect (batched)
  (nxr/register-effect! :db/transact
    ^:nexus/batch
    (fn [_ {:keys [conn]} txes]
      (ds/transact! conn (apply concat (map first txes))))))
