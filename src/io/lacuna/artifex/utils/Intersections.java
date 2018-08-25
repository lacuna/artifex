package io.lacuna.artifex.utils;

import io.lacuna.artifex.Bezier2.CubicBezier2;
import io.lacuna.artifex.Bezier2.QuadraticBezier2;
import io.lacuna.artifex.*;
import io.lacuna.bifurcan.IList;
import io.lacuna.bifurcan.LinearList;

import java.util.Arrays;
import java.util.Comparator;

import static io.lacuna.artifex.Box.box;
import static io.lacuna.artifex.Vec.dot;
import static io.lacuna.artifex.Vec.vec;
import static io.lacuna.artifex.Vec2.cross;
import static io.lacuna.artifex.utils.Scalars.EPSILON;
import static java.lang.Math.*;

/**
 * @author ztellman
 */
public class Intersections {

  // utilities

  public static final double PARAMETRIC_EPSILON = 1e-6;
  public static final double SPATIAL_EPSILON = 1e-10;

  public static final int MAX_CUBIC_CUBIC_INTERSECTIONS = 9;

  public static final Box2 PARAMETRIC_BOUNDS = box(vec(0, 0), vec(1, 1));

  //

  private static double signedDistance(Vec2 p, Vec2 a, Vec2 b) {
    Vec2 d = b.sub(a);
    return (cross(p, d) + cross(b, a)) / d.length();
  }

  private static Vec2[] convexHull(Vec2 a, Vec2 b, Vec2... points) {
    Vec2[] mapped = new Vec2[points.length];
    for (int i = 0; i < points.length; i++) {
      mapped[i] = vec((double) i / (points.length - 1), signedDistance(points[i], a, b));
    }

    Vec2[] result = new Vec2[points.length + 1];

    int idx = 0;
    Vec2 v = mapped[mapped.length - 1].sub(mapped[0]);

    result[idx++] = mapped[0];
    for (int i = 1; i < points.length; i++) {
      if (cross(v, mapped[i].sub(mapped[0])) < 0) {
        result[idx++] = mapped[i];
      }
    }

    v = v.negate();
    for (int i = points.length - 1; i >= 0; i--) {
      if (cross(v, mapped[i].sub(mapped[mapped.length - 1])) < 0) {
        result[idx++] = mapped[i];
      }
    }

    return result;
  }

  //

  public static class CurveInterval {
    public final Curve2 curve;
    public final boolean isFlat;
    public final double tLo, tHi;
    public final Vec2 pLo, pHi;

    public CurveInterval(Curve2 curve, double tLo, double tHi, Vec2 pLo, Vec2 pHi) {
      this.curve = curve;
      this.tLo = tLo;
      this.tHi = tHi;
      this.pLo = pLo;
      this.pHi = pHi;
      this.isFlat = Vec.equals(pLo, pHi, SPATIAL_EPSILON)
        || (tHi - tLo) < PARAMETRIC_EPSILON
        || curve.range(tLo, tHi).isFlat(SPATIAL_EPSILON);
    }

    public Box2 bounds() {
      return box(pLo, pHi);
    }

    public boolean intersects(CurveInterval c) {
      Box2 bounds = c.bounds().expand(SPATIAL_EPSILON);
      if (!bounds.intersects(bounds())) {
        return false;
      }

      if (isFlat) {
        boolean neg = false, pos = false;
        for (Vec2 v : bounds.vertices()) {
          double d = signedDistance(v, pLo, pHi);
          neg = neg || d <= 0;
          pos = pos || d >= 0;
        }
        return neg && pos;
      } else if (c.isFlat) {
        return c.intersects(this);
      }

      return true;
    }
    
    public CurveInterval[] split() {
      if (isFlat) {
        return new CurveInterval[] {this};
      } else {
        double tMid = (tLo + tHi) / 2;
        Vec2 pMid = curve.position(tMid);
        return new CurveInterval[]{
          new CurveInterval(curve, tLo, tMid, pLo, pMid),
          new CurveInterval(curve, tMid, tHi, pMid, pHi)
        };
      }
    }

    public static CurveInterval[] from(Curve2 c) {
      double[] ts = c.inflections();
      Arrays.sort(ts);

      if (ts.length == 0) {
        return new CurveInterval[]{new CurveInterval(c, 0, 1, c.start(), c.end())};
      } else {
        CurveInterval[] ls = new CurveInterval[ts.length + 1];
        for (int i = 0; i < ls.length; i++) {
          double lo = i == 0 ? 0 : ts[i - 1];
          double hi = i == ls.length - 1 ? 1 : ts[i];
          ls[i] = new CurveInterval(c, lo, hi, c.position(lo), c.position(hi));
        }

        return ls;
      }
    }

    public void intersections(CurveInterval c, IList<Vec2> acc) {
      for (Vec2 i : lineLine(Line2.from(pLo, pHi), Line2.from(c.pLo, c.pHi))) {
        if (PARAMETRIC_BOUNDS.expand(PARAMETRIC_EPSILON).contains(i)) {
          acc.addLast(Vec.lerp(vec(tLo, c.tLo), vec(tHi, c.tHi), i));
        }
      }
    }

