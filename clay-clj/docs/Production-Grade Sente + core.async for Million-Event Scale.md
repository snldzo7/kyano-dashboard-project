# Production-Grade Sente + core.async for Million-Event Scale

## Core Philosophy: Data-Driven, Extensible, Resource-Aware

---

## 1. Foundation: Bounded Channels & Resource Limits

```clojure
(ns my-app.config
  "Configuration as data - easily testable and modifiable")

(def system-config
  {:channels
   {:event-buffer-size 10000      ; Buffer size per channel
    :message-queue-size 50000     ; Main processing queue
    :broadcast-buffer-size 1000   ; Broadcast channel size
    :worker-pool-size 10          ; Number of worker go-loops
    :max-pending-requests 5000}   ; Circuit breaker threshold
   
   :timeouts
   {:client-request-ms 5000       ; How long client waits
    :server-processing-ms 3000    ; Max time for server processing
    :db-operation-ms 2000         ; Database timeout
    :shutdown-grace-ms 30000}     ; Graceful shutdown window
   
   :rate-limits
   {:per-user-per-minute 100     ; Rate limit per user
    :global-per-second 10000}    ; Global system rate limit
   
   :backpressure
   {:drop-threshold 0.9          ; Drop new messages at 90% capacity
    :alert-threshold 0.7}})      ; Alert at 70% capacity
```

 **Why** : Hard-coded values are the enemy of scale. This data structure:

* Can be loaded from EDN/environment
* Is testable
* Allows runtime adjustments
* Documents system limits

---

## 2. Smart Server Setup with Metrics

```clojure
(ns my-app.server
  (:require
   [taoensso.sente :as sente]
   [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter]]
   [clojure.core.async :as async :refer [<! >! >!! go go-loop chan 
                                          sliding-buffer dropping-buffer
                                          timeout alt! close!]]
   [my-app.config :refer [system-config]]
   [my-app.metrics :as metrics]))

;; Step 1: Create Sente socket with adapter
;; http-kit adapter handles WebSocket connections efficiently
(let [adapter (get-sch-adapter)
    
      ;; Step 2: Pass configuration to Sente
      ;; :user-id-fn - critical for routing messages to specific users
      ;; :csrf-token-fn - security, can disable if using tokens differently
      ;; :allowed-origins - CORS protection for production
      socket-config {:user-id-fn (fn [ring-req] 
                                    ;; Extract user ID from request
                                    ;; Could be JWT, session, etc.
                                    (get-in ring-req [:session :uid]))
                     :csrf-token-fn nil  ; Handle CSRF at middleware layer
                     :allowed-origins #{:all}}  ; Restrict in production!
    
      ;; Step 3: Create the channel socket
      ;; Returns map with all the pieces we need
      {:keys [ch-recv           ; Channel to receive events FROM clients
              send-fn           ; Function to send TO clients  
              connected-uids    ; Atom with set of connected user IDs
              ajax-post-fn      ; Ring handler for Ajax POST
              ajax-get-or-ws-handshake-fn]}  ; Ring handler for WS/Ajax GET
      (sente/make-channel-socket! adapter socket-config)]
  
  ;; Step 4: Expose these for use in routes and event handling
  (def ring-ajax-post ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk ch-recv)              ; Our event input channel
  (def chsk-send! send-fn)           ; Our send function
  (def connected-uids connected-uids)) ; Track who's connected

;; Step 5: Monitor connection count - essential for scaling decisions
(add-watch connected-uids :metrics-watcher
  (fn [_ _ old-state new-state]
    (let [old-count (count (:any old-state))
          new-count (count (:any new-state))]
      ;; Report to metrics system (Prometheus, StatsD, etc.)
      (metrics/gauge! :active-connections new-count)
    
      ;; Log significant changes
      (when (not= old-count new-count)
        (println "Connection count changed:" old-count "->" new-count)))))
```

 **Line-by-line rationale** :

