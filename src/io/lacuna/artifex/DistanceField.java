package io.lacuna.artifex;

import io.lacuna.artifex.utils.Images;

import java.util.*;
import java.util.stream.Collectors;

import static io.lacuna.artifex.Vec2.cross;
import static io.lacuna.artifex.Vec2.dot;
import static java.lang.Integer.bitCount;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.signum;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

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

  public static List<Integer> cornerIndices(List<Curve2> curves, double angleThreshold) {
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

  public static Map<Curve2, Byte> edgeColors(List<Curve2> ring, double angleThreshold) {
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

  private static boolean clash(float[] a, float[] b) {
    byte aPos, bPos, aNeg, bNeg;
    aPos = bPos = aNeg = bNeg = 0;
    for (int i = 0; i < 3; i++) {
      if (a[i] < 0.5) {
        aNeg |= 1;
      } else if (a[i] > 0.5) {
        aPos |= 1;
      }

      if (b[i] < 0.5) {
        bNeg |= 1;
      } else if (b[i] > 0.5) {
        bPos |= 1;
      }

      aPos <<= 1;
      bPos <<= 1;
      aNeg <<= 1;
      bNeg <<= 1;
    }

    return (bitCount(aNeg) == 2 && bitCount(bNeg) == 2 && bitCount(aNeg ^ bNeg) == 2)
            || (bitCount(aPos) == 2 && bitCount(bPos) == 2 && bitCount(aPos ^ bPos) == 2)
            || (bitCount(aPos) == 1 && bitCount(bPos) == 1 && bitCount(aPos | bPos) == 2);
  }

  private static void fixClashes(float[][][] field) {
    int width = field.length;
    int height = field[0].length;
    for (int i = 0; i < width; i++) {
      for (int j = 0; j < height; j++) {
        float[] color = field[i][j];
        if ((i > 0 && clash(color, field[i - 1][j]))
                || (i < (width - 1) && clash(color, field[i + 1][j]))
                || (j > 0 && clash(color, field[i][j - 1]))
                || (j < (height - 1) && clash(color, field[i][j + 1]))) {
          float median = (float) Images.median(color[0], color[1], color[2]);
          color[0] = color[1] = color[2] = median;
        }
      }
    }
  }

  public static boolean insideRings(List<List<Curve2>> rings, Vec2 point) {
    return rings.stream()
            .flatMap(List::stream)
            .map(c -> new SignedDistance(c, point))
            .sorted()
            .findFirst()
            .get()
            .inside;
  }

  public static List<Curve2> reverseRing(List<Curve2> ring) {
    List<Curve2> result = ring.stream().map(Curve2::reverse).collect(Collectors.toList());
    Collections.reverse(result);
    return result;
  }

  public static float[][][] distanceField(
          List<List<Curve2>> rings,
          int w,
          int h,
          Box2 bounds,
          double angleThreshold,
          float scale) {

    if (insideRings(rings, bounds.lerp(Vec2.ORIGIN))) {
      rings = rings.stream().map(DistanceField::reverseRing).collect(Collectors.toList());
    }

    Map<Curve2, Byte> curves = new HashMap<>();
    for (List<Curve2> ring : rings) {
      curves.putAll(edgeColors(ring, angleThreshold));
    }

    float[][][] field = new float[w][h][3];
    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        SignedDistance r, g, b;
        r = g = b = null;
        Vec2 t = new Vec2((x + 0.5) / (w + 1), (y + 0.5) / (h + 1));
        Vec2 p = bounds.lerp(t);

        for (Map.Entry<Curve2, Byte> e: curves.entrySet()) {
          Curve2 c = e.getKey();
          byte color = e.getValue();
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

        field[x][y][0] = r != null ? (float) r.distance() : 0f;
        field[x][y][1] = g != null ? (float) g.distance() : 0f;
        field[x][y][2] = b != null ? (float) b.distance() : 0f;

        for (int z = 0; z < 3; z++) {
          field[x][y][z] = min(1f, max(0f, (field[x][y][z] / (scale * 2)) + 0.5f));
        }
      }
    }

    fixClashes(field);

    return field;
  }

  public static float[][][] distanceField(List<List<Curve2>> curveRings, int w, int h) {
    Box2 b = Box2.EMPTY;
    for (List<Curve2> curves : curveRings) {
      for (Curve2 c : curves) {
        b = b.union(c.bounds());
      }
    }
    b = new Box2(b.left - 1, b.right + 1, b.bottom - 1, b.top + 1);

    return distanceField(curveRings, w, h, b, Math.toRadians(3), (float) b.size().reduce(Math::max) / 4);
  }
}
