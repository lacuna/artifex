package io.lacuna.artifex;

/**
 * @author ztellman
 */
public class CollinearException extends Exception {

  public final Curve2 a, b;

  public CollinearException(Curve2 a, Curve2 b) {
    this.a = a;
    this.b = b;
  }
}
