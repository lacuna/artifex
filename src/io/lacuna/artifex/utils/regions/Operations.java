package io.lacuna.artifex.utils.regions;

import io.lacuna.artifex.Curve2;
import io.lacuna.artifex.Region2.Ring;
import io.lacuna.artifex.Vec2;
import io.lacuna.artifex.utils.DoubleAccumulator;
import io.lacuna.artifex.utils.EdgeList;
import io.lacuna.artifex.utils.SweepQueue;
import io.lacuna.bifurcan.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;

import static io.lacuna.artifex.Vec.dot;

/**
 * @author ztellman
 */
public class Operations {

  public static final int OUT_A = 1, IN_A = 2, OUT_B = 4, IN_B = 8;

  private static class Intersection {
    public final double t;
    public final Vec2 p;
    public final boolean collinear;

    public Intersection(Vec2 p, double t, boolean collinear) {
      this.t = t;
      this.p = p;
      this.collinear = collinear;
    }

    public Intersection merge(Intersection i) {
      assert t == i.t;
      return new Intersection(i.p, t, collinear || i.collinear);
    }
  }

  private static class Overlay {
    private final EdgeList list = new EdgeList();
    private final SweepQueue<Curve2>[] queues = new SweepQueue[]{new SweepQueue(), new SweepQueue()};
    private final IMap<Curve2, DoubleAccumulator> lower = new LinearMap<>();
    private final IMap<Curve2, IList<Intersection>> upper = new LinearMap<>();

    private Overlay(Iterable<Ring> a, Iterable<Ring> b) {
      for (Ring r : a) {
        for (Curve2 c : r.curves) {
          add(0, c);
        }
      }

      for (Ring r : b) {
        for (Curve2 c : r.curves) {
          add(1, c);
        }
      }

      sweep();
      populate();
    }

    static EdgeList overlay(Iterable<Ring> a, Iterable<Ring> b) {
      return new Overlay(a, b).list;
    }

    // find all intersections
    private void sweep() {
      Curve2[] cs = new Curve2[2];
      for (; ; ) {
        int idx = SweepQueue.next(queues);
        cs[idx] = queues[idx].take();

        if (cs[idx] == null) {
          break;
        }

        if (idx == 0) {
          lower.put(cs[0], new DoubleAccumulator());
        } else {
          upper.put(cs[1], new LinearList<>());
        }

        for (Curve2 c : queues[1 - idx].active()) {
          cs[1 - idx] = c;
          double[] ts = cs[0].intersections(cs[1]);
          if (ts.length > 0) {
            for (int i = 0; i < ts.length; i += 2) {
              lower.get(cs[0]).get().add(ts[i]);
              upper.get(cs[1]).get().addLast(new Intersection(cs[0].position(ts[i]), ts[i + 1], ts.length > 2));
            }
          }
        }
      }
    }

    // split the edges and add them to the list
    private void populate() {
      // split the lower edges at all intersection points
      lower.stream()
        .flatMap(e -> Arrays.stream(e.key().split(e.value().toArray())))
        .forEach(c -> list.add(c, IN_A, OUT_A));

      // split the upper edges at all intersection points, and snap them to the lower vertices
      upper.stream()
        .flatMap(e -> split(e.key(), e.value()).stream())
        .forEach(c -> list.add(c, IN_B, OUT_B));
    }

    private void add(int idx, Curve2 c) {
      queues[idx].add(c, c.start().x, c.end().x);
    }

    private IList<Curve2> split(Curve2 c, IList<Intersection> is) {
      if (is.size() == 0) {
        return LinearList.of(c);
      }

      LinearList<Intersection> acc = new LinearList<>();
      for (Intersection i : Lists.sort(is, Comparator.comparingDouble(i -> i.t))) {
        if (acc.size() > 0 && i.t == acc.last().t) {
          acc.addLast(acc.popLast().merge(i));
        } else {
          acc.addLast(i);
        }
      }

      if (acc.first().t > 0) {
        acc.addFirst(new Intersection(c.start(), 0, false));
      }

      if (acc.last().t < 1) {
        acc.addLast(new Intersection(c.end(), 1, false));
      }

      IList<Curve2> result = new LinearList<>();
      for (int i = 1; i < acc.size(); i++) {
        Intersection prev = acc.nth(i - 1);
        Intersection curr = acc.nth(i);
        Curve2[] cs = c.split((curr.t - prev.t) / (1 - prev.t));

        if (prev.collinear && curr.collinear) {
          // anything to do here?
        } else {
          result.addLast(cs[0].endpoints(prev.p, curr.p));
        }

        c = cs.length > 1 ? cs[1] : null;
      }

      return result;
    }


  }

