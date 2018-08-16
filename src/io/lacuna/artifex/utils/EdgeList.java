package io.lacuna.artifex.utils;

import io.lacuna.artifex.*;
import io.lacuna.artifex.utils.regions.Overlay;
import io.lacuna.bifurcan.*;

import java.util.Iterator;
import java.util.function.IntPredicate;

import static io.lacuna.artifex.Vec.vec;
import static io.lacuna.artifex.utils.Scalars.EPSILON;
import static io.lacuna.artifex.utils.Scalars.angleEquals;
import static io.lacuna.artifex.utils.Scalars.inside;

/**
 * An implementation of a doubly-connected edge list.  Since this is an inherently mutable data structure, it is
 * not exposed at the top-level, and instead only used to transform between immutable geometric representations.
 *
 * @author ztellman
 */
public class EdgeList {

  private static final double ANGLE_EPSILON = Math.PI / 8;

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

  public static double angleBetween(Curve2 a, Curve2 b) {
    return -Vec2.angleBetween(a.direction(0), b.direction(0));
  }

  public static Orientation orientation(Curve2 a, Curve2 b) {
    Vec2 origin = a.start();

    double t = 1;
    while (t >= 0.5) {
      Vec2 pa = a.position(t);
      Vec2 pb = b.position(t);

      if (pa.sub(origin).lengthSquared() < pb.sub(origin).lengthSquared()) {
        pb = b.position(Scalars.clamp(EPSILON, b.nearestPoint(pa), 1));
      } else {
        pa = a.position(Scalars.clamp(EPSILON, a.nearestPoint(pb), 1));
      }

      if (!Vec.equals(pa, pb, Overlay.SPATIAL_EPSILON)) {
        Vec2 da = pa.sub(origin).norm();
        Vec2 db = pb.sub(origin).norm();
        double cross = Vec2.cross(da, db);

        if (cross < -EPSILON) {
          return Orientation.LEFT;
        } else if (cross > EPSILON) {
          return Orientation.RIGHT;
        }
      }

      t -= 0.25;
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
      double t0 = angleBetween(prev.twin.curve, c);
      double t1 = angleBetween(c, curve);

      System.out.println(t0 + " " + t1);
      System.out.println(prev.twin.curve + "\n" + curve);

      if (angleEquals(t0, 0, ANGLE_EPSILON)) {
        System.out.println("a " + orientation(prev.twin.curve, c));
        switch (orientation(prev.twin.curve, c)) {
          case LEFT:
            t0 = 0;
            break;
          case RIGHT:
          case COLLINEAR:
            return Visibility.OUTSIDE;
        }
      }

      if (angleEquals(t1, 0, ANGLE_EPSILON)) {
        System.out.println("b " + orientation(c, curve));
        switch (orientation(c, curve)) {
          case LEFT:
            return Visibility.INSIDE;
          case COLLINEAR:
            return Visibility.COLLINEAR;
          case RIGHT:
            return Visibility.OUTSIDE;
        }
      }

      System.out.println("sums " + (t0 + t1) + " " + interiorAngle() + " " + orientation(prev.twin.curve, c) + " " + orientation(c, curve));

      return (t0 + t1) < (interiorAngle() + ANGLE_EPSILON) ? Visibility.INSIDE : Visibility.OUTSIDE;
    }

    public double interiorAngle() {
      double theta = angleBetween(prev.twin.curve, curve);
      if (angleEquals(theta, 0, ANGLE_EPSILON)) {
        theta = orientation(prev.twin.curve, curve) == Orientation.RIGHT ? Math.PI * 2 : 0;
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
          if (j++ > 1_000_000) {
            throw new IllegalStateException(e.toString());
          }
          curr.flag = flag = flag | curr.flag;
          pseudoFaces.remove(curr);
          if (curr.next == null) {
            System.out.println("NULL! " + curr);
          }
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

  public HalfEdge add(Vec2 a, Vec2 b, int left, int right) {
    return add(Line2.from(a, b), left, right);
  }

  private void insert(HalfEdge src, HalfEdge e) {
    src.prev.link(e);
    e.twin.link(src);
    registerFace(e);
  }

  private void overlay(HalfEdge a, HalfEdge b) {

    System.out.println("OVERLAY " + a + "\n" + b);

    if (!a.end().equals(b.end())) {
      Vec2[] is = Intersections.collinearIntersection(a.curve, b.curve);
      Vec2 in;
      if (is.length > 1) {
        in = is[1];
      } else {
        double la = a.end().sub(a.start()).length();
        double lb = b.end().sub(b.start()).length();
        in = vec(la / lb, lb / la);
      }
      in = in.clamp(EPSILON, 1);

      assert a.start().equals(b.start());
      assert in.x == 1 || in.y == 1;

      // b stretches beyond a
      if (in.x == 1 && in.y < 1) {
        Curve2[] cs = b.curve.split(in.y);
        add(cs[1].endpoints(a.end(), b.end()), b.flag, b.twin.flag);

        // a stretches beyond b
      } else if (in.y == 1 && in.x < 1) {

        Curve2[] cs = a.curve.split(in.x);
        replace(a,
          new HalfEdge(cs[0].endpoints(a.start(), b.end()), 0, 0),
          new HalfEdge(cs[1].endpoints(b.end(), a.end()), a.flag, a.twin.flag));
      }
    }

    //a.flag |= b.flag;
    //a.twin.flag |= b.twin.flag;
  }

  private void add(HalfEdge e) {
    System.out.println("\nADDING " + e + " " + vertices.contains(e.start()) + " " + vertices.contains(e.end()));
    Vec2 start = e.start();
    Vec2 end = e.end();

    // add source vertex
    HalfEdge src = vertices.get(start, null);
    if (src == null) {
      vertices.put(start, e);
      registerFace(e);

    } else if (src.prev == null) {
      e.twin.link(src);
      src.twin.link(e);

    } else {
      HalfEdge curr = src;
      for (; ; ) {
        Visibility v = curr.visible(e.curve);
        if (v == Visibility.INSIDE) {
          insert(curr, e);
          break;

        } else if (v == Visibility.COLLINEAR) {
          overlay(curr, e);
          return;

        } else {
          curr = curr.twin.next;
        }

        assert curr != src;
      }
    }

    // add destination vertex
    HalfEdge dst = vertices.get(end, null);
    if (dst == null) {
      vertices.put(end, e.twin);
      registerFace(e.twin);

    } else if (dst.prev == null) {
      e.link(dst);
      dst.twin.link(e.twin);

    } else {

      HalfEdge curr = e.prev;
      HalfEdge match = null;
      for (; ; ) {
        if (curr == null || curr == e || curr == e.twin) {
          break;
        } else if (curr.start().equals(end) &&
          (curr.prev == null
            || match == null
            || curr.visible(e.twin.curve) == Visibility.INSIDE)) {
          match = curr;
        }

        curr = curr.prev;
      }

      if (match != null) {
        if (match.prev == null) {
          e.link(match);
          match.twin.link(e.twin);
        } else {
          insert(match, e.twin);
        }
        return;
      }

      curr = dst;
      for (; ; ) {
        Visibility v = curr.visible(e.twin.curve);
        if (v == Visibility.INSIDE) {
          insert(curr, e.twin);
          break;

        } else if (v == Visibility.COLLINEAR) {
          remove(e);
          add(e.twin);
          break;

        } else {
          curr = curr.twin.next;
        }

        assert curr != dst;
      }
    }

  }

  private void describe(Vec2 v) {
    System.out.println("\n--- " + v);
    HalfEdge e = vertices.get(v, null);
    System.out.println(e.interiorAngle() + " " + orientation(e.prev.twin.curve, e.curve) + " " + e);
    if (e != null) {
      HalfEdge curr = e.prev.twin;
      while (curr != null && curr != e) {
        System.out.println(curr.interiorAngle() + " " + orientation(e.prev.twin.curve, e.curve) + " " + curr);
        curr = curr.prev.twin;
      }
    }
  }

  public HalfEdge add(Curve2 c, int left, int right) {
    HalfEdge e = new HalfEdge(c, left, right);
    add(e);
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
      vertices.put(e.start(), e.twin.next);

      // singly-linked
      if (e.twin.next == prev.twin) {
        prev.next = null;
        prev.twin.prev = null;

        // multi-linked
      } else {
        prev.link(e.twin.next);
        invalidated = true;
      }
    } else if (vertices.get(e.start(), null) == e) {
      vertices.remove(e.start());
    }

    HalfEdge next = e.next;
    if (next != null) {
      vertices.put(e.end(), e.next);

      // singly-linked
      if (e.twin.prev == next.twin) {
        next.prev = null;
        next.twin.next = null;

        // multi-linked
      } else {
        e.twin.prev.link(next);
        invalidated = true;
      }
    } else if (vertices.get(e.end(), null) == e.twin) {
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

  private void replace(HalfEdge e, HalfEdge a, HalfEdge b) {

    describe(e.start());

    /*if (e.prev != null) {
      e.prev.link(a);
      a.twin.link(e.twin.next);
    }

    a.link(b);
    b.twin.link(a.twin);

    if (e.next != null) {
      b.link(e.next);
      e.twin.prev.link(b.twin);
    }

    vertices.put(e.start(), a);
    vertices.put(e.end(), b.twin);
    pseudoFaces.remove(e).remove(e.twin);
    registerFace(a);
    registerFace(a.twin);*/

    remove(e);

    describe(e.start());
    add(a);
    add(b);


  }

  public HalfEdge split(HalfEdge e, double t) {
    if (t == 0) {
      return e;
    } else if (t == 1) {
      return e.next;
    }

    Curve2[] cs = e.curve.split(t);

    HalfEdge a = new HalfEdge(cs[0], e.flag, e.twin.flag);
    HalfEdge b = new HalfEdge(cs[1], e.flag, e.twin.flag);
    replace(e, a, b);

    return a;
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
