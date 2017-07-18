package io.lacuna.artifex;

/**
 * @author ztellman
 */
public class Box3 extends Box<Vec3, Box3> {

  public static final Box3 EMPTY = new Box3();

  public Box3() {
    super(null, null, true);
  }

  public Box3(Vec3 a, Vec3 b) {
    super(a.zip(b, Math::min), a.zip(b, Math::max), false);
  }

  @Override
  protected Box3 construct(Vec3 lower, Vec3 upper) {
    return null;
  }

  @Override
  protected Box3 empty() {
    return EMPTY;
  }
}
