(ns clay.hiccup2
  "Hiccup DSL → Nested Tree transformation.

   Clean, simple DSL that produces nested tree structures for layout.
   No begin/end ceremony - just data in, data out.

   Example:
   (def tree
     (parse viewport
       [:col {:padding 16 :gap 8 :bg :slate-100}
         [:text \"Hello\" {:font-size 32 :color :slate-900}]
         [:row {:gap 8}
           [:box {:size 100 :bg :red-500 :radius 8}]
           [:box {:size :grow :bg :blue-500}]]]
       measure-fn))

   (layout/layout tree)  ; => render commands

   Supported tags:
   - :col      Vertical container (top-to-bottom)
   - :row      Horizontal container (left-to-right)
   - :box      Generic container
   - :text     Text element
   - :spacer   Flexible space
   - :scroll   Scrollable container"
  (:require [clay.tree :as t]
            [clay.layout :as layout]
            [clay.dsl.normalize :as normalize]))

;; ============================================================================
;; CUSTOM COMPONENT REGISTRY
;; ============================================================================

(defonce components (atom {}))

(defn register-component!
  "Register a custom component tag.

   component-fn receives (props & children) and returns hiccup.

   Example:
   (register-component! :card
     (fn [props & children]
       [:box (merge {:bg :white :radius 8 :padding 16} props)
         children]))"
  [tag component-fn]
  (swap! components assoc tag component-fn))

(defn unregister-component! [tag]
  (swap! components dissoc tag))

;; ============================================================================
;; ID GENERATION
;; ============================================================================

(def ^:private id-counter (atom 0))

(defn- next-id []
  (swap! id-counter inc))

(defn reset-ids! []
  (reset! id-counter 0))

;; ============================================================================
;; PROPS NORMALIZATION
;; ============================================================================

(defn- normalize-layout-config
  "Convert DSL props to layout config."
  [props direction]
  (let [normalized (normalize/normalize-props props)]
    {:sizing (or (:size normalized)
                 {:width (or (:width normalized) {:type :fit})
                  :height (or (:height normalized) {:type :fit})})
     :padding (or (:padding normalized) (:pad normalized)
                  {:top 0 :right 0 :bottom 0 :left 0})
     :child-gap (or (:gap normalized) (:child-gap normalized) 0)
     :layout-direction (or direction
                           (case (:direction props)
                             :horizontal :left-to-right
                             :vertical :top-to-bottom
                             :row :left-to-right
                             :col :top-to-bottom
                             :left-to-right))
     :child-alignment (or (:align normalized)
                          {:x :left :y :top})}))

(defn- extract-configs
  "Extract non-layout configs (background, border, etc.) from props."
  [props]
  (let [normalized (normalize/normalize-props props)]
    (cond-> {}
      ;; Background
      (or (:bg normalized) (:background normalized))
      (assoc :background {:color (or (:bg normalized) (:background normalized))
                          :corner-radius (or (:radius normalized) (:corner-radius normalized)
                                             {:top-left 0 :top-right 0
                                              :bottom-left 0 :bottom-right 0})})

      ;; Border
      (:border normalized)
      (assoc :border (:border normalized))

      ;; Clip/Scroll
      (:scroll normalized)
      (assoc :clip {:vertical (= (:direction (:scroll normalized)) :vertical)
                    :horizontal (= (:direction (:scroll normalized)) :horizontal)})

      ;; Floating elements (already normalized by normalize/normalize-props)
      (:floating normalized)
      (assoc :floating (:floating normalized))

      ;; Aspect ratio (already normalized by normalize/normalize-props)
      (:aspect-ratio normalized)
      (assoc :aspect-ratio (:aspect-ratio normalized))

      ;; Image
      (:src props)
      (assoc :image {:src (:src props)
                     :fit (or (:fit props) :cover)}))))

(defn- normalize-text-config
  "Convert DSL text props to text config."
  [props]
  (let [normalized (normalize/normalize-props props)]
    {:font-id (or (:font-id normalized) (:font normalized) 0)
     :font-size (or (:font-size normalized) 16)
     :text-color (or (:text-color normalized) (:color normalized)
                     {:r 0 :g 0 :b 0 :a 255})
     :letter-spacing (:letter-spacing normalized)
     :line-height (:line-height normalized)
     :wrap-mode (or (:text-wrap normalized) (:wrap normalized) :words)}))

;; ============================================================================
;; ARGUMENT PARSING
;; ============================================================================

(defn- parse-args
  "Parse hiccup element arguments into [props children].
   Handles: [tag props ...children] or [tag ...children]"
  [args]
  (if (map? (first args))
    [(first args) (rest args)]
    [{} args]))

(defn- parse-text-args
  "Parse text element arguments.
   Handles: [:text content props] or [:text content]"
  [args]
  (let [[content & rest-args] args
        props (if (map? (first rest-args)) (first rest-args) {})]
    [(str content) props]))

;; ============================================================================
;; NODE CONSTRUCTION
;; ============================================================================

(declare parse-node)

(defn- node?
  "Check if x is a parsed node (map with :type)."
  [x]
  (and (map? x) (contains? x :type)))

(defn- parse-container
  "Parse a container element (col, row, box)."
  [direction props children measure-fn]
  (let [layout-config (normalize-layout-config props direction)
        configs (extract-configs props)
        id (or (:id props) (next-id))
        ;; Parse children, flattening vectors of nodes (from `for` expressions)
        ;; parse-node returns vectors for seqs, so we need to flatten them
        parsed-children (into []
                              (comp (map #(parse-node % measure-fn))
                                    (mapcat #(cond
                                               (node? %)        [%]  ; single node
                                               (sequential? %)  %    ; vector of nodes (from for)
                                               :else            [%])) ; anything else
                                    (filter node?))
                              children)]
    {:type :container
     :id id
     :layout layout-config
     :configs configs
     :dimensions {:width nil :height nil}
     :bounding-box {:x 0 :y 0 :width 0 :height 0}
     :children parsed-children}))

(defn- parse-text
  "Parse a text element."
  [content props measure-fn]
  (let [text-config (normalize-text-config props)
        id (or (:id props) (next-id))
        measured (if measure-fn
                   (measure-fn content text-config)
                   {:width 0 :height 16})]
    {:type :text
     :id id
     :text-content content
     :text-config text-config
     :measured measured
     :dimensions {:width (:width measured 0)
                  :height (:height measured 16)}
     :bounding-box {:x 0 :y 0 :width 0 :height 0}
     :wrapped-lines nil}))

(defn- parse-spacer
  "Parse a spacer element."
  [props]
  (let [normalized (normalize/normalize-props (merge {:size :grow} props))
        id (or (:id props) (next-id))]
    {:type :container
     :id id
     :layout {:sizing (or (:size normalized)
                          {:width {:type :grow}
                           :height {:type :grow}})
              :padding {:top 0 :right 0 :bottom 0 :left 0}
              :child-gap 0
              :layout-direction :left-to-right
              :child-alignment {:x :left :y :top}}
     :configs {}
     :dimensions {:width nil :height nil}
     :bounding-box {:x 0 :y 0 :width 0 :height 0}
     :children []}))

(defn- parse-scroll
  "Parse a scroll container."
  [props children measure-fn]
  (let [scroll-dir (or (:direction props) :vertical)
        base-props (dissoc props :direction)
        container (parse-container nil base-props children measure-fn)]
    (assoc-in container [:configs :clip]
              {:vertical (= scroll-dir :vertical)
               :horizontal (= scroll-dir :horizontal)})))

(defn- parse-image
  "Parse an image element."
  [props _measure-fn]
  (let [layout-config (normalize-layout-config props nil)
        configs (extract-configs props)
        id (or (:id props) (next-id))]
    {:type :container
     :id id
     :layout layout-config
     :configs configs
     :dimensions {:width nil :height nil}
     :bounding-box {:x 0 :y 0 :width 0 :height 0}
     :children []}))

(defn- parse-floating
  "Parse a floating container - positions independently of flow."
  [props children measure-fn]
  (let [base-props (dissoc props :floating)
        container (parse-container nil base-props children measure-fn)
        normalized (normalize/normalize-props props)]
    (assoc-in container [:configs :floating]
              (:floating normalized))))

(defn- parse-element
  "Parse a hiccup element vector."
  [[tag & args] measure-fn]
  (let [[props children] (parse-args args)]
    (case tag
      :col (parse-container :top-to-bottom props children measure-fn)
      :row (parse-container :left-to-right props children measure-fn)
      :box (parse-container nil props children measure-fn)
      :scroll (parse-scroll props children measure-fn)
      :spacer (parse-spacer props)
      :image (parse-image props measure-fn)
      :floating (parse-floating props children measure-fn)
      :text (let [[content text-props] (parse-text-args args)]
              (parse-text content text-props measure-fn))

      ;; Custom component
      (if-let [component-fn (get @components tag)]
        (let [expanded (apply component-fn props children)]
          (parse-node expanded measure-fn))
        ;; Unknown tag - treat as container
        (do
          #?(:cljs (js/console.warn "Unknown hiccup tag:" (str tag)))
          (parse-container nil props children measure-fn))))))

(defn- parse-node
  "Parse a hiccup node (vector, seq, string, or nil)."
  [node measure-fn]
  (cond
    (vector? node) (parse-element node measure-fn)
    (seq? node) (mapv #(parse-node % measure-fn) node)
    (list? node) (mapv #(parse-node % measure-fn) node)
    (string? node) (parse-text node {} measure-fn)
    :else nil))

;; ============================================================================
;; PUBLIC API
;; ============================================================================

(defn parse
  "Parse hiccup DSL to nested tree structure.

   Parameters:
   - viewport: {:width :height} - root dimensions
   - hiccup: Hiccup data structure
   - measure-fn: Text measurement fn (text config) -> {:width :height}

   Returns: Nested tree ready for layout/layout"
  ([viewport hiccup]
   (parse viewport hiccup nil))
  ([viewport hiccup measure-fn]
   (reset-ids!)
   (let [content (parse-node hiccup measure-fn)]
     (t/root-container viewport
                       (if (vector? content)
                         content
                         (if content [content] []))))))

(defn render
  "Complete pipeline: hiccup → tree → layout → render commands.

   This is the main entry point - users call this, never touch layout code.

   Parameters:
   - viewport: {:width :height}
   - hiccup: Hiccup data structure
   - measure-fn: Text measurement function
   - opts: Optional map with :scroll-state

   Returns: Vector of render commands"
  ([viewport hiccup]
   (render viewport hiccup nil))
  ([viewport hiccup measure-fn]
   (render viewport hiccup measure-fn {}))
  ([viewport hiccup measure-fn opts]
   (-> (parse viewport hiccup measure-fn)
       (layout/layout (merge {:measure-fn measure-fn} opts)))))

(defn render-tree
  "Like render but returns the positioned tree (for debugging)."
  ([viewport hiccup]
   (render-tree viewport hiccup nil))
  ([viewport hiccup measure-fn]
   (render-tree viewport hiccup measure-fn {}))
  ([viewport hiccup measure-fn opts]
   (-> (parse viewport hiccup measure-fn)
       (layout/layout-tree (merge {:measure-fn measure-fn} opts)))))
