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
public class SweepQueue<T> {

  private enum EventType {
    OPEN,
    CLOSED
  }

  static class Event<T> {

    static final Comparator<Event> COMPARATOR = Comparator
      .comparingDouble((Event e) -> e.key)
      .thenComparing(e -> e.type == EventType.OPEN ? 0 : 1);

    final double key;
    final T value;
    final EventType type;

    public Event(double key, T value, EventType type) {
      this.key = key;
      this.value = value;
      this.type = type;
    }
  }

  private final PriorityQueue<Event<T>> queue = new PriorityQueue<>(Event.COMPARATOR);
  private final ISet<T> set = new LinearSet<>();

  public void add(T value, double a, double b) {
    if (b > a) {
      double tmp = a;
      a = b;
      b = tmp;
    }

    queue.add(new Event<>(a, value, EventType.OPEN));
    queue.add(new Event<>(b, value, EventType.CLOSED));
  }

  public T next() {
    if (queue.isEmpty()) {
      return null;
    }

    Event<T> e = queue.poll();
    while (e.type == EventType.CLOSED) {
      set.remove(e.value);
      if (queue.isEmpty()) {
        return null;
      }
      e = queue.poll();
    }

    set.add(e.value);
    return e.value;
  }

  public ISet<T> active() {
    return set;
  }
}
