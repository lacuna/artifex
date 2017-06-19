package io.lacuna.artifex;

/**
 * @author ztellman
 */
public class LinearSegment2 implements Curve2 {
  public final Vec2 a, b;

  public LinearSegment2(Vec2 a, Vec2 b) {
    this.a = a;
    this.b = b;
  }

  /**
   * @return the point of intersection between the two line segments, or null if none exists or the segments are collinear
   */
  public static Vec2 intersection(LinearSegment2 p, LinearSegment2 q) {
    Vec2 pv = p.b.sub(p.a);
    Vec2 qv = q.b.sub(q.a);

    double d = (-qv.x * pv.y) + (pv.x * qv.y);
    if (d == 0) {
      return null;
    }

    double s = ((-pv.y * (p.a.x - q.a.x)) + (pv.x * (p.a.y - q.a.y))) / d;
    if (s >= 0 && s <= 1) {
      double t = ((qv.x * (p.a.y - q.a.y)) - (qv.y * (p.a.x - q.a.x))) / d;
      if (t >= 0 && t <= 1) {
        return new Vec2(s, t);
      }
    }

    return null;
  }

  public LinearSegment2 transform(Matrix3 m) {
    return new LinearSegment2(m.transform(a), m.transform(b));
  }

  @Override
  public LinearSegment2 reverse() {
    return new LinearSegment2(b, a);
  }

  @Override
  public double[] inflections() {
    return new double[0];
  }

  @Override
  public Vec2 position(double t) {
    return Vec.lerp(a, b, t);
  }

  @Override
  public Vec2 direction(double t) {
    return b.sub(a);
  }

  @Override
  public LinearSegment2[] split(double t) {
    if (t == 0 || t == 1) {
      return new LinearSegment2[]{this};
    } else if (t < 0 || t > 1) {
      throw new IllegalArgumentException("t must be within [0,1]");
    }

    Vec2 v = position(t);
    return new LinearSegment2[]{new LinearSegment2(a, v), new LinearSegment2(v, b)};
  }

  @Override
  public double nearestPoint(Vec2 p) {
    Vec2 bSa = b.sub(a);
    Vec2 pSa = p.sub(a);
    return Vec.dot(bSa, pSa) / bSa.lengthSquared();
  }

  @Override
  public Interval2 endpoints() {
    return bounds();
  }

  @Override
  public Interval2 bounds() {
    return Interval.from(a, b);
  }

  /**
   * @param p a point in 2D space
   * @return the distance from this segment to the point
   */
  public double distance(Vec2 p) {
    double t = nearestPoint(p);

    if (t <= 0) {
      return p.sub(a).length();
    } else if (t >= 1) {
      return p.sub(b).length();
    } else {
      return p.sub(b.sub(a).mul(t)).length();
    }
  }

  @Override
  public int hashCode() {
    return 31 * a.hashCode() + b.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof LinearSegment2) {
      LinearSegment2 s = (LinearSegment2) obj;
      return a.equals(s.a) && b.equals(s.b);
    }
    return false;
  }

  @Override
  public String toString() {
    return "a=" + a + ", b=" + b;
  }
}
