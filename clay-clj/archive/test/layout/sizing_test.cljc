(ns clay.layout.sizing-test
  "Tests for Clay sizing algorithm.

   Tests the four sizing types (matching Clay.h Clay__SizingType):
   - :fit   - Shrink to content (CLAY__SIZING_TYPE_FIT)
   - :grow  - Expand to fill (CLAY__SIZING_TYPE_GROW)
   - :fixed - Exact pixel value (CLAY__SIZING_TYPE_FIXED)
   - :percent - Percentage of parent (CLAY__SIZING_TYPE_PERCENT)"
  (:require [clojure.test :refer [deftest is testing]]
            [clay.layout.core :as layout]
            [clay.layout.sizing :as sizing]))

;; ============================================================================
;; TEST FIXTURES
;; ============================================================================

(def test-viewport {:width 800 :height 600})

(defn mock-measure-fn [text config]
  {:width (* (count text) 8) :height 16})

;; ============================================================================
;; FIXED SIZING TESTS
;; ============================================================================

(deftest fixed-width-honored
  (testing "Fixed width is set exactly"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element {:sizing {:width {:type :fixed :value 200}
                                                    :height {:type :fixed :value 100}}})
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          child (get (:layout-elements result) 1)]
      (is (= 200 (get-in child [:dimensions :width])))
      (is (= 100 (get-in child [:dimensions :height]))))))

(deftest fixed-with-min-max-constraints
  (testing "Fixed sizing respects min/max constraints"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element {:sizing {:width {:type :fixed :value 50 :min 100}
                                                    :height {:type :fixed :value 200 :max 150}}})
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          child (get (:layout-elements result) 1)]
      (is (= 100 (get-in child [:dimensions :width])) "Width clamped to min")
      (is (= 150 (get-in child [:dimensions :height])) "Height clamped to max"))))

;; ============================================================================
;; FIT SIZING TESTS
;; ============================================================================

(deftest fit-shrinks-to-content
  (testing "Fit sizing shrinks to content size"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element {:sizing {:width {:type :fit}
                                                    :height {:type :fit}}
                                           :layout-direction :left-to-right})
                     ;; Add fixed-size children
                     (layout/open-element {:sizing {:width {:type :fixed :value 100}
                                                    :height {:type :fixed :value 50}}})
                     (layout/close-element)
                     (layout/open-element {:sizing {:width {:type :fixed :value 100}
                                                    :height {:type :fixed :value 50}}})
                     (layout/close-element)
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          container (get (:layout-elements result) 1)]
      ;; Width should be sum of children (200), height should be max (50)
      (is (= 200 (get-in container [:dimensions :width])))
      (is (= 50 (get-in container [:dimensions :height]))))))

(deftest fit-with-padding
  (testing "Fit sizing includes padding"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element {:sizing {:width {:type :fit}
                                                    :height {:type :fit}}
                                           :padding {:top 10 :right 10 :bottom 10 :left 10}
                                           :layout-direction :left-to-right})
                     (layout/open-element {:sizing {:width {:type :fixed :value 100}
                                                    :height {:type :fixed :value 50}}})
                     (layout/close-element)
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          container (get (:layout-elements result) 1)]
      ;; Should include padding: 100 + 10 + 10 = 120 width, 50 + 10 + 10 = 70 height
      (is (= 120 (get-in container [:dimensions :width])))
      (is (= 70 (get-in container [:dimensions :height]))))))

(deftest fit-with-gap
  (testing "Fit sizing includes child gaps"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element {:sizing {:width {:type :fit}
                                                    :height {:type :fit}}
                                           :child-gap 10
                                           :layout-direction :left-to-right})
                     (layout/open-element {:sizing {:width {:type :fixed :value 100}
                                                    :height {:type :fixed :value 50}}})
                     (layout/close-element)
                     (layout/open-element {:sizing {:width {:type :fixed :value 100}
                                                    :height {:type :fixed :value 50}}})
                     (layout/close-element)
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          container (get (:layout-elements result) 1)]
      ;; Should include gap: 100 + 10 + 100 = 210 width
      (is (= 210 (get-in container [:dimensions :width]))))))

;; ============================================================================
;; GROW SIZING TESTS
;; ============================================================================

(deftest grow-fills-remaining-space
  (testing "Grow sizing fills remaining space"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element {:sizing {:width {:type :grow}
                                                    :height {:type :fixed :value 100}}
                                           :layout-direction :left-to-right})
                     ;; Fixed child
                     (layout/open-element {:sizing {:width {:type :fixed :value 200}
                                                    :height {:type :fixed :value 50}}})
                     (layout/close-element)
                     ;; Grow child should fill remaining
                     (layout/open-element {:sizing {:width {:type :grow}
                                                    :height {:type :fixed :value 50}}})
                     (layout/close-element)
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          grow-child (get (:layout-elements result) 3)]
      ;; Parent is 800 (viewport), fixed child is 200, so grow should be 600
      (is (= 600 (get-in grow-child [:dimensions :width]))))))

