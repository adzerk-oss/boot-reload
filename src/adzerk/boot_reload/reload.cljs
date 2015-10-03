(ns adzerk.boot-reload.reload
  (:require [goog.Uri                :as guri]
            [goog.async.DeferredList :as deferred-list]
            [goog.net.jsloader       :as jsloader]
            [goog.object             :as gobj]
            [goog.string             :as gstring]
            [clojure.set             :as set]))

(def ^:private page-uri (goog.Uri. (.. js/window -location -href)))

(defn- ends-with? [s pat]
  (= pat (subs s (- (count s) (count pat)))))

(defn- reload-page! []
  (.reload (.-location js/location)))

(defn- normalize-href-or-uri [href-or-uri]
  (let [uri  (goog.Uri. href-or-uri)]
    (.getPath (.resolve page-uri uri))))

(defn- changed-href? [href-or-uri changed]
  (when href-or-uri
    (let [path (normalize-href-or-uri href-or-uri)]
      (when (not-empty (filter #(ends-with? (normalize-href-or-uri %) path) changed))
        (guri/parse path)))))

(defn- reload-css [changed]
  (let [sheets (.. js/document -styleSheets)]
    (doseq [s (range 0 (.-length sheets))]
      (when-let [sheet (aget sheets s)]
        (when-let [href-uri (changed-href? (.-href sheet) changed)]
          (set! (.. sheet -ownerNode -href) (.toString (.makeUnique href-uri))))))))

(defn- reload-img [changed]
  (let [images (.. js/document -images)]
    (doseq [s (range 0 (.-length images))]
      (when-let [image (aget images s)]
        (when-let [href-uri (changed-href? (.-src image) changed)]
          (set! (.-src image) (.toString (.makeUnique href-uri))))))))

;;
;; From figwheel: https://github.com/bhauman/lein-figwheel/blob/f7d993e110fcb9db227d936bd04d5b3904f8f87f/support/src/figwheel/client/file_reloading.cljs
;;

(defn name->path [ns]
  ; (dev-assert (string? ns))
  (aget js/goog.dependencies_.nameToPath ns))

(defn provided? [ns]
  (aget js/goog.dependencies_.written (name->path ns)))

(defn immutable-ns? [name]
  (or (#{"goog"
         "cljs.core"
         "an.existing.path"
         "dup.base"
         "far.out"
         "ns"
         "someprotopackage.TestPackageTypes"
         "svgpan.SvgPan"
         "testDep.bar"} name)
      (some
       (partial goog.string/startsWith name)
       ["goog." "cljs." "clojure." "fake." "proto2."])))

(defn get-requires [ns]
  (->> ns
    name->path
    (aget js/goog.dependencies_.requires)
    (gobj/getKeys)
    (filter #(not (immutable-ns? %)))
    set))

(defonce dependency-data (atom {:pathToName {} :dependents {}}))

(defn path-to-name! [path name]
  (swap! dependency-data update-in [:pathToName path] (fnil clojure.set/union #{}) #{name}))

(defn setup-path->name!
  "Setup a path to name dependencies map.
   That goes from path -> #{ ns-names }"
  []
  ;; we only need this for dependents
  (let [nameToPath (gobj/filter js/goog.dependencies_.nameToPath
                                (fn [v k o] (gstring/startsWith v "../")))]
    (gobj/forEach nameToPath (fn [v k o] (path-to-name! v k)))))

(defn path->name
  "returns a set of namespaces defined by a path"
  [path]
  (get-in @dependency-data [:pathToName path]))

(defn name-to-parent! [ns parent-ns]
  (swap! dependency-data update-in [:dependents ns] (fnil clojure.set/union #{}) #{parent-ns}))

(defn setup-ns->dependents!
  "This reverses the goog.dependencies_.requires for looking up ns-dependents."
  []
  (let [requires (gobj/filter js/goog.dependencies_.requires
                              (fn [v k o] (gstring/startsWith k "../")))]
    (gobj/forEach
     requires
     (fn [v k _]
       (gobj/forEach
        v
        (fn [v' k' _]
          (doseq [n (path->name k)]
            (name-to-parent! k' n))))))))

(defn unprovide! [ns]
  (let [path (name->path ns)]
    (gobj/remove js/goog.dependencies_.visited path)
    (gobj/remove js/goog.dependencies_.written path)
    (gobj/remove js/goog.dependencies_.written (str js/goog.basePath path))))

(defn resolve-ns [ns] (str goog/basePath (name->path ns)))

(defn addDependency [path provides requires]
  (doseq [prov provides]
    (path-to-name! path prov)
    (doseq [req requires]
      (name-to-parent! req prov))))

(defn ns->dependents [ns]
  (get-in @dependency-data [:dependents ns]))

(defn build-topo-sort [get-deps]
  (let [get-deps (memoize get-deps)]
    (letfn [(topo-sort-helper* [x depth state]
              (let [deps (get-deps x)]
                (when-not (empty? deps) (topo-sort* deps depth state))))
            (topo-sort*
              ([deps]
               (topo-sort* deps 0 (atom (sorted-map))))
              ([deps depth state]
               (swap! state update-in [depth] (fnil into #{}) deps)
               (doseq [dep deps]
                 (topo-sort-helper* dep (inc depth) state))
               (when (= depth 0)
                 (elim-dups* (reverse (vals @state))))))
            (elim-dups* [[x & xs]]
              (if (nil? x)
                (list)
                (cons x (elim-dups* (map #(set/difference % x) xs)))))]
      topo-sort*)))

(defn get-all-dependencies [ns]
  (let [topo-sort' (build-topo-sort get-requires)]
    (apply concat (topo-sort' (set [ns])))))

(defn get-all-dependents [nss]
  (let [topo-sort' (build-topo-sort ns->dependents)]
    (reverse (apply concat (topo-sort' (set nss))))))

(defn figwheel-require [src reload]
  ;; require is going to be called
  (set! (.-require js/goog) figwheel-require)
  (when (= reload "reload-all")
    (doseq [ns (get-all-dependencies src)] (unprovide! ns)))
  (when reload (unprovide! src))
  (.require_figwheel_backup_ js/goog src))

(defn expand-files [files]
  (let [deps (get-all-dependents (map :namespace files))]
    (filter (comp not #{"figwheel.connect"} :namespace)
            (map
              (fn [n]
                (if-let [file-msg (first (filter #(= (:namespace %) n) files))]
                  file-msg
                  {:type :namespace :namespace n}))
              deps))))

(defonce on-load-callbacks (atom {}))

(defonce reloader-loop
  ; (go-loop []
  ;   (when-let [url (<! reload-chan)]
  ;     (let [file-msg (<! (blocking-load url))]
  ;       (if-let [callback (get @on-load-callbacks url)]
  ;         (callback file-msg)
  ;         (swap! dependencies-loaded conj file-msg))
  ;       (recur))))
  nil
  )

(defn queued-file-reload [url]
  ; (put! reload-chan url)
  )

(defn require-with-callback [{:keys [namespace] :as file-msg} callback]
  (let [request-url (resolve-ns namespace)]
    (swap! on-load-callbacks assoc request-url
           (fn [file-msg']
             (swap! on-load-callbacks dissoc request-url)
             (apply callback [(merge file-msg (select-keys file-msg' [:loaded-file]))])))
    ;; we are forcing reload here
    (figwheel-require (name namespace) true)))

(defn reload-file? [{:keys [namespace] :as file-msg}]
  ; (dev-assert (namespace-file-map? file-msg))
  ;; might want to use .-visited here
  (provided? (name namespace)))

(defn js-reload [{:keys [request-url namespace] :as file-msg} callback]
  ; (dev-assert (namespace-file-map? file-msg))
  (if (reload-file? file-msg)
    (require-with-callback file-msg callback)
    (do
      (js/console.log (str "Figwheel: Not trying to load file " request-url))
      (apply callback [file-msg]))))

(defn reload-js-file [file-msg]
  ; (jsloader/load (.makeUnique (guri/parse file-url)))
  (js-reload file-msg (fn callback [url] nil)))

(defn bootstrap-goog-base
  "Reusable browser REPL bootstrapping. Patches the essential functions
  in goog.base to support re-loading of namespaces after page load."
  []
  ;; The biggest problem here is that clojure.browser.repl might have
  ;; patched this or might patch this afterward
  (when-not js/COMPILED
    ;;
    (set! (.-require_figwheel_backup_ js/goog) (or js/goog.require__ js/goog.require))
    ;; suppress useless Google Closure error about duplicate provides
    (set! (.-isProvided_ js/goog) (fn [name] false))
    ;; provide cljs.user
    (setup-path->name!)
    (setup-ns->dependents!)

    (set! (.-addDependency_figwheel_backup_ js/goog) js/goog.addDependency)
    (set! (.-addDependency js/goog)
          (fn [& args]
            (apply addDependency args)
            (apply (.-addDependency_figwheel_backup_ js/goog) args)))

    (goog/constructNamespace_ "cljs.user")
    ;; we must reuse Closure library dev time dependency management, under namespace
    ;; reload scenarios we simply delete entries from the correct
    ;; private locations
    (set! (.-CLOSURE_IMPORT_SCRIPT goog/global) queued-file-reload)
    (set! (.-require js/goog) figwheel-require)))

(defn patch-goog-base! []
  (defonce bootstrapped-cljs (do (bootstrap-goog-base) true)))

;;
;; End of parts from figwheel
;;

(defn- reload-js [changed {:keys [on-jsload]
                           :or {on-jsload identity}}]
  (let [js-files (filter #(ends-with? % ".js") changed)
        js-files (expand-files js-files)]
    (when (seq js-files)
      (-> (map reload-js-file js-files)
        clj->js
        deferred-list/gatherResults
        (.addCallbacks
          (fn [& _] (on-jsload))
          (fn [e] (.error js/console "Load failed:" (.-message e)))))
      (when (aget js/window "jQuery")
        (.trigger (js/jQuery js/document) "page-load")))))

(defn- reload-html [changed]
  (let [page-path (.getPath page-uri)
        html-path (if (ends-with? page-path "/")
                    (str page-path "index.html")
                    page-path)]
    (when (changed-href? html-path changed) (reload-page!))))

(defn- group-log [title things-to-log]
  (.groupCollapsed js/console title)
  (doseq [t things-to-log] (.log js/console t))
  (.groupEnd js/console))

(defn reload [changed opts]
  (group-log "Reload" changed)
  (doto changed
    (reload-js opts)
    reload-html
    reload-css
    reload-img))
