(ns deduplicatr.fstree-test
  (:require [midje.sweet :refer :all]
            [deduplicatr.fstree :refer :all]
            [deduplicatr.file :as df]
            [deduplicatr.hash :as dh]
            [clojure.java.io :refer [file]]))

(def fixtures (file "test" "fixtures"))
(def simple-fixture (file fixtures "simple"))
(def foo-file (file "foo"))
(def bar-file (file "bar"))
(def baz-file (file "baz"))

(defn stub-filesummaryfn
  "stub summary for testing - just use 1 as the hash value"
  [group file] (df/->FileSummary group file 1N (.length file)))

(with-redefs [df/file-summary stub-filesummaryfn]
  (fact "treeifying an empty directory produces mostly empty results"
    (treeify :group (file simple-fixture "parent" "child" "empty_grandchild"))
    => {:files []
        :dirs []
        :summary (df/->DirSummary :group (file simple-fixture "parent" "child" "empty_grandchild") 0N 0 0)})

  (fact "treeifying a directory containg files produces a list of those files, and the directory summary"
    (treeify :group (file simple-fixture "ab"))
    => {:files [(df/->FileSummary :group (file simple-fixture "ab" "a.txt") 1N, 1)
                (df/->FileSummary :group (file simple-fixture "ab" "b.txt") 1N, 1)]
        :dirs []
        :summary (df/->DirSummary :group (file simple-fixture "ab") 2N 2 2)})

  (fact "treeifying a more complex directory structure produces a tree of summary information"
    (treeify :group (file simple-fixture "parent" "child"))
    => {:files [(df/->FileSummary :group (file simple-fixture "parent" "child" "child_a.txt") 1N, 1)
                (df/->FileSummary :group (file simple-fixture "parent" "child" "child_b.txt") 1N, 1)]
        :dirs [{:files []
                :dirs []
                :summary (df/->DirSummary :group 
                                           (file simple-fixture "parent" "child" "empty_grandchild")
                                           0N 0 0)}]
        :summary (df/->DirSummary :group (file simple-fixture "parent" "child") 2N 2 2)})
  
  (fact "the summary of a directory includes the accumulated summary from all descendant files"
    (:summary (treeify :group (file simple-fixture "parent")))
    => (df/->DirSummary :group (file simple-fixture "parent") 4N 4 4)))

(fact "two directories with same contents have same hash and size"
   (let [tree1 (treeify :group (file simple-fixture "ab"))
         tree2 (treeify :group (file simple-fixture "parent" "child"))
         summary1 (:summary tree1)
         summary2 (:summary tree2)]
     (:hash summary1) => (:hash summary2)
     (:bytes summary1) => (:bytes summary2)))

(fact "two directories with same contents have same hash and size even if structure is different"
   (let [tree (treeify :group (file simple-fixture))
         summary1 (-> tree :dirs (get "ab") :summary)
         summary2 (-> tree :dirs (get "ab_split") :summary)]
     (:hash summary1) => (:hash summary2)
     (:bytes summary1) => (:bytes summary2)))
