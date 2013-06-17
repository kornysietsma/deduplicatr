# deduplicatr

A Clojure application designed to find duplicates in a large set of files

This is a work in progress - it is currently working, but very much in a beta state.

Note that as of the latest update, you need Java 7 to build and run this, as it uses new language features - most significantly, the ability to ignore symlinks!

## Usage

Currently this needs Java 7, and leningen 2.0+ to run - you can use "lein uberjar" to produce a standalone runnable jar, for running without clojure/leiningen.

Usage at this stage is fairly simple:

$ lein run [directories]

$ java -jar deduplicatr.jar [directories]

This will scan all files in the directory, then print out all duplicate directories / files, largest first.  Pipe the results through "less" or similar if you get a lot of output

For help/options you need to specify '--' to skip leiningen option processing:

$ lein run -- -h

$ java -jar deduplicatr.jar -h

You can ignore files and directories with "-i" and a comma-separated list of names - note this is only exact names at this stage (and you can't ignore a name with a comma in it!).  By default the MacOS metadata file ".DS_Store" is ignored, but you can specify your own list:

$ lein run -- -i ".git,.svn,.DS_Store" foo/bar

$ java -jar deduplicatr.jar -i ".git,.svn,.DS_Store" foo/bar

## Documentation
Introductory documentation is in [the wiki on github](https://github.com/kornysietsma/deduplicatr/wiki).
Finally the [tests](https://github.com/kornysietsma/deduplicatr/tree/master/test/deduplicatr) are also a great way to understand what is going on - this project is mostly test-driven.

## Thanks
Lots of thanks to Hank at the Melbourne clojure group for prompting some radical refactorings

## License

Copyright © 2012 Kornelis Sietsma

Distributed under the Eclipse Public License, the same as Clojure.

The use and distribution terms for this software are covered by the Eclipse Public License 1.0, which can be found in the file epl-v10.html at the root of this distribution. By using this software in any fashion, you are agreeing to be bound by the terms of this license. You must not remove this notice, or any other, from this software.
