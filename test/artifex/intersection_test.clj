(ns artifex.intersection-test
  (:require
   [artifex.test-utils :refer :all]
   [clojure.test :refer :all])
  (:import
   [java.util
    Arrays]
   [io.lacuna.artifex
    Box
    Vec
    Vec2]
   [io.lacuna.artifex.utils
    Scalars
    DoubleAccumulator
    Intersections]))

(defn compare-intersections [expected f p q]
  (let [vs (->> [p q]
             (map #(apply curve (map (partial apply v) %)))
             (apply f)
             (mapcat unvertex))]
    (prn vs)
    (and (= (count expected) (count vs))
      (->> (map vector vs expected)
        (every? #(Scalars/equals (first %) (second %) Scalars/EPSILON))))))

(deftest test-line-curve-intersection
  (are [expected p q]
      (compare-intersections expected
        #(Intersections/lineCurve %1 %2 Scalars/EPSILON)
        p q)

    ;; single intersection
    [0.5 0.5]
    [[0 0] [1 1]]
    [[1 0] [0 1]]

    [2 -1]
    [[0 0] [1 1]]
    [[3 1] [4 0]]

    [0.5 0.5]
    [[0 0.5] [1 0.5]]
    [[0 0] [0.5 1] [1 0]]

    [0.5 0.5]
    [[0 0.75] [1 0.75]]
    [[0 0] [0.5 1] [0.5 1] [1 0]]

    ;; collinear
    [0 0 1 1]
    [[0 0] [1 1]]
    [[0 0] [1 1]]

    [0.5 0 1 0.5]
    [[0 0] [1 0]]
    [[0.5 0] [1.5 0]]))

(defn quantile [ary q]
  (aget ary (int (* (dec (alength ary)) q))))

(def n 1e3)

(deftest test-random-collinear-curves
  (doseq [degree [2 3 4]]
    (let [c (random-curve degree 0 1)
          [start end] (sort (repeatedly 2 #(+ 0.01 (* 0.98 (rand)))))]
      (when (< 1e-10 (- end start))
        (let [c' (second (.split c (double-array [start end])))]
          ())))))

(deftest test-random-intersections
  (doall
    (for [a-degree [2 3 4]
          b-degree [2 3 4]]
      (let [acc (DoubleAccumulator.)]
        (dotimes [_ n]
          (let [a             (random-curve a-degree 0 1)
                b             (random-curve b-degree 0 1)
                intersections (.intersections a b 1e-14)]
            (doseq [^Vec2 i intersections]
              (let [u (.position a (.x i))
                    v (.position b (.y i))]
                (.add acc (.length (.sub u v)))))))

        (let [ary (doto (.toArray acc) Arrays/sort)]
          (when-not (empty? ary)
            (prn a-degree b-degree (quantile ary 0.99999) (quantile ary 0.999))
            (is (< (quantile ary 0.99999) 1e-6))
            (is (< (quantile ary 0.999) 1e-10))))))))
