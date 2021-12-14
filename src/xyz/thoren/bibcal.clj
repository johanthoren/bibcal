(ns xyz.thoren.bibcal
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [me.raynes.fs :as fs]
            [tick.core :as tick]
            [trptcolin.versioneer.core :refer [get-version]]
            [xyz.thoren.luminary :as l])
  (:gen-class))

(def version-number
  "The version number as defined in project.clj."
  ;; Note that this is evaluated at build time by native-image.
  (get-version "xyz.thoren" "bibcal"))

(defn print-v [n & more]
  (when (pos? n)
    (apply println more)))

(defn print-d [n & more]
  (when (> n 1)
    (apply println (cons "DEBUG:" more))))

(defn config-dir []
  (let [os (System/getProperty "os.name")
        home (System/getProperty "user.home")]
    (if (str/starts-with? os "Windows")
      (str home "\\AppData\\Roaming\\bibcal\\")
      (str home "/.config/bibcal/"))))

(defn config-file []
  (str (config-dir) "config.edn"))

(defn valid-zone?
  [s]
  (l/valid-zone? s))

(defn read-config
  ([k]
   (try
     (let [config (edn/read-string (slurp (config-file)))]
       (get config k))
     (catch java.io.FileNotFoundException _e nil)))
  ([]
   (try
     (edn/read-string (slurp (config-file)))
     (catch java.io.FileNotFoundException _e nil))))

(defn create-config-dir [v]
  (when-not (fs/exists? (config-dir))
    (print-v v "Creating directory" (config-dir))
    (fs/mkdirs (config-dir))))

(defn create-config-file [v]
  (when-not (fs/exists? (config-file))
    (print-v v "Creating file" (config-file))
    (fs/create (fs/file (config-file)))))

(defn write-config
  [m v]
  (print-v v "Will try to save configuration to" (config-file))
  (create-config-dir v)
  (create-config-file v)
  (print-v v "Saving" m "to" (config-file))
  (spit (config-file) m))

