(ns deduplicatr.t-core
  (:require
   [midje.sweet :refer :all]
   [deduplicatr.core :refer :all]
   [deduplicatr.duplicates :refer [duplicates]]
   [deduplicatr.fstree :refer [treeify]]
   [deduplicatr.diffs :as diffs]
   [taoensso.timbre :as timbre]
   [fileutils.fu :as fu]))

(timbre/set-level! :warn)

(def fixtures (fu/path "test" "fixtures"))
(def simple-fixture (fu/rel-path fixtures "simple"))
(def complex-fixture (fu/rel-path fixtures "complex"))

(defmacro with-out-ignored
  [& body]
  `(
    binding [*out* (new java.io.StringWriter)]
    ~@body
       ))

(defn find-dups-quietly
  "just to keep functional testing simpler"
  ([roots] (find-dups-quietly roots {}))
  ([roots options]
      (with-out-ignored
        (find-dups roots (merge options {:ignore #{}})))))

(defn find-diffs-quietly
  [root1 root2]
  (with-out-ignored
    (diffs/find-pruned-diffs root1 root2 nil {:ignore #{}})))

(defchecker with-path-ending [expected]
  (chatty-checker [actual]
                  (.endsWith (fu/get-path actual) expected)))

(defchecker file-like
  [{:keys [group bytes name]}]
  (every-checker
   (chatty-checker [actual] (or (nil? group) (= group (:group actual))))
   (chatty-checker [actual] (or (nil? bytes) (= bytes (:bytes actual))))
   (chatty-checker [actual] (or (nil? name) (.endsWith (fu/get-path (:file actual)) name)))))

(defchecker dir-like
  [{:keys [group bytes filecount name]}]
  (every-checker
   (chatty-checker [actual] (or (nil? group) (= group (:group actual))))
   (chatty-checker [actual] (or (nil? bytes) (= bytes (:bytes actual))))
   (chatty-checker [actual] (or (nil? filecount) (= filecount (:filecount actual))))
   (fn [actual] (or (nil? name) (.endsWith (fu/get-path (:file actual)) name)))))

(fact "letters gives sequential letters starting with 'a'"
  (take 5 (letters)) => ["a" "b" "c" "d" "e"])

(fact "duplicates in a tree include all duplicate sets in descending size order"
  (find-dups-quietly {"a" simple-fixture})
  => (just
      (three-of
       (dir-like {:bytes 2 :filecount 2}))
      (four-of
       (file-like {:name "b.txt"}))
      (four-of
       (file-like {:name "a.txt"})))
  
  (find-dups-quietly {"a" complex-fixture})
  => (just
      (just
       (file-like {:bytes 108 :name "big_files/my_old_file.txt"})
       (file-like {:bytes 108 :name "big_files/my_other_old_file.txt"})
       :in-any-order)             
      (two-of
       (dir-like {:name "123"}))
      (three-of
       (file-like {:name "three.txt"}))
      (three-of
       (file-like {:name  "two.txt"}))
      (three-of
       (file-like {:name "one.txt"})))
  )

(fact "you can sort results by number of files rather than just size"
  (find-dups-quietly {"a" complex-fixture} {:sort-by :files})
  => (just
      (two-of
       (dir-like {:name "123"}))
      (just
       (file-like {:bytes 108 :name "big_files/my_old_file.txt"})
       (file-like {:bytes 108 :name "big_files/my_other_old_file.txt"})
       :in-any-order)             
      (three-of
       (file-like {:name "three.txt"}))
      (three-of
       (file-like {:name  "two.txt"}))
      (three-of
       (file-like {:name "one.txt"}))
      ))


(fact "duplicates in a tree don't include child dirs if their parents match"
  (find-dups-quietly {"a" (fu/rel-path fixtures "no-kids")})
  => (just
      (two-of
       (dir-like {:bytes 13 :filecount 2}))))

(fact "multiple directories can be checked"
  (find-dups-quietly {"a" (fu/rel-path simple-fixture "ab") "b" (fu/rel-path simple-fixture "ab_split")})
  => (just
      (just (dir-like {:group "a" :name "ab"})
            (dir-like {:group "b" :name "ab_split"}))))
; note other children of this test case pruned by prune-children

(fact "diffs of a tree report differences with pruning to limit noise"
      (find-diffs-quietly
       (fu/rel-path fixtures "diff_a")
       (fu/rel-path fixtures "diff_b"))
      => (just
          {:in-a (just (dir-like {:group :a :name "in_a"}))
           :in-b (just (dir-like {:group :b :name "in_b"}))
           :in-both (contains
                     (dir-like {:group :a :name "in_both"})
                     (dir-like {:group :b :name "in_both"})
                     (dir-like {:group :a :name "in_a_empty"})
                     (dir-like {:group :b :name "in_b_empty"})
                     :in-any-order)}))
