(ns adzerk.boot-reload.client
  (:require
   [adzerk.boot-reload.websocket :as ws]
   [adzerk.boot-reload.reload    :as rl]
   [adzerk.boot-reload.warn      :as w]
   [clojure.browser.net          :as net]
   [clojure.browser.event        :as event]
   [cljs.reader                  :as reader]
   [goog.net.jsloader            :as jsloader]))

(def ^:private ws-conn (atom nil))

(defn alive? [] (not (nil? @ws-conn)))

;; Thanks, lein-figwheel & lively!
(defn patch-goog-base! []
  (set! (.-provide js/goog) (.-exportPath_ js/goog))
  (set! (.-CLOSURE_IMPORT_SCRIPT (.-global js/goog)) (fn [file]
                                                       (when (.inHtmlDocument_ js/goog)
                                                         (jsloader/load file)))))

(defmulti handle
  (fn [msg opts] (first msg)))

(defmethod handle :reload
  [msg opts]
  (rl/reload (rest msg) opts))

(defmethod handle :display
  [msg opts]
  (js/console.log (pr-str (first (rest msg))))
  (w/warn (first (rest msg)) opts))

(defn connect [url & [opts]]
  (let [conn (ws/websocket-connection)]
    (patch-goog-base!)
    (reset! ws-conn conn)

    (event/listen conn :opened
      (fn [evt]
        (net/transmit conn (pr-str (.. js/window -location -protocol)))
        (.info js/console "Reload websocket connected.")))

    (event/listen conn :message
      (fn [evt]
        (let [msg (reader/read-string (.-message evt))]
          (when (vector? msg) (handle msg opts)))))

    (event/listen conn :closed
      (fn [evt]
        (reset! ws-conn nil)
        (.info js/console "Reload websocket connection closed.")))

    (event/listen conn :error
      (fn [evt]
        (.error js/console "Reload websocket error:" evt)))

    (net/connect conn url)))
