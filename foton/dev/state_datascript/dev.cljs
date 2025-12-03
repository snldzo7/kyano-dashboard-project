(ns state-datascript.dev
  (:require [datascript.core :as ds]
            [dataspex.core :as dataspex]
            [nexus.action-log :as action-log]
            [state-datascript.core :as app]
            [state-datascript.schema :as schema]))

(defonce conn (ds/create-conn schema/schema))
(defonce el (js/document.getElementById "app"))
(dataspex/inspect "Datascript" conn)
(action-log/inspect)

(defn main []
  ;; Add additional dev-time tooling here
  (app/main conn el))
