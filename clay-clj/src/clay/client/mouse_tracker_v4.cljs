(ns clay.client.mouse-tracker-v4
  "Mouse Tracker v4 - Bidirectional Clock Sync with Sente.
   Features:
   - Garden CSS for styling
   - Replicant for hiccup-based DOM rendering
   - js-interop for cleaner JavaScript interop
   - Missionary for throttled mouse event streaming
   - Sente with request/reply for clock synchronization

   Key difference from v3: Uses send-fn with callback to receive
   server response containing clock gap."
  (:require [taoensso.sente :as sente]
            [garden.core :refer [css]]
            [applied-science.js-interop :as j]
            [replicant.dom :as r]))

;; =============================================================================
;; Garden CSS Styles
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
                      :padding "20px"
                      :border-radius "15px"
                      :margin "15px 0"
                      :font-size "1.8em"
                      :font-family "'Courier New', monospace"
                      :letter-spacing "2px"}]
   [:.clock-display {:background "rgba(0, 0, 0, 0.3)"
                     :padding "20px"
                     :border-radius "15px"
                     :margin "15px 0"
                     :font-size "1.5em"
                     :font-family "'Courier New', monospace"}]
   [:.label {:font-size "0.6em"
             :opacity 0.7
             :margin-bottom "8px"}]
   [:.status {:margin-top "20px"
              :font-size "1em"
              :padding "10px"
              :border-radius "5px"
              :background "rgba(0, 0, 0, 0.2)"}]
   [:.connected {:background "rgba(76, 175, 80, 0.3)"}]
   [:.disconnected {:background "rgba(244, 67, 54, 0.3)"}]))

;; =============================================================================
;; Application State
;; =============================================================================

(defonce !app-state
  (atom {:coords {:x 0 :y 0}
         :clock-gap nil
         :server-time nil
         :status "Initializing..."
         :status-class "disconnected"}))

;; =============================================================================
;; Sente Client Setup
;; =============================================================================

(defonce !sente (atom nil))

(defn get-csrf-token
  "Extract CSRF token from DOM element"
  []
  (when-let [el (j/call js/document :getElementById "sente-csrf-token")]
    (j/call el :getAttribute "data-csrf-token")))

(defn init-sente!
  "Initialize Sente channel socket client for v4 server"
  []
  (println "Initializing Sente v4 client...")
  (let [{:keys [chsk ch-recv send-fn state]}
        (sente/make-channel-socket-client!
         "/chsk"
         (get-csrf-token)
         {:type :auto
          :host "localhost:9092"  ;; v4 server port
          :wrap-recv-evs? false})]
    (reset! !sente {:chsk chsk
                    :ch-recv ch-recv
                    :send-fn send-fn
                    :state state})
    (println "Sente v4 client initialized")))

;; Forward declaration for render-ui!
(declare render-ui!)

;; KEY FEATURE: Send coords with client timestamp and receive reply
(defn send-coords!
  "Send mouse coordinates with client timestamp.
   Receives server response with clock gap via callback."
  [coords]
  (when-let [{:keys [send-fn]} @!sente]
    (let [client-time (.now js/Date)
          payload (assoc coords :client-time client-time)]
      ;; Send with 5000ms timeout and callback for reply
      (send-fn [:mouse/coords payload] 5000
               (fn [reply]
                 (when (map? reply)
                   (let [{:keys [server-time clock-gap]} reply]
                     (swap! !app-state assoc
                            :server-time server-time
                            :clock-gap clock-gap)
                     (render-ui!))))))))

;; =============================================================================
;; Replicant UI (Hiccup)
;; =============================================================================

(defn ui-view
  "Render the UI as hiccup"
  [{:keys [coords clock-gap server-time status status-class]}]
  [:div.container
   [:div.title "Mouse Tracker v4"]
   [:div.subtitle "Bidirectional Clock Sync"]
   [:div.coords-display
    [:div.label "COORDINATES"]
    [:div (str "x: " (:x coords) ", y: " (:y coords))]]
   [:div.clock-display
    [:div.label "CLOCK GAP (server - client)"]
    [:div (if clock-gap
            (str clock-gap " ms")
            "waiting...")]]
   [:div {:class (str "status " status-class)} status]])

