(ns state-datascript.ui
  (:require [datascript.core :as ds]
            [foton.demo :as demo]))

(defn render-frontpage [db _]
  (let [app (ds/entity db :system/app)
        clicks (:clicks app)]
    [:div {:style {:margin-top "2rem" :padding "1rem"}}
     [:h1 "Hello world"]
     [:p "Started at " (:app/started-at app)]
     [:button
      {:on {:click [[:counter/inc app]]}}
      "Click me"]
     (when (< 0 clicks)
       [:p
        "Button was clicked "
        clicks
        (if (= 1 clicks) " time" " times")])
     [:p [:ui/a {:ui/location {:location/page-id :pages/episode
                               :location/params {:episode/id "s2e1"}}}
          "Episode 1"]]

     ;; Foton demo link
     [:div {:style {:margin-top "2rem" :padding "1rem"}}
      [:h2 "Foton Demo"]
      [:p [:ui/a {:ui/location {:location/page-id :pages/demo}} "View Foton Primitives Demo"]]
      [:p [:ui/a {:ui/location {:location/page-id :pages/demo2}} "View Demo 2 (No Foton)"]]]]))

    
(defn render-episode [_ location]
  [:main
   [:h1 "Episode " (-> location :location/params :episode/id)]
   (if (-> location :location/hash-params :description)
     (list
      [:p "It's an episode of Parens of the dead"]
      [:ui/a {:ui/location (update location :location/hash-params dissoc :description)}
       "Hide description"])
     [:ui/a {:ui/location (assoc-in location [:location/hash-params :description] "1")}
      "Show description"])
   [:p
    [:ui/a {:ui/location {:location/page-id :pages/frontpage}}
     "Back to frontpage"]]])

(defn render-not-found [_ _]
  [:h1 "Not found"])

(defn render-demo2 [_ location]
  ;; Simple demo with NO Foton - just plain hiccup
  [:div {:style {:min-height "100vh" :padding "2rem" :background "#f0f0f0"}}
   [:h1 "Demo 2 - No Foton"]
   [:p "This page uses only plain hiccup, no Foton primitives."]
   [:p "Current location: " (pr-str location)]
   [:hr]
   [:p [:ui/a {:ui/location {:location/page-id :pages/frontpage}} "Back to Frontpage"]]
   [:p [:ui/a {:ui/location {:location/page-id :pages/demo}} "Go to Foton Demo"]]
   [:p [:ui/a {:ui/location {:location/page-id :pages/episode
                             :location/params {:episode/id "s2e1"}}} "Go to Episode"]]])

(defn render-demo [_ location]
  [:div {:style {:min-height "100vh" :background "linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%)"}}
   ;; Nav bar
   [:nav {:style {:display "flex" :gap "1rem" :padding "1rem" :background "rgba(255,255,255,0.1)" :color "white"}}
    [:ui/a {:ui/location {:location/page-id :pages/frontpage}} "Home"]
    [:span {:style {:font-weight "bold"}} "Foton Demo"]]
   ;; Header
   [:div {:style {:padding "2rem" :text-align "center"}}
    [:h1 {:style {:color "white" :font-size "2.5rem" :margin "0"}} "Foton Primitives"]
    [:p {:style {:color "rgba(255,255,255,0.9)" :font-size "1.2rem"}} "UI primitives rendered with raw CSS"]]
   ;; Demo content
   [:div {:style {:padding "1rem"}}
    (demo/primitives-demo)]])

(defn render-page [db]
  (let [location (into {} (ds/entity db :ui/location))
        page-id (:location/page-id location)
        f (case page-id
            :pages/frontpage render-frontpage
            :pages/episode render-episode
            :pages/demo render-demo
            :pages/demo2 render-demo2
            render-not-found)]
    (f db location)))
