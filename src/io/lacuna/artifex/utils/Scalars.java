package io.lacuna.artifex.utils;

/**
 * @author ztellman
 */
public class Scalars {

  public static boolean equals(double a, double b, double epsilon) {
    return Math.abs(a - b) <= epsilon;
  }

  public static double lerp(double a, double b, double t) {
    return a + ((b - a) * t);
  }

  public static boolean inside(double n, double min, double max) {
    return min <= n && n <= max;
  }
}
