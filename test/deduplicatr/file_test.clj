(ns deduplicatr.file-test
  (:use midje.sweet
        deduplicatr.file
        [deduplicatr.hash :only [digest-of-long add-long-to-digest! digest-as-bigint]]
        [deduplicatr.hash-test :only [empty-digest]]
        [clojure.java.io :only [file]])
  (:import (org.apache.commons.codec.binary Hex)
           (java.security MessageDigest)
           (java.io File RandomAccessFile)
           (deduplicatr.file FileSummary)))

(def testbytes (byte-array (map byte [0 1 2 3 4 5 6 7 8 9 10 11])))
(def testbytes-partial (byte-array (map byte [0 1 2 4 5 6 9 10 11])))

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
        (file-hash tempfile)
        => (let [d (MessageDigest/getInstance "MD5")]
             (add-long-to-digest! 12 d)
             (.update d testbytes)
             (make-file-summary tempfile (digest-as-bigint d) 12)))
  (fact "hash of a file bigger than thrice the chunk size is partial"
        (binding [hash-chunk-size 3]
        (file-hash tempfile)
        => (let [d (MessageDigest/getInstance "MD5")]
             (add-long-to-digest! 12 d)
             (.update d testbytes-partial)
             (make-file-summary tempfile (digest-as-bigint d) 12))))
  (fact "hash of a zero byte file should be the same as hash of 0"
        (file-hash empty-tempfile)
        => (let [d (MessageDigest/getInstance "MD5")]
             (add-long-to-digest! 0 d)
             (make-file-summary empty-tempfile (digest-as-bigint d) 0))))

(fact "dir-summary accumulates total file count and size"
  (dir-summary (dir-summary (file "foo")) "ignored" (BigInteger/ONE) 123)
  => (make-dir-summary
       (file "foo")
       (BigInteger/ONE)
       123
       1))

(def hash1 (digest-as-bigint (digest-of-long 1234)))
(def hash2 (digest-as-bigint (digest-of-long 4567)))

(fact "dir-summary accumulates hashes the same in any order"
  (dir-summary
    (dir-summary (dir-summary (file "foo")) "ignored" hash1 123)
    "ignored two"
    hash2
    456)
  => (dir-summary
       (dir-summary (dir-summary (file "foo")) "ignored two" hash2 456)
      "ignored"
       hash1
       123))
