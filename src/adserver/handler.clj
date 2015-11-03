(ns adserver.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.handler.dump :refer [handle-dump]]
            [adserver.handler.welcome :as welcome]
            [adserver.handler.ad :as ad]))

(defroutes public-routes
  (GET "/"             [] welcome/handle-welcome)
  (GET "/ad"           [] ad/handle-random-ad)
  (GET "/ad/image/:id" [] handle-dump)
  (GET "/ad/click/:id" [] handle-dump))

(defroutes auth-routes
  (GET  "/login"  [] handle-dump)
  (POST "/login"  [] handle-dump)
  (ANY  "/logout" [] handle-dump))

(defroutes admin-routes
  (GET  "/create"     [] handle-dump)
  (POST "/create"     [] handle-dump)
  (GET  "/list"       [] handle-dump)
  (GET  "/show/:id"   [] handle-dump)
  (POST "/delete/:id" [] handle-dump))

(defroutes app-routes
  public-routes
  auth-routes
  (context "/admin" [] admin-routes)
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))
