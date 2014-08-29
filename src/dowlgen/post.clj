(ns dowlgen.post
  (:require [schema.core :as s]
            [dowlgen.category :as category]
            [clj-time.core :as t]
            [clj-time.format :as tf]))

(def Artist
  "schema for artists of main images"
  {:name s/Str
   :url s/Str})

(def schema
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

(defn- frontmatter-date [date-str]
  (let [datetime (tf/parse (tf/formatter "yyyy-MM-dd") date-str)]
    (t/local-date (t/year datetime) (t/month datetime) (t/day datetime))))

(defn- read-split-frontmatter [s]
  (try 
    (let [reader (java.io.PushbackReader. (java.io.StringReader. s))
          frontmatter (clojure.edn/read reader)]
      (if (map? frontmatter)
        [frontmatter (slurp reader)]
        [nil s]))
    (catch Exception e [nil s])))

(defn- remove-extension [path]
  (let [dot-idx (.lastIndexOf path ".")]
    (if (= dot-idx -1)
      path
      (.substring path 0 dot-idx))))

(defn- last-path-component [path]
  (let [idx (.lastIndexOf path "/")]
    (if (= idx -1)
      path
      (.substring path (inc idx)))))

(defn- split-post-filename [fname]
  (let [[date-str uri-name] (clojure.string/split (last-path-component fname) #"_" 2)]
    [(frontmatter-date date-str) (remove-extension uri-name)]))

(defn build-post [site-url path file-content]
  (s/validate schema
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
