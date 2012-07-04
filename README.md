# deduplicatr

A Clojure application designed to find duplicates in a large set of files

This is a work in progress - it is currently working, but very much in a beta state.

## Usage

Currently this needs leningen 2.0+ to run - you can use "lein uberjar" to produce a standalone runnable jar, but I haven't published this anywhere until the app is stabler.

Usage at this stage is simple:

$ lein run [base directory]

This will scan all files in the directory, then print out all duplicate directories / files, largest first.  Pipe the results through "less" or similar if you get a lot of output

## Limitations
* doesn't currently know to ignore symlinks - this is a Java restriction!  Java 7 has code to recognise symlinks, earlier Java versions require hacky slow workarounds.  (I'm investigating options here still)
* only does one thing so far - this is my MVP, more features will come in time!
* I'm still pretty new to clojure, the code might need a bunch of cleaning and gardening!

## Testing
This is mostly test-driven code - test with midje:
$ lein midje

or with the lazytest plugin installed:
$ lein midje --lazytest

## License

Copyright © 2012 Kornelis Sietsma

Distributed under the Eclipse Public License, the same as Clojure.

The use and distribution terms for this software are covered by the Eclipse Public License 1.0, which can be found in the file epl-v10.html at the root of this distribution. By using this software in any fashion, you are agreeing to be bound by the terms of this license. You must not remove this notice, or any other, from this software.
