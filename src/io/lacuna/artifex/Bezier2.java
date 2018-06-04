package io.lacuna.artifex;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

import static io.lacuna.artifex.Vec.dot;
import static io.lacuna.artifex.Vec.lerp;
import static io.lacuna.artifex.Vec2.cross;
import static io.lacuna.artifex.utils.Equations.solveCubic;
import static io.lacuna.artifex.utils.Equations.solveQuadratic;
import static io.lacuna.artifex.utils.Scalars.inside;
import static java.lang.Math.abs;
import static java.lang.Math.max;

/**
 * @author ztellman
 */
public class Bezier2 {

  public static LineSegment2 bezier(Vec2 p0, Vec2 p1) {
    return LineSegment2.from(p0, p1);
  }

  public static QuadraticBezier2 bezier(Vec2 p0, Vec2 p1, Vec2 p2) {
    return new QuadraticBezier2(p0, p1, p2);
  }

  public static CubicBezier2 bezier(Vec2 p0, Vec2 p1, Vec2 p2, Vec2 p3) {
    return new CubicBezier2(p0, p1, p2, p3);
  }

  private static double sign(double n) {
    double s = Math.signum(n);
    return s == 0 ? -1 : s;
  }

  private static <V extends Curve2> void subdivide(List<Vec2> result, V c, ToDoubleFunction<V> error, double maxError) {
    if (error.applyAsDouble(c) <= maxError) {
      result.add(c.start());
    } else {
      Curve2[] split = c.split(0.5);
      subdivide(result, (V) split[0], error, maxError);
      subdivide(result, (V) split[1], error, maxError);
    }
  }

  public static class QuadraticBezier2 implements Curve2 {

    public final Vec2 p0, p1, p2;
    private double[] inflections = null;

    QuadraticBezier2(Vec2 p0, Vec2 p1, Vec2 p2) {
      this.p0 = p0;
      this.p1 = p1;
      this.p2 = p2;
    }

    @Override
    public Vec2 start() {
      return p0;
    }

    @Override
    public Vec2 end() {
      return p2;
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
    public QuadraticBezier2 end(Vec2 pos) {
      Vec2 delta = pos.sub(p2);
      return new QuadraticBezier2(p0, p1.add(delta), pos);
    }

    @Override
    public Curve2[] split(double t) {
      if (t == 0 || t == 1) {
        return new QuadraticBezier2[]{this};
      } else if (t < 0 || t > 1) {
        throw new IllegalArgumentException("t must be within [0,1]");
      }

      Vec2 e = lerp(p0, p1, t);
      Vec2 f = lerp(p1, p2, t);
      Vec2 g = lerp(e, f, t);
      return new QuadraticBezier2[]{bezier(p0, e, g), bezier(g, f, p2)};
    }

    @Override
    public Vec2[] subdivide(double error) {
      ArrayList<Vec2> points = new ArrayList<>();
      Bezier2.subdivide(points, this, b -> Vec.lerp(b.p0, b.p2, 0.5).sub(b.p1).lengthSquared(), error * error);
      points.add(end());

      return points.toArray(new Vec2[points.size()]);
    }

    @Override
    public double nearestPoint(Vec2 p) {

      Vec2 qa = p0.sub(p);
      Vec2 ab = p1.sub(p0);
      Vec2 bc = p2.sub(p1);
      Vec2 qc = p2.sub(p);
      Vec2 ac = p2.sub(p0);
      Vec2 br = p0.add(p2).sub(p1).sub(p1);

      double minDistance = sign(cross(ab, qa)) * qa.length();
      double param = -dot(qa, ab) / dot(ab, ab);

      double distance = sign(cross(bc, qc)) * qc.length();
      if (abs(distance) < abs(minDistance)) {
        minDistance = distance;
        param = max(1, dot(p.sub(p1), bc) / dot(bc, bc));
      }

      double a = dot(br, br);
      double b = 3 * dot(ab, br);
      double c = (2 * dot(ab, ab)) + dot(qa, br);
      double d = dot(qa, ab);
      double[] ts = solveCubic(a, b, c, d);

      for (double t : ts) {
        if (t > 0 && t < 1) {
          Vec2 endpoint = position(t);
          distance = sign(cross(ac, endpoint.sub(p))) * endpoint.sub(p).length();
          if (abs(distance) < abs(minDistance)) {
            minDistance = distance;
            param = t;
          }
        }
      }

      return param;
    }

    @Override
    public Curve2 transform(Matrix3 m) {
      return new QuadraticBezier2(p0.transform(m), p1.transform(m), p2.transform(m));
    }

    @Override
    public QuadraticBezier2 reverse() {
      return new QuadraticBezier2(p2, p1, p0);
    }

    @Override
    public double[] inflections() {

      if (inflections == null) {
        Vec2 div = p0.sub(p1.mul(2)).add(p2);
        if (div.x == 0 || div.y == 0) {
          inflections = new double[0];
        } else {
          Vec2 v = p0.sub(p1).div(div);
          boolean x = inside(v.x, 0, 1);
          boolean y = inside(v.y, 0, 1);
          if (x && y) {
            inflections = new double[]{v.x, v.y};
          } else if (x ^ y) {
            inflections = new double[]{x ? v.x : v.y};
          } else {
            inflections = new double[0];
          }
        }
      }

      return inflections;
    }

    @Override
    public int hashCode() {
      return ((p0.hashCode() * 31) + p1.hashCode()) * 31 + p2.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof QuadraticBezier2) {
        QuadraticBezier2 b = (QuadraticBezier2) obj;
        return p0.equals(b.p0) && p1.equals(b.p1) && p2.equals(b.p2);
      }
      return false;
    }

