(ns lumo-example.core)

(defn ^:export example [event ctx cb]
  (js/console.log "Called with" event ctx cb)
  (cb nil event))
