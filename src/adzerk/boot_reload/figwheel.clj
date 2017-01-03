(ns ^{:doc "Figwheel bindings and/or functions copied over."
      :author "Andrea Richiardi"}
    adzerk.boot-reload.figwheel
  (:require [clojure.string :as str]
            [clojure.walk :as walk]
            [figwheel-sidecar.config :as config]
            [figwheel-sidecar.build-middleware.javascript-reloading :as js-reload]
            [figwheel-sidecar.cljs-utils.exception-parsing :as ex-parsing]
            [figwheel-sidecar.build-middleware.injection :as injection]
            [cljs.compiler]
            [adzerk.boot-reload.util :as util]
            [adzerk.boot-reload.messages :as msgs]))

;;;;;;;;;;;;;;;;;;;;
;; DIRECT IMPORTS ;;
;;;;;;;;;;;;;;;;;;;;

(def ^{:doc "Generate the bootstrap namespace path string"
       :arglists '([build-config])}
  bootstrap-ns-name
  (var-get #'figwheel-sidecar.build-middleware.injection/figwheel-connect-ns-name))

(def ^{:doc "Generate the bootstrap namespace path string"
       :arglists '([build-config])}
  bootstrap-ns-path
  (var-get #'figwheel-sidecar.build-middleware.injection/figwheel-connect-ns-path))

(def ^{:doc "Generate the bootstrap namespace content"
       :arglists '([build-config])}
  bootstrap-ns-content
  (var-get #'figwheel-sidecar.build-middleware.injection/generate-connect-script))

(defn wrap-msg
  "Add common fields  to the message

  Note that msg keys always override in case of conflict."
  ([msg] (wrap-msg msg nil))
  ([msg opts]
   (-> opts
       (select-keys [:project-id :build-id])
       (assoc :figwheel-version config/_figwheel-version_)
       (merge msg)
       util/remove-nils)))

;; Reusing figwheel's make-sendable-file is not possible at the moment
;; https://github.com/bhauman/lein-figwheel/blob/e47da1658a716f83888e5a5164ee88e59b2d8c1e/sidecar/src/figwheel_sidecar/build_middleware/notifications.clj#L78
;; (intern *ns* 'make-sendable-file (var-get #'notifications/make-sendable-file))

(defn- client-path
  "Return the path that the client uses (relative to :asset-path)

  The input file map "
  [compile-opts file-map]
  (assert (:relative-path file-map) "The file-map is missing some fields, this is a bug.")
  (-> (str/replace (:relative-path file-map)
                   (or (:asset-path file-map) "")
                   "")
      (str/replace #"^/" "")))

(defn- guess-namespace
  [compile-opts file-map]
  (let [js-file-path (:full-path file-map)]
    (assert (string? js-file-path) ":full-path must be a string, This is an bug, please report it.")
    (assert (re-find #"\.js$" js-file-path) ":full-path must point to a Javascript file. This is an bug, please report it.")
    (->> js-file-path
         (js-reload/js-file->namespaces compile-opts)
         first
         cljs.compiler/munge)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; :msg-name :files-changed ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- sendable-js-map
  "Make a (Javascript) file map sendable according to figwheel protocol.

  Mimicking the function to
  figwheel-sidecar.build-middleware.notifications/make-sendable-file

  Sample return map:
    {:file \"resources/public/js/compiled/out/test_fig/core.js\"
     :namespace \"test_fig.core\"
     :type :namespace}"
  [js-and-opts]
  {:namespace (guess-namespace (:cljs-opts js-and-opts) js-and-opts)
   :file (client-path (:cljs-opts js-and-opts) js-and-opts)
   :type :namespace})

(defn- sendable-css-map
  "Make a (Javascript) file map sendable according to figwheel protocol.

  Mimicking the function to
  figwheel-sidecar.build-middleware.notifications/make-sendable-file

  Sample return map:
    {:file \"resources/public/js/compiled/out/test_fig/core.js\"
     :namespace \"test_fig.core\"
     :type :namespace}"
  [js-and-opts]
  {:file (client-path (:cljs-opts js-and-opts) js-and-opts)
   :type :css})

(defmethod msgs/file-payload-by-extension :js
  [opts [ext change-maps]]
  (when (seq change-maps)
    (-> {:msg-name :files-changed
         :files (mapv sendable-js-map change-maps)
         :figwheel-meta {"figwheel.client.utils" {:figwheel-no-load true}}}
        (wrap-msg opts))))

(defmethod msgs/file-payload-by-extension :css
  [opts [ext change-maps]]
  (when (seq change-maps)
    (-> {:msg-name :css-files-changed
         :files (mapv sendable-css-map change-maps)}
        (wrap-msg opts))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; :msg-name :compile-warning ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; AR - TODO handle multiple warnings, now we return only one
(defmethod msgs/visual-payload-by-type :warnings
  [opts [type warning-maps]]
  (when (seq warning-maps)
    (-> {:msg-name :compile-warning
         :message (ex-parsing/parse-warning (first warning-maps))}
        (wrap-msg opts))))

(defn- class-form?
  [form]
  (and (vector? form) (= :class (first form))))

(defn- clean-class-form
  [form]
  (update form 1 #(->> %
                       (re-find #"^adzerk.boot_cljs.util.proxy\$(.+)\$.+")
                       second
                       symbol
                       resolve)))

(defn figwheelify-exception
  "Boot-cljs to figwheel exception map

  Just to make it super clear, we receive a serialized exception map
  from boot-cljs and we want to convert it to a format that figwheel can
  parse."
  [ex]
  (walk/postwalk #(if-not (class-form? %)
                    %
                    (clean-class-form %))
                 ex))

(defmethod msgs/visual-payload-by-type :exception
  [opts [type ex-map]]
  ;; AR - this is real hammering
  (-> {:msg-name :compile-failed
       :exception-data (-> ex-map
                           figwheelify-exception
                           (ex-parsing/parse-inspected-exception opts))}
      (wrap-msg opts)))
