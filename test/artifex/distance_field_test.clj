(ns artifex.distance-field-test
  (:require
   [clojure.test :refer :all]
   [clojure.java.io :as io]
   [criterium.core :as c])
  (:import
   [io.lacuna.artifex
    Bezier2
    Matrix3
    Vec2
    Vec3
    Vec4]
   [io.lacuna.artifex.utils
    Images
    Equations
    DistanceField]
   [io.lacuna.artifex.formats
    Glyphs
    Glyphs$Field]
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

(defn save-image [image filename]
  (ImageIO/write image "png" (io/file filename)))

(defn render [str field-res image-res]
  (-> (Font. "Helvetica" Font/PLAIN 10)
    (Glyphs/glyph str field-res)
    #_.field
    (.render image-res)))
