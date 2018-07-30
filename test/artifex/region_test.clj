(ns artifex.region-test
  (:require
   [artifex.test-utils :refer :all]
   [clojure.test :refer :all]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]
   [clojure.test.check.clojure-test :as ct :refer (defspec)])
  (:import
   [io.lacuna.artifex.utils
    Scalars
    Intersections]
   [io.lacuna.artifex
    Curve2
    Region2
    Ring2
    Ring2$Location
    Vec2
    Matrix3]))

(defn parse-curve [s]
  (->> s
    (re-seq #"=([-+]?[0-9]*\.?[0-9]+([eE][-+]?[0-9]+)?)")
    (map second)
    (map #(Double/parseDouble %))
    (partition 2)
    (map #(apply v %))
    (apply curve)))

(def resolution 1e1)

(defn gen-float [min max]
  (gen/fmap
    #(/ % resolution)
    (gen/choose (* min resolution) (* max resolution))))

(defn gen-shape [shape]
  (gen/fmap
    #(apply vector shape %)
    (gen/tuple
      (gen-float 0 1)
      (gen-float 0 1)
      (gen-float 0.1 1)
      (gen-float 0.1 1))))

(declare gen-compound-shape)

(defn gen-op [op gen]
  (gen/fmap
    #(apply vector op %)
    (gen/tuple gen gen)))

(defn gen-compound-shape [depth]
  (if (zero? depth)
    (gen/one-of
      [(gen/return [:none])
       (gen-shape :square)
       (gen-shape :circle)])
    (let [gen (gen-compound-shape (dec depth))]
      (gen/one-of
        [(gen-op :union gen)
         #_(gen-op :intersection gen)
         #_(gen-op :difference gen)]))))

(defn matrix [[tx ty sx sy]]
  (.mul
    (Matrix3/translate tx ty)
    (Matrix3/scale sx sy)))

(defn parse [[type & args]]
  (case type
    :none         (Region2. [])
    :circle       (.transform (.region (Ring2/circle)) (matrix args))
    :square       (.transform (.region (Ring2/square)) (matrix args))
    :union        (.union (parse (first args)) (parse (second args)))
    :difference   (.difference (parse (first args)) (parse (second args)))
    :intersection (.intersection (parse (first args)) (parse (second args)))))

(defn union [a b]
  (or a b))

(defn intersection [a b]
  (and a b))

(defn difference [a b]
  (and a (not b)))

(defn test-point [[type & args :as descriptor] v]
  (case type
    :none             false
    (:square :circle) (-> descriptor parse (.contains v))
    :union            (->> args
                        (map #(test-point % v))
                        (apply union))
    :difference       (->> args
                        (map #(test-point % v))
                        (apply difference))
    :intersection     (->> args
                        (map #(test-point % v))
                        (apply intersection))))

(defn compare-outcomes [x v]
  [(test-point x v) (.test (parse x) v)])

(defn spread [^Vec2 v delta]
  (for [x [-1 0 1]
        y [-1 0 1]]
    (.add v (Vec2. (* x delta) (* y delta)))))

;;;

(defspec test-region-ops 1e3
  (prop/for-all [descriptor (gen-compound-shape 1)
                 points (gen/list (gen/tuple (gen-float 0 1) (gen-float 0 1)))]
    #_(prn descriptor)
    (let [^Region2 r (parse descriptor)]
      (if (every?
            (fn [[x y]]
              (let [v        (Vec2. x y)
                    expected (test-point descriptor v)]
                (->> (spread v (/ 0.1 resolution))
                  (some #(= expected (.contains r %)))
                  boolean)))
            points)
        (do
          (prn '.)
          true)
        (do
          (prn 'fail)
          false)))))
