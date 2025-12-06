(ns kanban.rheon-server
  "Kanban Rheon WebSocket Server.

   Uses Rheon WebSocket communication with Datomic Local for persistence.
   Signal wire for tasks state, Discrete wire for commands."
  (:require [rheon.core :as r]
            [kanban.wires :as wires]
            [kanban.db :as db])
  (:import [java.util.concurrent Executors TimeUnit]))

;; =============================================================================
;; Cached State (for change detection)
;; =============================================================================

(defonce cached-tasks (atom {}))

;; =============================================================================
;; Connection
;; =============================================================================

(defonce conn (atom nil))

(defn get-conn []
  (when-not @conn
    (reset! conn (r/connection {:transport :ws-server :port 8088})))
  @conn)

;; =============================================================================
;; Wire Instances
;; =============================================================================

(defn tasks-wire []
  (r/wire (get-conn) wires/tasks-ref))

(defn commands-wire []
  (r/wire (get-conn) wires/commands-ref))

;; =============================================================================
;; Command Handlers
;; =============================================================================

(defn handle-command [{:command/keys [kind data]}]
  (println "Command received:" kind)
  (println "Command data:" (pr-str data))
  (try
    (let [result (case kind
                   :commands/create-task
                   (db/create-task! data)

                   :commands/set-task-status
                   (db/set-task-status! data)

                   :commands/update-task
                   (db/update-task! (:task/id data) (dissoc data :task/id))

                   :commands/delete-task
                   (db/delete-task! (:task/id data))

                   :commands/get-task-history
                   {:success? true :history (db/get-task-history (:task/id data))}

                   {:success? false :error (str "Unknown command: " kind)})]
      (println "Command result:" (pr-str result))
      ;; Sync signal after mutating commands (not for history queries)
      (when (and (:success? result)
                 (not= kind :commands/get-task-history))
        (let [new-tasks (db/get-all-tasks)]
          (reset! cached-tasks new-tasks)
          (r/signal! (tasks-wire) new-tasks)))
      result)
    (catch Exception e
      (println "Command error:" (.getMessage e))
      (.printStackTrace e)
      {:success? false :error (.getMessage e)})))

;; =============================================================================
;; Database Polling (for external changes)
;; =============================================================================

(defonce poll-executor (atom nil))

(defn check-for-changes!
  "Poll database for changes and update signal if needed."
  []
  (try
    (let [current-tasks (db/get-all-tasks)]
      (when (not= current-tasks @cached-tasks)
        (println "Detected database change, syncing signal...")
        (reset! cached-tasks current-tasks)
        (r/signal! (tasks-wire) current-tasks)))
    (catch Exception e
      (println "Poll error:" (.getMessage e)))))

(defn start-polling!
  "Start polling for database changes (for external modifications)."
  []
  (when-not @poll-executor
    (println "Starting database poll (every 2 seconds)...")
    (let [executor (Executors/newSingleThreadScheduledExecutor)]
      (reset! poll-executor executor)
      (.scheduleAtFixedRate executor
                            ^Runnable check-for-changes!
                            2000  ;; initial delay
                            2000  ;; period (2 seconds)
                            TimeUnit/MILLISECONDS))))

(defn stop-polling! []
  (when-let [executor @poll-executor]
    (println "Stopping database poll...")
    (.shutdown executor)
    (reset! poll-executor nil)))

;; =============================================================================
;; Server Lifecycle
;; =============================================================================

(defn start! []
  (println "")
  (println "====================================================")
  (println "  Kanban Rheon Server + Datomic Peer")
  (println "  WebSocket: ws://localhost:8088")
  (println "  Database: datomic:mem://kanban")
  (println "  Wires: :tasks (signal), :commands (discrete)")
  (println "====================================================")
  (println "")

  ;; Initialize Datomic
  (db/init!)

  ;; Initialize Rheon connection
  (get-conn)
  (println "WebSocket server started on port 8088")

  ;; Load initial tasks from Datomic
  (let [tasks (db/get-all-tasks)]
    (reset! cached-tasks tasks)
    (r/signal! (tasks-wire) tasks)
    (println "Tasks signal initialized with" (count tasks) "tasks from Datomic"))

  ;; Start polling for external changes
  (start-polling!)

  ;; Setup command handler
  (r/reply! (commands-wire) handle-command)
  (println "Command handler registered")

  (println "")
  (println "Server ready."))

(defn stop! []
  (println "Shutting down server...")
  (stop-polling!)
  (when @conn
    (r/close! @conn)
    (reset! conn nil))
  (db/close!)
  (println "Server stopped."))

;; =============================================================================
;; Main Entry Point
;; =============================================================================

(defn -main [& _args]
  (start!)
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. ^Runnable stop!))
  @(promise))

(comment
  (start!)
  (stop!)

  ;; Check current tasks from Datomic
  (db/get-all-tasks)

  ;; Create a task
  (db/create-task! {:task/title "Test from REPL"
                    :task/description "Testing Datomic"})

  ;; Update a task status
  (db/set-task-status! {:task/id #uuid "98f1dcf4-c539-4f14-9c62-2a4a2c408d4b"
                        :task/status :status/in-progress})
  )
