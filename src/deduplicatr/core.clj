;; ## a command-line application for finding duplicates in a file system
;; see https://github.com/kornysietsma/deduplicatr for code and other docs

;; Note that some of this is best documented through the tests - see the test directory in the source for more.

(ns deduplicatr.core
  (:use [clojure.tools.cli :only [cli]]
        [clojure.java.io :only [file]]
        [deduplicatr.duplicates :only [duplicates]]
        [deduplicatr.fstree :only [treeify]])
  (:import (deduplicatr.file FileSummary))
  (:gen-class :main true))

(defn show-duplicates
  "print names of duplicate file sets to standard output"
  [root]
  (let [tree (treeify :root root)
        _ (println "tree parsed")
        dups (duplicates tree)
        _ (println "dups found")]
  (doseq [identical-files dups]
    (println (count identical-files) " matches of size " (.bytes (first identical-files)))
    (doseq [summary identical-files]
      (if (.is-dir summary)
        (println "   " (str (.getPath (.file summary)) "/ (" (.filecount summary) " files)"))
        (println "   " (.getPath (.file summary)))
        )))))

(defn -main
  "The main entry point - collects command-line arguments and calls show-duplicates."
  [& args]
  (let [[options args banner]
        (cli args
             ["-h" "--help" "Show help" :default false :flag true]
             )
        root (file (first args))]
    ; warning: procedural code follows!
    (when (:help options)
      (println banner "\n followed by a directory to scan")
      (System/exit 0))
    (when (not (= 1 (count args)))
      (println "You must specify a directory to scan (and only one!)")
      (System/exit 1))
    (when (not (.isDirectory root))
      (println (first args) " is not a valid directory")
      (System/exit 1))
    (show-duplicates root)
    ))
