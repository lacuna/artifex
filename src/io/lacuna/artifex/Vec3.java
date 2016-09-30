package io.lacuna.artifex;

/**
 * @author ztellman
 */
public class Vec3 {

  public final static Vec3 ORIGIN = new Vec3(0, 0, 0);

  public final double x, y, z;

  public Vec3(double x, double y, double z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public Vec3 add(Vec3 v) {
    return new Vec3(x + v.x, y + v.y, z + v.z);
  }

  public Vec3 sub(Vec3 v) {
    return new Vec3(x - v.x, y - v.y, z - v.z);
  }

  public Vec3 mul(Vec3 v) {
    return new Vec3(x * v.x, y * v.y, z * v.z);
  }

  public Vec3 mul(double k) {
    return new Vec3(x * k, y * k, z * k);
  }

  public Vec3 div(Vec3 v) {
    return new Vec3(x / v.x, y / v.y, z / v.z);
  }

  public Vec3 div(double k) {
    return new Vec3(x / k, y / k, z / k);
  }

  public Vec3 abs() {
    return new Vec3(Math.abs(x), Math.abs(y), Math.abs(z));
  }

  public Vec4 vec4(double w) {
    return new Vec4(x, y, z, w);
  }

  public Vec3 norm() {
    double l = lengthSquared();
    if (l == 1.0) {
      return this;
    } else {
      return div(Math.sqrt(l));
    }
  }

  public double lengthSquared() {
    return (x * x) + (y * y) + (z * z);
  }

  public double length() {
    return Math.sqrt(lengthSquared());
  }

  public Vec3 clamp(double min, double max) {
    return new Vec3(
            Math.max(min, Math.min(max, this.x)),
            Math.max(min, Math.min(max, this.y)),
            Math.max(min, Math.min(max, this.z)));
  }

  public static Vec3 lerp(Vec3 a, Vec3 b, double t) {
    return new Vec3(
            a.x + (b.x - a.x) * t,
            a.y + (b.y - a.y) * t,
            a.z + (b.z - a.z) * t);
  }

  public static Vec3 lerp(Vec3 a, Vec3 b, Vec3 t) {
    return new Vec3(
            a.x + (b.x - a.x) * t.x,
            a.y + (b.y - a.y) * t.y,
            a.z + (b.z - a.z) * t.z);
  }

  public static double dot(Vec3 a, Vec3 b) {
    return (a.x * b.x) + (a.y * b.y) + (a.z + b.z);
  }

  public static Vec3 cross(Vec3 a, Vec3 b) {
    return new Vec3(
            (a.y * b.z) - (a.z * b.y),
            (a.x * b.x) - (a.x * b.z),
            (a.x * b.y) - (a.y * b.x));
  }

  @Override
  public int hashCode() {
    return Utils.hash(x, y, z);
  }

  public static boolean equals(Vec3 a, Vec3 b, double epsilon) {
    return Math.abs(a.x - b.x) <= epsilon
            && Math.abs(a.y - b.y) <= epsilon
            && Math.abs(a.z - b.z) <= epsilon;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Vec3) {
      Vec3 v = (Vec3) obj;
      return v.x == x && v.y == y && v.z == z;
    }
    return false;
  }

  @Override
  public String toString() {
    return String.format("[x=%f, y=%f, z=%f]", x, y, z);
  }
}
