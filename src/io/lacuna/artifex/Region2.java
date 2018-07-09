package io.lacuna.artifex;

import io.lacuna.artifex.utils.EdgeList;
import io.lacuna.artifex.utils.regions.Hulls;
import io.lacuna.artifex.utils.regions.Monotonic;
import io.lacuna.artifex.utils.regions.Operations;
import io.lacuna.artifex.utils.regions.Triangles;
import io.lacuna.bifurcan.IList;
import io.lacuna.bifurcan.LinearList;
import io.lacuna.bifurcan.Lists;

import java.util.Arrays;
import java.util.Comparator;

import static io.lacuna.artifex.Curve2.SPLIT_EPSILON;
import static io.lacuna.artifex.Vec.vec;
import static io.lacuna.artifex.utils.Scalars.EPSILON;
import static java.lang.Math.abs;

/**
 * @author ztellman
 */
public class Region2 {

  public enum Location {
    INSIDE,
    OUTSIDE,
    EDGE
  }

  public static class Interior {
    public final Vec2 a, b, c;

    public Interior(Vec2 a, Vec2 b, Vec2 c) {
      this.a = a;
      this.b = b;
      this.c = c;
    }
  }

  public static class Exterior {
    public final Curve2 edge;
    public final Vec2 vertex;

    public Exterior(Curve2 edge, Vec2 vertex) {
      this.edge = edge;
      this.vertex = vertex;
    }

    public boolean isConvex() {
      return vertex == null;
    }
  }

  public static class Triangulation {
    public final Exterior[][] exteriors;
    public final Interior[][] interiors;

    public Triangulation(Exterior[][] exteriors, Interior[][] interiors) {
      this.exteriors = exteriors;
      this.interiors = interiors;
    }
  }

  public static class Ring {

    public final Curve2[] curves;
    public final Box2 bounds;
    public final boolean isClockwise;
    public final double area;

    private Ring(Curve2[] curves, Box2 bounds, boolean isClockwise, double area) {
      this.curves = curves;
      this.bounds = bounds;
      this.isClockwise = isClockwise;
      this.area = area;
    }

    public Ring(Iterable<Curve2> cs) {

      // TODO: dedupe collinear adjacent lines
      Box2 bounds = Box2.EMPTY;
      double signedArea = 0;
      LinearList<Curve2> list = new LinearList<>();
      for (Curve2 a : cs) {
        for (Curve2 b : a.split(a.inflections())) {
          list.addLast(b);
          bounds = bounds.union(b.start()).union(b.end());

          Vec2 i = b.start();
          Vec2 j = b.end();
          signedArea += (i.x * j.y) - (j.x * i.y);
        }
      }

      this.isClockwise = signedArea < 0;
      this.area = abs(signedArea);
      this.bounds = bounds;

      curves = list.toArray(Curve2[]::new);
      for (int i = 0; i < curves.length - 1; i++) {
        curves[i] = curves[i].endpoints(curves[i].start(), curves[i + 1].start());
      }
      int lastIdx = curves.length - 1;
      curves[lastIdx] = curves[lastIdx].endpoints(curves[lastIdx].start(), curves[0].start());
    }

    public static Ring of(Curve2... cs) {
      return new Ring(Lists.from(cs));
    }

    public Ring reverse() {
      return new Ring(
        LinearList.from(Lists.reverse(Lists.lazyMap(Lists.from(curves), Curve2::reverse))).toArray(Curve2[]::new),
        bounds,
        !isClockwise,
        area);
    }

    public Location test(Vec2 p) {
      if (!bounds.expand(EPSILON).contains(p)) {
        return Location.OUTSIDE;
      }

      LineSegment2 ray = LineSegment2.from(p, vec(bounds.ux + 1, p.y));
      int count = 0;

      // find effective predecessor of the first curve, ignoring all the flat lines
      Curve2 prev = null;
      for (int i = curves.length - 1; i >= 0; i--) {
        Curve2 c = curves[i];
        if (c.start().y != c.end().y) {
          prev = c;
          break;
        }
      }

      for (Curve2 curr : curves) {
        double[] ts = curr.intersections(ray);

        // if the ray starts on an edge, short-circuit
        for (int i = 1; i < ts.length; i += 2) {
          if (ts[i] == 0) {
            return Location.EDGE;
          }
        }

        if (ts.length == 2 && ts[0] == 0) {
          // if it's a '\/' or '/\' intersection at the vertex, undo the other time we've counted it
          if (Math.signum(prev.start().y - prev.end().y) != Math.signum(curr.end().y - curr.start().y)) {
            count++;
          }

          // if we're collinear, pretend like our neighbors are collapsed together
        } else if (ts.length > 2
          && abs(curr.start().y - curr.end().y) < SPLIT_EPSILON
          && abs(curr.direction(ts[0]).y) < SPLIT_EPSILON) {

        } else {

          for (int i = 0; i < ts.length; i += 2) {
            if (ts[i] < 1) {
              count++;
            }
          }
          prev = curr;
        }
      }

      return count % 2 == 1 ? Location.INSIDE : Location.OUTSIDE;
    }

