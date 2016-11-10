package io.lacuna.artifex;

import io.lacuna.artifex.utils.Hash;

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoublePredicate;
import java.util.function.DoubleUnaryOperator;

/**
 * @author ztellman
 */
public class Vec4 extends Vec<Vec4> {
  public final double x, y, z, w;

  public Vec4(double x, double y, double z, double w) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.w = w;
  }

  @Override
  public final Vec4 map(DoubleUnaryOperator f) {
    return new Vec4(f.applyAsDouble(x), f.applyAsDouble(y), f.applyAsDouble(z), f.applyAsDouble(w));
  }

  @Override
  public final double reduce(DoubleBinaryOperator f) {
    return f.applyAsDouble(f.applyAsDouble(x, y), f.applyAsDouble(z, w));
  }

  @Override
  public final Vec4 zip(final Vec4 v, final DoubleBinaryOperator f) {
    return new Vec4(f.applyAsDouble(x, v.x), f.applyAsDouble(y, v.y), f.applyAsDouble(z, v.z), f.applyAsDouble(w, v.w));
  }

  @Override
  public boolean every(DoublePredicate f) {
    return f.test(x) && f.test(y) & f.test(z) && f.test(w);
  }

  @Override
  public boolean any(DoublePredicate f) {
    return f.test(x) || f.test(y) || f.test(z) || f.test(w);
  }

  @Override
  public int hashCode() {
    return Hash.hash(x, y, z, w);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Vec4) {
      Vec4 v = (Vec4) obj;
      return v.x == x && v.y == y && v.z == z && v.w == w;
    }
    return false;
  }

  @Override
  public String toString() {
    return String.format("[x=%f, y=%f, z=%f, w=%f]", x, y, z, w);
  }
}
