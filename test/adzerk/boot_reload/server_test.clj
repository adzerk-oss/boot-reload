(ns adzerk.boot-reload.server-test
  (:require [clojure.test :refer :all]
            [adzerk.boot-reload.server :refer :all]))

(deftest web-path-test
  (testing "basic"
    (is (= "/js/out/saapas/core.js"
           (web-path {:protocol "http:" :target-path "target"} "js/out/saapas/core.js"))))

  (testing "asset-path"
    (is (= "/js/out/saapas/core.js"
           (web-path {:protocol "http:" :asset-path "public"} "public/js/out/saapas/core.js")))

    (is (= "/js/out/saapas/core.js"
           (web-path {:protocol "http:" :asset-path "/public"} "public/js/out/saapas/core.js")))

    (is (= "/public/js/out/saapas/core.js"
           (web-path {:protocol "http:" :asset-path "foobar"} "public/js/out/saapas/core.js"))))

  (testing "cljs-asset-path"
    (is (= "js/saapas.out/saapas/core.js"
           (web-path {:protocol "http:"
                      :asset-path "resources/public/js/saapas.out"
                      :cljs-asset-path "js/saapas.out"}
                     "resources/public/js/saapas.out/saapas/core.js"))))

  (testing "windows style paths"
    (is (= "js/saapas.out/saapas/core.js"
           (web-path {:protocol "http:"
                      :asset-path "resources/public/js/saapas.out"
                      :cljs-asset-path "js/saapas.out"}
                     "resources\\public\\js\\saapas.out\\saapas\\core.js")))))
