(ns user
  (:require [adserver.handler :as h]
            [adserver.db :as db]
            [ring.adapter.jetty :refer [run-jetty]]))

(comment
  (h/init)

  (db/load-fixtures h/data-source)

  (def server (future (run-jetty #'h/app {:port 3000})))

  (h/destroy)

  (future-cancel server)

  )
