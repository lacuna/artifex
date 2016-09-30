package io.lacuna.artifex;

/**
 * @author ztellman
 */
class Utils {

  public static int hash(double x, double y) {
    long hash = 1L;
    hash = (hash * 31) + Double.doubleToLongBits(x);
    hash = (hash * 31) + Double.doubleToLongBits(y);
    return (int) (hash ^ (hash >>> 32));
  }

  public static int hash(double x, double y, double z) {
    long hash = 1L;
    hash = (hash * 31) + Double.doubleToLongBits(x);
    hash = (hash * 31) + Double.doubleToLongBits(y);
    hash = (hash * 31) + Double.doubleToLongBits(z);
    return (int) (hash ^ (hash >>> 32));
  }

  public static int hash(double x, double y, double z, double w) {
    long hash = 1L;
    hash = (hash * 31) + Double.doubleToLongBits(x);
    hash = (hash * 31) + Double.doubleToLongBits(y);
    hash = (hash * 31) + Double.doubleToLongBits(z);
    hash = (hash * 31) + Double.doubleToLongBits(w);
    return (int) (hash ^ (hash >>> 32));
  }
}
