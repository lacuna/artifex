package io.lacuna.artifex;

import java.util.List;

/**
 * @author ztellman
 */
public class Shape2 {

  private final List<List<Curve2>> perimeters, holes;



  private Shape2(List<List<Curve2>> perimeters, List<List<Curve2>> holes) {
    this.perimeters = perimeters;
    this.holes = holes;
  }


}
