(ns deduplicatr.t-diffs
  (:require [deduplicatr.diffs :as diffs]
            [fileutils.fu :as fu]
            [midje.sweet :refer :all]
            [deduplicatr.file :refer [->FileSummary ->DirSummary]]))

(fact "diffs of two sequences returns seqs in one or the other"
      (diffs/calc-diffs
       [(->FileSummary :a (fu/path "in-a") 1M 1)
        (->FileSummary :a (fu/path "also-in-a") 4M 1)
        (->FileSummary :a (fu/path "in-both-a") 2M 1)
        (->FileSummary :a (fu/path "in-both-a2") 2M 1)
        (->FileSummary :a (fu/path "also-in-both") 5M 1)
        ]
       [(->FileSummary :b (fu/path "in-b") 3M 1)
        (->FileSummary :b (fu/path "in-b2") 3M 1)
        (->FileSummary :b (fu/path "in-both-b") 2M 1)
        (->FileSummary :b (fu/path "also-in-both") 5M 1)]
       )
      => {
          :in-a { 1M [(->FileSummary :a (fu/path "in-a") 1M 1)]
                  4M [(->FileSummary :a (fu/path "also-in-a") 4M 1)] }
          :in-b { 3M [(->FileSummary :b (fu/path "in-b") 3M 1)
                      (->FileSummary :b (fu/path "in-b2") 3M 1)] }
          :in-both { 2M [(->FileSummary :a (fu/path "in-both-a") 2M 1)
                         (->FileSummary :a (fu/path "in-both-a2") 2M 1)
                         (->FileSummary :b (fu/path "in-both-b") 2M 1)]
                    5M [(->FileSummary :a (fu/path "also-in-both") 5M 1)
                        (->FileSummary :b (fu/path "also-in-both") 5M 1)]}})


