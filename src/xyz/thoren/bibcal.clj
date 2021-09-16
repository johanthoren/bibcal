(ns xyz.thoren.bibcal
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.tools.logging :as log]
            [clj-logging-config.log4j]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [me.raynes.fs :as fs]
            [tick.core :as tick]
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

(defn set-log-level!
  "Set the output level based on `verbosity`.
  See also [[set-default-root-logger!]]."
  [verbosity]
  (case verbosity
    0 nil
    1 (set-default-root-logger! :info "%m%n")
    2 (set-default-root-logger! :debug "[%p] %m%n")
    (set-default-root-logger! :trace "[%p] %m%n")))

(def build-env (current-build-env))

(def version-number
  "The version number as defined in project.clj."
  ;; Note that this is evaluated at build time by native-image.
  (:version build-env))

(defn- config-dir []
  (let [os (System/getProperty "os.name")
        home (System/getProperty "user.home")]
    (if (str/starts-with? os "Windows")
      (str home "\\AppData\\Roaming\\bibcal\\")
      (str home "/.config/bibcal/"))))

(defn- config-file []
  (str (config-dir) "config.edn"))

(def exit-messages
  "Exit messages used by `exit`."
  {:64 "ERROR: The configuration file already exists. Use -f to overwrite."
   :65 (str/join \newline
        ["ERROR: Something went wrong while validating the saved config."
         "       Inspect the config file for more details:"
         (str "       " (config-file))])
   :66 (str/join \newline
        ["ERROR:   The options --lat and --lon are both needed, and --zone"
         "         is highly recommended. You can provide them either as"
         "         options to the command or saved in the config file:"
         ""
         (str "         " (config-file))
         ""
         "         Use them with the option -c to save them to the"
         "         config file."
         ""
         (str "EXAMPLE: bibcal -c --lat " l/jerusalem-lat " --lon "
              l/jerusalem-lon " --zone " l/jerusalem-zone)])
   :67 "ERROR: You can't use option -f without option -c."
   :68 "ERROR: Arguments can't be used with options -c, -f, or -s."
   :69 (str/join \newline
        ["ERROR: Wrong number of arguments or wrong type of arguments."
         "       Either use just 1 integer to print the feast days of a"
         "       year, or use between 3 and 7 integers to calculate a "
         "       certain time."])
   :70 "ERROR: You can't use both options -t and -T at the same time."
   :71 (str/join \newline
        ["ERROR: You can't use option -c with other options than "
         "       -f, -l, -L, -v, and/or -z."])
   :72 "ERROR: Options -Y and -y can only be used together with option -T."
   :73 "ERROR: Options -y and -Y are mutually exclusive."
   :74 (str/join \newline
        ["ERROR: Options -t and -T can only be used with either 0 or"
         "       between 3 and 7 arguments."])
   :75 "ERROR: Year is outside of range 1584 to 2100."})

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

(defn- feast-or-false
  [{:keys [name day-of-feast days-in-feast] :or {name nil}}]
  (cond
    (not name) false
    (< days-in-feast 3) name
    (= days-in-feast 8) (str (l/day-numbers (dec day-of-feast)) " day of " name)
    :else (str (l/day-numbers (dec day-of-feast)) " day of the " name)))

(defn print-brief-date
  [lat lon time & {:keys [year trad-year] :or {year false trad-year false}}]
  (when-not (<= 1584 (tick/int (tick/year time)) 2100)
    (exit 75 (:75 exit-messages)))
  (let [d (l/date lat lon time)
        h (:hebrew d)
        n (:names h)
        without-y (str (:day-of-month n) " of " (:traditional-month-of-year n))
        with-y (str without-y ", " (:year h))
        with-trad-y (str without-y ", " (:traditional-year h))]
    (println (cond
               year with-y
               trad-year with-trad-y
               :else without-y))))

(defn- iso-date
  [y m d]
  (str y "-" (format "%02d" m) "-" (format "%02d" d)))

