package io.lacuna.artifex.utils;

import static java.lang.System.arraycopy;

/**
 * @author ztellman
 */
public class DoubleAccumulator {

  private double[] values = new double[1];
  private int index = 0;

  public void add(double v) {
    if (index == this.values.length - 1) {
      double[] values = new double[this.values.length << 1];
      arraycopy(this.values, 0, values, 0, this.values.length);
      this.values = values;
    }

    values[index++] = v;
  }

  public int size() {
    return index;
  }

  public double get(int index) {
    return values[index];
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
