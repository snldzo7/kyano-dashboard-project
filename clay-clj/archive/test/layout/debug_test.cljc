(ns clay.layout.debug-test
  "Tests for Clay debug system - debug mode, culling, element ID generation."
  (:require [clojure.test :refer [deftest is testing]]
            [clay.layout.debug :as debug]
            [clay.layout.core :as layout]))

;; ============================================================================
;; TEST FIXTURES
;; ============================================================================

(def test-viewport {:width 800 :height 600})

(defn mock-measure-fn [text config]
  {:width (* (count text) 8) :height 16})

;; ============================================================================
;; DEBUG MODE TESTS
;; ============================================================================

(deftest debug-mode-toggle-test
  (testing "Debug mode can be enabled and disabled"
    (let [state (layout/begin-layout test-viewport)]
      (is (not (debug/debug-mode-enabled? state)))

      (let [enabled-state (debug/set-debug-mode-enabled state true)]
        (is (debug/debug-mode-enabled? enabled-state)))

      (let [disabled-state (-> state
                               (debug/set-debug-mode-enabled true)
                               (debug/set-debug-mode-enabled false))]
        (is (not (debug/debug-mode-enabled? disabled-state)))))))

;; ============================================================================
;; CULLING TESTS
;; ============================================================================

(deftest culling-toggle-test
  (testing "Culling can be enabled and disabled"
    (let [state (layout/begin-layout test-viewport)]
      (is (not (debug/culling-enabled? state)))

      (let [enabled-state (debug/set-culling-enabled state true)]
        (is (debug/culling-enabled? enabled-state))))))

(deftest culling-visible-elements-test
  (testing "Visible elements pass culling"
    (let [visible-cmd {:bounding-box {:x 100 :y 100 :width 200 :height 200}
                       :command-type :rectangle}
          state (-> (layout/begin-layout test-viewport)
                    (debug/set-culling-enabled true))
          result (debug/apply-culling state [visible-cmd])]
      (is (= 1 (count result))))))

(deftest culling-off-screen-elements-test
  (testing "Off-screen elements are culled"
    (let [off-screen-cmd {:bounding-box {:x 1000 :y 1000 :width 100 :height 100}
                          :command-type :rectangle}
          state (-> (layout/begin-layout test-viewport)
                    (debug/set-culling-enabled true))
          result (debug/apply-culling state [off-screen-cmd])]
      (is (= 0 (count result))))))

(deftest culling-disabled-no-filter-test
  (testing "Culling disabled passes all elements"
    (let [off-screen-cmd {:bounding-box {:x 1000 :y 1000 :width 100 :height 100}
                          :command-type :rectangle}
          state (layout/begin-layout test-viewport)
          result (debug/apply-culling state [off-screen-cmd])]
      (is (= 1 (count result))))))

;; ============================================================================
;; ELEMENT ID GENERATION TESTS
;; ============================================================================

(deftest get-element-id-test
  (testing "Element ID generation from string"
    (let [id (debug/get-element-id "my-element")]
      (is (number? (:id id)))
      (is (= (:id id) (:base-id id)))
      (is (= 0 (:offset id))))))

(deftest get-element-id-consistency-test
  (testing "Same string produces same ID"
    (let [id1 (debug/get-element-id "test-element")
          id2 (debug/get-element-id "test-element")]
      (is (= (:id id1) (:id id2))))))

(deftest get-element-id-uniqueness-test
  (testing "Different strings produce different IDs"
    (let [id1 (debug/get-element-id "element-a")
          id2 (debug/get-element-id "element-b")]
      (is (not= (:id id1) (:id id2))))))

(deftest get-element-id-with-index-test
  (testing "Indexed element ID generation"
    (let [id0 (debug/get-element-id-with-index "list-item" 0)
          id1 (debug/get-element-id-with-index "list-item" 1)
          id2 (debug/get-element-id-with-index "list-item" 2)]
      ;; Each should have unique ID
      (is (not= (:id id0) (:id id1)))
      (is (not= (:id id1) (:id id2)))
      ;; But same base-id
      (is (= (:base-id id0) (:base-id id1) (:base-id id2)))
      ;; And correct offset
      (is (= 0 (:offset id0)))
      (is (= 1 (:offset id1)))
      (is (= 2 (:offset id2))))))

