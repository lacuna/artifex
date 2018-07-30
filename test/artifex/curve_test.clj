(ns artifex.curve-test
  (:require
   [clojure.test :refer :all]
   [artifex.test-utils :refer :all])
  (:import
   [io.lacuna.artifex
    Curve2
    Vec
    Vec2
    Box
    Box2]
   [io.lacuna.artifex.utils
    Scalars]))

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
    (let [linear (random-curve 2 -1 1)
          quad   (random-curve 3 -1 1)
          cubic  (random-curve 4 -1 1)
          splits (->> (range 1 10) (map #(/ 1 %)) (map double))]
      (doseq [t splits]
        (doseq [c [linear quad cubic]]
          (split-and-check c t 100))))))

;; Curve2.nearestPoint

(defn nearest-point [^Curve2 c ^Vec2 v]
  (->> (range (inc 1e3))
    (map #(/ % 1e3))
    (map double)
    (sort-by #(-> (.position c %) (.sub v) .lengthSquared))
    first))

(deftest test-nearest-point
  (let [linear (curve (v 0 0) (v 1 1))
        quad   (curve (v 0 0) (v 1 1) (v 2 0))
        cubic  (curve (v 0 0) (v 1 1) (v 2 -1) (v 3 0))]
    (doseq [v (repeatedly 1e3 #(random-vector -5 5))]
      (doseq [c [linear quad cubic]]
        (let [t0 (nearest-point c v)
              t1 (max 0 (min 1 (.nearestPoint c v)))
              d0 (-> c (.position t0) (.sub v) .length)
              d1 (-> c (.position t1) (.sub v) .length)]
          (is (< (Math/abs (- d0 d1)) 1e-2) [c t0 t1 (Math/abs (- d0 d1))]))))))

;; Curve2.subdivide

(defn subdivision-error [^Curve2 c ^double error]
  (let [vs       (.subdivide c error)
        segments (->> vs
                   (partition 2 1)
                   (map #(curve (first %) (second %))))]
    (->> (range 1e2)
      (map #(.position c (/ % 1e2)))
      (map (fn [v]
             (->> segments
               (map (fn [^Curve2 c] (.position c (Scalars/clamp 0 (.nearestPoint c v) 1))))
               (map #(.lengthSquared (.sub v ^Vec2 %)))
               (apply min)
               Math/sqrt)))
      (apply max))))

(deftest test-subdivision
  (doseq [points [3 4]]
    (doseq [error (->> (range 4) (map #(Math/pow 10 (- %))))]
      (dotimes [_ 10]
        (is (>= error (subdivision-error (random-curve points -10 10) error)))))))

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
      (check-bounds (random-curve points -10 10)))))