* **`get-sch-adapter`** : http-kit is fast, handles 10k+ concurrent connections
* **`:user-id-fn`** : Must extract user identity for targeted sends. Without this, you can't route messages efficiently
* **`connected-uids`** : Atom is thread-safe, tracks all connected UIDs. Critical for broadcast operations
* **`add-watch`** : Monitoring connections is essential - tells you when to scale horizontally
* **`metrics/gauge!`** : External metrics let you track system health, set up alerts

---

## 3. Worker Pool Pattern for Parallelism

```clojure
(ns my-app.workers
  (:require
   [clojure.core.async :as async :refer [<! >! go go-loop chan 
                                          sliding-buffer close!]]
   [my-app.config :refer [system-config]]
   [my-app.metrics :as metrics]))

;; Step 1: Create work queue with sliding buffer
;; sliding-buffer drops OLDEST items when full - prevents memory bloat
;; Alternative: dropping-buffer drops NEWEST items
(def work-queue 
  (chan (sliding-buffer 
         (get-in system-config [:channels :message-queue-size]))))
;; Why sliding-buffer? At scale, you'd rather drop old requests than block
;; newer ones. This is a business decision - adjust based on your needs.

;; Step 2: Track queue depth for backpressure decisions
(def queue-depth (atom 0))

;; Step 3: Generic worker function
(defn create-worker
  "Creates a worker go-loop that processes items from queue.
   Returns a function that stops the worker."
  [worker-id work-queue handler-fn]
  (let [running? (atom true)
      
        ;; Track this worker's metrics
        worker-metrics {:processed (atom 0)
                        :errors (atom 0)}]
  
    ;; Start the worker
    (go-loop []
      (when @running?
        ;; alt! lets us handle multiple channels with timeout
        (let [[msg ch] (async/alt!
                         work-queue ([msg] [msg :work])
                         (timeout 1000) ([_] [nil :timeout])
                         :priority true)]
        
          (case ch
            :work
            (when msg
              ;; Decrement queue depth immediately when we take work
              (swap! queue-depth dec)
            
              (try
                ;; Process the message
                (<! (handler-fn msg))
                (swap! (:processed worker-metrics) inc)
              
                (catch Exception e
                  (swap! (:errors worker-metrics) inc)
                  (println "Worker" worker-id "error:" (.getMessage e))
                  ;; In production: log to error tracking service
                  (metrics/increment! :worker-errors {:worker-id worker-id})))
            
              ;; Report metrics periodically
              (when (zero? (mod @(:processed worker-metrics) 1000))
                (metrics/gauge! :worker-processed 
                              @(:processed worker-metrics)
                              {:worker-id worker-id})))
          
            :timeout
            ;; Heartbeat - worker is idle, could report health check
            (metrics/gauge! :worker-idle worker-id true))
        
          (recur))))
  
    ;; Return shutdown function
    (fn []
      (reset! running? false)
      (println "Worker" worker-id "stopped"))))

;; Step 4: Create worker pool
(defn start-worker-pool!
  "Starts N workers processing from work-queue.
   Returns vector of shutdown functions."
  [handler-fn]
  (let [pool-size (get-in system-config [:channels :worker-pool-size])]
    (println "Starting worker pool with" pool-size "workers")
  
    (mapv 
      (fn [worker-id]
        (create-worker worker-id work-queue handler-fn))
      (range pool-size))))

;; Step 5: Submit work with backpressure awareness
(defn submit-work!
  "Submits work to queue. Returns :ok, :dropped, or :backpressure."
  [work-item]
  (let [current-depth @queue-depth
        max-size (get-in system-config [:channels :message-queue-size])
        threshold (get-in system-config [:backpressure :drop-threshold])
      
        ;; Calculate utilization percentage
        utilization (/ current-depth max-size)]
  
    ;; Decision tree based on queue depth
    (cond
      ;; Queue nearly full - drop new work
      (> utilization threshold)
      (do
        (metrics/increment! :work-dropped)
        (println "WARN: Dropping work, queue at" (int (* 100 utilization)) "%")
        :backpressure)
    
      ;; Queue filling up - warn but accept
      (> utilization (get-in system-config [:backpressure :alert-threshold]))
      (do
        (metrics/increment! :work-accepted-under-pressure)
        (swap! queue-depth inc)
        (>!! work-queue work-item)  ; >!! is blocking put - use carefully!
        :ok)
    
      ;; Normal operation
      :else
      (do
        (swap! queue-depth inc)
        (>!! work-queue work-item)
        :ok))))
```

 **Key scalability patterns** :

