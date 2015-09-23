(ns adzerk.boot-reload.warn
  (:require [clojure.string :as string]
            [goog.dom :as dom]))

(defn ->css [m]
  (let [attrs (map name (keys m))]
    (apply str (interleave attrs (repeat ":") (vals m) (repeat ";")))))

(defn mk-node
  ([type] (mk-node type {} nil))
  ([type attrs] (mk-node type attrs nil))
  ([type attrs & content]
   (dom/createDom (name type) (clj->js attrs) (clj->js content))))

(defn styles [type]
  {:style (->css (type {:heading {:margin 0, :padding 0}
                        :warning-short {:margin-right "10px"}
                        :warning-detail {:margin-right "10px"}
                        :error   {}
                        :container {:background-color "yellow"
                                    :color "black"
                                    :font-family "sans-serif"
                                    :position "fixed"
                                    :padding "8px"
                                    :left "0px"
                                    :right "0px"
                                    :bottom "0px"}}))})

(defn parse-warning [warning]
  (let [segs           (map string/trim (string/split warning #"at line"))
        [line-no file] (string/split (second segs) #" " 2)
        warning        {:warning-txt (first segs)
                        :line-number line-no
                        :file        file}]
    warning))

(defn construct-warning-node [warning]
  (let [w (parse-warning warning)]
    (mk-node :div nil
             (mk-node :span (styles :warning-short) (:warning-txt w))
             (mk-node :span (styles :warning-detail) (str "at line " (:line-number w)))
             (mk-node :span (styles :warning-detail) (:file w)))))

(defn construct-exception-node [ex]
  (mk-node :div nil ex))

(defn construct-hud-node [id {:keys [warnings exception]}]
  (js/console.log exception)
  (let [node (mk-node :div (merge (styles :container) {:id id}))]
    (dom/append node (mk-node :h4 (styles :heading) "Compiler Warnings"))
    (if exception
      (dom/append node (construct-warning-node exception))
      (apply dom/append node (map construct-warning-node warnings)))
    node))

(defonce current-container (atom))

(defn gen-id []
  (str "boot-reload-hud-" (name (gensym))))

(defn warn [warnings opts]
  (let [id (gen-id)]
    (dom/removeNode (dom/getElement @current-container))
    (dom/appendChild js/document.body (construct-hud-node id warnings))
    (reset! current-container id)))
