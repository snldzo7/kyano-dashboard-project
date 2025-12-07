(ns demo.wires
  "Wire definitions for the Clay video demo.

   Architecture: Server-side layout
   - Server owns state AND computes layout
   - Clients receive render commands, not state
   - Much more efficient - layout computed once, not per-client")

;; ============================================================================
;; RENDER COMMANDS (Server → Client)
;; ============================================================================

(def render-commands-ref
  "Render commands signal - server computes layout, clients just render."
  {:wire-id :render-commands
   :type :signal
   :initial []})

;; ============================================================================
;; VIEWPORT (Client → Server)
;; ============================================================================

(def viewport-ref
  "Client viewport dimensions - server needs this for layout."
  {:wire-id :viewport
   :type :signal
   :initial {:width 1280 :height 720}})

;; ============================================================================
;; COMMANDS (Client → Server)
;; ============================================================================

(def commands-ref
  "UI commands sent from client to server."
  {:wire-id :commands
   :type :discrete})

;; Command types:
;; {:type :select-document :index 0}
;; {:type :toggle-file-menu}
;; {:type :close-file-menu}
;; {:type :scroll :element-id :main-content :offset {:x 0 :y -10}}

;; ============================================================================
;; POINTER (Client → Server)
;; ============================================================================

(def pointer-ref
  "Mouse position stream - for hover detection."
  {:wire-id :pointer
   :type :stream})
