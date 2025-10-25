/*
 * You are in a city that consists of n intersections numbered from 0 to n - 1
 * with bi-directional roads between some intersections. The inputs are
 * generated such that you can reach any intersection from any other
 * intersection and that there is at most one road between any two
 * intersections.
 * 
 * You are given an integer n and a 2D integer array roads where roads[i] = [ui,
 * vi, timei] means that there is a road between intersections ui and vi that
 * takes timei minutes to travel. You want to know in how many ways you can
 * travel from intersection 0 to intersection n - 1 in the shortest amount of
 * time.
 * 
 * Return the number of ways you can arrive at your destination in the shortest
 * amount of time. Since the answer may be large, return it modulo 109 + 7.
 * 
 * Example 1:
 * Input: n = 7, roads =
 * [[0,6,7],[0,1,2],[1,2,3],[1,3,3],[6,3,3],[3,5,1],[6,5,1],[2,5,1],[0,4,5],[4,6
 * ,2]]
 * Output: 4
 * Explanation: The shortest amount of time it takes to go from intersection 0
 * to intersection 6 is 7 minutes.
 * The four ways to get there in 7 minutes are:
 * - 0 ➝ 6
 * - 0 ➝ 4 ➝ 6
 * - 0 ➝ 1 ➝ 2 ➝ 5 ➝ 6
 * - 0 ➝ 1 ➝ 3 ➝ 5 ➝ 6
 * 
 * Example 2:
 * Input: n = 2, roads = [[1,0,10]]
 * Output: 1
 * Explanation: There is only one way to go from intersection 0 to intersection
 * 1, and it takes 10 minutes.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

class NumberOfWaysToArriveAtDestination1 {

    static final int MOD = 1_000_000_007;

    record Edge(int node, int time) {
    }

    record State(int node, long totalTime) {
    }

    public int countPaths(int n, int[][] roads) {
        // Build undirected graph
        List<List<Edge>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++)
            adj.add(new ArrayList<>());
        for (int[] r : roads) {
            int u = r[0], v = r[1], t = r[2];
            adj.get(u).add(new Edge(v, t));
            adj.get(v).add(new Edge(u, t));
        }

        long[] dist = new long[n];
        Arrays.fill(dist, Long.MAX_VALUE / 4);
        int[] ways = new int[n];

        PriorityQueue<State> pq = new PriorityQueue<>(Comparator.comparingLong(State::totalTime));
        dist[0] = 0L;
        ways[0] = 1;
        pq.offer(new State(0, 0L));

        while (!pq.isEmpty()) {
            State cur = pq.poll();
            int u = cur.node();
            long tU = cur.totalTime();

            // Skip stale
            if (tU > dist[u])
                continue;

            for (Edge e : adj.get(u)) {
                int v = e.node();
                long cand = tU + (long) e.time();

                if (cand < dist[v]) {
                    dist[v] = cand;
                    ways[v] = ways[u];
                    pq.offer(new State(v, cand));
                } else if (cand == dist[v]) {
                    ways[v] = (int) ((ways[v] + (long) ways[u]) % MOD);
                    // no need to push; distance unchanged
                }
            }
        }

        return ways[n - 1];
    }

}

class NumberOfWaysToArriveAtDestination {

    private static final int MOD = 1_000_000_007;

    // Solution 1: Dijkstra + Path Counting (RECOMMENDED)
    // Time Complexity: O((E + n) * log n)
    // Space Complexity: O(n + E)
    public int countPaths(int n, int[][] roads) {
        // Build adjacency list
        Map<Integer, List<int[]>> graph = new HashMap<>();
        for (int[] road : roads) {
            graph.putIfAbsent(road[0], new ArrayList<>());
            graph.putIfAbsent(road[1], new ArrayList<>());
            graph.get(road[0]).add(new int[] { road[1], road[2] });
            graph.get(road[1]).add(new int[] { road[0], road[2] });
        }

        // Distance and ways arrays
        long[] dist = new long[n];
        long[] ways = new long[n];
        Arrays.fill(dist, Long.MAX_VALUE);

        dist[0] = 0;
        ways[0] = 1;

        // Priority queue: {distance, node}
        PriorityQueue<long[]> pq = new PriorityQueue<>((a, b) -> Long.compare(a[0], b[0]));
        pq.offer(new long[] { 0, 0 });

        while (!pq.isEmpty()) {
            long[] curr = pq.poll();
            long currDist = curr[0];
            int node = (int) curr[1];

            // Skip if we've found a better path
            if (currDist > dist[node]) {
                continue;
            }

            if (!graph.containsKey(node)) {
                continue;
            }

            for (int[] neighbor : graph.get(node)) {
                int nextNode = neighbor[0];
                int weight = neighbor[1];
                long newDist = currDist + weight;

                if (newDist < dist[nextNode]) {
                    // Found shorter path
                    dist[nextNode] = newDist;
                    ways[nextNode] = ways[node];
                    pq.offer(new long[] { newDist, nextNode });
                } else if (newDist == dist[nextNode]) {
                    // Found another shortest path
                    ways[nextNode] = (ways[nextNode] + ways[node]) % MOD;
                }
            }
        }

        return (int) ways[n - 1];
    }

    // Solution 2: Modified Dijkstra with Explicit Path Tracking
    // Time Complexity: O((E + n) * log n)
    // Space Complexity: O(n + E)
    public int countPathsWithTracking(int n, int[][] roads) {
        Map<Integer, List<int[]>> graph = new HashMap<>();
        for (int[] road : roads) {
            graph.putIfAbsent(road[0], new ArrayList<>());
            graph.putIfAbsent(road[1], new ArrayList<>());
            graph.get(road[0]).add(new int[] { road[1], road[2] });
            graph.get(road[1]).add(new int[] { road[0], road[2] });
        }

        long[] dist = new long[n];
        int[] ways = new int[n];
        Arrays.fill(dist, Long.MAX_VALUE);

        dist[0] = 0;
        ways[0] = 1;

        PriorityQueue<long[]> pq = new PriorityQueue<>((a, b) -> Long.compare(a[0], b[0]));
        pq.offer(new long[] { 0, 0 });

        boolean[] processed = new boolean[n];

        while (!pq.isEmpty()) {
            long[] curr = pq.poll();
            long currDist = curr[0];
            int node = (int) curr[1];

            if (processed[node]) {
                continue;
            }
            processed[node] = true;

            if (!graph.containsKey(node)) {
                continue;
            }

            for (int[] neighbor : graph.get(node)) {
                int nextNode = neighbor[0];
                int weight = neighbor[1];
                long newDist = currDist + weight;

                if (newDist < dist[nextNode]) {
                    dist[nextNode] = newDist;
                    ways[nextNode] = ways[node];
                    pq.offer(new long[] { newDist, nextNode });
                } else if (newDist == dist[nextNode] && !processed[nextNode]) {
                    ways[nextNode] = (ways[nextNode] + ways[node]) % MOD;
                }
            }
        }

        return ways[n - 1];
    }

    // Solution 3: Bellman-Ford with Path Counting
    // Time Complexity: O(n * E)
    // Space Complexity: O(n)
    public int countPathsBellmanFord(int n, int[][] roads) {
        long[] dist = new long[n];
        long[] ways = new long[n];
        Arrays.fill(dist, Long.MAX_VALUE);

        dist[0] = 0;
        ways[0] = 1;

        // Relax edges n-1 times
        for (int i = 0; i < n - 1; i++) {
            boolean updated = false;

            for (int[] road : roads) {
                int u = road[0];
                int v = road[1];
                int w = road[2];

                // Check both directions (undirected graph)
                if (dist[u] != Long.MAX_VALUE && dist[u] + w < dist[v]) {
                    dist[v] = dist[u] + w;
                    ways[v] = ways[u];
                    updated = true;
                } else if (dist[u] != Long.MAX_VALUE && dist[u] + w == dist[v]) {
                    ways[v] = (ways[v] + ways[u]) % MOD;
                }

                if (dist[v] != Long.MAX_VALUE && dist[v] + w < dist[u]) {
                    dist[u] = dist[v] + w;
                    ways[u] = ways[v];
                    updated = true;
                } else if (dist[v] != Long.MAX_VALUE && dist[v] + w == dist[u]) {
                    ways[u] = (ways[u] + ways[v]) % MOD;
                }
            }

            if (!updated)
                break;
        }

        return (int) ways[n - 1];
    }

    // Solution 4: BFS + Topological Sort (for finding paths on shortest path DAG)
    // Time Complexity: O(n + E)
    // Space Complexity: O(n + E)
    public int countPathsBFS(int n, int[][] roads) {
        // First, find shortest distances using Dijkstra
        Map<Integer, List<int[]>> graph = new HashMap<>();
        for (int[] road : roads) {
            graph.putIfAbsent(road[0], new ArrayList<>());
            graph.putIfAbsent(road[1], new ArrayList<>());
            graph.get(road[0]).add(new int[] { road[1], road[2] });
            graph.get(road[1]).add(new int[] { road[0], road[2] });
        }

        long[] dist = new long[n];
        Arrays.fill(dist, Long.MAX_VALUE);
        dist[0] = 0;

        PriorityQueue<long[]> pq = new PriorityQueue<>((a, b) -> Long.compare(a[0], b[0]));
        pq.offer(new long[] { 0, 0 });

        while (!pq.isEmpty()) {
            long[] curr = pq.poll();
            long currDist = curr[0];
            int node = (int) curr[1];

            if (currDist > dist[node])
                continue;
            if (!graph.containsKey(node))
                continue;

            for (int[] neighbor : graph.get(node)) {
                int nextNode = neighbor[0];
                int weight = neighbor[1];
                long newDist = currDist + weight;

                if (newDist < dist[nextNode]) {
                    dist[nextNode] = newDist;
                    pq.offer(new long[] { newDist, nextNode });
                }
            }
        }

        // Now count paths using BFS on shortest path DAG
        long[] ways = new long[n];
        ways[0] = 1;

        Queue<Integer> queue = new LinkedList<>();
        queue.offer(0);
        boolean[] visited = new boolean[n];

        while (!queue.isEmpty()) {
            int node = queue.poll();

            if (!graph.containsKey(node))
                continue;

            for (int[] neighbor : graph.get(node)) {
                int nextNode = neighbor[0];
                int weight = neighbor[1];

                // Only consider edges on shortest paths
                if (dist[node] + weight == dist[nextNode]) {
                    ways[nextNode] = (ways[nextNode] + ways[node]) % MOD;

                    if (!visited[nextNode]) {
                        visited[nextNode] = true;
                        queue.offer(nextNode);
                    }
                }
            }
        }

        return (int) ways[n - 1];
    }

    // Helper method to visualize the graph
    public void visualizeGraph(int n, int[][] roads) {
        System.out.println("\n=== Graph Visualization ===");
        System.out.println("Intersections: 0 to " + (n - 1));
        System.out.println("\nRoads (bidirectional):");

        for (int[] road : roads) {
            System.out.printf("  %d ↔ %d (time: %d)\n", road[0], road[1], road[2]);
        }
    }

    // Helper method to find and display all shortest paths
    public void showAllShortestPaths(int n, int[][] roads) {
        Map<Integer, List<int[]>> graph = new HashMap<>();
        for (int[] road : roads) {
            graph.putIfAbsent(road[0], new ArrayList<>());
            graph.putIfAbsent(road[1], new ArrayList<>());
            graph.get(road[0]).add(new int[] { road[1], road[2] });
            graph.get(road[1]).add(new int[] { road[0], road[2] });
        }

        // Find shortest distances
        long[] dist = new long[n];
        Arrays.fill(dist, Long.MAX_VALUE);
        dist[0] = 0;

        PriorityQueue<long[]> pq = new PriorityQueue<>((a, b) -> Long.compare(a[0], b[0]));
        pq.offer(new long[] { 0, 0 });

        while (!pq.isEmpty()) {
            long[] curr = pq.poll();
            long currDist = curr[0];
            int node = (int) curr[1];

            if (currDist > dist[node])
                continue;
            if (!graph.containsKey(node))
                continue;

            for (int[] neighbor : graph.get(node)) {
                int nextNode = neighbor[0];
                int weight = neighbor[1];
                long newDist = currDist + weight;

                if (newDist < dist[nextNode]) {
                    dist[nextNode] = newDist;
                    pq.offer(new long[] { newDist, nextNode });
                }
            }
        }

        System.out.println("\n=== All Shortest Paths ===");
        System.out.println("Shortest time: " + dist[n - 1] + " minutes");
        System.out.println("\nPaths:");

        // Find all paths with DFS
        List<List<Integer>> allPaths = new ArrayList<>();
        List<Integer> currentPath = new ArrayList<>();
        currentPath.add(0);
        findAllPaths(graph, 0, n - 1, dist, currentPath, allPaths);

        int count = 1;
        for (List<Integer> path : allPaths) {
            System.out.print("  Path " + count++ + ": ");
            for (int i = 0; i < path.size(); i++) {
                System.out.print(path.get(i));
                if (i < path.size() - 1)
                    System.out.print(" → ");
            }
            System.out.println();
        }
    }

    private void findAllPaths(Map<Integer, List<int[]>> graph, int curr, int dest,
            long[] dist, List<Integer> path, List<List<Integer>> allPaths) {
        if (curr == dest) {
            allPaths.add(new ArrayList<>(path));
            return;
        }

        if (!graph.containsKey(curr))
            return;

        for (int[] neighbor : graph.get(curr)) {
            int next = neighbor[0];
            int weight = neighbor[1];

            // Only follow edges on shortest paths
            if (dist[curr] + weight == dist[next]) {
                path.add(next);
                findAllPaths(graph, next, dest, dist, path, allPaths);
                path.remove(path.size() - 1);
            }
        }
    }

    // Test cases
    public static void main(String[] args) {
        NumberOfWaysToArriveAtDestination solution = new NumberOfWaysToArriveAtDestination();

        // Test Case 1
        int n1 = 7;
        int[][] roads1 = {
                { 0, 6, 7 }, { 0, 1, 2 }, { 1, 2, 3 }, { 1, 3, 3 }, { 6, 3, 3 },
                { 3, 5, 1 }, { 6, 5, 1 }, { 2, 5, 1 }, { 0, 4, 5 }, { 4, 6, 2 }
        };

        System.out.println("Example 1:");
        System.out.println("Input: n = 7");
        solution.visualizeGraph(n1, roads1);

        System.out.println("\nOutput (Dijkstra): " + solution.countPaths(n1, roads1));
        System.out.println("Output (Dijkstra + Tracking): " + solution.countPathsWithTracking(n1, roads1));
        System.out.println("Output (Bellman-Ford): " + solution.countPathsBellmanFord(n1, roads1));
        System.out.println("Output (BFS): " + solution.countPathsBFS(n1, roads1));

        solution.showAllShortestPaths(n1, roads1);
        System.out.println("\nExpected: 4\n");

        // Test Case 2
        int n2 = 2;
        int[][] roads2 = { { 1, 0, 10 } };

        System.out.println("Example 2:");
        System.out.println("Input: n = 2");
        solution.visualizeGraph(n2, roads2);
        System.out.println("\nOutput: " + solution.countPaths(n2, roads2));
        solution.showAllShortestPaths(n2, roads2);
        System.out.println("\nExpected: 1\n");

        // Additional Test: Multiple equal shortest paths
        int n3 = 4;
        int[][] roads3 = { { 0, 1, 1 }, { 0, 2, 1 }, { 1, 3, 1 }, { 2, 3, 1 } };

        System.out.println("Additional Test: Diamond Graph");
        solution.visualizeGraph(n3, roads3);
        System.out.println("\nOutput: " + solution.countPaths(n3, roads3));
        solution.showAllShortestPaths(n3, roads3);
        System.out.println("Expected: 2 (0→1→3 and 0→2→3)\n");

        // Test: Single path
        int n4 = 5;
        int[][] roads4 = { { 0, 1, 1 }, { 1, 2, 1 }, { 2, 3, 1 }, { 3, 4, 1 } };

        System.out.println("Test: Linear Graph");
        solution.visualizeGraph(n4, roads4);
        System.out.println("\nOutput: " + solution.countPaths(n4, roads4));
        System.out.println("Expected: 1\n");

        // Test: Complex graph with many paths
        int n5 = 5;
        int[][] roads5 = {
                { 0, 1, 10 }, { 0, 2, 10 }, { 1, 3, 10 }, { 2, 3, 10 },
                { 1, 4, 10 }, { 2, 4, 10 }, { 3, 4, 10 }
        };

        System.out.println("Test: Complex Graph");
        solution.visualizeGraph(n5, roads5);
        System.out.println("\nOutput: " + solution.countPaths(n5, roads5));
        solution.showAllShortestPaths(n5, roads5);

        // Performance comparison
        System.out.println("\n=== Performance Comparison ===");
        int nLarge = 100;
        int[][] roadsLarge = generateRandomGraph(100, 300);

        long start = System.nanoTime();
        int result1 = solution.countPaths(nLarge, roadsLarge);
        long end = System.nanoTime();
        System.out.println("Dijkstra: " + result1 + " (Time: " + (end - start) / 1000 + " μs)");

        start = System.nanoTime();
        int result2 = solution.countPathsWithTracking(nLarge, roadsLarge);
        end = System.nanoTime();
        System.out.println("Dijkstra + Tracking: " + result2 + " (Time: " + (end - start) / 1000 + " μs)");

        start = System.nanoTime();
        int result3 = solution.countPathsBellmanFord(nLarge, roadsLarge);
        end = System.nanoTime();
        System.out.println("Bellman-Ford: " + result3 + " (Time: " + (end - start) / 1000 + " μs)");

        start = System.nanoTime();
        int result4 = solution.countPathsBFS(nLarge, roadsLarge);
        end = System.nanoTime();
        System.out.println("BFS: " + result4 + " (Time: " + (end - start) / 1000 + " μs)");
    }

    // Helper to generate random graph
    private static int[][] generateRandomGraph(int n, int numEdges) {
        Random rand = new Random(42);
        Set<String> edges = new HashSet<>();
        int[][] roads = new int[numEdges][3];
        int count = 0;

        // Ensure connectivity by creating a path
        for (int i = 0; i < n - 1; i++) {
            String key = Math.min(i, i + 1) + "-" + Math.max(i, i + 1);
            edges.add(key);
            roads[count++] = new int[] { i, i + 1, rand.nextInt(10) + 1 };
        }

        // Add random edges
        while (count < numEdges) {
            int u = rand.nextInt(n);
            int v = rand.nextInt(n);
            if (u == v)
                continue;

            String key = Math.min(u, v) + "-" + Math.max(u, v);
            if (!edges.contains(key)) {
                edges.add(key);
                roads[count++] = new int[] { u, v, rand.nextInt(10) + 1 };
            }
        }

        return roads;
    }
}

/*
 * COMPLEXITY ANALYSIS:
 * 
 * Solution 1: Dijkstra + Path Counting (RECOMMENDED)
 * - Time Complexity: O((E + n) * log n)
 * Standard Dijkstra with additional counting
 * Priority queue operations: O(log n)
 * Process each edge at most once
 * - Space Complexity: O(n + E)
 * Graph adjacency list: O(E)
 * Distance and ways arrays: O(n)
 * Priority queue: O(n)
 * 
 * Solution 2: Modified Dijkstra with Tracking
 * - Time Complexity: O((E + n) * log n)
 * Similar to Solution 1
 * Adds processed tracking for optimization
 * - Space Complexity: O(n + E)
 * Additional boolean array: O(n)
 * 
 * Solution 3: Bellman-Ford with Path Counting
 * - Time Complexity: O(n * E)
 * Relax edges n-1 times
 * Each relaxation: O(E)
 * - Space Complexity: O(n)
 * Only distance and ways arrays
 * Most space efficient!
 * 
 * Solution 4: BFS on Shortest Path DAG
 * - Time Complexity: O((E + n) * log n) + O(n + E)
 * First phase: Dijkstra to find distances
 * Second phase: BFS to count paths
 * - Space Complexity: O(n + E)
 * Two separate phases
 * 
 * KEY INSIGHTS:
 * 
 * 1. Two-Phase Problem:
 * Phase 1: Find shortest path length (Dijkstra)
 * Phase 2: Count paths of that length
 * 
 * Can be combined into single phase!
 * 
 * 2. Path Counting Logic:
 * When exploring edge (u → v):
 * 
 * if (dist[u] + weight < dist[v]):
 * // Found SHORTER path
 * dist[v] = dist[u] + weight
 * ways[v] = ways[u] // Reset count
 * 
 * else if (dist[u] + weight == dist[v]):
 * // Found ANOTHER shortest path
 * ways[v] += ways[u] // Add to count
 * 
 * 3. Why Dijkstra is Perfect:
 * - Processes nodes in order of distance
 * - When we process a node, we've found its shortest path
 * - Can count paths incrementally as we discover them
 * 
 * 4. Initialization:
 * - dist[0] = 0, ways[0] = 1
 * - dist[i] = ∞, ways[i] = 0 for i > 0
 * - ways[0] = 1 means "one way to stay at start"
 * 
 * 5. Modulo Arithmetic:
 * - Result can be very large
 * - Take mod 10^9 + 7 at each addition
 * - ways[v] = (ways[v] + ways[u]) % MOD
 * 
 * 6. Undirected Graph Handling:
 * - Add edges in both directions
 * - graph[u].add({v, w})
 * - graph[v].add({u, w})
 * 
 * 7. Edge Cases:
 * - n = 1: return 1 (already at destination)
 * - n = 2, no roads: return 0
 * - Disconnected graph: ways[n-1] = 0
 * - Multiple shortest paths of same length
 * 
 * 8. Why Use long for distances:
 * - Edge weights can be large
 * - Sum of distances can overflow int
 * - Use long to prevent overflow
 * - Cast to int for final result
 * 
 * 9. Comparison to Similar Problems:
 * - Network Delay: find MAX shortest path
 * - Cheapest Flights: shortest path with constraints
 * - This problem: COUNT shortest paths
 * 
 * 10. Optimization Techniques:
 * - Skip already processed nodes
 * - Early termination when destination reached
 * - Use processed array to avoid re-processing
 * - Combine distance finding and counting
 * 
 * 11. Common Mistakes:
 * - Forgetting to use modulo
 * - Not handling equal distance case
 * - Using int instead of long for distances
 * - Not resetting ways when finding shorter path
 * - Forgetting bidirectional edges
 * 
 * 12. Real-World Applications:
 * - Route planning with alternatives
 * - Network redundancy analysis
 * - Supply chain optimization
 * - Finding backup routes
 * - Load balancing across paths
 * 
 * 13. When to Use Each Approach:
 * - Dijkstra + Counting: General purpose (BEST)
 * - Bellman-Ford: Simple implementation
 * - BFS: When you want two separate phases
 * - Modified Dijkstra: When you need more control
 * 
 * 14. Mathematical Insight:
 * - This is counting paths in a DAG
 * - The shortest path graph forms a DAG
 * - Topological order given by distances
 * - Dynamic programming on this DAG
 * 
 * 15. Why This Works:
 * - Dijkstra guarantees optimal substructure
 * - ways[v] = Σ ways[u] for all u where (u,v) on shortest path
 * - Principle of optimality: shortest path composed of shortest paths
 */
