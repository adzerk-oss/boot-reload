(ns adzerk.boot-reload
  {:boot/export-tasks true}
  (:require
   [boot.core          :as b]
   [clojure.java.io    :as io]
   [clojure.set        :as set]
   [clojure.string     :as string]
   [boot.pod           :as pod]
   [boot.file          :as file]
   [boot.util          :as butil]
   [boot.core          :refer :all]
   [boot.from.digest        :as digest]
   [adzerk.boot-reload.util :as util]))

(def ^:private deps '[[http-kit "2.1.18"]
                      [figwheel-sidecar "0.5.9-SNAPSHOT"]])

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
           (not-by-path static-files)
           (sort-by :dependency-order)
           (reduce #(conj %1 {:relative-path (-> %2 tmp-path)
                              :dir-path (-> %2 tmp-dir .getCanonicalPath)
                              :full-path (-> %2 tmp-file .getCanonicalPath)})
                   #{})))))

(defn- start-server [pod {:keys [ip port ws-host ws-port secure?] :as opts}]
  (let [{:keys [ip port]} (pod/with-call-in pod (adzerk.boot-reload.server/start ~opts))
        listen-host       (cond (= ip "0.0.0.0") "localhost" :else ip)
        client-host       (cond ws-host ws-host (= ip "0.0.0.0") "localhost" :else ip)
        proto             (if secure? "wss" "ws")]
    (butil/info "Starting reload server on %s\n" (format "%s://%s:%d" proto listen-host port))
    (format "%s://%s:%d" proto client-host (or ws-port port))))

(defn- write-bootstrap-ns! [pod parent-path build-config]
  (let [[ns-sym ns-path ns-content] (pod/with-call-in pod
                                      (adzerk.boot-reload.server/bootstrap-ns
                                       ~build-config))
        f (io/file parent-path ns-path)]
    (io/make-parents f)
    (butil/info "Writing %s namespace to %s...\n" ns-sym (.getName f))
    (butil/dbug "%s\n" ns-content)
    (spit f ns-content)))

(defn- send-visual! [pod client-opts visual-map]
  (pod/with-call-in pod
    (adzerk.boot-reload.server/send-visual!
     ~client-opts
     ~visual-map)))

(defn- send-changed! [pod client-opts change-map]
  (pod/with-call-in pod
    (adzerk.boot-reload.server/send-changed!
     ~client-opts
     ~change-map)))

(defn- add-init!
  [pod build-config old-spec out-file]
  (when (not= :nodejs (-> old-spec :compiler-options :target))
    (io/make-parents out-file)
    (let [new-spec (pod/with-call-in pod
                     (adzerk.boot-reload.server/add-cljs-edn-init
                      ~build-config
                      ~old-spec))]
      (butil/info "Adding :require(s) to %s...\n" (.getName out-file))
      (butil/dbug "%s\n" (butil/pp-str new-spec))
      (->> new-spec
           pr-str
           (spit out-file)))))

