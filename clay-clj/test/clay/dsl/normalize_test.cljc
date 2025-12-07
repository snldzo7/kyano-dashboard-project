(ns clay.dsl.normalize-test
  "Tests for DSL normalization multi-method system."
  (:require [clojure.test :refer [deftest is testing]]
            [clay.dsl.normalize :as norm]))

;; ============================================================================
;; DISPATCH FUNCTION TESTS
;; ============================================================================

(deftest dispatch-function-test
  (testing "Dispatch function returns correct key-spec and value-spec"
    (is (= [::norm/dimension-prop ::norm/keyword] (norm/dispatch-normalize :width :grow)))
    (is (= [::norm/padding-prop ::norm/number] (norm/dispatch-normalize :padding 16)))
    (is (= [::norm/sizing-prop ::norm/vector] (norm/dispatch-normalize :size [:grow :fit])))
    (is (= [::norm/color-prop ::norm/keyword] (norm/dispatch-normalize :bg :red-500)))
    (is (= [::norm/border-prop ::norm/number] (norm/dispatch-normalize :border 2)))))

(deftest infer-value-spec-test
  (testing "Value spec inference from Malli schemas"
    (is (= ::norm/keyword (norm/infer-value-spec :grow)))
    (is (= ::norm/number (norm/infer-value-spec 300)))
    (is (= ::norm/vector (norm/infer-value-spec [255 0 0])))
    (is (= ::norm/color (norm/infer-value-spec {:r 255 :g 0 :b 0})))
    (is (= ::norm/padding (norm/infer-value-spec {:top 16 :left 16 :right 16 :bottom 16})))))

;; ============================================================================
;; DIMENSION NORMALIZATION TESTS
;; ============================================================================

(deftest dimension-keyword-test
  (testing "Dimension keyword normalization"
    (is (= {:type :grow} (norm/normalize :width :grow)))
    (is (= {:type :fit} (norm/normalize :height :fit)))))

(deftest dimension-number-test
  (testing "Dimension number normalization (fixed)"
    (is (= {:type :fixed :value 300} (norm/normalize :width 300)))
    (is (= {:type :fixed :value 100} (norm/normalize :height 100)))))

(deftest dimension-percent-test
  (testing "Dimension percent normalization"
    (is (= {:type :percent :value 0.5} (norm/normalize :width [:% 50])))
    (is (= {:type :percent :value 0.75} (norm/normalize :width [:percent 75])))))

(deftest dimension-constrained-test
  (testing "Constrained dimension - no args"
    (is (= {:type :grow} (norm/normalize :width [:grow])))
    (is (= {:type :fit} (norm/normalize :width [:fit]))))

  (testing "Constrained dimension - min only"
    (is (= {:type :grow :min 100} (norm/normalize :width [:grow 100])))
    (is (= {:type :fit :min 50} (norm/normalize :height [:fit 50]))))

  (testing "Constrained dimension - min and max"
    (is (= {:type :grow :min 100 :max 500} (norm/normalize :width [:grow 100 500]))))

  (testing "Constrained dimension - map form"
    (is (= {:type :grow :max 500} (norm/normalize :width [:grow {:max 500}])))
    (is (= {:type :grow :min 100 :max 500} (norm/normalize :width [:grow {:min 100 :max 500}])))
    (is (= {:type :fit :min 50 :max 200} (norm/normalize :height [:fit {:min 50 :max 200}])))))

;; ============================================================================
;; SIZING NORMALIZATION TESTS
;; ============================================================================

(deftest sizing-keyword-test
  (testing "Sizing keyword applies to both axes"
    (is (= {:width {:type :grow} :height {:type :grow}} (norm/normalize :size :grow)))
    (is (= {:width {:type :fit} :height {:type :fit}} (norm/normalize :size :fit)))))

(deftest sizing-vector-test
  (testing "Sizing vector [width height]"
    (is (= {:width {:type :grow} :height {:type :fit}} (norm/normalize :size [:grow :fit])))
    (is (= {:width {:type :fixed :value 300} :height {:type :fixed :value 400}}
           (norm/normalize :size [300 400])))))

;; ============================================================================
;; PADDING NORMALIZATION TESTS
;; ============================================================================

(deftest padding-uniform-test
  (testing "Uniform padding (single number)"
    (is (= {:top 16 :right 16 :bottom 16 :left 16} (norm/normalize :padding 16)))))

(deftest padding-vertical-horizontal-test
  (testing "Vertical/horizontal padding [v h]"
    (is (= {:top 16 :right 8 :bottom 16 :left 8} (norm/normalize :padding [16 8])))))

