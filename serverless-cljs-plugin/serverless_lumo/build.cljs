(ns serverless-lumo.build
  (:require fs path archiver
            [lumo.core]
            [clojure.string :as str]
            [goog.string.format]
            [cljs.reader :as reader]
            [lumo.io :as io]
            [lumo.build.api]
            [serverless-lumo.index :as index]))

(defn zip!
  "Create the zip in output-dir. Return a promise containing the path.

  The zip-opts map is:

     {:dirs #{\"dir1\" \"dir2\"}
      :files #{[\"path/to/file\" {:name \"file\"}]
               \"another/file\"}}

  Seqable :files entries are passed as parameters to the archiver.file
  function, for details check:
    https://archiverjs.com/docs/Archiver.html#file"
  [output-path zip-opts compiler-opts]

  (let [archiver      (archiver "zip" #js {:zlib {:level 9}})
        output-stream (.createWriteStream fs output-path)]
    (js/Promise.
     (fn [resolve-fn reject-fn]
       (.on output-stream "close" #(resolve-fn output-path))
       (.on archiver "warning" (fn [err]
                                 (if (= (.-code err) "ENOENT")
                                   (js/console.warn (.-message err))
                                   (reject-fn err))))
       (.on archiver "error" reject-fn)
       (.pipe archiver output-stream)
       (doseq [d (:dirs zip-opts)]
         (.directory archiver d))
       (doseq [f (:files zip-opts)]
         (if (string? f)
           (.file archiver f)
           (.apply (.-file archiver) archiver (clj->js (seq f)))))
       (.finalize archiver)))))

(defn compile!
  "Compile ClojureScript using the lumo api."
  [inputs compiler-opts]
  (js/console.info
   "Invoking the Lumo compiler w/ inputs"
   (.stringify js/JSON (clj->js inputs)))
  (lumo.build.api/build
   (apply lumo.build.api/inputs inputs)
   compiler-opts))

(defn generate-index
  "Generate the necessary index.js"
  [opts compiler]
  (index/generate-index (:functions opts) compiler))

(defn write-index [content outpath]
  (.writeFileSync fs outpath content)
  outpath)

(defn read-conf!
  "Read and return the configuration map."
  []
  (try
    (-> "serverless-lumo.edn" io/slurp reader/read-string)
    (catch js/Error e
      (when-not (= (.. e -code) "ENOENT")
        (throw e)))))

(def ^{:doc "The default build config."}
  default-config
  {:source-paths ["src"]
   :compiler     {:output-to     (.format path #js {:dir "out" :base "lambda.js"})
                  :output-dir    "out"
                  :source-map    false ;; lumo bug
                  :target        :nodejs
                  :optimizations :none}})

(defn- output-dir [compiler]
  (let [s (:output-dir compiler)]
    (str/replace s #"/+$" "")))

(defn build!
  "Build a project."
  [opts cljs-lambda-opts]
  (let [compiler (:compiler cljs-lambda-opts)
        index    (-> (generate-index opts compiler)
                     (write-index (.resolve path (:zip-path opts) "../index.js")))]
    (compile! (:source-paths cljs-lambda-opts) compiler)
    (zip! (:zip-path opts)
          {:dirs  #{(output-dir compiler) "node_modules"}
           :files #{[index {:name "index.js"}]}}
          compiler)))

(def cli-option-map
  {:z :zip-path
   :f :functions})

(defmulti  parse-option (fn [k v] k))
(defmethod parse-option :default   [k v] v)
(defmethod parse-option :functions [k v] (reader/read-string v))

(defn cli-options "Compute the option map"
  [argv]
  (into {}
    (for [[k v] (partition 2 argv)
          :let [k (keyword (str/replace k #"^-{1,2}" ""))
                k (cli-option-map k k)]]
      [k (parse-option k v)])))

(defn cmd-line-args []
  (if-let [args cljs.core/*command-line-args*] ;; for lumo > 1.7.0
    args
    (when-let [args lumo.core/*command-line-args*] ;; for lumo <= 1.7.0
      args)))

(defn ^:export -main [& args]
  (let [opts (cli-options (cmd-line-args))
        conf (read-conf!)]
    (build! opts (merge-with merge default-config (read-conf!)))))

(set! *main-cli-fn* -main)
