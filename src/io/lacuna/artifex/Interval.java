package io.lacuna.artifex;

import java.awt.geom.Rectangle2D;
import java.util.function.DoublePredicate;

/**
 * @author ztellman
 */
@SuppressWarnings("unchecked")
public abstract class Interval<T extends Vec<T>, U extends Interval<T, U>> {

  public static Interval2 from(Rectangle2D rect) {
    return new Interval2(new Vec2(rect.getMinX(), rect.getMinY()), new Vec2(rect.getMaxX(), rect.getMaxY()));
  }

  public static Interval2 from(Vec2 a, Vec2 b) {
    return new Interval2(a, b);
  }

  private static final DoublePredicate POSITIVE = d -> d >= 0;

  protected final T lower, upper;
  protected final boolean isEmpty;

  Interval(T lower, T upper, boolean isEmpty) {
    this.lower = lower;
    this.upper = upper;
    this.isEmpty = isEmpty;
  }

  protected abstract U construct(T lower, T upper);

  protected abstract U empty();

  public double distanceSquared(T point) {
    T l = lower.sub(point);
    T u = point.sub(upper);
    return u.zip(l, (a, b) -> Math.max(0, Math.max(a, b))).lengthSquared();
  }

  public double distance(T point) {
    return Math.sqrt(distanceSquared(point));
  }

  public U union(U b) {
    if (isEmpty) {
      return b;
    } else if (b.isEmpty) {
      return (U) this;
    }
    return construct(lower.zip(b.lower, Math::min), upper.zip(b.upper, Math::max));
  }

  public U union(T v) {
    if (isEmpty) {
      return construct(v, v);
    }
    return construct(lower.zip(v, Math::min), upper.zip(v, Math::max));
  }

  public U intersection(U b) {
    if (isEmpty || b.isEmpty || !intersects(b)) {
      return empty();
    }
    return construct(lower.zip(b.lower, Math::max), upper.zip(b.upper, Math::min));
  }

  public boolean intersects(U b) {
    if (isEmpty || b.isEmpty) {
      return false;
    }
    return b.upper.sub(lower).every(POSITIVE) && upper.sub(b.lower).every(POSITIVE);
  }

  public boolean contains(T v) {
    return v.sub(lower).every(POSITIVE) && upper.sub(v).every(POSITIVE);
  }

  public T size() {
    return upper.sub(lower);
  }

  public T lower() {
    return lower;
  }

  public T upper() {
    return upper;
  }

  public T lerp(double t) {
    return lower.add(size().mul(t));
  }

  public T lerp(T v) {
    return lower.add(size().mul(v));
  }

  public U subInterval(double lower, double upper) {
    return construct(lerp(lower), lerp(upper));
  }

  public U subInterval(T lower, T upper) {
    return construct(lerp(lower), lerp(upper));
  }

  public U expand(double t) {
    if (isEmpty) {
      return (U) this;
    }

    T nLower = lower.map(n -> n - t);
    T nUpper = upper.map(n -> n + t);
    if (nLower.sub(nUpper).any(POSITIVE)) {
      return empty();
    } else {
      return construct(nLower, nUpper);
    }
  }

  public U expand(T v) {
    if (isEmpty) {
      return (U) this;
    }

    T nLower = lower.sub(v);
    T nUpper = upper.add(v);
    if (nLower.sub(nUpper).any(POSITIVE)) {
      return empty();
    } else {
      return construct(nLower, nUpper);
    }
  }

  @Override
  public int hashCode() {
    if (isEmpty) {
      return 0;
    }
    return (31 * lower.hashCode()) ^ upper.hashCode();
  }

  public static <T extends Vec<T>, U extends Interval<T, U>> boolean equals(Interval<T, U> a, Interval<T, U> b, double epsilon) {
    return Vec.equals(a.lower, b.lower, epsilon) && Vec.equals(a.upper, b.upper, epsilon);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Interval) {
      Interval b = (Interval) obj;
      if (isEmpty) {
        return b.isEmpty;
      }
      return lower.equals(b.lower) && upper.equals(b.upper);
    }
    return false;
  }

  @Override
  public String toString() {
    return "[" + lower + ", " + upper + "]";
  }
}
