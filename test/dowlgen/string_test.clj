(ns dowlgen.string-test
  (:require [midje.sweet :refer :all]
            [dowlgen.string :refer :all]))

(fact "starts-with?"
  (starts-with? "hello there" "hello") => truthy
  (starts-with? "hello there" "") => truthy
  (starts-with? "hello there" "nope") => falsey
  (starts-with? "" "nope") => falsey
  (starts-with? "" "") => truthy)

(fact "ends-with?"
  (ends-with? "hello there" "there") => truthy
  (ends-with? "hello there" "") => truthy
  (ends-with? "hello there" "nope") => falsey
  (ends-with? "" "nope") => falsey
  (ends-with? "" "") => truthy)

(fact "next-match (string matcher)"
  (next-match "ll" "hello" 0) => {:start 2 :end 4}
  (next-match "h" "hello" 0)  => {:start 0 :end 1}
  (next-match "W" "hello" 0)  => falsey)

(fact "next-match (regex matcher)"
  (next-match #"l+" "hello" 0) => {:start 2 :end 4}
  (next-match #"h" "hello" 0)  => {:start 0 :end 1}
  (next-match #"W" "hello" 0)  => falsey)

(fact "empty-matcher?"
  (empty-matcher? "") => truthy
  (empty-matcher? "hello") => falsey)

(fact "last-match"
  (last-match "," "a,b,c") => {:start 3 :end 4}
  (last-match "X" "abc") => falsey)

(fact "split-inclusive"
  (split-inclusive "a1b23c4" #"\d+") => ["a" "1" "b" "23" "c" "4" ""])

(fact "split"
  (split "1,2,3" ",") => ["1" "2" "3"]
  (split ",1,2,3," ",") => ["" "1" "2" "3" ""]
  (split "abc" "") => ["a" "b" "c"])

(fact "join"
  (join ["a" "b" "cdef"]) => "abcdef")

(fact "lpartition"
  (lpartition "file.whatever.clj" ".") => ["file" "." "whatever.clj"]
  (lpartition "file.whatever.clj" "")  => ["" "" "file.whatever.clj"]
  (lpartition "file.whatever.clj" "!") => ["file.whatever.clj" "" ""])

(fact "rpartition"
  (rpartition "file.whatever.clj" ".") => ["file.whatever" "." "clj"]
  (rpartition "file.whatever.clj" "")  => ["file.whatever.clj" "" ""]
  (rpartition "file.whatever.clj" "!") => ["" "" "file.whatever.clj"])

(fact "template"
  (template "{fname} {lname} is feeling {mood}."
            {"fname" "Alice", "lname" "Brown", "mood" "crunk"})
    => "Alice Brown is feeling crunk."
  (template "This doesn't exist: {nope}" {} "???")
    => "This doesn't exist: ???")


