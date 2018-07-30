package io.lacuna.artifex.utils;

import static io.lacuna.artifex.utils.Scalars.EPSILON;
import static java.lang.Math.*;

/**
 * @author ztellman
 */
public class Equations {

  // Adapted from https://github.com/Chlumsky/msdfgen/blob/master/core/equation-solver.cpp
  public static double[] solveLinear(double a, double b, double epsilon) {
    if (abs(a) < epsilon) {
      return new double[0];
    } else {
      return new double[]{-b / a};
    }
  }

  public static double[] solveQuadratic(double a, double b, double c, double epsilon) {
    double d = (b * b) - (4 * a * c);
    double aa = a * 2;

    if (Math.abs(a) < epsilon) {
      return solveLinear(b, c, epsilon);
    } else if (d > 0) {
      d = sqrt(d);
      return new double[]{(-b + d) / aa, (-b - d) / aa};
    } else if (d + epsilon > 0) {
      return new double[]{-b / aa};
    } else {
      return new double[0];
    }
  }

  private static double[] solveCubicNormed(double a, double b, double c, double epsilon) {

    double d0 = ((3 * b) - (a * a)) / 3;
    double d1 = ((2 * a * a * a) - (9 * a * b) + (27 * c)) / 27;
    double d2 = d1 / 2;
    double offset = a / 3;

    double disc = (d1 * d1 / 4) + (d0 * d0 * d0 / 27);
    if (abs(disc) < epsilon) {
      disc = 0;
    }

    double[] result;
    if (disc > 0) {
      double e = sqrt(disc);
      double n1 = e - d2;
      double n2 = -e - d2;
      double root =
        (n1 >= 0 ? pow(n1, 1 / 3.0) : -pow(-n1, 1 / 3.0))
          + (n2 >= 0 ? pow(n2, 1 / 3.0) : -pow(-n2, 1 / 3.0));

      result = new double[]{root - offset};

    } else if (disc < 0) {

      double dist = sqrt(-d0 / 3);
      double angle = atan2(sqrt(-disc), -d2) / 3;
      double cos = cos(angle);
      double sin = sin(angle);
      double sqrt3 = sqrt(3);

      result = new double[]{
        (2 * dist * cos) - offset,
        -dist * (cos + (sqrt3 * sin)) - offset,
        -dist * (cos - (sqrt3 * sin)) - offset
      };
    } else {

      double n = d1 >= 0 ? -pow(d2, 1 / 3.0) : pow(-d2, 1 / 3.0);

      result = new double[]{
        (2 * n) - offset,
        -n - offset
      };
    }

    return result;


    /*double a2 = a * a;
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

      //System.out.println(res0 + " " + res1 + " " + res2);

      return abs(res2) < epsilon
        ? new double[]{res0, res1}
        : new double[]{res0};
    }*/
  }

  public static double[] solveCubic(double a, double b, double c, double d, double epsilon) {
    if (Math.abs(a) < epsilon) {
      return solveQuadratic(b, c, d, epsilon);
    } else {
      return solveCubicNormed(b / a, c / a, d / a, epsilon);
    }
  }
}
