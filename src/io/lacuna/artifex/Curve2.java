package io.lacuna.artifex;

import io.lacuna.artifex.utils.Intersections;
import io.lacuna.artifex.utils.Scalars;

import java.util.Arrays;
import java.util.List;

import static io.lacuna.artifex.Box.box;
import static io.lacuna.artifex.utils.Scalars.EPSILON;

/**
 * @author ztellman
 */
public interface Curve2 {

  double SPLIT_EPSILON = 1e-9;

  /**
   * @param t a value within [0,1]
   * @return the interpolated position on the curve
   */
  Vec2 position(double t);

  default Vec2 start() {
    return position(0);
  }

  default Vec2 end() {
    return position(1);
  }

  /**
   * @return an updated curve with the start point as {@code pos}
   */
  default Curve2 start(Vec2 pos) {
    return reverse().end(pos).reverse();
  }

  /**
   * @return an updated curve with the end point at {@code pos}
   */
  Curve2 end(Vec2 pos);

  /**
   * @param t a value within [0,1]
   * @return the tangent at the interpolated position on the curve, which is not normalized
   */
  Vec2 direction(double t);

  /**
   * @param t a value within [0,1]
   * @return an array representing the lower and upper regions of the curve, split at {@code t}
   */
  Curve2[] split(double t);

  /**
   * Performs multiple splits. Typically for N split points this will return N+1 curves, but any split intervals
   * less than {@code Curve2.SPLIT_EPSILON} will be ignored.
   *
   * @param ts an array of parametric split points, which will be sorted in-place
   * @return an array of curves, split at the specified points.
   */
  default Curve2[] split(double[] ts) {
    Arrays.sort(ts);

    int len = 0;
    double prev = 0;
    for (int i = 0; i < ts.length; i++) {
      if (ts[i] - prev > SPLIT_EPSILON && ts[i] < 1 - SPLIT_EPSILON) {
        prev = ts[i];
        ts[len++] = prev;
      }
    }

    Curve2[] result = new Curve2[len + 1];
    Curve2 c = this;

    prev = 0;
    for (int i = 0; i < len; i++) {
      double p = ts[i];
      Curve2[] parts = c.split((p - prev) / (1 - prev));
      prev = p;

      result[i] = parts[0];
      c = parts[1];
    }
    result[result.length - 1] = c;

    return result;
  }

  /**
   * @param p a point in 2D space
   * @return the {@code t} parameter representing the closest point on the curve, not necessarily within [0,1].  If
   * outside that range, it indicates the distance from the respective endpoints.
   */
  double nearestPoint(Vec2 p);

  default Box2 bounds() {
    Box2 bounds = box(start(), end());
    for (double t : inflections()) {
      bounds = bounds.union(position(t));
    }
    return bounds;
  }

  Vec2[] subdivide(double error);

  Curve2 transform(Matrix3 m);

  Curve2 reverse();

  double[] inflections();

  default boolean collinear(Curve2 c) {
    double[] ts = intersections(c);
    Vec2 u = direction(ts[0]).norm();
    Vec2 v = c.direction(ts[1]).norm();
    return Vec.equals(u, v, EPSILON) || Vec.equals(u, v.negate(), EPSILON);
  }

  default boolean intersects(Curve2 c, boolean includeEndpoints) {
    double[] ts = intersections(c);
    if (ts.length > 2) {
      return true;
    } else if (ts.length == 2) {
      return includeEndpoints || !((ts[0] == 0 || ts[0] == 1) && (ts[1] == 0 || ts[1] == 1));
    } else {
      return false;
    }
  }

  default double[] intersections(Curve2 c, double epsilon) {
    return Intersections.intersections(this, c, epsilon);
  }

  default double[] intersections(Curve2 c) {
    return intersections(c, EPSILON);
  }
}
