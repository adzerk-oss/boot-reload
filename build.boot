(set-env!
  :source-paths #{"test"}
  :dependencies '[[org.clojure/clojure "1.7.0"     :scope "provided"]
                  [boot/core           "2.5.5"     :scope "provided"]
                  [adzerk/bootlaces    "0.1.13"    :scope "test"]
                  [http-kit            "2.1.19"    :scope "test"]])

(require '[adzerk.bootlaces :refer :all])

(def +version+ "0.4.3-SNAPSHOT")

(bootlaces! +version+)

(task-options!
  pom {:project     'adzerk/boot-reload
       :version     +version+
       :description "Boot task to automatically reload page resources in the browser."
       :url         "https://github.com/adzerk/boot-reload"
       :scm         {:url "https://github.com/adzerk/boot-reload"}
       :license     {"Eclipse Public License"
                     "http://www.eclipse.org/legal/epl-v10.html"}})
