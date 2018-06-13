package io.lacuna.artifex.utils;

import io.lacuna.artifex.*;
import io.lacuna.artifex.utils.EdgeList.HalfEdge;
import io.lacuna.bifurcan.*;

import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static io.lacuna.artifex.Vec.vec;
import static io.lacuna.artifex.Vec2.angleBetween;
import static io.lacuna.artifex.Vec2.cross;

/**
 * @author ztellman
 */
public class Triangulation {

  /// linearize

  public static boolean isClipped(HalfEdge e) {
    return !(e.curve instanceof LineSegment2)
      || !(e.next.curve instanceof LineSegment2)
      || !(e.prev.curve instanceof  LineSegment2);
  }

  public static boolean isConvex(Curve2 c) {
    return -angleBetween(c.direction(0).negate(), c.direction(1)) < Math.PI;
  }

  public static Vec2 tangentIntersection(Curve2 c) {
    Vec2 pv = c.direction(0);
    Vec2 qv = c.direction(1).negate();

    double d = cross(pv, qv);
    if (d == 0) {
      System.out.println(c);
      throw new IllegalStateException();
    }

    double s = cross(qv, c.start().sub(c.end())) / d;
    return c.start().add(pv.mul(s));
  }

  public static double hullArea(Curve2 curve) {
    if (curve instanceof LineSegment2) {
      return 0;
    }

    Vec2 a = curve.start();
    Vec2 b = curve.end();
    Vec2 c = tangentIntersection(curve);
    return Math.abs(((a.x - c.x) * (b.y - a.y)) - ((a.x - b.x) * (c.y - a.y))) / 2;
  }

  public static LineSegment2 intersector(Curve2 c) {
    if (c instanceof LineSegment2) {
      return (LineSegment2) c;
    } else if (isConvex(c)) {
      return LineSegment2.from(c.start(), c.end());
    } else {
      return LineSegment2.from(c.start(), tangentIntersection(c));
    }
  }

  public static void add(SweepQueue<HalfEdge> queue, IMap<HalfEdge, LineSegment2> intersectors, HalfEdge e) {
    LineSegment2 l = intersector(e.curve);
    intersectors.put(e, l);
    queue.add(e, l.start().x, l.end().x);
  }

  public static void linearize(EdgeList edges) {
    IMap<HalfEdge, LineSegment2> intersectors = new LinearMap<>();
    SweepQueue<HalfEdge> queue = new SweepQueue<>();
    for (Vec2 v : edges.vertices()) {
      add(queue, intersectors, edges.edge(v));
    }

    HalfEdge curr = queue.next();
    while (curr != null) {
      LineSegment2 a = intersectors.get(curr).get();
      for (HalfEdge e : queue.active()) {
        if (e == curr) {
          continue;
        }

        LineSegment2 b = intersectors.get(e, null);
        if (b != null && a.intersects(b, false)) {
          System.out.println(e + " " + a);
          System.out.println(curr + " " + b);
          double[] ts = a.intersections(b);
          for (int i = 0; i < ts.length; i++) {
            System.out.print(ts[i] + " ");
          }
          System.out.println();

          HalfEdge toSplit = hullArea(e.curve) < hullArea(curr.curve) ? curr : e;
          HalfEdge split = edges.split(toSplit, 0.5);

          intersectors.remove(toSplit);
          add(queue, intersectors, split);
          add(queue, intersectors, split.prev);

          if (toSplit == curr) {
            break;
          }
        }
      }

      curr = queue.next();
    }

    for (HalfEdge e : intersectors.keys()) {
      Curve2 c = e.curve;
      if (c instanceof LineSegment2) {
        continue;
      }

      if (isConvex(c)) {
        edges.add(c.end(), c.start());
      } else {
        Vec2 v = tangentIntersection(c);
        edges.add(c.end(), v);
        edges.add(v, c.start());
      }
    }
  }

  /// monotonize

  private static final Comparator<Vec2> COMPARATOR = Comparator
    .comparingDouble((Vec2 a) -> -a.y)
    .thenComparingDouble(a -> a.x);

  private static class MonotonicState {
    private final LinearMap<HalfEdge, Vec2> helper = new LinearMap<>();
    private final FloatMap<HalfEdge> map = new FloatMap<HalfEdge>().linear();

    public MonotonicState() {
    }

    private static double key(HalfEdge e) {
      return Math.min(e.curve.start().x, e.curve.end().x);
    }

    public void add(HalfEdge e) {
      map.put(key(e), e);
      helper.put(e, e.origin());
    }

    public void helper(HalfEdge a, Vec2 v) {
      helper.put(a, v);
    }

    public Vec2 helper(HalfEdge a) {
      return helper.get(a).get();
    }

    public void remove(HalfEdge e) {
      HalfEdge x = map.get(key(e), null);
      if (x == e) {
        map.remove(key(e));
      }
      helper.remove(e);
    }

    public HalfEdge search(Vec2 v) {
      return map.floor(v.x).value();
    }

    @Override
    public String toString() {
      return map.keys().toString();
    }
  }

  enum VertexType {
    REGULAR,
    SPLIT,
    MERGE,
    START,
    END
  }

  private static boolean above(Vec2 a, Vec2 b) {
    return COMPARATOR.compare(a, b) < 0;
  }

