(ns adserver.handler.welcome
  (:require [ring.util.response :refer [response content-type]]
            [adserver.view.common :as common]))

(defn render-welcome
  []
  (common/layout
   {:title "Welcome to NonDysfunctional AdServer"
    :content [:h1 "Welcome to NonDysfunctionalAdserver"]}))

(defn handle-welcome
  [request]
  (-> (response (render-welcome))
      (content-type "text/html")))
