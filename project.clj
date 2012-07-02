(defproject deduplicatr "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories {"stuart" "http://stuartsierra.com/maven2"}
  :dependencies {org.clojure/clojure "1.4.0",
                 commons-codec/commons-codec "1.4"
                 org.clojure/tools.cli "0.2.1"}
  :profiles {:dev {:dependencies {midje "1.4.0",
                                  lein-midje "2.0.0-SNAPSHOT"
                                  com.stuartsierra/lazytest "1.2.3"
                                 }}}
  :min-lein-version "2.0.0"
  :main deduplicatr.core
  :jvm-opts ["-Xss1500m"] 
)
