package io.lacuna.artifex;

/**
 * @author ztellman
 */
public class Interval4 extends Interval<Vec4, Interval4> {

  public static final Interval4 EMPTY = new Interval4();

  public Interval4() {
    super(null, null, true);
  }

  public Interval4(Vec4 a, Vec4 b) {
    super(a.zip(b, Math::min), a.zip(b, Math::max), false);
  }

  @Override
  protected Interval4 construct(Vec4 lower, Vec4 upper) {
    return new Interval4(lower, upper);
  }

  @Override
  protected Interval4 empty() {
    return EMPTY;
  }
}
