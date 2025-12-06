(ns kanban.db
  "Datomic Peer database for Kanban.

   Provides persistent storage for tasks with full history.
   Uses datomic.api (Peer library) for local development."
  (:require [datomic.api :as d]))

;; =============================================================================
;; Schema
;; =============================================================================

(def task-schema
  [{:db/ident :task/id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "Unique identifier for the task"}

   {:db/ident :task/title
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Task title"}

   {:db/ident :task/description
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Task description"}

   {:db/ident :task/status
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "Task status: :status/open, :status/in-progress, :status/closed"}

   {:db/ident :task/priority
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "Task priority: :priority/low, :priority/medium, :priority/high"}

   {:db/ident :task/tags
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/many
    :db/doc "Task tags"}

   {:db/ident :task/created-at
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "When the task was created"}

   {:db/ident :task/changed-status-at
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "When the task status was last changed"}])

;; =============================================================================
;; Connection
;; =============================================================================

(defonce conn (atom nil))

;; Use in-memory database for development
;; For persistent storage, use: "datomic:dev://localhost:4334/kanban"
(def db-uri "datomic:mem://kanban")

(defn get-conn
  "Get or create database connection."
  []
  (when-not @conn
    ;; Create database if it doesn't exist
    (d/create-database db-uri)
    (let [connection (d/connect db-uri)]
      ;; Transact schema
      @(d/transact connection task-schema)
      (reset! conn connection)))
  @conn)

(defn db
  "Get current database value."
  []
  (d/db (get-conn)))

;; =============================================================================
;; Queries
;; =============================================================================

(def all-tasks-query
  '[:find (pull ?e [*])
    :where [?e :task/id]])

(defn get-all-tasks
  "Get all tasks as a map of task-id -> task."
  []
  (->> (d/q all-tasks-query (db))
       (map first)
       (map (fn [task]
              ;; Remove :db/id to not leak Datomic internals
              (dissoc task :db/id)))
       (map (juxt :task/id identity))
       (into {})))

(defn get-task
  "Get a single task by ID."
  [task-id]
  (let [result (d/q '[:find (pull ?e [*])
                      :in $ ?id
                      :where [?e :task/id ?id]]
                    (db)
                    task-id)]
    (when-let [task (ffirst result)]
      (dissoc task :db/id))))

;; =============================================================================
;; History Queries
;; =============================================================================

(defn get-task-history
  "Get all historical versions of a task with transaction times."
  [task-id]
  (let [history-db (d/history (db))
        ;; Find all transactions where this task was modified
        tx-times (d/q '[:find ?tx ?inst
                        :in $ ?id
                        :where
                        [?e :task/id ?id]
                        [?e _ _ ?tx true]
                        [?tx :db/txInstant ?inst]]
                      history-db task-id)]
    (->> tx-times
         (into #{})  ;; dedupe
         (sort-by second)
         (map (fn [[tx inst]]
                (let [as-of-db (d/as-of (db) tx)
                      task (ffirst (d/q '[:find (pull ?e [*])
                                          :in $ ?id
                                          :where [?e :task/id ?id]]
                                        as-of-db task-id))]
                  (when task
                    (-> task
                        (dissoc :db/id)
                        (assoc :history/timestamp inst))))))
         (remove nil?)
         vec)))

;; =============================================================================
;; Commands
;; =============================================================================

(defn create-task!
  "Create a new task. Returns the task with its id."
  [task]
  ;; Always generate UUID - client may send string IDs that don't match schema
  (let [task-id (if (uuid? (:task/id task))
                  (:task/id task)
                  (random-uuid))
        now (java.util.Date.)
        full-task (merge {:task/id task-id
                          :task/status :status/open
                          :task/priority :priority/medium
                          :task/created-at now}
                         (select-keys task [:task/title :task/description
                                            :task/status :task/priority
                                            :task/tags :task/created-at]))]
    (if (:task/title full-task)
      (do
        @(d/transact (get-conn) [full-task])
        {:success? true :task/id task-id})
      {:success? false :error "Task must have a title"})))

(defn set-task-status!
  "Update a task's status."
  [{:task/keys [id status]}]
  (if (get-task id)
    (do
      @(d/transact (get-conn) [{:task/id id
                                :task/status status
                                :task/changed-status-at (java.util.Date.)}])
      {:success? true})
    {:success? false :error "Task not found"}))

(defn update-task!
  "Update any task attributes."
  [task-id updates]
  (if (get-task task-id)
    (do
      @(d/transact (get-conn) [(assoc updates :task/id task-id)])
      {:success? true})
    {:success? false :error "Task not found"}))

