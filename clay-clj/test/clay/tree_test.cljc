(ns clay.tree-test
  "Tests for Specter navigators and tree utilities."
  (:require [clojure.test :refer [deftest is testing]]
            [com.rpl.specter :as s]
            [clay.tree :as t]))

;; ============================================================================
;; TEST DATA
;; ============================================================================

(def simple-tree
  {:type :container
   :id :root
   :layout {:sizing {:width {:type :fixed :value 800}
                     :height {:type :fixed :value 600}}
            :padding {:top 0 :right 0 :bottom 0 :left 0}
            :child-gap 0
            :layout-direction :top-to-bottom
            :child-alignment {:x :left :y :top}}
   :configs {}
   :dimensions {:width 800 :height 600}
   :bounding-box {:x 0 :y 0 :width 800 :height 600}
   :children [{:type :container
               :id :child-1
               :layout {:sizing {:width {:type :fixed :value 100}
                                 :height {:type :fixed :value 50}}
                        :padding {:top 0 :right 0 :bottom 0 :left 0}
                        :child-gap 0
                        :layout-direction :left-to-right
                        :child-alignment {:x :left :y :top}}
               :configs {}
               :dimensions {:width 100 :height 50}
               :bounding-box {:x 0 :y 0 :width 100 :height 50}
               :children []}
              {:type :text
               :id :text-1
               :text-content "Hello"
               :text-config {:font-id 0 :font-size 16}
               :measured {:width 50 :height 16}
               :dimensions {:width 50 :height 16}
               :bounding-box {:x 0 :y 0 :width 50 :height 16}
               :wrapped-lines nil}]})

(def nested-tree
  {:type :container
   :id :root
   :layout t/default-layout
   :configs {}
   :dimensions {:width nil :height nil}
   :bounding-box {:x 0 :y 0 :width 0 :height 0}
   :children [{:type :container
               :id :level-1
               :layout t/default-layout
               :configs {}
               :dimensions {:width nil :height nil}
               :bounding-box {:x 0 :y 0 :width 0 :height 0}
               :children [{:type :text
                           :id :level-2-text
                           :text-content "Deep"
                           :text-config {}
                           :measured {:width 40 :height 16}
                           :dimensions {:width 40 :height 16}
                           :bounding-box {:x 0 :y 0 :width 0 :height 0}
                           :wrapped-lines nil}]}]})

;; ============================================================================
;; NAVIGATOR TESTS
;; ============================================================================

(deftest tree-nodes-navigator
  (testing "TREE-NODES navigates to all nodes in pre-order"
    (let [nodes (s/select [t/TREE-NODES] simple-tree)
          ids (map :id nodes)]
      (is (= 3 (count nodes)) "Should find 3 nodes")
      (is (= [:root :child-1 :text-1] ids) "Pre-order: root first, then children"))))

(deftest tree-nodes-post-navigator
  (testing "TREE-NODES-POST navigates to all nodes in post-order"
    (let [nodes (s/select [t/TREE-NODES-POST] simple-tree)
          ids (map :id nodes)]
      (is (= 3 (count nodes)) "Should find 3 nodes")
      (is (= [:child-1 :text-1 :root] ids) "Post-order: children first, then root"))))

