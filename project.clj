(defproject adserver "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [compojure "1.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/tools.reader "0.10.0"]
                 [org.clojure/java.jdbc "0.4.2"]
                 [ch.qos.logback/logback-classic "1.1.3"]
                 [ch.qos.logback/logback-core "1.1.3"]
                 [org.slf4j/slf4j-api "1.7.12"]
                 [org.apache.derby/derby "10.12.1.1"]
                 [com.layerware/hugsql "0.2.2"]
                 [clj-time "0.11.0"]
                 [commons-codec "1.10"]
                 [ring/ring-defaults "0.1.5"]
                 [ring-middleware-format "0.6.0" :exclusions [ring cheshire]]
                 [clj-http-status "0.1.0"]
                 [buddy/buddy-auth "0.7.1"]
                 [environ "1.0.1"]
                 [medley "0.7.0"]]
  :plugins [[lein-ring "0.9.7"]
            [lein-environ "1.0.1"]]
  :ring {:handler adserver.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]]}})
