(ns clay.hiccup-test
  "Hiccup DSL Test - CLIENT SIDE ONLY.

   This file is the browser client that connects to the Clojure server via Rheon.
   Layout computation happens on the server, rendering happens here.

   To run:
   1. Start Clojure REPL: clojure -M:dev
   2. In REPL: (user/start-clay-server!)
   3. Open browser: http://localhost:8085/hiccup-test.html"
  (:require [clay.client :as client]))

;; ============================================================================
;; STATE
;; ============================================================================

(defonce state (atom {:client nil}))

;; ============================================================================
;; INIT
;; ============================================================================

(defn ^:export init! []
  (js/console.log "Clay Hiccup Test - Client Starting")
  (js/console.log "Connecting to server at ws://localhost:8080...")

  (let [root (js/document.getElementById "root")]
    (if root
      (let [c (client/create-client root {:url "ws://localhost:8080"
                                           :renderer :html})]
        (reset! state {:client c})
        (client/start! c)
        (js/console.log "Client started! Waiting for server render commands..."))
      (js/console.error "No root element found!"))))

(defn ^:export reload! []
  (js/console.log "Hot reload - server handles UI updates")
  ;; No action needed - server pushes new commands automatically
  nil)
