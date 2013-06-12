(defproject deduplicatr "0.2.0-SNAPSHOT"
  :description "A command-line application for finding duplicate files and directories"
  :url "https://github.com/kornysietsma/deduplicatr"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [commons-codec/commons-codec "1.4"]
                 [org.clojure/tools.cli "0.2.2"]
                 [com.taoensso/timbre "2.1.2"]]
  :profiles {:dev {:dependencies [[midje "1.5.1"]]
                   :plugins [[lein-midje "3.0.1"]
                             [lein-marginalia "0.7.1"]]  }}
  :min-lein-version "2.0.0"
  :main deduplicatr.core
  :jvm-opts ["-Xmx7500m" "-Xss100m"] 
)
