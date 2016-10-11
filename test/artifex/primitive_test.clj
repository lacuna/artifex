(ns artifex.primitive-test
  (:require
   [clojure.test :refer :all])
  (:import
   [io.lacuna.artifex
    Box2
    SegmentRing2
    CircularSegment2
    Curve2
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

(defn bezier
  ([a b]
   (Bezier2/from a b))
  ([a b c]
   (Bezier2/from a b c))
  ([a b c d]
   (Bezier2/from a b c d)))

(defn unvertex [v]
  (condp instance? v
    Vec2 [(.x v) (.y v)]
    Vec3 [(.x v) (.y v) (.z v)]
    Vec4 [(.x v) (.y v) (.z v) (.w v)]))

;;;

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

(deftest test-curves
  (let [quad   (Bezier2/from (v 0 0) (v 1 1) (v 2 0))
        cubic  (Bezier2/from (v 0 0) (v 1 1) (v 2 1) (v 3 0))
        circle (CircularSegment2/from (v 0 -1) (v 0 1) 1)
        isq2   (/ 1 (Math/sqrt 2))]

    ;; position
    (are [b t coords]
        (Vec2/equals (apply v coords) (.position b t) 1e-14)

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
        (Vec2/equals (apply v coords) (.norm (.direction b t)) 1e-14)

      circle 0   [-1 0]
      circle 0.5 [0 1]
      circle 1   [1 0]

      quad   0   [isq2 isq2]
      quad   0.5 [1 0]
      quad   1   [isq2 (- isq2)]

      cubic  0.5 [1 0])

    ))

;;;

(defn sample-curve [^Curve2 c n]
  (->> (range n)
    (map #(/ % (dec n)))
    (map #(.position c %))))

(defn split-and-check [^Curve2 c t n]
  (let [[a b] (seq (.split c t))
        pa (sample-curve a n)
        pb (sample-curve b n)
        ts (concat
             (->> (range n)
               (map #(* t (/ % (dec n)))))
             (->> (range n)
               (map #(+ t (* (- 1 t) (/ % (dec n)))))))]
    (doseq [[a b] (->> ts
                    (map #(.position c %))
                    (map vector (concat pa pb)))]
      (is (Vec2/equals a b 1e-14)))))

(deftest test-curve-split
  (let [linear (Bezier2/from (v 0 0) (v 1 1))
        quad   (Bezier2/from (v 0 0) (v 1 1) (v 2 0))
        cubic  (Bezier2/from (v 0 0) (v 1 1) (v 2 1) (v 3 0))
        circle (CircularSegment2/from (v 0 -1) (v 0 1) 1)
        splits (->> (range 1 10) (map #(/ 1 %)) (map double))]
    (doseq [t splits]
      (doseq [c [linear quad cubic circle]]
        (split-and-check c t 100)))))

;;;

(defn nearest-point [^Curve2 c ^Vec2 v]
  (->> (range (inc 1e3))
    (map #(/ % 1e3))
    (map double)
    (sort-by #(-> (.position c %) (.sub v) .lengthSquared))
    first))

(defn random-vector [min max]
  (let [x (+ min (* (rand) (- max min)))
        y (+ min (* (rand) (- max min)))]
    (v x y)))

(deftest test-nearest-point
  (let [linear (Bezier2/from (v 0 0) (v 1 1))
        quad   (Bezier2/from (v 0 0) (v 1 1) (v 2 0))
        cubic  (Bezier2/from (v 0 0) (v 1 1) (v 2 1) (v 3 0))
        circle (CircularSegment2/from (v 0 -1) (v 0 1) 1)]
    (doseq [v (repeatedly 1e3 #(random-vector -5 5))]
      (doseq [c [linear quad cubic circle]]
        (let [t0 (nearest-point c v)
              t1 (max 0 (min 1 (.nearestPoint c v)))
              d0 (-> (.position c t0) (.sub v) .length)
              d1 (-> (.position c t1) (.sub v) .length)]
          (is (< (Math/abs (- d0 d1)) 1e-2)))))))

;;;

(defn sampled-bounds [^Curve2 c n]
  (->> (range n)
    (map #(/ % (dec n)))
    (map #(.position c %))
    (reduce #(.union ^Box2 %1 ^Vec2 %2) Box2/EMPTY)))

(defn check-bounds [^Curve2 c]
  (let [bounds (.bounds c)]
    (is (Box2/equals bounds (.union bounds (sampled-bounds c 100)) 1e-14))))

(deftest test-bounds
  ;; TODO: test CircularSegment2, also
  (doseq [points [2 3 4]]
    (dotimes [_ 1e3]
      (check-bounds (apply bezier (repeatedly points #(random-vector -10 10)))))))
