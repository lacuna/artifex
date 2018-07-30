(ns artifex.primitive-test
  (:require
   [artifex.test-utils :refer :all]
   [criterium.core :as c]
   [clojure.string :as str]
   [clojure.test :refer :all]
   [clojure.pprint :refer :all])
  (:import
   [io.lacuna.artifex.utils
    Intersections
    Scalars
    EdgeList
    Equations]
   [io.lacuna.artifex
    Box
    Box2
    Region2
    Region2$Ring
    Polygon2$Ring
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

;; vectors

(deftest test-angle-between
  (let [half-pi (/ Math/PI 2)]
    (is (= (- half-pi) (Vec2/angleBetween (v -1 0) (v 0 1))))
    (is (= (* -3 half-pi) (Vec2/angleBetween (v 0 1) (v -1 0)))))

  (dotimes [_ 1e2]
    (let [[a b] (map #(.norm %) (repeatedly 2 #(random-vector -1 1)))]
      (Vec/equals b (.rotate a (Vec2/angleBetween a b)) Scalars/EPSILON))))

(deftest test-vector-arithmetic
  (are [expected op a b]
      (= (apply v expected) (op (apply v a) (apply v b)))

    [2 4] .add [1 2] [1 2]
    [0 2] .sub [2 3] [2 1]
    [1 4] .mul [1 2] [1 2]
    [1 2] .div [4 4] [4 2]

    [2 4 6] .add [1 2 3] [1 2 3]
    [0 2 1] .sub [2 3 4] [2 1 3]
    [1 4 9] .mul [1 2 3] [1 2 3]
    [1 2 1] .div [2 2 2] [2 1 2]

    [1 2 3 4] .add [0 0 0 0] [1 2 3 4]
    [0 2 1 3] .sub [2 3 4 5] [2 1 3 2]
    [2 3 4 5] .mul [1 1 1 1] [2 3 4 5]
    [8 4 2 1] .div [8 8 8 8] [1 2 4 8]

    ))

;; basic curves

(deftest test-curves
  (let [quad   (curve (v 0 0) (v 1 1) (v 2 0))
        cubic  (curve (v 0 0) (v 1 1) (v 2 1) (v 3 0))
        isq2   (/ 1 (Math/sqrt 2))]

    ;; position
    (are [b t coords]
        (Vec/equals (apply v coords) (.position b t) Scalars/EPSILON)

      quad   0   [0 0]
      quad   0.5 [1 0.5]
      quad   1   [2 0]

      cubic  0   [0 0]
      cubic  0.5 [1.5 0.75]
      cubic  1   [3 0])

    ;; direction
    (are [b t coords]
        (Vec/equals (apply v coords) (.norm (.direction b t)) Scalars/EPSILON)

      quad   0   [isq2 isq2]
      quad   0.5 [1 0]
      quad   1   [isq2 (- isq2)]

      cubic  0.5 [1 0])

    ))

;; Matrix3

(deftest test-matrix-multiplication
  (are [a b v]
      (= (->> v (.transform a) (.transform b))
        (.transform (.mul b a) v))

    (Matrix3/scale 2.0) (Matrix3/translate 1 1) (v 1 1)
    (Matrix3/rotate 1) (Matrix3/translate 1 1) (v 1 1)
    (Matrix3/translate 1 1) (Matrix3/rotate 1) (v 1 1)

    (Matrix4/translate 1 1 1) (Matrix4/scale 2.0) (v 1 1 1)
    (Matrix4/scale 2.0) (Matrix4/translate 1 1 1) (v 1 1 1)))
