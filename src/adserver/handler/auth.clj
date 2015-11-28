(ns adserver.handler.auth
  (:require [clojure.tools.logging :as log]
            [ring.util.response :refer [response content-type redirect status]]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [ring.util.codec :refer [url-encode]]
            [hiccup.core :refer [h]]
            [hiccup.form :as f]
            [hiccup.element :as e]
            [buddy.hashers :as hashers]
            [buddy.auth :refer [authenticated?]]
            [adserver.db :as db]
            [adserver.handler.common :as common]))

(defn render-login-form
  ([params]
     (render-login-form params nil))
  ([params {:keys [error-msg]}]
     (common/layout
      [:div
       [:h1 "Login"]
       (when error-msg
         [:div.error [:p error-msg]])
       (f/form-to
        [:post (str "/login?next=" (url-encode (:next params "/")))]
        (anti-forgery-field)
        [:div.row
         [:div.six.columns
          (f/label "user-name" "Username")
          (f/text-field {:class "u-full-width" :required true :type "text"}
                        "user-name" (h (:user-name params "")))]]
        [:div.row
         [:div.six.columns
          (f/label "password" "Password")
          (f/password-field {:class "u-full-width" :required true} "password")]]
        [:div.row
         [:div.six.columns
          (f/submit-button {:class "button-primary"} "Login")]])])))

(defn handle-show-login
  [request]
  (-> (response (render-login-form (:params request)))
      (content-type "text/html")))

(defn authenticate
  [db-conn {:keys [user-name password]}]
  (when (and user-name password)
    (when-let [user (db/retrieve-user db-conn user-name)]
      (when (hashers/check password (:password user))
        (dissoc user :password)))))

(defn handle-login
  [{:keys [db-conn session params]}]
  (if-let [user (authenticate db-conn (select-keys params [:user-name :password]))]
    (let [next-url (:next params "/")]
      (-> (redirect next-url)
          (assoc :session (assoc session :identity user))))
    (render-login-form params {:error-msg "Incorrect username or password"})))

(defn handle-logout
  [_]
  (-> (redirect "/login")
      (assoc :session nil)))

(defn render-not-authorized
  []
  (common/layout
   [:div
    [:h1 "Authorization Error"]
    [:p "You are not authorized for that action."]
    [:p (e/link-to "/logout" "Login as a different user")]]))

;; This is a Buddy unauthorized-handler, and receives Buddy metadata in
;; a second argument that we ignore.
(defn handle-unauthorized
  [request _]
  (log/debug "handle-unauthorized" request)
  (if (log/spy (authenticated? request))
    (-> (response (render-not-authorized))
        (content-type "text/html")
        (status 403))
    (redirect (str "/login?next=" (url-encode (:uri request))))))
