package io.lacuna.artifex.utils.regions;

import io.lacuna.artifex.*;
import io.lacuna.artifex.Ring2.Result;
import io.lacuna.bifurcan.*;
import io.lacuna.bifurcan.utils.Iterators;

import java.util.Comparator;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.lang.Math.abs;

public class Clip {

  private static final ISet<Vec2> VERTICES = new LinearSet<>();

  private final static class Arc extends LinearList<Curve2> {

    double length() {
      return stream().mapToDouble(c -> c.end().sub(c.start()).length()).sum();
    }

    Vec2 head() {
      return first().start();
    }

    Vec2 tail() {
      return last().end();
    }

    Vec2 position(double t) {
      double length = length(),
        offset = 0,
        threshold = length * t;

      for (Curve2 c : this) {
        double l = c.end().sub(c.start()).length();
        Interval i = new Interval(offset, offset + l);
        if (i.contains(threshold)) {
          return c.position(i.normalize(threshold));
        }
        offset = i.hi;
      }

      throw new IllegalStateException();
    }

    Arc reverse() {
      Arc result = new Arc();
      forEach(c -> result.addFirst(c.reverse()));
      return result;
    }

    IList<Vec2> vertices() {
      IList<Vec2> result = new LinearList<Vec2>().addLast(head());
      forEach(c -> result.addLast(c.end()));
      return result;
    }

    double signedArea() {
      return stream().mapToDouble(Curve2::signedArea).sum();
    }
  }

  private static Ring2 ring(IList<Arc> arcs) {
    IList<Curve2> acc = new LinearList<>();
    arcs.forEach(arc -> arc.forEach(acc::addLast));
    return new Ring2(acc);
  }

  private static Arc arc(Curve2... cs) {
    Arc result = new Arc();
    for (Curve2 c : cs) {
      result.addLast(c);
    }
    return result;
  }

  private static <U, V> IList<V> edges(IList<U> vertices, BiFunction<U, U, V> edge) {
    IList<V> result = new LinearList<>();
    for (int i = 0; i < vertices.size() - 1; i++) {
      result.addLast(edge.apply(vertices.nth(i), vertices.nth(i + 1)));
    }
    return result;
  }

  private enum Operation {
    UNION,
    INTERSECTION,
    DIFFERENCE
  }

  private enum Type {
    OUTSIDE,
    INSIDE,
    SAME_EDGE,
    DIFF_EDGE
  }

  /// debug

  public static void describe(String prefix, IList<Vec2>... arcs) {
    for (IList<Vec2> arc : arcs) {
      arc.forEach(VERTICES::add);
      System.out.print(prefix + " ");
      arc.forEach(v -> System.out.print(VERTICES.indexOf(v) + " "));

      System.out.println();
    }
  }

  /// arc accessors

  private static IMap<Vec2, Integer> edgeCounts(Iterable<Arc> arcs) {
    IMap<Vec2, Integer> result = new LinearMap<>();
    for (Arc arc : arcs) {
      result.update(arc.head(), n -> (n == null ? 0 : n) + 1);
      result.update(arc.tail(), n -> (n == null ? 0 : n) + 1);
    }
    return result;
  }

  ///

  private static boolean isTop(Curve2 c) {
    if (c == null) {
      return false;
    }

    double delta = c.end().x - c.start().x;
    if (delta == 0) {
      return c.end().y > c.start().y;
    }
    return delta < 0;
  }

  private static IList<Arc> partition(Region2 region, ISet<Vec2> vertices) {
    IList<Arc> result = new LinearList<>();

    for (Ring2 r : region.rings) {
      Curve2[] cs = r.curves;
      int offset = 0;
      for (; offset < cs.length; offset++) {
        if (vertices.contains(cs[offset].start())) {
          break;
        }
      }

      if (offset == cs.length) {
        result.addLast(arc(cs));
      } else {
        Arc acc = new Arc();
        for (int i = offset; i < cs.length; i++) {
          Curve2 c = cs[i];
          if (vertices.contains(c.start())) {
            if (acc.size() > 0) {
              result.addLast(acc);
            }
            acc = arc(c);
          } else {
            acc.addLast(c);
          }
        }

        for (int i = 0; i < offset; i++) {
          acc.addLast(cs[i]);
        }

        if (acc.size() > 0) {
          result.addLast(acc);
        }
      }
    }

    return result;
  }

  private static Type classify(Region2 region, Arc arc) {
    Result result = region.test(arc.position(0.5));
    if (!result.inside) {
      return Type.OUTSIDE;
    } else if (result.curve == null) {
      return Type.INSIDE;
    } else {
      return isTop(arc.first()) == isTop(result.curve) ? Type.SAME_EDGE : Type.DIFF_EDGE;
    }
  }

  public static <V> IList<IList<V>> permutations(IList<IList<V>> paths) {
    if (paths.stream().anyMatch(l -> l.size() == 0)) {
      return Lists.EMPTY;
    } else if (paths.stream().allMatch(l -> l.size() == 1)) {
      return LinearList.of(paths.stream().map(IList::first).collect(Lists.linearCollector()));
    }

    int[] indices = new int[(int) paths.size()];
    IList<IList<V>> result = new LinearList<>();

    while (indices[0] < paths.first().size()) {
      IList<V> path = new LinearList<>(indices.length);
      for (int i = 0; i < indices.length; i++) {
        path.addLast(paths.nth(i).nth(indices[i]));
      }
      result.addLast(path);

      for (int i = indices.length - 1; i >= 0; i--) {
        if (++indices[i] < paths.nth(i).size()) {
          break;
        } else if (i > 0) {
          indices[i] = 0;
        }
      }
    }

    return result;
  }

