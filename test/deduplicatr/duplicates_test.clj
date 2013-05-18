(ns deduplicatr.duplicates-test
  (:require [midje.sweet :refer :all]
            [deduplicatr.duplicates :refer :all]
            [clojure.java.io :refer [file]]
            [deduplicatr.fstree :refer [treeify]]
            [deduplicatr.file :refer [->FileSummary ->DirSummary]]))

(def fixtures (file "test" "fixtures"))
(def simple-fixture (file fixtures "simple"))

(def simplest-tree {
   :files [(->FileSummary :group (file "tree" "foo.txt") 10000, 1) ]
   :dirs []
   :summary (->DirSummary :group (file "tree") 20000 1 1)})

(def simple-tree
  {:files [(->FileSummary :group (file "tree" "foo.txt") 10000, 1)
           (->FileSummary :group (file "tree" "bar.txt") 20000, 10)]
   :dirs [{:files []
           :dirs []
           :summary (->DirSummary :group (file "tree" "empty_child") 0 0 0) }
          {:files [(->FileSummary :group (file "tree" "child" "foo.txt") 10000, 1)]
           :dirs []
           :summary (->DirSummary :group (file "tree" "child") 10000 1 1) }]
   :summary (->DirSummary :group (file "tree") 30000 12 3)})

(facts "fstree-seq returns a seq of the summaries in a file system tree"
   (fstree-seq simplest-tree)
   => (just [(->FileSummary :group (file "tree" "foo.txt") 10000, 1)
             (->DirSummary :group (file "tree") 20000 1 1)]
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
                      [(->FileSummary :group (file "foo") 100 10)
                       (->FileSummary :group (file "bar") 100 20)
                       (->FileSummary :group (file "baz") 200 20)
                       (->DirSummary :group (file "boo") 100 10 1)]))
  => [(file "bar") (file "baz") (file "boo") (file "foo")])

(fact "is-ancestor-of determines if a file is an ancestor of another file"
  (is-ancestor-of (file "foo") (file "foo")) => false
  (is-ancestor-of (file "foo") (file "foo" "bar")) => true
  (is-ancestor-of (file "foo") (file "foo" "bar" "baz")) => true
  (is-ancestor-of (file "foo" "ba") (file "foo" "bar")) => false)

(fact "is-summary-ancestor-of won't match files in different groups"
  (is-summary-ancestor-of (->FileSummary :a (file "foo") 1 1)
                          (->FileSummary :a (file "foo" "bar") 1 1))
  => true
  (is-summary-ancestor-of (->FileSummary :a (file "foo") 1 1)
                          (->FileSummary :b (file "foo" "bar") 1 1))
  => false)

(fact "without-ancestors filters a sequence of matching summaries, removing any that are ancestors of another in the seq"
  (without-ancestors [(->DirSummary :group (file "root") 100 1 1)
                      (->DirSummary :group (file "root" "child") 100 1 1)
                      (->DirSummary :group (file "root" "child" "grandchild") 100 1 1)
                      (->DirSummary :group (file "other") 100 1 1)])
  => [(->DirSummary :group (file "root" "child" "grandchild") 100 1 1)
      (->DirSummary :group (file "other") 100 1 1)])

(facts "duplicates returns a list of all duplicate groups in a file system tree, sorted as above, with self-ancestors removed"
  (fact "if there are no duplicates, returns an empty seq" 
    (duplicates simplest-tree)
    => [])
  (fact "if there is a single duplicate, return a single result containing all duplicates"
    (duplicates simple-tree)
    => [[ (->FileSummary :group (file "tree" "child" "foo.txt") 10000 1)
          (->FileSummary :group (file "tree" "foo.txt") 10000, 1)]])

   ; TODO: redundant?  See integration test in core_test.clj
  (fact "if there are more than one duplicate, return them sorted as above"
    (let [complex-result (duplicates (treeify :group (file simple-fixture)))]
      (count complex-result) 
      => 3
      (map #(:bytes (first %)) complex-result)
      => [2 1 1]
      (map :file (first complex-result)) 
      => [(file simple-fixture "ab") (file simple-fixture "ab_split") (file simple-fixture "parent" "child")]
      (map #(.getName (:file %)) (nth complex-result 1)) 
      => ["b.txt" "b.txt" "child_b.txt" "parent_b.txt"]
      (map #(.getName (:file %)) (nth complex-result 2)) 
      => ["a.txt" "a.txt" "child_a.txt" "parent_a.txt"])))
