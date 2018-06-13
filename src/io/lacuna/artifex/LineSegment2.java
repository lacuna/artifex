package io.lacuna.artifex;

import io.lacuna.artifex.utils.Hashes;
import io.lacuna.artifex.utils.Intersections;

import static io.lacuna.artifex.Box.box;
import static io.lacuna.artifex.Vec.vec;
import static io.lacuna.artifex.Vec2.cross;
import static io.lacuna.artifex.utils.Scalars.EPSILON;

/**
 * @author ztellman
 */
public class LineSegment2 implements Curve2 {

  private final double ax, ay, bx, by;
  private int hash = -1;

  private LineSegment2(double ax, double ay, double bx, double by) {
    this.ax = ax;
    this.ay = ay;
    this.bx = bx;
    this.by = by;
  }

  public static LineSegment2 from(Vec2 a, Vec2 b) {
    if (Vec.equals(a, b, EPSILON)) {
      throw new IllegalArgumentException("segments must have non-zero length");
    }
    return new LineSegment2(a.x, a.y, b.x, b.y);
  }

  public static LineSegment2 from(Box2 b) {
    return new LineSegment2(b.lx, b.ly, b.ux, b.uy);
  }

  public LineSegment2 transform(Matrix3 m) {
    return LineSegment2.from(start().transform(m), end().transform(m));
  }

  @Override
  public LineSegment2 reverse() {
    return new LineSegment2(bx, by, ax, ay);
  }

  @Override
  public double[] inflections() {
    return new double[0];
  }

  @Override
  public Vec2 position(double t) {
    return new Vec2(ax + (bx - ax) * t, ay + (by - ay) * t);
  }

  @Override
  public Vec2 direction(double t) {
    return new Vec2(bx - ax, by - ay);
  }

  @Override
  public LineSegment2[] split(double t) {
    if (t < 0 || t > 1) {
      throw new IllegalArgumentException("t must be within [0,1]");
    } else if (t < SPLIT_EPSILON || t > 1 - SPLIT_EPSILON) {
      return new LineSegment2[]{this};
    }

    Vec2 v = position(t);
    return new LineSegment2[]{from(start(), v), from(v, end())};
  }

  @Override
  public double nearestPoint(Vec2 p) {
    Vec2 bSa = end().sub(start());
    Vec2 pSa = p.sub(start());
    return Vec.dot(bSa, pSa) / bSa.lengthSquared();
  }

  @Override
  public LineSegment2 end(Vec2 pos) {
    return from(start(), pos);
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
    return new Vec2[] {start(), end()};
  }

  @Override
  public double[] intersections(Curve2 c, double epsilon) {

    if (c instanceof LineSegment2) {
      LineSegment2 p = this;
      LineSegment2 q = (LineSegment2) c;

      Vec2 pv = p.end().sub(p.start());
      Vec2 qv = q.end().sub(q.start());

      double d = cross(pv, qv);
      Vec2 psq = p.start().sub(q.start());

      if (d == 0) {
        return cross(psq, pv) == 0
          ? Intersections.collinearIntersection(p, q)
          : new double[0];
      }

      double s = cross(qv, psq) / d;
      if (s >= 0 && s <= 1) {
        double t = cross(pv, psq) / d;
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
    if (hash == -1) {
      hash = Hashes.hash(ax, ay, bx, by);
    }
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof LineSegment2) {
      LineSegment2 s = (LineSegment2) obj;
      return ax == s.ax && ay == s.ay && bx == s.bx && by == s.by;
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    return "a=" + start() + ", b=" + end();
  }
}
