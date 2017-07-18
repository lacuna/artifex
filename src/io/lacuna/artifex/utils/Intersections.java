package io.lacuna.artifex.utils;

import io.lacuna.artifex.*;

/**
 * @author ztellman
 */
public class Intersections {

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

    public boolean intersects(CurveInterval c) {
      return Box.from(pLo, pHi).intersects(Box.from(c.pLo, c.pHi));
    }

    public CurveInterval[] split() {
      double tMid = (tLo + tHi) / 2;
      Vec2 pMid = c.position(tMid);
      return new CurveInterval[]{new CurveInterval(c, tLo, tMid, pLo, pMid), new CurveInterval(c, tMid, tHi, pMid, pHi)};
    }

    public Vec2 dir(double t) {
      return c.direction(t).norm();
    }
  }

  private static int addIntersection(CurveInterval a, CurveInterval b, int idx, double[] ts, double epsilon) {
    double aMid = (a.tLo + a.tHi) / 2;
    double bMid = (b.tLo + b.tHi) / 2;
    boolean aEndpoint = a.tLo < epsilon || (a.tHi + epsilon) > 1;
    boolean bEndpoint = b.tLo < epsilon || (b.tHi + epsilon) > 1;

    // touching endpoints aren't an intersection
    if (aEndpoint && bEndpoint) {
      return 0;
    }

    // is this a duplicate match
    if (idx > 0 && Scalars.equals(aMid, ts[idx - 2], epsilon)) {
      return 0;
    }

    // if the curves share a tangent at the first point of intersection, that's a problem
    Vec2 aDir = a.dir(aMid);
    Vec2 bDir = b.dir(bMid);
    Vec2 nbDir = bDir.map(x -> -x);
    if (idx == 0 && (Vec.equals(aDir, bDir, epsilon) || Vec.equals(aDir, nbDir, epsilon))) {
      throw new IllegalArgumentException("overlapping curves have infinite points of intersection");
    }

    ts[idx] = aMid;
    ts[idx + 1] = bMid;
    return 2;
  }

  private static int intersections(CurveInterval a, CurveInterval b, int idx, double[] ts, double epsilon) {

    if (a.tHi - a.tLo <= epsilon) {
      return addIntersection(a, b, idx, ts, epsilon);
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
        CurveInterval ai = as[i];
        CurveInterval bi = bs[j];
        if (ai.intersects(bi)) {
          idx += intersections(ai, bi, idx, ts, epsilon);
        }
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
}