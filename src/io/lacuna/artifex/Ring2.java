package io.lacuna.artifex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static io.lacuna.artifex.Vec2.cross;
import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * @author ztellman
 */
public class Ring2 {

  private final List<Curve2> curves;

  public Ring2(List<Curve2> curves) {
    this.curves = curves;
  }

  public Ring2 reverse() {
    return new Ring2(curves.stream().map(Curve2::reverse).collect(Collectors.toList()));
  }

  public List<Curve2> curves() {
    return curves;
  }

  public boolean inside(Vec2 p) {
    Curve2 curve = curves.get(0);

    double param = curve.nearestPoint(p);
    double clampedParam = min(1, max(0, param));
    Vec2 pos = curve.position(clampedParam);
    Vec2 dir = curve.direction(clampedParam).norm();
    Vec2 pSo = pos.sub(p);

    return cross(dir, pSo) > 0;
  }
}