(defn print-date
  [lat lon time]
  (when-not (<= 1584 (tick/int (tick/year time)) 2100)
    (exit 75 (:75 exit-messages)))
  (let [d (l/date lat lon time)
        h (:hebrew d)
        n (:names h)
        moy (:month-of-year h)
        dom (:day-of-month h)
        t (:time d)
        tf (tick/formatter "yyy-MM-dd HH:mm:ss")
        fmt-time #(tick/format tf (get-in t [%1 %2]))
        fmt #(format "%-24s%s" %1 %2)
        msgs [["Gregorian time" (tick/format tf time)]
              ["Name" (str (:day-of-month n) " day of the "
                           (:month-of-year n) " month")]
              ["Traditional name" (str (:day-of-month n) " of "
                                       (:traditional-month-of-year n))]
              ["ISO date" (iso-date (:year h) moy dom)]
              ["Traditional ISO date" (iso-date (:traditional-year h) moy dom)]
              ["Day of week" (:day-of-week h)]
              ["Sabbath" (:sabbath h)]
              ["Major feast day" (feast-or-false (:major-feast-day h))]
              ["Minor feast day" (feast-or-false (:minor-feast-day h))]
              ["Start of year" (fmt-time :year :start)]
              ["Start of month" (fmt-time :month :start)]
              ["Start of week" (fmt-time :week :start)]
              ["Start of day" (fmt-time :day :start)]
              ["End of day" (fmt-time :day :end)]
              ["End of week" (fmt-time :week :end)]
              ["End of month" (fmt-time :month :end)]
              ["End of year" (fmt-time :year :end)]
              ["Coordinates" (str lat "," lon)]
              ["Timezone" (str (tick/zone time))]
              ["Config file" (if (read-config) (config-file) "None")]]]
    (doseq [m msgs] (println (apply fmt m)))))

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
     ["-l" "--lat NUMBER"
      "The latitude of the location."
      :parse-fn read-string
      :default (:lat config)
      :validate [#(or (nil? %) (and (number? %) (<= -90 % 90)))
                 #(str % " is not a number between -90 and 90.")]]
     ["-L" "--lon NUMBER"
      "The longitude of the location."
      :parse-fn read-string
      :default (:lon config)
      :validate [#(or (nil? %) (and (number? %) (<= -180 % 180)))
                 #(str % " is not a number between -180 and 180.")]]
     ["-s" "--sabbath"
      "Check Sabbath status. Silent by default."
      :default false]
     ["-t" "--today"
      "Long summary of the current Biblical date."
      :default false]
     ["-T" "--today-brief"
      "Short summary of the current Biblical date."
      :default false]
     ["-v" nil
      "Verbosity level; specify multiple times to increase value."
      :id :verbosity
      :default 0
      :update-fn inc]
     ["-V" "--version"
      "Print the current version number."
      :default false]
     ["-y" "--include-trad-year"
      "Include traditional Jewish year in the output."
      :default false]
     ["-Y" "--include-year"
      "Include potential Biblical year in the output."
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
    "Result:  Show a summary of the Biblical date at 2021-04-13T18:00."]))

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
      (and (:include-year options) (:include-trad-year options))
      (exit 73 (:73 exit-messages))
      (and (or (:include-year options) (:include-trad-year options))
           (not (:today-brief options)))
      (exit 72 (:72 exit-messages))
      (and (:force options) (not (:create-config options)))
      (exit 67 (:67 exit-messages))
      (and (or (> (count arguments) 2) (:today options) (:today-brief options))
           (or (nil? (:lat options)) (nil? (:lon options))))
      (exit 66 (:66 exit-messages))
      (and (:create-config options)
           (->> (dissoc options
                        :create-config
                        :force
                        :lat
                        :lon
                        :zone
                        :verbosity)
                (vals)
                (remove #(or (false? %) (nil? %)))
                (count)
                (< 0)))
      (exit 71 (:71 exit-messages))
      (and (seq arguments)
           (or (:force options) (:create-config options) (:sabbath options)))
      (exit 68 (:68 exit-messages))
      (and (or (:today options) (:today-brief options))
           (<= 1 (count arguments) 2))
      (exit 74 (:74 exit-messages))
      (and (:today options) (:today-brief options))
      (exit 70 (:70 exit-messages))
      (seq (remove int? (map read-string arguments)))
      (exit 69 (:69 exit-messages))
      (and (seq arguments) (not (<= 1584 (read-string (first arguments)) 2100)))
      (exit 75 (:75 exit-messages))
      :else
      (assoc (select-keys options [:include-trad-year :include-year
                                   :create-config :force :lat :lon :sabbath
                                   :today :today-brief :verbosity :zone])
             :arguments
             (map read-string arguments)))))

;; End of command line parsing.

(defn -main [& args]
  (let [{:keys [arguments include-trad-year include-year create-config force
                lat lon sabbath today today-brief verbosity zone exit-message
                ok?]}
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
        (let [d (apply l/zdt (cons (or zone (tick/zone)) arguments))]
          (if today-brief
            (print-brief-date
             lat lon d :year include-year :trad-year include-trad-year)
            (print-date lat lon d)))
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
       lat lon (l/in-zone (or zone (tick/zone)) (l/now))
       :year include-year :trad-year include-trad-year)
      ;;
      today
      (print-date
       lat lon (l/in-zone (or zone (tick/zone)) (l/now)))
      ;;
      :else (print-feast-days-in-year (tick/int (tick/year (l/now))))))
  (System/exit 0))
