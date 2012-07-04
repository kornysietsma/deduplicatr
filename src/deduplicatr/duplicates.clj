(ns deduplicatr.duplicates
  (:use deduplicatr.fstree)
  (:import (deduplicatr.file FileSummary)
           (java.io File)))

(set! *warn-on-reflection* true)

(defn- all-children
  "traverse both child directories, and child file summaries, of a directory in a fstree"
  [node]
  (concat (vals (:dirs node)) (vals (:files node))))

(defn- is-dir?
  "test if a node is a FileSummary (i.e. a file) or a map (i.e. a directory)"
  [node]
  (not (isa? FileSummary node)))

(defn- summary-of-dir-or-filesummary
  "traversal of nodes returns either a map for a dir, or a FileSummary for a file - extract appropriate summary"
  [file-or-dir]
  (if (is-dir? file-or-dir)
    (:summary file-or-dir)
    file-or-dir))

(defn fstree-seq
  "returns a lazy sequence of the summary nodes in a fstree"
  [root]
  (map (fn [n] (if (contains? n :summary) (:summary n) n)) (tree-seq #(contains? % :files) all-children root)))

(defn size-and-hash-sort-key
  [^FileSummary summary]
  [(- (.bytes summary)) (.hash summary) (.getPath (.file summary))])

(defn is-ancestor-of
  "checks if a file is another file's ancestor - assumes they are from same root though!"
  [^File file1 ^File file2]
  (.startsWith (.getPath file2) (str (.getPath file1) File/separator)))

(defn- is-summary-ancestor-of
  [^FileSummary summary1 ^FileSummary summary2]
  (is-ancestor-of (.file summary1) (.file summary2)))

(defn- is-ancestor-of-any
  [summary summaries]
  (if (isa? FileSummary summary)
    false
    (some (partial is-summary-ancestor-of summary) summaries)))

(defn without-ancestors
  "remove summaries in a matching summary seq that are ancestors of others in the seq - assumes files sorted by path!"
  [summaries]
  (keep-indexed 
    (fn [index item] (if (is-ancestor-of-any item (nthrest summaries (inc index))) nil item))
    summaries))

(defn duplicates
  [tree]
; traverse tree, build up list of summaries, sort so duplicates are together, partition by hash, filter out all singles.
  (let [nodeseq (fstree-seq tree)
        sorted (sort-by size-and-hash-sort-key nodeseq)
        partitioned (partition-by :hash sorted)
        only-dups (remove #(= 1 (count %)) partitioned)
        with-ancestors-pruned (map without-ancestors only-dups)
        with-ancestors-pruned-only-dups (remove #(= 1 (count %)) with-ancestors-pruned)]
    with-ancestors-pruned-only-dups)
)