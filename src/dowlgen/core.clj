(ns dowlgen.core
  (:require [me.raynes.cegdown :as markdown]
            [dowlgen.string :as dstr]))

(def path-separator java.io.File/separator)
(def input-path ["site"])
(def output-path ["site-out"])

(defn path-split [path]
  (dstr/split path path-separator))

(defn path-join [& components]
  (->> (apply concat components)
       (dstr/join path-separator)))

(defn path-info [path]
  (let [components (path-split path)
        filename (last components)
        [base dot ext] (dstr/rpartition filename ".")]
    {:path path
     :dir (path-join (drop-last components))
     :filename filename
     :basename (if (empty? dot) filename base)
     :extension (if (empty? dot) "" ext)}))

(defn files-in [parent directory]
  (->> (clojure.java.io/file (path-join parent directory))
       (file-seq)
       (filter #(.isFile %))
       (map #(.getPath %))
       (map #(path-split %))
       (map #(drop (count parent) %))))

(defn -main
  []
  (doseq [f (files-in input-path ["blog"])]
    (let [in-path (path-join input-path f)
          out-path (path-join output-path f)]
      (println input-path (path-info in-path))))
  (println "Done."))

;;;; Idea for DSL ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; {path} = "wig/wam/boo.png"
; {dir} = "wig/wam"
; {basename} = "boo"
; {extension} = "png"
; {build-id} = "abasd24184sda" (unique per build)

#_(defn group-by-tags [fs]
    (let [tags (into #{} (mapcat fs #(attribute % "tags")))]
      (for [t tags]
        [t (filter #(has-tag t %) fs)])))

#_(dowlgen/site
    {:input-dir "input/"
     :output-dir "output/"}

    ;copy files without changing them
    (=> "static/**"
      (files "static/**")

    (=> "images/posts/{basename}_{width}x{height}.{extension}"
      ;take every file in the `images/posts/` directory
      (files "images/posts/*")
      ;create two resized copies
      (image-resize [[800 600] [640 480]])
      ;pngcrush the two copies
      (pngcrush))
    
    (=> "blog/{meta:urlname}.html"
      ;take every `.md` file in the `blog/` directory
      (files "blog/*.md")
      ;take metadata out of file (allows use of {meta:*})
      (extract-metadata)
      ;convert from markdown to html
      (md->html)
      ;find and insert code snippets
      (insert-snippets)
      ;combine into given layout
      (layout "_layouts/theme.html"))

    (=> "js/all_{build-id}.js"
      ;take all the coffeescript files in the `js/` dir
      (files "js/*.coffee")
      ;convert from coffeescript to javascript
      (coffee->js)
      (files "js/thirdparty/*")
      ;combine into a single file
      (concatenate)
      (minify))

    (=> "css/all_{build-id}.css"
      (files "css/*.scss")
      (scss->css)
      (files :prepend "css/bootstrap.css")
      (concatenate)
      (minify))
      
    (=> "rss/feed.xml"
      (files "blog/*.md")
      (extract-metadata)
      (sort-by #(attribute % "meta:date"))
      (take 10)
      (rss-feed)))

    (=> "index.html"
      (files "blog/*.md")
      (extract-metadata)
      (sort-by #(attribute % "meta:date"))
      (take 2)
      (shorten-post)
      (layout "_layouts/post_homepage.html")
      (concatenate)
      (layout "_layouts/homepage.html"))

    (=> "blog/category/{category}.html"
      (files "blog/*.md")
      (shorten-post)
      (group-by-tags)
      (category-page)
      (layout "_layouts/theme.html"))

    (=> "blog/archive/{year}/{month}.html"
      (files "blog/*.md")
      (shorten-post)
      (group-by-month)
      (archive-page)
      (layout "_layouts/theme.html")))
