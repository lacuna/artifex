package io.lacuna.artifex;

/**
 * @author ztellman
 */
public class Interval3 extends Interval<Vec3, Interval3> {

  public static final Interval3 EMPTY = new Interval3();

  public Interval3() {
    super(null, null, true);
  }

  public Interval3(Vec3 a, Vec3 b) {
    super(a.zip(b, Math::min), a.zip(b, Math::max), false);
  }

  @Override
  protected Interval3 construct(Vec3 lower, Vec3 upper) {
    return null;
  }

  @Override
  protected Interval3 empty() {
    return EMPTY;
  }
}
