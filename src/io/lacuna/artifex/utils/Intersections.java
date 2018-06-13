package io.lacuna.artifex.utils;

import io.lacuna.artifex.Curve2;
import io.lacuna.artifex.Vec;
import io.lacuna.artifex.Vec2;
import io.lacuna.bifurcan.LinearList;

import static io.lacuna.artifex.Box.box;

/**
 * @author ztellman
 */
public class Intersections {

  private static class CurveInterval {
    public final Curve2 c;
    public final double tLo, tHi, tEpsilon;
    public final Vec2 pLo, pHi;

    public CurveInterval(Curve2 c, double tLo, double tHi, double tEpsilon, Vec2 pLo, Vec2 pHi) {
      this.c = c;
      this.tLo = tLo;
      this.tHi = tHi;
      this.tEpsilon = tEpsilon;
      this.pLo = pLo;
      this.pHi = pHi;
    }

    public CurveInterval(Curve2 c) {
      this.c = c;
      this.tLo = 0;
      this.tHi = 1;
      this.tEpsilon = 1;
      this.pLo = c.start();
      this.pHi = c.end();
    }

    public static CurveInterval[] from(Curve2 c) {
      double[] ts = c.inflections();
      if (ts.length == 0) {
        return new CurveInterval[]{new CurveInterval(c)};
      } else {
        double tEpsilon = 1;
        for (int i = 0; i < ts.length - 1; i++) {
          tEpsilon = Math.min(tEpsilon, ts[i + 1] - ts[i]);
        }

        CurveInterval[] is = new CurveInterval[ts.length + 1];
        for (int i = 0; i < is.length; i++) {
          double lo = i == 0 ? 0 : ts[i - 1];
          double hi = i == is.length - 1 ? 1 : ts[i];
          Vec2 pLo = lo == 0 ? c.start() : is[i - 1].pHi;
          Vec2 pHi = hi == 1 ? c.end() : c.position(hi);
          is[i] = new CurveInterval(c, lo, hi, tEpsilon, pLo, pHi);
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

    public boolean intersects(CurveInterval c) {
      // TODO avoid allocation here
      return box(pLo, pHi).intersects(box(c.pLo, c.pHi));
    }

    public CurveInterval[] split() {
      double tMid = (tLo + tHi) / 2;
      Vec2 pMid = c.position(tMid);
      return new CurveInterval[]{
        new CurveInterval(c, tLo, tMid, tEpsilon, pLo, pMid),
        new CurveInterval(c, tMid, tHi, tEpsilon, pMid, pHi)};
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

  public static double[] collinearIntersection(Curve2 a, Curve2 b) {
    DoubleAccumulator acc = new DoubleAccumulator();

    for (int i = 0; i < 2; i++) {
      double tb = b.nearestPoint(a.position(i));

      // a overhangs the start of b
      if (tb <= 0) {
        double s = a.nearestPoint(b.start());
        if (0 <= s && s <= 1) {
          acc.add(s, 0);
        }

        // a overhangs the end of b
      } else if (tb >= 1) {
        double s = a.nearestPoint(b.end());
        if (0 <= s && s <= 1) {
          acc.add(s, 1);
        }

        // a is contained in b
      } else {
        return new double[]{i, tb};
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
    if (len > 0
      && Scalars.equals(aMid, acc.get(len - 2), a.tEpsilon)
      && Scalars.equals(bMid, acc.get(len - 1), b.tEpsilon)) {

      acc.set(len - 2, preferred(acc.get(len - 2), aMid));
      acc.set(len - 1, preferred(acc.get(len - 1), bMid));
      return false;
    }

    // if the curves share a tangent at the first point of intersection, they're collinear
    if (acc.size() == 0) {
      Vec2 aDir = a.dir(aMid).norm();
      Vec2 bDir = b.dir(bMid).norm();

      if (Vec.equals(aDir, bDir, epsilon) || Vec.equals(aDir, bDir.negate(), epsilon)) {
        double[] ts = collinearIntersection(a.c, b.c);
        acc.add(ts[0], ts[1]);
        acc.add(ts[2], ts[3]);

        return true;
      }
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
            if (as[i].intersects(bs[j])) {
              stack.addLast(as[i]).addLast(bs[j]);
            }
          }
        }
      }
    }

    return false;
  }

  public static double[] intersections(Curve2 a, Curve2 b, double epsilon) {

    if (!a.bounds().intersects(b.bounds())) {
      return new double[0];
    }

    CurveInterval[] as = CurveInterval.from(a);
    CurveInterval[] bs = CurveInterval.from(b);

    DoubleAccumulator accumulator = new DoubleAccumulator();
    for (int i = 0; i < as.length; i++) {
      for (int j = 0; j < bs.length; j++) {
        CurveInterval ai = as[i];
        CurveInterval bi = bs[j];
        if (ai.intersects(bi)) {
          if (intersections(ai, bi, accumulator, epsilon)) {
            return accumulator.toArray();
          }
        }
      }
    }

    return accumulator.toArray();
  }
}