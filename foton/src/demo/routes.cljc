(ns demo.routes
  "Demo application route definitions."
  (:require [domkm.silk :as silk]))

(def routes
  "Demo routes - pattern matching for pages."
  (silk/routes
   [[:pages/typography [["typography"]]]
    [:pages/colors [["colors"]]]
    [:pages/primitives [["primitives"]]]
    [:pages/effects [["effects"]]]
    [:pages/layout [["layout"]]]
    [:pages/frontpage []]]))  ;; Empty pattern LAST - catches "/" only
