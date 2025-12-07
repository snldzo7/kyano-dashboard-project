(ns clay.layout.aspect-ratio-test
  "Tests for aspect ratio constraint system."
  (:require [clojure.test :refer [deftest is testing]]
            [clay.layout.core :as layout]
            [clay.layout.aspect-ratio :as aspect]))

;; ============================================================================
;; TEST FIXTURES
;; ============================================================================

(def test-viewport {:width 800 :height 600})

(defn mock-measure-fn [text config]
  {:width (* (count text) 8) :height 16})

(defn approx=
  "Approximate equality for floating point comparisons."
  [expected actual & [epsilon]]
  (let [eps (or epsilon 0.001)]
    (< (Math/abs (- (double expected) (double actual))) eps)))

;; ============================================================================
;; ASPECT RATIO CONFIG DETECTION
;; ============================================================================

(deftest aspect-ratio-config-detection
  (testing "Element with aspect ratio config is detected"
    (let [element {:configs [{:type :aspect-ratio :config {:ratio 1.5}}]}]
      (is (aspect/aspect-ratio-config? element))
      (is (= 1.5 (aspect/get-aspect-ratio element)))))

  (testing "Element without aspect ratio config is not detected"
    (let [element {:configs [{:type :background :config {:color {:r 255 :g 0 :b 0 :a 1}}}]}]
      (is (not (aspect/aspect-ratio-config? element)))
      (is (nil? (aspect/get-aspect-ratio element))))))

;; ============================================================================
;; UPDATE ASPECT RATIO BOX
;; ============================================================================

(deftest update-aspect-ratio-box-test
  (testing "Width calculated from height"
    (let [element {:dimensions {:width 0 :height 100}
                   :configs [{:type :aspect-ratio :config {:ratio 2.0}}]}
          updated (aspect/update-aspect-ratio-box element)]
      ;; width = height * ratio = 100 * 2.0 = 200
      (is (= 200.0 (get-in updated [:dimensions :width])))))

  (testing "Height calculated from width"
    (let [element {:dimensions {:width 200 :height 0}
                   :configs [{:type :aspect-ratio :config {:ratio 2.0}}]}
          updated (aspect/update-aspect-ratio-box element)]
      ;; height = width / ratio = 200 / 2.0 = 100
      (is (= 100.0 (get-in updated [:dimensions :height])))))

  (testing "No change when both dimensions set"
    (let [element {:dimensions {:width 200 :height 100}
                   :configs [{:type :aspect-ratio :config {:ratio 2.0}}]}
          updated (aspect/update-aspect-ratio-box element)]
      (is (= 200 (get-in updated [:dimensions :width])))
      (is (= 100 (get-in updated [:dimensions :height])))))

  (testing "No change when aspect ratio is zero"
    (let [element {:dimensions {:width 0 :height 100}
                   :configs [{:type :aspect-ratio :config {:ratio 0}}]}
          updated (aspect/update-aspect-ratio-box element)]
      (is (= 0 (get-in updated [:dimensions :width]))))))

;; ============================================================================
;; APPLY ASPECT HEIGHT
;; ============================================================================

(deftest apply-aspect-height-test
  (testing "Height calculated from width"
    (let [element {:dimensions {:width 300 :height 0}
                   :layout {:sizing {:height {}}}
                   :configs [{:type :aspect-ratio :config {:ratio 1.5}}]}
          updated (aspect/apply-aspect-height element)]
      ;; height = width / ratio = 300 / 1.5 = 200
      (is (= 200.0 (get-in updated [:dimensions :height])))
      ;; Max height should also be set
      (is (= 200.0 (get-in updated [:layout :sizing :height :max])))))

  (testing "Square aspect ratio"
    (let [element {:dimensions {:width 100 :height 0}
                   :layout {:sizing {:height {}}}
                   :configs [{:type :aspect-ratio :config {:ratio 1.0}}]}
          updated (aspect/apply-aspect-height element)]
      ;; height = width / 1.0 = 100
      (is (= 100.0 (get-in updated [:dimensions :height]))))))

;; ============================================================================
;; APPLY ASPECT WIDTH
;; ============================================================================

(deftest apply-aspect-width-test
  (testing "Width calculated from height"
    (let [element {:dimensions {:width 0 :height 200}
                   :configs [{:type :aspect-ratio :config {:ratio 1.5}}]}
          updated (aspect/apply-aspect-width element)]
      ;; width = height * ratio = 200 * 1.5 = 300
      (is (= 300.0 (get-in updated [:dimensions :width])))))

  (testing "16:9 aspect ratio"
    (let [element {:dimensions {:width 0 :height 90}
                   :configs [{:type :aspect-ratio :config {:ratio (/ 16 9)}}]}
          updated (aspect/apply-aspect-width element)]
      ;; width = 90 * (16/9) = 160
      (is (= 160.0 (get-in updated [:dimensions :width]))))))

