package algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Successive shortest augmenting paths using Bellman-Ford for residual reverse edges. */
public final class MinCostMaxFlow {
    public static final class Edge {
        public final int to;
        public final int cost;
        private int capacity;
        private final int initialCapacity;
        private Edge reverse;

        private Edge(int to, int capacity, int cost) {
            this.to = to;
            this.capacity = capacity;
            this.initialCapacity = capacity;
            this.cost = cost;
        }

        public int flow() { return initialCapacity - capacity; }
    }

    private final List<List<Edge>> graph;

    public MinCostMaxFlow(int vertices) {
        graph = new ArrayList<>(vertices);
        for (int i = 0; i < vertices; i++) graph.add(new ArrayList<>());
    }

    public Edge addEdge(int from, int to, int capacity, int cost) {
        if (capacity < 0) throw new IllegalArgumentException("Capacity cannot be negative.");
        Edge forward = new Edge(to, capacity, cost);
        Edge reverse = new Edge(from, 0, -cost);
        forward.reverse = reverse;
        reverse.reverse = forward;
        graph.get(from).add(forward);
        graph.get(to).add(reverse);
        return forward;
    }

    /** Maximizes flow first, then minimizes total cost among maximum flows. */
    public int[] solve(int source, int sink) {
        int flow = 0;
        long totalCost = 0;
        int n = graph.size();
        while (true) {
            long[] distance = new long[n];
            Arrays.fill(distance, Long.MAX_VALUE / 4);
            Edge[] previous = new Edge[n];
            int[] previousNode = new int[n];
            distance[source] = 0;
            boolean changed = true;
            for (int k = 0; k < n - 1 && changed; k++) {
                changed = false;
                for (int u = 0; u < n; u++) {
                    if (distance[u] == Long.MAX_VALUE / 4) continue;
                    for (Edge edge : graph.get(u)) {
                        if (edge.capacity > 0 && distance[edge.to] > distance[u] + edge.cost) {
                            distance[edge.to] = distance[u] + edge.cost;
                            previous[edge.to] = edge;
                            previousNode[edge.to] = u;
                            changed = true;
                        }
                    }
                }
            }
            if (previous[sink] == null) break;
            int amount = Integer.MAX_VALUE;
            for (int v = sink; v != source; v = previousNode[v]) amount = Math.min(amount, previous[v].capacity);
            for (int v = sink; v != source; v = previousNode[v]) {
                Edge edge = previous[v];
                edge.capacity -= amount;
                edge.reverse.capacity += amount;
            }
            flow += amount;
            totalCost += (long) amount * distance[sink];
        }
        return new int[]{flow, (int) Math.min(Integer.MAX_VALUE, totalCost)};
    }
}
