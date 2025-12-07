(ns clay.layout.core-test
  "Tests for the Clay layout engine core module.

   These tests verify the layout engine faithfully translates Clay.h logic:
   - Layout state initialization
   - Element tree building
   - Position calculation
   - Render command generation"
  (:require [clojure.test :refer [deftest is testing]]
            [clay.layout.core :as layout]))

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
     :min-width width  ; Single word
     :words [{:text text :width width :is-whitespace false :is-newline false}]}))

;; ============================================================================
;; BEGIN-LAYOUT TESTS
;; ============================================================================

(deftest begin-layout-creates-valid-state
  (testing "begin-layout returns a valid layout state"
    (let [state (layout/begin-layout test-viewport)]
      (is (map? state) "State should be a map")
      (is (= test-viewport (:viewport state)) "Viewport should be stored")
      (is (vector? (:layout-elements state)) "layout-elements should be a vector")
      (is (= 1 (count (:layout-elements state))) "Should have root element")
      (is (= [0] (:element-stack state)) "Element stack should contain root index"))))

(deftest root-element-has-viewport-dimensions
  (testing "Root element has viewport dimensions"
    (let [state (layout/begin-layout test-viewport)
          root (first (:layout-elements state))]
      (is (= {:width 800 :height 600} (:dimensions root)))
      (is (= {:x 0 :y 0 :width 800 :height 600} (:bounding-box root))))))

;; ============================================================================
;; ELEMENT TREE TESTS
;; ============================================================================

(deftest open-element-adds-to-tree
  (testing "open-element adds a new element to the tree"
    (let [state (-> (layout/begin-layout test-viewport)
                    (layout/open-element))]
      (is (= 2 (count (:layout-elements state))) "Should have 2 elements")
      (is (= [0 1] (:element-stack state)) "Stack should have both indices"))))

(deftest close-element-pops-stack
  (testing "close-element pops from stack and adds to parent children"
    (let [state (-> (layout/begin-layout test-viewport)
                    (layout/open-element)
                    (layout/close-element))]
      (is (= [0] (:element-stack state)) "Stack should only have root")
      (let [root (first (:layout-elements state))]
        (is (= [1] (:children root)) "Root should have child index 1")))))

(deftest nested-elements-build-tree
  (testing "Nested elements build correct tree structure"
    (let [state (-> (layout/begin-layout test-viewport)
                    ;; First child of root
                    (layout/open-element)
                    ;; Grandchild
                    (layout/open-element)
                    (layout/close-element)  ; Close grandchild
                    (layout/close-element)  ; Close first child
                    ;; Second child of root
                    (layout/open-element)
                    (layout/close-element))]
      (is (= 4 (count (:layout-elements state))) "Should have 4 elements")
      (let [root (get (:layout-elements state) 0)
            first-child (get (:layout-elements state) 1)]
        (is (= [1 3] (:children root)) "Root has 2 children")
        (is (= [2] (:children first-child)) "First child has grandchild")))))

;; ============================================================================
;; TEXT ELEMENT TESTS
;; ============================================================================

(deftest open-text-element-stores-dimensions
  (testing "open-text-element stores measured dimensions"
    (let [measured {:width 100 :height 16 :min-width 100}
          state (-> (layout/begin-layout test-viewport)
                    (layout/open-text-element
                     "Hello"
                     {:font-id 0 :font-size 16}
                     measured))]
      (is (= 2 (count (:layout-elements state))))
      (let [text-elem (get (:layout-elements state) 1)]
        (is (= :text (:type text-elem)))
        (is (= "Hello" (:text-content text-elem)))
        (is (= {:width 100 :height 16} (:dimensions text-elem)))))))

;; ============================================================================
;; CONFIGURE ELEMENT TESTS
;; ============================================================================

(deftest configure-element-adds-config
  (testing "configure-element adds config to current element"
    (let [state (-> (layout/begin-layout test-viewport)
                    (layout/open-element)
                    (layout/configure-element :background {:color {:r 255 :g 0 :b 0 :a 1}})
                    (layout/configure-element :border {:width 2}))]
      (let [elem (get (:layout-elements state) 1)]
        (is (= 2 (count (:configs elem))))
        (is (= :background (:type (first (:configs elem)))))
        (is (= :border (:type (second (:configs elem)))))))))

;; ============================================================================
;; END-LAYOUT TESTS (FULL PIPELINE)
;; ============================================================================

(deftest end-layout-computes-positions
  (testing "end-layout computes positions for all elements"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element {:sizing {:width {:type :fixed :value 200}
                                                    :height {:type :fixed :value 100}}})
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          child (get (:layout-elements result) 1)]
      (is (map? (:bounding-box child)) "Child should have bounding box")
      (is (= 0 (:x (:bounding-box child))) "Child x should be 0")
      (is (= 0 (:y (:bounding-box child))) "Child y should be 0")
      (is (= 200 (:width (:bounding-box child))) "Child width should be 200")
      (is (= 100 (:height (:bounding-box child))) "Child height should be 100"))))

