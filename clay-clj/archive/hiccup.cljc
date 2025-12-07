(ns clay.hiccup
  "Hiccup-style DSL for Clay layouts.

   Users define UI as pure data - NEVER touch layout code directly.
   This is the main entry point for building Clay UIs.

   Example:
   (render viewport
     [:col {:size :grow :bg :slate-100 :padding 16 :gap 8}
       [:text \"Hello\" {:font-size 32 :color :slate-900}]
       [:row {:gap 8}
         [:box {:size 100 :bg :red-500 :radius 8}]
         [:box {:size :grow :bg :blue-500}]]]
     measure-fn)

   Supported tags:
   - :col      Vertical container (top-to-bottom)
   - :row      Horizontal container (left-to-right)
   - :box      Generic container
   - :text     Text element
   - :spacer   Flexible space
   - :scroll   Scrollable container

   Custom components can be registered with register-component!"
  (:require [clay.layout.core :as layout]
            [clay.dsl.normalize :as normalize]))

;; ============================================================================
;; CUSTOM COMPONENT REGISTRY (Phase 3)
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

(defn unregister-component!
  "Remove a registered component."
  [tag]
  (swap! components dissoc tag))

;; ============================================================================
;; INTERACTIVITY (Phase 4 - faithful Clay.h port)
;; ============================================================================

(defn hovered?
  "Returns true if element with given id is under the pointer.
   Port of Clay_Hovered() - must be called during element declaration.

   Usage:
   [:box {:id :my-button
          :bg (if (hovered? state :my-button) :hover-color :normal-color)}
     [:text \"Hover me\"]]"
  [state element-id]
  (contains? (set (:pointer-over-ids state)) element-id))

(defn on-hover
  "Register a hover callback for current element.
   Port of Clay_OnHover(callback, userData).
   Callback receives: (element-id pointer-data user-data)

   Click detection is done by checking pointer-data inside callback:
   (defn my-handler [element-id pointer-data user-data]
     (when (= (:state pointer-data) :pressed-this-frame)
       (println \"Clicked!\" user-data)))"
  [callback & [user-data]]
  {:type :on-hover
   :callback callback
   :user-data user-data})

;; ============================================================================
;; PROPS NORMALIZATION
;; ============================================================================

(defn- normalize-layout-config
  "Normalize DSL props to layout config format."
  [props]
  (let [normalized (normalize/normalize-props props)
        layout-direction (case (:direction props)
                           :horizontal :left-to-right
                           :vertical :top-to-bottom
                           :row :left-to-right
                           :col :top-to-bottom
                           :column :top-to-bottom
                           (:layout-direction props)
                           (:direction normalized)
                           nil)]
    (cond-> {}
      ;; Sizing
      (:size normalized)
      (assoc :sizing (:size normalized))

      (:width normalized)
      (assoc-in [:sizing :width] (:width normalized))

      (:height normalized)
      (assoc-in [:sizing :height] (:height normalized))

      ;; Padding
      (or (:padding normalized) (:pad normalized))
      (assoc :padding (or (:padding normalized) (:pad normalized)))

      ;; Gap
      (or (:gap normalized) (:child-gap normalized))
      (assoc :child-gap (or (:gap normalized) (:child-gap normalized)))

      ;; Layout direction
      layout-direction
      (assoc :layout-direction layout-direction)

      ;; Child alignment
      (:align normalized)
      (assoc :child-alignment (:align normalized))

      ;; Element ID for interactivity
      (:id props)
      (assoc :id (:id props)))))

(defn- extract-configs
  "Extract configuration types (bg, border, etc.) from props."
  [props]
  (let [normalized (normalize/normalize-props props)]
    (cond-> []
      ;; Background
      (or (:bg normalized) (:background normalized))
      (conj {:type :background
             :config {:color (or (:bg normalized) (:background normalized))
                      :corner-radius (or (:radius normalized) (:corner-radius normalized)
                                         {:top-left 0 :top-right 0
                                          :bottom-left 0 :bottom-right 0})}})

      ;; Border
      (:border normalized)
      (conj {:type :border :config (:border normalized)})

      ;; Clip/Scroll
      (:scroll normalized)
      (conj {:type :clip :config {:vertical (= (:direction (:scroll normalized)) :vertical)
                                  :horizontal (= (:direction (:scroll normalized)) :horizontal)}}))))

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

(defn- text-args
  "Parse text element arguments.
   Handles: [:text content props] or [:text content]"
  [args]
  (let [[content & rest-args] args
        props (if (map? (first rest-args)) (first rest-args) {})]
    [content props]))

;; ============================================================================
;; NODE PROCESSING
;; ============================================================================

(declare process-node)

(defn- open-container
  "Open a container element with the given direction."
  [state direction props]
  (let [layout-config (cond-> (normalize-layout-config props)
                        direction (assoc :layout-direction direction))
        configs (extract-configs props)]
    (reduce
     (fn [s {:keys [type config]}]
       (layout/configure-element s type config))
     (layout/open-element state layout-config)
     configs)))

(defn- process-text
  "Process a text element."
  [state content props measure-fn]
  (let [normalized (normalize/normalize-props props)
        text-config {:font-id (or (:font-id normalized) (:font normalized) 0)
                     :font-size (or (:font-size normalized) 16)
                     :text-color (or (:text-color normalized) (:color normalized))
                     :letter-spacing (:letter-spacing normalized)
                     :line-height (:line-height normalized)
                     :wrap-mode (or (:text-wrap normalized) (:wrap normalized))}
        measured (measure-fn (str content) text-config)]
    #?(:cljs (js/console.log "process-text measured:" (str content) (clj->js measured)))
    (layout/open-text-element state (str content) text-config measured)))

(defn- process-spacer
  "Process a spacer element."
  [state props]
  (let [normalized (normalize/normalize-props (merge {:size :grow} props))]
    (-> state
        (layout/open-element {:sizing (:size normalized)})
        layout/close-element)))

(defn- process-children
  "Process children nodes."
  [state children measure-fn]
  (reduce
   (fn [s child]
     (process-node s child measure-fn))
   state
   children))

(defn- process-custom-component
  "Process a custom registered component."
  [state tag props children measure-fn]
  (if-let [component-fn (get @components tag)]
    (let [expanded (apply component-fn props children)]
      (process-node state expanded measure-fn))
    ;; Unknown tag - just process children as container
    (do
      (js/console.warn "Unknown hiccup tag:" (str tag))
      state)))

(defn- process-element
  "Process a hiccup element vector."
  [state element measure-fn]
  (let [[tag & args] element
        [props children] (parse-args args)]
    (case tag
      ;; Container elements
      :col
      (-> state
          (open-container :top-to-bottom props)
          (process-children children measure-fn)
          layout/close-element)

      :row
      (-> state
          (open-container :left-to-right props)
          (process-children children measure-fn)
          layout/close-element)

      :box
      (-> state
          (open-container nil props)
          (process-children children measure-fn)
          layout/close-element)

      ;; Scroll container
      :scroll
      (let [scroll-dir (or (:direction props) :vertical)]
        (-> state
            (open-container nil (dissoc props :direction))
            (layout/configure-element :clip {:vertical (= scroll-dir :vertical)
                                             :horizontal (= scroll-dir :horizontal)})
            (process-children children measure-fn)
            layout/close-element))

      ;; Text element
      :text
      (let [[content text-props] (text-args args)]
        (process-text state content text-props measure-fn))

      ;; Spacer
      :spacer
      (process-spacer state props)

      ;; Custom component or unknown
      (process-custom-component state tag props children measure-fn))))

(defn- process-node
  "Process a hiccup node (can be vector, seq, string, or nil)."
  [state node measure-fn]
  (cond
    ;; Hiccup element vector
    (vector? node)
    (process-element state node measure-fn)

    ;; Sequence of nodes (e.g., from map)
    (seq? node)
    (reduce #(process-node %1 %2 measure-fn) state node)

    ;; List of nodes
    (list? node)
    (reduce #(process-node %1 %2 measure-fn) state node)

    ;; on-hover callback (Phase 4)
    (and (map? node) (= (:type node) :on-hover))
    (do
      ;; Store callback for processing by server
      ;; (This would be handled by the render loop)
      state)

    ;; nil or other - ignore
    :else state))

;; ============================================================================
;; MAIN RENDER FUNCTION
;; ============================================================================

(defn render
  "Render hiccup UI to Clay render commands.

   This is the main entry point - users call this, never touch layout code.

   Parameters:
   - viewport: {:width number :height number}
   - hiccup: Hiccup data structure
   - measure-fn: Text measurement function (fn [text config] -> {:width :height})

   Returns: Vector of render commands"
  [viewport hiccup measure-fn]
  (-> (layout/begin-layout viewport)
      (process-node hiccup measure-fn)
      (layout/end-layout measure-fn)
      layout/get-render-commands))

(defn render-with-state
  "Render hiccup UI with application state for interactivity.

   Parameters:
   - viewport: {:width number :height number}
   - ui-fn: Function (state) -> hiccup
   - app-state: Application state map (includes :pointer-over-ids for hover)
   - measure-fn: Text measurement function

   Returns: Vector of render commands"
  [viewport ui-fn app-state measure-fn]
  (let [hiccup (ui-fn app-state)]
    (render viewport hiccup measure-fn)))
