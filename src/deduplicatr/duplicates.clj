;; ## Logic to find duplicate sets in a tree of directory and file information.
(ns deduplicatr.duplicates
  (:use deduplicatr.fstree)
  (:import (deduplicatr.file FileSummary)
           (java.io File)))

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
  "sort key for sorting FileSummaries by (decreasing) size, then by hash"
  [^FileSummary summary]
  [(- (.bytes summary)) (.hash summary) (.getPath (.file summary))])

(defn is-ancestor-of
  "checks if a file is another file's ancestor - assumes they share a common root directory"
  [^File file1 ^File file2]
  (.startsWith (.getPath file2) (str (.getPath file1) File/separator)))

(defn- is-summary-ancestor-of
  "checks if a FileSummary is another FileSummary's ancestor - assumes they share a common root directory"
  [^FileSummary summary1 ^FileSummary summary2]
  (is-ancestor-of (.file summary1) (.file summary2)))

(defn- is-ancestor-of-any
  "check if a FileSummary is the ancestor of any of a list of other FileSummaries"
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

(defn duplicates
  "finds duplicate directory/file sets in a fstree

   * returns a seq of seqs of identical files/directories
   * results are sorted so the largest sets are first
   "
  [tree]
  (->> tree
       (fstree-seq) ; convert to seq
       (sort-by size-and-hash-sort-key) ; sort largest first, then by hash
       (partition-by :hash) ; partition into subseqs by hash
       (filter next) ; remove any with a single subseq (i.e. not a duplicate)
       (map without-ancestors) ; remove ancestors
       (filter next)) ; remove non-duplicates again after ancestor prune
)

