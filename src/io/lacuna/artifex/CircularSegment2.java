package io.lacuna.artifex;

import static io.lacuna.artifex.Vec2.cross;
import static io.lacuna.artifex.Vec2.dot;
import static java.lang.Math.atan2;

/**
 * @author ztellman
 */
public class CircularSegment2 implements Curve2 {

  public final Vec2 c;
  private final Vec2 ca;
  public final double theta;
  private final double r;

  /**
   * @param a a point on the circle
   * @param b a point on the circle, clockwise from {@code a}
   * @param r the radius of the circle
   * @return a circular arc between {@code a} and {@code b}
   */
  public static CircularSegment2 from(Vec2 a, Vec2 b, double r) {
    Vec2 chord = b.sub(a);

    // distance-squared from center of chord to center of circle
    double d2 = (r * r) - chord.lengthSquared() / 4;
    if (d2 < 0) {
      throw new IllegalArgumentException("points are too far apart for the given radius");
    }

    Vec2 c = Vec2.lerp(a, b, 0.5).add(chord.rotate(-Math.PI/2).norm().mul(Math.sqrt(d2)));
    Vec2 ca = a.sub(c).norm();
    Vec2 cb = b.sub(c).norm();
    double theta = Vec2.angleBetween(ca, cb);

    return new CircularSegment2(c, ca, theta, r);
  }

  private CircularSegment2(Vec2 c, Vec2 ca, double theta, double r) {
    this.c = c;
    this.ca = ca;
    this.theta = theta;
    this.r = r;
  }

  @Override
  public Vec2 position(double t) {
    return ca.rotate(theta * t).mul(r);
  }

  @Override
  public Vec2 direction(double t) {
    return position(t).sub(c).rotate(-Math.PI/2);
  }

  @Override
  public Curve2[] split(double t) {
    return new Curve2[]{
            new CircularSegment2(c, ca, (theta * t), r),
            new CircularSegment2(c, position(t), theta * (1 - t), r)};
  }

  @Override
  public double nearestPoint(Vec2 p) {
    // the point at which we wraparound and are closer to the beginning of the arc
    double threshold = 0.5 + (Math.PI / -theta);

    double t = Vec2.angleBetween(ca, p.sub(c)) / theta;
    if (t > threshold) {
      t = threshold - t;
    }
    return t;
  }
}
