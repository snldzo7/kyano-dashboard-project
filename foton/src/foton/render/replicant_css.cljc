(ns foton.render.replicant-css
  "Replicant + raw CSS render implementations for Foton primitives"
  (:require [foton.core :as core]
            [foton.theme :as theme]))

;; -----------------------------------------------------------------------------
;; CSS Style Helpers
;; -----------------------------------------------------------------------------

(defn- spacing->px [v]
  (cond
    (number? v) (str v "px")
    (keyword? v) (str (theme/resolve-spacing v) "px")
    :else nil))

(defn- color->css [color]
  (cond
    (string? color) color
    (keyword? color) (theme/resolve-color color)
    (vector? color) (theme/resolve-color color)
    :else nil))

(defn- radius->px [v]
  (cond
    (number? v) (str v "px")
    (keyword? v) (str (theme/resolve-radius v) "px")
    :else nil))

;; -----------------------------------------------------------------------------
;; Frame Styles
;; -----------------------------------------------------------------------------

(defn frame-style [{:keys [fill radius padding direction gap align justify width height opacity]}]
  (cond-> {:display "flex"
           :box-sizing "border-box"}
    fill (assoc :background-color (color->css fill))
    radius (assoc :border-radius (radius->px radius))
    padding (assoc :padding (spacing->px padding))
    (= direction :horizontal) (assoc :flex-direction "row")
    (= direction :vertical) (assoc :flex-direction "column")
    gap (assoc :gap (spacing->px gap))
    (= align :start) (assoc :align-items "flex-start")
    (= align :center) (assoc :align-items "center")
    (= align :end) (assoc :align-items "flex-end")
    (= align :stretch) (assoc :align-items "stretch")
    (= justify :start) (assoc :justify-content "flex-start")
    (= justify :center) (assoc :justify-content "center")
    (= justify :end) (assoc :justify-content "flex-end")
    (= justify :space-between) (assoc :justify-content "space-between")
    (= width :fill) (assoc :width "100%")
    (= width :hug) (assoc :width "fit-content")
    (number? width) (assoc :width (str width "px"))
    (= height :fill) (assoc :height "100%")
    (= height :hug) (assoc :height "fit-content")
    (number? height) (assoc :height (str height "px"))
    opacity (assoc :opacity opacity)))

;; -----------------------------------------------------------------------------
;; Text Styles
;; -----------------------------------------------------------------------------

(defn text-style [{:keys [size weight color text-align tracking line-height]}]
  (cond-> {}
    size (assoc :font-size (if (number? size) (str size "px") size))
    weight (assoc :font-weight weight)
    color (assoc :color (color->css color))
    text-align (assoc :text-align (name text-align))
    tracking (assoc :letter-spacing (str tracking "em"))
    line-height (assoc :line-height line-height)))

;; -----------------------------------------------------------------------------
;; Icon Styles
;; -----------------------------------------------------------------------------

(def fa-icons
  {:check "fa-check"
   :x "fa-xmark"
   :arrow-up "fa-arrow-up"
   :arrow-down "fa-arrow-down"
   :arrow-right "fa-arrow-right"
   :arrow-left "fa-arrow-left"
   :dot "fa-circle"
   :warning "fa-triangle-exclamation"
   :info "fa-circle-info"
   :chart "fa-chart-line"
   :plus "fa-plus"
   :minus "fa-minus"
   :edit "fa-pen"
   :trash "fa-trash"
   :close "fa-xmark"
   :menu "fa-bars"
   :home "fa-house"
   :settings "fa-gear"
   :search "fa-magnifying-glass"})

(defn icon-style [{:keys [size color]}]
  (let [px (case size :xs 16 :sm 20 :md 24 :lg 32 :xl 40 24)]
    (cond-> {:width (str px "px")
             :height (str px "px")}
      color (assoc :color (color->css color)))))

;; -----------------------------------------------------------------------------
;; Input Styles
;; -----------------------------------------------------------------------------

