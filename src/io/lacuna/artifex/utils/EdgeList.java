package io.lacuna.artifex.utils;

import io.lacuna.artifex.*;
import io.lacuna.bifurcan.*;

import java.util.Iterator;
import java.util.function.IntPredicate;

import static io.lacuna.artifex.Vec2.angleBetween;
import static io.lacuna.artifex.utils.Scalars.EPSILON;
import static java.lang.Math.abs;

/**
 * An implementation of a doubly-connected edge list.  Since this is an inherently mutable data structure, it is
 * not exposed at the top-level, and instead only used to transform between immutable geometric representations.
 *
 * @author ztellman
 */
public class EdgeList {

  private enum Orientation {
    LEFT,
    RIGHT,
    COLLINEAR
  }

  private enum Visibility {
    INSIDE,
    OUTSIDE,
    COLLINEAR
  }

  private static Orientation orientation(Curve2 a, Curve2 b) {
    Vec2 origin = a.start();

    double t = 1;
    while (t >= 0.5) {
      Vec2 pa = a.position(t);
      Vec2 pb = b.position(t);

      if (pa.sub(origin).lengthSquared() < pb.sub(origin).lengthSquared()) {
        pb = b.position(Scalars.clamp(0, b.nearestPoint(pa), 1));
      } else {
        pa = a.position(Scalars.clamp(0, a.nearestPoint(pb), 1));
      }

      Vec2 da = pa.sub(origin).norm();
      Vec2 db = pb.sub(origin).norm();
      double cross = Vec2.cross(da, db);

      if (cross < -EPSILON) {
        return Orientation.LEFT;
      } else if (cross > EPSILON) {
        return Orientation.RIGHT;
      }

      t /= 2;
    }

    return Orientation.COLLINEAR;
  }

  public static class HalfEdge {
    public HalfEdge prev, next, twin;
    public int flag;

    public final Curve2 curve;

    private HalfEdge(Curve2 curve, HalfEdge twin, int flag) {
      this.curve = curve;
      this.twin = twin;
      this.flag = flag;
    }

    public HalfEdge(Curve2 curve, int left, int right) {
      this.curve = curve;
      this.flag = left;
      this.twin = new HalfEdge(curve.reverse(), this, right);
    }

    public Visibility visible(Curve2 c) {
      // the precision of angleBetween is a little coarse, so give ourselves some room
      double epsilon = 1e-6;

      double t0 = -angleBetween(prev.twin.curve.direction(0), c.direction(0));
      double t1 = interiorAngle();

      if (t0 < t1 && abs(t0) < epsilon) {
        switch (orientation(prev.twin.curve, c)) {
          case LEFT:
            return Visibility.INSIDE;
          default:
            return Visibility.OUTSIDE;
        }
      } else if (abs(t1 - t0) < epsilon) {
        switch (orientation(curve, c)) {
          case RIGHT:
            return t0 == 0 ? Visibility.OUTSIDE : Visibility.INSIDE;
          case COLLINEAR:
            return Visibility.COLLINEAR;
          default:
            return Visibility.OUTSIDE;
        }
      }

      return t0 < t1 ? Visibility.INSIDE : Visibility.OUTSIDE;
    }

    public double interiorAngle() {
      double theta = -angleBetween(prev.twin.curve.direction(0), curve.direction(0));
      if (Scalars.equals(theta, 0, 1e-6) || Scalars.equals(theta, Math.PI * 2, 1e-6)) {
        theta = orientation(prev.twin.curve, curve) == Orientation.LEFT ? 0 : Math.PI * 2;
      }
      return theta;
    }

    public void link(HalfEdge e) {
      assert end().equals(e.start());

      this.next = e;
      e.prev = this;
    }

    public Vec2 start() {
      return curve.start();
    }

    public Vec2 end() {
      return curve.end();
    }

    public Iterable<HalfEdge> face() {
      return () -> new Iterator<HalfEdge>() {
        HalfEdge curr = HalfEdge.this;
        boolean started = false;

        @Override
        public boolean hasNext() {
          return !started || curr != HalfEdge.this;
        }

        @Override
        public HalfEdge next() {
          HalfEdge result = curr;
          started = true;
          curr = curr.next;
          return result;
        }
      };
    }

    @Override
    public String toString() {
      return curve.toString() + " " + flag + " " + twin.flag;
    }
  }

  ///

  private final LinearMap<Vec2, HalfEdge> vertices = new LinearMap<>();

  // a potentially redundant list of edges on different faces, which will be cleaned up if we ever iterate over them
  private final LinearSet<HalfEdge> pseudoFaces = new LinearSet<>();
  private boolean invalidated = false;

  public EdgeList() {
  }

  public static EdgeList from(Iterable<Ring2> rings, int inside, int outside) {
    EdgeList result = new EdgeList();
    for (Ring2 r : rings) {
      for (Curve2 c : r.curves) {
        result.add(c, inside, outside);
      }
    }

    return result;
  }

  /// accessors

  public ISet<Vec2> vertices() {
    return vertices.keys();
  }

  public HalfEdge edge(Vec2 v, int flag) {
    HalfEdge init = vertices.get(v).get();

    HalfEdge curr = init;
    while (curr.flag != flag) {
      curr = curr.twin.next;
      if (curr == init) {
        return null;
      }
    }

    return curr;
  }