;; ============================================================================
;; INTEGRATION WITH LAYOUT
;; ============================================================================

(deftest aspect-ratio-in-layout-test
  (testing "Aspect ratio element maintains ratio in layout"
    (let [result (-> (layout/begin-layout test-viewport)
                     ;; Create element with fixed width and aspect ratio
                     (layout/open-element {:sizing {:width {:type :fixed :value 300}
                                                    :height {:type :fit}}})
                     (layout/configure-element :aspect-ratio {:ratio 1.5})
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          element (get (:layout-elements result) 1)
          box (:bounding-box element)]
      ;; Width is 300 (fixed), height should be 300/1.5 = 200
      (is (= 300.0 (:width box)))
      (is (= 200.0 (:height box)))))

  (testing "Aspect ratio with fixed height"
    (let [result (-> (layout/begin-layout test-viewport)
                     ;; Create element with fixed height and aspect ratio
                     (layout/open-element {:sizing {:width {:type :fit}
                                                    :height {:type :fixed :value 200}}})
                     (layout/configure-element :aspect-ratio {:ratio 2.0})
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          element (get (:layout-elements result) 1)
          box (:bounding-box element)]
      ;; Height is 200 (fixed), width should be 200*2.0 = 400
      (is (approx= 400.0 (:width box)))
      (is (approx= 200.0 (:height box)))))

  (testing "16:9 video aspect ratio"
    (let [result (-> (layout/begin-layout test-viewport)
                     (layout/open-element {:sizing {:width {:type :fixed :value 640}
                                                    :height {:type :fit}}})
                     (layout/configure-element :aspect-ratio {:ratio (/ 16 9)})
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          element (get (:layout-elements result) 1)
          box (:bounding-box element)]
      ;; Width 640, height = 640 / (16/9) = 360
      (is (= 640.0 (:width box)))
      (is (= 360.0 (:height box))))))

;; ============================================================================
;; ASPECT RATIO WITH PARENT CONSTRAINTS
;; ============================================================================

(deftest aspect-ratio-with-constraints-test
  (testing "Aspect ratio element inside fixed container"
    (let [result (-> (layout/begin-layout test-viewport)
                     ;; Container with fixed size
                     (layout/open-element {:sizing {:width {:type :fixed :value 400}
                                                    :height {:type :fixed :value 300}}})
                     ;; Child with grow and aspect ratio
                     (layout/open-element {:sizing {:width {:type :grow}
                                                    :height {:type :fit}}})
                     (layout/configure-element :aspect-ratio {:ratio 2.0})
                     (layout/close-element)
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          child (get (:layout-elements result) 2)
          child-box (:bounding-box child)]
      ;; Child should grow to container width (400)
      ;; Height = 400 / 2.0 = 200
      (is (= 400.0 (:width child-box)))
      (is (= 200.0 (:height child-box))))))

;; ============================================================================
;; MULTIPLE ASPECT RATIO ELEMENTS
;; ============================================================================

(deftest multiple-aspect-ratio-elements-test
  (testing "Multiple aspect ratio elements"
    (let [result (-> (layout/begin-layout test-viewport)
                     ;; First element: 4:3
                     (layout/open-element {:sizing {:width {:type :fixed :value 400}
                                                    :height {:type :fit}}})
                     (layout/configure-element :aspect-ratio {:ratio (/ 4.0 3.0)})
                     (layout/close-element)
                     ;; Second element: 16:9
                     (layout/open-element {:sizing {:width {:type :fixed :value 320}
                                                    :height {:type :fit}}})
                     (layout/configure-element :aspect-ratio {:ratio (/ 16.0 9.0)})
                     (layout/close-element)
                     (layout/end-layout mock-measure-fn))
          elem1 (get (:layout-elements result) 1)
          elem2 (get (:layout-elements result) 2)]
      ;; Element 1: 400 width, 300 height (4:3)
      (is (approx= 400.0 (get-in elem1 [:bounding-box :width])))
      (is (approx= 300.0 (get-in elem1 [:bounding-box :height])))
      ;; Element 2: 320 width, 180 height (16:9)
      (is (approx= 320.0 (get-in elem2 [:bounding-box :width])))
      (is (approx= 180.0 (get-in elem2 [:bounding-box :height]))))))
