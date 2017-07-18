package io.lacuna.artifex;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * @author ztellman
 */
public class CurveRing2 {

  private final List<Curve2> curves;
  private final Box2 bounds;

  public CurveRing2(List<Curve2> curves) {
    this.curves = curves.stream()
        .flatMap(c -> Arrays.stream(c.split(c.inflections())))
        .collect(Collectors.toList());

    bounds = curves.stream().map(c -> Box.from(c.start(), c.end())).reduce(Box2::union).get();
  }

  public CurveRing2 reverse() {
    return new CurveRing2(curves.stream().map(Curve2::reverse).collect(Collectors.toList()));
  }

  public List<Curve2> curves() {
    return curves;
  }

  public boolean inside(Vec2 p) {
    LinearSegment2 ray = new LinearSegment2(p, Vec.from(bounds.upper.x + 1, p.y));

    return curves.stream().filter(c -> c.intersections(ray).length > 0).count() % 2 == 1;
  }

  public Box2 bounds() {
    return bounds;
  }
}
