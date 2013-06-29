# deduplicatr

A Clojure application designed to find duplicates in a large set of files

Note that as of the latest update, you need Java 7 to build and run this, as it uses new language features - most significantly, the ability to ignore symlinks!

## Usage

Currently this needs Java 7, and leningen 2.0+ to run - you can use `"lein uberjar"` to produce a standalone runnable jar, for running without clojure/leiningen.

Generally it's fastest to generate an uberjar and then run the commands with:

`$ java -jar deduplicatr.jar [parameters]`

In dev mode you can use "lein run" to run it while playing with changes; the command is then

`$ lein run [parameters]`

If you want to pass extra options you need a "--" to tell leiningen to pass commands to the app:

`$ lein run -- [parameters]`

### Standard mode - duplicate finding

`$ java -jar deduplicatr.jar [directories]`

This will scan all files in the directory, then print out all duplicate directories / files, largest first.  Pipe the results through "less" or similar if you get a lot of output.

So for example if you point this at three big directories "foo", "bar" and "baz", you might get output like:

```
duplicates:
2 matches of 4,125,793,965 bytes
   a: 2008_ebooks/gutenberg/ (13645 files)
   b: 2009_ebooks_sorting/old_ebooks/gutenberg/ (13645 files)
2 matches of 14,117,470 bytes
   b: calibre/Michael Fogus_ Chris Houser/The Joy of Clojure (1607)/The Joy of Clojure - Michael Fogus_ Chris Houser.pdf
   c: tech/manning/clojure/TheJoyofClojure.pdf
```

This indicates that "foo/" and "bar/" both have a project gutenberg dump from old backups, and that "bar/" and "baz/" both have a copy of the Joy of Clojure (a great book!)

### Diff mode - finding differences between two directories

`$ java -jar deduplicatr.jar --diff [directory a] [directory b]`

This will scan two similar directories, and output:

- all dirs/files in both directories
- all dirs/files only in directory "a"
- all dirs/files only in directory "b"

Note these lists are pruned so only the topmost relevant directory is reported.
So given the following structures:

```
dir_a/
  in_a/
    a.txt
  in_both/
    foo.txt

dir_b/
  in_b/
    b.txt
  in_both/
    foo.txt
```

The output will be something like:

```
in both:
  a: in_both
  b: in_both
in dir_a:
  a: in_a
in dir_b:
  b: in_b
```

and the individual files "a.txt", "foo.txt" etc. are not listed.

### Other options

#### Ignoring files

You can ignore files and directories with "-i" and a comma-separated list of names - note this is only exact names at this stage (and you can't ignore a name with a comma in it!).  By default the MacOS metadata file ".DS_Store" is ignored, but you can specify your own list:

`$ java -jar deduplicatr.jar -i ".git,.svn,.DS_Store" foo/bar`

#### Sorting by file count

By default, deduplicatr sorts big directories first.

If you specify "--by-files" it will instead sort by file counts - so big directories will be listed first, even if they count for fewer total bytes than a few large files elsewhere.

Note this won't work currently for diff mode - which doesn't clump groups of files together anyway.

#### Help

For command line help run

`$ java -jar deduplicatr.jar -h`

## Documentation
Out of date documentation is in [the wiki on github](https://github.com/kornysietsma/deduplicatr/wiki) - what you are reading is probably more useful right now

The [tests](https://github.com/kornysietsma/deduplicatr/tree/master/test/deduplicatr) are also a great way to understand what is going on - this project is mostly test-driven.

## Thanks
Lots of thanks to Hank at the Melbourne clojure group for prompting some radical refactorings

## License

Copyright © 2012, 2013 Kornelis Sietsma

Distributed under the Eclipse Public License, the same as Clojure.

The use and distribution terms for this software are covered by the Eclipse Public License 1.0, which can be found in the file epl-v10.html at the root of this distribution. By using this software in any fashion, you are agreeing to be bound by the terms of this license. You must not remove this notice, or any other, from this software.
