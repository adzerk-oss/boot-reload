(ns adzerk.boot-reload.server-test
  (:require [clojure.test :refer :all]
            [adzerk.boot-reload.server :refer :all]))

(deftest web-path-test
  (testing "Basic"
    (is (= "js/out/saapas/core.js"
           (web-path {:protocol "http:" :target-path "target"} "js/out/saapas/core.js"))))

  (testing "Asset-path"
    (is (= "js/out/saapas/core.js"
           (web-path {:protocol "http:" :asset-path "public"} "public/js/out/saapas/core.js")))

    (is (= "js/out/saapas/core.js"
           (web-path {:protocol "http:" :asset-path "/public"} "public/js/out/saapas/core.js")))

    (is (= "public/js/out/saapas/core.js"
           (web-path {:protocol "http:" :asset-path "foobar"} "public/js/out/saapas/core.js"))))

  (testing "Cljs-asset-path"
    (is (= "js/saapas.out/saapas/core.js"
           (web-path {:protocol "http:"
                      :asset-path "resources/public/js/saapas.out"
                      :cljs-asset-path "js/saapas.out"}
                     "resources/public/js/saapas.out/saapas/core.js")))))
