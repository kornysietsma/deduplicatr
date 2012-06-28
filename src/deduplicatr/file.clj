(ns deduplicatr.file
  (:use deduplicatr.hash)
  (:import (java.security MessageDigest)
           (java.nio ByteBuffer)
           (java.io File RandomAccessFile)
           (org.apache.commons.codec.binary Hex)))

 ; note mostly dynamic for testing, but it might be useful in some circumstances anyway:
(def ^:dynamic hash-chunk-size "size of each part of a file to read for hash info" 1024)

(defn chunk-of-file
   "return an array of bytes from a binary file
   note - no error handling, assumes there are [size] bytes available at [offset]"
   ([filehandle offset] (chunk-of-file filehandle hash-chunk-size offset))
   ([filehandle size offset]
     (let [buffer (byte-array size)]
            ; bizarrely, the 'offset' in .read is the _destination_ offset! so need to seek:
            (.seek filehandle offset)
            (.read filehandle buffer 0 size)
            buffer)))

(defrecord FileSummary [file hash bytes])  ; can't use "size" as defrecord defines this!
(defrecord DirSummary [file hash bytes filecount])

(defn file-hash
   "hash of a file - size, plus, for small files, whole file, for big files, partial hash. Also returns file size for use for stat accumulation"
   [file]
   (let [md (MessageDigest/getInstance "MD5")
         size (.length file)
         ]
     (add-long-to-digest! size md)
     (with-open [raf (RandomAccessFile. file "r")]
       (if (> size (* hash-chunk-size 3))
         (do
           (.update md (chunk-of-file raf 0))
           (.update md (chunk-of-file raf  (- (/ size 2) (/ hash-chunk-size 2))))
           (.update md (chunk-of-file raf (- size hash-chunk-size))))
         (.update md (chunk-of-file raf size 0))
       )
       (FileSummary. file (digest-as-bigint md) size)
     )))

(defn dir-summary
  "accumulated hash of multiple files"
  ([file] (DirSummary. file (BigInteger/ZERO) 0 0))
  ([prevsummary filename hash size]
    (DirSummary.
      (.file prevsummary)
      (.add (.hash prevsummary) hash)
      (+ (.bytes prevsummary) size)
      (inc (.filecount prevsummary))
    )))
; TODO: fix the above using 'into'