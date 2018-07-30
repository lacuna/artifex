package io.lacuna.artifex.utils;

import io.lacuna.artifex.Bezier2.CubicBezier2;
import io.lacuna.artifex.Bezier2.QuadraticBezier2;
import io.lacuna.artifex.*;
import io.lacuna.artifex.Curve2.Type;
import io.lacuna.bifurcan.IList;
import io.lacuna.bifurcan.LinearList;
import io.lacuna.bifurcan.Lists;

import java.util.Comparator;

import static io.lacuna.artifex.Box.box;
import static io.lacuna.artifex.Vec.dot;
import static io.lacuna.artifex.Vec.vec;
import static io.lacuna.artifex.Vec2.cross;
import static io.lacuna.artifex.utils.Scalars.EPSILON;
import static io.lacuna.artifex.utils.Scalars.inside;
import static java.lang.Math.abs;
import static java.lang.Math.signum;

/**
 * @author ztellman
 */
public class Intersections {

  // utilities

  public static final Box2 PARAMETRIC_BOUNDS = box(vec(0, 0), vec(1, 1));

  public static double round(double t, double epsilon) {
    if (inside(-epsilon, t, epsilon)) {
      return 0;
    } else if (inside(1 - epsilon, t, 1 + epsilon)) {
      return 1;
    } else {
      return t;
    }
  }

  // numerical methods

  private static final int SEARCH_ITERATIONS = 16;
  // TODO: make this smarter
  private static final int SEARCH_STEPS = 3;

  private static class CurveInterval {
    public final Curve2 c;
    public final double tLo, tHi;
    public final Vec2 pLo, pHi;

    public CurveInterval(Curve2 c, double tLo, double tHi, Vec2 pLo, Vec2 pHi) {
      this.c = c;
      this.tLo = tLo;
      this.tHi = tHi;
      this.pLo = pLo;
      this.pHi = pHi;
    }

    public CurveInterval(Curve2 c) {
      this.c = c;
      this.tLo = 0;
      this.tHi = 1;
      this.pLo = c.start();
      this.pHi = c.end();
    }

    public static CurveInterval[] from(Curve2 c) {
      double[] ts = c.inflections();
      if (ts.length == 0) {
        return new CurveInterval[]{new CurveInterval(c)};
      } else {
        CurveInterval[] is = new CurveInterval[ts.length + 1];
        for (int i = 0; i < is.length; i++) {
          double lo = i == 0 ? 0 : ts[i - 1];
          double hi = i == is.length - 1 ? 1 : ts[i];
          Vec2 pLo = lo == 0 ? c.start() : is[i - 1].pHi;
          Vec2 pHi = hi == 1 ? c.end() : c.position(hi);
          is[i] = new CurveInterval(c, lo, hi, pLo, pHi);
        }

        return is;
      }
    }

    public double mid(double epsilon) {
      return round((tLo + tHi) / 2, epsilon);
    }

    public boolean intersects(CurveInterval c, double epsilon) {
      // TODO avoid allocation here
      return box(pLo, pHi).expand(epsilon).intersects(box(c.pLo, c.pHi));
    }

    public CurveInterval[] split() {
      double tMid = (tLo + tHi) / 2;
      Vec2 pMid = c.position(tMid);
      return new CurveInterval[]{
        new CurveInterval(c, tLo, tMid, pLo, pMid),
        new CurveInterval(c, tMid, tHi, pMid, pHi)};
    }

    public boolean contains(double t, double epsilon) {
      return tLo <= t + epsilon && t - epsilon <= tHi;
    }
  }

  private static Vec2 closestIntersection(IList<Vec2> intersections) {
    if (intersections.size() == 0) {
      return null;
    }

    Vec2 result = intersections.nth(0);
    for (int i = 1; i < intersections.size(); i++) {
      Vec2 v = intersections.nth(i);
      if (abs(v.x) < abs(result.x)) {
        result = v;
      }
    }
    return result;
  }

  private static void intersections(CurveInterval a, CurveInterval b, IList<Vec2> acc, boolean inverted, double epsilon) {
    if (!a.intersects(b, epsilon)) {
      return;
    }

    //System.out.println("\n" + a.c + "\n" + b.c);

    boolean missed = false;

    for (int i = 0; i < SEARCH_STEPS; i++) {
      double ta = Scalars.lerp(a.tLo, a.tHi, (double) i / (SEARCH_STEPS - 1));

      int j = 0;
      for (; j < SEARCH_ITERATIONS; j++) {
        //System.out.println(ta);
        Vec2 pa = a.c.position(ta);
        Vec2 da = a.c.direction(ta).norm();
        Vec2 ia = closestIntersection(lineCurve(Line2.from(pa, pa.add(da)), b.c, 1e-14));
        if (ia == null) {
          missed = true;
          break;
        }

        double tb = ia.y;
        Vec2 pb = b.c.position(tb);
        Vec2 db = b.c.direction(tb).norm();
        Vec2 ib = closestIntersection(lineCurve(Line2.from(pb, pb.add(db)), a.c, 1e-14));
        if (ib == null) {
          missed = true;
          break;
        }

        //System.out.println(ia.x + " " + ib.x + " " + vec(ta, tb));
        if (abs(ia.x) < epsilon && abs(ib.x) < epsilon) {
          acc.addLast(inverted ? vec(tb, ta) : vec(ta, tb));
          break;
        }

        ta = ib.y;
      }

      if (j == SEARCH_ITERATIONS) {
        missed = true;
      }

      if (missed) {
        break;
      }
    }

    if (missed) {
      for (CurveInterval c : a.split()) {
        intersections(b, c, acc, !inverted, epsilon);
      }
    }
  }

