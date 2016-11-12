(ns artifex.distance-field-test
  (:require
   [clojure.test :refer :all]
   [clojure.java.io :as io]
   [criterium.core :as c])
  (:import
   [io.lacuna.artifex
    DistanceField
    Bezier2
    Vec2
    Vec3
    Vec4]
   [io.lacuna.artifex.utils
    Images
    Equations]
   [io.lacuna.artifex.formats
    Glyphs]
   [javax.imageio
    ImageIO]
   [java.awt
    Font]
   [java.awt.image
    BufferedImage]))

(defn v
  ([x y]
   (Vec2. x y))
  ([x y z]
   (Vec3. x y z))
  ([x y z w]
   (Vec4. x y z w)))

(defn bezier
  ([a b]
   (Bezier2/from a b))
  ([a b c]
   (Bezier2/from a b c))
  ([a b c d]
   (Bezier2/from a b c d)))

(defn distance-field [curves w h]
  (DistanceField/distanceField curves w h))

(defn save-image [image filename]
  (ImageIO/write image "png" (io/file filename)))

(deftest test-benchmark
  (let [curves (-> (Font. "Helvetica" Font/PLAIN 10) (Glyphs/outline \a) Glyphs/curves)]
    (c/quick-bench
      (distance-field curves 32 32))))
