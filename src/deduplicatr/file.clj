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

(defprotocol Summary
  (file-count [this])
  (print-summary [this]))

;; ### FileSummary holds summary info about a file
;; * 'bytes' is the size of the file (can't use "size" as it's already
;; defined for records)
(defrecord FileSummary
    [^clojure.lang.Keyword group ^File file ^clojure.lang.BigInt hash ^long bytes]
  Summary
  (file-count [this]
    1)
  (print-summary [this]
    (.getPath (.file this))))

;; ### DirSummary holds summary info about a dir
;; * 'bytes' is the cumulative size
;; * filecount is the total number of files contained
(defrecord DirSummary
    [^clojure.lang.Keyword group ^File file ^clojure.lang.BigInt hash ^long bytes ^long filecount]
  Summary
  (file-count [this]
    (.filecount this))
  (print-summary [this]
     (str (.getPath (.file this)) "/ (" (.filecount this) " files)")))


(defn file-summary
   "Build a FileSummary for a physical file

* for small files, hash is the md5sum of the file size + the binary file contents
* for larger files, hash is the md5sum of the file size + the start of the file + the middle of the file + the end of the file"
   [group ^File file]
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
       (->FileSummary group file (digest-as-bigint md) size)
     )))

(defn empty-dir-summary
  "Starting FileSummary for a directory with no files"
  [group ^File file]
  (->DirSummary group file 0N 0 0))

(defn dir-summary
  "build a DirSummary for a physical directory by adding all child FileSummaries to an existing summary
   
   the directory hash is simply the sum of individual child hashes - we store hashes as positive BigInts so this works"
  [^DirSummary prevsummary & summaries] 
    (->DirSummary
      (.group prevsummary)
      (.file prevsummary)
      (apply + (.hash prevsummary) (map :hash summaries))
      (apply + (.bytes prevsummary) (map :bytes summaries))
      (apply + (.filecount prevsummary) (map file-count summaries))
    ))

