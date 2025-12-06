(ns kanban.rheon-client
  "Rheon client for Kanban.
   Signal-based state sync to O'Doyle session, discrete wire for commands."
  (:require [rheon.core :as r]
            [kanban.wires :as wires]
            [kanban.session :as session]))

;; =============================================================================
;; Connection and Wires
;; =============================================================================

(defn- ws-url
  "Build WebSocket URL using the page's hostname.
   Works both locally (localhost) and in Docker (host.docker.internal)."
  []
  (let [hostname (.-hostname js/location)]
    (str "ws://" hostname ":8088")))

(defonce conn (r/connection {:transport :ws
                             :url (ws-url)}))

(defonce tasks-wire (r/wire conn wires/tasks-ref))
(defonce commands-wire (r/wire conn wires/commands-ref))

(defonce tasks-watch-setup? (atom false))

;; =============================================================================
;; Public API
;; =============================================================================

(defn connect!
  "Setup Rheon connection and signal watchers.
   Tasks signal syncs automatically to session."
  []
  (js/console.log "Rheon: Connecting...")

  (letfn [(setup-tasks-watch! []
            (when (compare-and-set! tasks-watch-setup? false true)
              (js/console.log "Rheon: Watching tasks signal...")
              (r/watch tasks-wire
                       (fn [tasks]
                         (js/console.log "Rheon: Tasks synced, count:" (count tasks))
                         (session/sync-tasks! tasks)))))]

    (add-watch (:state conn) :connection
               (fn [_ _ old-state new-state]
                 (let [connected? (:connected? new-state)
                       was-connected? (:connected? old-state)
                       error (:error new-state)]
                   (cond
                     ;; Just connected
                     (and connected? (not was-connected?))
                     (do
                       (js/console.log "Rheon: Connected")
                       (session/connection-opened!)
                       (setup-tasks-watch!))

                     ;; Just disconnected
                     (and (not connected?) was-connected?)
                     (do
                       (js/console.log "Rheon: Disconnected")
                       (session/connection-closed!))

                     ;; Connection error
                     error
                     (do
                       (js/console.log "Rheon: Error" error)
                       (session/connection-error! error))))))

    ;; Initial state check
    (let [state @(:state conn)]
      (if (:connected? state)
        (do
          (session/connection-opened!)
          (setup-tasks-watch!))
        (session/connection-closed!)))))

(defn send!
  "Send command via discrete wire. Fire-and-forget.
   Signal will sync any resulting state changes."
  [command]
  (js/console.log "Rheon: Sending" (pr-str (:command/kind command)))
  (r/send! commands-wire command {}))

(defn request!
  "Send command and handle reply via discrete wire.
   Use for request/response patterns like fetching history."
  [command on-reply]
  (js/console.log "Rheon: Request" (pr-str (:command/kind command)))
  (r/send! commands-wire command {:on-reply on-reply}))

(defn disconnect!
  "Close Rheon connection."
  []
  (js/console.log "Rheon: Disconnecting...")
  (remove-watch (:state conn) :connection)
  (r/close! conn))

;; =============================================================================
;; Register callbacks with session (at end to avoid forward references)
;; =============================================================================

(reset! session/*connect! connect!)
(reset! session/*send! send!)
(reset! session/*request! request!)
