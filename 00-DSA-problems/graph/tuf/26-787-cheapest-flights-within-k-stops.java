import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;

/*
 * There are n cities connected by some number of flights. You are given an
 * array flights where flights[i] = [fromi, toi, pricei] indicates that there is
 * a flight from city fromi to city toi with cost pricei.
 * 
 * You are also given three integers src, dst, and k, return the cheapest price
 * from src to dst with at most k stops. If there is no such route, return -1.
 * 
 * Example 1:
 * Input: n = 4, flights = [[0,1,100],[1,2,100],[2,0,100],[1,3,600],[2,3,200]],
 * src = 0, dst = 3, k = 1
 * Output: 700
 * Explanation:
 * The graph is shown above.
 * The optimal path with at most 1 stop from city 0 to 3 is marked in red and
 * has cost 100 + 600 = 700.
 * Note that the path through cities [0,1,2,3] is cheaper but is invalid because
 * it uses 2 stops.
 * 
 * Example 2:
 * Input: n = 3, flights = [[0,1,100],[1,2,100],[0,2,500]], src = 0, dst = 2, k
 * = 1
 * Output: 200
 * Explanation:
 * The graph is shown above.
 * The optimal path with at most 1 stop from city 0 to 2 is marked in red and
 * has cost 100 + 100 = 200.
 * 
 * Example 3:
 * Input: n = 3, flights = [[0,1,100],[1,2,100],[0,2,500]], src = 0, dst = 2, k
 * = 0
 * Output: 500
 * Explanation:
 * The graph is shown above.
 * The optimal path with no stops from city 0 to 2 is marked in red and has cost
 * 500.
 */

class CheapestFlightsWithinKStopsSolution {

    record Edge(int to, int cost) {
    }

    record State(int node, int totalCost, int stops) {
    }

    public int findCheapestPrice(int n, int[][] flights, int src, int dst, int k) {
        // build adjacency list
        List<List<Edge>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++)
            adj.add(new ArrayList<>());
        for (int[] f : flights)
            adj.get(f[0]).add(new Edge(f[1], f[2]));

        // bestCost[node] = minimum cost found so far to reach node
        // using any number of stops <= current examined.
        int[] bestCost = new int[n];
        Arrays.fill(bestCost, Integer.MAX_VALUE);
        bestCost[src] = 0;

        // min-heap ordered by totalCost
        PriorityQueue<State> pq = new PriorityQueue<>(Comparator.comparingInt(State::totalCost));
        pq.offer(new State(src, 0, 0)); // at src with cost 0 and 0 stops used

        while (!pq.isEmpty()) {
            State cur = pq.poll();
            int node = cur.node();
            int cost = cur.totalCost();
            int stops = cur.stops();

            // If popped state is already worse than best known, skip it (stale)
            if (cost > bestCost[node])
                continue;

            // If reached destination, the first time we pop dst is the cheapest cost
            // because PQ is ordered by cost.
            if (node == dst)
                return cost;

            // We are allowed to take at most k stops -> expand neighbors only if
            // stops <= k (we can take another flight and that increases stops by 1).
            if (stops > k)
                continue; // cannot expand further (used too many stops)

            for (Edge e : adj.get(node)) {
                int nxt = e.to();
                int nxtCost = cost + e.cost();

                // Prune: if this new cost is better than bestCost[nxt], update and push.
                // Note: we push only when we improved the best known cost to nxt.
                if (nxtCost < bestCost[nxt]) {
                    bestCost[nxt] = nxtCost;
                    pq.offer(new State(nxt, nxtCost, stops + 1));
                }
            }
        }

        return -1; // not reachable within k stops
    }

}

class CheapestFlightsKStops {

