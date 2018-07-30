package io.lacuna.artifex.utils.regions;

import io.lacuna.artifex.Curve2;
import io.lacuna.artifex.Ring2;
import io.lacuna.artifex.Vec2;
import io.lacuna.artifex.utils.DoubleAccumulator;
import io.lacuna.artifex.utils.SweepQueue;
import io.lacuna.bifurcan.IMap;
import io.lacuna.bifurcan.LinearMap;

import java.util.Arrays;

/**
 * Given a ring, returns one or more rings split at points of self-intersection.
 */
public class Simplify {

  public static final double SIMPLIFY_EPSILON = 1e-9;

  private final Curve2[] curves;
  private final SweepQueue<Integer> queue = new SweepQueue<>();
  private final IMap<Vec2, Vec2> parent = new LinearMap<>();
  private final IMap<Curve2, DoubleAccumulator> intersections = new LinearMap<>();

  private Simplify(Ring2 ring) {
    this.curves = ring.curves;
    for (int i = 0; i < curves.length; i++) {
      Curve2 c = curves[i];
      queue.add(i, c.start().x, c.end().x);
    }

    if (!sweep()) {

    }
    populate();
  }

  // find all intersections
  private boolean sweep() {
    boolean intersection = false;

    for (; ; ) {
      Integer next = queue.take();
      if (next == null) {
        break;
      }

      int i = next;
      Curve2 a = curves[i];
      for (int j : queue.active()) {
        if (i == j) {
          continue;
        }

        Curve2 b = curves[j];
        Vec2[] ts = a.intersections(b, SIMPLIFY_EPSILON);
        for (int k = 0; k < ts.length; k++) {
          double t0 = ts[k].x;
          double t1 = ts[k].y;
          if ((i == j - 1 && t0 == 1) || (i == j + 1 && t0 == 0)) {
            continue;
          }

          intersection = true;
          intersections.getOrCreate(a, DoubleAccumulator::new).add(t0);
          intersections.getOrCreate(b, DoubleAccumulator::new).add(t1);
          join(a.position(t0), b.position(t1));
        }
      }
    }

    return intersection;
  }

  private DoubleAccumulator dedupe(Curve2 c, DoubleAccumulator acc) {
    if (acc.size() < 2) {
      return acc;
    }

    double[] ts = acc.toArray();
    Arrays.sort(ts);

    DoubleAccumulator result = new DoubleAccumulator();
    result.add(ts[0]);
    for (int i = 1; i < ts.length; i++) {
      double t0 = result.last();
      double t1 = ts[i];
      if (t0 + SIMPLIFY_EPSILON > t1) {
        join(c.position(t0), c.position(t1));
      } else {
        result.add(t1);
      }
    }

    return result;
  }

  private void join(Vec2 a, Vec2 b) {
    int cmp = a.compareTo(b);
    if (cmp < 0) {
      parent.put(b, a);
    } else if (cmp > 0) {
      parent.put(a, b);
    }
  }

  private Vec2 parent(Vec2 p) {
    for (; ; ) {
      Vec2 next = parent.get(p, null);
      if (next == null) {
        return p;
      }
      p = next;
    }
  }

  // split the edges and add them to the list
  private void populate() {
    IMap<Curve2, DoubleAccumulator> intersections = this.intersections.mapValues(this::dedupe);


  }
}