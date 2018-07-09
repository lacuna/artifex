package io.lacuna.artifex.utils;

import io.lacuna.artifex.Curve2;
import io.lacuna.artifex.Vec;
import io.lacuna.artifex.Vec2;
import io.lacuna.bifurcan.LinearList;

import static io.lacuna.artifex.Box.box;
import static io.lacuna.artifex.utils.Scalars.EPSILON;

/**
 * @author ztellman
 */
public class Intersections {

  // in parametric space, how close can two intersection points be before they're treated as a single point
  public static final double INTERSECTION_EPSILON = 1e-10;

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
      if (tLo < epsilon) {
        return 0;
      }
      if (tHi + epsilon > 1) {
        return 1;
      }

      return (tLo + tHi) / 2;
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

    public Vec2 dir(double t) {
      return c.direction(t).norm();
    }
  }

  private static double preferred(double a, double b) {
    if (a < 0.5) {
      return Math.min(a, b);
    } else {
      return Math.max(a, b);
    }
  }

  public static double round(double t, double epsilon) {
    if (t < epsilon) {
      return 0;
    } else if (t + epsilon > 1) {
      return 1;
    } else {
      return t;
    }
  }

  public static double[] collinearIntersection(Curve2 a, Curve2 b) {
    DoubleAccumulator acc = new DoubleAccumulator();

    for (int i = 0; i < 2; i++) {
      double tb = b.nearestPoint(a.position(i));

      // a overhangs the start of b
      if (tb <= 0) {
        double s = a.nearestPoint(b.start());
        if (Scalars.inside(-EPSILON, s, 1 + EPSILON)) {
          acc.add(round(s, EPSILON), 0);
        }

        // a overhangs the end of b
      } else if (tb >= 1) {
        double s = a.nearestPoint(b.end());
        if (Scalars.inside(-EPSILON, s, 1 + EPSILON)) {
          acc.add(round(s, EPSILON), 1);
        }

        // a is contained in b
      } else {
        acc.add(i, tb);
      }
    }

    if (acc.size() == 4 && acc.get(0) == acc.get(2)) {
      acc.pop(2);
    }



    return acc.toArray();
  }

  private static boolean addIntersection(CurveInterval a, CurveInterval b, DoubleAccumulator acc, double epsilon) {
    double aMid = a.mid(epsilon);
    double bMid = b.mid(epsilon);

    // is this a duplicate match
    int len = acc.size();
    if (len > 0 &&
      (Scalars.equals(aMid, acc.get(len - 2), INTERSECTION_EPSILON)
        || Scalars.equals(bMid, acc.get(len - 1), INTERSECTION_EPSILON))) {
      acc.set(len - 2, preferred(acc.get(len - 2), aMid));
      acc.set(len - 1, preferred(acc.get(len - 1), bMid));
      return false;
    }

    // if we've found more than two intersection points, we must be collinear
    // TODO there must be a more elegant way to figure this out which is also robust
    if (acc.size() == 4) {
      acc.clear();
      acc.add(collinearIntersection(a.c, b.c));
      return true;
    }

    acc.add(aMid, bMid);

    return false;
  }

  private static boolean intersections(CurveInterval a, CurveInterval b, DoubleAccumulator acc, double epsilon) {

    LinearList<CurveInterval> stack = new LinearList<>();
    stack.addLast(a).addLast(b);

    while (stack.size() > 0) {

      b = stack.popLast();
      a = stack.popLast();

      if (a.tHi - a.tLo <= epsilon) {
        if (addIntersection(a, b, acc, epsilon)) {
          return true;
        }
      } else {
        CurveInterval[] as = a.split();
        CurveInterval[] bs = b.split();

        for (int i = 0; i < as.length; i++) {
          for (int j = 0; j < bs.length; j++) {
            if (as[i].intersects(bs[j], epsilon)) {
              stack.addLast(as[i]).addLast(bs[j]);
            }
          }
        }
      }
    }

    return false;
  }

  public static double[] intersections(Curve2 a, Curve2 b, double epsilon) {

    if (!a.bounds().expand(epsilon).intersects(b.bounds())) {
      return new double[0];
    }

    CurveInterval[] as = CurveInterval.from(a);
    CurveInterval[] bs = CurveInterval.from(b);

    DoubleAccumulator accumulator = new DoubleAccumulator();
    for (int i = 0; i < as.length; i++) {
      for (int j = 0; j < bs.length; j++) {
        CurveInterval ai = as[i];
        CurveInterval bi = bs[j];
        if (ai.intersects(bi, epsilon)) {
          if (intersections(ai, bi, accumulator, epsilon)) {
            return accumulator.toArray();
          }
        }
      }
    }

    return accumulator.toArray();
  }
}