    // Solution 1: BFS with Level-Order Traversal (MOST INTUITIVE)
    // Time Complexity: O(n + E * k) where E is number of flights
    // Space Complexity: O(n + E)
    public int findCheapestPrice(int n, int[][] flights, int src, int dst, int k) {
        // Build adjacency list
        Map<Integer, List<int[]>> graph = new HashMap<>();
        for (int[] flight : flights) {
            graph.putIfAbsent(flight[0], new ArrayList<>());
            graph.get(flight[0]).add(new int[] { flight[1], flight[2] });
        }

        // Track minimum cost to reach each city
        int[] minCost = new int[n];
        Arrays.fill(minCost, Integer.MAX_VALUE);

        Queue<int[]> queue = new LinkedList<>();
        queue.offer(new int[] { src, 0 }); // {city, cost}
        int stops = 0;

        while (!queue.isEmpty() && stops <= k) {
            int size = queue.size();

            for (int i = 0; i < size; i++) {
                int[] curr = queue.poll();
                int city = curr[0];
                int cost = curr[1];

                if (!graph.containsKey(city))
                    continue;

                for (int[] next : graph.get(city)) {
                    int nextCity = next[0];
                    int nextCost = cost + next[1];

                    // Only explore if we found a cheaper path
                    if (nextCost < minCost[nextCity]) {
                        minCost[nextCity] = nextCost;
                        queue.offer(new int[] { nextCity, nextCost });
                    }
                }
            }
            stops++;
        }

        return minCost[dst] == Integer.MAX_VALUE ? -1 : minCost[dst];
    }

    // Solution 2: Modified Dijkstra's Algorithm with Priority Queue
    // Time Complexity: O((E + n) * log(E))
    // Space Complexity: O(n + E)
    public int findCheapestPriceDijkstra(int n, int[][] flights, int src, int dst, int k) {
        // Build adjacency list
        Map<Integer, List<int[]>> graph = new HashMap<>();
        for (int[] flight : flights) {
            graph.putIfAbsent(flight[0], new ArrayList<>());
            graph.get(flight[0]).add(new int[] { flight[1], flight[2] });
        }

        // Priority queue: {cost, city, stops}
        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[0] - b[0]);
        pq.offer(new int[] { 0, src, 0 });

        // Track minimum stops to reach each city at each cost
        int[][] visited = new int[n][k + 2];
        for (int[] row : visited) {
            Arrays.fill(row, Integer.MAX_VALUE);
        }

        while (!pq.isEmpty()) {
            int[] curr = pq.poll();
            int cost = curr[0];
            int city = curr[1];
            int stops = curr[2];

            if (city == dst) {
                return cost;
            }

            if (stops > k) {
                continue;
            }

            // Skip if we've visited this state with fewer stops
            if (stops >= visited[city][stops]) {
                continue;
            }
            visited[city][stops] = stops;

            if (!graph.containsKey(city)) {
                continue;
            }

            for (int[] next : graph.get(city)) {
                int nextCity = next[0];
                int nextCost = cost + next[1];

                pq.offer(new int[] { nextCost, nextCity, stops + 1 });
            }
        }