  private static Ring parent(Ring ring, IList<Ring> candidates) {
    return candidates.stream()
      .filter(r -> r.contains(ring.curves[0].start()))
      .findFirst()
      .orElse(null);
  }


  private static IMap<Ring, Ring> hierarchy(IMap<Ring, Integer> rings) {
    IList<Ring> sorted = Lists.sort(rings.keys().elements(), Comparator.comparingDouble(r -> r.area));
    IMap<Ring, Ring> result = new LinearMap<>();

    // TODO: make this better than quadratic
    for (int i = 0; i < sorted.size(); i++) {
      Ring a = sorted.nth(i);
      Vec2 p = a.curves[0].start();
      int flag = rings.get(a).get();
      if (disjoint(flag)) {
        result.put(a, null);
        for (int j = i + 1; j < sorted.size(); j++) {
          Ring b = sorted.nth(j);
          if (sets(flag) != sets(rings.get(b).get()) && b.contains(p)) {
            result.put(a, b);
            break;
          }
        }
      }
    }

    return result;
  }

  public static IMap<Ring, Integer> overlay(Iterable<Ring> a, Iterable<Ring> b) {
    return Overlay.overlay(a, b).rings();
  }

  public static boolean disjoint(int flag) {
    int s = sets(flag);
    return (s & 1) != (s >> 1);
  }

  public static boolean disjoint(int a, int b) {
    return sets(a) != sets(b);
  }

  public static int sets(int flag) {
    return ((flag | (flag >> 1)) & 1) | ((flag >> 1 | flag >> 2) & 2);
  }

  public static IList<Ring> union(Iterable<Ring> a, Iterable<Ring> b) {
    IMap<Ring, Integer> rings = Overlay.overlay(a, b).boundaries(i -> (i & (IN_A | IN_B)) == 0);
    IMap<Ring, Ring> parents = hierarchy(rings);

    for (IEntry<Ring, Ring> e : parents) {
      Ring child = e.key();
      Ring parent = e.value();

      if (parent == null) {

        // redundant ring
      } else if (child.isClockwise == parent.isClockwise) {
        rings.remove(child);

        // overriden by parent
      } else if (!parent.isClockwise && disjoint(rings.get(child).get(), rings.get(parent).get())) {
        rings.remove(child);
      }
    }

    return rings.keys().elements();
  }


  public static IList<Ring> intersection(Iterable<Ring> a, Iterable<Ring> b) {
    EdgeList list = Overlay.overlay(a, b);
    list.removeFaces(i -> i == (IN_A | OUT_B) || i == (OUT_A | IN_B), OUT_B | OUT_A);
    IMap<Ring, Integer> rings = list.boundaries(i -> (i & (IN_A | IN_B)) == 0);
    IMap<Ring, Ring> parents = hierarchy(rings);

    for (IEntry<Ring, Ring> e : parents) {
      Ring child = e.key();
      Ring parent = e.value();

      if (parent == null || (rings.get(child).get() | rings.get(parent).get()) != (IN_A | IN_B)) {
        rings.remove(child);
      }
    }

    return rings.keys().elements();
  }


  public static IList<Ring> difference(Iterable<Ring> a, Iterable<Ring> b) {
    EdgeList list = Overlay.overlay(a, b);
    list.removeFaces(i -> (i & IN_B) > 0, OUT_B | OUT_A);
    IMap<Ring, Integer> rings = list.boundaries(i -> (i & (IN_A | IN_B)) == 0);
    IMap<Ring, Ring> parents = hierarchy(rings);

    for (IEntry<Ring, Ring> e : parents) {
      Ring child = e.key();
      Ring parent = e.value();

      if (parent == null) {
        if (child.isClockwise) {
          rings.remove(child);
        }

        // redundant ring
      } else if (child.isClockwise != parent.isClockwise) {
        rings.remove(child);

        // overriden by parent
      } else if (parent.isClockwise && (rings.get(parent).get() & IN_B) > 0) {
        rings.remove(child);
      }
    }

    return rings.keys().elements();
  }

}
