(ns fu.t-core
  (:require
   [midje.sweet :refer :all]
   [fu.core :as fu])
  (:import [java.nio.file Path Paths Files OpenOption StandardOpenOption]
           [java.nio.channels FileChannel]
           [java.nio.file.attribute FileAttribute]
           [org.apache.commons.codec.binary Hex]))

(defn hex [bytes] (Hex/encodeHexString bytes))

(fact "java version must be at least 7"
      (fu/java-version) => (just {:major 1 :minor (partial < 6) :sub anything})
      (fu/at-least-java-7) => true)

(fact "can create a path"
      (.toString (fu/path "src")) => "src"
      (.toString (fu/path "foo" "bar")) => (str "foo" (java.io.File/separator) "bar"))

(fact "can create an array of StandardOpenOptions"
      (.getName (class (fu/open-option-array :append))) => "[Ljava.nio.file.OpenOption;"
      (seq (fu/open-option-array :append :create)) => (just [StandardOpenOption/APPEND
                                                             StandardOpenOption/CREATE]))

(fact "can create a set of StandardOpenOptions"
      (fu/open-option-set :append :create) => #{ StandardOpenOption/APPEND
                                                 StandardOpenOption/CREATE})

(defn file-with-data [bytes]
  (let [f (Files/createTempFile "fu" ".test" (into-array FileAttribute []))
            data (byte-array (map byte bytes))
        _ (Files/write f data (fu/open-option-array :create))]
    f))

(fact "can find path size"
      (fu/size (file-with-data [0 1 2 3])) => 4)

(fact "can get a byte channel and read bytes from it"
      (let [f (file-with-data [0 1 2 3 4 5 6 7 8 9])]
        (with-open [bc (fu/ro-file-channel f)]
          (hex (fu/read-bytes bc 0 4)) => "00010203"
          (hex (fu/read-bytes bc 8 2)) => "0809")))

(fact "can get the file name of a path"
      (fu/file-name (fu/path "foo" "bar.txt")) => "bar.txt")

(fact "can list all children of a dir"
      (map fu/file-name  (fu/children (fu/path "test" "fixtures" "simple")))
      => (just "ab" "ab_split" "parent"))
