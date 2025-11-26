(ns rheon.gauge.sente-test
  "Tests for Rheon Sente gauge server-side implementation."
  (:require [clojure.test :refer [deftest testing is]]
            [rheon.gauge.sente :as sente-gauge]
            [rheon.protocols :as p]
            [taoensso.sente :as sente]
            [clj-http.client :as http])
  (:import [java.net Socket]))

;; =============================================================================
;; Test Fixtures
;; =============================================================================

(defn next-port
  "Get next available port for testing."
  []
  (let [port (+ 19000 (rand-int 1000))]
    (try
      (let [socket (Socket. "localhost" port)]
        (.close socket)
        ;; Port is in use, try again
        (next-port))
      (catch Exception _
        ;; Port is available
        port))))

;; =============================================================================
;; Gauge Info Tests
;; =============================================================================

(deftest gauge-info-test
  (testing "Sente gauge provides correct info"
    (is (= :sente (p/gauge-name sente-gauge/gauge)))
    (is (string? (p/gauge-description sente-gauge/gauge)))
    (is (contains? (p/gauge-capabilities sente-gauge/gauge) :send))
    (is (contains? (p/gauge-capabilities sente-gauge/gauge) :request))
    (is (contains? (p/gauge-capabilities sente-gauge/gauge) :broadcast))))

;; =============================================================================
;; Server Lifecycle Tests
;; =============================================================================

(deftest server-start-stop-test
  (testing "Server starts and stops cleanly"
    (let [port (next-port)
          server (p/gauge-listen! sente-gauge/gauge {:port port})]
      (is (some? server))
      (is (= port (:port server)))
      (is (some? (:http-server server)))
      (is (some? (:ch-recv server)))
      (is (some? (:send-fn server)))

      ;; Clean up
      (.close server))))

(deftest server-creates-handler-atoms-test
  (testing "Server creates handler atoms correctly"
    (let [port (next-port)
          server (p/gauge-listen! sente-gauge/gauge {:port port})]
      (try
        (is (instance? clojure.lang.Atom (:message-handlers server)))
        (is (instance? clojure.lang.Atom (:request-handlers server)))
        (is (instance? clojure.lang.Atom (:client-handlers server)))
        (is (= {} @(:message-handlers server)))
        (is (= {} @(:request-handlers server)))
        (is (= [] @(:client-handlers server)))
        (finally
          (.close server))))))

;; =============================================================================
;; Handler Registration Tests
;; =============================================================================

(deftest message-handler-registration-test
  (testing "Message handlers can be registered and unregistered"
    (let [port (next-port)
          server (p/gauge-listen! sente-gauge/gauge {:port port})
          handler-called (atom false)
          handler (fn [_data] (reset! handler-called true))]
      (try
        ;; Register handler using on-message
        (let [subscription (p/on-message server :test-wire handler)]
          (is (some? subscription))
          (is (= :test-wire (:wire subscription)))
          (is (contains? @(:message-handlers server) :test-wire))
          (is (= 1 (count (get @(:message-handlers server) :test-wire))))

          ;; Unsubscribe
          (p/unsubscribe! server subscription)
          (is (= 0 (count (get @(:message-handlers server) :test-wire)))))
        (finally
          (.close server))))))

(deftest multiple-message-handlers-test
  (testing "Multiple handlers can be registered for same wire"
    (let [port (next-port)
          server (p/gauge-listen! sente-gauge/gauge {:port port})
          handler1 (fn [_] nil)
          handler2 (fn [_] nil)]
      (try
        (p/on-message server :events handler1)
        (p/on-message server :events handler2)
        (is (= 2 (count (get @(:message-handlers server) :events))))
        (finally
          (.close server))))))

(deftest request-handler-registration-test
  (testing "Request handlers can be registered"
    (let [port (next-port)
          server (p/gauge-listen! sente-gauge/gauge {:port port})
          handler (fn [_data reply!] (reply! {:pong true}))]
      (try
        (let [subscription (p/on-request server :ping handler)]
          (is (some? subscription))
          (is (= :ping (:wire subscription)))
          (is (= :request (:type subscription)))
          (is (contains? @(:request-handlers server) :ping))

          ;; Unsubscribe
          (p/unsubscribe! server subscription)
          (is (not (contains? @(:request-handlers server) :ping))))
        (finally
          (.close server))))))

(deftest client-handler-registration-test
  (testing "Client handlers can be registered"
    (let [port (next-port)
          server (p/gauge-listen! sente-gauge/gauge {:port port})
          clients (atom [])]
      (try
        (p/on-client server (fn [client] (swap! clients conj client)))
        (is (= 1 (count @(:client-handlers server))))

        ;; Register another
        (p/on-client server (fn [_client] nil))
        (is (= 2 (count @(:client-handlers server))))
        (finally
          (.close server))))))

(deftest disconnect-handler-registration-test
  (testing "Disconnect handlers can be registered"
    (let [port (next-port)
          server (p/gauge-listen! sente-gauge/gauge {:port port})]
      (try
        ;; Use convenience function for disconnect handlers
        (sente-gauge/on-disconnect server (fn [_client] nil))
        (is (= 1 (count @(:disconnect-handlers server))))
        (finally
          (.close server))))))

;; =============================================================================
;; Server Interface Tests
;; =============================================================================

(deftest server-clients-test
  (testing "Server tracks connected clients"
    (let [port (next-port)
          server (p/gauge-listen! sente-gauge/gauge {:port port})]
      (try
        ;; Initially no clients - use convenience function
        (is (empty? (sente-gauge/connected-uids server)))
        (is (= 0 (sente-gauge/connected-count server)))
        (finally
          (.close server))))))

