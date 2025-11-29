(ns rheon.core-test
  "Tests for Rheon v2 core API."
  (:require [clojure.test :refer [deftest testing is]]
            [rheon.core :as r]
            [missionary.core :as m]))

;; =============================================================================
;; Connection Tests
;; =============================================================================

(deftest connection-test
  (testing "creates in-memory connection by default"
    (let [conn (r/connection {:transport :mem})]
      (is (some? conn))))

  (testing "throws on unknown transport"
    (is (thrown-with-msg? Exception #"Unknown transport"
          (r/connection {:transport :unknown})))))

;; =============================================================================
;; Stream Tests
;; =============================================================================

(deftest stream-basic-test
  (testing "stream emit/listen works with sequence numbers"
    (let [conn (r/connection {:transport :mem})
          mouse (r/stream :mouse conn)
          received (atom [])]
      (r/listen mouse (fn [data] (swap! received conj data)))
      (r/emit! mouse {:x 100 :y 200})
      (r/emit! mouse {:x 101 :y 201})
      ;; Data is enriched with :rheon/seq for ordering
      (is (= [{:x 100 :y 200 :rheon/seq 1} {:x 101 :y 201 :rheon/seq 2}] @received)))))

(deftest stream-multiple-listeners-test
  (testing "multiple listeners receive same emissions with sequence numbers"
    (let [conn (r/connection {:transport :mem})
          mouse (r/stream :mouse conn)
          received-1 (atom [])
          received-2 (atom [])]
      (r/listen mouse (fn [data] (swap! received-1 conj data)))
      (r/listen mouse (fn [data] (swap! received-2 conj data)))
      (r/emit! mouse {:x 100})
      (is (= [{:x 100 :rheon/seq 1}] @received-1))
      (is (= [{:x 100 :rheon/seq 1}] @received-2)))))

(deftest stream-unsubscribe-test
  (testing "unsubscribe stops receiving"
    (let [conn (r/connection {:transport :mem})
          mouse (r/stream :mouse conn)
          received (atom [])
          sub (r/listen mouse (fn [data] (swap! received conj data)))]
      (r/emit! mouse {:x 1})
      (r/unsubscribe! sub)
      (r/emit! mouse {:x 2})
      (is (= [{:x 1 :rheon/seq 1}] @received)))))

(deftest stream-idempotent-test
  (testing "same wire-id returns same wire"
    (let [conn (r/connection {:transport :mem})
          mouse1 (r/stream :mouse conn)
          mouse2 (r/stream :mouse conn)]
      (is (= mouse1 mouse2)))))

;; =============================================================================
;; Discrete Tests
;; =============================================================================

(deftest discrete-basic-test
  (testing "discrete send/reply works"
    (let [conn (r/connection {:transport :mem})
          clock (r/discrete :clock conn)
          reply-received (atom nil)]
      ;; Set up handler that returns reply
      (r/reply! clock
        (fn [{:keys [client-time]}]
          {:server-time 1000
           :gap (- 1000 client-time)}))
      ;; Send request
      (r/send! clock
        {:client-time 900}
        {:on-reply (fn [reply] (reset! reply-received reply))})
      (is (= {:server-time 1000 :gap 100} @reply-received)))))

(deftest discrete-no-handler-test
  (testing "send with no handler calls on-error"
    (let [conn (r/connection {:transport :mem})
          clock (r/discrete :clock conn)
          error-received (atom nil)]
      (r/send! clock
        {:data "test"}
        {:on-error (fn [e] (reset! error-received e))})
      (is (some? @error-received)))))

(deftest discrete-handler-error-test
  (testing "handler exception calls on-error"
    (let [conn (r/connection {:transport :mem})
          clock (r/discrete :clock conn)
          error-received (atom nil)]
      (r/reply! clock (fn [_] (throw (ex-info "Handler error" {}))))
      (r/send! clock
        {:data "test"}
        {:on-error (fn [e] (reset! error-received e))})
      (is (some? @error-received)))))

