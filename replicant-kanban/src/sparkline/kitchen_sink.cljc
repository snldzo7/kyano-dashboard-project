(ns sparkline.kitchen-sink
  (:require [sparkline.core :as sp]))

(defn- section-title [title subtitle]
  [:<>
   [:div.border-b.border-gray-700.pb-2.mb-10
    [:h2.text-xl.font-medium title]
    [:p.text-gray-500 subtitle]]])

(def bar-examples
  [{:points [14 19 7 9 18 20 17 33 21 23 45 23 12 34] :size [200 50]}
   {:points [18 13 19 14 14 3 4 8 7 9 2 8 0 20 7 3 2 5 12 15]
    :colors [:white :gray] :size [300 50] :gap 5}
   {:points [16 2 5 5 20 11 12 19 17 18 13 8 3 5 14 19 6 1 12 4]
    :size [100 50] :gap 2 :colors [:purple]}
   {:points [20 14 4 3 17 2 8 12 19 11 6 17 5 8 13 12 17 9 1 0]
    :size [300 50] :gap 15 :colors ["#ffff00"]}
   {:points [4 8 10 3 16 15 0 0 8 18 11 14 14 0 11 14 16 14 15 6] :size [200 50]}
   {:points [10 4 10 15 18 10 12 8 11 4 13 14 6 18 17 12 0 13 14 3] :size [200 50]}])

(def line-examples
  [{:type :line :points [15 3 17 15 7 11 11 5 0 16 1 13 1 12 13 8 16 4 0 0]
    :size [200 30] :colors [:white] :stroke-width 3}
   {:type :line :points [3 8 0 14 11 0 7 1 15 15 20 12 8 13 13 5 1 12 1 11]
    :size [200 30] :colors [:gray] :stroke-width 1}
   {:type :line :points [2 16 8 18 2 11 19 12 7 12 12 15 0 0 18 1 7 18 1 13]
    :size [200 30] :colors [:yellow] :stroke-width 2}])

(def pie-examples
  [{:type :pie :points [10 16 8 18 5 11] :size [40 40] :colors [:purple :brown :aqua :yellow]}
   {:type :pie :points [8 44] :size [40 40] :colors ["transparent" :silver]}
   {:type :pie :points [11 14 20 21 37] :size [40 40] :colors [:indigo :silver :linen :khaki :crimson]}
   {:type :pie :points [44 99] :size [40 40] :colors [:silver :gray]}])

(def stacked-examples
  [{:type :stacked :points [33 23 44] :size [300 20] :colors [:white :purple :orange]}
   {:type :stacked :points [14 19 44] :size [300 20] :gap 0 :colors [:white :purple :orange]}
   {:type :stacked :points [14 19] :size [300 20] :gap 0 :colors [:white :gray]}])

(def label-examples
  [{:points [14 19 7]
    :labels [:Apple :Banana :Mango]
    :format "%label is %point (%percent)"}
   {:type :pie
    :points [14 19 7]
    :labels ["Apple" "Banana" "Mango"]
    :format "%label is %point (%percent)"
    :size [40 40]
    :colors [:tan :brown :aqua :yellow]}
   {:type :stacked
    :points [14 19 7]
    :labels [:Apple :Banana] ;; shorter labels: missing label => blank
    :format "%label is %point (%percent)"
    :size [300 20] :gap 0
    :colors [:white :tan :purple]}])

(def initial-state
  {:charts {:some-1 {:type :stacked
                     :points [14 19 7]
                     :size [300 20]
                     :colors [:white :purple :orange :gray :yellow]}
            :some-2 {:type :line
                     :points [15 3 17 15 7 11 11 5 0 16 1 13 1 12 13 8 16 4 0 0]
                     :size [300 30]
                     :colors ["rgba(255,255,255,0.9)"] ; rgba supported  [oai_citation:4‡MDN Web Docs](https://developer.mozilla.org/en-US/docs/Web/CSS/Reference/Values/color_value/rgb?utm_source=chatgpt.com)
                     :stroke-width 2}
            :some-3 {:points [18 13 19 14 14 3 4 8 7 9 2 8 0 20 7 3 2 5 12 15]
                     :size [300 30]
                     :gap 5}}})

(defn page [{:keys [charts]}]
  [:div.mx-auto.p-6.md:p-24.bg-black.text-white {:style {:max-width "1200px"}}

   [:header.mb-10
    [:h1.text-3xl.font-bold "Sparkline Kitchensink"]
    [:p.text-gray-500
     "All the code for this project is available on "
     [:a.underline {:href "https://github.com/mitjafelicijan/sparklines"} "https://github.com/mitjafelicijan/sparklines"]
     "."]]

   (section-title "Bar charts" "Bar charts are the default charts if no type is provided.")
   [:section.grid.grid-cols-1.md:grid-cols-2.lg:grid-cols-3.gap-12.mb-16
    (for [[i cfg] (map-indexed vector bar-examples)]
      [:div {:replicant/key i} [::sp/sparkline cfg]])]

   (section-title "Line charts" "Line charts can have a bunch of data and still be readable.")
   [:section.grid.grid-cols-1.md:grid-cols-3.gap-12.mb-16
    (for [[i cfg] (map-indexed vector line-examples)]
      [:div {:replicant/key i} [::sp/sparkline cfg]])]

   (section-title "Pie charts" "For better readability, use pie charts only when you have small amount of data points.")
   [:section.grid.grid-cols-2.lg:grid-cols-4.gap-12.mb-16
    (for [[i cfg] (map-indexed vector pie-examples)]
      [:div {:replicant/key i} [::sp/sparkline cfg]])]

   (section-title "Stacked charts" "Stacked charts can only be horizontal. Rotate with CSS to have them be vertical.")
   [:section.grid.grid-cols-1.md:grid-cols-2.lg:grid-cols-3.gap-12.mb-16
    (for [[i cfg] (map-indexed vector stacked-examples)]
      [:div {:replicant/key i} [::sp/sparkline cfg]])]

   (section-title "Using labels" "Hover over charts to see detailed data about that datapoint.")
   [:section.grid.grid-cols-1.md:grid-cols-2.lg:grid-cols-3.gap-12.mb-16
    (for [[i cfg] (map-indexed vector label-examples)]
      [:div {:replicant/key i} [::sp/sparkline cfg]])]

   (section-title "Programmatic update" "Either click on the button or let timers do it periodically.")
   [:section.grid.grid-cols-1.md:grid-cols-2.lg:grid-cols-3.gap-12.mb-16
    [:div.flex.flex-col.gap-6
     [:div [::sp/sparkline (get charts :some-1)]]
     [:div
      [:button.border.border-gray-600.bg-gray-800.hover:bg-gray-700.rounded.px-3.py-1
       {:on {:click [[:charts/randomize :some-1 5]]}}
       "Manually update"]]]

    [:div.flex.flex-col.gap-4
     [:div [::sp/sparkline (get charts :some-2)]]
     [:div.text-gray-400.italic "Updates every 1 second"]]

    [:div.flex.flex-col.gap-4
     [:div [::sp/sparkline (get charts :some-3)]]
     [:div.text-gray-400.italic "Updates every 2 seconds"]]]])