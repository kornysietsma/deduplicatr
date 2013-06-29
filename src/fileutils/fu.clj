(ns fileutils.fu
  (:import [java.nio ByteBuffer]
           [java.nio.file Path Paths Files OpenOption StandardOpenOption LinkOption]
           [java.nio.channels SeekableByteChannel FileChannel]
           [java.nio.file.attribute FileAttribute]))

(defn java-version []
  (let [[maj min sub] (clojure.string/split (System/getProperty "java.version") #"\.")]
    {:major (Integer/parseInt maj) :minor (Integer/parseInt min) :sub sub}))

(defn at-least-java-7 []
  (let [{:keys [major minor]} (java-version)]
    (and (= major 1) (>= minor 7))))

(defn path
  ^Path [first & paths]
  (Paths/get first (into-array String paths)))

(defn rel-path
  ^Path [^Path first & paths]
  (.resolve first (apply path paths)))

(defn ^Path relative-to [^Path basepath ^Path path]
  (.relativize basepath path))

(defn size
  [^Path path]
  (Files/size path))

(defn- to-standard-open-option [keyword]
  (case keyword
    :append StandardOpenOption/APPEND
    :create StandardOpenOption/CREATE
    :create_new StandardOpenOption/CREATE_NEW
    :delete_on_close StandardOpenOption/DELETE_ON_CLOSE
    :dsync StandardOpenOption/DSYNC
    :read StandardOpenOption/READ
    :sparse StandardOpenOption/SPARSE
    :sync StandardOpenOption/SYNC
    :truncate_existing StandardOpenOption/TRUNCATE_EXISTING
    :write StandardOpenOption/WRITE
    (throw (Exception. (str "no such option:" keyword)))))

(defn open-option-array
  "converts keyword-style option options such as :read :append :sparse etc. to array of OpenOption"
  [& options]
  (into-array OpenOption (map to-standard-open-option options)))

(defn open-option-set
  "converts keyword-style open options such as :read :append etc into a set"
  [& options]
  (into #{} (map to-standard-open-option options)))

(def empty-file-attributes
  (into-array FileAttribute []))

(defn ro-file-channel
  ^FileChannel [^Path path]
  (Files/newByteChannel path (open-option-set :read) empty-file-attributes))

(defn read-bytes ^bytes [^FileChannel channel offset bytes]
  (let [buf (ByteBuffer/allocate bytes)
        bytes-read (.read channel buf offset)]
    (if (= bytes-read bytes)
      (.array buf)
      (throw (Exception. (str "failed to read " bytes " returned value:" bytes-read))))))

(defn file-name [^Path path]
  (-> path .getFileName .toString))

(defn get-path
  "equivalent of File/getPath - rename this once refactor done"
  [^Path path]
  (-> path .toString))

(def use-path true)

(defn children [^Path path]
  (if use-path
    (with-open [ds (Files/newDirectoryStream path)]
      (doall (seq ds))))
  (doall (map #(.toPath %) (.listFiles (.toFile path)))))

(defn parent ^Path [^Path path]
  (.getParent path))

(def no-links (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))

(defn is-real-file [^Path path]
  (Files/isRegularFile path no-links))

(defn is-real-dir [^Path path]
  (Files/isDirectory path no-links))

(defn is-symlink [^Path path]
  (Files/isSymbolicLink path))

(defn temp-file-with-data
  "create a temp file with specific byte data - mostly for tests"
  [bytes]
  (let [f (Files/createTempFile "fu" ".test" (into-array FileAttribute []))
            data (byte-array (map byte bytes))
        _ (Files/write f data (open-option-array :create))]
    f))
