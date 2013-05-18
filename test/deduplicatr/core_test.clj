(ns deduplicatr.core-test
  (:require
   [midje.sweet :refer :all]
   [deduplicatr.core :refer :all]
   [deduplicatr.duplicates :refer [duplicates]]
   [deduplicatr.fstree :refer [treeify]]
   [clojure.java.io :refer [file]]))

(def fixtures (file "test" "fixtures"))
(def simple-fixture (file fixtures "simple"))
(def complex-fixture (file fixtures "complex"))

(defn treeify-and-find-duplicates
  "just to keep functional testing simpler" 
  [root]
  (duplicates (treeify :root root)))

(defchecker with-path-ending [expected]
  (chatty-checker [actual]
                  (.endsWith (.getPath actual) expected)))

(defchecker file-like [bytes path-end]
  (every-checker
   (chatty-checker [actual] (= bytes (:bytes actual)))
   (chatty-checker [actual] (.endsWith (.getPath (:file actual)) path-end))))

(defchecker dir-like [bytes filecount]
  (every-checker
   (chatty-checker [actual] (= bytes (:bytes actual)))
   (chatty-checker [actual] (= filecount (:filecount actual)))))

(defchecker dir-like-with-path [bytes filecount path-end]
  (every-checker
   (chatty-checker [actual] (= bytes (:bytes actual)))
   (chatty-checker [actual] (= filecount (:filecount actual)))
   (chatty-checker [actual] (.endsWith (.getPath (:file actual)) path-end))))

(fact "duplicates in a tree include all duplicate sets in descending size order"
  (treeify-and-find-duplicates simple-fixture)
  => (just
      (three-of
       (dir-like 2 2))
      (four-of
       (file-like 1 "b.txt"))
      (four-of
       (file-like 1 "a.txt")))
  
  (treeify-and-find-duplicates complex-fixture)
  => (just
      (just
       (file-like 108 "big_files/my_old_file.txt")
       (file-like 108 "big_files/my_other_old_file.txt")
       :in-any-order)             
      (two-of
       (dir-like-with-path 14 3 "123"))
      (three-of
       (file-like 6 "three.txt"))
      (three-of
       (file-like 4 "two.txt"))
      (three-of
       (file-like 4 "one.txt"))))

