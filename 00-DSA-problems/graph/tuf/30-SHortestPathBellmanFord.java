import java.util.Arrays;
import java.util.List;

class ShortestPathBellmanFord {

    record Edge(int src, int dst, int weight) {
    }

    // Use long distances to avoid overflow when summing weights.
    private static final long INF = Long.MAX_VALUE / 4;

    /**
     * Returns shortest distances from src to every vertex as a long[].
     * Throws IllegalStateException if a negative-weight cycle is reachable.
     */
    public long[] shortestPath(int n, List<Edge> edges, int src) {
        long[] dist = new long[n];
        Arrays.fill(dist, INF);
        dist[src] = 0L;

        // Relax edges up to (n-1) times
        for (int iter = 0; iter < n - 1; iter++) {
            boolean updated = false;
            for (Edge e : edges) {
                int u = e.src();
                int v = e.dst();
                long w = e.weight();
                if (dist[u] != INF && dist[u] + w < dist[v]) {
                    dist[v] = dist[u] + w;
                    updated = true;
                }
            }
            // early stop: if nothing changed in this pass, distances are final
            if (!updated)
                break;
        }

        // Check for negative-weight cycles reachable from source
        for (Edge e : edges) {
            int u = e.src();
            int v = e.dst();
            long w = e.weight();
            if (dist[u] != INF && dist[u] + w < dist[v]) {
                throw new IllegalStateException("Graph contains a negative-weight cycle reachable from source");
            }
        }

        return dist;
    }

    // Example quick test (manual)
    public static void main(String[] args) {
        List<Edge> edges = List.of(
                new Edge(0, 1, 4),
                new Edge(0, 2, 5),
                new Edge(1, 2, -6),
                new Edge(2, 3, 2));
        ShortestPathBellmanFord solver = new ShortestPathBellmanFord();
        long[] d = solver.shortestPath(4, edges, 0);
        System.out.println(Arrays.toString(d)); // safe to inspect; INF indicates unreachable
    }

}
/*
 * Time: O(V * E) — relax edges V-1 times, each pass checks E edges.
 * You can do an early stop when an entire pass causes no updates (common
 * optimization).
 * 
 * Space: O(V + E) to store distances and edges (edges list + dist array).
 * 
 * Use Bellman–Ford when edges can have negative weights and you also want to
 * detect negative cycles. For non-negative weights prefer Dijkstra (O((V+E) log
 * V) with a heap).
 */
