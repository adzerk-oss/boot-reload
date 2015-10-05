(ns adzerk.boot-reload.server
  (:require
   [clojure.java.io    :as io]
   [boot.util          :as util]
   [org.httpkit.server :as http]
   [clojure.string     :as string]
   [adzerk.boot-reload.figwheel :as figwheel]
   [adzerk.boot-reload.messages :as msgs])
  (:import
   [java.io IOException]))

(def options (atom {:open-file nil}))
(def clients (atom #{}))
(def stop-fn (atom nil))

(defn set-options [opts]
  (reset! options opts))

(defn bootstrap-ns
  "Computes the bootstrap namespace

  Return [ns-sym ns-path ns-content]."
  [build-config]
  [(figwheel/bootstrap-ns-name build-config)
   (figwheel/bootstrap-ns-path build-config)
   (->> build-config
        figwheel/bootstrap-ns-content
        (map util/pp-str)
        (interpose "\n")
        (apply str))])

;; AR The cousin of:
;; https://github.com/bhauman/lein-figwheel/blob/cc2d188ab041fc92551d3c4a8201729c47fe5846/sidecar/src/figwheel_sidecar/build_middleware/injection.clj#L171
(defn- add-cljs-edn-init
  "Add requires and other init stuff to the .cljs.edn spec

  Return the new cljs.edn spec as data (not a string)."
  [build-config cljs-spec]
  (update cljs-spec :require #(->> (conj %
                                         (-> build-config
                                             figwheel/bootstrap-ns-name
                                             symbol)
                                         (when (get-in build-config [:figwheel :devcards])
                                           'devcards.core))
                                   (into #{})
                                   (remove nil?)
                                   vec)))

;;;;;;;;;;;;;;;
;; WEBSOCKET ;;
;;;;;;;;;;;;;;;

(defn send-changed!
  ([change-map] (send-changed! {} change-map))
  ([opts change-map]
   (when (> @boot.util/*verbosity* 2)
     (util/dbug "Watch received:\n%s\n" (util/pp-str change-map)))
   (doseq [channel @clients]
     (let [payloads (->> change-map
                         (msgs/changed-messages opts)
                         (mapv #(merge % (select-keys opts [:figwheel-version]))))]
       (run! #(let [payload (util/pp-str %)]
                (util/dbug "Sending:\n%s\n" payload)
                (http/send! channel payload))
             payloads)))))

(defn send-visual!
  "Send a visual notification to the connected clients.

  A visual map is a map that has its key equal to the type (:warning,
  exception, ...) of the notification and its value equal to a sequence
  of payloads."
  ([visual-map] (send-visual! {} visual-map))
  ([opts visual-map]
   (when (> @boot.util/*verbosity* 2)
     (util/dbug "Watch received:\n%s\n" visual-map))
   (doseq [channel @clients]
     ;; AR - TODO handle multiple warnings and exceptions
     (let [payload (->> visual-map
                        (msgs/visual-message opts)
                        (merge (select-keys opts [:figwheel-version])))]
       (when-not (empty? payload)
         (util/dbug "Sending:\n%s\n" payload)
         (http/send! channel (util/pp-str payload)))))))

;;;;;;;;;;;;;;;
;; WEBSOCKET ;;
;;;;;;;;;;;;;;;

(defmulti handle-message (fn [channel message] (:type message)))

(defmethod handle-message :open-file [channel {:keys [file line column]}]
  (when-let [open-file (:open-file @options)]
    (let [cmd (format open-file (or line 0) (or column 0) (or file ""))]
      (util/dbug "Open-file call: %s\n" cmd)
      (try
        (.exec (Runtime/getRuntime) cmd)
        (catch Exception e
          (util/fail "There was a problem running open-file command: %s\n" cmd))))))

(defn connect! [channel]
  (util/dbug "Channel \"%s\" opened...\n" (str channel))

  (swap! clients conj channel)
  (when (> @boot.util/*verbosity* 2)
    (util/dbug "Connected clients %s\n" (mapv str @clients)))

  (http/on-close channel (fn [_] (swap! clients disj channel)))
  (http/on-receive channel (fn [data]
                             (when (> @boot.util/*verbosity* 2)
                               (util/dbug "Websocket received:\n%s\n" (util/pp-str data)))
                             (handle-message channel (read-string data))))

  (util/info "New websocket client connected!\n"))

(defn handler [request]
  (if-not (:websocket? request)
    {:status 501 :body "Websocket connections only."}
    (do (when (> @boot.util/*verbosity* 2)
          (util/dbug "Websocket received:\n%s\n" (util/pp-str request)))
        (http/with-channel request channel (connect! channel)))))

(defn start
  [{:keys [ip port] :as opts}]
  (let [o {:ip (or ip "0.0.0.0") :port (or port 0)}
        stop-fn* (http/run-server handler o)]
    (reset! stop-fn stop-fn*)
    (assoc o :port (-> stop-fn* meta :local-port))))

(defn stop []
  (when @stop-fn
    (@stop-fn)
    (reset! stop-fn nil)))
