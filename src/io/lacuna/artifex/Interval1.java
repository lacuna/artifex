package io.lacuna.artifex;

/**
 *
 */
public class Interval1 extends Interval<Vec1, Interval1> {

  public static final Interval1 EMPTY = new Interval1();

  private Interval1() {
    super(null, null, true);
  }

  public Interval1(Vec1 lower, Vec1 upper) {
    super(lower, upper, false);
  }

  public Interval1(double lower, double upper) {
    this(new Vec1(lower), new Vec1(upper));
  }

  @Override
  protected Interval1 construct(Vec1 lower, Vec1 upper) {
    return new Interval1(lower, upper);
  }

  @Override
  protected Interval1 empty() {
    return EMPTY;
  }
}
