# deduplicatr

A Clojure application designed to find duplicates in a large set of files

This is a work in progress - it is currently working, but very much in a beta state.

## Usage

Currently this needs leningen 2.0+ to run - you can use "lein uberjar" to produce a standalone runnable jar, for running without clojure/leiningen.

Usage at this stage is simple:

$ lein run [base directory]

This will scan all files in the directory, then print out all duplicate directories / files, largest first.  Pipe the results through "less" or similar if you get a lot of output

## Documentation
Introductory documentation is in [the wiki on github](https://github.com/kornysietsma/deduplicatr/wiki).
There is also some code annotated with marginalia [here](http://cloud.github.com/downloads/kornysietsma/deduplicatr/uberdoc.html)
Finally the [tests](https://github.com/kornysietsma/deduplicatr/tree/master/test/deduplicatr) are also a great way to understand what is going on - this project is mostly test-driven.

## License

Copyright © 2012 Kornelis Sietsma

Distributed under the Eclipse Public License, the same as Clojure.

The use and distribution terms for this software are covered by the Eclipse Public License 1.0, which can be found in the file epl-v10.html at the root of this distribution. By using this software in any fashion, you are agreeing to be bound by the terms of this license. You must not remove this notice, or any other, from this software.
