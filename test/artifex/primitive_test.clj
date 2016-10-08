(ns artifex.primitive-test
  (:require
   [clojure.test :refer :all])
  (:import
   [io.lacuna.artifex
    SegmentRing2
    Bezier2
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

(defn ring2 [& vs]
  (->> vs
    (map #(apply v %))
    vec
    SegmentRing2.))

(defn unvertex [v]
  (condp instance? v
    Vec2 [(.x v) (.y v)]
    Vec3 [(.x v) (.y v) (.z v)]
    Vec4 [(.x v) (.y v) (.z v) (.w v)]))

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

(deftest test-ring2
  (let [vs [[0.0 0.0] [1.0 0.0] [1.0 1.0]]
        r (apply ring2 vs)]

    (is (= vs (->> r .vertices iterator-seq (map unvertex) butlast)))
    (is (= (first vs) (->> r .vertices iterator-seq (map unvertex) last)))

    (is (= 0.5 (.area r)))

    (is (Vec2/equals (v 0.66 0.33) (.centroid r) 0.01))

    (is (= true (->> vs (apply ring2) .isClockwise)))
    (is (= false (->> vs reverse (apply ring2) .isClockwise)))))

(deftest test-is-convex
  (are [convex? vs]
      (= convex? (.isConvex (apply ring2 vs)))

    true  [[0 0] [1 0] [1 1]]
    false [[0 0] [1 0] [1 1] [0.9 0.5]]
    true  [[0 0] [1 0] [1 1] [0 1]]))

(deftest test-bezier
  (let [quad  (Bezier2/from (v 0 0) (v 1 1) (v 2 0))
        cubic (Bezier2/from (v 0 0) (v 1 1) (v 2 1) (v 3 0))
        isq2  (/ 1 (Math/sqrt 2))]

    ;; position
    (are [b t coords]
        (Vec2/equals (apply v coords) (.position b t) 1e-14)

      quad  0   [0 0]
      quad  0.5 [1 0.5]
      quad  1   [2 0]

      cubic 0   [0 0]
      cubic 0.5 [1.5 0.75]
      cubic 1   [3 0])

    ;; direction
    (are [b t coords]
        (Vec2/equals (apply v coords) (.norm (.direction b t)) 1e-14)

      quad  0   [isq2 isq2]
      quad  0.5 [1 0]
      quad  1   [isq2 (- isq2)]

      cubic 0.5 [1 0])

    ;; split


    ;; nearest point
    ))
