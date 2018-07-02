package io.lacuna.artifex.utils.regions;

import io.lacuna.artifex.Curve2;
import io.lacuna.artifex.Region2;
import io.lacuna.artifex.Vec2;
import io.lacuna.artifex.utils.EdgeList;
import io.lacuna.artifex.utils.EdgeList.HalfEdge;
import io.lacuna.bifurcan.FloatMap;
import io.lacuna.bifurcan.IMap;
import io.lacuna.bifurcan.LinearList;
import io.lacuna.bifurcan.LinearMap;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static io.lacuna.artifex.utils.regions.Hulls.INSIDE;

/**
 * @author ztellman
 */
public class Monotonic {

  private static final Comparator<Vec2> COMPARATOR = Comparator
    .comparingDouble((Vec2 a) -> -a.y)
    .thenComparingDouble(a -> a.x);

  private static class State {
    private final LinearMap<HalfEdge, Vec2> helper = new LinearMap<>();
    private final FloatMap<HalfEdge> map = new FloatMap<HalfEdge>().linear();

    public State() {
    }

    private static double key(HalfEdge e) {
      return Math.min(e.curve.start().x, e.curve.end().x);
    }

    public void add(HalfEdge e) {
      map.put(key(e), e);
      helper.put(e, e.start());
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
    PriorityQueue<HalfEdge> heap = new PriorityQueue<>((a, b) -> COMPARATOR.compare(a.start(), b.start()));
    IMap<Vec2, VertexType> type = new LinearMap<>();
    State state = new State();

    for (Vec2 v : edges.vertices()) {
      HalfEdge e = edges.edge(v, INSIDE);
      heap.add(e);
      type.put(e.start(), vertexType(e));
    }

    // helpers
    BiConsumer<HalfEdge, HalfEdge> connectMergeHelper = (a, b) -> {
      Vec2 v = state.helper(b);
      if (type.get(v).get() == VertexType.MERGE) {
        edges.add(a.start(), v, INSIDE, INSIDE);
      }
    };

    Consumer<HalfEdge> connectLeftHelper = e -> {
      HalfEdge left = state.search(e.start());
      connectMergeHelper.accept(e, left);
      state.helper(left, e.start());
    };

    // add diagonals
    while (!heap.isEmpty()) {
      HalfEdge curr = heap.poll();
      HalfEdge prev = curr.prev;
      switch (type.get(curr.start()).get()) {
        case START:
          state.add(curr);
          break;

        case END:
          connectMergeHelper.accept(curr, prev);
          state.remove(prev);
          break;

        case SPLIT:
          HalfEdge left = state.search(curr.start());
          Vec2 v = state.helper(left);
          edges.add(curr.start(), v, INSIDE, INSIDE);

          state.helper(left, curr.start());
          state.add(curr);
          break;

        case MERGE:
          connectMergeHelper.accept(curr, prev);

          state.remove(prev);
          connectLeftHelper.accept(curr);
          break;

        case REGULAR:
          if (above(prev.start(), curr.start())) {
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
}
