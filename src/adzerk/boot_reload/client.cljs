(ns adzerk.boot-reload.client
  (:require
   [adzerk.boot-reload.websocket :as ws]
   [adzerk.boot-reload.reload    :as rl]
   [clojure.browser.net          :as net]
   [clojure.browser.event        :as event]
   [cljs.reader                  :as reader]))

(def ^:private ws-conn (atom nil))

(defn alive? [] (not (nil? @ws-conn)))

(defn connect [url]
  (let [conn (ws/websocket-connection)]
    (reset! ws-conn conn)

    (event/listen conn :opened
      (fn [evt]
        (net/transmit conn (pr-str (.. js/window -location -protocol)))
        (.info js/console "Reload websocket connected.")))

    (event/listen conn :message
      (fn [evt]
        (let [msg (reader/read-string (.-message evt))]
          (when (set? msg) (rl/reload msg)))))

    (event/listen conn :closed
      (fn [evt]
        (reset! ws-conn nil)
        (.info js/console "Reload websocket connection closed.")))

    (event/listen conn :error
      (fn [evt]
        (.error js/console "Reload websocket error:" evt)))

    (net/connect conn url)))
