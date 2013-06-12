(ns deduplicatr.progress-logger
  (:require [deduplicatr.throttler :as throttler]
            [taoensso.timbre :as timbre]))

(defn new-logger [throttler-agent]
  (agent {:t-agent throttler-agent
          :dirs 0
          :files 0
          :bytes 0
          }))

(def kilo 1024)
(def meg (* 1024 kilo))
(def gig (* 1024 meg))

(defn- bytefmt [bytes]
  (cond
   (> bytes gig) (format "%,.3f gb" (/ (double bytes) gig))
   (> bytes meg) (format "%,.3f mb" (/ (double bytes) meg))
   (> bytes kilo) (format "%,.3f kb" (/ (double bytes) kilo))
   :else (format "%,d b" bytes)
  ))

(defn- log-message
  [{:keys [dirs files bytes]} message]
  (timbre/info (format "%,d dirs %,d files %s %s" dirs files  (bytefmt bytes) message)))

(defn- update-and-maybe-log
  [new-dirs new-files new-bytes message logger]
  (let [{:keys [t-agent dirs files bytes]} logger
        updated-logger (assoc logger
                               :dirs (+ dirs new-dirs)
                               :files (+ files new-files)
                               :bytes (+ bytes new-bytes))]
    (throttler/run t-agent #(log-message updated-logger message))
    updated-logger))

(defn- update [logger new-dirs new-files new-bytes message]
  (send logger (partial update-and-maybe-log new-dirs new-files new-bytes message)))

(defn log-file [logger bytes filename]
  (update logger 0 1 bytes (str "file: " filename)))

(defn log-dir [logger dirname]
  (update logger 1 0 0 (str "dir: " dirname)))
