(ns deduplicatr.fstree
  (:use deduplicatr.hash
        [deduplicatr.file :only [file-hash dir-summary]])
  (:import (java.io File RandomAccessFile)))

(def ^:dynamic *file-hash-fn* deduplicatr.file/file-hash)

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

(defn- hash-files-by-name
  "return a map by file name of file hash results"
  [files]
  (reduce
    (fn [memo ^File file]
      (assoc memo (.getName file) (*file-hash-fn* file)))
    {}
    files))

(defn add-files-to-summaries
  "add all files in a map of files already hashed and mapped by name, to a seq of summaries"
  [summaries-so-far file-hashes-by-name]
  (reduce
    (fn [summaries file-and-hash]
      (let [[filename {:keys [hash bytes]}] file-and-hash]
        ; NOTE: doall to force non-laziness - without this you get stack overflows on large filesystems
       (doall (map #(*dir-summary-fn* % filename hash bytes) summaries))))
    summaries-so-far
    file-hashes-by-name
    ))

(defn treeify-and-summarize
  "traverses a directory, building a tree of contents and adding summaries of all met files to the visitors passed in
  - returns a tuple of [(hash info about this dir),(updated summaries)]"
  [^File dir summaries-so-far]
    (let [my-summary (*dir-summary-fn* dir)  ; 1-arg call gives an initial summary
          children (.listFiles dir)
          child-files (filter (fn [^File f] (.isFile f)) children)
          child-dirs (filter (fn [^File f] (.isDirectory f)) children)
         ; need to summarise to all passed summary info, as well as our new one
          summaries-including-mine (conj summaries-so-far my-summary)
         ; traverse child directories, updating summaries as we go
          [child-dir-trees summaries-including-descendants] (populate-child-dirs-and-summaries child-dirs summaries-including-mine)
         ; calculate hashes of immediate children of this dir
         child-file-hashes-by-name (hash-files-by-name child-files)
         ; and add immediate children to the summaries
          resulting-summaries (add-files-to-summaries summaries-including-descendants child-file-hashes-by-name)
           ]
      [{:files child-file-hashes-by-name
        :dirs child-dir-trees
        :summary (first resulting-summaries)}
       (rest resulting-summaries)]
    ))

(defn treeify
  "traverses a dir"
  [root]
  (first (treeify-and-summarize root '())))