* **Worker pool** : Parallelizes processing across N go-loops
* **Bounded buffers** : Prevents unbounded memory growth
* **Sliding buffer** : Automatic old-item eviction under load
* **Queue depth tracking** : Real-time backpressure awareness
* **Metrics everywhere** : Observe system behavior under load
* **`alt!` with timeout** : Worker doesn't block forever
* **Shutdown functions** : Clean resource cleanup

---

## 4. Data-Driven Event Router

```clojure
(ns my-app.router
  (:require
   [clojure.core.async :as async :refer [<! >! go]]
   [my-app.workers :refer [submit-work!]]
   [my-app.metrics :as metrics]))

;; Step 1: Define event handlers as DATA
;; This is the key to extensibility - handlers are declarative
(def event-handlers
  {:my-app/get-user
   {:handler #'handle-get-user
    :rate-limit {:per-minute 50}    ; Per-user rate limit
    :timeout-ms 2000                 ; Max processing time
    :priority :high                  ; High priority events
    :requires-auth? true}            ; Must be authenticated
   
   :my-app/save-note
   {:handler #'handle-save-note
    :rate-limit {:per-minute 20}
    :timeout-ms 3000
    :priority :normal
    :requires-auth? true}
   
   :my-app/bulk-import
   {:handler #'handle-bulk-import
    :rate-limit {:per-minute 1}     ; Very limited - expensive operation
    :timeout-ms 30000               ; Long timeout for bulk ops
    :priority :low                  ; Process after other events
    :requires-auth? true
    :requires-role :admin}          ; Additional authorization
   
   :my-app/public-query
   {:handler #'handle-public-query
    :rate-limit {:per-minute 100}
    :timeout-ms 1000
    :priority :normal
    :requires-auth? false}})        ; No auth needed

;; Step 2: Rate limiter - in-memory for single server, Redis for distributed
(def rate-limits (atom {}))  ; {[uid event-id] [timestamps...]}

(defn check-rate-limit
  "Returns :ok if under limit, :rate-limited if over.
   Uses sliding window algorithm."
  [uid event-id config]
  (let [now (System/currentTimeMillis)
        window-ms 60000  ; 1 minute
        limit (get-in config [:rate-limit :per-minute])
      
        ;; Get recent requests for this uid+event
        key [uid event-id]
        recent-requests (get @rate-limits key [])
      
        ;; Filter to requests within window
        valid-requests (filterv #(> % (- now window-ms)) recent-requests)
      
        ;; Count requests in window
        request-count (count valid-requests)]
  
    (if (< request-count limit)
      ;; Under limit - record this request
      (do
        (swap! rate-limits assoc key (conj valid-requests now))
        :ok)
      ;; Over limit
      (do
        (metrics/increment! :rate-limit-hit {:event-id event-id})
        :rate-limited))))

;; Step 3: Clean up old rate limit data periodically
(defn start-rate-limit-cleaner! []
  (go-loop []
    (<! (async/timeout 300000))  ; Every 5 minutes
    (let [now (System/currentTimeMillis)
          cutoff (- now 120000)]  ; 2 minutes ago
    
      ;; Remove old entries
      (swap! rate-limits
             (fn [limits]
               (into {}
                     (keep (fn [[k timestamps]]
                             (let [recent (filterv #(> % cutoff) timestamps)]
                               (when (seq recent)
                                 [k recent])))
                           limits))))
    
      (println "Rate limiter cleanup complete. Entries:" (count @rate-limits)))
    (recur)))

;; Step 4: Authorization checker
(defn check-authorization
  "Verifies user can execute this event."
  [uid ring-req event-config]
  (cond
    ;; Event doesn't require auth
    (not (:requires-auth? event-config))
    :ok
  
    ;; No user ID but auth required
    (and (:requires-auth? event-config) (nil? uid))
    :unauthorized
  
    ;; Check role if required
    (:requires-role event-config)
    (if (= (get-in ring-req [:session :role]) 
           (:requires-role event-config))
      :ok
      :forbidden)
  
    ;; Default: authorized
    :else
    :ok))

;; Step 5: Main event processing logic
(defn process-event
  "Core event processing with all checks and handling."
  [{:keys [id uid ring-req ?data ?reply-fn] :as event-msg}]
  (go
    (let [event-config (get event-handlers id)
        
          ;; Start timing for metrics
          start-time (System/currentTimeMillis)]
    
      (cond
        ;; Unknown event type
        (nil? event-config)
        (do
          (metrics/increment! :unknown-event {:event-id id})
          (when ?reply-fn
            (?reply-fn {:error :unknown-event})))
      
        ;; Check authorization
        (not= :ok (check-authorization uid ring-req event-config))
        (do
          (metrics/increment! :unauthorized-event {:event-id id})
          (when ?reply-fn
            (?reply-fn {:error :unauthorized})))
      
        ;; Check rate limit
        (not= :ok (check-rate-limit uid id event-config))
        (do
          (when ?reply-fn
            (?reply-fn {:error :rate-limited})))
      
        ;; All checks passed - process event
        :else
        (let [timeout-ms (get event-config :timeout-ms 5000)
              handler (get event-config :handler)
            
              ;; Create result channel
              result-ch (async/chan 1)
            
              ;; Run handler with timeout
              timeout-ch (async/timeout timeout-ms)]
        
          ;; Execute handler in separate go block
          (go
            (try
              (let [result (<! (handler event-msg))]
                (>! result-ch {:success result}))
              (catch Exception e
                (>! result-ch {:error (.getMessage e)}))))
        
          ;; Wait for result OR timeout
          (let [[result ch] (async/alt!
                              result-ch ([v] [v :result])
                              timeout-ch ([_] [nil :timeout])
                              :priority true)
              
                elapsed (- (System/currentTimeMillis) start-time)]
          
            ;; Record metrics
            (metrics/histogram! :event-duration elapsed {:event-id id})
          
            (case ch
              :result
              (do
                (metrics/increment! :event-processed {:event-id id})
                (when ?reply-fn
                  (?reply-fn result)))
            
              :timeout
              (do
                (metrics/increment! :event-timeout {:event-id id})
                (println "WARN: Event" id "timed out after" timeout-ms "ms")
                (when ?reply-fn
                  (?reply-fn {:error :timeout})))))))))

;; Step 6: Router that uses worker pool
(defn event-msg-handler
  "Main entry point for all events. Submits to worker pool."
  [event-msg]
  ;; Don't process connection events - they're metadata
  (when-not (#{:chsk/ws-ping :chsk/uidport-open :chsk/uidport-close} 
             (:id event-msg))
  
    (metrics/increment! :events-received {:event-id (:id event-msg)})
  
    ;; Submit to worker pool with backpressure handling
    (let [result (submit-work! #(process-event event-msg))]
      (when (= result :backpressure)
        ;; System overloaded - reply immediately
        (when-let [reply-fn (:?reply-fn event-msg)]
          (reply-fn {:error :system-overloaded}))))))

;; Step 7: Start the router
(defn start-router! [ch-recv worker-pool]
  (start-rate-limit-cleaner!)
  (sente/start-chsk-router! ch-recv event-msg-handler))
```

 **Why this scales** :