(defn render-ui!
  "Render UI to DOM using Replicant"
  []
  (r/render (j/call js/document :getElementById "app")
            (ui-view @!app-state)))

;; =============================================================================
;; Sente Event Handler
;; =============================================================================

(defn handle-sente-event!
  "Handle incoming Sente events"
  [{:keys [id ?data] :as ev}]
  (case id
    :chsk/state
    (let [[_ new-state] ?data]
      (println "Sente state change:" new-state)
      (if (:open? new-state)
        (do
          (swap! !app-state assoc
                 :status "Connected via Sente"
                 :status-class "connected")
          (render-ui!))
        (do
          (swap! !app-state assoc
                 :status "Disconnected"
                 :status-class "disconnected")
          (render-ui!))))

    :chsk/handshake
    (let [[?uid] ?data]
      (println "Sente handshake complete. UID:" ?uid))

    :chsk/recv
    (let [[event-id event-data] ?data]
      (println "Received from server:" event-id event-data))

    :chsk/ws-ping
    nil  ;; Ignore heartbeat pings

    ;; Default
    (println "Sente event:" id)))

(defonce !router (atom nil))

(defn start-sente-router!
  "Start the Sente event router"
  []
  (when @!router
    (@!router)
    (reset! !router nil))
  (when-let [{:keys [ch-recv]} @!sente]
    (println "Starting Sente v4 client router...")
    (reset! !router (sente/start-client-chsk-router! ch-recv handle-sente-event!))))

;; =============================================================================
;; Mouse Tracking with Simple Throttle
;; =============================================================================

(defonce mouse-flow (atom nil))
(defonce last-send-time (atom 0))

(defn start-mouse-tracking!
  "Start mouse tracking with simple throttle (no Missionary for network)"
  []
  ;; Clean up existing flow
  (when @mouse-flow
    (println "Stopping existing mouse tracking...")
    (@mouse-flow)
    (reset! mouse-flow nil))

  (println "Starting mouse tracking...")

  (let [;; DOM event handler - updates UI immediately AND sends throttled
        handler (fn [e]
                  (let [x (j/get e :clientX)
                        y (j/get e :clientY)
                        now (.now js/Date)]
                    ;; Always update UI immediately
                    (swap! !app-state assoc :coords {:x x :y y})
                    (render-ui!)
                    ;; Throttle network sends to every 50ms
                    (when (> (- now @last-send-time) 50)
                      (reset! last-send-time now)
                      (send-coords! {:x x :y y}))))]

    ;; Attach DOM listener
    (j/call js/document :addEventListener "mousemove" handler)
    (println "Mouse listener attached")

    ;; Store cleanup function
    (reset! mouse-flow
            (fn []
              (println "Cleaning up mouse tracking...")
              (j/call js/document :removeEventListener "mousemove" handler)))))

(defn stop-mouse-tracking!
  "Stop mouse tracking"
  []
  (when @mouse-flow
    (@mouse-flow)
    (reset! mouse-flow nil)))

;; =============================================================================
;; Lifecycle Functions
;; =============================================================================

(defn inject-styles!
  "Inject Garden CSS into DOM"
  []
  (let [style (j/call js/document :createElement "style")]
    (j/assoc! style :textContent styles)
    (j/call (j/get js/document :head) :appendChild style)))

(defn init!
  "Initialize the application"
  []
  (println "Initializing Mouse Tracker v4...")
  (inject-styles!)
  (render-ui!)
  (init-sente!)
  (start-sente-router!)
  (start-mouse-tracking!)
  (println "Mouse Tracker v4 initialized"))

(defn reload!
  "Hot reload handler for shadow-cljs"
  []
  (println "Reloading Mouse Tracker v4...")
  (stop-mouse-tracking!)
  (render-ui!)
  (js/setTimeout start-mouse-tracking! 100))

;; =============================================================================
;; REPL Helpers
;; =============================================================================

(comment
  ;; Check Sente state
  @!sente

  ;; Check app state
  @!app-state

  ;; Manual send
  (send-coords! {:x 100 :y 200})

  ;; Restart tracking
  (stop-mouse-tracking!)
  (start-mouse-tracking!)
  )
