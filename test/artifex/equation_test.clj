(ns artifex.equation-test
  (:require
   [clojure.test :refer :all])
  (:import
   [io.lacuna.artifex.utils
    Scalars
    Equations]))

(defn approx-contains? [s x]
  (->> s
    (filter #(Scalars/equals x % Scalars/EPSILON))
    empty?
    not))

(defn approx= [a b]
  (every? #(approx-contains? b %) a))

;;;

(deftest test-linear
  (are [expected a b]
      (approx= (set (map double expected))
        (when-let [r (Equations/solveLinear a b)]
          (set r)))

    []   0 1
    [0]  1 0
    [-1] 1 1))

(deftest test-quadratic
  (are [expected a b c]
      (approx= (set (map double expected))
        (set (Equations/solveQuadratic a b c)))

    [0]                           1 0 0
    [1 -1]                        1 0 -1
    []                            1 0 1

    [(/ (- -1 (Math/sqrt 5)) 2)
     (/ (+ -1 (Math/sqrt 5)) 2)] 1 1 -1
    ))

(deftest test-cubic
  (are [expected a b c d]
      (approx= (set (map double expected))
        (set (Equations/solveCubic a b c d)))

    [0
     (/ (- 1 (Math/sqrt 5)) 2)
     (/ (+ 1 (Math/sqrt 5)) 2)] -1 1 1 0))

(deftest test-cubic-random
  (dotimes [_ 1e6]
    (let [[a b c d] (repeatedly 4 #(* 10 (rand)))
          roots (Equations/solveCubic a b c d)]
      (doseq [root roots]
        (let [result (+ (* a root root root)
                       (* b root root)
                       (* c root)
                       d)]
          (if (<= 0 root 1)
            (is (Scalars/equals result 0 1e-14) [(count roots) result])))))))
