(ns kanban.wires
  "Shared Wire-Ref Definitions for Kanban.

   These are PURE DATA - no runtime, no side effects.
   Both client and server require this namespace and instantiate
   the same wire definitions with `r/wire`.

   Includes Malli specs for validation.")

;; =============================================================================
;; Malli Schemas
;; =============================================================================

(def TaskId
  "UUID or string task identifier."
  [:or :uuid :string])

(def Priority
  [:enum :priority/high :priority/medium :priority/low])

(def Status
  [:enum :status/open :status/in-progress :status/closed])

(def Task
  "Schema for a task."
  [:map
   [:task/id TaskId]
   [:task/title :string]
   [:task/description {:optional true} [:maybe :string]]
   [:task/status {:optional true} Status]
   [:task/priority {:optional true} Priority]
   ;; Accept both sets and vectors for tags (Datomic returns vectors)
   [:task/tags {:optional true} [:or [:set :keyword] [:vector :keyword]]]
   [:task/created-at {:optional true} :any]
   [:task/changed-status-at {:optional true} :any]])

(def TasksMap
  "Map of task-id to task."
  [:map-of TaskId Task])

;; -----------------------------------------------------------------------------
;; Command Schemas
;; -----------------------------------------------------------------------------

(def CreateTaskCommand
  [:map
   [:command/kind [:= :commands/create-task]]
   [:command/data [:map
                   [:task/id TaskId]
                   [:task/title :string]
                   [:task/description {:optional true} [:maybe :string]]
                   [:task/status {:optional true} Status]
                   [:task/priority {:optional true} Priority]
                   ;; Accept both sets and vectors for tags
                   [:task/tags {:optional true} [:or [:set :keyword] [:vector :keyword]]]
                   [:task/created-at {:optional true} :any]]]])

(def SetTaskStatusCommand
  [:map
   [:command/kind [:= :commands/set-task-status]]
   [:command/data [:map
                   [:task/id TaskId]
                   [:task/status Status]]]])

(def UpdateTaskCommand
  [:map
   [:command/kind [:= :commands/update-task]]
   [:command/data Task]])

(def DeleteTaskCommand
  [:map
   [:command/kind [:= :commands/delete-task]]
   [:command/data [:map [:task/id TaskId]]]])

(def GetTaskHistoryCommand
  [:map
   [:command/kind [:= :commands/get-task-history]]
   [:command/data [:map [:task/id TaskId]]]])

(def Command
  "Union of all command types."
  [:or
   CreateTaskCommand
   SetTaskStatusCommand
   UpdateTaskCommand
   DeleteTaskCommand
   GetTaskHistoryCommand])

(def CommandReply
  "Reply from command execution."
  [:map
   [:success? :boolean]
   [:error {:optional true} :string]
   [:history {:optional true} [:vector :map]]])

;; =============================================================================
;; Signal Wire-Refs (current value, server is source of truth)
;; =============================================================================

(def tasks-ref
  "Tasks signal - server owns the state, clients watch.
   Value is a map of task-id -> task."
  {:wire-id :tasks
   :type :signal
   :initial {}
   :spec TasksMap})

;; =============================================================================
;; Discrete Wire-Refs (request/response)
;; =============================================================================

(def commands-ref
  "Commands discrete - client sends, server replies.
   Commands: create-task, set-task-status, update-task, delete-task, get-task-history."
  {:wire-id :commands
   :type :discrete
   :spec {:request Command
          :reply CommandReply}})

;; =============================================================================
;; All Wire-Refs
;; =============================================================================

(def all-refs
  "All wire-refs for the kanban application."
  [tasks-ref commands-ref])
