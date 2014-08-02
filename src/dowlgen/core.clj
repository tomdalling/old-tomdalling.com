(ns dowlgen.core
  (:require [me.raynes.cegdown :as markdown]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [hiccup.page :refer [html5]]
            [net.cgrand.enlive-html :as enlive]
            [stasis.core :as stasis]
            [sass.core :as sass]
            [optimus.link :as link]
            [optimus.assets :as assets]
            [optimus.optimizations :as optimizations]
            [optimus.prime :as optimus]
            [optimus.strategies :refer [serve-live-assets]]
            [optimus-sass.core]))

(def input-dir "site")
(def output-dir "dist")
(def markdown-options [:autolinks :fenced-code-blocks :strikethrough])

(defn map-map [key-fn val-fn coll]
  (zipmap (map key-fn (keys coll))
          (map val-fn (vals coll))))

(defn map-map2 [f coll]
  (into {} (map #(apply f %) coll)))

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

(enlive/deftemplate layout-article "templates/article.html" [article]
  [:title] (enlive/content (:title article))
  [:h1] (enlive/content (:title article))
  [:.post-date] (enlive/content (:date article))
  [:.post-content] (enlive/html-content (:content article))
  [(enlive/attr= :rel "stylesheet")] (enlive/set-attr :href "/style.css"))

(defn md->html [md]
  (markdown/to-html md markdown-options))

(defn blog-post-html [post-md]
  (let [[frontmatter markdown] (read-split-frontmatter post-md)]
    (apply str (layout-article (assoc frontmatter :content (md->html markdown))))))

(defn transform-blog-posts [path content]
  [(str (remove-extension path) "/index.html")
   (fn [_] (blog-post-html content))])

(defn transform-scss [path content]
  [(change-extension path "css")
   (fn [_] (sass/render-string content :syntax :scss :style :nested))])

(defn transform-pages* [kw regex f]
  {kw (map-map2 f (stasis/slurp-directory input-dir regex))})

(defn transform-pages [& keyword-regex-f-pairs]
  (->> keyword-regex-f-pairs
    (partition 3)
    (map #(apply transform-pages* %))
    (reduce merge)
    (stasis/merge-page-sources)))

(defn get-assets []
  (concat (assets/load-assets "." ["/style.scss"])))

(defn get-pages []
  (transform-pages
    :blog #"^/blog/.*\.markdown$" transform-blog-posts
    :scss #"^/style.scss$" transform-scss))

(def app
  (optimus/wrap (stasis/serve-pages get-pages)
                get-assets
                optimizations/all
                serve-live-assets))

(defn export []
  (stasis/empty-directory! output-dir)
  (stasis/export-pages (get-pages) output-dir))

