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

    private Ring(Curve2[] curves, Box2 bounds, boolean isClockwise) {
      this.curves = curves;
      this.bounds = bounds;
      this.isClockwise = isClockwise;
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
      this.bounds = bounds;
      this.curves = curves.update(curves.size() - 1, c -> c.end(curves.first().start())).toArray(Curve2[]::new);
    }

    public Ring reverse() {
      return new Ring(
        LinearList.from(Lists.reverse(Lists.lazyMap(Lists.from(curves), Curve2::reverse))).toArray(Curve2[]::new),
        bounds,
        !isClockwise);
    }

    public boolean contains(Vec2 p) {
      if (!bounds.contains(p)) {
        return false;
      }

      LineSegment2 ray = LineSegment2.from(p, vec(bounds.ux + 1, p.y));
      return Arrays.stream(curves).filter(c -> c.intersects(ray)).count() % 2 == 1;
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
  private final LinearMap<Ring, IList<Ring>> subRegions = new LinearMap<>();

  public Region2(Ring perimeter, Iterable<Ring> holes) {
    subRegions.put(
      perimeter.isClockwise ? perimeter.reverse() : perimeter,
      toStream(holes.iterator()).map(r -> r.isClockwise ? r : r.reverse()).collect(Lists.linearCollector()));
  }

  public EdgeList edgeList() {
    return EdgeList.from(subRegions);
  }

  public Fan2[] fans() {
    return EdgeList.from(subRegions).fans().toArray(Fan2[]::new);
  }
}
