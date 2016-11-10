package io.lacuna.artifex;

import java.util.*;

import static io.lacuna.artifex.Vec2.cross;
import static io.lacuna.artifex.Vec2.dot;
import static java.lang.Math.*;

/**
 * Much of the implementation here is adapted from https://github.com/Chlumsky/msdfgen, which is available under the MIT
 * license.
 *
 * @author ztellman
 */
public class DistanceField {

  private static final byte BLACK = 0, RED = 1, GREEN = 2, YELLOW = 3, BLUE = 4, MAGENTA = 5, CYAN = 6, WHITE = 7;

  public static class SignedDistance implements Comparable<SignedDistance> {
    public final double distSquared;
    public final double pseudoDistSquared;
    public final double dot;
    public boolean inside;

    public SignedDistance(Curve2 curve, Vec2 origin) {
      double param = curve.nearestPoint(origin);
      double clampedParam = min(1, max(0, param));
      Vec2 pos = curve.position(clampedParam);
      Vec2 dir = curve.direction(clampedParam).norm();
      Vec2 pSo = pos.sub(origin);

      distSquared = pSo.lengthSquared();
      inside = cross(dir, pSo) > 0;

      if (param == clampedParam) {
        dot = 0;
        pseudoDistSquared = -1;
      } else {
        dot = abs(dot(dir, pSo.norm()));

        // calculate pseudo-distance
        double ts = dot(pSo, dir);
        if (signum(ts) == signum(param)) {
          double pseudoDistance = cross(pSo, dir);
          pseudoDistSquared = pseudoDistance * pseudoDistance;
        } else {
          pseudoDistSquared = -1;
        }
      }
    }

    public double distance() {
      return sqrt(distSquared) * (inside ? 1 : -1);
    }

    public double pseudoDistance() {
      double d2 = pseudoDistSquared > 0 && pseudoDistSquared < distSquared ? pseudoDistSquared : distSquared;
      return sqrt(d2) * (inside ? 1 : -1);
    }

    @Override
    public int compareTo(SignedDistance o) {
      if (distSquared != o.distSquared) {
        return (int) signum(distSquared - o.distSquared);
      } else {
        return (int) signum(dot - o.dot);
      }
    }
  }

  private static boolean isCorner(Curve2 a, Curve2 b, double crossThreshold) {
    Vec2 ta = a.direction(1).norm();
    Vec2 tb = b.direction(0).norm();

    return dot(ta, tb) <= 0 || abs(cross(ta, tb)) > crossThreshold;
  }

  private static List<Integer> cornerIndices(List<Curve2> curves, double angleThreshold) {
    List<Integer> corners = new ArrayList<>();
    double crossThreshold = sin(angleThreshold);

    Curve2 prev = curves.get(curves.size() - 1);
    for (int i = 0; i < curves.size(); i++) {
      Curve2 curr = curves.get(i);
      if (isCorner(prev, curr, crossThreshold)) {
        corners.add(i);
      }
      prev = curr;
    }

    return corners;
  }

  private static Curve2[] splitIntoThirds(Curve2 c) {
    Curve2[] a = c.split(0.33);
    Curve2[] b = a[1].split(0.5);
    return new Curve2[]{a[0], b[1], b[2]};
  }

  private static Map<Curve2, Byte> edgeColors(List<Curve2> ring, double angleThreshold) {
    Map<Curve2, Byte> edgeColors = new HashMap<>();
    List<Integer> corners = cornerIndices(ring, angleThreshold);

    if (corners.isEmpty()) {
      // smooth contour
      for (Curve2 c : ring) {
        edgeColors.put(c, WHITE);
      }
    } else if (corners.size() == 1) {
      // teardrop
      int offset = corners.get(0);
      byte[] colors = {MAGENTA, WHITE, YELLOW};
      int num = ring.size();

      if (num >= 3) {
        for (int i = 0; i < num; i++) {
          Curve2 c = ring.get((i + offset) % num);
          int colorIdx = (int) (((3 + ((2.875 * i) / (num - 1))) - 1.4375) + .5) - 2;
          edgeColors.put(c, colors[colorIdx]);
        }
      } else if (num == 2) {
        Curve2[] a = splitIntoThirds(ring.get(0));
        Curve2[] b = splitIntoThirds(ring.get(1));
        for (int i = 0; i < 6; i++) {
          edgeColors.put(i < 3 ? a[i] : b[i - 3], colors[i / 2]);
        }
      } else {
        Curve2[] thirds = splitIntoThirds(ring.get(0));
        for (int i = 0; i < 3; i++) {
          edgeColors.put(thirds[i], colors[i]);
        }
      }
    } else {
      // multi-corner
      int offset = corners.get(0);
      int cIdx = 0;
      byte[] colors = new byte[]{corners.size() % 3 == 1 ? YELLOW : CYAN, CYAN, MAGENTA, YELLOW};

      for (int i = 0; i < ring.size(); i++) {
        int idx = (i + offset) % ring.size();
        if (cIdx + 1 < corners.size() && corners.get(cIdx + 1) == idx) {
          cIdx++;
        }
        edgeColors.put(ring.get(idx), colors[1 + (cIdx % 3) - (cIdx == 0 ? 1 : 0)]);
      }
    }

    return edgeColors;
  }

  public static float[][][] distanceField(
          Iterable<List<Curve2>> curveRings,
          int w,
          int h,
          Box2 bounds,
          double angleThreshold,
          float scale) {
    float[][][] field = new float[w][h][3];

    Map<Curve2, Byte> curves = new HashMap<>();
    for (List<Curve2> ring : curveRings) {
      curves.putAll(edgeColors(ring, angleThreshold));
    }

    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        SignedDistance r, g, b;
        r = g = b = null;
        Vec2 t = new Vec2((x + 0.5) / (w + 1), (y + 0.5) / (h + 1));
        Vec2 p = bounds.lerp(t);

        for (Curve2 c : curves.keySet()) {
          byte color = curves.get(c);
          SignedDistance d = new SignedDistance(c, p);

          if ((color & RED) > 0 && (r == null || r.compareTo(d) > 0)) {
            r = d;
          }
          if ((color & GREEN) > 0 && (g == null || g.compareTo(d) > 0)) {
            g = d;
          }
          if ((color & BLUE) > 0 && (b == null || b.compareTo(d) > 0)) {
            b = d;
          }
        }

        field[x][y][0] = r != null ? (float) r.pseudoDistance() : 0f;
        field[x][y][1] = g != null ? (float) g.pseudoDistance() : 0f;
        field[x][y][2] = b != null ? (float) b.pseudoDistance() : 0f;

        for (int z = 0; z < 3; z++) {
          field[x][y][z] = min(1f, max(0f, (field[x][y][z] / (scale * 2)) + 0.5f));
        }
      }
    }

    return field;
  }

  public static float[][][] distanceField(Iterable<List<Curve2>> curveRings, int w, int h) {
    Box2 b = Box2.EMPTY;
    for (List<Curve2> curves : curveRings) {
      for (Curve2 c : curves) {
        b = b.union(c.bounds());
      }
    }
    b = new Box2(b.left - 1, b.right + 1, b.bottom - 1, b.top + 1);

    return distanceField(curveRings, w, h, b, Math.toRadians(3), 0.25f);
  }
}
