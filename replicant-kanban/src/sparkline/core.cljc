(ns sparkline.core
  (:require [clojure.string :as str]
            [replicant.alias :refer [defalias]]))

;; We only support plain keys in the public API (no ::keys, no :data-*).
;; Internally, we *do* use namespaced keys to stash normalized data; Replicant ignores those as DOM attrs.

;; --------- small utils ---------

(defn- kebab-name [kw]
  (-> (name kw)
      (str/replace "_" "-")))

(defn- as-string [x]
  (cond
    (string? x) x
    (keyword? x) (name x)
    :else (str x)))

(defn- parse-type [t]
  (cond
    (keyword? t) t
    (string? t)  (keyword (str/lower-case t))
    :else        :bar))

(defn- pct [part total]
  (if (pos? total)
    (.toFixed (* 100 (/ part total)) 2)
    "0.00"))

(defn- normalize-color
  "Color can be:
   - keyword => resolved via palette or kebab keyword-name (e.g. :light-gray -> \"light-gray\")
   - string  => used as-is (supports hex, rgb/rgba, hsl, etc.)"
  [palette c]
  (cond
    (string? c) c
    (keyword? c) (or (get palette c)
                     (get palette (keyword (kebab-name c)))
                     (kebab-name c))
    :else (str c)))

(defn- normalize-labels [labels]
  (->> (or labels [])
       (map as-string)
       vec))

(defn- title-for
  "Supports tokens: %label %point %percent %idx"
  [{:keys [labels format total]} idx point]
  (let [label (or (nth labels idx nil) "")
        s (or format "%point")]
    (-> s
        (str/replace "%label" (str label))
        (str/replace "%point" (str point))
        (str/replace "%idx" (str idx))
        (str/replace "%percent" (str (pct point total) "%")))))

(defn- normalize-opts
  [{:keys [type points width height size gap stroke-width colors labels format palette]
    :or {type :bar
         width 100
         height 30
         gap 5
         stroke-width 2
         colors [:gray]
         labels []
         palette {}}}]
  (let [[w h] (if (and (vector? size) (= 2 (count size))) size [width height])
        pts   (vec (or points []))
        cols  (->> (if (sequential? colors) colors [colors])
                   (map #(normalize-color palette %))
                   (remove nil?)
                   vec)
        total (reduce + 0 pts)]
    {:type (parse-type type)
     :points pts
     :width w
     :height h
     :gap gap
     :stroke-width stroke-width
     :colors (if (seq cols) cols ["gray"])
     :labels (normalize-labels labels)
     :format format
     :total total
     :maxv (apply max 0 pts)}))

(defn- svg-attrs [attrs {:keys [width height]}]
  (-> attrs
      (dissoc :type :points :width :height :size :gap :stroke-width :colors :labels :format :palette)
      (assoc :width width
             :height height
             :viewBox (str "0 0 " width " " height))))

;; --------- renderers (functions, not aliases!) ---------
;; These return hiccup data that gets inlined into the SVG.
;; Using functions instead of aliases preserves SVG namespace for children.

(defn- render-bars [opts]
  (let [{:keys [points width height gap colors maxv labels format total]} opts
        n (count points)]
    (if (pos? n)
      (let [column-w (+ (/ width n) (/ gap n) (- gap))]
        (into [:g]
              (for [[idx point] (map-indexed vector points)]
                (let [rect-h (if (pos? maxv) (* (/ point maxv) height) 0)
                      x (+ (* idx column-w) (* idx gap))
                      y (- height rect-h)
                      color (nth colors (mod idx (count colors)))]
                  [:rect {:replicant/key idx
                          :x x :y y :width column-w :height rect-h
                          :style {:fill color}}
                   [:title (title-for {:labels labels :format format :total total} idx point)]]))))
      [:g])))

(defn- render-line [opts]
  (let [{:keys [points width height stroke-width colors maxv labels format total]} opts
        n (count points)]
    (if (pos? n)
      (let [spacing (if (> n 1) (/ width (dec n)) 0)
            coords (->> (map-indexed
                         (fn [idx point]
                           (let [h (if (pos? maxv) (* (/ point maxv) height) 0)
                                 x (* idx spacing)
                                 y (- height h)]
                             (str x "," y)))
                         points)
                        (str/join " "))]
        (into [:g
               [:polyline {:points coords
                           :fill "none"
                           :stroke (first colors)
                           :stroke-width stroke-width}]]
              ;; invisible hover targets to show tooltips per point
              (for [[idx point] (map-indexed vector points)]
                (let [h (if (pos? maxv) (* (/ point maxv) height) 0)
                      x (* idx spacing)
                      y (- height h)]
                  [:circle {:replicant/key idx
                            :cx x :cy y :r 8
                            :fill "transparent"}
                   [:title (title-for {:labels labels :format format :total total} idx point)]]))))
      [:g])))

(defn- render-pie [opts]
  (let [{:keys [points width height colors labels format total]} opts
        r (/ (min width height) 2)
        cx (/ width 2)
        cy (/ height 2)
        pi #?(:cljs js/Math.PI :clj Math/PI)]
    (into [:g]
          (when (pos? total)
            (loop [pts points, idx 0, start 0, out []]
              (if-let [p (first pts)]
                (let [slice (* (/ p total) 2 pi)
                      end (+ start slice)
                      cos-fn #?(:cljs js/Math.cos :clj #(Math/cos %))
                      sin-fn #?(:cljs js/Math.sin :clj #(Math/sin %))
                      x1 (+ cx (* r (cos-fn start)))
                      y1 (+ cy (* r (sin-fn start)))
                      x2 (+ cx (* r (cos-fn end)))
                      y2 (+ cy (* r (sin-fn end)))
                      large-arc (if (> slice pi) 1 0)
                      d (str "M " cx "," cy
                             " L " x1 "," y1
                             " A " r "," r " 0 " large-arc " 1 " x2 "," y2
                             " Z")
                      color (nth colors (mod idx (count colors)))]
                  (recur (rest pts) (inc idx) end
                         (conj out
                               [:path {:replicant/key idx :d d :style {:fill color}}
                                [:title (title-for {:labels labels :format format :total total} idx p)]])))
                out))))))

(defn- render-stacked [opts]
  (let [{:keys [points width height gap colors labels format total]} opts
        available (- width (* (dec (count points)) gap))]
    (into [:g]
          (when (pos? total)
            (loop [pts points, idx 0, x 0, out []]
              (if-let [p (first pts)]
                (let [w (* (/ p total) available)
                      color (nth colors (mod idx (count colors)))]
                  (recur (rest pts) (inc idx) (+ x w gap)
                         (conj out
                               [:rect {:replicant/key idx
                                       :x x :y 0 :width w :height height
                                       :style {:fill color}}
                                [:title (title-for {:labels labels :format format :total total} idx p)]])))
                out))))))

;; --------- public alias ---------

(defalias sparkline
  "Sparkline component. Returns an SVG with bars, line, pie, or stacked chart.
   Renderer content is inlined (not via child aliases) to preserve SVG namespace."
  [attrs _children]
  (let [{:keys [type] :as opts} (normalize-opts attrs)]
    [:svg (svg-attrs attrs opts)
     (case type
       :line    (render-line opts)
       :pie     (render-pie opts)
       :stacked (render-stacked opts)
       ;; default: bars
       (render-bars opts))]))