  public static Region2 operation(Region2 ra, Region2 rb, Operation operation, Predicate<Type> aPredicate, Predicate<Type> bPredicate) {
    Split.Result split = Split.split(ra, rb);
    Region2 a = split.a;
    Region2 b = split.b;

    // partition rings into arcs separated at intersection points
    IList<Arc>
      pa = partition(a, split.splits),
      pb = partition(b, split.splits);

    if (operation == Operation.DIFFERENCE) {
      pb = pb.stream().map(Arc::reverse).collect(Lists.linearCollector());
    }

    // classify arcs w.r.t. the other region
    IMap<Arc, Type> types =
      LinearSet.from(pa).zip(arc -> classify(b, arc))
        .union(LinearSet.from(pb).zip(arc -> classify(a, arc)));

    // filter out arcs which are to be ignored, per our operation
    ISet<Arc> arcs = new LinearSet<>();
    pa.stream()
      .filter(arc -> aPredicate.test(types.get(arc).get()))
      .forEach(arcs::add);
    pb.stream()
      .filter(arc -> bPredicate.test(types.get(arc).get()))
      .forEach(arcs::add);

    /*
    System.out.println("\n---");
    describe("splits", split.splits.elements());
    describe("arcs", arcs.stream().map(Arc::vertices).toArray(IList[]::new));
    edgeCounts(arcs).forEach(e -> System.out.println(VERTICES.indexOf(e.key()) + " = " + e.value()));
    VERTICES.forEach(v -> System.out.println(VERTICES.indexOf(v) + " " + v));
    //*/

    ISet<Vec2> dangling = edgeCounts(arcs).stream()
      .filter(e -> e.value() % 2 == 1)
      .map(IEntry::key)
      .collect(Sets.linearCollector());
    if (dangling.size() > 0) {
      IGraph<Vec2, Arc> allArcs = new DirectedGraph<Vec2, Arc>().linear();
      pa.concat(pb).forEach(arc -> allArcs.link(arc.head(), arc.tail(), arc, (x, y) -> x.length() < y.length() ? x : y));

      //describe("dangling", dangling.elements());
      //describe("allEdges", List.from(allArcs.edges()).stream().map(e -> e.value().vertices()).toArray(IList[]::new));

      while (dangling.size() > 0) {
        IList<Vec2> path = Graphs.shortestPath(allArcs, dangling, dangling::contains, Arc::length).get();
        dangling.remove(path.first()).remove(path.last());

        for (Arc arc : edges(path, allArcs::edge)) {
          if (arcs.contains(arc)) {
            //describe("removing", arc.vertices());
            arcs.remove(arc);
          } else {
            //describe("adding", arc.vertices());
            arcs.add(arc);
          }
        }
      }
    }

    IList<Ring2> result = new LinearList<>();

    IGraph<Vec2, ISet<Arc>> graph = new DirectedGraph<Vec2, ISet<Arc>>().linear();
    arcs.forEach(arc -> graph.link(arc.head(), arc.tail(), LinearSet.of(arc), ISet::union));

    //describe("arcs", arcs.stream().map(Arc::vertices).toArray(IList[]::new));
    //describe("graph", List.from(graph.edges()).stream().map(e -> List.of(e.from(), e.to())).toArray(IList[]::new));

    for (Set<Vec2> component : Graphs.stronglyConnectedComponents(graph, true)) {
      //describe("component", component.elements());
      IGraph<Vec2, ISet<Arc>> subgraph = graph.select(component).linear();

      for (;;) {
        IList<Arc> largestCycle = Graphs.cycles(subgraph).stream()
          .map(cycle -> edges(cycle, (x, y) -> subgraph.edge(x, y).elements()))
          .flatMap(arcSets -> permutations(arcSets).stream())
          .max(Comparator.comparingDouble(path -> abs(path.stream().mapToDouble(Arc::signedArea).sum())))
          .orElse(null);

        if (largestCycle == null) {
          break;
        }

        //describe("cycle", largestCycle.stream().map(Arc::vertices).toArray(IList[]::new));
        result.addLast(ring(largestCycle));

        for (Arc arc : largestCycle) {
          ISet<Arc> edge = subgraph.edge(arc.head(), arc.tail()).remove(arc);
          if (edge.size() == 0) {
            subgraph.unlink(arc.head(), arc.tail());
          } else {
            subgraph.link(arc.head(), arc.tail(), edge);
          }
        }
      }
    }


    return new Region2(result);

  }

  ///

  public static Region2 union(Region2 a, Region2 b) {
    return operation(a, b,
      Operation.UNION,
      t -> t == Type.OUTSIDE || t == Type.SAME_EDGE,
      t -> t == Type.OUTSIDE);
  }

  public static Region2 intersection(Region2 a, Region2 b) {
    return operation(a, b,
      Operation.INTERSECTION,
      t -> t == Type.INSIDE || t == Type.SAME_EDGE,
      t -> t == Type.INSIDE);
  }

  public static Region2 difference(Region2 a, Region2 b) {
    return operation(a, b,
      Operation.DIFFERENCE,
      t -> t == Type.OUTSIDE || t == Type.DIFF_EDGE,
      t -> t == Type.INSIDE);
  }

}
