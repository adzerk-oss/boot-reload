(set-env!
  :source-paths #{"test"}
  :dependencies '[[org.clojure/clojure "1.7.0"     :scope "provided"]
                  [boot/core           "2.5.5"     :scope "provided"]
                  [http-kit            "2.1.19"    :scope "test"]])


(def +version+ "0.4.3-SNAPSHOT")

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

(deftask deploy []
  (comp
   (build)
   (push :repo "clojars" :gpg-sign (not (.endsWith +version+ "-SNAPSHOT")))))
