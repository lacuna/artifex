package io.lacuna.artifex;

/**
 * @author ztellman
 */
public class Box2 extends Box<Vec2, Box2> {

  public static final Box2 EMPTY = new Box2();

  private Box2() {
    super(null, null, true);
  }

  public Box2(Vec2 a, Vec2 b) {
    super(a.zip(b, Math::min), a.zip(b, Math::max), false);
  }

  public Box2 translate(double x, double y) {
    return translate(Vec.from(x, y));
  }

  public Box2 translate(Vec2 v) {
    return new Box2(lower.add(v), upper.add(v));
  }

  public Box2 scale(double x) {
    return scale(x, x);
  }

  public Box2 scale(double x, double y) {
    return scale(Vec.from(x, y));
  }

  public Box2 scale(Vec2 v) {
    return new Box2(lower.mul(v), upper.mul(v));
  }

  @Override
  protected Box2 construct(Vec2 lower, Vec2 upper) {
    return new Box2(lower, upper);
  }

  @Override
  protected Box2 empty() {
    return EMPTY;
  }
}
