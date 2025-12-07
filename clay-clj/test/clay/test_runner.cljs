(ns clay.test-runner
  "Test runner for Clay layout engine browser tests."
  (:require [cljs.test :as t :include-macros true]
            ;; Specter-based tests
            [clay.tree-test]
            [clay.layout-test]
            [clay.hiccup2-test]))

(defn ^:export run-tests []
  (t/run-tests
    'clay.tree-test
    'clay.layout-test
    'clay.hiccup2-test))
