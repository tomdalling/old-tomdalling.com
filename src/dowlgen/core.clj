(ns dowlgen.core
  (:require [me.raynes.cegdown :as markdown]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [hiccup.page :refer [html5]]
            [net.cgrand.enlive-html :as enlive]
            [stasis.core :as stasis]
            [sass.core :as sass]))

(def input-dir "site")
(def output-dir "dist")
(def markdown-options [:autolinks :fenced-code-blocks :strikethrough])

(defn map-map [key-fn val-fn coll]
  (zipmap (map key-fn (keys coll))
          (map val-fn (vals coll))))

(defn map-vals [f coll]
  (map-map identity f coll))

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

(defn layout-page [page title]
  (html5
    [:head
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport"
              :content "width=device-width, initial-scale=1.0"}]
      [:title (str title " - TomDalling.com")]
      [:link {:rel "stylesheet" :href "/style.css"}]]
    [:body
      [:h1 title]
      page]))

(defn md->html [md]
  (markdown/to-html md markdown-options))

(defn blog-post-html [post-md]
  (let [[frontmatter markdown] (read-split-frontmatter post-md)]
    (layout-page (md->html markdown) (:title frontmatter))))

(defn transform-blog-posts [pages]
  (map-map #(str (remove-extension %) "/index.html")
           blog-post-html
           pages))

(defn transform-html [pages]
  (map-vals #(layout-page % "Boo") pages))

(defn transform-pages* [kw regex f]
  {kw (f (stasis/slurp-directory input-dir regex))})

(defn transform-pages [& keyword-regex-f-pairs]
  (->> keyword-regex-f-pairs
    (partition 3)
    (map #(apply transform-pages* %))
    (reduce merge)
    (stasis/merge-page-sources)))

(defn transform-scss [pages]
    (map-map #(change-extension % "css")
             #(sass/render-string % :syntax :scss :style :nested)
             pages))

(defn get-pages []
  (transform-pages
    :html #"\.html$" transform-html
    :blog #"^/blog/.*\.markdown$" transform-blog-posts
    :scss  #"^/style.scss$" transform-scss))

(def app
  (stasis/serve-pages get-pages))

(defn export []
  (stasis/empty-directory! output-dir)
  (stasis/export-pages (get-pages) output-dir))

