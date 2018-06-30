(set-env!
  :resource-paths #{"src"}
  :source-paths #{"test"}
  :dependencies '[[org.clojure/clojure "1.9.0" :scope "provided"]
                  [http-kit "2.3.0" :scope "test"]
                  [metosin/bat-test "0.4.0" :scope "test"]])

(require '[metosin.bat-test :refer [bat-test]])

(def +version+ "0.6.0-SNAPSHOT")

(task-options!
  pom {:project     'adzerk/boot-reload
       :version     +version+
       :description "Boot task to automatically reload page resources in the browser."
       :url         "https://github.com/adzerk/boot-reload"
       :scm         {:url "https://github.com/adzerk/boot-reload"}
       :license     {"Eclipse Public License"
                     "http://www.eclipse.org/legal/epl-v10.html"}})

(deftask build []
  (comp
   (pom)
   (jar)
   (install)))

(deftask run-tests []
  (comp
    (bat-test)))

(deftask dev []
  (comp
   (watch)
   (repl :server true)
   (run-tests)
   (build)
   (target)))

(deftask deploy []
  (comp
    (run-tests)
    (build)
    (push :repo "clojars" :gpg-sign (not (.endsWith +version+ "-SNAPSHOT")))))
