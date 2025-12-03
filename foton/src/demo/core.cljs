(ns demo.core
  "Demo application entry point."
  (:require [datascript.core :as ds]
            [nexus.registry :as nxr]
            [replicant.dom :as r]
            [replicant-kit.aliases :as aliases]
            [replicant-kit.effects :as effects]
            [demo.routes :as routes]
            [demo.routing :as routing]
            [demo.actions :as actions]
            [demo.ui :as ui]
            [foton.replicant :as foton]))

;; Register system->state
(nxr/register-system->state! (comp ds/db :conn))

;; Initialize all registrations
(aliases/register-aliases!)
(effects/register-effects!)
(actions/register-actions!)

;; Initialize Foton CSS primitives as :foton.css/* aliases
(foton/init!)

(defn main
  "Initialize and run the demo application."
  [conn el]
  (let [system {:conn conn, :routes routes/routes}]
    (add-watch
     conn ::render
     (fn [_ _ _ _]
       (r/render el (ui/render-page (ds/db conn))
                 {:alias-data {:routes routes/routes}})))

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
