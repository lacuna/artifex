package io.lacuna.artifex;

import io.lacuna.artifex.utils.Intersections;
import io.lacuna.artifex.utils.Scalars;

import java.util.Arrays;
import java.util.List;

import static io.lacuna.artifex.Box.box;
import static io.lacuna.artifex.Vec2.angleBetween;
import static io.lacuna.artifex.utils.Scalars.EPSILON;

/**
 * @author ztellman
 */
public interface Curve2 {

  double SPLIT_EPSILON = 1e-10;

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
   * @return an updated curve with the specified endpoints.
   */
  Curve2 endpoints(Vec2 start, Vec2 end);

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
    if (ts.length == 1) {
      return split(ts[0]);
    }

    Arrays.sort(ts);
    int len = 0;
    double prev = 0;
    for (int i = 0; i < ts.length; i++) {
      if (ts[i] - prev > SPLIT_EPSILON && ts[i] < 1 - SPLIT_EPSILON) {
        prev = ts[i];
        ts[len++] = prev;
      }
    }

    // we want the endpoints of the split curves to *exactly* equal the values returned by position()
    Vec2[] endpoints = new Vec2[len + 2];
    for (int i = 0; i < len; i++) {
      endpoints[i + 1] = position(ts[i]);
    }
    endpoints[0] = start();
    endpoints[endpoints.length - 1] = end();

    Curve2[] result = new Curve2[len + 1];
    Curve2 c = this;
    prev = 0;
    for (int i = 0; i < len; i++) {
      double p = ts[i];
      Curve2[] parts = c.split((p - prev) / (1 - prev));
      prev = p;

      result[i] = parts[0].endpoints(endpoints[i], endpoints[i + 1]);
      c = parts[1];
    }
    result[len] = c.endpoints(endpoints[len], endpoints[len + 1]);

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

  /**
   * @returns whether the curve is convex, assuming that the interior is always to the left of the curve
   */
  default boolean isConvex() {
    return -angleBetween(direction(0).negate(), direction(1)) < Math.PI;
  }

  default double[] intersections(Curve2 c, double epsilon) {
    return Intersections.intersections(this, c, epsilon);
  }

  default double[] intersections(Curve2 c) {
    return intersections(c, SPLIT_EPSILON);
  }
}