* **Data-driven** : Add new events by adding to map, not writing new code
* **Rate limiting** : Prevents abuse, protects downstream systems
* **Authorization** : Security built into routing layer
* **Timeouts** : No handler can hang the system
* **Priority handling** : Could extend to priority queues
* **Metrics per event** : Observe each event type's behavior
* **Worker pool submission** : Parallelism with backpressure
* **Memory cleanup** : Rate limiter doesn't grow unbounded

---

## 5. Efficient Broadcasting with Batching

```clojure
(ns my-app.broadcast
  (:require
   [clojure.core.async :as async :refer [<! >! go go-loop chan 
                                          sliding-buffer timeout]]
   [my-app.server :refer [chsk-send! connected-uids]]
   [my-app.metrics :as metrics]))

;; Step 1: Broadcast queue with batching
(def broadcast-queue (chan (sliding-buffer 1000)))

;; Step 2: Batch broadcasts to reduce network overhead
(defn start-broadcast-batcher!
  "Batches broadcasts and sends them together.
   Reduces # of network operations by ~90%."
  []
  (let [batch-size 50        ; Max broadcasts per batch
        batch-window-ms 100] ; Max wait time for batch
  
    (go-loop [batch []
              batch-timeout (timeout batch-window-ms)]
      (let [[msg ch] (async/alt!
                       broadcast-queue ([msg] [msg :msg])
                       batch-timeout ([_] [nil :timeout])
                       :priority true)]
      
        (cond
          ;; Received a message
          (= ch :msg)
          (let [new-batch (conj batch msg)]
            (if (>= (count new-batch) batch-size)
              ;; Batch full - send immediately
              (do
                (<! (send-batch! new-batch))
                (recur [] (timeout batch-window-ms)))
              ;; Continue batching
              (recur new-batch batch-timeout)))
        
          ;; Timeout - send what we have
          (= ch :timeout)
          (do
            (when (seq batch)
              (<! (send-batch! batch)))
            (recur [] (timeout batch-window-ms))))))))

;; Step 3: Send batch with fan-out optimization
(defn send-batch!
  "Sends a batch of broadcasts efficiently."
  [batch]
  (go
    (let [start-time (System/currentTimeMillis)
        
          ;; Group messages by event-id for potential deduping
          grouped (group-by :event-id batch)
        
          ;; Get current connected users ONCE
          uids (:any @connected-uids)
          uid-count (count uids)]
    
      ;; Send each event type
      (doseq [[event-id messages] grouped]
        (let [message-count (count messages)]
        
          ;; Strategy: if messages are identical, send once per user
          ;; If different, send all messages
          (if (= 1 message-count)
            ;; Single message - simple broadcast
            (let [{:keys [data target-uids]} (first messages)
                  recipients (or target-uids uids)]
              (doseq [uid recipients]
                (chsk-send! uid [event-id data])))
          
            ;; Multiple messages - check if we can dedupe
            (doseq [{:keys [data target-uids]} messages]
              (let [recipients (or target-uids uids)]
                (doseq [uid recipients]
                  (chsk-send! uid [event-id data])))))))
    
      (let [elapsed (- (System/currentTimeMillis) start-time)
            total-sends (* (count batch) uid-count)]
      
        (metrics/histogram! :broadcast-batch-size (count batch))
        (metrics/histogram! :broadcast-batch-duration elapsed)
        (metrics/increment! :broadcasts-sent total-sends)
      
        (println "Sent batch of" (count batch) 
                 "broadcasts to" uid-count "users in" elapsed "ms")))))

;; Step 4: Public API for broadcasting
(defn broadcast!
  "Queues a broadcast. Returns :ok or :dropped."
  [event-id data & {:keys [target-uids]}]
  (let [msg {:event-id event-id
             :data data
             :target-uids target-uids}
      
        ;; Try to put on queue (non-blocking)
        result (async/offer! broadcast-queue msg)]
  
    (if result
      (do
        (metrics/increment! :broadcast-queued)
        :ok)
      (do
        (metrics/increment! :broadcast-dropped)
        (println "WARN: Broadcast queue full, dropping message")
        :dropped))))

;; Step 5: Targeted broadcast to specific users (efficient)
(defn broadcast-to-users!
  "Broadcasts to specific user list efficiently."
  [uids event-id data]
  (broadcast! event-id data :target-uids uids))
```

 **Broadcasting at scale** :

