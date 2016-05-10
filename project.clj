(defproject deduplicatr "0.4.1"
  :description "A command-line application for finding duplicate files and directories"
  :url "https://github.com/kornysietsma/deduplicatr"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [commons-codec/commons-codec "1.10"]
                 [org.clojure/tools.cli "0.3.1"]
                 [com.taoensso/timbre "3.4.0"]]
  :profiles {:dev {:dependencies [[midje "1.6.3"]]
                   :plugins [[lein-midje "3.1.3"]
                             [lein-marginalia "0.8.0"]]  }}
  :min-lein-version "2.0.0"
  :main deduplicatr.core
  :jvm-opts ["-Xmx2048m" "-Xss100m"] 
)
