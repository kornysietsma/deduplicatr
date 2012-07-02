(ns deduplicatr.duplicates-test
  (:use midje.sweet
          deduplicatr.duplicates
          [clojure.java.io :only [file]])
  (:import (deduplicatr.file FileSummary DirSummary)))

#_(def fixtures (file "test" "fixtures"))
#_(def simple-fixture (file fixtures "simple"))

(def simplest-tree {
   :files {
      "foo.txt", (FileSummary. (file "tree" "foo.txt") 10000, 1)
   }
   :dirs {
    }
    :summary (DirSummary. (file "tree") 10000 1 1)
   })


(def simple-tree {
   :files {
      "foo.txt", (FileSummary. (file "tree" "foo.txt") 10000, 1)
      "bar.txt", (FileSummary. (file "tree" "bar.txt") 10000, 10)
   }
   :dirs {
      "empty_child", {
        :files {}
        :dirs {}
        :summary (DirSummary. (file "tree" "empty_child") 20000 0 0) 
      }
      "child", {
        :files { "foo.txt", (FileSummary. (file "tree" "child" "foo.txt") 10000, 1) }
        :dirs {}
        :summary (DirSummary. (file "tree" "child") 10000 1 1) 
      }
    }
    :summary (DirSummary. (file "tree") 12345 12 3)
      })

(facts "fstree-seq returns a seq of the summaries in a file system tree"
   (map :file (fstree-seq simplest-tree))
      => (just [(file "tree" "foo.txt")
		            (file "tree")] :in-any-order)
      
   (map :file (fstree-seq simple-tree))
      => (just [(file "tree" "empty_child")
		            (file "tree" "child" "foo.txt")
		            (file "tree" "child")
		            (file "tree" "foo.txt")
		            (file "tree" "bar.txt")
		            (file "tree")] :in-any-order))
            

#_(fact "duplicates finds all duplicates in a file system tree, in order from largest to smallest"
      (duplicates (deduplicatr.fstree/treeify simple-fixture))
       => {})