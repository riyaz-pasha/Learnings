import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

class Edge {
    int to, cost;

    public Edge(int to, int cost) {
        this.to = to;
        this.cost = cost;
    }
}

class State {
    int city, cost, stops;

    public State(int city, int cost, int stops) {
        this.city = city;
        this.cost = cost;
        this.stops = stops;
    }
}

class CheapestFlightsKStops {

    public int findCheapestPrice(int n, int[][] flights, int src, int dst, int k) {
        Map<Integer, List<Edge>> graph = new HashMap<>();
        for (int[] flight : flights) {
            graph.computeIfAbsent(flight[0], x -> new ArrayList<>()).add(new Edge(flight[1], flight[2]));
        }

        PriorityQueue<State> pq = new PriorityQueue<>((a, b) -> a.cost - b.cost);
        pq.offer(new State(src, 0, 0));

        Map<Integer, Integer> visited = new HashMap<>();

        while (!pq.isEmpty()) {
            State cur = pq.poll();

            if (cur.city == dst)
                return cur.cost;

            if (cur.stops > k)
                continue;

            // Prune if visited with fewer stops before
            if (visited.containsKey(cur.city) && visited.get(cur.city) <= cur.stops)
                continue;
            visited.put(cur.city, cur.stops);

            for (Edge edge : graph.getOrDefault(cur.city, Collections.emptyList())) {
                pq.offer(new State(edge.to, cur.cost + edge.cost, cur.stops + 1));
            }
        }

        return -1;
    }

    /*
     * 🔹 Time Complexity:
     * Time Complexity: O(E log(V * K)). This is similar to Dijkstra's algorithm.
     * Each edge E can be pushed into the priority queue. The size of the priority
     * queue is bounded by V * K, where V is the number of cities and K is the
     * number of stops.
     * 
     * 🔹 Space Complexity:
     * O(V * K) to store the dist array and the priority queue.
     * The size of the priority queue can be at most V * K.
     */

}

/*
 * ✅ Clarifying Questions
 * Is K the number of stops or edges?
 * - Stops = number of intermediate nodes (i.e., edges = stops + 1)
 * 
 * Can there be multiple flights between the same two cities?
 * - Yes, handle duplicates with different costs.
 * 
 * Is it a directed graph?
 * - Yes, flights[i] = [from, to, cost] implies directed edge.
 * 
 * Can cost be negative?
 * - No, assume non-negative costs.
 * 
 * Do we count the starting city as a stop?
 * - No. A stop is when you land in an intermediate city, not source or
 * destination.
 */

class BellmanFordSolution {

    /*
     * ❓Problem:
     * If flight costs can be negative, Dijkstra’s algorithm fails because it
     * assumes once a node is visited with the shortest cost, it never needs to be
     * updated again.
     * 
     * ✅ Solution: Use Bellman-Ford Algorithm
     * Bellman-Ford works fine with negative weights and can detect negative cycles
     * (if needed).
     * To handle at most K stops, we adapt Bellman-Ford to K+1 iterations.
     * 
     * ✅ Why This Works
     * Each iteration allows up to 1 more stop.
     * 
     * After K+1 passes, we’ve computed the minimum cost to each node using at most
     * K+1 edges (i.e., at most K stops).
     * 
     */
    public int findCheapestPrice(int n, int[][] flights, int src, int dst, int k) {
        int[] prev = new int[n];
        Arrays.fill(prev, Integer.MAX_VALUE);
        prev[src] = 0;

        for (int i = 0; i <= k; i++) {
            int[] curr = Arrays.copyOf(prev, n);
            for (int[] flight : flights) {
                int u = flight[0], v = flight[1], w = flight[2];
                if (prev[u] != Integer.MAX_VALUE) {
                    curr[v] = Math.min(curr[v], prev[u] + w);
                }
            }
            prev = curr;
        }

        return prev[dst] == Integer.MAX_VALUE ? -1 : prev[dst];
    }
    /*
     * Time: O(K × E) → At most K+1 passes over E edges
     * Space: O(V) → Two arrays of size n (prev and curr)
     */

}

// ---------------------------------------------------------------------------

/* What if you need to find the cheapest flights for a series of queries */

