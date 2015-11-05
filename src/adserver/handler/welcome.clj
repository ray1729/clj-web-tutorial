(ns adserver.handler.welcome
  (:require [ring.util.response :refer [response content-type]]
            [adserver.handler.common :as common]))

(defn render-welcome
  []
  (common/layout
   [:h1 "Welcome to NonDysfunctionalAdserver"]))

(defn handle-welcome
  [request]
  (-> (response (render-welcome))
      (content-type "text/html")))
