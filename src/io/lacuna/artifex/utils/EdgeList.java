package io.lacuna.artifex.utils;

import io.lacuna.artifex.*;
import io.lacuna.artifex.Region2.Ring;
import io.lacuna.bifurcan.*;

import java.util.Iterator;

import static io.lacuna.artifex.Vec.dot;
import static io.lacuna.artifex.Vec2.angleBetween;

/**
 * @author ztellman
 */
public class EdgeList {

  public static class HalfEdge {
    public HalfEdge prev, next, twin;
    public final Curve2 curve;

    public HalfEdge(Curve2 curve) {
      this.curve = curve;
    }

    public double interiorAngle() {
      Vec2 in = prev.curve.direction(1).negate();
      Vec2 out = curve.direction(0);
      return -angleBetween(in, out);
    }

    public boolean visible(Vec2 p) {
      Vec2 in = prev.curve.direction(1).negate();
      Vec2 out = curve.direction(0);
      return -angleBetween(in, p.sub(origin())) < -angleBetween(in, out);
    }

    public Vec2 origin() {
      return curve.start();
    }

    @Override
    public String toString() {
      return curve.toString();
    }
  }

  private static HalfEdge ring(LinearMap<Vec2, HalfEdge> vertices, Ring ring) {
    HalfEdge[] edges = new HalfEdge[ring.curves.length];
    for (int i = 0; i < edges.length; i++) {
      edges[i] = new HalfEdge(ring.curves[i]);
    }

    for (int i = 0; i < edges.length; i++) {
      HalfEdge e = edges[i];
      vertices.put(e.curve.start(), e);
      e.next = edges[(i + 1) % edges.length];
      e.prev = edges[(i + edges.length - 1) % edges.length];
    }

    return edges[0];
  }

  //

  private final LinearMap<Vec2, HalfEdge> vertices;

  private final LinearSet<HalfEdge> faces;
  private boolean dirty = false;

  private EdgeList(LinearMap<Vec2, HalfEdge> vertices, LinearSet<HalfEdge> faces) {
    this.vertices = vertices;
    this.faces = faces;
  }

  public static EdgeList from(IMap<Ring, IList<Ring>> region) {
    LinearMap<Vec2, HalfEdge> vertices = new LinearMap<>();
    LinearSet<HalfEdge> faces = new LinearSet<>();

    for (IEntry<Ring, IList<Ring>> e : region) {
      faces.add(ring(vertices, e.key()));
      for (Ring r : e.value()) {
        faces.add(ring(vertices, r));
      }
    }

    return new EdgeList(vertices, faces);
  }

  public ISet<Vec2> vertices() {
    return vertices.keys();
  }

  public HalfEdge edge(Vec2 v) {
    return vertices.get(v).get();
  }

  public HalfEdge split(HalfEdge e, double t) {
    if (t < Curve2.SPLIT_EPSILON) {
      return e;
    } else if (t > 1 - Curve2.SPLIT_EPSILON) {
      return e.next;
    }

    Curve2[] cs = e.curve.split(t);
    HalfEdge a = new HalfEdge(cs[0]);
    HalfEdge b = new HalfEdge(cs[1]);

    if (faces.contains(e)) {
      faces.remove(e).add(a);
    }

    link(e.prev, a);
    link(a, b);
    link(b, e.next);

    vertices.put(a.origin(), a);
    vertices.put(b.origin(), b);

    if (e.twin != null) {
      e.twin.twin = null;
      HalfEdge ta = split(e.twin, 1 - t);
      twin(a, ta);
      twin(b, ta.prev);
    }

    return b;
  }

  public void add(Vec2 a, Vec2 b) {
    add(LineSegment2.from(a, b));
  }

  public void add(Curve2 c) {
    HalfEdge a = new HalfEdge(c);
    HalfEdge b = new HalfEdge(c.reverse());

    twin(a, b);

    if (vertices.contains(a.origin())) {
      HalfEdge e = edge(a.origin());
      // dangling vertex
      if (e.prev == null) {
        link(e.twin, a);
        link(b, e);

        // split existing loop
      } else {
        HalfEdge[] src = splitPair(a.curve);
        link(src[0], a);
        link(b, src[1]);
      }
    }

    if (vertices.contains(b.origin())) {
      HalfEdge[] dst = splitPair(b.curve);
      link(dst[0], b);
      link(a, dst[1]);
    }

    vertices
      .put(a.origin(), base(a))
      .put(b.origin(), base(b));

    registerFace(a);
    registerFace(b);
  }

