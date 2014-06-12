(ns dowlgen.string)
  ;:refer-clojure :exclude [format])

(defn starts-with? [s prefix]
  (.startsWith s prefix))

(defn ends-with? [s suffix]
  (.endsWith s suffix))

(defprotocol IMatcher
  (next-match [matcher s start-index]))

(extend-protocol IMatcher String
  (next-match [matcher s start-index]
    (let [found-index (.indexOf s matcher start-index)]
      (when (>= found-index 0)
        {:start found-index,
         :end (+ found-index (count matcher))}))))

(extend-protocol IMatcher java.util.regex.Pattern
  (next-match [matcher s start-index]
    (let [m (re-matcher matcher s)]
      (if (.find m start-index)
        {:start (.start m) :end (.end m)}
        false))))

(defn empty-matcher? [matcher]
  (let [m (next-match matcher "X" 0)]
    (when m
      (= 0 (:start m) (:end m)))))

(defn last-match [matcher s]
  (loop [index (count s)]
    (when (>= index 0)
      (let [m (next-match matcher s index)]
        (if m m (recur (dec index)))))))

(defn split-inclusive [s matcher]
  (if (empty-matcher? matcher)
    (map str (drop-last (interleave (seq s) (repeat ""))))
    (loop [index 0, components []]
      (let [match (next-match matcher s index)]
        (if-not match
          (conj components (subs s index))
          (recur (:end match)
                 (conj components
                       (subs s index (:start match))
                       (subs s (:start match) (:end match)))))))))

(defn split [s matcher]
  (take-nth 2 (split-inclusive s matcher)))

(def join clojure.string/join)

(defn lpartition [s matcher]
  (if (empty-matcher? matcher)
    ["" "" s]
    (let [m (next-match matcher s 0)]
      (if-not m
        [s "" ""]
        [(subs s 0 (:start m))
         (subs s (:start m) (:end m))
         (subs s (:end m))]))))

(defn rpartition [s matcher]
  (if (empty-matcher? matcher)
    [s "" ""]
    (let [m (last-match matcher s)]
      (if-not m
        ["" "" s]
        [(subs s 0 (:start m))
         (subs s (:start m) (:end m))
         (subs s (:end m))]))))

(defn template
  ([fmt substitutions]
    (template fmt substitutions ""))
  ([fmt substitutions missing]
    (let [parts (split-inclusive fmt #"\{[_a-zA-Z]+\}")
          literal-parts (take-nth 2 parts)
          unsubbed-parts (take-nth 2 (drop 1 parts))
          subbed-parts (for [p unsubbed-parts]
                         (get substitutions
                              (subs p 1 (dec (count p))) ;strip curlies
                              missing))
          reinterleaved (interleave literal-parts subbed-parts)]
      (join (concat reinterleaved [(last literal-parts)])))))
