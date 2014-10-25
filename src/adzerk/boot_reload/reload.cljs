(ns adzerk.boot-reload.reload
  (:require
   [clojure.string :as string]
   [goog.Uri       :as guri]))

(def page-uri (goog.Uri. (.. js/window -location -href)))

(defn reload-css [changed]
  (let [sheets (.. js/document -styleSheets)]
    (doseq [s (range 0 (.-length sheets))]
      (when-let [sheet (aget sheets s)]
        (when-let [href (.-href sheet)]
          (let [uri (.resolve page-uri (goog.Uri. href))]
            (when (contains? changed (.getPath uri))
              (set! (.. sheet -ownerNode -href)
                (.toString (.makeUnique (goog.Uri. href)))))))))))
