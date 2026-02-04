// exchange rate [fromCurrency,toCurrency,rate]
// is rate floating point number?
// directed graph?
// 

// how many currencies are there?
// can the rate be negative?
// is rate floating point number?
// is directed graph?
// how many digits of precision we are looking for?

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Arbitrage Detection using Bellman-Ford
 *
 * Key Idea:
 * ----------
 * Currency exchange rates are multiplicative.
 * Arbitrage exists if:
 *
 *   rate[a][b] * rate[b][c] * rate[c][a] > 1
 *
 * Taking log transforms multiplication into addition:
 *
 *   log(a * b * c) = log(a) + log(b) + log(c)
 *
 * Using negative log:
 *   -log(rate)
 *
 * Arbitrage ⇔ negative cycle in the transformed graph.
 */
class ArbitrageDetector {

    /**
     * Directed edge in currency graph
     * from   -> source currency
     * to     -> destination currency
     * weight -> -log(exchange rate)
     */
    record Edge(int from, int to, double weight) {}

    /**
     * Detects whether an arbitrage opportunity exists.
     *
     * Time Complexity:
     *   - Building edges: O(n^2)
     *   - Bellman-Ford relaxations: O(n * n^2) = O(n^3)
     *
     * Space Complexity:
     *   - Edge list: O(n^2)
     *   - Distance array: O(n)
     */
    public boolean findArbitrage(int n, double[][] rates) {

        // -----------------------------------------
        // 1. Convert exchange rates to graph edges
        // -----------------------------------------
        // Each exchange i -> j becomes an edge with weight = -log(rate[i][j])
        List<Edge> edges = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i != j) {
                    edges.add(new Edge(i, j, -Math.log(rates[i][j])));
                }
            }
        }

        // -----------------------------------------
        // 2. Bellman-Ford initialization
        // -----------------------------------------
        // We can start from ANY currency.
        // To ensure all nodes are reachable, initialize all distances to 0.
        double[] dist = new double[n];
        Arrays.fill(dist, 0.0);

        // -----------------------------------------
        // 3. Relax edges n - 1 times
        // -----------------------------------------
        // If no negative cycle exists, shortest paths stabilize after n - 1 passes
        for (int i = 1; i < n; i++) {
            for (Edge edge : edges) {
                if (dist[edge.from] + edge.weight < dist[edge.to]) {
                    dist[edge.to] = dist[edge.from] + edge.weight;
                }
            }
        }

        // -----------------------------------------
        // 4. One more relaxation to detect cycle
        // -----------------------------------------
        // If we can still relax, a negative cycle exists
        for (Edge edge : edges) {
            if (dist[edge.from] + edge.weight < dist[edge.to]) {
                return true; // Arbitrage opportunity found
            }
        }

        return false; // No arbitrage
    }
}
