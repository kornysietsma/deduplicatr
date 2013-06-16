(ns deduplicatr.t-progress-logger
  (:require [deduplicatr.throttler :as throttler]
            [deduplicatr.progress-logger :as plog]
            [taoensso.timbre :as timbre]
            [midje.sweet :refer :all]))

(tabular
 (fact "can log byte values prettily"
       (plog/bytefmt ?bytes)
       => ?result)
 ?bytes ?result
 0      "0 b"
 1      "1 b"
 1023   "1,023 b"
 1024   "1.000 kb"
 2049   "2.001 kb"
 (- (* 1024 1024) 1) "1,023.999 kb"
 (* 1024 1024) "1.000 mb"
 (* 1024 1024 1024) "1.000 gb"
 (* 1024 1024 1024 1024) "1,024.000 gb")

