(ns clay.hiccup2-test
  "Tests for the hiccup2 DSL to tree transformation."
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [clay.hiccup2 :as h]
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
     :words [{:text text :width width :is-whitespace false :is-newline false}]}))

(use-fixtures :each
  (fn [f]
    (h/reset-ids!)
    (f)))

;; ============================================================================
;; PARSE TESTS - BASIC ELEMENTS
;; ============================================================================

(deftest parse-col-test
  (testing "[:col] creates vertical container"
    (let [tree (h/parse test-viewport [:col])]
      (is (= :container (:type tree)))
      (is (= :root (:id tree)))
      (is (= 1 (count (:children tree))))
      (let [col (first (:children tree))]
        (is (= :container (:type col)))
        (is (= :top-to-bottom (get-in col [:layout :layout-direction])))))))

(deftest parse-row-test
  (testing "[:row] creates horizontal container"
    (let [tree (h/parse test-viewport [:row])]
      (is (= 1 (count (:children tree))))
      (let [row (first (:children tree))]
        (is (= :left-to-right (get-in row [:layout :layout-direction])))))))

(deftest parse-box-test
  (testing "[:box] creates generic container"
    (let [tree (h/parse test-viewport [:box {:bg :red}])]
      (let [box (first (:children tree))]
        (is (= :container (:type box)))
        (is (some? (get-in box [:configs :background])))))))

(deftest parse-text-test
  (testing "[:text \"content\"] creates text node"
    (let [tree (h/parse test-viewport [:text "Hello"] mock-measure-fn)]
      (let [text (first (:children tree))]
        (is (= :text (:type text)))
        (is (= "Hello" (:text-content text)))
        (is (some? (:measured text)))))))

(deftest parse-spacer-test
  (testing "[:spacer] creates grow container"
    (let [tree (h/parse test-viewport [:spacer])]
      (let [spacer (first (:children tree))]
        (is (= :container (:type spacer)))
        (is (= :grow (get-in spacer [:layout :sizing :width :type])))
        (is (= :grow (get-in spacer [:layout :sizing :height :type])))))))

;; ============================================================================
;; PARSE TESTS - NESTED STRUCTURE
;; ============================================================================

(deftest parse-nested-test
  (testing "Nested elements create tree structure"
    (let [tree (h/parse test-viewport
                        [:col
                         [:row
                          [:box]
                          [:box]]])]
      (let [col (first (:children tree))
            row (first (:children col))]
        (is (= :container (:type col)))
        (is (= :container (:type row)))
        (is (= 2 (count (:children row))))))))

