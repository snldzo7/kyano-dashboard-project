# Kyano Nestle - Working Capital Control Tower

## Architecture: Two Layers

### Layer 1: `kyano.ui` - Generic Reusable Component Library
A **use-case agnostic** component library that works for ANY dashboard/app.
Pure functions `(props) → hiccup`. No business logic. Just presentation.

### Layer 2: `nestle` - Working Capital Use Case
Imports `kyano.ui` and composes it with Nestle-specific data and views.

## Tech Stack
- **Replicant** - Pure hiccup UI rendering
- **Rheon** - Wire-as-Value real-time communication (future)
- **Datomic** - Temporal database via Docker (future)
- **Malli** - Schema validation and data synthesis

## Project Structure

```
kyano-nestle/
├── deps.edn
├── shadow-cljs.edn
├── package.json
├── tailwind.config.js
├── public/
│   ├── index.html
│   └── css/output.css
│
└── src/
    ├── kyano/ui/                    # GENERIC LIBRARY (reusable anywhere)
    │   ├── primitives.cljs          # Badge, Button, Icon, Avatar
    │   ├── layout.cljs              # Container, Grid, Flex, Stack, Section
    │   ├── cards.cljs               # Card, StatCard, MetricCard
    │   ├── charts.cljs              # Sparkline, ProgressBar, FlowDiagram
    │   ├── graphs.cljs              # DependencyGraph, DAG, Tree
    │   ├── forms.cljs               # Slider, Toggle, Input, Select, DatePicker
    │   ├── feedback.cljs            # StatusDot, Alert, Toast, Modal
    │   ├── navigation.cljs          # Nav, Tabs, Breadcrumb
    │   └── table.cljs               # Table, DataGrid
    │
    └── nestle/                      # USE CASE SPECIFIC
        ├── app.cljs                 # Entry point, routing
        ├── state.cljs               # App state atom
        ├── views/
        │   ├── dashboard.cljs       # Composes kyano.ui for dashboard
        │   ├── lineage.cljs         # Dependency graph view
        │   ├── scenario.cljs        # What-if simulator
        │   ├── collab.cljs          # CPG × Retail collaboration
        │   └── time_travel.cljs     # Temporal queries
        └── data/
            ├── observations.cljs    # Static facts
            ├── derived.cljs         # Calculated values
            └── stakeholders.cljs    # Roles
```

### 1.2 Atomic Component Design

All components are **pure functions**: `(data) → hiccup`

```clojure
;; primitives.cljs
(defn badge [{:keys [label variant]}]
  [:span {:class (badge-class variant)} label])

(defn card [{:keys [title subtitle children class]}]
  [:div {:class (str "bg-slate-800 rounded-xl p-6 " class)}
   (when title [:h3.text-lg.font-semibold title])
   (when subtitle [:p.text-slate-400.text-sm subtitle])
   children])

;; data_display.cljs
(defn stat [{:keys [label value delta trend]}]
  [:div.stat
   [:div.stat-label label]
   [:div.stat-value value]
   (when delta
     [:div {:class (trend-class trend)} delta])])

(defn kpi-card [{:keys [title value sparkline-data status why-data]}]
  [card {:class (status-border-class status)}
   [:div.flex.justify-between
    [:div
     [:div.text-slate-400 title]
     [:div.text-3xl.font-bold value]]
    [sparkline {:data sparkline-data}]]
   (when why-data
     [why-explainer why-data])])
```

### 1.3 Static Data Structure

```clojure
;; data/observations.cljs
(def observations
  {:supplier-otif {:value 0.78 :tx-time #inst "2024-10-23" :source "SAP"}
   :forecast-error {:value 0.12 :tx-time #inst "2024-10-22" :source "APO"}
   :consensus-forecast {:value 55000 :tx-time #inst "2024-10-21" :source "IBP"}
   ;; ... all 18 observations
   })

;; data/derived.cljs
(def derived
  {:demand-mean 61600
   :demand-std 5500
   :fg-inventory-position 42000
   :rpm-inventory-position 95000
   :service-risk 0.23
   :cash-impact 485000
   ;; ... all calculated values
   })
```

### 1.4 Views to Implement

1. **Dashboard View** (Primary)
   - 3 KPI Cards (Inventory Value, Service Risk, Cash Impact)
   - 2 Inventory Pool Cards (FG, RPM)
   - 4 Stat Cards (Demand, Production, Supplier OTIF, Forecast Error)
   - Why Explainer panel

