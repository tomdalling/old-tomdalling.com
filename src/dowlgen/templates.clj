(ns dowlgen.templates
  (:require [net.cgrand.enlive-html :refer
              [deftemplate attr= set-attr content html-content clone-for do->]]
            [net.cgrand.reload]
            [clojure.data.json :as json]
            [clj-time.core :as clj-time]
            [clj-time.format :as time-format]))

(net.cgrand.reload/auto-reload *ns*)

(def human-date-formatter (time-format/formatter "dd MMM, yyyy"))

(def archive-month-formatter (time-format/formatter "MMM yyyy"))

(defn post-month [post]
  (time-format/unparse archive-month-formatter (:date post)))

(defn archived-posts [posts]
  (group-by post-month posts))

(defn categorized-posts [posts]
  (group-by :category posts))

(defn recent-posts [posts n]
  (take n
    (sort-by :date posts)))

(defn disqus-js [post]
  (str "var disqus_shortname = 'tomdalling';\n"
       "var disqus_identifier = " (json/write-str (:disqus-id post)) ";\n"
       "var disqus_title = " (json/write-str (:title post)) ";\n"
       "var disqus_url = " (json/write-str (:full-url post)) ";\n"))

(deftemplate post-template "templates/post.html" [post all-posts]
  [[:link (attr= :rel "canonical")]]
  (set-attr :href (:uri post))

  [:title]
  (content (:title post))

  [:h1]
  (content (:title post))

  [:.post-date]
  (content (time-format/unparse human-date-formatter (:date post)))

  [:.post-content]
  (html-content (:content post))

  [:#disqus_script]
  (content (disqus-js post))

  [:ul.recent-posts :li]
  (clone-for [post (recent-posts all-posts 5)]
             [:a] (do-> (set-attr :href (:uri post))
                        (content (:title post))))

  [:ul.archives :li]
  (clone-for [[month posts] (archived-posts all-posts)]
             [:a] (content (str month " (" (count posts) ")")))

  [:ul.categories :li]
  (clone-for [[category posts] (categorized-posts all-posts)]
             [:a] (content (str category " (" (count posts) ")"))))
