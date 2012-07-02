(ns deduplicatr.core
  (:use [clojure.tools.cli :only [cli]]
        [clojure.java.io :only [file]]
        [deduplicatr.duplicates :only [duplicates]]
        [deduplicatr.fstree :only [treeify]])
  (:import (deduplicatr.file FileSummary DirSummary))
  (:gen-class :main true))

(defn show-duplicates
  [root]
  (let [tree (treeify root)
        _ (println "tree parsed")
        dups (duplicates tree)
        _ (println "dups found")]
  (doseq [files dups]
    (println (count files) " files of size " (.bytes (first files)))
    (doseq [summary files]
      (if (instance? DirSummary summary)
        (println "   " (str (.getPath (.file summary)) "/ (" (.filecount summary) " files)"))
        (println "   " (.getPath (.file summary)))
        )))))

(defn -main
  "The main entry point"
  [& args]
  (let [[options args banner]
        (cli args
             ["-h" "--help" "Show help" :default false :flag true]
             ["-root" "root directory to scan" :default "." :parse-fn #(file %)]
             )
        root (file (:root options))]
    (when (:help options)
      (println banner)
      (System/exit 0))
    (when (not (.isDirectory root))
      (println root " is not a valid directory")
      (System/exit 0))
    (show-duplicates root)
    ))
