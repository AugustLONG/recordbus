(ns org.spootnik.recordbus
  (:gen-class)
  (:require [org.spootnik.recordbus.sql    :as sql]
            [org.spootnik.recordbus.kafka  :as kafka]
            [org.spootnik.recordbus.config :as config]
            [cheshire.core                 :as json]
            [clojure.tools.logging         :as log]))

(defn callback
  [{:keys [kafka topic]}]
  (let [producer (kafka/producer kafka)]
    (fn [{:keys [server-id] :as event}]
      (kafka/publish
       producer
       (kafka/record topic server-id (json/generate-string event))))))

(defn -main
  [& [path]]
  (let [cfg    (config/read-props (or path "/etc/recordbus.conf"))
        client (sql/replication-client (:sql cfg) (callback cfg))]
    (log/info "connecting to mysql")
    (loop []
      (try
        (sql/connect! client)
        (catch java.io.IOException e))
      (log/info "no replication connection open, retry in 500ms")
      (Thread/sleep 500)
      (recur))))
