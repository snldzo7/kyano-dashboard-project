(ns rheon.spec
  "Wire-ref and connection-ref schemas for Rheon v2.

   Wire-refs are data. This namespace defines what that data looks like.

   WireRef - describes a wire that can be instantiated
   ConnRef - describes a connection that can be established"
  (:require [malli.core :as m]
            [malli.error :as me]))

;; =============================================================================
;; Connection Reference Schema
;; =============================================================================

(def ConnRef
  "Schema for a connection reference.

   A ConnRef is pure data describing how to establish a connection.
   When a WireRef includes a :conn, the system can automatically
   establish the connection when instantiating the wire."
  [:map
   [:transport [:enum :ws-client :ws-server :mem]]
   [:url {:optional true} :string]
   [:opts {:optional true} :map]])

;; =============================================================================
;; Wire Reference Schema
;; =============================================================================

(def WireRef
  "Schema for a wire reference.

   A WireRef is pure data describing a wire that can be instantiated.
   Give it to `wire` to get a live wire back.

   The :spec field is where the magic happens:
   - For streams: the shape of emitted data
   - For discrete: {:request <schema> :reply <schema>}
   - For signals: the shape of the value

   When :reply is WireRef, you're declaring 'my reply IS a wire-ref'.
   The client then knows exactly what to do with it."
  [:map
   [:wire-id :keyword]
   [:type [:enum :stream :discrete :signal]]
   [:spec {:optional true} :any]
   [:initial {:optional true} :any]
   [:opts {:optional true} :map]
   [:conn {:optional true} ConnRef]])

;; =============================================================================
;; Validation Functions
;; =============================================================================

(defn valid-wire-ref?
  "Check if x is a valid wire-ref."
  [x]
  (m/validate WireRef x))

(defn valid-conn-ref?
  "Check if x is a valid conn-ref."
  [x]
  (m/validate ConnRef x))

(defn validate-wire-ref!
  "Validate a wire-ref, throwing on failure with a descriptive error."
  [ref]
  (when-not (m/validate WireRef ref)
    (throw (ex-info "Invalid wire-ref"
                    {:ref ref
                     :errors (me/humanize (m/explain WireRef ref))})))
  ref)

(defn validate-conn-ref!
  "Validate a conn-ref, throwing on failure with a descriptive error."
  [ref]
  (when-not (m/validate ConnRef ref)
    (throw (ex-info "Invalid conn-ref"
                    {:ref ref
                     :errors (me/humanize (m/explain ConnRef ref))})))
  ref)

;; =============================================================================
;; Message Validation Functions
;; =============================================================================

(defn validate-data
  "Validate data against a Malli schema.

   Args:
     schema - Malli schema (e.g., [:map [:x :int] [:y :int]])
     data   - Data to validate

   Returns:
     {:valid? true/false :errors <humanized errors or nil>}"
  [schema data]
  (if (m/validate schema data)
    {:valid? true :errors nil}
    {:valid? false :errors (me/humanize (m/explain schema data))}))

(defn validate-data!
  "Validate data against a Malli schema, throwing on failure.

   Args:
     schema - Malli schema
     data   - Data to validate
     context - Map with contextual info for error message

   Returns:
     data (unchanged) if valid

   Throws:
     ExceptionInfo with :data, :schema, :errors, and context info"
  [schema data context]
  (when-not (m/validate schema data)
    (throw (ex-info "Data validation failed"
                    (merge context
                           {:data data
                            :schema schema
                            :errors (me/humanize (m/explain schema data))}))))
  data)

(defn get-wire-spec
  "Extract the relevant spec from a wire-ref based on operation.

   For streams: returns the :spec directly (shape of emitted data)
   For discrete: returns {:request ... :reply ...} map
   For signals: returns the :spec directly (shape of signal value)

   Args:
     wire-ref - Wire reference map
     operation - :emit, :send, :reply, or :signal

   Returns:
     The schema to use for validation, or nil if no spec"
  [wire-ref operation]
  (let [spec (:spec wire-ref)
        wire-type (:type wire-ref)]
    (case wire-type
      :stream   spec  ;; Stream spec IS the data shape
      :discrete (case operation
                  :send  (:request spec)
                  :reply (:reply spec)
                  nil)
      :signal   spec  ;; Signal spec IS the value shape
      nil)))

(defn validate-wire-data!
  "Validate data for a wire operation using the wire's spec.

   This is the main validation entry point. It:
   1. Gets the wire-ref from the wire
   2. Extracts the appropriate spec for the operation
   3. Validates the data against the spec

   Args:
     wire-ref  - Wire reference map (from wire-ref function)
     operation - :emit, :send, :reply, or :signal
     data      - Data to validate

   Returns:
     data (unchanged) if valid or no spec defined

   Throws:
     ExceptionInfo if validation fails"
  [wire-ref operation data]
  (if-let [spec (get-wire-spec wire-ref operation)]
    (validate-data! spec data {:wire-id (:wire-id wire-ref)
                               :wire-type (:type wire-ref)
                               :operation operation})
    ;; No spec - pass through without validation
    data))

;; =============================================================================
;; Schema Registry (for nested WireRef references)
;; =============================================================================

(def registry
  "Malli registry with Rheon schemas.

   This allows specs to reference WireRef directly:
   {:request [:map [:resource :keyword]]
    :reply   WireRef}  ;; <-- The reply IS a wire-ref"
  {::wire-ref WireRef
   ::conn-ref ConnRef})