  private static VertexType vertexType(HalfEdge e) {
    Vec2 a = e.prev.curve.start();
    Vec2 b = e.curve.start();
    Vec2 c = e.curve.end();

    if (above(a, b) && above(c, b)) {
      return e.interiorAngle() < Math.PI ? VertexType.END : VertexType.MERGE;
    } else if (above(b, a) && above(b, c)) {
      return e.interiorAngle() < Math.PI ? VertexType.START : VertexType.SPLIT;
    } else {
      return VertexType.REGULAR;
    }
  }

  public static void monotonize(EdgeList edges) {

    // state
    PriorityQueue<HalfEdge> heap = new PriorityQueue<>((a, b) -> COMPARATOR.compare(a.origin(), b.origin()));
    IMap<Vec2, VertexType> type = new LinearMap<>();
    MonotonicState state = new MonotonicState();

    for (Vec2 v : edges.vertices()) {
      HalfEdge e = edges.edge(v);
      while (isClipped(e)) {
        e = e.twin.next;
      }
      heap.add(e);
      type.put(e.origin(), vertexType(e));
    }

    LinearList<Curve2> perimeter = new LinearList<>();
    HalfEdge currEdge = heap.peek();
    do {
      perimeter.addLast(currEdge.curve);
      System.out.println(type.get(currEdge.origin()).get() + " " + above(currEdge.prev.origin(), currEdge.origin()) + " " + currEdge);
      currEdge = currEdge.next;
    } while (currEdge != heap.peek());
    System.out.println(new Region2.Ring(perimeter).isClockwise);

    System.out.println(heap.size());

    // helpers
    BiConsumer<HalfEdge, HalfEdge> connectMergeHelper = (a, b) -> {
      Vec2 v = state.helper(b);
      if (type.get(v).get() == VertexType.MERGE) {
        edges.add(a.origin(), v);
      }
    };

    Consumer<HalfEdge> connectLeftHelper = e -> {
      HalfEdge left = state.search(e.origin());
      connectMergeHelper.accept(e, left);
      state.helper(left, e.origin());
    };

    // add diagonals
    while (!heap.isEmpty()) {
      HalfEdge curr = heap.poll();
      HalfEdge prev = curr.prev;
      //System.out.println(curr.origin() + " " + type.get(curr.origin()).get() + " " + above(curr.prev.origin(), curr.origin()));
      switch (type.get(curr.origin()).get()) {
        case START:
          state.add(curr);
          break;

        case END:
          connectMergeHelper.accept(curr, prev);
          state.remove(prev);
          break;

        case SPLIT:
          HalfEdge left = state.search(curr.origin());
          Vec2 v = state.helper(left);
          edges.add(curr.origin(), v);

          state.helper(left, curr.origin());
          state.add(curr);
          break;

        case MERGE:
          connectMergeHelper.accept(curr, prev);

          state.remove(prev);
          connectLeftHelper.accept(curr);
          break;

        case REGULAR:
          if (above(prev.origin(), curr.origin())) {
            connectMergeHelper.accept(curr, prev);
            state.remove(prev);
            state.add(curr);
          } else {
            connectLeftHelper.accept(curr);
          }
          break;
      }
    }
  }

  /// triangulate

  public static void triangulate(EdgeList edges) {
    Iterator<HalfEdge> it = edges.faces();
    while (it.hasNext()) {
      triangulate(edges, it.next());
    }
  }

  public static void triangulate(EdgeList edges, HalfEdge init) {

    IList<HalfEdge> halfEdges = new LinearList<>();

    HalfEdge curr = init;
    do {
      halfEdges.addLast(curr);
      curr = curr.next;
    } while (curr != init);

    if (halfEdges.size() <= 3) {
      return;
    }
    halfEdges = Lists.sort(halfEdges, (a, b) -> COMPARATOR.compare(a.origin(), b.origin()));

    ISet<Vec2> left = new LinearSet<>();
    curr = halfEdges.first();
    while (curr != halfEdges.last()) {
      left.add(curr.origin());
      curr = curr.next;
    }

    IList<Vec2> vertices = halfEdges.stream().map(HalfEdge::origin).collect(Lists.linearCollector());

    LinearList<Vec2> stack = new LinearList<Vec2>()
      .addLast(vertices.nth(0))
      .addLast(vertices.nth(1));

    for (int i = 2; i < vertices.size() - 1; i++) {
      Vec2 u = vertices.nth(i);
      boolean isLeft = left.contains(u);

      if (isLeft == left.contains(stack.last())) {
        Vec2 popped = stack.popLast();
        while (stack.size() > 0) {
          Vec2 v = stack.last();

          double theta = -angleBetween(popped.sub(u), v.sub(u));
          boolean isVisible = isLeft
            ? Scalars.inside(1e-3, theta, Math.PI)
            : Scalars.inside(1e-3, (Math.PI * 2) - theta, Math.PI);

          if (isVisible) {
            popped = stack.popLast();
            edges.add(u, v);
          } else {
            break;
          }
        }

        stack
          .addLast(popped)
          .addLast(u);
      } else {
        while (stack.size() > 1) {
          Vec2 v = stack.popLast();
          edges.add(u, v);
        }
        stack
          .removeLast()
          .addLast(vertices.nth(i - 1))
          .addLast(u);
      }
    }

    stack.removeLast();
    while (stack.size() > 1) {
      edges.add(vertices.last(), stack.popLast());
    }
  }


}
