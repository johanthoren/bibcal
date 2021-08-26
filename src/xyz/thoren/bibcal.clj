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
  {:64 "ERROR: The configuration file already exists. Use -f to overwrite."
   :65 (str "ERROR: Something went wrong while validating the saved config.\n"
            "       Inspect the config file for more details:\n"
            "       " (config-file))
   :66 (str "ERROR:   The options --lat and --lon are both needed, and --zone\n"
            "         is highly recommended. You can provide them either as\n"
            "         options to the command or saved in the config file:\n"
            "\n"
            "         " (config-file) "\n"
            "\n"
            "         Use them with the option -c to save them to the\n"
            "         config file.\n"
            "\n"
            "EXAMPLE: bibcal -c --lat " l/jerusalem-lat " --lon "
            l/jerusalem-lon " --zone " l/jerusalem-zone)
   :67 "ERROR: You can't use option -f without option -c."
   :68 "ERROR: Arguments can only be used together with -v, -x, -y, -Y, or -z."
   :69 (str "ERROR: Wrong number or wrong type of arguments."
            "       Either use just one integer to print the feast days of a"
            "       year, or use between 3 and 7 integers to calculate a "
            "       certain time.")
   :70 "ERROR: You can't use both options -t and -T at the same time."
   :71 (str "ERROR: You can't use option -c with other options than -v, -x, -y,"
            "       and -z.")})

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
  (when-not (fs/exists? (config-dir))
    (log/debug "Creating directory" (config-dir))
    (fs/mkdirs (config-dir)))
  (when-not (fs/exists? (config-file))
    (log/debug "Creating file" (config-file))
    (fs/create (fs/file (config-file))))
  (log/debug "Saving" config "to" (config-file))
  (spit (config-file) config)
  (if (= (read-config) config)
    (println "The configuration file has been successfully saved.")
    (exit 65 (:65 exit-messages))))

