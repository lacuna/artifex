package io.lacuna.artifex;

import javax.sound.sampled.Line;

import static io.lacuna.artifex.Vec2.cross;
import static io.lacuna.artifex.Vec2.dot;
import static io.lacuna.artifex.utils.Equations.solveCubic;
import static java.lang.Math.abs;
import static java.lang.Math.signum;

/**
 * @author ztellman
 */
public class Bezier2 {

  public static LinearSegment2 from(Vec2 p0, Vec2 p1) {
    return new LinearSegment2(p0, p1);
  }

  public static QuadraticBezier2 from(Vec2 p0, Vec2 p1, Vec2 p2) {
    return new QuadraticBezier2(p0, p1, p2);
  }

  public static CubicBezier2 from(Vec2 p0, Vec2 p1, Vec2 p2, Vec2 p3) {
    return new CubicBezier2(p0, p1, p2, p3);
  }

  static class QuadraticBezier2 implements Curve2 {

    private final Vec2 p0, p1, p2;

    QuadraticBezier2(Vec2 p0, Vec2 p1, Vec2 p2) {
      this.p0 = p0;
      this.p1 = p1;
      this.p2 = p2;
    }

    @Override
    public Vec2 position(double t) {
      double mt = 1 - t;

      // (1 - t)^2 * p0 + 2t(1 - t) * p1 + t^2 * p2;
      return p0.mul(mt * mt)
              .add(p1.mul(2 * t * mt))
              .add(p2.mul(t * t));
    }

    @Override
    public Vec2 direction(double t) {
      double mt = 1 - t;

      // 2(1 - t) * (p1 - p0) + 2t * (p2 - p1)
      return p1.sub(p0).mul(2 * mt)
              .add(p2.sub(p1).mul(2 * t));
    }

    @Override
    public Curve2[] split(double t) {
      Vec2 e = Vec2.lerp(p0, p1, t);
      Vec2 f = Vec2.lerp(p1, p2, t);
      Vec2 g = Vec2.lerp(e, f, t);
      return new QuadraticBezier2[]{Bezier2.from(p0, e, g), Bezier2.from(g, f, p2)};
    }

    @Override
    public double nearestPoint(Vec2 p) {

      Vec2 qa = p0.sub(p);
      Vec2 ab = p1.sub(p0);
      Vec2 bc = p2.sub(p1);
      Vec2 qc = p2.sub(p);
      Vec2 ac = p2.sub(p0);
      Vec2 br = p0.add(p2).sub(p1).sub(p1);

      double a = dot(br, br);
      double b = 3 * dot(ab, br);
      double c = (2 * dot(ab, ab)) + dot(qa, br);
      double d = dot(qa, ab);

      double[] ts = solveCubic(a, b, c, d);
      double minDistance = signum(cross(ab, qa)) * qa.length();
      double param = -dot(qa, ab) / dot(ab, ab);

      double distance = signum(cross(bc, qc)) * qc.length();
      if (abs(distance) < abs(minDistance)) {
        minDistance = distance;
        param = dot(p.sub(p1), bc) / dot(bc, bc);
      }

      for (double t : ts) {
        if (t > 0 && t < 1) {
          Vec2 endpoint = p0.add(ab.mul(2 * t)).add(br.mul(t * t));
          distance = signum(cross(ac, endpoint.sub(p))) * endpoint.sub(p).length();
          if (abs(distance) < abs(minDistance)) {
            minDistance = distance;
            param = t;
          }
        }
      }

      return param;
    }
  }

  public static class CubicBezier2 implements Curve2 {

    private static final int SEARCH_STARTS = 4;
    private static final int SEARCH_STEPS = 4;

    private final Vec2 p0, p1, p2, p3;

    CubicBezier2(Vec2 p0, Vec2 p1, Vec2 p2, Vec2 p3) {
      this.p0 = p0;
      this.p1 = p1;
      this.p2 = p2;
      this.p3 = p3;
    }

    @Override
    public Vec2 position(double t) {
      double mt = 1 - t;
      double mt2 = mt * mt;
      double t2 = t * t;

      // (1 - t)^3 * p0 + 3t(1 - t)^2 * p1 + 3(1 - t)t^2 * p2 + t^3 * p3;
      return p0.mul(mt2 * mt)
              .add(p1.mul(3 * mt2 * t))
              .add(p2.mul(3 * mt * t2))
              .add(p3.mul(t2 * t));
    }

    @Override
    public Vec2 direction(double t) {
      double mt = 1 - t;

      // 3(1 - t)^2 * (p1 - p0) + 6(1 - t)t * (p2 - p1) + 3t^2 * (p3 - p2)
      return p1.sub(p0).mul(3 * mt * mt)
              .add(p2.sub(p1).mul(6 * mt * t))
              .add(p3.sub(p2).mul(3 * t * t));
    }

    @Override
    public Curve2[] split(double t) {
      Vec2 e = Vec2.lerp(p0, p1, t);
      Vec2 f = Vec2.lerp(p1, p2, t);
      Vec2 g = Vec2.lerp(p2, p3, t);
      Vec2 h = Vec2.lerp(e, f, t);
      Vec2 j = Vec2.lerp(f, g, t);
      Vec2 k = Vec2.lerp(f, j, t);
      return new CubicBezier2[]{Bezier2.from(p0, e, h, k), Bezier2.from(k, j, g, p3)};
    }

    @Override
    public double nearestPoint(Vec2 p) {
      Vec2 qa = p0.sub(p);
      Vec2 ab = p1.sub(p0);
      Vec2 bc = p2.sub(p1);
      Vec2 cd = p3.sub(p2);
      Vec2 qd = p3.sub(p);
      Vec2 br = bc.sub(ab);
      Vec2 as = cd.sub(bc).sub(br);

      double minDistance = signum(cross(ab, qa)) * qa.length();
      double param = -dot(qa, ab) / dot(ab, ab);

      double distance = signum(cross(cd, qd)) * qd.length();
      if (abs(distance) < abs(minDistance)) {
        minDistance = distance;
        param = dot(p.sub(p2), cd) / dot(cd, cd);
      }

      for (int i = 0; i < SEARCH_STARTS; i++) {
        double t = (double) i / SEARCH_STARTS;
        for (int step = 0; ; step++) {
          Vec2 qpt = position(t).sub(p);
          distance = signum(cross(direction(t), qpt)) * qpt.length();
          if (abs(distance) < abs(minDistance)) {
            minDistance = distance;
            param = t;
          }

          if (step == SEARCH_STEPS) {
            break;
          }

          Vec2 d1 = as.mul(3 * t * t).add(br.mul(6 * t)).add(ab.mul(3));
          Vec2 d2 = as.mul(6 * t).add(br.mul(6));
          t -= dot(qpt, d1) / (dot(d1, d1) + dot(qpt, d2));
          if (t < 0 || t > 1) {
            break;
          }
        }
      }

      return param;
    }
  }
}
