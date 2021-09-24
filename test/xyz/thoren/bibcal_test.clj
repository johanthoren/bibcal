(ns xyz.thoren.bibcal-test
  (:require [clojure.test :refer [deftest is testing]]
            [xyz.thoren.luminary :as l]
            [xyz.thoren.bibcal :as b]
            [clojure.string :as str]))

(deftest test-valid-zone
  (testing "that strings are correctly validated"
    (let [r #(#'xyz.thoren.bibcal/valid-zone? %)]
      (is (true? (r "UTC")))
      (is (true? (r "Europe/Stockholm")))
      (is (false? (r "Europe Stockholm")))
      (is (false? (r "Foo/Bar"))))))

(deftest test-print-brief-date
  (testing "that the expected output is printed for"
    (let [r #(with-out-str (as-> (l/zdt l/jerusalem-zone %1 %2 %3 %4 %5) <>
                                 (b/print-brief-date l/jerusalem-lat
                                                     l/jerusalem-lon
                                                     <>
                                                    :year %6
                                                    :trad-year %7)))]
      (testing "2021-09-09T23:08"
        (testing "without :year or :trad-year"
          (let [t (r 2021 9 9 23 8 nil nil)]
            (is (= "3rd of Elul\n" t))))
        (testing "with :year"
          (let [t (r 2021 9 9 23 8 true nil)]
            (is (= "3rd of Elul, 6021\n" t))))
        (testing "without :year or :trad-year"
          (let [t (r 2021 9 9 23 8 nil true)]
            (is (= "3rd of Elul, 5781\n" t))))))))

(deftest test-print-date
  (testing "that the expected output is printed for"
    (let [r #(->> (l/zdt l/jerusalem-zone %1 %2 %3 %4 %5)
                  (b/print-date l/jerusalem-lat l/jerusalem-lon)
                  (with-out-str)
                  (str/split-lines)
                  (drop-last))]
      (testing "2021-09-09T23:08"
        (let [t (r 2021 9 9 23 8)]
          (is (= t ["Gregorian time          2021-09-09 23:08:00"
                    "Date                    3rd day of the 6th month"
                    "ISO date                6021-06-03"
                    "Traditional date        3rd of Elul"
                    "Traditional ISO date    5781-06-03"
                    "Day of week             6"
                    "Sabbath                 false"
                    "Major feast day         false"
                    "Minor feast day         false"
                    "Start of year           2021-04-12 19:06:00"
                    "Start of month          2021-09-07 18:55:00"
                    "Start of week           2021-09-04 18:59:00"
                    "Start of day            2021-09-09 18:52:00"
                    "End of day              2021-09-10 18:50:59"
                    "End of week             2021-09-11 18:49:59"
                    "End of month            2021-10-06 18:16:59"
                    "End of year             2022-04-01 18:57:59"
                    "Coordinates             31.7781161,35.233804"
                    "Timezone                Asia/Jerusalem"])))))))

(deftest test-config
  (testing "that the correct config is produced"
    (let [r #(b/config %)
          t #(is (= %2 (r %1)))]
      (testing "with all nil vals"
        (t {:lat nil :lon nil :zone nil} nil))
      (testing "with no nil vals"
        (t {:lat 1 :lon 2 :zone "foo"} {:lat 1 :lon 2 :zone "foo"}))
      (testing "with some invalid keys"
        (t {:lat 1 :lon 2 :zone "foo" :foo "bar"} {:lat 1 :lon 2 :zone "foo"}))
      (testing "with all invalid keys"
        (t {:baz "foo" :foo "bar"} nil)))))

(deftest test-sabbath?
  (testing "that the sabbath is correctly"
    (testing "true"
      (is (true? (b/sabbath? l/jerusalem-lat
                             l/jerusalem-lon
                             l/jerusalem-zone
                             (l/zdt l/jerusalem-zone 2021 9 25 12 0)))))
    (testing "false"
      (is (false? (b/sabbath? l/jerusalem-lat
                              l/jerusalem-lon
                              l/jerusalem-zone
                              (l/zdt l/jerusalem-zone 2021 9 23 12 0)))))))
