package io.lacuna.artifex.utils;

import io.lacuna.artifex.*;
import io.lacuna.artifex.Region2.Ring;
import io.lacuna.artifex.utils.EdgeList.HalfEdge;
import io.lacuna.bifurcan.*;

import java.util.Comparator;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.LongPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.lacuna.artifex.Vec2.angleBetween;

/**
 * @author ztellman
 */
public class Triangulation {

  private static final Comparator<Vec2> COMPARATOR = Comparator
    .comparingDouble((Vec2 a) -> -a.y)
    .thenComparingDouble(a -> a.x);

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

  public static VertexType vertexType(HalfEdge e) {
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

  private static void add(FloatMap<HalfEdge> m, HalfEdge e) {
    m.put(e.origin().x, e);
  }

  private static void remove(FloatMap<HalfEdge> m, HalfEdge e) {
    HalfEdge x = m.get(e.origin().x, null);
    if (e.equals(x)) {
      m.remove(e.origin().x);
    }
  }

  private static HalfEdge closest(FloatMap<HalfEdge> m, HalfEdge e) {
    IEntry<Double, HalfEdge> entry = m.floor(e.origin().x);
    return entry == null ? null : entry.value();
  }

  private static boolean isLeft(HalfEdge e) {
    return above(e.prev.origin(), e.origin());
  }

  private static void connect(EdgeList edges, Vec2 reflexVertex, Vec2 v) {
    LineSegment2 diagonal = LineSegment2.from(reflexVertex, v);
    if (edges.edgeFrom(v, reflexVertex) != null) {
      edges.add(diagonal);
    } else {
      HalfEdge l = edges.edge(v);

      HalfEdge r = l;
      while (r.twin != null) {
        r = r.twin.prev;
      }
      r = r.prev;

      for (HalfEdge e : new HalfEdge[] {l, r}) {
        double[] ts = e.curve.intersections(diagonal);
        if (ts.length > 2) {
          edges.add(
            reflexVertex,
            edges.split(e, ts[1] == 1.0 ? ts[2] : ts[0]).origin());
          break;
        }
      }
    }
  }

  public static void monotonize(EdgeList edges) {

    // state
    PriorityQueue<HalfEdge> heap = new PriorityQueue<>((a, b) -> COMPARATOR.compare(a.origin(), b.origin()));
    IMap<HalfEdge, HalfEdge> helper = new LinearMap<>();
    IMap<HalfEdge, VertexType> type = new LinearMap<>();
    FloatMap<HalfEdge> horizontal = new FloatMap<HalfEdge>().linear();

    for (Vec2 v : edges.vertices()) {
      HalfEdge e = edges.edge(v);
      heap.add(e);
      type.put(e, vertexType(e));
    }

    // helpers
    BiConsumer<HalfEdge, HalfEdge> connectHelper = (curr, prev) -> {
      HalfEdge h = helper.get(prev).get();
      if (type.get(h).get() == VertexType.MERGE) {
        connect(edges, h.origin(), curr.origin());
      }
    };

    Consumer<HalfEdge> connectLeftHelper = e -> {
      HalfEdge left = closest(horizontal, e);
      connectHelper.accept(e, left);
      helper.put(left, e);
    };

    // add diagonals
    while (!heap.isEmpty()) {
      HalfEdge curr = heap.poll();
      switch (type.get(curr).get()) {
        case START:
          add(horizontal, curr);
          helper.put(curr, curr);
          break;

        case END:
          connectHelper.accept(curr, curr.prev);
          remove(horizontal, curr);
          break;

        case SPLIT:
          HalfEdge left = closest(horizontal, curr);
          HalfEdge h = helper.get(left).get();
          connect(edges, curr.origin(), h.origin());

          helper.put(left, curr).put(curr, curr);
          add(horizontal, curr);
          break;

        case MERGE:
          connectHelper.accept(curr, curr.prev);

          remove(horizontal, curr.prev);
          connectLeftHelper.accept(curr);

          helper.put(curr, curr);
          break;

        case REGULAR:
          if (isLeft(curr)) {
            connectHelper.accept(curr, curr.prev);
            remove(horizontal, curr.prev);
            add(horizontal, curr);
            helper.put(curr, curr);
          } else {
            connectLeftHelper.accept(curr);
          }
          break;
      }
    }
  }

  private static HalfEdge tangentSplit(EdgeList edges, HalfEdge splitter, HalfEdge target) {
    double dist = splitter.curve.bounds().union(target.curve.bounds()).size().length();
    Vec2 tangent = splitter.curve.direction(0).norm().mul(dist);
    double[] ts = target.curve.intersections(LineSegment2.from(splitter.origin(), splitter.origin().add(tangent)));

    return edges.split(target, ts[0]);
  }

  public static void triangulate(EdgeList edges) {
    for (HalfEdge e : edges.faces().toArray(HalfEdge[]::new)) {
      triangulate(edges, e);
    }
  }

  public static void triangulate(EdgeList edges, HalfEdge init) {

    IList<HalfEdge> halfEdges = new LinearList<>();

    HalfEdge curr = init;
    do {
      halfEdges.addLast(curr);
      curr = curr.next;
    } while (curr != init);

    if (halfEdges.size() == 3) {
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

          if (isVisible && edges.visible(u, v)) {
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
          if (edges.visible(u, v)) {
            edges.add(u, v);
          } else {
            System.out.println("cannot draw between " + u + " " + v + " " + (edges.edgeFrom(u, v) != null) + " " + (edges.edgeFrom(v, u) != null));
            // TODO need to split the curve above us
            throw new IllegalStateException();
          }
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
