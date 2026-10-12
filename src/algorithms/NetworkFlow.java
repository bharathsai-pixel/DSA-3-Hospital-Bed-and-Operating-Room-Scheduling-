package algorithms;

import java.util.*;

/**
 * Implementation of the Edmonds-Karp Network Flow Algorithm (O(VE^2))
 * Solves the Max-Flow problem.
 * Used for routing patients across multiple hospitals to maximize resource utilization,
 * bypassing full hospitals.
 */
public class NetworkFlow {
    private int V; // Number of vertices
    private int[][] capacity; // Capacity graph
    private int[][] flow; // Flow graph
    private List<List<Integer>> graph;

    public NetworkFlow(int vertices) {
        this.V = vertices;
        this.capacity = new int[V][V];
        this.flow = new int[V][V];
        this.graph = new ArrayList<>(V);
        for (int i = 0; i < V; i++) {
            graph.add(new ArrayList<>());
        }
    }

    public void addEdge(int u, int v, int cap) {
        capacity[u][v] = cap;
        graph.get(u).add(v);
        graph.get(v).add(u); // Reverse edge for residual graph
    }

    private boolean bfs(int source, int sink, int[] parent) {
        boolean[] visited = new boolean[V];
        Queue<Integer> queue = new LinkedList<>();

        queue.add(source);
        visited[source] = true;
        parent[source] = -1;

        while (!queue.isEmpty()) {
            int u = queue.poll();

            for (int v : graph.get(u)) {
                // If not visited and residual capacity is > 0
                if (!visited[v] && capacity[u][v] - flow[u][v] > 0) {
                    queue.add(v);
                    parent[v] = u;
                    visited[v] = true;
                    if (v == sink) {
                        return true; // Path found
                    }
                }
            }
        }
        return false;
    }

    public int getMaxFlow(int source, int sink) {
        int maxFlow = 0;
        int[] parent = new int[V];

        // While there is a path from source to sink in the residual graph
        while (bfs(source, sink, parent)) {
            // Find the minimum residual capacity of the edges along the path
            int pathFlow = Integer.MAX_VALUE;
            int s = sink;
            while (s != source) {
                int p = parent[s];
                pathFlow = Math.min(pathFlow, capacity[p][s] - flow[p][s]);
                s = p;
            }

            // Update residual capacities of the edges and reverse edges along the path
            s = sink;
            while (s != source) {
                int p = parent[s];
                flow[p][s] += pathFlow;
                flow[s][p] -= pathFlow; // Reverse flow
                s = p;
            }

            maxFlow += pathFlow;
        }
        return maxFlow;
    }

    public int[][] getFlowMatrix() {
        return flow;
    }
}
