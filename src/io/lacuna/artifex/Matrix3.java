package io.lacuna.artifex;

/**
 * @author ztellman
 */
public class Matrix3 {

  public static final Matrix3 IDENTITY = new Matrix3(1, 0, 0, 0, 1, 0, 0, 0, 1);

  private final double m00, m01, m02, m10, m11, m12, m20, m21, m22;

  private Matrix3(double m00, double m01, double m02,
                  double m10, double m11, double m12,
                  double m20, double m21, double m22) {
    this.m00 = m00;
    this.m01 = m01;
    this.m02 = m02;
    this.m10 = m10;
    this.m11 = m11;
    this.m12 = m12;
    this.m20 = m20;
    this.m21 = m21;
    this.m22 = m22;
  }

  public static Matrix3 from(Vec2 a, Vec2 b) {
    return new Matrix3(a.x, b.x, 0, a.y, b.y, 0, 0, 0, 1);
  }

  public static Matrix3 from(Vec3 a, Vec3 b) {
    return new Matrix3(a.x, a.x, 0, a.y, b.y, 0, a.z, b.z, 1);
  }

  public static Matrix3 from(Vec3 a, Vec3 b, Vec3 c) {
    return new Matrix3(a.x, b.x, c.x, a.y, b.y, c.y, a.z, b.z, c.z);
  }

  public static Matrix3 translate(double x, double y) {
    return new Matrix3(1, 0, x, 0, 1, y, 0, 0, 1);
  }

  public static Matrix3 translate(Vec2 v) {
    return translate(v.x, v.y);
  }

  public static Matrix3 scale(double x, double y) {
    return new Matrix3(x, 0, 0, 0, y, 0, 0, 0, 1);
  }

  public static Matrix3 scale(Vec2 v) {
    return scale(v.x, v.y);
  }

  public static Matrix3 rotate(double radians) {
    double c = Math.cos(radians);
    double s = Math.sin(radians);
    return new Matrix3(c, -s, 0, s, c, 0, 0, 0, 1);
  }

  public Matrix3 mul(double k) {
    return new Matrix3(m00 * k, m01 * k, m02 * k, m10 * k, m11 * k, m12 * k, m20 * k, m21 * k, m22 * k);
  }

  public Matrix3 mul(Matrix3 b) {
    return new Matrix3(
            (m00 * b.m00) + (m01 * b.m10) + (m02 * b.m20),
            (m00 * b.m01) + (m01 * b.m11) + (m02 * b.m21),
            (m00 * b.m02) + (m01 * b.m12) + (m02 * b.m22),
            (m10 * b.m00) + (m11 * b.m10) + (m12 * b.m20),
            (m10 * b.m01) + (m11 * b.m11) + (m12 * b.m21),
            (m10 * b.m02) + (m11 * b.m12) + (m12 * b.m22),
            (m20 * b.m00) + (m21 * b.m10) + (m22 * b.m20),
            (m20 * b.m01) + (m21 * b.m11) + (m22 * b.m21),
            (m20 * b.m02) + (m21 * b.m12) + (m22 * b.m22));
  }

  public Matrix3 add(Matrix3 b) {
    return new Matrix3(
            m00 + b.m00,
            m01 + b.m01,
            m02 + b.m02,
            m10 + b.m10,
            m11 + b.m11,
            m12 + b.m12,
            m20 + b.m20,
            m21 + b.m21,
            m22 + b.m22);
  }

  public Matrix3 transpose() {
    return new Matrix3(m00, m10, m20, m01, m11, m21, m02, m12, m22);
  }

  public Vec2 transform(Vec2 v) {
    return new Vec2((v.x * m00) + (v.y * m01) + m02, (v.x * m10) + (v.y * m11) + m12);
  }

}
