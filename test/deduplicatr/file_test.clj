(ns deduplicatr.file-test
  (:use midje.sweet
        deduplicatr.file
        [deduplicatr.hash :only [digest-of-long add-long-to-digest! digest-as-bigint]]
        [clojure.java.io :only [file]])
  (:import (org.apache.commons.codec.binary Hex)
           (java.security MessageDigest)
           (java.io File RandomAccessFile)
           (deduplicatr.file FileSummary DirSummary)))

(def testbytes (byte-array (map byte [0 1 2 3 4 5 6 7 8 9 10 11])))
(def testbytes-partial (byte-array (map byte [0 1 2 4 5 6 9 10 11])))

(fact "relative paths can be determined"
  (relative-path (file "base") (file "base" "child" "grandchild")) => "child/grandchild")
(fact "relative paths do nothing if child is not under base"
  (relative-path (file "foo") (file "bar" "baz")) => "bar/baz")

(against-background
  [(around :contents
           (let [tempfile (File/createTempFile "deduplicatr_test" ".tmp")
                 filename (.getCanonicalPath tempfile)
                 empty-tempfile (File/createTempFile "deduplicatr_test" ".tmp")
                 empty-tempfile-name (.getCanonicalPath empty-tempfile)
                 ]
             (.deleteOnExit tempfile)
             (.deleteOnExit empty-tempfile)
             (with-open [raf (RandomAccessFile. filename "rw")]
               (.write raf testbytes)
               (.seek raf 0)
               ?form
               )))]
  (fact "can read a chunk from the start of a file"
    (Hex/encodeHexString (chunk-of-file raf 5 0))
        => "0001020304")
  (fact "can read a chunk from the end of a file"
    (Hex/encodeHexString (chunk-of-file raf 5 7))
        => "0708090a0b")
  (fact "can read a chunk which is the whole file"
    (Hex/encodeHexString (chunk-of-file raf 12 0))
        => "000102030405060708090a0b")
  (fact "hash of a small file is the digest of the size and the contents"
        (file-summary :group tempfile)
        => (let [d (MessageDigest/getInstance "MD5")]
             (add-long-to-digest! 12 d)
             (.update d testbytes)
             (->FileSummary :group tempfile (digest-as-bigint d) 12)))
  (fact "hash of a file bigger than thrice the chunk size is partial"
        (binding [hash-chunk-size 3]
        (file-summary :group tempfile)
        => (let [d (MessageDigest/getInstance "MD5")]
             (add-long-to-digest! 12 d)
             (.update d testbytes-partial)
             (->FileSummary :group tempfile (digest-as-bigint d) 12))))
  (fact "hash of a zero byte file should be the same as hash of 0"
        (file-summary :group empty-tempfile)
        => (let [d (MessageDigest/getInstance "MD5")]
             (add-long-to-digest! 0 d)
             (->FileSummary :group empty-tempfile (digest-as-bigint d) 0))))

(fact "dir-summary has no hash nor size nor files"
  (empty-dir-summary :group (file "foo"))
  => (->DirSummary
       :group
       (file "foo")
       0N
       0
       0))

(fact "dir-summary adds file hash, count and size to the initial summary"
  (dir-summary (empty-dir-summary :group (file "foo")) (->FileSummary :group (file "ignored") 1N 123))
  => (->DirSummary
       :group
       (file "foo")
       1N
       123
       1))

(fact "dir-summary can add multiple summaries at once"
  (dir-summary (empty-dir-summary :group (file "foo")) 
               (->FileSummary :group (file "ignored") 1N 111)
               (->FileSummary :group (file "ignored") 2N 222)
               (->FileSummary :group (file "ignored") 3N 333))
  => (->DirSummary
       :group
       (file "foo")
       6N
       666
       3))

(def hash1 (digest-as-bigint (digest-of-long 1234)))
(def hash2 (digest-as-bigint (digest-of-long 4567)))

(fact "dir-summary accumulates hashes the same in any order"
  (dir-summary (empty-dir-summary :group (file "foo"))
               (->FileSummary :group (file "ignored") hash1 123)
               (->FileSummary :group (file "ignored") hash2 456))
  => (dir-summary (empty-dir-summary :group (file "foo"))
                  (->FileSummary :group (file "ignored") hash2 456)
                  (->FileSummary :group (file "ignored") hash1 123)))
