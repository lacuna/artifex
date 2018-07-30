package io.lacuna.artifex;

import io.lacuna.artifex.utils.Intersections;
import io.lacuna.artifex.utils.Scalars;

import java.util.Arrays;
import java.util.Comparator;

import static io.lacuna.artifex.Box.box;
import static io.lacuna.artifex.Vec.vec;
import static io.lacuna.artifex.Vec2.angleBetween;
import static io.lacuna.artifex.Vec2.cross;
import static io.lacuna.artifex.utils.Intersections.PARAMETRIC_BOUNDS;
import static io.lacuna.artifex.utils.Scalars.EPSILON;
import static java.lang.Math.abs;

/**
 * @author ztellman
 */
public interface Curve2 {

  enum Type {
    CONVEX,
    CONCAVE,
    FLAT
  }

  /**
   * @param t a parametric point on the curve, not necessarily within [0, 1]
   * @return the interpolated position on the curve
   */
  Vec2 position(double t);

  /**
   * Given a ring of curves, the sum of area() will be the are enclosed by that ring. For clockwise rings, the sum will
   * be negative, for counter-clockwise rings it will be positive.
   *
   * @return the signed area of the curve
   */
  double signedArea();

  double length();

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
   * @param t a parametric point on the curve, not necessarily within [0, 1]
   * @return the tangent at the interpolated position on the curve, which is not normalized
   */
  Vec2 direction(double t);

  /**
   * @param t a parametric point within the curve, which must be within (0, 1)
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

    int offset = 0;
    int len = 0;
    if (ts.length > 0) {
      offset = ts[0] == 0 ? 1 : 0;
      len = ts.length - offset;
      if (ts[ts.length - 1] == 1) {
        len--;
      }
    }

    if (len == 0) {
      return new Curve2[] {this};
    } else if (len == 1) {
      return split(ts[offset]);
    }

    // we want the endpoints of the split curves to *exactly* equal the values returned by position()
    Vec2[] endpoints = new Vec2[len + 2];
    endpoints[0] = start();
    for (int i = 0; i < len; i++) {
      endpoints[i + 1] = position(ts[offset + i]);
    }
    endpoints[endpoints.length - 1] = end();

    Curve2[] result = new Curve2[len + 1];
    Curve2 c = this;
    double prev = 0;
    for (int i = 0; i < len; i++) {
      double p = ts[i + offset];
      Curve2[] parts = c.split((p - prev) / (1 - prev));
      prev = p;

      //assert !Vec.equals(endpoints[i], endpoints[i + 1], EPSILON);
      result[i] = parts[0].endpoints(endpoints[i], endpoints[i + 1]);
      c = parts[1];
    }
    result[len] = c.endpoints(endpoints[len], endpoints[len + 1]);

    return result;
  }

  /**
   * @param p a point in 2D space
   * @return the {@code t} parameter representing the closest point on the curve, not necessarily within [0,1]
   */
  double nearestPoint(Vec2 p);

  default Box2 bounds() {
    Box2 bounds = box(start(), end());
    for (double t : inflections()) {
      bounds = bounds.union(position(t));
    }
    return bounds;
  }

  default Type type() {
    Vec2 u = direction(0);
    Vec2 v = end().sub(start());
    double det = cross(u, v);

    if (abs(det) < 1e-3) {
      u = u.norm();
      v = v.norm();
      det = cross(u, v);
    }

    if (det < -EPSILON) {
      return Type.CONVEX;
    } else if (det > EPSILON) {
      return Type.CONVEX;
    } else {
      return Type.FLAT;
    }
  }

  Vec2[] subdivide(double error);

  Curve2 transform(Matrix3 m);

  Curve2 reverse();

  double[] inflections();

  default Vec2[] intersections(Curve2 c, double epsilon) {
    Vec2[] result;
    if (c instanceof Line2) {
      result = Intersections.lineCurve((Line2) c, this, epsilon)
        .stream()
        .map(v -> v.map(n -> Intersections.round(n, epsilon)))
        .filter(PARAMETRIC_BOUNDS::contains)
        .map(Vec2::swap)
        .toArray(Vec2[]::new);
    } else {
      result = Intersections.curveCurve(this, c, epsilon)
        .stream()
        .map(v -> v.map(n -> Intersections.round(n, epsilon)))
        .filter(PARAMETRIC_BOUNDS::contains)
        .toArray(Vec2[]::new);
    }

    if (result.length > 1) {
      Arrays.sort(result, Comparator.comparingDouble(v -> v.x));
    }
    return result;
  }

  default Vec2[] intersections(Curve2 c) {
    return intersections(c, EPSILON);
  }
}