    public Ring transform(Matrix3 m) {
      return new Ring(() -> Arrays.stream(curves).map(c -> c.transform(m)).iterator());
    }
  }

  ///

  private final IList<Ring> rings;
  private final Box2 bounds;

  public Region2(Iterable<Ring> rings) {
    // TODO: normalize the rings, to prevent self-intersection, etc.

    this.rings = Lists.sort(LinearList.from(rings), Comparator.comparingDouble(r -> r.area));
    this.bounds = this.rings.stream()
      .map(r -> r.bounds)
      .reduce(Box2.EMPTY, Box2::union);
  }

  /**
   * @return a unit square from [0, 0] to [1, 1]
   */
  public static Region2 square() {
    return Box.box(vec(0, 0), vec(1, 1)).region();
  }

  /**
   * @return a unit circle with radius of 1, centered at [0, 0]
   */
  public static Region2 circle() {
    // adapted from http://spencermortensen.com/articles/bezier-circle/
    double c = 0.551915024494;
    return Region2.of(
      Ring.of(
        Bezier2.bezier(vec(1, 0), vec(1, c), vec(c, 1), vec(0, 1)),
        Bezier2.bezier(vec(0, 1), vec(-c, 1), vec(-1, c), vec(-1, 0)),
        Bezier2.bezier(vec(-1, 0), vec(-1, -c), vec(-c, -1), vec(0, -1)),
        Bezier2.bezier(vec(0, -1), vec(c, -1), vec(1, -c), vec(1, 0))));
  }

  ///

  public Ring[] rings() {
    return rings.toArray(Ring[]::new);
  }

  public static Region2 of(Ring... rings) {
    return new Region2(Lists.from(rings));
  }

  public Box2 bounds() {
    return bounds;
  }

  public Location test(Vec2 p) {
    for (Ring r : rings) {
      Location loc = r.test(p);
      if (loc == Location.INSIDE) {
        return r.isClockwise ? Location.OUTSIDE : Location.INSIDE;
      } else if (loc == Location.EDGE) {
        return Location.EDGE;
      }
    }

    return Location.OUTSIDE;
  }

  public boolean contains(Vec2 p) {
    return test(p) != Location.OUTSIDE;
  }

  /// transforms and set operations

  public Region2 transform(Matrix3 m) {
    return new Region2(() -> rings.stream().map(r -> r.transform(m)).iterator());
  }

  public Region2 intersection(Region2 region) {
    return new Region2(Operations.intersection(rings, region.rings));
  }

  public Region2 union(Region2 region) {
    return new Region2(Operations.union(rings, region.rings));
  }

  public Region2 difference(Region2 region) {
    return new Region2(Operations.difference(rings, region.rings));
  }

  /// triangulation

  public Ring[] triangulated() {
    EdgeList l = new EdgeList();
    for (Ring r : rings) {
      for (Curve2 c : r.curves) {
        l.add(c, Hulls.INSIDE, Hulls.OUTSIDE);
      }
    }

    Hulls.create(l);
    Monotonic.monotonize(l);
    Triangles.triangulate(l);

    return l.faces().stream()
      .map(l::face)
      .map(LinearList::from)
      .map(edges -> Ring.of(edges.stream().map(e -> e.curve).toArray(Curve2[]::new)))
      .toArray(Ring[]::new);
  }

  public Fan2[] fans() {
    EdgeList l = new EdgeList();
    for (Ring r : rings) {
      for (Curve2 c : r.curves) {
        l.add(c, Hulls.INSIDE, Hulls.OUTSIDE);
      }
    }

    Hulls.create(l);
    Monotonic.monotonize(l);
    Triangles.triangulate(l);

    return Triangles.fans(l).toArray(Fan2[]::new);
  }

}
