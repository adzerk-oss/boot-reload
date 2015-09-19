(ns adzerk.boot-reload
  {:boot/export-tasks true}
  (:require
   [boot.core          :as b]
   [clojure.java.io    :as io]
   [clojure.set        :as set]
   [boot.pod           :as pod]
   [boot.file          :as file]
   [boot.util          :as util]
   [boot.core          :refer :all]
   [boot.from.backtick :refer [template]]))

(def ^:private deps '[[http-kit "2.1.18"]])

(defn- make-pod []
  (future (-> (get-env) (update-in [:dependencies] into deps) pod/make-pod)))

(defn- changed [before after]
  (when before
    (->> (fileset-diff before after :hash)
         output-files
         (sort-by :dependency-order)
         (map tmp-path))))

(defn- start-server [pod {:keys [ip port ws-host] :as opts}]
  (let [{:keys [ip port]}
        (pod/with-call-in pod (adzerk.boot-reload.server/start ~opts))
        host
        (cond ws-host ws-host (= ip "0.0.0.0") "localhost" :else ip)]
    (util/with-let [url (format "ws://%s:%d" host port)]
      (util/info "Starting reload server at %s\n" url))))

(defn- write-cljs! [f url on-jsload]
  (util/info "Writing %s...\n" (.getName f))
  (->> (template
         ((ns adzerk.boot-reload
            (:require
             [adzerk.boot-reload.client :as client]
             ~@(when on-jsload [(symbol (namespace on-jsload))])))
          (when-not (client/alive?)
            (client/connect ~url {:on-jsload #(~(or on-jsload '+))}))))
    (map pr-str) (interpose "\n") (apply str) (spit f)))

(defn- send-changed! [pod asset-path changed]
  (when-not (empty? changed)
    (pod/with-call-in pod
      (adzerk.boot-reload.server/send-changed!
        ~(get-env :target-path)
        ~asset-path
        ~changed))))

(defn- add-init!
  [in-file out-file]
  (let [ns 'adzerk.boot-reload
        spec (-> in-file slurp read-string)]
    (when (not= :nodejs (-> spec :compiler-options :target))
      (util/info "Adding :require %s to %s...\n" ns (.getName in-file))
      (io/make-parents out-file)
      (-> spec
        (update-in [:require] conj ns)
        pr-str
        ((partial spit out-file))))))

(defn relevant-cljs-edn [fileset ids]
  (let [relevant  (map #(str % ".cljs.edn") ids)
        f         (if ids
                    #(b/by-name relevant %)
                    #(b/by-ext [".cljs.edn"] %))]
    (-> fileset b/input-files f)))

(deftask reload
  "Live reload of page resources in browser via websocket.

  The default configuration starts a websocket server on a random available
  port on localhost."

  [b ids BUILD_IDS #{str} "Only inject reloading into these builds (= .cljs.edn files)"
   i ip ADDR         str  "The (optional) IP address for the websocket server to listen on."
   p port PORT       int  "The (optional) port the websocket server listens on."
   w ws-host WSADDR  str  "The (optional) websocket host address to pass to clients."
   j on-jsload SYM   sym  "The (optional) callback to call when JS files are reloaded."
   a asset-path PATH str  "The (optional) asset-path. This is removed from the start of reloaded urls."]

  (let [pod  (make-pod)
        src  (tmp-dir!)
        tmp  (tmp-dir!)
        prev (atom nil)
        out  (doto (io/file src "adzerk" "boot_reload.cljs") io/make-parents)
        url  (start-server @pod {:ip ip :port port :ws-host ws-host})]
    (set-env! :source-paths #(conj % (.getPath src)))
    (write-cljs! out url on-jsload)
    (comp
      (with-pre-wrap fileset
        (doseq [f (relevant-cljs-edn fileset ids)]
          (let [path     (tmp-path f)
                in-file  (tmp-file f)
                out-file (io/file tmp path)]
            (add-init! in-file out-file)))
        (-> fileset (add-resource tmp) commit!))
      (with-post-wrap fileset
        (send-changed! @pod asset-path (changed @prev fileset))
        (reset! prev fileset)))))
