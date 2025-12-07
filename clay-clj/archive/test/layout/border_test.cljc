(ns clay.layout.border-test
  "Tests for betweenChildren border system."
  (:require [clojure.test :refer [deftest is testing]]
            [clay.layout.core :as layout]))

;; ============================================================================
;; TEST FIXTURES
;; ============================================================================

(def test-viewport {:width 800 :height 600})

(defn mock-measure-fn [text config]
  {:width (* (count text) 8) :height 16})

(def red-color {:r 255 :g 0 :b 0 :a 1})

;; ============================================================================
;; BETWEEN CHILDREN HORIZONTAL
;; ============================================================================

(deftest between-children-horizontal-test
  (testing "betweenChildren borders in horizontal layout"
    (let [result (-> (layout/begin-layout test-viewport)
                     ;; Parent with horizontal layout and betweenChildren border
                     (layout/open-element {:sizing {:width {:type :fixed :value 400}
                                                    :height {:type :fixed :value 100}}
                                           :layout-direction :left-to-right
                                           :child-gap 10})
                     (layout/configure-element :border {:color red-color
                                                        :width {:between-children 2}})
                     ;; Child 1: 100px wide
                     (layout/open-element {:sizing {:width {:type :fixed :value 100}
                                                    :height {:type :fixed :value 80}}})
                     (layout/close-element)
                     ;; Child 2: 100px wide
                     (layout/open-element {:sizing {:width {:type :fixed :value 100}
                                                    :height {:type :fixed :value 80}}})
                     (layout/close-element)
                     ;; Child 3: 100px wide
                     (layout/open-element {:sizing {:width {:type :fixed :value 100}
                                                    :height {:type :fixed :value 80}}})
                     (layout/close-element)
                     (layout/close-element)  ; Close parent
                     (layout/end-layout mock-measure-fn))
          commands (layout/get-render-commands result)
          ;; Filter for rectangle commands (betweenChildren borders are rectangles)
          rect-commands (filter #(= :rectangle (:command-type %)) commands)]
      ;; With 3 children, there should be 2 betweenChildren borders
      (is (= 2 (count rect-commands)))
      ;; Each border should be 2px wide (vertical line)
      (is (every? #(= 2 (get-in % [:bounding-box :width])) rect-commands))
      ;; Each border should be full parent height
      (is (every? #(= 100 (get-in % [:bounding-box :height])) rect-commands)))))

;; ============================================================================
;; BETWEEN CHILDREN VERTICAL
;; ============================================================================

(deftest between-children-vertical-test
  (testing "betweenChildren borders in vertical layout"
    (let [result (-> (layout/begin-layout test-viewport)
                     ;; Parent with vertical layout and betweenChildren border
                     (layout/open-element {:sizing {:width {:type :fixed :value 200}
                                                    :height {:type :fixed :value 300}}
                                           :layout-direction :top-to-bottom
                                           :child-gap 10})
                     (layout/configure-element :border {:color red-color
                                                        :width {:between-children 2}})
                     ;; Child 1: 80px tall
                     (layout/open-element {:sizing {:width {:type :fixed :value 180}
                                                    :height {:type :fixed :value 80}}})
                     (layout/close-element)
                     ;; Child 2: 80px tall
                     (layout/open-element {:sizing {:width {:type :fixed :value 180}
                                                    :height {:type :fixed :value 80}}})
                     (layout/close-element)
                     (layout/close-element)  ; Close parent
                     (layout/end-layout mock-measure-fn))
          commands (layout/get-render-commands result)
          rect-commands (filter #(= :rectangle (:command-type %)) commands)]
      ;; With 2 children, there should be 1 betweenChildren border
      (is (= 1 (count rect-commands)))
      ;; The border should be full parent width (horizontal line)
      (is (= 200 (get-in (first rect-commands) [:bounding-box :width])))
      ;; The border should be 2px tall
      (is (= 2 (get-in (first rect-commands) [:bounding-box :height]))))))

;; ============================================================================
;; NO BORDERS WHEN SINGLE CHILD OR NO CHILDREN
;; ============================================================================

(deftest no-borders-single-child-test
  (testing "No betweenChildren borders with single child"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element {:sizing {:width {:type :fixed :value 200}
                                                    :height {:type :fixed :value 100}}})
                     (layout/configure-element :border {:color red-color
                                                        :width {:between-children 2}})
                     ;; Single child
                     (layout/open-element {:sizing {:width {:type :fixed :value 100}
                                                    :height {:type :fixed :value 80}}})
                     (layout/close-element)
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          commands (layout/get-render-commands result)
          rect-commands (filter #(= :rectangle (:command-type %)) commands)]
      ;; No betweenChildren borders with single child
      (is (= 0 (count rect-commands))))))

(deftest no-borders-no-children-test
  (testing "No betweenChildren borders with no children"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element {:sizing {:width {:type :fixed :value 200}
                                                    :height {:type :fixed :value 100}}})
                     (layout/configure-element :border {:color red-color
                                                        :width {:between-children 2}})
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          commands (layout/get-render-commands result)
          rect-commands (filter #(= :rectangle (:command-type %)) commands)]
      ;; No betweenChildren borders with no children
      (is (= 0 (count rect-commands))))))

;; ============================================================================
;; ZERO WIDTH MEANS NO BORDERS
;; ============================================================================

(deftest zero-width-no-borders-test
  (testing "Zero betweenChildren width produces no borders"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element {:sizing {:width {:type :fixed :value 200}
                                                    :height {:type :fixed :value 100}}})
                     (layout/configure-element :border {:color red-color
                                                        :width {:between-children 0}})
                     ;; Two children
                     (layout/open-element {:sizing {:width {:type :fixed :value 80}
                                                    :height {:type :fixed :value 80}}})
                     (layout/close-element)
                     (layout/open-element {:sizing {:width {:type :fixed :value 80}
                                                    :height {:type :fixed :value 80}}})
                     (layout/close-element)
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          commands (layout/get-render-commands result)
          rect-commands (filter #(= :rectangle (:command-type %)) commands)]
      ;; No borders when width is 0
      (is (= 0 (count rect-commands))))))

;; ============================================================================
;; COMBINED WITH REGULAR BORDER
;; ============================================================================

(deftest combined-with-regular-border-test
  (testing "betweenChildren can be combined with regular border"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element {:sizing {:width {:type :fixed :value 200}
                                                    :height {:type :fixed :value 100}}})
                     (layout/configure-element :border {:color red-color
                                                        :width {:left 1 :right 1 :top 1 :bottom 1
                                                                :between-children 2}})
                     ;; Two children
                     (layout/open-element {:sizing {:width {:type :fixed :value 80}
                                                    :height {:type :fixed :value 80}}})
                     (layout/close-element)
                     (layout/open-element {:sizing {:width {:type :fixed :value 80}
                                                    :height {:type :fixed :value 80}}})
                     (layout/close-element)
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          commands (layout/get-render-commands result)
          border-commands (filter #(= :border (:command-type %)) commands)
          rect-commands (filter #(= :rectangle (:command-type %)) commands)]
      ;; Should have 1 regular border command
      (is (= 1 (count border-commands)))
      ;; Should have 1 betweenChildren rectangle
      (is (= 1 (count rect-commands))))))