(defn input-style [{:keys [size radius disabled]}]
  (let [px (case size :sm 8 :lg 16 12)]
    (cond-> {:background-color "#1e293b"
             :border "1px solid #334155"
             :color "#ffffff"
             :padding (str px "px")
             :border-radius (radius->px (or radius :md))
             :outline "none"}
      disabled (assoc :opacity 0.5 :cursor "not-allowed"))))

;; -----------------------------------------------------------------------------
;; Textarea Styles
;; -----------------------------------------------------------------------------

(defn textarea-style [{:keys [size radius disabled]}]
  (let [px (case size :sm 8 :lg 16 12)]
    (cond-> {:background-color "#1e293b"
             :border "1px solid #334155"
             :color "#ffffff"
             :padding (str px "px")
             :border-radius (radius->px (or radius :md))
             :outline "none"
             :resize "none"}
      disabled (assoc :opacity 0.5 :cursor "not-allowed"))))

;; -----------------------------------------------------------------------------
;; Link Styles
;; -----------------------------------------------------------------------------

(defn link-style [{:keys [color underline]}]
  (cond-> {:color (color->css (or color [:text :primary]))}
    (= underline :none) (assoc :text-decoration "none")
    (not= underline :none) (assoc :text-decoration "underline")))

;; -----------------------------------------------------------------------------
;; Image Styles
;; -----------------------------------------------------------------------------

(defn image-style [{:keys [radius fit width height]}]
  (cond-> {}
    radius (assoc :border-radius (radius->px radius))
    (= fit :cover) (assoc :object-fit "cover")
    (= fit :contain) (assoc :object-fit "contain")
    (= fit :fill) (assoc :object-fit "fill")
    (= width :fill) (assoc :width "100%")
    (number? width) (assoc :width (str width "px"))
    (= height :fill) (assoc :height "100%")
    (number? height) (assoc :height (str height "px"))))

;; -----------------------------------------------------------------------------
;; Video Styles
;; -----------------------------------------------------------------------------

(defn video-style [{:keys [radius width height]}]
  (cond-> {}
    radius (assoc :border-radius (radius->px radius))
    (= width :fill) (assoc :width "100%")
    (number? width) (assoc :width (str width "px"))
    (= height :fill) (assoc :height "100%")
    (number? height) (assoc :height (str height "px"))))

;; -----------------------------------------------------------------------------
;; SVG Styles
;; -----------------------------------------------------------------------------

(defn svg-style [{:keys [size color width height]}]
  (let [px (case size :xs 16 :sm 20 :md 24 :lg 32 :xl 40 nil)]
    (cond-> {}
      px (assoc :width (str px "px") :height (str px "px"))
      color (assoc :color (color->css color))
      (number? width) (assoc :width (str width "px"))
      (number? height) (assoc :height (str height "px")))))

;; -----------------------------------------------------------------------------
;; Attr Helpers - separate Foton attrs from user pass-through attrs
;; -----------------------------------------------------------------------------

