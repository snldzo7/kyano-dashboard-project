(ns user
  (:require [clay.server.mouse-server :as mouse-server]))

(defn start []
  (mouse-server/start-server!)
  :started)

(defn stop []
  (mouse-server/stop-server!)
  :stopped)

(defn restart []
  (stop)
  (start))

(comment
  (start)
  (stop)
  (restart))