(deftest end-layout-generates-render-commands
  (testing "end-layout generates render commands"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element {:sizing {:width {:type :fixed :value 200}
                                                    :height {:type :fixed :value 100}}})
                     (layout/configure-element :background {:color {:r 255 :g 0 :b 0 :a 1}})
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          commands (layout/get-render-commands result)]
      (is (vector? commands) "Commands should be a vector")
      (is (pos? (count commands)) "Should have at least one command"))))

(deftest horizontal-layout-positions-children
  (testing "Horizontal layout positions children side by side"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element {:sizing {:width {:type :grow}
                                                    :height {:type :fixed :value 100}}
                                           :layout-direction :left-to-right
                                           :child-gap 10})
                     ;; Child 1
                     (layout/open-element {:sizing {:width {:type :fixed :value 100}
                                                    :height {:type :fixed :value 50}}})
                     (layout/close-element)
                     ;; Child 2
                     (layout/open-element {:sizing {:width {:type :fixed :value 100}
                                                    :height {:type :fixed :value 50}}})
                     (layout/close-element)
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          child1 (get (:layout-elements result) 2)
          child2 (get (:layout-elements result) 3)]
      (is (= 0 (:x (:bounding-box child1))) "First child at x=0")
      (is (= 110 (:x (:bounding-box child2))) "Second child at x=110 (100 + 10 gap)"))))

(deftest vertical-layout-positions-children
  (testing "Vertical layout positions children stacked"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element {:sizing {:width {:type :fixed :value 200}
                                                    :height {:type :grow}}
                                           :layout-direction :top-to-bottom
                                           :child-gap 10})
                     ;; Child 1
                     (layout/open-element {:sizing {:width {:type :fixed :value 100}
                                                    :height {:type :fixed :value 50}}})
                     (layout/close-element)
                     ;; Child 2
                     (layout/open-element {:sizing {:width {:type :fixed :value 100}
                                                    :height {:type :fixed :value 50}}})
                     (layout/close-element)
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          child1 (get (:layout-elements result) 2)
          child2 (get (:layout-elements result) 3)]
      (is (= 0 (:y (:bounding-box child1))) "First child at y=0")
      (is (= 60 (:y (:bounding-box child2))) "Second child at y=60 (50 + 10 gap)"))))

;; ============================================================================
;; TEXT RENDER COMMAND TESTS
;; ============================================================================

(deftest text-element-generates-text-command
  (testing "Text element generates :text render command"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-text-element
                      "Hello World"
                      {:font-id 0 :font-size 16 :text-color {:r 0 :g 0 :b 0 :a 1}}
                      {:width 100 :height 16})
                     (layout/end-layout mock-measure-fn))
          commands (layout/get-render-commands result)
          text-cmd (first (filter #(= :text (:command-type %)) commands))]
      (is (some? text-cmd) "Should have text command")
      (when text-cmd
        (is (= "Hello World" (get-in text-cmd [:render-data :text :chars])))
        (is (= 16 (get-in text-cmd [:render-data :font-size])))))))

;; ============================================================================
;; PADDING TESTS
;; ============================================================================

(deftest padding-affects-child-positions
  (testing "Padding offsets child positions"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element {:sizing {:width {:type :grow}
                                                    :height {:type :grow}}
                                           :padding {:top 20 :right 20 :bottom 20 :left 20}})
                     (layout/open-element {:sizing {:width {:type :fixed :value 100}
                                                    :height {:type :fixed :value 50}}})
                     (layout/close-element)
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          child (get (:layout-elements result) 2)]
      (is (= 20 (:x (:bounding-box child))) "Child x offset by left padding")
      (is (= 20 (:y (:bounding-box child))) "Child y offset by top padding"))))

;; ============================================================================
;; WITH-ELEMENT HELPER TESTS
;; ============================================================================

(deftest with-element-helper-works
  (testing "with-element helper creates nested structure"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/with-element {:sizing {:width {:type :fixed :value 200}
                                                    :height {:type :fixed :value 100}}}
                       (fn [s]
                         (layout/open-text-element s "Nested"
                           {:font-id 0 :font-size 16}
                           {:width 50 :height 16})))
                     (layout/end-layout mock-measure-fn))]
      (is (= 3 (count (:layout-elements result))) "Should have root, container, and text")
      (let [container (get (:layout-elements result) 1)]
        (is (= [2] (:children container)) "Container should have text as child")))))
