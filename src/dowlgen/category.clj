(ns dowlgen.category
  (:require [schema.core :as s]
            [dowlgen.config :as config]))

(def schema {:keyword s/Keyword
             :name s/Str})

(def all
  (s/validate [schema]
    (for [[kw n] config/categories]
      {:keyword kw :name n})))

(defn uri-name [c]
  (name (:keyword c)))

(defn uri [c]
  (str "/blog/category/" (uri-name c) "/"))

(defn feed-uri [c]
  (str (uri c) "feed/"))

(defn for-keyword [kw]
  (let [found (first (filter #(= kw (:keyword %)) all))]
    (if found
      found
      (throw (Exception. (str "Category not found for keyword: " kw))))))

