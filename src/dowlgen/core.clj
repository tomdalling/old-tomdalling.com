(ns dowlgen.core
  (:require [me.raynes.cegdown :as markdown]
            [net.cgrand.enlive-html :as enlive]
            [net.cgrand.reload]
            [stasis.core :as stasis]
            [optimus.export]
            [optimus.assets :as assets]
            [optimus.optimizations :as optimizations]
            [optimus.prime :as optimus]
            [optimus.strategies :refer [serve-live-assets]]
            [optimus-sass.core]))

(net.cgrand.reload/auto-reload *ns*)

(def input-dir "resources")
(def output-dir "dist")

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

(enlive/deftemplate layout-article "templates/article.html" [article]
  [:title] (enlive/content (:title article))
  [:h1] (enlive/content (:title article))
  [:.post-date] (enlive/content (:date article))
  [:.post-content] (enlive/html-content (:content article))
  [(enlive/attr= :rel "stylesheet")] (enlive/set-attr :href "/style.css")) ;; TODO: get stylesheet url from optimus

(defn blog-post-html [post-md]
  (let [[frontmatter md] (read-split-frontmatter post-md)
        content (markdown/to-html md [:autolinks :fenced-code-blocks :strikethrough])
        article (assoc frontmatter :content content)]
    (apply str (layout-article article))))

(defn blog-post [path content]
  [(str (remove-extension path) "/index.html")
   (fn [_] (blog-post-html content))])

(defn transform-pages* [kw regex f]
  {kw (map-map f (stasis/slurp-directory input-dir regex))})

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
    :blog #"^/blog/.*\.markdown$" blog-post))

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

