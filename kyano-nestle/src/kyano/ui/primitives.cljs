(ns kyano.ui.primitives
  "Generic primitive UI components - Badge, Button, Icon, Avatar"
  (:require [clojure.string :as str]))

;; -----------------------------------------------------------------------------
;; Utilities
;; -----------------------------------------------------------------------------

(defn classes
  "Join class names, filtering nils"
  [& class-names]
  (->> class-names
       (flatten)
       (filter some?)
       (str/join " ")))

;; -----------------------------------------------------------------------------
;; Badge
;; -----------------------------------------------------------------------------

(def badge-variants
  {:default "bg-slate-700 text-slate-300"
   :primary "bg-blue-500/20 text-blue-400"
   :success "bg-emerald-500/20 text-emerald-400"
   :warning "bg-amber-500/20 text-amber-400"
   :danger  "bg-red-500/20 text-red-400"
   :info    "bg-cyan-500/20 text-cyan-400"})

(def badge-sizes
  {:sm "text-xs px-1.5 py-0.5"
   :md "text-sm px-2 py-0.5"
   :lg "text-base px-2.5 py-1"})

(defn badge
  "Inline badge/tag component

   Props:
   - :label    - Text to display
   - :variant  - :default :primary :success :warning :danger :info
   - :size     - :sm :md :lg
   - :class    - Additional classes"
  [{:keys [label variant size class]
    :or {variant :default size :md}}]
  [:span {:class (classes
                  "inline-flex items-center font-medium rounded-full"
                  (badge-variants variant)
                  (badge-sizes size)
                  class)}
   label])

;; -----------------------------------------------------------------------------
;; Button
;; -----------------------------------------------------------------------------

(def button-variants
  {:default  "bg-slate-700 hover:bg-slate-600 text-white"
   :primary  "bg-blue-600 hover:bg-blue-500 text-white"
   :success  "bg-emerald-600 hover:bg-emerald-500 text-white"
   :danger   "bg-red-600 hover:bg-red-500 text-white"
   :ghost    "bg-transparent hover:bg-slate-700 text-slate-300"
   :outline  "bg-transparent border border-slate-600 hover:bg-slate-700 text-slate-300"})

(def button-sizes
  {:sm "text-sm px-3 py-1.5"
   :md "text-sm px-4 py-2"
   :lg "text-base px-5 py-2.5"})

(defn button
  "Button component - data-driven

   Props:
   - :label    - Button text
   - :variant  - :default :primary :success :danger :ghost :outline
   - :size     - :sm :md :lg
   - :disabled - Boolean
   - :on-click - Action vector to dispatch on click
   - :class    - Additional classes
   - :icon     - Optional icon (hiccup) to prepend"
  [{:keys [label variant size disabled on-click class icon]
    :or {variant :default size :md}}]
  [:button {:class (classes
                    "inline-flex items-center justify-center gap-2 font-medium rounded-lg transition-colors"
                    (button-variants variant)
                    (button-sizes size)
                    (when disabled "opacity-50 cursor-not-allowed")
                    class)
            :disabled disabled
            :on {:click on-click}}
   (when icon icon)
   label])

;; -----------------------------------------------------------------------------
;; Icon (emoji-based for simplicity, can swap for SVG library)
;; -----------------------------------------------------------------------------

(def icons
  {:chart       "📊"
   :package     "📦"
   :factory     "🏭"
   :truck       "🚚"
   :handshake   "🤝"
   :shopping    "🛍️"
   :clipboard   "📋"
   :trending    "📈"
   :briefcase   "💼"
   :warehouse   "🏗️"
   :clock       "🕐"
   :calendar    "📅"
   :warning     "⚠️"
   :check       "✓"
   :x           "✕"
   :arrow-up    "↑"
   :arrow-down  "↓"
   :arrow-right "→"
   :dot         "•"
   :info        "ℹ️"})

(defn icon
  "Icon component

   Props:
   - :name  - Keyword from icons map
   - :size  - :sm :md :lg :xl
   - :class - Additional classes"
  [{:keys [name size class]
    :or {size :md}}]
  (let [size-class (case size
                     :sm "text-sm"
                     :md "text-base"
                     :lg "text-xl"
                     :xl "text-2xl"
                     "text-base")]
    [:span {:class (classes size-class class)}
     (get icons name name)]))

;; -----------------------------------------------------------------------------
;; Avatar
;; -----------------------------------------------------------------------------

(def avatar-sizes
  {:sm "w-6 h-6 text-xs"
   :md "w-8 h-8 text-sm"
   :lg "w-10 h-10 text-base"
   :xl "w-12 h-12 text-lg"})

(defn avatar
  "Avatar component - shows initials or icon

   Props:
   - :name    - Full name (will extract initials)
   - :icon    - Icon keyword (alternative to name)
   - :src     - Image URL (not implemented yet)
   - :size    - :sm :md :lg :xl
   - :variant - :default :primary :success etc.
   - :class   - Additional classes"
  [{:keys [name icon size variant class]
    :or {size :md variant :default}}]
  (let [initials (when name
                   (->> (str/split name #"\s+")
                        (take 2)
                        (map first)
                        (str/join "")))]
    [:div {:class (classes
                   "inline-flex items-center justify-center rounded-full font-medium"
                   (avatar-sizes size)
                   (badge-variants variant)
                   class)}
     (if icon
       (icon {:name icon :size size})
       initials)]))

;; -----------------------------------------------------------------------------
;; Status Dot
;; -----------------------------------------------------------------------------

(def status-colors
  {:good    "bg-emerald-500"
   :warning "bg-amber-500"
   :danger  "bg-red-500"
   :neutral "bg-slate-500"
   :info    "bg-blue-500"})

(defn status-dot
  "Small status indicator dot

   Props:
   - :status - :good :warning :danger :neutral :info
   - :pulse  - Boolean, add pulse animation
   - :size   - :sm :md :lg
   - :class  - Additional classes"
  [{:keys [status pulse size class]
    :or {status :neutral size :md}}]
  (let [size-class (case size
                     :sm "w-1.5 h-1.5"
                     :md "w-2 h-2"
                     :lg "w-3 h-3"
                     "w-2 h-2")]
    [:span {:class (classes
                    "inline-block rounded-full"
                    size-class
                    (status-colors status)
                    (when pulse "animate-pulse")
                    class)}]))

;; -----------------------------------------------------------------------------
;; Divider
;; -----------------------------------------------------------------------------

(defn divider
  "Horizontal divider line

   Props:
   - :class - Additional classes"
  [{:keys [class]}]
  [:hr {:class (classes "border-slate-700/50" class)}])

;; -----------------------------------------------------------------------------
;; Text utilities
;; -----------------------------------------------------------------------------

(defn label
  "Muted label text

   Props:
   - :text  - Label text
   - :class - Additional classes"
  [{:keys [text class]}]
  [:span {:class (classes "text-sm text-slate-400" class)} text])

(defn value
  "Emphasized value text

   Props:
   - :text  - Value text
   - :size  - :sm :md :lg :xl :2xl :3xl
   - :class - Additional classes"
  [{:keys [text size class]
    :or {size :xl}}]
  (let [size-class (case size
                     :sm "text-sm"
                     :md "text-base"
                     :lg "text-lg"
                     :xl "text-xl"
                     :2xl "text-2xl"
                     :3xl "text-3xl"
                     "text-xl")]
    [:span {:class (classes "font-semibold text-white" size-class class)} text]))
