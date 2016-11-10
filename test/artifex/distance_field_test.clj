(ns artifex.distance-field-test
  (:require
   [clojure.java.io :as io
    ])
  (:import
   [io.lacuna.artifex
    DistanceField
    Bezier2
    Vec2
    Vec3
    Vec4]
   [io.lacuna.artifex.utils
    Images]
   [javax.imageio
    ImageIO]
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

(def distance-field
  (DistanceField/distanceField
    [[(bezier (v 0 -1) (v -1 0) (v 0 1))
      (bezier (v 0 1) (v 1 0) (v 0 -1))]]
    32 32))

(defn save-image [image filename]
  (ImageIO/write image "png" (io/file filename)))
