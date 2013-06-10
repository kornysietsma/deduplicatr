;; ## Functions to build and manipulate "fstree"s - trees of file system information and summaries.
;; some of this is best documented through the tests - see the test directory in the source.
(ns deduplicatr.fstree
  (:use deduplicatr.hash
        [deduplicatr.file :only [file-summary dir-summary empty-dir-summary]]
        [fileutils.fu :as fu])
  (:import [java.nio.file Path]))

(defn treeify
  "traverses a directory, building a tree of contents as a map
    where each node is a hash of :

*     :files -> a seq of each child file's FileSummary
*     :dirs -> a seq of each child dir's tree structure
*     :summary -> the DirSummary of the directory - it's name, size, file count, and accumulated hash of all descendants"

  [group ^Path dir]
    (let [initial-dir-summary (empty-dir-summary group dir)  ; 1-arg call gives an initial summary
          children (fu/children dir)
          child-files (filter fu/is-real-file children)
          child-dirs (filter fu/is-real-dir children)
          child-file-summaries (map (partial file-summary group) child-files)
          child-dir-trees (map (partial treeify group) child-dirs)
          all-child-summaries (concat child-file-summaries (map :summary child-dir-trees))
          my-summary (if (seq all-child-summaries) ; if we have any children at all
                         (apply dir-summary initial-dir-summary all-child-summaries)
                         initial-dir-summary)
         ]
      {:files child-file-summaries
        :dirs child-dir-trees
        :summary my-summary }
    ))
