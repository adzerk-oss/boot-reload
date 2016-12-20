(ns adzerk.boot-reload.server-test
  (:require [clojure.test :refer :all]
            [adzerk.boot-reload.server :refer :all]
            [clojure.java.io :as io]))

(deftest web-path-test
  (testing "basic"
    (is (= {:web-path "/js/out/saapas/core.js"
            :canonical-path (.getCanonicalPath (io/file "target" "js" "out" "saapas" "core.js"))}
           (web-path {:target-path "target"} "js/out/saapas/core.js"))))

  (testing "asset-path"
    (is (= {:web-path "/js/out/saapas/core.js"
            :canonical-path (.getCanonicalPath (io/file "target" "public" "js" "out" "saapas" "core.js"))}
           (web-path {:asset-path "public"
                      :target-path "target"}
                     "public/js/out/saapas/core.js")))

    (is (= {:web-path "/js/out/saapas/core.js"
            :canonical-path (.getCanonicalPath (io/file "target" "public" "js" "out" "saapas" "core.js"))}
           (web-path {:asset-path "/public"
                      :target-path "target"}
                     "public/js/out/saapas/core.js")))

    (is (= {:web-path "/public/js/out/saapas/core.js"
            :canonical-path (.getCanonicalPath (io/file "target" "public" "js" "out" "saapas" "core.js"))}
           (web-path {:asset-path "foobar"
                      :target-path "target"}
                     "public/js/out/saapas/core.js"))))

  (testing "cljs-asset-path"
    (is (= {:web-path "js/saapas.out/saapas/core.js"
            :canonical-path (.getCanonicalPath (io/file "target" "resources" "public" "js" "saapas.out" "saapas" "core.js"))}
           (web-path {:asset-path "resources/public/js/saapas.out"
                      :cljs-asset-path "js/saapas.out"
                      :target-path "target"}
                     "resources/public/js/saapas.out/saapas/core.js"))))

  (testing "windows style paths"
    (is (= {:web-path "js/saapas.out/saapas/core.js"
            :canonical-path (.getCanonicalPath (io/file "target" "resources" "public" "js" "saapas.out" "saapas" "core.js"))}
           (web-path {:asset-path "resources/public/js/saapas.out"
                      :cljs-asset-path "js/saapas.out"
                      :target-path "target"}
                     "resources\\public\\js\\saapas.out\\saapas\\core.js")))))