  public static IList<Vec2> curveCurve(Curve2 a, Curve2 b, double epsilon) {

    LinearList<Vec2> acc = new LinearList<>();

    for (CurveInterval ca : CurveInterval.from(a)) {
      for (CurveInterval cb : CurveInterval.from(b)) {
        intersections(ca, cb, acc, false, epsilon);
        //intersections(cb, ca, acc, true, epsilon);
      }
    }

    if (acc.size() < 2) {
      return acc;
    }

    acc = LinearList.from(Lists.sort(acc, Comparator.comparingDouble(v -> v.x)));

    Box2 bounds = PARAMETRIC_BOUNDS.expand(epsilon);

    IList<Vec2> result = new LinearList<>();
    while (acc.size() > 0) {
      Vec2 v = acc.popFirst();
      if (bounds.contains(v) && (result.size() == 0 || !Vec.equals(v, result.last(), epsilon))) {
        result.addLast(v);
      }
    }

    //System.out.println(result);

    return result.size() > 3
      ? collinearIntersection(a, b, epsilon)
      : result;
  }

  /// analytical solutions


  public static IList<Vec2> collinearIntersection(Curve2 a, Curve2 b, double epsilon) {
    LinearList<Vec2> result = new LinearList<>();

    for (int i = 0; i < 2; i++) {
      double tb = b.nearestPoint(a.position(i));

      // a overhangs the start of b
      if (tb <= 0) {
        double s = a.nearestPoint(b.start());
        if (inside(-epsilon, s, 1 + epsilon)) {
          result.addLast(vec(round(s, epsilon), 0));
        }

        // a overhangs the end of b
      } else if (tb >= 1) {
        double s = a.nearestPoint(b.end());
        if (inside(-epsilon, s, 1 + epsilon)) {
          result.addLast(vec(round(s, epsilon), 1));
        }

        // a is contained in b
      } else {
        result.addLast(vec(i, tb));
      }
    }

    if (result.size() == 2 && Vec.equals(result.nth(0), result.nth(1), epsilon)) {
      result.popLast();
    }

    //System.out.println(result);

    return result;
  }

  public static IList<Vec2> lineCurve(Line2 a, Curve2 b, double epsilon) {
    if (b instanceof Line2) {
      return lineLine(a, (Line2) b, epsilon);
    } else if (b.type() == Type.FLAT) {
      return lineLine(a, Line2.from(b.start(), b.end()), epsilon);
    } else if (b instanceof QuadraticBezier2) {
      return lineQuadratic(a, (QuadraticBezier2) b, epsilon);
    } else {
      return lineCubic(a, (CubicBezier2) b, epsilon);
    }
  }

  public static IList<Vec2> lineLine(Line2 a, Line2 b, double epsilon) {

    Vec2 av = a.end().sub(a.start());
    Vec2 bv = b.end().sub(b.start());

    double d = cross(av, bv);
    Vec2 asb = a.start().sub(b.start());

    if (inside(-epsilon, d, epsilon)) {
      return inside(-epsilon, cross(asb, av), epsilon)
        ? Intersections.collinearIntersection(a, b, epsilon)
        : Lists.EMPTY;
    }

    double s = cross(bv, asb) / d;
    double t = cross(av, asb) / d;
    return LinearList.of(vec(s, t));
  }

  public static IList<Vec2> lineQuadratic(Line2 p, QuadraticBezier2 q, double epsilon) {

    // (p0 - 2p1 + p2) t^2 + (-2p0 + 2p1) t + p0
    Vec2 a = q.p0.add(q.p1.mul(-2)).add(q.p2);
    Vec2 b = q.p0.mul(-2).add(q.p1.mul(2));
    Vec2 c = q.p0;

    Vec2 dir = p.end().sub(p.start());
    Vec2 n = vec(-dir.y, dir.x);

    double[] roots = Equations.solveQuadratic(
      dot(n, a),
      dot(n, b),
      dot(n, c) + cross(p.start(), p.end()),
      epsilon);

    IList<Vec2> result = new LinearList<>();
    if (inside(-EPSILON, dir.x, EPSILON)) {
      double y0 = p.start().y;
      for (double t : roots) {
        double y1 = q.position(t).y;
        result.addLast(vec((y1 - y0) / dir.y, t));
      }
    } else {
      double x0 = p.start().x;
      for (double t : roots) {
        double x1 = q.position(t).x;
        result.addLast(vec((x1 - x0) / dir.x, t));
      }
    }

    return result;
  }

  public static IList<Vec2> lineCubic(Line2 p, CubicBezier2 q, double epsilon) {

    // (-p0 + 3p1 - 3p2 + p3) t^3 + (3p0 - 6p1 + 3p2) t^2 + (-3p0 + 3p1) t + p0
    Vec2 a = q.p0.mul(-1).add(q.p1.mul(3)).add(q.p2.mul(-3)).add(q.p3);
    Vec2 b = q.p0.mul(3).add(q.p1.mul(-6)).add(q.p2.mul(3));
    Vec2 c = q.p0.mul(-3).add(q.p1.mul(3));
    Vec2 d = q.p0;

    Vec2 dir = p.end().sub(p.start());
    Vec2 n = vec(-dir.y, dir.x);

    double[] roots = Equations.solveCubic(
      dot(n, a),
      dot(n, b),
      dot(n, c),
      dot(n, d) + cross(p.start(), p.end()),
      epsilon);

    IList<Vec2> result = new LinearList<>();
    double dLen = dir.length();
    for (double t : roots) {
      Vec2 v = q.position(t).sub(p.start());
      double vLen = v.length();
      double s = (vLen / dLen) * signum(dot(dir, v));
      result.addLast(vec(s, t));
    }

    return result;
  }


}