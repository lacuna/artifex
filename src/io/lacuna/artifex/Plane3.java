package io.lacuna.artifex;

/**
 * @author ztellman
 */
public class Plane3 {
  public final Vec3 normal;
  public final double p;

  /**
   * @param normal the normal vector for the plane
   * @param p      the distance of the plane from the origin
   */
  public Plane3(Vec3 normal, double p) {
    this.normal = normal.norm();
    this.p = p;
  }

  /**
   * @param v a point
   * @return the signed distance from the point to the plane's surface
   */
  public double distance(Vec3 v) {
    return Vec.dot(v, normal) + p;
  }

  @Override
  public int hashCode() {
    return normal.hashCode() ^ (int) p;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Plane3) {
      Plane3 plane = (Plane3) obj;
      return normal.equals(plane.normal) && p == plane.p;
    }
    return false;
  }
}
