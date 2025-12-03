(ns replicant-kit.router
  "URL parsing and routing utilities.

   These are reusable functions for URL<->location conversion.
   Your app provides the routes definition."
  (:require [domkm.silk :as silk]
            [lambdaisland.uri :as uri]))

(defn url->location
  "Parse a URL into a location map using silk routes.

   Returns:
   {:location/page-id :page-name
    :location/params {:id \"123\"}
    :location/query-params {\"foo\" \"bar\"}
    :location/hash-params {\"tab\" \"1\"}}"
  [routes url]
  (let [uri (cond-> url (string? url) uri/uri)]
    (when-let [arrived (silk/arrive routes (:path uri))]
      (let [query-params (uri/query-map uri)
            hash-params (some-> uri :fragment uri/query-string->map)]
        (cond-> {:location/page-id (:domkm.silk/name arrived)
                 :location/params (dissoc arrived
                                          :domkm.silk/name
                                          :domkm.silk/pattern
                                          :domkm.silk/routes
                                          :domkm.silk/url)}
          (seq query-params) (assoc :location/query-params query-params)
          (seq hash-params) (assoc :location/hash-params hash-params))))))

(defn location->url
  "Convert a location map back to a URL string."
  [routes {:location/keys [page-id params query-params hash-params]}]
  (cond-> (silk/depart routes page-id params)
    (seq query-params)
    (str "?" (uri/map->query-string query-params))

    (seq hash-params)
    (str "#" (uri/map->query-string hash-params))))

(defn essentially-same?
  "Check if two locations are essentially the same (same page, params, query)."
  [l1 l2]
  (and (= (:location/page-id l1) (:location/page-id l2))
       (= (not-empty (:location/params l1))
          (not-empty (:location/params l2)))
       (= (not-empty (:location/query-params l1))
          (not-empty (:location/query-params l2)))))
