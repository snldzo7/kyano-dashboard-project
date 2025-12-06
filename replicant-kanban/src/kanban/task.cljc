(ns kanban.task
  "Task utilities and queries.")

(defn get-tasks
  "Get tasks from state. With Rheon, tasks are always in :tasks from the signal."
  [state]
  (if (:tasks state)
    {:tasks (vals (:tasks state))}
    {:loading? true}))

(defn get-new-tasks
  "Return set of new task IDs (tasks in new state but not in old)."
  [state-old state-new]
  (let [old-tasks (:tasks (get-tasks state-old))
        new-tasks (:tasks (get-tasks state-new))]
    (when (and (seq old-tasks) (seq new-tasks))
      (->> (mapv :task/id new-tasks)
           (remove (set (mapv :task/id old-tasks)))
           set))))

(defn expanded-path
  "Path to expanded state for a task.
   The transient map stores keys as keywords (from (keyword (str id)))."
  [{:task/keys [id]}]
  [:transient (keyword (str id)) :expanded?])

(defn expanded?
  "Is task expanded?"
  [state task]
  (boolean (get-in state (expanded-path task))))
