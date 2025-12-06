(ns kanban.dev
  (:require [clj-reload.core :as clj-reload]
            [kanban.rheon-server :as server]))

(def server-atom (atom nil))

(defn start []
  (when-not @server-atom
    (reset! server-atom (server/start!))
    (println "Started Rheon server")))

(defn stop []
  (when @server-atom
    (server/stop!)
    (reset! server-atom nil)))

(defn reset []
  (stop)
  (clj-reload/reload)
  ((requiring-resolve `start)))

(comment ;; s-:
  (start)
  (stop)
  (reset)
  (set! *print-namespace-maps* false))
