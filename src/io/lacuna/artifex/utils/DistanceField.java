package io.lacuna.artifex.utils;

import io.lacuna.artifex.*;

import java.util.*;
import java.util.stream.Collectors;

import static io.lacuna.artifex.Vec2.cross;
import static io.lacuna.artifex.Vec.dot;
import static java.lang.Integer.bitCount;
import static java.lang.Math.*;

/**
 * Much of the implementation here is adapted from https://github.com/Chlumsky/msdfgen, which is available under the MIT
 * license.
 *
 * @author ztellman
 */
public class DistanceField {

  private static final byte BLACK = 0, RED = 1, GREEN = 2, YELLOW = 3, BLUE = 4, MAGENTA = 5, CYAN = 6, WHITE = 7;

  static class SignedDistance implements Comparable<SignedDistance> {
    public final double distSquared;
    public final double pseudoDistSquared;
    public final double dot;
    public boolean inside;

    public SignedDistance(Curve2 curve, Vec2 origin) {

      double param = curve.nearestPoint(origin);
      double clampedParam = min(1, max(0, param));
      Vec2 pos = curve.position(clampedParam);
      Vec2 dir = curve.direction(clampedParam).norm();
      Vec2 po = origin.sub(pos);

      distSquared = po.lengthSquared();
      inside = cross(dir, po) > 0;

      if (param == clampedParam) {
        dot = 0;
        pseudoDistSquared = -1;
      } else {
        // calculate pseudo-distance
        double ts = dot(po, dir);
        dot = abs(dot(dir, po.norm()));

        if (signum(ts) == signum(param)) {
          double pseudoDistance = cross(po, dir);
          pseudoDistSquared = pseudoDistance * pseudoDistance;
        } else {
          pseudoDistSquared = -1;
        }
      }
    }

    public double distance() {
      return sqrt(distanceSquared()) * (inside ? 1 : -1);
    }

