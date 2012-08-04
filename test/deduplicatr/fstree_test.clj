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

#_(fact "populate-child-dirs-and-summaries produces a map of file names to their accumulated summaries"
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

#_(fact "summarize-files-by-name summarizes a list of files and returns a map of them by name"
  (binding [*file-summary-fn* #(str (.getName %) "_hash")]
    (#'deduplicatr.fstree/summarize-files-by-name [foo-file bar-file])
    => {"foo" "foo_hash", "bar" "bar_hash"})
  )

; TODO: test add-files-to-summaries - slipped through somehow.

(defn stub-filesummaryfn
  "stub summary for a file - just use 1 as the hash value"
  [file] (make-file-summary file 1 (.length file)))

(defn stub-dirsummaryfn
  "stub summary for a directory - just use file count as the hash function"
  ([file] (make-dir-summary file 0 0 0))
  ([prevsummary & summaries]
     (make-dir-summary
      (.file prevsummary)
      (apply + (.hash prevsummary) (map :hash summaries))
      (apply + (.bytes prevsummary) (map :bytes summaries))
      (apply + (.filecount prevsummary) (map :filecount summaries))
      )))

(fact "stub-dirsummaryfn simply counts files and adds sizes"
  (stub-dirsummaryfn (stub-dirsummaryfn foo-file) (make-file-summary "ignored" 1 17) (make-file-summary "ignored" 1 23))
  => (make-dir-summary foo-file 2 40 2))

(binding [*file-summary-fn* stub-filesummaryfn
          *dir-summary-fn* stub-dirsummaryfn]
  (fact "treeifying an empty directory produces mostly empty results"
    (treeify (file simple-fixture "parent" "child" "empty_grandchild"))
    => {
        :files []
        :dirs []
        :summary (make-dir-summary (file simple-fixture "parent" "child" "empty_grandchild") 0 0 0)})

  (fact "treeifying a directory containg files produces a list of those files, and the directory summary"
    (treeify (file simple-fixture "ab"))
    => {
        :files [
                (make-file-summary (file simple-fixture "ab" "a.txt") 1, 1)
                (make-file-summary (file simple-fixture "ab" "b.txt") 1, 1)
               ]
        :dirs []
        :summary (make-dir-summary (file simple-fixture "ab") 2 2 2)})

  (fact "treeifying a more complex directory structure produces a tree of summary information"
    (treeify (file simple-fixture "parent" "child"))
    => {
        :files [
                (make-file-summary (file simple-fixture "parent" "child" "child_a.txt") 1, 1)
                (make-file-summary (file simple-fixture "parent" "child" "child_b.txt") 1, 1)
                ]
        :dirs [
               {
                :files []
                :dirs []
                :summary (make-dir-summary 
                           (file simple-fixture "parent" "child" "empty_grandchild")
                           0 0 0)
                }
               ]
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
