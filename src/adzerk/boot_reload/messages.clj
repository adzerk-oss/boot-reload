(ns ^{:doc "Message management."
      :author "Andrea Richiardi"}
    adzerk.boot-reload.messages)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; FILE CHANGED NOTIFICATIONS ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- file-map-matches?
  [file-map compile-opts]
  (assert (:asset-path compile-opts) "The compiler options :asset-path cannot be nil. This might be a bug.")
  (assert (:relative-path file-map) "The relative path in the file-map cannot be nil. This might be a bug.")
  (re-find (re-pattern (:asset-path compile-opts)) (:relative-path file-map)))

(defn- assign-cljs-opts
  "Assign the correct cljs options to the file-map based on pred.

  The (fn [data compile-opts] ...) predicate decides what \"correct\"
  means, returning true or false according to the input data and the
  candidate compile-opts.

  The correct options will be assigned under the :cljs-opts key."
  [pred cljs-opts-seq file-maps]
  (mapv #(if-let [opts (->> cljs-opts-seq
                            (filter (partial pred %))
                            first)]
           (assoc % :cljs-opts opts)
           %)
        file-maps))

(def ^{:private true
       :doc "Assign the correct cljs options to the correct file-map based on its path"
       :arglists '([cljs-opts-seq file-maps])}
  assign-cljs-opts-by-path
  (partial assign-cljs-opts file-map-matches?))

(defmulti file-payload-by-extension
  "Calculate payloads of change-maps based on extension.

  The first parameter is obviously the client option map, while the
  second is a vector containing extension first and then a seq of file
  maps:

    [:css [[file-map1] [file-map2]]]"
  (fn [client-opts [ext change-maps]] ext))

(defmethod file-payload-by-extension :default
  [_ _]
  (assert false "This should never happen, did you forget to handle a file extension?"))

(defn changed-messages
  "Return (file) change-related messages"
  [client-opts watch-map]
  (let [cljs-opts-seq (:cljs-opts-seq watch-map)
        change-maps-by-ext (select-keys
                            (->> watch-map
                                 :change-set
                                 (assign-cljs-opts-by-path cljs-opts-seq)
                                 (group-by #(->> (:relative-path %)
                                                 (re-find #"\.(\p{Alnum}+$)")
                                                 second
                                                 keyword)))
                            [:css :js])]
    (map (partial file-payload-by-extension client-opts) change-maps-by-ext)))

;;;;;;;;;;;;;;;;;;;;;;;;;;
;; VISUAL NOTIFICATIONS ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti visual-payload-by-type
  "Calculate payloads of a visual-map based on it type

  The first parameter is obviously the client option map, while the
  second is a vector [type data]. The dispatch is on the type."
  (fn [client-opts [type data]] type))

(defmethod visual-payload-by-type :default
  [_ _]
  (assert false "This should never happen, did you forget to handle a visual notification type?"))

(defn visual-message
  "Return a visual message.

  It expects something like:
    {:warnings [{:k1 :v1} {:k2 :v2} ...]}
     ^- type   ^- data (not only seqs, it depends on the the type)"
  [client-opts visual-map]
  ;; AR - I know I know we need clojure.spec
  (visual-payload-by-type client-opts (first (seq visual-map))))
