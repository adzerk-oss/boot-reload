(ns adzerk.boot-reload.server-test
  (:require [clojure.test :refer :all]
            [adzerk.boot-reload.server :refer :all]))

(deftest web-path-test
  (testing "Basic"
    (is (= "/js/out/saapas/core.js"
           (web-path "http:" "js/out/saapas/core.js" "target" nil))))

  (testing "Asset-path"
    (is (= "js/out/saapas/core.js"
           (web-path "http:" "public/js/out/saapas/core.js" nil "public")))

    (is (= "public/js/out/saapas/core.js"
           (web-path "http:" "public/js/out/saapas/core.js" nil "foobar")))
    ))
