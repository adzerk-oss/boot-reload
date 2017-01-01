(ns adzerk.boot-reload.util
  (:require [clojure.string :as string]
            [clojure.java.io :as io]))

(defn path->ns
  [path]
  (-> path
      (string/replace #"\..+$" "")
      (string/replace #"_" "-")
      (string/replace #"[/\\]" ".")))

(defn ns->file
  ([ns-name ext]
   (let [ns-name (string/replace ns-name #"-" "_")
         parts (string/split ns-name #"\.")]
     (apply io/file (conj (vec (butlast parts)) (str (last parts) "." ext)))))
  ([parent ns-name ext]
   (let [ns-name (string/replace ns-name #"-" "_")
         parts (string/split ns-name #"\.")]
     (apply io/file parent (conj (vec (butlast parts)) (str (last parts) "." ext))))))
