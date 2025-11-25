(ns user
  "REPL utilities for development.

  This namespace is automatically loaded when starting a REPL with the :dev alias.

  Launchpad Integration:
  - Start REPL: bin/launchpad (loads this namespace automatically)
  - With Calva: bin/launchpad --cider-nrepl
  - Customize: Copy deps.local.edn.template to deps.local.edn

  Usage:
  - (start)   ; Start the WebSocket server
  - (stop)    ; Stop the WebSocket server
  - (restart) ; Restart the server"
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
