(ns xyz.thoren.bibcal-test
  (:require [clojure.test :refer [deftest is testing]]
            [xyz.thoren.bibcal :as b]))

(deftest test-valid-zone
  (testing "that strings are correctly validated"
    (let [r #(#'xyz.thoren.bibcal/valid-zone? %)]
      (is (true? (r "UTC")))
      (is (true? (r "Europe/Stockholm")))
      (is (false? (r "Europe Stockholm")))
      (is (false? (r "Foo/Bar"))))))
