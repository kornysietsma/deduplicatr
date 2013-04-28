(ns deduplicatr.core-test
  (:use midje.sweet
        deduplicatr.core
        [deduplicatr.duplicates :only [duplicates]]
        [deduplicatr.fstree :only [treeify]]
        [clojure.java.io :only [file]])
  (:import (java.io File)))

(def fixtures (file "test" "fixtures"))
(def simple-fixture (file fixtures "simple"))
(def complex-fixture (file fixtures "complex"))

(defn treeify-and-find-duplicates
  "just to keep functional testing simpler" 
  [root]
  (duplicates (treeify :root root)))

(defchecker with-path-ending [expected]
  (checker [actual]
    (.endsWith (.getPath actual) expected)))

(fact "duplicates in a tree include all duplicate sets in descending size order"
      (treeify-and-find-duplicates simple-fixture)
      => (just
           (three-of 
             (contains {:bytes 2, :filecount 2}))
           (four-of
             (contains {:bytes 1, :file (with-path-ending "b.txt")}))
           (four-of
             (contains {:bytes 1, :file (with-path-ending "a.txt")}))
           )
      (treeify-and-find-duplicates complex-fixture)
      => (just
           (just
             (contains {:bytes 108, :file (with-path-ending "big_files/my_old_file.txt")})
             (contains {:bytes 108, :file (with-path-ending "big_files/my_other_old_file.txt")})
             :in-any-order)             
           (two-of 
             (contains {:bytes 14, :filecount 3, :file (with-path-ending "123")}))
           (three-of
             (contains {:bytes 6, :file (with-path-ending "three.txt")}))
           (three-of
             (contains {:bytes 4, :file (with-path-ending "two.txt")}))
           (three-of
             (contains {:bytes 4, :file (with-path-ending "one.txt")}))
           )
      )

