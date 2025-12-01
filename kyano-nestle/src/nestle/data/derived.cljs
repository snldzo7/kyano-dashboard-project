(ns nestle.data.derived
  "Derived/calculated values from observations"
  (:require [nestle.data.observations :refer [observations]]))

;; -----------------------------------------------------------------------------
;; Error function for normal distribution CDF
;; -----------------------------------------------------------------------------

(defn erf
  "Approximation of the error function"
  [x]
  (let [a1 0.254829592
        a2 -0.284496736
        a3 1.421413741
        a4 -1.453152027
        a5 1.061405429
        p 0.3275911
        sign (if (neg? x) -1 1)
        x (Math/abs x)
        t (/ 1.0 (+ 1.0 (* p x)))
        y (- 1.0 (* (+ (* (+ (* (+ (* (+ (* a5 t) a4) t) a3) t) a2) t) a1)
                    t
                    (Math/exp (- (* x x)))))]
    (* sign y)))

;; -----------------------------------------------------------------------------
;; Calculate derived values from observations
;; -----------------------------------------------------------------------------

(defn calculate-derived
  "Calculate all derived values from observations"
  [obs]
  (let [;; Demand distribution
        demand-mean (* (get-in obs [:consensus-forecast-qty :value])
                       (+ 1 (get-in obs [:forecast-error-dist :value])))
        demand-std (* (get-in obs [:consensus-forecast-qty :value])
                      (get-in obs [:order-variability-cv :value]))

        ;; RPM flows
        rpm-inflows (* (get-in obs [:open-po-qty-rpm :value])
                       (get-in obs [:supplier-otif-dist :value]))
        rpm-available (+ (get-in obs [:rpm-opening-stock :value]) rpm-inflows)
        max-production-from-rpm (/ rpm-available
                                   (get-in obs [:rpm-consumption-ratio :value]))

        ;; FG flows
        fg-inflows (min (* (get-in obs [:planned-production-qty :value])
                           (get-in obs [:mfg-adherence-pct :value]))
                        max-production-from-rpm)
        fg-outflows demand-mean
        rpm-outflows (* fg-inflows (get-in obs [:rpm-consumption-ratio :value]))

        ;; Inventory positions
        fg-inventory-position (- (+ (get-in obs [:fg-opening-stock :value]) fg-inflows)
                                 fg-outflows
                                 (get-in obs [:fg-blocked-stock-proj :value]))
        rpm-inventory-position (- (+ (get-in obs [:rpm-opening-stock :value]) rpm-inflows)
                                  rpm-outflows)

        ;; Inventory values
        fg-inventory-value (* (max 0 fg-inventory-position)
                              (get-in obs [:fg-unit-cost :value]))
        rpm-inventory-value (* (max 0 rpm-inventory-position)
                               (get-in obs [:rpm-unit-cost :value]))
        total-inventory-value (+ fg-inventory-value rpm-inventory-value)

        ;; Service risk (normal CDF)
        available-fg (max 0 fg-inventory-position)
        z-score (/ (- demand-mean available-fg) (max 1 demand-std))
        service-risk (min 0.99 (max 0.01 (* 0.5 (+ 1 (erf (/ z-score (Math/sqrt 2)))))))

        ;; Cash impact
        holding-cost (* total-inventory-value (/ (get-in obs [:holding-cost-rate :value]) 12))
        stockout-cost (* service-risk
                         (get-in obs [:cost-of-failure-unit :value])
                         demand-mean
                         0.1)
        cash-impact (+ holding-cost stockout-cost)]

    {:demand-distribution {:mean demand-mean :std demand-std}
     :rpm-inflows rpm-inflows
     :rpm-outflows rpm-outflows
     :rpm-available rpm-available
     :max-production-from-rpm max-production-from-rpm
     :fg-inflows fg-inflows
     :fg-outflows fg-outflows
     :fg-inventory-position fg-inventory-position
     :rpm-inventory-position rpm-inventory-position
     :fg-inventory-value fg-inventory-value
     :rpm-inventory-value rpm-inventory-value
     :total-inventory-value total-inventory-value
     :service-risk service-risk
     :holding-cost holding-cost
     :stockout-cost stockout-cost
     :cash-impact cash-impact}))

;; -----------------------------------------------------------------------------
;; Pre-calculated derived values (static for now)
;; -----------------------------------------------------------------------------

(def derived (calculate-derived observations))

;; -----------------------------------------------------------------------------
;; Status determination
;; -----------------------------------------------------------------------------

(defn get-status
  "Determine status color based on value and thresholds"
  [value {:keys [good-below warning-below good-above warning-above]}]
  (cond
    ;; Lower is better
    good-below
    (cond
      (< value good-below) :good
      (< value warning-below) :warning
      :else :danger)

    ;; Higher is better
    good-above
    (cond
      (> value good-above) :good
      (> value warning-above) :warning
      :else :danger)

    :else :neutral))

(def statuses
  {:service-risk (get-status (:service-risk derived)
                             {:good-below 0.10 :warning-below 0.20})
   :supplier-otif (get-status (get-in observations [:supplier-otif-dist :value])
                              {:good-above 0.90 :warning-above 0.80})
   :forecast-error (get-status (get-in observations [:forecast-error-dist :value])
                               {:good-below 0.05 :warning-below 0.10})})
