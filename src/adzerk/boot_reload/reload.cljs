(ns adzerk.boot-reload.reload
  (:require
   [clojure.string :as string]
   [goog.Uri       :as guri]))

(def ^:private page-uri (goog.Uri. (.. js/window -location -href)))

(defn- changed-href? [href changed]
  (when href
    (let [uri (goog.Uri. href)]
      (when (contains? changed (.getPath (.resolve page-uri uri)))
        uri))))

(defn- ends-with? [s pat]
  (= pat (subs s (- (count s) (count pat)))))

(defn- reload-css [changed]
  (let [sheets (.. js/document -styleSheets)]
    (doseq [s (range 0 (.-length sheets))]
      (when-let [sheet (aget sheets s)]
        (when-let [href (changed-href? (.-href sheet) changed)]
          (set! (.. sheet -ownerNode -href) (.toString (.makeUnique href))))))))

(defn- reload-js [changed]
  (when (some #(ends-with? % ".js") changed)
    (.reload (.-location js/window))))

(defn reload [changed]
  (doto changed reload-js reload-css))
