package io.lacuna.artifex;

import io.lacuna.bifurcan.IList;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static java.lang.Math.abs;

/**
 * @author ztellman
 */
public class Polygon2 {

  public static class Ring {
    private final double[] coords;
    private byte isConvex;

    public Ring(IList<Vec2> vertices) {
      coords = new double[(int) vertices.size() << 1];

      int idx = 0;
      for (Vec2 v : vertices) {
        coords[idx++] = v.x;
        coords[idx++] = v.y;
      }
    }

    /**
     * @return true if the polygon is convex, false otherwise
     */
    public boolean isConvex() {
      if (isConvex != 0) {
        return isConvex == 1;
      }

      Iterator<Vec2> it = vertices();
      Vec2 a;
      Vec2 b = it.next();
      Vec2 c = it.next();

      double z = 0.0;

      while (it.hasNext()) {
        a = b;
        b = c;
        c = it.next();

        double z2 = Math.signum(Vec2.cross(b.sub(a), c.sub(b)));

        if (z == 0.0) {
          z = z2;
        } else if (z2 != 0.0 && z2 != z) {
          isConvex = 2;
          return false;
        }
      }

      isConvex = 1;
      return true;
    }

    private double signedArea() {
      double area = 0.0;
      int len = coords.length;

      for (int i = 2; i < len; i += 2) {
        area += (coords[i] - coords[i - 2]) * (coords[i + 1] + coords[i - 1]);
      }
      area += (coords[0] - coords[len - 2]) * (coords[1] + coords[len - 1]);

      return area / 2.0;
    }

    /**
     * @return the area encompassed by the ring
     */
    public double area() {
      return abs(signedArea());
    }

    /**
     * @return true if the vertices are in clockwise order, false otherwise
     */
    public boolean isClockwise() {
      return signedArea() < 0.0;
    }

    /**
     * @return an iterator over all the vertices, with the first vertex repeated at the end
     */
    public Iterator<Vec2> vertices() {
      return new Iterator<Vec2>() {

        int idx = 0;

        @Override
        public boolean hasNext() {
          return idx < (coords.length + 1);
        }

        @Override
        public Vec2 next() {
          return new Vec2(coords[idx++ % coords.length], coords[idx++ % coords.length]);
        }
      };
    }

    /**
     * @param epsilon the maximum difference allowed between the respective vertices
     * @return true if the rings are similar within the threshold, false otherwise
     */
    public static boolean equals(Ring a, Ring b, double epsilon) {
      if (a.coords.length != b.coords.length) {
        return false;
      }

      for (int i = 0; i < a.coords.length; i++) {
        if (abs(a.coords[i] - b.coords[i]) > epsilon) {
          return false;
        }
      }

      return true;
    }

    /**
     * @return an iterator over all the edges
     */
    public Iterator<LineSegment2> edges() {
      Iterator<Vec2> vertices = vertices();

      return new Iterator<LineSegment2>() {

        Vec2 v = vertices.next();

        @Override
        public boolean hasNext() {
          return vertices.hasNext();
        }

        @Override
        public LineSegment2 next() {
          LineSegment2 edge = LineSegment2.from(v, vertices.next());
          v = edge.end();
          return edge;
        }
      };
    }
  }

  private final Ring perimiter;
  private final IList<Ring> holes;

  Polygon2(Ring perimeter, IList<Ring> holes) {
    this.perimiter = perimeter;
    this.holes = holes;
  }
}