(def ^:private frame-keys #{:fill :radius :padding :direction :gap :align :justify :width :height :opacity})
(def ^:private text-keys #{:preset :size :weight :color :text-align :tracking :line-height})
(def ^:private icon-keys #{:name :size :color})
(def ^:private input-keys #{:size :radius :disabled :type :placeholder :value :name})
(def ^:private textarea-keys #{:size :radius :disabled :placeholder :value :name :rows})
(def ^:private link-keys #{:color :underline :href :target})
(def ^:private image-keys #{:radius :fit :width :height :src :alt})
(def ^:private video-keys #{:radius :width :height :src :poster :controls :autoplay :loop :muted})
(def ^:private svg-keys #{:size :color :width :height :viewBox :fill :stroke})

(defn- user-attrs
  "Extract user attrs (on, class, id, etc.) that should pass through to HTML"
  [attrs foton-keys]
  (apply dissoc attrs foton-keys))

;; -----------------------------------------------------------------------------
;; Render Implementations - all pass through user attrs
;; -----------------------------------------------------------------------------

(defmethod core/render [:foton/frame :replicant-css] [_ _ {:keys [attrs children]}]
  (let [attrs (or attrs {})
        style (frame-style (core/expand-frame-attrs attrs))
        pass-through (user-attrs attrs frame-keys)]
    (into [:div (merge pass-through {:style style})]
          children)))

(defmethod core/render [:foton/text :replicant-css] [_ _ {:keys [attrs children]}]
  (let [attrs (or attrs {})
        style (text-style (core/expand-text-attrs attrs))
        pass-through (user-attrs attrs text-keys)]
    (into [:span (merge pass-through {:style style})]
          children)))

(defmethod core/render [:foton/icon :replicant-css] [_ _ {:keys [attrs]}]
  (let [attrs (or attrs {})
        style (icon-style (core/expand-icon-attrs attrs))
        icon-name (:name attrs)
        fa-class (get fa-icons icon-name (str "fa-" (name (or icon-name :circle))))
        pass-through (user-attrs attrs icon-keys)]
    [:i (merge pass-through {:class (str "fa-solid " fa-class) :style style})]))

(defmethod core/render [:foton/input :replicant-css] [_ _ {:keys [attrs]}]
  (let [attrs (or attrs {})
        style (input-style attrs)
        pass-through (user-attrs attrs input-keys)]
    [:input (merge pass-through
                   (cond-> {:type (or (:type attrs) "text") :style style}
                     (:placeholder attrs) (assoc :placeholder (:placeholder attrs))
                     (:value attrs) (assoc :value (:value attrs))
                     (:name attrs) (assoc :name (:name attrs))
                     (:disabled attrs) (assoc :disabled true)))]))

(defmethod core/render [:foton/textarea :replicant-css] [_ _ {:keys [attrs]}]
  (let [attrs (or attrs {})
        style (textarea-style attrs)
        pass-through (user-attrs attrs textarea-keys)]
    [:textarea (merge pass-through
                      (cond-> {:style style}
                        (:placeholder attrs) (assoc :placeholder (:placeholder attrs))
                        (:value attrs) (assoc :value (:value attrs))
                        (:name attrs) (assoc :name (:name attrs))
                        (:rows attrs) (assoc :rows (:rows attrs))
                        (:disabled attrs) (assoc :disabled true)))]))

(defmethod core/render [:foton/link :replicant-css] [_ _ {:keys [attrs children]}]
  (let [attrs (or attrs {})
        style (link-style attrs)
        pass-through (user-attrs attrs link-keys)]
    (into [:a (merge pass-through
                     (cond-> {:style style}
                       (:href attrs) (assoc :href (:href attrs))
                       (:target attrs) (assoc :target (:target attrs))))]
          children)))

(defmethod core/render [:foton/image :replicant-css] [_ _ {:keys [attrs]}]
  (let [attrs (or attrs {})
        style (image-style attrs)
        pass-through (user-attrs attrs image-keys)]
    [:img (merge pass-through
                 (cond-> {:style style}
                   (:src attrs) (assoc :src (:src attrs))
                   (:alt attrs) (assoc :alt (:alt attrs))))]))

(defmethod core/render [:foton/video :replicant-css] [_ _ {:keys [attrs]}]
  (let [attrs (or attrs {})
        style (video-style attrs)
        pass-through (user-attrs attrs video-keys)]
    [:video (merge pass-through
                   (cond-> {:style style}
                     (:src attrs) (assoc :src (:src attrs))
                     (:poster attrs) (assoc :poster (:poster attrs))
                     (:controls attrs) (assoc :controls true)
                     (:autoplay attrs) (assoc :autoplay true)
                     (:loop attrs) (assoc :loop true)
                     (:muted attrs) (assoc :muted true)))]))

(defmethod core/render [:foton/svg :replicant-css] [_ _ {:keys [attrs children]}]
  (let [attrs (or attrs {})
        style (svg-style attrs)
        pass-through (user-attrs attrs svg-keys)]
    (into [:svg (merge pass-through
                       (cond-> {:style style}
                         (:viewBox attrs) (assoc :viewBox (:viewBox attrs))
                         (:fill attrs) (assoc :fill (if (keyword? (:fill attrs)) "currentColor" (:fill attrs)))
                         (:stroke attrs) (assoc :stroke (:stroke attrs))))]
          children)))
