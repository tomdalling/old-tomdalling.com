(ns dowlgen.templates
  (:require [net.cgrand.enlive-html :refer
              [deftemplate defsnippet attr= set-attr content html-content
              clone-for do-> replace-vars text-node append html-snippet
              sniptest select pred wrap]]
            [net.cgrand.reload]
            [dowlgen.post :as post]
            [dowlgen.category :as category]
            [dowlgen.config :as config]
            [me.raynes.cegdown :as markdown]
            [clojure.data.json :as json]
            [clojure.data.xml :as xml]
            [clj-time.core :as t]
            [clygments.core :as clygments]
            [clj-time.format :as tformat]
            [clj-time.coerce :as tcoerce]))

(net.cgrand.reload/auto-reload *ns*)

(def human-date-formatter (tformat/formatter config/date-format))
(def archive-month-formatter (tformat/formatter config/month-format))

(defn unparse-yearmonth [yearmonth]
  (tformat/unparse-local archive-month-formatter (tcoerce/to-local-date yearmonth)))

(defn markdown-code-block? [node]
  (and (= 1 (-> node :content count)) ;; one child
       (= :code (-> node :content first :tag)) ;; the child is <code> tag
       (-> node :content first :attrs :class))) ;; the child must have a class

(defn highlight-code [pre-node]
  (let [code-node (-> pre-node :content first)
        code (->> code-node :content (apply str))
        lang (->> code-node :attrs :class keyword)]
    (assoc (first (html-snippet (clygments/highlight code lang :html)))
           :attrs {:class "highlight"})))

(defn xform-post-content [content]
  (sniptest content
    [[:pre (pred markdown-code-block?)]]
    highlight-code

    [:table]
    (set-attr :class "table table-hover table-bordered")))

(defn post-content [post]
  (-> post
      :content-markdown
      (markdown/to-html [:autolinks :fenced-code-blocks :strikethrough :tables])
      xform-post-content))

(defn post-shortened-content [post]
  (let [content (post-content post)
        idx (.indexOf content "<!--more-->")]
    (if (= idx -1)
      content ;; separator not found, so use whole content
      (.substring content 0 idx))))

(defn full-url [& uris]
  (apply str config/base-url uris))

(defsnippet post-single-snippet "templates/post-single.html" [:article] [post]
  [:h1 :a]
  (do-> (content (:title post))
        (set-attr :href (:uri post)))

  [:header :.main-image]
  (when (:main-image post)
    identity)

  [:header :.main-image :.credit]
  (when (-> post :main-image :artist)
    identity)

  [:header :.main-image :img]
  (set-attr :src (-> post :main-image :uri))

  [:header :.main-image :a.artist]
  (do-> (content (-> post :main-image :artist :name))
        (set-attr :href (-> post :main-image :artist :url)))

  [:header :a.category]
  (do-> (set-attr :href (category/uri (:category post)))
        (content (-> post :category :name)))

  [:.post-date]
  (content (tformat/unparse-local human-date-formatter (:date post)))

  [:.post-content]
  (html-content (post-content post))

  [:#disqus_script text-node]
  (replace-vars {:disqus-id (json/write-str (:disqus-id post))
                 :disqus-title (json/write-str (:title post))
                 :disqus-url (json/write-str (full-url (:uri post)))}))

(defsnippet post-list-snippet "templates/post-list.html" [:.post-list] [listed-posts title feed-uri]
  [:h1 :.title]
  (content title)

  [:h1 :a.feed]
  (when feed-uri
    (set-attr :href feed-uri))

  [:article]
  (clone-for [post listed-posts]
             [:h2 :a]
             (do-> (content (:title post))
                   (set-attr :href (:uri post)))

             [:header :a.category]
             (do-> (set-attr :href (category/uri (:category post)))
                   (content (-> post :category :name)))

             [:.listed-main-image]
             (set-attr :src (-> post :main-image :uri))
 
             [:.post-date]
             (content (tformat/unparse-local human-date-formatter (:date post)))
 
             [:.post-content]
             (html-content (post-shortened-content post))
 
             [:a.more]
             (set-attr :href (:uri post))))

(deftemplate page-template "templates/page.html" [page all-posts]
  [:head]
  (if (:uri page)
    (append {:tag :link
             :attrs {:rel "canonical" :href (:uri page)}})
    identity)

  [:title]
  (content (:title page) (html-snippet " &mdash; Tom Dalling"))

  [:main]
  (content (:content page))

  [:ul.recent-posts :li]
  (clone-for [post (take 5 all-posts)]
             [:a] (do-> (set-attr :href (:uri post))
                        (content (:title post))))

  [:ul.archives :li]
  (clone-for [[yearmonth posts] (post/archived all-posts)]
             [:a] (set-attr :href (post/archive-uri yearmonth))
             [:.month] (content (str (unparse-yearmonth yearmonth)))
             [:.post-count] (content (str (count posts))))

  [:ul.categories :li]
  (clone-for [[cat posts] (post/categorized all-posts)]
             [:a.category] (do-> (set-attr :href (category/uri cat))
                                 (content (:name cat)))
             [:a.feed] (set-attr :href (category/feed-uri cat))
             [:.post-count] (content (str (count posts))))

  [:.current-year]
  (content (-> (t/today) t/year str)))

(defn renderfn-post [post all-posts]
  (fn [_]
    (apply str
      (page-template {:uri (:uri post)
                      :title (:title post)
                      :content (post-single-snippet post)}
                     all-posts))))

(defn renderfn-post-list [listed-posts title uri feed-uri all-posts]
  (fn [_]
    (apply str
      (page-template {:uri uri
                      :title title
                      :content (post-list-snippet listed-posts title feed-uri)}
                     all-posts))))

(defn renderfn-page-html [page-html title uri all-posts]
  (fn [_]
    (apply str
      (page-template {:uri uri
                      :title title
                      :content (html-snippet page-html)}
                     all-posts))))

(defn rss-date-format [date]
  (tformat/unparse (tformat/formatters :rfc822)
                   (tcoerce/from-long (tcoerce/to-long date))))

(defn renderfn-rss [post-list feed-uri]
  (fn [_] 
    (xml/emit-str
      (xml/sexp-as-element
        [:rss {:version "2.0" :xmlns:sy "http://purl.org/rss/1.0/modules/syndication/" :xmlns:atom "http://www.w3.org/2005/Atom"}
          (conj
            [:channel
              [:title "Tom Dalling"]
              [:link (full-url "/?utm_source=rss&utm_medium=rss")]
              [:atom:link {:href (full-url feed-uri)
                           :rel "self"
                           :type "application/rss+xml"}]
              [:description "Web & software developer"]
              [:language "en"]
              [:generator "Tom Dalling's fingertips"]
              [:sy:updatePeriod "daily"]
              [:sy:updateFrequency "1"]]
            (for [post post-list]
              [:item
                [:title (:title post)]
                [:link (full-url (:uri post) "?utm_source=rss&utm_medium=rss")]
                [:description [:-cdata (post-shortened-content post)]]
                [:pubDate (rss-date-format (:date post))]
                [:category [:-cdata (-> post :category :name)]]
                [:guid {:isPermaLink "false"} (:disqus-id post)]]))]))))

