package io.lacuna.artifex.utils;

import io.lacuna.artifex.Box2;
import io.lacuna.artifex.Vec2;

import java.util.Collections;
import java.util.List;

/**
 * @author ztellman
 */
public class BinPack {

    private static List<Box2> remainder(Box2 box, Vec2 size) {
        if (size.equals(box.size())) {
            return Collections.emptyList();
        }

        return null;
    }

    public static List<Box2> pack(Iterable<Vec2> sizes) {
        return null;
    }
}
