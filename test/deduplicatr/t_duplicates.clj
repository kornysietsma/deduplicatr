(ns deduplicatr.t-duplicates
  (:require [midje.sweet :refer :all]
            [deduplicatr.duplicates :refer :all]
            [fileutils.fu :as fu]
            [deduplicatr.fstree :refer [treeify]]
            [deduplicatr.file :refer [->FileSummary ->DirSummary]]))

(def fixtures (fu/path "test" "fixtures"))
(def simple-fixture (fu/rel-path fixtures "simple"))

(defn make-simplest-tree [group] {
   :files [(->FileSummary group (fu/path "tree" "foo.txt") 10000, 1) ]
   :dirs []
   :summary (->DirSummary group (fu/path "tree") 20000 1 1)})

(def simplest-tree (make-simplest-tree :group))

(def simple-tree
  {:files [(->FileSummary :group (fu/path "tree" "foo.txt") 10000, 1)
           (->FileSummary :group (fu/path "tree" "bar.txt") 20000, 10)]
   :dirs [{:files []
           :dirs []
           :summary (->DirSummary :group (fu/path "tree" "empty_child") 0 0 0) }
          {:files [(->FileSummary :group (fu/path "tree" "child" "foo.txt") 10000, 1)]
           :dirs []
           :summary (->DirSummary :group (fu/path "tree" "child") 10000 1 1) }]
   :summary (->DirSummary :group (fu/path "tree") 30000 12 3)})

(facts "fstree-seq returns a seq of the summaries in a file system tree"
   (fstree-seq simplest-tree)
   => (just [(->FileSummary :group (fu/path "tree" "foo.txt") 10000, 1)
             (->DirSummary :group (fu/path "tree") 20000 1 1)]
            :in-any-order)
      
   (map :file (fstree-seq simple-tree))
   => (just [(fu/path "tree" "empty_child")
             (fu/path "tree" "child" "foo.txt")
             (fu/path "tree" "child")
             (fu/path "tree" "foo.txt")
             (fu/path "tree" "bar.txt")
             (fu/path "tree")] :in-any-order))
            

(fact "duplicates are found by sorting nodes by decreasing size, then by hash, then by path"
  (map :file (sort-by size-and-hash-sort-key 
                      [(->FileSummary :group (fu/path "foo") 100 10)
                       (->FileSummary :group (fu/path "bar") 100 20)
                       (->FileSummary :group (fu/path "baz") 200 20)
                       (->DirSummary :group (fu/path "boo") 100 10 1)]))
  => [(fu/path "bar") (fu/path "baz") (fu/path "boo") (fu/path "foo")])

(fact "is-ancestor-of determines if a file is an ancestor of another file"
  (is-ancestor-of (fu/path "foo") (fu/path "foo")) => false
  (is-ancestor-of (fu/path "foo") (fu/path "foo" "bar")) => true
  (is-ancestor-of (fu/path "foo") (fu/path "foo" "bar" "baz")) => true
  (is-ancestor-of (fu/path "foo" "ba") (fu/path "foo" "bar")) => false)

(fact "is-summary-ancestor-of won't match files in different groups"
  (is-summary-ancestor-of (->FileSummary :a (fu/path "foo") 1 1)
                          (->FileSummary :a (fu/path "foo" "bar") 1 1))
  => true
  (is-summary-ancestor-of (->FileSummary :a (fu/path "foo") 1 1)
                          (->FileSummary :b (fu/path "foo" "bar") 1 1))
  => false)

(fact "without-ancestors filters a sequence of matching summaries, removing any that are ancestors of another in the seq"
  (without-ancestors [(->DirSummary :group (fu/path "root") 100 1 1)
                      (->DirSummary :group (fu/path "root" "child") 100 1 1)
                      (->DirSummary :group (fu/path "root" "child" "grandchild") 100 1 1)
                      (->DirSummary :group (fu/path "other") 100 1 1)])
  => [(->DirSummary :group (fu/path "root" "child" "grandchild") 100 1 1)
      (->DirSummary :group (fu/path "other") 100 1 1)])

(facts "duplicates returns a list of all duplicate groups in a file system tree, sorted as above, with self-ancestors removed"
  (fact "if there are no duplicates, returns an empty seq" 
    (duplicates [simplest-tree])
    => [])
  (fact "if there is a single duplicate, return a single result containing all duplicates"
    (duplicates [simple-tree])
    => [[ (->FileSummary :group (fu/path "tree" "child" "foo.txt") 10000 1)
          (->FileSummary :group (fu/path "tree" "foo.txt") 10000, 1)]])

    (fact "checks across multiple trees and groups"
    (duplicates [simple-tree (make-simplest-tree :2nd)])
    => [[ (->FileSummary :group (fu/path "tree" "child" "foo.txt") 10000 1)
          (->FileSummary :group (fu/path "tree" "foo.txt") 10000 1)
          (->FileSummary :2nd (fu/path "tree" "foo.txt") 10000 1)]])

   ; TODO: redundant?  See integration test in core_test.clj
  (fact "if there are more than one duplicate, return them sorted as above"
        (let [complex-result (duplicates [(treeify :group simple-fixture)])]
      (count complex-result) 
      => 3
      (map #(:bytes (first %)) complex-result)
      => [2 1 1]
      (map :file (first complex-result)) 
      => [(fu/rel-path simple-fixture "ab") (fu/rel-path simple-fixture "ab_split") (fu/rel-path simple-fixture "parent" "child")]
      (map #(fu/file-name (:file %)) (nth complex-result 1)) 
      => ["b.txt" "b.txt" "child_b.txt" "parent_b.txt"]
      (map #(fu/file-name (:file %)) (nth complex-result 2)) 
      => ["a.txt" "a.txt" "child_a.txt" "parent_a.txt"])))
