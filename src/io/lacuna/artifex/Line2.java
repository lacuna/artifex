package io.lacuna.artifex;

import io.lacuna.artifex.utils.Hashes;

import static io.lacuna.artifex.Box.box;
import static io.lacuna.artifex.Vec.vec;
import static io.lacuna.artifex.utils.Scalars.EPSILON;

/**
 * @author ztellman
 */
public class Line2 implements Curve2 {

  private final double ax, ay, bx, by;

  private Line2(double ax, double ay, double bx, double by) {
    this.ax = ax;
    this.ay = ay;
    this.bx = bx;
    this.by = by;
  }

  public static Line2 from(Vec2 a, Vec2 b) {
    if (a.equals(b)) {
      throw new IllegalArgumentException("segments must have non-zero length " + a + " " + b);
    }
    return new Line2(a.x, a.y, b.x, b.y);
  }

  public static Line2 from(Box2 b) {
    return new Line2(b.lx, b.ly, b.ux, b.uy);
  }

  public Line2 transform(Matrix3 m) {
    return Line2.from(start().transform(m), end().transform(m));
  }

  @Override
  public boolean isFlat(double epsilon) {
    return true;
  }

  @Override
  public double signedArea() {
    return ((ax * by) - (bx * ay)) / 2;
  }

  @Override
  public double length() {
    return end().sub(start()).length();
  }

  @Override
  public Line2 reverse() {
    return new Line2(bx, by, ax, ay);
  }

  @Override
  public double[] inflections() {
    return new double[0];
  }

  @Override
  public Vec2 position(double t) {
    if (t == 0) {
      return start();
    } else if (t == 1) {
      return end();
    }

    return new Vec2(ax + (bx - ax) * t, ay + (by - ay) * t);
  }

  @Override
  public Vec2 direction(double t) {
    return new Vec2(bx - ax, by - ay);
  }

  @Override
  public Curve2 range(double tMin, double tMax) {
    return Line2.from(position(tMin), position(tMax));
  }

  @Override
  public Line2[] split(double t) {
    if (t <= 0 || t >= 1) {
      throw new IllegalArgumentException("t must be within (0,1)");
    }

    Vec2 v = position(t);
    return new Line2[]{from(start(), v), from(v, end())};
  }

  @Override
  public double nearestPoint(Vec2 p) {
    Vec2 bSa = end().sub(start());
    Vec2 pSa = p.sub(start());
    return Vec.dot(bSa, pSa) / bSa.lengthSquared();
  }

  @Override
  public Line2 endpoints(Vec2 start, Vec2 end) {
    return from(start, end);
  }

  @Override
  public Vec2 start() {
    return vec(ax, ay);
  }

  @Override
  public Vec2 end() {
    return vec(bx, by);
  }

  @Override
  public Vec2[] subdivide(double error) {
    return new Vec2[]{start(), end()};
  }

  @Override
  public Box2 bounds() {
    return box(start(), end());
  }

  /**
   * @param p a point in 2D space
   * @return the distance from this segment to the point
   */
  public double distance(Vec2 p) {
    double t = nearestPoint(p);

    if (t <= 0) {
      return p.sub(start()).length();
    } else if (t >= 1) {
      return p.sub(end()).length();
    } else {
      return p.sub(end().sub(start()).mul(t)).length();
    }
  }

  @Override
  public String toString() {
    return "a=" + start() + ", b=" + end();
  }
}
