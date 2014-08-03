(ns dowlgen.templates
  (:require [net.cgrand.enlive-html :refer
              [deftemplate attr= set-attr content html-content clone-for do->]]
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
  (content (tformat/unparse-local human-date-formatter (:date post)))

  [:.post-content]
  (html-content (:content post))

  [:#disqus_script]
  (content (disqus-js post))

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
