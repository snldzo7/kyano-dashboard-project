(ns demo.ui
  "Minimal UI for debugging layout.")

;; Simple test layout
(defn test-layout
  "Minimal layout for debugging positioning."
  [_state]
  [:col {:id :root-col
         :size :grow
         :padding 20
         :gap 10
         :bg [50 50 50]}

   ;; Row 1: Header
   [:row {:id :header
          :size [:grow 50]
          :padding 10
          :gap 10
          :bg [80 80 80]}
    [:box {:id :btn1 :size [100 30] :bg [120 120 120]}
     [:text "Button 1" {:font-size 14 :color [255 255 255]}]]
    [:box {:id :btn2 :size [100 30] :bg [120 120 120]}
     [:text "Button 2" {:font-size 14 :color [255 255 255]}]]]

   ;; Row 2: Content
   [:row {:id :content
          :size :grow
          :gap 10
          :bg [100 50 50]}
    ;; Sidebar
    [:col {:id :sidebar
           :size [200 :grow]
           :padding 10
           :gap 5
           :bg [70 70 70]}
     [:text "Sidebar" {:font-size 16 :color [255 255 255]}]
     [:box {:id :item1 :size [:grow 30] :bg [100 100 100]}
      [:text "Item 1" {:font-size 14 :color [255 255 255]}]]
     [:box {:id :item2 :size [:grow 30] :bg [100 100 100]}
      [:text "Item 2" {:font-size 14 :color [255 255 255]}]]]
    ;; Main
    [:col {:id :main
           :size :grow
           :padding 10
           :bg [60 60 60]}
     [:text "Main Content" {:font-size 20 :color [255 255 255]}]]]])

;; Alias for video-demo-layout
(def video-demo-layout test-layout)
