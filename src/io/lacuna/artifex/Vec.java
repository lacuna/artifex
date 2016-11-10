package io.lacuna.artifex;

import java.util.function.*;

/**
 * @author ztellman
 */
@SuppressWarnings("unchecked")
public abstract class Vec<T extends Vec<T>> {

  private static final DoubleBinaryOperator ADD = (a, b) -> a + b;
  private static final DoubleBinaryOperator MUL = (a, b) -> a * b;
  private static final DoubleBinaryOperator SUB = (a, b) -> a - b;
  private static final DoubleBinaryOperator DIV = (a, b) -> a / b;
  private static final DoubleBinaryOperator DELTA = (a, b) -> Math.abs(a - b);

  public abstract T map(DoubleUnaryOperator f);

  public abstract double reduce(DoubleBinaryOperator f);

  public abstract T zip(T v, DoubleBinaryOperator f);

  public abstract boolean every(DoublePredicate f);

  public abstract boolean any(DoublePredicate f);

  public T add(T v) {
    return zip(v, ADD);
  }

  public T sub(T v) {
    return zip(v, SUB);
  }

  public T mul(T v) {
    return zip(v, MUL);
  }

  public T mul(final double k) {
    return map(i -> i * k);
  }

  public T div(T v) {
    return zip(v, DIV);
  }

  public T div(double k) {
    return map(i -> i / k);
  }

  public T abs() {
    return map(Math::abs);
  }

  public double lengthSquared() {
    return dot((T) this, (T) this);
  }

  public double length() {
    return Math.sqrt(lengthSquared());
  }

  public T norm() {
    double l = lengthSquared();
    if (l == 1.0) {
      return (T) this;
    } else {
      return div(Math.sqrt(l));
    }
  }

  public T clamp(double min, double max) {
    return map(i -> Math.max(min, Math.min(max, i)));
  }

  public T clamp(T min, T max) {
    return zip(min, Math::max).zip(max, Math::min);
  }

  public static <T extends Vec<T>> double dot(T a, T b) {
    return a.zip(b, MUL).reduce(ADD);
  }

  public static <T extends Vec<T>> T lerp(T a, T b, double t) {
    return a.add(b.sub(a).mul(t));
  }

  public static <T extends Vec<T>> T lerp(T a, T b, T t) {
    return a.add(b.sub(a).mul(t));
  }

  public static <T extends Vec<T>> boolean equals(T a, T b, double epsilon) {
    return a.zip(b, DELTA).every(i -> i <= epsilon);
  }

}
