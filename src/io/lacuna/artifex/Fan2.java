package io.lacuna.artifex;

import java.util.Objects;

/**
 * @author ztellman
 */
public class Fan2 {
  public final Vec2 origin;
  public final Curve2 edge;
  public final boolean internal;

  public Fan2(Vec2 origin, Curve2 edge) {
    this(origin, edge, false);
  }

  public Fan2(Vec2 origin, Curve2 edge, boolean internal) {

    assert !origin.equals(edge.start());
    assert !origin.equals(edge.end());

    this.origin = origin;
    this.edge = edge;
    this.internal = internal;
  }

  @Override
  public int hashCode() {
    return Objects.hash(origin, edge, internal);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof Fan2) {
      Fan2 f = (Fan2) obj;
      return origin.equals(f.origin) && edge.equals(f.edge) && internal == f.internal;
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    return origin + " -> " + edge;
  }
}
