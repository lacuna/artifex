package io.lacuna.artifex;

/**
 * @author ztellman
 */
public class CircularSegment2 implements Curve2 {

  @Override
  public Vec2 position(double t) {
    return null;
  }

  @Override
  public Vec2 direction(double t) {
    return null;
  }

  @Override
  public Curve2[] split(double t) {
    return new Curve2[0];
  }

  @Override
  public double nearestPoint(Vec2 p) {
    return 0;
  }
}
