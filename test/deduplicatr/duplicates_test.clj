(ns deduplicatr.duplicates-test
  (:use midje.sweet
          deduplicatr.duplicates
          [clojure.java.io :only [file]]
          [deduplicatr.fstree :only [treeify]])
  (:import (deduplicatr.file FileSummary DirSummary)))

(def fixtures (file "test" "fixtures"))
(def simple-fixture (file fixtures "simple"))

(def simplest-tree {
   :files {
      "foo.txt", (FileSummary. (file "tree" "foo.txt") 10000, 1)
   }
   :dirs {}
   :summary (DirSummary. (file "tree") 20000 1 1)
 })


(def simple-tree {
   :files {
      "foo.txt", (FileSummary. (file "tree" "foo.txt") 10000, 1)
      "bar.txt", (FileSummary. (file "tree" "bar.txt") 20000, 10)
   }
   :dirs {
      "empty_child", {
        :files {}
        :dirs {}
        :summary (DirSummary. (file "tree" "empty_child") 0 0 0) 
      }
      "child", {
        :files { "foo.txt", (FileSummary. (file "tree" "child" "foo.txt") 10000, 1) }
        :dirs {}
        :summary (DirSummary. (file "tree" "child") 10000 1 1) 
      }
    }
    :summary (DirSummary. (file "tree") 30000 12 3)
      })

(facts "fstree-seq returns a seq of the summaries in a file system tree"
   (fstree-seq simplest-tree)
      => (just [(FileSummary. (file "tree" "foo.txt") 10000, 1)
		             (DirSummary. (file "tree") 20000 1 1)]
               :in-any-order)
      
   (map :file (fstree-seq simple-tree))
      => (just [(file "tree" "empty_child")
		            (file "tree" "child" "foo.txt")
		            (file "tree" "child")
		            (file "tree" "foo.txt")
		            (file "tree" "bar.txt")
		            (file "tree")] :in-any-order))
            

(fact "duplicates are found by sorting nodes by decreasing size, then by hash, then by path"
      (map :file (sort-by size-and-hash-sort-key 
               [(FileSummary. (file "foo") 100 10)
                (FileSummary. (file "bar") 100 20)
                (FileSummary. (file "baz") 200 20)
                (DirSummary. (file "boo") 100 10 1)]))
      => [(file "bar") (file "baz") (file "boo") (file "foo")])

(facts "duplicates returns a list of all duplicate groups in a file system tree, sorted as above"
   (fact "if there are no duplicates, returns an empty seq" 
      (duplicates simplest-tree)
      => [])
   (fact "if there is a single duplicate, return a single result containing all duplicates"
      (duplicates simple-tree)
      => [[
          (DirSummary. (file "tree" "child") 10000 1 1)
          (FileSummary. (file "tree" "child" "foo.txt") 10000 1)
          (FileSummary. (file "tree" "foo.txt") 10000, 1)
          ]])
   (fact "if there are more than one duplicate, return them sorted as above"
         (let [complex-result (duplicates (treeify (file simple-fixture)))]
         (count complex-result) 
           => 3
         (map #(:bytes (first %)) complex-result)
           => [2 1 1]
         (map :file (first complex-result)) 
           => [(file simple-fixture "ab") (file simple-fixture "ab_split") (file simple-fixture "parent" "child")]
         (map #(.getName (:file %)) (nth complex-result 1)) 
           => ["b.txt" "b.txt" "child2" "child_b.txt" "parent_b.txt"]
         (map #(.getName (:file %)) (nth complex-result 2)) 
           => ["a.txt" "a.txt" "child1" "child_a.txt" "parent_a.txt"]
           ))
)