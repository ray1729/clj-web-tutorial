(ns adserver.db
  (:require [clojure.string :as str]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [medley.core :as medley])
  (:import [com.jolbox.bonecp BoneCPDataSource]
           [org.flywaydb.core Flyway]
           [java.io PushbackReader]))

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

(defn file-to-bytes
  [file]
  (let [bytes (byte-array (.length file))]
    (with-open [r (io/input-stream file)]
      (.read r bytes))
    bytes))

(defn blob-to-bytes
  [blob]
  (.getBytes blob 1 (.length blob)))

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
    (when-let [image (first (jdbc/query t-con
                                        ["SELECT ad_id, content_type, content_bytes, created_at, updated_at
                                          FROM images
                                          WHERE ad_id = ?"
                                         id]
                                        :identifiers from-identifier))]
      (update image :content-bytes blob-to-bytes))))

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
                        ORDER BY ads.ad_id"]
                :identifiers from-identifier)))

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

(defn load-fixtures
  [db]
  (let [fixtures (with-open [r (PushbackReader.
                                (io/reader (io/resource "fixtures/fixtures.edn")))]
                   (edn/read r))]
    (jdbc/with-db-transaction [t-con db]
      (doseq [f fixtures]
        (println (str "Loading " (:title f)))
        (let [ad (select-keys f [:title :content :url :height :width])
              image-bytes (file-to-bytes (io/file (io/resource (:image f))))
              image {:content-type "image/gif"
                     :content-bytes image-bytes
                     :size (count image-bytes)}]
          (create-ad! t-con ad image))))))
