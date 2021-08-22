(ns xyz.thoren.bibcal
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.tools.logging :as log]
            [clj-logging-config.log4j]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [tick.core :as tick]
            [table.core :refer [table]]
            [say-cheez.core :refer [current-build-env]]
            [xyz.thoren.luminary :as l])
  (:gen-class))

(defn set-default-root-logger!
  [loglevel pattern]
  (clj-logging-config.log4j/set-loggers! :root
                                         {:level loglevel
                                          :pattern pattern
                                          :out :console}))

(set-default-root-logger! :fatal "%p: %m%n")

(def build-env (current-build-env))

(defn set-log-level!
  "Set the output level based on `verbosity`.
  See also [[set-default-root-logger!]]."
  [verbosity]
  (case verbosity
    0 nil
    1 (set-default-root-logger! :info "%m%n")
    2 (set-default-root-logger! :debug "[%p] %m%n")
    (set-default-root-logger! :trace "[%p] %m%n")))

(def version-number
  "The version number as defined in project.clj."
  ;; Note that this is evaluated at build time by native-image.
  (:version build-env))

(def exit-messages
  "Exit messages used by `exit`."
  {:64 "Placeholder message."})

(defn exit
  "Print a `message` and exit the program with the given `status` code.
  See also [[exit-messages]]."
  [status message]
  (println message)
  (System/exit status))

(defn- valid-zone?
  [s]
  (log/debug "Validating zone:" s)
  (l/valid-zone? s))

(defn- config-file
  []
  (let [os (System/getProperty "os.name")
        home (System/getProperty "user.home")]
    (if (str/starts-with? os "Windows")
      (str home "\\AppData\\Roaming\\bibcal\\config.edn")
      (str home "/.config/bibcal/config.edn"))))


(defn- read-config
  ([k]
   (try
     (let [config (edn/read-string (slurp (config-file)))]
       (get config k))
     (catch java.io.FileNotFoundException _e nil)))
  ([]
   (try
     (edn/read-string (slurp (config-file)))
     (catch java.io.FileNotFoundException _e nil))))

(defn print-feast-days-in-year
  [y]
  (if (l/feast-days y)
    (doseq [d (l/list-of-feast-days-in-year y)]
      (println d))
    (do
      (println (str "Calculating feast days in " y ". Please wait..."))
      (let [days (l/list-of-feast-days-in-year y)]
        (doseq [d days]
          (println d))))))

(defn sabbath?
  ([lat lon z date]
   (let [h (l/date lat lon (l/in-zone z date))
         s (get-in h [:hebrew :sabbath])]
     (log/trace "Checking Sabbath for the following hebrew date:" h)
     (if s true false)))
  ([lat lon date]
   (sabbath? lat lon (str (tick/zone date)) date))
  ([lat lon]
   (sabbath? lat lon (l/now))))

(defn exit-with-sabbath
  [s]
  (log/info (if s "It's Sabbath!" "It's not Sabbath."))
  (System/exit (if s 0 1)))

(defn- feast-day-name
  [n day-of-feast days-in-feast]
  (if (< days-in-feast 3)
    n
    (if (= days-in-feast 8)
      (str (l/day-numbers (dec day-of-feast)) " day of " n)
      (str (l/day-numbers (dec day-of-feast)) " day of the " n))))

(defn- feast-or-false
  [feast]
  (if feast
    (feast-day-name (:name feast) (:day-of-feast feast) (:days-in-feast feast))
    false))

(defn print-date
  [lat lon time]
  (let [d (l/date lat lon time)
        h (:hebrew d)
        n (:names h)
        t (:time d)
        tf (tick/formatter "yyy-MM-dd HH:MM")]
    (print (table
            [{"Key" "Configuration file"
              "Value" (when (read-config) (config-file))}
             {"Key" "Current location" "Value" (str lat "," lon)}
             {"Key" "Current timezone" "Value" (str (tick/zone time))}
             {"Key" "Month" "Value" (:month-of-year h)}
             {"Key" "Day of month" "Value" (:day-of-month h)}
             {"Key" "Day of week" "Value" (:day-of-week h)}
             {"Key" "Sabbath" "Value" (:sabbath h)}
             {"Key" "Major feast day"
              "Value" (feast-or-false (:major-feast-day h))}
             {"Key" "Minor feast day"
              "Value" (feast-or-false (:minor-feast-day h))}
             {"Key" "Start of current day"
              "Value" (tick/format tf (get-in t [:day :start]))}
             {"Key" "End of current day"
              "Value" (tick/format tf (get-in t [:day :end]))}
             {"Key" "Start of current week"
              "Value" (tick/format tf (get-in t [:week :start]))}
             {"Key" "End of current week"
              "Value" (tick/format tf (get-in t [:week :end]))}
             {"Key" "Start of current month"
              "Value" (tick/format tf (get-in t [:month :start]))}
             {"Key" "End of current month"
              "Value" (tick/format tf (get-in t [:month :end]))}
             {"Key" "Start of current year"
              "Value" (tick/format tf (get-in t [:year :start]))}
             {"Key" "End of current year"
              "Value" (tick/format tf (get-in t [:year :end]))}]))))

