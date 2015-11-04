(ns adserver.db
  (:require [clojure.string :as str]
            [clojure.java.jdbc :as jdbc]
            [medley.core :as medley])
  (:import [com.jolbox.bonecp BoneCPDataSource]
           [org.flywaydb.core Flyway]))

(defn pooled-datasource
  [db-spec]
  (let [{:keys [classname subprotocol subname user password
                init-pool-size max-pool-size idle-time partitions]} db-spec
        min-connections (inc (quot init-pool-size partitions))
        max-connections (inc (quot max-pool-size partitions))
        cpds (doto (BoneCPDataSource.)
                   (.setDriverClass classname)
                   (.setJdbcUrl (str "jdbc:" subprotocol ":" subname))
                   (.setUsername user)
                   (.setPassword password)
                   (.setMinConnectionsPerPartition min-connections)
                   (.setMaxConnectionsPerPartition max-connections)
                   (.setPartitionCount partitions)
                   (.setStatisticsEnabled true)
                   (.setIdleMaxAgeInMinutes (or idle-time 60)))]
       {:datasource cpds}))

(defn migrate
  [{:keys [datasource]}]
  (doto (Flyway.) (.setDataSource datasource) .migrate))

(defn clean
  [{:keys [datasource]}]
  (doto (Flyway.) (.setDataSource datasource) .clean .migrate))

(defn init
  [db-spec & {:keys [clean?]}]
  (let [ds (pooled-datasource db-spec)]
    (if clean?
      (clean ds)
      (migrate ds))
    ds))

(defn from-identifier
  [s]
  (-> (name s)
      (str/lower-case)
      (str/replace "_" "-")
      (keyword)))

(defn to-identifier
  [s]
  (-> (name s)
      (str/lower-case)
      (str/replace "-" "_")
      (keyword)))

(defn create-ad!
  "Insert an ad and image, return the generated key."
  [db ad image]
  (jdbc/with-db-transaction [t-con db]
    (let [[res] (jdbc/insert! t-con :ads (medley/map-keys to-identifier ad))
          ad-id (res :1)]
      (when image
        (jdbc/insert! t-con :images (medley/map-keys to-identifier (assoc image :ad-id ad-id))))
      ad-id)))

(defn retrieve-ad
  [db id]
  (jdbc/with-db-transaction [t-con db]
    (first (jdbc/query t-con ["SELECT ads.ad_id, ads.title, ads.content, ads.width, ads.height,
                                      ads.url, ads.is_active, ads.created_at, ads.updated_at,
                                      COUNT(DISTINCT click_id) AS clicks
                               FROM ads
                               LEFT OUTER JOIN clicks USING(ad_id)
                               WHERE ads.ad_id = ?
                               GROUP BY ads.ad_id, ads.title, ads.content, ads.width, ads.height,
                                        ads.url, ads.is_active, ads.created_at, ads.updated_at"
                              id]
                       :identifiers from-identifier))))

(defn retrieve-image
  [db id]
  (jdbc/with-db-transaction [t-con db]
    (first (jdbc/query t-con ["SELECT ad_id, size, content_type, content_bytes, created_at, updated_at
                               FROM images
                               WHERE ad_id = ?"
                              id]
                       :identifiers from-identifier))))

(defn retrieve-random-ad
  [db]
  (jdbc/with-db-transaction [t-con db]
    (let [[{:keys [n]}] (jdbc/query t-con ["SELECT COUNT(*) AS n FROM ads WHERE is_active"])]
      (first (jdbc/query t-con ["SELECT ad_id, title, content, width, height, url
                                 FROM ads
                                 WHERE is_active
                                 OFFSET ? ROWS
                                 FETCH NEXT 1 ROW ONLY"
                                (rand-int n)]
                         :identifiers from-identifier)))))

(defn list-ads
  [db]
  (jdbc/with-db-transaction [t-con db]
    (jdbc/query t-con ["SELECT ads.ad_id, ads.title, ads.created_at, ads.updated_at, ads.is_active,
                               COUNT(DISTINCT click_id) AS clicks
                        FROM ads
                        LEFT OUTER JOIN clicks USING(ad_id)
                        GROUP BY ads.ad_id, ads.title, ads.created_at, ads.updated_at, ads.is_active
                        ORDER BY ads.ad_id"])))

(defn create-click!
  [db click]
  (jdbc/with-db-transaction [t-con db]
    (jdbc/insert! t-con :clicks (medley/map-keys to-identifier click))))

(defn delete-ad!
  [db id]
  (jdbc/with-db-transaction [t-con db]
    (jdbc/execute! t-con ["DELETE FROM clicks WHERE ad_id = ?" id])
    (jdbc/execute! t-con ["DELETE FROM images WHERE ad_id = ?" id])
    (jdbc/execute! t-con ["DELETE FROM ads WHERE ad_id = ?" id])))


(comment
  (require '[inflections.core :refer [ordinalize]])

  (defn create-fixtures
    [ds num-fixtures]
    (jdbc/with-db-transaction [t-con ds]
      (dotimes [n num-fixtures]
        (create-ad! t-con
                    {:title (str "Ad " n)
                     :content (str "The " (ordinalize (inc n)) " ad.")
                     :is-active (rand-nth [true false])
                     :width 10
                     :height 20
                     :url (str "http://example.com/ad/" n)}
                    nil)))))