        return -1;
    }

    // Solution 3: Bellman-Ford Algorithm (MOST ELEGANT)
    // Time Complexity: O(k * E)
    // Space Complexity: O(n)
    public int findCheapestPriceBellmanFord(int n, int[][] flights, int src, int dst, int k) {
        // Distance array
        int[] dist = new int[n];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[src] = 0;

        // Relax edges k+1 times (k stops = k+1 flights)
        for (int i = 0; i <= k; i++) {
            int[] temp = dist.clone();

            for (int[] flight : flights) {
                int from = flight[0];
                int to = flight[1];
                int price = flight[2];

                if (dist[from] != Integer.MAX_VALUE) {
                    temp[to] = Math.min(temp[to], dist[from] + price);
                }
            }

            dist = temp;
        }

        return dist[dst] == Integer.MAX_VALUE ? -1 : dist[dst];
    }

    // Solution 4: Dynamic Programming (2D)
    // Time Complexity: O(k * E)
    // Space Complexity: O(k * n)
    public int findCheapestPriceDP(int n, int[][] flights, int src, int dst, int k) {
        // dp[i][j] = min cost to reach city j using at most i flights
        int[][] dp = new int[k + 2][n];

        for (int[] row : dp) {
            Arrays.fill(row, Integer.MAX_VALUE);
        }

        // Base case: cost to reach source is 0
        for (int i = 0; i <= k + 1; i++) {
            dp[i][src] = 0;
        }

        for (int i = 1; i <= k + 1; i++) {
            for (int[] flight : flights) {
                int from = flight[0];
                int to = flight[1];
                int price = flight[2];

                if (dp[i - 1][from] != Integer.MAX_VALUE) {
                    dp[i][to] = Math.min(dp[i][to], dp[i - 1][from] + price);
                }
            }
        }

        return dp[k + 1][dst] == Integer.MAX_VALUE ? -1 : dp[k + 1][dst];
    }

    // Solution 5: DFS with Memoization
    // Time Complexity: O(n * k * E)
    // Space Complexity: O(n * k)
    public int findCheapestPriceDFS(int n, int[][] flights, int src, int dst, int k) {
        // Build adjacency list
        Map<Integer, List<int[]>> graph = new HashMap<>();
        for (int[] flight : flights) {
            graph.putIfAbsent(flight[0], new ArrayList<>());
            graph.get(flight[0]).add(new int[] { flight[1], flight[2] });
        }

        // memo[city][stops] = min cost to reach dst from city with stops remaining
        Integer[][] memo = new Integer[n][k + 2];

        int result = dfs(graph, src, dst, k + 1, memo);
        return result >= Integer.MAX_VALUE / 2 ? -1 : result;
    }

    private int dfs(Map<Integer, List<int[]>> graph, int curr, int dst,
            int stopsLeft, Integer[][] memo) {
        if (curr == dst) {
            return 0;
        }

        if (stopsLeft == 0) {
            return Integer.MAX_VALUE / 2; // Avoid overflow
        }

        if (memo[curr][stopsLeft] != null) {
            return memo[curr][stopsLeft];
        }

        int minCost = Integer.MAX_VALUE / 2;

        if (graph.containsKey(curr)) {
            for (int[] next : graph.get(curr)) {
                int nextCity = next[0];
                int price = next[1];

                int cost = price + dfs(graph, nextCity, dst, stopsLeft - 1, memo);
                minCost = Math.min(minCost, cost);
            }
        }

        memo[curr][stopsLeft] = minCost;
        return minCost;
    }

    // Helper method to visualize the graph
    public void visualizeGraph(int n, int[][] flights) {
        System.out.println("\nFlight Network:");
        Map<Integer, List<String>> graph = new HashMap<>();

        for (int[] flight : flights) {
            graph.putIfAbsent(flight[0], new ArrayList<>());
            graph.get(flight[0]).add(flight[1] + "($" + flight[2] + ")");
        }

        for (int i = 0; i < n; i++) {
            if (graph.containsKey(i)) {
                System.out.println("City " + i + " -> " + graph.get(i));
            }
        }
    }

    // Helper method to find and display the actual path
    public List<Integer> findPath(int n, int[][] flights, int src, int dst, int k) {
        Map<Integer, List<int[]>> graph = new HashMap<>();
        for (int[] flight : flights) {
            graph.putIfAbsent(flight[0], new ArrayList<>());
            graph.get(flight[0]).add(new int[] { flight[1], flight[2] });
        }

        // BFS with path tracking
        Queue<Object[]> queue = new LinkedList<>();
        queue.offer(new Object[] { src, 0, new ArrayList<>(Arrays.asList(src)) });

        int[] minCost = new int[n];
        Arrays.fill(minCost, Integer.MAX_VALUE);
        minCost[src] = 0;

        int stops = 0;
        List<Integer> bestPath = null;
        int bestCost = Integer.MAX_VALUE;

        while (!queue.isEmpty() && stops <= k) {
            int size = queue.size();

            for (int i = 0; i < size; i++) {
                Object[] curr = queue.poll();
                int city = (int) curr[0];
                int cost = (int) curr[1];
                @SuppressWarnings("unchecked")
                List<Integer> path = (List<Integer>) curr[2];

                if (city == dst && cost < bestCost) {
                    bestCost = cost;
                    bestPath = new ArrayList<>(path);
                }

                if (!graph.containsKey(city))
                    continue;

                for (int[] next : graph.get(city)) {
                    int nextCity = next[0];
                    int nextCost = cost + next[1];

                    if (nextCost < minCost[nextCity]) {
                        minCost[nextCity] = nextCost;
                        List<Integer> newPath = new ArrayList<>(path);
                        newPath.add(nextCity);
                        queue.offer(new Object[] { nextCity, nextCost, newPath });
                    }
                }
            }
            stops++;
        }

        return bestPath;
    }

    // Test cases
    public static void main(String[] args) {
        CheapestFlightsKStops solution = new CheapestFlightsKStops();

        // Test Case 1
        int n1 = 4;
        int[][] flights1 = { { 0, 1, 100 }, { 1, 2, 100 }, { 2, 0, 100 }, { 1, 3, 600 }, { 2, 3, 200 } };
        int src1 = 0, dst1 = 3, k1 = 1;

        System.out.println("Example 1:");
        System.out.println("n = " + n1 + ", src = " + src1 + ", dst = " + dst1 + ", k = " + k1);
        solution.visualizeGraph(n1, flights1);

        System.out.println("\nOutput (BFS): " + solution.findCheapestPrice(n1, flights1, src1, dst1, k1));
        System.out.println("Output (Dijkstra): " + solution.findCheapestPriceDijkstra(n1, flights1, src1, dst1, k1));
        System.out.println(
                "Output (Bellman-Ford): " + solution.findCheapestPriceBellmanFord(n1, flights1, src1, dst1, k1));
        System.out.println("Output (DP): " + solution.findCheapestPriceDP(n1, flights1, src1, dst1, k1));
        System.out.println("Output (DFS): " + solution.findCheapestPriceDFS(n1, flights1, src1, dst1, k1));

        List<Integer> path1 = solution.findPath(n1, flights1, src1, dst1, k1);
        System.out.println("Path: " + path1);
        System.out.println("Expected: 700 (path: 0→1→3)\n");

        // Test Case 2
        int n2 = 3;
        int[][] flights2 = { { 0, 1, 100 }, { 1, 2, 100 }, { 0, 2, 500 } };
        int src2 = 0, dst2 = 2, k2 = 1;

        System.out.println("Example 2:");
        System.out.println("n = " + n2 + ", src = " + src2 + ", dst = " + dst2 + ", k = " + k2);
        solution.visualizeGraph(n2, flights2);
        System.out.println("\nOutput: " + solution.findCheapestPrice(n2, flights2, src2, dst2, k2));

        List<Integer> path2 = solution.findPath(n2, flights2, src2, dst2, k2);
        System.out.println("Path: " + path2);
        System.out.println("Expected: 200 (path: 0→1→2)\n");

        // Test Case 3
        int n3 = 3;
        int[][] flights3 = { { 0, 1, 100 }, { 1, 2, 100 }, { 0, 2, 500 } };
        int src3 = 0, dst3 = 2, k3 = 0;

        System.out.println("Example 3:");
        System.out.println("n = " + n3 + ", src = " + src3 + ", dst = " + dst3 + ", k = " + k3);
        System.out.println("Output: " + solution.findCheapestPrice(n3, flights3, src3, dst3, k3));

        List<Integer> path3 = solution.findPath(n3, flights3, src3, dst3, k3);
        System.out.println("Path: " + path3);
        System.out.println("Expected: 500 (direct flight: 0→2)\n");

        // Additional Test: No path exists
        int n4 = 3;
        int[][] flights4 = { { 0, 1, 100 }, { 1, 2, 100 } };
        int src4 = 2, dst4 = 0, k4 = 1;

        System.out.println("Test Case: No path exists");
        System.out.println("Output: " + solution.findCheapestPrice(n4, flights4, src4, dst4, k4));
        System.out.println("Expected: -1\n");

        // Performance comparison
        System.out.println("=== Performance Comparison ===");
        int nLarge = 100;
        int[][] flightsLarge = generateRandomFlights(100, 500);
        int srcLarge = 0, dstLarge = 99, kLarge = 10;

        long start = System.nanoTime();
        int result1 = solution.findCheapestPrice(nLarge, flightsLarge, srcLarge, dstLarge, kLarge);
        long end = System.nanoTime();
        System.out.println("BFS: " + result1 + " (Time: " + (end - start) / 1000 + " μs)");

        start = System.nanoTime();
        int result2 = solution.findCheapestPriceDijkstra(nLarge, flightsLarge, srcLarge, dstLarge, kLarge);
        end = System.nanoTime();
        System.out.println("Dijkstra: " + result2 + " (Time: " + (end - start) / 1000 + " μs)");

        start = System.nanoTime();
        int result3 = solution.findCheapestPriceBellmanFord(nLarge, flightsLarge, srcLarge, dstLarge, kLarge);
        end = System.nanoTime();
        System.out.println("Bellman-Ford: " + result3 + " (Time: " + (end - start) / 1000 + " μs)");

        start = System.nanoTime();
        int result4 = solution.findCheapestPriceDP(nLarge, flightsLarge, srcLarge, dstLarge, kLarge);
        end = System.nanoTime();
        System.out.println("DP: " + result4 + " (Time: " + (end - start) / 1000 + " μs)");
    }

    // Helper to generate random flights
    private static int[][] generateRandomFlights(int n, int numFlights) {
        Random rand = new Random(42);
        int[][] flights = new int[numFlights][3];

        for (int i = 0; i < numFlights; i++) {
            int from = rand.nextInt(n);
            int to = rand.nextInt(n);
            while (to == from) {
                to = rand.nextInt(n);
            }
            int price = rand.nextInt(1000) + 1;
            flights[i] = new int[] { from, to, price };
        }

        return flights;
    }
}

