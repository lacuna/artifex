package io.lacuna.artifex;

/**
 * @author ztellman
 */
public interface Curve2 {

  /**
   * @param t a value within [0,1]
   * @return the interpolated position on the curve
   */
  Vec2 position(double t);

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

  /**
   * @param p a point in 2D space
   * @return the {@code t} parameter representing the closest point on the curve, not necessarily within [0,1].  If
   * outside that range, it indicates the distance from the respective endpoints.
   */
  double nearestPoint(Vec2 p);

  Box2 bounds();
}
