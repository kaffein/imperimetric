(ns clj.convert-test
  (:require [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [imperimetric.convert :refer [convert-text parse-text]]
            [imperimetric.handler :refer [handler]]
            [ring.mock.request :refer [request header]]
            [imperimetric.util :refer [map-all-to]]
            [frinj.jvm :refer [frinj-init!]]
            [clojure.string :as str]))

(frinj-init!)

(def us-text (str "Four cups sugar, 1 1/2 Fluid Ounces lime, 5 tbsps salt, Twenty-five teaspoons pepper, "
                  "½ gallon water, three pints beer, 2 quarts milk, 2 gill gin, nine miles away, 3 yards away, "
                  "2 1/2 feet away, 2 inches away, 2 pounds, 2 ounces, 4 tons."))
(def metric-text (str "Four liters sugar, .90 Decilitres lime, 5 cL salt, Twenty-five ml pepper, nine km away, "
                      "9 meters away, 2 centimetres away, 120 millimetre away, 2 kg, 2 hg, 2 Grams, 2 milligrams, 4 tons."))

(facts "About map-all-to"
  (fact "empty vector gives empty map"
        (map-all-to [] "test") => {})
  (fact "correctly maps all input keywords to value"
        (map-all-to [:recipe :token :word] "test") => {:recipe "test"
                                                       :token  "test"
                                                       :word   "test"}))


(facts "About conversion properties"
  (fact "Empty conversion results in nil"
        (convert-text "" :us :metric) => nil)
  (fact "Conversions use correct precision (number of significant digits)"
        (convert-text "42195 m, 42.195 km" :metric :us) => "46145 yards, 26.219 miles")
  (fact "Conversions which result in exactly 1 use singular units"
        (convert-text "0.918 m" :metric :us) => "1.00 yard")
  (fact "Combined units result in single correct converted unit"
        (convert-text "6 feet 4 ½ in tall, weight: 8 lb 3 oz." :us :metric) =>
        "1.94 m tall, weight: 3.71 kg.")
  (fact "Does not try to parse division by zero"
        (convert-text "1/0 oz." :us :metric) => "1/0 oz.")
  (fact "Converts units in parens"
        (convert-text "(1/4 oz)" :us :metric) "(7.09 g)"))


(facts "About conversions from US customary units"
  (fact "Correctly converts to metric"
        (convert-text us-text :us :metric) =>
        (str "9.46 dl sugar, 4.44 cl lime, 73.9 ml salt, 123 ml pepper, 1.89 l water, 1.42 l beer, 1.89 l milk, 23.7 cl"
             " gin, 14.5 km away, 2.74 m away, 0.762 m away, 5.08 cm away, 0.907 kg, 56.7 g, 3.63 tonnes."))
  (fact "Correctly converts to Imperial"
        (convert-text us-text :us :imperial) =>
        (str "3.33 cups sugar, 1.56 fl. oz lime, 4.93 tbsp salt, 24.6 tsp pepper, 0.416 gallons water, 2.50 pints beer, 1.67"
             " quarts milk, 1.67 gills gin, nine miles away, 3 yards away, 2 1/2 feet away, 2 inches away, 2 pounds, 2"
             " ounces, 3.57 tons.")))


(facts "About conversions from Imperial"
  (fact "Correctly converts to metric"
        (convert-text us-text :imperial :metric) =>
        (str "11.4 dl sugar, 4.26 cl lime, 75 ml salt, 125 ml pepper, 2.27 l water, 1.70 l beer,"
             " 2.27 l milk, 28.4 cl gin, 14.5 km away, 2.74 m away, 0.762 m away, 5.08 cm away, 0.907 kg, 56.7 g, 4.06 tonnes."))
  (fact "Correctly converts to US customary units"
        (convert-text us-text :imperial :us) =>
        (str "4.80 cups sugar, 1.44 fl. oz lime, 5.07 tbsp salt, 25.4 tsp pepper, 0.600 gallons water, 3.60 pints beer,"
             " 2.40 quarts milk, 2.40 gills gin, nine miles away, 3 yards away, 2 1/2 feet away, 2 inches away, 2 pounds,"
             " 2 ounces, 4.48 tons.")))


(facts "About conversions from metric"
  (fact "Correctly converts to US customary units"
        (convert-text metric-text :metric :us) =>
        (str "8.45 pints sugar, 0.380 cups lime, 1.69 fl. oz salt, 5.07 tsp pepper, 5.59 miles away,"
             " 9.84 yards away, 0.787 inches away, 4.72 inches away, 4.41 pounds, 7.05 oz, 0.0705 oz, 0.0000705 oz,"
             " 4.41 tons."))
  (fact "Correctly converts to Imperial"
        (convert-text metric-text :metric :imperial) =>
        (str "7.04 pints sugar, 0.317 cups lime, 1.76 fl. oz salt, 5 tsp pepper, 5.59 miles"
             " away, 9.84 yards away, 0.787 inches away, 4.72 inches away, 4.41 pounds, 7.05 oz, 0.0705 oz, 0.0000705"
             " oz, 3.94 tons.")))


(facts "About api conversions"
  (fact "A typical api call returns success and correct json"
        (handler (request :get "/convert" {"text" "1 fl. oz of water."
                                           "from" "us"
                                           "to"   "metric"})) =>
        {:status  200
         :headers {"Content-Type" "application/json;charset=UTF-8"
                   "Vary"         "Accept"}
         :body    "{\"converted-text\":\"2.96 cl of water.\",\"original-text\":\"1 fl. oz of water.\"}"})
  (fact "A very long URL results in a uri too long response"
        (handler (request :get "/convert" {"text" (str/join (repeat 3000 "a"))
                                           "from" "us"
                                           "to"   "metric"})) =>
        {:status  414
         :headers {"Content-Type" "text/plain;charset=UTF-8"}
         :body    "Request URI too long."}))
