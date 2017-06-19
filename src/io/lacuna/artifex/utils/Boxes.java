package io.lacuna.artifex.utils;

import io.lacuna.artifex.Interval2;
import io.lacuna.artifex.Vec2;

import java.util.Collections;
import java.util.List;

/**
 * @author ztellman
 */
public class Boxes {

    private static List<Interval2> remainder(Interval2 box, Vec2 size) {
        if (size.equals(box.size())) {
            return Collections.emptyList();
        }

        return null;
    }

    public static List<Interval2> pack(Iterable<Vec2> sizes) {
        return null;
    }
}
