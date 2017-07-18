package io.lacuna.artifex;

import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;

/**
 *
 */
public class Matrix4 {
  public static final Matrix4 IDENTITY = new Matrix4(
          1, 0, 0, 0,
          0, 1, 0, 0,
          0, 0, 1, 0,
          0, 0, 0, 1);

  private final double[] elements;

  private Matrix4(double m00, double m01, double m02, double m03,
                  double m10, double m11, double m12, double m13,
                  double m20, double m21, double m22, double m23,
                  double m30, double m31, double m32, double m33) {

    elements = new double[]{
            m00, m01, m02, m03,
            m10, m11, m12, m13,
            m20, m21, m22, m23,
            m30, m31, m32, m33};
  }

  private Matrix4(double[] elements) {
    this.elements = elements;
  }


  public static Matrix4 translate(double x, double y, double z) {
    return new Matrix4(
            1, 0, 0, x,
            0, 1, 0, y,
            0, 0, 1, z,
            0, 0, 0, 1);
  }

  public static Matrix4 translate(Vec3 v) {
    return translate(v.x, v.y, v.z);
  }

  public static Matrix4 scale(double x, double y, double z) {
    return new Matrix4(
            x, 0, 0, 0,
            0, y, 0, 0,
            0, 0, z, 0,
            0, 0, 0, 1);
  }

  public static Matrix4 scale(Vec3 v) {
    return scale(v.x, v.y, v.z);
  }

  public static Matrix4 scale(double k) {
    return scale(k, k, k);
  }

  public Matrix4 mul(Matrix4... matrices) {
    Matrix4 m = matrices[0];
    for (int i = 1; i < matrices.length; i++) {
      m = m.mul(matrices[i]);
    }
    return m;
  }

  public Matrix4 mul(double k) {
    double[] es = new double[16];
    for (int i = 0; i < 16; i++) {
      es[i] = elements[i] * k;
    }
    return new Matrix4(es);
  }

  public double get(int row, int column) {
    return elements[(row << 2) + column];
  }

  public Matrix4 mul(Matrix4 b) {
    double[] es = new double[16];
    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        double n = 0;
        for (int k = 0; k < 4; k++) {
          n += get(k, j) * b.get(i, k);
        }
        es[(i << 2) + j] = n;
      }
    }
    return new Matrix4(es);
  }

  public Matrix4 add(Matrix4 b) {
    double[] es = new double[16];
    for (int i = 0; i < 16; i++) {
      es[i] = elements[i] + b.elements[i];
    }
    return new Matrix4(es);
  }

  public Matrix4 transpose() {
    return new Matrix4(
            elements[0], elements[4], elements[8], elements[12],
            elements[1], elements[5], elements[9], elements[13],
            elements[2], elements[6], elements[10], elements[14],
            elements[3], elements[7], elements[11], elements[15]);
  }

  public Vec3 transform(Vec3 v) {
    return new Vec3(
            (v.x * elements[0]) + (v.y * elements[1]) + (v.z * elements[2]) + elements[3],
            (v.x * elements[4]) + (v.y * elements[5]) + (v.z * elements[6]) + elements[7],
            (v.x * elements[8]) + (v.y * elements[9]) + (v.z * elements[10]) + elements[11]);
  }

  public PrimitiveIterator.OfDouble rowMajor() {
    return new PrimitiveIterator.OfDouble() {
      int idx = 0;

      public boolean hasNext() {
        return idx < 16;
      }

      public double nextDouble() {
        if (idx < 9) {
          return elements[idx++];
        } else {
          throw new NoSuchElementException();
        }
      }
    };
  }

  public PrimitiveIterator.OfDouble columnMajor() {
    return transpose().rowMajor();
  }


}
