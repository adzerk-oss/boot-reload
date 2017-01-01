(ns adzerk.boot-reload.util-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [adzerk.boot-reload.util :as util]))

(deftest path->ns-test
  (is (= "foo.bar" (util/path->ns "foo/bar.clj")))
  (is (= "foo-bar" (util/path->ns "foo_bar.clj")))
  (is (= "foo-bar" (util/path->ns "foo-bar.clj")))

  (is (= "foo.bar" (util/path->ns "foo/bar.cljs.edn")))
  (is (= "foo.bar" (util/path->ns "foo\\bar.cljs.edn")))
  )

(deftest ns->file-test
  (is (= (io/file "foo" "bar.cljs") (util/ns->file "foo.bar" "cljs")))
  (is (= (io/file "foo_bar.cljs") (util/ns->file "foo-bar" "cljs")))
  )