  public IList<Ring2> boundaries(IntPredicate flagPredicate) {
    IList<Ring2> result = new LinearList<>();
    for (HalfEdge e : faces()) {
      if (flagPredicate.test(e.flag)) {
        IList<Curve2> cs = new LinearList<>();
        for (HalfEdge edge : e.face()) {
          cs.addFirst(edge.twin.curve);
        }
        result.addLast(new Ring2(cs));
      }
    }
    return result;
  }

  public IMap<Ring2, Integer> rings() {
    return faces().stream().collect(Maps.linearCollector(this::ring, e -> e.flag));
  }

  public IList<HalfEdge> faces() {
    if (invalidated) {
      for (int i = 0; i < pseudoFaces.size(); i++) {
        HalfEdge e = pseudoFaces.nth(i);
        int flag = e.flag;

        int j = 0;
        HalfEdge curr = e.next;
        while (curr != e) {
          if (j++ > 1_000) {
            throw new IllegalStateException(e.toString());
          }
          curr.flag = flag = flag | curr.flag;
          pseudoFaces.remove(curr);
          curr = curr.next;
        }

        for (HalfEdge edge : e.face()) {
          if (edge.flag == flag) {
            break;
          }
          edge.flag = flag;
        }
      }
      invalidated = false;
    }

    return LinearList.from(pseudoFaces.elements());
  }

  public Ring2 ring(HalfEdge e) {
    IList<Curve2> cs = new LinearList<>();
    e.face().forEach(edge -> cs.addLast(edge.curve));
    return new Ring2(cs);
  }

  /// modifiers

  private boolean checkForNearMiss(Vec2 v) {
    Vec2 match = vertices.keys().stream().filter(u -> Vec.equals(u, v, EPSILON)).findAny().orElse(null);
    if (match != null) {
      System.out.println(v + " " + match + " " + v.sub(match).length());
      return true;
    }
    return false;
  }

  public HalfEdge add(Vec2 a, Vec2 b, int left, int right) {
    return add(Line2.from(a, b), left, right);
  }

  private void add(HalfEdge e) {
    Curve2 c = e.curve;
    Vec2 start = c.start();

    if (vertices.contains(start)) {
      HalfEdge src = vertices.get(start).get();

      // we're connecting from a dangling edge
      if (src.prev == null) {
        e.twin.link(src);
        src.twin.link(e);

        // split the vertex appropriately
      } else {
        HalfEdge curr = src;
        for (; ; ) {
          switch (curr.visible(c)) {
            case INSIDE:
              curr.prev.link(e);
              e.twin.link(curr);
              registerFace(e);
              return;

            case COLLINEAR:
              assert curr.end().equals(c.end());
              curr.flag |= e.flag;
              return;

            case OUTSIDE:
              curr = curr.twin.next;
          }

          assert curr != src;
        }
      }
    } else {
      //assert !checkForNearMiss(start);
      vertices.put(start, e);
      registerFace(e);
    }

  }

  public HalfEdge add(Curve2 c, int left, int right) {
    HalfEdge e = new HalfEdge(c, left, right);
    add(e);
    add(e.twin);
    return e;
  }

  public void removeFaces(IntPredicate toRemove, int outside) {
    for (HalfEdge e : faces()) {
      if (e.flag != outside && toRemove.test(e.flag)) {
        removeFace(e, outside);
      }
    }
  }

  public void removeFace(HalfEdge e, int outside) {
    for (HalfEdge edge : LinearList.from(e.face())) {
      if (edge.twin.flag == outside) {
        remove(edge);
      } else {
        edge.flag = outside;
      }
    }
  }

  public void remove(HalfEdge e) {

    HalfEdge prev = e.prev;
    if (prev != null) {
      vertices.put(e.start(), prev.twin);

      // singly-linked
      if (e.twin.next == prev.twin) {
        prev.next = null;
        prev.twin.prev = null;

        // multi-linked
      } else {
        prev.link(e.twin.next);
        invalidated = true;
      }
    } else {
      vertices.remove(e.start());
    }

    HalfEdge next = e.next;
    if (next != null) {
      vertices.put(e.end(), next);

      // singly-linked
      if (e.twin.prev == next.twin) {
        next.prev = null;
        next.twin.next = null;

        // multi-linked
      } else {
        e.twin.prev.link(next);
        invalidated = true;
      }
    } else {
      vertices.remove(e.end());
    }

    pseudoFaces.remove(e).remove(e.twin);
    if (prev != null) {
      registerFace(prev);
    }
    if (next != null) {
      registerFace(next);
    }
  }

  public HalfEdge split(HalfEdge e, double t) {
    if (t == 0) {
      return e;
    } else if (t == 1) {
      return e.next;
    }

    Curve2[] cs = e.curve.split(t);

    remove(e);
    add(cs[0], e.flag, e.twin.flag);
    return add(cs[1], e.flag, e.twin.flag);
  }

  /// helpers

  private void registerFace(HalfEdge e) {

    pseudoFaces.add(e).add(e.twin);
    invalidated = true;

  }

  ///


  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();
    for (HalfEdge e : faces()) {
      Ring2 r = ring(e);
      b.append("RING: " + e.flag + "\n");
      for (Curve2 c : r.curves) {
        b.append("  " + c + "\n");
      }
      b.append("\n");
    }

    return b.toString();
  }
}
