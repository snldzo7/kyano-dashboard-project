(ns rheon.encoder
  "Default Transit encoders for Rheon.

   Transit is used internally for wire format encoding. Users don't need
   to interact with encoders directly - a sensible default is picked.

   Available formats:
   - :json     - Transit JSON (default, browser-compatible)
   - :msgpack  - Transit MessagePack (more compact, JVM only)

   Custom encoders can implement the IEncoder protocol."
  (:require [rheon.protocols :as p]
            [cognitect.transit :as transit])
  #?(:clj (:import [java.io ByteArrayInputStream ByteArrayOutputStream])))

;; =============================================================================
;; Transit JSON Encoder (default, works everywhere)
;; =============================================================================

#?(:clj
   (defrecord TransitJsonEncoder []
     p/IEncoder
     (encode [_ msg]
       (let [out (ByteArrayOutputStream.)
             w (transit/writer out :json)]
         (transit/write w msg)
         (.toString out "UTF-8")))

     (decode [_ data]
       (let [bytes (if (string? data)
                     (.getBytes ^String data "UTF-8")
                     data)
             in (ByteArrayInputStream. bytes)
             r (transit/reader in :json)]
         (transit/read r))))

   :cljs
   (defrecord TransitJsonEncoder [reader writer]
     p/IEncoder
     (encode [_ msg]
       (transit/write writer msg))

     (decode [_ data]
       (transit/read reader data))))

#?(:clj
   (defn transit-json-encoder
     "Create a Transit JSON encoder (default for browser compatibility)."
     []
     (->TransitJsonEncoder))

   :cljs
   (defn transit-json-encoder
     "Create a Transit JSON encoder (default for browser compatibility)."
     []
     (->TransitJsonEncoder
      (transit/reader :json)
      (transit/writer :json))))

;; =============================================================================
;; Transit MessagePack Encoder (JVM only, more compact)
;; =============================================================================

#?(:clj
   (defrecord TransitMsgpackEncoder []
     p/IEncoder
     (encode [_ msg]
       (let [out (ByteArrayOutputStream.)
             w (transit/writer out :msgpack)]
         (transit/write w msg)
         (.toByteArray out)))

     (decode [_ data]
       (let [bytes (cond
                     (string? data) (.getBytes ^String data "UTF-8")
                     (bytes? data) data
                     :else (byte-array data))
             in (ByteArrayInputStream. bytes)
             r (transit/reader in :msgpack)]
         (transit/read r)))))

#?(:clj
   (defn transit-msgpack-encoder
     "Create a Transit MessagePack encoder (JVM only, more compact)."
     []
     (->TransitMsgpackEncoder)))

;; =============================================================================
;; Default Encoder Selection
;; =============================================================================

(defn default-encoder
  "Get the default encoder for the current platform.

   Returns Transit JSON encoder which works everywhere.
   Users can override by passing :encoder in connection options."
  []
  (transit-json-encoder))

(defn encoder-for-format
  "Get an encoder for the specified format.

   Args:
     format - :json or :msgpack (JVM only)

   Returns an IEncoder implementation."
  [format]
  (case format
    :json (transit-json-encoder)
    #?(:clj :msgpack) #?(:clj (transit-msgpack-encoder))
    (throw (ex-info (str "Unknown encoder format: " format
                         ". Available: :json" #?(:clj ", :msgpack"))
                    {:format format}))))
