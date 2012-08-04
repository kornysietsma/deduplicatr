;; ## Functions to build and manipulate "fstree"s - trees of file system information and summaries.
;; some of this is best documented through the tests - see the test directory in the source.
(ns deduplicatr.fstree
  (:use deduplicatr.hash
        [deduplicatr.file :only [file-summary dir-summary]])
  (:import (java.io File RandomAccessFile)))

(defn treeify
  "traverses a directory, building a tree of contents as a map
    where each node is a hash of :

*     :files -> a seq of each child file's FileSummary
*     :dirs -> a seq of each child dir's tree structure
*     :summary -> the DirSummary of the directory - it's name, size, file count, and accumulated hash of all descendants"

  [^File dir]
    (let [current-dir-summary (dir-summary dir)  ; 1-arg call gives an initial summary
          children (.listFiles dir)
          child-files (filter (fn [^File f] (.isFile f)) children)
          child-dirs (filter (fn [^File f] (.isDirectory f)) children)
          child-file-summaries (map file-summary child-files)
          child-dir-trees (map treeify child-dirs)
          all-child-summaries (concat child-file-summaries (map :summary child-dir-trees))
          my-summary (if (seq all-child-summaries) ; if we have any children at all
                         (apply dir-summary current-dir-summary all-child-summaries)
                         current-dir-summary)
         ]
      {:files child-file-summaries
        :dirs child-dir-trees
        :summary my-summary }
    ))
