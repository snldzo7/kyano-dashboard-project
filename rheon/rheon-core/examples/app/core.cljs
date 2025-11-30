(ns app.core
  "Demo app shell with path-based routing.

   Routes:
   - /              Landing page with demo cards
   - /mouse-tracker Mouse tracker demo

   Architecture: Pure data-driven Replicant approach.
   State (data) in, hiccup (data) out. No local component state."
  (:require [replicant.dom :as d]
            [mouse-tracker.main :as mouse-tracker]))

;; =============================================================================
;; Router State
;; =============================================================================

(defonce app-state
  (atom {:route :home
         :demo-initialized? false
         :nav-open? false}))

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
;; Side Panel Navigation
;; =============================================================================

(defn side-panel-item
  "Render a single demo item in the side panel. Pure function."
  [{:keys [id path title description color active?]} dispatch!]
  [:a {:replicant/key id
       :href path
       :on {:click (fn [e]
                     (.preventDefault e)
                     (dispatch! {:type :navigate :path path})
                     (dispatch! {:type :close-nav}))}
       :class (str "block p-4 rounded-lg transition-all duration-200 "
                   (if active?
                     "bg-cyan-500/20 border border-cyan-500/50"
                     "hover:bg-white/10 border border-transparent"))}
   [:div {:class "flex items-center gap-3 mb-1"}
    [:div {:class "w-2 h-2 rounded-full"
           :style {:background-color color}}]
    [:span {:class "font-medium text-white"} title]]
   [:p {:class "text-sm text-white/50 ml-5"} description]])

(defn side-panel
  "Side panel overlay with demo navigation. Pure function of state."
  [{:keys [nav-open? route]} dispatch!]
  [:div {:replicant/key "side-panel"}
   ;; Backdrop overlay (click to close)
   (when nav-open?
     [:div {:class "fixed inset-0 bg-black/50 backdrop-blur-sm z-40 transition-opacity"
            :on {:click (fn [_] (dispatch! {:type :close-nav}))}}])

   ;; Side panel drawer
   [:div {:class (str "fixed top-0 left-0 h-full w-80 bg-gradient-to-b from-[#1a1a2e] to-[#0f0f1a] "
                      "border-r border-white/10 z-50 transform transition-transform duration-300 "
                      (if nav-open? "translate-x-0" "-translate-x-full"))}
    ;; Panel header
    [:div {:class "p-6 border-b border-white/10"}
     [:div {:class "flex items-center justify-between"}
      [:h2 {:class "text-xl font-bold text-white"} "Rheon Demos"]
      [:button {:class "p-2 rounded-lg hover:bg-white/10 text-white/60 hover:text-white transition-colors"
                :on {:click (fn [_] (dispatch! {:type :close-nav}))}}
       [:svg {:xmlns "http://www.w3.org/2000/svg" :width "20" :height "20"
              :viewBox "0 0 24 24" :fill "none" :stroke "currentColor"
              :stroke-width "2" :stroke-linecap "round" :stroke-linejoin "round"}
        [:line {:x1 "18" :y1 "6" :x2 "6" :y2 "18"}]
        [:line {:x1 "6" :y1 "6" :x2 "18" :y2 "18"}]]]]]

    ;; Demo list
    (into
     [:div {:class "p-4 space-y-2"}
      ;; Home link
      [:a {:replicant/key "home-link"
           :href "/"
           :on {:click (fn [e]
                         (.preventDefault e)
                         (dispatch! {:type :navigate :path "/"})
                         (dispatch! {:type :close-nav}))}
           :class (str "block p-4 rounded-lg transition-all duration-200 "
                       (if (= route :home)
                         "bg-cyan-500/20 border border-cyan-500/50"
                         "hover:bg-white/10 border border-transparent"))}
       [:div {:class "flex items-center gap-3"}
        [:svg {:xmlns "http://www.w3.org/2000/svg" :width "16" :height "16"
               :viewBox "0 0 24 24" :fill "none" :stroke "currentColor"
               :stroke-width "2" :class "text-white/60"}
         [:path {:d "M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"}]
         [:polyline {:points "9 22 9 12 15 12 15 22"}]]
        [:span {:class "font-medium text-white"} "Home"]]]]
     ;; Demo items
     (for [demo demos]
       (side-panel-item (assoc demo :active? (= route (:id demo))) dispatch!)))]])

(defn top-bar
  "Top navigation bar with hamburger menu. Pure function of state."
  [{:keys [route]} dispatch!]
  [:div {:class "fixed top-0 left-0 right-0 h-14 bg-black/40 backdrop-blur-md border-b border-white/10 z-30"}
   [:div {:class "max-w-6xl mx-auto px-4 h-full flex items-center justify-between"}
    ;; Hamburger menu button
    [:button {:class "p-2 rounded-lg hover:bg-white/10 text-white transition-colors"
              :on {:click (fn [_] (dispatch! {:type :toggle-nav}))}}
     [:svg {:xmlns "http://www.w3.org/2000/svg" :width "24" :height "24"
            :viewBox "0 0 24 24" :fill "none" :stroke "currentColor"
            :stroke-width "2" :stroke-linecap "round" :stroke-linejoin "round"}
      [:line {:x1 "3" :y1 "12" :x2 "21" :y2 "12"}]
      [:line {:x1 "3" :y1 "6" :x2 "21" :y2 "6"}]
      [:line {:x1 "3" :y1 "18" :x2 "21" :y2 "18"}]]]

    ;; Current page title
    [:span {:class "text-white font-medium"}
     (case route
       :home "Rheon Demos"
       :mouse-tracker "Mouse Tracker"
       :not-found "Not Found"
       "Rheon")]

    ;; Placeholder for symmetry
    [:div {:class "w-10"}]]])

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
  [:div {:class "min-h-screen bg-gradient-to-br from-[#1a1a2e] to-[#16213e] p-8 pt-20"}
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
                 flex items-center justify-center pt-14"}
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

(defonce nav-el (atom nil))
(defonce root-el (atom nil))

(defn dispatch!
  "Handle app-level actions. Pure state transitions."
  [action]
  (case (:type action)
    :navigate (navigate! (:path action))
    :toggle-nav (swap! app-state update :nav-open? not)
    :close-nav (swap! app-state assoc :nav-open? false)
    nil))

(defn render-nav!
  "Render navigation (top bar + side panel). Pure functions of state."
  []
  (let [state @app-state]
    (when @nav-el
      (d/render @nav-el
                [:div {:replicant/key "nav-container"}
                 (top-bar state dispatch!)
                 (side-panel state dispatch!)]))))

(defn render-shell! []
  (let [{:keys [route]} @app-state]
    ;; Always render nav
    (render-nav!)

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

  ;; Get DOM elements
  (reset! nav-el (.getElementById js/document "nav"))
  (reset! root-el (.getElementById js/document "app"))

  ;; Parse initial route
  (swap! app-state assoc :route (parse-route))

  ;; Handle browser back/forward
  (setup-popstate!)

  ;; Watch state for any changes that affect UI
  (add-watch app-state :router
             (fn [_ _ old-state new-state]
               (when (or (not= (:route old-state) (:route new-state))
                         (not= (:demo-initialized? old-state) (:demo-initialized? new-state))
                         (not= (:nav-open? old-state) (:nav-open? new-state)))
                 (render-shell!))))

  ;; Initial render
  (render-shell!)

  (js/console.log "Rheon Demo App - Ready!"))
