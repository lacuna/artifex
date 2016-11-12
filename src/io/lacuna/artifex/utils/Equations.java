package io.lacuna.artifex.utils;

import static java.lang.Math.*;

/**
 * @author ztellman
 */
public class Equations {
  private static final double EPSILON = 1e-14;

  // Adapted from https://github.com/Chlumsky/msdfgen/blob/master/core/equation-solver.cpp
  public static double[] solveLinear(double a, double b) {
    if (abs(a) < EPSILON) {
      return b == 0 ? null : new double[0];
    } else {
      return new double[]{-b / a};
    }
  }

  public static double[] solveQuadratic(double a, double b, double c) {
    double d = (b * b) - (4 * a * c);
    double aa = a * 2;

    if (Math.abs(a) < EPSILON) {
      return solveLinear(b, c);
    } else if (d > 0) {
      d = sqrt(d);
      return new double[]{(-b + d) / aa, (-b - d) / aa};
    } else if (d == 0) {
      return new double[]{-b / aa};
    } else {
      return new double[0];
    }
  }

  // adapted from http://http.developer.nvidia.com/Cg/acos.html
  public static double acos(double x) {
    double negate = x < 0 ? 1 : 0;
    x = abs(x);
    double ret = -0.0187293;
    ret *= x;
    ret += 0.0742610;
    ret *= x;
    ret -= 0.2121144;
    ret *= x;
    ret += 1.5707288;
    ret *= sqrt(1.0 - x);
    ret -= 2 * negate * ret;
    return (negate * PI) + ret;
  }

  private static double[] solveCubicNormed(double a, double b, double c) {
    double a2 = a * a;
    double q = (a2 - (3 * b)) / 9;
    double r = (a * ((2 * a2) - (9 * b)) + (27 * c)) / 54;
    double r2 = r * r;
    double q3 = q * q * q;

    if (r2 < q3) {
      double t = r / sqrt(q3);
      if (t < -1) t = -1;
      if (t > 1) t = 1;
      t = Math.acos(t);

      a /= 3;
      q = -2 * sqrt(q);

      return new double[]{
              (q * cos(t / 3)) - a,
              (q * cos((t + (2 * PI)) / 3)) - a,
              (q * cos((t - (2 * PI)) / 3)) - a};
    } else {
      double A = -pow(abs(r) + sqrt(r2 - q3), 1 / 3.0);
      if (r < 0) A = -A;
      double B = A == 0 ? 0 : q / A;
      a /= 3;

      double res0 = (A + B) - a;
      double res1 = -0.5 * (A + B) - a;
      double res2 = 0.5 * sqrt(3.0) * (A - B);

      return (abs(res2) < EPSILON) ? new double[]{res0, res1} : new double[]{res0};
    }
  }

  public static double[] solveCubic(double a, double b, double c, double d) {
    if (Math.abs(a) < EPSILON) {
      return solveQuadratic(b, c, d);
    } else {
      return solveCubicNormed(b/a, c/a, d/a);
    }
  }
}
