package io.lacuna.artifex;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.PathIterator;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class Glyph {

  private static final FontRenderContext FONT_RENDER_CONTEXT = new FontRenderContext(null, false, false);

  public final DistanceField field;
  public final Box2 bounds;

  private Glyph(DistanceField field, Box2 bounds) {
    this.field = field;
    this.bounds = bounds;
  }

  public static List<List<Curve2>> path(Font font, String s) {
    List<List<Curve2>> result = new ArrayList<>();
    List<Curve2> curves = new ArrayList<>();

    double[] coords = new double[6];
    Vec2 prev, curr = Vec2.ORIGIN, move = Vec2.ORIGIN;
    GlyphVector glyphVector = glyphVector(font, s);
    PathIterator it = glyphVector.getOutline().getPathIterator(null);
    while (!it.isDone()) {
      prev = curr;
      switch (it.currentSegment(coords)) {
        case PathIterator.SEG_MOVETO:
          curves = new ArrayList<>();
          curr = move = new Vec2(coords[0], coords[1]);
          break;
        case PathIterator.SEG_LINETO:
          curr = new Vec2(coords[0], coords[1]);
          curves.add(Bezier2.from(prev, curr));
          break;
        case PathIterator.SEG_QUADTO:
          curr = new Vec2(coords[2], coords[3]);
          curves.add(Bezier2.from(prev, new Vec2(coords[0], coords[1]), curr));
          break;
        case PathIterator.SEG_CUBICTO:
          curr = new Vec2(coords[4], coords[5]);
          curves.add(Bezier2.from(prev, new Vec2(coords[0], coords[1]), new Vec2(coords[2], coords[3]), curr));
          break;
        case PathIterator.SEG_CLOSE:
          if (!prev.equals(move)) {
            curves.add(Bezier2.from(prev, move));
          }
          result.add(curves);
          break;
      }

      it.next();
    }

    return result;
  }

  public static Glyph from(Font font, String s, double sampleFrequency) {
    List<List<Curve2>> result = new ArrayList<>();
    List<Curve2> curves = new ArrayList<>();

    double[] coords = new double[6];
    Vec2 prev, curr = Vec2.ORIGIN, move = Vec2.ORIGIN;
    GlyphVector glyphVector = glyphVector(font, s);
    PathIterator it = glyphVector.getOutline().getPathIterator(null);
    while (!it.isDone()) {
      prev = curr;
      switch (it.currentSegment(coords)) {
        case PathIterator.SEG_MOVETO:
          curves = new ArrayList<>();
          curr = move = new Vec2(coords[0], coords[1]);
          break;
        case PathIterator.SEG_LINETO:
          curr = new Vec2(coords[0], coords[1]);
          curves.add(Bezier2.from(prev, curr));
          break;
        case PathIterator.SEG_QUADTO:
          curr = new Vec2(coords[2], coords[3]);
          curves.add(Bezier2.from(prev, new Vec2(coords[0], coords[1]), curr));
          break;
        case PathIterator.SEG_CUBICTO:
          curr = new Vec2(coords[4], coords[5]);
          curves.add(Bezier2.from(prev, new Vec2(coords[0], coords[1]), new Vec2(coords[2], coords[3]), curr));
          break;
        case PathIterator.SEG_CLOSE:
          if (!prev.equals(move)) {
            curves.add(Bezier2.from(prev, move));
          }
          result.add(curves);
          break;
      }

      it.next();
    }

    return new Glyph(
            DistanceField.from(result.stream().map(CurveRing2::new).collect(Collectors.toList()), sampleFrequency),
            Box.from(glyphVector.getLogicalBounds()));
  }

  public static Box2 visualBounds(Font font, String s) {
    return Box.from(glyphVector(font, s).getVisualBounds());
  }

  private static GlyphVector glyphVector(Font font, String s) {
    return font.createGlyphVector(FONT_RENDER_CONTEXT, s.toCharArray());
  }

}