    @Override
    public String toString() {
      return "[" + tLo + ", " + tHi + "]";
    }
  }

  // post-processing

  public static double round(double n, double epsilon) {
    if (Scalars.equals(n, 0, epsilon)) {
      return 0;
    } else if (Scalars.equals(n, 1, epsilon)) {
      return 1;
    } else {
      return n;
    }
  }

  private static boolean isCollinear(Curve2 a, Curve2 b, Vec2[] is) {
    if (is.length != 2) {
      return false;
    }

    for (int i = 0; i < MAX_CUBIC_CUBIC_INTERSECTIONS + 1; i++) {
      double t = (double) i / MAX_CUBIC_CUBIC_INTERSECTIONS;
      Vec2 pa = a.position(Scalars.lerp(is[0].x, is[1].x, t));
      Vec2 pb = b.position(Scalars.lerp(is[0].y, is[1].y, t));
      if (!Vec.equals(pa, pb, SPATIAL_EPSILON)) {
        return false;
      }
    }

    return true;
  }

  public static Vec2[] normalize(Curve2 a, Curve2 b, Vec2[] intersections) {

    int limit = intersections.length;
    if (limit == 0) {
      return intersections;
    }

    int readIdx, writeIdx;

    // round and filter within [0, 1]
    for (readIdx = 0, writeIdx = 0; readIdx < limit; readIdx++) {
      Vec2 i = intersections[readIdx].map(n -> round(n, PARAMETRIC_EPSILON));
      if (PARAMETRIC_BOUNDS.contains(i)) {
        intersections[writeIdx++] = i;
      }
    }
    limit = writeIdx;

    if (limit > 1) {
      // dedupe intersections on b
      Arrays.sort(intersections, 0, limit, Comparator.comparingDouble(v -> v.y));
      for (readIdx = 0, writeIdx = -1; readIdx < limit; readIdx++) {
        Vec2 i = intersections[readIdx];
        if (writeIdx < 0 || !Scalars.equals(intersections[writeIdx].y, i.y, EPSILON)) {
          intersections[++writeIdx] = i;
        }
      }
      limit = writeIdx + 1;
    }

    if (limit > 1) {
      // dedupe intersections on a
      Arrays.sort(intersections, 0, limit, Comparator.comparingDouble(v -> v.x));
      for (readIdx = 0, writeIdx = -1; readIdx < limit; readIdx++) {
        Vec2 i = intersections[readIdx];
        if (writeIdx < 0 || !Scalars.equals(intersections[writeIdx].x, i.x, EPSILON)) {
          intersections[++writeIdx] = i;
        }
      }
      limit = writeIdx + 1;
    }

    Vec2[] result = new Vec2[limit];
    System.arraycopy(intersections, 0, result, 0, limit);
    return result;
  }

  // analytical methods

  public static Vec2[] collinearIntersection(Curve2 a, Curve2 b) {
    LinearList<Vec2> result = new LinearList<>();

    for (int i = 0; i < 2; i++) {
      double tb = b.nearestPoint(a.position(i));

      // a overhangs the start of b
      if (tb <= 0) {
        double s = round(a.nearestPoint(b.start()), PARAMETRIC_EPSILON);
        if (0 <= s && s <= 1) {
          result.addLast(vec(s, 0));
        }

        // a overhangs the end of b
      } else if (tb >= 1) {
        double s = round(a.nearestPoint(b.end()), PARAMETRIC_EPSILON);
        if (0 <= s && s <= 1) {
          result.addLast(vec(s, 1));
        }

        // a is contained in b
      } else {
        result.addLast(vec(i, tb));
      }
    }

    //System.out.println(result);

    if (result.size() == 2 && Vec.equals(result.nth(0), result.nth(1), PARAMETRIC_EPSILON)) {
      result.popLast();
    }

    return result.toArray(Vec2[]::new);
  }

  public static Vec2[] lineCurve(Line2 a, Curve2 b) {
    if (b instanceof Line2) {
      return lineLine(a, (Line2) b);
    } else if (b.isFlat(SPATIAL_EPSILON)) {
      return lineLine(a, Line2.from(b.start(), b.end()));
    } else if (b instanceof QuadraticBezier2) {
      return lineQuadratic(a, (QuadraticBezier2) b);
    } else {
      return lineCubic(a, (CubicBezier2) b);
    }
  }

  public static Vec2[] lineLine(Line2 a, Line2 b) {

    Vec2 av = a.end().sub(a.start());
    Vec2 bv = b.end().sub(b.start());

    double d = cross(av, bv);
    Vec2 asb = a.start().sub(b.start());

    if (abs(d) < 1e-6) {
      Vec2[] is = collinearIntersection(a, b);
      if (Arrays.stream(is).allMatch(v -> Vec.equals(a.position(v.x), b.position(v.y), SPATIAL_EPSILON))) {
        return is;
      } else if (abs(d) < EPSILON) {
        return new Vec2[0];
      }
    }

    double s = cross(bv, asb) / d;
    double t = cross(av, asb) / d;
    return new Vec2[]{vec(s, t)};
  }