(deftest discrete-unsubscribe-test
  (testing "unsubscribe removes handler"
    (let [conn (r/connection {:transport :mem})
          clock (r/discrete :clock conn)
          sub (r/reply! clock (fn [_] {:result "ok"}))
          error-received (atom nil)]
      (r/unsubscribe! sub)
      (r/send! clock
        {:data "test"}
        {:on-error (fn [e] (reset! error-received e))})
      (is (some? @error-received)))))

;; =============================================================================
;; Signal Tests
;; =============================================================================

(deftest signal-basic-test
  (testing "signal watch gets initial value"
    (let [conn (r/connection {:transport :mem})
          status (r/signal :status conn {:state :starting})
          received (atom [])]
      (r/watch status (fn [v] (swap! received conj v)))
      (is (= [{:state :starting}] @received)))))

(deftest signal-update-test
  (testing "signal! notifies watchers"
    (let [conn (r/connection {:transport :mem})
          status (r/signal :status conn {:state :starting})
          received (atom [])]
      (r/watch status (fn [v] (swap! received conj v)))
      (r/signal! status {:state :running})
      (r/signal! status {:state :stopped})
      (is (= [{:state :starting}
              {:state :running}
              {:state :stopped}] @received)))))

(deftest signal-multiple-watchers-test
  (testing "multiple watchers all notified"
    (let [conn (r/connection {:transport :mem})
          status (r/signal :status conn :initial)
          received-1 (atom [])
          received-2 (atom [])]
      (r/watch status (fn [v] (swap! received-1 conj v)))
      (r/watch status (fn [v] (swap! received-2 conj v)))
      (r/signal! status :updated)
      (is (= [:initial :updated] @received-1))
      (is (= [:initial :updated] @received-2)))))

(deftest signal-unsubscribe-test
  (testing "unsubscribe stops watching"
    (let [conn (r/connection {:transport :mem})
          status (r/signal :status conn :initial)
          received (atom [])
          sub (r/watch status (fn [v] (swap! received conj v)))]
      (r/unsubscribe! sub)
      (r/signal! status :updated)
      (is (= [:initial] @received)))))

;; =============================================================================
;; Wire Type Mismatch Tests
;; =============================================================================

(deftest wire-type-mismatch-test
  (testing "same name different type throws"
    (let [conn (r/connection {:transport :mem})]
      (r/stream :my-wire conn)
      (is (thrown-with-msg? Exception #"already exists"
            (r/discrete :my-wire conn))))))

;; =============================================================================
;; Connection Close Tests
;; =============================================================================

(deftest close-connection-test
  (testing "close clears all wires"
    (let [conn (r/connection {:transport :mem})
          _ (r/stream :mouse conn)
          _ (r/discrete :clock conn)]
      (r/close! conn)
      ;; After close, can create wires with same names
      (is (some? (r/stream :mouse conn))))))

;; =============================================================================
;; Hub Tests - Multi-participant communication
;; =============================================================================

(deftest hub-stream-cross-connection-test
  (testing "stream emit on one connection reaches listeners on another"
    (let [hub (r/create-hub)
          server (r/connection {:transport :mem :hub hub})
          client (r/connection {:transport :mem :hub hub})
          ;; Server emits on mouse wire
          mouse-server (r/stream :mouse server)
          ;; Client listens on mouse wire
          mouse-client (r/stream :mouse client)
          received (atom [])]
      ;; Client listens
      (r/listen mouse-client (fn [data] (swap! received conj data)))
      ;; Server emits
      (r/emit! mouse-server {:x 100 :y 200})
      (r/emit! mouse-server {:x 101 :y 201})
      ;; Client receives
      (is (= [{:x 100 :y 200 :rheon/seq 1}
              {:x 101 :y 201 :rheon/seq 2}] @received)))))

(deftest hub-stream-bidirectional-test
  (testing "both connections can emit and listen"
    (let [hub (r/create-hub)
          server (r/connection {:transport :mem :hub hub})
          client (r/connection {:transport :mem :hub hub})
          mouse-server (r/stream :mouse server)
          mouse-client (r/stream :mouse client)
          server-received (atom [])
          client-received (atom [])]
      ;; Both listen
      (r/listen mouse-server (fn [data] (swap! server-received conj (:from data))))
      (r/listen mouse-client (fn [data] (swap! client-received conj (:from data))))
      ;; Server emits
      (r/emit! mouse-server {:from :server})
      ;; Client emits
      (r/emit! mouse-client {:from :client})
      ;; Both receive both emissions
      (is (= [:server :client] @server-received))
      (is (= [:server :client] @client-received)))))

