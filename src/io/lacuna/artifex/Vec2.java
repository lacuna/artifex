package io.lacuna.artifex;

import io.lacuna.artifex.utils.Hash;

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoublePredicate;
import java.util.function.DoubleUnaryOperator;

/**
 * @author ztellman
 */
public class Vec2 extends Vec<Vec2> {

  public final static Vec2 ORIGIN = new Vec2(0, 0);
  public static final Vec2 X_AXIS = new Vec2(1, 0);
  public static final Vec2 Y_AXIS = new Vec2(0, 1);

  public final double x, y;

  public Vec2(double x, double y) {
    this.x = x;
    this.y = y;
  }

  @Override
  public final Vec2 map(DoubleUnaryOperator f) {
    return new Vec2(f.applyAsDouble(x), f.applyAsDouble(y));
  }

  @Override
  public final double reduce(DoubleBinaryOperator f) {
    return f.applyAsDouble(x, y);
  }

  @Override
  public final Vec2 zip(final Vec2 v, final DoubleBinaryOperator f) {
    return new Vec2(f.applyAsDouble(x, v.x), f.applyAsDouble(y, v.y));
  }

  @Override
  public boolean every(DoublePredicate f) {
    return f.test(x) && f.test(y);
  }

  @Override
  public boolean any(DoublePredicate f) {
    return f.test(x) || f.test(y);
  }

  public Vec3 vec3(double z) {
    return new Vec3(x, y, z);
  }

  public Vec4 vec4(double z, double w) {
    return new Vec4(x, y, z, w);
  }

  public Vec2 transform(Matrix3 m) {
    return m.transform(this);
  }

  /**
   * @return a rotated vector
   */
  public Vec2 rotate(double radians) {
    double s = Math.sin(radians);
    double c = Math.cos(radians);
    return new Vec2((c * x) + (-s * y), (s * x) + (c * y));
  }

  public static double cross(Vec2 a, Vec2 b) {
    return (a.x * b.y) - (a.y * b.x);
  }

  /**
   * @return the clockwise angle between the two vectors, which don't have to be normalized
   */
  public static double angleBetween(Vec2 a, Vec2 b) {
    Vec2 na = a.norm();
    Vec2 nb = b.norm();
    double theta = Math.acos(dot(na, nb));
    if (cross(na, nb) > 0) {
      theta = (Math.PI * 2) - theta;
    }
    return -theta;
  }

  public Polar2 polar2() {
    return new Polar2(Math.atan2(y, x), length());
  }

  @Override
  public int hashCode() {
    return Hash.hash(x, y);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Vec2) {
      Vec2 v = (Vec2) obj;
      return v.x == x && v.y == y;
    }
    return false;
  }

  @Override
  public String toString() {
    return String.format("[x=%f, y=%f]", x, y);
  }

}
