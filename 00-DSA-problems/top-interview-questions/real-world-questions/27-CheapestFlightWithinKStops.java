import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/*
 * Find the cheapest flight cost from src to dst with at most K stops.
 *
 * Problem interpretation:
 * - n = number of cities labeled 0..n-1
 * - flights = list of [u, v, price]
 * - K = maximum number of stops (intermediate nodes) allowed
 *
 * Common interpretation:
 * - If K = 0 -> only direct flights allowed (no intermediate).
 * - We allow up to K stops => up to K+1 flights in the path.
 *
 * We'll implement the PQ approach where we keep (cost, city, stopsUsed).
 */

class CheapestFlightWithinKStops {

    static class Edge {
        int to;
        int price;
        Edge(int to, int price) { this.to = to; this.price = price; }
    }

    static class State {
        int city;
        int cost;
        int stops; // number of flights used so far

        State(int city, int cost, int stops) {
            this.city = city; this.cost = cost; this.stops = stops;
        }
    }

    /**
     * Modified Dijkstra / best-first search with stops tracking.
     *
     * @param n number of cities
     * @param flights list of edges [u, v, price]
     * @param src source city
     * @param dst destination city
     * @param K max stops (intermediate nodes)
     * @return minimum cost or -1 if no such route
     */
    public static int findCheapestPrice(int n, int[][] flights, int src, int dst, int K) {
        // Build adjacency list
        List<List<Edge>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) graph.add(new ArrayList<>());
        for (int[] f : flights) {
            int u = f[0], v = f[1], w = f[2];
            graph.get(u).add(new Edge(v, w));
        }

        // min-heap by cost
        PriorityQueue<State> pq = new PriorityQueue<>(Comparator.comparingInt(s -> s.cost));
        // start with cost 0, at src, 0 flights used
        pq.offer(new State(src, 0, 0));

        // Best seen: map (city -> minimal stops seen for given or lower cost)
        // Simpler: use two arrays: bestCost[city] or bestStops[city] for pruning.
        // We'll maintain bestCosts per (city, stops) implicitly limited by K.
        // But to be efficient we can keep best cost to reach city with <= stops flights.
        int[][] best = new int[n][K + 2]; // best[city][flightsUsed] = min cost, flightsUsed in [0..K+1]
        for (int i = 0; i < n; i++) Arrays.fill(best[i], Integer.MAX_VALUE);
        best[src][0] = 0;

        while (!pq.isEmpty()) {
            State cur = pq.poll();
            int city = cur.city, cost = cur.cost, flightsUsed = cur.stops;

            // If reached destination, return cost (first time is minimal cost due to PQ)
            if (city == dst) return cost;

            // If we have used more flights than allowed (K stops => max flights = K+1), skip expansion
            if (flightsUsed == K + 1) continue;

            // Expand neighbors
            for (Edge e : graph.get(city)) {
                int nextCity = e.to;
                int nextCost = cost + e.price;
                int nextFlightsUsed = flightsUsed + 1;

                if (nextCost < best[nextCity][nextFlightsUsed]) {
                    best[nextCity][nextFlightsUsed] = nextCost;
                    pq.offer(new State(nextCity, nextCost, nextFlightsUsed));
                }
            }
        }

        return -1; // destination not reachable within K stops
    }

    // Demo / simple tests
    public static void main(String[] args) {
        // Example 1 (from common leetcode sample)
        int n1 = 3;
        int[][] flights1 = {{0,1,100}, {1,2,100}, {0,2,500}};
        int src1 = 0, dst1 = 2, K1 = 1; // allow at most 1 stop
        System.out.println(findCheapestPrice(n1, flights1, src1, dst1, K1));
        // expected 200 via 0->1->2

        // Example 2: no valid with 0 stops
        int K2 = 0;
        System.out.println(findCheapestPrice(n1, flights1, src1, dst1, K2));
        // expected 500 (direct) because 0 stops allows only direct flights

        // Example 3: unreachable
        int[][] flights2 = {{0,1,100}, {1,0,100}}; // no connection to city 2
        System.out.println(findCheapestPrice(3, flights2, 0, 2, 1)); // expected -1
    }
}
