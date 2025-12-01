(ns kyano.ui.layout
  "Generic layout components - Container, Grid, Flex, Stack, Section"
  (:require [kyano.ui.primitives :refer [classes]]))

;; -----------------------------------------------------------------------------
;; Container
;; -----------------------------------------------------------------------------

(defn container
  "Centered container with max-width

   Props:
   - :size     - :sm :md :lg :xl :2xl :full
   - :padding  - Boolean, add horizontal padding (default true)
   - :class    - Additional classes
   - :children - Content"
  [{:keys [size padding class children]
    :or {size :xl padding true}}]
  (let [max-width (case size
                    :sm "max-w-screen-sm"
                    :md "max-w-screen-md"
                    :lg "max-w-screen-lg"
                    :xl "max-w-screen-xl"
                    :2xl "max-w-screen-2xl"
                    :full "max-w-full"
                    "max-w-screen-xl")]
    [:div {:class (classes
                   "mx-auto w-full"
                   max-width
                   (when padding "px-4 sm:px-6 lg:px-8")
                   class)}
     children]))

;; -----------------------------------------------------------------------------
;; Grid
;; -----------------------------------------------------------------------------

(defn grid
  "CSS Grid layout

   Props:
   - :cols     - Number of columns (1-12) or responsive map {:sm 1 :md 2 :lg 3}
   - :gap      - Gap size (1-8)
   - :class    - Additional classes
   - :children - Grid items"
  [{:keys [cols gap class children]
    :or {cols 1 gap 4}}]
  (let [cols-class (cond
                     (number? cols)
                     (str "grid-cols-" cols)

                     (map? cols)
                     (classes
                      (when (:default cols) (str "grid-cols-" (:default cols)))
                      (when (:sm cols) (str "sm:grid-cols-" (:sm cols)))
                      (when (:md cols) (str "md:grid-cols-" (:md cols)))
                      (when (:lg cols) (str "lg:grid-cols-" (:lg cols)))
                      (when (:xl cols) (str "xl:grid-cols-" (:xl cols))))

                     :else "grid-cols-1")
        gap-class (str "gap-" gap)]
    [:div {:class (classes "grid" cols-class gap-class class)}
     children]))

;; -----------------------------------------------------------------------------
;; Flex
;; -----------------------------------------------------------------------------

(defn flex
  "Flexbox layout

   Props:
   - :direction - :row :col :row-reverse :col-reverse
   - :justify   - :start :end :center :between :around :evenly
   - :align     - :start :end :center :baseline :stretch
   - :wrap      - Boolean
   - :gap       - Gap size (1-8)
   - :class     - Additional classes
   - :children  - Flex items"
  [{:keys [direction justify align wrap gap class children]
    :or {direction :row}}]
  (let [dir-class (case direction
                    :row "flex-row"
                    :col "flex-col"
                    :row-reverse "flex-row-reverse"
                    :col-reverse "flex-col-reverse"
                    "flex-row")
        justify-class (when justify
                        (case justify
                          :start "justify-start"
                          :end "justify-end"
                          :center "justify-center"
                          :between "justify-between"
                          :around "justify-around"
                          :evenly "justify-evenly"
                          nil))
        align-class (when align
                      (case align
                        :start "items-start"
                        :end "items-end"
                        :center "items-center"
                        :baseline "items-baseline"
                        :stretch "items-stretch"
                        nil))
        gap-class (when gap (str "gap-" gap))]
    [:div {:class (classes
                   "flex"
                   dir-class
                   justify-class
                   align-class
                   (when wrap "flex-wrap")
                   gap-class
                   class)}
     children]))

;; -----------------------------------------------------------------------------
;; Stack (vertical flex shorthand)
;; -----------------------------------------------------------------------------

(defn stack
  "Vertical stack (flex-col shorthand)

   Props:
   - :gap      - Gap size (1-8)
   - :align    - :start :end :center :stretch
   - :class    - Additional classes
   - :children - Stack items"
  [{:keys [gap align class children]
    :or {gap 4}}]
  [flex {:direction :col
         :gap gap
         :align align
         :class class
         :children children}])

;; -----------------------------------------------------------------------------
;; HStack (horizontal flex shorthand)
;; -----------------------------------------------------------------------------

(defn hstack
  "Horizontal stack (flex-row shorthand)

   Props:
   - :gap      - Gap size (1-8)
   - :align    - :start :end :center :baseline :stretch
   - :justify  - :start :end :center :between :around :evenly
   - :class    - Additional classes
   - :children - Stack items"
  [{:keys [gap align justify class children]
    :or {gap 4 align :center}}]
  [flex {:direction :row
         :gap gap
         :align align
         :justify justify
         :class class
         :children children}])

;; -----------------------------------------------------------------------------
;; Section
;; -----------------------------------------------------------------------------

(defn section
  "Page section with optional title

   Props:
   - :title    - Section title
   - :subtitle - Optional subtitle
   - :actions  - Optional action buttons (hiccup)
   - :class    - Additional classes
   - :children - Section content"
  [{:keys [title subtitle actions class children]}]
  [:section {:class (classes "py-6" class)}
   (when (or title subtitle actions)
     [flex {:justify :between :align :center :class "mb-4"}
      [:div
       (when title [:h2 {:class "text-lg font-semibold text-white"} title])
       (when subtitle [:p {:class "text-sm text-slate-400 mt-0.5"} subtitle])]
      (when actions [:div actions])])
   children])

;; -----------------------------------------------------------------------------
;; Spacer
;; -----------------------------------------------------------------------------

(defn spacer
  "Flexible spacer for flex layouts

   Props:
   - :size  - Fixed size in tailwind units (optional)
   - :class - Additional classes"
  [{:keys [size class]}]
  (if size
    [:div {:class (classes (str "w-" size " h-" size) class)}]
    [:div {:class (classes "flex-1" class)}]))

;; -----------------------------------------------------------------------------
;; Sidebar Layout
;; -----------------------------------------------------------------------------

(defn sidebar-layout
  "Layout with sidebar and main content

   Props:
   - :sidebar       - Sidebar content (hiccup)
   - :sidebar-width - :sm :md :lg (default :md)
   - :children      - Main content"
  [{:keys [sidebar sidebar-width children]
    :or {sidebar-width :md}}]
  (let [width-class (case sidebar-width
                      :sm "w-48"
                      :md "w-64"
                      :lg "w-80"
                      "w-64")]
    [:div {:class "flex min-h-screen"}
     [:aside {:class (classes
                      "fixed left-0 top-0 h-screen"
                      width-class
                      "bg-slate-900 border-r border-slate-700/50")}
      sidebar]
     [:main {:class (classes "flex-1" (str "ml-" (case sidebar-width :sm "48" :md "64" :lg "80" "64")))}
      children]]))