    @Override
    public String toString() {
      return "p0=" + p0 + ", p1=" + p1 + ", p2=" + p2;
    }

  }

  public static class CubicBezier2 implements Curve2 {

    private static final int SEARCH_STARTS = 8;
    private static final int SEARCH_STEPS = 8;

    private final Vec2 p0, p1, p2, p3;
    private double[] inflections;

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
    public CubicBezier2 end(Vec2 pos) {
      Vec2 delta = pos.sub(p3);
      return new CubicBezier2(p0, p1, p2.add(delta), pos);
    }

    @Override
    public Vec2 start() {
      return p0;
    }

    @Override
    public Vec2 end() {
      return p3;
    }

    @Override
    public CubicBezier2[] split(double t) {
      if (t == 0 || t == 1) {
        return new CubicBezier2[]{this};
      } else if (t < 0 || t > 1) {
        throw new IllegalArgumentException("t must be within [0,1]");
      }

      Vec2 e = lerp(p0, p1, t);
      Vec2 f = lerp(p1, p2, t);
      Vec2 g = lerp(p2, p3, t);
      Vec2 h = lerp(e, f, t);
      Vec2 j = lerp(f, g, t);
      Vec2 k = lerp(h, j, t);
      return new CubicBezier2[]{bezier(p0, e, h, k), bezier(k, j, g, p3)};
    }

    @Override
    public Vec2[] subdivide(double error) {
      List<Vec2> points = new ArrayList<>();
      Bezier2.subdivide(points, this,
        b -> Math.max(
          Vec.lerp(b.p0, b.p3, 1.0 / 3).sub(b.p1).lengthSquared(),
          Vec.lerp(b.p0, b.p3, 2.0 / 3).sub(b.p2).lengthSquared()),
        error * error);
      points.add(end());

      return points.toArray(new Vec2[points.size()]);
    }

    @Override
    /**
     * This quintic solver is adapted from https://github.com/Chlumsky/msdfgen, which is available under the MIT
     * license.
     */
    public double nearestPoint(Vec2 p) {
      Vec2 qa = p0.sub(p);
      Vec2 ab = p1.sub(p0);
      Vec2 bc = p2.sub(p1);
      Vec2 cd = p3.sub(p2);
      Vec2 qd = p3.sub(p);
      Vec2 br = bc.sub(ab);
      Vec2 as = cd.sub(bc).sub(br);

      double minDistance = sign(cross(ab, qa)) * qa.length();
      double param = -dot(qa, ab) / dot(ab, ab);

      double distance = sign(cross(cd, qd)) * qd.length();
      if (abs(distance) < abs(minDistance)) {
        minDistance = distance;
        param = max(1, dot(p.sub(p2), cd) / dot(cd, cd));
      }

      for (int i = 0; i < SEARCH_STARTS; i++) {
        double t = (double) i / SEARCH_STARTS;
        for (int step = 0; ; step++) {
          Vec2 qpt = position(t).sub(p);
          distance = sign(cross(direction(t), qpt)) * qpt.length();
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

    @Override
    public Curve2 transform(Matrix3 m) {
      return new CubicBezier2(p0.transform(m), p1.transform(m), p2.transform(m), p3.transform(m));
    }

    @Override
    public CubicBezier2 reverse() {
      return new CubicBezier2(p3, p2, p1, p0);
    }

    @Override
    public double[] inflections() {

      if (inflections == null) {
        Vec2 a0 = p1.sub(p0);
        Vec2 a1 = p2.sub(p1).sub(a0).mul(2);
        Vec2 a2 = p3.sub(p2.mul(3)).add(p1.mul(3)).sub(p0);

        double[] s1 = solveQuadratic(a2.x, a1.x, a0.x);
        double[] s2 = solveQuadratic(a2.y, a1.y, a0.y);

        int solutions = 0;
        for (double n : s1) if (inside(n, 0, 1)) solutions++;
        for (double n : s2) if (inside(n, 0, 1)) solutions++;

        inflections = new double[solutions];
        if (solutions > 0) {
          int idx = 0;
          for (double n : s1) if (inside(n, 0, 1)) inflections[idx++] = n;
          for (double n : s2) if (inside(n, 0, 1)) inflections[idx++] = n;
        }
      }

      return inflections;
    }

    @Override
    public int hashCode() {
      return (((p0.hashCode() * 31) + p1.hashCode()) * 31 + p2.hashCode()) * 31 + p3.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof CubicBezier2) {
        CubicBezier2 b = (CubicBezier2) obj;
        return p0.equals(b.p0) && p1.equals(b.p1) && p2.equals(b.p2) && p3.equals(b.p3);
      }
      return false;
    }

    @Override
    public String toString() {
      return "p0=" + p0 + ", p1=" + p1 + ", p2=" + p2 + ", p3=" + p3;
    }
  }
}
