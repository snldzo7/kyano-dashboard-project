(ns clay.layout.floating-test
  "Tests for floating elements system."
  (:require [clojure.test :refer [deftest is testing]]
            [clay.layout.core :as layout]
            [clay.layout.floating :as floating]))

;; ============================================================================
;; TEST FIXTURES
;; ============================================================================

(def test-viewport {:width 800 :height 600})

(defn mock-measure-fn [text config]
  {:width (* (count text) 8) :height 16})

;; ============================================================================
;; ATTACH POINT CALCULATION TESTS
;; ============================================================================

(deftest attach-point-x-calculation
  (testing "X coordinate calculation for attach points"
    (let [box {:x 100 :y 50 :width 200 :height 100}]
      ;; Left points
      (is (= 100 (#'floating/get-attach-x :left-top box)))
      (is (= 100 (#'floating/get-attach-x :left-center box)))
      (is (= 100 (#'floating/get-attach-x :left-bottom box)))
      ;; Center points
      (is (= 200 (#'floating/get-attach-x :center-top box)))
      (is (= 200 (#'floating/get-attach-x :center-center box)))
      (is (= 200 (#'floating/get-attach-x :center-bottom box)))
      ;; Right points
      (is (= 300 (#'floating/get-attach-x :right-top box)))
      (is (= 300 (#'floating/get-attach-x :right-center box)))
      (is (= 300 (#'floating/get-attach-x :right-bottom box))))))

(deftest attach-point-y-calculation
  (testing "Y coordinate calculation for attach points"
    (let [box {:x 100 :y 50 :width 200 :height 100}]
      ;; Top points
      (is (= 50 (#'floating/get-attach-y :left-top box)))
      (is (= 50 (#'floating/get-attach-y :center-top box)))
      (is (= 50 (#'floating/get-attach-y :right-top box)))
      ;; Center points
      (is (= 100 (#'floating/get-attach-y :left-center box)))
      (is (= 100 (#'floating/get-attach-y :center-center box)))
      (is (= 100 (#'floating/get-attach-y :right-center box)))
      ;; Bottom points
      (is (= 150 (#'floating/get-attach-y :left-bottom box)))
      (is (= 150 (#'floating/get-attach-y :center-bottom box)))
      (is (= 150 (#'floating/get-attach-y :right-bottom box))))))

;; ============================================================================
;; FLOATING POSITION CALCULATION TESTS
;; ============================================================================

(deftest floating-position-left-top-to-right-top
  (testing "Floating element left-top attaches to parent right-top"
    (let [config {:attach-to :parent
                  :attach-points {:element :left-top :parent :right-top}
                  :offset {:x 0 :y 0}}
          floating-dims {:width 100 :height 50}
          parent-box {:x 100 :y 100 :width 200 :height 150}
          root-box {:x 0 :y 0 :width 800 :height 600}
          position (floating/calculate-floating-position config floating-dims parent-box root-box)]
      ;; Parent right edge is at x=300, floating element left-top attaches there
      (is (= 300 (:x position)))
      (is (= 100 (:y position))))))

(deftest floating-position-center-to-center
  (testing "Floating element center attaches to parent center"
    (let [config {:attach-to :parent
                  :attach-points {:element :center-center :parent :center-center}
                  :offset {:x 0 :y 0}}
          floating-dims {:width 100 :height 50}
          parent-box {:x 100 :y 100 :width 200 :height 150}
          root-box {:x 0 :y 0 :width 800 :height 600}
          position (floating/calculate-floating-position config floating-dims parent-box root-box)]
      ;; Parent center is at (200, 175), floating center should align there
      ;; So floating x = 200 - 50 = 150, y = 175 - 25 = 150
      (is (= 150 (:x position)))
      (is (= 150 (:y position))))))

(deftest floating-position-with-offset
  (testing "Floating element with offset"
    (let [config {:attach-to :parent
                  :attach-points {:element :left-top :parent :left-top}
                  :offset {:x 10 :y 20}}
          floating-dims {:width 100 :height 50}
          parent-box {:x 100 :y 100 :width 200 :height 150}
          root-box {:x 0 :y 0 :width 800 :height 600}
          position (floating/calculate-floating-position config floating-dims parent-box root-box)]
      (is (= 110 (:x position)))
      (is (= 120 (:y position))))))

(deftest floating-position-attach-to-root
  (testing "Floating element attaches to root (absolute positioning)"
    (let [config {:attach-to :root
                  :attach-points {:element :center-center :parent :center-center}
                  :offset {:x 0 :y 0}}
          floating-dims {:width 100 :height 50}
          parent-box {:x 100 :y 100 :width 200 :height 150}
          root-box {:x 0 :y 0 :width 800 :height 600}
          position (floating/calculate-floating-position config floating-dims parent-box root-box)]
      ;; Should be centered in root, not parent
      ;; Root center is (400, 300), floating center aligns there
      ;; x = 400 - 50 = 350, y = 300 - 25 = 275
      (is (= 350 (:x position)))
      (is (= 275 (:y position))))))

;; ============================================================================
;; INTEGRATION TESTS
;; ============================================================================

(deftest floating-element-in-layout
  (testing "Floating element is positioned correctly in full layout"
    (let [result (-> (layout/begin-layout test-viewport)
                     ;; Parent container
                     (layout/open-element {:sizing {:width {:type :fixed :value 200}
                                                    :height {:type :fixed :value 100}}})
                     ;; Floating child (tooltip-like)
                     (layout/open-element {:sizing {:width {:type :fixed :value 80}
                                                    :height {:type :fixed :value 30}}})
                     (layout/configure-element :floating
                                               {:attach-to :parent
                                                :attach-points {:element :left-top
                                                               :parent :right-top}
                                                :offset {:x 5 :y 0}
                                                :z-index 10})
                     (layout/close-element)  ; Close floating
                     (layout/close-element)  ; Close parent
                     (layout/end-layout mock-measure-fn))
          parent (get (:layout-elements result) 1)
          floating-elem (get (:layout-elements result) 2)]
      ;; Parent is at (0, 0) with width 200
      ;; Floating should be at parent's right edge + 5 offset
      (is (= 205 (:x (:bounding-box floating-elem))))
      (is (= 0 (:y (:bounding-box floating-elem)))))))

(deftest z-index-sorting
  (testing "Render commands are sorted by z-index"
    (let [result (-> (layout/begin-layout test-viewport)
                     ;; Element with z-index 0 (default)
                     (layout/open-element {:sizing {:width {:type :fixed :value 100}
                                                    :height {:type :fixed :value 100}}})
                     (layout/configure-element :background {:color {:r 255 :g 0 :b 0 :a 1}})
                     (layout/close-element)
                     ;; Element with z-index 10
                     (layout/open-element {:sizing {:width {:type :fixed :value 100}
                                                    :height {:type :fixed :value 100}}})
                     (layout/configure-element :background {:color {:r 0 :g 255 :b 0 :a 1}})
                     (layout/configure-element :floating
                                               {:attach-to :root
                                                :attach-points {:element :left-top :parent :left-top}
                                                :z-index 10})
                     (layout/close-element)
                     ;; Element with z-index 5
                     (layout/open-element {:sizing {:width {:type :fixed :value 100}
                                                    :height {:type :fixed :value 100}}})
                     (layout/configure-element :background {:color {:r 0 :g 0 :b 255 :a 1}})
                     (layout/configure-element :floating
                                               {:attach-to :root
                                                :attach-points {:element :left-top :parent :left-top}
                                                :z-index 5})
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          commands (layout/get-render-commands result)
          elements (:layout-elements result)
          ;; Build a map from element ID to z-index
          id->z-index (into {}
                            (map (fn [el]
                                   [(get-in el [:id :id])
                                    (floating/get-element-z-index el)])
                                 elements))
          ;; Get z-indices for commands in order
          z-indices (mapv #(get id->z-index (get-in % [:id :id]) 0) commands)]
      ;; Commands should be returned as a vector
      (is (vector? commands))
      ;; Z-indices should be in ascending order
      (is (= z-indices (sort z-indices))))))
