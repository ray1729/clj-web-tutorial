(ns adserver.handler.ad
  (:require [ring.util.response :refer [response content-type redirect]]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [stencil.core :as stencil]
            [hiccup.form :as f]
            [hiccup.element :as e]
            [hiccup.core :refer [h]]
            [adserver.db :as db]
            [adserver.handler.common :as common])
  (:import [java.io ByteArrayInputStream]))

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
    (-> (response (ByteArrayInputStream. (:content-bytes image)))
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
    [:p (e/link-to "/admin/list" "Back to list")]
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

(defn handle-create
  [{:keys [db-conn params] :as request}]
  (let [ad (select-keys params [:title :content :height :width :url])
        image {:content-type  (get-in params [:image :content-type] "application/octet-stream")
               :content-bytes (db/file-to-bytes (get-in params [:image :tempfile]))
               :size          (get-in params [:image :size])}
        id (db/create-ad! db-conn ad image)]
    (-> (redirect "/admin/list")
        (assoc :flash {:info (str "Created ad " id)}))))

(defn render-list
  [ads flash]
  (common/layout
   [:div
    [:h1 "Ads"]
    [:p (e/link-to "/admin/create" "Create new ad")]
    (when-let [info-mesg (:info flash)]
      [:div.info [:p info-mesg]])
    [:table
     [:thead
      [:tr (map (partial vector :th) ["Id" "Title" "Created" "Updated" "Active?" "Clicks"])]]
     [:tbody
      (map (fn [{:keys [ad-id title created-at updated-at is-active clicks]}]
             [:tr
              [:td (e/link-to (str "/admin/show/" ad-id) ad-id)]
              (map (comp (partial vector :td) h) [title created-at updated-at is-active clicks])])
           ads)]]]))

(defn handle-list
  [{:keys [db-conn flash] :as request}]
  (-> (response (render-list (db/list-ads db-conn) flash))
      (content-type "text/html")))

(defn render-show
  [{:keys [ad-id title content width height url is-active created-at updated-at clicks]}]
  (common/layout
   [:div
    [:h1 (h title)]
    [:p (e/link-to "/admin/list" "Back to list")]
    [:div.row
     [:div.two.columns
      (e/image {:height height :width width} (str "/ad/image/" ad-id) title)]
     [:div.ten.columns
      [:p (h content)]]]
    [:div.row
     [:div.one.column.heading "Active?"]
     [:div.one.column is-active]
     [:div.one.column.heading "Clicks"]
     [:div.one.column clicks]]
    [:div.row
     [:div.one.column.heading "Height"]
     [:div.one.column height]
     [:div.one.column.heading "Width"]
     [:div.one.column width]]
    [:div.row
     [:div.one.column.heading "URL"]
     [:div.ten.columns (e/link-to url (h url))]]
    [:div.row
     [:div.one.column.heading "Created"]
     [:div.three.columns created-at]
     [:div.one.column.heading "Updated"]
     [:div.three.columns updated-at]]
     [:div.row
      (f/form-to
       [:post (str "/admin/delete/" ad-id)]
       (anti-forgery-field)
       (f/submit-button "Delete this Ad"))]]))

(defn handle-show
  [{:keys [db-conn params] :as request}]
  (when-let [ad (db/retrieve-ad db-conn (:id params))]
    (-> (response (render-show ad))
        (content-type "text/html"))))

(defn handle-delete
  [{:keys [db-conn params session] :as request}]
  (db/delete-ad! db-conn (:id params))
  (-> (redirect "/admin/list")
      (assoc :flash {:info (str "Deleted ad " (:id params))})))
