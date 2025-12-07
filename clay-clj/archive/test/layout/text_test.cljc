(ns clay.layout.text-test
  "Tests for Clay text measurement and wrapping.

   Tests text handling matching Clay.h behavior:
   - Word splitting (Clay__MeasuredWord)
   - Text measurement (Clay__MeasureTextCached)
   - Text wrapping (word wrap in Clay__CalculateFinalLayout)"
  (:require [clojure.test :refer [deftest is testing]]
            [clay.layout.text :as text]))

;; ============================================================================
;; WORD SPLITTING TESTS
;; ============================================================================

(deftest split-simple-sentence
  (testing "Splits sentence into words and spaces"
    (let [result (text/split-into-words "Hello World")]
      (is (= 3 (count result)) "Should have 3 tokens (word, space, word)")
      (is (= "Hello" (:text (first result))))
      (is (= false (:is-whitespace (first result))))
      (is (= " " (:text (second result))))
      (is (= true (:is-whitespace (second result))))
      (is (= "World" (:text (nth result 2)))))))

(deftest split-with-newlines
  (testing "Newlines are separate tokens"
    (let [result (text/split-into-words "Line1\nLine2")]
      (is (= 3 (count result)))
      (is (= "Line1" (:text (first result))))
      (is (= "\n" (:text (second result))))
      (is (= true (:is-newline (second result))))
      (is (= "Line2" (:text (nth result 2)))))))

(deftest split-multiple-spaces
  (testing "Multiple consecutive spaces are grouped"
    (let [result (text/split-into-words "Hello  World")]
      (is (= 3 (count result)))
      (is (= "  " (:text (second result)))))))

(deftest split-empty-string
  (testing "Empty string returns nil/empty"
    (is (nil? (text/split-into-words "")))
    (is (nil? (text/split-into-words nil)))))

(deftest split-only-whitespace
  (testing "Only whitespace returns whitespace token"
    (let [result (text/split-into-words "   ")]
      (is (= 1 (count result)))
      (is (= true (:is-whitespace (first result)))))))

;; ============================================================================
;; TEXT MEASUREMENT TESTS
;; ============================================================================

(defn mock-measure-fn
  "Simple mock: 8px per character, 16px height"
  [text config]
  {:width (* (count text) 8)
   :height 16})

(deftest measure-words-returns-dimensions
  (testing "measure-words returns unwrapped dimensions"
    (let [result (text/measure-words "Hello World" {:font-size 16} mock-measure-fn)]
      (is (map? result))
      (is (number? (get-in result [:unwrapped-dimensions :width])))
      (is (number? (get-in result [:unwrapped-dimensions :height])))
      ;; "Hello World" = 11 chars * 8 = 88
      (is (= 88 (get-in result [:unwrapped-dimensions :width]))))))

(deftest measure-words-returns-min-width
  (testing "min-width is width of longest word"
    (let [result (text/measure-words "Hi There Everybody" {:font-size 16} mock-measure-fn)]
      ;; "Everybody" = 9 chars * 8 = 72
      (is (= 72 (:min-width result))))))

(deftest measure-words-detects-newlines
  (testing "contains-newlines is set correctly"
    (let [no-newlines (text/measure-words "Hello World" {} mock-measure-fn)
          has-newlines (text/measure-words "Hello\nWorld" {} mock-measure-fn)]
      (is (= false (:contains-newlines no-newlines)))
      (is (= true (:contains-newlines has-newlines))))))

(deftest measure-words-returns-word-list
  (testing "Returns measured word list"
    (let [result (text/measure-words "A B" {:font-size 16} mock-measure-fn)]
      (is (= 3 (count (:words result))))
      (is (= "A" (:text (first (:words result)))))
      (is (= 8 (:width (first (:words result))))))))

;; ============================================================================
;; TEXT WRAPPING TESTS - WORD MODE
;; ============================================================================

(deftest wrap-words-single-line-fits
  (testing "Text that fits returns single line"
    (let [words [{:text "Hello" :width 40 :is-whitespace false :is-newline false}
                 {:text " " :width 8 :is-whitespace true :is-newline false}
                 {:text "World" :width 40 :is-whitespace false :is-newline false}]
          result (text/wrap-text-words words 200 16)]
      (is (= 1 (count result)))
      (is (= "Hello World" (:text (first result)))))))

