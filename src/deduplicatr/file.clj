;; ## File reading and hashing logic

(ns deduplicatr.file
  (:use deduplicatr.hash)
  (:import (java.security MessageDigest)
           (java.nio ByteBuffer)
           (java.io File RandomAccessFile)
           (org.apache.commons.codec.binary Hex)))

(def ^:dynamic hash-chunk-size
  "File hashes for large files are based on reading this many bytes from the start, middle, and end of the file
   (this is dynamic primarily for testing)"
  ; TODO: consider making this not dynamic again... what is the speed impact here?
  1024)

(defn chunk-of-file
   "read part of a binary file as a byte array

   note - no error handling, assumes there are [size] bytes available at [offset]"
   (^bytes [^RandomAccessFile filehandle offset] (chunk-of-file filehandle hash-chunk-size offset))
   (^bytes [^RandomAccessFile filehandle size offset]
     (let [buffer (byte-array size)]
            ; bizarrely, the 'offset' in .read is the _destination_ offset! so need to seek:
            (.seek filehandle offset)
            (.read filehandle buffer 0 size)
            buffer)))

;; ### FileSummary holds summary info about a file or a directory
;; * 'bytes' is the size of the file (can't use "size" as it's already defined for records)
;; * 'is-dir' is true for a directory, false for a file
;; * filecount is the number of files contained, 1 for a file (and generally ignored)
(defrecord FileSummary
  [^File file ^BigInteger hash ^long bytes ^boolean is-dir ^long filecount])

(defn make-file-summary
  "construct a FileSummary record manually for a file"
  [^File file ^BigInteger hash ^long bytes]
  (FileSummary. file hash bytes false 1))

(defn make-dir-summary
  "construct a FileSummary record manually for a directory"
  [^File file ^BigInteger hash ^long bytes ^long filecount]
    (FileSummary. file hash bytes true filecount))

(defn file-summary
   "Build a FileSummary for a physical file

* for small files, hash is the md5sum of the file size + the binary file contents
* for larger files, hash is the md5sum of the file size + the start of the file + the middle of the file + the end of the file"
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
  "build a FileSummary for a physical directory

   the single-argument version builds a summary for an empty directory

   the multi-argument version builds a summary by adding a child FileSummary to an existing summary
   - you construct directory summaries by repeatedly adding children to an empty directory summary.
   
   the directory hash is simply the sum of individual child hashes - we store hashes as positive BigIntegers so this works"
  ([^File file]
    (make-dir-summary file (BigInteger/ZERO) 0 0))
  ([^FileSummary prevsummary & summaries]  ; TODO: can we type hint the summaries? probably doesn't matter as we use :keyword
    (make-dir-summary
      (.file prevsummary)
      (apply add-bigints (.hash prevsummary) (map :hash summaries))
      (apply + (.bytes prevsummary) (map :bytes summaries))
      (apply + (.filecount prevsummary) (map :filecount summaries))
    ))
  )

;; TODO: improve the above using 'into'?  Consider a shortcut to make a dirsummary from multiple files at once?