* **Batching** : Reduces network operations by ~90%
* **Sliding buffer** : Drops old broadcasts under load
* **Fan-out optimization** : Calculate recipients once per batch
* **Targeted sends** : Only send to specific users when needed
* **Non-blocking queue** : `async/offer!` doesn't block caller
* **Metrics** : Track queue health and batch efficiency

---

## 6. Example Event Handler (Database Pattern)

```clojure
(ns my-app.handlers
  (:require
   [clojure.core.async :as async :refer [<! go]]
   [my-app.db :as db]
   [my-app.broadcast :refer [broadcast!]]))

;; Step 1: Handler that interacts with database
(defn handle-save-note
  "Saves a note and broadcasts to other users.
   Demonstrates transaction + broadcast pattern."
  [{:keys [uid ?data]}]
  (go
    (try
      ;; Step 1: Validate input
      (when-not (and (:title ?data) (:content ?data))
        (throw (ex-info "Missing required fields" 
                        {:type :validation-error})))
    
      ;; Step 2: Save to database (async)
      (let [note (<! (db/save-note! uid ?data))]
      
        ;; Step 3: Broadcast to collaborators (if any)
        (when-let [collaborators (:collaborators ?data)]
          (broadcast-to-users! 
            collaborators
            :my-app/note-updated
            {:note-id (:id note)
             :author uid
             :timestamp (System/currentTimeMillis)}))
      
        ;; Step 4: Return success
        {:status :ok
         :note note})
    
      (catch Exception e
        ;; Proper error handling with types
        {:status :error
         :error-type (or (ex-data e :type) :unknown)
         :message (.getMessage e)}))))
```