(deftest padding-four-sides-test
  (testing "Four sides padding [top right bottom left]"
    (is (= {:top 10 :right 20 :bottom 30 :left 40} (norm/normalize :padding [10 20 30 40])))))

;; ============================================================================
;; RADIUS NORMALIZATION TESTS
;; ============================================================================

(deftest radius-uniform-test
  (testing "Uniform radius (single number)"
    (is (= {:top-left 8 :top-right 8 :bottom-left 8 :bottom-right 8}
           (norm/normalize :radius 8)))))

(deftest radius-four-corners-test
  (testing "Four corners radius [tl tr bl br]"
    (is (= {:top-left 8 :top-right 8 :bottom-left 0 :bottom-right 0}
           (norm/normalize :radius [8 8 0 0])))))

;; ============================================================================
;; ALIGNMENT NORMALIZATION TESTS
;; ============================================================================

(deftest align-keyword-test
  (testing "Alignment keyword shortcuts"
    (is (= {:x :center :y :center} (norm/normalize :align :center)))
    (is (= {:x :left :y :center} (norm/normalize :align :left)))
    (is (= {:x :right :y :center} (norm/normalize :align :right)))
    (is (= {:x :center :y :top} (norm/normalize :align :top)))
    (is (= {:x :center :y :bottom} (norm/normalize :align :bottom)))))

(deftest align-tuple-test
  (testing "Alignment tuple [x y]"
    (is (= {:x :left :y :bottom} (norm/normalize :align [:left :bottom])))
    (is (= {:x :right :y :top} (norm/normalize :align [:right :top])))))

(deftest align-map-test
  (testing "Alignment map form"
    (is (= {:x :left :y :top} (norm/normalize :align {:x :left :y :top})))
    (is (= {:x :center :y :bottom} (norm/normalize :align {:x :center :y :bottom})))))

;; ============================================================================
;; COLOR NORMALIZATION TESTS
;; ============================================================================

(deftest color-keyword-test
  (testing "Color keyword (palette lookup)"
    (let [red-color (norm/normalize :bg :red-500)]
      (is (= 251 (:r red-color)))
      (is (map? red-color)))))

(deftest color-vector-test
  (testing "Color RGB vector"
    (let [rgb-color (norm/normalize :color [255 0 0])]
      (is (= 255 (:r rgb-color)))
      (is (= 0 (:g rgb-color)))
      (is (= 0 (:b rgb-color))))))

;; ============================================================================
;; BORDER NORMALIZATION TESTS
;; ============================================================================

(deftest border-number-test
  (testing "Border width only"
    (is (= {:width 2 :color nil :radius nil} (norm/normalize :border 2)))))

(deftest border-map-test
  (testing "Border map form"
    (let [result (norm/normalize :border {:width 3 :color :blue-500})]
      (is (= 3 (:width result)))
      (is (map? (:color result))))))

(deftest border-tuple2-test
  (testing "Border tuple [color width]"
    (let [result (norm/normalize :border [:red-500 2])]
      (is (= 2 (:width result)))
      (is (map? (:color result))))
    (let [result-rgb (norm/normalize :border [[255 0 0] 3])]
      (is (= 3 (:width result-rgb))))))

(deftest border-tuple3-test
  (testing "Border tuple [color width radius]"
    (let [result (norm/normalize :border [:red-500 2 8])]
      (is (= 2 (:width result)))
      (is (= 8 (:radius result)))
      (is (map? (:color result))))
    (let [result-blue (norm/normalize :border [:blue-300 3 4])]
      (is (= 3 (:width result-blue)))
      (is (= 4 (:radius result-blue))))))

;; ============================================================================
;; FLOATING NORMALIZATION TESTS
;; ============================================================================

(deftest floating-vector-test
  (testing "Floating vector [x y] offset"
    (is (= {:x 100 :y 200} (norm/normalize :floating [100 200])))))

(deftest floating-map-test
  (testing "Floating map form"
    (is (= {:to :parent :at nil :offset [5 10] :z nil}
           (norm/normalize :floating {:to :parent :offset [5 10]})))
    (is (= {:to :none :at [:left-top :right-bottom] :offset [0 0] :z 10}
           (norm/normalize :floating {:at [:left-top :right-bottom] :z 10})))
    (is (= {:to :root :at nil :offset [0 0] :z nil}
           (norm/normalize :floating {:to :root})))))

;; ============================================================================
;; SCROLL NORMALIZATION TESTS
;; ============================================================================

