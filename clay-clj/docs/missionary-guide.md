# Mastering Missionary: A Complete Guide to Continuous Time Flow Programming

Missionary is a functional effect and streaming system for Clojure/ClojureScript that unifies asynchronous programming, reactive streams, and functional reactive programming (FRP) under a single, elegant abstraction. Created by Léo Noel, it serves as the reactive foundation for Electric Clojure and represents a fundamental rethinking of how we handle concurrency through **structured supervision** rather than manual coordination.

**Core value**: Missionary provides automatic supervision, proper cancellation semantics, glitch-free reactive computations, and unified discrete/continuous flow handling—solving problems that traditional async libraries leave to user space. It prioritizes **computation over conveyance**, making process structure first-class rather than treating communication channels as the primary abstraction.

## Foundation: Tasks and Flows

Missionary has two core abstractions that cover all async patterns: **tasks** (single-value async operations) and **flows** (multi-value streams). Both are first-class values that compose functionally with automatic error propagation and cancellation.

### Tasks: Asynchronous Single Values

A **task** represents an action to be performed later. Tasks are cold (don't execute until run), cancellable, and can be executed multiple times.

**Creating tasks with `sp` (Sequential Process)**:

```clojure
(require '[missionary.core :as m])

;; Basic task - prints and returns value
(def hello-task
  (m/sp
    (println "Hello")
    (m/? (m/sleep 1000))  ; ? parks on a task
    (println "World!")
    42))

;; Run at REPL (JVM only - blocks calling thread)
(m/? hello-task)
; Hello
; ... 1 second delay ...
; World!
;=> 42

;; Run with callbacks (works everywhere)
(hello-task
  (fn [result] (println "Success:" result))
  (fn [error] (println "Failed:" error)))
```

**The `?` operator** inside `sp` parks the process without blocking the thread. The process suspends, yielding control, then resumes when the task completes. This works on both JVM and JavaScript.

**Concurrent task execution**:

```clojure
;; join - run tasks concurrently, combine results
(m/? (m/join vector
       (m/sleep 1000 :a)
       (m/sleep 1000 :b)
       (m/sleep 1000 :c)))
;=> [:a :b :c]  ; after 1 second (parallel), not 3

;; race - return first successful completion
(m/? (m/race
       (m/sleep 2000 :slow)
       (m/sleep 500 :fast)
       (m/sleep 1000 :medium)))
;=> :fast  ; after 500ms, other tasks cancelled

;; Parallelism scales automatically
(m/? (m/join +
       (m/sleep 100 1)
       (m/sleep 100 2)
       (m/sleep 100 3)
       (m/sleep 100 4)))
;=> 10  ; after ~100ms
```

**Thread offloading (JVM only)**:

```clojure
;; CPU-bound work - runs on cpu executor (sized to cores)
(def heavy-computation
  (m/via m/cpu
    (reduce + (range 1000000))))

;; I/O-bound work - runs on blk executor (unbounded pool)
(def file-read
  (m/via m/blk
    (slurp "large-file.txt")))

;; Custom executor
(import '(java.util.concurrent Executors))
(def custom-pool (Executors/newFixedThreadPool 4))

(m/via-call custom-pool
  (fn [] (blocking-operation)))
```

**Task operators**:

```clojure
;; timeout - limit execution time
(m/? (m/timeout (m/sleep 2000 :result) 1000 :timeout))
;=> :timeout

;; memo - cache result (run once, share result)
(def expensive (m/memo (m/via m/cpu (expensive-calc))))
(m/? expensive)  ; computes
(m/? expensive)  ; returns cached

;; attempt - capture success/failure as thunk
(m/? (m/sp
       (let [result (m/? (m/attempt risky-task))]
         (if (fn? result)
           (result)  ; success - call to unwrap
           :failed))))  ; error thunk not called

;; absolve - unwrap attempt (throws on error)
(m/? (m/sp
       (m/? (m/absolve (m/attempt risky-task)))))

;; compel - make uncancellable (runs to completion)
(m/compel critical-task)

;; never - task that never completes
m/never
```

### Flows: Multi-Value Streams

A **flow** represents a sequence of values produced over time. Flows come in two flavors:

**Discrete flows** - event streams with backpressure (each value has individual significance):

```clojure
;; seed - create flow from collection
(def numbers (m/seed [1 2 3 4 5]))

;; reduce - consume flow into task
(m/? (m/reduce + 0 numbers))
;=> 15

;; reductions - flow of intermediate results
(m/? (m/reduce conj []
       (m/reductions + 0 (m/seed [1 2 3 4]))))
;=> [0 1 3 6 10]

;; eduction - apply transducers
(m/? (m/reduce conj []
       (m/eduction
         (comp (map inc) (filter even?))
         (m/seed (range 10)))))
;=> [2 4 6 8 10]
```

**Continuous flows** - time-varying values (intermediate updates can be discarded):

```clojure
;; watch - create continuous flow from atom
(def !state (atom 0))
(def state-flow (m/watch !state))

;; latest - combine latest values from multiple flows
(def combined
  (m/latest vector
    (m/watch !atom1)
    (m/watch !atom2)))

;; relieve - convert discrete to continuous (relieves backpressure)
(def continuous
  (m/relieve {} discrete-flow))  ; keeps only latest

;; signal - shared continuous flow with memoization
(def shared-signal
  (m/signal (m/watch !state)))
```

**Creating flows with `ap` (Ambiguous Process)**:

The `ap` macro is the flow equivalent of `sp`, enabling powerful forking and choice operations:

```clojure
;; Basic ap - generate squared values
(def squares
  (m/ap
    (let [x (m/?> (m/seed [1 2 3 4 5]))]  ; ?> forks for each value
      (* x x))))

(m/? (m/reduce conj [] squares))
;=> [1 4 9 16 25]

;; ap with async operations
(def async-squares
  (m/ap
    (let [x (m/?> (m/seed (range 5)))]
      (m/? (m/sleep 100))  ; ? parks on task
      (* x x))))

;; Forking with parallelism control
(def parallel-processing
  (m/ap
    (let [x (m/?> 3 (m/seed (range 20)))]  ; process 3 concurrently
      (m/? (expensive-task x)))))
```

**Forking operators** (`?>`, `?<`, `?=`):

```clojure
;; ?> (sequential fork) - backpressure-passing
;; Processes one value at a time, waits for downstream
(m/ap
  (let [n (m/?> (m/seed (range 10)))]
    (m/? (m/sleep 100 n))))
; Emits: 0, 1, 2, ... (sequentially)

;; ?< (concurrent fork) - cancels current when new arrives
;; For fast-changing sources, drops slow processing
(m/ap
  (let [n (m/?< fast-stream)]
    (try
      (m/? (m/sleep 200 n))  ; might be cancelled
      (catch missionary.Cancelled _
        (m/amb)))))  ; emit nothing if cancelled
; Only emits values that complete before next arrives

;; ?= (unlimited parallel) - all values processed concurrently
;; Alias for (m/?> ##Inf flow)
(m/ap
  (let [x (m/?= (m/seed (range 10)))]
    (m/? (process x))))
; All 10 items processed in parallel

;; ?> with parallelism parameter
(m/ap
  (let [x (m/?> 5 (m/seed (range 100)))]  ; max 5 concurrent
    (m/? (api-call x))))
```

**Choice operators** (`amb`, `amb=`):

```clojure
;; amb - sequential ambiguous choice (forks into branches)
(m/ap
  (let [x (m/?> (m/seed [1 2 3]))]
    (m/amb x (* x x))))  ; emit both x and x²
; Emits: 1, 1, 2, 4, 3, 9

;; amb with zero args - terminates current branch
(m/ap
  (let [x (m/?> (m/seed [1 2 3 4 5]))]
    (if (even? x)
      x
      (m/amb))))  ; skip odd numbers
; Emits: 2, 4

;; amb= - concurrent ambiguous choice
(m/ap
  (let [x (m/?> (m/seed [:a :b :c]))]
    (m/amb=
      (m/sleep 300 [x 1])
      (m/sleep 100 [x 2])
      (m/sleep 200 [x 3]))))
; Results emitted in completion order
```

**Flow operators**:

```clojure
;; zip - combine flows element-wise
(m/? (m/reduce conj []
       (m/zip vector
         (m/seed [1 2 3])
         (m/seed [:a :b :c]))))
;=> [[1 :a] [2 :b] [3 :c]]

;; group-by - split flow by key function
(m/group-by even? (m/seed (range 10)))
; Returns map of {true flow-of-evens, false flow-of-odds}

;; buffer - buffer overflow up to capacity
(m/buffer 100 producer-flow)

;; sample - sample flows driven by sampler
(m/sample vector sampler-flow flow1 flow2)

;; none - empty flow (terminates immediately)
m/none
```

**Continuous process with `cp`**:

```clojure
;; cp - creates continuous flow (always ready to transfer)
;; Only ?< allowed (no ?> or ?=)
(def continuous-computation
  (m/cp
    (let [x (m/?< (m/watch !state))]
      (* x 2))))
```

## Complete API Reference with Examples

### Task Combinators

**Sequential composition** - Run tasks one after another:

```clojure
(m/sp
  (let [user (m/? (fetch-user-task 123))
        posts (m/? (fetch-posts-task (:id user)))
        comments (m/? (fetch-comments-task posts))]
    {:user user :posts posts :comments comments}))
```

**Parallel composition** - Run tasks concurrently:

```clojure
;; join - all tasks must succeed
(m/? (m/join
       (fn [users posts comments]
         {:users users :posts posts :comments comments})
       (fetch-users-task)
       (fetch-posts-task)
       (fetch-comments-task)))

;; If any fails, all are cancelled and error propagates

;; race - first success wins, others cancelled
(m/? (m/race
       (fetch-from-primary-db)
       (fetch-from-secondary-db)
       (fetch-from-cache)))
```

**Timeout and retry patterns**:

```clojure
;; Simple timeout
(m/? (m/timeout api-call 5000 :timeout-value))

;; Retry with exponential backoff
(defn retry-with-backoff
  ([task] (retry-with-backoff task {:max-attempts 3 :initial-delay 1000}))
  ([task {:keys [max-attempts initial-delay multiplier]
          :or {multiplier 2}}]
   (m/sp
     (loop [attempt 0
            delay initial-delay]
       (if (>= attempt max-attempts)
         (throw (ex-info "Max retries exceeded" {:attempts attempt}))
         (let [result (m/? (m/attempt task))]
           (if (fn? result)
             (result)  ; success
             (do
               (println "Retry" (inc attempt) "after" delay "ms")
               (m/? (m/sleep delay))
               (recur (inc attempt) (* delay multiplier))))))))))

;; Usage
(m/? (retry-with-backoff (flaky-api-call)))
```

**Circuit breaker pattern**:

```clojure
(defn circuit-breaker [task {:keys [failure-threshold reset-timeout]
                             :or {failure-threshold 5 reset-timeout 60000}}]
  (let [!state (atom {:failures 0 :state :closed :last-failure nil})]
    (fn []
      (m/sp
        (let [{:keys [state failures last-failure]} @!state]
          (case state
            :open
            (if (> (- (System/currentTimeMillis) last-failure) reset-timeout)
              (do
                (swap! !state assoc :state :half-open)
                (try
                  (let [result (m/? task)]
                    (swap! !state assoc :state :closed :failures 0)
                    result)
                  (catch Exception e
                    (swap! !state assoc :state :open 
                           :last-failure (System/currentTimeMillis))
                    (throw e))))
              (throw (ex-info "Circuit breaker open" @!state)))
            
            (:closed :half-open)
            (try
              (let [result (m/? task)]
                (swap! !state assoc :failures 0 :state :closed)
                result)
              (catch Exception e
                (let [new-failures (inc failures)]
                  (if (>= new-failures failure-threshold)
                    (swap! !state assoc 
                           :state :open 
                           :failures new-failures
                           :last-failure (System/currentTimeMillis))
                    (swap! !state update :failures inc))
                  (throw e))))))))))

;; Usage
(def protected-api (circuit-breaker (api-call) {}))
(m/? (protected-api))
```

### Flow Transformations

**Pagination pattern** (iterative queries):

```clojure
;; Stream all pages from paginated API
(defn pages
  ([] (pages :start))
  ([id]
   (m/ap
     (loop [id id]
       (let [{:keys [page next]} (m/? (api-call id))]
         (if next
           (m/amb page (recur next))  ; emit page, continue
           page))))))  ; emit final page, stop

;; Consume all pages
(m/? (m/reduce into [] (pages)))

;; With transducer to count items
(m/? (m/reduce + 0 
       (m/eduction (map count) (pages))))
```

**Debounce pattern**:

```clojure
;; Emit only values not followed by another within delay
(defn debounce [delay flow]
  (m/ap
    (let [x (m/?< flow)]  ; Preemptive fork
      (try
        (m/? (m/sleep delay x))  ; Wait delay period
        (catch missionary.Cancelled _
          (m/amb))))))  ; Cancelled by new value - skip

;; Usage with clock
(defn clock [intervals]
  (m/ap
    (let [i (m/?> (m/seed intervals))]
      (m/? (m/sleep i i)))))

(m/? (->> (clock [24 79 67 34 18 9 99 37])
          (debounce 50)
          (m/reduce conj [])))
;=> [24 79 99]  ; only values with >50ms gap after
```

**Throttle pattern**:

```clojure
;; Emit at most one value per time window
(defn throttle [ms flow]
  (m/ap
    (let [x (m/?> flow)]
      (m/? (m/sleep ms x)))))

;; Usage
(m/? (m/reduce conj []
       (throttle 100 (m/seed (range 10)))))
```

**Backpressure strategies**:

```clojure
;; Strategy 1: Backpressure-passing (default with ?>)
;; Upstream waits for downstream to be ready
(m/ap
  (let [item (m/?> slow-producer)]  ; producer waits
    (m/? (slow-processing item))))  ; safe, no unbounded buffering

;; Strategy 2: Drop intermediates (use relieve)
(m/relieve {} fast-producer)  ; keeps only latest
(m/relieve + number-stream)   ; aggregates with semigroup

;; Strategy 3: Buffer with capacity
(m/buffer 100 producer-flow)

;; Strategy 4: Cancel slow processing (use ?<)
(m/ap
  (let [x (m/?< fast-stream)]
    (try
      (m/? (slow-task x))
      (catch missionary.Cancelled _
        (m/amb)))))  ; drop cancelled work

;; Strategy 5: Controlled parallelism
(m/ap
  (let [req (m/?> 5 request-queue)]  ; max 5 concurrent
    (m/? (api-call req))))
```

**Parallel map**:

```clojure
(defn pmap-missionary [f coll parallelism]
  (m/? (m/reduce conj []
         (m/ap
           (let [x (m/?> parallelism (m/seed coll))]
             (m/? (f x)))))))

;; Usage
(pmap-missionary 
  (fn [x] (m/sp (m/? (m/sleep 100)) (* x x)))
  (range 10)
  3)  ; process 3 at a time
```

### Reactive Patterns

**Watch atoms with derived computations**:

```clojure
(def !input (atom 1))

;; Diamond-shaped dependency (glitch-free!)
(def main
  (let [<x (m/signal (m/watch !input))
        <y (m/signal (m/latest + <x <x))]  ; derived: x + x
    (m/reduce (fn [_ x] (prn x)) nil <y)))

(def dispose! (main #(prn ::success %) #(prn ::crash %)))
; prints 2

(swap! !input inc)
; prints 4 (not 3!)
; 3 would be inconsistent state (old x + new x)
; Missionary prevents this "glitch"

(dispose!)  ; cleanup
```

**Reactive form with validation**:

```clojure
(defn reactive-form []
  (let [!email (atom "")
        !name (atom "")
        <email (m/signal (m/watch !email))
        <name (m/signal (m/watch !name))
        <valid? (m/signal 
                  (m/latest 
                    (fn [email name]
                      (and (re-matches #".+@.+" email)
                           (> (count name) 2)))
                    <email <name))]
    
    ;; Reactive computation runs whenever inputs change
    (m/reduce 
      (fn [_ valid?] 
        (println "Form valid:" valid?)
        valid?)
      nil 
      <valid?)))
```

**Event stream processing**:

```clojure
;; DOM events as flow (ClojureScript)
(defn event-flow [element event-type]
  (m/observe
    (fn [emit!]
      (let [handler #(emit! %)]
        (.addEventListener element event-type handler)
        #(.removeEventListener element event-type handler)))))

;; Click stream with debounce
(def clicks (event-flow (js/document.getElementById "btn") "click"))

(def debounced-clicks
  (m/ap
    (let [click (m/?< clicks)]
      (try
        (m/? (m/sleep 300 click))
        (catch missionary.Cancelled _
          (m/amb))))))

;; Count clicks
(m/reduce (fn [cnt _] (inc cnt)) 0 debounced-clicks)
```

### Concurrency Primitives

**Mailbox** (asynchronous queue):

```clojure
(def mbx (m/mbx))

;; Producer
(m/sp
  (dotimes [i 10]
    (m/? (mbx i))  ; post value
    (m/? (m/sleep 100))))

;; Consumer
(m/sp
  (loop []
    (let [x (m/? (mbx))]  ; fetch value
      (println "Received:" x)
      (recur))))
```

**Rendezvous** (synchronous handoff):

```clojure
(def rdv (m/rdv))

;; Producer waits for consumer
(m/sp
  (dotimes [i 5]
    (m/? (rdv i))  ; blocks until taken
    (println "Gave" i)))

;; Consumer waits for producer
(m/sp
  (loop []
    (let [x (m/? (rdv))]  ; blocks until given
      (println "Took" x)
      (recur))))
```

**Semaphore**:

```clojure
(def sem (m/sem))

;; Acquire/release manually
(m/sp
  (m/? (sem))  ; acquire
  (try
    (critical-section)
    (finally
      (sem))))  ; release

;; Or use holding macro
(m/sp
  (holding sem
    (critical-section)))
```

**Dining Philosophers** (semaphore example):

```clojure
(defn philosopher [name fork-left fork-right]
  (m/sp
    (while true
      (println name "thinking")
      (m/? (m/sleep 500))
      (holding fork-left
        (holding fork-right
          (println name "eating")
          (m/? (m/sleep 600)))))))

(def forks (vec (repeatedly 5 m/sem)))

(m/? (m/timeout 10000
       (m/join vector
         (philosopher "Descartes" (forks 0) (forks 1))
         (philosopher "Hume" (forks 1) (forks 2))
         (philosopher "Plato" (forks 2) (forks 3))
         (philosopher "Nietzsche" (forks 3) (forks 4))
         (philosopher "Kant" (forks 0) (forks 4)))))
```

**Dataflow variable** (single assignment):

```clojure
(def dfv (m/dfv))

;; Writer
(m/sp
  (m/? (m/sleep 1000))
  (dfv 42))  ; assign once

;; Readers (multiple can read)
(m/sp (println "Reader 1:" (m/? (dfv))))
(m/sp (println "Reader 2:" (m/? (dfv))))
```

## Platform-Specific Programming

### Cross-Platform Code (.cljc)

Most of Missionary's API works in `.cljc` files. Use reader conditionals for platform-specific parts:

```clojure
(ns app.core
  (:require [missionary.core :as m]
            #?(:clj  [clojure.java.io :as io]
               :cljs [cljs.reader :as reader])))

;; ✅ Works everywhere - pure async
(def fetch-data
  (m/sp
    (let [delay (rand-int 1000)
          _ (m/? (m/sleep delay))]
      {:timestamp #?(:clj (System/currentTimeMillis)
                     :cljs (.now js/Date))
       :delay delay})))

;; ✅ Cross-platform flow
(defn process-stream [source]
  (m/ap
    (let [x (m/?> source)
          processed (transform x)
          _ (m/? (m/sleep 100))]
      processed)))
```

### JVM-Specific (.clj)

**Blocking operations** require `via`:

```clojure
;; ❌ BAD - blocks thread inside sp
(m/sp
  (Thread/sleep 1000)  ; blocks executor thread!
  (slurp "file.txt"))

;; ✅ GOOD - offloads to blocking executor
(m/sp
  (m/? (m/via m/blk
         (Thread/sleep 1000)
         (slurp "file.txt"))))

;; CPU-bound work
(m/sp
  (m/? (m/via m/cpu
         (reduce + (range 1000000)))))
```

**Top-level execution** (REPL):

```clojure
;; ✅ Works on JVM - blocks calling thread
(m/? (m/sleep 1000 42))
;=> 42

;; ❌ Doesn't work in ClojureScript - would block event loop
```

**Database example** (JDBC):

```clojure
(require '[next.jdbc :as jdbc])

(defn query-db [ds sql]
  (m/via m/blk  ; I/O operation
    (jdbc/execute! ds sql)))

(defn parallel-queries [ds]
  (m/sp
    (let [[users posts] 
          (m/? (m/join vector
                 (query-db ds ["SELECT * FROM users"])
                 (query-db ds ["SELECT * FROM posts"])))]
      {:users users :posts posts})))

;; With transaction
(defn with-transaction [ds f]
  (m/sp
    (m/? (m/via m/blk
           (jdbc/with-transaction [tx ds]
             (m/? (f tx)))))))
```

### ClojureScript-Specific (.cljs)

**No blocking operations**:

```clojure
;; ❌ Doesn't exist in ClojureScript
(m/via m/blk (blocking-io))

;; ✅ Use async APIs
(m/sp
  (m/? (promise->task (js/fetch url))))
```

**Promise interop**:

```clojure
;; Convert Promise to task
(defn await-promise [promise]
  (fn [success failure]
    (.then promise success failure)
    #()))  ; cancellation no-op

;; Usage
(m/sp
  (let [response (m/? (await-promise (js/fetch "/api/data")))
        data (m/? (await-promise (.json response)))]
    data))
```

**Top-level execution**:

```clojure
;; ❌ Can't use m/? at top level
(m/? some-task)  ; Error!

;; ✅ Convert to Promise
(js/Promise.
  (fn [resolve reject]
    ((m/sleep 1000 42) resolve reject)))

;; ✅ Or use callbacks
((m/sleep 1000 42)
 #(js/console.log "Success:" %)
 #(js/console.error "Error:" %))
```

**Event handling**:

```clojure
(defn clicks [element]
  (m/observe
    (fn [emit!]
      (let [handler #(emit! %)]
        (.addEventListener element "click" handler)
        #(.removeEventListener element "click" handler)))))

;; Throttled clicks
(def button-clicks
  (m/ap
    (let [click (m/?< (clicks (js/document.getElementById "btn")))]
      (try
        (m/? (m/sleep 300 click))
        (catch missionary.Cancelled _
          (m/amb))))))
```

### Key Platform Differences

| Feature | JVM (.clj) | JavaScript (.cljs) | Cross-platform (.cljc) |
|---------|------------|-------------------|------------------------|
| `m/?` at top level | ✅ Blocks thread | ❌ Not available | ❌ Not available |
| `m/via` | ✅ Full support | ❌ Not available | ❌ Not available |
| `m/cpu`, `m/blk` | ✅ Thread pools | ❌ Not available | ❌ Not available |
| `sp`, `ap`, `cp` | ✅ Works | ✅ Works | ✅ Works |
| `?`, `?>`, `?<`, `?=` | ✅ Works | ✅ Works | ✅ Works |
| Flow operators | ✅ Works | ✅ Works | ✅ Works |
| True parallelism | ✅ Multi-threaded | ❌ Event loop only | N/A |
| Blocking allowed | ✅ With `via` | ❌ Never | ❌ Never |

## Practical Production Patterns

### HTTP Client with Retry

```clojure
(require '[hato.client :as http])

(defn request [{:keys [url method] :as opts}]
  (m/sp
    (m/? (m/via m/blk
           (http/request (assoc opts :async? false))))))

(defn with-retry [task retries]
  (m/sp
    (loop [attempt 0]
      (let [result (m/? (m/attempt task))]
        (if (fn? result)
          (result)  ; success
          (if (< attempt retries)
            (do
              (m/? (m/sleep (* 1000 (Math/pow 2 attempt))))
              (recur (inc attempt)))
            (throw (result))))))))  ; final failure

;; Usage
(m/? (with-retry
       (request {:url "https://api.example.com" :method :get})
       3))
```

### Web Server with Ring

```clojure
(require '[ring.adapter.jetty :as jetty])

(defn async-handler [request]
  (m/sp
    (try
      (let [data (m/? (fetch-data-task (:params request)))]
        {:status 200
         :headers {"Content-Type" "application/json"}
         :body (json/encode data)})
      (catch Exception e
        {:status 500
         :body (str "Error: " (.getMessage e))}))))

;; Synchronous wrapper for Ring
(defn wrap-missionary [handler]
  (fn [request]
    (let [task (handler request)]
      (if (fn? task)  ; Is it a Missionary task?
        (let [result (promise)]
          (task
            (fn [response] (deliver result response))
            (fn [error] (deliver result 
                          {:status 500 :body (str error)})))
          @result)
        task))))  ; Not a task, return as-is

(def app
  (wrap-missionary async-handler))

(jetty/run-jetty app {:port 3000})
```

### Kafka Consumer

```clojure
(import '(org.apache.kafka.clients.consumer KafkaConsumer))

(defn poll [^KafkaConsumer consumer]
  (m/via m/blk
    (.poll consumer Long/MAX_VALUE)))

(defn forever [task]
  (m/ap
    (m/? (m/?> (m/seed (repeat task))))))

(defn consume-kafka [consumer process-fn]
  (->> (forever (poll consumer))
       (m/eduction (comp cat (map #(.value %))))
       (m/reduce process-fn nil)
       m/?))

;; Usage
(def consumer (create-kafka-consumer config))
(consume-kafka consumer
  (fn [state record]
    (println "Processing:" record)
    (updated-state state record)))
```

### Resource Management

```clojure
;; Pattern: Bracket resource acquisition/release
(defn with-resource [acquire release f]
  (m/sp
    (let [resource (m/? acquire)]
      (try
        (m/? (f resource))
        (finally
          (m/? release resource))))))

;; Database connection pool
(defn with-db-conn [pool f]
  (with-resource
    (m/via m/blk (get-connection pool))
    (fn [conn] (m/via m/blk (return-connection pool conn)))
    f))

;; Usage
(m/? (with-db-conn db-pool
       (fn [conn]
         (m/sp
           (m/? (query conn "SELECT * FROM users"))))))
```

### Error Recovery

```clojure
;; Supervisor that restarts failed tasks
(defn supervised [task max-restarts restart-delay]
  (m/sp
    (loop [restarts 0]
      (let [result (m/? (m/attempt task))]
        (if (fn? result)
          (result)  ; success
          (if (< restarts max-restarts)
            (do
              (println "Task failed, restarting" (inc restarts))
              (m/? (m/sleep restart-delay))
              (recur (inc restarts)))
            (throw (result))))))))  ; give up

;; Usage
(m/? (supervised
       (flaky-service-task)
       5     ; max 5 restarts
       2000)) ; 2 second delay
```

### Happy Eyeballs (RFC 8305)

Connect to multiple servers, use first success:

```clojure
(defn connect-with-delay [host port delay]
  (m/sp
    (when (pos? delay)
      (m/? (m/sleep delay)))
    (m/? (m/via m/blk
           (connect-socket host port)))))

(defn happy-eyeballs [hosts]
  (m/race
    (connect-with-delay (first hosts) 0)
    (connect-with-delay (second hosts) 250)  ; stagger by 250ms
    (connect-with-delay (nth hosts 2) 500)))

;; First successful connection wins, others cancelled
(m/? (happy-eyeballs ["frankfurt" "boston" "sydney"]))
```

## Architecture and Design Philosophy

### Computation Over Conveyance

Traditional async libraries (core.async, actors) focus on **conveyance**—communication devices like channels and mailboxes are first-class, processes coordinate through message passing. This requires **manual supervision** in user space.

Missionary focuses on **computation**—tasks and flows are first-class, functional composition creates a strict hierarchy. The runtime knows program structure and provides **automatic supervision**. When a child fails:
1. Error propagates to parent
2. Sibling tasks are cancelled
3. Parent waits for cleanup
4. Error bubbles up or is handled

This is **structured concurrency**—every spawned process has a supervisor, resources are cleaned up automatically.

```clojure
;; Automatic supervision example
(m/? (m/join vector
       (m/sp (println "Task 1") 42)
       (m/sp (throw (Exception. "Task 2 failed")))
       (m/sp (println "Task 3") 42)))

; Prints: "Task 1"
; Prints: "Task 3"
; Task 2 fails → Task 1 and Task 3 cancelled
; Exception propagates
; Guaranteed cleanup
```

### Continuous Time Flow Programming

Most streaming systems handle only **discrete events** (clicks, messages, records). Missionary unifies discrete and **continuous flows** (time-varying values like mouse position, account balance).

**The innovation**: Flows can signal "value ready" without eager computation. This enables:
- **Discrete flows**: Backpressured event streaming (each value significant)
- **Continuous flows**: Lazy sampling (intermediate values discarded)

**Glitch-free propagation**:

```clojure
;; Diamond dependency
(def !x (atom 1))
(let [<x (m/signal (m/watch !x))
      <sum (m/signal (m/latest + <x <x))]  ; sum = x + x
  
  (swap! !x inc)
  ; <x changes 1 → 2
  ; <sum recomputes ONCE as (+ 2 2) = 4
  ; The value 3 (old x + new x) is NEVER computed
  ; This is glitch-free!)
```

Each change propagates **atomically** through the dependency graph. No transient inconsistent states.

### The Process Macros

`sp`, `ap`, and `cp` are **compiler macros** that transform synchronous-looking code into asynchronous state machines:

```clojure
;; You write
(m/sp
  (let [x (m/? (task1))
        y (m/? (task2 x))]
    (+ x y)))

;; Missionary compiles to (conceptually):
;; State machine with suspension points at each ?
;; No thread blocked, execution resumes via callbacks
```

Similar to core.async's `go`, but supports:
- Function boundaries (parking can cross function calls)
- Full Clojure metaprogramming
- Forking operations (`?>`, `?<`, `?=`)
- Ambiguous choice (`amb`, `amb=`)

Built on **cloroutine**, a generic coroutine library for Clojure/Script.

### Cancellation and Resource Safety

**Automatic cancellation propagation**:
- Parent cancelled → all children cancelled
- Child fails → siblings cancelled, error propagates
- Downstream cancelled → upstream cancelled

**Idempotent cancellation**:

```clojure
(let [cancel! (task success-fn failure-fn)]
  (cancel!)  ; safe to call
  (cancel!)  ; safe to call again
  (cancel!)) ; no-op
```

**Exception model**:
- `missionary.Cancelled` extends `Throwable` (not `Exception`)
- Generally not caught by `(catch Exception e ...)`
- Indicates cancellation, not failure
- Propagates through `try`/`catch` unless explicitly caught

```clojure
(m/sp
  (try
    (m/? (task-that-might-be-cancelled))
    (catch Exception e
      ;; Cancellation still propagates!
      (handle-error e))
    (finally
      ;; Always runs, even if cancelled
      (cleanup))))
```

**Compel** - make uncancellable:

```clojure
;; Critical section runs to completion
(m/compel
  (m/sp
    (m/? (save-to-database important-data))
    (m/? (send-confirmation-email))))

;; Even if parent is cancelled, this finishes
```

## Comparative Analysis: When to Use Missionary

### vs core.async

**Use core.async when**:
- CSP model fits problem (producer/consumer with channels)
- Want Go-like programming style
- Need `alt!`/`alts!` selective operations
- Simpler mental model acceptable
- Maximum ClojureScript maturity required

**Use Missionary when**:
- Need structured concurrency with supervision
- Want functional composition over imperative channels
- Require FRP (continuous flows)
- Need backpressure in streaming
- Building reactive UIs (Electric Clojure)

**Key differences**:
- **core.async**: Channels first-class, manual supervision, discrete only
- **Missionary**: Computation first-class, automatic supervision, continuous+discrete

### vs Manifold

**Use Manifold when**:
- Using Aleph web server
- Need interop layer between async systems
- Prefer callback composition (`chain`)
- JVM-only acceptable

**Use Missionary when**:
- Need ClojureScript support
- Want DSL-based composition
- Require FRP capabilities
- Prefer functional over callbacks

### vs Promises/Async-Await

**Use Promises when**:
- Simple async sufficient
- Heavy JavaScript interop
- No cancellation needed
- Team familiar with JS patterns

**Use Missionary when**:
- Need cancellation
- Handling streams (multiple values)
- Need backpressure
- Want structured error handling

### Decision Matrix

| Requirement | Missionary | core.async | Manifold | Promises |
|-------------|-----------|-----------|----------|----------|
| Supervision | ✅ Built-in | ❌ Manual | ❌ Manual | ❌ None |
| Cancellation | ✅ Automatic | ⚠️ Manual | ⚠️ Manual | ❌ None |
| Backpressure | ✅ Built-in | ⚠️ Manual | ⚠️ Manual | ❌ None |
| FRP/Continuous | ✅ Yes | ❌ No | ❌ No | ❌ No |
| ClojureScript | ✅ Full | ✅ Full | ⚠️ Limited | ✅ Native |
| Learning curve | ⚠️ Steep | ⚠️ Moderate | ⚠️ Moderate | ✅ Easy |
| Maturity | ⚠️ Experimental | ✅ Stable | ✅ Stable | ✅ Stable |
| Community | ⚠️ Small | ✅ Large | ⚠️ Medium | ✅ Huge |

## Testing Strategies

### Cross-Platform Testing

```clojure
(ns app.core-test
  (:require
    #?(:clj  [clojure.test :refer [deftest is testing]]
       :cljs [cljs.test :refer-macros [deftest is testing]])
    [missionary.core :as m]))

(deftest task-completion-test
  (testing "Task completes with value"
    #?(:clj
       (is (= 42 (m/? (m/sleep 10 42))))
       
       :cljs
       (async done
         ((m/sleep 10 42)
          (fn [v] (is (= 42 v)) (done))
          (fn [e] (is false "Should not fail") (done)))))))

(deftest flow-reduction-test
  (is (= 15
         #?(:clj (m/? (m/reduce + (m/seed [1 2 3 4 5])))
            :cljs (atom nil))))  ; would need async wrapper
  
  ;; Better: use cross-platform helper
  #?(:clj
     (is (= 15 (m/? (m/reduce + (m/seed [1 2 3 4 5])))))))
```

### Error Handling Tests

```clojure
(deftest error-propagation-test
  (is (thrown? Exception
        (m/? (m/sp
               (throw (Exception. "Expected error")))))))

(deftest cancellation-test
  (let [cancelled? (atom false)
        task (m/sp
               (try
                 (m/? (m/sleep 5000))
                 (catch missionary.Cancelled _
                   (reset! cancelled? true))))]
    (let [cancel! (task identity identity)]
      (Thread/sleep 100)
      (cancel!)
      (Thread/sleep 100)
      (is @cancelled?))))
```

### Integration Testing

```clojure
(deftest http-integration-test
  (with-redefs [http/request (constantly (m/sp {:status 200 :body "ok"}))]
    (is (= "ok"
           (:body (m/? (api-call "/test")))))))

(deftest database-integration-test
  (with-test-db [db]
    (is (= [{:id 1 :name "Alice"}]
           (m/? (query-db db ["SELECT * FROM users WHERE id = ?" 1]))))))
```

### Property-Based Testing

```clojure
(require '[clojure.test.check.generators :as gen]
         '[clojure.test.check.properties :as prop]
         '[clojure.test.check.clojure-test :refer [defspec]])

(defspec parallel-map-preserves-order 100
  (prop/for-all [coll (gen/vector gen/int)]
    (= (map inc coll)
       (m/? (pmap-missionary #(m/sp (inc %)) coll 10)))))
```

## Common Pitfalls and Anti-Patterns

### ❌ Blocking inside `sp` without `via`

```clojure
;; BAD - blocks executor thread
(m/sp
  (Thread/sleep 1000)
  (slurp "file.txt"))

;; GOOD - offloads to blocking executor
(m/sp
  (m/? (m/via m/blk
         (Thread/sleep 1000)
         (slurp "file.txt"))))
```

### ❌ Using `m/?` at top level in ClojureScript

```clojure
;; BAD - doesn't exist in ClojureScript
(m/? some-task)

;; GOOD - wrap in Promise
(js/Promise.
  (fn [resolve reject]
    ((some-task) resolve reject)))
```

### ❌ Catching `missionary.Cancelled`

```clojure
;; BAD - breaks cancellation propagation
(m/sp
  (try
    (m/? task)
    (catch Throwable t
      (handle-error t))))  ; Catches Cancelled!

;; GOOD - only catch Exception
(m/sp
  (try
    (m/? task)
    (catch Exception e
      (handle-error e))))  ; Cancelled still propagates
```

### ❌ Unbounded recursion in flows

```clojure
;; BAD - blows stack
(defn infinite-range [n]
  (m/ap
    (m/amb n (m/?> (infinite-range (inc n))))))

;; GOOD - use explicit loop
(defn infinite-range [n]
  (m/ap
    (loop [i n]
      (m/amb i (recur (inc i))))))
```

### ❌ Not handling backpressure

```clojure
;; BAD - unbounded queue grows without limit
(let [queue (atom [])]
  (m/ap
    (swap! queue conj (m/?> fast-producer))
    (m/? (slow-consumer))))

;; GOOD - use backpressure or buffer
(m/ap
  (let [x (m/?> fast-producer)]  ; backpressure-passing
    (m/? (slow-consumer x))))

;; Or buffer with capacity
(m/buffer 100 fast-producer)
```

### ❌ Forgetting cleanup

```clojure
;; BAD - leak if exception before cleanup
(m/sp
  (let [conn (m/? (acquire-connection))]
    (m/? (use-connection conn))
    (m/? (release-connection conn))))  ; might not run

;; GOOD - use try/finally
(m/sp
  (let [conn (m/? (acquire-connection))]
    (try
      (m/? (use-connection conn))
      (finally
        (m/? (release-connection conn))))))
```

## Advanced Topics

### Publishers and Sharing

**Problem**: Multiple consumers of same flow recompute everything.

**Solution**: Publishers share computation across consumers.

```clojure
;; Without sharing - each consumer runs independent computation
(def expensive-flow
  (m/ap
    (let [x (m/?> (m/seed (range 100)))]
      (m/? (expensive-computation x)))))

(m/? (m/reduce conj [] expensive-flow))  ; computes
(m/? (m/reduce + expensive-flow))        ; computes again!

;; With sharing - computation runs once
(def shared-flow
  (m/stream
    (m/ap
      (let [x (m/?> (m/seed (range 100)))]
        (m/? (expensive-computation x))))))

(m/? (m/reduce conj [] (shared-flow)))  ; computes
(m/? (m/reduce + (shared-flow)))        ; reuses results!
```

**Publisher types**:
- `m/memo` - Memoize task result
- `m/signal` - Continuous flow (latest value cached)
- `m/stream` - Discrete flow (buffers for consumers)

### Reactive Dataflow Graphs

```clojure
;; Build complex reactive computation
(def !a (atom 1))
(def !b (atom 2))

(let [<a (m/signal (m/watch !a))
      <b (m/signal (m/watch !b))
      <sum (m/signal (m/latest + <a <b))
      <product (m/signal (m/latest * <a <b))
      <result (m/signal (m/latest 
                          (fn [sum prod] {:sum sum :product prod})
                          <sum <product))]
  
  ;; Consume reactive result
  (m/reduce
    (fn [_ x] (println "Result:" x))
    nil
    <result))
```

**Glitch-free guarantee**: Even with diamond dependencies, each change propagates atomically—no transient inconsistencies.

### Lazy vs Eager Evaluation

**Discrete flows** (created with `ap` using `?>`): **Eager push**
- Values produced and pushed to consumer
- Backpressure when consumer not ready
- Consumer pull = upstream production

**Continuous flows** (created with `cp` using `?<`): **Lazy pull**
- Values only computed when sampled
- No backpressure (sampling is instant)
- Consumer controls evaluation timing

```clojure
;; Eager discrete
(m/ap
  (let [x (m/?> (m/seed [1 2 3]))]
    (println "Computing" x)  ; prints immediately
    (* x x)))

;; Lazy continuous
(m/cp
  (let [x (m/?< (m/watch !state))]
    (println "Computing" x)  ; only when sampled
    (* x x)))
```

### DAGs and Cycles

**DAG** (Directed Acyclic Graph) - Pure functional dataflow:

```clojure
(let [<input (m/signal (m/watch !input))
      <derived1 (m/signal (m/latest f <input))
      <derived2 (m/signal (m/latest g <input))
      <result (m/signal (m/latest h <derived1 <derived2))]
  <result)
```

**Cycle** - Requires explicit state (use atoms/refs):

```clojure
;; Feedback loop: output affects next input
(defn feedback-loop [init f]
  (let [!state (atom init)
        <state (m/signal (m/watch !state))]
    (m/reduce
      (fn [_ x]
        (reset! !state (f x))
        x)
      nil
      <state)))
```

Most reactive programs should use DAGs. Cycles introduce local state and should be used sparingly.

## Real-World Example: Complete HTTP API Client

```clojure
(ns api-client
  (:require [missionary.core :as m]
            [hato.client :as http]
            [cheshire.core :as json]))

;; Base HTTP client with retry and circuit breaker
(defn request [opts]
  (m/via m/blk
    (http/request opts)))

(defn with-retry [task max-attempts initial-delay]
  (m/sp
    (loop [attempt 0
           delay initial-delay]
      (if (>= attempt max-attempts)
        (throw (ex-info "Max retries exceeded" {:attempts attempt}))
        (let [result (m/? (m/attempt task))]
          (if (fn? result)
            (result)
            (do
              (println "Retry" (inc attempt) "after" delay "ms")
              (m/? (m/sleep delay))
              (recur (inc attempt) (* delay 2)))))))))

(def circuit-breaker-state (atom {:failures 0 :state :closed}))

(defn with-circuit-breaker [task threshold timeout]
  (m/sp
    (let [{:keys [state failures]} @circuit-breaker-state]
      (case state
        :open (throw (ex-info "Circuit breaker open" {}))
        (:closed :half-open)
        (try
          (let [result (m/? task)]
            (swap! circuit-breaker-state assoc :failures 0 :state :closed)
            result)
          (catch Exception e
            (let [new-failures (inc failures)]
              (when (>= new-failures threshold)
                (swap! circuit-breaker-state assoc :state :open))
              (swap! circuit-breaker-state assoc :failures new-failures)
              (throw e))))))))

;; API client with all features
(defn api-call [endpoint opts]
  (with-circuit-breaker
    (with-retry
      (request (merge {:url (str "https://api.example.com" endpoint)
                       :method :get
                       :as :json}
                      opts))
      3 1000)
    5 60000))

;; Paginated resource fetching
(defn fetch-pages [endpoint]
  (m/ap
    (loop [url endpoint]
      (let [{:keys [data next_page]} (m/? (api-call url {}))]
        (if next_page
          (m/amb data (recur next_page))
          data)))))

;; Parallel batch fetching
(defn fetch-users [user-ids parallelism]
  (m/? (m/reduce conj []
         (m/ap
           (let [id (m/?> parallelism (m/seed user-ids))]
             (m/? (api-call (str "/users/" id) {})))))))

;; Usage
(comment
  ;; Fetch single user
  (m/? (api-call "/users/123" {}))
  
  ;; Fetch all pages
  (m/? (m/reduce into [] (fetch-pages "/posts")))
  
  ;; Fetch users in parallel (max 5 concurrent)
  (fetch-users [1 2 3 4 5 6 7 8 9 10] 5))
```

## Key Takeaways for Claude Code

**1. Missionary = Structured Concurrency**
- Parent supervises children automatically
- Errors propagate up, cancellation down
- Resources cleaned up automatically
- No manual coordination needed

**2. Two Abstractions Cover Everything**
- **Tasks** (`sp`, `?`) - single async values
- **Flows** (`ap`, `?>`, `?<`, `?=`) - streams
- Both compose functionally with same patterns

**3. Platform Awareness Required**
- JVM: Use `via` for blocking ops, `m/?` works at REPL
- JavaScript: No blocking ever, no top-level `m/?`
- Cross-platform: Stick to `sp`/`ap`, avoid `via`

**4. Backpressure is Default**
- `?>` passes backpressure (safe, controlled)
- `?<` relieves backpressure (drops work)
- Choose based on whether intermediate values matter

**5. Error Handling is Automatic**
- Exceptions propagate to parent
- Siblings cancelled on failure
- Use `try`/`catch` for recovery
- Never catch `missionary.Cancelled`

**6. Testing Strategy**
- JVM: Block with `m/?` in tests
- ClojureScript: Use `async done` pattern
- Write `.cljc` tests with reader conditionals
- Mock I/O, test business logic

**7. Production Patterns**
- Retry with exponential backoff
- Circuit breakers for external services
- Resource bracketing with `try`/`finally`
- Pagination with `loop` in `ap`
- Parallel processing with `?> parallelism`

**8. When to Use Missionary**
- Building reactive UIs (Electric Clojure)
- Need FRP (continuous flows)
- Want automatic supervision
- Complex async orchestration
- Willing to invest in learning

**9. Learn by Example**
Start with simple tasks, progress to flows, then reactive patterns:
- Tasks: `sp`, `?`, `join`, `race`
- Discrete flows: `ap`, `?>`, `seed`, `reduce`
- Continuous flows: `watch`, `latest`, `signal`
- Production: Add retry, circuit breakers, resource management

**10. Key Resources**
- GitHub: leonoel/missionary
- Tutorial: "Missionary for Dummies" (nextjournal.com)
- Real usage: Electric Clojure (hyperfiddle/electric)
- Community: #missionary on Clojurians Slack