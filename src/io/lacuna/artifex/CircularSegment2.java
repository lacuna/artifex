package io.lacuna.artifex;

import io.lacuna.artifex.utils.Hashes;

import static io.lacuna.artifex.Vec.lerp;
import static io.lacuna.artifex.Vec2.angleBetween;
import static java.lang.Math.PI;
import static java.lang.Math.acos;

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
   * @return a clockwise circular arc between {@code a} and {@code b}
   */
  public static CircularSegment2 from(Vec2 a, Vec2 b, double r) {

    if (r <= 0) {
      throw new IllegalArgumentException("radius must be greater than 0");
    }

    Vec2 chord = b.sub(a);

    // distance-squared from center of chord to center of circle
    double d2 = (r * r) - chord.lengthSquared() / 4;
    if (d2 < 0) {
      throw new IllegalArgumentException("points are too far apart for the given radius");
    }

    Vec2 c = lerp(a, b, 0.5).add(chord.rotate(-PI / 2).norm().mul(Math.sqrt(d2)));
    Vec2 ca = a.sub(c).norm();
    Vec2 cb = b.sub(c).norm();
    double theta = angleBetween(ca, cb);

    return new CircularSegment2(c, ca, theta > 0 ? theta - (PI * 2) : theta, r);
  }

  private CircularSegment2(Vec2 c, Vec2 ca, double theta, double r) {
    this.c = c;
    this.ca = ca;
    this.theta = theta;
    this.r = r;
  }

  /**
   * @return a circular segment covering the opposite range of the circle
   */
  public CircularSegment2 invert() {
    return CircularSegment2.from(position(1), position(0), r);
  }

  @Override
  public Vec2 position(double t) {
    return c.add(ca.rotate(theta * t).mul(r));
  }

  @Override
  public Vec2 direction(double t) {
    return position(t).sub(c).rotate(-PI / 2);
  }

  @Override
  public CircularSegment2[] split(double t) {
    if (t == 0 || t == 1) {
      return new CircularSegment2[]{this};
    } else if (t < 0 || t > 1) {
      throw new IllegalArgumentException("t must be within [0,1]");
    }

    return new CircularSegment2[]{
            new CircularSegment2(c, ca, (theta * t), r),
            new CircularSegment2(c, ca.rotate(theta * t), theta * (1 - t), r)};
  }

  @Override
  public Vec2[] subdivide(double error) {
    double thetaIncrement = 2 * acos((-error / r) + 1);

    Vec2[] rs = new Vec2[2 + (int) (-this.theta / thetaIncrement)];
    for (int i = 0; i < rs.length; i++) {
      rs[i] = position(i / (double) (rs.length - 1));
    }
    return rs;
  }

  @Override
  public double nearestPoint(Vec2 p) {

    double t = angleBetween(position(0.5).sub(c), p.sub(c));

    if (Double.isNaN(t)) {
      return 0;
    }

    if (t > PI) {
      t = (2 * PI) - t;
    } else if (t < -PI) {
      t = t + (2 * PI);
    }

    return (t / theta) + 0.5;
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
  public double[] inflections() {

    double halfPi = PI / 2;
    double theta = -this.theta;

    double angleToAxis = -angleBetween(ca, Vec2.Y_AXIS);
    double phi = angleToAxis - (int) (angleToAxis / halfPi) * halfPi;
    if (phi < 0) {
      phi = halfPi + phi;
    }

    if (phi > theta) {
      return new double[0];
    }

    double[] ts = new double[1 + (int) ((theta - phi) / halfPi)];

    for (int i = 0; i < ts.length; i++) {
      ts[i] = phi / theta;
      phi += halfPi;
    }

    return ts;
  }

  @Override
  public int hashCode() {
    return Hashes.hash(theta, r) ^ c.hashCode() ^ ca.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof CircularSegment2) {
      CircularSegment2 s = (CircularSegment2) obj;
      return c.equals(s.c) && ca.equals(s.ca) && theta == s.theta;
    }
    return false;
  }

  @Override
  public String toString() {
    return "p0=" + start() + ", p1=" + end() + ", radius=" + r;
  }
}