  ///

  private void link(HalfEdge a, HalfEdge b) {
    a.next = b;
    b.prev = a;
  }

  private void twin(HalfEdge a, HalfEdge b) {
    a.twin = b;
    b.twin = a;
  }

  private void registerFace(HalfEdge e) {
    faces.add(e);
    dirty = true;
  }

  // the most counter-clockwise edge, if there's not a complete circuit around the vertex
  private HalfEdge base(HalfEdge init) {
    HalfEdge curr = init;
    while (curr.prev != null && curr.prev.twin != null) {
      curr = curr.prev.twin;
      assert curr.origin().equals(init.origin());
      if (curr == init) {
        break;
      }
    }

    return curr;
  }

  private HalfEdge[] splitPair(Curve2 c) {
    Vec2 p = c.start().add(c.direction(0));
    HalfEdge e = vertices.get(c.start()).get();
    for (; ; ) {
      if (e.twin == null || e.visible(p)) {
        break;
      }

      e = e.twin.next;
    }
    return new HalfEdge[]{e.prev, e};
  }

  public Iterator<HalfEdge> faces() {
    return new Iterator<HalfEdge>() {
      int i = 0;

      @Override
      public boolean hasNext() {
        return i < faces.size();
      }

      @Override
      public HalfEdge next() {
        HalfEdge e = faces.nth(i++);
        if (dirty) {
          HalfEdge curr = e;
          while (curr.next != e) {
            curr = curr.next;
            faces.remove(curr);
          }
        }

        if (i == faces.size()) {
          dirty = false;
        }

        return e;
      }
    };
  }

  public IList<Curve2> ring(HalfEdge e) {
    IList<Curve2> result = new LinearList<>();
    result.addLast(e.curve);
    HalfEdge curr = e.next;

    while (curr != null && curr != e) {
      result.addLast(curr.curve);
      curr = curr.next;
    }

    return result;
  }

  ///

  public IList<Fan2> fans() {

    IList<Fan2> result = new LinearList<>();

    Triangulation.monotonize(this);
    Triangulation.triangulate(this);

    for (HalfEdge init : faces) {
      HalfEdge a = init;
      HalfEdge b = a.next;
      HalfEdge c = b.next;

      //assert c.next == a;

      boolean xa = a.twin == null;
      boolean xb = b.twin == null;
      boolean xc = c.twin == null;

      int flag = (xa ? 1 : 0) | (xb ? 2 : 0) | (xc ? 4 : 0);
      if (flag == 0) {
        result.addLast(new Fan2(a.origin(), b.curve, true));
      } else {
        Vec2 centroid = null;
        switch (flag) {
          case 1:
            centroid = c.origin();
            break;
          case 2:
            centroid = a.origin();
            break;
          case 3:
            centroid = c.curve.position(0.5);
            break;
          case 4:
            centroid = b.origin();
            break;
          case 5:
            centroid = b.curve.position(0.5);
            break;
          case 6:
            centroid = a.curve.position(0.5);
            break;
          case 7:
            centroid = a.origin().add(b.origin()).add(c.origin()).div(3);
            break;
        }

        if (xa) result.addLast(new Fan2(centroid, a.curve));
        if (xb) result.addLast(new Fan2(centroid, b.curve));
        if (xc) result.addLast(new Fan2(centroid, c.curve));
      }
    }

    return result;
  }

  ///

  public void describe(Vec2 p) {
    System.out.println("from " + p);
    HalfEdge init = edge(p);
    HalfEdge curr = init;
    for (; ; ) {
      System.out.println("  " + curr.curve);
      if (curr.twin == null || curr.twin.next == init) {
        System.out.println(curr.twin == null ? "none" : "looped");
        break;
      }
      curr = curr.twin.next;
    }
  }

}
