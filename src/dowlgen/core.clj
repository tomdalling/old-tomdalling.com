(ns dowlgen.core
  (:require [dowlgen.templates :as templates]
            [me.raynes.cegdown :as markdown]
            [net.cgrand.enlive-html :as enlive]
            [net.cgrand.reload]
            [stasis.core :as stasis]
            [optimus.export]
            [optimus.assets :as assets]
            [optimus.optimizations :as optimizations]
            [optimus.prime :as optimus]
            [optimus.strategies :refer [serve-live-assets]]
            [optimus-sass.core]
            [clj-time.core :as t]
            [clj-time.format :as tformat]))

(def input-dir "resources")
(def output-dir "dist")
(def site-url "http://www.tomdalling.com")
(def frontmatter-date-formatter (tformat/formatter "yyyy-MM-dd"))

(defn frontmatter-date [date-str]
  (let [datetime (tformat/parse frontmatter-date-formatter date-str)]
    (t/local-date (t/year datetime) (t/month datetime) (t/day datetime))))

(defn map-map [f coll]
  (into {} (map #(apply f %) coll)))

(defn read-split-frontmatter [s]
  (try 
    (let [reader (java.io.PushbackReader. (java.io.StringReader. s))
          frontmatter (clojure.edn/read reader)]
      (if (map? frontmatter)
        [frontmatter (slurp reader)]
        [nil s]))
    (catch Exception e [nil s])))

(defn remove-extension [path]
  (let [dot-idx (.lastIndexOf path ".")]
    (if (= dot-idx -1)
      path
      (.substring path 0 dot-idx))))

(defn change-extension [path extension]
  (str (remove-extension path) "." extension))

(defn build-post [path file-content]
  (let [[frontmatter md] (read-split-frontmatter file-content)]
    (as-> {} post
      (merge post frontmatter)
      (assoc post :uri (str (remove-extension path) "/"))
      (assoc post :full-url (str site-url (:uri post)))
      (assoc post :content (markdown/to-html md [:autolinks :fenced-code-blocks :strikethrough]))
      (update-in post [:date] frontmatter-date))))

(defn get-posts []
  (reverse
    (sort-by :date
      (map #(apply build-post %)
           (stasis/slurp-directory input-dir #"^/blog/.*\.markdown$")))))

(defn get-assets []
  (concat (assets/load-assets "." ["/style.scss"])
          (assets/load-bundle "." "all.js"
                              ["/jquery-1.11.1.js"
                               "/bootstrap/js/bootstrap.js"])))

(defn get-pages []
  (let [all-posts (get-posts)]
    (into {}
      (concat
        (for [post all-posts]
          [(str (:uri post) "index.html")
           (templates/render-post post all-posts)])
        [["/index.html"
          (templates/render-post-list all-posts all-posts)]]))))

(def app
  (optimus/wrap (stasis/serve-pages get-pages)
                get-assets
                optimizations/all
                serve-live-assets))

(defn export []
  (let [assets (optimizations/all (get-assets) {})]
    (stasis/empty-directory! output-dir)
    (optimus.export/save-assets assets output-dir)
    (stasis/export-pages (get-pages) output-dir {:optimus-assets assets})))

