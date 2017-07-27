package io.lacuna.artifex;

import java.awt.geom.Rectangle2D;
import java.util.function.DoublePredicate;

/**
 * @author ztellman
 */
@SuppressWarnings("unchecked")
public abstract class Box<T extends Vec<T>, U extends Box<T, U>> {

  public static Box2 from(Rectangle2D rect) {
    return new Box2(new Vec2(rect.getMinX(), rect.getMinY()), new Vec2(rect.getMaxX(), rect.getMaxY()));
  }

  public static Box2 box(Vec2 a, Vec2 b) {
    return new Box2(a, b);
  }

  public static Box3 box(Vec3 a, Vec3 b) {
    return new Box3(a, b);
  }

  public static Box4 box(Vec4 a, Vec4 b) {
    return new Box4(a, b);
  }

  private static final DoublePredicate POSITIVE = d -> d > 0;
  private static final DoublePredicate NOT_NEGATIVE = d -> d >= 0;

  public abstract T lower();

  public abstract T upper();

  public abstract boolean isEmpty();

  protected abstract U construct(T a, T b);

  protected abstract U empty();

  public double distanceSquared(T point) {
    T l = lower().sub(point);
    T u = point.sub(upper());
    return u.zip(l, (a, b) -> Math.max(0, Math.max(a, b))).lengthSquared();
  }

  public double distance(T point) {
    return Math.sqrt(distanceSquared(point));
  }

  public U union(U b) {
    if (isEmpty()) {
      return b;
    } else if (b.isEmpty()) {
      return (U) this;
    }
    return construct(lower().zip(b.lower(), Math::min), upper().zip(b.upper(), Math::max));
  }

  public U union(T v) {
    if (isEmpty()) {
      return construct(v, v);
    }
    return construct(lower().zip(v, Math::min), upper().zip(v, Math::max));
  }

  public U intersection(U b) {
    if (isEmpty() || b.isEmpty() || !intersects(b)) {
      return empty();
    }
    return construct(lower().zip(b.lower(), Math::max), upper().zip(b.upper(), Math::min));
  }

  public boolean intersects(U b) {
    if (isEmpty() || b.isEmpty()) {
      return false;
    }
    return b.upper().sub(lower()).every(POSITIVE) && upper().sub(b.lower()).every(POSITIVE);
  }

  public boolean contains(T v) {
    return v.sub(lower()).every(NOT_NEGATIVE) && upper().sub(v).every(NOT_NEGATIVE);
  }

  public T size() {
    return upper().sub(lower());
  }

  public T lerp(double t) {
    return lower().add(size().mul(t));
  }

  public T lerp(T v) {
    return lower().add(size().mul(v));
  }

  public U translate(T v) {
    return construct(lower().add(v), upper().add(v));
  }

  public U scale(T v) {
    return construct(lower().mul(v), upper().mul(v));
  }

  public U expand(double t) {
    if (isEmpty()) {
      return (U) this;
    }

    T nLower = lower().map(n -> n - t);
    T nUpper = upper().map(n -> n + t);
    if (nLower.sub(nUpper).any(NOT_NEGATIVE)) {
      return empty();
    } else {
      return construct(nLower, nUpper);
    }
  }

  public U expand(T v) {
    if (isEmpty()) {
      return (U) this;
    }

    T nLower = lower().sub(v);
    T nUpper = upper().add(v);
    if (nLower.sub(nUpper).any(NOT_NEGATIVE)) {
      return empty();
    } else {
      return construct(nLower, nUpper);
    }
  }

  @Override
  public int hashCode() {
    if (isEmpty()) {
      return 0;
    }
    return (31 * lower().hashCode()) ^ upper().hashCode();
  }

  public static <T extends Vec<T>, U extends Box<T, U>> boolean equals(Box<T, U> a, Box<T, U> b, double epsilon) {
    return Vec.equals(a.lower(), b.lower(), epsilon) && Vec.equals(a.upper(), b.upper(), epsilon);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Box) {
      Box b = (Box) obj;
      if (isEmpty()) {
        return b.isEmpty();
      }
      return lower().equals(b.lower()) && upper().equals(b.upper());
    }
    return false;
  }

  @Override
  public String toString() {
    return "[" + lower() + ", " + upper() + "]";
  }
}
