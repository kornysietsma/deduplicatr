;; ## Functions to build and manipulate "fstree"s - trees of file system information and summaries.
;; some of this is best documented through the tests - see the test directory in the source.
(ns deduplicatr.fstree
  (:use deduplicatr.hash
        [deduplicatr.file :only [file-summary dir-summary]])
  (:import (java.io File RandomAccessFile)))

;; the file summary function is declared dynamic so it can be overridden in tests
(def ^:dynamic *file-summary-fn* deduplicatr.file/file-summary)
;; the dir summary function is declared dynamic so it can be overridden in tests
(def ^:dynamic *dir-summary-fn* deduplicatr.file/dir-summary)

;; treeify-and-summarize is called recursively from update-map-and-summaries-for-a-dir
;;  and this is unavoidable - we can't use tail-call-optimisation in any way,
;;  as we need to keep the whole stack as we traverse the file system.
(declare treeify-and-summarize)

(defn- update-map-for-a-dir
  "intermediate fn for use in reduce below -
 takes a directory map so far and adds a single dir to the directory map"
  [map-so-far ^File child-dir]
  (let [child-name (.getName child-dir)
        child-treeified (treeify-and-summarize child-dir)
        new-hash (assoc map-so-far child-name child-treeified)]
      new-hash))

(defn- populate-child-dirs
  "takes a list of child directories, calls treeify-and-hash on each child
  and returns a map of child dir trees by name"
  [child-dirs]
  (reduce
      update-map-for-a-dir
      {}
      child-dirs))

(defn- summarize-files-by-name
  "takes a seq of files, returns a map of their summaries by name"
  [files]
  (reduce
    (fn [memo ^File file]
      (assoc memo (.getName file) (*file-summary-fn* file)))
    {}
    files))

(defn add-files-to-summaries
  "add a list of filesummaries, to each dir summary in a seq of summaries"
  [dir-summaries-so-far file-summaries]
  (reduce
    (fn [summaries file-summary]
        ; NOTE: doall to force non-laziness - without this you get stack overflows on large filesystems
       (doall (map #(*dir-summary-fn* % file-summary) summaries)))
    dir-summaries-so-far
    file-summaries
    ))

(defn treeify-and-summarize ; TODO: this is now completely redundant, merge with treeify (and of course drastically simplify)
  "traverses a directory, building a fstree of files/directories contained"

  [^File dir]
    (let [current-dir-summary (*dir-summary-fn* dir)  ; 1-arg call gives an initial summary
          children (.listFiles dir)
          child-files (filter (fn [^File f] (.isFile f)) children)
          child-dirs (filter (fn [^File f] (.isDirectory f)) children)
          ; traverse child directories
          child-dir-trees (populate-child-dirs child-dirs)
          ; calculate summaries of files in this dir
          child-file-summaries-by-name (summarize-files-by-name child-files)
          child-file-summaries (vals child-file-summaries-by-name)
          child-dir-summaries (map :summary (vals child-dir-trees))
          all-child-summaries (concat child-file-summaries child-dir-summaries)
          my-summary (if (seq all-child-summaries)
                         (apply *dir-summary-fn* current-dir-summary (concat child-file-summaries child-dir-summaries))
                         current-dir-summary)
         ]
      {:files child-file-summaries-by-name
        :dirs child-dir-trees
        :summary my-summary }
    ))

(defn treeify  
  "traverses a directory, building a tree of contents as a map
    where each node is a hash of :

*     :files -> a map of each child file (by name) and it's FileSummary  TODO: could be a seq now FileSummary has a file!
*     :dirs -> a map of each child dir (by name) and it's associated tree  TODO: could be a seq now FileSummary has a file!
*     :summary -> the DirSummary of the directory - it's name, size, file count, and accumulated hash of all descendants"
  [root]
  (treeify-and-summarize root ))
