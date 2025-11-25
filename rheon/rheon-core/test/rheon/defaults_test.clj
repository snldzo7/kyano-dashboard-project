(ns rheon.defaults-test
  (:require [clojure.test :refer [deftest testing is]]
            [rheon.defaults :as defaults]))

(deftest defaults-exist-test
  (testing "All default vars exist and have values"
    (is (keyword? defaults/gauge))
    (is (keyword? defaults/mode))
    (is (number? defaults/buffer-size))
    (is (keyword? defaults/drop-policy))
    (is (number? defaults/timeout-ms))))

(deftest default-values-test
  (testing "Default values are sane"
    (is (= :mem defaults/gauge) "Default gauge should be :mem")
    (is (= :discrete defaults/mode) "Default mode should be :discrete")
    (is (= 100 defaults/buffer-size))
    (is (= :oldest defaults/drop-policy))
    (is (= 5000 defaults/timeout-ms))))

(deftest get-default-test
  (testing "get-default returns correct values"
    (is (= :mem (defaults/get-default :gauge)))
    (is (= :discrete (defaults/get-default :mode)))
    (is (nil? (defaults/get-default :nonexistent)))))

(deftest merge-with-defaults-test
  (testing "merge-with-defaults merges correctly"
    (let [merged (defaults/merge-with-defaults {:gauge :kafka})]
      (is (= :kafka (:gauge merged)) "Custom value should override")
      (is (= :discrete (:mode merged)) "Default should be preserved"))))

(deftest all-defaults-test
  (testing "all-defaults returns complete map"
    (let [all (defaults/all-defaults)]
      (is (map? all))
      (is (contains? all :gauge))
      (is (contains? all :mode))
      (is (contains? all :buffer-size))
      (is (contains? all :timeout-ms)))))
