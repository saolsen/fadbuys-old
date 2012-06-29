(ns fadbuys-collector.core
  (:require [aws.sdk.s3 :as s3]
            [ring.adapter.jetty :as web]
            [cheshire.core :as json]
            [twitter-streaming-client.core :as client]
            [twitter.oauth :as oauth]))

;; Mah creds
(def aws-cred {:access-key (System/getenv "AWS_ACCESS")
               :secret-key (System/getenv "AWS_SECRET")})
(def tweet-cred (oauth/make-oauth-creds
                 (System/getenv "TWITTER_CONSUMER_KEY")
                 (System/getenv "TWITTER_CONSUMER_SECRET")
                 (System/getenv "TWITTER_ACCESS_KEY")
                 (System/getenv "TWITTER_ACCESS_SECRET")))

;; S3 stuff
(def bucket "com.fadbuys.twitter")

(defn get-key
  "Gets the key to write to."
  []
  (.toGMTString (new java.util.Date)))

(defn write-tweets-to-s3
  "Writes the vector to an s3 bucket"
  [tweets]
  (let [text (apply str (interpose "\n" tweets))]
    (s3/put-object aws-cred bucket (get-key) text)))

;; Twitter stuff
(def stream  (client/create-twitter-stream
              twitter.api.streaming/statuses-sample
              :oauth-creds tweet-cred))

(defn sync-tweets
  []
  (loop []
    (let [queue (client/retrieve-queues stream)
          tweets (map :text (:tweet queue))]
      (write-tweets-to-s3 tweets))
    (recur)))

(defn -main
  "Main entry point"
  []
  (do
    (client/start-twitter-stream stream)
    (future (sync-tweets))))
