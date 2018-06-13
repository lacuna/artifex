package io.lacuna.artifex;

import io.lacuna.artifex.utils.EdgeList;
import io.lacuna.artifex.utils.Triangulation;
import io.lacuna.bifurcan.*;
import io.lacuna.bifurcan.utils.Iterators;

import java.util.Arrays;
import java.util.stream.Collectors;

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
      LinearList<Curve2> curves = new LinearList<>();
      for (Curve2 a : cs) {
        for (Curve2 b : a.split(a.inflections())) {
          curves.addLast(b);
          bounds = bounds.union(b.start()).union(b.end());

          Vec2 i = b.start();
          Vec2 j = b.end();
          signedArea += (i.x * j.y) - (j.x * i.y);
        }
      }

      this.isClockwise = signedArea < 0;
      this.area = Math.abs(signedArea);
      this.bounds = bounds;
      this.curves = curves.update(curves.size() - 1, c -> c.end(curves.first().start())).toArray(Curve2[]::new);
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
      return Arrays.stream(curves).filter(c -> c.intersects(ray, true)).count() % 2 == 1;
    }
  }

  public static Ring ring(Iterable<Vec2> vertices) {
    LinearList<Vec2> vs = LinearList.from(vertices);
    LinearList<Curve2> segments = new LinearList<>();
    for (int i = 1; i < vs.size(); i++) {
      segments.addLast(LineSegment2.from(vs.nth(i - 1), vs.nth(i)));
    }
    return new Ring(segments);
  }

  // perimeters onto interior holes
  private final LinearMap<Ring, IList<Ring>> subRegions;

  private Region2(LinearMap<Ring, IList<Ring>> subRegions) {
    this.subRegions = subRegions;
  }

  public Region2(Ring perimeter, Iterable<Ring> holes) {
    this(new LinearMap<>());

    subRegions.put(
      perimeter.isClockwise ? perimeter.reverse() : perimeter,
      toStream(holes.iterator()).map(r -> r.isClockwise ? r : r.reverse()).collect(Lists.linearCollector()));
  }

  public static Region2 from(Iterable<Ring> boundaries) {
    LinearMap<Ring, IList<Ring>> subRegions = new LinearMap<>();

    for (Ring r : boundaries) {
      System.out.println(r.curves.length + " " + r.isClockwise);
      if (!r.isClockwise) {
        subRegions.put(r, new LinearList<>());
      }
    }

    for (Ring r : boundaries) {
      if (r.isClockwise) {
        boolean found = false;
        for (Ring p : subRegions.keys()) {
          if (p.contains(r.curves[0].start())) {
            subRegions.get(p).get().addLast(r);
            found = true;
            break;
          }
        }

        if (!found) {
          throw new IllegalArgumentException();
        }
      }
    }

    return new Region2(subRegions);
  }

  public Ring[] rings() {
    IList<Ring> result = new LinearList<>();
    subRegions.keys().forEach(result::addLast);
    subRegions.values().forEach(l -> l.forEach(result::addLast));

    return result.toArray(Ring[]::new);
  }

  public EdgeList edgeList() {
    return EdgeList.from(subRegions);
  }

  public Box2 bounds() {
    return subRegions.keys().stream()
      .map(r -> r.bounds)
      .reduce(Box2.EMPTY, Box2::union);
  }

}