(defn- save-config
  [force & {:keys [lat lon z]}]
  (let [config (->> {:lat lat, :lon lon, :zone z}
                    (remove #(nil? (second %)))
                    (map #(apply hash-map %))
                    (apply merge))]
    (if (and (fs/exists? (config-file)) (not force))
      (exit 64 (:64 exit-messages))
      (write-config config))))

(defn print-feast-days-in-year
  [y]
  (cond
    (coll? y) (doseq [d y] (println d))
    (l/feast-days y) (doseq [d (l/list-of-feast-days-in-year y)] (println d))
    :else (do (println (str "Calculating feast days in " y ". Please wait..."))
              (print-feast-days-in-year (l/list-of-feast-days-in-year y)))))

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

(defn print-brief-date
  [lat lon time & {:keys [year] :or {year false}}]
  (let [d (l/date lat lon time)
        h (:hebrew d)
        n (:names h)
        without-y (str (:day-of-month n) " of " (:traditional-month-of-year n))
        with-y (str without-y ", " (:year h))]
    (println (if year with-y without-y))))

(defn print-date
  [lat lon time & {:keys [year] :or {year false}}]
  (let [d (l/date lat lon time)
        h (:hebrew d)
        t (:time d)
        tf (tick/formatter "yyy-MM-dd HH:mm:ss")
        base-table [{:Key "Biblical month" :Value (:month-of-year h)}
                    {:Key "Biblical day of month" :Value (:day-of-month h)}
                    {:Key "Biblical day of week" :Value (:day-of-week h)}
                    {:Key "Sabbath" :Value (:sabbath h)}
                    {:Key "Major feast day"
                     :Value (feast-or-false (:major-feast-day h))}
                    {:Key "Minor feast day"
                     :Value (feast-or-false (:minor-feast-day h))}
                    {:Key "Start of year"
                     :Value (tick/format tf (get-in t [:year :start]))}
                    {:Key "Start of month"
                     :Value (tick/format tf (get-in t [:month :start]))}
                    {:Key "Start of week"
                     :Value (tick/format tf (get-in t [:week :start]))}
                    {:Key "Start of day"
                     :Value (tick/format tf (get-in t [:day :start]))}
                    {:Key "End of day"
                     :Value (tick/format tf (get-in t [:day :end]))}
                    {:Key "End of week"
                     :Value (tick/format tf (get-in t [:week :end]))}
                    {:Key "End of month"
                     :Value (tick/format tf (get-in t [:month :end]))}
                    {:Key "End of year"
                     :Value (tick/format tf (get-in t [:year :end]))}
                    {:Key "Location" :Value (str lat "," lon)}
                    {:Key "Timezone" :Value (str (tick/zone time))}
                    {:Key "Config file"
                     :Value (if (read-config) (config-file) "None")}]
        table-with-y (cons {:Key "Biblical year"
                            :Value (:year h)}
                           base-table)
        table-with-g (cons {:Key "Gregorian time" :Value (tick/format tf time)}
                           (if year table-with-y base-table))]
    (table table-with-g)))

;; Beginning of command line parsing.

(defn- cli-options
  ;; First three strings describe a short-option, long-option with optional
  ;; example argument description, and a description. All three are optional
  ;; and positional.
  []
  (let [config (read-config)]
    [["-c" "--create-config"
      "Save --lat, --lon, and --zone to the configuration file."
      :default false]
     ["-f" "--force"
      "Force saving of configuration file even if it already exists."
      :default false]
     ["-h" "--help"
      "Print this help message."
      :default false]
     ["-s" "--sabbath"
      "Check Sabbath status. Silent by default."
      :default false]
     ["-t" "--today"
      "Long summary of the current biblical date."
      :default false]
     ["-T" "--today-brief"
      "Short summary of the current biblical date."
      :default false]
     ["-v" nil
      "Verbosity level; specify multiple times to increase value."
      :id :verbosity
      :default 0
      :update-fn inc]
     ["-V" "--version"
      "Print the current version number."
      :default false]
     ["-x" "--lon NUMBER"
      "The longitude of the location."
      :parse-fn read-string
      :default (:lon config)
      :validate [#(or (nil? %) (and (number? %) (<= -180 % 180)))
                 #(str % " is not a number between -180 and 180.")]]
     ["-y" "--lat NUMBER"
      "The latitude of the location."
      :parse-fn read-string
      :default (:lat config)
      :validate [#(or (nil? %) (and (number? %) (<= -90 % 90)))
                 #(str % " is not a number between -90 and 90.")]]
     ["-Y" "--include-year"
      "Include potential biblical year in the output."
      :default false]
     ["-z" "--zone STRING"
      "The timezone of the location."
      :default (:zone config)
      :validate [valid-zone?
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
    "Usage: bibcal [options] [YEAR]"
    "       bibcal [options] YEAR MONTH DAY [HOUR] [MINUTE] [SECOND]"
    ""
    "Options:"
    options-summary
    ""
    "Examples:"
    ""
    "Command: $ bibcal 2021"
    "Result:  Display all feast days in the gregorian year 2021."
    ""
    "Command: $ bibcal 2021 4 13 18"
    "Result:  Show a summary of the biblical date at 2021-04-13T18:00."]))

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
      (and (:force options) (not (:create-config options)))
      (exit 67 (:67 exit-messages))
      (and (> (count arguments) 2)
           (or (nil? (:lat options)) (nil? (:lon options))))
      (exit 66 (:66 exit-messages))
      (and (:create-config options)
           (->> (dissoc options :create-config :lat :lon :zone :verbosity)
                (vals)
                (remove #(or (false? %) (nil? %)))
                (count)
                (< 0)))
      (exit 71 (:71 exit-messages))
      (and (seq arguments)
           (->> (dissoc options :include-year :lat :lon :zone :verbosity)
                (vals)
                (remove #(or (false? %) (nil? %)))
                (count)
                (< 0)))
      (exit 68 (:68 exit-messages))
      (and (:today options) (:today-brief options))
      (exit 70 (:70 exit-messages))
      :else
      (assoc (select-keys options [:include-year :create-config :force :lat :lon
                                   :sabbath :today :today-brief :verbosity
                                   :zone])
             :arguments
             (map read-string arguments)))))

;; End of command line parsing.

(defn -main [& args]
  (let [{:keys [arguments include-year create-config force lat lon sabbath today
                today-brief verbosity zone exit-message ok?]}
        (validate-args args)]
    (when exit-message
      (exit (if ok? 0 1) exit-message))
    (set-log-level! verbosity)
    (log/debug "Configuration file:" (if (read-config) (config-file) "None"))
    (log/debug "Latitude:" lat)
    (log/debug "Longitude:" lon)
    (log/debug "TimeZone:" zone)
    (log/debug "Arguments:" arguments)
    (cond
      (seq arguments)
      (cond
        (and (= (count arguments) 1)
             (and (int? (first arguments))
                  (<= 1584 (first arguments) 2100)))
        (print-feast-days-in-year (first arguments))
        ;;
        (and (<= 3 (count arguments) 7)
             (empty? (remove int? arguments)))
        (print-date lat
                    lon
                    (apply l/zdt (cons (or zone (tick/zone)) arguments))
                    :year include-year)
        ;;
        :else (exit 69 (:69 exit-messages)))
      ;;
      sabbath
      (exit-with-sabbath (sabbath? lat lon zone (l/now)))
      ;;
      create-config
      (save-config force :lat lat :lon lon :z zone)
      ;;
      today-brief
      (print-brief-date
       lat lon (l/in-zone (or zone (tick/zone)) (l/now)) :year include-year)
      ;;
      today
      (print-date
       lat lon (l/in-zone (or zone (tick/zone)) (l/now)) :year include-year)
      ;;
      :else (print-feast-days-in-year (tick/int (tick/year (l/now))))))
  (System/exit 0))