(deftest containers-navigator
  (testing "CONTAINERS navigates to container nodes only"
    (let [containers (s/select [t/CONTAINERS] simple-tree)]
      (is (= 2 (count containers)) "Should find 2 containers")
      (is (every? #(= :container (:type %)) containers)))))

(deftest containers-post-navigator
  (testing "CONTAINERS-POST navigates in post-order"
    (let [containers (s/select [t/CONTAINERS-POST] simple-tree)
          ids (map :id containers)]
      (is (= [:child-1 :root] ids) "Post-order for containers"))))

(deftest text-nodes-navigator
  (testing "TEXT-NODES navigates to text nodes only"
    (let [texts (s/select [t/TEXT-NODES] simple-tree)]
      (is (= 1 (count texts)) "Should find 1 text node")
      (is (= :text-1 (:id (first texts)))))))

(deftest leaves-navigator
  (testing "LEAVES navigates to leaf nodes"
    (let [leaves (s/select [t/LEAVES] simple-tree)]
      (is (= 2 (count leaves)) "Should find 2 leaves (empty container + text)")
      (is (some #(= :child-1 (:id %)) leaves) "Empty container is a leaf")
      (is (some #(= :text-1 (:id %)) leaves) "Text is a leaf"))))

(deftest element-by-id-navigator
  (testing "ELEMENT-BY-ID finds element by ID"
    (let [found (s/select-one [(t/ELEMENT-BY-ID :text-1)] simple-tree)]
      (is (some? found) "Should find element")
      (is (= :text-1 (:id found)))))

  (testing "ELEMENT-BY-ID in nested tree"
    (let [found (s/select-one [(t/ELEMENT-BY-ID :level-2-text)] nested-tree)]
      (is (some? found) "Should find deeply nested element")
      (is (= "Deep" (:text-content found))))))

;; ============================================================================
;; PREDICATE NAVIGATOR TESTS
;; ============================================================================

(deftest with-sizing-type-navigator
  (testing "with-sizing-type filters by sizing"
    (let [fixed-tree {:type :container
                      :id :test
                      :layout {:sizing {:width {:type :fixed :value 100}
                                        :height {:type :grow}}}
                      :children []}
          width-fixed? (s/select-one [t/TREE-NODES (t/with-sizing-type :fixed true)] fixed-tree)
          height-grow? (s/select-one [t/TREE-NODES (t/with-sizing-type :grow false)] fixed-tree)]
      (is (some? width-fixed?) "Should find fixed width")
      (is (some? height-grow?) "Should find grow height"))))

(deftest with-config-navigator
  (testing "with-config filters by config presence"
    (let [tree-with-bg {:type :container
                        :id :test
                        :layout t/default-layout
                        :configs {:background {:color {:r 255 :g 0 :b 0}}}
                        :children []}
          found (s/select-one [t/TREE-NODES (t/with-config :background)] tree-with-bg)]
      (is (some? found) "Should find node with background config"))))

;; ============================================================================
;; DIMENSION HELPER TESTS
;; ============================================================================

(deftest dim-helpers
  (testing "dim gets dimension from node"
    (let [node {:dimensions {:width 100 :height 50}}]
      (is (= 100 (t/dim node true)) "Width dimension")
      (is (= 50 (t/dim node false)) "Height dimension")))

  (testing "set-dim sets dimension on node"
    (let [node {:dimensions {:width nil :height nil}}
          updated (-> node
                      (t/set-dim true 200)
                      (t/set-dim false 100))]
      (is (= 200 (t/dim updated true)))
      (is (= 100 (t/dim updated false))))))

(deftest sizing-type-helpers
  (testing "sizing-type returns correct type"
    (let [node {:layout {:sizing {:width {:type :fixed :value 100}
                                  :height {:type :grow}}}}]
      (is (= :fixed (t/sizing-type node true)))
      (is (= :grow (t/sizing-type node false)))))

  (testing "sizing-value returns value for fixed/percent"
    (let [node {:layout {:sizing {:width {:type :fixed :value 100}
                                  :height {:type :percent :value 50}}}}]
      (is (= 100 (t/sizing-value node true)))
      (is (= 50 (t/sizing-value node false))))))

(deftest padding-total-helper
  (testing "padding-total calculates total padding"
    (let [node {:layout {:padding {:top 10 :right 20 :bottom 10 :left 20}}}]
      (is (= 40 (t/padding-total node true)) "Horizontal: left + right")
      (is (= 20 (t/padding-total node false)) "Vertical: top + bottom"))))

(deftest along-axis-helper
  (testing "along-axis? checks layout direction"
    (let [horizontal {:layout {:layout-direction :left-to-right}}
          vertical {:layout {:layout-direction :top-to-bottom}}]
      (is (t/along-axis? horizontal true) "Horizontal along x-axis")
      (is (not (t/along-axis? horizontal false)) "Horizontal not along y-axis")
      (is (not (t/along-axis? vertical true)) "Vertical not along x-axis")
      (is (t/along-axis? vertical false) "Vertical along y-axis"))))

;; ============================================================================
;; TREE CONSTRUCTION TESTS
;; ============================================================================

(deftest container-construction
  (testing "container creates valid container node"
    (let [node (t/container {:id :test
                             :layout {:sizing {:width {:type :fixed :value 100}}}
                             :configs {:background {:color :red}}})]
      (is (= :container (:type node)))
      (is (= :test (:id node)))
      (is (= :fixed (get-in node [:layout :sizing :width :type])))
      (is (some? (get-in node [:configs :background]))))))

(deftest text-node-construction
  (testing "text-node creates valid text node"
    (let [node (t/text-node {:id :text
                             :content "Hello"
                             :config {:font-size 20}
                             :measured {:width 60 :height 20}})]
      (is (= :text (:type node)))
      (is (= "Hello" (:text-content node)))
      (is (= 60 (:width (:dimensions node))))
      (is (= 20 (:height (:dimensions node)))))))

(deftest root-container-construction
  (testing "root-container creates viewport-sized root"
    (let [viewport {:width 1024 :height 768}
          root (t/root-container viewport [{:type :text :id :child}])]
      (is (= :root (:id root)))
      (is (= 1024 (t/dim root true)))
      (is (= 768 (t/dim root false)))
      (is (= 1024 (get-in root [:bounding-box :width])))
      (is (= 768 (get-in root [:bounding-box :height])))
      (is (= 1 (count (:children root)))))))

;; ============================================================================
;; TRANSFORM TESTS
;; ============================================================================

(deftest transform-all-nodes
  (testing "Can transform all nodes with TREE-NODES"
    (let [tree {:type :container
                :id :root
                :layout t/default-layout
                :dimensions {:width nil :height nil}
                :bounding-box {:x 0 :y 0 :width 0 :height 0}
                :configs {}
                :children [{:type :text
                            :id :text
                            :text-content "Test"
                            :text-config {}
                            :measured {:width 40 :height 16}
                            :dimensions {:width nil :height nil}
                            :bounding-box {:x 0 :y 0 :width 0 :height 0}
                            :wrapped-lines nil}]}
          marked (s/transform [t/TREE-NODES]
                              #(assoc % :visited true)
                              tree)]
      (is (:visited marked) "Root visited")
      (is (:visited (first (:children marked))) "Child visited"))))

(deftest transform-post-order
  (testing "TREE-NODES-POST visits children before parents"
    (let [visit-order (atom [])
          tree {:type :container
                :id :parent
                :layout t/default-layout
                :configs {}
                :dimensions {:width nil :height nil}
                :bounding-box {:x 0 :y 0 :width 0 :height 0}
                :children [{:type :container
                            :id :child
                            :layout t/default-layout
                            :configs {}
                            :dimensions {:width nil :height nil}
                            :bounding-box {:x 0 :y 0 :width 0 :height 0}
                            :children []}]}]
      (s/transform [t/TREE-NODES-POST]
                   (fn [node]
                     (swap! visit-order conj (:id node))
                     node)
                   tree)
      (is (= [:child :parent] @visit-order) "Child visited before parent"))))
