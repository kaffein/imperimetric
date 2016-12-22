(ns imperimetric.handler-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [imperimetric.handlers :refer [make-ounces-fluid failed-response-handler
                                           from-button-clicked-handler to-button-clicked-handler
                                           text-wait-over-handler]]
            [pjstadig.humane-test-output]))

(deftest make-ounces-fluid-test
  (testing "make-ounces-fluid"
    (testing "converts 'oz' to 'fl. oz'"
      (is (= "1000 fl. oz, 3 fl. oz"
             (make-ounces-fluid "1000 oz, 3 oz"))))
    (testing "does not convert if text contains fluid ounces"
      (is (= "1000 fl. oz, 3 oz"
             (make-ounces-fluid "1000 fl. oz, 3 oz"))))))

(deftest failed-response-handler-test
  (testing "failed-response-handler"
    (testing "removes loading and adds an error text"
      (is (= {:converted-text "Something went wrong when converting text :("}
             (failed-response-handler {:loading? true} [{:status 404 :status-text ""}]))))))

(deftest button-click-handlers-test
  (testing "from-button-clicked-handler"
    (testing "performs an api call when the from system changes and updates db"
      (is (= {:db               {:from-system :imperial
                                 :to-system   :metric
                                 :loading?    true
                                 :text        "3 oz"}
              :api-convert-call [:imperial :metric "3 oz"]}
             (from-button-clicked-handler {:db {:from-system :us
                                                :to-system   :metric
                                                :text        "3 oz"}}
                                          [:imperial])))))
  (testing "to-button-clicked-handler"
    (testing "performs an api call when the from system changes and updates db"
      (is (= {:db               {:from-system :us
                                 :to-system   :imperial
                                 :loading?    true
                                 :text        "3 oz"}
              :api-convert-call [:us :imperial "3 oz"]}
             (to-button-clicked-handler {:db {:from-system :us
                                              :to-system   :metric
                                              :text        "3 oz"}}
                                        [:imperial]))))))

(deftest text-wait-over-handler-test
  (testing "text-wait-over-handler"
    (testing "doesn't return any effects when timestamp is not the latest one"
      (is (= nil
             (text-wait-over-handler {:db {:latest-text-timestamp 2
                                           :text                  "3 oz"
                                           :from-system           :us
                                           :to-system             :metric}}
                                     [1]))))
    (testing "makes an api call if the timestamp is the latest one"
      (is (= {:db               {:latest-text-timestamp 2
                                 :latest-requested-text "3 oz"
                                 :loading?              true
                                 :text                  "3 oz"
                                 :from-system           :us
                                 :to-system             :metric}
              :api-convert-call [:us :metric "3 oz"]}
             (text-wait-over-handler {:db {:latest-text-timestamp 2
                                           :text                  "3 oz"
                                           :from-system           :us
                                           :to-system             :metric}}
                                     [2]))))))