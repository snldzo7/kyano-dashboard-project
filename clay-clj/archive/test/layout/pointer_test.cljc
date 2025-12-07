(ns clay.layout.pointer-test
  "Tests for pointer/hit testing system."
  (:require [clojure.test :refer [deftest is testing]]
            [clay.layout.core :as layout]
            [clay.layout.pointer :as pointer]))

;; ============================================================================
;; TEST FIXTURES
;; ============================================================================

(def test-viewport {:width 800 :height 600})

(defn mock-measure-fn [text config]
  {:width (* (count text) 8) :height 16})

;; ============================================================================
;; POINT-IN-RECT TESTS
;; ============================================================================

(deftest point-inside-rect-test
  (testing "Point inside rect returns true"
    (let [rect {:x 100 :y 100 :width 200 :height 150}]
      (is (pointer/point-inside-rect? {:x 150 :y 150} rect))
      (is (pointer/point-inside-rect? {:x 100 :y 100} rect))  ; Top-left corner
      (is (pointer/point-inside-rect? {:x 300 :y 250} rect)))) ; Bottom-right corner

  (testing "Point outside rect returns false"
    (let [rect {:x 100 :y 100 :width 200 :height 150}]
      (is (not (pointer/point-inside-rect? {:x 50 :y 150} rect)))   ; Left of rect
      (is (not (pointer/point-inside-rect? {:x 350 :y 150} rect)))  ; Right of rect
      (is (not (pointer/point-inside-rect? {:x 150 :y 50} rect)))   ; Above rect
      (is (not (pointer/point-inside-rect? {:x 150 :y 300} rect)))))) ; Below rect

;; ============================================================================
;; POINTER STATE TESTS
;; ============================================================================

(deftest pointer-state-transitions
  (testing "Initial state is released"
    (let [state {}
          pointer-data (pointer/get-pointer-data state)]
      (is (= :released (:state pointer-data)))))

  (testing "Pressing sets state to pressed-this-frame"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element {:sizing {:width {:type :fixed :value 100}
                                                    :height {:type :fixed :value 100}}})
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn)
                     (pointer/set-pointer-state {:x 50 :y 50} true))]
      (is (= :pressed-this-frame (:state (pointer/get-pointer-data result))))))

  (testing "Holding keeps pressed state"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element {:sizing {:width {:type :fixed :value 100}
                                                    :height {:type :fixed :value 100}}})
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn)
                     (pointer/set-pointer-state {:x 50 :y 50} true)
                     (pointer/set-pointer-state {:x 50 :y 50} true))]
      (is (= :pressed (:state (pointer/get-pointer-data result))))))

  (testing "Releasing sets state to released-this-frame"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element {:sizing {:width {:type :fixed :value 100}
                                                    :height {:type :fixed :value 100}}})
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn)
                     (pointer/set-pointer-state {:x 50 :y 50} true)
                     (pointer/set-pointer-state {:x 50 :y 50} false))]
      (is (= :released-this-frame (:state (pointer/get-pointer-data result)))))))

;; ============================================================================
;; HIT TESTING TESTS
;; ============================================================================

(deftest hit-testing-basic
  (testing "Pointer over element is detected"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element {:sizing {:width {:type :fixed :value 100}
                                                    :height {:type :fixed :value 100}}})
                     (layout/configure-element :background {:color {:r 255 :g 0 :b 0 :a 1}})
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn)
                     (pointer/set-pointer-state {:x 50 :y 50} false))
          element (get (:layout-elements result) 1)
          element-id (get-in element [:id :id])]
      (is (pointer/pointer-over? result element-id))))

  (testing "Pointer outside element is not detected"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element {:sizing {:width {:type :fixed :value 100}
                                                    :height {:type :fixed :value 100}}})
                     (layout/configure-element :background {:color {:r 255 :g 0 :b 0 :a 1}})
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn)
                     (pointer/set-pointer-state {:x 500 :y 500} false))
          element (get (:layout-elements result) 1)
          element-id (get-in element [:id :id])]
      (is (not (pointer/pointer-over? result element-id))))))

(deftest hit-testing-nested
  (testing "Nested elements are all detected"
    (let [result (-> (layout/begin-layout test-viewport)
                     ;; Parent at idx 1
                     (layout/open-element {:sizing {:width {:type :fixed :value 200}
                                                    :height {:type :fixed :value 200}}})
                     (layout/configure-element :background {:color {:r 255 :g 0 :b 0 :a 1}})
                     ;; Child at idx 2
                     (layout/open-element {:sizing {:width {:type :fixed :value 100}
                                                    :height {:type :fixed :value 100}}})
                     (layout/configure-element :background {:color {:r 0 :g 255 :b 0 :a 1}})
                     (layout/close-element)
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn)
                     (pointer/set-pointer-state {:x 50 :y 50} false))
          parent (get (:layout-elements result) 1)
          child (get (:layout-elements result) 2)
          parent-id (get-in parent [:id :id])
          child-id (get-in child [:id :id])
          over-ids (pointer/get-pointer-over-ids result)]
      ;; Both parent and child should be detected
      (is (pointer/pointer-over? result parent-id))
      (is (pointer/pointer-over? result child-id))
      ;; Root should also be detected (pointer is within viewport)
      (is (>= (count over-ids) 2)))))

;; ============================================================================
;; POINTER OVER IDS TESTS
;; ============================================================================

(deftest pointer-over-ids-test
  (testing "Get all elements under pointer"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element {:sizing {:width {:type :fixed :value 100}
                                                    :height {:type :fixed :value 100}}})
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn)
                     (pointer/set-pointer-state {:x 50 :y 50} false))
          over-ids (pointer/get-pointer-over-ids result)]
      ;; Should have at least root and the element
      (is (vector? over-ids))
      (is (>= (count over-ids) 1)))))

;; ============================================================================
;; HELPER FUNCTION TESTS
;; ============================================================================

(deftest helper-functions-test
  (testing "pressed-this-frame? works"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/end-layout mock-measure-fn)
                     (pointer/set-pointer-state {:x 0 :y 0} true))]
      (is (pointer/pressed-this-frame? result))))

  (testing "pressed? works for both states"
    (let [result1 (-> (layout/begin-layout test-viewport)
                      (layout/end-layout mock-measure-fn)
                      (pointer/set-pointer-state {:x 0 :y 0} true))
          result2 (-> result1
                      (pointer/set-pointer-state {:x 0 :y 0} true))]
      (is (pointer/pressed? result1))  ; pressed-this-frame
      (is (pointer/pressed? result2)))) ; pressed

  (testing "released-this-frame? works"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/end-layout mock-measure-fn)
                     (pointer/set-pointer-state {:x 0 :y 0} true)
                     (pointer/set-pointer-state {:x 0 :y 0} false))]
      (is (pointer/released-this-frame? result)))))

;; ============================================================================
;; GET ELEMENT AT POINT TESTS
;; ============================================================================

(deftest get-element-at-point-test
  (testing "Get topmost element at point"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element {:sizing {:width {:type :fixed :value 100}
                                                    :height {:type :fixed :value 100}}})
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          element-id (pointer/get-element-at-point result {:x 50 :y 50})]
      (is (some? element-id))))

  (testing "No element at point returns nil for out of bounds"
    (let [result (-> (layout/begin-layout {:width 100 :height 100})
                     (layout/open-element {:sizing {:width {:type :fixed :value 50}
                                                    :height {:type :fixed :value 50}}})
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          ;; Point outside the small element
          element-id (pointer/get-element-at-point result {:x 75 :y 75})]
      ;; Should still hit root since viewport is 100x100
      (is (= 0 element-id)))))
