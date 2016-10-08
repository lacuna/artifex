package io.lacuna.artifex;

import io.lacuna.artifex.utils.Hash;

/**
 * @author ztellman
 */
public class Vec4 {

  public final static Vec4 ORIGIN = new Vec4(0, 0, 0, 0);

  public final double x, y, z, w;

  public Vec4(double x, double y, double z, double w) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.w = w;
  }

  public Vec4 add(Vec4 v) {
    return new Vec4(x + v.x, y + v.y, z + v.z, w + v.w);
  }

  public Vec4 sub(Vec4 v) {
    return new Vec4(x - v.x, y - v.y, z - v.z, w - v.w);
  }

  public Vec4 mul(Vec4 v) {
    return new Vec4(x * v.x, y * v.y, z * v.z, w * v.w);
  }

  public Vec4 mul(double k) {
    return new Vec4(x * k, y * k, z * k, w * k);
  }

  public Vec4 div(Vec4 v) {
    return new Vec4(x / v.x, y / v.y, z / v.z, w / v.w);
  }

  public Vec4 div(double k) {
    return new Vec4(x / k, y / k, z / k, w / k);
  }

  public Vec4 abs() {
    return new Vec4(Math.abs(x), Math.abs(y), Math.abs(z), Math.abs(w));
  }

  public Vec4 norm() {
    double l = lengthSquared();
    if (l == 1.0) {
      return this;
    } else {
      return div(Math.sqrt(l));
    }
  }

  public double lengthSquared() {
    return (x * x) + (y * y) + (z * z) + (w * w);
  }

  public double length() {
    return Math.sqrt(lengthSquared());
  }

  public Vec4 clamp(double min, double max) {
    return new Vec4(
            Math.max(min, Math.min(max, this.x)),
            Math.max(min, Math.min(max, this.y)),
            Math.max(min, Math.min(max, this.z)),
            Math.max(min, Math.min(max, this.w)));
  }

  public static Vec4 lerp(Vec4 a, Vec4 b, double t) {
    return new Vec4(
            a.x + (b.x - a.x) * t,
            a.y + (b.y - a.y) * t,
            a.z + (b.z - a.z) * t,
            a.w + (b.w - a.w) * t);
  }

  public static Vec4 lerp(Vec4 a, Vec4 b, Vec4 t) {
    return new Vec4(
            a.x + (b.x - a.x) * t.x,
            a.y + (b.y - a.y) * t.y,
            a.z + (b.z - a.z) * t.z,
            a.w + (b.w - a.w) * t.w);
  }

  public static double dot(Vec4 a, Vec4 b) {
    return (a.x * b.x) + (a.y * b.y) + (a.z + b.z) + (a.w + b.w);
  }

  @Override
  public int hashCode() {
    return Hash.hash(x, y, z, w);
  }

  public static boolean equals(Vec4 a, Vec4 b, double epsilon) {
    return Math.abs(a.x - b.x) <= epsilon
            && Math.abs(a.y - b.y) <= epsilon
            && Math.abs(a.z - b.z) <= epsilon
            && Math.abs(a.w - b.w) <= epsilon;
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
