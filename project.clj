(defproject deduplicatr "0.1.0-SNAPSHOT"
  :description "A command-line application for finding duplicates in a file system"
  :url "https://github.com/kornysietsma/deduplicatr"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [commons-codec/commons-codec "1.4"]
                 [org.clojure/tools.cli "0.2.2"]]
  :profiles {:dev {:dependencies [[midje "1.5.0"]]
                   :plugins [[lein-midje "3.0-alpha4"]
                             [lein-marginalia "0.7.1"]]  }}
  :min-lein-version "2.0.0"
  :main deduplicatr.core
  :jvm-opts ["-Xmx1024m" "-Xss100m"] 
)
