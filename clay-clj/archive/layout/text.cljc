(ns clay.layout.text
  "Text measurement and wrapping - port of Clay.h text handling.

   Key functions:
   - measure-text: Get dimensions of text string
   - split-into-words: Break text into measurable word units
   - wrap-text: Wrap text to fit container width
   - wrap-all-text: Apply wrapping to all text elements

   Text wrapping modes:
   - :words - Break on word boundaries (default)
   - :newlines - Only break on explicit newlines
   - :none - No wrapping, single line"
  (:require [clojure.string :as str]))

;; ============================================================================
;; HELPERS
;; ============================================================================

(defn- get-element [state idx]
  (get-in state [:layout-elements idx]))

(defn- update-element [state idx f & args]
  (apply update-in state [:layout-elements idx] f args))

;; ============================================================================
;; TEXT SPLITTING
;; ============================================================================

(defn split-into-words
  "Split text into words and whitespace for measurement.

   Returns vector of {:text string :is-whitespace boolean}

   Handles:
   - Spaces between words
   - Newlines (explicit line breaks)
   - Consecutive whitespace"
  [text]
  (when (and text (pos? (count text)))
    (loop [remaining text
           result []
           current ""
           in-whitespace false]
      (if (empty? remaining)
        ;; Finish up
        (if (pos? (count current))
          (conj result {:text current
                        :is-whitespace in-whitespace
                        :is-newline false})
          result)
        (let [ch (first remaining)
              is-space (or (= ch \space) (= ch \tab))
              is-newline (= ch \newline)]
          (cond
            ;; Newline is always a separate token
            is-newline
            (let [result' (if (pos? (count current))
                            (conj result {:text current
                                          :is-whitespace in-whitespace
                                          :is-newline false})
                            result)]
              (recur (rest remaining)
                     (conj result' {:text "\n"
                                    :is-whitespace true
                                    :is-newline true})
                     ""
                     false))

            ;; Switching between word and whitespace
            (and (not is-newline)
                 (not= is-space in-whitespace)
                 (pos? (count current)))
            (recur remaining
                   (conj result {:text current
                                 :is-whitespace in-whitespace
                                 :is-newline false})
                   ""
                   is-space)

            ;; Continue current token
            :else
            (recur (rest remaining)
                   result
                   (str current ch)
                   (or in-whitespace is-space))))))))

;; ============================================================================
;; TEXT MEASUREMENT
;; ============================================================================

(defn measure-words
  "Measure each word in the text and return measurement data.

   Parameters:
   - text: String to measure
   - text-config: {:font-id :font-size :letter-spacing}
   - measure-fn: Function (text config) -> {:width :height}

   Returns:
   {:unwrapped-dimensions {:width :height}
    :min-width number  ; Width of longest single word
    :words [{:text :width :is-whitespace :is-newline} ...]
    :contains-newlines boolean}"
  [text text-config measure-fn]
  (if (or (nil? text) (empty? text))
    {:unwrapped-dimensions {:width 0 :height 0}
     :min-width 0
     :words []
     :contains-newlines false}
    (let [tokens (split-into-words text)
          ;; Measure each token
          measured-words (mapv
                          (fn [{:keys [text is-whitespace is-newline]}]
                            (let [dims (measure-fn text text-config)]
                              {:text text
                               :width (:width dims)
                               :height (:height dims)
                               :is-whitespace is-whitespace
                               :is-newline is-newline}))
                          tokens)
          ;; Calculate total unwrapped width
          total-width (reduce + 0 (map :width measured-words))
          ;; Get height from first measurement (should all be same for same font)
          height (or (:height (first measured-words)) 0)
          ;; Find longest non-whitespace word for min-width
          min-width (reduce
                     (fn [max-w word]
                       (if (:is-whitespace word)
                         max-w
                         (max max-w (:width word))))
                     0
                     measured-words)
          contains-newlines (some :is-newline measured-words)]
      {:unwrapped-dimensions {:width total-width :height height}
       :min-width min-width
       :words measured-words
       :contains-newlines (boolean contains-newlines)})))

;; ============================================================================
;; TEXT WRAPPING
;; ============================================================================

