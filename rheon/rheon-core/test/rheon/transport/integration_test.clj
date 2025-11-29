(ns rheon.transport.integration-test
  "Integration tests for WebSocket client-server communication.

   Tests actual communication between ws-server and ws-client transports,
   including advanced Missionary flow compositions."
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [rheon.core :as r]
            [rheon.protocols :as p]
            [rheon.transport.ws-server :as ws-server]
            [rheon.transport.ws-client :as ws-client]
            [missionary.core :as m])
  (:import [java.util.concurrent CountDownLatch TimeUnit]))

;; =============================================================================
;; Test Helpers
;; =============================================================================

(def ^:dynamic *test-port* 19080)

(defn next-port []
  (let [port *test-port*]
    (alter-var-root #'*test-port* inc)
    port))

(defn wait-for-connection
  "Wait for client to connect. Returns true if connected within timeout."
  [client-state timeout-ms]
  (let [start (System/currentTimeMillis)]
    (loop []
      (if (:connected? @client-state)
        true
        (if (> (- (System/currentTimeMillis) start) timeout-ms)
          false
          (do (Thread/sleep 10) (recur)))))))

;; =============================================================================
;; Basic Connectivity Tests
;; =============================================================================

(deftest client-server-connect-test
  (testing "Client connects to server successfully"
    (let [port (next-port)
          connected? (promise)
          server (r/connection {:transport :ws-server :port port})
          client (ws-client/connection {:url (str "ws://localhost:" port)
                                        :on-connect #(deliver connected? true)
                                        :auto-reconnect? false})]
      (try
        (is (deref connected? 3000 false) "Client should connect within 3 seconds")
        (is (:connected? @(:state client)) "Client state should show connected")
        (finally
          (r/close! client)
          (r/close! server))))))

;; =============================================================================
;; Stream Wire Integration Tests
;; =============================================================================

(deftest stream-server-to-client-test
  (testing "Server stream emissions reach client"
    (let [port (next-port)
          server (r/connection {:transport :ws-server :port port})
          mouse-server (r/stream :mouse server)
          received (atom [])
          latch (CountDownLatch. 2)
          connected? (promise)
          client (ws-client/connection {:url (str "ws://localhost:" port)
                                        :on-connect #(deliver connected? true)
                                        :auto-reconnect? false})]
      (try
        ;; Wait for connection
        (is (deref connected? 3000 false))
        ;; Get client-side wire and listen
        (let [mouse-client (r/stream :mouse client)]
          (r/listen mouse-client (fn [data]
                                   (swap! received conj data)
                                   (.countDown latch))))
        ;; Small delay for listener setup
        (Thread/sleep 100)
        ;; Emit from server
        (r/emit! mouse-server {:x 100 :y 200})
        (r/emit! mouse-server {:x 300 :y 400})
        ;; Wait for messages
        (is (.await latch 2 TimeUnit/SECONDS) "Should receive 2 messages")
        ;; Verify data
        (is (= 2 (count @received)))
        (is (= {:x 100 :y 200} (dissoc (first @received) :rheon/seq)))
        (is (= {:x 300 :y 400} (dissoc (second @received) :rheon/seq)))
        (finally
          (r/close! client)
          (r/close! server))))))

(deftest stream-client-to-server-test
  (testing "Client stream emissions reach server listeners"
    (let [port (next-port)
          server (r/connection {:transport :ws-server :port port})
          mouse-server (r/stream :mouse server)
          received (atom [])
          latch (CountDownLatch. 2)
          connected? (promise)
          client (ws-client/connection {:url (str "ws://localhost:" port)
                                        :on-connect #(deliver connected? true)
                                        :auto-reconnect? false})]
      (try
        ;; Listen on server
        (r/listen mouse-server (fn [data]
                                 (swap! received conj data)
                                 (.countDown latch)))
        ;; Wait for connection
        (is (deref connected? 3000 false))
        ;; Get client-side wire and emit
        (let [mouse-client (r/stream :mouse client)]
          (Thread/sleep 100)
          (r/emit! mouse-client {:x 50 :y 75})
          (r/emit! mouse-client {:x 150 :y 175}))
        ;; Wait for messages
        (is (.await latch 2 TimeUnit/SECONDS) "Server should receive 2 messages")
        ;; Verify data
        (is (= 2 (count @received)))
        (is (= {:x 50 :y 75} (dissoc (first @received) :rheon/seq)))
        (finally
          (r/close! client)
          (r/close! server))))))

;; =============================================================================
;; Discrete Wire Integration Tests
;; =============================================================================

(deftest discrete-request-response-test
  (testing "Client can send request and receive reply from server"
    (let [port (next-port)
          server (r/connection {:transport :ws-server :port port})
          clock-server (r/discrete :clock server)
          reply-received (promise)
          connected? (promise)
          client (ws-client/connection {:url (str "ws://localhost:" port)
                                        :on-connect #(deliver connected? true)
                                        :auto-reconnect? false})]
      (try
        ;; Set up handler on server
        (r/reply! clock-server (fn [data]
                                 {:server-time (System/currentTimeMillis)
                                  :client-data data}))
        ;; Wait for connection
        (is (deref connected? 3000 false))
        ;; Send request from client
        (let [clock-client (r/discrete :clock client)]
          (Thread/sleep 100)
          (r/send! clock-client {:request "time"}
                   {:on-reply #(deliver reply-received %)
                    :timeout-ms 5000}))
        ;; Wait for reply
        (let [reply (deref reply-received 3000 nil)]
          (is (some? reply) "Should receive reply")
          (is (number? (:server-time reply)))
          (is (= {:request "time"} (:client-data reply))))
        (finally
          (r/close! client)
          (r/close! server))))))

(deftest discrete-error-handling-test
  (testing "Server error is propagated to client"
    (let [port (next-port)
          server (r/connection {:transport :ws-server :port port})
          error-wire (r/discrete :error-test server)
          error-received (promise)
          connected? (promise)
          client (ws-client/connection {:url (str "ws://localhost:" port)
                                        :on-connect #(deliver connected? true)
                                        :auto-reconnect? false})]
      (try
        ;; Set up handler that throws
        (r/reply! error-wire (fn [_]
                               (throw (ex-info "Intentional error" {:reason :test}))))
        ;; Wait for connection
        (is (deref connected? 3000 false))
        ;; Send request from client
        (let [error-client (r/discrete :error-test client)]
          (Thread/sleep 100)
          (r/send! error-client {:data "trigger"}
                   {:on-error #(deliver error-received %)
                    :timeout-ms 5000}))
        ;; Wait for error
        (let [error (deref error-received 3000 nil)]
          (is (some? error) "Should receive error"))
        (finally
          (r/close! client)
          (r/close! server))))))

;; =============================================================================
;; Signal Wire Integration Tests
;; =============================================================================

(deftest signal-server-to-client-test
  (testing "Server signal updates reach client watchers"
    (let [port (next-port)
          server (r/connection {:transport :ws-server :port port})
          status-server (r/signal :status server :initial)
          received (atom [])
          ;; Expect: nil (local initial) + :initial (from watch response) + :running (from signal!)
          latch (CountDownLatch. 3)
          connected? (promise)
          client (ws-client/connection {:url (str "ws://localhost:" port)
                                        :on-connect #(deliver connected? true)
                                        :auto-reconnect? false})]
      (try
        ;; Wait for connection
        (is (deref connected? 3000 false))
        ;; Watch on client
        (let [status-client (r/signal :status client nil)]
          (r/watch status-client (fn [v]
                                   (swap! received conj v)
                                   (.countDown latch))))
        ;; Wait for watch response to arrive before updating
        (Thread/sleep 200)
        ;; Update from server
        (r/signal! status-server :running)
        ;; Wait for all updates
        (.await latch 3 TimeUnit/SECONDS)
        ;; Should have received the update
        (is (some #{:running} @received) "Should receive :running signal")
        (finally
          (r/close! client)
          (r/close! server))))))

;; =============================================================================
;; Multiple Clients Tests
;; =============================================================================

(deftest multiple-clients-broadcast-test
  (testing "Multiple clients receive server broadcasts"
    (let [port (next-port)
          server (r/connection {:transport :ws-server :port port})
          mouse-server (r/stream :mouse server)
          received1 (atom [])
          received2 (atom [])
          latch1 (CountDownLatch. 1)
          latch2 (CountDownLatch. 1)
          connected1? (promise)
          connected2? (promise)
          client1 (ws-client/connection {:url (str "ws://localhost:" port)
                                         :on-connect #(deliver connected1? true)
                                         :auto-reconnect? false})
          client2 (ws-client/connection {:url (str "ws://localhost:" port)
                                         :on-connect #(deliver connected2? true)
                                         :auto-reconnect? false})]
      (try
        ;; Wait for both connections
        (is (deref connected1? 3000 false) "Client 1 should connect")
        (is (deref connected2? 3000 false) "Client 2 should connect")
        ;; Set up listeners
        (let [mouse1 (r/stream :mouse client1)
              mouse2 (r/stream :mouse client2)]
          (r/listen mouse1 (fn [data]
                             (swap! received1 conj data)
                             (.countDown latch1)))
          (r/listen mouse2 (fn [data]
                             (swap! received2 conj data)
                             (.countDown latch2))))
        ;; Wait for setup
        (Thread/sleep 200)
        ;; Emit from server
        (r/emit! mouse-server {:x 500 :y 600})
        ;; Wait for messages
        (is (.await latch1 2 TimeUnit/SECONDS) "Client 1 should receive")
        (is (.await latch2 2 TimeUnit/SECONDS) "Client 2 should receive")
        ;; Verify both received
        (is (= 1 (count @received1)))
        (is (= 1 (count @received2)))
        (is (= {:x 500 :y 600} (dissoc (first @received1) :rheon/seq)))
        (is (= {:x 500 :y 600} (dissoc (first @received2) :rheon/seq)))
        (finally
          (r/close! client1)
          (r/close! client2)
          (r/close! server))))))

;; =============================================================================
;; Advanced Missionary Flow Tests
;; =============================================================================

(deftest flow-filtering-integration-test
  (testing "Missionary flow filtering works over network"
    (let [port (next-port)
          server (r/connection {:transport :ws-server :port port})
          mouse-server (r/stream :mouse server)
          received (atom [])
          latch (CountDownLatch. 2) ;; Expect only 2 values (x > 100)
          connected? (promise)
          client (ws-client/connection {:url (str "ws://localhost:" port)
                                        :on-connect #(deliver connected? true)
                                        :auto-reconnect? false})]
      (try
        (is (deref connected? 3000 false))
        ;; Set up filtered flow on client
        (let [mouse-client (r/stream :mouse client)
              flow (r/->flow mouse-client)
              filtered (m/eduction (filter #(> (:x %) 100)) flow)
              cancel ((m/reduce (fn [_ v]
                                  (swap! received conj v)
                                  (.countDown latch)
                                  nil)
                                nil filtered)
                      (fn [_] nil)
                      (fn [_] nil))]
          (Thread/sleep 100)
          ;; Emit various values
          (r/emit! mouse-server {:x 50 :y 1})   ;; filtered out
          (r/emit! mouse-server {:x 150 :y 2})  ;; passes
          (r/emit! mouse-server {:x 75 :y 3})   ;; filtered out
          (r/emit! mouse-server {:x 200 :y 4})  ;; passes
          ;; Wait
          (is (.await latch 3 TimeUnit/SECONDS) "Should receive 2 filtered values")
          ;; Verify only high values received
          (is (= 2 (count @received)))
          (is (every? #(> (:x %) 100) @received))
          (cancel))
        (finally
          (r/close! client)
          (r/close! server))))))

(deftest flow-mapping-integration-test
  (testing "Missionary flow mapping transforms data over network"
    (let [port (next-port)
          server (r/connection {:transport :ws-server :port port})
          mouse-server (r/stream :mouse server)
          received (atom [])
          latch (CountDownLatch. 2)
          connected? (promise)
          client (ws-client/connection {:url (str "ws://localhost:" port)
                                        :on-connect #(deliver connected? true)
                                        :auto-reconnect? false})]
      (try
        (is (deref connected? 3000 false))
        ;; Set up mapped flow on client - extract only :x values
        (let [mouse-client (r/stream :mouse client)
              flow (r/->flow mouse-client)
              mapped (m/eduction (map :x) flow)
              cancel ((m/reduce (fn [_ v]
                                  (swap! received conj v)
                                  (.countDown latch)
                                  nil)
                                nil mapped)
                      (fn [_] nil)
                      (fn [_] nil))]
          (Thread/sleep 100)
          (r/emit! mouse-server {:x 10 :y 20})
          (r/emit! mouse-server {:x 30 :y 40})
          (is (.await latch 2 TimeUnit/SECONDS))
          ;; Should have just x values
          (is (= [10 30] @received))
          (cancel))
        (finally
          (r/close! client)
          (r/close! server))))))

(deftest flow-take-integration-test
  (testing "Missionary take limits flow consumption"
    (let [port (next-port)
          server (r/connection {:transport :ws-server :port port})
          mouse-server (r/stream :mouse server)
          received (atom [])
          done? (promise)
          connected? (promise)
          client (ws-client/connection {:url (str "ws://localhost:" port)
                                        :on-connect #(deliver connected? true)
                                        :auto-reconnect? false})]
      (try
        (is (deref connected? 3000 false))
        ;; Set up flow that takes only first 3
        (let [mouse-client (r/stream :mouse client)
              flow (r/->flow mouse-client)
              taken (m/eduction (take 3) flow)
              cancel ((m/reduce (fn [_ v]
                                  (swap! received conj (:x v))
                                  nil)
                                nil taken)
                      (fn [_] (deliver done? true))
                      (fn [_] nil))]
          (Thread/sleep 100)
          ;; Emit 5 values
          (dotimes [i 5]
            (r/emit! mouse-server {:x i}))
          ;; Wait for completion
          (deref done? 2000 false)
          ;; Should only have first 3
          (is (= [0 1 2] @received)))
        (finally
          (r/close! client)
          (r/close! server))))))

(deftest signal-flow-integration-test
  (testing "Signal flow provides continuous value stream"
    (let [port (next-port)
          server (r/connection {:transport :ws-server :port port})
          status-server (r/signal :status server :initializing)
          received (atom [])
          latch (CountDownLatch. 3) ;; initial + 2 updates
          connected? (promise)
          client (ws-client/connection {:url (str "ws://localhost:" port)
                                        :on-connect #(deliver connected? true)
                                        :auto-reconnect? false})]
      (try
        (is (deref connected? 3000 false))
        ;; Use Missionary flow to track signal
        (let [status-client (r/signal :status client :unknown)
              flow (r/->flow status-client)
              cancel ((m/reduce (fn [_ v]
                                  (swap! received conj v)
                                  (.countDown latch)
                                  nil)
                                nil flow)
                      (fn [_] nil)
                      (fn [_] nil))]
          (Thread/sleep 100)
          ;; Update signal
          (r/signal! status-server :starting)
          (Thread/sleep 50)
          (r/signal! status-server :running)
          ;; Wait for updates
          (.await latch 2 TimeUnit/SECONDS)
          ;; Verify sequence
          (is (>= (count @received) 2) "Should receive multiple values")
          (cancel))
        (finally
          (r/close! client)
          (r/close! server))))))

;; =============================================================================
;; Sequence Number and Ordering Tests
;; =============================================================================

(deftest sequence-numbers-preserved-test
  (testing "Sequence numbers are correctly transmitted"
    (let [port (next-port)
          server (r/connection {:transport :ws-server :port port})
          mouse-server (r/stream :mouse server)
          received (atom [])
          latch (CountDownLatch. 5)
          connected? (promise)
          client (ws-client/connection {:url (str "ws://localhost:" port)
                                        :on-connect #(deliver connected? true)
                                        :auto-reconnect? false})]
      (try
        (is (deref connected? 3000 false))
        (let [mouse-client (r/stream :mouse client)]
          (r/listen mouse-client (fn [data]
                                   (swap! received conj (:rheon/seq data))
                                   (.countDown latch))))
        (Thread/sleep 100)
        ;; Emit 5 times
        (dotimes [_ 5]
          (r/emit! mouse-server {:x 1}))
        (is (.await latch 2 TimeUnit/SECONDS))
        ;; Verify sequence numbers are sequential
        (is (= [1 2 3 4 5] @received))
        (finally
          (r/close! client)
          (r/close! server))))))

;; =============================================================================
;; Bidirectional Flow Test
;; =============================================================================

(deftest bidirectional-communication-test
  (testing "Client and server can exchange messages bidirectionally"
    (let [port (next-port)
          server (r/connection {:transport :ws-server :port port})
          ;; Use separate wires for each direction to avoid local listener confusion
          ;; Server emit! also calls local listeners, so we use :client-to-server for client->server
          ;; and :server-to-client for server->client
          client-to-server-wire (r/stream :client-to-server server)
          server-to-client-wire (r/stream :server-to-client server)
          server-received (atom [])
          client-received (atom [])
          server-latch (CountDownLatch. 2)
          client-latch (CountDownLatch. 2)
          connected? (promise)
          client (ws-client/connection {:url (str "ws://localhost:" port)
                                        :on-connect #(deliver connected? true)
                                        :auto-reconnect? false})]
      (try
        ;; Server listens on client-to-server wire (receives client messages)
        (r/listen client-to-server-wire (fn [data]
                                          (swap! server-received conj (:msg data))
                                          (.countDown server-latch)))
        ;; Wait for connection
        (is (deref connected? 3000 false))
        ;; Set up client
        (let [client-to-server-client (r/stream :client-to-server client)
              server-to-client-client (r/stream :server-to-client client)]
          ;; Client listens on server-to-client wire (receives server messages)
          (r/listen server-to-client-client (fn [data]
                                              (swap! client-received conj (:msg data))
                                              (.countDown client-latch)))
          (Thread/sleep 100)
          ;; Client sends on client-to-server wire
          (r/emit! client-to-server-client {:msg "Hello from client!"})
          (r/emit! client-to-server-client {:msg "Another message"})
          ;; Server sends on server-to-client wire
          (r/emit! server-to-client-wire {:msg "Server response"})
          (r/emit! server-to-client-wire {:msg "More from server"}))
        ;; Wait for both
        (is (.await server-latch 2 TimeUnit/SECONDS) "Server should receive")
        (is (.await client-latch 2 TimeUnit/SECONDS) "Client should receive")
        ;; Verify
        (is (= ["Hello from client!" "Another message"] @server-received))
        (is (= ["Server response" "More from server"] @client-received))
        (finally
          (r/close! client)
          (r/close! server))))))

;; =============================================================================
;; Missionary Concurrent Process Tests
;; =============================================================================

(deftest missionary-concurrent-processes-test
  (testing "Server and client using Missionary flows for network communication"
    (let [port (next-port)
          server-received (atom [])
          client-received (atom [])
          connected? (promise)

          ;; Start server
          server (r/connection {:transport :ws-server :port port})
          mouse-server (r/stream :mouse server)

          ;; Start client
          client (ws-client/connection
                  {:url (str "ws://localhost:" port)
                   :on-connect #(deliver connected? true)
                   :auto-reconnect? false})
          mouse-client (r/stream :mouse client)]

      (try
        ;; Set up listeners on both sides
        (r/listen mouse-server (fn [data] (swap! server-received conj (:pos data))))
        (r/listen mouse-client (fn [data] (swap! client-received conj (:pos data))))

        ;; Wait for connection
        (is (deref connected? 3000 false) "Client should connect")
        (Thread/sleep 100)

        ;; Client sends to server
        (r/emit! mouse-client {:pos {:x 10 :y 20}})
        (r/emit! mouse-client {:pos {:x 30 :y 40}})

        ;; Server sends to client
        (r/emit! mouse-server {:pos {:x 100 :y 200}})
        (r/emit! mouse-server {:pos {:x 150 :y 250}})

        ;; Give time for async processing
        (Thread/sleep 300)

        ;; Verify bidirectional exchange
        (is (seq @server-received) "Server should receive client messages")
        (is (seq @client-received) "Client should receive server messages")
        (finally
          (r/close! client)
          (r/close! server))))))

(deftest missionary-flow-pipeline-test
  (testing "Missionary flow pipeline across network boundary"
    (let [port (next-port)
          server (r/connection {:transport :ws-server :port port})
          sensor-wire (r/stream :sensor server)
          processed-results (atom [])
          done-dfv (m/dfv)
          connected? (promise)
          client (ws-client/connection {:url (str "ws://localhost:" port)
                                        :on-connect #(deliver connected? true)
                                        :auto-reconnect? false})]
      (try
        ;; Wait for connection
        (is (deref connected? 3000 false))

        (let [sensor-client (r/stream :sensor client)
              ;; Get raw flow from client wire
              client-flow (p/->flow sensor-client)
              ;; Build processing pipeline using Missionary operators
              pipeline (->> client-flow
                            ;; Filter: only values > 50
                            (m/eduction (filter #(> (:value %) 50)))
                            ;; Map: extract and transform
                            (m/eduction (map #(* (:value %) 2)))
                            ;; Take first 3
                            (m/eduction (take 3)))
              ;; Run pipeline as a task
              _ ((m/reduce (fn [acc v]
                             (swap! processed-results conj v)
                             (when (= 3 (count @processed-results))
                               (done-dfv true))
                             acc)
                           nil
                           pipeline)
                 (fn [_]) (fn [_]))]

          ;; Server emits sensor readings
          (Thread/sleep 100)
          (r/emit! sensor-wire {:value 30})   ;; Filtered out (< 50)
          (r/emit! sensor-wire {:value 60})   ;; Passes -> 120
          (r/emit! sensor-wire {:value 40})   ;; Filtered out
          (r/emit! sensor-wire {:value 80})   ;; Passes -> 160
          (r/emit! sensor-wire {:value 100})  ;; Passes -> 200
          (r/emit! sensor-wire {:value 90})   ;; Would pass but take 3 already done

          ;; Wait for pipeline to complete
          (Thread/sleep 500)

          ;; Verify pipeline processed correctly
          (is (= [120 160 200] @processed-results)
              "Pipeline should filter, map, and take values"))
        (finally
          (r/close! client)
          (r/close! server))))))

(deftest missionary-signal-continuous-flow-test
  (testing "Signal wire as Missionary continuous flow"
    (let [port (next-port)
          server (r/connection {:transport :ws-server :port port})
          temp-wire (r/signal :temperature server 20.0)
          readings (atom [])
          connected? (promise)
          client (ws-client/connection {:url (str "ws://localhost:" port)
                                        :on-connect #(deliver connected? true)
                                        :auto-reconnect? false})]
      (try
        (is (deref connected? 3000 false))

        (let [temp-client (r/signal :temperature client nil)
              ;; Get continuous flow from signal
              temp-flow (p/->flow temp-client)
              ;; Sample and collect readings
              _ ((m/reduce (fn [_ v]
                             (when v (swap! readings conj v))
                             nil)
                           nil
                           (m/eduction (take 4) temp-flow))
                 (fn [_]) (fn [_]))]

          ;; Request initial value
          (r/watch temp-client (fn [_] nil))
          (Thread/sleep 200)

          ;; Server updates temperature
          (r/signal! temp-wire 22.5)
          (Thread/sleep 100)
          (r/signal! temp-wire 25.0)
          (Thread/sleep 100)
          (r/signal! temp-wire 23.0)
          (Thread/sleep 300)

          ;; Should have captured temperature readings
          (is (>= (count @readings) 2) "Should capture multiple temperature readings")
          (is (some #(> % 20.0) @readings) "Should see temperature updates"))
        (finally
          (r/close! client)
          (r/close! server))))))

;; =============================================================================
;; Spec Validation Integration Tests
;; =============================================================================

(deftest spec-validated-stream-over-websocket-test
  (testing "Stream wire with spec - valid data passes over WebSocket"
    (let [port (next-port)
          server (r/connection {:transport :ws-server :port port})
          ;; Server creates wire with spec
          mouse-ref {:wire-id :mouse :type :stream :spec [:map [:x :int] [:y :int]]}
          mouse-server (r/wire server mouse-ref)
          received (atom [])
          latch (CountDownLatch. 2)
          connected? (promise)
          client (ws-client/connection {:url (str "ws://localhost:" port)
                                        :on-connect #(deliver connected? true)
                                        :auto-reconnect? false})]
      (try
        (is (deref connected? 3000 false))
        ;; Client creates wire with same spec
        (let [mouse-client (r/wire client mouse-ref)]
          (r/listen mouse-client (fn [data]
                                   (swap! received conj data)
                                   (.countDown latch))))
        (Thread/sleep 100)
        ;; Server emits valid data
        (r/emit! mouse-server {:x 100 :y 200})
        (r/emit! mouse-server {:x 300 :y 400})
        ;; Wait for messages
        (is (.await latch 2 TimeUnit/SECONDS) "Should receive 2 valid messages")
        (is (= 2 (count @received)))
        (is (= {:x 100 :y 200} (dissoc (first @received) :rheon/seq)))
        (is (= {:x 300 :y 400} (dissoc (second @received) :rheon/seq)))
        (finally
          (r/close! client)
          (r/close! server)))))

  (testing "Stream wire with spec - invalid data throws before sending"
    (let [port (next-port)
          server (r/connection {:transport :ws-server :port port})
          mouse-ref {:wire-id :mouse :type :stream :spec [:map [:x :int] [:y :int]]}
          mouse-server (r/wire server mouse-ref)
          connected? (promise)
          client (ws-client/connection {:url (str "ws://localhost:" port)
                                        :on-connect #(deliver connected? true)
                                        :auto-reconnect? false})]
      (try
        (is (deref connected? 3000 false))
        ;; Try to emit invalid data from server - should throw before going over wire
        (is (thrown-with-msg? Exception #"Data validation failed"
              (r/emit! mouse-server {:x "not-an-int" :y 200})))
        ;; Try to emit from client with invalid data
        (let [mouse-client (r/wire client mouse-ref)]
          (is (thrown-with-msg? Exception #"Data validation failed"
                (r/emit! mouse-client {:x 100 :y "bad"}))))
        (finally
          (r/close! client)
          (r/close! server))))))

(deftest spec-validated-discrete-over-websocket-test
  (testing "Discrete wire with spec - valid request/reply over WebSocket"
    (let [port (next-port)
          server (r/connection {:transport :ws-server :port port})
          ;; Discrete wire with request and reply specs
          clock-ref {:wire-id :clock
                     :type :discrete
                     :spec {:request [:map [:action :keyword]]
                            :reply [:map [:time :int] [:zone :string]]}}
          clock-server (r/wire server clock-ref)
          reply-received (promise)
          connected? (promise)
          client (ws-client/connection {:url (str "ws://localhost:" port)
                                        :on-connect #(deliver connected? true)
                                        :auto-reconnect? false})]
      (try
        ;; Server handler returns valid reply
        (r/reply! clock-server (fn [_data]
                                 {:time (System/currentTimeMillis)
                                  :zone "UTC"}))
        (is (deref connected? 3000 false))
        ;; Client sends valid request
        (let [clock-client (r/wire client clock-ref)]
          (Thread/sleep 100)
          (r/send! clock-client {:action :get-time}
                   {:on-reply #(deliver reply-received %)
                    :timeout-ms 5000}))
        ;; Wait for reply
        (let [reply (deref reply-received 3000 nil)]
          (is (some? reply) "Should receive valid reply")
          (is (number? (:time reply)))
          (is (= "UTC" (:zone reply))))
        (finally
          (r/close! client)
          (r/close! server)))))

  (testing "Discrete wire with spec - invalid request throws"
    (let [port (next-port)
          server (r/connection {:transport :ws-server :port port})
          clock-ref {:wire-id :clock
                     :type :discrete
                     :spec {:request [:map [:action :keyword]]
                            :reply [:map [:time :int]]}}
          clock-server (r/wire server clock-ref)
          connected? (promise)
          client (ws-client/connection {:url (str "ws://localhost:" port)
                                        :on-connect #(deliver connected? true)
                                        :auto-reconnect? false})]
      (try
        (r/reply! clock-server (fn [_] {:time 12345}))
        (is (deref connected? 3000 false))
        ;; Try to send invalid request - should throw before going over wire
        (let [clock-client (r/wire client clock-ref)]
          (is (thrown-with-msg? Exception #"Data validation failed"
                (r/send! clock-client {:action "not-a-keyword"}
                         {:timeout-ms 5000}))))
        (finally
          (r/close! client)
          (r/close! server))))))

(deftest spec-validated-signal-over-websocket-test
  (testing "Signal wire with spec - valid values over WebSocket"
    (let [port (next-port)
          server (r/connection {:transport :ws-server :port port})
          ;; Signal wire with spec
          status-ref {:wire-id :status
                      :type :signal
                      :spec :keyword
                      :initial :initializing}
          status-server (r/wire server status-ref)
          received (atom [])
          running-received? (promise)
          connected? (promise)
          client (ws-client/connection {:url (str "ws://localhost:" port)
                                        :on-connect #(deliver connected? true)
                                        :auto-reconnect? false})]
      (try
        (is (deref connected? 3000 false))
        ;; Client watches signal
        (let [status-client (r/wire client status-ref)]
          (r/watch status-client (fn [v]
                                   (when v
                                     (swap! received conj v)
                                     (when (= v :running)
                                       (deliver running-received? true))))))
        (Thread/sleep 300)
        ;; Server updates with valid value
        (r/signal! status-server :running)
        ;; Wait for the :running value specifically
        (is (deref running-received? 3000 false) "Should receive :running signal")
        ;; Verify we got it
        (is (some #{:running} @received) "Should have :running in received values")
        (finally
          (r/close! client)
          (r/close! server)))))

  (testing "Signal wire with spec - invalid value throws"
    (let [port (next-port)
          server (r/connection {:transport :ws-server :port port})
          status-ref {:wire-id :status
                      :type :signal
                      :spec :keyword
                      :initial :ready}
          status-server (r/wire server status-ref)
          connected? (promise)
          client (ws-client/connection {:url (str "ws://localhost:" port)
                                        :on-connect #(deliver connected? true)
                                        :auto-reconnect? false})]
      (try
        (is (deref connected? 3000 false))
        ;; Try to signal invalid value - should throw
        (is (thrown-with-msg? Exception #"Data validation failed"
              (r/signal! status-server "not-a-keyword")))
        (finally
          (r/close! client)
          (r/close! server))))))

(deftest spec-validated-bidirectional-over-websocket-test
  (testing "Bidirectional communication with specs on both sides"
    (let [port (next-port)
          server (r/connection {:transport :ws-server :port port})
          ;; Client->Server wire
          c2s-ref {:wire-id :client-to-server
                   :type :stream
                   :spec [:map [:msg :string] [:from :keyword]]}
          ;; Server->Client wire
          s2c-ref {:wire-id :server-to-client
                   :type :stream
                   :spec [:map [:msg :string] [:timestamp :int]]}
          c2s-server (r/wire server c2s-ref)
          s2c-server (r/wire server s2c-ref)
          server-received (atom [])
          client-received (atom [])
          server-latch (CountDownLatch. 2)
          client-latch (CountDownLatch. 2)
          connected? (promise)
          client (ws-client/connection {:url (str "ws://localhost:" port)
                                        :on-connect #(deliver connected? true)
                                        :auto-reconnect? false})]
      (try
        ;; Server listens on c2s wire
        (r/listen c2s-server (fn [data]
                               (swap! server-received conj data)
                               (.countDown server-latch)))
        (is (deref connected? 3000 false))
        ;; Client creates wires and listens on s2c
        (let [c2s-client (r/wire client c2s-ref)
              s2c-client (r/wire client s2c-ref)]
          (r/listen s2c-client (fn [data]
                                 (swap! client-received conj data)
                                 (.countDown client-latch)))
          (Thread/sleep 100)
          ;; Client sends valid messages
          (r/emit! c2s-client {:msg "Hello" :from :client})
          (r/emit! c2s-client {:msg "World" :from :client})
          ;; Server sends valid messages
          (r/emit! s2c-server {:msg "Response" :timestamp 123456})
          (r/emit! s2c-server {:msg "Update" :timestamp 123457}))
        ;; Wait for both
        (is (.await server-latch 2 TimeUnit/SECONDS) "Server should receive")
        (is (.await client-latch 2 TimeUnit/SECONDS) "Client should receive")
        ;; Verify
        (is (= 2 (count @server-received)))
        (is (= 2 (count @client-received)))
        (is (= "Hello" (:msg (first @server-received))))
        (is (= "Response" (:msg (first @client-received))))
        (finally
          (r/close! client)
          (r/close! server))))))

(deftest spec-no-spec-wire-over-websocket-test
  (testing "Wire without spec allows any map data over WebSocket"
    (let [port (next-port)
          server (r/connection {:transport :ws-server :port port})
          ;; Wire without spec - no validation
          any-ref {:wire-id :anything :type :stream}
          any-server (r/wire server any-ref)
          received (atom [])
          latch (CountDownLatch. 3)
          connected? (promise)
          client (ws-client/connection {:url (str "ws://localhost:" port)
                                        :on-connect #(deliver connected? true)
                                        :auto-reconnect? false})]
      (try
        (is (deref connected? 3000 false))
        (let [any-client (r/wire client any-ref)]
          (r/listen any-client (fn [data]
                                 (swap! received conj data)
                                 (.countDown latch))))
        (Thread/sleep 100)
        ;; Can send any map data without spec - streams expect maps for :rheon/seq metadata
        (r/emit! any-server {:x 1 :y 2})
        (r/emit! any-server {:msg "hello" :count 42})
        (r/emit! any-server {:nested {:data [1 2 3]}})
        (is (.await latch 2 TimeUnit/SECONDS))
        (is (= 3 (count @received)))
        ;; Verify all messages arrived (with :rheon/seq stripped for comparison)
        (is (= {:x 1 :y 2} (dissoc (first @received) :rheon/seq)))
        (is (= {:msg "hello" :count 42} (dissoc (second @received) :rheon/seq)))
        (is (= {:nested {:data [1 2 3]}} (dissoc (nth @received 2) :rheon/seq)))
        (finally
          (r/close! client)
          (r/close! server))))))
