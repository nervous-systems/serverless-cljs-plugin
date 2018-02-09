(ns serverless-lumo.build
  (:require fs path
            [clojure.string :as str]
            [cljs.reader :as reader]
            [lumo.build.api]
            [lumo.classpath :as classpath]
            [lumo.core]
            [lumo.io :as io]
            [cljs.analyzer :as ana]
            [serverless-lumo.index :as index]))

(defn compile!
  "Compile ClojureScript using the lumo api."
  [inputs compiler-opts]
  (js/console.info
   "Invoking the Lumo compiler w/ inputs"
   (.stringify js/JSON (clj->js inputs)))
  (run! classpath/add! inputs)
  (lumo.build.api/build
   (apply lumo.build.api/inputs inputs)
   compiler-opts))

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
                  :source-map    true
                  :target        :nodejs
                  :optimizations :none}})

(defn- output-dir [compiler]
  (let [s (:output-dir compiler)]
    (str/replace s #"/+$" "")))

(defn dump-index!
  [path functions compiler]
  (-> (index/generate-index functions compiler)
      (index/write-index! path)))

(defn exit-on-warning!
  [warning-type env extra]
  (when (warning-type ana/*cljs-warnings*)
    (when-let [s (ana/error-message warning-type extra)]
      (binding [*print-fn* *print-err-fn*]
        (println "lumo error:" (ana/message env s)))
      (lumo.core/exit 1))))

(defn build!
  "Build a project."
  [opts cljs-lambda-opts]
  (let [compiler   (merge (when (:warning-exit opts)
                            {:warning-handlers [exit-on-warning!]})
                          (:compiler cljs-lambda-opts))
        output-dir (output-dir compiler)]
    (dump-index! (.resolve path (:service-path opts) "index.js")
                 (:functions opts)
                 compiler)
    (compile! (:source-paths cljs-lambda-opts) compiler)))

(def cli-option-map
  {:s :service-path
   :f :functions
   :w :warning-exit})

(defmulti  parse-option (fn [k v] k))
(defmethod parse-option :default   [k v] v)
(defmethod parse-option :functions [k v] (reader/read-string v))
(defmethod parse-option :warning-exit [k v] (reader/read-string v))

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

(defn- merge-maps [x y]
  (merge-with
   (fn [x y]
     (if (map? x)
       (merge-maps x y)
       y))
   x y))

(defn ^:export -main [& args]
  (let [opts (cli-options (cmd-line-args))]
    (build! opts (merge-maps default-config (read-conf!)))))

(set! *main-cli-fn* -main)