    public double distanceSquared() {
      return pseudoDistSquared > 0 && pseudoDistSquared < distSquared ? pseudoDistSquared : distSquared;
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

  // returns true if this should be treated as a "sharp" corner
  private static boolean isCorner(Curve2 a, Curve2 b, double crossThreshold) {
    Vec2 ta = a.direction(1).norm();
    Vec2 tb = b.direction(0).norm();

    return dot(ta, tb) <= 0 || abs(cross(ta, tb)) > crossThreshold;
  }

  private static List<Integer> cornerIndices(CurveRing2 ring, double angleThreshold) {
    List<Integer> corners = new ArrayList<>();
    List<Curve2> curves = ring.curves();
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
    return c.split(new double[]{0.33, 0.66});
  }

  private static Map<Curve2, Byte> edgeColors(CurveRing2 ring, double angleThreshold) {
    Map<Curve2, Byte> edgeColors = new HashMap<>();
    List<Integer> corners = cornerIndices(ring, angleThreshold);
    List<Curve2> curves = ring.curves();

    if (corners.isEmpty()) {
      // smooth contour
      for (Curve2 c : curves) {
        edgeColors.put(c, WHITE);
      }
    } else if (corners.size() == 1) {
      // teardrop
      int offset = corners.get(0);
      byte[] colors = {MAGENTA, WHITE, YELLOW};
      int num = curves.size();

      if (num >= 3) {
        for (int i = 0; i < num; i++) {
          Curve2 c = curves.get((i + offset) % num);
          int colorIdx = (int) (((3 + ((2.875 * i) / (num - 1))) - 1.4375) + .5) - 2;
          edgeColors.put(c, colors[colorIdx]);
        }
      } else if (num == 2) {
        Curve2[] a = splitIntoThirds(curves.get(0));
        Curve2[] b = splitIntoThirds(curves.get(1));
        for (int i = 0; i < 6; i++) {
          edgeColors.put(i < 3 ? a[i] : b[i - 3], colors[i / 2]);
        }
      } else {
        Curve2[] thirds = splitIntoThirds(curves.get(0));
        for (int i = 0; i < 3; i++) {
          edgeColors.put(thirds[i], colors[i]);
        }
      }
    } else {
      // multi-corner
      int offset = corners.get(0);
      int cIdx = 0;
      byte[] colors = new byte[]{corners.size() % 3 == 1 ? YELLOW : CYAN, CYAN, MAGENTA, YELLOW};

      for (int i = 0; i < curves.size(); i++) {
        int idx = (i + offset) % curves.size();
        if (cIdx + 1 < corners.size() && corners.get(cIdx + 1) == idx) {
          cIdx++;
        }
        edgeColors.put(curves.get(idx), colors[1 + (cIdx % 3) - (cIdx == 0 ? 1 : 0)]);
      }
    }

    return edgeColors;
  }

  public static double median(double a, double b, double c) {
    return max(min(a, b), min(max(a, b), c));
  }

  /**
   * Checks whether two adjacent texels are "inside" on different channels, such that a linear interpolation might make
   * something in between "outside".
   */
  private static boolean clash(float[] a, float[] b, float threshold) {
    byte aPos, bPos, aNeg, bNeg;
    aPos = bPos = aNeg = bNeg = 0;
    for (int i = 0; i < 3; i++) {
      if (a[i] < 0) {
        aNeg |= 1;
      } else if (a[i] > 0) {
        aPos |= 1;
      }

      if (b[i] < 0) {
        bNeg |= 1;
      } else if (b[i] > 0) {
        bPos |= 1;
      }

      aPos <<= 1;
      bPos <<= 1;
      aNeg <<= 1;
      bNeg <<= 1;
    }

    int intersection = 0;
    if (bitCount(aPos) == 2 && bitCount(bPos) == 2 && bitCount(aPos ^ bPos) == 2) {
      intersection = aPos & bPos;
    } else if (bitCount(aNeg) == 2 && bitCount(bNeg) == 2 && bitCount(aNeg ^ bNeg) == 2) {
      intersection = aNeg & bNeg;
    }

    switch (intersection) {
      case RED:
        return //abs(a[1] - b[1]) > threshold && abs(a[2] - b[2]) > threshold &&
            abs(a[0]) >= abs(b[0]);
      case BLUE:
        return //abs(a[0] - b[0]) > threshold && abs(a[2] - b[2]) > threshold &&
            abs(a[1]) >= abs(b[1]);
      case GREEN:
        return //abs(a[0] - b[0]) > threshold && abs(a[1] - b[1]) > threshold &&
            abs(a[2]) >= abs(b[2]);
      default:
        return false;
    }


    /*return (bitCount(aNeg) == 2 && bitCount(bNeg) == 2 && bitCount(aNeg ^ bNeg) == 2)
        || (bitCount(aPos) == 2 && bitCount(bPos) == 2 && bitCount(aPos ^ bPos) == 2);
    //|| (bitCount(aPos) == 1 && bitCount(bPos) == 1 && bitCount(aPos ^ bPos) == 0);
    */

  }

  private static boolean clash1(float[] a, float[] b, float threshold) {
    // Only consider pair where both are on the inside or both are on the outside
    boolean aIn = (a[0] > 0 ? 1 : 0) + (a[1] > 0 ? 1 : 0) + (a[2] > 0 ? 1 : 0) >= 2;
    boolean bIn = (b[0] > 0 ? 1 : 0) + (b[1] > 0 ? 1 : 0) + (b[2] > 0 ? 1 : 0) >= 2;
    if (aIn != bIn) return false;
    // If the change is 0 <-> 1 or 2 <-> 3 channels and not 1 <-> 1 or 2 <-> 2, it is not a clash
    if ((a[0] > 0 && a[1] > 0 && a[2] > 0) || (a[0] < 0 && a[1] < 0 && a[2] < 0)
        || (b[0] > 0 && b[1] > 0 && b[2] > 0) || (b[0] < 0 && b[1] < 0 && b[2] < 0))
      return false;
    // Find which color is which: _a, _b = the changing channels, _c = the remaining one
    float aa, ab, ba, bb, ac, bc;
    if ((a[0] > 0) != (b[0] > 0) && (a[0] < 0) != (b[0] < 0)) {
      aa = a[0];
      ba = b[0];
      if ((a[1] > 0) != (b[1] > 0) && (a[1] < 0) != (b[1] < 0)) {
        ab = a[1];
        bb = b[1];
        ac = a[2];
        bc = b[2];
      } else if ((a[2] > 0) != (b[2] > 0) && (a[2] < 0) != (b[2] < 0)) {
        ab = a[2];
        bb = b[2];
        ac = a[1];
        bc = b[1];
      } else
        return false; // this should never happen
    } else if ((a[1] > 0) != (b[1] > 0) && (a[1] < 0) != (b[1] < 0)
        && (a[2] > 0) != (b[2] > 0) && (a[2] < 0) != (b[2] < 0)) {
      aa = a[1];
      ba = b[1];
      ab = a[2];
      bb = b[2];
      ac = a[0];
      bc = b[0];
    } else
      return false;
    // Find if the channels are in fact discontinuous
    return (abs(aa - ba) >= threshold)
        && (abs(ab - bb) >= threshold)
        && abs(ac) >= abs(bc); // Out of the pair, only flag the pixel farther from a shape edge

  }

  /**
   * If there's potential for a clash between two texels which are both inside, just set all channels to the same value.
   */
  private static void fixClashes(float[][][] field, float threshold) {
    int width = field.length;
    int height = field[0].length;
    for (int i = 0; i < width; i++) {
      for (int j = 0; j < height; j++) {
        float[] color = field[i][j];
        if ((i > 0 && clash(color, field[i - 1][j], threshold))
            || (i < (width - 1) && clash(color, field[i + 1][j], threshold))
            || (j > 0 && clash(color, field[i][j - 1], threshold))
            || (j < (height - 1) && clash(color, field[i][j + 1], threshold))) {
          System.out.println("clash! " + i + " " + j);
          float median = (float) median(color[0], color[1], color[2]);
          color[0] = color[1] = color[2] = median;
        }
      }
    }
  }

  public static boolean insideRings(List<CurveRing2> rings, Vec2 point) {
    return rings.stream()
        .flatMap(rs -> rs.curves().stream())
        .map(c -> new SignedDistance(c, point))
        .sorted()
        .findFirst()
        .get()
        .inside;
  }

  public static float[][][] distanceField(
      List<CurveRing2> rings,
      int w,
      int h,
      Interval2 bounds,
      double angleThreshold) {

    Map<Curve2, Byte> curveMap = new HashMap<>();
    for (CurveRing2 ring : rings) {
      curveMap.putAll(edgeColors(ring, angleThreshold));
    }

    List<Curve2> curves = new ArrayList<>(curveMap.keySet());
    List<Interval2> boxes = curves.stream().map(Curve2::bounds).collect(Collectors.toList());
    byte[] colors = new byte[curves.size()];

    Iterator<Byte> it = curveMap.values().iterator();
    for (int i = 0; i < colors.length; i++) {
      colors[i] = it.next();
    }

    float[][][] field = new float[w][h][3];
    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        SignedDistance r, g, b;
        r = g = b = null;
        Vec2 t = new Vec2((x + 0.5) / (w + 1), (y + 0.5) / (h + 1));
        Vec2 p = bounds.lerp(t);

        for (int i = 0; i < curves.size(); i++) {
          byte color = colors[i];
          Curve2 c = curves.get(i);
          Interval2 box = boxes.get(i);

          // this is the minimum possible distance, based on the bounding box
          double minDS = box.distanceSquared(p);
          if (r == null || g == null || b == null || minDS <= r.distSquared || minDS <= g.distSquared || minDS <= b.distSquared) {
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
        }

        field[x][y][0] = r != null ? (float) r.distance() : 0f;
        field[x][y][1] = g != null ? (float) g.distance() : 0f;
        field[x][y][2] = b != null ? (float) b.distance() : 0f;
      }
    }

    //fixClashes(field, 0f);

    return field;
  }
}
