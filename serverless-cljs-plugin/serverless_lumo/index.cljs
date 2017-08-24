(ns serverless-lumo.index
  (:require mustache fs path process
            [clojure.string :as str]
            [serverless-lumo.templates :refer [templates]]))

(defn- export-name [sym]
  (str/replace (name (munge sym)) #"\." "_"))

(defn- fns->module-template [fns]
  (for [[ns fns] (group-by namespace (map :invoke fns))]
    {:name (munge ns)
     :function
     (for [f fns]
       ;; This is Clojure's munge, which isn't always going to be right
       {:export  (export-name f)
        :js-name (str (munge ns) "." (munge (name f)))})}))

(defn- generate-index [fns compiler]
  (.render
   mustache
   (templates (compiler :optimizations))
   (clj->js (assoc compiler :module (fns->module-template fns)))))
