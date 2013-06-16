;; ## Logic to find duplicate sets in a tree of directory and file information.
(ns deduplicatr.duplicates
  (:use deduplicatr.fstree)
  (:require [fileutils.fu :as fu]
            [taoensso.timbre :refer [info] :as timbre])
  (:import [deduplicatr.file FileSummary]
           [java.nio.file Path]))

(defn- summary-of-dir-or-filesummary
  "returns appropriate FileSummary for a node being traversed - the summary entry from a map (for a directory) or the node itself (for a file)"
  [file-or-dir]
  (if (contains? file-or-dir :summary)
    (:summary file-or-dir)
    file-or-dir))

(defn fstree-seq
  "returns a lazy sequence of the summary nodes in a fstree"
  [root]
  (map summary-of-dir-or-filesummary 
       (tree-seq 
         #(contains? % :files)
         #(concat (:dirs %) (:files %)) 
         root)))

(defn size-and-hash-sort-key
  "sort key for sorting Summaries by (decreasing) size, then by hash"
  [summary]
  [(- (:bytes summary)) (:hash summary) (fu/get-path (:file summary))])

(defn is-ancestor-of
  [^Path ancestor ^Path descendant]
  (and (.startsWith descendant ancestor)
       (not (= descendant ancestor))))

(defn is-summary-ancestor-of
  "checks if a FileSummary is another FileSummary's ancestor - assumes they share a common root directory"
  [ancestor descendant]
  (and
   (= (:group ancestor) (:group descendant))
   (is-ancestor-of (:file ancestor) (:file descendant))))

(defn- is-ancestor-of-any
  "check if a summary is the ancestor of any of a list of other FileSummaries"
  [summary summaries]
  (if (isa? FileSummary summary)
    false
    (some (partial is-summary-ancestor-of summary) summaries)))

(defn without-ancestors
  "given a seq of FileSummaries, remove any that are ancestors of later summaries - assumes files are sorted by path!"
  [summaries]
  (keep-indexed 
    ; TODO: does subvec work better than nthrest?
    ; TODO: consider removing sort prerequisite - feels ugly, only small speed improvement
    (fn [index item] (if (is-ancestor-of-any item (nthrest summaries (inc index))) nil item))
    summaries))

(defn- log-in-thread [message thread-thing]
  (info message)
  thread-thing)

(defn all-descendants-of
  "true if all members of a group are descendants of members of another group"
  [group possible-ancestor]
  (every?
   (fn [summary]
     (some
      (fn [ancestor-summary]
        (is-summary-ancestor-of ancestor-summary summary))
      possible-ancestor))
   group))

(defn all-descendants-of-any
  "true if all members of a group are descendants of one of the supplied groups"
  [group groups]
  (some (partial all-descendants-of group) groups))

(defn child-pruning-reducer [groups-so-far group]
  (if (all-descendants-of-any group groups-so-far)
    groups-so-far
    (conj groups-so-far group)))

(defn prune-children
  "given a seq of duplicate summary groups, in decreasing size order,
   remove any that are children of an earlier group.
   note that all groups in the child group must have ancestors in the same ancestor group"
  [groups]
  (reduce child-pruning-reducer
          '[]
          groups))

(defn- remove-singles
  [summary-groups]
  (filter next summary-groups))

(defn duplicates
  "finds duplicate directory/file sets in seq of fstrees
   * returns a seq of seqs of identical files/directories
   * results are sorted so the largest sets are first
   * assumes fstrees know about groups already
   "
  [trees]
  (->> trees
       (#(flatten (map fstree-seq %)))
       (log-in-thread "sorting...")
       (sort-by size-and-hash-sort-key) ; sort largest first, then by hash
       (log-in-thread "partitioning")
       (partition-by :hash) ; partition into subseqs by hash
       (log-in-thread "removing unduplicated")
       remove-singles ; remove any with a single subseq (i.e. not a duplicate)
       (log-in-thread "removing ancestors")
       (map without-ancestors) ; remove ancestors
       (log-in-thread "removing unduplicated again")
       remove-singles ; remove non-duplicates again after ancestor
; prune
       (log-in-thread "pruning subsets of groups already matched")
       prune-children)) ; remove child groups that match earlier ones
