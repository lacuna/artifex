package io.lacuna.artifex.utils.regions;

import io.lacuna.artifex.Region2.Ring;
import io.lacuna.artifex.Vec2;
import io.lacuna.artifex.utils.EdgeList;
import io.lacuna.artifex.utils.EdgeList.HalfEdge;
import io.lacuna.bifurcan.*;

import java.util.Comparator;

import static io.lacuna.artifex.Region2.Location.*;
import static io.lacuna.artifex.Vec.dot;
import static io.lacuna.artifex.utils.regions.Overlay.*;

/**
 * @author ztellman
 */
public class Operations {

  public static final int REMOVED = 16;

  private static IMap<Ring, Ring> hierarchy(IList<Ring> rings) {
    IList<Ring> sorted = Lists.sort(rings, Comparator.comparingDouble(r -> r.area));
    IMap<Ring, Ring> result = new LinearMap<>();

    // TODO: make this better than quadratic
    for (int i = 0; i < sorted.size(); i++) {
      Ring a = sorted.nth(i);
      Vec2 p = a.curves[0].start();

      for (int j = i + 1; j < sorted.size(); j++) {
        Ring b = sorted.nth(j);
        if (b.test(p) == INSIDE) {
          result.put(a, b);
          break;
        }
      }
    }

    return result;
  }

  public static IMap<Ring, Integer> unmingledRings(EdgeList list) {
    IMap<Ring, Integer> result = new LinearMap<>();
    for (HalfEdge e : list.faces()) {
      if (e.flag == IN_A || e.flag == IN_B) {
        result.put(list.ring(e), e.flag);
        list.removeFace(e, n -> n == e.flag >> 1, REMOVED);
      }
    }
    return result;
  }

  public static boolean isUnmingled(int flag) {
    int s = sets(flag);
    return (s & 1) != (s >> 1);
  }

  public static int sets(int flag) {
    return ((flag | (flag >> 1)) & 1) | ((flag >> 1 | flag >> 2) & 2);
  }

  public static IList<Ring> union(Iterable<Ring> a, Iterable<Ring> b) {
    EdgeList list = overlay(a, b);
    IList<Ring> rings = list.boundaries(i -> (i & (IN_A | IN_B)) == 0);
    IMap<Ring, Ring> hierarchy = hierarchy(rings);

    return rings.stream()
      .filter(r -> {
        Ring parent = hierarchy.get(r, null);
        return parent == null || r.isClockwise != parent.isClockwise;
      }).collect(Lists.linearCollector());
  }


  public static IList<Ring> intersection(Iterable<Ring> a, Iterable<Ring> b) {
    EdgeList list = overlay(a, b);
    IMap<Ring, Integer> unmingled = unmingledRings(list);
    list.removeFaces(i -> i != (IN_A | IN_B), OUT_A | OUT_B);
    IList<Ring> mingled = list.boundaries(i -> (i & (OUT_A | OUT_B)) != 0);
    IMap<Ring, Ring> hierarchy = hierarchy(Lists.concat(mingled, unmingled.keys().elements()));

    IList<Ring> result = LinearList.from(mingled);
    for (IEntry<Ring, Integer> e : unmingled) {
      Ring r = e.key();
      int flag = e.value();
      if (!r.isClockwise) {
        Ring parent = hierarchy.get(r, null);
        if (parent != null
          && !parent.isClockwise
          && (flag | unmingled.get(parent, 0)) == (IN_A | IN_B)) {
          result.addLast(r);
        }
      }
    }

    return result;
  }


  public static IList<Ring> difference(Iterable<Ring> a, Iterable<Ring> b) {
    EdgeList list = overlay(a, b);
    IMap<Ring, Integer> unmingled = unmingledRings(list);
    list.removeFaces(i -> i != (IN_A | OUT_B), OUT_A | OUT_B);
    IList<Ring> mingled = list.boundaries(i -> i == (OUT_A | OUT_B));
    IMap<Ring, Ring> hierarchy = hierarchy(Lists.concat(mingled, unmingled.keys().elements()));

    IList<Ring> result = LinearList.from(mingled);
    for (IEntry<Ring, Integer> e : unmingled) {
      Ring r = e.key();
      int flag = e.value();
      Ring parent = hierarchy.get(r, null);


      if (flag == IN_A) {

        // make sure A is not in B
        if (parent == null
          || parent.isClockwise
          || unmingled.get(parent, 0) != IN_B) {
          result.addLast(r);
        }

        // if B is in A, add it as a hole
      } else if (!r.isClockwise
        && parent != null
        && !parent.isClockwise
        && unmingled.get(parent, 0) != IN_B) {

        result.addLast(r.reverse());
      }
    }

    return result;
  }

}
