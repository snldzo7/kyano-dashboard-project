(ns foton.theme
  "Theme system for Foton - pure semantic tokens (renderer-agnostic)")

;; -----------------------------------------------------------------------------
;; Default Theme (pure tokens - no renderer-specific data)
;; -----------------------------------------------------------------------------

(def default-theme
  {;; Semantic color tokens
   :colors {:fill {:card "#1e293b"          ; slate-800
                   :elevated "#334155"       ; slate-700
                   :surface "#0f172a"        ; slate-900
                   :muted "#475569"}         ; slate-600

            :text {:primary "#ffffff"
                   :secondary "#94a3b8"      ; slate-400
                   :muted "#64748b"}         ; slate-500

            :status {:good "#10b981"         ; emerald-500
                     :warning "#f59e0b"      ; amber-500
                     :danger "#ef4444"       ; red-500
                     :info "#06b6d4"}        ; cyan-500

            :border {:default "#334155"      ; slate-700
                     :muted "#1e293b"}}      ; slate-800

   ;; Dimension tokens (in pixels)
   :spacing {:xs 4 :sm 8 :md 16 :lg 24 :xl 32 :2xl 48}
   :radius {:none 0 :sm 4 :md 8 :lg 12 :xl 16 :2xl 24 :full 9999}
   :size {:xs 16 :sm 24 :md 32 :lg 48 :xl 64}

   ;; Typography presets
   :typography {:heading {:size 18 :weight 600 :color [:text :primary]}
                :subheading {:size 14 :weight 500 :color [:text :secondary]}
                :body {:size 14 :weight 400 :color [:text :primary]}
                :label {:size 12 :weight 500 :color [:text :secondary]}
                :value {:size 30 :weight 700 :tracking -0.02 :color [:text :primary]}
                :metric {:size 24 :weight 600 :color [:text :primary]}}})

;; -----------------------------------------------------------------------------
;; Theme State
;; -----------------------------------------------------------------------------

(defonce ^:dynamic *theme* (atom default-theme))

(defn set-theme! [theme]
  (reset! *theme* theme))

(defn merge-theme! [overrides]
  (swap! *theme* #(merge-with merge % overrides)))

(defn get-theme []
  @*theme*)

;; -----------------------------------------------------------------------------
;; Resolution Functions (pure - no renderer specifics)
;; -----------------------------------------------------------------------------

(defn resolve-path
  "Resolve a path in the theme. Path can be keyword or vector."
  [theme-section path]
  (let [theme (get-theme)]
    (cond
      (keyword? path)
      (get-in theme [theme-section path])

      (vector? path)
      (get-in theme (into [theme-section] path))

      :else
      path)))

(defn resolve-color
  "Resolve color from theme. Returns hex string."
  [color]
  (cond
    (string? color) color  ; Already hex
    (keyword? color) (resolve-path :colors color)
    (vector? color) (resolve-path :colors color)
    :else nil))

(defn resolve-spacing
  "Resolve spacing dimension from theme. Returns pixels."
  [dim]
  (cond
    (number? dim) dim
    (keyword? dim) (resolve-path :spacing dim)
    :else nil))

(defn resolve-radius
  "Resolve border radius from theme. Returns pixels."
  [radius]
  (cond
    (number? radius) radius
    (keyword? radius) (resolve-path :radius radius)
    :else nil))

(defn typography
  "Resolve typography preset from theme. Returns map of attrs."
  [preset]
  (when (keyword? preset)
    (get-in (get-theme) [:typography preset])))
