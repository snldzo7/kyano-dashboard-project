(ns clay.layout.scroll-test
  "Tests for scroll containers system."
  (:require [clojure.test :refer [deftest is testing]]
            [clay.layout.core :as layout]
            [clay.layout.scroll :as scroll]))

;; ============================================================================
;; TEST FIXTURES
;; ============================================================================

(def test-viewport {:width 800 :height 600})

(defn mock-measure-fn [text config]
  {:width (* (count text) 8) :height 16})

;; ============================================================================
;; SCROLL CONFIGURATION TESTS
;; ============================================================================

(deftest scroll-config-detection
  (testing "Element with scroll config is detected"
    (let [element {:configs [{:type :scroll :config {:horizontal false :vertical true}}]}]
      (is (scroll/scroll-config? element))
      (is (= {:horizontal false :vertical true :child-offset {:x 0 :y 0}}
             (scroll/get-scroll-config element)))))

  (testing "Element without scroll config is not detected"
    (let [element {:configs [{:type :background :config {:color {:r 255 :g 0 :b 0 :a 1}}}]}]
      (is (not (scroll/scroll-config? element)))
      (is (nil? (scroll/get-scroll-config element))))))

;; ============================================================================
;; SCROLL CONTAINER IN LAYOUT TESTS
;; ============================================================================

(deftest scroll-container-layout
  (testing "Scroll container is created and tracked"
    (let [result (-> (layout/begin-layout test-viewport)
                     ;; Scroll container
                     (layout/open-element {:sizing {:width {:type :fixed :value 300}
                                                    :height {:type :fixed :value 200}}})
                     (layout/configure-element :scroll {:horizontal false :vertical true})
                     (layout/configure-element :background {:color {:r 200 :g 200 :b 200 :a 1}})
                     ;; Content taller than container
                     (layout/open-element {:sizing {:width {:type :grow}
                                                    :height {:type :fixed :value 500}}})
                     (layout/configure-element :background {:color {:r 100 :g 100 :b 100 :a 1}})
                     (layout/close-element)  ; Close content
                     (layout/close-element)  ; Close scroll container
                     (layout/end-layout mock-measure-fn))
          scroll-container (get (:layout-elements result) 1)]
      ;; Verify scroll config is present
      (is (scroll/scroll-config? scroll-container))
      (is (:vertical (scroll/get-scroll-config scroll-container))))))

(deftest scroll-offset-application
  (testing "Scroll offset is applied to children"
    (let [scroll-positions {1 {:x 0 :y 50}}  ; Scroll down 50px
          result (-> (layout/begin-layout test-viewport)
                     ;; Scroll container at idx 1
                     (layout/open-element {:sizing {:width {:type :fixed :value 300}
                                                    :height {:type :fixed :value 200}}})
                     (layout/configure-element :scroll {:horizontal false :vertical true})
                     ;; Child content at idx 2
                     (layout/open-element {:sizing {:width {:type :fixed :value 280}
                                                    :height {:type :fixed :value 100}}})
                     (layout/close-element)  ; Close child
                     (layout/close-element)  ; Close scroll container
                     (layout/end-layout mock-measure-fn scroll-positions))
          child (get (:layout-elements result) 2)
          child-box (:bounding-box child)]
      ;; Child should be offset by -50 in Y (scroll down = content moves up)
      (is (= -50 (:y child-box))))))

;; ============================================================================
;; CLIP COMMAND GENERATION TESTS
;; ============================================================================

