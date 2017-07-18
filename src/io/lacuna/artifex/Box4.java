package io.lacuna.artifex;

/**
 * @author ztellman
 */
public class Box4 extends Box<Vec4, Box4> {

  public static final Box4 EMPTY = new Box4();

  public Box4() {
    super(null, null, true);
  }

  public Box4(Vec4 a, Vec4 b) {
    super(a.zip(b, Math::min), a.zip(b, Math::max), false);
  }

  @Override
  protected Box4 construct(Vec4 lower, Vec4 upper) {
    return new Box4(lower, upper);
  }

  @Override
  protected Box4 empty() {
    return EMPTY;
  }
}
