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

(defn- update-map-and-summaries-for-a-dir
  "intermediate fn for use in reduce below -
 takes a memo of [directory map so far, summaries so far] and adds a single dir to the directory map and the summaries, returning a new memo"
  [memo ^File child-dir]
  (let [[map-so-far summaries-so-far] memo
        child-name (.getName child-dir)
        [child-treeified updated-summaries] (treeify-and-summarize child-dir summaries-so-far)
        new-hash (assoc map-so-far child-name child-treeified)]
      [new-hash updated-summaries]))

(defn- populate-child-dirs-and-summaries
  "takes a list of child directories, and a seq of summaries, calls treeify-and-hash on each child, updates summaries,
  and returns a tuple of [{map of child dir trees by name} [updated summaries]] for use by caller"
  [child-dirs summaries-so-far]
  (reduce
      update-map-and-summaries-for-a-dir
      [{}, summaries-so-far]
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

(defn treeify-and-summarize
  "traverses a directory, building a fstree of files/directories contained, and also adds summaries of all files to the summaries-so-far parameter

   function parameters are a directory to scan, and a seq of FileSummaries that need to be updated
     (generally, the summaries of all parents of this dir)

   returns a tuple of [{resulting fstree},(updated directory summaries)]"
  [^File dir summaries-so-far]
    (let [current-dir-summary (*dir-summary-fn* dir)  ; 1-arg call gives an initial summary
          children (.listFiles dir)
          child-files (filter (fn [^File f] (.isFile f)) children)
          child-dirs (filter (fn [^File f] (.isDirectory f)) children)
          ; add current summary to summaries-so-far so we can pass it to child directories - this is basically the visitor pattern
          summaries-including-current (conj summaries-so-far current-dir-summary)
          ; traverse child directories, updating summaries as we go
          [child-dir-trees summaries-including-descendants] (populate-child-dirs-and-summaries child-dirs summaries-including-current)
          ; calculate summaries of files in this dir
          child-file-summaries-by-name (summarize-files-by-name child-files)
          ; and add file summaries to the dir summaries
          resulting-summaries (add-files-to-summaries summaries-including-descendants (.values child-file-summaries-by-name))
         ]
      [{:files child-file-summaries-by-name
        :dirs child-dir-trees
        :summary (first resulting-summaries)}  ; pop the current directory summary off the list
       (rest resulting-summaries)]  ; pass parent summaries back to the caller
    ))

(defn treeify
  "traverses a directory, building a tree of contents as a map
    where each node is a hash of :

*     :files -> a map of each child file (by name) and it's FileSummary
*     :dirs -> a map of each child dir (by name) and it's associated tree
*     :summary -> the DirSummary of the directory - it's name, size, file count, and accumulated hash of all descendants"
  [root]
  (first (treeify-and-summarize root '())))
