(ns clay.client.mouse-tracker-v2
  "Mouse Tracker v2 - Modern ClojureScript with Garden, js-interop, and Replicant"
  (:require [missionary.core :as m]
            [cognitect.transit :as transit]
            [garden.core :refer [css]]
            [applied-science.js-interop :as j]
            [replicant.dom :as r]))

;; =============================================================================
;; State
;; =============================================================================

(defonce !app-state
  (atom {:coords {:x 0 :y 0}
         :status "Initializing..."
         :status-class "disconnected"}))

(defonce ws-connection (atom nil))
(defonce mouse-flow (atom nil))

;; =============================================================================
;; Styles (Garden CSS)
;; =============================================================================

(def styles
  (css
   [:body {:margin 0
           :padding "20px"
           :font-family "-apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif"
           :background "linear-gradient(135deg, #667eea 0%, #764ba2 100%)"
           :color "white"
           :height "100vh"
           :display "flex"
           :flex-direction "column"
           :align-items "center"
           :justify-content "center"}]
   [:.container {:text-align "center"
                 :background "rgba(255, 255, 255, 0.1)"
                 :padding "40px"
                 :border-radius "20px"
                 :backdrop-filter "blur(10px)"
                 :box-shadow "0 8px 32px rgba(0, 0, 0, 0.1)"
                 :min-width "400px"}]
   [:.title {:margin "0 0 10px 0"
             :font-size "2.5em"}]
   [:.subtitle {:font-size "1.2em"
                :opacity 0.9
                :margin-bottom "30px"}]
   [:.coords-display {:background "rgba(0, 0, 0, 0.3)"
                      :padding "30px"
                      :border-radius "15px"
                      :margin "20px 0"
                      :font-size "2em"
                      :font-family "'Courier New', monospace"
                      :letter-spacing "2px"}]
   [:.coords-label {:font-size "0.5em"
                    :opacity 0.7
                    :margin-bottom "10px"}]
   [:.status {:margin-top "20px"
              :font-size "1em"
              :padding "10px"
              :border-radius "5px"
              :background "rgba(0, 0, 0, 0.2)"}]
   [:.connected {:background "rgba(76, 175, 80, 0.3)"}]
   [:.disconnected {:background "rgba(244, 67, 54, 0.3)"}]))

;; =============================================================================
;; UI (Replicant Hiccup)
;; =============================================================================

(defn ui-view [{:keys [coords status status-class]}]
  [:div.container
   [:div.title "🖱️ Mouse Tracker v2"]
   [:div.subtitle "Garden + js-interop + Replicant"]
   [:div.coords-display
    [:div.coords-label "CURRENT COORDINATES"]
    [:div (str "x: " (:x coords) ", y: " (:y coords))]]
   [:div {:class (str "status " status-class)} status]])

(defn render-ui! []
  (r/render (j/call js/document :getElementById "app")
            (ui-view @!app-state)))

(defn inject-styles! []
  (let [style (j/call js/document :createElement "style")]
    (j/assoc! style :textContent styles)
    (j/call (j/get js/document :head) :appendChild style)))

;; =============================================================================
;; WebSocket (js-interop)
;; =============================================================================

(defn update-status! [message class]
  (swap! !app-state assoc :status message :status-class class)
  (render-ui!))

(defn connect-websocket! [url]
  (let [ws (js/WebSocket. url)]
    (j/assoc! ws :onopen
              (fn [_]
                (println "WebSocket connected to" url)
                (update-status! "✅ Connected - Move your mouse!" "connected")))
    (j/assoc! ws :onclose
              (fn [_]
                (println "WebSocket disconnected")
                (update-status! "❌ Disconnected from server" "disconnected")))
    (j/assoc! ws :onerror
              (fn [err]
                (println "WebSocket error:" err)
                (update-status! "❌ Connection error" "disconnected")))
    (reset! ws-connection ws)
    ws))

(defn send-coordinates! [coords]
  (when-let [ws @ws-connection]
    (when (= (j/get ws :readyState) js/WebSocket.OPEN)
      (let [writer (transit/writer :json)
            transit-str (transit/write writer coords)]
        (j/call ws :send transit-str)))))

;; =============================================================================
;; Mouse Tracking (Missionary)
;; =============================================================================

(defn start-mouse-tracking! []
  (when @mouse-flow
    (println "⏹️ Stopping existing mouse tracking...")
    (@mouse-flow)
    (reset! mouse-flow nil))

  (println "▶️ Starting mouse tracking v2...")
  (connect-websocket! "ws://localhost:9090")

  (let [handler (fn [e]
                  (swap! !app-state assoc :coords {:x (j/get e :clientX)
                                                   :y (j/get e :clientY)})
                  (render-ui!))

        ;; Network sender - samples every 50ms (throttled)
        net-task (m/reduce
                  (fn [_ _]
                    (let [coords (:coords @!app-state)]
                      (println "📤 Sending:" coords)
                      (send-coordinates! coords)))
                  nil
                  (m/sample
                   (m/ap (loop [] (m/? (m/sleep 50)) (recur)))
                   (m/watch !app-state)))]

    ;; Add DOM listener
    (j/call js/document :addEventListener "mousemove" handler)
    (println "✅ Mouse listener attached")

    ;; Start network task
    (let [net-cancel (net-task
                      (fn [result] (println "✨ Network task done:" result))
                      (fn [error] (println "❌ Network error:" error)))]

      (println "🚀 Mouse tracking v2 started")

      ;; Store cleanup function
      (reset! mouse-flow
              (fn []
                (println "🧹 Cleaning up...")
                (j/call js/document :removeEventListener "mousemove" handler)
                (net-cancel))))))

(defn stop-mouse-tracking! []
  (when @mouse-flow
    (println "Stopping mouse tracking...")
    (@mouse-flow)
    (reset! mouse-flow nil))
  (when-let [ws @ws-connection]
    (j/call ws :close)
    (reset! ws-connection nil)))

;; =============================================================================
;; Lifecycle
;; =============================================================================

(defn init! []
  (println "Initializing mouse tracker v2...")
  (inject-styles!)
  (render-ui!)
  (start-mouse-tracking!))

(defn reload! []
  (println "Reloading v2...")
  (stop-mouse-tracking!)
  (render-ui!)
  (js/setTimeout start-mouse-tracking! 100))

(comment
  (start-mouse-tracking!)
  (stop-mouse-tracking!)
  @!app-state)