(deftest hub-discrete-cross-connection-test
  (testing "discrete send/reply works across connections"
    (let [hub (r/create-hub)
          server (r/connection {:transport :mem :hub hub})
          client (r/connection {:transport :mem :hub hub})
          clock-server (r/discrete :clock server)
          clock-client (r/discrete :clock client)
          reply-received (atom nil)]
      ;; Server handles requests
      (r/reply! clock-server
        (fn [{:keys [client-time]}]
          {:server-time 1000
           :gap (- 1000 client-time)}))
      ;; Client sends request
      (r/send! clock-client
        {:client-time 900}
        {:on-reply (fn [reply] (reset! reply-received reply))})
      ;; Client receives reply
      (is (= {:server-time 1000 :gap 100} @reply-received)))))

(deftest hub-signal-cross-connection-test
  (testing "signal updates are visible across connections"
    (let [hub (r/create-hub)
          server (r/connection {:transport :mem :hub hub})
          client (r/connection {:transport :mem :hub hub})
          status-server (r/signal :status server {:state :starting})
          status-client (r/signal :status client {:state :starting})
          received (atom [])]
      ;; Client watches
      (r/watch status-client (fn [v] (swap! received conj v)))
      ;; Server updates
      (r/signal! status-server {:state :running})
      ;; Client sees initial + update
      (is (= [{:state :starting} {:state :running}] @received)))))

(deftest hub-same-wire-instance-test
  (testing "different connections get the same wire instance"
    (let [hub (r/create-hub)
          conn1 (r/connection {:transport :mem :hub hub})
          conn2 (r/connection {:transport :mem :hub hub})
          mouse1 (r/stream :mouse conn1)
          mouse2 (r/stream :mouse conn2)]
      ;; Same wire instance because they share the hub
      (is (= mouse1 mouse2)))))

(deftest standalone-isolation-test
  (testing "standalone connections (no hub) are isolated"
    (let [conn1 (r/connection {:transport :mem})
          conn2 (r/connection {:transport :mem})
          mouse1 (r/stream :mouse conn1)
          mouse2 (r/stream :mouse conn2)
          received1 (atom [])
          received2 (atom [])]
      ;; Both listen
      (r/listen mouse1 (fn [data] (swap! received1 conj data)))
      (r/listen mouse2 (fn [data] (swap! received2 conj data)))
      ;; Emit on conn1
      (r/emit! mouse1 {:from :conn1})
      ;; Emit on conn2
      (r/emit! mouse2 {:from :conn2})
      ;; Each only receives their own
      (is (= [{:from :conn1 :rheon/seq 1}] @received1))
      (is (= [{:from :conn2 :rheon/seq 1}] @received2)))))

;; =============================================================================
;; Flow Tests - Raw Missionary Access
;; =============================================================================

(deftest stream-flow-test
  (testing "->flow returns Missionary flow for stream"
    (let [conn (r/connection {:transport :mem})
          mouse (r/stream :mouse conn)
          flow (r/->flow mouse)
          received (atom [])
          ;; Start consuming the flow
          cancel ((m/reduce (fn [_ v] (swap! received conj v) nil) nil flow)
                  (fn [_] nil)   ;; success callback
                  (fn [_] nil))] ;; failure callback
      ;; Emit some values
      (r/emit! mouse {:x 100})
      (r/emit! mouse {:x 200})
      ;; Flow consumers receive values
      (is (= [{:x 100 :rheon/seq 1} {:x 200 :rheon/seq 2}] @received))
      ;; Cancel to clean up
      (cancel))))

