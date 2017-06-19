package io.lacuna.artifex.utils;

import io.lacuna.artifex.*;

import java.util.HashSet;
import java.util.Set;

import static java.lang.Math.abs;

/**
 * @author ztellman
 */
public class Curves {

  private static final double EPSILON = 1e-14;

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
      Interval2 endpoints = c.endpoints();

      this.c = c;
      this.tLo = 0;
      this.tHi = 1;
      this.pLo = endpoints.lower();
      this.pHi = endpoints.upper();
    }

    public static CurveInterval[] from(Curve2 c) {
      double[] ts = c.inflections();
      if (ts.length == 0) {
        return new CurveInterval[]{new CurveInterval(c)};
      } else {
        CurveInterval[] is = new CurveInterval[ts.length + 1];

        Interval2 endpoints = c.endpoints();
        for (int i = 0; i < is.length; i++) {
          double lo = i == 0 ? 0 : ts[i - 1];
          double hi = i == is.length - 1 ? 1 : ts[i];
          Vec2 pLo = lo == 0 ? endpoints.lower() : is[i - 1].pHi;
          Vec2 pHi = hi == 1 ? endpoints.upper() : c.position(hi);
          is[i] = new CurveInterval(c, lo, hi, pLo, pHi);
        }

        return is;
      }
    }

    public boolean smallerThan(double epsilon) {
      return pHi.sub(pLo).every(n -> abs(n) < epsilon);
    }

    public boolean intersects(CurveInterval c) {
      return Interval.from(pLo, pHi).intersects(Interval.from(c.pLo, c.pHi));
    }

    public CurveInterval[] split() {
      double tMid = (tLo + tHi) / 2;
      Vec2 pMid = c.position(tMid);
      return new CurveInterval[]{new CurveInterval(c, tLo, tMid, pLo, pMid), new CurveInterval(c, tMid, tHi, pMid, pHi)};
    }
  }

  private static int intersections(CurveInterval a, CurveInterval b, int idx, double[] ts, double epsilon) {

    if (a.smallerThan(epsilon)) {
      double mid = (a.tLo + a.tHi) / 2;
      if (idx > 0 && Scalars.equals(mid, ts[idx - 1], epsilon)) {
        return 0;
      } else {
        ts[idx] = mid;
        return 1;
      }
    }

    CurveInterval[] as = a.split();
    CurveInterval[] bs = b.split();

    int prevIdx = idx;

    if (as[0].intersects(bs[0])) {
      idx += intersections(as[0], bs[0], idx, ts, epsilon);
    }

    if (as[0].intersects(bs[1])) {
      idx += intersections(as[0], bs[1], idx, ts, epsilon);
    }

    if (as[1].intersects(bs[0])) {
      idx += intersections(as[1], bs[0], idx, ts, epsilon);
    }

    if (as[1].intersects(bs[1])) {
      idx += intersections(as[1], bs[1], idx, ts, epsilon);
    }

    return idx - prevIdx;
  }

  public static double[] intersections(Curve2 a, Curve2 b, double epsilon) {
    CurveInterval[] as = CurveInterval.from(a);
    CurveInterval[] bs = CurveInterval.from(b);

    int idx = 0;
    double[] ts = new double[as.length * bs.length * 2];
    for (int i = 0; i < as.length; i++) {
      for (int j = 0; j < bs.length; j++) {
        idx += intersections(as[i], bs[j], idx, ts, epsilon);
      }
    }

    if (idx == ts.length) {
      return ts;
    } else {
      double[] result = new double[idx];
      System.arraycopy(ts, 0, result, 0, idx);
      return result;
    }
  }

  public static double[] intersections(Curve2 a, Curve2 b) {
    return intersections(a, b, EPSILON);
  }
}
