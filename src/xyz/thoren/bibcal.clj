(ns xyz.thoren.bibcal
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.tools.logging :as log]
            [clj-logging-config.log4j]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [me.raynes.fs :as fs]
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

(defn- config-dir
  []
  (let [os (System/getProperty "os.name")
        home (System/getProperty "user.home")]
    (if (str/starts-with? os "Windows")
      (str home "\\AppData\\Roaming\\bibcal\\")
      (str home "/.config/bibcal/"))))

(defn- config-file
  []
  (str (config-dir) "config.edn"))

(def exit-messages
  "Exit messages used by `exit`."
  {:64 "The configuration file already exists. Use -F to overwrite."
   :65 (str "Something went wrong while validating the saved configuration. "
            "Inspect the file " (config-file) " for more details.")
   :66 (str "--latitude and --longitude are both needed, either as options or "
            "saved in the\nconfig file: " (config-file))})

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

(defn- write-config
  [config]
  (log/debug "Will try to save configuration to" (config-file))
  (when (not (fs/exists? (config-dir)))
    (log/debug "Creating directory" (config-dir))
    (fs/mkdirs (config-dir)))
  (when (not (fs/exists? (config-file)))
    (log/debug "Creating file" (config-file))
    (fs/create (fs/file (config-file))))
  (log/debug "Saving" config "to" (config-file))
  (spit (config-file) config)
  (if (= (read-config) config)
    (println "The configuration file has been successfully saved.")
    (exit 65 (:65 exit-messages))))

(defn- save-config
  [force & {:keys [lat lon z]}]
  (let [config (->> {:latitude lat, :longitude lon, :timezone z}
                    (remove #(nil? (second %)))
                    (map #(apply hash-map %))
                    (apply merge))]
    (if (and (fs/exists? (config-file)) (not force))
      (exit 64 (:64 exit-messages))
      (write-config config))))

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
              "Value" (if (read-config) (config-file) "None")}
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
    [["-c" "--create-config"
      "Save --latitude, --longitude, and --timezone to a configuration file."
      :default false]
     ["-f" "--feast-days YEAR"
      "Calculate and print a list of feast days in a gregorian YEAR"
      :parse-fn #(read-string %)
      :validate [#(and (int? %) (<= 1584 % 2100))
                 #(str % " is not an integer between 1584 and 2100")]
      :id :year-to-calculate-feast-days]
     ["-F" "--force"
      "Force saving of configuration file even if it already exists."
      :default false]
     ["-h" "--help"
      "Print this help message."
      :default false]
     ["-s" "--sabbath"
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
      :default (:longitude config)
      :validate [#(or (nil? %) (and (number? %) (<= -180 % 180)))
                 #(str % " is not a number between -180 and 180.")]]
     ["-y" "--latitude NUMBER"
      "The latitude of the location."
      :parse-fn #(read-string %)
      :default (:latitude config)
      :validate [#(or (nil? %) (and (number? %) (<= -90 % 90)))
                 #(str % " is not a number between -90 and 90.")]]
     ["-z" "--timezone STRING"
      "The timezone of the location."
      :default (:timezone config)
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
      (or (nil? (:latitude options)) (nil? (:longitude options)))
      (exit 66 (:66 exit-messages))
      errors ; errors => exit with description of errors
      {:exit-message (str/join \newline errors)}
      :else
      (select-keys options [:create-config :force :latitude :longitude :sabbath
                            :timezone :verbosity :year-to-calculate-feast-days]))))

;; End of command line parsing.

(defn -main [& args]
  (let [{:keys [create-config force latitude longitude sabbath timezone verbosity
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
      sabbath
      (exit-with-sabbath (sabbath? latitude longitude timezone (l/now)))
      ;;
      create-config
      (save-config force :lat latitude :lon longitude :z timezone)
      ;;
      year-to-calculate-feast-days
      (print-feast-days-in-year year-to-calculate-feast-days)
      ;;
      :else (print-date latitude longitude (l/in-zone (or timezone (tick/zone))
                                                      (l/now)))))
  (System/exit 0))
