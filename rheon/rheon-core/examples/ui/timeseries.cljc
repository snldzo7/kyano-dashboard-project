(ns ui.timeseries
  "Time-series data structures for real-time charts.
   Ring buffer implementation with speed derivation.")

;; =============================================================================
;; Ring Buffer (fixed-size circular buffer)
;; =============================================================================

(defn ring-buffer
  "Create an empty ring buffer with given capacity."
  [capacity]
  {:capacity capacity
   :items []})

(defn rb-push
  "Push a value to the ring buffer, dropping oldest if at capacity."
  [rb value]
  (let [{:keys [capacity items]} rb
        new-items (conj items value)]
    (assoc rb :items
           (if (> (count new-items) capacity)
             (vec (rest new-items))
             new-items))))

(defn rb-values
  "Get all values in the ring buffer as a vector."
  [rb]
  (:items rb))

;; =============================================================================
;; Speed Derivation
;; =============================================================================

(defn derive-speeds
  "Derive speed time-series from position data.
   Input: [{:t timestamp :x x :y y} ...]
   Output: [{:t timestamp :v speed-in-px-per-sec} ...]"
  [positions]
  (when (> (count positions) 1)
    (mapv (fn [[p1 p2]]
            (let [dt (- (:t p2) (:t p1))
                  dx (- (:x p2) (:x p1))
                  dy (- (:y p2) (:y p1))
                  dist #?(:clj (Math/sqrt (+ (* dx dx) (* dy dy)))
                          :cljs (js/Math.sqrt (+ (* dx dx) (* dy dy))))
                  speed (if (pos? dt)
                          (* 1000 (/ dist dt))  ; px per second
                          0)]
              {:t (:t p2) :v speed}))
          (partition 2 1 positions))))
