(ns dowlgen.core
  (:require [dowlgen.templates :as templates]
            [ring.util.response]
            [net.cgrand.enlive-html :as enlive]
            [net.cgrand.reload]
            [stasis.core :as stasis]
            [schema.core :as schema]
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
(def categories {:software-design "Software Design"
                 :coding-tips "Coding Tips"
                 :cocoa "Cocoa"
                 :coding-styleconventions "Coding Style/Conventions"
                 :software-processes "Software Processes"
                 :web "Web"
                 :modern-opengl "Modern OpenGL Series"
                 :random-stuff "Miscellaneous"})

(def Category
  "schema for categories"
  {:keyword schema/Keyword
   :name schema/Str
   :uri schema/Str})

(def Artist
  "schema for artists of main images"
  {:name schema/Str
   :url schema/Str})

(def Post
  "A schema for blog posts"
  {:title schema/Str
   :date org.joda.time.LocalDate
   :category Category
   :disqus-id schema/Str
   :draft schema/Bool
   :main-image (schema/maybe {:uri schema/Str
                              (schema/optional-key :artist) Artist})
   :content-markdown schema/Str
   :uri schema/Str
   :full-url schema/Str})

(defn get-category [kw]
  (schema/validate Category
    (let [found (kw categories)]
      (if found
        {:keyword kw
         :name found
         :uri (str "/blog/category/" (name kw) "/")}
        (throw (Exception. (str "Category not found: " kw)))))))

(defn frontmatter-date [date-str]
  (let [datetime (tformat/parse frontmatter-date-formatter date-str)]
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
  (schema/validate Post
    (let [[frontmatter md] (read-split-frontmatter file-content)
          [date uri-name] (split-post-filename path)
          category (get-category (:category frontmatter))]
      (as-> {} post
        (assoc post :title (:title frontmatter))
        (assoc post :disqus-id (:disqus-id frontmatter))
        (assoc post :main-image (:main-image frontmatter))
        (assoc post :draft (boolean (:draft frontmatter)))
        (assoc post :uri (str "/blog/" (name (:keyword category)) "/" uri-name "/"))
        (assoc post :full-url (str site-url (:uri post)))
        (assoc post :content-markdown md)
        (assoc post :date date)
        (assoc post :category category)))))

(defn get-posts []
  (reverse
    (sort-by :date
      (map #(apply build-post %)
           (stasis/slurp-directory input-dir #"^/blog-posts/.*\.markdown$")))))

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
                                       all-posts)])))

(defn post-category-pages [all-posts]
  (for [[category posts] (group-by :category all-posts)]
    [(:uri category)
     (templates/render-post-list posts
                                 (str "Category: " (:name category))
                                 (:uri category)
                                 all-posts)]))

(defn get-pages []
  (let [all-posts (get-posts)]
    (into {}
      (concat
        (post-category-pages all-posts)
        (post-archive-pages all-posts)
        (for [post all-posts]
          [(:uri post) (templates/render-post post all-posts)])
        [["/blog/" (templates/render-post-list all-posts "All Posts" "/blog/" all-posts)]
         ["/" (templates/render-page-html (slurp "resources/pages/home.html") "Home" "/" all-posts)]
         ["/feed/index.xml" (templates/render-rss (take 10 all-posts) "http://www.tomdalling.com")]]))))

(defn wrap-utf8 [handler]
  (fn [request]
    (let [response (handler request)]
      (if (or (= "text/html" (get-in response [:headers "Content-Type"]))
              (.endsWith (:uri request) ".xml"))
        (ring.util.response/charset response "UTF-8")
        response))))

(def app
  (wrap-utf8
    (optimus/wrap (stasis/serve-pages get-pages)
                  get-assets
                  (fn [assets _] (optimizations/concatenate-bundles assets))
                  serve-live-assets)))

(defn export []
  (let [assets (as-> (get-assets) a
                     (optimizations/all a {})
                     (remove :bundled a)
                     (remove #(not (:outdated %)) a))]
    (stasis/empty-directory! output-dir)
    (optimus.export/save-assets assets output-dir)
    (stasis/export-pages (get-pages) output-dir {:optimus-assets assets})))

