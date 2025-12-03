(ns demo.ui
  "Demo application UI views.

   All views use Foton primitives - no raw HTML/CSS.
   Organized into separate page modules for maintainability."
  (:require [datascript.core :as ds]
            [foton.theme :as theme]
            [demo.pages.shared :as shared]
            [demo.pages.home :as home]
            [demo.pages.colors :as colors]
            [demo.pages.typography :as typography]
            [demo.pages.primitives :as primitives]
            [demo.pages.effects :as effects]
            [demo.pages.layout :as layout]))

;; =============================================================================
;; 404 Not Found
;; =============================================================================

(defn render-not-found
  "Render 404 page using Foton."
  [_current-page theme-id]
  (shared/page-layout
   nil theme-id

   [:foton.css/frame {:align :center :justify :center :grow 1}
    [:foton/scale-in {:duration 400}
     [:foton.css/frame {:fill :card
                        :radius :xl
                        :padding :xl
                        :direction :vertical
                        :gap :lg
                        :align :center
                        :shadow :lg}
      [:foton.css/text {:size 72 :weight 700 :color [:text :muted]} "404"]
      [:foton.css/text {:size 24 :weight 600 :color [:text :primary]} "Page Not Found"]
      [:foton.css/text {:size 14 :color [:text :secondary] :text-align :center}
       "The page you're looking for doesn't exist."]
      [:ui/a {:ui/location {:location/page-id :pages/frontpage}}
       [:foton/button {:variant :primary}
        [:foton.css/frame {:direction :horizontal :gap :sm :align :center}
         [:foton.css/icon {:name :home :color :white :size :sm}]
         [:foton.css/text {:color :white} "Go Home"]]]]]]]))

;; =============================================================================
;; Page Router
;; =============================================================================

(defn render-page
  "Route to the correct page renderer."
  [db]
  (let [location (into {} (ds/entity db :ui/location))
        page-id (:location/page-id location)
        app (ds/entity db :system/app)
        theme-id (or (:ui/theme app) :dark)
        tokens (theme/get-theme theme-id)

        ;; Get the render function for the page
        render-fn (case page-id
                    :pages/frontpage home/render
                    :pages/colors colors/render
                    :pages/typography typography/render
                    :pages/primitives primitives/render
                    :pages/effects effects/render
                    :pages/layout layout/render
                    render-not-found)]

    ;; Render and apply theme
    (-> (render-fn page-id theme-id)
        (theme/apply-theme tokens))))
