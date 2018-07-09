package io.lacuna.artifex.utils.regions;

import io.lacuna.artifex.Curve2;
import io.lacuna.artifex.Region2;
import io.lacuna.artifex.Vec;
import io.lacuna.artifex.Vec2;
import io.lacuna.artifex.utils.DoubleAccumulator;
import io.lacuna.artifex.utils.EdgeList;
import io.lacuna.artifex.utils.SweepQueue;
import io.lacuna.bifurcan.IEntry;
import io.lacuna.bifurcan.IMap;
import io.lacuna.bifurcan.LinearMap;

import static io.lacuna.artifex.Curve2.SPLIT_EPSILON;
import static io.lacuna.artifex.utils.Scalars.EPSILON;

/**
 * Overlays one set of rings atop another, inserting intersection points where necessary.
 */
public class Overlay {

  public static final int OUT_A = 1, IN_A = 2, OUT_B = 4, IN_B = 8;

  private final EdgeList list = new EdgeList();
  private final SweepQueue<Curve2>[] queues = new SweepQueue[]{new SweepQueue(), new SweepQueue()};
  private final IMap<Vec2, Vec2> snapTo = new LinearMap<>();
  private final IMap<Curve2, DoubleAccumulator> lower = new LinearMap<>();
  private final IMap<Curve2, DoubleAccumulator> upper = new LinearMap<>();

  private Overlay(Iterable<Region2.Ring> a, Iterable<Region2.Ring> b) {
    for (Region2.Ring r : a) {
      for (Curve2 c : r.curves) {
        add(0, c);
      }
    }

    for (Region2.Ring r : b) {
      for (Curve2 c : r.curves) {
        add(1, c);
      }
    }

    sweep();
    populate();
  }

  public static EdgeList overlay(Iterable<Region2.Ring> a, Iterable<Region2.Ring> b) {
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
        upper.put(cs[1], new DoubleAccumulator());
      }

      for (Curve2 c : queues[1 - idx].active()) {
        cs[1 - idx] = c;
        double[] ts = cs[0].intersections(cs[1]);

        for (int i = 0; i < ts.length; i += 2) {
          double t0 = ts[i];
          double t1 = ts[i + 1];

          // if it's at the very end, let the next curve handle everything
          if (t1 == 1) {
            continue;
          }

          lower.get(cs[0]).get().add(t0);
          Vec2 p = cs[0].position(t0);
          upper.get(cs[1]).get().add(t1);
          snapTo.put(cs[1].position(t1), p);

          if (!Vec.equals(cs[1].position(t1), p, SPLIT_EPSILON)) {
            System.out.println(cs[1].position(t1) + " " + p);
            throw new IllegalStateException();
          }
        }
      }
    }
  }

  // split the edges and add them to the list
  private void populate() {
    for (IEntry<Curve2, DoubleAccumulator> e : lower) {
      for (Curve2 c : e.key().split(e.value().toArray())) {
        list.add(c, IN_A, OUT_A);
      }
    }

    for (IEntry<Curve2, DoubleAccumulator> e : upper) {
      for (Curve2 c : e.key().split(e.value().toArray())) {
        Vec2 p0 = c.start();
        Vec2 p1 = c.end();

        Vec2 q0 = snapTo.get(p0, p0);
        Vec2 q1 = snapTo.get(p1, p1);
        if (!q0.equals(q1)) {
          list.add(c.endpoints(snapTo.get(p0, p0), snapTo.get(p1, p1)), IN_B, OUT_B);
        }
      }
    }
  }

  private void add(int idx, Curve2 c) {
    queues[idx].add(c, c.start().x, c.end().x);
  }
}