(defn config
  [m]
  (as-> (remove #(nil? (val %)) m) <>
        (into (empty m) <>)
        (select-keys <> [:lat :lon :zone])
        (when (seq <>) <>)))

(defn print-feast-days-in-year
  [y]
  (cond
    (coll? y) (doseq [d y] (println d))
    (l/feast-days y) (doseq [d (l/list-of-feast-days-in-year y)] (println d))
    :else (do (println (str "Calculating feast days in " y ". Please wait..."))
              (print-feast-days-in-year (l/list-of-feast-days-in-year y)))))

(defn sabbath?
  ([v lat lon time]
   (let [d (l/date lat lon time)
         s (get-in d [:hebrew :sabbath])]
     (print-d v "Checking Sabbath for the following hebrew date:" d)
     (boolean s)))
  ([v lat lon]
   (sabbath? v lat lon (l/now))))

(defn print-sabbath
  [b v]
  (print-v v (if b "It's Sabbath!" "It's not Sabbath.")))

(defn feast-or-false
  [{:keys [name day-of-feast days-in-feast] :or {name nil}}]
  (cond
    (not name) false
    (< days-in-feast 3) name
    (= days-in-feast 8) (str (l/day-numbers (dec day-of-feast)) " day of " name)
    :else (str (l/day-numbers (dec day-of-feast)) " day of the " name)))

(defn print-brief-date
  [lat lon time & {:keys [year trad-year] :or {year false trad-year false}}]
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

(defn iso-date
  [y m d]
  (str y "-" (format "%02d" m) "-" (format "%02d" d)))

(defn print-date
  [v lat lon time]
  (let [d (l/date lat lon time)
        h (:hebrew d)
        n (:names h)
        moy (:month-of-year h)
        dom (:day-of-month h)
        t (:time d)
        tf (tick/formatter "yyy-MM-dd HH:mm:ss")
        fmt-time #(tick/format tf (get-in t [%1 %2]))
        fmt #(format "%-24s%s" %1 %2)
        sabbath (:sabbath h)
        major-f (feast-or-false (:major-feast-day h))
        minor-f (feast-or-false (:minor-feast-day h))
        next-day (l/go-forward 1 :seconds (get-in t [:day :end]))]
    (->> [["Date" (str (:day-of-month n) " day of the "
                       (:month-of-year n) " month")]
          ["ISO date" (iso-date (:year h) moy dom)]
          ["Traditional date" (str (:day-of-month n) " of "
                                        (:traditional-month-of-year n))]
          ["Traditional ISO date" (iso-date (:traditional-year h) moy dom)]
          ["Day of week" (:day-of-week h)]
          (when (or sabbath (pos? v)) ["Sabbath" sabbath])
          (when (or major-f (pos? v)) ["Major feast day" major-f])
          (when (or minor-f (pos? v)) ["Minor feast day" minor-f])
          ["Current local time" (tick/format tf time)]
          (when-not (pos? v) ["Start of next day" (tick/format tf next-day)])
          (when (pos? v) ["Start of year" (fmt-time :year :start)])
          (when (pos? v) ["Start of month" (fmt-time :month :start)])
          (when (pos? v) ["Start of week" (fmt-time :week :start)])
          (when (pos? v) ["Start of day" (fmt-time :day :start)])
          (when (pos? v) ["End of day" (fmt-time :day :end)])
          (when (pos? v) ["End of week" (fmt-time :week :end)])
          (when (pos? v) ["End of month" (fmt-time :month :end)])
          (when (pos? v) ["End of year" (fmt-time :year :end)])
          (when (pos? v) ["Coordinates" (str lat "," lon)])
          (when (pos? v) ["Timezone" (str (tick/zone time))])
          (when (pos? v) ["Config file" (if (read-config)
                                          (config-file)
                                          "None")])]
         (remove nil?)
         (run! #(println (apply fmt %))))))

;; Beginning of command line parsing.

(def cli-options
  (delay
    (let [config (read-config)]
      [["-c"
        "--create-config"
        "Save --lat, --lon, and --zone to the configuration file."
        :default false]
       ["-f"
        "--force"
        "Force saving of configuration file even if it already exists."
        :default false]
       ["-h"
        "--help"
        "Print this help message."
        :default false]
       ["-l"
        "--lat NUMBER"
        "The latitude of the location."
        :parse-fn read-string
        :default (:lat config)
        :validate [#(or (nil? %) (and (number? %) (<= -90 % 90)))
                   #(str % " is not a number between -90 and 90.")]]
       ["-L"
        "--lon NUMBER"
        "The longitude of the location."
        :parse-fn read-string
        :default (:lon config)
        :validate [#(or (nil? %) (and (number? %) (<= -180 % 180)))
                   #(str % " is not a number between -180 and 180.")]]
       ["-s"
        "--sabbath"
        "Check Sabbath status. Silent by default."
        :default false]
       ["-t"
        "--today"
        "Long summary of the current Biblical date."
        :default false]
       ["-T"
        "--today-brief"
        "Short summary of the current Biblical date."
        :default false]
       ["-v" nil
        "Verbosity level; specify multiple times to increase value."
        :id :verbosity
        :default 0
        :update-fn inc]
       ["-V"
        "--version"
        "Print the current version number."
        :default false]
       ["-y"
        "--include-trad-year"
        "Include traditional Jewish year in the output."
        :default false]
       ["-Y"
        "--include-year"
        "Include potential Biblical year in the output."
        :default false]
       ["-z"
        "--zone STRING"
        "The timezone of the location."
        :default (:zone config)
        :validate [valid-zone? #(str % " is not a valid zone id string")]]])))

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
   :68 "ERROR: Arguments can't be used with options -c or -f."
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
        ["ERROR: Options -s, -t, and -T can only be used with either 0 or"
         "       between 3 and 7 arguments."])
   :75 "ERROR: Year is outside of range 1584 to 2100."})

(defn validate-args
  [args]
  (let [{:keys [options arguments errors summary]}
        (parse-opts args @cli-options)]
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :exit-code 0}
      ;;
      (:version options) ; version => exit OK with version number
      {:exit-message version-number :exit-code 0}
      ;;
      errors ; errors => exit with description of errors
      {:exit-message (str/join \newline errors)}
      ;;
      (and (:create-config options)
           (fs/exists? (config-file))
           (not (:force options)))
      {:exit-message (:64 exit-messages) :exit-code 64}
      ;;
      (and (or (> (count arguments) 2)
               (:sabbath options)
               (:today options)
               (:today-brief options))
           (or (nil? (:lat options))
               (nil? (:lon options))))
      {:exit-message (:66 exit-messages) :exit-code 66}
      ;;
      (and (:force options) (not (:create-config options)))
      {:exit-message (:67 exit-messages) :exit-code 67}
      ;;
      (and (seq arguments)
           (or (:force options)
               (:create-config options)))
      {:exit-message (:68 exit-messages) :exit-code 68}
      ;;
      (seq (remove int? (map read-string arguments)))
      {:exit-message (:69 exit-messages) :exit-code 69}
      ;;
      (and (:today options) (:today-brief options))
      {:exit-message (:70 exit-messages) :exit-code 70}
      ;;
      (and (:create-config options)
           (->> (dissoc options
                        :create-config
                        :force
                        :lat
                        :lon
                        :zone
                        :verbosity)
                vals
                (remove #(or (false? %) (nil? %)))
                seq))
      {:exit-message (:71 exit-messages) :exit-code 71}
      ;;
      (and (or (:include-year options) (:include-trad-year options))
           (not (:today-brief options)))
      {:exit-message (:72 exit-messages) :exit-code 72}
      ;;
      (and (:include-year options) (:include-trad-year options))
      {:exit-message (:73 exit-messages) :exit-code 73}
      ;;
      (and (or (:sabbath options) (:today options) (:today-brief options))
           (<= 1 (count arguments) 2))
      {:exit-message (:74 exit-messages) :exit-code 74}
      ;;
      (and (seq arguments)
           (not (<= 1584 (read-string (first arguments)) 2100)))
      {:exit-message (:75 exit-messages) :exit-code 75}
      ;;
      :else
      (assoc (select-keys options [:include-trad-year :include-year
                                   :create-config :force :lat :lon :sabbath
                                   :today :today-brief :verbosity :zone])
             :arguments
             (map read-string arguments)))))

;; End of command line parsing.

(defn exit
  "Print a `message` and exit the program with the given `status` code.
  See also [[exit-messages]]."
  [status message]
  (println message)
  (System/exit status))

(defn -main [& args]
  (let [{:keys [arguments include-trad-year include-year create-config
                lat lon sabbath today today-brief verbosity zone exit-message
                exit-code]}
        (validate-args args)]
    (when exit-message
      (exit exit-code exit-message))
    (print-d verbosity "Configuration file:" (if (read-config)
                                               (config-file)
                                               "None"))
    (print-d verbosity "Latitude:" lat)
    (print-d verbosity "Longitude:" lon)
    (print-d verbosity "TimeZone:" zone)
    (print-d verbosity "Arguments:" arguments)
    (if (seq arguments)
      (cond
        (not (<= 1584 (first arguments) 2100))
        (exit 75 (:75 exit-messages))
        ;;
        (= (count arguments) 1)
        (print-feast-days-in-year (first arguments))
        ;;
        (<= 3 (count arguments) 7)
        (let [d (apply l/zdt (cons (or zone (tick/zone)) arguments))]
          (cond
            sabbath
            (let [s (sabbath? verbosity lat lon d)]
              (print-sabbath s verbosity)
              (when-not s (System/exit 1)))
            ;;
            today-brief
            (print-brief-date
             lat lon d :year include-year :trad-year include-trad-year)
            ;;
            :else
            (print-date verbosity lat lon d)))
        ;;
        :else (exit 69 (:69 exit-messages)))
      (cond
        ;;
        sabbath
        (let [s (sabbath? verbosity lat lon (l/in-zone
                                             (or zone (tick/zone)) (l/now)))]
          (print-sabbath s verbosity)
          (when-not s (System/exit 1)))
        ;;
        create-config
        (let [c (config {:lat lat :lon lon :z zone})]
          (write-config c verbosity)
          (if (= (read-config) c)
            (println "The configuration file has been successfully saved.")
            (exit 65 (:65 exit-messages))))
        ;;
        today-brief
        (print-brief-date lat
                          lon
                          (l/in-zone (or zone (tick/zone)) (l/now))
                          :year include-year
                          :trad-year include-trad-year)
        ;;
        today
        (print-date verbosity lat lon (l/in-zone (or zone (tick/zone)) (l/now)))
        ;;
        :else (print-feast-days-in-year (tick/int (tick/year (l/now)))))))
  (System/exit 0))
