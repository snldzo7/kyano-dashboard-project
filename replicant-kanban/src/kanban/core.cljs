(ns kanban.core
  (:require [odoyle.rules :as o]
            [kanban.session :as session]
            [kanban.task :as task]
            [kanban.ui :as ui]
            [replicant.dom :as r]))

(defn- do-render [el on-render old-state new-state]
  (let [hiccup (-> (assoc new-state :new-tasks (task/get-new-tasks old-state new-state))
                   ui/render-app)]
    (when (fn? on-render)
      (on-render hiccup))
    (r/render el hiccup)))

(defn ^{:indent 2} boot [el & [{:keys [on-render]}]]
  ;; Initialize O'Doyle session (no rules needed - dispatch! handles actions directly)
  (session/init-session!)
  (swap! session/*session
         (fn [s] (-> s
                     (o/insert :system :now (js/Date.))
                     o/fire-rules)))

  ;; Set Replicant dispatch to use session
  (r/set-dispatch! session/dispatch!)

  ;; Watch session for re-renders
  (add-watch session/*session ::render
    (fn [_ _ old-session new-session]
      (when (and old-session new-session (not= old-session new-session))
        (let [old-state (session/->render-state old-session)
              new-state (session/->render-state new-session)]
          (do-render el on-render old-state new-state)))))

  ;; Initial render - show loading state before tasks sync
  (let [initial-state (session/->render-state @session/*session)]
    (do-render el on-render {} initial-state)))
