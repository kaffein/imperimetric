(ns imperimetric.convert
  (:require [instaparse.core :as insta]
            [clojure.string :as str]
            [imperimetric.util :refer [map-all-to]]
            [frinj.ops :refer [fj]]))

(def parsers
  {:metric   (insta/parser "src/clj/imperimetric/metric-grammar.bnf")
   :us       (insta/parser "src/clj/imperimetric/us-grammar.bnf")
   :imperial (insta/parser "src/clj/imperimetric/us-grammar.bnf")})

(def numeral->int
  {"one"       1 "two" 2 "three" 3 "four" 4 "five" 5 "six" 6 "seven" 7 "eight" 8 "nine" 9
   "ten"       10 "eleven" 11 "twelve" 12 "thirteen" 13 "fourteen" 14 "fifteen" 15 "sixteen" 16
   "seventeen" 17 "eighteen" 18 "nineteen" 19 "twenty" 20 "thirty" 30 "forty" 40 "fifty" 50
   "sixty"     60 "seventy" 70 "eighty" 80 "ninety" 90})

(defn parse-recipe [recipe from-system]
  ((parsers from-system) recipe))

(defn decimal-round [n]
  (format "%.1f" n))

(defn convert-units [from to q]
  (double (:v (fj q from :to to))))

(defn convert-str [from to quantity suffix]
  (str (decimal-round (convert-units from to quantity)) " " suffix))

(defmulti convert
  (fn [from-system to-system quantity unit] [from-system to-system unit]))

;; US customary units
(defmethod convert [:us :metric :cup] [_ _ q _] (convert-str :cup :dl q "dl"))
(defmethod convert [:us :metric :oz] [_ _ q _] (convert-str :floz :cl q "cl"))
(defmethod convert [:us :metric :tablespoon] [_ _ q _] (convert-str :tbsp :ml q "ml"))
(defmethod convert [:us :metric :teaspoon] [_ _ q _] (convert-str :tsp :ml q "ml"))

(defmethod convert [:us :imperial :cup] [_ _ q _] (convert-str :cup :brcup q "cups"))
(defmethod convert [:us :imperial :oz] [_ _ q _] (convert-str :floz :brfloz q "oz"))
(defmethod convert [:us :imperial :tablespoon] [_ _ q _] (convert-str :tbsp :brtablespoon q "tbsp"))
(defmethod convert [:us :imperial :teaspoon] [_ _ q _] (convert-str :tsp :brtsp q "tsp"))

;; Imperial
(defmethod convert [:imperial :metric :cup] [_ _ q _] (convert-str :brcup :dl q "dl"))
(defmethod convert [:imperial :metric :oz] [_ _ q _] (convert-str :brfloz :cl q "cl"))
(defmethod convert [:imperial :metric :tablespoon] [_ _ q _] (convert-str :brtablespoon :ml q "ml"))
(defmethod convert [:imperial :metric :teaspoon] [_ _ q _] (convert-str :brtsp :ml q "ml"))

(defmethod convert [:imperial :us :cup] [_ _ q _] (convert-str :brcup :cup q "cups"))
(defmethod convert [:imperial :us :oz] [_ _ q _] (convert-str :brfloz :floz q "oz"))
(defmethod convert [:imperial :us :tablespoon] [_ _ q _] (convert-str :brtablespoon :tbsp q "tbsp"))
(defmethod convert [:imperial :us :teaspoon] [_ _ q _] (convert-str :brtsp :tsp q "tsp"))

;; Metric
(defmethod convert [:metric :us :l] [_ _ q _] (convert-str :liter :cup q "cups"))
(defmethod convert [:metric :us :dl] [_ _ q _] (convert-str :dl :cup q "cups"))
(defmethod convert [:metric :us :cl] [_ _ q _] (convert-str :cl :floz q "oz"))
(defmethod convert [:metric :us :ml] [_ _ q _] (convert-str :ml :tsp q "tsp"))

(defmethod convert [:metric :imperial :l] [_ _ q _] (convert-str :liter :brcup q "cups"))
(defmethod convert [:metric :imperial :dl] [_ _ q _] (convert-str :dl :brcup q "cups"))
(defmethod convert [:metric :imperial :cl] [_ _ q _] (convert-str :cl :brfloz q "oz"))
(defmethod convert [:metric :imperial :ml] [_ _ q _] (convert-str :ml :brtsp q "tsp"))

(defn transform-map [from-system to-system]
  (merge
    (map-all-to [:recipe :token :word :whitespace] str)
    (map-all-to [:integer :fraction :decimal] read-string)
    (map-all-to [:quantity :numeral :20-99] identity)
    (map-all-to [:1-9 :10-19 :base] (comp numeral->int str/lower-case))
    (map-all-to [:base-with-suffix :mixed] +)
    {:measurement (partial convert from-system to-system)
     :unit        first}))

(defn convert-recipe [recipe from-system to-system]
  (let [parsed (parse-recipe recipe from-system)]
    (if (insta/failure? parsed)
      nil
      (str/join (insta/transform (transform-map from-system to-system) parsed)))))
