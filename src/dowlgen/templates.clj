(ns dowlgen.templates
  (:require [net.cgrand.enlive-html :refer
              [deftemplate defsnippet attr= set-attr content html-content
              clone-for do-> replace-vars text-node]]
            [net.cgrand.reload]
            [clojure.data.json :as json]
            [clj-time.core :as t]
            [clj-time.format :as tformat]
            [clj-time.coerce :as tcoerce]))

(net.cgrand.reload/auto-reload *ns*)

(def human-date-formatter (tformat/formatter "dd MMM, yyyy"))

(def archive-month-formatter (tformat/formatter "MMM yyyy"))

(defn post-yearmonth [post]
  (let [d (:date post)]
    (t/year-month (t/year d) (t/month d))))

(defn unparse-yearmonth [yearmonth]
  (tformat/unparse-local archive-month-formatter (tcoerce/to-local-date yearmonth)))

(defn archived-posts [posts]
  (reverse
    (sort-by first
      (group-by post-yearmonth posts))))

(defn categorized-posts [posts]
  (sort-by first
    (group-by :category posts)))

(defn recent-posts [posts n]
  (take n
    (reverse 
      (sort-by :date posts))))

(defn shortened-content [content]
  (let [idx (.indexOf content "<!--more-->")]
    (if (= idx -1)
      content ;; separator not found, so use whole content
      (.substring content 0 idx))))

(defsnippet post-single-snippet "templates/post-single.html" [:article] [post]
  [:h1]
  (content (:title post))

  [:.post-date]
  (content (tformat/unparse-local human-date-formatter (:date post)))

  [:.post-content]
  (html-content (:content post))

  [:#disqus_script text-node]
  (replace-vars {:disqus-id (json/write-str (:disqus-id post))
                 :disqus-title (json/write-str (:title post))
                 :disqus-url (json/write-str (:full-url post))}))

(defsnippet post-list-snippet "templates/post-list.html" [:article] [post]
  [:h2]
  (content (:title post))

  [:.post-date]
  (content (tformat/unparse-local human-date-formatter (:date post)))

  [:.post-content]
  (html-content (shortened-content (:content post)))

  [:a.more]
  (set-attr :href (:uri post)))

(deftemplate page-template "templates/page.html" [page all-posts]
  [[:link (attr= :rel "canonical")]]
  (set-attr :href (:uri page))

  [:title]
  (content (:title page))

  [:main]
  (content (:content page))

  [:ul.recent-posts :li]
  (clone-for [post (recent-posts all-posts 5)]
             [:a] (do-> (set-attr :href (:uri post))
                        (content (:title post))))

  [:ul.archives :li]
  (clone-for [[yearmonth posts] (archived-posts all-posts)]
             [:a] (content (str (unparse-yearmonth yearmonth)
                                " (" (count posts) ")")))

  [:ul.categories :li]
  (clone-for [[category posts] (categorized-posts all-posts)]
             [:a] (content (str category " (" (count posts) ")"))))

(defn render-post [post all-posts]
  (apply str
    (page-template {:uri (:uri post)
                    :title (:title post)
                    :content (post-single-snippet post)}
                   all-posts)))
