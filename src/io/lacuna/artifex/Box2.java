package io.lacuna.artifex;

import io.lacuna.artifex.utils.Hash;

import java.awt.geom.Rectangle2D;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * @author ztellman
 */
public class Box2 {

  public static final Box2 EMPTY = new Box2();

  public final double left, right, top, bottom;
  private final boolean isEmpty;

  private Box2() {
    left = right = top = bottom = Double.NaN;
    isEmpty = true;
  }

  public Box2(double left, double right, double bottom, double top) {
    this.left = left;
    this.right = right;
    this.top = top;
    this.bottom = bottom;
    this.isEmpty = false;
  }

  public static Box2 from(Rectangle2D rect) {
    return new Box2(rect.getMinX(), rect.getMaxX(), rect.getMinY(), rect.getMaxY());
  }

  public static Box2 from(Vec2 a, Vec2 b) {
    double left, right, top, bottom;
    if (a.x < b.x) {
      left = a.x;
      right = b.x;
    } else {
      right = a.x;
      left = b.x;
    }

    if (a.y < b.y) {
      bottom = a.y;
      top = b.y;
    } else {
      top = a.y;
      bottom = b.y;
    }

    return new Box2(left, right, bottom, top);
  }

  public Vec2 size() {
    return new Vec2(right - left, top - bottom);
  }

  public Vec2 centroid() {
    return new Vec2((left + right) / 2, (bottom + top) + 2);
  }

  public boolean inside(Vec2 v) {
    if (isEmpty) {
      return false;
    }
    return left <= v.x && v.x <= right && bottom <= v.y && v.y <= top;
  }

  public Vec2 lerp(Vec2 t) {
    return new Vec2(left + (right - left) * t.x, bottom + (top - bottom) * t.y);
  }

  public Box2 union(Vec2 v) {
    if (isEmpty) {
      return Box2.from(v, v);
    } else if (inside(v)) {
      return this;
    }

    return new Box2(min(left, v.x), max(right, v.x), min(bottom, v.y), max(top, v.y));
  }

  public Box2 union(Box2 b) {
    if (isEmpty) {
      return b;
    } else if (b.isEmpty) {
      return this;
    }

    return new Box2(min(left, b.left), max(right, b.right), min(bottom, b.bottom), max(top, b.top));
  }

  @Override
  public int hashCode() {
    if (isEmpty) {
      return 0;
    }
    return Hash.hash(left, right, bottom, top);
  }

  public static boolean equals(Box2 a, Box2 b, double epsilon) {
    if (a.isEmpty) {
      return b.isEmpty;
    }

    return abs(a.left - b.left) < epsilon
            && abs(a.right - b.right) < epsilon
            && abs(a.bottom - b.bottom) < epsilon
            && abs(a.top - b.top) < epsilon;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Box2)) {
      return false;
    }
    Box2 b = (Box2) obj;

    if (isEmpty) {
      return b.isEmpty;
    }

    return left == b.left && right == b.right && bottom == b.bottom && top == b.top;
  }

  @Override
  public String toString() {
    return "[" + left + "," + bottom + "],[" + right + "," + top + "]";
  }
}
