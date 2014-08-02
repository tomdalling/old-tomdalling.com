(defproject dowlgen "0.1.0-SNAPSHOT"
  :description "Static site generator for tomdalling.com"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [me.raynes/cegdown "0.1.1"]
                 [stasis "2.1.1"]
                 [ring "1.3.0"]
                 [enlive "1.1.5"]
                 [optimus "0.15.0"]
                 [optimus-sass "0.0.2"]
                 [org.clojure/data.json "0.2.5"]
                 [clj-time "0.8.0"]]
  :ring {:handler dowlgen.core/app}
  :aliases {"build-site" ["run" "-m" "dowlgen.core/export"]}
  :profiles {
    :dev {
      :plugins [[lein-ring "0.8.10"]]
      :dependencies [[midje "1.5.1"]]}}
  :main dowlgen.core)
