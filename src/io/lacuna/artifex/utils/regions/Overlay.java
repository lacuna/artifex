package io.lacuna.artifex.utils.regions;

import io.lacuna.artifex.Curve2;
import io.lacuna.artifex.Ring2;
import io.lacuna.artifex.Vec;
import io.lacuna.artifex.Vec2;
import io.lacuna.artifex.utils.DoubleAccumulator;
import io.lacuna.artifex.utils.EdgeList;
import io.lacuna.artifex.utils.EdgeList.HalfEdge;
import io.lacuna.artifex.utils.SweepQueue;
import io.lacuna.bifurcan.IMap;
import io.lacuna.bifurcan.LinearMap;

import java.util.Arrays;

import static java.lang.Math.max;

/**
 * Overlays one set of rings atop another, inserting intersection points where necessary.
 */
public class Overlay {

  public static final double PARAMETRIC_EPSILON = 1e-6;
  public static final double SPATIAL_EPSILON = 1e-6;

  public static final int OUT_A = 1, IN_A = 2, OUT_B = 4, IN_B = 8;

  private final Ring2[] a, b;

  private final EdgeList list = new EdgeList();
  private final SweepQueue<Curve2>[] queues = new SweepQueue[]{new SweepQueue(), new SweepQueue()};

  private final IMap<Vec2, Vec2> parent = new LinearMap<>();
  private final IMap<Curve2, DoubleAccumulator> lower = new LinearMap<>();
  private final IMap<Curve2, DoubleAccumulator> upper = new LinearMap<>();

  private Overlay(Ring2[] a, Ring2[] b) {
    this.a = a;
    this.b = b;

    for (Ring2 r : a) {
      for (Curve2 c : r.curves) {
        add(0, c);
      }
    }

    for (Ring2 r : b) {
      for (Curve2 c : r.curves) {
        add(1, c);
      }
    }

    sweep();
    populate();
  }

  public static EdgeList overlay(Ring2[] a, Ring2[] b) {
    //System.out.println("\noverlay");
    EdgeList result = new Overlay(a, b).list;

    /*for (HalfEdge edge : result.faces()) {
      if (edge.flag == IN_A || edge.flag == IN_B) {
        int flag = outerFlag(edge, edge.flag == IN_A ? (IN_B | OUT_B) : (IN_A | OUT_A));
        if (flag != 0) {
          edge.face().forEach(e -> e.flag |= flag);
        }
      }
    }*/

    return result;
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

      (idx == 0 ? lower : upper).put(cs[idx], new DoubleAccumulator());

      for (Curve2 c : queues[1 - idx].active()) {
        cs[1 - idx] = c;
        Vec2[] ts = cs[0].intersections(cs[1]);
        if (ts.length > 0) {
          //System.out.println("\n" + ts.length + " " + cs[0] + " " + cs[1]);
        }

        for (int i = 0; i < ts.length; i++) {
          //System.out.println(ts.length + " " + ts[i] + " " + cs[0].position(ts[i].x) + " " + cs[0].position(ts[i].x).sub(cs[1].position(ts[i].y)).length() + " " + ts[i].sub(ts[max(0, i - 1)]));

          double t0 = ts[i].x;
          double t1 = ts[i].y;

          // if it's at the very end, let the next curve handle everything
          if (t1 == 1) {
            //continue;
          }

          lower.get(cs[0]).get().add(t0);
          upper.get(cs[1]).get().add(t1);

          Vec2 p0 = cs[0].position(t0);
          Vec2 p1 = cs[1].position(t1);
          join(p0, p1);
        }
      }
    }
  }



  private DoubleAccumulator dedupe(Curve2 c, DoubleAccumulator acc) {

    double[] ts = acc.toArray();
    Arrays.sort(ts);

    DoubleAccumulator result = new DoubleAccumulator();
    for (int i = 0; i < ts.length; i++) {
      double t0 = result.size() == 0 ? 0 : result.last();
      double t1 = ts[i];
      if (t0 + PARAMETRIC_EPSILON > t1
        || Vec.equals(c.position(t0), c.position(t1), SPATIAL_EPSILON)) {
        join(c.position(t0), c.position(t1));

      } else if (t1 + PARAMETRIC_EPSILON > 1
        || Vec.equals(c.position(t1), c.end(), SPATIAL_EPSILON)) {
        join(c.position(t1), c.end());

      } else {
        result.add(t1);
      }
    }

    return result;
  }

  private void elide(Curve2 c) {
    Vec2 pa = parent(c.start());
    Vec2 pb = parent(c.end());
    if (Vec.equals(pa, pb, SPATIAL_EPSILON)) {
      join(pa, pb);
    }
  }

  private void join(Vec2 a, Vec2 b) {
    a = parent(a);
    b = parent(b);
    int cmp = a.compareTo(b);
    if (cmp < 0) {
      parent.put(b, a);
    } else if (cmp > 0) {
      parent.put(a, b);
    }
  }

  private Vec2 parent(Vec2 p) {
    Vec2 curr = p;
    for (; ; ) {
      Vec2 next = parent.get(curr, null);
      if (next == null) {
        if (!curr.equals(p)) {
          parent.put(p, curr);
        }
        return curr;
      }
      curr = next;
    }
  }

  // split the edges and add them to the list
  private void populate() {

    IMap<Curve2, DoubleAccumulator> lower = this.lower.mapValues(this::dedupe);
    IMap<Curve2, DoubleAccumulator> upper = this.upper.mapValues(this::dedupe);

    lower.keys().forEach(this::elide);
    upper.keys().forEach(this::elide);

    for (int i = 0; i < 2; i++) {
      IMap<Curve2, DoubleAccumulator> intersections = i == 0 ? lower : upper;
      int inFlag = i == 0 ? IN_A : IN_B;
      int outFlag = i == 0 ? OUT_A : OUT_B;

      for (Ring2 r : i == 0 ? a : b) {
        for (Curve2 c : r.curves) {
          DoubleAccumulator acc = intersections.get(c, null);
          if (acc == null) {
            add(c, inFlag, outFlag);
          } else {
            for (Curve2 d : c.split(acc.toArray())) {
              add(d, inFlag, outFlag);
            }
          }
        }
      }
    }
  }

  private void add(Curve2 c, int inFlag, int outFlag) {
    Vec2 p0 = parent(c.start());
    Vec2 p1 = parent(c.end());
    if (!p0.equals(p1)) {
      list.add(c.endpoints(p0, p1), inFlag, outFlag);
    }
  }

  private void add(int idx, Curve2 c) {
    queues[idx].add(c, c.start().x, c.end().x);
  }
}