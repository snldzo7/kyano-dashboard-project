(ns foton.demo
  "Demo components for all Foton primitives")

(defn primitives-demo
  "Demo showing all 9 Foton primitives"
  []
  [:foton.css/frame {:direction :vertical :gap :lg :padding :lg :fill :background}

   ;; Header
   [:foton.css/text {:preset :title} "Foton Primitives Demo"]
   [:foton.css/text {:color :secondary} "All 9 primitives rendered with raw CSS"]

   ;; 1. Frame
   [:foton.css/frame {:direction :vertical :gap :sm}
    [:foton.css/text {:preset :heading} "1. Frame"]
    [:foton.css/frame {:fill :card :radius :lg :padding :md :direction :horizontal :gap :md}
     [:foton.css/frame {:fill :surface :radius :md :padding :sm :width 100 :height 60 :align :center :justify :center}
      [:foton.css/text {:color :secondary :size 12} "Box 1"]]
     [:foton.css/frame {:fill :surface :radius :md :padding :sm :width 100 :height 60 :align :center :justify :center}
      [:foton.css/text {:color :secondary :size 12} "Box 2"]]
     [:foton.css/frame {:fill :surface :radius :md :padding :sm :width 100 :height 60 :align :center :justify :center}
      [:foton.css/text {:color :secondary :size 12} "Box 3"]]]]

   ;; 2. Text
   [:foton.css/frame {:direction :vertical :gap :sm}
    [:foton.css/text {:preset :heading} "2. Text"]
    [:foton.css/frame {:fill :card :radius :lg :padding :md :direction :vertical :gap :xs}
     [:foton.css/text {:preset :title} "Title Text"]
     [:foton.css/text {:preset :heading} "Heading Text"]
     [:foton.css/text {:preset :body} "Body text - Lorem ipsum dolor sit amet"]
     [:foton.css/text {:preset :small} "Small text for captions"]
     [:foton.css/text {:color [:status :good]} "Success colored text"]
     [:foton.css/text {:color [:status :bad]} "Error colored text"]]]

   ;; 3. Icon
   [:foton.css/frame {:direction :vertical :gap :sm}
    [:foton.css/text {:preset :heading} "3. Icon"]
    [:foton.css/frame {:fill :card :radius :lg :padding :md :direction :horizontal :gap :md :align :center}
     [:foton.css/icon {:name :check :size :sm :color [:status :good]}]
     [:foton.css/icon {:name :warning :size :md :color [:status :warning]}]
     [:foton.css/icon {:name :x :size :lg :color [:status :bad]}]
     [:foton.css/icon {:name :info :size :md :color [:status :info]}]
     [:foton.css/icon {:name :home :size :md}]
     [:foton.css/icon {:name :settings :size :md}]
     [:foton.css/icon {:name :search :size :md}]
     [:foton.css/icon {:name :chart :size :md}]]]

   ;; 4. Input
   [:foton.css/frame {:direction :vertical :gap :sm}
    [:foton.css/text {:preset :heading} "4. Input"]
    [:foton.css/frame {:fill :card :radius :lg :padding :md :direction :vertical :gap :sm}
     [:foton.css/input {:placeholder "Default input" :size :md}]
     [:foton.css/input {:placeholder "Small input" :size :sm}]
     [:foton.css/input {:placeholder "Large input" :size :lg}]
     [:foton.css/input {:placeholder "Disabled input" :disabled true}]]]

   ;; 5. Textarea
   [:foton.css/frame {:direction :vertical :gap :sm}
    [:foton.css/text {:preset :heading} "5. Textarea"]
    [:foton.css/frame {:fill :card :radius :lg :padding :md :direction :vertical :gap :sm}
     [:foton.css/textarea {:placeholder "Write something..." :rows 3}]
     [:foton.css/textarea {:placeholder "Disabled textarea" :rows 2 :disabled true}]]]

   ;; 6. Link
   [:foton.css/frame {:direction :vertical :gap :sm}
    [:foton.css/text {:preset :heading} "6. Link"]
    [:foton.css/frame {:fill :card :radius :lg :padding :md :direction :horizontal :gap :md}
     [:foton.css/link {:href "#"} "Default link"]
     [:foton.css/link {:href "#" :underline :none} "No underline"]
     [:foton.css/link {:href "#" :target "_blank"} "External link"]]]

   ;; 7. Image
   [:foton.css/frame {:direction :vertical :gap :sm}
    [:foton.css/text {:preset :heading} "7. Image"]
    [:foton.css/frame {:fill :card :radius :lg :padding :md :direction :horizontal :gap :md}
     [:foton.css/image {:src "https://picsum.photos/100/100" :alt "Demo image" :radius :md :width 100 :height 100}]
     [:foton.css/image {:src "https://picsum.photos/150/100" :alt "Wide image" :radius :lg :width 150 :height 100 :fit :cover}]]]

   ;; 8. Video
   [:foton.css/frame {:direction :vertical :gap :sm}
    [:foton.css/text {:preset :heading} "8. Video"]
    [:foton.css/frame {:fill :card :radius :lg :padding :md}
     [:foton.css/video {:src "https://www.w3schools.com/html/mov_bbb.mp4"
                        :controls true
                        :width 320
                        :radius :md}]]]

   ;; 9. SVG
   [:foton.css/frame {:direction :vertical :gap :sm}
    [:foton.css/text {:preset :heading} "9. SVG"]
    [:foton.css/frame {:fill :card :radius :lg :padding :md :direction :horizontal :gap :md :align :center}
     [:foton.css/svg {:viewBox "0 0 24 24" :size :md :fill "currentColor" :color [:status :good]}
      [:path {:d "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"}]]
     [:foton.css/svg {:viewBox "0 0 24 24" :size :lg :fill "currentColor" :color [:status :warning]}
      [:path {:d "M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z"}]]
     [:foton.css/svg {:viewBox "0 0 100 100" :width 60 :height 60}
      [:circle {:cx "50" :cy "50" :r "40" :stroke "#3b82f6" :stroke-width "4" :fill "none"}]
      [:rect {:x "30" :y "30" :width "40" :height "40" :fill "#3b82f6" :opacity "0.5"}]]]]])
