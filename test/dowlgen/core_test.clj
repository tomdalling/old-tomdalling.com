(ns dowlgen.core-test
  (:require [midje.sweet :refer :all]
            [dowlgen.core :refer :all]))

(facts "about read-split-frontmatter"
    (read-split-frontmatter "{:k 5} Rest") => [{:k 5} " Rest"]
    (read-split-frontmatter "{} blam")     => [{} " blam"]
    (read-split-frontmatter "Wah blah")    => [nil "Wah blah"]
    (read-split-frontmatter "[5] novecs")  => [nil "[5] novecs"]
    (read-split-frontmatter "} moo")       => [nil "} moo"])

(facts "about remove-extension"
  (remove-extension "moo.jpg")     => "moo"
  (remove-extension "moo.tar.bz2") => "moo.tar"
  (remove-extension "moo")         => "moo")

(facts "about change-extension"
  (change-extension "moo.jpg" "exe")      => "moo.exe"
  (change-extension "moo.tar.bz2" "blam") => "moo.tar.blam"
  (change-extension "moo" "cat")          => "moo.cat")

(facts "about get-posts"
  (let [posts (get-posts)]
    (fact "each post has a unique :disqus-id"
      (->> posts (map :disqus-id) set count) => (count posts))
    (fact "each post has a unique :uri"
      (->> posts (map :uri) set count) => (count posts))
    (fact "each post has a <!--more--> separator"
      (doseq [p posts]
        [(:uri p) (-> p :content-markdown (.indexOf "<!--more-->") (= -1))] => [(:uri p) false]))))