;; Beginning of command line parsing.

(defn- cli-options
  ;; First three strings describe a short-option, long-option with optional
  ;; example argument description, and a description. All three are optional
  ;; and positional.
  []
  (let [config (read-config)]
    [["-f" "--calculate-feast-days YEAR"
      "Calculate and print a list of feast days in a gregorian YEAR"
      :parse-fn #(read-string %)
      :validate [#(and (int? %) (<= 1584 % 2100))
                 #(str % " is not an integer between 1584 and 2100")]
      :id :year-to-calculate-feast-days]
     ["-h" "--help"
      "Print this help message."
      :default false]
     ["-s" "--check-sabbath"
      "Check Sabbath status. Silent by default."
      :default false]
     ["-v" nil
      "Verbosity level; specify multiple times to increase value."
      :id :verbosity
      :default 0
      :update-fn inc]
     ["-V" "--version"
      "Print the current version number."
      :default false]
     ["-x" "--longitude NUMBER"
      "The longitude of the location."
      :parse-fn #(read-string %)
      :default (or (:longitude config)
                   l/jerusalem-lon)
      :validate [#(and (number? %) (<= -180 % 180))
                 #(str % " is not a number between -180 and 180.")]]
     ["-y" "--latitude NUMBER"
      "The latitude of the location."
      :parse-fn #(read-string %)
      :default (or (:latitude config)
                   l/jerusalem-lat)
      :validate [#(and (number? %) (<= -90 % 90))
                 #(str % " is not a number between -90 and 90.")]]
     ["-z" "--timezone STRING"
      "The timezone of the location."
      :default (or (:timezone config)
                   "Asia/Jerusalem")
      :validate [#(valid-zone? %)
                 #(str % " is not a valid zone id string")]]]))

(defn usage
  "Print a brief description and a short list of available options."
  [options-summary]
  (str/join
   \newline
   ["bibcal: A command-line tool for calculating dates based on the"
    "        Bible and the 1st Book of Enoch."
    ""
    (str "Version: " version-number)
    ""
    "Usage: bibcal [options]"
    ""
    "Options (short, long, [type], [default], description):"
    options-summary]))
    ;; ""]))
    ;; "Example output:"
    ;; "FIXME: Examples..."]))

(defn validate-args
  [args]
  (let [{:keys [options arguments errors summary]}
        (parse-opts args (cli-options))]
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}
      (:version options) ; version => exit OK with version number
      {:exit-message version-number :ok? true}
      errors ; errors => exit with description of errors
      {:exit-message (str/join \newline errors)}
      :else
      (select-keys options [:latitude :longitude :check-sabbath :timezone
                            :verbosity :year-to-calculate-feast-days]))))

;; End of command line parsing.

(defn -main [& args]
  (let [{:keys [latitude longitude check-sabbath timezone verbosity
                year-to-calculate-feast-days exit-message ok?]}
        (validate-args args)]
    (when exit-message
      (exit (if ok? 0 1) exit-message))
    (set-log-level! verbosity)
    (log/debug "Configuration file:" (if (read-config) (config-file) "None"))
    (log/debug "Latitude:" latitude)
    (log/debug "Longitude:" longitude)
    (log/debug "TimeZone:" timezone)
    (cond
      check-sabbath
      (exit-with-sabbath (sabbath? latitude longitude timezone (l/now)))
      ;;
      year-to-calculate-feast-days
      (print-feast-days-in-year year-to-calculate-feast-days)
      ;;
      :else (print-date latitude longitude (l/in-zone timezone (l/now)))))
  (System/exit 0))
