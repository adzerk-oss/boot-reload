(ns adzerk.boot-reload.server
  (:require
   [clojure.java.io    :as io]
   [boot.util          :as util]
   [org.httpkit.server :as http])
  (:import
   [java.io IOException]))

(def state (atom {:channel nil :proto nil}))

(defn web-path [proto rel-path tgt-path]
  (if-not (= "file:" (:proto @state))
    (str "/" rel-path)
    (.getCanonicalPath (io/file tgt-path rel-path))))

(defn send-changed! [tgt-path changed]
  (let [{:keys [proto channel]} @state]
    (when (and channel proto)
      (http/send! channel
        (pr-str (into #{} (map #(web-path proto % tgt-path) changed)))))))

(defn occupied! [channel]
  (http/send! channel (pr-str :occupied))
  (http/close channel))

(defn connect! [channel]
  (swap! state assoc :channel channel)
  (http/on-close channel (fn [_] (swap! state assoc :channel nil)))
  (http/on-receive channel #(swap! state assoc :proto (read-string %))))

(defn handler [request]
  (if-not (:websocket? request)
    {:status 501 :body "Websocket connections only."}
    (http/with-channel request channel
      (if (:channel @state) (occupied! channel) (connect! channel)))))

(defn start
  [& {:keys [ip port] :as opts}]
  (let [o (merge {:ip "0.0.0.0" :port 0} opts)]
    (assoc o :port (-> (http/run-server handler o) meta :local-port))))