;; ============================================================================
;; TEXT CACHE TESTS
;; ============================================================================

(deftest reset-text-cache-test
  (testing "Text cache can be reset"
    (let [state {:text-cache {"word1" {:width 50} "word2" {:width 60}}
                 :generation 5}
          reset-state (debug/reset-text-cache state)]
      (is (empty? (:text-cache reset-state))))))

(deftest text-cache-stats-test
  (testing "Text cache stats are reported"
    (let [state {:text-cache {"a" 1 "b" 2 "c" 3}
                 :generation 10}
          stats (debug/get-text-cache-stats state)]
      (is (= 3 (:entry-count stats)))
      (is (= 10 (:generation stats))))))

;; ============================================================================
;; HOVER HELPERS TESTS
;; ============================================================================

(deftest hovered-check-test
  (testing "hovered? checks pointer-over-ids"
    (let [state {:pointer-over-ids [42 100 7]}]
      (is (debug/hovered? state 42))
      (is (debug/hovered? state 100))
      (is (debug/hovered? state 7))
      (is (not (debug/hovered? state 999))))))

(deftest get-hovered-element-test
  (testing "Get topmost hovered element"
    (let [state {:pointer-over-ids [42]
                 :layout-elements [{:id {:id 1}}
                                   {:id {:id 42} :type :container}
                                   {:id {:id 100}}]}
          result (debug/get-hovered-element state)]
      (is (= 42 (get-in result [:id :id])))
      (is (= :container (:type result))))))

(deftest get-hovered-element-none-test
  (testing "No hovered element returns nil"
    (let [state {:pointer-over-ids []
                 :layout-elements [{:id {:id 1}}]}]
      (is (nil? (debug/get-hovered-element state))))))

;; ============================================================================
;; DEBUG OVERLAY TESTS
;; ============================================================================

(deftest debug-overlay-disabled-test
  (testing "Debug overlay returns nil when disabled"
    (let [state (layout/begin-layout test-viewport)]
      (is (nil? (debug/generate-debug-overlay state))))))

(deftest debug-overlay-enabled-test
  (testing "Debug overlay generates commands when enabled"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element {:sizing {:width {:type :fixed :value 100}
                                                     :height {:type :fixed :value 100}}})
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn)
                     (debug/set-debug-mode-enabled true)
                     (debug/generate-debug-overlay))]
      ;; Should have overlay commands (2 per element: fill + border)
      (is (pos? (count result)))
      ;; Commands should have high z-index
      (is (every? #(>= (:z-index % 0) 9998) result)))))

(deftest debug-overlay-highlights-hovered-test
  (testing "Debug overlay highlights hovered elements"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element {:sizing {:width {:type :fixed :value 100}
                                                     :height {:type :fixed :value 100}}})
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn)
                     (assoc :pointer-over-ids [1])  ; Element 1 is hovered
                     (debug/set-debug-mode-enabled true)
                     (debug/generate-debug-overlay))
          hovered-cmds (filter #(= 1 (get-in % [:id :id])) result)]
      ;; Hovered element should have overlay commands
      (is (pos? (count hovered-cmds))))))

;; ============================================================================
;; ELEMENT VISIBILITY TEST
;; ============================================================================

(deftest element-visible-test
  (testing "Element visibility check"
    (let [viewport {:width 800 :height 600}
          visible-element {:bounding-box {:x 100 :y 100 :width 200 :height 200}}
          off-screen-element {:bounding-box {:x 1000 :y 1000 :width 100 :height 100}}
          partial-element {:bounding-box {:x 700 :y 500 :width 200 :height 200}}]
      (is (debug/element-visible? visible-element viewport))
      (is (not (debug/element-visible? off-screen-element viewport)))
      ;; Partially visible should count as visible
      (is (debug/element-visible? partial-element viewport)))))
