(ns demo.actions
  "Demo application action registrations."
  (:require [datascript.core :as ds]
            [nexus.registry :as nxr]
            [demo.routing :as routing]
            [foton.theme :as theme]))

(defn register-actions!
  "Register demo application actions."
  []
  ;; Navigation action
  (nxr/register-action! :actions/navigate
    (fn [db location]
      [[:effects/update-url location (ds/entity db :ui/location)]
       [:db/transact [(routing/get-location-entity location)]]]))

  ;; Generic DataScript operations
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

  ;; Demo-specific actions
  (nxr/register-action! :counter/inc
    (fn [_ entity]
      [[:db/add (:db/id entity) :clicks (inc (:clicks entity))]]))

  (nxr/register-action! :ui/set-theme
    (fn [_ theme-id]
      ;; Update Foton theme resolution
      (theme/set-theme! theme-id)
      ;; Persist to DataScript for UI state
      [[:db/transact [[:db/add :system/app :ui/theme theme-id]]]])))
