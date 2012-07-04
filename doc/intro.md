# Introduction to deduplicatr

Deduplicatr is a Clojure application designed to find duplicates in a large set of files

This is a work in progress - it is currently working, but very much in a beta state.

## Usage

Currently this needs leningen 2.0+ to run - you can use "lein uberjar" to produce a standalone runnable jar, but I haven't published this anywhere until the app is stabler.

Usage at this stage is simple:

$ lein run [base directory]

This will scan all files in the directory, then print out all duplicate directories / files, largest first.  Pipe the results through "less" or similar if you get a lot of output

