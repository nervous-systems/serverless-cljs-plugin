(ns serverless-lumo.build
  (:require fs path archiver
            [clojure.string :as str]
            [goog.string.format]
            [cljs.reader :as reader]
            [lumo.io :as io]
            [lumo.build.api]
            [serverless-lumo.index :as index]))

(defn zip!
  "Create the zip in output-dir. Return a promise containing the path.

  The dirs will be included in the zip, the compression level defaults
  to 9."
  [output-path zip-opts compiler-opts]

  (let [archiver      (archiver "zip" #js {:zlib {:level 9}})
        output-stream (.createWriteStream fs output-path)]
    (js/Promise.
     (fn [resolve-fn reject-fn]
       (.on output-stream "close" #(resolve-fn output-path))
       (.on archiver "error" reject-fn)

       (.pipe archiver output-stream)
       (doseq [d (zip-opts :dirs)]
         (.directory archiver d))
       (.file archiver (zip-opts :index) #js {:name "index.js"})
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
  (index/generate-index (opts :functions) compiler))

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
  (let [s (compiler :output-dir)]
    (str/replace s #"/+$" "")))

(defn build!
  "Build a project."
  [opts cljs-lambda-opts]
  (let [compiler (cljs-lambda-opts :compiler)
        index    (-> (generate-index opts compiler)
                     (write-index (.resolve path (opts :zip-path) "../index.js")))]
    (compile! (cljs-lambda-opts :source-paths) compiler)
    (zip! (opts :zip-path)
          {:dirs  #{(output-dir compiler) "node_modules"}
           :index index}
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

(defn ^:export -main [& args]
  (let [opts (cli-options *command-line-args*)
        conf (read-conf!)]
    (build! opts (merge-with merge default-config (read-conf!)))))

(set! *main-cli-fn* -main)
