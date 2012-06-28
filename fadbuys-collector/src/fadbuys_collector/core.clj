(ns fadbuys-collector.core
  (:require [http.async.client :as http]
            [aws.sdk.s3 :as s3]
            [ring.adapter.jetty :as web]
            [cheshire.core :as json]))

;; S3 stuff
(def cred {:access-key (System/getenv "AWS_ACCESS")
           :secret-key (System/getenv "AWS_SECRET")})

(def bucket "com.fadbuys.twitter")

(defn get-key
  "Gets the key to write to."
  []
  (.toGMTString (new java.util.Date)))

(defn write-tweets-to-s3
  "Writes the vector to an s3 bucket"
  [tweets]
  (let [text (apply str (interpose "\n" tweets))]
    (s3/put-object cred bucket (get-key) text)))

;; Twitter stuff
(defn get-text
  "Just a quick ghetto filter and text extractor"
  [tweet]
  (:text tweet))

(defn consume-twitter
  "Connects to the twitter public stream and calls consumer on each tweet,
   never returns"
  [username password consumer]
  (loop []
    (with-open [twitter (http/create-client)]
      (let [tweet (http/stream-seq
                   twitter
                   :get "https://stream.twitter.com/1/statuses/sample.json"
                   :auth {:user username :password password})]
        (doseq [s (http/string tweet)]
          (consumer (get-text (json/parse-string s true))))))
    (recur)))

;; Setup Agents
(def collection (agent []))
(def s3-writer (agent nil))

(defn log-tweet
  [tweet]
  (send collection conj tweet))

;; If there are more than 500 tweets sync to s3
(defn sync-watcher
  [key a old new]
  (if (> (count new) 500)
    (do
      (send s3-writer (fn [x] (write-tweets-to-s3 new)))
      (send collection (fn [x] [])))
    nil))

(add-watch collection :syncer sync-watcher)

;; Web interface
(defn count-collection
  "Shows what is currently in the collection agent."
  []
  (count @collection))

(defn handler [req]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (count-collection)})

(defn start-server
  []
  (web/run-jetty handler {:port (Integer/parseInt (System/getenv "PORT_WWW"))}))

(defn -main
  "Main entry point"
  []
  (let [user (System/getenv "TWITTER_UNAME")
        pass (System/getenv "TWITTER_PASS")
        consumer (future (consume-twitter user pass log-tweet))]
    (start-server)))
