(defproject fadbuys-collector "0.1.0-SNAPSHOT"
  :description "Collects data for analysis"
  :url "http://collector.fadbuys.com/"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main fadbuys-collector.core
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [clj-aws-s3 "0.3.2"]
                 [http.async.client "0.4.5"]
                 [ring "1.1.1"]
                 [cheshire "4.0.0"]])
