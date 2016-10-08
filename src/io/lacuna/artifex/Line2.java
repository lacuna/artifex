package io.lacuna.artifex;

import io.lacuna.artifex.utils.Hash;

/**
 * @author ztellman
 */
public class Line2 {
  public final double slope, intercept;

  public Line2(double slope, double intercept) {
    this.slope = slope;
    this.intercept = intercept;
  }

  public double y(double x) {
    return intercept + (x * slope);
  }

  public Vec2 closestPoint(Vec2 v) {
    double d = 1 + (slope * slope);
    return new Vec2(
            (v.x + (slope * v.y) + (slope * intercept)) / d,
            ((slope * (v.x + (slope * v.y))) + intercept) / d);
  }

  public double distance(Vec2 v) {
    return ((slope * v.x) - v.y + intercept) / Math.sqrt(1 + (slope * slope));
  }

  public static boolean equals(Line2 a, Line2 b, double epsilon) {
    return Math.abs(a.slope - b.slope) <= epsilon
            && Math.abs(a.intercept - b.intercept) <= epsilon;
  }

  @Override
  public int hashCode() {
    return Hash.hash(slope, intercept);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Line2) {
      Line2 l = (Line2) obj;
      return l.slope == slope && l.intercept == intercept;
    }
    return false;
  }

  @Override
  public String toString() {
    return String.format("[slope=%f, intercept=%f]", slope, intercept);
  }
}
