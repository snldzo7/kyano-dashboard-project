(ns clay.layout-test
  "Tests for the Specter-based layout engine."
  (:require [clojure.test :refer [deftest is testing]]
            [clay.layout :as layout]
            [clay.tree :as t]))

;; ============================================================================
;; TEST FIXTURES
;; ============================================================================

(def test-viewport {:width 800 :height 600})

(defn mock-measure-fn
  "Simple mock text measurement - fixed width per character."
  [text config]
  (let [font-size (or (:font-size config) 16)
        char-width (* font-size 0.6)
        width (* (count text) char-width)
        height font-size]
    {:width width
     :height height
     :min-width width
     :words (mapv (fn [word]
                    {:text (str word " ")
                     :width (* (inc (count word)) char-width)
                     :is-whitespace false
                     :is-newline false})
                  (clojure.string/split text #"\s+"))}))

(defn make-container
  "Helper to create a container node for testing."
  [{:keys [id width height direction padding gap children]
    :or {id (random-uuid)
         width {:type :fit}
         height {:type :fit}
         direction :top-to-bottom
         padding {:top 0 :right 0 :bottom 0 :left 0}
         gap 0
         children []}}]
  {:type :container
   :id id
   :layout {:sizing {:width width :height height}
            :padding padding
            :child-gap gap
            :layout-direction direction
            :child-alignment {:x :left :y :top}}
   :configs {}
   :dimensions {:width nil :height nil}
   :bounding-box {:x 0 :y 0 :width 0 :height 0}
   :children children})

(defn make-text
  "Helper to create a text node for testing."
  [{:keys [id content font-size]
    :or {id (random-uuid)
         content "Text"
         font-size 16}}]
  (let [measured (mock-measure-fn content {:font-size font-size})]
    {:type :text
     :id id
     :text-content content
     :text-config {:font-id 0 :font-size font-size}
     :measured measured
     :dimensions {:width (:width measured)
                  :height (:height measured)}
     :bounding-box {:x 0 :y 0 :width 0 :height 0}
     :wrapped-lines nil}))

;; ============================================================================
;; FIXED SIZING TESTS
;; ============================================================================

(deftest apply-fixed-sizes-test
  (testing "Fixed sizes are applied correctly"
    (let [tree (make-container {:width {:type :fixed :value 200}
                                :height {:type :fixed :value 100}})
          result-x (layout/apply-fixed-sizes tree true)
          result-xy (layout/apply-fixed-sizes result-x false)]
      (is (= 200 (t/dim result-x true)) "Fixed width applied")
      (is (= 100 (t/dim result-xy false)) "Fixed height applied")))

  (testing "Fixed sizes respect min/max constraints"
    (let [tree (make-container {:width {:type :fixed :value 50 :min 100}
                                :height {:type :fixed :value 200 :max 150}})
          result (-> tree
                     (layout/apply-fixed-sizes true)
                     (layout/apply-fixed-sizes false))]
      (is (= 100 (t/dim result true)) "Min constraint enforced")
      (is (= 150 (t/dim result false)) "Max constraint enforced"))))

(deftest grow-initialization-test
  (testing "Grow elements start at min size"
    (let [tree (make-container {:width {:type :grow :min 50}
                                :height {:type :grow :min 30}})
          result (-> tree
                     (layout/apply-fixed-sizes true)
                     (layout/apply-fixed-sizes false))]
      (is (= 50 (t/dim result true)) "Width starts at min")
      (is (= 30 (t/dim result false)) "Height starts at min"))))

;; ============================================================================
;; FIT SIZING TESTS
;; ============================================================================

(deftest calculate-fit-sizes-test
  (testing "Fit container sizes to children along axis"
    (let [tree (make-container
                {:direction :left-to-right
                 :gap 10
                 :children [(make-container {:width {:type :fixed :value 100}
                                             :height {:type :fixed :value 50}})
                            (make-container {:width {:type :fixed :value 100}
                                             :height {:type :fixed :value 50}})]})
          result (-> tree
                     (layout/apply-fixed-sizes true)
                     (layout/calculate-fit-sizes true))]
      ;; 100 + 10 (gap) + 100 = 210
      (is (= 210 (t/dim result true)) "Width = sum of children + gaps")))

  (testing "Fit container sizes to max child off axis"
    (let [tree (make-container
                {:direction :left-to-right
                 :children [(make-container {:width {:type :fixed :value 50}
                                             :height {:type :fixed :value 30}})
                            (make-container {:width {:type :fixed :value 50}
                                             :height {:type :fixed :value 60}})]})
          result (-> tree
                     (layout/apply-fixed-sizes false)
                     (layout/calculate-fit-sizes false))]
      (is (= 60 (t/dim result false)) "Height = max of children")))

  (testing "Padding is included in fit size"
    (let [tree (make-container
                {:padding {:top 10 :right 20 :bottom 10 :left 20}
                 :children [(make-container {:width {:type :fixed :value 100}
                                             :height {:type :fixed :value 50}})]})
          result (-> tree
                     (layout/apply-fixed-sizes true)
                     (layout/apply-fixed-sizes false)
                     (layout/calculate-fit-sizes true)
                     (layout/calculate-fit-sizes false))]
      (is (= 140 (t/dim result true)) "Width includes horizontal padding")
      (is (= 70 (t/dim result false)) "Height includes vertical padding"))))

;; ============================================================================
;; PERCENT SIZING TESTS
;; ============================================================================

(deftest apply-percent-sizes-test
  (testing "Percent sizing is calculated from parent"
    (let [tree (make-container
                {:width {:type :fixed :value 200}
                 :height {:type :fixed :value 200}
                 :children [(make-container {:width {:type :percent :value 50}
                                             :height {:type :percent :value 25}})]})
          result (-> tree
                     (layout/apply-fixed-sizes true)
                     (layout/apply-fixed-sizes false)
                     (layout/apply-percent-sizes true)
                     (layout/apply-percent-sizes false))
          child (first (:children result))]
      (is (= 100 (t/dim child true)) "50% of 200 = 100")
      (is (= 50 (t/dim child false)) "25% of 200 = 50")))

  (testing "Percent respects padding"
    (let [tree (make-container
                {:width {:type :fixed :value 200}
                 :padding {:top 0 :right 50 :bottom 0 :left 50}
                 :children [(make-container {:width {:type :percent :value 100}})]})
          result (-> tree
                     (layout/apply-fixed-sizes true)
                     (layout/apply-percent-sizes true))
          child (first (:children result))]
      ;; 200 - 50 - 50 = 100 available, 100% = 100
      (is (= 100 (t/dim child true))))))

;; ============================================================================
;; GROW DISTRIBUTION TESTS
;; ============================================================================

(deftest distribute-grow-test
  (testing "Grow elements expand to fill space"
    (let [tree (make-container
                {:width {:type :fixed :value 300}
                 :direction :left-to-right
                 :children [(make-container {:width {:type :fixed :value 100}
                                             :height {:type :fixed :value 50}})
                            (make-container {:width {:type :grow}
                                             :height {:type :fixed :value 50}})]})
          result (-> tree
                     (layout/apply-fixed-sizes true)
                     (layout/distribute-grow true))
          grow-child (second (:children result))]
      ;; 300 - 100 = 200 available for grow
      (is (= 200 (t/dim grow-child true)))))

  (testing "Multiple grow elements share space equally"
    (let [tree (make-container
                {:width {:type :fixed :value 400}
                 :direction :left-to-right
                 :gap 0
                 :children [(make-container {:width {:type :grow}})
                            (make-container {:width {:type :grow}})]})
          result (-> tree
                     (layout/apply-fixed-sizes true)
                     (layout/distribute-grow true))
          [child1 child2] (:children result)]
      (is (= 200 (t/dim child1 true)) "First grow gets half")
      (is (= 200 (t/dim child2 true)) "Second grow gets half")))

  (testing "Grow respects max constraint"
    (let [tree (make-container
                {:width {:type :fixed :value 400}
                 :direction :left-to-right
                 :children [(make-container {:width {:type :grow :max 100}})]})
          result (-> tree
                     (layout/apply-fixed-sizes true)
                     (layout/distribute-grow true))
          child (first (:children result))]
      (is (= 100 (t/dim child true)) "Grow capped at max"))))

;; ============================================================================
;; POSITION CALCULATION TESTS
;; ============================================================================

(deftest calculate-positions-test
  (testing "Children positioned at parent padding offset"
    (let [tree (t/root-container
                test-viewport
                [(make-container
                  {:width {:type :fixed :value 100}
                   :height {:type :fixed :value 50}
                   :padding {:top 20 :right 20 :bottom 20 :left 20}
                   :children [(make-container {:width {:type :fixed :value 40}
                                               :height {:type :fixed :value 20}})]})])
          result (-> tree
                     (layout/apply-fixed-sizes true)
                     (layout/apply-fixed-sizes false)
                     (layout/calculate-positions))
          inner (first (:children (first (:children result))))]
      (is (= 20 (:x (:bounding-box inner))) "X offset by left padding")
      (is (= 20 (:y (:bounding-box inner))) "Y offset by top padding")))

  (testing "Horizontal layout positions children left-to-right"
    (let [tree (t/root-container
                test-viewport
                [(make-container
                  {:width {:type :fixed :value 300}
                   :height {:type :fixed :value 100}
                   :direction :left-to-right
                   :gap 10
                   :children [(make-container {:width {:type :fixed :value 50}
                                               :height {:type :fixed :value 50}})
                              (make-container {:width {:type :fixed :value 50}
                                               :height {:type :fixed :value 50}})]})])
          result (-> tree
                     (layout/apply-fixed-sizes true)
                     (layout/apply-fixed-sizes false)
                     (layout/calculate-positions))
          container (first (:children result))
          [child1 child2] (:children container)]
      (is (= 0 (:x (:bounding-box child1))) "First child at x=0")
      (is (= 60 (:x (:bounding-box child2))) "Second child at x=60 (50+10 gap)")))

  (testing "Vertical layout positions children top-to-bottom"
    (let [tree (t/root-container
                test-viewport
                [(make-container
                  {:width {:type :fixed :value 100}
                   :height {:type :fixed :value 300}
                   :direction :top-to-bottom
                   :gap 10
                   :children [(make-container {:width {:type :fixed :value 50}
                                               :height {:type :fixed :value 50}})
                              (make-container {:width {:type :fixed :value 50}
                                               :height {:type :fixed :value 50}})]})])
          result (-> tree
                     (layout/apply-fixed-sizes true)
                     (layout/apply-fixed-sizes false)
                     (layout/calculate-positions))
          container (first (:children result))
          [child1 child2] (:children container)]
      (is (= 0 (:y (:bounding-box child1))) "First child at y=0")
      (is (= 60 (:y (:bounding-box child2))) "Second child at y=60 (50+10 gap)"))))

;; ============================================================================
;; RENDER COMMAND GENERATION TESTS
;; ============================================================================

(deftest generate-render-commands-test
  (testing "Background generates rectangle command"
    (let [tree (t/root-container
                test-viewport
                [(-> (make-container {:width {:type :fixed :value 100}
                                      :height {:type :fixed :value 50}})
                     (assoc-in [:configs :background]
                               {:color {:r 255 :g 0 :b 0 :a 255}
                                :corner-radius {:top-left 0 :top-right 0
                                                :bottom-left 0 :bottom-right 0}}))])
          result (-> tree
                     (layout/apply-fixed-sizes true)
                     (layout/apply-fixed-sizes false)
                     (layout/calculate-positions)
                     (layout/generate-render-commands))
          rect-cmd (first (filter #(= :rectangle (:command-type %)) result))]
      (is (some? rect-cmd) "Should have rectangle command")
      (is (= 255 (get-in rect-cmd [:render-data :color :r])))))

  (testing "Text generates text command"
    (let [text-node (make-text {:content "Hello"})
          tree (t/root-container test-viewport [text-node])
          result (-> tree
                     (layout/apply-fixed-sizes true)
                     (layout/apply-fixed-sizes false)
                     (layout/calculate-positions)
                     (layout/generate-render-commands))
          text-cmd (first (filter #(= :text (:command-type %)) result))]
      (is (some? text-cmd) "Should have text command")
      (is (= "Hello" (get-in text-cmd [:render-data :text :chars])))))

  (testing "Border generates border command"
    (let [tree (t/root-container
                test-viewport
                [(-> (make-container {:width {:type :fixed :value 100}
                                      :height {:type :fixed :value 50}})
                     (assoc-in [:configs :border]
                               {:width 2 :color {:r 0 :g 0 :b 0 :a 255}}))])
          result (-> tree
                     (layout/apply-fixed-sizes true)
                     (layout/apply-fixed-sizes false)
                     (layout/calculate-positions)
                     (layout/generate-render-commands))
          border-cmd (first (filter #(= :border (:command-type %)) result))]
      (is (some? border-cmd) "Should have border command")))

  (testing "Clip generates clip and clip-end commands"
    (let [tree (t/root-container
                test-viewport
                [(-> (make-container {:width {:type :fixed :value 100}
                                      :height {:type :fixed :value 50}})
                     (assoc-in [:configs :clip]
                               {:vertical true :horizontal false}))])
          result (-> tree
                     (layout/apply-fixed-sizes true)
                     (layout/apply-fixed-sizes false)
                     (layout/calculate-positions)
                     (layout/generate-render-commands))
          clip-cmds (filter #(= :clip (:command-type %)) result)
          clip-end-cmds (filter #(= :clip-end (:command-type %)) result)]
      (is (= 1 (count clip-cmds)) "Should have 1 clip command")
      (is (= 1 (count clip-end-cmds)) "Should have 1 clip-end command"))))

;; ============================================================================
;; FULL PIPELINE TESTS
;; ============================================================================

(deftest layout-pipeline-test
  (testing "Full layout pipeline produces render commands"
    (let [tree (t/root-container
                test-viewport
                [(make-container
                  {:width {:type :grow}
                   :height {:type :fixed :value 100}
                   :direction :left-to-right
                   :gap 10
                   :children [(-> (make-container {:width {:type :fixed :value 100}
                                                   :height {:type :fixed :value 80}})
                                  (assoc-in [:configs :background]
                                            {:color {:r 200 :g 200 :b 200 :a 255}}))
                              (make-text {:content "Hello World"})]})])
          commands (layout/layout tree mock-measure-fn)]
      (is (vector? commands) "Returns vector of commands")
      (is (pos? (count commands)) "Has at least one command")
      (is (some #(= :rectangle (:command-type %)) commands) "Has rectangle")
      (is (some #(= :text (:command-type %)) commands) "Has text")))

  (testing "layout-tree returns positioned tree for debugging"
    (let [tree (t/root-container
                {:width 200 :height 100}
                [(make-container {:width {:type :fixed :value 50}
                                  :height {:type :fixed :value 50}})])
          result (layout/layout-tree tree)]
      (is (map? result) "Returns tree structure")
      (is (some? (:bounding-box result)) "Has bounding box")
      (is (= 200 (:width (:bounding-box result)))))))

;; ============================================================================
;; EDGE CASES
;; ============================================================================

(deftest empty-container-test
  (testing "Empty container with fit sizing has zero content size"
    (let [tree (make-container {:width {:type :fit}
                                :height {:type :fit}})
          result (-> tree
                     (layout/apply-fixed-sizes true)
                     (layout/calculate-fit-sizes true))]
      (is (= 0 (t/dim result true)) "Fit with no children = 0"))))

(deftest deeply-nested-test
  (testing "Layout works with deeply nested structure"
    (let [tree (t/root-container
                {:width 400 :height 400}
                [(make-container
                  {:width {:type :grow}
                   :height {:type :grow}
                   :children [(make-container
                               {:width {:type :grow}
                                :height {:type :grow}
                                :children [(make-container
                                            {:width {:type :fixed :value 50}
                                             :height {:type :fixed :value 50}})]})]})])
          commands (layout/layout tree)]
      (is (vector? commands) "Handles deep nesting"))))