(defn wrap-text-words
  "Wrap text by words to fit within container width.

   Parameters:
   - measured-words: Vector of measured word maps
   - container-width: Available width for text
   - line-height: Height per line

   Returns vector of wrapped lines:
   [{:text string :width number :height number} ...]"
  [measured-words container-width line-height]
  (if (empty? measured-words)
    []
    (loop [words measured-words
           lines []
           current-line {:text "" :width 0 :height line-height}]
      (if (empty? words)
        ;; Finish up - add final line if non-empty
        (if (pos? (count (:text current-line)))
          (conj lines current-line)
          lines)
        (let [{:keys [text width is-whitespace is-newline]} (first words)]
          (cond
            ;; Explicit newline - start new line
            is-newline
            (recur (rest words)
                   (conj lines current-line)
                   {:text "" :width 0 :height line-height})

            ;; Would overflow - wrap to new line (unless first word)
            (and (> (+ (:width current-line) width) container-width)
                 (pos? (count (:text current-line))))
            ;; Trim trailing whitespace from line before wrapping
            (let [trimmed-text (str/trimr (:text current-line))
                  ;; Recalculate width based on trimmed content
                  trimmed-line (assoc current-line :text trimmed-text)]
              (recur words  ; Don't consume word yet
                     (conj lines trimmed-line)
                     {:text "" :width 0 :height line-height}))

            ;; Skip leading whitespace on new line
            (and is-whitespace
                 (zero? (count (:text current-line))))
            (recur (rest words) lines current-line)

            ;; Add word to current line
            :else
            (recur (rest words)
                   lines
                   {:text (str (:text current-line) text)
                    :width (+ (:width current-line) width)
                    :height line-height})))))))

(defn wrap-text-newlines
  "Wrap text only on explicit newlines.

   Parameters:
   - measured-words: Vector of measured word maps
   - line-height: Height per line

   Returns vector of lines."
  [measured-words line-height]
  (if (empty? measured-words)
    []
    (loop [words measured-words
           lines []
           current-line {:text "" :width 0 :height line-height}]
      (if (empty? words)
        (if (pos? (count (:text current-line)))
          (conj lines current-line)
          lines)
        (let [{:keys [text width is-newline]} (first words)]
          (if is-newline
            (recur (rest words)
                   (conj lines current-line)
                   {:text "" :width 0 :height line-height})
            (recur (rest words)
                   lines
                   {:text (str (:text current-line) text)
                    :width (+ (:width current-line) width)
                    :height line-height})))))))

(defn wrap-text
  "Wrap text according to wrap mode.

   Parameters:
   - element: Text element with :measured-words and :text-config
   - container-width: Available width
   - wrap-mode: :words, :newlines, or :none

   Returns vector of wrapped lines."
  [element container-width]
  (let [{:keys [measured-words text-config]} element
        wrap-mode (get text-config :wrap-mode :words)
        line-height (or (:line-height text-config)
                        (:font-size text-config 16))]
    (case wrap-mode
      :words (wrap-text-words measured-words container-width line-height)
      :newlines (wrap-text-newlines measured-words line-height)
      :none [{:text (:text-content element)
              :width (get-in element [:dimensions :width])
              :height line-height}]
      ;; Default to :words wrapping for nil or unknown modes
      (wrap-text-words measured-words container-width line-height))))

;; ============================================================================
;; LAYOUT INTEGRATION
;; ============================================================================

(defn wrap-all-text
  "Apply text wrapping to all text elements in the layout.

   This is called after X-axis sizing, when container widths are known.

   Parameters:
   - state: Layout state
   - measure-fn: Optional function to re-measure wrapped lines

   Returns updated state with:
   - :wrapped-lines set on text elements
   - :dimensions :height updated based on line count"
  [state measure-fn]
  (reduce
   (fn [state idx]
     (let [element (get-element state idx)]
       (if (= :text (:type element))
         (let [;; Find parent to get container width
               parent-idx (first (keep-indexed
                                  (fn [pidx parent]
                                    (when (and (= :container (:type parent))
                                               (some #{idx} (:children parent)))
                                      pidx))
                                  (:layout-elements state)))
               parent (when parent-idx (get-element state parent-idx))
               padding (get-in parent [:layout :padding] {:left 0 :right 0})
               container-width (if parent
                                 (- (get-in parent [:dimensions :width])
                                    (:left padding 0)
                                    (:right padding 0))
                                 (get-in state [:viewport :width]))

               ;; Wrap text
               wrapped-lines (wrap-text element container-width)
               line-count (count wrapped-lines)
               line-height (or (get-in element [:text-config :line-height])
                               (get-in element [:text-config :font-size] 16))
               new-height (* line-count line-height)]
           (-> state
               (update-element idx assoc :wrapped-lines wrapped-lines)
               (update-element idx assoc-in [:dimensions :height] new-height)))
         state)))
   state
   (range (count (:layout-elements state)))))

;; ============================================================================
;; TEXT ALIGNMENT
;; ============================================================================

(defn align-text-lines
  "Align wrapped text lines within bounding box.

   Parameters:
   - wrapped-lines: Vector of {:text :width :height}
   - bounding-box: {:x :y :width :height}
   - alignment: :left, :center, or :right

   Returns wrapped-lines with :x offset added to each line."
  [wrapped-lines bounding-box alignment]
  (let [{:keys [x width]} bounding-box]
    (mapv
     (fn [line]
       (let [line-x (case alignment
                      :left x
                      :center (+ x (/ (- width (:width line)) 2))
                      :right (- (+ x width) (:width line))
                      x)]
         (assoc line :x line-x)))
     wrapped-lines)))
