package io.lacuna.artifex.utils;

import io.lacuna.bifurcan.ISet;
import io.lacuna.bifurcan.LinearSet;

import java.util.*;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * A quasi-implementation of the plane sweep algorithm.  This will only compare edges which overlap on the x-axis, but
 * does not maintain a sorted tree of the y-axis to ensure only adjacent curves have intersection checks.  This means
 * that this is worst-case O(N ^ 2) rather than O(N log N), but outside of heavy-duty GIS applications I'm not sure this
 * is a real problem.  This may be worth revisiting.
 *
 * @author ztellman
 */
public class SweepQueue<T> {

  private enum EventType {
    OPEN,
    CLOSED
  }

  public static class Event<T> {

    static final Comparator<Event> COMPARATOR = Comparator
      .comparingDouble((Event e) -> e.key)
      .thenComparingInt(e -> e.type == EventType.OPEN ? 0 : 1);

    public final double key;
    public final T value;
    public final EventType type;

    Event(double key, T value, EventType type) {
      this.key = key;
      this.value = value;
      this.type = type;
    }
  }

  private final PriorityQueue<Event<T>> queue = new PriorityQueue<>(Event.COMPARATOR);
  private final ISet<T> set = new LinearSet<>();

  public void add(T value, double a, double b) {
    if (!set.contains(value)) {
      queue.add(new Event<>(min(a, b), value, EventType.OPEN));
      queue.add(new Event<>(max(a, b), value, EventType.CLOSED));
    }
  }

  public double peek() {
    return queue.isEmpty() ? Double.MAX_VALUE : queue.peek().key;
  }

  private static <T> int compare(SweepQueue<T> a, SweepQueue<T> b) {
    return Event.COMPARATOR.compare(a.queue.peek(), b.queue.peek());
  }

  public static <T> int next(SweepQueue<T>... queues) {
    for (; ; ) {
      int minIdx = 0;
      for (int i = 1; i < queues.length; i++) {
        if (queues[minIdx].queue.isEmpty() || (!queues[i].queue.isEmpty() && compare(queues[i], queues[minIdx]) < 0)) {
          minIdx = i;
        }
      }

      SweepQueue<T> q = queues[minIdx];
      if (q.queue.isEmpty() || q.queue.peek().type == EventType.OPEN) {
        return minIdx;
      } else {
        q.next();
      }
    }
  }

  public Event<T> next() {
    Event<T> e = queue.poll();
    if (e == null) {
      return null;
    }

    if (e.type == EventType.CLOSED) {
      set.remove(e.value);
    } else {
      set.add(e.value);
    }
    return e;
  }

  public T take() {
    while (!queue.isEmpty()) {
      Event<T> e = next();
      if (e.type == EventType.OPEN) {
        return e.value;
      }
    }
    return null;
  }

  public ISet<T> active() {
    return set;
  }
}