2. **Lineage View**
   - 4-level dependency graph
   - Node selection → detail panel
   - Ancestor/descendant highlighting

3. **Scenario View**
   - Parameter sliders
   - Before/After comparison cards
   - Impact visualization

4. **Collaborative Room**
   - Stakeholder cards (CPG + Retail)
   - Global KPI dashboard
   - Causation chains

5. **Time Travel View**
   - Date picker (as-of navigation)
   - Fact evolution timeline
   - Decision context recall

---

## Phase 2: Malli Schemas & Data Synthesis

### 2.1 Schema Definitions

```clojure
;; schema/observations.cljc
(def Observation
  [:map
   [:value :double]
   [:tx-time inst?]
   [:valid-time {:optional true} inst?]
   [:source :string]
   [:note {:optional true} :string]])

(def SupplierOTIF
  [:map
   [:value [:double {:min 0.0 :max 1.0}]]
   ;; ... constraints
   ])

;; schema/derived.cljc
(def ServiceRisk
  [:double {:min 0.01 :max 0.99}])

(def CashImpact
  [:double {:min 0}])
```

### 2.2 Data Generators

```clojure
;; generators/scenarios.cljc
(require '[malli.generator :as mg])

(defn generate-baseline-scenario []
  {:observations (mg/generate observations-schema)
   :derived (calculate-derived observations)})

(defn generate-stress-scenario []
  ;; Low supplier OTIF, high forecast error
  )

(defn generate-optimal-scenario []
  ;; High performance across all metrics
  )
```

---

## Phase 3: Rheon Integration

### 3.1 Wire Definitions

```clojure
;; wires.cljc
(def wire-refs
  {:observations {:type :signal :spec Observations}
   :derived {:type :signal :spec Derived}
   :scenario-params {:type :signal :spec ScenarioParams}
   :time-cursor {:type :signal :spec inst?}
   :selected-node {:type :signal :spec :keyword}})
```

### 3.2 Server-Side Calculations

```clojure
;; server/engine.clj
(defn recalculate-derived [observations]
  (let [demand-mean (calc-demand-mean observations)
        demand-std (calc-demand-std observations)
        ;; ... all calculations
        ]
    {:demand-mean demand-mean
     :service-risk (calc-service-risk ...)
     :cash-impact (calc-cash-impact ...)}))
```

---

## Phase 4: Datomic Integration

### 4.1 Schema (Lexon Pattern)

```clojure
;; datomic/schema.clj
(def observation-schema
  [{:db/ident :obs/type
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one}
   {:db/ident :obs/value
    :db/valueType :db.type/double
    :db/cardinality :db.cardinality/one}
   {:db/ident :obs/valid-time
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one}
   {:db/ident :obs/source
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}])
```

### 4.2 Time Travel Queries

```clojure
;; datomic/queries.clj
(defn observations-as-of [db t]
  (d/q '[:find [(pull ?e [*]) ...]
         :in $ ?t
         :where
         [?e :obs/valid-time ?vt]
         [(<= ?vt ?t)]]
       (d/as-of db t) t))
```

---

## Implementation Order

### Step 1: Project Setup
- [ ] Create deps.edn with dependencies
- [ ] Configure shadow-cljs.edn
- [ ] Set up Tailwind CSS pipeline
- [ ] Create index.html

### Step 2: Core UI Components
- [ ] primitives.cljs (Badge, Card, Icon)
- [ ] layout.cljs (Container, Grid, Section)
- [ ] data_display.cljs (Stat, KPICard)
- [ ] charts.cljs (Sparkline)

### Step 3: Dashboard View
- [ ] Static data setup
- [ ] KPI cards with sparklines
- [ ] Inventory pool cards
- [ ] Why explainer

### Step 4: Navigation & Views
- [ ] App shell with nav
- [ ] Lineage view
- [ ] Scenario view
- [ ] Collab room
- [ ] Time travel

### Step 5: Interactivity
- [ ] View switching
- [ ] Node selection
- [ ] Slider interactions

---

## Dependencies

```clojure
;; deps.edn
{:deps
 {org.clojure/clojure {:mvn/version "1.12.0"}
  org.clojure/clojurescript {:mvn/version "1.11.132"}
  no.cjohansen/replicant {:mvn/version "2025.06.21"}
  metosin/malli {:mvn/version "0.19.2"}

  ;; Future phases
  ;; com.datomic/peer {:mvn/version "1.0.7394"}
  ;; rheon/rheon-core {:local/root "../rheon/rheon-core"}
  }}
```
