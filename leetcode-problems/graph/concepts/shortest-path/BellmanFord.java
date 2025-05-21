import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class BellmanFord {
    private final int INF = Integer.MAX_VALUE;
    private final int numVertices;
    private final List<Edge> edges;

    public BellmanFord(int numVertices) {
        this.numVertices = numVertices;
        this.edges = new ArrayList<>();
    }

    public int[] shortestPaths(int sourceVertex) {
        int[] distances = initializeDistance(sourceVertex);
        for (int relax = 0; relax < numVertices - 1; relax++) {
            for (Edge edge : edges) {
                relaxEdge(edge, distances);
            }
        }
        if (hasNegativeCycle(distances)) {
            throw new Error("Has Negative cycle. Can not find shortest distance");
        }
        return distances;
    }

    private boolean hasNegativeCycle(int[] distances) {
        for (Edge edge : edges) {
            if (canUpdateDistance(edge, distances)) {
                return true;
            }
        }
        return false;
    }

    private void relaxEdge(BellmanFord.Edge edge, int[] distances) {
        if (canUpdateDistance(edge, distances)) {
            distances[edge.destination] = distances[edge.source] + edge.weight;
        }
    }

    private boolean canUpdateDistance(BellmanFord.Edge edge, int[] distances) {
        return distances[edge.source] != INF
                && distances[edge.source] + edge.weight < distances[edge.destination];
    }

    private int[] initializeDistance(int sourceVertex) {
        int[] distances = new int[numVertices];
        Arrays.fill(distances, INF);
        distances[sourceVertex] = 0;
        return distances;
    }

    public void addEdge(int source, int destination, int weight) {
        this.edges.add(new Edge(source, destination, weight));
    }

    private class Edge {
        int source, destination, weight;

        public Edge(int source, int destination, int weight) {
            this.source = source;
            this.destination = destination;
            this.weight = weight;
        }
    }
}

/*
 * Bellman-Ford Algorithm - Step-by-Step Explanation
 *
 * Step 1: Initialize
 *   - Set distance[] to ∞ (Integer.MAX_VALUE) for all vertices.
 *   - Set distance[source] = 0.
 *
 * Step 2: Relax all edges (V - 1) times:
 *   - For each edge (u → v) with weight w:
 *       If distance[u] + w < distance[v]:
 *           distance[v] = distance[u] + w
 *   - Repeat this for all edges and for (V - 1) iterations.
 *   - Why (V - 1) times? The shortest path can have at most (V - 1) edges.
 *
 * Step 3: Check for Negative Weight Cycles
 *   - Perform one more pass over all edges.
 *   - If any edge still offers a shorter path:
 *       A negative weight cycle exists, and the algorithm should stop.
 *
 * Step 4: Output
 *   - If no negative cycles, distance[] contains the shortest distances
 *     from the source to all other vertices.
 *
 * Time Complexity: O(V * E), where V = number of vertices, E = number of edges.
 * Space Complexity: O(V) for the distance array.
 *
 * Pros:
 *   - Works with graphs containing negative edge weights.
 *   - Detects negative weight cycles.
 *
 * Cons:
 *   - Slower than Dijkstra’s algorithm for graphs with non-negative weights.
 *   - Cannot produce valid shortest paths if a negative cycle is reachable.
 */


/**
 * Bellman-Ford Algorithm implementation to find the shortest path from a single source.
 * 
 * Time Complexity:
 *   - O(V * E), where V is the number of vertices and E is the number of edges.
 *   - Suitable for sparse and dense graphs, but slower than Dijkstra's for non-negative weights.
 * 
 * Space Complexity:
 *   - O(V) for storing distances.
 * 
 * Pros:
 *   - Works with negative edge weights.
 *   - Can detect negative weight cycles.
 * 
 * Cons:
 *   - Slower than Dijkstra’s algorithm (which is O(E + V log V) with a min-heap).
 *   - Cannot return correct paths if there’s a negative weight cycle.
 */