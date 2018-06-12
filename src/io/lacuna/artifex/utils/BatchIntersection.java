package io.lacuna.artifex.utils;

import io.lacuna.artifex.Curve2;
import io.lacuna.bifurcan.IList;
import io.lacuna.bifurcan.ISet;
import io.lacuna.bifurcan.LinearList;
import io.lacuna.bifurcan.LinearSet;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

/**
 * @author ztellman
 */
public class BatchIntersection {

  private enum EventType {
    OPEN,
    CLOSED
  }

  public static class Intersection<T> {
    public final T a, b;

    public Intersection(T a, T b) {
      this.a = a;
      this.b = b;
    }
  }

  private static class Event<T> {

    static final Comparator<Event> COMPARATOR = Comparator
      .comparingDouble((Event e) -> e.key)
      .thenComparing(e -> e.type == EventType.OPEN ? 0 : 1);

    final double key;
    final T value;
    final EventType type;
    final byte set;

    public Event(double key, T value, EventType type, int set) {
      this.key = key;
      this.value = value;
      this.type = type;
      this.set = (byte) set;
    }
  }

  public static <T> IList<Intersection<T>> intersections(Iterable<T> a, Iterable<T> b, Function<T, Curve2> curve) {

    IList<Intersection<T>> result = new LinearList<>();
    PriorityQueue<Event<T>> queue = new PriorityQueue<>(Event.COMPARATOR);
    ISet<T>[] active = new ISet[] {new LinearSet(), new LinearSet()};

    for (int i = 0; i < 2; i++) {
      Iterable<T> it = i == 0 ? a : b;
      for (T e : it) {
        Curve2 c = curve.apply(e);
        double p0 = c.start().x;
        double p1 = c.end().x;
        boolean inOrder = p0 < p1;
        queue.add(new Event<T>(p0, e, inOrder ? EventType.OPEN : EventType.CLOSED, i));
        queue.add(new Event<T>(p1, e, inOrder ? EventType.CLOSED : EventType.OPEN, i));
      }
    }

    while (!queue.isEmpty()) {
      Event<T> event = queue.poll();
      if (event.type == EventType.OPEN) {
        active[event.set].add(event.value);
        for (T e : active[event.set ^ 1]) {
          if (curve.apply(event.value).intersects(curve.apply(e))) {
            result.addLast(new Intersection<>(e, event.value));
          }
        }
      } else {
        active[event.set].remove(event.value);
      }
    }

    return result;
  }
}
