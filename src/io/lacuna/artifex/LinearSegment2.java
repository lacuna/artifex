package io.lacuna.artifex;

import io.lacuna.artifex.utils.Hashes;
import io.lacuna.artifex.utils.Intersections;

import static io.lacuna.artifex.Box.box;
import static io.lacuna.artifex.Vec.vec;

/**
 * @author ztellman
 */
public class LinearSegment2 implements Curve2 {

  private final double ax, ay, bx, by;

  private LinearSegment2(double ax, double ay, double bx, double by) {
    this.ax = ax;
    this.ay = ay;
    this.bx = bx;
    this.by = by;
  }

  public static LinearSegment2 from(Vec2 a, Vec2 b) {
    return new LinearSegment2(a.x, a.y, b.x, b.y);
  }

  public static LinearSegment2 from(Box2 b) {
    return new LinearSegment2(b.lx, b.ly, b.ux, b.uy);
  }

  public LinearSegment2 transform(Matrix3 m) {
    return LinearSegment2.from(start().transform(m), end().transform(m));
  }

  @Override
  public LinearSegment2 reverse() {
    return new LinearSegment2(bx, by, ax, ay);
  }

  @Override
  public double[] inflections() {
    return new double[0];
  }

  @Override
  public Vec2 position(double t) {
    return Vec.lerp(start(), end(), t);
  }

  @Override
  public Vec2 direction(double t) {
    return end().sub(start());
  }

  @Override
  public LinearSegment2[] split(double t) {
    if (t == 0 || t == 1) {
      return new LinearSegment2[]{this};
    } else if (t < 0 || t > 1) {
      throw new IllegalArgumentException("t must be within [0,1]");
    }

    Vec2 v = position(t);
    return new LinearSegment2[]{LinearSegment2.from(start(), v), LinearSegment2.from(v, end())};
  }

  @Override
  public double nearestPoint(Vec2 p) {
    Vec2 bSa = end().sub(start());
    Vec2 pSa = p.sub(start());
    return Vec.dot(bSa, pSa) / bSa.lengthSquared();
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
  public double[] intersections(Curve2 c, double epsilon) {
    if (c instanceof LinearSegment2) {
      LinearSegment2 p = this;
      LinearSegment2 q = (LinearSegment2) c;

      Vec2 pv = p.end().sub(p.start());
      Vec2 qv = q.end().sub(q.start());

      double d = (-qv.x * pv.y) + (pv.x * qv.y);
      if (d == 0) {
        return new double[0];
      }

      double s = ((-pv.y * (p.ax - q.ax)) + (pv.x * (p.ay - q.ay))) / d;
      if (s >= 0 && s <= 1) {
        double t = ((qv.x * (p.ay - q.ay)) - (qv.y * (p.ax - q.ax))) / d;
        if (t >= 0 && t <= 1) {
          return new double[]{s, t};
        }
      }

      return new double[0];
    } else {
      return Intersections.intersections(this, c, epsilon);
    }
  }

  @Override
  public double[] intersections(Curve2 c) {
    return new double[0];
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
  public int hashCode() {
    return Hashes.hash(ax, ay, bx, by);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof LinearSegment2) {
      LinearSegment2 s = (LinearSegment2) obj;
      return ax == s.ax && ay == s.ay && bx == s.bx && by == s.by;
    }
    return false;
  }

  @Override
  public String toString() {
    return "a=" + start() + ", b=" + end();
  }
}
