(ns ui.charts
  "Vega-Lite chart components.
   Charts are pure data (EDN) - the Clojure way."
  (:require ["vega" :as vega]
            ["vega-embed" :default vega-embed]))

;; =============================================================================
;; Vega View Management
;; =============================================================================

(defonce ^:private views (atom {}))

(defn- store-view! [id view]
  (swap! views assoc id view))

(defn- get-view [id]
  (get @views id))

;; =============================================================================
;; Chart Specs (Pure Data)
;; =============================================================================

(defn speed-chart-spec
  "Line chart showing mouse speed over time.
   data: vector of {:t timestamp :v speed}"
  []
  {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
   :width 300  ;; Default, will be overridden with actual container width
   :height 120
   :autosize {:type "fit" :contains "padding"}
   :background "transparent"
   :config {:axis {:labelColor "rgba(255,255,255,0.6)"
                   :titleColor "rgba(255,255,255,0.8)"
                   :gridColor "rgba(255,255,255,0.1)"
                   :domainColor "rgba(255,255,255,0.2)"
                   :labelFont "JetBrains Mono"
                   :labelFontSize 10}
            :view {:stroke "transparent"}}
   :data {:name "speed"
          :values [{:t (js/Date.now) :v 0}]}
   :mark {:type "area"
          :line {:color "#00d9ff" :strokeWidth 2}
          :color {:x1 1 :y1 1 :x2 1 :y2 0
                  :gradient "linear"
                  :stops [{:offset 0 :color "rgba(0,217,255,0)"}
                          {:offset 1 :color "rgba(0,217,255,0.3)"}]}}
   :encoding {:x {:field "t"
                  :type "temporal"
                  :axis {:title nil
                         :format "%H:%M:%S"
                         :labelAngle 0
                         :tickCount 3}}
              :y {:field "v"
                  :type "quantitative"
                  :axis {:title "px/s"
                         :tickCount 3}
                  :scale {:zero true
                          :nice true}}}})

;; =============================================================================
;; Chart Lifecycle
;; =============================================================================

(defn init-speed-chart!
  "Initialize the speed chart. Call once on mount.
   el: DOM element to render into (optional, falls back to getElementById)"
  ([]
   (init-speed-chart! (.getElementById js/document "speed-chart")))
  ([el]
   (when el
     (js/console.log "init-speed-chart! called with el:" el)
     ;; Use requestAnimationFrame to ensure DOM is laid out
     (js/requestAnimationFrame
      (fn []
        (let [width (.-clientWidth el)]
          (js/console.log "RAF fired, width:" width)
          (if (pos? width)
            (-> (vega-embed el
                            (clj->js (assoc (speed-chart-spec) :width width))
                            #js {:actions false})
                (.then (fn [result]
                         (store-view! :speed (.-view result))
                         (js/console.log "Vega chart initialized with width:" width)))
                (.catch (fn [err]
                          (js/console.error "Vega-Lite init error:" err))))
            (js/console.warn "Chart container has no width, skipping init"))))))))

(defn update-speed-chart!
  "Update the speed chart with new data. Call on each render."
  [data]
  (if-let [^js view (get-view :speed)]
    ;; Stream data to existing view
    (let [changeset (-> (vega/changeset)
                        (.remove (fn [_] true))
                        (.insert (clj->js (or data []))))]
      (-> view
          (.change "speed" changeset)
          (.run)))
    ;; View not ready yet, init it (chart shows even when empty)
    (init-speed-chart!)))