(deftest scroll-boolean-test
  (testing "Scroll boolean shortcut"
    (is (= {:direction :vertical :show-scrollbars true} (norm/normalize :scroll true)))))

(deftest scroll-keyword-test
  (testing "Scroll direction keyword"
    (is (= {:direction :horizontal :show-scrollbars true} (norm/normalize :scroll :horizontal)))))

(deftest scroll-map-test
  (testing "Scroll map form"
    (is (= {:direction :y :show-scrollbars false}
           (norm/normalize :scroll {:direction :y :show-scrollbars false})))
    (is (= {:direction :both :show-scrollbars true}
           (norm/normalize :scroll {:direction :both})))
    (is (= {:direction :vertical :show-scrollbars false}
           (norm/normalize :scroll {:show-scrollbars false})))))

;; ============================================================================
;; WRAP NORMALIZATION TESTS
;; ============================================================================

(deftest wrap-normalization-test
  (testing "Wrap mode normalization"
    (is (= :words (norm/normalize :wrap true)))
    (is (= :none (norm/normalize :wrap false)))
    (is (= :none (norm/normalize :wrap :none)))
    (is (= :words (norm/normalize :wrap :words)))))

;; ============================================================================
;; IMAGE NORMALIZATION TESTS
;; ============================================================================

(deftest image-string-test
  (testing "Image URL string"
    (is (= {:src "photo.jpg" :aspect nil :fit :contain}
           (norm/normalize :image "photo.jpg")))))

(deftest image-keyword-test
  (testing "Image keyword reference"
    (is (= {:src :my-image :aspect nil :fit :contain}
           (norm/normalize :image :my-image)))
    (is (= {:src :logo :aspect nil :fit :contain}
           (norm/normalize :image :logo)))))

(deftest image-function-test
  (testing "Image dynamic function"
    (let [img-fn (fn [_] "computed.png")
          result (norm/normalize :image img-fn)]
      (is (= img-fn (:src result)))
      (is (nil? (:aspect result)))
      (is (= :contain (:fit result))))))

(deftest image-map-test
  (testing "Image map form"
    (is (= {:src "img.png" :aspect 1.5 :fit :contain :position nil}
           (norm/normalize :image {:src "img.png" :aspect 1.5})))
    (is (= {:src :logo :aspect nil :fit :cover :position nil}
           (norm/normalize :image {:src :logo :fit :cover})))
    (is (= {:src "banner.jpg" :aspect 2.0 :fit :fill :position [:center :top]}
           (norm/normalize :image {:src "banner.jpg" :aspect 2.0 :fit :fill :position [:center :top]})))))

;; ============================================================================
;; TEXT NORMALIZATION TESTS
;; ============================================================================

(deftest font-size-normalization-test
  (testing "Font size normalization"
    (is (= 24 (norm/normalize :font-size 24)))
    (is (= 16 (norm/normalize :font-size :md)))
    (is (= 24 (norm/normalize :font-size :xl)))
    (is (= 32 (norm/normalize :font-size :2xl)))))

(deftest text-align-normalization-test
  (testing "Text alignment normalization"
    (is (= :left (norm/normalize :text-align :left)))
    (is (= :center (norm/normalize :text-align :center)))
    (is (= :right (norm/normalize :text-align :right)))))

(deftest text-wrap-normalization-test
  (testing "Text wrap mode normalization"
    (is (= :words (norm/normalize :text-wrap :words)))
    (is (= :newlines (norm/normalize :text-wrap :newlines)))
    (is (= :none (norm/normalize :text-wrap :none)))
    (is (= :words (norm/normalize :text-wrap true)))
    (is (= :none (norm/normalize :text-wrap false)))))

(deftest text-config-normalization-test
  (testing "Text config map normalization"
    (let [result (norm/normalize :text {:size :lg :color :red-500 :align :center})]
      (is (= 20 (:font-size result)))
      (is (= :center (:alignment result)))
      (is (map? (:text-color result))))))

;; ============================================================================
;; FULL PROPS NORMALIZATION TEST
;; ============================================================================

(deftest normalize-props-test
  (testing "Full props map normalization"
    (let [props {:size [:grow :fit]
                 :padding 16
                 :bg :red-500
                 :radius 8
                 :align :center}
          result (norm/normalize-props props)]
      (is (= :grow (get-in result [:size :width :type])))
      (is (= 16 (get-in result [:padding :top])))
      (is (= 8 (get-in result [:radius :top-left])))
      (is (= :center (get-in result [:align :x]))))))
