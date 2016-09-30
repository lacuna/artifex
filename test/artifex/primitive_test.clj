(ns artifex.primitive-test
  (:require
   [clojure.test :refer :all])
  (:import
   [io.lacuna.artifex
    Ring2
    Line2
    Vec2
    Vec3
    Vec4]))

(defn v
  ([x y]
   (Vec2. x y))
  ([x y z]
   (Vec3. x y z))
  ([x y z w]
   (Vec4. x y z w)))

(defn line [slope intercept]
  (Line2. slope intercept))

(defn ring [& vs]
  (->> vs
    (map #(apply v %))
    vec
    Ring2.))

;;;

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

(deftest test-is-convex
  (are [convex? vs]
      (= convex? (.isConvex (apply ring vs)))

    true  [[0 0] [1 0] [1 1]]
    true  [[0 0] [1 0] [1 1] [1 0.5]]
    false [[0 0] [1 0] [1 1] [0.5 0.5]]))
