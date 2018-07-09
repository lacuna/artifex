package io.lacuna.artifex.utils;

import io.lacuna.artifex.Curve2;
import io.lacuna.artifex.LineSegment2;
import io.lacuna.artifex.Region2.Ring;
import io.lacuna.artifex.Vec;
import io.lacuna.artifex.Vec2;
import io.lacuna.bifurcan.*;

import java.util.Iterator;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;

import static io.lacuna.artifex.Vec2.angleBetween;
import static io.lacuna.artifex.utils.Scalars.EPSILON;

/**
 * An implementation of a doubly-connected edge list.  Since this is an inherently mutable data structure, it is
 * not exposed at the top-level, and instead only used to transform between immutable geometric representations.
 *
 * @author ztellman
 */
public class EdgeList {

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

    public double interiorAngle() {
      Vec2 in = prev.curve.direction(1).negate();
      Vec2 out = curve.direction(0);
      return -angleBetween(in, out);
    }

    public boolean visible(Vec2 p, Vec2 d) {
      if (flag == 0) {
        return false;
      }

      Vec2 a = prev.start();
      Vec2 b = start();
      Vec2 c = end();

      /*Vec2 in = a.sub(b).norm();
      Vec2 ray = p.sub(b).norm();
      Vec2 out = c.sub(b).norm();*/

      Vec2 in = prev.curve.direction(1).negate().norm();
      Vec2 ray = d.norm();
      Vec2 out = curve.direction(0).norm();

      if (Vec.equals(in, ray, EPSILON) || Vec.equals(out, ray, EPSILON)) {
        in = a.sub(b).norm();
        ray = p.sub(b).norm();
        out = c.sub(b).norm();

        /*in = prev.curve.direction(1).negate().norm();
        ray = d.norm();
        out = curve.direction(0).norm();*/
      }

      double t0 = -angleBetween(in, ray);
      double t1 = -angleBetween(in, out);

      return t0 != 0 && t0 <= t1;
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

  public static EdgeList from(Iterable<Ring> rings, int inside, int outside) {
    EdgeList result = new EdgeList();
    for (Ring r : rings) {
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

  public IList<Ring> boundaries(IntPredicate flagPredicate) {
    IList<Ring> result = new LinearList<>();
    for (HalfEdge e : faces()) {
      if (flagPredicate.test(e.flag)) {
        IList<Curve2> cs = new LinearList<>();
        for (HalfEdge edge : face(e)) {
          cs.addFirst(edge.twin.curve);
        }
        result.addLast(new Ring(cs));
      }
    }
    return result;
  }

  public IMap<Ring, Integer> rings() {
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

        for (HalfEdge edge : face(e)) {
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

  public Ring ring(HalfEdge e) {
    IList<Curve2> cs = new LinearList<>();
    face(e).forEach(edge -> cs.addLast(edge.curve));
    return new Ring(cs);
  }

  public Iterable<HalfEdge> face(HalfEdge e) {
    return () -> new Iterator<HalfEdge>() {
      HalfEdge curr = e;
      boolean started = false;

      @Override
      public boolean hasNext() {
        return !started || curr != e;
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
    return add(LineSegment2.from(a, b), left, right);
  }

  public HalfEdge add(Curve2 c, int left, int right) {

    HalfEdge e = new HalfEdge(c, left, right);

    Vec2 start = c.start();
    Vec2 end = c.end();

    if (vertices.contains(start)) {
      HalfEdge src = vertices.get(start).get();

      // we're connecting from a dangling edge
      if (src.prev == null) {
        e.twin.link(src);
        src.twin.link(e);

        // split the vertex appropriately
      } else {
        Vec2 p = c.end();
        Vec2 d = c.direction(0);
        while (!src.visible(p, d)) {
          src = src.twin.next;
        }

        // it's an equivalent edge, just update the flags and return
        if (src.end().equals(c.end()) && Vec.equals(src.curve.direction(0), c.direction(0), EPSILON)) {
          src.flag |= left;
          src.twin.flag |= right;
          return src;
        }

        src.prev.link(e);
        e.twin.link(src);
        registerFace(e);
      }
    } else {
      assert !checkForNearMiss(start);
      vertices.put(start, e);
      registerFace(e);
    }

    if (vertices.contains(end)) {
      HalfEdge dst = vertices.get(end).get();

      // we're connecting to a dangling edge
      if (dst.prev == null) {
        e.link(dst);
        dst.twin.link(e.twin);

        // split the vertex appropriately
      } else {
        Vec2 p = c.start();
        Vec2 d = c.direction(1).negate();
        while (!dst.visible(p, d)) {
          dst = dst.twin.next;
        }

        dst.prev.link(e.twin);
        e.link(dst);
        registerFace(e.twin);
      }
    } else {
      assert !checkForNearMiss(end);
      vertices.put(end, e.twin);
      registerFace(e);
    }

    return e;
  }

  public void removeFaces(IntPredicate toRemove, int outside) {
    for (HalfEdge e : faces()) {
      if (e.flag != outside && toRemove.test(e.flag)) {
        removeFace(e, n -> n == outside, outside);
      }
    }
  }

  public void removeFace(HalfEdge e, IntPredicate isOutside, int flag) {
    for (HalfEdge edge : LinearList.from(face(e))) {
      if (isOutside.test(edge.twin.flag)) {
        remove(edge);
      } else {
        edge.flag = flag;
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
      Ring r = ring(e);
      b.append("RING: " + e.flag + "\n");
      for (Curve2 c : r.curves) {
        b.append("  " + c + "\n");
      }
      b.append("\n");
    }

    return b.toString();
  }
}
