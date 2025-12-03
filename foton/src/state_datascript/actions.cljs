(ns state-datascript.actions
  "Nexus action registrations"
  (:require [datascript.core :as ds]
            [nexus.registry :as nxr]
            [state-datascript.routing :as routing]))

(defn register-actions! []
  (nxr/register-action! :actions/navigate
    (fn [db location]
      [[:effects/update-url location (ds/entity db :ui/location)]
       [:db/transact [(routing/get-location-entity location)]]]))

  (nxr/register-action! :db/add
    (fn [_ eid attr value]
      [[:db/transact [[:db/add eid attr value]]]]))

  (nxr/register-action! :db/retract
    (fn [_ eid attr & [value]]
      [[:db/transact [(cond-> [:db/retract eid attr]
                        value (conj value))]]]))

  (nxr/register-action! :db/retractEntity
    (fn [_ eid]
      [[:db/transact [[:db/retractEntity eid]]]]))

  (nxr/register-action! :counter/inc
    (fn [_ entity]
      [[:db/add (:db/id entity) :clicks (inc (:clicks entity))]])))