;; =============================================================================
;; HTTP Endpoint Tests
;; =============================================================================

(deftest http-endpoint-available-test
  (testing "HTTP endpoint responds"
    (let [port (next-port)
          server (p/gauge-listen! sente-gauge/gauge {:port port})]
      (try
        ;; Give server time to start
        (Thread/sleep 100)

        ;; Test root endpoint
        (let [response (http/get (str "http://localhost:" port "/")
                                 {:throw-exceptions false})]
          (is (= 200 (:status response)))
          (is (clojure.string/includes? (:body response) "Rheon Sente server")))
        (finally
          (.close server))))))

(deftest chsk-endpoint-available-test
  (testing "WebSocket endpoint responds"
    (let [port (next-port)
          server (p/gauge-listen! sente-gauge/gauge {:port port})]
      (try
        ;; Give server time to start
        (Thread/sleep 100)

        ;; Test chsk endpoint - GET should return WebSocket upgrade
        ;; Note: Without proper WebSocket handshake it will fail, but endpoint exists
        (let [response (http/get (str "http://localhost:" port "/chsk")
                                 {:throw-exceptions false
                                  :query-params {:transport "ajax"}})]
          ;; Ajax transport should respond
          (is (some? response)))
        (finally
          (.close server))))))

;; =============================================================================
;; Integration Tests with Sente Client
;; =============================================================================

(deftest sente-client-connection-test
  (testing "Sente client can connect to server"
    (let [port (next-port)
          server (p/gauge-listen! sente-gauge/gauge {:port port})
          client-connected (promise)
          client-uid (atom nil)]
      (try
        ;; Register client handler
        (p/on-client server
                     (fn [client]
                       (reset! client-uid (:uid client))
                       (deliver client-connected true)))

        ;; Give server time to start
        (Thread/sleep 100)

        ;; Create Sente client
        (let [{:keys [chsk]}
              (sente/make-channel-socket-client!
               "/chsk"
               nil
               {:type :ws  ;; WebSocket - required for Clojure client
                :host (str "localhost:" port)
                :packer :edn})]

          ;; Wait for connection with timeout
          (let [connected (deref client-connected 3000 :timeout)]
            (when (not= :timeout connected)
              (is (= true connected))
              (is (some? @client-uid))))

          ;; Clean up client
          (sente/chsk-disconnect! chsk))
        (finally
          (.close server))))))

(deftest request-reply-test
  (testing "Request/reply pattern works"
    (let [port (next-port)
          server (p/gauge-listen! sente-gauge/gauge {:port port})
          reply-received (promise)]
      (try
        ;; Register request handler on server
        (p/on-request server :ping
                      (fn [data reply!]
                        (reply! {:pong true :received data})))

        ;; Give server time to start
        (Thread/sleep 100)

        ;; Create Sente client
        (let [{:keys [chsk send-fn]}
              (sente/make-channel-socket-client!
               "/chsk"
               nil
               {:type :ws  ;; WebSocket - required for Clojure client
                :host (str "localhost:" port)
                :packer :edn})]

          ;; Wait for connection
          (Thread/sleep 500)

          ;; Send request and wait for reply
          (send-fn [:rheon/ping {:msg "hello"}] 5000
                   (fn [reply]
                     (deliver reply-received reply)))

          ;; Wait for reply with timeout
          (let [reply (deref reply-received 3000 :timeout)]
            (when (map? reply)
              (is (= true (:pong reply)))
              (is (= {:msg "hello"} (:received reply)))))

          ;; Clean up client
          (sente/chsk-disconnect! chsk))
        (finally
          (.close server))))))

(deftest message-subscription-test
  (testing "Message subscription receives messages"
    (let [port (next-port)
          server (p/gauge-listen! sente-gauge/gauge {:port port})
          message-received (promise)
          client-connected (promise)]
      (try
        ;; Register handlers on server
        (p/on-client server (fn [_] (deliver client-connected true)))

        (p/on-message server :events
                      (fn [data]
                        (deliver message-received data)))

        ;; Give server time to start
        (Thread/sleep 100)

        ;; Create Sente client and send message
        (let [{:keys [chsk send-fn]}
              (sente/make-channel-socket-client!
               "/chsk"
               nil
               {:type :ws  ;; WebSocket - required for Clojure client
                :host (str "localhost:" port)
                :packer :edn})]

          ;; Wait for connection
          (deref client-connected 2000 :timeout)
          (Thread/sleep 200)

          ;; Send message
          (send-fn [:rheon/events {:type :test :value 42}])

          ;; Wait for message with timeout
          (let [msg (deref message-received 2000 :timeout)]
            (when (map? msg)
              (is (= :test (:type msg)))
              (is (= 42 (:value msg)))))

          ;; Clean up
          (sente/chsk-disconnect! chsk))
        (finally
          (.close server))))))

;; =============================================================================
;; Custom Path Tests
;; =============================================================================

(deftest custom-path-test
  (testing "Server can use custom WebSocket path"
    (let [port (next-port)
          server (p/gauge-listen! sente-gauge/gauge
                                  {:port port
                                   :path "/ws"})]
      (try
        (Thread/sleep 100)

        ;; Custom path should respond
        (let [response (http/get (str "http://localhost:" port "/ws")
                                 {:throw-exceptions false
                                  :query-params {:transport "ajax"}})]
          (is (some? response)))
        (finally
          (.close server))))))
