import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 * Bellman-Ford Algorithm (Single Source Shortest Path)
 *
 * Works for:
 *  - Directed / Undirected graphs
 *  - Supports NEGATIVE edge weights
 *
 * Special Feature:
 *  - Detects NEGATIVE WEIGHT CYCLE
 *
 * Why do we need Bellman-Ford?
 *  - Dijkstra fails if negative edges exist
 *  - Bellman-Ford is slower but works safely
 *
 * Core Idea:
 *  - Relax all edges (V - 1) times
 *  - Because the shortest path can have at most (V - 1) edges
 *    (a shortest path never repeats vertices, otherwise cycle exists)
 *
 * Negative Cycle Check:
 *  - Do one more relaxation round
 *  - If we can still reduce distance => negative cycle exists
 *
 * Time Complexity: O(V * E)
 * Space Complexity: O(V)
 */
class BellmanFordAlgorithm {

    static class Edge {
        int from;
        int to;
        int weight;

        Edge(int from, int to, int weight) {
            this.from = from;
            this.to = to;
            this.weight = weight;
        }
    }

    /*
     * Returns shortest distances from source to all vertices.
     *
     * If a negative cycle is reachable from source,
     * we return null (or you can throw exception).
     */
    public int[] shortestPath(int V, List<Edge> edges, int source) {

        int[] dist = new int[V];
        Arrays.fill(dist, Integer.MAX_VALUE);

        dist[source] = 0;

        // ----------------------------------------------------
        // Step 1: Relax all edges (V - 1) times
        // ----------------------------------------------------
        for (int i = 1; i <= V - 1; i++) {

            boolean updated = false;

            for (Edge edge : edges) {

                // If "from" is unreachable, skip
                if (dist[edge.from] == Integer.MAX_VALUE) {
                    continue;
                }

                // Relaxation:
                // if dist[u] + w < dist[v] => update dist[v]
                if (dist[edge.from] + edge.weight < dist[edge.to]) {
                    dist[edge.to] = dist[edge.from] + edge.weight;
                    updated = true;
                }
            }

            // Optimization:
            // If in one full pass no updates happen,
            // then shortest paths are already finalized.
            if (!updated) {
                break;
            }
        }

        // ----------------------------------------------------
        // Step 2: Check for Negative Weight Cycle
        // ----------------------------------------------------
        for (Edge edge : edges) {

            if (dist[edge.from] == Integer.MAX_VALUE) {
                continue;
            }

            // If we can still relax, it means negative cycle exists
            if (dist[edge.from] + edge.weight < dist[edge.to]) {
                System.out.println("Negative weight cycle detected!");
                return null;
            }
        }

        return dist;
    }

}

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
