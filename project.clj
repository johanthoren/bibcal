(defproject xyz.thoren/bibcal "0.5.8-SNAPSHOT"
  :description (str "Calculate dates based on the Bible and the "
                    "1st Book of Enoch.")
  :url "https://github.com/johanthoren/bibcal"
  :license {:name "ISC"
            :url "https://choosealicense.com/licenses/isc"
            :comment "ISC License"
            :year 2021
            :key "isc"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [org.clojure/tools.cli "1.0.206"]
                 [org.clojure/tools.logging "1.1.0"]
                 [clj-logging-config "1.9.12"]
                 [say-cheez "0.2.0"]
                 [tick "0.5.0-RC1"]
                 [clj-commons/fs "1.6.307"]
                 [xyz.thoren/luminary "0.7.2"]]
  :plugins [[lein-kibit "0.1.8"]
            [jonase/eastwood "0.9.9"]]
  :main xyz.thoren.bibcal
  :target-path "target/%s"
  :aliases
  {"lint"
   ["do" ["kibit"] ["eastwood"]]
   "make-uberjars"
   ["do" ["test"] ["clean"] ["uberjar"]]}
  :release-tasks [["lint"]
                  ["test"]
                  ["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]])
