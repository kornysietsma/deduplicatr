(ns deduplicatr.fstree-test
  (:use midje.sweet
        deduplicatr.fstree
        [deduplicatr.file :only [make-file-summary make-dir-summary]]
        [deduplicatr.hash :only [digest-of-long]]
        [clojure.java.io :only [file]])
  (:import (java.io File RandomAccessFile)
           (deduplicatr.file FileSummary)))

(def fixtures (file "test" "fixtures"))
(def simple-fixture (file fixtures "simple"))
(def foo-file (file "foo"))
(def bar-file (file "bar"))
(def baz-file (file "baz"))

(fact "populate-child-dirs-and-summaries produces a map of file names to their accumulated summaries"
  (#'deduplicatr.fstree/populate-child-dirs-and-summaries 
   [foo-file bar-file baz-file] ...original-summaries...)
  => [{"foo" ...foo-summary...
       "bar" ...bar-summary...
       "baz" ...baz-summary...}
      ...summaries-with-foo-bar-baz...]
  (provided
    (treeify-and-summarize foo-file ...original-summaries...)
    => [...foo-summary... ...summaries-with-foo...]
    (treeify-and-summarize bar-file ...summaries-with-foo...)
    => [...bar-summary... ...summaries-with-foo-bar...]
    (treeify-and-summarize baz-file ...summaries-with-foo-bar...)
    => [...baz-summary... ...summaries-with-foo-bar-baz...]))

(fact "hash-files-by-name hashes a list of files and returns a map of them by name"
  (binding [*file-hash-fn* #(str (.getName %) "_hash")]
    (#'deduplicatr.fstree/hash-files-by-name [foo-file bar-file])
    => {"foo" "foo_hash", "bar" "bar_hash"})
  )

; TODO: test add-files-to-summaries - slipped through somehow.

(defn stub-hashfn
  "stub hash for a file - just use file name as the hash function"
  [file] (make-file-summary file (.getName file) (.length file)))

(defn stub-dirsummaryfn
  "stub summary for a directory - just use file count as the hash function"
  ([file] (make-dir-summary file 0 0 0)) 
  ([prevsummary filename hash bytes] 
     (make-dir-summary
      (.file prevsummary)
      (inc (.hash prevsummary))
      (+ bytes (.bytes prevsummary))
      (inc (.filecount prevsummary))
      )))

(fact "stub-dirsummaryfn simply counts files and adds sizes"
  (stub-dirsummaryfn (stub-dirsummaryfn foo-file) "foo" 1234 17)
  => (make-dir-summary foo-file 1 17 1)
  (stub-dirsummaryfn
   (stub-dirsummaryfn (stub-dirsummaryfn foo-file) "foo" 1234 17)
   "bar" 2345 23)
  => (make-dir-summary foo-file 2 40 2))

(binding [*file-hash-fn* stub-hashfn
          *dir-summary-fn* stub-dirsummaryfn ]
  (fact "treeifying an empty directory produces mostly empty results"
    (treeify (file simple-fixture "parent" "child" "empty_grandchild"))
    => {
        :files {}
        :dirs {}
        :summary (make-dir-summary (file simple-fixture "parent" "child" "empty_grandchild") 0 0 0)})

  (fact "treeifying a directory containg files produces a map of summaries of those files, and the directory summary"
    (treeify (file simple-fixture "ab"))
    => {
        :files {
                "a.txt", (make-file-summary (file simple-fixture "ab" "a.txt") "a.txt", 1)
                "b.txt", (make-file-summary (file simple-fixture "ab" "b.txt") "b.txt", 1)
                }
        :dirs {}
        :summary (make-dir-summary (file simple-fixture "ab") 2 2 2)})

  (fact "treeifying a more complex directory structure produces a tree of summary information"
    (treeify (file simple-fixture "parent" "child"))
    => {
        :files {
                "child_a.txt", (make-file-summary (file simple-fixture "parent" "child" "child_a.txt") "child_a.txt", 1)
                "child_b.txt", (make-file-summary (file simple-fixture "parent" "child" "child_b.txt") "child_b.txt", 1)
                }
        :dirs {
               "empty_grandchild", {
                  :files {}
                  :dirs {}
                  :summary (make-dir-summary 
                            (file simple-fixture "parent" "child" "empty_grandchild")
                            0 0 0)
                                    }
               }
        :summary (make-dir-summary (file simple-fixture "parent" "child") 2 2 2)
      })
  
(fact "the summary of a directory includes the accumulated summary from all descendant files"
  (:summary (treeify (file simple-fixture "parent")))
  => (make-dir-summary (file simple-fixture "parent") 4 4 4))
)

(fact "two directories with same contents have same hash and size"
   (let [tree1 (treeify (file simple-fixture "ab"))
         tree2 (treeify (file simple-fixture "parent" "child"))
         summary1 (:summary tree1)
         summary2 (:summary tree2)]
     (:hash summary1) => (:hash summary2)
     (:bytes summary1) => (:bytes summary2)))

(fact "two directories with same contents have same hash and size even if structure is different"
   (let [tree (treeify (file simple-fixture))
         summary1 (-> tree :dirs (get "ab") :summary)
         summary2 (-> tree :dirs (get "ab_split") :summary)]
     (:hash summary1) => (:hash summary2)
     (:bytes summary1) => (:bytes summary2)))
