(ns demo.server
  "Immediate Mode UI Application - Server Side.

   Server-side layout architecture:
   - Server owns UI state (internal atom)
   - Server computes layout on state/viewport change
   - Server sends render commands to clients
   - Clients just render - no layout computation

   This is more efficient:
   - Layout computed once, not per-client
   - Less data over wire (commands < full state)
   - Server can batch updates"
  (:require [demo.ui :as ui]
            [demo.wires :as wires]
            [clay.hiccup2 :as hiccup]
            [clay.layout :as layout]
            [rheon.core :as r]
            [clojure.string :as str]))

;; ============================================================================
;; CONNECTION
;; ============================================================================

(defonce conn (atom nil))

;; ============================================================================
;; INTERNAL STATE (not a wire - server-owned)
;; ============================================================================

(defonce ui-state
  (atom {:selected-document 0
         :file-menu-open? false
         :scroll-state {}}))

;; ============================================================================
;; WIRES
;; ============================================================================

(defonce render-commands-wire (atom nil))
(defonce viewport-wire (atom nil))
(defonce commands-wire (atom nil))
(defonce pointer-wire (atom nil))

;; ============================================================================
;; TEXT MEASUREMENT (server-side)
;; ============================================================================

(defn create-measure-fn
  "Create a simple text measurement function.
   In production, could use Java AWT or a font metrics library."
  []
  (fn [text {:keys [font-size] :or {font-size 16}}]
    ;; Simple approximation: ~0.6 * font-size per character
    (let [char-width (* 0.6 font-size)
          words (str/split (str text) #"\s+")]
      {:width (* (count text) char-width)
       :height font-size
       :words (mapv (fn [word]
                      {:text (str word " ")
                       :width (* (inc (count word)) char-width)})
                    words)})))

(defonce measure-fn (create-measure-fn))

;; ============================================================================
;; LAYOUT COMPUTATION
;; ============================================================================

(defn compute-layout!
  "Compute layout and send render commands to clients.
   Called when state or viewport changes."
  []
  (when @render-commands-wire
    (let [state @ui-state
          viewport (if @viewport-wire
                     @(:value @viewport-wire)
                     {:width 1280 :height 720})

          ;; 1. Generate hiccup from state
          hiccup-data (ui/video-demo-layout state)

          ;; 2. Parse hiccup to tree
          tree (hiccup/parse viewport hiccup-data measure-fn)

          ;; 3. Layout tree to render commands
          commands (layout/layout tree {:measure-fn measure-fn
                                        :scroll-state (:scroll-state state)})]

      ;; 4. Send to all clients
      (r/signal! @render-commands-wire commands)

      (println "[server] Layout computed:" (count commands) "commands"))))

;; ============================================================================
;; COMMAND HANDLERS
;; ============================================================================

(defn handle-command
  "Handle commands from clients. Returns acknowledgment."
  [{:keys [type] :as cmd}]
  (case type
    :select-document
    (do
      (swap! ui-state assoc :selected-document (:index cmd))
      (compute-layout!)
      {:status :ok})

    :toggle-file-menu
    (do
      (swap! ui-state update :file-menu-open? not)
      (compute-layout!)
      {:status :ok})

    :close-file-menu
    (do
      (swap! ui-state assoc :file-menu-open? false)
      (compute-layout!)
      {:status :ok})

    :scroll
    (do
      (swap! ui-state assoc-in [:scroll-state (:element-id cmd)] (:offset cmd))
      (compute-layout!)
      {:status :ok})

    ;; Unknown command
    {:status :error :message (str "Unknown command: " type)}))

;; ============================================================================
;; VIEWPORT CHANGE HANDLER
;; ============================================================================

(defn on-viewport-change
  "Re-layout when client viewport changes."
  [viewport]
  (println "[server] Viewport changed:" viewport)
  (compute-layout!))

;; ============================================================================
;; POINTER HANDLING
;; ============================================================================

(defn handle-pointer
  "Handle pointer position updates from clients."
  [_pointer]
  ;; Could use for hover state, collaborative cursors, etc.
  ;; For now, no-op
  nil)

;; ============================================================================
;; LIFECYCLE
;; ============================================================================

(defn start!
  "Start the server."
  ([] (start! 8088))
  ([port]
   (println "[clay-server] Starting on port" port "...")

   ;; Create connection
   (reset! conn (r/connection {:transport :ws-server :port port}))

   ;; Create wires
   (reset! render-commands-wire (r/wire @conn wires/render-commands-ref))
   (reset! viewport-wire (r/wire @conn wires/viewport-ref))
   (reset! commands-wire (r/wire @conn wires/commands-ref))
   (reset! pointer-wire (r/wire @conn wires/pointer-ref))

   ;; Watch viewport changes - re-layout on resize
   (r/watch @viewport-wire on-viewport-change)

   ;; Setup command handler
   (r/reply! @commands-wire handle-command)

   ;; Setup pointer listener (sampled)
   (r/listen @pointer-wire handle-pointer {:backpressure :sample :interval-ms 100})

   ;; Initial layout
   (compute-layout!)

   (println "[clay-server] Ready! Waiting for clients...")))

(defn stop!
  "Stop the server."
  []
  (when @conn
    (r/close! @conn)
    (reset! conn nil)
    (reset! render-commands-wire nil)
    (reset! viewport-wire nil)
    (reset! commands-wire nil)
    (reset! pointer-wire nil)
    (println "[clay-server] Stopped.")))

(defn restart!
  "Restart the server."
  []
  (stop!)
  (start!))

;; ============================================================================
;; REPL HELPERS
;; ============================================================================

(comment
  ;; Start server
  (start!)

  ;; Stop server
  (stop!)

  ;; Check current state
  @ui-state

  ;; Manually update state (triggers re-layout)
  (swap! ui-state assoc :selected-document 2)
  (compute-layout!)

  ;; Toggle file menu
  (swap! ui-state update :file-menu-open? not)
  (compute-layout!)
  )
