(ns deduplicatr.core
  "a command-line application for finding duplicates in a file system"
  (:require [clojure.java.io :refer [file]]
            [fu.core :as fu]
            [clojure.tools.cli :refer [cli]]
            [deduplicatr.duplicates :refer [duplicates]]
            [deduplicatr.fstree :refer [treeify]]
            [deduplicatr.file :refer [print-summary]])
  (:gen-class :main true))

;; ## a command-line application for finding duplicates in a file system
;; see https://github.com/kornysietsma/deduplicatr for code and other docs

;; Note that some of this is best documented through the tests - see the test directory in the source for more.

(defn letters []
  (map (comp str char) (iterate inc (int \a))))

(defn treeify-named
  [named-roots]
  (for [[group root] named-roots]
    (do 
      (println "reading files from: (" group ") " (fu/get-path root))
      (treeify group root))))

(defn find-dups
  [named-roots]
  (let [trees (treeify-named named-roots)
        _ (println "looking for duplicates...")
        dups (duplicates trees)
        _ (println "dups found")]
    dups))

(defn show-duplicates
  "print names of duplicate file sets to standard output"
  [roots]
  (let [named-roots (into {} (map vector (letters) roots))
        dups (find-dups named-roots)]
  (doseq [identical-files dups]
    (println (count identical-files) " matches of size " (.bytes (first identical-files)))
    (doseq [summary identical-files]
      (let [base (named-roots (.group summary))]
        (println "  " (print-summary summary base)))))))

(defn -main
  "The main entry point - collects command-line arguments and calls show-duplicates."
  [& args]
  (let [[options args banner]
        (cli args
             ["-h" "--help" "Show help" :default false :flag true])
        roots (map fu/path args)]
    ; warning: procedural code follows!
    (when (:help options)
      (println banner "\n followed by a directory to scan")
      (System/exit 0))
    (when-not (> (count roots) 0)
      (println "You must specify at least one directory to scan")
      (System/exit 1))
    (doseq [root roots]
      (when (not (fu/is-real-dir root))
        (println (.getPath root) " is not a valid directory")
        (System/exit 1)))
    (show-duplicates roots)
    ))
