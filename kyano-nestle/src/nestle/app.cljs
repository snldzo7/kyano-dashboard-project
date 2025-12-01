(ns nestle.app
  "Main application entry point"
  (:require [replicant.dom :as r]
            [nestle.state :as state]
            [nestle.dispatch :as dispatch]
            [nestle.views.dashboard :as dashboard]
            [nestle.views.lineage :as lineage]
            [nestle.views.scenario :as scenario]
            [nestle.views.time-travel :as time-travel]
            [nestle.views.collaborative :as collaborative]
            [kyano.ui.primitives :as p]))

;; -----------------------------------------------------------------------------
;; Navigation - Matching React styling exactly
;; -----------------------------------------------------------------------------

(def nav-tabs
  [{:id :dashboard :label "Dashboard"
    ;; House icon - matches React exactly
    :icon "M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"}
   {:id :lineage :label "Lineage"
    ;; Grid/Template icon - matches React exactly
    :icon "M4 5a1 1 0 011-1h14a1 1 0 011 1v2a1 1 0 01-1 1H5a1 1 0 01-1-1V5zM4 13a1 1 0 011-1h6a1 1 0 011 1v6a1 1 0 01-1 1H5a1 1 0 01-1-1v-6zM16 13a1 1 0 011-1h2a1 1 0 011 1v6a1 1 0 01-1 1h-2a1 1 0 01-1-1v-6z"}
   {:id :scenario :label "Scenario & Decide"
    ;; Paint brush icon - matches React exactly
    :icon "M7 21a4 4 0 01-4-4V5a2 2 0 012-2h4a2 2 0 012 2v12a4 4 0 01-4 4zm0 0h12a2 2 0 002-2v-4a2 2 0 00-2-2h-2.343M11 7.343l1.657-1.657a2 2 0 012.828 0l2.829 2.829a2 2 0 010 2.828l-8.486 8.485M7 17h.01"}
   {:id :collab :label "\uD83E\uDD1D Collaborative Room"
    ;; People icon - matches React exactly
    :icon "M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"}
   {:id :time-travel :label "\u23F1 Time Travel"
    ;; Clock icon - matches React exactly
    :icon "M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"}])

(defn nav-tab
  "Navigation tab - pure data-driven, no functions"
  [{:keys [id label icon active?]}]
  [:button {:class (p/classes
                    "flex items-center gap-2 px-3 py-1.5 rounded-md text-sm font-medium transition-all"
                    (if active?
                      "bg-cyan-600 text-white"
                      "text-slate-400 hover:text-slate-200 hover:bg-slate-700"))
            :on {:click [:app/set-view id]}}
   [:svg {:class "w-4 h-4" :fill "none" :viewBox "0 0 24 24" :stroke "currentColor"}
    [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d icon}]]
   label])

(defn header []
  (let [current-view (state/get-view)]
    [:header {:class "border-b border-slate-800 bg-slate-900"}
     [:div {:class "max-w-7xl mx-auto px-6 py-4"}
      [:div {:class "flex items-center justify-between"}
       ;; Logo + Title
       [:div {:class "flex items-center gap-4"}
        [:div {:class "w-10 h-10 rounded-lg bg-gradient-to-br from-cyan-500 to-blue-600 flex items-center justify-center"}
         [:svg {:class "w-6 h-6 text-white" :fill "none" :viewBox "0 0 24 24" :stroke "currentColor"}
          [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                  :d "M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"}]]]
        [:div
         [:h1 {:class "text-lg font-semibold tracking-tight text-white"} "Working Capital Control Tower"]
         [:p {:class "text-xs text-slate-400"} "Fact-Based Decision Intelligence • Temporal Immutability"]]]

       ;; Nav tabs
       [:nav {:class "flex items-center gap-1 bg-slate-800 rounded-lg p-1"}
        (for [{:keys [id label icon]} nav-tabs]
          ^{:key id}
          (nav-tab {:id id
                    :label label
                    :icon icon
                    :active? (= current-view id)}))]

       ;; Status indicator
       [:div {:class "flex items-center gap-3"}
        [:div {:class "text-right"}
         [:div {:class "text-xs text-slate-500"} "Last Update"]
         [:div {:class "text-sm text-slate-300 font-mono"}
          (.toLocaleTimeString (js/Date.))]]
        [:div {:class "w-2 h-2 rounded-full bg-emerald-500 animate-pulse"}]]]]]))

;; -----------------------------------------------------------------------------
;; View Router
;; -----------------------------------------------------------------------------

(defn placeholder-view [title]
  [:div {:class "flex items-center justify-center min-h-[60vh]"}
   [:div {:class "text-center"}
    [:div {:class "text-6xl mb-4"} "🚧"]
    [:h2 {:class "text-xl font-semibold text-white mb-2"} title]
    [:p {:class "text-slate-400"} "Coming soon..."]]])

(defn current-view []
  (case (state/get-view)
    :dashboard (dashboard/dashboard-view)
    :lineage (lineage/lineage-view)
    :scenario (scenario/scenario-view)
    :collab (collaborative/collaborative-view)
    :time-travel (time-travel/time-travel-view)
    (dashboard/dashboard-view)))

;; -----------------------------------------------------------------------------
;; Main App
;; -----------------------------------------------------------------------------

(defn app []
  [:div {:class "min-h-screen bg-slate-950 text-slate-100"}
   (header)
   [:main {:class "max-w-7xl mx-auto px-6 py-8"}
    (current-view)]])

;; -----------------------------------------------------------------------------
;; Render & Init
;; -----------------------------------------------------------------------------

(defn render! []
  (r/render
   (.getElementById js/document "app")
   (app)))

(defn init! []
  (println "Initializing Kyano Nestle app...")
  ;; Initialize data-driven action dispatcher
  (dispatch/init!)
  (render!)
  ;; Re-render on state changes
  (add-watch state/!app-state :render
             (fn [_ _ _ _] (render!))))
