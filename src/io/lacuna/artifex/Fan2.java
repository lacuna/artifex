package io.lacuna.artifex;

import io.lacuna.artifex.Bezier2.CubicBezier2;
import io.lacuna.artifex.utils.regions.Hulls;

import static io.lacuna.artifex.Vec.vec;

public class Fan2 {

  public enum Type {
    CONVEX,
    CONCAVE,
    INTERNAL
  }

  public final Vec2 origin;
  public final Curve2 edge;
  public final Type type;

  private Fan2(Vec2 origin, Curve2 edge, Type type) {
    this.origin = origin;
    this.edge = edge;
    this.type = type;
  }

  public static Fan2 curve(Curve2 c) {
    return new Fan2(Hulls.tangentIntersection(c), c, c.isConvex() ? Type.CONVEX : Type.CONCAVE);
  }

  public static Fan2 internal(Vec2 a, Vec2 b, Vec2 c) {
    return new Fan2(a, LineSegment2.from(b, c), Type.INTERNAL);
  }

  public static Fan2 external(Vec2 origin, LineSegment2 line) {
    return new Fan2(origin, line, Type.CONVEX);
  }

  ///

  public boolean isInternal() {
    return type == Type.INTERNAL;
  }

  public boolean isConvex() {
    return type == Type.CONVEX;
  }

  /// adapted from https://developer.nvidia.com/gpugems/GPUGems3/gpugems3_ch25.html

  private enum CubicType {
    LINE,
    QUADRATIC,
    SERPENTINE,
    CUSP,
    LOOP
  }

  public static Vec3 coefficients(CubicBezier2 c) {
    Vec2 p0 = c.p0,
      p1 = c.p1,
      p2 = c.p2,
      p3 = c.p3;

    double a1 = p0.y * (p2.x - p3.x) + p3.x * p2.y - p2.x * p3.y + p0.x * (p3.y - p2.y),
      a2 = p0.x * p3.y - p3.x * p0.y + p1.y * (p3.x - p0.x) + p1.x * (p0.y - p3.y),
      a3 = p1.x * p0.y - p0.x * p1.y + p2.x * (p1.y - p0.y) + p2.y * (p0.x - p1.x),
      d3 = 3 * a3,
      d2 = d3 - a2,
      d1 = d2 - a2 + a1;

    return vec(d1, d2, d3);
  }

  public CubicType cubicType(Vec3 d) {
    double d1 = d.x, d2 = d.y, d3 = d.z;
    double disc = (d1 * d1) * ((3 * d2 * d2) - (4 * d1 * d3));

    if (disc < 0) {
      return CubicType.LOOP;
    } else if (disc > 0) {
      return CubicType.SERPENTINE;
    } else if (d1 == d2) {
      return d2 == d3 ? CubicType.LINE : CubicType.QUADRATIC;
    } else {
      return CubicType.CUSP;
    }
  }

  public static Matrix4 reverse(Matrix4 m) {
    return m.mul(Matrix4.scale(-1, -1, 1));
  }

  public static Matrix4 serpentine(Vec3 d) {

    final double
      d1 = d.x,
      d2 = d.y,
      d3 = d.z,
      disc = Math.sqrt((9 * d2 * d2) - (12 * d1 * d3)),
      t = 6 * d1,
      l = (3 * d2) - disc,
      m = (3 * d2) + disc,
      lm = l * m,
      mt = m * t,
      lt = l * t,
      tt = t * t,
      ll = l * l,
      mm = m * m;

    Vec4 v1 = vec(
      lm,
      ((3 * lm) - lt - mt) / 3,
      ((tt - (2 * mt)) + ((3 * lm) - (2 * lt))) / 3,
      (t - l) * (t - m));

    final double tsl = t - l;
    Vec4 v2 = vec(
      ll * l,
      ll * (l - t),
      tsl * tsl * l,
      -tsl * tsl * tsl);

    final double tsm = t - m;
    Vec4 v3 = vec(
      mm * m,
      (mm * m) - (mm * t),
      tsm * tsm * m,
      -tsm * tsm * tsm);

    Matrix4 result = Matrix4.from(v1, v2, v3, vec(0, 0, 0, 1));
    return d1 < 0 ? reverse(result) : result;
  }

  public static Matrix4 loop(Vec3 d) {

    final double
      d1 = d.x,
      d2 = d.y,
      d3 = d.z,
      disc = Math.sqrt((4 * d1 * d3) - (3 * d2 * d2)),
      l = d2 - disc,
      m = d2 + disc,
      t = 2 * d1,
      lm = l * m,
      mt = m * t,
      lt = l * t,
      tt = t * t,
      mm = m * m,
      ll = l * l;

    Vec4 v1 = vec(
      l * m,
      (-lt - mt + lm) / 3,
      ((tt - (2 * mt)) + ((3 * lm) - (2 * lt))) / 3,
      (t - l) * (t - m));

    Vec4 v2 = vec(
      ll * m,
      (-l / 3) * (lt - (3 * lm) + (2 * mt)),
      ((t - l) / 3) * ((2 * lt) - (3 * lm) + mt),
      -(t - l) * (t - l) * (t - m));

    Vec4 v3 = vec(
      l * mm,
      (-m / 3) * ((2 * lt) - (3 * lm) + mt),
      ((t - m) / 3) * (lt - (3 * lm) + (2 * mt)),
      -(t - l) * (t - m) * (t - m));

    Matrix4 result = Matrix4.from(v1, v2, v3, vec(0, 0, 0, 1));
    return (d1 < 0 && v1.y > 0) || (d1 > 0 && v1.y < 0) ? reverse(result) : result;
  }

  public static Matrix4 cusp(Vec3 d) {

    final double
      d2 = d.y,
      d3 = d.z,
      l = d3,
      t = 3 * d2,
      ll = l * l,
      lst = l - t;

    Vec4 v1 = vec(
      l,
      l - (t / 3),
      l - (2 * t / 3),
      lst);

    Vec4 v2 = vec(
      ll * l,
      ll * lst,
      lst * lst * l,
      lst * lst * lst);

    return Matrix4.from(v1, v2, vec(1, 1, 1, 1), vec(0, 0, 0, 1));
  }

  public static Matrix4 quadratic(Vec3 d) {
    return null;
  }
}