(deftest multiple-grow-splits-evenly
  (testing "Multiple grow elements split space evenly"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element {:sizing {:width {:type :grow}
                                                    :height {:type :fixed :value 100}}
                                           :layout-direction :left-to-right})
                     ;; Two grow children
                     (layout/open-element {:sizing {:width {:type :grow}
                                                    :height {:type :fixed :value 50}}})
                     (layout/close-element)
                     (layout/open-element {:sizing {:width {:type :grow}
                                                    :height {:type :fixed :value 50}}})
                     (layout/close-element)
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          grow1 (get (:layout-elements result) 2)
          grow2 (get (:layout-elements result) 3)]
      ;; Each should get 400 (800 / 2)
      (is (= 400 (get-in grow1 [:dimensions :width])))
      (is (= 400 (get-in grow2 [:dimensions :width]))))))

(deftest grow-respects-min-constraint
  (testing "Grow sizing respects minimum constraint"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element {:sizing {:width {:type :grow}
                                                    :height {:type :fixed :value 100}}
                                           :layout-direction :left-to-right})
                     ;; Fixed child takes most of space
                     (layout/open-element {:sizing {:width {:type :fixed :value 700}
                                                    :height {:type :fixed :value 50}}})
                     (layout/close-element)
                     ;; Grow child with min
                     (layout/open-element {:sizing {:width {:type :grow :min 150}
                                                    :height {:type :fixed :value 50}}})
                     (layout/close-element)
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          grow-child (get (:layout-elements result) 3)]
      ;; Would only get 100, but min is 150
      (is (>= (get-in grow-child [:dimensions :width]) 150)))))

;; ============================================================================
;; PERCENT SIZING TESTS
;; ============================================================================

(deftest percent-calculates-from-parent
  (testing "Percent sizing calculates from parent size"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element {:sizing {:width {:type :percent :value 50}
                                                    :height {:type :percent :value 25}}})
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          child (get (:layout-elements result) 1)]
      ;; 50% of 800 = 400, 25% of 600 = 150
      (is (== 400 (get-in child [:dimensions :width])))
      (is (== 150 (get-in child [:dimensions :height]))))))

(deftest percent-respects-parent-padding
  (testing "Percent sizing accounts for parent padding"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element {:sizing {:width {:type :grow}
                                                    :height {:type :grow}}
                                           :padding {:top 50 :right 50 :bottom 50 :left 50}})
                     (layout/open-element {:sizing {:width {:type :percent :value 50}
                                                    :height {:type :percent :value 50}}})
                     (layout/close-element)
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          child (get (:layout-elements result) 2)]
      ;; Parent inner: 800-100=700 width, 600-100=500 height
      ;; 50% of 700 = 350, 50% of 500 = 250
      (is (== 350 (get-in child [:dimensions :width])))
      (is (== 250 (get-in child [:dimensions :height]))))))

;; ============================================================================
;; MIXED SIZING TESTS
;; ============================================================================

(deftest mixed-sizing-types
  (testing "Different sizing types work together"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element {:sizing {:width {:type :grow}
                                                    :height {:type :fixed :value 100}}
                                           :layout-direction :left-to-right})
                     ;; Fixed
                     (layout/open-element {:sizing {:width {:type :fixed :value 100}
                                                    :height {:type :fixed :value 50}}})
                     (layout/close-element)
                     ;; Percent (of remaining after fixed)
                     (layout/open-element {:sizing {:width {:type :percent :value 25}
                                                    :height {:type :fixed :value 50}}})
                     (layout/close-element)
                     ;; Grow (fills remaining)
                     (layout/open-element {:sizing {:width {:type :grow}
                                                    :height {:type :fixed :value 50}}})
                     (layout/close-element)
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          fixed-child (get (:layout-elements result) 2)
          percent-child (get (:layout-elements result) 3)
          grow-child (get (:layout-elements result) 4)]
      (is (= 100 (get-in fixed-child [:dimensions :width])) "Fixed = 100")
      (is (== 200 (get-in percent-child [:dimensions :width])) "Percent = 25% of 800 = 200")
      ;; Grow = 800 - 100 - 200 = 500
      (is (== 500 (get-in grow-child [:dimensions :width])) "Grow fills remaining"))))

;; ============================================================================
;; CROSS-AXIS SIZING TESTS
;; ============================================================================

(deftest vertical-container-sizes-width-correctly
  (testing "Vertical container: children width is max, height is sum"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element {:sizing {:width {:type :fit}
                                                    :height {:type :fit}}
                                           :layout-direction :top-to-bottom})
                     (layout/open-element {:sizing {:width {:type :fixed :value 100}
                                                    :height {:type :fixed :value 50}}})
                     (layout/close-element)
                     (layout/open-element {:sizing {:width {:type :fixed :value 150}
                                                    :height {:type :fixed :value 50}}})
                     (layout/close-element)
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          container (get (:layout-elements result) 1)]
      ;; Width = max(100, 150) = 150
      ;; Height = sum(50, 50) = 100
      (is (= 150 (get-in container [:dimensions :width])))
      (is (= 100 (get-in container [:dimensions :height]))))))
