package io.lacuna.artifex;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ztellman
 */
public class Subdivision2<T> {

  private static class Face<T> {

    public final T value;
    public final HalfEdge outer;
    public final HalfEdge[] inner;

    public Face(T value, HalfEdge outer) {
      this(value, outer, new HalfEdge[0]);
    }

    public Face(T value, HalfEdge outer, HalfEdge[] inner) {
      this.value = value;
      this.outer = outer;
      this.inner = inner;
    }
  }

  private static class HalfEdge {
    public final Face face;
    public final Curve2 curve;
    public final HalfEdge twin, prev, next;

    public HalfEdge(Face face, Curve2 curve, HalfEdge twin, HalfEdge prev, HalfEdge next) {
      this.face = face;
      this.curve = curve;
      this.twin = twin;
      this.prev = prev;
      this.next = next;
    }

    @Override
    public int hashCode() {
      return curve.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      } else if (obj instanceof HalfEdge) {
        HalfEdge h = (HalfEdge) obj;
        return curve.equals(h.curve);
      } else {
        return false;
      }
    }
  }

  private final T unbounded;
  private final Map<Face<T>, HalfEdge> faces = new HashMap<>();

  public Subdivision2(T unbounded) {
    this.unbounded = unbounded;
    faces.put(new Face<T>(unbounded, null), null);
  }





}
