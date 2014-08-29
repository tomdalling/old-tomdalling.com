(ns dowlgen.core
  (:require [dowlgen.templates :as templates]
            [dowlgen.category :as category]
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

(def Artist
  "schema for artists of main images"
  {:name s/Str
   :url s/Str})

(def Post
  "A schema for blog posts"
  {:title s/Str
   :date org.joda.time.LocalDate
   :category category/schema
   :disqus-id s/Str
   :draft s/Bool
   :main-image (s/maybe {:uri s/Str
                         (s/optional-key :artist) Artist})
   :content-markdown s/Str
   :uri s/Str
   :full-url s/Str})

(def site-url "http://www.tomdalling.com")

(defn frontmatter-date [date-str]
  (let [datetime (tformat/parse (tformat/formatter "yyyy-MM-dd") date-str)]
    (t/local-date (t/year datetime) (t/month datetime) (t/day datetime))))

(defn last-path-component [path]
  (let [idx (.lastIndexOf path "/")]
    (if (= idx -1)
      path
      (.substring path (inc idx)))))

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

(defn split-post-filename [fname]
  (let [[date-str uri-name] (clojure.string/split (last-path-component fname) #"_" 2)]
    [(frontmatter-date date-str) (remove-extension uri-name)]))

(defn build-post [path file-content]
  (s/validate Post
    (let [[frontmatter md] (read-split-frontmatter file-content)
          [date uri-name] (split-post-filename path)
          cat (category/for-keyword (:category frontmatter))
          uri (str "/blog/" (category/uri-name cat) "/" uri-name "/")]
      {:title (:title frontmatter)
       :disqus-id (:disqus-id frontmatter)
       :main-image (:main-image frontmatter)
       :draft (boolean (:draft frontmatter))
       :uri uri
       :full-url (str site-url uri)
       :content-markdown md
       :date date
       :category cat})))

(defn get-posts [include-drafts]
  (reverse
    (sort-by :date
      (filter (if include-drafts (fn [_] true) #(not (:draft %)))
              (map #(apply build-post %)
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
        (for [post all-posts]
          [(:uri post) (templates/render-post post all-posts)])
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

