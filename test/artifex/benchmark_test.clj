(ns artifex.benchmark-test
  (:require
   [clojure.test :refer :all]
   [criterium.core :as c])
  (:import
   [io.lacuna.artifex
    Region2
    Matrix3
    Vec2
    Vec3
    Vec4
    Vec]))

#_(deftest ^:benchmark benchmark-vector
  (let [a (Vec2. 0 0)
        b (Vec2. 1 1)]
    (c/quick-bench
      (.add a b))
    (c/quick-bench
      (Vec/dot a b))
    (c/quick-bench
      (Vec/lerp a b 0.5))
    (c/quick-bench
      (.clamp a a b))
    (c/quick-bench
      (Vec/equals a b 0.5))))

(deftest ^:benchmark benchmark-regions
  (let [regions (->> (cycle [(Region2/circle) (Region2/square)])
                  (take 1e3)
                  (map #(.transform % (Matrix3/translate (rand) (rand)))))]
    (c/quick-bench
      (reduce #(.intersection ^Region2 %1 %2) regions))))
