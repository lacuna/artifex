package io.lacuna.artifex.formats;

import io.lacuna.artifex.Bezier2;
import io.lacuna.artifex.Box2;
import io.lacuna.artifex.Curve2;
import io.lacuna.artifex.Vec2;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ztellman
 */
public class Glyphs {

  private static final FontRenderContext FONT_RENDER_CONTEXT = new FontRenderContext(null, false, false);

  public static Box2 logicalBounds(GlyphVector glyph) {
    return Box2.from(glyph.getLogicalBounds());
  }

  public static Box2 visualBounds(GlyphVector glyph) {
    return Box2.from(glyph.getVisualBounds());
  }

  public static List<List<Curve2>> curves(Shape shape) {
    List<List<Curve2>> result = new ArrayList<>();
    List<Curve2> curves = new ArrayList<>();

    double[] coords = new double[6];
    Vec2 prev, curr = Vec2.ORIGIN, move = Vec2.ORIGIN;
    PathIterator it = shape.getPathIterator(null);
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

  public static Shape outline(Font font, char a) {
    return font.createGlyphVector(FONT_RENDER_CONTEXT, new char[]{a}).getOutline();
  }
}
