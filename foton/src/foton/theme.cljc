(ns foton.theme
  "Theme - pure Specter transformer for semantic tokens.

   Theme transforms trees, not individual values.
   Flow: hiccup -> (apply-theme tree tokens) -> themed-hiccup -> render

   Usage:
   (apply-theme [:foton.css/frame {:fill :primary}] tokens)
   => [:foton.css/frame {:fill \"#3b82f6\"}]"
  (:require [com.rpl.specter :as sp]))

;; =============================================================================
;; Theme: Dark (default)
;; =============================================================================

;; =============================================================================
;; Design Scale (4px base for concentricity)
;; All dimensions follow this scale for visual harmony
;; =============================================================================

(def scale
  "4px-based scale for concentric design.
   When nesting rounded corners: inner-radius = outer-radius - padding
   This scale ensures all dimensions harmonize."
  {:0 0 :1 4 :2 8 :3 12 :4 16 :5 20 :6 24 :7 28 :8 32 :9 36 :10 40
   :12 48 :14 56 :16 64 :20 80 :24 96 :32 128 :40 160 :48 192})

(def dark
  {:colors {:primary "#3b82f6"
            :secondary "#64748b"
            :background "#0f172a"
            :surface "#1e293b"
            :card "#1e293b"
            :elevated "#334155"
            :muted "#475569"

            ;; Common colors
            :white "#ffffff"
            :black "#000000"
            :transparent "transparent"

            :text {:primary "#ffffff"
                   :secondary "#94a3b8"
                   :muted "#64748b"}

            :status {:good "#10b981"
                     :warning "#f59e0b"
                     :bad "#ef4444"
                     :info "#06b6d4"}

            :border {:default "#334155"
                     :muted "#1e293b"}}

   ;; Modern fonts (Inter as primary, with fallbacks)
   :fonts {:sans "Inter, 'Geist Sans', system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif"
           :serif "'Source Serif Pro', Georgia, 'Times New Roman', Times, serif"
           :mono "'JetBrains Mono', 'Fira Code', ui-monospace, 'SF Mono', Menlo, Consolas, monospace"}

   ;; Spacing follows 4px scale for concentricity
   :spacing {:xs 4 :sm 8 :md 16 :lg 24 :xl 32 :2xl 48 :3xl 64}

   ;; Radius follows same scale - inner = outer - padding
   :radius {:none 0 :xs 4 :sm 8 :md 12 :lg 16 :xl 20 :2xl 24 :3xl 32 :full 9999}

   ;; More apparent shadows with colored ambient + crisp shadow
   :shadows {:none []
             :sm [{:type :drop-shadow :x 0 :y 1 :blur 2 :spread 0 :color "#000000" :opacity 0.4}
                  {:type :drop-shadow :x 0 :y 1 :blur 4 :spread 0 :color "#3b82f6" :opacity 0.1}]
             :md [{:type :drop-shadow :x 0 :y 4 :blur 6 :spread -2 :color "#000000" :opacity 0.5}
                  {:type :drop-shadow :x 0 :y 2 :blur 8 :spread 0 :color "#3b82f6" :opacity 0.15}]
             :lg [{:type :drop-shadow :x 0 :y 8 :blur 16 :spread -4 :color "#000000" :opacity 0.6}
                  {:type :drop-shadow :x 0 :y 4 :blur 12 :spread 0 :color "#3b82f6" :opacity 0.2}]
             :xl [{:type :drop-shadow :x 0 :y 16 :blur 32 :spread -8 :color "#000000" :opacity 0.7}
                  {:type :drop-shadow :x 0 :y 8 :blur 24 :spread 0 :color "#3b82f6" :opacity 0.25}]
             :2xl [{:type :drop-shadow :x 0 :y 24 :blur 48 :spread -12 :color "#000000" :opacity 0.8}
                   {:type :drop-shadow :x 0 :y 12 :blur 32 :spread 0 :color "#3b82f6" :opacity 0.3}]
             :inner [{:type :inner-shadow :x 0 :y 2 :blur 4 :spread 0 :color "#000000" :opacity 0.25}]
             ;; Glow variants for emphasis
             :glow [{:type :drop-shadow :x 0 :y 0 :blur 16 :spread 0 :color "#3b82f6" :opacity 0.5}]
             :glow-lg [{:type :drop-shadow :x 0 :y 0 :blur 32 :spread 0 :color "#3b82f6" :opacity 0.6}]}

   ;; Typography following Apple HIG principles
   :typography {;; Hero text for landing pages
                :hero {:size 64 :weight 700 :line-height 1.1 :tracking -0.03}
                ;; Large title - 34pt equivalent
                :large-title {:size 40 :weight 700 :line-height 1.15 :tracking -0.02}
                ;; Title levels - clear hierarchy
                :title1 {:size 32 :weight 700 :line-height 1.2 :tracking -0.02}
                :title2 {:size 26 :weight 700 :line-height 1.25 :tracking -0.01}
                :title3 {:size 22 :weight 600 :line-height 1.3}
                ;; Headline - emphasized body text
                :headline {:size 18 :weight 600 :line-height 1.4}
                ;; Body - primary reading text (17pt Apple standard)
                :body {:size 17 :weight 400 :line-height 1.6}
                :body-emphasis {:size 17 :weight 600 :line-height 1.6}
                ;; Callout - slightly smaller than body
                :callout {:size 16 :weight 400 :line-height 1.5}
                ;; Subhead - secondary information
                :subhead {:size 15 :weight 400 :line-height 1.45}
                ;; Footnote & captions
                :footnote {:size 13 :weight 400 :line-height 1.4}
                :caption1 {:size 12 :weight 400 :line-height 1.35}
                :caption2 {:size 11 :weight 400 :line-height 1.3}
                ;; Labels for UI elements
                :label {:size 13 :weight 500 :line-height 1.2 :tracking 0.02}
                :label-sm {:size 11 :weight 600 :line-height 1.1 :tracking 0.04 :transform :uppercase}
                ;; Legacy aliases for compatibility
                :display {:size 40 :weight 700 :line-height 1.15 :tracking -0.02}
                :title {:size 32 :weight 700 :line-height 1.2 :tracking -0.02}
                :heading {:size 22 :weight 600 :line-height 1.3}
                :subheading {:size 18 :weight 600 :line-height 1.4}
                :small {:size 13 :weight 400 :line-height 1.4}
                :caption {:size 11 :weight 400 :line-height 1.3}}

   :sizes {:viewport "100vh"
           :viewport-width "100vw"
           :full "100%"
           :half "50%"
           :auto "auto"}

   :easing {:linear "linear"
            :ease "ease"
            :ease-in "cubic-bezier(0.4, 0, 1, 1)"
            :ease-out "cubic-bezier(0, 0, 0.2, 1)"
            :ease-in-out "cubic-bezier(0.4, 0, 0.2, 1)"
            :spring "cubic-bezier(0.34, 1.56, 0.64, 1)"
            :bounce "cubic-bezier(0.68, -0.55, 0.265, 1.55)"}})

;; =============================================================================
;; Theme: Light
;; =============================================================================

(def light
  {:colors {:primary "#2563eb"
            :secondary "#475569"
            :background "#ffffff"
            :surface "#f8fafc"
            :card "#ffffff"
            :elevated "#f1f5f9"
            :muted "#e2e8f0"

            ;; Common colors
            :white "#ffffff"
            :black "#000000"
            :transparent "transparent"

            :text {:primary "#0f172a"
                   :secondary "#475569"
                   :muted "#94a3b8"}

            :status {:good "#059669"
                     :warning "#d97706"
                     :bad "#dc2626"
                     :info "#0891b2"}

            :border {:default "#e2e8f0"
                     :muted "#f1f5f9"}}

   ;; Modern fonts
   :fonts {:sans "Inter, 'Geist Sans', system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif"
           :serif "'Source Serif Pro', Georgia, 'Times New Roman', Times, serif"
           :mono "'JetBrains Mono', 'Fira Code', ui-monospace, 'SF Mono', Menlo, Consolas, monospace"}

   ;; Spacing follows 4px scale
   :spacing {:xs 4 :sm 8 :md 16 :lg 24 :xl 32 :2xl 48 :3xl 64}

   ;; Radius follows same scale
   :radius {:none 0 :xs 4 :sm 8 :md 12 :lg 16 :xl 20 :2xl 24 :3xl 32 :full 9999}

   ;; More apparent shadows with blue tint
   :shadows {:none []
             :sm [{:type :drop-shadow :x 0 :y 1 :blur 2 :spread 0 :color "#000000" :opacity 0.08}
                  {:type :drop-shadow :x 0 :y 1 :blur 3 :spread 0 :color "#2563eb" :opacity 0.04}]
             :md [{:type :drop-shadow :x 0 :y 4 :blur 6 :spread -2 :color "#000000" :opacity 0.12}
                  {:type :drop-shadow :x 0 :y 2 :blur 8 :spread 0 :color "#2563eb" :opacity 0.06}]
             :lg [{:type :drop-shadow :x 0 :y 8 :blur 16 :spread -4 :color "#000000" :opacity 0.15}
                  {:type :drop-shadow :x 0 :y 4 :blur 12 :spread 0 :color "#2563eb" :opacity 0.08}]
             :xl [{:type :drop-shadow :x 0 :y 16 :blur 32 :spread -8 :color "#000000" :opacity 0.18}
                  {:type :drop-shadow :x 0 :y 8 :blur 24 :spread 0 :color "#2563eb" :opacity 0.1}]
             :2xl [{:type :drop-shadow :x 0 :y 24 :blur 48 :spread -12 :color "#000000" :opacity 0.22}
                   {:type :drop-shadow :x 0 :y 12 :blur 32 :spread 0 :color "#2563eb" :opacity 0.12}]
             :inner [{:type :inner-shadow :x 0 :y 2 :blur 4 :spread 0 :color "#000000" :opacity 0.06}]
             :glow [{:type :drop-shadow :x 0 :y 0 :blur 16 :spread 0 :color "#2563eb" :opacity 0.25}]
             :glow-lg [{:type :drop-shadow :x 0 :y 0 :blur 32 :spread 0 :color "#2563eb" :opacity 0.35}]}

   ;; Typography following Apple HIG principles
   :typography {:hero {:size 64 :weight 700 :line-height 1.1 :tracking -0.03}
                :large-title {:size 40 :weight 700 :line-height 1.15 :tracking -0.02}
                :title1 {:size 32 :weight 700 :line-height 1.2 :tracking -0.02}
                :title2 {:size 26 :weight 700 :line-height 1.25 :tracking -0.01}
                :title3 {:size 22 :weight 600 :line-height 1.3}
                :headline {:size 18 :weight 600 :line-height 1.4}
                :body {:size 17 :weight 400 :line-height 1.6}
                :body-emphasis {:size 17 :weight 600 :line-height 1.6}
                :callout {:size 16 :weight 400 :line-height 1.5}
                :subhead {:size 15 :weight 400 :line-height 1.45}
                :footnote {:size 13 :weight 400 :line-height 1.4}
                :caption1 {:size 12 :weight 400 :line-height 1.35}
                :caption2 {:size 11 :weight 400 :line-height 1.3}
                :label {:size 13 :weight 500 :line-height 1.2 :tracking 0.02}
                :label-sm {:size 11 :weight 600 :line-height 1.1 :tracking 0.04 :transform :uppercase}
                :display {:size 40 :weight 700 :line-height 1.15 :tracking -0.02}
                :title {:size 32 :weight 700 :line-height 1.2 :tracking -0.02}
                :heading {:size 22 :weight 600 :line-height 1.3}
                :subheading {:size 18 :weight 600 :line-height 1.4}
                :small {:size 13 :weight 400 :line-height 1.4}
                :caption {:size 11 :weight 400 :line-height 1.3}}

   :sizes {:viewport "100vh"
           :viewport-width "100vw"
           :full "100%"
           :half "50%"
           :auto "auto"}

   :easing {:linear "linear"
            :ease "ease"
            :ease-in "cubic-bezier(0.4, 0, 1, 1)"
            :ease-out "cubic-bezier(0, 0, 0.2, 1)"
            :ease-in-out "cubic-bezier(0.4, 0, 0.2, 1)"
            :spring "cubic-bezier(0.34, 1.56, 0.64, 1)"
            :bounce "cubic-bezier(0.68, -0.55, 0.265, 1.55)"}})

;; =============================================================================
;; Theme: Nord
;; =============================================================================

(def nord
  {:colors {:primary "#88c0d0"
            :secondary "#81a1c1"
            :background "#2e3440"
            :surface "#3b4252"
            :card "#3b4252"
            :elevated "#434c5e"
            :muted "#4c566a"

            ;; Common colors
            :white "#eceff4"
            :black "#2e3440"
            :transparent "transparent"

            :text {:primary "#eceff4"
                   :secondary "#d8dee9"
                   :muted "#a3afc2"}

            :status {:good "#a3be8c"
                     :warning "#ebcb8b"
                     :bad "#bf616a"
                     :info "#88c0d0"}

            :border {:default "#4c566a"
                     :muted "#3b4252"}}

   ;; Modern fonts
   :fonts {:sans "Inter, 'Geist Sans', system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif"
           :serif "'Source Serif Pro', Georgia, 'Times New Roman', Times, serif"
           :mono "'JetBrains Mono', 'Fira Code', ui-monospace, 'SF Mono', Menlo, Consolas, monospace"}

   ;; Spacing follows 4px scale
   :spacing {:xs 4 :sm 8 :md 16 :lg 24 :xl 32 :2xl 48 :3xl 64}

   ;; Radius follows same scale
   :radius {:none 0 :xs 4 :sm 8 :md 12 :lg 16 :xl 20 :2xl 24 :3xl 32 :full 9999}

   ;; Nord-appropriate shadows with frost tint
   :shadows {:none []
             :sm [{:type :drop-shadow :x 0 :y 1 :blur 2 :spread 0 :color "#000000" :opacity 0.25}
                  {:type :drop-shadow :x 0 :y 1 :blur 4 :spread 0 :color "#88c0d0" :opacity 0.08}]
             :md [{:type :drop-shadow :x 0 :y 4 :blur 6 :spread -2 :color "#000000" :opacity 0.35}
                  {:type :drop-shadow :x 0 :y 2 :blur 8 :spread 0 :color "#88c0d0" :opacity 0.12}]
             :lg [{:type :drop-shadow :x 0 :y 8 :blur 16 :spread -4 :color "#000000" :opacity 0.45}
                  {:type :drop-shadow :x 0 :y 4 :blur 12 :spread 0 :color "#88c0d0" :opacity 0.15}]
             :xl [{:type :drop-shadow :x 0 :y 16 :blur 32 :spread -8 :color "#000000" :opacity 0.55}
                  {:type :drop-shadow :x 0 :y 8 :blur 24 :spread 0 :color "#88c0d0" :opacity 0.18}]
             :2xl [{:type :drop-shadow :x 0 :y 24 :blur 48 :spread -12 :color "#000000" :opacity 0.65}
                   {:type :drop-shadow :x 0 :y 12 :blur 32 :spread 0 :color "#88c0d0" :opacity 0.22}]
             :inner [{:type :inner-shadow :x 0 :y 2 :blur 4 :spread 0 :color "#000000" :opacity 0.15}]
             :glow [{:type :drop-shadow :x 0 :y 0 :blur 16 :spread 0 :color "#88c0d0" :opacity 0.4}]
             :glow-lg [{:type :drop-shadow :x 0 :y 0 :blur 32 :spread 0 :color "#88c0d0" :opacity 0.5}]}

   ;; Typography following Apple HIG principles
   :typography {:hero {:size 64 :weight 700 :line-height 1.1 :tracking -0.03}
                :large-title {:size 40 :weight 700 :line-height 1.15 :tracking -0.02}
                :title1 {:size 32 :weight 700 :line-height 1.2 :tracking -0.02}
                :title2 {:size 26 :weight 700 :line-height 1.25 :tracking -0.01}
                :title3 {:size 22 :weight 600 :line-height 1.3}
                :headline {:size 18 :weight 600 :line-height 1.4}
                :body {:size 17 :weight 400 :line-height 1.6}
                :body-emphasis {:size 17 :weight 600 :line-height 1.6}
                :callout {:size 16 :weight 400 :line-height 1.5}
                :subhead {:size 15 :weight 400 :line-height 1.45}
                :footnote {:size 13 :weight 400 :line-height 1.4}
                :caption1 {:size 12 :weight 400 :line-height 1.35}
                :caption2 {:size 11 :weight 400 :line-height 1.3}
                :label {:size 13 :weight 500 :line-height 1.2 :tracking 0.02}
                :label-sm {:size 11 :weight 600 :line-height 1.1 :tracking 0.04 :transform :uppercase}
                :display {:size 40 :weight 700 :line-height 1.15 :tracking -0.02}
                :title {:size 32 :weight 700 :line-height 1.2 :tracking -0.02}
                :heading {:size 22 :weight 600 :line-height 1.3}
                :subheading {:size 18 :weight 600 :line-height 1.4}
                :small {:size 13 :weight 400 :line-height 1.4}
                :caption {:size 11 :weight 400 :line-height 1.3}}

   :sizes {:viewport "100vh"
           :viewport-width "100vw"
           :full "100%"
           :half "50%"
           :auto "auto"}

   :easing {:linear "linear"
            :ease "ease"
            :ease-in "cubic-bezier(0.4, 0, 1, 1)"
            :ease-out "cubic-bezier(0, 0, 0.2, 1)"
            :ease-in-out "cubic-bezier(0.4, 0, 0.2, 1)"
            :spring "cubic-bezier(0.34, 1.56, 0.64, 1)"
            :bounce "cubic-bezier(0.68, -0.55, 0.265, 1.55)"}})

;; =============================================================================
;; Theme Registry
;; =============================================================================

(def themes
  {:dark dark
   :light light
   :nord nord})

(def default-tokens dark)

;; =============================================================================
;; Active Theme State
;; =============================================================================

(defonce ^:private current-theme-id (atom :dark))

(defn get-theme
  "Get theme tokens by id. Returns dark if not found."
  [theme-id]
  (get themes theme-id dark))

(defn set-theme!
  "Set the active theme by id.
   All subsequent token resolution will use this theme.

   (set-theme! :light)"
  [theme-id]
  (reset! current-theme-id theme-id))

(defn current-theme
  "Get the current theme id."
  []
  @current-theme-id)

(defn active-tokens
  "Get currently active theme tokens."
  []
  (get-theme @current-theme-id))

;; =============================================================================
;; Specter Navigators
;; =============================================================================

(def HICCUP-RECURSIVE
  "Navigate all hiccup vectors recursively (depth-first)"
  (sp/recursive-path [] p
    (sp/if-path vector?
      (sp/continue-then-stay sp/ALL p))))

(def HICCUP-ATTRS
  "Navigate to attrs map (second element if it's a map)"
  (sp/if-path [#(and (vector? %) (> (count %) 1) (map? (second %)))]
    (sp/nthpath 1)
    sp/STOP))

;; =============================================================================
;; Token Resolution
;; =============================================================================

(defn- resolve-token
  "Resolve a semantic token to its value."
  [tokens v]
  (cond
    ;; Keyword like :primary -> lookup in colors, spacing, radius
    (keyword? v)
    (or (get-in tokens [:colors v])
        (get-in tokens [:spacing v])
        (get-in tokens [:radius v])
        v)

    ;; Vector path like [:status :good] -> nested lookup
    (and (vector? v) (every? keyword? v))
    (or (get-in tokens v)
        (get-in tokens (into [:colors] v))
        v)

    :else v))

;; =============================================================================
;; Transformers
;; =============================================================================

(defn apply-theme
  "Transform tree by resolving all semantic tokens.

   (apply-theme tree tokens)
   (apply-theme tree) ; uses default-tokens"
  ([tree] (apply-theme tree default-tokens))
  ([tree tokens]
   (sp/transform
     [HICCUP-RECURSIVE HICCUP-ATTRS sp/ALL]
     (fn [[k v]] [k (resolve-token tokens v)])
     tree)))

(defn scale-spacing
  "Scale all spacing values (:gap :padding :margin) by factor."
  [tree factor]
  (sp/transform
    [HICCUP-RECURSIVE HICCUP-ATTRS (sp/submap #{:gap :padding :margin}) sp/MAP-VALS]
    #(if (number? %) (* % factor) %)
    tree))

(defn scale-radii
  "Scale all :radius values by factor."
  [tree factor]
  (sp/transform
    [HICCUP-RECURSIVE HICCUP-ATTRS (sp/must :radius)]
    #(if (number? %) (* % factor) %)
    tree))

(defn transform-colors
  "Apply function to all color values (:fill :color :background :border-color)."
  [tree color-fn]
  (sp/transform
    [HICCUP-RECURSIVE HICCUP-ATTRS (sp/submap #{:fill :color :background :border-color}) sp/MAP-VALS]
    color-fn
    tree))

(defn transform-key
  "Apply function to all values of a specific key."
  [tree k transform-fn]
  (sp/transform
    [HICCUP-RECURSIVE HICCUP-ATTRS (sp/must k)]
    transform-fn
    tree))

;; =============================================================================
;; Resolution Helpers (use *current-tokens* dynamic binding)
;; =============================================================================

(defn resolve-color
  "Resolve color token using active theme tokens."
  [color]
  (let [tokens (active-tokens)]
    (cond
      (string? color) color
      (keyword? color) (get-in tokens [:colors color])
      (vector? color) (or (get-in tokens color)
                          (get-in tokens (into [:colors] color)))
      :else nil)))

(defn resolve-spacing
  "Resolve spacing token using active theme tokens."
  [spacing]
  (if (keyword? spacing)
    (get-in (active-tokens) [:spacing spacing])
    spacing))

(defn resolve-radius
  "Resolve radius token using active theme tokens."
  [radius]
  (if (keyword? radius)
    (get-in (active-tokens) [:radius radius])
    radius))

(defn typography
  "Get typography preset from active theme tokens."
  [preset]
  (get-in (active-tokens) [:typography preset]))

(defn resolve-shadow
  "Resolve shadow preset using active theme tokens.
   Returns a vector of effect maps."
  [shadow]
  (let [tokens (active-tokens)]
    (cond
      (keyword? shadow) (get-in tokens [:shadows shadow])
      (vector? shadow) shadow  ; Already a vector of effects
      (map? shadow) [shadow]   ; Single effect, wrap in vector
      :else nil)))

(defn resolve-size
  "Resolve size token using active theme tokens.
   Supports: :viewport, :viewport-width, :full, :half, :auto"
  [size]
  (if (keyword? size)
    (get-in (active-tokens) [:sizes size])
    size))

(defn resolve-easing
  "Resolve easing token using active theme tokens.
   Supports: :linear, :ease, :ease-in, :ease-out, :ease-in-out, :spring, :bounce"
  [easing]
  (if (keyword? easing)
    (or (get-in (active-tokens) [:easing easing]) "ease-out")
    easing))

(defn resolve-font
  "Resolve font family token using active theme tokens.
   Supports: :sans, :serif, :mono, or custom string"
  [family]
  (if (keyword? family)
    (get-in (active-tokens) [:fonts family])
    family))
