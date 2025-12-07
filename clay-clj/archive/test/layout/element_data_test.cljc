(ns clay.layout.element-data-test
  "Tests for element data queries API."
  (:require [clojure.test :refer [deftest is testing]]
            [clay.layout.core :as layout]
            [clay.layout.element-data :as elem]))

;; ============================================================================
;; TEST FIXTURES
;; ============================================================================

(def test-viewport {:width 800 :height 600})

(defn mock-measure-fn [text config]
  {:width (* (count text) 8) :height 16})

;; ============================================================================
;; GET ELEMENT DATA TESTS
;; ============================================================================

(deftest get-element-data-test
  (testing "Get element data returns bounding box"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element {:sizing {:width {:type :fixed :value 100}
                                                    :height {:type :fixed :value 50}}})
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          element (get (:layout-elements result) 1)
          element-id (get-in element [:id :id])
          data (elem/get-element-data result element-id)]
      (is (:found data))
      (is (= 100 (get-in data [:bounding-box :width])))
      (is (= 50 (get-in data [:bounding-box :height])))))

  (testing "Get element data for non-existent returns not found"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/end-layout mock-measure-fn))
          data (elem/get-element-data result 999999)]
      (is (not (:found data))))))

;; ============================================================================
;; GET ELEMENT BY ID TESTS
;; ============================================================================

(deftest get-element-by-id-test
  (testing "Get element by ID returns element"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element {:sizing {:width {:type :fixed :value 100}
                                                    :height {:type :fixed :value 50}}})
                     (layout/configure-element :background {:color {:r 255 :g 0 :b 0 :a 1}})
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          element (get (:layout-elements result) 1)
          element-id (get-in element [:id :id])
          found (elem/get-element-by-id result element-id)]
      (is (some? found))
      (is (= element-id (get-in found [:id :id])))))

  (testing "Get element by non-existent ID returns nil"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/end-layout mock-measure-fn))
          found (elem/get-element-by-id result 999999)]
      (is (nil? found)))))

;; ============================================================================
;; ELEMENT QUERY TESTS
;; ============================================================================

(def default-sizing {:sizing {:width {:type :fit} :height {:type :fit}}})

(deftest get-all-element-ids-test
  (testing "Get all element IDs"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element default-sizing)
                     (layout/close-element)
                     (layout/open-element default-sizing)
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          ids (elem/get-all-element-ids result)]
      ;; Root + 2 elements
      (is (= 3 (count ids)))
      (is (every? number? ids)))))

(deftest find-elements-test
  (testing "Find elements by type"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element default-sizing)
                     (layout/open-text-element "Hello" {:font-size 16} {:width 40 :height 16})
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          containers (elem/find-elements-by-type result :container)
          texts (elem/find-elements-by-type result :text)]
      ;; Root + 1 container
      (is (= 2 (count containers)))
      (is (= 1 (count texts)))))

  (testing "Find elements with config"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element default-sizing)
                     (layout/configure-element :background {:color {:r 255 :g 0 :b 0 :a 1}})
                     (layout/close-element)
                     (layout/open-element default-sizing)
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          with-bg (elem/find-elements-with-config result :background)]
      (is (= 1 (count with-bg))))))

;; ============================================================================
;; TREE QUERY TESTS
;; ============================================================================

(deftest get-children-test
  (testing "Get children of element"
    (let [result (-> (layout/begin-layout test-viewport)
                     ;; Parent at idx 1
                     (layout/open-element default-sizing)
                     ;; Child 1 at idx 2
                     (layout/open-element default-sizing)
                     (layout/close-element)
                     ;; Child 2 at idx 3
                     (layout/open-element default-sizing)
                     (layout/close-element)
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          parent (get (:layout-elements result) 1)
          parent-id (get-in parent [:id :id])
          children (elem/get-children result parent-id)]
      (is (= 2 (count children))))))

(deftest get-parent-test
  (testing "Get parent of element"
    (let [result (-> (layout/begin-layout test-viewport)
                     ;; Parent at idx 1
                     (layout/open-element default-sizing)
                     ;; Child at idx 2
                     (layout/open-element default-sizing)
                     (layout/close-element)
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          child (get (:layout-elements result) 2)
          child-id (get-in child [:id :id])
          parent (elem/get-parent result child-id)]
      (is (some? parent))
      ;; Parent should be at idx 1
      (is (= 1 (get-in parent [:id :id]))))))

(deftest get-descendants-test
  (testing "Get all descendants"
    (let [result (-> (layout/begin-layout test-viewport)
                     ;; Root children
                     (layout/open-element default-sizing)
                     ;; Nested children
                     (layout/open-element default-sizing)
                     (layout/open-element default-sizing)
                     (layout/close-element)
                     (layout/close-element)
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          root-id 0
          descendants (elem/get-descendants result root-id)]
      ;; Root has 1 child, which has 1 child, which has 1 child = 3 descendants
      (is (= 3 (count descendants))))))

;; ============================================================================
;; HELPER FUNCTION TESTS
;; ============================================================================

(deftest helper-functions-test
  (testing "get-element-position"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element {:sizing {:width {:type :fixed :value 100}
                                                    :height {:type :fixed :value 50}}})
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          element (get (:layout-elements result) 1)
          element-id (get-in element [:id :id])
          pos (elem/get-element-position result element-id)]
      (is (= 0 (:x pos)))
      (is (= 0 (:y pos)))))

  (testing "get-element-size"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element {:sizing {:width {:type :fixed :value 100}
                                                    :height {:type :fixed :value 50}}})
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          element (get (:layout-elements result) 1)
          element-id (get-in element [:id :id])
          size (elem/get-element-size result element-id)]
      (is (= 100 (:width size)))
      (is (= 50 (:height size)))))

  (testing "element-contains-point?"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element {:sizing {:width {:type :fixed :value 100}
                                                    :height {:type :fixed :value 50}}})
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          element (get (:layout-elements result) 1)
          element-id (get-in element [:id :id])]
      (is (elem/element-contains-point? result element-id {:x 50 :y 25}))
      (is (not (elem/element-contains-point? result element-id {:x 200 :y 200}))))))
