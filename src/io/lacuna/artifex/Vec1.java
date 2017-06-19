package io.lacuna.artifex;

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoublePredicate;
import java.util.function.DoubleUnaryOperator;

/**
 *
 */
public class Vec1 implements Vec<Vec1> {

  public final double x;

  public Vec1(double x) {
    this.x = x;
  }

  @Override
  public Vec1 map(DoubleUnaryOperator f) {
    return new Vec1(f.applyAsDouble(x));
  }

  @Override
  public double reduce(DoubleBinaryOperator f, double init) {
    return f.applyAsDouble(init, x);
  }

  @Override
  public double reduce(DoubleBinaryOperator f) {
    throw new IllegalStateException("can't perform reduction over vector of dim=1");
  }

  @Override
  public Vec1 zip(Vec1 v, DoubleBinaryOperator f) {
    return new Vec1(f.applyAsDouble(x, v.x));
  }

  @Override
  public boolean every(DoublePredicate f) {
    return f.test(x);
  }

  @Override
  public boolean any(DoublePredicate f) {
    return f.test(x);
  }

  @Override
  public double nth(int idx) {
    if (idx == 0) {
      return x;
    } else {
      throw new IndexOutOfBoundsException();
    }
  }

  @Override
  public int dim() {
    return 1;
  }
}

