package io.lacuna.artifex;

/**
 * @author ztellman
 */
public class Interval2 extends Interval<Vec2, Interval2> {

  public static final Interval2 EMPTY = new Interval2();

  private Interval2() {
    super(null, null, true);
  }

  public Interval2(Vec2 a, Vec2 b) {
    super(a.zip(b, Math::min), a.zip(b, Math::max), false);
  }

  @Override
  protected Interval2 construct(Vec2 lower, Vec2 upper) {
    return new Interval2(lower, upper);
  }

  @Override
  protected Interval2 empty() {
    return EMPTY;
  }
}
