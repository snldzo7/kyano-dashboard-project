(ns demo.prod
  "Production entry point."
  (:require [datascript.core :as ds]
            [demo.core :as app]
            [demo.schema :as schema]))

(defn main
  "Production entry point."
  []
  ;; Make production adjustments here
  (app/main (ds/create-conn schema/schema) (js/document.getElementById "app")))
