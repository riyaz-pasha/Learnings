import java.util.Arrays;

class ShortestPathFloydWarshall {

    // Use long INF to avoid overflow in sums.
    private static final long INF = Long.MAX_VALUE / 4;
    // Use NEG_INF to mark distances that are -infinity due to negative cycles.
    private static final long NEG_INF = Long.MIN_VALUE / 4;

    /**
     * Runs Floyd–Warshall on an adjacency matrix representation.
     *
     * Input contract:
     * - n: number of vertices (0..n-1)
     * - graph: int[n][n] where
     * graph[u][v] >= 0 -> edge weight from u to v
     * graph[u][v] == -1 -> no edge
     *
     * Returns long[n][n] where:
     * - dist[u][v] == INF means v unreachable from u
     * - dist[u][v] == NEG_INF means shortest path is -infinity (reachable negative
     * cycle)
     * - otherwise dist[u][v] is the shortest path weight (as long)
     */
    public long[][] shortestPath(int n, int[][] graph) {
        long[][] dist = new long[n][n];

        // init
        for (int i = 0; i < n; i++) {
            Arrays.fill(dist[i], INF);
            for (int j = 0; j < n; j++) {
                if (graph[i][j] != -1) {
                    dist[i][j] = graph[i][j];
                }
            }
            dist[i][i] = Math.min(dist[i][i], 0L); // self distance is 0 unless a negative self-loop exists
        }

        // standard Floyd–Warshall
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                if (dist[i][k] == INF)
                    continue; // small optimization
                for (int j = 0; j < n; j++) {
                    if (dist[k][j] == INF)
                        continue;
                    long candidate = dist[i][k] + dist[k][j];
                    if (candidate < dist[i][j]) {
                        dist[i][j] = candidate;
                    }
                }
            }
        }

        // detect negative cycles and propagate NEG_INF to all affected pairs
        for (int k = 0; k < n; k++) {
            if (dist[k][k] < 0) {
                // any pair (i,j) that can go through k gets -INF
                for (int i = 0; i < n; i++) {
                    if (dist[i][k] == INF)
                        continue;
                    for (int j = 0; j < n; j++) {
                        if (dist[k][j] == INF)
                            continue;
                        dist[i][j] = NEG_INF;
                    }
                }
            }
        }

        return dist;
    }
}

/*
 * Complexity
 * - Time: O(n^3) (three nested loops).
 * - Space: O(n^2) for the distance matrix.
 * 
 * When to use Floyd–Warshall
 * - Small to medium dense graphs (n ≲ 400–700 depending on time limits).
 * - When you need all-pairs shortest paths or to answer many queries quickly.
 * - It supports negative weights and can detect negative cycles (unlike
 * Dijkstra).
 * 
 * Alternatives
 * - For sparse graphs and single-source queries: use Dijkstra (O((V + E) log
 * V)).
 * - For multiple single-source queries: run Dijkstra from each source (if
 * feasible).
 * - For negative weights but only single-source: use Bellman–Ford.
 */
