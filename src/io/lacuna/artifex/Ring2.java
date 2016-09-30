package io.lacuna.artifex;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author ztellman
 */
public class Ring2 {

  private final double[] coords;
  private byte isConvex;
  private int hash = -1;

  public Ring2(Collection<Vec2> vertices) {
    coords = new double[vertices.size() << 1];

    int idx = 0;
    for (Vec2 v : vertices) {
      coords[idx++] = v.x;
      coords[idx++] = v.y;
    }
  }

  /**
   * @return true if the polygon is convex, false otherwise
   */
  public boolean isConvex() {
    if (isConvex != 0) {
      return isConvex == 1;
    }

    Iterator<Vec2> it = vertices();
    Vec2 a;
    Vec2 b = it.next();
    Vec2 c = it.next();

    double z = 0.0;

    while (it.hasNext()) {
      a = b;
      b = c;
      c = it.next();

      double z2 = Math.signum(Vec2.cross(b.sub(a), c.sub(b)));

      if (z == 0.0) {
        z = z2;
      } else if (z2 != 0.0 && z2 != z) {
        isConvex = 2;
        return false;
      }
    }

    isConvex = 1;
    return true;
  }

  /**
   * @return an iterator over all the vertices, with the first vertex repeated at the end
   */
  public Iterator<Vec2> vertices() {
    return new Iterator<Vec2>() {

      int idx = 0;

      @Override
      public boolean hasNext() {
        return idx < coords.length;
      }

      @Override
      public Vec2 next() {
        return new Vec2(coords[idx++], coords[idx % coords.length]);
      }
    };
  }

  /**
   * @return an iterator over all the edges
   */
  public Iterator<Segment2> edges() {
    Iterator<Vec2> vertices = vertices();

    return new Iterator<Segment2>() {

      Vec2 v = vertices.next();

      @Override
      public boolean hasNext() {
        return vertices.hasNext();
      }

      @Override
      public Segment2 next() {
        Segment2 edge = new Segment2(v, vertices.next());
        v = edge.b;
        return edge;
      }
    };
  }

  @Override
  public int hashCode() {
    if (hash != -1) {
      return hash;
    }

    long h = 1L;
    for (int i = 0; i < coords.length; i++) {
      h = (31 * h) + Double.doubleToLongBits(coords[i]);
    }

    hash = (int) (h ^ (h >> 32));
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Ring2)) {
      return false;
    }

    Ring2 r = (Ring2) obj;
    if (coords.length != r.coords.length) {
      return false;
    }

    for (int i = 0; i < coords.length; i++) {
      if (coords[i] != r.coords[i]) {
        return false;
      }
    }

    return true;
  }
}
