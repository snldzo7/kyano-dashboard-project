(ns demo.dev
  "Development entry point with hot reloading."
  (:require [datascript.core :as ds]
            [demo.core :as app]
            [demo.schema :as schema]))

(defn ^:dev/after-load reload
  "Called after hot reload."
  []
  (js/console.log "Hot reload"))

(defn main
  "Development entry point."
  []
  (app/main (ds/create-conn schema/schema) (js/document.getElementById "app")))
