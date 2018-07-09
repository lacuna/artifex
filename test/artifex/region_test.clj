(ns artifex.region-test
  (:require
   [clojure.test :refer :all]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]
   [clojure.test.check.clojure-test :as ct :refer (defspec)])
  (:import
   [io.lacuna.artifex.utils
    Scalars]
   [io.lacuna.artifex
    Bezier2
    Region2
    Region2$Location
    Vec2
    Matrix3]))

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
      [#_(gen-shape :circle)
       (gen-shape :square)])
    (let [gen (gen-compound-shape (dec depth))]
      (gen/one-of
        [(gen-op :union gen)
         (gen-op :intersection gen)
         (gen-op :difference gen)]))))

(defn matrix [[tx ty sx sy]]
  (.mul
    (Matrix3/translate tx ty)
    (Matrix3/scale sx sy)))

(defn location [x]
  (condp = x
    Region2$Location/INSIDE  :inside
    Region2$Location/OUTSIDE :outside
    Region2$Location/EDGE    :edge))

(defn parse [[type & args]]
  (case type
    :circle       (.transform (Region2/circle) (matrix args))
    :square       (.transform (Region2/square) (matrix args))
    :union        (.union (parse (first args)) (parse (second args)))
    :difference   (.difference (parse (first args)) (parse (second args)))
    :intersection (.intersection (parse (first args)) (parse (second args)))))

(defn union [a b]
  (let [s (set [a b])]
    (cond
      (contains? s :unknown) :unknown
      (contains? s :inside) :inside
      (= s #{:edge})        :unknown
      (contains? s :edge)   :edge
      :else                 :outside)))

(defn intersection [a b]
  (if (contains? (set [a b]) :unknown)
    :unknown
    (condp = (set [a b])
      #{:inside}       :inside
      #{:edge :inside} :edge
      #{:edge}         :unknown
      :outside)))

(defn difference [a b]
  (if (contains? (set [a b]) :unknown)
    :unknown
    (condp = [a b]
      [:inside :edge]    :edge
      [:inside :outside] :inside
      [:edge :edge]      :unknown
      [:edge :outside]   :edge
      :outside)))

(defn test-point [[type & args :as descriptor] v]
  (case type
    (:square :circle) (-> descriptor parse (.test v) location)
    :union            (->> args
                        (map #(test-point % v))
                        (apply union))
    :difference       (->> args
                        (map #(test-point % v))
                        (apply difference))
    :intersection     (->> args
                        (map #(test-point % v))
                        (apply intersection))))

;;;

(defspec test-region-ops 1e4
  (prop/for-all [descriptor (gen-compound-shape 2)
                 points (gen/list
                          (gen/tuple (gen-float 0 1) (gen-float 0 1)))]
    #_(prn descriptor)
    (let [r (parse descriptor)]
      (every?
        (fn [[x y]]
          (let [v (Vec2. x y)
                expected (test-point descriptor v)]
            (or
              (= :unknown expected)
              (= (not= :outside expected) (.contains ^Region2 r v)))))
        points))))