(deftest parse-multiple-children-test
  (testing "Multiple children are parsed correctly"
    (let [tree (h/parse test-viewport
                        [:col
                         [:text "One"]
                         [:text "Two"]
                         [:text "Three"]]
                        mock-measure-fn)]
      (let [col (first (:children tree))]
        (is (= 3 (count (:children col))))
        (is (every? #(= :text (:type %)) (:children col)))))))

;; ============================================================================
;; PARSE TESTS - PROPS
;; ============================================================================

(deftest parse-sizing-props-test
  (testing "Size props are normalized"
    (let [tree (h/parse test-viewport
                        [:box {:size 100}])]
      (let [box (first (:children tree))]
        (is (= :fixed (get-in box [:layout :sizing :width :type])))
        (is (= 100 (get-in box [:layout :sizing :width :value]))))))

  (testing ":grow size creates grow sizing"
    (let [tree (h/parse test-viewport
                        [:box {:size :grow}])]
      (let [box (first (:children tree))]
        (is (= :grow (get-in box [:layout :sizing :width :type])))
        (is (= :grow (get-in box [:layout :sizing :height :type])))))))

(deftest parse-padding-props-test
  (testing "Padding props are normalized"
    (let [tree (h/parse test-viewport
                        [:col {:padding 16}])]
      (let [col (first (:children tree))
            padding (get-in col [:layout :padding])]
        (is (= 16 (:top padding)))
        (is (= 16 (:right padding)))
        (is (= 16 (:bottom padding)))
        (is (= 16 (:left padding))))))

  (testing "Asymmetric padding"
    (let [tree (h/parse test-viewport
                        [:col {:padding [10 20]}])]
      (let [col (first (:children tree))
            padding (get-in col [:layout :padding])]
        (is (= 10 (:top padding)))
        (is (= 20 (:right padding)))
        (is (= 10 (:bottom padding)))
        (is (= 20 (:left padding)))))))

(deftest parse-gap-props-test
  (testing "Gap props are normalized"
    (let [tree (h/parse test-viewport
                        [:col {:gap 8}])]
      (let [col (first (:children tree))]
        (is (= 8 (get-in col [:layout :child-gap])))))))

(deftest parse-background-props-test
  (testing "Background color creates config"
    (let [tree (h/parse test-viewport
                        [:box {:bg :red-500}])]
      (let [box (first (:children tree))]
        (is (some? (get-in box [:configs :background])))
        (is (= :red-500 (get-in box [:configs :background :color])))))))

(deftest parse-border-radius-test
  (testing "Radius prop creates corner radius"
    (let [tree (h/parse test-viewport
                        [:box {:bg :blue :radius 8}])]
      (let [box (first (:children tree))
            radius (get-in box [:configs :background :corner-radius])]
        (is (= 8 (:top-left radius)))
        (is (= 8 (:top-right radius)))
        (is (= 8 (:bottom-left radius)))
        (is (= 8 (:bottom-right radius)))))))

;; ============================================================================
;; PARSE TESTS - TEXT PROPS
;; ============================================================================

(deftest parse-text-props-test
  (testing "Text props are normalized"
    (let [tree (h/parse test-viewport
                        [:text "Hello" {:font-size 24 :color :white}]
                        mock-measure-fn)]
      (let [text (first (:children tree))
            config (:text-config text)]
        (is (= 24 (:font-size config)))
        (is (= :white (:text-color config)))))))

;; ============================================================================
;; PARSE TESTS - SCROLL
;; ============================================================================

(deftest parse-scroll-test
  (testing "Scroll container creates clip config"
    (let [tree (h/parse test-viewport
                        [:scroll {:direction :vertical}
                         [:col]])]
      (let [scroll (first (:children tree))
            clip (get-in scroll [:configs :clip])]
        (is (some? clip))
        (is (true? (:vertical clip)))
        (is (false? (:horizontal clip)))))))

;; ============================================================================
;; PARSE TESTS - CUSTOM COMPONENTS
;; ============================================================================

(deftest custom-component-test
  (testing "Custom components expand correctly"
    (h/register-component! :card
      (fn [props & children]
        [:box (merge {:bg :white :radius 8 :padding 16} props)
         children]))

    (let [tree (h/parse test-viewport
                        [:card {:bg :gray}
                         [:text "Content"]]
                        mock-measure-fn)]
      (let [card (first (:children tree))]
        (is (= :container (:type card)))
        (is (= :gray (get-in card [:configs :background :color])))))

    (h/unregister-component! :card)))

;; ============================================================================
;; RENDER TESTS
;; ============================================================================

(deftest render-basic-test
  (testing "render produces render commands"
    (let [commands (h/render test-viewport
                             [:col {:bg :slate-100}
                              [:text "Hello"]]
                             mock-measure-fn)]
      (is (vector? commands))
      (is (pos? (count commands)))
      (is (some #(= :rectangle (:command-type %)) commands) "Has background")
      (is (some #(= :text (:command-type %)) commands) "Has text"))))

(deftest render-complex-layout-test
  (testing "Complex layout renders correctly"
    (let [commands (h/render test-viewport
                             [:col {:size :grow :padding 16 :gap 8}
                              [:row {:gap 8}
                               [:box {:size 100 :bg :red-500}]
                               [:box {:size :grow :bg :blue-500}]]
                              [:text "Footer" {:font-size 12}]]
                             mock-measure-fn)]
      (is (vector? commands))
      ;; Should have multiple rectangle commands (for the boxes)
      (let [rects (filter #(= :rectangle (:command-type %)) commands)]
        (is (>= (count rects) 2))))))

(deftest render-tree-test
  (testing "render-tree returns positioned tree"
    (let [tree (h/render-tree test-viewport
                              [:box {:size 100}]
                              mock-measure-fn)]
      (is (map? tree))
      (is (= :container (:type tree)))
      (is (= :root (:id tree)))
      (let [box (first (:children tree))]
        (is (some? (:bounding-box box)))
        (is (= 100 (:width (:bounding-box box))))
        (is (= 100 (:height (:bounding-box box))))))))

;; ============================================================================
;; EDGE CASES
;; ============================================================================

(deftest empty-hiccup-test
  (testing "Nil hiccup returns root-only tree"
    (let [tree (h/parse test-viewport nil)]
      (is (= :root (:id tree)))
      (is (empty? (:children tree))))))

(deftest string-content-test
  (testing "Raw string is converted to text node"
    (let [tree (h/parse test-viewport "Just text" mock-measure-fn)]
      (let [text (first (:children tree))]
        (is (= :text (:type text)))
        (is (= "Just text" (:text-content text)))))))

(deftest sequence-children-test
  (testing "Sequence of elements are flattened"
    (let [items ["One" "Two" "Three"]
          tree (h/parse test-viewport
                        [:col
                         (map (fn [s] [:text s]) items)]
                        mock-measure-fn)]
      (let [col (first (:children tree))]
        (is (= 3 (count (:children col))))))))

(deftest element-id-test
  (testing "Custom IDs are preserved"
    (let [tree (h/parse test-viewport
                        [:box {:id :my-box}])]
      (let [box (first (:children tree))]
        (is (= :my-box (:id box)))))))

;; ============================================================================
;; INTEGRATION TEST
;; ============================================================================

(deftest full-dashboard-test
  (testing "Dashboard-style layout works"
    (let [commands (h/render
                    {:width 1200 :height 800}
                    [:row {:size :grow}
                     ;; Sidebar
                     [:col {:size [200 :grow] :bg :slate-800 :padding 16 :gap 8}
                      [:text "Menu" {:color :white}]
                      [:spacer]]
                     ;; Main content
                     [:col {:size :grow :padding 24 :gap 16}
                      ;; Header
                      [:row {:size [:grow 48] :bg :white :radius 8 :padding [0 16]}
                       [:text "Dashboard"]]
                      ;; Cards
                      [:row {:size [:grow 200] :gap 16}
                       [:box {:size :grow :bg :white :radius 8}]
                       [:box {:size :grow :bg :white :radius 8}]
                       [:box {:size :grow :bg :white :radius 8}]]
                      [:spacer]]]
                    mock-measure-fn)]
      (is (vector? commands))
      (is (pos? (count commands)))
      ;; Should have many rectangles for all the cards/boxes
      (let [rects (filter #(= :rectangle (:command-type %)) commands)]
        (is (>= (count rects) 5) "Multiple background rectangles")))))
