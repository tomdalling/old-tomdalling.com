(ns dowlgen.category
  (:require [schema.core :as s]))

(def schema {:keyword s/Keyword
             :name s/Str})

(def all
  (s/validate [schema]
    (map (fn [[kw n]] {:keyword kw :name n})
      {:software-design "Software Design"
       :coding-tips "Coding Tips"
       :cocoa "Cocoa"
       :coding-styleconventions "Coding Style/Conventions"
       :software-processes "Software Processes"
       :web "Web"
       :modern-opengl "Modern OpenGL Series"
       :random-stuff "Miscellaneous"})))

(defn uri [c]
  (str "/blog/category/" (-> c :keyword name) "/"))

(defn feed-uri [c]
  (str (uri c) "feed/"))

(defn uri-name [c]
  (name (:keyword c)))

(defn for-keyword [kw]
  (let [found (first (filter #(= kw (:keyword %)) all))]
    (if found
      found
      (throw (Exception. (str "Category not found for keyword: " kw))))))