  public static Vec2[] lineQuadratic(Line2 p, QuadraticBezier2 q) {

    // (p0 - 2p1 + p2) t^2 + (-2p0 + 2p1) t + p0
    Vec2 a = q.p0.add(q.p1.mul(-2)).add(q.p2);
    Vec2 b = q.p0.mul(-2).add(q.p1.mul(2));
    Vec2 c = q.p0;

    Vec2 dir = p.end().sub(p.start());
    Vec2 n = vec(-dir.y, dir.x);

    double[] roots = Equations.solveQuadratic(
      dot(n, a),
      dot(n, b),
      dot(n, c) + cross(p.start(), p.end()));

    Vec2[] result = new Vec2[roots.length];
    if (Scalars.equals(dir.x, 0, EPSILON)) {
      double y0 = p.start().y;
      for (int i = 0; i < roots.length; i++) {
        double t = roots[i];
        double y1 = q.position(t).y;
        result[i] = vec((y1 - y0) / dir.y, t);
      }
    } else {
      double x0 = p.start().x;
      for (int i = 0; i < roots.length; i++) {
        double t = roots[i];
        double x1 = q.position(t).x;
        result[i] = vec((x1 - x0) / dir.x, t);
      }
    }

    return result;
  }

  public static Vec2[] lineCubic(Line2 p, CubicBezier2 q) {

    // (-p0 + 3p1 - 3p2 + p3) t^3 + (3p0 - 6p1 + 3p2) t^2 + (-3p0 + 3p1) t + p0
    Vec2 a = q.p0.mul(-1).add(q.p1.mul(3)).add(q.p2.mul(-3)).add(q.p3);
    Vec2 b = q.p0.mul(3).add(q.p1.mul(-6)).add(q.p2.mul(3));
    Vec2 c = q.p0.mul(-3).add(q.p1.mul(3));
    Vec2 d = q.p0;

    Vec2 dir = p.end().sub(p.start());
    double dLen = dir.length();
    Vec2 n = vec(-dir.y, dir.x);

    double[] roots = Equations.solveCubic(
      dot(n, a),
      dot(n, b),
      dot(n, c),
      dot(n, d) + cross(p.start(), p.end()));

    Vec2[] result = new Vec2[roots.length];
    for (int i = 0; i < roots.length; i++) {
      double t = roots[i];
      Vec2 v = q.position(t).sub(p.start());
      double vLen = v.length();
      double s = (vLen / dLen) * signum(dot(dir, v));

      result[i] = vec(s, t);
    }

    return result;
  }

  // numerical methods

  public static Vec2[] curveCurve(Curve2 a, Curve2 b) {

    LinearList<CurveInterval> queue = new LinearList<>();
    CurveInterval[] as = CurveInterval.from(a);
    CurveInterval[] bs = CurveInterval.from(b);
    for (CurveInterval ap : as) {
      for (CurveInterval bp : bs) {
        queue.addLast(ap).addLast(bp);
      }
    }

    int iterations = 0;
    LinearList<Vec2> acc = new LinearList<>();
    while (queue.size() > 0) {
      iterations++;
      CurveInterval cb = queue.popLast();
      CurveInterval ca = queue.popLast();

      //System.out.println(ca + " " + cb + " " + ca.isFlat + " " + cb.isFlat);

      if (!ca.intersects(cb)) {
        continue;
      }

      if (ca.isFlat && cb.isFlat) {
        long size = acc.size();
        ca.intersections(cb, acc);

        // if we've crossed the magic threshold, check once (and only once) whether they're collinear
        if (size < MAX_CUBIC_CUBIC_INTERSECTIONS && acc.size() >= MAX_CUBIC_CUBIC_INTERSECTIONS) {
          Vec2[] is = collinearIntersection(a, b);
          if (isCollinear(a, b, is)) {
            return is;
          }
        }
      } else {
        for (CurveInterval ap : ca.split()) {
          for (CurveInterval bp : cb.split()) {
            queue.addLast(ap).addLast(bp);
          }
        }
      }
    }

    //System.out.println(iterations);

    return normalize(a, b, acc.toArray(Vec2[]::new));
  }

  ///

  public static Vec2[] intersections(Curve2 a, Curve2 b) {
    if (!a.bounds().expand(SPATIAL_EPSILON).intersects(b.bounds())) {
      return new Vec2[0];
    }

    if (a instanceof Line2) {
      return normalize(a, b, lineCurve((Line2) a, b));
    } else if (b instanceof Line2) {
      Vec2[] result = normalize(b, a, lineCurve((Line2) b, a));
      for (int i = 0; i < result.length; i++) {
        result[i] = result[i].swap();
      }
      return result;
    } else {
      return curveCurve(a, b);
    }
  }


}