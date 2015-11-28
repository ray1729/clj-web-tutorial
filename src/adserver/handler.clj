(ns adserver.handler
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :refer [redirect]]
            [ring.handler.dump :refer [handle-dump]]
            [buddy.auth :refer [authenticated?]]
            [buddy.auth.accessrules :as authz]
            [buddy.auth.backends.session :refer [session-backend]]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [adserver.config :as config]
            [adserver.db :as db]
            [adserver.handler.ad :as ad]
            [adserver.handler.auth :as auth]))

(defroutes public-routes
  (GET "/"             [] (redirect "/admin/list"))
  (GET "/ad"           [] ad/handle-random-ad)
  (GET "/ad/image/:id" [] ad/handle-image)
  (GET "/ad/click/:id" [] ad/handle-click))

(defroutes auth-routes
  (GET  "/login"  [] auth/handle-show-login)
  (POST "/login"  [] auth/handle-login)
  (ANY  "/logout" [] auth/handle-logout))

(defroutes admin-routes
  (GET  "/create"     [] ad/handle-show-create)
  (POST "/create"     [] ad/handle-create)
  (GET  "/list"       [] ad/handle-list)
  (GET  "/show/:id"   [] ad/handle-show)
  (POST "/delete/:id" [] ad/handle-delete))

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
    (.close ds)
    (try
      ;; Embedded Derby shutdown
      (jdbc/get-connection {:connection-uri "jdbc:derby:;shutdown=true;deregister=false"})
      (catch Exception e
        (log/info (.getMessage e)))))
  (alter-var-root (var data-source) (constantly nil))
  (alter-var-root (var config) (constantly nil)))

;; Buddy authentication backend
(def auth-backend (session-backend {:unauthorized-handler auth/handle-unauthorized}))

(defn require-admin
  [request]
  (let [roles (get-in request [:identity :roles] #{})]
    (contains? roles "admin")))

(defroutes app-routes
  public-routes
  auth-routes
  (context "/admin" [] (authz/restrict admin-routes {:handler require-admin}))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      (wrap-authorization auth-backend)
      (wrap-authentication auth-backend)
      (wrap-defaults site-defaults)
      wrap-config
      wrap-db-connection))
