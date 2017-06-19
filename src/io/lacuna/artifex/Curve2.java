package io.lacuna.artifex;

import io.lacuna.artifex.utils.Curves;

/**
 * @author ztellman
 */
public interface Curve2 {

  /**
   * @param t a value within [0,1]
   * @return the interpolated position on the curve
   */
  Vec2 position(double t);

  default Interval2 endpoints() {
    return Interval.from(position(0), position(1));
  }

  /**
   * @param t a value within [0,1]
   * @return the tangent at the interpolated position on the curve
   */
  Vec2 direction(double t);

  /**
   * @param t a value within [0,1]
   * @return an array representing the lower and upper regions of the curve, split at {@code t}
   */
  Curve2[] split(double t);

  default Curve2[] split(double[] ts) {

    if (ts.length == 0) {
      return new Curve2[]{this};
    }

    Curve2[] result = new Curve2[ts.length + 1];
    Curve2 c = this;

    double scale = 1;
    for (int i = 0; i < ts.length; i++) {
      double p = ts[i];
      Curve2[] parts = c.split(p * scale);
      result[i] = parts[0];
      c = parts[1];
      scale /= p;
    }
    result[ts.length - 1] = c;

    return result;
  }

  /**
   * @param p a point in 2D space
   * @return the {@code t} parameter representing the closest point on the curve, not necessarily within [0,1].  If
   * outside that range, it indicates the distance from the respective endpoints.
   */
  double nearestPoint(Vec2 p);

  default Interval2 bounds() {
    Interval2 bounds = endpoints();
    for (double t : inflections()) {
      bounds = bounds.union(position(t));
    }
    return bounds;
  }

  Curve2 transform(Matrix3 m);

  Curve2 reverse();

  double[] inflections();

  default double[] intersections(Curve2 c, double epsilon) {
    return Curves.intersections(this, c, epsilon);
  }

  default double[] intersections(Curve2 c) {
    return Curves.intersections(this, c);
  }
}