(deftest stream-flow-cleanup-test
  (testing "cancelling flow removes callback"
    (let [conn (r/connection {:transport :mem})
          mouse (r/stream :mouse conn)
          flow (r/->flow mouse)
          received (atom [])
          cancel ((m/reduce (fn [_ v] (swap! received conj v) nil) nil flow)
                  (fn [_] nil)
                  (fn [_] nil))]
      (r/emit! mouse {:x 1})
      ;; Cancel
      (cancel)
      ;; This emission should not be received
      (r/emit! mouse {:x 2})
      (is (= [{:x 1 :rheon/seq 1}] @received)))))

(deftest signal-flow-test
  (testing "->flow returns Missionary continuous flow for signal"
    (let [conn (r/connection {:transport :mem})
          status (r/signal :status conn :initial)
          flow (r/->flow status)
          received (atom [])
          ;; m/watch returns continuous flow - emits immediately with current value
          cancel ((m/reduce (fn [_ v] (swap! received conj v) nil) nil flow)
                  (fn [_] nil)
                  (fn [_] nil))]
      ;; Should have received initial value
      (Thread/sleep 10) ;; Give time for async
      (is (= [:initial] @received))
      ;; Update signal
      (r/signal! status :running)
      (Thread/sleep 10)
      (is (= [:initial :running] @received))
      (cancel))))

(deftest discrete-flow-test
  (testing "->flow returns Missionary flow for discrete requests"
    (let [conn (r/connection {:transport :mem})
          clock (r/discrete :clock conn)
          flow (r/->flow clock)
          received (atom [])
          ;; Start consuming incoming requests
          cancel ((m/reduce (fn [_ v] (swap! received conj (:request v)) nil) nil flow)
                  (fn [_] nil)
                  (fn [_] nil))]
      ;; Set up a handler (otherwise send! will error)
      (r/reply! clock (fn [_] {:result :ok}))
      ;; Send some requests
      (r/send! clock {:t 1} {:on-reply (fn [_])})
      (r/send! clock {:t 2} {:on-reply (fn [_])})
      ;; Flow receives the request data
      (is (= [{:t 1} {:t 2}] @received))
      (cancel))))

(deftest flow-multiple-consumers-test
  (testing "multiple flow consumers all receive values"
    (let [conn (r/connection {:transport :mem})
          mouse (r/stream :mouse conn)
          flow (r/->flow mouse)
          received1 (atom [])
          received2 (atom [])
          cancel1 ((m/reduce (fn [_ v] (swap! received1 conj v) nil) nil flow)
                   (fn [_] nil) (fn [_] nil))
          cancel2 ((m/reduce (fn [_ v] (swap! received2 conj v) nil) nil flow)
                   (fn [_] nil) (fn [_] nil))]
      (r/emit! mouse {:x 100})
      (is (= [{:x 100 :rheon/seq 1}] @received1))
      (is (= [{:x 100 :rheon/seq 1}] @received2))
      (cancel1)
      (cancel2))))

