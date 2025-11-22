(ns clay.client.mouse-tracker
  (:require [missionary.core :as m]
            [clojure.string :as str]
            [cognitect.transit :as transit]))

(defonce ws-connection (atom nil))
(defonce mouse-flow (atom nil))

;; DOM Helpers
(defn create-element [tag & {:keys [class id text]}]
  (let [el (.createElement js/document tag)]
    (when class (set! (.-className el) class))
    (when id (set! (.-id el) id))
    (when text (set! (.-textContent el) text))
    el))

(defn set-style! [el styles]
  (doseq [[k v] styles]
    (aset (.-style el) (name k) v)))

(defn append-child! [parent child]
  (.appendChild parent child))

;; CSS Styles
(defn inject-styles! []
  (let [style (create-element "style")
        css "
body {
  margin: 0;
  padding: 20px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.container {
  text-align: center;
  background: rgba(255, 255, 255, 0.1);
  padding: 40px;
  border-radius: 20px;
  backdrop-filter: blur(10px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  min-width: 400px;
}
.title {
  margin: 0 0 10px 0;
  font-size: 2.5em;
}
.subtitle {
  font-size: 1.2em;
  opacity: 0.9;
  margin-bottom: 30px;
}
.coords-display {
  background: rgba(0, 0, 0, 0.3);
  padding: 30px;
  border-radius: 15px;
  margin: 20px 0;
  font-size: 2em;
  font-family: 'Courier New', monospace;
  letter-spacing: 2px;
}
.coords-label {
  font-size: 0.5em;
  opacity: 0.7;
  margin-bottom: 10px;
}
.status {
  margin-top: 20px;
  font-size: 1em;
  padding: 10px;
  border-radius: 5px;
  background: rgba(0, 0, 0, 0.2);
}
.connected {
  background: rgba(76, 175, 80, 0.3);
}
.disconnected {
  background: rgba(244, 67, 54, 0.3);
}
"]
    (set! (.-textContent style) css)
    (append-child! (.-head js/document) style)))

;; UI Construction
(defn build-ui! []
  (let [app (.getElementById js/document "app")
        container (create-element "div" :class "container")

        title (create-element "div" :class "title" :text "🖱️ Mouse Tracker")
        subtitle (create-element "div" :class "subtitle" :text "Missionary Network Bridge Test")

        coords-display (create-element "div" :class "coords-display")
        coords-label (create-element "div" :class "coords-label" :text "CURRENT COORDINATES")
        coords-value (create-element "div" :id "coords-value" :text "x: 0, y: 0")

        status (create-element "div" :id "status" :class "status disconnected" :text "Initializing...")]

    (append-child! coords-display coords-label)
    (append-child! coords-display coords-value)

    (append-child! container title)
    (append-child! container subtitle)
    (append-child! container coords-display)
    (append-child! container status)

    (set! (.-innerHTML app) "")
    (append-child! app container)))

(defn update-coords-display! [coords]
  (when-let [el (.getElementById js/document "coords-value")]
    (set! (.-textContent el) (str "x: " (:x coords) ", y: " (:y coords)))))

(defn update-status! [message class]
  (when-let [el (.getElementById js/document "status")]
    (set! (.-textContent el) message)
    (set! (.-className el) (str "status " class))))

;; State to hold mouse coordinates
(defonce !coords (atom {:x 0 :y 0}))

(defn connect-websocket! [url]
  (let [ws (js/WebSocket. url)]
    (set! (.-onopen ws)
          (fn [_]
            (println "WebSocket connected to" url)
            (update-status! "✅ Connected - Move your mouse!" "connected")))
    (set! (.-onclose ws)
          (fn [_]
            (println "WebSocket disconnected")
            (update-status! "❌ Disconnected from server" "disconnected")))
    (set! (.-onerror ws)
          (fn [err]
            (println "WebSocket error:" err)
            (update-status! "❌ Connection error" "disconnected")))
    (reset! ws-connection ws)
    ws))

(defn send-coordinates! [coords]
  (when-let [ws @ws-connection]
    (when (= (.-readyState ws) js/WebSocket.OPEN)
      (let [writer (transit/writer :json)
            transit-str (transit/write writer coords)]
        (.send ws transit-str)))))


(defn start-mouse-tracking! []
  (when @mouse-flow
    (println "⏹️ Stopping existing mouse tracking...")
    (@mouse-flow)
    (reset! mouse-flow nil))

  (println "▶️ Starting simplified mouse tracking...")
  (connect-websocket! "ws://localhost:9090")

  ;; Simple DOM handler updates atom
  (let [handler (fn [e]
                  (reset! !coords {:x (.-clientX e) :y (.-clientY e)}))

        ;; UI updater - watches atom, updates on every change
        ui-task (m/reduce
                 (fn [_ coords]
                   (println "🎨 UI update:" coords)
                   (update-coords-display! coords)
                   coords)
                 nil
                 (m/watch !coords))

        ;; Network sender - samples atom every 50ms (throttled)
        net-task (m/reduce
                  (fn [_ coords]
                    (println "📤 Sending:" coords)
                    (send-coordinates! coords)
                    coords)
                  nil
                  (m/sample
                   (m/ap (loop [] (m/? (m/sleep 50)) (recur)))
                   (m/watch !coords)))]

    ;; Add DOM listener
    (.addEventListener js/document "mousemove" handler)
    (println "✅ Mouse listener attached")

    ;; Start both Missionary tasks
    (let [ui-cancel (ui-task
                     (fn [result] (println "✨ UI task done:" result))
                     (fn [error] (println "❌ UI error:" error)))
          net-cancel (net-task
                      (fn [result] (println "✨ Network task done:" result))
                      (fn [error] (println "❌ Network error:" error)))]

      (println "🚀 Both tasks started")

      ;; Store cleanup function
      (reset! mouse-flow
              (fn []
                (println "🧹 Cleaning up...")
                (.removeEventListener js/document "mousemove" handler)
                (ui-cancel)
                (net-cancel))))))

(defn stop-mouse-tracking! []
  (when @mouse-flow
    (println "Stopping mouse tracking...")
    (@mouse-flow)
    (reset! mouse-flow nil))
  (when-let [ws @ws-connection]
    (.close ws)
    (reset! ws-connection nil)))

(defn init! []
  (println "Initializing mouse tracker...")
  (inject-styles!)
  (build-ui!)
  (start-mouse-tracking!))

(defn reload! []
  (println "Reloading...")
  (stop-mouse-tracking!)
  (build-ui!)
  (js/setTimeout start-mouse-tracking! 100))

(comment
  (start-mouse-tracking!)
  (stop-mouse-tracking!))
