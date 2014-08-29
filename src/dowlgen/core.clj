(ns dowlgen.core
  (:require [dowlgen.templates :as templates]
            [dowlgen.category :as category]
            [dowlgen.post :as post]
            [ring.util.response]
            [net.cgrand.enlive-html :as enlive]
            [net.cgrand.reload]
            [stasis.core :as stasis]
            [schema.core :as s]
            [optimus.export]
            [optimus.assets :as assets]
            [optimus.optimizations :as optimizations]
            [optimus.prime :as optimus]
            [optimus.strategies :refer [serve-live-assets]]
            [optimus-sass.core]
            [clj-time.core :as t]
            [clj-time.format :as tformat]))

(def site-url "http://www.tomdalling.com")

(defn change-extension [path extension]
  (str (remove-extension path) "." extension))

(defn get-posts [include-drafts]
  (reverse
    (sort-by :date
      (filter (if include-drafts (fn [_] true) #(not (:draft %)))
              (map #(apply post/build-post site-url %)
                   (stasis/slurp-directory "resources" #"^/blog-posts/.*\.markdown$"))))))

(defn get-assets []
  (concat (assets/load-assets "theme" ["/style.scss"])
          (assets/load-assets "static" [#"^/images/.*(png|jpg|gif)$"])
          (assets/load-bundle "js" "all.js" ["/jquery-1.11.1.js" #".*\.js"])))

(defn post-archive-pages [all-posts]
  (for [[ym posts] (templates/archived-posts all-posts)]
    (let [uri (templates/archive-uri ym)]
      [uri (templates/render-post-list posts
                                       (str "Archives: " (templates/unparse-yearmonth ym))
                                       uri
                                       nil ;;no feed for archives
                                       all-posts)])))

(defn post-category-pages [all-posts]
  (for [[cat posts] (group-by :category all-posts)]
    [(category/uri cat)
     (templates/render-post-list posts
                                 (str "Category: " (:name cat))
                                 (category/uri cat)
                                 (category/feed-uri cat)
                                 all-posts)]))

(defn category-filter [cat posts]
  (filter #(= cat (:category %)) posts))

(defn category-rss-feeds [all-categories posts]
  (for [cat all-categories]
    [(str (category/feed-uri cat) "index.xml")
     (templates/render-rss (take 10 (category-filter cat posts))
                           site-url
                           (category/feed-uri cat))]))

(defn strict-get [m k]
  (if-let [[k v] (find m k)]
    v
    (throw (Exception. (str "key not found: " k)))))

(defn duplicate-pages [pages dup-pairs]
  (into pages
    (for [[dup-uri original-uri] dup-pairs]
      [dup-uri (strict-get pages original-uri)])))

(defn get-original-pages [include-drafts]
  (let [all-posts (get-posts include-drafts)]
    (into {}
      (concat
        (post-category-pages all-posts)
        (post-archive-pages all-posts)
        (category-rss-feeds category/all all-posts)
        (for [p all-posts]
          [(:uri p) (templates/render-post p all-posts)])
        [["/blog/" (templates/render-post-list (take 10 all-posts) "Recent Posts" "/blog/" "/blog/feed/" all-posts)]
         ["/" (templates/render-page-html (slurp "resources/pages/home.html") "Home" "/" all-posts)]
         ["/blog/feed/index.xml" (templates/render-rss (take 10 all-posts) site-url "/blog/feed/")]]))))

(defn get-pages [include-drafts]
  (duplicate-pages (get-original-pages include-drafts)
                   (into {"/feed/index.xml" "/blog/feed/index.xml"}
                         ;; categories can be accessed from "/blog/X" or "/blog/category/X"
                         (for [cat category/all]
                           [(str "/blog/" (category/uri-name cat) "/")
                            (category/uri cat)]))))

(defn wrap-utf8 [handler]
  (fn [request]
    (let [response (handler request)]
      (if (or (= "text/html" (get-in response [:headers "Content-Type"]))
              (.endsWith (:uri request) ".xml"))
        (ring.util.response/charset response "UTF-8")
        response))))

#_(def app
  (wrap-utf8
    (optimus/wrap (stasis/serve-pages #(get-pages true))
                  get-assets
                  (fn [assets _] (optimizations/concatenate-bundles assets))
                  serve-live-assets)))

(defn export []
  (let [output-dir "dist"
        assets (as-> (get-assets) a
                     (optimizations/all a {})
                     (remove :bundled a)
                     (remove #(not (:outdated %)) a))]
    (stasis/empty-directory! output-dir)
    (optimus.export/save-assets assets output-dir)
    (stasis/export-pages (get-pages false) output-dir {:optimus-assets assets})))

