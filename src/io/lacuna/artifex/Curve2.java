package io.lacuna.artifex;

/**
 * @author ztellman
 */
public interface Curve2 {
  Vec2 position(double t);
  Vec2 direction(double t);
  Curve2[] split(double t);
  double nearestPoint(Vec2 p);
}
