(ns kanban.dev
  (:require [dataspex.core :as dataspex]
            [kanban.core :as kanban]
            [kanban.rheon-client :as rheon]
            [kanban.session :as session]
            [odoyle.rules :as o]))

(defonce el (js/document.getElementById "app"))

(dataspex/inspect "Session" session/*session)

(defn ^:export main []
  (kanban/boot el)

  ;; Connect to Rheon server - tasks will sync automatically via signal
  (session/dispatch! {} [[:actions/connect]])
  (swap! session/*session
         (fn [s] (o/insert s :system :started-at (js/Date.)))))

(defn ^:dev/after-load refresh []
  ;; Reconnect on hot reload to pick up new connection code
  (rheon/disconnect!)
  (session/dispatch! {} [[:actions/connect]])
  (swap! session/*session
         (fn [s] (o/insert s :system :refreshed-at (js/Date.)))))

(comment
  (set! *print-namespace-maps* false))
