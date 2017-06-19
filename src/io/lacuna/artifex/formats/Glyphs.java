package io.lacuna.artifex.formats;

import io.lacuna.artifex.*;
import io.lacuna.artifex.utils.DistanceField;
import io.lacuna.artifex.utils.Images;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ztellman
 */
public class Glyphs {

  public static class Field {
    public final Interval2 fieldBounds, logicalBounds, visualBounds;
    private final float[][][] field;

    public Field(GlyphVector glyph, int resolution) {
      visualBounds = Interval.from(glyph.getVisualBounds());
      logicalBounds = Interval.from(glyph.getLogicalBounds());

      fieldBounds = visualBounds.expand(visualBounds.size().y * (2.0 / resolution));

      List<CurveRing2> rings = rings(glyph.getOutline());

      // if the upper corner is inside the shape, our curve directions are inverted
      if (DistanceField.insideRings(rings, fieldBounds.lerp(Vec2.ORIGIN))) {
        rings = rings.stream().map(CurveRing2::reverse).collect(Collectors.toList());
      }

      field = DistanceField.distanceField(
          rings,
          (int) Math.ceil(aspectRatio() * resolution),
          resolution,
          fieldBounds,
          Math.toRadians(3));
    }

    public double aspectRatio() {
      return fieldBounds.size().x / fieldBounds.size().y;
    }

    public BufferedImage field() {
      return Images.distanceFieldImage(field, (float) fieldBounds.size().y / 2);
    }

    public BufferedImage render(int resolution) {
      return Images.renderDistanceField(
          field(),
          (int) Math.ceil(aspectRatio() * resolution),
          resolution,
          (float) (fieldBounds.size().y * 1.2) / resolution);
    }
  }

  private static final FontRenderContext FONT_RENDER_CONTEXT = new FontRenderContext(null, false, false);

  public static List<CurveRing2> rings(Shape shape) {
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

    return result.stream().map(CurveRing2::new).collect(Collectors.toList());
  }

  public static Field glyph(Font font, String s, int resolution) {
    return new Field(font.createGlyphVector(FONT_RENDER_CONTEXT, s.toCharArray()), resolution);
  }
}
