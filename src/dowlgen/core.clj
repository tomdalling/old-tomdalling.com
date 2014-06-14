(ns dowlgen.core
  (:require [me.raynes.cegdown :as markdown]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [hiccup.page :refer [html5]]
            [stasis.core :as stasis]))

(defn map-map [key-fn val-fn coll]
  (zipmap (map key-fn (keys coll))
          (map val-fn (vals coll))))

(defn map-vals [f coll]
  (map-map identity f coll))

(defn layout-page [page]
  (html5
    [:head
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport"
              :content "width=device-width, initial-scale=1.0"}]
      [:title "Tech blog"]
      [:link {:rel "stylesheet" :href "/styles/styles.css"}]]
    [:body
      [:div.logo "cjohansen.no"]
      [:div.body page]]))

(defn markdown-pages [pages]
  (map-map #(string/replace % #"\.md$" "/")
           #(layout-page (markdown/to-html %))
           pages))

(defn partial-pages [pages]
  (map-vals layout-page pages))

(defn get-pages []
  (stasis/merge-page-sources
    {:public
     (stasis/slurp-directory "resources/public" #".*\.(html|css|js)$")
     :partials
     (partial-pages (stasis/slurp-directory "resources/partials" #".*\.html$"))
     :markdown
     (markdown-pages (stasis/slurp-directory "resources/md" #".*\.md$"))}))

(def app
  (stasis/serve-pages get-pages))

(def export-dir "dist")

(defn export []
  (stasis/empty-directory! export-dir)
  (stasis/export-pages (get-pages) export-dir))

(defn -main []
  (println "Hello"))
