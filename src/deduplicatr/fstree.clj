;; ## Functions to build and manipulate "fstree"s - trees of file system information and summaries.
;; some of this is best documented through the tests - see the test directory in the source.
(ns deduplicatr.fstree
  (:use deduplicatr.hash)
  (:require 
   [deduplicatr.file :refer [file-summary dir-summary empty-dir-summary]]
   [fileutils.fu :as fu]
   [deduplicatr.progress-logger :as plog]
   [taoensso.timbre :refer [info]])
  (:import [java.nio.file Path]))

(defn- summarize-file-and-log [logger group file]
  (let [summary (file-summary group file)]
    (do (when logger (plog/log-file logger (:bytes summary) (fu/get-path file)))
        summary)))

(defn treeify
  "traverses a directory, building a tree of contents as a map
    where each node is a hash of :

*     :files -> a seq of each child file's FileSummary
*     :dirs -> a seq of each child dir's tree structure
*     :summary -> the DirSummary of the directory - it's name, size, file count, and accumulated hash of all descendants"

  [group ^Path dir logger]
  (let [initial-dir-summary (empty-dir-summary group dir)  ; 1-arg call gives an initial summary
        _ (when logger (plog/log-dir logger (fu/get-path dir)))

        children (fu/children dir)
        child-files (filter fu/is-real-file children)
        child-dirs (filter fu/is-real-dir children)
        child-file-summaries (map (partial summarize-file-and-log logger group) child-files)
        child-dir-trees (map #(treeify group % logger) child-dirs)
        all-child-summaries (concat child-file-summaries (map :summary child-dir-trees))
        my-summary (if (seq all-child-summaries) ; if we have any children at all
                     (apply dir-summary initial-dir-summary all-child-summaries)
                     initial-dir-summary)
        ]
    {:files child-file-summaries
     :dirs child-dir-trees
     :summary my-summary }
    ))
