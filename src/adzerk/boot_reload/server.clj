(ns adzerk.boot-reload.server
  (:require
   [clojure.java.io    :as io]
   [boot.util          :as util]
   [org.httpkit.server :as http]
   [clojure.string     :as string])
  (:import
   [java.io IOException]))

(def state (atom {}))

(defn web-path [protocol rel-path tgt-path asset-path]
  (if-not (= "file:" protocol)
    (if asset-path
      (string/replace rel-path (re-pattern (str "^" (string/replace asset-path #"^/" "") "/")) "")
      (str "/" rel-path))
    (.getCanonicalPath (io/file tgt-path rel-path))))

(defn send-visual! [messages]
  (doseq [[id {:keys [channel]}] @state]
    (http/send! channel (pr-str [:visual messages]))))

(defn send-changed! [tgt-path asset-path changed]
  (doseq [[id {:keys [protocol channel]}] @state]
    (http/send! channel
      (pr-str (into [:reload] (map #(web-path protocol % tgt-path asset-path) changed))))))

(defmulti handle-message (fn [_ channel message] (:type message)))

(defmethod handle-message :set-protocol [_ channel {:keys [protocol]}]
  (doseq [[id {c :channel}] @state]
    (when (= c channel) (swap! state assoc-in [id :protocol] protocol))))

(defmethod handle-message :open-file [{:keys [open-file]} channel {:keys [file line column]}]
  (when open-file
    (let [cmd (format open-file (or line 0) (or column 0) (or file ""))]
      (util/dbug "Open-file call: %s\n" cmd)
      (try
        (.exec (Runtime/getRuntime) cmd)
        (catch Exception e
          (util/fail "There was a problem running open-file command: %s\n" cmd))))))

(defn connect! [opts channel]
  (let [id (gensym)]
    (swap! state assoc id {:channel channel})
    (http/on-close channel (fn [_] (swap! state dissoc id)))
    (http/on-receive channel #(handle-message opts channel (read-string %)))))

(defn handler [opts request]
  (if-not (:websocket? request)
    {:status 501 :body "Websocket connections only."}
    (http/with-channel request channel (connect! opts channel))))

(defn start
  [{:keys [ip port] :as opts}]
  (let [o {:ip (or ip "0.0.0.0") :port (or port 0)}]
    (assoc o :port (-> (http/run-server (partial handler opts) o) meta :local-port))))
