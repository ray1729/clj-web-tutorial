(ns adserver.config
  (:require [environ.core :refer [env]]
            [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:import [java.io PushbackReader]))

(defn file-or-resource
  [path]
  (let [f (io/file path)]
    (if (.exists f)
      f
      (if-let [r (io/resource path)]
        r
        (throw (Exception. (str "File or resource not found: " path)))))))

(defn read-config
  [path]
  (with-open [r (PushbackReader. (io/reader (file-or-resource path)))]
    (edn/read r)))

(defn get-config
  []
  (if-let [path (env :config-path)]
    (read-config path)
    (throw (Exception. "CONFIG_PATH not set"))))
