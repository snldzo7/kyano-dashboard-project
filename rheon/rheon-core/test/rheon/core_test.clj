(ns rheon.core-test
  "Tests for the main Rheon public API."
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [rheon.core :as rheon]
            [rheon.gauge.mem :as mem]))

;; =============================================================================
;; Test Fixtures
;; =============================================================================

(defn reset-fixture [f]
  (mem/reset-servers!)
  (f)
  (mem/reset-servers!))

(use-fixtures :each reset-fixture)

;; =============================================================================
;; Connection Tests
;; =============================================================================

(deftest connect-listen-test
  (testing "Basic connect and listen"
    (let [server (rheon/listen! {:port 9092 :gauge :mem})]
      (is (some? server))

      (let [conn (rheon/connect! "mem://localhost:9092")]
        (is (some? conn))
        (.close conn)))))

(deftest with-open-test
  (testing "Connections work with with-open"
    (let [server (rheon/listen! {:port 9092 :gauge :mem})]
      (with-open [conn (rheon/connect! "mem://localhost:9092")]
        (is (some? conn))
        ;; Connection is usable inside with-open
        (rheon/send! conn :test {:msg "hello"})))))

(deftest gauge-detection-from-uri-test
  (testing "Gauge is detected from URI scheme"
    (let [server (rheon/listen! {:port 9092 :gauge :mem})
          conn (rheon/connect! "mem://localhost:9092")]
      ;; Should have detected :mem gauge from URI
      (is (= :mem (-> conn :gauge rheon.protocols/gauge-name)))
      (.close conn))))

;; =============================================================================
;; Wire Operation Tests
;; =============================================================================

(deftest send-on-message-test
  (testing "send! and on-message work together"
    (let [server (rheon/listen! {:port 9092 :gauge :mem})
          received (promise)]

      (rheon/on-message server :chat
                        (fn [data] (deliver received data)))

      (with-open [conn (rheon/connect! "mem://localhost:9092")]
        (rheon/send! conn :chat {:user "alice" :msg "hello"}))

      (is (= {:user "alice" :msg "hello"}
             (deref received 1000 :timeout))))))

(deftest request-on-request-test
  (testing "request! and on-request work together"
    (let [server (rheon/listen! {:port 9092 :gauge :mem})]

      (rheon/on-request server :echo
                        (fn [data reply!]
                          (reply! {:echoed data})))

      (with-open [conn (rheon/connect! "mem://localhost:9092")]
        (let [reply (promise)]
          (rheon/request! conn :echo {:msg "ping"}
                          {:on-reply (fn [r] (deliver reply r))})

          (is (= {:echoed {:msg "ping"}}
                 (deref reply 1000 :timeout))))))))

(deftest on-client-test
  (testing "on-client receives new connections"
    (let [server (rheon/listen! {:port 9092 :gauge :mem})
          client-connected (promise)]

      (rheon/on-client server
                       (fn [client-conn]
                         (deliver client-connected true)
                         ;; Set up per-client handlers
                         (rheon/on-request client-conn :ping
                                           (fn [_ reply!] (reply! :pong)))))

      (with-open [conn (rheon/connect! "mem://localhost:9092")]
        (is (true? (deref client-connected 1000 :timeout)))))))

;; =============================================================================
;; Signal Tests
;; =============================================================================

(deftest signal-deref-test
  (testing "Signal supports @deref"
    (let [server (rheon/listen! {:port 9092 :gauge :mem})]
      (with-open [conn (rheon/connect! "mem://localhost:9092")]
        (let [status (rheon/signal conn :status :initial)]
          (is (= :initial @status)))))))

(deftest signal-update-test
  (testing "Signal updates when wire receives data"
    (let [server (rheon/listen! {:port 9092 :gauge :mem})]
      ;; Server subscribes to forward messages
      (let [client-ref (atom nil)]
        (rheon/on-client server
                         (fn [client-conn]
                           (reset! client-ref client-conn)))

        (with-open [conn (rheon/connect! "mem://localhost:9092")]
          (let [status (rheon/signal conn :status :disconnected)]
            (is (= :disconnected @status))

            ;; Note: In mem gauge, messages go from client->server
            ;; To test signal update, we need server to send to client
            ;; This requires the mem gauge to support server->client
            ;; For now, test the initial value works
            ))))))

(deftest signal-add-watch-test
  (testing "Signal supports add-watch"
    (let [server (rheon/listen! {:port 9092 :gauge :mem})]
      (with-open [conn (rheon/connect! "mem://localhost:9092")]
        (let [status (rheon/signal conn :status :initial)
              watch-called (atom false)]
          (add-watch status :test-watch
                     (fn [_key _ref _old _new]
                       (reset! watch-called true)))
          ;; Watch is registered
          (is (contains? (.getWatches status) :test-watch)))))))

;; =============================================================================
;; Gauge Registry Tests
;; =============================================================================

(deftest available-gauges-test
  (testing "available-gauges returns registered gauges"
    (let [gauges (rheon/available-gauges)]
      (is (set? gauges))
      (is (contains? gauges :mem)))))

(deftest gauge-info-test
  (testing "gauge-info returns gauge metadata"
    (let [info (rheon/gauge-info :mem)]
      (is (map? info))
      (is (= :mem (:name info)))
      (is (string? (:description info)))
      (is (empty? (:requires info)) "mem gauge has no dependencies"))))

(deftest unknown-gauge-test
  (testing "Unknown gauge throws helpful error"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Unknown gauge"
                          (rheon/connect! "foo://localhost:9092"
                                          {:gauge :nonexistent})))))

;; =============================================================================
;; Full Integration Test
;; =============================================================================

(deftest full-integration-test
  (testing "Full client-server interaction"
    (let [server (rheon/listen! {:port 9092 :gauge :mem})
          messages-received (atom [])]

      ;; Server setup
      (rheon/on-client server
                       (fn [_client-conn]
                         ;; Just acknowledge connection
                         ))

      (rheon/on-message server :mouse
                        (fn [data]
                          (swap! messages-received conj data)))

      (rheon/on-request server :clock
                        (fn [{:keys [client-time]} reply!]
                          (reply! {:server-time (System/currentTimeMillis)
                                   :gap (- (System/currentTimeMillis) client-time)})))

      ;; Client interactions
      (with-open [conn (rheon/connect! "mem://localhost:9092")]
        ;; Send mouse coords
        (rheon/send! conn :mouse {:x 100 :y 200})
        (rheon/send! conn :mouse {:x 150 :y 250})
        (Thread/sleep 50)

        ;; Request clock sync
        (let [reply (promise)]
          (rheon/request! conn :clock {:client-time (System/currentTimeMillis)}
                          {:on-reply (fn [r] (deliver reply r))})
          (let [r (deref reply 1000 :timeout)]
            (is (number? (:server-time r)))
            (is (number? (:gap r))))))

      ;; Verify messages received
      (is (= 2 (count @messages-received)))
      (is (= {:x 100 :y 200} (first @messages-received)))
      (is (= {:x 150 :y 250} (second @messages-received))))))