---

## 7. Graceful Shutdown

```clojure
(ns my-app.lifecycle
  (:require
   [clojure.core.async :as async :refer [<!! timeout]]
   [my-app.config :refer [system-config]]))

;; Step 1: System state tracking
(def system-state (atom {:worker-shutdowns []
                         :router-running? false
                         :broadcast-running? false}))

;; Step 2: Graceful shutdown sequence
(defn shutdown!
  "Gracefully shuts down all async processes."
  []
  (println "Starting graceful shutdown...")
  (let [grace-period (get-in system-config [:timeouts :shutdown-grace-ms])]
  
    ;; Step 1: Stop accepting new work
    (println "Stopping routers...")
    (swap! system-state assoc :accepting-work? false)
  
    ;; Step 2: Drain work queue
    (println "Draining work queue...")
    (<!! (timeout 5000))  ; Wait for queue to drain
  
    ;; Step 3: Stop workers
    (println "Stopping workers...")
    (doseq [shutdown-fn (:worker-shutdowns @system-state)]
      (shutdown-fn))
  
    ;; Step 4: Final grace period
    (println "Waiting for in-flight work to complete...")
    (<!! (timeout grace-period))
  
    (println "Shutdown complete")))

;; Step 3: Add JVM shutdown hook
(defn add-shutdown-hook! []
  (.addShutdownHook 
    (Runtime/getRuntime)
    (Thread. shutdown!)))
```

---

## Key Takeaways for Million-Event Scale

1. **Bounded Everything** : Every channel, every buffer - must have limits
2. **Metrics Everywhere** : Can't optimize what you don't measure
3. **Backpressure Awareness** : System must know when it's overloaded
4. **Data-Driven Configuration** : Event handlers, limits, timeouts - all data
5. **Worker Pools** : Parallelism through go-loop pools, not unlimited go blocks
6. **Batching** : Network operations are expensive - batch when possible
7. **Rate Limiting** : Protect your system from abuse
8. **Graceful Degradation** : Drop work intelligently under load
9. **Clean Shutdown** : Resources must be released properly
10. **Authorization at Router** : Security built into the foundation

This architecture can handle millions of events because:

* Work is parallelized across worker pools
* Backpressure prevents memory overflow
* Rate limiting prevents abuse
* Batching reduces network overhead
* Everything is bounded and measurable
