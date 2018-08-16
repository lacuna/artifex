(ns artifex.test-utils
  (:import
   [io.lacuna.artifex.utils
    Intersections
    Scalars
    EdgeList
    Equations]
   [java.util.function
    ToDoubleFunction
    Function
    BiPredicate]
   [io.lacuna.artifex
    Box
    Box2
    Region2
    Ring2
    Line2
    Curve2
    Bezier2
    Region2
    Path2
    Vec
    Vec2
    Vec3
    Vec4
    Matrix3
    Matrix4]))

(defn to-double-function [f]
  (reify ToDoubleFunction
    (applyAsDouble [_ x]
      (f x))))

(defn function [f]
  (reify Function
    (apply [_ x]
      (f x))))

(defn bipredicate [f]
  (reify BiPredicate
    (test [_ a b]
      (f a b))))

(defn v
  ([^double x ^double y]
   (Vec2. x y))
  ([^double x ^double y ^double z]
   (Vec3. x y z))
  ([^double x ^double y ^double z ^double w]
   (Vec4. x y z w)))

(defn curve
  ([a b]
   (Bezier2/curve a b))
  ([a b c]
   (Bezier2/curve a b c))
  ([a b c d]
   (Bezier2/curve a b c d)))

(defn region [rings]
  (Region2. (map #(Ring2. %) rings)))

(defn unvertex [v]
  (condp instance? v
    Vec2 [(.x v) (.y v)]
    Vec3 [(.x v) (.y v) (.z v)]
    Vec4 [(.x v) (.y v) (.z v) (.w v)]))

(defn parse-curve [s]
  (->> s
    (re-seq #"=([-+]?[0-9]*\.?[0-9]+([eE][-+]?[0-9]+)?)")
    (map second)
    (map #(Double/parseDouble %))
    (partition 2)
    (map #(apply v %))
    (apply curve)))

;;;

(defn random-vector [min max]
  (let [x (+ min (* (rand) (- max min)))
        y (+ min (* (rand) (- max min)))]
    (v x y)))

(defn random-curve [points min max]
  (apply curve (repeatedly points #(random-vector min max))))
