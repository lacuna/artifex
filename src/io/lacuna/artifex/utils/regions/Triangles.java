package io.lacuna.artifex.utils.regions;

import io.lacuna.artifex.Vec2;
import io.lacuna.artifex.utils.EdgeList;
import io.lacuna.artifex.utils.EdgeList.HalfEdge;
import io.lacuna.artifex.utils.Scalars;
import io.lacuna.bifurcan.*;

import java.util.Comparator;

import static io.lacuna.artifex.Vec2.angleBetween;
import static io.lacuna.artifex.utils.regions.Hulls.INSIDE;

/**
 * @author ztellman
 */
public class Triangles {

  private static final Comparator<Vec2> COMPARATOR = Comparator
    .comparingDouble((Vec2 a) -> -a.y)
    .thenComparingDouble(a -> a.x);

  public static void triangulate(EdgeList edges) {
    for (HalfEdge e : edges.faces()) {
      if (e.flag == INSIDE) {
        triangulate(edges, e);
      }
    }
  }

  public static void triangulate(EdgeList edges, HalfEdge init) {

    IList<HalfEdge> halfEdges = new LinearList<>();

    HalfEdge curr = init;
    do {
      halfEdges.addLast(curr);
      curr = curr.next;
    } while (curr != init);

    if (halfEdges.size() <= 3) {
      return;
    }
    halfEdges = Lists.sort(halfEdges, (a, b) -> COMPARATOR.compare(a.start(), b.start()));

    ISet<Vec2> left = new LinearSet<>();
    curr = halfEdges.first();
    while (curr != halfEdges.last()) {
      left.add(curr.start());
      curr = curr.next;
    }

    IList<Vec2> vertices = halfEdges.stream().map(HalfEdge::start).collect(Lists.linearCollector());

    LinearList<Vec2> stack = new LinearList<Vec2>()
      .addLast(vertices.nth(0))
      .addLast(vertices.nth(1));

    for (int i = 2; i < vertices.size() - 1; i++) {
      Vec2 u = vertices.nth(i);
      boolean isLeft = left.contains(u);

      if (isLeft == left.contains(stack.last())) {
        Vec2 popped = stack.popLast();
        while (stack.size() > 0) {
          Vec2 v = stack.last();

          double theta = -angleBetween(popped.sub(u), v.sub(u));
          boolean isVisible = isLeft
            ? Scalars.inside(1e-3, theta, Math.PI)
            : Scalars.inside(1e-3, (Math.PI * 2) - theta, Math.PI);

          if (isVisible) {
            popped = stack.popLast();
            edges.add(u, v, INSIDE, INSIDE);
          } else {
            break;
          }
        }

        stack
          .addLast(popped)
          .addLast(u);
      } else {
        while (stack.size() > 1) {
          Vec2 v = stack.popLast();
          edges.add(u, v, INSIDE, INSIDE);
        }
        stack
          .removeLast()
          .addLast(vertices.nth(i - 1))
          .addLast(u);
      }
    }

    stack.removeLast();
    while (stack.size() > 1) {
      edges.add(vertices.last(), stack.popLast(), INSIDE, INSIDE);
    }
  }

}
