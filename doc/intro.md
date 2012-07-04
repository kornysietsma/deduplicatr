# Introduction to deduplicatr

Deduplicatr is a Clojure application designed to find duplicates in a large set of files

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

## History
This started as a script I wrote in python (a long time ago!) which sort-of worked, but was slow and messy.

I re-wrote it in ruby, but it still had issues; overly complex code, lots of ugly tree state manipulation and complex comparison operations.

The third incarnation, in clojure, is a lot simpler and cleaner.  Using an order-independant hash (actually just the unsigned bigint sum of file hashes!) makes directory comparison vastly faster and simpler than the old code.
