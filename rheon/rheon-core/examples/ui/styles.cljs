(ns ui.styles
  "Minimal styles for Rheon UI.
   Most styling is done via Tailwind CSS classes.
   This file only handles things Tailwind CDN can't do easily.")

;; Body gradient background - inject once at app init
(def body-styles
  "body {
    font-family: 'Inter', system-ui, sans-serif;
    background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
    background-attachment: fixed;
    min-height: 100vh;
    color: rgba(255, 255, 255, 0.95);
  }")

(defn inject!
  "Inject minimal styles into document head. Call once at app init."
  []
  (let [existing (.getElementById js/document "rheon-ui-styles")
        style-el (or existing (.createElement js/document "style"))]
    (set! (.-type style-el) "text/css")
    (set! (.-id style-el) "rheon-ui-styles")
    (set! (.-innerHTML style-el) body-styles)
    (when-not existing
      (.appendChild (.-head js/document) style-el))
    true))
