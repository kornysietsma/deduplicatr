(ns deduplicatr.fstree
  (:use deduplicatr.hash
        [deduplicatr.file :only [file-summary dir-summary]])
  (:import (java.io File RandomAccessFile)))

(def ^:dynamic *file-summary-fn* deduplicatr.file/file-summary)
(def ^:dynamic *dir-summary-fn* deduplicatr.file/dir-summary)

; note that treeify-and-summarize is called recursively from update-map-and-summaries-for-a-dir
;  and it's unavoidable - no trampoline will save us, we just need to have a stack big enough
;  to cope with the directory we are traversing.
(declare treeify-and-summarize)

(defn- update-map-and-summaries-for-a-dir
  "intermediate fn for use in reduce below - takes a memo of [results so far, summaries so far] and adds a single dir"
  [memo ^File child-dir]
  (let [[map-so-far summaries-so-far] memo
        child-name (.getName child-dir)
        [child-treeified updated-summaries] (treeify-and-summarize child-dir summaries-so-far)
        new-hash (assoc map-so-far child-name child-treeified)]
      [new-hash updated-summaries]))

(defn- populate-child-dirs-and-summaries
  "takes a list of child dirs, and a seq of summaries, calls treeify-and-hash on each child, updates summaries,
  and returns a tuple of [{:name treeify-results} [updated summaries]] for use by caller"
  [child-dirs summaries-so-far]
  (reduce
      update-map-and-summaries-for-a-dir
      [{}, summaries-so-far]
      child-dirs))

(defn- summarize-files-by-name
  "return a map by file name of file summary"
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
  "traverses a directory, building a tree of contents and adding summaries of all met files to the visitors passed in
   parameters are a directory, and a seq of dirSummaries that need to be updated
     (generally, the summaries of all parents of this dir)
   returns a tuple of [{tree of info about this dir and it's descendents},(updated directory summaries)]"
  [^File dir summaries-so-far]
    (let [my-summary (*dir-summary-fn* dir)  ; 1-arg call gives an initial summary
          children (.listFiles dir)
          child-files (filter (fn [^File f] (.isFile f)) children)
          child-dirs (filter (fn [^File f] (.isDirectory f)) children)
         ; need to summarise to all passed summary info, as well as our new one
          summaries-including-mine (conj summaries-so-far my-summary)
         ; traverse child directories, updating summaries as we go
          [child-dir-trees summaries-including-descendants] (populate-child-dirs-and-summaries child-dirs summaries-including-mine)
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
     :files -> a map of each child file (by name) and it's FileSummary
     :dirs -> a map of each child dir (by name) and it's associated tree
     :summary -> the DirSummary of the directory - it's name, size, file count, and accumulated hash of all descendants"
  [root]
  (first (treeify-and-summarize root '())))