(deftest flow-with-eduction-test
  (testing "flows can be composed with Missionary eduction"
    (let [conn (r/connection {:transport :mem})
          mouse (r/stream :mouse conn)
          flow (r/->flow mouse)
          ;; Filter to only x > 50
          filtered-flow (m/eduction (filter #(> (:x %) 50)) flow)
          received (atom [])
          cancel ((m/reduce (fn [_ v] (swap! received conj v) nil) nil filtered-flow)
                  (fn [_] nil)
                  (fn [_] nil))]
      (r/emit! mouse {:x 30})  ;; filtered out
      (r/emit! mouse {:x 100}) ;; passes
      (r/emit! mouse {:x 40})  ;; filtered out
      (r/emit! mouse {:x 200}) ;; passes
      (is (= [{:x 100 :rheon/seq 2} {:x 200 :rheon/seq 4}] @received))
      (cancel))))

;; =============================================================================
;; Wire-Ref Tests - Wire as Data
;; =============================================================================

(deftest wire-basic-test
  (testing "wire creates stream from wire-ref"
    (let [conn (r/connection {:transport :mem})
          mouse (r/wire conn {:wire-id :mouse :type :stream})
          received (atom [])]
      (r/listen mouse (fn [data] (swap! received conj data)))
      (r/emit! mouse {:x 100})
      (is (= [{:x 100 :rheon/seq 1}] @received))))

  (testing "wire creates discrete from wire-ref"
    (let [conn (r/connection {:transport :mem})
          clock (r/wire conn {:wire-id :clock :type :discrete})
          reply-received (atom nil)]
      (r/reply! clock (fn [data] {:echo data}))
      (r/send! clock {:t 1} {:on-reply (fn [r] (reset! reply-received r))})
      (is (= {:echo {:t 1}} @reply-received))))

  (testing "wire creates signal from wire-ref with initial value"
    (let [conn (r/connection {:transport :mem})
          status (r/wire conn {:wire-id :status :type :signal :initial :starting})
          received (atom [])]
      (r/watch status (fn [v] (swap! received conj v)))
      (is (= [:starting] @received)))))

(deftest wire-ref-round-trip-test
  (testing "wire-ref extracts ref from stream wire"
    (let [conn (r/connection {:transport :mem})
          mouse (r/wire conn {:wire-id :mouse :type :stream})
          ref (r/wire-ref mouse)]
      (is (= :mouse (:wire-id ref)))
      (is (= :stream (:type ref)))))

  (testing "wire-ref extracts ref from discrete wire"
    (let [conn (r/connection {:transport :mem})
          clock (r/wire conn {:wire-id :clock :type :discrete})
          ref (r/wire-ref clock)]
      (is (= :clock (:wire-id ref)))
      (is (= :discrete (:type ref)))))

  (testing "wire-ref extracts ref from signal wire with initial"
    (let [conn (r/connection {:transport :mem})
          status (r/wire conn {:wire-id :status :type :signal :initial {:state :starting}})
          ref (r/wire-ref status)]
      (is (= :status (:wire-id ref)))
      (is (= :signal (:type ref)))
      (is (= {:state :starting} (:initial ref))))))

(deftest wire-ref-validation-test
  (testing "wire throws on missing wire-id"
    (let [conn (r/connection {:transport :mem})]
      (is (thrown? Exception
            (r/wire conn {:type :stream})))))

  (testing "wire throws on missing type"
    (let [conn (r/connection {:transport :mem})]
      (is (thrown? Exception
            (r/wire conn {:wire-id :mouse})))))

  (testing "wire throws on invalid type"
    (let [conn (r/connection {:transport :mem})]
      (is (thrown? Exception
            (r/wire conn {:wire-id :mouse :type :invalid}))))))

(deftest wire-ref-auto-connect-test
  (testing "wire with :conn in ref auto-connects"
    (let [ref {:wire-id :mouse
               :type :stream
               :conn {:transport :mem}}
          mouse (r/wire ref)
          received (atom [])]
      (r/listen mouse (fn [data] (swap! received conj data)))
      (r/emit! mouse {:x 100})
      (is (= [{:x 100 :rheon/seq 1}] @received))))

  (testing "wire without :conn and no conn arg throws"
    (is (thrown-with-msg? Exception #"must have :conn"
          (r/wire {:wire-id :mouse :type :stream})))))

(deftest wire-ref-idempotent-test
  (testing "wire returns same instance for same wire-id"
    (let [conn (r/connection {:transport :mem})
          mouse1 (r/wire conn {:wire-id :mouse :type :stream})
          mouse2 (r/wire conn {:wire-id :mouse :type :stream})]
      (is (= mouse1 mouse2)))))

(deftest wire-ref-convenience-equivalence-test
  (testing "wire and stream produce equivalent results"
    (let [conn (r/connection {:transport :mem})
          mouse1 (r/wire conn {:wire-id :mouse1 :type :stream})
          mouse2 (r/stream :mouse2 conn)
          received1 (atom [])
          received2 (atom [])]
      (r/listen mouse1 (fn [d] (swap! received1 conj d)))
      (r/listen mouse2 (fn [d] (swap! received2 conj d)))
      (r/emit! mouse1 {:x 1})
      (r/emit! mouse2 {:x 2})
      (is (= [{:x 1 :rheon/seq 1}] @received1))
      (is (= [{:x 2 :rheon/seq 1}] @received2))))

  (testing "wire and discrete produce equivalent results"
    (let [conn (r/connection {:transport :mem})
          clock1 (r/wire conn {:wire-id :clock1 :type :discrete})
          clock2 (r/discrete :clock2 conn)
          reply1 (atom nil)
          reply2 (atom nil)]
      (r/reply! clock1 (fn [_] {:from :clock1}))
      (r/reply! clock2 (fn [_] {:from :clock2}))
      (r/send! clock1 {} {:on-reply #(reset! reply1 %)})
      (r/send! clock2 {} {:on-reply #(reset! reply2 %)})
      (is (= {:from :clock1} @reply1))
      (is (= {:from :clock2} @reply2))))

  (testing "wire and signal produce equivalent results"
    (let [conn (r/connection {:transport :mem})
          status1 (r/wire conn {:wire-id :status1 :type :signal :initial :init1})
          status2 (r/signal :status2 conn :init2)
          values1 (atom [])
          values2 (atom [])]
      (r/watch status1 (fn [v] (swap! values1 conj v)))
      (r/watch status2 (fn [v] (swap! values2 conj v)))
      (is (= [:init1] @values1))
      (is (= [:init2] @values2)))))

;; =============================================================================
;; Wire-Ref Integration Tests - Cross-connection scenarios
;; =============================================================================

(deftest wire-ref-hub-integration-test
  (testing "wire-ref works across hub connections"
    (let [hub (r/create-hub)
          server (r/connection {:transport :mem :hub hub})
          client (r/connection {:transport :mem :hub hub})
          ;; Server creates wire using wire-ref
          mouse-server (r/wire server {:wire-id :mouse :type :stream})
          ;; Client creates wire using wire-ref
          mouse-client (r/wire client {:wire-id :mouse :type :stream})
          received (atom [])]
      ;; Client listens
      (r/listen mouse-client (fn [d] (swap! received conj d)))
      ;; Server emits
      (r/emit! mouse-server {:x 100})
      ;; Verify cross-connection communication
      (is (= [{:x 100 :rheon/seq 1}] @received))))

  (testing "wire-ref round-trip through discrete reply"
    (let [hub (r/create-hub)
          server (r/connection {:transport :mem :hub hub})
          client (r/connection {:transport :mem :hub hub})
          ;; Registry wire that returns wire-refs
          registry-server (r/wire server {:wire-id :registry :type :discrete})
          registry-client (r/wire client {:wire-id :registry :type :discrete})
          returned-ref (atom nil)]
      ;; Server replies with a wire-ref
      (r/reply! registry-server
        (fn [{:keys [resource]}]
          {:wire-id (keyword (str (name resource) "-stream"))
           :type :stream}))
      ;; Client requests and gets wire-ref back
      (r/send! registry-client {:resource :events}
        {:on-reply (fn [ref] (reset! returned-ref ref))})
      ;; Verify returned ref is valid
      (is (= :events-stream (:wire-id @returned-ref)))
      (is (= :stream (:type @returned-ref)))
      ;; Client can use returned ref to create wire
      (let [events-wire (r/wire client @returned-ref)
            received (atom [])]
        (r/listen events-wire (fn [d] (swap! received conj d)))
        ;; Server creates same wire and emits
        (r/emit! (r/wire server @returned-ref) {:event :test})
        (is (= [{:event :test :rheon/seq 1}] @received))))))

;; =============================================================================
;; Spec Validation Tests - Message Validation using :spec
;; =============================================================================

(deftest spec-validation-stream-test
  (testing "emit! validates data against stream spec"
    (let [conn (r/connection {:transport :mem})
          mouse (r/wire conn {:wire-id :mouse
                              :type :stream
                              :spec [:map [:x :int] [:y :int]]})
          received (atom [])]
      (r/listen mouse (fn [d] (swap! received conj d)))
      ;; Valid data passes
      (r/emit! mouse {:x 100 :y 200})
      (is (= [{:x 100 :y 200 :rheon/seq 1}] @received))))

  (testing "emit! throws on invalid data"
    (let [conn (r/connection {:transport :mem})
          mouse (r/wire conn {:wire-id :mouse
                              :type :stream
                              :spec [:map [:x :int] [:y :int]]})]
      ;; Invalid data - x is not int
      (is (thrown-with-msg? Exception #"Data validation failed"
            (r/emit! mouse {:x "not-an-int" :y 200})))))

  (testing "emit! works without spec"
    (let [conn (r/connection {:transport :mem})
          mouse (r/wire conn {:wire-id :mouse :type :stream})
          received (atom [])]
      (r/listen mouse (fn [d] (swap! received conj d)))
      ;; Any data passes when no spec
      (r/emit! mouse {:anything "goes"})
      (is (= 1 (count @received))))))

(deftest spec-validation-discrete-test
  (testing "send! validates request data against discrete spec"
    (let [conn (r/connection {:transport :mem})
          clock (r/wire conn {:wire-id :clock
                              :type :discrete
                              :spec {:request [:map [:client-time :int]]
                                     :reply [:map [:server-time :int]]}})
          reply-received (atom nil)]
      (r/reply! clock (fn [{:keys [client-time]}]
                        {:server-time (+ client-time 100)}))
      ;; Valid request data passes
      (r/send! clock {:client-time 900}
               {:on-reply (fn [r] (reset! reply-received r))})
      (is (= {:server-time 1000} @reply-received))))

  (testing "send! throws on invalid request data"
    (let [conn (r/connection {:transport :mem})
          clock (r/wire conn {:wire-id :clock
                              :type :discrete
                              :spec {:request [:map [:client-time :int]]}})]
      ;; Invalid request - client-time is not int
      (is (thrown-with-msg? Exception #"Data validation failed"
            (r/send! clock {:client-time "not-an-int"} {})))))

  (testing "send! works without spec"
    (let [conn (r/connection {:transport :mem})
          clock (r/wire conn {:wire-id :clock :type :discrete})
          reply-received (atom nil)]
      (r/reply! clock (fn [_] {:result :ok}))
      ;; Any data passes when no spec
      (r/send! clock {:any "data"} {:on-reply #(reset! reply-received %)})
      (is (= {:result :ok} @reply-received)))))

(deftest spec-validation-signal-test
  (testing "signal! validates value against signal spec"
    (let [conn (r/connection {:transport :mem})
          status (r/wire conn {:wire-id :status
                               :type :signal
                               :initial {:state :starting :count 0}
                               :spec [:map [:state :keyword] [:count :int]]})
          received (atom [])]
      (r/watch status (fn [v] (swap! received conj v)))
      ;; Valid value passes
      (r/signal! status {:state :running :count 5})
      (is (= [{:state :starting :count 0} {:state :running :count 5}] @received))))

  (testing "signal! throws on invalid value"
    (let [conn (r/connection {:transport :mem})
          status (r/wire conn {:wire-id :status
                               :type :signal
                               :initial {:state :starting :count 0}
                               :spec [:map [:state :keyword] [:count :int]]})]
      ;; Invalid value - count is not int
      (is (thrown-with-msg? Exception #"Data validation failed"
            (r/signal! status {:state :running :count "not-an-int"})))))

  (testing "signal! works without spec"
    (let [conn (r/connection {:transport :mem})
          status (r/wire conn {:wire-id :status :type :signal :initial nil})
          received (atom [])]
      (r/watch status (fn [v] (swap! received conj v)))
      ;; Any value passes when no spec
      (r/signal! status {:any "value"})
      (is (= 2 (count @received))))))

(deftest spec-preserves-in-wire-ref-test
  (testing "spec is preserved in wire-ref round-trip"
    (let [conn (r/connection {:transport :mem})
          spec [:map [:x :int] [:y :int]]
          mouse (r/wire conn {:wire-id :mouse :type :stream :spec spec})
          ref (r/wire-ref mouse)]
      (is (= spec (:spec ref)))))

  (testing "nested spec structures are preserved"
    (let [conn (r/connection {:transport :mem})
          spec {:request [:map [:id :uuid]]
                :reply [:map [:data :any]]}
          api (r/wire conn {:wire-id :api :type :discrete :spec spec})
          ref (r/wire-ref api)]
      (is (= spec (:spec ref))))))

