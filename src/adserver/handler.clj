(ns adserver.handler
  (:require [clojure.java.jdbc :as jdbc]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.handler.dump :refer [handle-dump]]
            [adserver.config :as config]
            [adserver.db :as db]
            [adserver.handler.welcome :as welcome]
            [adserver.handler.ad :as ad]))

(defroutes public-routes
  (GET "/"             [] welcome/handle-welcome)
  (GET "/ad"           [] ad/handle-random-ad)
  (GET "/ad/image/:id" [] ad/handle-image)
  (GET "/ad/click/:id" [] ad/handle-click))

(defroutes auth-routes
  (GET  "/login"  [] handle-dump)
  (POST "/login"  [] handle-dump)
  (ANY  "/logout" [] handle-dump))

(defroutes admin-routes
  (GET  "/create"     [] ad/handle-show-create)
  (POST "/create"     [] ad/handle-create)
  (GET  "/list"       [] ad/handle-list)
  (GET  "/show/:id"   [] ad/handle-show)
  (POST "/delete/:id" [] ad/handle-delete))

(defroutes app-routes
  public-routes
  auth-routes
  (context "/admin" [] admin-routes)
  (route/resources "/")
  (route/not-found "Not Found"))

(defonce config nil)
(defonce data-source nil)

(defn wrap-config
  [handler]
  (fn [request]
    (handler (assoc request :config config))))

(defn wrap-db-connection
  [handler]
  (fn [request]
    (jdbc/with-db-connection [conn data-source]
      (handler (assoc request :db-conn conn)))))

(defn init
  []
  (let [conf (config/get-config)
        ds   (db/init (:database conf))]
    (alter-var-root (var config) (constantly conf))
    (alter-var-root (var data-source) (constantly ds))
    nil))

(defn destroy
  []
  (when-let [ds (:datasource data-source)]
    (.close ds))
  (alter-var-root (var data-source) (constantly nil))
  (alter-var-root (var config) (constantly nil)))

(def app
  (-> (wrap-defaults app-routes site-defaults)
      wrap-config
      wrap-db-connection))