(deftest wrap-words-breaks-on-overflow
  (testing "Text wraps when exceeding container width"
    (let [words [{:text "Hello" :width 40 :is-whitespace false :is-newline false}
                 {:text " " :width 8 :is-whitespace true :is-newline false}
                 {:text "World" :width 40 :is-whitespace false :is-newline false}]
          result (text/wrap-text-words words 50 16)]  ; Only 50px wide
      (is (= 2 (count result)) "Should wrap to 2 lines")
      (is (= "Hello" (:text (first result))))
      (is (= "World" (:text (second result)))))))

(deftest wrap-words-handles-explicit-newlines
  (testing "Explicit newlines create new lines"
    (let [words [{:text "Line1" :width 40 :is-whitespace false :is-newline false}
                 {:text "\n" :width 0 :is-whitespace true :is-newline true}
                 {:text "Line2" :width 40 :is-whitespace false :is-newline false}]
          result (text/wrap-text-words words 200 16)]
      (is (= 2 (count result)))
      (is (= "Line1" (:text (first result))))
      (is (= "Line2" (:text (second result)))))))

(deftest wrap-words-skips-leading-whitespace
  (testing "Leading whitespace on new line is skipped"
    (let [words [{:text "Word1" :width 45 :is-whitespace false :is-newline false}
                 {:text " " :width 8 :is-whitespace true :is-newline false}
                 {:text "Word2" :width 45 :is-whitespace false :is-newline false}]
          ;; Container is 50px, so Word1 fits, space+Word2 doesn't
          result (text/wrap-text-words words 50 16)]
      (is (= 2 (count result)))
      (is (= "Word1" (:text (first result))))
      ;; Second line should NOT have leading space
      (is (= "Word2" (:text (second result)))))))

(deftest wrap-words-line-height-is-set
  (testing "Each line has correct height"
    (let [words [{:text "Line" :width 40 :is-whitespace false :is-newline false}]
          result (text/wrap-text-words words 200 24)]
      (is (= 24 (:height (first result)))))))

;; ============================================================================
;; TEXT WRAPPING TESTS - NEWLINES MODE
;; ============================================================================

(deftest wrap-newlines-only-on-newlines
  (testing "Newlines mode only breaks on \\n"
    (let [words [{:text "This" :width 32 :is-whitespace false :is-newline false}
                 {:text " " :width 8 :is-whitespace true :is-newline false}
                 {:text "is" :width 16 :is-whitespace false :is-newline false}
                 {:text " " :width 8 :is-whitespace true :is-newline false}
                 {:text "long" :width 32 :is-whitespace false :is-newline false}
                 {:text "\n" :width 0 :is-whitespace true :is-newline true}
                 {:text "Next" :width 32 :is-whitespace false :is-newline false}]
          result (text/wrap-text-newlines words 16)]
      (is (= 2 (count result)))
      (is (= "This is long" (:text (first result))))
      (is (= "Next" (:text (second result)))))))

;; ============================================================================
;; TEXT ALIGNMENT TESTS
;; ============================================================================

(deftest align-text-left
  (testing "Left alignment puts text at x"
    (let [lines [{:text "Hello" :width 40 :height 16}]
          bbox {:x 100 :y 50 :width 200 :height 100}
          result (text/align-text-lines lines bbox :left)]
      (is (= 100 (:x (first result)))))))

(deftest align-text-center
  (testing "Center alignment centers text"
    (let [lines [{:text "Hello" :width 40 :height 16}]
          bbox {:x 100 :y 50 :width 200 :height 100}
          result (text/align-text-lines lines bbox :center)]
      ;; Center: 100 + (200 - 40) / 2 = 100 + 80 = 180
      (is (== 180 (:x (first result)))))))

(deftest align-text-right
  (testing "Right alignment puts text at right edge"
    (let [lines [{:text "Hello" :width 40 :height 16}]
          bbox {:x 100 :y 50 :width 200 :height 100}
          result (text/align-text-lines lines bbox :right)]
      ;; Right: 100 + 200 - 40 = 260
      (is (= 260 (:x (first result)))))))

;; ============================================================================
;; EDGE CASES
;; ============================================================================

(deftest empty-text-returns-empty-lines
  (testing "Empty text returns empty lines"
    (let [result (text/wrap-text-words [] 200 16)]
      (is (empty? result)))))

(deftest single-long-word-not-broken
  (testing "Single word longer than container is not broken"
    (let [words [{:text "Supercalifragilisticexpialidocious" :width 300
                  :is-whitespace false :is-newline false}]
          result (text/wrap-text-words words 100 16)]
      ;; Word doesn't fit but should still be rendered (overflow)
      (is (= 1 (count result)))
      (is (= "Supercalifragilisticexpialidocious" (:text (first result)))))))