/*
 * COMPLEXITY ANALYSIS:
 * 
 * Solution 1: BFS with Level-Order Traversal (RECOMMENDED for small k)
 * - Time Complexity: O(n + E * k)
 * E = number of flights (edges)
 * Process each level up to k+1 levels
 * Each flight explored at most k+1 times
 * - Space Complexity: O(n + E)
 * Graph adjacency list: O(E)
 * Queue: O(E) in worst case
 * Cost array: O(n)
 * 
 * Solution 2: Modified Dijkstra's Algorithm
 * - Time Complexity: O((E + n) * log(E))
 * Priority queue operations dominate
 * Each edge may be processed multiple times with different stop counts
 * - Space Complexity: O(n * k + E)
 * Visited array: O(n * k)
 * Priority queue: O(E * k)
 * Graph: O(E)
 * 
 * Solution 3: Bellman-Ford Algorithm (MOST ELEGANT)
 * - Time Complexity: O(k * E)
 * Relax all edges k+1 times
 * Each relaxation: O(E)
 * - Space Complexity: O(n)
 * Distance arrays: O(n)
 * Very space efficient!
 * 
 * Solution 4: Dynamic Programming
 * - Time Complexity: O(k * E)
 * k+1 iterations
 * Each iteration processes all E flights
 * - Space Complexity: O(k * n)
 * 2D DP table
 * 
 * Solution 5: DFS with Memoization
 * - Time Complexity: O(n * k * E)
 * Each state (city, stops) computed once
 * n cities, k stops: n*k states
 * - Space Complexity: O(n * k)
 * Memoization table + recursion stack
 * 
 * KEY INSIGHTS:
 * 
 * 1. Problem Constraints:
 * - Standard shortest path with additional STOP constraint
 * - k stops = k+1 flights maximum
 * - Must balance cost vs. number of stops
 * 
 * 2. Why Not Standard Dijkstra?
 * - Standard Dijkstra finds shortest path without constraints
 * - Here: cheaper path with more stops might be invalid
 * - Need to track BOTH cost and stops
 * 
 * 3. Bellman-Ford Intuition:
 * - Relax edges iteratively
 * - After i iterations: found shortest paths using ≤ i edges
 * - Perfect for "at most k stops" constraint!
 * - Clone array each iteration to avoid using updated values
 * 
 * 4. BFS Level-Order:
 * - Each level = one more stop
 * - Stop at level k+1
 * - Track minimum cost to each city
 * - Only explore if we found cheaper path
 * 
 * 5. DP State Definition:
 * dp[i][j] = min cost to reach city j using at most i flights
 * 
 * Transition:
 * For each flight (u → v, price):
 * dp[i][v] = min(dp[i][v], dp[i-1][u] + price)
 * 
 * 6. Important Edge Cases:
 * - k = 0: only direct flights allowed
 * - No path exists within k stops
 * - Multiple paths with same cost but different stops
 * - Self-loops (rare but possible)
 * 
 * 7. Why Bellman-Ford Often Best:
 * + Simple implementation
 * + Excellent time complexity O(k * E)
 * + Space efficient O(n)
 * + Handles negative weights (if needed)
 * + Easy to understand
 * 
 * 8. When to Use Each:
 * - BFS: Small k, sparse graph
 * - Bellman-Ford: General purpose, clean solution
 * - DP: When you want to see all intermediate states
 * - Dijkstra: Dense graphs, larger k values
 * - DFS: When you need actual path reconstruction
 * 
 * 9. Common Pitfalls:
 * - Forgetting k stops = k+1 flights
 * - Not cloning array in Bellman-Ford
 * - Integer overflow with large costs
 * - Not handling "no path exists" case
 * 
 * 10. Optimization Tips:
 * - Early termination when destination reached
 * - Prune paths exceeding current best
 * - Use visited array wisely
 * - Consider bidirectional search for large graphs
 */
