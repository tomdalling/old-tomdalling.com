(ns dowlgen.core
  (:require [dowlgen.templates :as templates]
            [dowlgen.category :as category]
            [dowlgen.post :as post]
            [dowlgen.config :as config]
            [ring.util.response]
            [stasis.core :as stasis]
            [optimus.export]
            [optimus.assets :as assets]
            [optimus.optimizations :as optimizations]
            [optimus.prime :as optimus]
            [optimus.strategies :refer [serve-live-assets]]
            [optimus-sass.core]))

(defn get-posts [include-drafts]
  (let [posts (post/from-map (stasis/slurp-directory "resources" #"^/blog-posts/.*\.markdown$"))]
    (if include-drafts
      posts
      (post/remove-drafts posts))))

(defn archive-pages [all-posts]
  (for [[ym posts] (post/archived all-posts)]
    (let [uri (post/archive-uri ym)]
      [uri (templates/renderfn-post-list posts
                                         (str "Archives: " (templates/unparse-yearmonth ym))
                                         uri
                                         nil ;;no feed for archives
                                         all-posts)])))

(defn category-pages [all-posts]
  (for [[cat posts] (post/categorized all-posts)]
    [(category/uri cat)
     (templates/renderfn-post-list posts
                                   (str "Category: " (:name cat))
                                   (category/uri cat)
                                   (category/feed-uri cat)
                                   all-posts)]))

(defn category-rss-feeds [posts]
  (for [cat category/all]
    [(str (category/feed-uri cat) "index.xml")
     (templates/renderfn-rss (take 10 (post/in-category posts cat))
                             (category/feed-uri cat))]))

(defn post-pages [all-posts]
  (for [p all-posts]
    [(:uri p) (templates/renderfn-post p all-posts)]))

(defn blog-index-page [all-posts]
  ["/blog/"
   (templates/renderfn-post-list (take 10 all-posts)
                                 "Recent Posts"
                                 "/blog/"
                                 "/blog/feed/"
                                 all-posts)])

(defn home-page [all-posts]
  ["/"
   (templates/renderfn-page-html (slurp "resources/pages/home.html")
                                 "Home"
                                 "/"
                                 all-posts)])

(defn blog-rss-feed [all-posts]
  ["/blog/feed/index.xml"
   (templates/renderfn-rss (take 10 all-posts) "/blog/feed/")])

(defn get-original-pages [include-drafts]
  (let [all-posts (get-posts include-drafts)]
    (into {}
      (concat
        (post-pages all-posts)
        (archive-pages all-posts)
        (category-pages all-posts)
        (category-rss-feeds all-posts)
        [(blog-index-page all-posts)
         (home-page all-posts)
         (blog-rss-feed all-posts)]))))

(defn strict-get [m k]
  (if-let [[k v] (find m k)]
    v
    (throw (Exception. (str "key not found: " k)))))

(defn duplicate-pages [pages dup-pairs]
  (into pages
    (for [[dup-uri original-uri] dup-pairs]
      [dup-uri (strict-get pages original-uri)])))

(defn get-pages [include-drafts]
  (duplicate-pages (get-original-pages include-drafts)
                   config/page-duplicates))

(defn get-assets []
  (concat (assets/load-assets "theme" ["/style.scss"])
          (assets/load-assets "static" [#"^/images/.*(png|jpg|gif)$"])
          (assets/load-bundle "js" "all.js" ["/jquery-1.11.1.js" #".*\.js"])))

(defn wrap-utf8 [handler]
  (fn [request]
    (let [response (handler request)]
      (if (or (= "text/html" (get-in response [:headers "Content-Type"]))
              (.endsWith (:uri request) ".xml"))
        (ring.util.response/charset response "UTF-8")
        response))))

(def app
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

