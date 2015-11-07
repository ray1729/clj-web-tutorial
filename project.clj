(defproject adserver "0.1.0-SNAPSHOT"
  :description "Demonstration application for web programming tutorial"
  :url "https://github.com/cam-clj/adserver"
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
                 [com.jolbox/bonecp "0.8.0.RELEASE"]
                 [org.flywaydb/flyway-core "3.2.1"]
                 [clj-time "0.11.0"]
                 [commons-codec "1.10"]
                 [ring-middleware-format "0.6.0" :exclusions [ring cheshire]]
                 [clj-http-status "0.1.0"]
                 [buddy/buddy-auth "0.7.1"]
                 [environ "1.0.1"]
                 [medley "0.7.0"]
                 [inflections "0.10.0"]
                 [stencil "0.5.0"]]
  :plugins [[lein-ring "0.9.7"]
            [lein-environ "1.0.1"]]
  :ring {:init    adserver.handler/init
         :handler adserver.handler/app
         :destroy adserver.handler/destroy}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]
                        [ring/ring-devel "1.4.0"]
                        [ring/ring-jetty-adapter "1.4.0"]]
         :resource-paths ["dev-resources" "resources"]
         :env {:config-path "config.dev.edn"}}
   :test {:env {:config-path "config.test.edn"}}})
