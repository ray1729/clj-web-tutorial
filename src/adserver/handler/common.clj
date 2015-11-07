(ns adserver.handler.common
  (:require [hiccup.def :refer [defhtml defelem]]
            [hiccup.element :as e]
            [hiccup.page :refer [html5 include-css]]))

(defn layout
  [content & [{:keys [title] :or {title "NonDysfunctional AdServer"}}]]
  (html5
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    (include-css "//fonts.googleapis.com/css?family=Raleway:400,300,600"
                 "/css/normalize.css"
                 "/css/skeleton.css"
                 "/css/app.css")
    [:title title]]
   [:body
    [:div.container
     content]]))

(def navigation
  {:logout ["/logout" "Logout"]
   :list   ["/admin/list" "Back to list"]
   :create ["/admin/create" "Create new ad"]})

(defelem nav-bar
  [& ks]
  [:div.row
   [:div.navbar.u-pull-right
    (map (fn [k]
           (when-let [[url label] (navigation k)]
             (e/link-to url label)))
         ks)]])
