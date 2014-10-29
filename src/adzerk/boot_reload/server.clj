(ns adzerk.boot-reload.server
  (:require
   [clojure.java.io    :as io]
   [boot.util          :as util]
   [org.httpkit.server :as http])
  (:import
   [java.io IOException]))

(def state (atom {}))

(defn web-path [proto rel-path tgt-path]
  (if-not (= "file:" proto)
    (str "/" rel-path)
    (.getCanonicalPath (io/file tgt-path rel-path))))

(defn send-changed! [tgt-path changed]
  (doseq [[id {:keys [proto channel]}] @state]
    (http/send! channel
      (pr-str (into #{} (map #(web-path proto % tgt-path) changed))))))

(defn set-proto! [channel proto]
  (doseq [[id {c :channel}] @state]
    (when (= c channel) (swap! state assoc-in [id :proto] proto))))

(defn connect! [channel]
  (let [id (gensym)]
    (swap! state assoc id {:channel channel})
    (http/on-close channel (fn [_] (swap! state dissoc id)))
    (http/on-receive channel #(set-proto! channel (read-string %)))))

(defn handler [request]
  (if-not (:websocket? request)
    {:status 501 :body "Websocket connections only."}
    (http/with-channel request channel (connect! channel))))

(defn start
  [{:keys [ip port] :as opts}]
  (let [o {:ip (or ip "0.0.0.0" :port (or port 0))}]
    (assoc o :port (-> (http/run-server handler o) meta :local-port))))
