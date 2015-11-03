(ns adserver.view.common
  (:require [hiccup.def :refer [defhtml]]
            [hiccup.page :refer [html5 include-css]]))

(defn layout
  [{:keys [title content] :or {title "NonDysfunctional AdServer"}}]
  (html5
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    (include-css "//fonts.googleapis.com/css?family=Raleway:400,300,600"
                 "///css/normalize.css"
                 "///css/skeleton.css")
    [:title title]]
   [:body
    [:div.container
     [:div.row
      content]]]))