(defn- relevant-cljs-edns [fileset ids]
  (let [relevant  (map #(str % ".cljs.edn") ids)
        f         (if ids
                    #(b/by-path relevant %)
                    #(b/by-ext [".cljs.edn"] %))]
    (-> fileset b/input-files f)))

(defn cljs-opts-seq
  "Return a sequence of compiler options given boot's .cljs.edn files on
  the fileset."
  [tmpfiles]
  (mapv #(-> {}
             (assoc :build-id (util/build-id (:path %)))
             (merge (:adzerk.boot-cljs/opts %)))
        tmpfiles))

(defn make-build-config
  "Return an ClojureScript build configuration map akin to leinengen and
  figwheel:
    https://github.com/bhauman/lein-figwheel#client-side-configuration-options

  The id is last because it is the one more likely to vary."
  [ws-url ws-host on-jsload client-opts build-id]
  ;; See https://github.com/bhauman/lein-figwheel/blob/0283f6d79af630854c1d3071eeb6f0ff8fd41676/sidecar/src/figwheel_sidecar/schemas/config.clj#L602
  (-> {:id build-id
       :figwheel {:websocket-host ws-host
                  :websocket-url ws-url}}
      (assoc-in [:figwheel :on-jsload] on-jsload)
      (update :figwheel merge client-opts)))

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
   i ip      ADDR    str  "The IP address for the websocket server to listen on. (optional)"
   p port    PORT    int  "The port the websocket server listens on. (optional)"
   _ ws-port PORT    int  "The port the websocket will connect to. (optional)"
   w ws-host WSADDR  str  "The websocket host clients connect to. Defaults to current host. (optional)"
   s secure          bool "Flag to indicate whether the client should connect via wss. Defaults to false."
   ;; Client Configuration
   j on-jsload       SYM    sym  "The callbacks to call when JS files are reloaded. (optional)"
   _ client-opts     OPTS   edn  "Options passed to the client directly (overrides the others)."
   v disable-hud            bool "Toggle to disable HUD. Defaults to false (visible)."
   o open-file       CMD    str  "The command to run when warning or exception is clicked on HUD. Passed to format. (optional)"
   ;; Other Configuration - AR - (?)
   _ asset-host      HOST   str  "The asset-host where to load files from. Defaults to host of opened page. (optional)"
   a asset-path      PATH   str  "Sets the output directory for temporary files used during compilation. (optional)"
   c cljs-asset-path PATH   str  "The actual asset path. This is added to the start of reloaded urls. (optional)"
   t target-path     VAL    str  "Target path to load files from, used WHEN serving files using file: protocol. (optional)"
   _ only-by-re      REGEX [regex] "Vector of path regexes (for `boot.core/by-re`) to restrict reloads to only files within these paths (optional)."]

  (let [ids  (or ids #{"main"})
        pod  (make-pod)
        tmp  (tmp-dir!)
        prev-pre (atom nil)
        prev (atom nil)
        url  (start-server @pod {:ip ip :port port :ws-host ws-host
                                 :ws-port ws-port :secure? secure
                                 :open-file open-file})
        client-opts (assoc client-opts :project-id (-> (b/get-env) :directories string/join digest/md5))]
    (b/cleanup (pod/with-call-in @pod (adzerk.boot-reload.server/stop)))
    (fn [next-task]
      (fn [fileset]
        ;; AR - TODO - pass the :open-file option down to figwheel and figure
        ;; out how to handle it
        (pod/with-call-in @pod
          (adzerk.boot-reload.server/set-options {:open-file ~open-file}))

        (let [cljs-edns (relevant-cljs-edns (b/fileset-diff @prev-pre fileset) ids)]
          (when (and (empty? @prev-pre) (empty? cljs-edns))
            (butil/warn "WARNING: No .cljs.edn file found on the fileset, boot-reload might misbehave.\n"))
          (doseq [f cljs-edns]
            (let [path     (tmp-path f)
                  spec     (-> f tmp-file slurp read-string)
                  build-config (make-build-config url
                                                  ws-host
                                                  (get spec :on-jsload on-jsload)
                                                  client-opts
                                                  (util/build-id path))]
              (butil/dbug "Build config:\n%s\n" (butil/pp-str build-config))
              ;; Writing the connection script to the tmp folder
              (write-bootstrap-ns! @pod tmp build-config)
              ;; Add the init files to the fileset
              (add-init! @pod build-config spec (io/file tmp path)))))
        (reset! prev-pre fileset)

        ;; Compile by triggering the chain of tasks
        (let [fileset (-> fileset (add-resource tmp) commit!)
              fileset (try
                        (next-task fileset)
                        (catch Exception e
                          ;; FIXME: Supports only single error, e.g. less compiler
                          ;; can give multiple errors.
                          (if (and (not disable-hud)
                                   (or (= :boot-cljs (:from (ex-data e)))
                                       (:adzerk.boot-reload/exception (ex-data e))))
                            (send-visual! @pod client-opts {:exception (util/serialize-exception e)}))
                          (throw e)))]
          (let [cljs-edns (relevant-cljs-edns fileset ids)
                ;; cljs uses specific key for now
                ;; but any other file can contain warnings for boot-reload
                warnings (concat (mapcat :adzerk.boot-cljs/warnings cljs-edns)
                                 (mapcat :adzerk.boot-reload/warnings (b/input-files fileset)))
                cljs-opts-seq (cljs-opts-seq cljs-edns)
                static-files (->> cljs-edns
                                  (map b/tmp-path)
                                  (map(fn [x] (string/replace x #"\.cljs\.edn$" ".js")))
                                  set)]
            (butil/dbug "Compile opts:\n%s\n" (butil/pp-str cljs-opts-seq))
            (when (> @butil/*verbosity* 2)
              (butil/dbug "Warnings:\n%s\n" (butil/pp-str warnings))
              (doseq [edn cljs-edns]
                (butil/dbug "Meta on %s:\n%s\n" (:path edn) (butil/pp-str (select-keys edn [])))))
            (if-not disable-hud
              (send-visual! @pod client-opts {:warnings warnings}))
            ;; Only send changed files when there are no warnings
            ;; As prev is updated only when changes are sent, changes are queued untill they can be sent
            ;;
            ;; AR - the above assumption changes when using figwheel's client,
            ;; where the event order (unfortunately) matters.
            (send-changed! @pod
                           client-opts
                           {:target-path target-path
                            :asset-path asset-path
                            :cljs-asset-path cljs-asset-path
                            :cljs-opts-seq cljs-opts-seq
                            :change-set (changed @prev fileset only-by-re static-files)})
            (reset! prev fileset)
            fileset))))))
