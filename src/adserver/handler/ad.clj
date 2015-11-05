(ns adserver.handler.ad
  (:require [ring.util.response :refer [response content-type redirect]]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [stencil.core :as stencil]
            [hiccup.form :as f]
            [adserver.db :as db]
            [adserver.handler.common :as common]))

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

(defn render-create-ad
  []
  (common/layout
   [:div
    [:h1 "Create New Ad"]
    (f/form-to
     {:enctype "multipart/form-data"}
     [:post "/admin/create"]
     (anti-forgery-field)
     [:div.row
      [:div.six.columns
       (f/label "title" "Title")
       (f/text-field {:class "u-full-width" :required true :type "text"} "title")]]
     [:div.row
      [:div.twelve.columns
       (f/label "content" "Content")
       (f/text-area {:class "u-full-width" :required true} "content")]]
     [:div.row
      [:div.six.columns
       (f/label "height" "Height")
       (f/text-field {:class "u-full-width" :required true :type "number"} "height")]
      [:div.six.columns
       (f/label "width" "Width")
       (f/text-field {:class "u-full-width" :required true :type "number"} "width")]]
     [:div.row
      [:div.twelve.columns
       (f/label "url" "URL")
       (f/text-field {:class "u-full-width" :required true :type "text"} "url")]]
     [:div.row
      [:div.six.columns
       (f/label "image" "Image")
       (f/file-upload {:class "u-full-width" :required true} "image")]]
     [:div.row
      [:div.six.columns
       (f/submit-button {:class "button-primary"} "Create")]])]))

(defn handle-show-create
  [request]
  (-> (response (render-create-ad))
      (content-type "text/html")))

(defn get-bytes
  [file]
  (let [bytes (byte-array (.length file))]
    (with-open [r (io/input-stream file)]
      (.read r bytes))
    bytes))

(defn handle-create
  [{:keys [db-conn params] :as request}]
  (let [ad (select-keys params [:title :content :height :width :url])
        image {:content-type  (get-in params [:image :content-type] "application/octet-stream")
               :content-bytes (get-bytes (get-in params [:image :tempfile]))
               :size          (get-in params [:image :size])}
        id (db/create-ad! db-conn ad image)]
    (redirect (str "/admin/show/" id))))

(defn render-list
  [ads]
  (common/layout
   [:div
    [:h1 "Ads"]
    [:table
     [:thead
      [:tr (map (partial vector :th) ["Id" "Title" "Created" "Updated" "Active?" "Clicks"])]]
     [:tbody
      (map (fn [{:keys [ad-id title created-at updated-at is-active clicks]}]
             [:tr (map (partial vector :td) [ad-id title created-at updated-at is-active clicks])])
           ads)]]]))

(defn handle-list
  [{:keys [db-conn] :as request}]
  (-> (response (render-list (db/list-ads db-conn)))
      (content-type "text/html")))
