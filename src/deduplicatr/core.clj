(ns deduplicatr.core
  "a command-line application for finding duplicates in a file system"
  (:require [fileutils.fu :as fu]
            [clojure.tools.cli :refer [cli]]
            [deduplicatr.duplicates :refer [duplicates]]
            [deduplicatr.fstree :refer [treeify]]
            [deduplicatr.file :refer [print-summary]]
            [deduplicatr.throttler :as throttler]
            [deduplicatr.progress-logger :as plog]
            [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal spy]])
  (:gen-class :main true))

;; ## a command-line application for finding duplicates in a file system
;; see https://github.com/kornysietsma/deduplicatr for code and other docs

;; Note that some of this is best documented through the tests - see the test directory in the source for more.

(defn letters []
  (map (comp str char) (iterate inc (int \a))))

(defn treeify-named
  [named-roots logger options]
  (for [[group root] named-roots]
    (do 
      (info "reading files from: (" group ") " (fu/get-path root))
      (treeify group root logger options))))

(defn find-dups
  [named-roots options]
  (info "looking for duplicates...")
  (let [progress-agent (throttler/new-agent 30000)
        logger (plog/new-logger progress-agent)
        _ (throttler/run progress-agent #(info "(progress logged every 30 secs only)"))
        trees (treeify-named named-roots logger options)
        dups (duplicates trees)
        _ (println "duplicates:")]
    dups))

(defn show-duplicates
  "print names of duplicate file sets to standard output"
  [roots options]
  (let [named-roots (into {} (map vector (letters) roots))
        dups (find-dups named-roots options)]
  (doseq [identical-files dups]
    (println (format "%,d matches of %,d bytes" (count identical-files) (.bytes (first identical-files))))
    (doseq [summary identical-files]
      (let [base (named-roots (.group summary))]
        (println "  " (print-summary summary base)))))))

(defn- terse-prefix-fn
  [{:keys [level]}]
  (-> level name clojure.string/upper-case))

(defn- process-options [raw-options]
  {:ignore (set (:ignore raw-options))})

(defn -main
  "The main entry point - collects command-line arguments and calls show-duplicates."
  [& args]
  (let [[options args banner]
        (cli args
             ["-h" "--help" "Show help" :default false :flag true]
             ["-i" "--ignore" "(exact) file/dir names to ignore"
              :parse-fn #(vec (.split % ","))
              :default [".DS_Store"]])
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
        (println (fu/get-path root) " is not a valid directory")
        (System/exit 1)))
    (timbre/set-config! [:prefix-fn] terse-prefix-fn)
    (show-duplicates roots (process-options options))
    (shutdown-agents)
    ))
