(ns fadbuys-collector.core
  (:require [clojure.java.io :as io]
            [ring.adapter.jetty :as web]
            [cheshire.core :as json]
            [twitter-streaming-client.core :as client]
            [twitter.oauth :as oauth]))

;; Mah creds
(def tweet-cred (oauth/make-oauth-creds
                 (System/getenv "TWITTER_CONSUMER_KEY")
                 (System/getenv "TWITTER_CONSUMER_SECRET")
                 (System/getenv "TWITTER_ACCESS_KEY")
                 (System/getenv "TWITTER_ACCESS_SECRET")))

;; S3 stuff
(defn get-filename
  "Gets the filename to write to."
  []
  (str "/users/steveo/dotcloud/store/" (.toGMTString (new java.util.Date))))

(defn write-tweets-to-s3fs
  "Writes the vector to s3fs"
  [tweets]
  (println tweets)
  (with-open [wrt (io/writer (get-filename))]
    (doseq [x tweets]
      (.write wrt (str x "\n")))))

;; Twitter stuff
(def stream  (client/create-twitter-stream
              twitter.api.streaming/statuses-sample
              :oauth-creds tweet-cred))

(defn sync-tweets
  []
  (loop []
    (let [queue (client/retrieve-queues stream)
          tweets (map :text (:tweet queue))]
      (if (> (count tweets) 0)
        (write-tweets-to-s3fs tweets)
        nil)
      (. Thread (sleep 30000)))
    (recur)))

(defn -main
  "Main entry point"
  []
  (do
    (client/start-twitter-stream stream)
    (sync-tweets)))
