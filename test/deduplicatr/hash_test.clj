(ns deduplicatr.hash-test
  (:use midje.sweet
        deduplicatr.hash)
  (:import (org.apache.commons.codec.binary Hex)
           (java.security MessageDigest)
           (java.io File RandomAccessFile)
           (java.math BigInteger)
           (java.util Random)))

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

(fact "you can add multiple bigints"
      (add-bigints (BigInteger. "1") (BigInteger. "2") (BigInteger. "3") (BigInteger. "4"))
      => (BigInteger. "10"))

(def random (Random.))

; TODO: consider using midje quickcheck stuff (in "ideas" package") instead of simple loop
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
	(fact "addition of digests (as bigints) is commutative"
       (add-bigints dig1 dig2 dig3) => (add-bigints dig3 dig1 dig2))
  (fact "this test depends on unique random numbers, oops"
        rand1 =not=> rand2)
  (fact "this test depends on unique random numbers, oops"
        rand2 =not=> rand3)
))
 