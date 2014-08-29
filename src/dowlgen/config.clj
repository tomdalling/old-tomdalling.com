(ns dowlgen.config)

(def base-url "http://www.tomdalling.com")

(def categories
  {:software-design "Software Design"
   :coding-tips "Coding Tips"
   :cocoa "Cocoa"
   :coding-styleconventions "Coding Style/Conventions"
   :software-processes "Software Processes"
   :web "Web"
   :modern-opengl "Modern OpenGL Series"
   :random-stuff "Miscellaneous"})

(def page-duplicates
  "Map from duplicate uri to canonical/original uri"
  {"/feed/index.xml" "/blog/feed/index.xml"
   ;; /blog/X <= /blog/category/X
   "/blog/software-design/" "/blog/category/software-design/"
   "/blog/coding-tips/" "/blog/category/coding-tips/"
   "/blog/cocoa/" "/blog/category/cocoa/"
   "/blog/coding-styleconventions/" "/blog/category/coding-styleconventions/"
   "/blog/software-processes/" "/blog/category/software-processes/"
   "/blog/web/" "/blog/category/web/"
   "/blog/modern-opengl/" "/blog/category/modern-opengl/"
   "/blog/random-stuff/" "/blog/category/random-stuff/"})

