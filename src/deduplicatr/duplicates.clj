(ns deduplicatr.duplicates
  (:use deduplicatr.fstree)
  (:import (deduplicatr.file FileSummary DirSummary)))

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

(defn duplicates
  [tree]
; traverse tree, build up list of summaries, partition by [size, hash], sort by size, filter out all singles.
  0)