(ns lumo.build
  (:require os fs path archiver
            [posix-getopt :as go]
            [clojure.string :as str]
            [goog.string :as gstr]
            [goog.string.format]
            [goog.object :as gobj]
            [cljs.reader :as reader]
            [lumo.io :as io]
            [lumo.build.api]))

(defn zip!
  "Create the zip in output-dir. Return a promise containing the path.

  The dirs will be included in the zip, the compression level defaults
  to 9."
  [output-path zip-opts compiler-opts]

  (let [{:keys [compression-level dirs files]
         :or {compression-level 9}} zip-opts
        archiver (archiver "zip" #js {:zlib {:level compression-level}})
        output-stream (.createWriteStream fs output-path)]
    (js/Promise.
     (fn [resolve-fn reject-fn]
       (.on output-stream "close"
            (fn []
              (.info js/console "Zip created in %s is %s bytes" output-path (.pointer archiver))
              (resolve-fn output-path)))

       (.on archiver "warning"
            (fn [err]
              (if (= (.-code err) "ENOENT")
                (.warn js/console (.-message err))
                (reject-fn err))))

       (.on archiver "error"
            (fn [err]
              (.error js/console (.-message err))
              (reject-fn err)))
       ;; start the piping of the following
       (.pipe archiver output-stream)
       ;; adding dirs
       (doseq [d dirs]
         (.directory archiver d))
       ;; adding files
       (doseq [f files]
         (.file archiver f))
       ;; finalize
       (.finalize archiver)))))

(defn compile!
  "Compile ClojureScript using the lumo api."
  [inputs compiler-opts]
  (do
    (.info js/console (gstr/format "Invoking the Lumo compiler w/ inputs %s" (.stringify js/JSON (clj->js inputs))))
    (lumo.build.api/build
     (apply lumo.build.api/inputs inputs compiler-opts)
     compiler-opts)))

(defn generate-index!
  "Generate the necessary index.js and copy it to target-path, ready for zipping."
  [target-path functions compiler-options]
  ;; TODO
  )

(defn read-conf!
  "Read and return the configuration map."
  []
  ;; TODO read from serverless.yml
  ;; TODO read from project.clj as safety net?
  (-> "serverless-lumo.edn"
      io/slurp
      reader/read-string))

(def target-path-default "out")

(def ^{:doc "The default compiler option map."}
  compiler-defaults
  {:output-to (.format path #js {:dir target-path-default :base "lambda.js"})
   :output-dir target-path-default
   :target :nodejs
   :optimizations :none})

(defn build!
  "Build a project.

  The cljs-lambda-opts map is:

    {:project {:source-paths [\"src\"]
               :target-path \"target\"}
     :compiler-options {:output-to \"anything\"  ;; will be overridden
                        :language-in   :ecmascript5
                        :optimizations :simple
                        :main \"my-artifact.core\"}}"
  [zip-path functions cljs-lambda-opts]
  (let [{:keys [target-path source-paths]
         :or {target-path target-path-default}}
        (:project cljs-lambda-opts)

        compiler-options (:compiler-options cljs-lambda-opts)]

    ;; improve this with clojure.spec
    ;; (assert (sequential? functions) (gstr/format "Functions should be a sequence, got %s, exiting." functions))
    (assert (map? compiler-options) (gstr/format "Compiler options should be a map, got %s. Exiting." compiler-options))
    (assert (seqable? source-paths) (gstr/format "Inputs should be a sequence, got %s. Exiting." source-paths))

    (let [compiler-options (merge compiler-defaults compiler-options)
          ;; TODO generate an index.js suitable for AWS Lambda
          ;; index-path (generate-index! target-path options functions))
          ]

      ;; compiling
      (compile! source-paths compiler-options)

      ;; the only async step is this
      (zip! zip-path
            {:dirs #{target-path "node_modules"}
             ;; :files index-path
             }
            compiler-options))))

(comment
  (def cljs-lambda-opts {:project {:source-paths ["src"]
                                   :target-path "target"}
                         :compiler-options {:language-in   :ecmascript5
                                            :optimizations :none
                                            :source-map    false}}))

(def cli-option-map
  {:z :zip-path
   :f :functions})

(defn cli-getopt-string
  "Convert a clojure map to a getopt-compatible option string."
  [cli-map]
  (->> cli-map
       (map #(str (-> %1 first name) ":(" (-> %1 second name) ")"))
       str/join))

(defn stringify-symbol
  [x]
  (if (symbol? x) (str x) x))

(defn- cli-opt-helper
  [parser opt-map-or-err]
  (if-let [option (.getopt parser)]
    (if-not (gobj/get option "error")
      (do
        (.log js/console " " (gobj/get option "option") " " (gobj/get option "optarg" "-"))
        (recur parser
               (if-let [long-key (get cli-option-map (keyword (gobj/get option "option")))]
                 (assoc opt-map-or-err
                        long-key
                        (-> option
                            (gobj/get "optarg" "true")
                            reader/read-string
                            stringify-symbol))
                 (js/Error. "Cannot find a short-to-long key map. This might be a bug."))))
      (js/Error. (gstr/format "command line argument error found at %s" (gobj/get option "optopt"))))
    opt-map-or-err))

(defn cli-options
  "Compute the option map or a js/Error."
  [argv]
  (if (seq argv)
    (do (.log js/console "parsing cli options")
        (cli-opt-helper (go/BasicParser. (cli-getopt-string cli-option-map) (clj->js argv) 0) {}))
    ;; TODO Improve this error, print out basic usage
    (js/Error. "command line arguments are required.")))

(comment
  (def argv ["-z" "path" "--functions" "{:name test :invoke bla}"])
  (cli-options argv)
  )

(defn ^:export -main [& args]
  (let [opts-or-err (cli-options *command-line-args*)
        conf (read-conf!)]
    (if-not (instance? js/Error opts-or-err)
      (do (.info js/console "Building" (get-in conf [:project :name]))
          (build! (:zip-path opts-or-err)
                  (:functions opts-or-err)
                  (read-conf!)))
      (.error js/console (gobj/get opts-or-err "message")))))

(set! *main-cli-fn* -main)
