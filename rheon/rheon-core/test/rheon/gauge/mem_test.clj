(ns rheon.gauge.mem-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [rheon.protocols :as p]
            [rheon.gauge.mem :as mem]))

;; =============================================================================
;; Test Fixtures
;; =============================================================================

(defn reset-servers-fixture [f]
  (mem/reset-servers!)
  (f)
  (mem/reset-servers!))

(use-fixtures :each reset-servers-fixture)

;; =============================================================================
;; Basic Gauge Tests
;; =============================================================================

(deftest gauge-info-test
  (testing "Gauge provides correct info"
    (is (= :mem (p/gauge-name mem/gauge)))
    (is (string? (p/gauge-description mem/gauge)))
    (is (vector? (p/gauge-requires mem/gauge)))
    (is (empty? (p/gauge-requires mem/gauge)) "mem gauge has no dependencies")))

(deftest listen-test
  (testing "Server can listen on a port"
    (let [server (p/gauge-listen! mem/gauge {:port 9092})]
      (is (some? server))
      (is (= 1 (mem/server-count))))))

(deftest connect-without-server-test
  (testing "Connect fails when no server is listening"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"No server listening"
                          (p/gauge-connect! mem/gauge "mem://localhost:9999" {})))))

(deftest connect-with-server-test
  (testing "Connect succeeds when server is listening"
    (let [_server (p/gauge-listen! mem/gauge {:port 9092})
          client (p/gauge-connect! mem/gauge "mem://localhost:9092" {})]
      (is (some? client))
      (is (uuid? (:id client))))))

;; =============================================================================
;; Message Passing Tests
;; =============================================================================

(deftest send-receive-test
  (testing "Messages can be sent and received"
    (let [server (p/gauge-listen! mem/gauge {:port 9092})
          received (promise)
          _ (p/gauge-subscribe! mem/gauge server :mouse
                                (fn [data] (deliver received data)))
          client (p/gauge-connect! mem/gauge "mem://localhost:9092" {})]

      (p/gauge-send! mem/gauge client :mouse {:x 100 :y 200} {})

      (is (= {:x 100 :y 200} (deref received 1000 :timeout))))))

(deftest multiple-wires-test
  (testing "Different wires are independent"
    (let [server (p/gauge-listen! mem/gauge {:port 9092})
          mouse-received (promise)
          status-received (promise)
          _ (p/gauge-subscribe! mem/gauge server :mouse
                                (fn [data] (deliver mouse-received data)))
          _ (p/gauge-subscribe! mem/gauge server :status
                                (fn [data] (deliver status-received data)))
          client (p/gauge-connect! mem/gauge "mem://localhost:9092" {})]

      (p/gauge-send! mem/gauge client :mouse {:x 50 :y 50} {})
      (p/gauge-send! mem/gauge client :status :connected {})

      (is (= {:x 50 :y 50} (deref mouse-received 1000 :timeout)))
      (is (= :connected (deref status-received 1000 :timeout))))))

;; =============================================================================
;; Request/Reply Tests
;; =============================================================================

(deftest request-reply-test
  (testing "Request/reply pattern works"
    (let [server (p/gauge-listen! mem/gauge {:port 9092})
          _ (p/gauge-on-request! mem/gauge server :clock
                                 (fn [{:keys [client-time]} reply!]
                                   (reply! {:server-time (System/currentTimeMillis)
                                            :gap (- (System/currentTimeMillis) client-time)})))
          client (p/gauge-connect! mem/gauge "mem://localhost:9092" {})
          reply-received (promise)]

      (p/gauge-request! mem/gauge client :clock
                        {:client-time (System/currentTimeMillis)}
                        {:on-reply (fn [reply] (deliver reply-received reply))})

      (let [reply (deref reply-received 1000 :timeout)]
        (is (map? reply))
        (is (contains? reply :server-time))
        (is (contains? reply :gap))
        (is (number? (:gap reply)))))))

;; =============================================================================
;; Connection Lifecycle Tests
;; =============================================================================

(deftest close-connection-test
  (testing "Closed connections don't send"
    (let [server (p/gauge-listen! mem/gauge {:port 9092})
          received (atom [])
          _ (p/gauge-subscribe! mem/gauge server :test
                                (fn [data] (swap! received conj data)))
          client (p/gauge-connect! mem/gauge "mem://localhost:9092" {})]

      ;; Send before close
      (p/gauge-send! mem/gauge client :test :before {})
      (Thread/sleep 50)

      ;; Close and try to send
      (p/gauge-close! mem/gauge client)
      (p/gauge-send! mem/gauge client :test :after {})
      (Thread/sleep 50)

      (is (= [:before] @received) "Only message before close should be received"))))

;; =============================================================================
;; On-Client Handler Tests
;; =============================================================================

(deftest on-client-handler-test
  (testing "Server receives notification of new clients"
    (let [server (p/gauge-listen! mem/gauge {:port 9092})
          clients-connected (atom [])]

      ;; Register client handler
      (p/on-client server (fn [client]
                            (swap! clients-connected conj (:id client))))

      ;; Connect a client
      (let [client (p/gauge-connect! mem/gauge "mem://localhost:9092" {})]
        (is (= 1 (count @clients-connected)))
        (is (= (:id client) (first @clients-connected)))))))