(defn delete-task!
  "Retract a task from the database."
  [task-id]
  (if (get-task task-id)
    (let [eid (ffirst (d/q '[:find ?e :in $ ?id :where [?e :task/id ?id]]
                           (db) task-id))]
      @(d/transact (get-conn) [[:db/retractEntity eid]])
      {:success? true})
    {:success? false :error "Task not found"}))

;; =============================================================================
;; Seed Data
;; =============================================================================

(def sample-tasks
  [{:task/id #uuid "98f1dcf4-c539-4f14-9c62-2a4a2c408d4b"
    :task/status :status/open
    :task/title "Add dark mode toggle"
    :task/tags #{:tags/feature :tags/theme}
    :task/priority :priority/medium
    :task/created-at #inst "2025-04-30T09:00:00.000Z"
    :task/description "Introduce a toggle to switch between light and dark themes for the board interface."}

   {:task/id #uuid "2b9d9dc0-5d99-4ae5-b2b1-993c2c41d676"
    :task/status :status/open
    :task/title "Auto-archive old closed tasks"
    :task/tags #{:tags/feature :tags/automation}
    :task/priority :priority/low
    :task/created-at #inst "2025-05-01T10:15:00.000Z"
    :task/description "Automatically archive tasks that have been in the closed column for more than 30 days."}

   {:task/id #uuid "b4e68c57-6fc4-4e87-b0c7-c5d7d80e0c47"
    :task/status :status/open
    :task/title "Add quick-add form for new cards"
    :task/tags #{:tags/feature :tags/ui}
    :task/priority :priority/high
    :task/created-at #inst "2025-04-29T13:00:00.000Z"
    :task/description "Implement a small form at the top of the board for quickly creating new tasks."}

   {:task/id #uuid "3de54672-d270-421e-a2e3-f16fef1d3dc1"
    :task/status :status/open
    :task/title "Keyboard shortcut for moving cards"
    :task/tags #{:tags/feature :tags/accessibility}
    :task/priority :priority/medium
    :task/created-at #inst "2025-05-03T14:45:00.000Z"
    :task/description "Allow cards to be moved left or right between columns using keyboard shortcuts."}

   {:task/id #uuid "ce6a5b1e-62f4-4a90-b899-bbca097da1a1"
    :task/status :status/open
    :task/title "Export board to JSON"
    :task/tags #{:tags/feature :tags/data}
    :task/priority :priority/low
    :task/created-at #inst "2025-04-26T08:00:00.000Z"
    :task/changed-status-at #inst "2025-04-27T16:00:00.000Z"
    :task/description "Provide a way to export the current board and task data as a JSON file for backup or import."}

   {:task/id #uuid "f68deba2-4169-4dfc-b204-d88887b0e6cf"
    :task/status :status/open
    :task/title "Animated column transitions"
    :task/tags #{:tags/ui :tags/animation}
    :task/priority :priority/medium
    :task/created-at #inst "2025-04-23T15:25:00.000Z"
    :task/changed-status-at #inst "2025-04-25T10:30:00.000Z"
    :task/description "Smoothly animate column changes and card movements for improved visual feedback."}])

(defn seed-sample-data!
  "Insert sample tasks if database is empty."
  []
  (when (empty? (get-all-tasks))
    (println "Seeding sample tasks...")
    (doseq [task sample-tasks]
      @(d/transact (get-conn) [task]))
    (println "Seeded" (count sample-tasks) "tasks")))

;; =============================================================================
;; Lifecycle
;; =============================================================================

(defn init!
  "Initialize the database connection and seed data if empty."
  []
  (println "Initializing Datomic Peer database...")
  (println "URI:" db-uri)
  (get-conn)
  (seed-sample-data!)
  (println "Database ready with" (count (get-all-tasks)) "tasks"))

(defn close!
  "Close database connection."
  []
  (when @conn
    (d/release @conn)
    (reset! conn nil)))

(comment
  ;; REPL testing
  (init!)
  (get-all-tasks)
  (get-task #uuid "98f1dcf4-c539-4f14-9c62-2a4a2c408d4b")
  (create-task! {:task/title "Test task"})
  (set-task-status! {:task/id #uuid "98f1dcf4-c539-4f14-9c62-2a4a2c408d4b"
                     :task/status :status/in-progress})
  (get-task-history #uuid "98f1dcf4-c539-4f14-9c62-2a4a2c408d4b")
  )
