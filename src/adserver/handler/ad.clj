(ns adserver.handler.ad
  (:require [ring.util.response :refer [response content-type redirect]]
            [clojure.string :as str]
            [stencil.core :as stencil]
            [adserver.db :as db]
            [adserver.view.common :as common]))

(defn handle-random-ad
  [request]
  (when-let [{:keys [ad-id title height width content]} (db/retrieve-random-ad (:db-conn request))]
    (-> (response (stencil/render-file "templates/ad"
                                       {:click-url (str "/ad/click/" ad-id)
                                        :title     title
                                        :image-url (str "/ad/image/" ad-id)
                                        :height    height
                                        :width     width
                                        :content   (-> content
                                                       (str/replace "\"" "\\\"")
                                                       (str/replace "'" "\\'")
                                                       (str/replace "\n" "\\\n"))}))
        (content-type "application/javascript"))))

(defn handle-image
  [{:keys [db-conn params] :as request}]
  (when-let [image (db/retrieve-image db-conn (:id params))]
    (-> (response (:content-bytes image))
        (content-type (:content-type image)))))

(defn handle-click
  [{:keys [db-conn remote-addr params] :as request}]
  (when-let [ad (db/retrieve-ad db-conn (:id params))]
    (db/create-click! db-conn {:ad-id (:id params)
                               :client-address remote-addr})
    (redirect (:url ad))))
