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
   (^bytes [^RandomAccessFile filehandle offset] (chunk-of-file filehandle hash-chunk-size offset))
   (^bytes [^RandomAccessFile filehandle size offset]
     (let [buffer (byte-array size)]
            ; bizarrely, the 'offset' in .read is the _destination_ offset! so need to seek:
            (.seek filehandle offset)
            (.read filehandle buffer 0 size)
            buffer)))

;summary info about a file or a directory - is-dir is used for speed, filecount ignored for non-dirs
; can't use "size" as defrecord defines this!
(defrecord FileSummary
  [^File file ^BigInteger hash ^long bytes ^boolean is-dir ^long filecount])

(defn make-file-summary
  [^File file ^BigInteger hash ^long bytes]
  (FileSummary. file hash bytes false 1))

(defn make-dir-summary
  [^File file ^BigInteger hash ^long bytes ^long filecount]
    (FileSummary. file hash bytes true filecount))

;TODO: consider renaming this, as it actually returns a FileSummary?
(defn file-summary
   "hash of a file - size, plus, for small files, whole file, for big files, partial hash. Also returns file size for use for stat accumulation"
   [^File file]
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
       (make-file-summary file (digest-as-bigint md) size)
     )))

(defn dir-summary
  "accumulated summary of multiple files"
  ; construct a dirSummary from a directory - no files yet
  ([^File file]
    (make-dir-summary file (BigInteger/ZERO) 0 0))
  ; construct a dirSummary from a partial summary and a new file
  ([^FileSummary prevsummary filename ^BigInteger hash size]
    (make-dir-summary
      (.file prevsummary)
      (.add (.hash prevsummary) hash)
      (+ (.bytes prevsummary) size)
      (inc (.filecount prevsummary))
    )))
; TODO: fix the above using 'into'?
; TODO: remove filename, we don't use it any more