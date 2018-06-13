package io.lacuna.artifex.utils;

import static java.lang.System.arraycopy;

/**
 * @author ztellman
 */
public class DoubleAccumulator {

  private double[] values = new double[2];
  private int index = 0;

  private void expand() {
    double[] values = new double[this.values.length << 1];
    arraycopy(this.values, 0, values, 0, this.values.length);
    this.values = values;
  }

  public void add(double n) {
    if (index > this.values.length - 1) {
      expand();
    }

    values[index++] = n;
  }

  public void add(double a, double b) {
    if (index > this.values.length - 2) {
      expand();
    }

    values[index++] = a;
    values[index++] = b;
  }

  public void pop(int num) {
    index -= num;
  }

  public int size() {
    return index;
  }

  public double get(int index) {
    return values[index];
  }

  public void set(int index, double n) {
    values[index] = n;
  }

  public double[] toArray() {
    if (index == values.length) {
      return values;
    }

    double[] result = new double[index];
    arraycopy(values, 0, result, 0, index);
    return result;
  }
}
