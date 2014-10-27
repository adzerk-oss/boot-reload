(set-env!
  :dependencies '[[org.clojure/clojure       "1.6.0"       :scope "provided"]
                  [boot/core                 "2.0.0-pre11" :scope "provided"]
                  [tailrecursion/boot-useful "0.1.3"       :scope "test"]
                  [http-kit                  "2.1.18"      :scope "test"]])

(require '[tailrecursion.boot-useful :refer :all])

(def +version+ "0.1.1")

(useful! +version+)

(task-options!
  pom  [:project     'adzerk/boot-reload
        :version     +version+
        :description "Boot task to automatically reload page resources in the browser."
        :url         "https://github.com/adzerk/boot-reload"
        :scm         {:url "https://github.com/adzerk/boot-reload"}
        :license     {:name "Eclipse Public License"
                      :url  "http://www.eclipse.org/legal/epl-v10.html"}])
