package io.lacuna.artifex;

import io.lacuna.bifurcan.LinearList;
import io.lacuna.bifurcan.Lists;

import java.util.Arrays;

import static io.lacuna.artifex.Vec.vec;
import static io.lacuna.artifex.utils.Scalars.EPSILON;
import static java.lang.Math.abs;
import static java.lang.Math.signum;

public class Ring2 {

  private static final double TEST_EPSILON = 1e-6;

  public enum Location {
    INSIDE,
    OUTSIDE,
    EDGE
  }

  public final Curve2[] curves;
  public final Box2 bounds;
  public final boolean isClockwise;
  public final double area;

  private Ring2(Curve2[] curves, Box2 bounds, boolean isClockwise, double area) {
    this.curves = curves;
    this.bounds = bounds;
    this.isClockwise = isClockwise;
    this.area = area;
  }

  public Ring2(Iterable<Curve2> cs) {

    // TODO: dedupe collinear adjacent lines
    Box2 bounds = Box2.EMPTY;
    double signedArea = 0;
    LinearList<Curve2> list = new LinearList<>();
    for (Curve2 a : cs) {
      for (Curve2 b : a.split(a.inflections())) {
        list.addLast(b);
        bounds = bounds.union(b.start()).union(b.end());
        signedArea += b.signedArea();

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

  public static Ring2 of(Curve2... cs) {
    return new Ring2(Lists.from(cs));
  }

  public Region2 region() {
    return new Region2(LinearList.of(this));
  }

  /**
   * @return a unit square from [0, 0] to [1, 1]
   */
  public static Ring2 square() {
    return Box.box(vec(0, 0), vec(1, 1)).outline();
  }

  /**
   * @return a unit circle with radius of 1, centered at [0, 0]
   */
  public static Ring2 circle() {
    // adapted from http://spencermortensen.com/articles/bezier-circle/
    double c = 0.551915024494;
    return Ring2.of(
        Bezier2.curve(vec(1, 0), vec(1, c), vec(c, 1), vec(0, 1)),
        Bezier2.curve(vec(0, 1), vec(-c, 1), vec(-1, c), vec(-1, 0)),
        Bezier2.curve(vec(-1, 0), vec(-1, -c), vec(-c, -1), vec(0, -1)),
        Bezier2.curve(vec(0, -1), vec(c, -1), vec(1, -c), vec(1, 0)));
  }

  public Path2 path() {
    return new Path2(this);
  }

  public Ring2 reverse() {
    return new Ring2(
      LinearList.from(Lists.reverse(Lists.lazyMap(Lists.from(curves), Curve2::reverse))).toArray(Curve2[]::new),
      bounds,
      !isClockwise,
      area);
  }

  private int verticalOrientation(Curve2 c) {
    double delta = c.end().y - c.start().y;
    return abs(delta) < EPSILON ? 0 : (int) signum(delta);
  }

  public Location test(Vec2 p) {
    if (!bounds.expand(TEST_EPSILON).contains(p)) {
      return Location.OUTSIDE;
    }

    Line2 ray = Line2.from(p, vec(bounds.ux + 1, p.y));
    int count = 0;

    // find effective predecessor of the first curve, ignoring all the horizontal lines
    Curve2 prev = null;
    for (int i = curves.length - 1; i >= 0; i--) {
      Curve2 c = curves[i];
      if (verticalOrientation(c) != 0) {
        prev = c;
        break;
      }
    }

    for (Curve2 curr : curves) {
      Vec2[] ts = curr.intersections(ray, TEST_EPSILON);

      // if the ray starts on an edge, short-circuit
      for (Vec2 i : ts) {
        //System.out.println(ts.length + " " + i);
        if (i.y == 0) {
          return Location.EDGE;
        }
      }

      if (ts.length == 1 && ts[0].x == 0) {
        // make sure it's not a '\/' or '/\' intersection at the vertex
        if (verticalOrientation(curr) != verticalOrientation(prev)) {
          count--;
        }
        prev = curr;

        // if we're collinear, pretend like our neighbors are collapsed together
      } else if (ts.length > 1 && curr.type() == Curve2.Type.FLAT) {

      } else {
        count += ts.length;
        prev = curr;
      }
    }

    return count % 2 == 1 ? Location.INSIDE : Location.OUTSIDE;
  }

  public Ring2 transform(Matrix3 m) {
    return new Ring2(() -> Arrays.stream(curves).map(c -> c.transform(m)).iterator());
  }
}