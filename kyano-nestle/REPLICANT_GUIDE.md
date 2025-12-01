# Replicant Data-Driven Event Handling Guide

This guide documents the data-driven event handling patterns used in this ClojureScript/Replicant application.

## Core Principle

**All event handlers are data, not functions.** Components never contain callback functions - instead, they specify action vectors that describe what should happen.

## Event Handler Syntax

### Correct Syntax
```clojure
;; Single action
[:button {:on {:click [:action/name arg1 arg2]}}
 "Click me"]

;; Multiple actions (executed sequentially)
[:button {:on {:click [[:action/first arg1]
                       [:action/second arg2]]}}
 "Do multiple things"]

;; Form inputs with value interpolation
[:input {:type "text"
         :on {:change [:action/set-value :event/target.value]}}]
```

### Incorrect Syntax (Don't Do This)
```clojure
;; WRONG: Using :on-click instead of :on {:click ...}
[:button {:on-click (fn [e] ...)} "Click"]

;; WRONG: Using functions as handlers
[:button {:on {:click #(do-something %)}} "Click"]

;; WRONG: Defining callbacks in render functions
(let [handle-click (fn [] (swap! state ...))]
  [:button {:on {:click handle-click}} "Click"])
```

## Event Value Interpolation

When you need values from DOM events, use these placeholders:

| Placeholder | Value |
|-------------|-------|
| `:event/target.value` | Input/select value |
| `:event/target.checked` | Checkbox checked state |
| `:event/key` | Keyboard key pressed |

Example:
```clojure
;; Text input
[:input {:type "text"
         :value current-value
         :on {:change [:form/set-field :username :event/target.value]}}]

;; Range slider
[:input {:type "range"
         :min 0 :max 100
         :value current-value
         :on {:change [:slider/update :event/target.value]}}]

;; Select dropdown
[:select {:value current-selection
          :on {:change [:dropdown/select :event/target.value]}}
 [:option {:value "a"} "Option A"]
 [:option {:value "b"} "Option B"]]
```

## Action Dispatch System

### Dispatcher Setup (`dispatch.cljs`)

```clojure
(ns myapp.dispatch
  (:require [replicant.dom :as r]
            [clojure.walk :as walk]))

;; Interpolate event values into action data
(defn interpolate-event-values [dom-event actions]
  (walk/postwalk
   (fn [x]
     (case x
       :event/target.value (.. dom-event -target -value)
       :event/target.checked (.. dom-event -target -checked)
       :event/key (.-key dom-event)
       x))
   actions))

;; Multimethod for action execution
(defmulti execute-action! (fn [[action-type & _]] action-type))

;; Example action handlers
(defmethod execute-action! :app/set-view
  [[_ view-id]]
  (swap! app-state assoc :view view-id))

(defmethod execute-action! :form/set-field
  [[_ field-key value]]
  (swap! app-state assoc-in [:form field-key] value))

;; Main dispatcher
(defn dispatch! [event-data handler-data]
  (when (= :replicant.trigger/dom-event (:replicant/trigger event-data))
    (let [dom-event (:replicant/dom-event event-data)
          actions (interpolate-event-values dom-event handler-data)]
      (if (keyword? (first actions))
        (execute-action! actions)          ;; Single action
        (doseq [action actions]            ;; Multiple actions
          (execute-action! action))))))

;; Initialize at app startup
(defn init! []
  (r/set-dispatch! dispatch!))
```

### Registering the Dispatcher

Call `dispatch/init!` once at app startup:

```clojure
(ns myapp.app
  (:require [myapp.dispatch :as dispatch]))

(defn init! []
  (dispatch/init!)
  (render!))
```

## Component Patterns

### Data-Driven Button Component
```clojure
(defn button
  "Button component - receives action data, not functions"
  [{:keys [label variant on-click disabled?]}]
  [:button {:class (get-button-classes variant disabled?)
            :disabled disabled?
            :on {:click on-click}}  ;; on-click is action DATA
   label])

;; Usage
(button {:label "Save"
         :variant :primary
         :on-click [:form/submit]})
```

### Data-Driven Form Input
```clojure
(defn text-input
  [{:keys [value label on-change]}]
  [:div
   [:label label]
   [:input {:type "text"
            :value value
            :on {:change on-change}}]])  ;; on-change is action DATA

;; Usage
(text-input {:value (:name form-data)
             :label "Name"
             :on-change [:form/set-field :name :event/target.value]})
```

### Complex Component with Multiple Actions
```clojure
(defn modal
  [{:keys [title on-confirm on-cancel]}]
  [:div.modal
   [:h2 title]
   [:button {:on {:click on-cancel}} "Cancel"]
   [:button {:on {:click on-confirm}} "Confirm"]])

;; Usage with multiple actions
(modal {:title "Delete Item?"
        :on-cancel [:modal/close]
        :on-confirm [[:item/delete item-id]
                     [:modal/close]
                     [:toast/show "Item deleted"]]})
```

## Action Naming Conventions

Use namespaced keywords for actions:

| Namespace | Purpose | Example |
|-----------|---------|---------|
| `:app/` | App-level navigation/state | `:app/set-view` |
| `:form/` | Form interactions | `:form/set-field` |
| `:modal/` | Modal dialogs | `:modal/open`, `:modal/close` |
| `:nav/` | Navigation | `:nav/goto` |
| `:<feature>/` | Feature-specific | `:scenario/reset` |

## Available Actions in This App

### Navigation
- `[:app/set-view :view-id]` - Navigate to view
- `[:app/toggle-kpi :kpi-id]` - Toggle KPI selection

### Lineage View
- `[:lineage/select-node :node-id]` - Select a node
- `[:lineage/toggle-node :node-id]` - Toggle node selection

### Scenario Simulator
- `[:scenario/start]` - Enter scenario mode
- `[:scenario/reset]` - Reset to baseline
- `[:scenario/apply]` - Apply changes
- `[:scenario/set-category :category]` - Set active category
- `[:scenario/update-observation :obs-key :event/target.value]` - Update slider
- `[:scenario/set-name :event/target.value]` - Set scenario name
- `[:scenario/toggle-participant :id]` - Toggle participant
- `[:scenario/record-decision]` - Record decision

### Time Travel
- `[:time-travel/set-cursor "2025-10-15"]` - Jump to date
- `[:time-travel/set-cursor-from-ms :event/target.value]` - From slider
- `[:time-travel/jump-to-now]` - Return to present
- `[:time-travel/select-decision :decision-id]` - Select decision
- `[:time-travel/set-selected-fact :fact-key]` - Select fact to track

### Collaborative Room
- `[:collab/toggle-cpg-stakeholder :key]` - Toggle CPG stakeholder
- `[:collab/toggle-retail-stakeholder :key]` - Toggle retail stakeholder
- `[:collab/toggle-scenario :scenario-key]` - Toggle complexity scenario

## Benefits of Data-Driven Handlers

1. **Serializable** - Actions can be logged, replayed, persisted
2. **Testable** - Test action handlers in isolation
3. **Debuggable** - Log all actions to see exactly what happened
4. **Decoupled** - Components don't know about state management
5. **Consistent** - One pattern for all interactions

## Migration Checklist

When converting from function-based to data-driven handlers:

- [ ] Replace `:on-click fn` with `:on {:click action-data}`
- [ ] Replace `:on-change fn` with `:on {:change action-data}`
- [ ] Replace inline `(fn [e] ...)` with action vectors
- [ ] Use `:event/target.value` for input values
- [ ] Remove function definitions from render functions
- [ ] Add action handlers to dispatcher multimethod
