(ns artifex.primitive-test
  (:require
   [criterium.core :as c]
   [clojure.string :as str]
   [clojure.test :refer :all])
  (:import
   [io.lacuna.artifex.utils
    Intersections
    PlaneSweep
    MonotoneRegion
    Scalars]
   [java.util.function
    ToDoubleFunction
    Function
    BiPredicate]
   [io.lacuna.artifex
    Box
    Box2
    Polygon2$Ring
    LineSegment2
    Arc2
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

;;;

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

(defn ring2 [& vs]
  (->> vs
    (map #(apply v %))
    vec
    io.lacuna.bifurcan.LinearList/from
    Polygon2$Ring.))

(defn bezier
  ([a b]
   (Bezier2/bezier a b))
  ([a b c]
   (Bezier2/bezier a b c))
  ([a b c d]
   (Bezier2/bezier a b c d)))

(defn arc
  [a b r]
  (Arc2/from a b r true))

(defn line-segment [a b]
  (LineSegment2/from a b))

(defn unvertex [v]
  (condp instance? v
    Vec2 [(.x v) (.y v)]
    Vec3 [(.x v) (.y v) (.z v)]
    Vec4 [(.x v) (.y v) (.z v) (.w v)]))

;;;

(defn random-vector [min max]
  (let [x (+ min (* (rand) (- max min)))
        y (+ min (* (rand) (- max min)))]
    (v x y)))

(defn random-bezier [points min max]
  (apply bezier (repeatedly points #(random-vector min max))))

(defn random-arc [min max]
  (let [[a b] (repeatedly 2 #(random-vector min max))
        r     (-> b (.sub a) .length (* (+ 0.5 (rand))))]
    (arc a b r)))

;; vectors

(deftest test-angle-between
  (let [half-pi (/ Math/PI 2)]
    (is (= (- half-pi) (Vec2/angleBetween (v -1 0) (v 0 1))))
    (is (= (* -3 half-pi) (Vec2/angleBetween (v 0 1) (v -1 0))))))

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

;; MonotoneRegion

(deftest test-vertex-type
  (are [type a b c]
      (= type
        (-> (MonotoneRegion/vertexType a b c)
          str
          str/lower-case
          keyword))

    :merge   (v -1 0) (v 0 1) (v 1 0)
    :start   (v -1 1) (v 0 0) (v 1 1)
    :end     (v 1 0)  (v 0 1) (v -1 0)
    :split   (v 1 1)  (v 0 0) (v -1 1)
    :regular (v 0 0)  (v 0 1) (v 0 2)
    :regular (v 0 2)  (v 0 1) (v 0 0)
    ))

;; PlaneSweep

(deftest test-plane-sweep
  (dotimes [_ 1e2]
    (let [[a b]
          (repeatedly 2
            (fn []
              (->> (repeatedly #(random-vector 0 1))
                (take 1e2)
                (partition 2)
                (map #(apply line-segment %)))))

          intersections
          (->> (for [x a, y b] [x y])
            (map set)
            (filter #(.intersects ^LineSegment2 (first %) (second %)))
            (into #{}))]
      (is
        (= intersections
          (->>
            (PlaneSweep/intersections a b (function identity))
            (map #(set [(.a %) (.b %)]))
            set))))))

;; LinearRing2

(deftest test-ring2
  (let [vs [[0.0 0.0] [1.0 0.0] [1.0 1.0]]
        r (apply ring2 vs)]

    (is (= vs (->> r .vertices iterator-seq (map unvertex) butlast)))
    (is (= (first vs) (->> r .vertices iterator-seq (map unvertex) last)))

    (is (= 0.5 (.area r)))

    (is (= true (->> vs (apply ring2) .isClockwise)))
    (is (= false (->> vs reverse (apply ring2) .isClockwise)))))

(deftest test-is-convex
  (are [convex? vs]
      (= convex? (.isConvex (apply ring2 vs)))

    true  [[0 0] [1 0] [1 1]]
    false [[0 0] [1 0] [1 1] [0.9 0.5]]
    true  [[0 0] [1 0] [1 1] [0 1]]))

;; basic curves

(deftest test-curves
  (let [quad   (bezier (v 0 0) (v 1 1) (v 2 0))
        cubic  (bezier (v 0 0) (v 1 1) (v 2 1) (v 3 0))
        circle (arc (v 0 -1) (v 0 1) 1)
        isq2   (/ 1 (Math/sqrt 2))]

    ;; position
    (are [b t coords]
        (Vec/equals (apply v coords) (.position b t) Scalars/EPSILON)

      circle 0   [0 -1]
      circle 0.5 [-1 0]
      circle 1   [0 1]

      quad   0   [0 0]
      quad   0.5 [1 0.5]
      quad   1   [2 0]

      cubic  0   [0 0]
      cubic  0.5 [1.5 0.75]
      cubic  1   [3 0])

    ;; direction
    (are [b t coords]
        (Vec/equals (apply v coords) (.norm (.direction b t)) Scalars/EPSILON)

      circle 0   [-1 0]
      circle 0.5 [0 1]
      circle 1   [1 0]

      quad   0   [isq2 isq2]
      quad   0.5 [1 0]
      quad   1   [isq2 (- isq2)]

      cubic  0.5 [1 0])

    ))

;; Curve2.split

(defn sample-curve [^Curve2 c n]
  (->> (range n)
    (map #(/ % (dec n)))
    (map #(.position c %))))

(defn split-and-check [^Curve2 c t n]
  (let [[a b] (seq (.split c t))
        pa (when a (sample-curve a n))
        pb (when b (sample-curve b n))
        ts (concat
             (->> (range n)
               (map #(* t (/ % (dec n)))))
             (->> (range n)
               (map #(+ t (* (- 1 t) (/ % (dec n)))))))]
    (doseq [[a b] (->> ts
                    (map #(.position c %))
                    (map vector (concat pa pb)))]
      (is (Vec/equals a b Scalars/EPSILON)))))

(deftest test-curve-split
  (dotimes [_ 1e2]
    (let [linear (random-bezier 2 -1 1)
          quad   (random-bezier 3 -1 1)
          cubic  (random-bezier 4 -1 1)
          circle (random-arc -1 1)
          splits (->> (range 1 10) (map #(/ 1 %)) (map double))]
      (doseq [t splits]
        (doseq [c [linear quad cubic circle]]
          (split-and-check c t 100))))))

;; Curve2.nearestPoint

(defn nearest-point [^Curve2 c ^Vec2 v]
  (->> (range (inc 1e3))
    (map #(/ % 1e3))
    (map double)
    (sort-by #(-> (.position c %) (.sub v) .lengthSquared))
    first))

(deftest test-nearest-point
  (let [linear (bezier (v 0 0) (v 1 1))
        quad   (bezier (v 0 0) (v 1 1) (v 2 0))
        cubic  (bezier (v 0 0) (v 1 1) (v 2 1) (v 3 0))
        circle (arc (v -1 0) (v 0 1) 1)]
    (doseq [v (repeatedly 1e3 #(random-vector -5 5))]
      (doseq [c [linear quad cubic circle]]
        (let [t0 (nearest-point c v)
              t1 (max 0 (min 1 (.nearestPoint c v)))
              d0 (-> c (.position t0) (.sub v) .length)
              d1 (-> c (.position t1) (.sub v) .length)]
          (is (< (Math/abs (- d0 d1)) 1e-2) v))))))

;; Curve2.subdivide

(defn subdivision-error [^Curve2 c ^double error]
  (let [vs       (.subdivide c error)
        segments (->> vs
                   (partition 2 1)
                   (map #(line-segment (first %) (second %))))]
    (->> (range 1e2)
      (map #(.position c (/ % 1e2)))
      (map (fn [v]
             (->> segments
               (map (fn [^Curve2 c] (.position c (.nearestPoint c v))))
               (map #(.lengthSquared (.sub v ^Vec2 %)))
               (apply min)
               Math/sqrt)))
      (apply max))))

(deftest test-subdivision
  (doseq [points [3 4]]
    (doseq [error (->> (range 4) (map #(Math/pow 10 (- %))))]
      (dotimes [_ 10]
        (is (>= error (subdivision-error (random-bezier points -10 10) error))))))

  (doseq [error (->> (range 4) (map #(Math/pow 10 (- %))))]
    (dotimes [_ 10]
      (is (>= error (subdivision-error (random-arc -10 10) error))))))

;; Curve2.bounds

(defn sampled-bounds [^Curve2 c n]
  (->> (range n)
    (map #(/ % (dec n)))
    (map #(.position c %))
    (reduce #(.union ^Box2 %1 ^Vec2 %2) Box2/EMPTY)))

(defn check-bounds [^Curve2 c]
  (let [bounds (.bounds c)]
    (is
      (Box/equals bounds (.union bounds (sampled-bounds c 100)) Scalars/EPSILON)
      (str c " "  bounds " " (.union bounds (sampled-bounds c 100))))))

(deftest test-bounds
  (doseq [points [2 3 4]]
    (dotimes [_ 1e3]
      (check-bounds (random-bezier points -10 10))))

  (dotimes [_ 1e3]
    (check-bounds (random-arc -10 10))))

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
