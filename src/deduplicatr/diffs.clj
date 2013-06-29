(ns deduplicatr.diffs
  (:require [deduplicatr.duplicates :as dups]
            [deduplicatr.fstree :as fstree]
            [deduplicatr.file :as file]
            [fileutils.fu :as fu]
            [taoensso.timbre :as timbre])
  (:import [deduplicatr.file FileSummary DirSummary]
           [java.nio.file Path]))


(defn- diffing-reducer
  [{:keys [in-a in-b in-both]} b-summary]
  (let [b-hash (:hash b-summary)]
    (cond
     (contains? in-a b-hash)
     {:in-a (dissoc in-a b-hash)
      :in-b in-b
      :in-both (assoc in-both b-hash (conj (in-a b-hash) b-summary))}
     (contains? in-both b-hash)
     {:in-a in-a
      :in-b in-b
      :in-both (assoc in-both b-hash (conj (in-both b-hash) b-summary))}
     (contains? in-b b-hash)
     {:in-a in-a
      :in-b (assoc in-b b-hash (conj (in-b b-hash) b-summary))
      :in-both in-both}
     :else
     {:in-a in-a
      :in-b (assoc in-b b-hash [b-summary])
      :in-both in-both}
     )))

(defn calc-diffs [seq-a seq-b]
 (reduce diffing-reducer
         {:in-a (group-by :hash seq-a)
          :in-b {}
          :in-both {}}
         seq-b))

(defn- descendant-of-any
  [summary summaries]
  (some #(dups/is-summary-ancestor-of % summary) summaries))

(defn- parent-of-any [summary summaries]
  (some #(= (fu/parent (.file %)) (.file summary)) summaries))

(defn- ancestor-of-any [summary summaries]
  (some #(dups/is-summary-ancestor-of summary %) summaries))

(defn child-pruning-reducer [summaries-so-far summary]
  (if (descendant-of-any summary summaries-so-far)
    summaries-so-far
    (conj summaries-so-far summary)))

(defn flatten-and-prune [map-by-hash]
  (let [summaries (flatten (vals map-by-hash))
        sorted (sort-by dups/size-and-hash-sort-key summaries)]
    (reduce child-pruning-reducer [] sorted)))

;; pruning is hard.
;; in-both can be pruned by removing all descendants of an existing
;; node - this is easy
;; in-a or in-b are more work.  Basically, for each N in decreasing size order:
;;   if N is a directory
;;      if no descendants of N are in :in-both  (or could be no _children_ of N?)
;;        then N is in :in-x in it's entirety, and we can emit it and prune all children
;;        else N is partly in :in-x so ignore it, keep going to it's children
;;   else (N is a file) - emit N unless it's a child of the prune list
;; note this should work with a pruned version of in-both...

(defn aorb-pruning-reducer [memo summary]
  (let [{:keys [in-both in-x to-prune]} memo]
    (if (instance? DirSummary summary)
      (if (parent-of-any summary in-both)
        memo ;; ignore
        {:in-both in-both
         :in-x (conj in-x summary)
         :to-prune (conj to-prune summary)})
      ; else must be FileSummary
      (if (descendant-of-any summary to-prune)
        memo ; ignore
        {:in-both in-both
         :in-x (conj in-x summary)
         :to-prune to-prune}))))

(defn prune-aorb [in-x-map in-both]
  (let [sorted-in-x (sort-by dups/size-and-hash-sort-key (flatten (vals in-x-map)))]
    (:in-x (reduce aorb-pruning-reducer {:in-both in-both :in-x [] :to-prune []} sorted-in-x))))

(defn calc-pruned-diffs [seq-a seq-b]
  (let [{:keys [in-a in-b in-both]} (calc-diffs seq-a seq-b)
        in-both-pruned (flatten-and-prune in-both)]
    {:in-a (prune-aorb in-a in-both-pruned)
     :in-b (prune-aorb in-b in-both-pruned)
     :in-both in-both-pruned}))

(defn calc-tree-diffs "find diffs between two summary trees
  returns map of :in-a :in-b and :in-both => sequences of relevant top-level summaries"
  [tree-a tree-b]
  (calc-pruned-diffs (dups/fstree-seq tree-a) (dups/fstree-seq tree-b)))

(defn find-pruned-diffs [root1 root2 logger options]
  (let [files1 (fstree/treeify :a root1 logger options)
        files2 (fstree/treeify :b root2 logger options)
        _ (timbre/info "calculating diffs")]
    (calc-tree-diffs files1 files2)))
