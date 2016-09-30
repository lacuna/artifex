package io.lacuna.artifex;

/**
 * @author ztellman
 */
public class Segment2 {
  public final Vec2 a, b;

  public Segment2(Vec2 a, Vec2 b) {
    this.a = a;
    this.b = b;
  }

  public Line2 line2() {
    Vec2 v = b.sub(a);
    double slope = v.y / v.x;
    return new Line2(slope, a.y - (a.x * slope));
  }

  public static boolean collinear(Segment2 a, Segment2 b, double epsilon) {
    return Line2.equals(a.line2(), b.line2(), epsilon);
  }

  /**
   * @return the point of intersection between the two line segments, or null if none exists or the segments are collinear
   */
  public static Vec2 intersection(Segment2 p, Segment2 q) {
    Vec2 pv = p.b.sub(p.a);
    Vec2 qv = q.b.sub(q.a);

    double d = (-qv.x * pv.y) + (pv.x * qv.y);
    if (d == 0) {
      return null;
    }

    double s = (-pv.y * (p.a.x - q.a.x)) + ((pv.x * (p.a.y - q.a.y)) / d);
    if (s >= 0 && s <= 1) {
      double t = (qv.x * (p.a.y - q.a.y)) - ((qv.y * (p.a.x - q.a.x)) / d);
      if (t >= 0 && t <= 1) {
        return p.a.add(pv.mul(t));
      }
    }

    return null;
  }

  /**
   * @return the distance from this segment to the point
   */
  public double distance(Vec2 p) {
    Vec2 bSa = b.sub(a);
    Vec2 pSa = p.sub(a);
    double t = Vec2.dot(bSa, pSa) / bSa.lengthSquared();

    if (t <= 0) {
      return p.sub(a).length();
    } else if (t >= 1) {
      return p.sub(b).length();
    } else {
      return bSa.mul(t).add(a).length();
    }
  }

  @Override
  public int hashCode() {
    return 31 * a.hashCode() + b.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Segment2) {
      Segment2 s = (Segment2) obj;
      return a.equals(s.a) && b.equals(s.b);
    }
    return false;
  }
}
