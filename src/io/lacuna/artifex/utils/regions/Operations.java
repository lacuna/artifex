package io.lacuna.artifex.utils.regions;

import io.lacuna.artifex.Curve2;
import io.lacuna.artifex.Region2;
import io.lacuna.artifex.Ring2;
import io.lacuna.artifex.Ring2.Location;
import io.lacuna.artifex.utils.EdgeList;
import io.lacuna.artifex.utils.EdgeList.HalfEdge;
import io.lacuna.bifurcan.*;

import java.util.Comparator;
import java.util.function.IntPredicate;

import static io.lacuna.artifex.utils.regions.Overlay.*;

/**
 * @author ztellman
 */
public class Operations {

  private static boolean contains(Ring2 a, Ring2 b) {
    for (Curve2 c : b.curves) {
      Location l = a.test(c.start());
      if (l == Location.INSIDE) {
        return true;
      } else if (l == Location.OUTSIDE) {
        return false;
      }
    }
    return true;
  }

  private static Ring2 parent(Ring2 ring, Region2 region) {
    for (Ring2 r : region.rings()) {
      if (r.area >= ring.area && contains(r, ring)) {
        return r;
      }
    }
    return null;
  }

  private static IMap<Ring2, Ring2> hierarchy(IList<Ring2> rings) {
    IList<Ring2> sorted = Lists.sort(rings, Comparator.comparingDouble(r -> r.area));
    IMap<Ring2, Ring2> result = new LinearMap<>();

    // TODO: make this better than quadratic
    for (int i = 0; i < sorted.size(); i++) {
      Ring2 a = sorted.nth(i);
      for (int j = i + 1; j < sorted.size(); j++) {
        Ring2 b = sorted.nth(j);
        if (contains(b, a)) {
          result.put(a, b);
          break;
        }
      }
    }

    return result;
  }

  private static IMap<Ring2, Integer> unmingledRing2s(EdgeList list) {
    IMap<Ring2, Integer> result = new LinearMap<>();
    for (HalfEdge e : list.faces()) {
      if (e.flag == IN_A || e.flag == IN_B) {
        result.put(list.ring(e), e.flag);
        list.removeFace(e, e.flag >> 1);
      }
    }
    return result;
  }

  public static EdgeList overlay(Region2 a, Region2 b) {
    return Overlay.overlay(a.rings(), b.rings());
  }

  public static Region2 union(Region2 a, Region2 b) {
    EdgeList list = overlay(a, b);
    //System.out.println(list.rings().values());
    IList<Ring2> rings = list.boundaries(i -> (i & (IN_A | IN_B)) == 0);
    IMap<Ring2, Ring2> hierarchy = hierarchy(rings);

    return new Region2(rings.stream()
      .filter(r -> {
        Ring2 parent = hierarchy.get(r, null);
        return parent == null || r.isClockwise != parent.isClockwise;
      }).toArray(Ring2[]::new));
  }


  public static Region2 intersection(Region2 a, Region2 b) {
    /*EdgeList list = overlay(a, b);
    IMap<Ring2, Integer> unmingled = unmingledRing2s(list);
    list.removeFaces(i -> i != (IN_A | IN_B), OUT_A | OUT_B);
    IList<Ring2> mingled = list.boundaries(i -> (i & (OUT_A | OUT_B)) != 0);
    IMap<Ring2, Ring2> hierarchy = hierarchy(Lists.concat(mingled, unmingled.keys().elements()));

    IList<Ring2> result = LinearList.from(mingled);
    for (IEntry<Ring2, Integer> e : unmingled) {
      Ring2 r = e.key();
      int flag = e.value();
      if (!r.isClockwise) {
        Ring2 parent = hierarchy.get(r, null);
        if (parent != null
          && !parent.isClockwise
          && (flag | unmingled.get(parent, 0)) == (IN_A | IN_B)) {
          result.addLast(r);
        }
      }
    }

    return result;*/

    return null;
  }


  public static Region2 difference(Region2 a, Region2 b) {
    /*EdgeList list = overlay(a, b);
    IMap<Ring2, Integer> unmingled = unmingledRing2s(list);
    list.removeFaces(i -> i != (IN_A | OUT_B), OUT_A | OUT_B);
    IList<Ring2> mingled = list.boundaries(i -> i == (OUT_A | OUT_B));
    IMap<Ring2, Ring2> hierarchy = hierarchy(Lists.concat(mingled, unmingled.keys().elements()));

    IList<Ring2> result = LinearList.from(mingled);
    for (IEntry<Ring2, Integer> e : unmingled) {
      Ring2 r = e.key();
      int flag = e.value();
      Ring2 parent = hierarchy.get(r, null);


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
  }*/

    return null;
  }

}
