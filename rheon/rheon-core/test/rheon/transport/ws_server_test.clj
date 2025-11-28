(ns rheon.transport.ws-server-test
  "Tests for WebSocket server transport."
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [rheon.core :as r]
            [rheon.transport.ws-server :as ws-server]
            [org.httpkit.client :as http-client]
            [cognitect.transit :as transit])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]
           [java.net URI]
           [java.net.http HttpClient WebSocket WebSocket$Listener]
           [java.util.concurrent CompletableFuture CountDownLatch TimeUnit]))

;; =============================================================================
;; Test Helpers
;; =============================================================================

(def ^:dynamic *test-port* 18080)

(defn next-port []
  (let [port *test-port*]
    (alter-var-root #'*test-port* inc)
    port))

(defn encode [msg]
  (let [out (ByteArrayOutputStream.)
        w (transit/writer out :json)]
    (transit/write w msg)
    (.toString out "UTF-8")))

(defn decode [data]
  (let [bytes (if (string? data)
                (.getBytes ^String data "UTF-8")
                data)
        in (ByteArrayInputStream. bytes)
        r (transit/reader in :json)]
    (transit/read r)))

;; =============================================================================
;; Server Lifecycle Tests
;; =============================================================================

(deftest server-lifecycle-test
  (testing "server starts and stops"
    (let [port (next-port)
          conn (r/connection {:transport :ws-server :port port})]
      (try
        (is (some? conn))
        (is (some? (:server conn)))
        (finally
          (r/close! conn)))))

  (testing "can restart on same port after close"
    (let [port (next-port)
          conn1 (r/connection {:transport :ws-server :port port})]
      (r/close! conn1)
      (Thread/sleep 100)
      (let [conn2 (r/connection {:transport :ws-server :port port})]
        (try
          (is (some? conn2))
          (finally
            (r/close! conn2)))))))

;; =============================================================================
;; Wire Creation Tests
;; =============================================================================

(deftest wire-creation-test
  (testing "creates stream wire"
    (let [port (next-port)
          conn (r/connection {:transport :ws-server :port port})]
      (try
        (let [mouse (r/stream :mouse conn)]
          (is (some? mouse))
          (is (= :mouse (:id mouse)))
          (is (= :stream (:wire-type mouse))))
        (finally
          (r/close! conn)))))

  (testing "creates discrete wire"
    (let [port (next-port)
          conn (r/connection {:transport :ws-server :port port})]
      (try
        (let [clock (r/discrete :clock conn)]
          (is (some? clock))
          (is (= :clock (:id clock)))
          (is (= :discrete (:wire-type clock))))
        (finally
          (r/close! conn)))))

  (testing "creates signal wire with initial value"
    (let [port (next-port)
          conn (r/connection {:transport :ws-server :port port})]
      (try
        (let [status (r/signal :status conn {:state :starting})]
          (is (some? status))
          (is (= :status (:id status)))
          (is (= :signal (:wire-type status)))
          (is (= {:state :starting} @(:value status))))
        (finally
          (r/close! conn)))))

  (testing "wire type mismatch throws"
    (let [port (next-port)
          conn (r/connection {:transport :ws-server :port port})]
      (try
        (r/stream :my-wire conn)
        (is (thrown-with-msg? Exception #"already exists"
              (r/discrete :my-wire conn)))
        (finally
          (r/close! conn))))))

;; =============================================================================
;; Local Stream Tests (without network client)
;; =============================================================================

(deftest local-stream-test
  (testing "local emit/listen works"
    (let [port (next-port)
          conn (r/connection {:transport :ws-server :port port})
          received (atom [])]
      (try
        (let [mouse (r/stream :mouse conn)]
          (r/listen mouse (fn [d] (swap! received conj d)))
          (r/emit! mouse {:x 1 :y 2})
          (r/emit! mouse {:x 3 :y 4})
          ;; Data is enriched with :rheon/seq for ordering
          (is (= [{:x 1 :y 2 :rheon/seq 1} {:x 3 :y 4 :rheon/seq 2}] @received)))
        (finally
          (r/close! conn)))))

  (testing "multiple listeners"
    (let [port (next-port)
          conn (r/connection {:transport :ws-server :port port})
          received-1 (atom [])
          received-2 (atom [])]
      (try
        (let [mouse (r/stream :mouse conn)]
          (r/listen mouse (fn [d] (swap! received-1 conj d)))
          (r/listen mouse (fn [d] (swap! received-2 conj d)))
          (r/emit! mouse {:x 1})
          (is (= [{:x 1 :rheon/seq 1}] @received-1))
          (is (= [{:x 1 :rheon/seq 1}] @received-2)))
        (finally
          (r/close! conn)))))

  (testing "unsubscribe stops listening"
    (let [port (next-port)
          conn (r/connection {:transport :ws-server :port port})
          received (atom [])]
      (try
        (let [mouse (r/stream :mouse conn)
              sub (r/listen mouse (fn [d] (swap! received conj d)))]
          (r/emit! mouse {:x 1})
          (r/unsubscribe! sub)
          (r/emit! mouse {:x 2})
          (is (= [{:x 1 :rheon/seq 1}] @received)))
        (finally
          (r/close! conn))))))

;; =============================================================================
;; Local Signal Tests
;; =============================================================================

(deftest local-signal-test
  (testing "watch receives initial value"
    (let [port (next-port)
          conn (r/connection {:transport :ws-server :port port})
          received (atom [])]
      (try
        (let [status (r/signal :status conn {:state :init})]
          (r/watch status (fn [v] (swap! received conj v)))
          (is (= [{:state :init}] @received)))
        (finally
          (r/close! conn)))))

  (testing "signal! notifies watchers"
    (let [port (next-port)
          conn (r/connection {:transport :ws-server :port port})
          received (atom [])]
      (try
        (let [status (r/signal :status conn :init)]
          (r/watch status (fn [v] (swap! received conj v)))
          (r/signal! status :running)
          (is (= [:init :running] @received)))
        (finally
          (r/close! conn)))))

  (testing "unsubscribe stops watching"
    (let [port (next-port)
          conn (r/connection {:transport :ws-server :port port})
          received (atom [])]
      (try
        (let [status (r/signal :status conn :init)
              sub (r/watch status (fn [v] (swap! received conj v)))]
          (r/unsubscribe! sub)
          (r/signal! status :updated)
          (is (= [:init] @received)))
        (finally
          (r/close! conn))))))

;; =============================================================================
;; WebSocket Client Connection Test
;; =============================================================================

(deftest websocket-client-connection-test
  (testing "WebSocket client can connect"
    (let [port (next-port)
          conn (r/connection {:transport :ws-server :port port})
          connected (promise)
          client (HttpClient/newHttpClient)]
      (try
        (let [listener (reify WebSocket$Listener
                         (onOpen [_ ws]
                           (deliver connected true)
                           (.request ws 1))
                         (onText [_ ws data last?]
                           (.request ws 1)
                           (CompletableFuture/completedFuture nil))
                         (onClose [_ ws code reason]
                           (CompletableFuture/completedFuture nil))
                         (onError [_ ws error]))
              ws-future (.buildAsync
                         (.newWebSocketBuilder client)
                         (URI. (str "ws://localhost:" port))
                         listener)
              ws (.get ws-future)]
          (is (deref connected 2000 false))
          (.sendClose ws WebSocket/NORMAL_CLOSURE "done"))
        (finally
          (r/close! conn))))))

(deftest websocket-stream-emit-test
  (testing "WebSocket client receives stream emissions"
    (let [port (next-port)
          conn (r/connection {:transport :ws-server :port port})
          mouse (r/stream :mouse conn)
          received (atom [])
          latch (CountDownLatch. 2)
          client (HttpClient/newHttpClient)]
      (try
        (let [listener (reify WebSocket$Listener
                         (onOpen [_ ws]
                           (.request ws 1))
                         (onText [_ ws data last?]
                           (let [msg (decode (str data))]
                             (swap! received conj msg)
                             (.countDown latch))
                           (.request ws 1)
                           (CompletableFuture/completedFuture nil))
                         (onClose [_ ws code reason]
                           (CompletableFuture/completedFuture nil))
                         (onError [_ ws error]))
              ws-future (.buildAsync
                         (.newWebSocketBuilder client)
                         (URI. (str "ws://localhost:" port))
                         listener)
              ws (.get ws-future)]
          ;; Wait for connection
          (Thread/sleep 100)
          ;; Emit data
          (r/emit! mouse {:x 1 :y 2})
          (r/emit! mouse {:x 3 :y 4})
          ;; Wait for messages
          (.await latch 2 TimeUnit/SECONDS)
          ;; Check received
          (is (= 2 (count @received)))
          (is (every? #(= :emit (:op %)) @received))
          (.sendClose ws WebSocket/NORMAL_CLOSURE "done"))
        (finally
          (r/close! conn))))))
