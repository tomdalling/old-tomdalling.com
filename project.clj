(defproject dowlgen "0.1.0-SNAPSHOT"
  :description "Static site generator for tomdalling.com"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [me.raynes/cegdown "0.1.1"]
                 [stasis "2.2.2"]
                 [ring "1.3.0"]
                 [enlive "1.1.5"]
                 [optimus "0.17.0"]
                 [optimus-sass "0.0.2"]
                 [org.clojure/data.json "0.2.5"]
                 [org.clojure/data.xml "0.0.7"]
                 [clygments "0.1.1"]
                 [clj-time "0.8.0"]
                 [prismatic/schema "0.2.6"]]
  :ring {:handler dowlgen.core/app
         :nrepl {:start? true}}
  :aliases {"build-site" ["run" "-m" "dowlgen.core/export"]}
  :profiles {
    :dev {
      :plugins [[lein-ring "0.8.10"]
                [lein-midje "3.2.2"]]
      :dependencies [[midje "1.5.1"]]}}
  :main dowlgen.core)
