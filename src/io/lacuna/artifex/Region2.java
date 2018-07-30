package io.lacuna.artifex;

import io.lacuna.artifex.Ring2.Location;
import io.lacuna.artifex.utils.EdgeList;
import io.lacuna.artifex.utils.regions.Hulls;
import io.lacuna.artifex.utils.regions.Monotonic;
import io.lacuna.artifex.utils.regions.Operations;
import io.lacuna.artifex.utils.regions.Triangles;
import io.lacuna.bifurcan.LinearList;
import io.lacuna.bifurcan.Lists;

import java.util.Arrays;
import java.util.Comparator;

/**
 * @author ztellman
 */
public class Region2 {

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



  ///

  private final Ring2[] rings;
  private final Box2 bounds;

  public Region2(Iterable<Ring2> rings) {
    this(LinearList.from(rings).toArray(Ring2[]::new));
  }

  public Region2(Ring2[] rings) {
    this.rings = rings.clone();
    Arrays.sort(this.rings, Comparator.comparingDouble(r -> r.area));

    this.bounds = Arrays.stream(this.rings)
      .map(r -> r.bounds)
      .reduce(Box2.EMPTY, Box2::union);
  }

  ///

  public Ring2[] rings() {
    return rings;
  }

  public static Region2 of(Ring2... rings) {
    return new Region2(Lists.from(rings));
  }

  public Box2 bounds() {
    return bounds;
  }

  public Location test(Vec2 p) {
    for (Ring2 r : rings) {
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
    return new Region2(Arrays.stream(rings).map(r -> r.transform(m)).toArray(Ring2[]::new));
  }

  public Region2 intersection(Region2 region) {
    return Operations.intersection(this, region);
  }

  public Region2 union(Region2 region) {
    return Operations.union(this, region);
  }

  public Region2 difference(Region2 region) {
    return Operations.difference(this, region);
  }

  /// triangulation

  public Ring2[] triangulated() {
    EdgeList l = new EdgeList();
    for (Ring2 r : rings) {
      for (Curve2 c : r.curves) {
        l.add(c, Hulls.INSIDE, Hulls.OUTSIDE);
      }
    }

    Hulls.create(l);
    Monotonic.monotonize(l);
    Triangles.triangulate(l);

    return l.faces().stream()
      .map(EdgeList.HalfEdge::face)
      .map(LinearList::from)
      .map(edges -> Ring2.of(edges.stream().map(e -> e.curve).toArray(Curve2[]::new)))
      .toArray(Ring2[]::new);
  }

}
