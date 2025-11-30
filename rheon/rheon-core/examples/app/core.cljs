(ns app.core
  "Demo app shell with path-based routing.

   Routes:
   - /              Landing page with demo cards
   - /mouse-tracker Mouse tracker demo"
  (:require [replicant.dom :as d]
            [mouse-tracker.main :as mouse-tracker]))

;; =============================================================================
;; Router State
;; =============================================================================

(defonce app-state
  (atom {:route :home
         :demo-initialized? false}))

(defn parse-route
  "Parse pathname to route keyword."
  []
  (case js/window.location.pathname
    "/" :home
    "/mouse-tracker" :mouse-tracker
    :not-found))

(defn navigate!
  "Navigate to a path, updating history and route state."
  [path]
  (.pushState js/history nil nil path)
  (swap! app-state assoc
         :route (parse-route)
         :demo-initialized? false))

;; =============================================================================
;; Demo Registry
;; =============================================================================

(def demos
  [{:id :mouse-tracker
    :path "/mouse-tracker"
    :title "Mouse Tracker"
    :description "Real-time collaborative cursor tracking with WebSocket sync"
    :color "#00d9ff"}])

;; =============================================================================
;; Landing Page View
;; =============================================================================

(defn demo-card [{:keys [id path title description color]} dispatch!]
  [:a {:replicant/key id
       :href path
       :on {:click (fn [e]
                     (.preventDefault e)
                     (dispatch! {:type :navigate :path path}))}
       :class "block p-6 rounded-xl border border-white/10 bg-white/5
               hover:bg-white/10 hover:border-white/20
               transition-all duration-200 group"}
   [:div {:class "flex items-center gap-3 mb-3"}
    [:div {:class "w-3 h-3 rounded-full"
           :style {:background-color color}}]
    [:h2 {:class "text-xl font-semibold text-white group-hover:text-cyan-400 transition-colors"}
     title]]
   [:p {:class "text-white/60 text-sm"}
    description]])

(defn landing-page [dispatch!]
  [:div {:class "min-h-screen bg-gradient-to-br from-[#1a1a2e] to-[#16213e] p-8"}
   [:div {:class "max-w-4xl mx-auto"}
    ;; Header
    [:div {:class "text-center mb-12"}
     [:h1 {:class "text-4xl font-bold text-white mb-4"}
      "Rheon Demos"]
     [:p {:class "text-white/60 text-lg"}
      "Interactive demonstrations of the Rheon wire protocol"]]

    ;; Demo Grid
    (into [:div {:class "grid gap-6 md:grid-cols-2"}]
          (for [demo demos]
            (demo-card demo dispatch!)))]])

;; =============================================================================
;; Not Found View
;; =============================================================================

(defn not-found-page [dispatch!]
  [:div {:class "min-h-screen bg-gradient-to-br from-[#1a1a2e] to-[#16213e]
                 flex items-center justify-center"}
   [:div {:class "text-center"}
    [:h1 {:class "text-6xl font-bold text-white/20 mb-4"} "404"]
    [:p {:class "text-white/60 mb-8"} "Demo not found"]
    [:a {:href "/"
         :on {:click (fn [e]
                       (.preventDefault e)
                       (dispatch! {:type :navigate :path "/"}))}
         :class "text-cyan-400 hover:text-cyan-300"}
     "← Back to demos"]]])

;; =============================================================================
;; App Shell
;; =============================================================================

(defonce root-el (atom nil))

(defn dispatch!
  "Handle app-level actions."
  [action]
  (case (:type action)
    :navigate (navigate! (:path action))
    nil))

(defn render-shell! []
  (let [{:keys [route]} @app-state]
    (when @root-el
      (case route
        :home
        (d/render @root-el (landing-page dispatch!))

        :mouse-tracker
        ;; For demo routes, we let the demo take over rendering
        ;; Just ensure it's initialized
        (when-not (:demo-initialized? @app-state)
          (swap! app-state assoc :demo-initialized? true)
          (mouse-tracker/init!))

        :not-found
        (d/render @root-el (not-found-page dispatch!))))))

;; =============================================================================
;; Init
;; =============================================================================

(defn setup-popstate! []
  (.addEventListener js/window "popstate"
                     (fn [_]
                       (swap! app-state assoc
                              :route (parse-route)
                              :demo-initialized? false))))

(defn ^:export init! []
  (js/console.log "Rheon Demo App - Starting...")

  ;; Get root element
  (reset! root-el (.getElementById js/document "app"))

  ;; Parse initial route
  (swap! app-state assoc :route (parse-route))

  ;; Handle browser back/forward
  (setup-popstate!)

  ;; Watch state for route changes
  (add-watch app-state :router
             (fn [_ _ old-state new-state]
               (when (or (not= (:route old-state) (:route new-state))
                         (not= (:demo-initialized? old-state) (:demo-initialized? new-state)))
                 (render-shell!))))

  ;; Initial render
  (render-shell!)

  (js/console.log "Rheon Demo App - Ready!"))
