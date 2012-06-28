(ns deduplicatr.hash-test
  (:use midje.sweet
        deduplicatr.hash)
  (:import (org.apache.commons.codec.binary Hex)
           (java.security MessageDigest)
           (java.io File RandomAccessFile)
           (java.math BigInteger)
           (java.util Random)))

(fact "you can get the md5 of a string"
	(Hex/encodeHexString (md5 (.getBytes "foo"))) => "acbd18db4cc2f85cedef654fccc4a4d8")

(defn empty-digest [] (MessageDigest/getInstance "MD5"))

(fact "a digest has 16 bytes"
  (alength (.digest (empty-digest)))
  => 16)
; TODO: show that bigint digests might go over 16 bytes... or something

(fact "you can digest longs" ; ok, not a great test, but it'll do for now
  (digest-as-hex (digest-of-long 1234))
    => "7c847915ab7a67822819476fe9c0fc50"
  )

(def positive-digest (digest-of-long 1234))
(def negative-digest (digest-of-long 4444))

(fact "you can get a digest as a bigint"
  (.toString (digest-as-bigint positive-digest) 16)
    => "7c847915ab7a67822819476fe9c0fc50"
  )

(fact "you can get a digest with a negative binary representation as a bigint"
  (.toString (digest-as-bigint negative-digest ) 16)
    => "e10819768b1d4d0e7249b5c5243c4464"
  )

; TODO: not really happy with any of these, but not clear on how to
; get midje to test java code easily.  Think about this further...

(fact "digests are consistent"
  (digest-as-bigint (digest-of-long 2345))
    => (digest-as-bigint (digest-of-long 2345)))

(fact "digests are unique"
  (digest-as-bigint (digest-of-long 2345))
    =not=> (digest-as-bigint (digest-of-long 3456)))

(def random (Random.))

(def rand1 (digest-as-bigint (digest-of-long 1234)))
(def rand2 (digest-as-bigint (digest-of-long 4567)))
(def rand3 (digest-as-bigint (digest-of-long 6789)))

(fact "bigdecimal addition is transitive"
  (.add (.add rand1 rand2) rand3) => (.add (.add rand3 rand1) rand2))
