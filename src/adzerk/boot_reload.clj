(ns adzerk.boot-reload
  {:boot/export-tasks true}
  (:require
   [boot.core          :as b]
   [clojure.java.io    :as io]
   [clojure.set        :as set]
   [clojure.string     :as string]
   [boot.pod           :as pod]
   [boot.file          :as file]
   [boot.util          :as util]
   [boot.core          :refer :all]
   [boot.from.backtick :as bt]
   [boot.from.digest   :as digest]))

(def ^:private deps '[[http-kit "2.1.18"]])

(defn- make-pod []
  (future (-> (get-env) (update-in [:dependencies] into deps) pod/make-pod)))

(defn- changed [before after only-by-re static-files]
  (letfn [(maybe-filter-by-re [files]
            (if only-by-re
              (by-re only-by-re files)
              files))]
    (when before
      (->> (fileset-diff before after :hash)
           output-files
           maybe-filter-by-re
           (sort-by :dependency-order)
           (map tmp-path)
           (remove static-files)))))

(defn- start-server [pod {:keys [ip port ws-host ws-port secure?] :as opts}]
  (let [{:keys [ip port]} (pod/with-call-in pod (adzerk.boot-reload.server/start ~opts))
        listen-host       (cond (= ip "0.0.0.0") "localhost" :else ip)
        client-host       (cond ws-host ws-host (= ip "0.0.0.0") "localhost" :else ip)
        proto             (if secure? "wss" "ws")]
    (util/info "Starting reload server on %s\n" (format "%s://%s:%d" proto listen-host port))
    (format "%s://%s:%d" proto client-host (or ws-port port))))

(defn- write-cljs! [f ns url ws-host on-jsload asset-host]
  (util/info "Writing adzerk/boot_reload/%s to connect to %s...\n" (.getName f) url)
  (let [ns (symbol (str 'adzerk.boot-reload "." ns))]
    (->> (bt/template
          ((ns ~ns
             (:require
              [adzerk.boot-reload.client :as client]
              ~@(when on-jsload [(symbol (namespace on-jsload))])))
           (client/connect ~url {:on-jsload #(~(or on-jsload '+))
                                 :asset-host ~asset-host
                                 :ws-host ~ws-host})))
         (map pr-str) (interpose "\n") (apply str) (spit f))))

(defn- send-visual! [pod messages]
  (when-not (empty? messages)
    (pod/with-call-in pod
      (adzerk.boot-reload.server/send-visual!
        ~messages))))

(defn- send-changed! [pod asset-path cljs-asset-path target-path changed]
  (when-not (empty? changed)
    (pod/with-call-in pod
      (adzerk.boot-reload.server/send-changed!
       {:target-path ~target-path
        :asset-path ~asset-path
        :cljs-asset-path ~cljs-asset-path}
        ~changed))))

(defn- add-init!
  [ns in-file out-file]
  (let [ns (symbol (str 'adzerk.boot-reload "." ns))
        spec (-> in-file slurp read-string)]
    (when (not= :nodejs (-> spec :compiler-options :target))
      (util/info "Adding :require %s to %s...\n" ns (.getName in-file))
      (io/make-parents out-file)
      (-> spec
        (update-in [:require] conj ns)
        pr-str
        ((partial spit out-file))))))

(defn- relevant-cljs-edn [fileset ids]
  (let [relevant  (map #(str % ".cljs.edn") ids)
        f         (if ids
                    #(b/by-path relevant %)
                    #(b/by-ext [".cljs.edn"] %))]
    (-> fileset b/input-files f)))

(defn- namespace-for-cljs-edn
  [tmpfile]
  (let [path (tmp-path tmpfile)
        clean-name (-> (.getName (io/file path))
                       (string/replace #"\.cljs\.edn$" "")
                       (string/replace #"[^a-zA-Z0-9]" ""))]
    (str "init-"
         clean-name
         "-"
         (subs (digest/md5 path) 0 8))))

(deftask reload
  "Live reload of page resources in browser via websocket.

  The default configuration starts a websocket server on a random available
  port on localhost.

  Open-file option takes three arguments: line number, column number, relative
  file path. You can use positional arguments if you need different order.
  Arguments shouldn't have spaces.
  Examples:
  vim --remote +norm%sG%s| %s
  emacsclient -n +%s:%s %s"

  [b ids BUILD_IDS #{str} "Only inject reloading into these builds (= .cljs.edn files)"
   ;; Websocket Server
   i ip ADDR         str  "The IP address for the websocket server to listen on. (optional)"
   p port PORT       int  "The port the websocket server listens on. (optional)"
   _ ws-port PORT    int  "The port the websocket will connect to. (optional)"
   w ws-host WSADDR  str  "The websocket host clients connect to. Defaults to current host. (optional)"
   s secure          bool "Flag to indicate whether the client should connect via wss. Defaults to false."
   ;; Other Configuration
   j on-jsload SYM     sym "The callback to call when JS files are reloaded. (optional)"
   _ asset-host HOST   str "The asset-host where to load files from. Defaults to host of opened page. (optional)"
   a asset-path PATH   str "Sets the output directory for temporary files used during compilation. (optional)"
   c cljs-asset-path PATH str "The actual asset path. This is added to the start of reloaded urls. (optional)"
   o open-file COMMAND str "The command to run when warning or exception is clicked on HUD. Passed to format. (optional)"
   v disable-hud      bool "Toggle to disable HUD. Defaults to false (visible)."
   t target-path      VAL str "Target path to load files from, used WHEN serving files using file: protocol. (optional)"
   _ only-by-re REGEXES [regex] "Vector of path regexes (for `boot.core/by-re`) to restrict reloads to only files within these paths (optional)."]

  (let [pod  (make-pod)
        src  (tmp-dir!)
        tmp  (tmp-dir!)
        prev-pre (atom nil)
        prev (atom nil)
        url  (start-server @pod {:ip ip :port port :ws-host ws-host
                                 :ws-port ws-port :secure? secure
                                 :open-file open-file})]
    (set-env! :source-paths #(conj % (.getPath src)))
    (write-cljs! out ns url ws-host on-jsload asset-host)
    (b/cleanup (pod/with-call-in @pod (adzerk.boot-reload.server/stop)))
    (fn [next-task]
      (fn [fileset]
        (pod/with-call-in @pod
          (adzerk.boot-reload.server/set-options {:open-file ~open-file}))
        (doseq [f (relevant-cljs-edn (b/fileset-diff @prev-pre fileset) ids)]
          (let [path     (tmp-path f)
                spec     (-> f tmp-file slurp read-string)
                ns       (namespace-for-cljs-edn f)
                out      (doto (io/file src "adzerk" "boot_reload" (str (string/replace ns "-" "_") ".cljs"))
                           io/make-parents)
                in-file  (tmp-file f)
                out-file (io/file tmp path)]
            (write-cljs! out ns url ws-host (get spec :on-jsload on-jsload) asset-host)
            (add-init! ns in-file out-file)))
        (reset! prev-pre fileset)
        (let [fileset (-> fileset (add-resource tmp) commit!)
              fileset (try
                        (next-task fileset)
                        (catch Exception e
                          ;; FIXME: Supports only single error, e.g. less compiler
                          ;; can give multiple errors.
                          (if (and (not disable-hud)
                                   (or (= :boot-cljs (:from (ex-data e)))
                                       (:adzerk.boot-reload/exception (ex-data e))))
                            (send-visual! @pod {:exception (merge {:message (.getMessage e)}
                                                                  (ex-data e))}))
                          (throw e)))]
          (let [cljs-edn (relevant-cljs-edn fileset ids)
                ;; cljs uses specific key for now
                ;; but any other file can contain warnings for boot-reload
                warnings (concat (mapcat :adzerk.boot-cljs/warnings cljs-edn)
                                 (mapcat :adzerk.boot-reload/warnings (b/input-files fileset)))
                static-files (->> cljs-edn
                                  (map b/tmp-path)
                                  (map(fn [x] (string/replace x #"\.cljs\.edn$" ".js")))
                                  set)]
            (if-not disable-hud
              (send-visual! @pod {:warnings warnings}))
            ; Only send changed files when there are no warnings
            ; As prev is updated only when changes are sent, changes are queued untill they can be sent
            (when (empty? warnings)
              (send-changed! @pod
                             asset-path
                             cljs-asset-path
                             target-path
                             (changed @prev fileset only-by-re static-files))
              (reset! prev fileset))
            fileset))))))
