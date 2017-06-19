package io.lacuna.artifex.utils;

import io.lacuna.artifex.Vec3;

import java.awt.image.BufferedImage;

import static io.lacuna.artifex.utils.DistanceField.median;
import static io.lacuna.artifex.Vec.lerp;
import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * @author ztellman
 */
public class Images {

  public static Vec3 pixel(BufferedImage image, int x, int y) {
    int color = image.getRGB(x, y);
    return new Vec3(
            ((color >> 16) & 0xFF) / 255f,
            ((color >> 8) & 0xFF) / 255f,
            (color & 0xFF) / 255f);
  }

  public static Vec3 blit(BufferedImage image, double x, double y) {

    int x1 = (int) (x * image.getWidth());
    int x2 = min(image.getWidth() - 1, x1 + 1);
    int y1 = (int) (y * image.getHeight());
    int y2 = min(image.getHeight() - 1, y1 + 1);

    double xt = (x * image.getWidth()) - x1;
    double yt = (y * image.getHeight()) - y1;

    return lerp(
            lerp(pixel(image, x1, y1), pixel(image, x1, y2), yt),
            lerp(pixel(image, x2, y1), pixel(image, x2, y2), yt),
            xt);
  }

  public static BufferedImage distanceFieldImage(float[][][] field, float scale) {
    int w = field.length;
    int h = field[0].length;

    BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        int color = 0;
        for (int z = 0; z < 3; z++) {
          color <<= 8;
          float scaled = max(0f, min(1f, (field[x][y][z] / (scale / 2)) + 0.5f));
          color += (int) (255 * scaled);
        }
        image.setRGB(x, y, color);
      }
    }
    return image;
  }

  public static BufferedImage renderDistanceField(BufferedImage fieldImage, int w, int h, float blur) {
    BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_USHORT_GRAY);
    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        Vec3 color = blit(fieldImage, ((double) x) / w, ((double) y) / h);
        double val = median(color.x, color.y, color.z);
        double lo = 0.5 - blur / 2;
        val = min(1, max(0, (val - lo) / blur));
        image.setRGB(x, y, (int) (val * Short.MAX_VALUE * 2));
      }
    }
    return image;
  }

  public static BufferedImage testRenderDistanceField(BufferedImage fieldImage, int w, int h, float blur) {
    BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        Vec3 color = blit(fieldImage, ((double) x) / w, ((double) y) / h);
        color = color.map(n -> n >= 0.5 ? 255 : 0);
        image.setRGB(x, y, (int) color.x << 16 | (int) color.y << 8 | (int) color.z);
      }
    }
    return image;
  }



}
