package io.lacuna.artifex.utils;

/**
 * @author ztellman
 */
public class Scalars {

  public static final double EPSILON = 1e-14;

  public static boolean equals(double a, double b, double epsilon) {
    return Math.abs(a - b) <= epsilon;
  }

  public static double lerp(double a, double b, double t) {
    return a + ((b - a) * t);
  }

  public static boolean inside(double min, double n, double max) {
    return min < n && n < max;
  }

  public static double clamp(double min, double n, double max) {
    if (n <= min) {
      return min;
    } else if (n >= max) {
      return max;
    } else {
      return n;
    }
  }

  public static double max(double a, double b) {
    return a < b ? b : a;
  }

  public static double max(double a, double b, double c) {
    return max(a, max(b, c));
  }
}
