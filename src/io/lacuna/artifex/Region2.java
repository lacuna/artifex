package io.lacuna.artifex;

import io.lacuna.artifex.utils.PlaneSweep;
import io.lacuna.artifex.utils.PlaneSweep.Intersection;
import io.lacuna.bifurcan.*;

import java.util.Arrays;
import java.util.stream.Collectors;

import static io.lacuna.artifex.Vec.vec;

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
          signedArea += (j.x - i.x) * (j.y + i.y);
        }
      }

      this.isClockwise = signedArea < 0;
      this.bounds = bounds;
      this.curves = curves.update(curves.size() - 1, c -> c.end(curves.first().start())).toArray(Curve2.class);
    }

    public Ring reverse() {
      return new Ring(
        LinearList.from(Lists.reverse(Lists.lazyMap(Lists.from(curves), Curve2::reverse))).toArray(Curve2.class),
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

    public Polygon2.Ring subdivide(double error) {
      IList<Vec2> result = new LinearList<>();
      for (Curve2 c : curves) {
        Vec2[] vertices = c.subdivide(error);
        for (int i = 1; i < vertices.length; i++) {
          result.addLast(vertices[i]);
        }
      }
      return new Polygon2.Ring(result);
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

  private final Ring perimeter;
  private final IList<Ring> holes;

  Region2(Ring perimeter, IList<Ring> holes) {
    this.perimeter = perimeter;
    this.holes = holes;
  }

  public Polygon2 subdivide(double error) {
    return new Polygon2(
      perimeter.subdivide(error),
      holes.stream().map(r -> r.subdivide(error)).collect(Lists.linearCollector()));
  }

}
