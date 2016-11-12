package io.lacuna.artifex;

import io.lacuna.artifex.utils.Hash;

import static io.lacuna.artifex.Vec2.angleBetween;
import static java.lang.Math.PI;

/**
 * @author ztellman
 */
public class CircularSegment2 implements Curve2 {

  public final Vec2 c, ca;
  public final double theta, r;

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

    Vec2 c = Vec2.lerp(a, b, 0.5).add(chord.rotate(-PI/2).norm().mul(Math.sqrt(d2)));
    Vec2 ca = a.sub(c).norm();
    Vec2 cb = b.sub(c).norm();
    double theta = angleBetween(ca, cb);

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
    return c.add(ca.rotate(theta * t).mul(r));
  }

  @Override
  public Vec2 direction(double t) {
    return position(t).sub(c).rotate(-PI/2);
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
    double threshold = 0.5 + (PI / -theta);

    double t = angleBetween(ca, p.sub(c)) / theta;
    if (t > threshold) {
      t = threshold - t;
    }
    return t;
  }

  @Override
  public Box2 bounds() {

    Box2 bounds = Box2.from(position(0), position(1));

    // todo: there's probably a more efficient way to do this
    Vec2 v = null;
    if (ca.x < 0) {
      v = ca.y < 0 ? new Vec2(-1, 0) : new Vec2(0, 1);
    } else {
      v = ca.y < 0 ? new Vec2(0, -1) : new Vec2(1, 0);
    }

    for (int i = 0; i < 4; i++) {
      if (angleBetween(ca, v) > theta) {
        bounds = bounds.union(c.add(v.mul(r)));
        v = v.rotate(-PI / 2);
      } else {
        break;
      }
    }

    return bounds;
  }

  @Override
  public Curve2 transform(Matrix3 m) {
    return CircularSegment2.from(position(0), position(1), r);
  }

  @Override
  public CircularSegment2 reverse() {
    return new CircularSegment2(c, position(1), -theta, r);
  }

  @Override
  public int hashCode() {
    return Hash.hash(theta, r) ^ c.hashCode() ^ ca.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof CircularSegment2) {
      CircularSegment2 s = (CircularSegment2) obj;
      return c.equals(s.c) && ca.equals(s.ca) && theta == s.theta;
    }
    return false;
  }
}
