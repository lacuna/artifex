package io.lacuna.artifex.utils;

import io.lacuna.artifex.Curve2;
import io.lacuna.artifex.LineSegment2;
import io.lacuna.artifex.Region2.Ring;
import io.lacuna.artifex.Vec2;
import io.lacuna.bifurcan.*;

import java.util.Comparator;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static io.lacuna.artifex.Vec2.angleBetween;

/**
 * @author ztellman
 */
public class MonotoneRegion {

  private static final Comparator<Vec2> VERTEX_COMPARATOR = Comparator
    .comparingDouble((Vec2 v) -> v.y)
    .thenComparingDouble(v -> v.x);

  enum VertexType {
    REGULAR,
    SPLIT,
    MERGE,
    START,
    END
  }

  private static boolean above(Vec2 a, Vec2 b) {
    return (a.y < b.y) || (a.y == b.y && a.x < b.x);
  }

  public static VertexType vertexType(Vec2 a, Vec2 b, Vec2 c) {
    if (above(a, b) && above(c, b)) {
      return -angleBetween(a.sub(b), c.sub(b)) < Math.PI ? VertexType.END : VertexType.MERGE;
    } else if (above(b, a) && above(b, c)) {
      return -angleBetween(a.sub(b), c.sub(b)) < Math.PI ? VertexType.START : VertexType.SPLIT;
    } else {
      return VertexType.REGULAR;
    }
  }

  private static class Vertex {
    final Ring ring;
    final int idx;
    private VertexType type = null;

    public Vertex(Ring ring, int idx) {
      this.ring = ring;
      this.idx = idx;
    }

    public boolean isMerge() {
      return type() == VertexType.MERGE;
    }

    public VertexType type() {
      if (type == null) {
        Vec2 a = ring.curves[idx == 0 ? ring.curves.length - 1 : idx - 1].start();
        Vec2 b = ring.curves[idx].start();
        Vec2 c = ring.curves[idx].end();
        type = vertexType(a, b, c);
      }

      return type;
    }

    public Curve2 curve() {
      return ring.curves[idx];
    }

    public Vec2 start() {
      return curve().start();
    }

    public Vertex next() {
      int idx = this.idx == ring.curves.length - 1 ? 0 : this.idx + 1;
      return new Vertex(ring, idx);
    }

    public Vertex prev() {
      int idx = this.idx == 0 ? ring.curves.length - 1 : this.idx - 1;
      return new Vertex(ring, idx);
    }

    @Override
    public int hashCode() {
      return Objects.hash(ring, idx);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      } else if (obj instanceof Vertex) {
        Vertex v = (Vertex) obj;
        return ring == v.ring && idx == v.idx;
      } else {
        return false;
      }
    }
  }

  private static class Node {
    Curve2 prev, next;

    public Node(Curve2 prev, Curve2 next) {
      this.prev = prev;
      this.next = next;
    }
  }

  private static void add(FloatMap<ISet<Vertex>> m, Vertex v) {
    m.put(v.start().x, new LinearSet<Vertex>().add(v), ISet::union);
  }

  private static void remove(FloatMap<ISet<Vertex>> m, Vertex v) {
    ISet<Vertex> s = m.get(v.start().x, null);
    if (s != null) {
      if (s.size() == 1) {
        m.remove(v.start().x);
      } else {
        s.remove(v);
      }
    }
  }

  private static Vertex closest(FloatMap<ISet<Vertex>> m, Vertex v) {
    IEntry<Double, ISet<Vertex>> e = m.floor(v.start().x);
    return e == null ? null : e.value().nth(0);
  }

  private static void connect(IMap<Curve2, Node> m, Vertex va, Vertex vb) {
    Curve2 a = va.curve();
    Curve2 b = vb.curve();

    Curve2 aPrev = m.get(a).get().prev;
    Curve2 bPrev = m.get(b).get().prev;

    LineSegment2 ab = LineSegment2.from(a.start(), b.start());
    LineSegment2 ba = ab.reverse();

    m.put(ab, new Node(aPrev, b));
    m.put(ba, new Node(bPrev, a));
    m.get(aPrev).get().next = ab;
    m.get(bPrev).get().next = ba;
  }

  public static Ring[] monotoneRegions(Ring perimeter, Iterable<Ring> holes) {

    PriorityQueue<Vertex> queue = new PriorityQueue<>((a, b) -> VERTEX_COMPARATOR.compare(a.start(), b.start()));
    IMap<Curve2, Node> edges = new LinearMap<>();
    IMap<Vertex, Vertex> helper = new LinearMap<>();
    FloatMap<ISet<Vertex>> active = new FloatMap<ISet<Vertex>>().linear();

    for (int i = 0; i < perimeter.curves.length; i++) {
      Vertex v = new Vertex(perimeter, i);
      queue.add(v);
      edges.put(v.curve(), new Node(v.prev().curve(), v.next().curve()));
    }

    for (Ring hole : holes) {
      for (int i = 0; i < hole.curves.length; i++) {
        Vertex v = new Vertex(hole, i);
        queue.add(v);
        edges.put(v.curve(), new Node(v.prev().curve(), v.next().curve()));
      }
    }

    Consumer<Vertex> connectLeftHelper = v -> {
      Vertex left = closest(active, v);
      Vertex h = helper.get(left).get();
      if (h.isMerge()) {
        connect(edges, v, h);
      }
      helper.put(left, v);
    };

    BiConsumer<Vertex, Vertex> connectPrevHelper = (curr, prev) -> {
      Vertex h = helper.get(prev).get();
      if (h.isMerge()) {
        connect(edges, curr, h);
      }
    };

    while (!queue.isEmpty()) {
      Vertex curr = queue.poll();
      Vertex prev = curr.prev();
      switch (curr.type()) {
        case START:
          add(active, curr);
          helper.put(curr, curr);
          break;

        case END:
          connectPrevHelper.accept(curr, prev);
          remove(active, curr);
          break;

        case SPLIT:
          Vertex left = closest(active, curr);
          Vertex h = helper.get(left).get();
          connect(edges, curr, h);

          helper.put(left, curr).put(curr, curr);
          add(active, curr);
          break;

        case MERGE:
          connectPrevHelper.accept(curr, prev);
          remove(active, prev);
          connectLeftHelper.accept(curr);
          helper.put(curr, curr);
          break;

        case REGULAR:
          if (perimeter.contains(curr.start().add(Scalars.EPSILON, 0))) {
            connectPrevHelper.accept(curr, prev);
            remove(active, prev);
            add(active, curr);
            helper.put(curr, curr);
          } else {
            connectLeftHelper.accept(curr);
          }
          break;
      }
    }

    IList<Ring> result = new LinearList<>();
    while (edges.size() > 0) {
      IList<Curve2> curves = new LinearList<>();
      curves.addLast(edges.nth(0).key());
      for (;;) {
        Curve2 next = edges.get(curves.last()).get().next;
        edges.remove(curves.last());

        if (next == curves.first()) {
          break;
        } else {
          curves.addLast(next);
        }
      }
      result.addLast(new Ring(curves));
    }

    return result.toArray(Ring.class);
  }


}
