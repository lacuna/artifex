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
    Vec2
    Curve2
    Line2]
   [io.lacuna.artifex.utils
    Scalars
    DoubleAccumulator
    Intersections]))

(defn compare-intersections [expected f p q]
  (let [vs (->> [p q]
             (map #(apply curve (map (partial apply v) %)))
             (apply f)
             (mapcat unvertex))]
    (and (= (count expected) (count vs))
      (->> (map vector vs expected)
        (every? #(Scalars/equals (first %) (second %) Intersections/SPATIAL_EPSILON))))))

(deftest test-line-curve-intersection
  (are [expected p q]
      (compare-intersections expected
        #(.intersections ^Curve2 %1 %2)
        p q)

    ;; single intersection
    [0.5 0.5]
    [[0 0] [1 1]]
    [[1 0] [0 1]]

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

(def n 1e5)

(deftest test-random-collinear-curves
  (doseq [degree [2 3 4]]
    (let [c (random-curve degree 0 1)
          [start end] (sort (repeatedly 2 #(+ 0.01 (* 0.98 (rand)))))]
      (when (< 1e-10 (- end start))
        (let [c' (second (.split c (double-array [start end])))]
          ())))))

(defn examine-intersections [a b]
  (let [a (parse-curve a)
        b (parse-curve b)]
    (doseq [v (.intersections a b)]
      (let [pa (.position a (.x v))
            pb (.position b (.y v))]
        (prn v)
        (prn pa)
        (prn pb)
        (prn (-> pa (.sub pb) .length))))))

(defn approx-curve [^Curve2 c n]
  (->> (range (inc n))
    (map #(.position c (/ % n)))
    (partition 2 1)
    (map #(Line2/from (first %) (second %)))
    (map vector (range))
    vec))

(defn approx-intersections [a b n]
  (let [as (approx-curve a n)
        bs (approx-curve b n)]
    (->>
      (for [[ia a] as
            [ib b] bs]
        (let [box (Box/box
                    (v (/ ia (count as)) (/ ib (count bs)))
                    (v (/ (inc ia) (count as)) (/ (inc ib) (count bs))))]
          (->> (.intersections a b)
            (map #(.lerp box %)))))
      (apply concat)
      distinct)))

(deftest test-random-intersections
  (doall
    (for [a-degree [2 3 4]
          b-degree [2 3 4]
          :when    (<= a-degree b-degree)]
      (let [l (java.util.ArrayList.)]
        (time
          (dotimes [_ n]
            (let [^Curve2 a     (random-curve a-degree 0 1)
                  ^Curve2 b     (random-curve b-degree 0 1)
                  intersections (.intersections a b)]
              (doseq [^Vec2 i intersections]
                (let [u (.position a (.x i))
                      v (.position b (.y i))
                      dist (.length (.sub u v))]
                  (.add l [dist (when (< 1e-5 dist)
                                  [a b])]))))))

        (let [ary (.toArray l)]
          (Arrays/sort ary (comparator #(< (first %1) (first %2))))
          (when-not (empty? ary)
            (prn a-degree b-degree)
            (prn (count ary) (first (quantile ary 0.99999)) (first (quantile ary 0.999)))
            (prn (last ary))
            (prn)
            (is (< (first (quantile ary 0.99999)) 1e-6) (pr-str (last ary)))
            (is (< (first (quantile ary 0.999)) 1e-10) (pr-str (last ary)))))))))
