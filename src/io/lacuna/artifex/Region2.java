package io.lacuna.artifex;

import io.lacuna.artifex.utils.EdgeList;
import io.lacuna.artifex.utils.Scalars;
import io.lacuna.artifex.utils.regions.Hulls;
import io.lacuna.artifex.utils.regions.Operations;
import io.lacuna.bifurcan.*;

import java.util.Arrays;
import java.util.Comparator;

import static io.lacuna.artifex.Vec.vec;
import static io.lacuna.bifurcan.utils.Iterators.toStream;

/**
 * @author ztellman
 */
public class Region2 {

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
      this.area = Math.abs(signedArea);
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

    public boolean contains(Vec2 p) {
      if (!bounds.contains(p)) {
        return false;
      }

      LineSegment2 ray = LineSegment2.from(p, vec(bounds.ux + 1, p.y));
      int count = 0;
      for (Curve2 c : curves) {
        Box2 bounds = c.bounds();
        if (bounds.contains(p)) {
          count += c.intersections(ray).length / 2;
        } else if (bounds.lx > p.x) {
          count += Scalars.inside(bounds.ly, p.y, bounds.uy) ? 1 : 0;
        }
      }

      return count % 2 == 1;
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

  public static Region2 square() {
    return Box.box(vec(0, 0), vec(1, 1)).region();
  }

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

  public boolean contains(Vec2 p) {
    for (Ring r : rings) {
      if (r.contains(p)) {
        return !r.isClockwise;
      }
    }

    return false;
  }

  ///

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

}
