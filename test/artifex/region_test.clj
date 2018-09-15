(ns artifex.region-test
  (:require
   [artifex.test-utils :refer :all]
   [clojure.test :refer :all]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]
   [clojure.test.check.clojure-test :as ct :refer (defspec)])
  (:import
   [io.lacuna.bifurcan
    LinearList
    List]
   [io.lacuna.artifex.utils
    Combinatorics
    Equations
    Scalars
    EdgeList
    Intersections]
   [io.lacuna.artifex.utils.regions
    Clip]
   [io.lacuna.artifex
    Interval
    Bezier2
    Curve2
    Region2
    Ring2
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

(defn simplify [x]
  (if (number? x)
    x
    (let [[type & args] x
          args (map simplify args)]
      (cond
        (= args [[:none] [:none]])
        [:none]

        (and
          (some #{[:none]} args)
          (= type :union))
        (->> args
          (remove #{[:none]})
          first)

        :else
        (vec (list* type args))))))

(defn gen-compound-shape [depth]
  (if (zero? depth)
    (gen/one-of
      [(gen/return [:none])
       (gen-shape :square)
       (gen-shape :circle)])
    (let [gen (gen-compound-shape (dec depth))]
      (gen/one-of
        [(gen-op :union gen)
         (gen-op :intersection gen)
         (gen-op :difference gen)]))))

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
  (when-not (or (nil? a) (nil? b))
    (or a b)))

(defn intersection [a b]
  (when-not (or (nil? a) (nil? b))
    (and a b)))

(defn difference [a b]
  (when-not (or (nil? a) (nil? b))
    (and a (not b))))

(defn test-point [[type & args :as descriptor] v]
  (case type
    :none             false
    (:square :circle) (let [r (.test (parse descriptor) v)]
                        (if (.curve r)
                          nil
                          (.inside r)))
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
  [(test-point x v) (.inside (.test (parse x) v))])

(defn spread [^Vec2 v delta]
  (for [x [-1 0 1]
        y [-1 0 1]]
    (.add v (Vec2. (* x delta) (* y delta)))))

;;;

#_[[:union [:intersection [:square 0.0 0.2 1.0 0.1] [:circle 0.0 0.0 0.7 1.0]] [:square 0.1 0.0 0.3 0.3]] ([0.5 0.3] [0.6 0.3])]

#_[[:union [:circle 0.8 0.3 0.5 0.2] [:intersection [:circle 0.7 0.3 0.6 0.1] [:circle 0.3 0.3 1.0 0.1]]] ([0.4 0.2] [0.5 0.2])]

(defspec test-region-ops 1e9
  (let [n (atom 0)]
    (prop/for-all [descriptor (->> (gen-compound-shape 4)
                                (gen/fmap simplify)
                                (gen/such-that #(not= % [:none])))
                   points (gen/fmap
                            distinct
                            (gen/vector
                             (gen/tuple (gen-float 0 1) (gen-float 0 1))
                             2 100))]
      (when (zero? (rem (swap! n inc) 1e6))
        (println (str (/ @n 1e6) "M")))
      #_(prn descriptor)
      (let [^Region2 r     (parse descriptor)
            invalid-points (->> points
                             (remove
                               (fn [[x y]]
                                 (let [v (Vec2. x y)]
                                   (if-let [expected (test-point descriptor v)]
                                     (->> (spread v (/ 0.1 resolution))
                                       (some #(= expected (.contains r %)))
                                       boolean)
                                     true)))))]
        (< (count invalid-points) 2)))))
