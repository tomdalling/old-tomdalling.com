(ns dowlgen.core
  (:require [me.raynes.cegdown :as markdown]
            [net.cgrand.enlive-html :as enlive]
            [net.cgrand.reload]
            [stasis.core :as stasis]
            [optimus.export]
            [optimus.assets :as assets]
            [optimus.optimizations :as optimizations]
            [optimus.prime :as optimus]
            [optimus.strategies :refer [serve-live-assets]]
            [optimus-sass.core]
            [clojure.data.json :as json]
            [clj-time.core :as clj-time]
            [clj-time.format :as time-format]))

(net.cgrand.reload/auto-reload *ns*)

(def input-dir "resources")
(def output-dir "dist")
(def site-url "http://www.tomdalling.com")
(def markdown-date-formatter (time-format/formatter "yyyy-MM-dd"))
(def human-date-formatter (time-format/formatter "dd MMM, yyyy"))

(defn map-map [f coll]
  (into {} (map #(apply f %) coll)))

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

(defn disqus-js [post]
  (when (:disqus-id post)
    (str "var disqus_shortname = 'tomdalling';\n"
         "var disqus_identifier = " (json/write-str (:disqus-id post)) ";\n"
         "var disqus_title = " (json/write-str (:title post)) ";\n"
         "var disqus_url = " (json/write-str (str site-url (:uri post))) ";\n")))

(defn build-post [path file-content]
  (let [[frontmatter md] (read-split-frontmatter file-content)]
    (as-> {} post
      (merge post frontmatter)
      (assoc post :uri (str (remove-extension path) "/"))
      (assoc post :content (markdown/to-html md [:autolinks :fenced-code-blocks :strikethrough]))
      (update-in post [:date] #(time-format/parse markdown-date-formatter %)))))

(defn get-posts []
  (map #(apply build-post %)
       (stasis/slurp-directory input-dir #"^/blog/.*\.markdown$")))

(def archive-month-formatter (time-format/formatter "MMM yyyy"))

(defn post-month [post]
  (time-format/unparse archive-month-formatter (:date post)))

(defn archived-posts []
  (group-by post-month (get-posts)))

(defn categorized-posts []
  (group-by :category (get-posts)))

(defn recent-posts [n]
  (take n
    (sort-by :date (get-posts))))

(enlive/deftemplate post-template "templates/post.html" [post]
  [[:link (enlive/attr= :rel "canonical")]] (enlive/set-attr :href (:uri post))
  [:title] (enlive/content (:title post))
  [:h1] (enlive/content (:title post))
  [:.post-date] (enlive/content (time-format/unparse human-date-formatter (:date post)))
  [:.post-content] (enlive/html-content (:content post))
  [:#disqus_script] (enlive/content (disqus-js post))
  [:ul.recent-posts :li] (enlive/clone-for [post (recent-posts 5)]
                                           [:a] (enlive/do-> (enlive/set-attr :href (:uri post))
                                                             (enlive/content (:title post))))
  [:ul.archives :li] (enlive/clone-for [[month posts] (archived-posts)]
                                       [:a] (enlive/content (str month " (" (count posts) ")")))
  [:ul.categories :li] (enlive/clone-for [[category posts] (categorized-posts)]
                                         [:a] (enlive/content (str category " (" (count posts) ")"))))


(defn get-assets []
  (concat (assets/load-assets "." ["/style.scss"])))

(defn get-pages []
  (into {}
    (for [post (get-posts)]
      [(str (:uri post) "index.html")
       (apply str (post-template post))])))

(def app
  (optimus/wrap (stasis/serve-pages get-pages)
                get-assets
                optimizations/all
                serve-live-assets))

(defn export []
  (let [assets (optimizations/all (get-assets) {})]
    (stasis/empty-directory! output-dir)
    (optimus.export/save-assets assets output-dir)
    (stasis/export-pages (get-pages) output-dir {:optimus-assets assets})))

