(ns adzerk.boot-reload.server
  (:require
   [clojure.java.io    :as io]
   [boot.util          :as util]
   [org.httpkit.server :as http]
   [clojure.string     :as string])
  (:import
   [java.io IOException]))

(def options (atom {:open-file nil}))
(def clients (atom {}))

(defn set-options [opts]
  (reset! options opts))

(defn web-path
  ([rel-path] (web-path {} rel-path))
  ([opts rel-path]
   ; windows fix, convert \ characters to / in rel-path
   (let [rel-path (string/replace rel-path #"\\" "/")
         {:keys [protocol target-path asset-path cljs-asset-path]} opts]
     (if (= "file:" protocol)
       (.getCanonicalPath (io/file target-path rel-path))
       (str
        (if cljs-asset-path (str cljs-asset-path "/") "")
        (string/replace rel-path
                        (re-pattern (str "^" (string/replace (or asset-path "") #"^/" "") "/"))
                        ""))))))

(defn send-visual! [messages]
  (doseq [[channel _] @clients]
    (http/send! channel (pr-str (merge {:type :visual}
                                       messages)))))

(defn send-changed!
  ([changed] (send-changed! {} changed))
  ([opts changed]
   (doseq [[channel {:keys [protocol]}] @clients]
     (http/send! channel
                 (pr-str {:type :reload
                          :files (map #(web-path (assoc opts :protocol protocol) %) changed)})))))

(defmulti handle-message (fn [channel message] (:type message)))

(defmethod handle-message :set-protocol [channel {:keys [protocol]}]
  (swap! clients assoc-in [channel :protocol] protocol))

(defmethod handle-message :open-file [channel {:keys [file line column]}]
  (when-let [open-file (:open-file @options)]
    (let [cmd (format open-file (or line 0) (or column 0) (or file ""))]
      (util/dbug "Open-file call: %s\n" cmd)
      (try
        (.exec (Runtime/getRuntime) cmd)
        (catch Exception e
          (util/fail "There was a problem running open-file command: %s\n" cmd))))))

(defn connect! [channel]
  (swap! clients assoc channel {})
  (http/on-close channel (fn [_] (swap! clients dissoc channel)))
  (http/on-receive channel #(handle-message channel (read-string %))))

(defn handler [request]
  (if-not (:websocket? request)
    {:status 501 :body "Websocket connections only."}
    (http/with-channel request channel (connect! channel))))

(defn start
  [{:keys [ip port] :as opts}]
  (let [o {:ip (or ip "0.0.0.0") :port (or port 0)}]
    (assoc o :port (-> (http/run-server handler o) meta :local-port))))
