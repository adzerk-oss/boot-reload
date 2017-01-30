(ns adzerk.boot-reload.util
  (:require [clojure.string :as string]
            [clojure.java.io :as io]))

;; From cljs/analyzer.cljc
(def js-reserved
  #{"arguments" "abstract" "boolean" "break" "byte" "case"
    "catch" "char" "class" "const" "continue"
    "debugger" "default" "delete" "do" "double"
    "else" "enum" "export" "extends" "final"
    "finally" "float" "for" "function" "goto" "if"
    "implements" "import" "in" "instanceof" "int"
    "interface" "let" "long" "native" "new"
    "package" "private" "protected" "public"
    "return" "short" "static" "super" "switch"
    "synchronized" "this" "throw" "throws"
    "transient" "try" "typeof" "var" "void"
    "volatile" "while" "with" "yield" "methods"
    "null" "constructor"})

(defn path->ns
  [path]
  (let [parts (-> path
                  (string/replace #"\..+$" "")
                  (string/replace #"_" "-")
                  (string/replace #"[/\\]" ".")
                  (string/split #"\."))]
    (->> parts
         (map #(if (js-reserved %) (str % "$") %))
         (string/join "."))))

(defn ns->file
  ([ns-name ext]
   (let [ns-name (string/replace ns-name #"-" "_")
         parts (string/split ns-name #"\.")]
     (apply io/file (conj (vec (butlast parts)) (str (last parts) "." ext)))))
  ([parent ns-name ext]
   (let [ns-name (string/replace ns-name #"-" "_")
         parts (string/split ns-name #"\.")]
     (apply io/file parent (conj (vec (butlast parts)) (str (last parts) "." ext))))))
