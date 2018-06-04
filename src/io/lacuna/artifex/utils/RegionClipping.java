package io.lacuna.artifex.utils;

import io.lacuna.artifex.CollinearException;
import io.lacuna.artifex.Curve2;
import io.lacuna.artifex.Region2;
import io.lacuna.bifurcan.*;

/**
 * @author ztellman
 */
public class RegionClipping {

  private enum EdgeState {
    INSIDE,
    OUTSIDE,
    ON
  }

  private enum IntersectionType {
    NONE,
    EXIT,
    ENTRY
  }

  private static class Node {
    final Curve2 curve;
    double neighbor;
    IntersectionType type;

    public Node(Curve2 curve) {
      this.curve = curve;
      this.neighbor = -1;
      this.type = IntersectionType.NONE;
    }

    public Node(Curve2 curve, double neighbor) {
      this.curve = curve;
      this.neighbor = neighbor;
      this.type = type;
    }

    public boolean isIntersection() {
      return neighbor >= 0;
    }
  }

  private static EdgeState initialState(FloatMap<Node> nodes, Region2.Ring ring) {
    if (!nodes.first().value().isIntersection()) {
      return ring.contains(nodes.first().value().curve.start()) ? EdgeState.INSIDE : EdgeState.OUTSIDE;
    } else {
      return EdgeState.ON;
    }
  }

  private static FloatMap<Node>[] intersections(Region2.Ring a, Region2.Ring b) {
    IMap<Region2.Ring, FloatMap<Node>> result = new LinearMap<>();

    FloatMap<Node> aNodes = new FloatMap<Node>().linear();
    for (int i = 0; i < a.curves.length; i++) {
      aNodes.put(i, new Node(a.curves[i]));
    }

    FloatMap<Node> bNodes = new FloatMap<Node>().linear();
    for (int i = 0; i < b.curves.length; i++) {
      bNodes.put(i, new Node(b.curves[i]));
    }

    IList<PlaneSweep.Intersection<IEntry<Double, Node>>> intersections =
      PlaneSweep.intersections(
        aNodes.entries(),
        bNodes.entries(),
        e -> e.value().curve);

    for (PlaneSweep.Intersection<IEntry<Double, Node>> i : intersections) {
      try {
        Curve2 ca = i.a.value().curve;
        Curve2 cb = i.b.value().curve;
        double[] ts = ca.intersections(cb);
        double ka = i.a.key() + ts[0];
        double kb = i.b.key() + ts[1];

        aNodes.put(ka, new Node(ca, kb));
        bNodes.put(kb, new Node(cb, ka));
      } catch (CollinearException e) {
        // TODO
      }
    }

    return new FloatMap[] {aNodes.forked(), bNodes.forked()};
  }


}
