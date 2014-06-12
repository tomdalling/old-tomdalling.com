(defproject dowlgen "0.1.0-SNAPSHOT"
  :description "Static site generator for tomdalling.com"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [me.raynes/cegdown "0.1.1"]]
  :profiles {:dev {:dependencies [[midje "1.5.1"]]}}
  :main dowlgen.core)
