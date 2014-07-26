(defproject dowlgen "0.1.0-SNAPSHOT"
  :description "Static site generator for tomdalling.com"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [me.raynes/cegdown "0.1.1"]
                 [stasis "2.1.1"]
                 [ring "1.3.0"]
                 [hiccup "1.0.5"]
                 [enlive "1.1.5"]
                 [sass "3.2.6"]]
  :ring {:handler dowlgen.core/app}
  :aliases {"build-site" ["run" "-m" "dowlgen.core/export"]}
  :profiles {
    :dev {
      :plugins [[lein-ring "0.8.10"]]
      :dependencies [[midje "1.5.1"]]}}
  :main dowlgen.core)
