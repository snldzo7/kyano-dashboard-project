(ns dev.build
  "Build hooks for running esbuild alongside shadow-cljs.
   Based on https://www.metosin.fi/blog/2024-09-05-using-shadow-cljs-with-esbuild"
  (:require [clojure.java.io :as io]
            [shadow.build :as build]))

(defonce ^:private esbuild-process (atom nil))

(defn- ensure-target-dir []
  (.mkdirs (io/file "target")))

(defn- start-esbuild-watch! []
  (ensure-target-dir)
  (when-let [p @esbuild-process]
    (.destroy p))
  (let [pb (ProcessBuilder. ["node" "esbuild.watch.mjs"])
        _ (.inheritIO pb)
        p (.start pb)]
    (reset! esbuild-process p)
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. (fn [] (when-let [p @esbuild-process]
                                        (.destroy p)))))
    (println "Started esbuild watch process")))

(defn- run-esbuild-build! []
  (ensure-target-dir)
  (let [pb (ProcessBuilder. ["node" "esbuild.build.mjs"])
        _ (.inheritIO pb)
        p (.start pb)
        exit-code (.waitFor p)]
    (when (not= 0 exit-code)
      (throw (ex-info "esbuild build failed" {:exit-code exit-code})))))

;; Build hook for dev mode - runs esbuild in watch mode
(defn run-esbuild-watch
  {:shadow.build/stage :configure}
  [build-state]
  (start-esbuild-watch!)
  build-state)

;; Build hook for release mode - runs esbuild once
(defn run-esbuild-build
  {:shadow.build/stage :flush}
  [build-state]
  (run-esbuild-build!)
  build-state)
