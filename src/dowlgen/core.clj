(ns dowlgen.core
  (:require [me.raynes.cegdown :as markdown]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [hiccup.page :refer [html5]]
            [net.cgrand.enlive-html :as enlive]
            [stasis.core :as stasis]))

(def input-dir "site")
(def output-dir "dist")

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
    [:body page]))

(defn transform-markdown [pages]
  (map-map #(string/replace % #"\.md$" ".html")
           #(layout-page (markdown/to-html %))
           pages))

(defn transform-html [pages]
  (map-vals layout-page pages))

(defn transform-pages* [kw regex f]
  {kw (f (stasis/slurp-directory input-dir regex))})

(defn transform-pages [& keyword-regex-f-pairs]
  (->> keyword-regex-f-pairs
    (partition 3)
    (map #(apply transform-pages* %))
    (reduce merge)
    (stasis/merge-page-sources)))

(defn get-pages []
  (transform-pages
    :html #"\.html$" transform-html
    :markdown #"\.md$" transform-markdown))

(def app
  (stasis/serve-pages get-pages))

(defn export []
  (stasis/empty-directory! export-dir)
  (stasis/export-pages (get-pages) export-dir))

(defn -main []
  (println "Hello"))
