(ns deduplicatr.hash
  (:import (java.security MessageDigest)
            (java.nio ByteBuffer)
            (java.io File RandomAccessFile)
            (java.math BigInteger)
            (org.apache.commons.codec.binary Hex)))


 (defn md5
 	"return a md5 hash of a byte array"
 	[^bytes bytes]
 	(let [md (MessageDigest/getInstance "MD5")]
 		(.update md bytes)
 		(.digest md))
 	)

 (defn add-long-to-digest!
   "add a long to a digest"
   [^Long l ^MessageDigest message-digest]
   (let [buffer (byte-array 8)
         bbuffer (ByteBuffer/wrap buffer)]
     (.putLong bbuffer l)
     (.update message-digest buffer)
     message-digest))

(defn digest-of-long
  "make a digest containing just a long - mostly for testing"
  [l]
    (add-long-to-digest! l (MessageDigest/getInstance "MD5")))

(defn digest-as-hex [^MessageDigest digest] (Hex/encodeHexString (.digest digest)))

(defn digest-as-bigint [^MessageDigest digest] (BigInteger. 1 (.digest digest)))
