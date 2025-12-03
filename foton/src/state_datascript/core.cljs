(ns state-datascript.core
  "Application entry point"
  (:require [datascript.core :as ds]
            [nexus.registry :as nxr]
            [replicant.dom :as r]
            [state-datascript.router :as router]
            [state-datascript.routing :as routing]
            [state-datascript.aliases :as aliases]
            [state-datascript.effects :as effects]
            [state-datascript.actions :as actions]
            [state-datascript.ui :as ui]
            [foton.replicant :as foton]))

;; Register system->state
(nxr/register-system->state! (comp ds/db :conn))

;; Initialize all registrations
(aliases/register-aliases!)
(effects/register-effects!)
(actions/register-actions!)

;; Initialize Foton CSS primitives as :foton.css/* aliases
(foton/init!)

(defn main [conn el]
  (let [system {:conn conn, :routes router/routes}]
    (add-watch
     conn ::render
     (fn [_ _ _ _]
       (r/render el (ui/render-page (ds/db conn))
                 {:alias-data {:routes router/routes}})))

    (r/set-dispatch!
     (fn [dispatch-data actions]
       (nxr/dispatch system dispatch-data actions)))

    (js/document.body.addEventListener
     "click"
     #(routing/route-click % system))

    (js/window.addEventListener
     "popstate"
     (fn [_]
       (nxr/dispatch system nil
        [[:db/transact [(routing/get-location-entity (routing/get-current-location))]]])))

    ;; Trigger the initial render
    (ds/transact! conn
     [{:db/ident :system/app
       :app/started-at (js/Date.)}
      (routing/get-location-entity (routing/get-current-location))])))
