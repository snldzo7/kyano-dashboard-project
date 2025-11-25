(ns rheon.conn
  "Rheon Connection type.

   Connections implement:
   - java.io.Closeable    - for with-open support
   - clojure.lang.ILookup - for (:wire conn) access
   - IConnection          - for send!, request!, on-message, etc."
  (:require [rheon.protocols :as p]
            [rheon.defaults :as defaults]))

;; =============================================================================
;; Connection Record
;; =============================================================================

(defrecord Connection [gauge        ;; IGauge implementation
                       gauge-state  ;; State returned by gauge-connect!
                       wire-config  ;; atom: {wire -> {:mode :buffer-size ...}}
                       opts]        ;; Connection options

  p/IConnection

  (send! [this wire data]
    (p/send! this wire data {}))

  (send! [_this wire data opts]
    (p/gauge-send! gauge gauge-state wire data opts))

  (request! [_this wire data opts]
    (let [opts' (merge {:timeout-ms defaults/timeout-ms} opts)]
      (p/gauge-request! gauge gauge-state wire data opts')))

  (on-message [_this wire handler]
    (p/gauge-subscribe! gauge gauge-state wire handler))

  (on-request [_this wire handler]
    (p/gauge-on-request! gauge gauge-state wire handler))

  (unsubscribe! [_this subscription]
    (p/gauge-unsubscribe! gauge subscription))

  (configure! [_this wire opts]
    (swap! wire-config assoc wire (defaults/merge-with-defaults opts)))

  ;; Closeable - for with-open
  ;; Note: defrecord already implements ILookup for record fields
  #?@(:clj
      [java.io.Closeable
       (close [_this]
         (p/gauge-close! gauge gauge-state))]))

;; =============================================================================
;; Server Record
;; =============================================================================

(defrecord Server [gauge        ;; IGauge implementation
                   gauge-state  ;; State returned by gauge-listen!
                   opts]        ;; Server options

  p/IServer
  (on-client [_this handler]
    (p/on-client gauge-state handler))

  ;; Also implement IConnection so server can receive messages
  ;; Note: defrecord already implements ILookup for record fields
  p/IConnection

  (send! [this wire data]
    (p/send! this wire data {}))

  (send! [_this wire data opts]
    (p/gauge-send! gauge gauge-state wire data opts))

  (request! [_this wire data opts]
    (let [opts' (merge {:timeout-ms defaults/timeout-ms} opts)]
      (p/gauge-request! gauge gauge-state wire data opts')))

  (on-message [_this wire handler]
    (p/gauge-subscribe! gauge gauge-state wire handler))

  (on-request [_this wire handler]
    (p/gauge-on-request! gauge gauge-state wire handler))

  (unsubscribe! [_this subscription]
    (p/gauge-unsubscribe! gauge subscription))

  (configure! [_this _wire _opts]
    ;; Server doesn't support per-wire config yet
    nil)

  ;; Closeable
  #?@(:clj
      [java.io.Closeable
       (close [_this]
         ;; TODO: Close server
         nil)]))

;; =============================================================================
;; Constructor Functions
;; =============================================================================

(defn make-connection
  "Create a new Connection."
  [gauge gauge-state opts]
  (->Connection gauge gauge-state (atom {}) opts))

(defn make-server
  "Create a new Server."
  [gauge gauge-state opts]
  (->Server gauge gauge-state opts))

;; =============================================================================
;; Manual Close for ClojureScript
;; =============================================================================

#?(:cljs
   (defn close!
     "Close a connection or server. Use this in ClojureScript since
      Closeable is not available."
     [conn-or-server]
     (p/gauge-close! (:gauge conn-or-server)
                     (:gauge-state conn-or-server))))
