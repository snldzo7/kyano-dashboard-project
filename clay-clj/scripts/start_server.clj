(require '[clay.server.mouse-server :as server])

(server/start-server!)

(println "Backend ready - Press Ctrl+C to stop")

;; Keep the process alive
(while true
  (Thread/sleep 1000))
