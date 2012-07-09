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

(fact "all digests are represented as positive bigints"
  (digest-as-bigint negative-digest )
    => #(> % 0)
  (digest-as-bigint positive-digest )
    => #(> % 0)
)

(def random (Random.))

(dotimes [n 10]
  (let [rand1 (.nextLong random)
        rand2 (.nextLong random)
        rand3 (.nextLong random)
        dig1 (digest-as-bigint (digest-of-long rand1))
        dig2 (digest-as-bigint (digest-of-long rand2))
        dig3 (digest-as-bigint (digest-of-long rand3))]
  (fact "digests are consistent"
        (digest-as-bigint (digest-of-long rand1))
        => dig1)
  (fact "digests are unique"
        dig1 =not=> dig2)
	(fact "addition of digests (as bigints) is transitive"
       (.add (.add dig1 dig2) dig3) => (.add (.add dig3 dig1) dig2))
  (fact "this test depends on unique random numbers, oops"
        rand1 =not=> rand2)
  (fact "this test depends on unique random numbers, oops"
        rand2 =not=> rand3)
))
 