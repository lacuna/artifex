package io.lacuna.artifex;

import io.lacuna.artifex.utils.Hashes;

import java.util.ArrayList;
import java.util.List;

import static io.lacuna.artifex.Vec.lerp;
import static io.lacuna.artifex.Vec2.angleBetween;
import static java.lang.Math.PI;
import static java.lang.Math.acos;

/**
 * @author ztellman
 */
public class Arc2 implements Curve2 {

  public final Vec2 c, ca;
  public final double theta, r;
  private double[] inflections;

  /**
   * @param a a point on the circle
   * @param b a point on the circle, clockwise from {@code a}
   * @param r the radius of the circle
   * @return a clockwise circular arc between {@code a} and {@code b}
   */
  public static Arc2 from(Vec2 a, Vec2 b, double r, boolean clockwise) {

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

    return new Arc2(
      c,
      clockwise ? ca : cb,
      (theta > 0 ? theta - (PI * 2) : theta) * (clockwise ? 1 : -1),
      r);
  }

  private Arc2(Vec2 c, Vec2 ca, double theta, double r) {
    this.c = c;
    this.ca = ca;
    this.theta = theta;
    this.r = r;
  }

  @Override
  public Arc2 end(Vec2 pos) {
    return from(start(), pos, r, theta > 0);
  }

  /**
   * @return a circular segment covering the opposite range of the circle
   */
  public Arc2 invert() {
    return new Arc2(
      c,
      end().sub(c).norm(),
      (theta > 0 ? (PI * 2) - theta : (-PI * 2) - theta),
      r);
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
  public Arc2[] split(double t) {
    if (t == 0 || t == 1) {
      return new Arc2[]{this};
    } else if (t < 0 || t > 1) {
      throw new IllegalArgumentException("t must be within [0,1]");
    }

    return new Arc2[]{
      new Arc2(c, ca, (theta * t), r),
      new Arc2(c, ca.rotate(theta * t), theta * (1 - t), r)};
  }

  @Override
  public Vec2[] subdivide(double error) {
    double thetaIncrement = 2 * acos((-error / r) + 1);

    int samples = 2 + (int) (-this.theta / thetaIncrement);
    Vec2[] result = new Vec2[samples];
    for (int i = 0; i < samples; i++) {
      result[i] = position((double) i / (samples - 1));
    }
    return result;
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
    Vec2 cp = c.transform(m);
    Vec2 cap = ca.transform(m);
    return new Arc2(cp, cap, theta, cap.sub(ca).length());
  }

  @Override
  public Arc2 reverse() {
    return new Arc2(c, position(1), -theta, r);
  }

  @Override
  public double[] inflections() {

    if (inflections == null) {
      double halfPi = PI / 2;
      double theta = -this.theta;

      double angleToAxis = -angleBetween(ca, Vec2.Y_AXIS);
      double phi = angleToAxis - (int) (angleToAxis / halfPi) * halfPi;
      if (phi < 0) {
        phi = halfPi + phi;
      }

      if (phi > theta) {
        inflections = new double[0];
      } else {
        inflections = new double[1 + (int) ((theta - phi) / halfPi)];
        for (int i = 0; i < inflections.length; i++) {
          inflections[i] = phi / theta;
          phi += halfPi;
        }
      }
    }

    return inflections;
  }

  @Override
  public int hashCode() {
    return Hashes.hash(theta, r) ^ c.hashCode() ^ ca.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Arc2) {
      Arc2 s = (Arc2) obj;
      return c.equals(s.c) && ca.equals(s.ca) && theta == s.theta;
    }
    return false;
  }

  @Override
  public String toString() {
    return "p0=" + start() + ", p1=" + end() + ", radius=" + r;
  }
}
