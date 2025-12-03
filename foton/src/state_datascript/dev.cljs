(ns state-datascript.dev
  (:require [datascript.core :as ds]
            [state-datascript.core :as app]
            [state-datascript.schema :as schema]))

(defn ^:dev/after-load reload []
  (js/console.log "Hot reload"))

(defn main []
  (app/main (ds/create-conn schema/schema) (js/document.getElementById "app")))
