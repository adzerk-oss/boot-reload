(ns adzerk.boot-reload.reload
  (:require
   [clojure.string :as string]
   [goog.Uri       :as guri]))

(def ^:private page-uri (goog.Uri. (.. js/window -location -href)))

(defn- reload-page! []
  (.reload (.-location js/window)))

(defn- changed-href? [href-or-uri changed]
  (when href-or-uri
    (let [uri (goog.Uri. href-or-uri)]
      (when (contains? changed (.getPath (.resolve page-uri uri)))
        uri))))

(defn- ends-with? [s pat]
  (= pat (subs s (- (count s) (count pat)))))

(defn- reload-css [changed]
  (let [sheets (.. js/document -styleSheets)]
    (doseq [s (range 0 (.-length sheets))]
      (when-let [sheet (aget sheets s)]
        (when-let [href-uri (changed-href? (.-href sheet) changed)]
          (set! (.. sheet -ownerNode -href) (.toString (.makeUnique href-uri))))))))

(defn- reload-img [changed]
  (let [images (.. js/document -images)]
    (doseq [s (range 0 (.-length images))]
      (when-let [image (aget images s)]
        (when-let [href-uri (changed-href? (.-src image) changed)]
          (set! (.-src image) (.toString (.makeUnique href-uri))))))))

(defn- reload-js [changed]
  (when (some #(ends-with? % ".js") changed) (reload-page!)))

(defn- reload-html [changed]
  (when (changed-href? page-uri changed) (reload-page!)))

(defn reload [changed]
  (doto changed reload-js reload-html reload-css reload-img))