(deftest clip-commands-generated
  (testing "Clip commands are generated for scroll containers"
    (let [result (-> (layout/begin-layout test-viewport)
                     ;; Scroll container
                     (layout/open-element {:sizing {:width {:type :fixed :value 300}
                                                    :height {:type :fixed :value 200}}})
                     (layout/configure-element :scroll {:horizontal false :vertical true})
                     (layout/configure-element :background {:color {:r 200 :g 200 :b 200 :a 1}})
                     ;; Content
                     (layout/open-element {:sizing {:width {:type :grow}
                                                    :height {:type :fixed :value 100}}})
                     (layout/configure-element :background {:color {:r 100 :g 100 :b 100 :a 1}})
                     (layout/close-element)
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          commands (layout/get-render-commands result)
          command-types (map :command-type commands)]
      ;; Should have clip and clip-end commands
      (is (some #{:clip} command-types))
      (is (some #{:clip-end} command-types)))))

;; ============================================================================
;; SCROLL CONTAINER DATA TESTS
;; ============================================================================

(deftest scroll-container-data-retrieval
  (testing "Scroll container data can be retrieved"
    (let [result (-> (layout/begin-layout test-viewport)
                     ;; Scroll container at idx 1
                     (layout/open-element {:sizing {:width {:type :fixed :value 300}
                                                    :height {:type :fixed :value 200}}})
                     (layout/configure-element :scroll {:horizontal false :vertical true})
                     ;; Tall content
                     (layout/open-element {:sizing {:width {:type :grow}
                                                    :height {:type :fixed :value 500}}})
                     (layout/close-element)
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          scroll-container (get (:layout-elements result) 1)
          element-id (get-in scroll-container [:id :id])
          scroll-data (layout/get-scroll-container-data result element-id)]
      ;; Should find the scroll container
      (is (:found scroll-data))
      ;; Container dimensions should match
      (is (= 300 (get-in scroll-data [:scroll-container-dimensions :width])))
      (is (= 200 (get-in scroll-data [:scroll-container-dimensions :height])))
      ;; Content should be taller
      (is (>= (get-in scroll-data [:content-dimensions :height]) 500)))))

(deftest scroll-container-not-found
  (testing "Non-scroll element returns not found"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element {:sizing {:width {:type :fixed :value 100}
                                                    :height {:type :fixed :value 100}}})
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          scroll-data (layout/get-scroll-container-data result 999)]
      (is (not (:found scroll-data))))))

;; ============================================================================
;; SCROLL POSITION CLAMPING TESTS
;; ============================================================================

(deftest scroll-position-clamping
  (testing "Scroll position is clamped to valid range"
    (let [scroll-data {:scroll-container-dimensions {:width 300 :height 200}
                       :content-dimensions {:width 300 :height 500}
                       :config {:horizontal false :vertical true}}]
      ;; Within range
      (is (= {:x 0 :y 100} (scroll/clamp-scroll-position scroll-data {:x 0 :y 100})))
      ;; Negative clamped to 0
      (is (= {:x 0 :y 0} (scroll/clamp-scroll-position scroll-data {:x 0 :y -50})))
      ;; Beyond max clamped
      (is (= {:x 0 :y 300} (scroll/clamp-scroll-position scroll-data {:x 0 :y 500})))
      ;; Horizontal disabled - always 0
      (is (= {:x 0 :y 100} (scroll/clamp-scroll-position scroll-data {:x 50 :y 100})))))

  (testing "Horizontal scroll when enabled"
    (let [scroll-data {:scroll-container-dimensions {:width 200 :height 200}
                       :content-dimensions {:width 500 :height 200}
                       :config {:horizontal true :vertical false}}]
      (is (= {:x 100 :y 0} (scroll/clamp-scroll-position scroll-data {:x 100 :y 50})))
      (is (= {:x 300 :y 0} (scroll/clamp-scroll-position scroll-data {:x 400 :y 0}))))))

;; ============================================================================
;; SCROLL POSITION UPDATE TESTS
;; ============================================================================

(deftest scroll-position-update
  (testing "Scroll position can be updated"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element {:sizing {:width {:type :fixed :value 300}
                                                    :height {:type :fixed :value 200}}})
                     (layout/configure-element :scroll {:horizontal false :vertical true})
                     (layout/open-element {:sizing {:width {:type :grow}
                                                    :height {:type :fixed :value 500}}})
                     (layout/close-element)
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          scroll-container (get (:layout-elements result) 1)
          element-id (get-in scroll-container [:id :id])
          {:keys [state new-position]} (layout/update-scroll-position result element-id {:x 0 :y 50})]
      ;; Position should be updated
      (is (= 50 (:y new-position)))
      ;; State should be updated
      (is (= 50 (get-in state [:layout-elements 1 :scroll-data :scroll-position :y]))))))
