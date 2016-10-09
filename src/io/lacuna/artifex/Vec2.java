package io.lacuna.artifex;

import io.lacuna.artifex.utils.Hash;

/**
 * @author ztellman
 */
public class Vec2 {

  public final static Vec2 ORIGIN = new Vec2(0, 0);

  public final double x, y;

  public Vec2(double x, double y) {
    this.x = x;
    this.y = y;
  }

  public Vec2 add(Vec2 v) {
    return new Vec2(x + v.x, y + v.y);
  }

  public Vec2 sub(Vec2 v) {
    return new Vec2(x - v.x, y - v.y);
  }

  public Vec2 mul(Vec2 v) {
    return new Vec2(x * v.x, y * v.y);
  }

  public Vec2 mul(double k) {
    return new Vec2(x * k, y * k);
  }

  public Vec2 div(Vec2 v) {
    return new Vec2(x / v.x, y / v.y);
  }

  public Vec2 div(double k) {
    return new Vec2(x / k, y / k);
  }

  public Vec2 abs() {
    return new Vec2(Math.abs(x), Math.abs(y));
  }

  public Vec3 vec3(double z) {
    return new Vec3(x, y, z);
  }

  public Vec4 vec4(double z, double w) {
    return new Vec4(x, y, z, w);
  }

  public Vec2 rotate(double radians) {
    double s = Math.sin(radians);
    double c = Math.cos(radians);
    return new Vec2((c * x) + (-s * y), (s * x) + (c * y));
  }

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

  public Vec2 norm() {
    double l = lengthSquared();
    if (l == 1.0) {
      return this;
    } else {
      return div(Math.sqrt(l));
    }
  }

  public double lengthSquared() {
    return (x * x) + (y * y);
  }

  public double length() {
    return Math.sqrt(lengthSquared());
  }

  public Vec2 clamp(double min, double max) {
    return new Vec2(
            Math.max(min, Math.min(max, this.x)),
            Math.max(min, Math.min(max, this.y)));
  }

  public static Vec2 lerp(Vec2 a, Vec2 b, double t) {
    return new Vec2(
            a.x + (b.x - a.x) * t,
            a.y + (b.y - a.y) * t);
  }

  public static Vec2 lerp(Vec2 a, Vec2 b, Vec2 t) {
    return new Vec2(
            a.x + (b.x - a.x) * t.x,
            a.y + (b.y - a.y) * t.y);
  }

  public static double dot(Vec2 a, Vec2 b) {
    return (a.x * b.x) + (a.y * b.y);
  }

  public static double cross(Vec2 a, Vec2 b) {
    return (a.x * b.y) - (a.y * b.x);
  }

  @Override
  public int hashCode() {
    return Hash.hash(x, y);
  }

  public static boolean equals(Vec2 a, Vec2 b, double epsilon) {
    return Math.abs(a.x - b.x) <= epsilon
            && Math.abs(a.y - b.y) <= epsilon;
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
