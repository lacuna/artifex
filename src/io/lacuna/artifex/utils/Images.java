package io.lacuna.artifex.utils;

import io.lacuna.artifex.Vec3;

import java.awt.image.BufferedImage;

import static java.lang.Math.min;

/**
 * @author ztellman
 */
public class Images {
  public static double median(double a, double b, double c) {
    if (a < b) {
      return b < c ? b : c;
    } else {
      return a < c ? a : c;
    }
  }

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

    return Vec3.lerp(
            Vec3.lerp(pixel(image, x1, y1), pixel(image, x1, y2), yt),
            Vec3.lerp(pixel(image, x2, y1), pixel(image, x2, y2), yt),
            xt);
  }

}
