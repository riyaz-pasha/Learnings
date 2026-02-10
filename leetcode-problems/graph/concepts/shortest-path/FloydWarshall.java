
/*
 * Floyd Warshall Algorithm (All-Pairs Shortest Path)
 *
 * Goal:
 * Find shortest distance between every pair of vertices (i -> j).
 *
 * Works for:
 * - Directed / Undirected graphs
 * - Supports negative weights
 *
 * IMPORTANT:
 * - Graph must NOT contain a negative weight cycle
 *   (if it does, shortest path is not well-defined).
 *
 * Core Idea:
 * dp[i][j] = shortest distance from i -> j
 *
 * Transition:
 * If we allow an intermediate node "k", then:
 *
 * dp[i][j] = min(dp[i][j], dp[i][k] + dp[k][j])
 *
 * Meaning:
 * Either shortest path i->j does not use k,
 * or it uses k somewhere in between.
 *
 * Time Complexity: O(V^3)
 * Space Complexity: O(V^2)
 */
class FloydWarshall {

    private static final int INF = (int) 1e9; // large number to represent infinity

    /*
     * Input: adjacency matrix graph
     *
     * graph[i][j] = weight of edge i -> j
     * graph[i][j] = INF if no edge exists
     * graph[i][i] = 0
     *
     * Output: dp matrix of shortest distances between all pairs
     */
    public int[][] shortestPaths(int[][] graph) {

        int V = graph.length;

        // dp[i][j] will store shortest distance from i -> j
        int[][] dp = new int[V][V];

        // Step 1: Initialize dp with the given graph distances
        for (int i = 0; i < V; i++) {
            for (int j = 0; j < V; j++) {
                dp[i][j] = graph[i][j];
            }
        }

        // Step 2: Try every vertex as an intermediate node
        for (int k = 0; k < V; k++) {

            // Step 3: For every pair (i, j), update using intermediate k
            for (int i = 0; i < V; i++) {
                for (int j = 0; j < V; j++) {

                    // If i->k or k->j is unreachable, skip
                    if (dp[i][k] == INF || dp[k][j] == INF) {
                        continue;
                    }

                    // Relaxation: check if going through k gives a shorter path
                    dp[i][j] = Math.min(dp[i][j], dp[i][k] + dp[k][j]);
                }
            }
        }

        return dp;
    }

    /*
     * Negative cycle detection:
     *
     * After Floyd Warshall,
     * if dp[i][i] < 0 for any i,
     * it means there is a negative cycle reachable from i.
     */
    public boolean hasNegativeCycle(int[][] dp) {

        int V = dp.length;

        for (int i = 0; i < V; i++) {
            if (dp[i][i] < 0) {
                return true;
            }
        }

        return false;
    }

    // ----------------------------
    // Example usage
    // ----------------------------
    public static void main(String[] args) {

        FloydWarshall solver = new FloydWarshall();

        int V = 4;

        int[][] graph = {
                {0, 3, INF, 7},
                {8, 0, 2, INF},
                {5, INF, 0, 1},
                {2, INF, INF, 0}
        };

        int[][] dp = solver.shortestPaths(graph);

        if (solver.hasNegativeCycle(dp)) {
            System.out.println("Negative Cycle Detected!");
        } else {
            System.out.println("All-Pairs Shortest Paths:");
            for (int i = 0; i < V; i++) {
                for (int j = 0; j < V; j++) {
                    if (dp[i][j] == INF) {
                        System.out.print("INF ");
                    } else {
                        System.out.print(dp[i][j] + " ");
                    }
                }
                System.out.println();
            }
        }
    }
}

/*
 * Floyd Warshall tries every node as an intermediate point.
 * If shortest path i->j can be improved using k,
 * then dp[i][j] = dp[i][k] + dp[k][j].
 */


class FloydWarshall2 {

    private final int INF = Integer.MAX_VALUE;

    public int[][] shortestPaths(int numVertices, int[][] graph) {
        int[][] dist = new int[numVertices][numVertices];
        for (int source = 0; source < numVertices; source++) {
            for (int destination = 0; destination < numVertices; destination++) {
                dist[source][destination] = graph[source][destination];
            }
        }

        for (int intermediate = 0; intermediate < numVertices; intermediate++) {
            for (int source = 0; source < numVertices; source++) {
                for (int destination = 0; destination < numVertices; destination++) {
                    if (dist[source][intermediate] != INF
                            && dist[intermediate][destination] != INF
                            && (dist[source][intermediate]
                                    + dist[intermediate][destination]) < dist[source][destination]) {
                        dist[source][destination] = dist[source][intermediate] + dist[intermediate][destination];
                    }
                }
            }
        }

        return dist;
    }

}
