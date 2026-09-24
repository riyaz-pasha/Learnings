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

/*
 * You are given a network of n nodes, labeled from 1 to n. You are also given
 * times, a list of travel times as directed edges times[i] = (ui, vi, wi),
 * where ui is the source node, vi is the target node, and wi is the time it
 * takes for a signal to travel from source to target.
 * 
 * We will send a signal from a given node k. Return the minimum time it takes
 * for all the n nodes to receive the signal. If it is impossible for all the n
 * nodes to receive the signal, return -1.
 * 
 * Example 1:
 * Input: times = [[2,1,1],[2,3,1],[3,4,1]], n = 4, k = 2
 * Output: 2
 * 
 * Example 2:
 * Input: times = [[1,2,1]], n = 2, k = 1
 * Output: 1
 * 
 * Example 3:
 * Input: times = [[1,2,1]], n = 2, k = 2
 * Output: -1
 */

class NetworkDelayTime1 {

    record Edge(int node, int travelTime) {
    };

    record State(int node, int totalTimeTaken) {
    }

    public int networkDelayTime(int[][] times, int n, int k) {
        List<List<Edge>> adj = new ArrayList<>();
        for (int i = 0; i <= n; i++) {
            adj.add(new ArrayList<>());
        }

        for (int[] time : times) {
            adj.get(time[0]).add(new Edge(time[1], time[2]));
        }

        int[] minTime = new int[n + 1];
        Arrays.fill(minTime, Integer.MAX_VALUE);

        PriorityQueue<State> pq = new PriorityQueue<>(Comparator.comparing(State::totalTimeTaken));
        pq.offer(new State(k, 0));
        minTime[k] = 0;
        minTime[0] = 0;

        while (!pq.isEmpty()) {
            State current = pq.poll();
            int node = current.node();
            int timeTaken = current.totalTimeTaken();

            if (timeTaken > minTime[node]) {
                continue;
            }

            for (Edge e : adj.get(node)) {
                int next = e.node();
                int nextNodeTravelTime = timeTaken + e.travelTime();
                if (nextNodeTravelTime < minTime[next]) {
                    minTime[next] = nextNodeTravelTime;
                    pq.offer(new State(next, nextNodeTravelTime));
                }
            }
        }

        int max = 0;
        for (int t : minTime) {
            if (t == Integer.MAX_VALUE) {
                return -1;
            }
            max = Math.max(max, t);
        }
        return max;
    }

}

class NetworkDelayTime2 {

    record Edge(int node, int travelTime) {
    }

    record State(int node, long totalTimeTaken) {
    }

    public int networkDelayTime(int[][] times, int n, int k) {
        // adjacency list (1-indexed nodes)
        List<List<Edge>> adj = new ArrayList<>();
        for (int i = 0; i <= n; i++)
            adj.add(new ArrayList<>());
        for (int[] t : times) {
            adj.get(t[0]).add(new Edge(t[1], t[2]));
        }

        final long INF = Long.MAX_VALUE / 4;
        long[] minTime = new long[n + 1];
        Arrays.fill(minTime, INF);

        PriorityQueue<State> pq = new PriorityQueue<>(Comparator.comparingLong(State::totalTimeTaken));
        pq.offer(new State(k, 0L));
        minTime[k] = 0L;

        while (!pq.isEmpty()) {
            State current = pq.poll();
            int node = current.node();
            long timeTaken = current.totalTimeTaken();

            // stale entry
            if (timeTaken > minTime[node])
                continue;

            for (Edge e : adj.get(node)) {
                int next = e.node();
                long candidate = timeTaken + (long) e.travelTime();
                if (candidate < minTime[next]) {
                    minTime[next] = candidate;
                    pq.offer(new State(next, candidate));
                }
            }
        }

        long max = 0L;
        for (int i = 1; i <= n; i++) {
            if (minTime[i] == INF)
                return -1; // unreachable
            max = Math.max(max, minTime[i]);
        }
        // safe to cast: problem constraints usually keep sums inside int, but cast if
        // you return int.
        return (int) max;
    }

    // optional main for quick test
    public static void main(String[] args) {
        NetworkDelayTime solver = new NetworkDelayTime();
        int[][] times = { { 2, 1, 1 }, { 2, 3, 1 }, { 3, 4, 1 } };
        System.out.println(solver.networkDelayTime(times, 4, 2)); // example
    }

}

class NetworkDelayTime {

    // Solution 1: Dijkstra's Algorithm with Priority Queue (RECOMMENDED)
    // Time Complexity: O((E + n) * log n) where E is number of edges
    // Space Complexity: O(n + E)
    public int networkDelayTime(int[][] times, int n, int k) {
        // Build adjacency list
        Map<Integer, List<int[]>> graph = new HashMap<>();
        for (int[] time : times) {
            graph.putIfAbsent(time[0], new ArrayList<>());
            graph.get(time[0]).add(new int[] { time[1], time[2] });
        }

        // Distance array (1-indexed, so size n+1)
        int[] dist = new int[n + 1];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[k] = 0;

        // Priority queue: {distance, node}
        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[0] - b[0]);
        pq.offer(new int[] { 0, k });

        while (!pq.isEmpty()) {
            int[] curr = pq.poll();
            int currDist = curr[0];
            int node = curr[1];

            // Skip if we already found a better path
            if (currDist > dist[node]) {
                continue;
            }

            // Explore neighbors
            if (graph.containsKey(node)) {
                for (int[] neighbor : graph.get(node)) {
                    int nextNode = neighbor[0];
                    int weight = neighbor[1];
                    int newDist = currDist + weight;

                    if (newDist < dist[nextNode]) {
                        dist[nextNode] = newDist;
                        pq.offer(new int[] { newDist, nextNode });
                    }
                }
            }
        }

        // Find maximum distance (time for all nodes to receive signal)
        int maxTime = 0;
        for (int i = 1; i <= n; i++) {
            if (dist[i] == Integer.MAX_VALUE) {
                return -1; // Some node unreachable
            }
            maxTime = Math.max(maxTime, dist[i]);
        }

        return maxTime;
    }

    // Solution 2: Dijkstra with Adjacency Matrix (Good for dense graphs)
    // Time Complexity: O(n^2)
    // Space Complexity: O(n^2)
    public int networkDelayTimeDijkstraMatrix(int[][] times, int n, int k) {
        // Build adjacency matrix
        int[][] graph = new int[n + 1][n + 1];
        for (int[] row : graph) {
            Arrays.fill(row, Integer.MAX_VALUE);
        }

        for (int[] time : times) {
            graph[time[0]][time[1]] = time[2];
        }

        // Distance array
        int[] dist = new int[n + 1];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[k] = 0;

        boolean[] visited = new boolean[n + 1];

        for (int i = 0; i < n; i++) {
            // Find unvisited node with minimum distance
            int minNode = -1;
            int minDist = Integer.MAX_VALUE;

            for (int j = 1; j <= n; j++) {
                if (!visited[j] && dist[j] < minDist) {
                    minDist = dist[j];
                    minNode = j;
                }
            }

            if (minNode == -1)
                break;

            visited[minNode] = true;

            // Update distances to neighbors
            for (int neighbor = 1; neighbor <= n; neighbor++) {
                if (graph[minNode][neighbor] != Integer.MAX_VALUE) {
                    dist[neighbor] = Math.min(dist[neighbor],
                            dist[minNode] + graph[minNode][neighbor]);
                }
            }
        }

        // Find maximum distance
        int maxTime = 0;
        for (int i = 1; i <= n; i++) {
            if (dist[i] == Integer.MAX_VALUE) {
                return -1;
            }
            maxTime = Math.max(maxTime, dist[i]);
        }

        return maxTime;
    }

    // Solution 3: Bellman-Ford Algorithm
    // Time Complexity: O(n * E)
    // Space Complexity: O(n)
    public int networkDelayTimeBellmanFord(int[][] times, int n, int k) {
        int[] dist = new int[n + 1];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[k] = 0;

        // Relax edges n-1 times
        for (int i = 0; i < n - 1; i++) {
            for (int[] time : times) {
                int u = time[0];
                int v = time[1];
                int w = time[2];

                if (dist[u] != Integer.MAX_VALUE) {
                    dist[v] = Math.min(dist[v], dist[u] + w);
                }
            }
        }

        // Find maximum distance
        int maxTime = 0;
        for (int i = 1; i <= n; i++) {
            if (dist[i] == Integer.MAX_VALUE) {
                return -1;
            }
            maxTime = Math.max(maxTime, dist[i]);
        }

        return maxTime;
    }

    // Solution 4: SPFA (Shortest Path Faster Algorithm)
    // Time Complexity: O(E) average, O(n * E) worst case
    // Space Complexity: O(n + E)
    public int networkDelayTimeSPFA(int[][] times, int n, int k) {
        // Build adjacency list
        Map<Integer, List<int[]>> graph = new HashMap<>();
        for (int[] time : times) {
            graph.putIfAbsent(time[0], new ArrayList<>());
            graph.get(time[0]).add(new int[] { time[1], time[2] });
        }

        int[] dist = new int[n + 1];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[k] = 0;

        Queue<Integer> queue = new LinkedList<>();
        boolean[] inQueue = new boolean[n + 1];

        queue.offer(k);
        inQueue[k] = true;

        while (!queue.isEmpty()) {
            int node = queue.poll();
            inQueue[node] = false;

            if (!graph.containsKey(node))
                continue;

            for (int[] neighbor : graph.get(node)) {
                int nextNode = neighbor[0];
                int weight = neighbor[1];

                if (dist[node] + weight < dist[nextNode]) {
                    dist[nextNode] = dist[node] + weight;

                    if (!inQueue[nextNode]) {
                        queue.offer(nextNode);
                        inQueue[nextNode] = true;
                    }
                }
            }
        }

        int maxTime = 0;
        for (int i = 1; i <= n; i++) {
            if (dist[i] == Integer.MAX_VALUE) {
                return -1;
            }
            maxTime = Math.max(maxTime, dist[i]);
        }

        return maxTime;
    }

    // Solution 5: DFS with Memoization
    // Time Complexity: O(n + E)
    // Space Complexity: O(n + E)
    public int networkDelayTimeDFS(int[][] times, int n, int k) {
        // Build adjacency list
        Map<Integer, List<int[]>> graph = new HashMap<>();
        for (int[] time : times) {
            graph.putIfAbsent(time[0], new ArrayList<>());
            graph.get(time[0]).add(new int[] { time[1], time[2] });
        }

        int[] dist = new int[n + 1];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[k] = 0;

        dfs(graph, k, 0, dist);

        int maxTime = 0;
        for (int i = 1; i <= n; i++) {
            if (dist[i] == Integer.MAX_VALUE) {
                return -1;
            }
            maxTime = Math.max(maxTime, dist[i]);
        }

        return maxTime;
    }

    private void dfs(Map<Integer, List<int[]>> graph, int node, int time, int[] dist) {
        if (time >= dist[node]) {
            return; // Already found better or equal path
        }

        dist[node] = time;

        if (!graph.containsKey(node)) {
            return;
        }

        for (int[] neighbor : graph.get(node)) {
            int nextNode = neighbor[0];
            int weight = neighbor[1];
            dfs(graph, nextNode, time + weight, dist);
        }
    }

    // Helper method to visualize the network
    public void visualizeNetwork(int[][] times, int n, int k) {
        System.out.println("\n=== Network Visualization ===");
        System.out.println("Starting node: " + k);
        System.out.println("Total nodes: " + n);
        System.out.println("\nEdges:");

        for (int[] time : times) {
            System.out.printf("  %d → %d (time: %d)\n", time[0], time[1], time[2]);
        }
    }

    // Helper method to show signal propagation step by step
    public void showSignalPropagation(int[][] times, int n, int k) {
        Map<Integer, List<int[]>> graph = new HashMap<>();
        for (int[] time : times) {
            graph.putIfAbsent(time[0], new ArrayList<>());
            graph.get(time[0]).add(new int[] { time[1], time[2] });
        }

        int[] dist = new int[n + 1];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[k] = 0;

        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[0] - b[0]);
        pq.offer(new int[] { 0, k });

        System.out.println("\n=== Signal Propagation Timeline ===");
        Set<Integer> received = new HashSet<>();

        while (!pq.isEmpty()) {
            int[] curr = pq.poll();
            int currDist = curr[0];
            int node = curr[1];

            if (received.contains(node))
                continue;
            received.add(node);

            System.out.printf("Time %d: Node %d receives signal\n", currDist, node);

            if (currDist > dist[node])
                continue;

            if (graph.containsKey(node)) {
                for (int[] neighbor : graph.get(node)) {
                    int nextNode = neighbor[0];
                    int weight = neighbor[1];
                    int newDist = currDist + weight;

                    if (newDist < dist[nextNode]) {
                        dist[nextNode] = newDist;
                        pq.offer(new int[] { newDist, nextNode });
                    }
                }
            }
        }

        if (received.size() < n) {
            System.out.println("\nSome nodes never received the signal!");
        }
    }

    // Test cases
    public static void main(String[] args) {
        NetworkDelayTime solution = new NetworkDelayTime();

        // Test Case 1
        int[][] times1 = { { 2, 1, 1 }, { 2, 3, 1 }, { 3, 4, 1 } };
        int n1 = 4, k1 = 2;

        System.out.println("Example 1:");
        System.out.println("Input: times = [[2,1,1],[2,3,1],[3,4,1]], n = 4, k = 2");
        solution.visualizeNetwork(times1, n1, k1);

        System.out.println("\nOutput (Dijkstra PQ): " + solution.networkDelayTime(times1, n1, k1));
        System.out.println("Output (Dijkstra Matrix): " + solution.networkDelayTimeDijkstraMatrix(times1, n1, k1));
        System.out.println("Output (Bellman-Ford): " + solution.networkDelayTimeBellmanFord(times1, n1, k1));
        System.out.println("Output (SPFA): " + solution.networkDelayTimeSPFA(times1, n1, k1));
        System.out.println("Output (DFS): " + solution.networkDelayTimeDFS(times1, n1, k1));

        solution.showSignalPropagation(times1, n1, k1);
        System.out.println("\nExpected: 2\n");

        // Test Case 2
        int[][] times2 = { { 1, 2, 1 } };
        int n2 = 2, k2 = 1;

        System.out.println("Example 2:");
        System.out.println("Input: times = [[1,2,1]], n = 2, k = 1");
        solution.visualizeNetwork(times2, n2, k2);
        System.out.println("\nOutput: " + solution.networkDelayTime(times2, n2, k2));
        solution.showSignalPropagation(times2, n2, k2);
        System.out.println("\nExpected: 1\n");

        // Test Case 3
        int[][] times3 = { { 1, 2, 1 } };
        int n3 = 2, k3 = 2;

        System.out.println("Example 3:");
        System.out.println("Input: times = [[1,2,1]], n = 2, k = 2");
        solution.visualizeNetwork(times3, n3, k3);
        System.out.println("\nOutput: " + solution.networkDelayTime(times3, n3, k3));
        System.out.println("Expected: -1 (node 1 unreachable from node 2)\n");

        // Additional Test: Complex network
        int[][] times4 = {
                { 1, 2, 1 }, { 1, 3, 4 }, { 2, 3, 2 }, { 2, 4, 7 }, { 3, 4, 1 }
        };
        int n4 = 4, k4 = 1;

        System.out.println("Additional Test: Complex Network");
        solution.visualizeNetwork(times4, n4, k4);
        System.out.println("\nOutput: " + solution.networkDelayTime(times4, n4, k4));
        solution.showSignalPropagation(times4, n4, k4);

        // Test with self-loop and cycles
        int[][] times5 = { { 1, 2, 1 }, { 2, 3, 2 }, { 3, 1, 3 }, { 2, 4, 1 } };
        int n5 = 4, k5 = 1;

        System.out.println("\n\nTest with Cycle:");
        solution.visualizeNetwork(times5, n5, k5);
        System.out.println("\nOutput: " + solution.networkDelayTime(times5, n5, k5));
        solution.showSignalPropagation(times5, n5, k5);

        // Performance comparison
        System.out.println("\n\n=== Performance Comparison ===");
        int[][] largeTimes = generateRandomNetwork(100, 500);
        int nLarge = 100, kLarge = 1;

        long start = System.nanoTime();
        int result1 = solution.networkDelayTime(largeTimes, nLarge, kLarge);
        long end = System.nanoTime();
        System.out.println("Dijkstra (PQ): " + result1 + " (Time: " + (end - start) / 1000 + " μs)");

        start = System.nanoTime();
        int result2 = solution.networkDelayTimeDijkstraMatrix(largeTimes, nLarge, kLarge);
        end = System.nanoTime();
        System.out.println("Dijkstra (Matrix): " + result2 + " (Time: " + (end - start) / 1000 + " μs)");

        start = System.nanoTime();
        int result3 = solution.networkDelayTimeBellmanFord(largeTimes, nLarge, kLarge);
        end = System.nanoTime();
        System.out.println("Bellman-Ford: " + result3 + " (Time: " + (end - start) / 1000 + " μs)");

        start = System.nanoTime();
        int result4 = solution.networkDelayTimeSPFA(largeTimes, nLarge, kLarge);
        end = System.nanoTime();
        System.out.println("SPFA: " + result4 + " (Time: " + (end - start) / 1000 + " μs)");

        start = System.nanoTime();
        int result5 = solution.networkDelayTimeDFS(largeTimes, nLarge, kLarge);
        end = System.nanoTime();
        System.out.println("DFS: " + result5 + " (Time: " + (end - start) / 1000 + " μs)");
    }

    // Helper to generate random network
    private static int[][] generateRandomNetwork(int n, int numEdges) {
        Random rand = new Random(42);
        int[][] times = new int[numEdges][3];

        for (int i = 0; i < numEdges; i++) {
            int u = rand.nextInt(n) + 1;
            int v = rand.nextInt(n) + 1;
            while (v == u) {
                v = rand.nextInt(n) + 1;
            }
            int w = rand.nextInt(10) + 1;
            times[i] = new int[] { u, v, w };
        }

        return times;
    }
}

/*
 * COMPLEXITY ANALYSIS:
 * 
 * Solution 1: Dijkstra with Priority Queue (RECOMMENDED)
 * - Time Complexity: O((E + n) * log n)
 * E edges can be added to priority queue
 * Each operation: O(log n)
 * n nodes processed
 * - Space Complexity: O(n + E)
 * Adjacency list: O(E)
 * Distance array: O(n)
 * Priority queue: O(n)
 * 
 * Solution 2: Dijkstra with Adjacency Matrix
 * - Time Complexity: O(n^2)
 * Finding minimum: O(n)
 * Done n times: O(n^2)
 * Good for dense graphs
 * - Space Complexity: O(n^2)
 * Adjacency matrix
 * 
 * Solution 3: Bellman-Ford Algorithm
 * - Time Complexity: O(n * E)
 * n-1 iterations
 * Each processes E edges
 * - Space Complexity: O(n)
 * Only distance array needed
 * Most space efficient!
 * 
 * Solution 4: SPFA (Shortest Path Faster Algorithm)
 * - Time Complexity: O(E) average, O(n * E) worst
 * Optimized Bellman-Ford
 * Uses queue to only process changed nodes
 * - Space Complexity: O(n + E)
 * Graph + queue + distance array
 * 
 * Solution 5: DFS with Pruning
 * - Time Complexity: O(n + E)
 * Each edge visited once if pruned correctly
 * - Space Complexity: O(n + E)
 * Graph + recursion stack
 * 
 * KEY INSIGHTS:
 * 
 * 1. Problem Understanding:
 * - Signal propagates from source node k
 * - Travel time along edges represents delay
 * - Answer = time when LAST node receives signal
 * - Return -1 if any node unreachable
 * 
 * 2. Why This is Single Source Shortest Path:
 * - Find shortest path from k to ALL nodes
 * - Maximum of these shortest paths = answer
 * - Classic Dijkstra application!
 * 
 * 3. Dijkstra's Algorithm Intuition:
 * - Start from source with distance 0
 * - Always process closest unvisited node
 * - Update neighbors with potential shorter paths
 * - Greedy: local optimum leads to global optimum
 * 
 * 4. Key Observation:
 * - Signal reaches all nodes "in parallel"
 * - Last node to receive = bottleneck
 * - Need shortest path to ALL nodes, take MAX
 * 
 * 5. Why Dijkstra Over Bellman-Ford Here:
 * + No negative weights in this problem
 * + Dijkstra faster: O(E log n) vs O(n*E)
 * + Dijkstra sufficient for non-negative weights
 * 
 * 6. Graph Representation Choices:
 * Adjacency List:
 * + Space efficient for sparse graphs
 * + Fast neighbor lookup
 * - More complex implementation
 * 
 * Adjacency Matrix:
 * + Simple implementation
 * + O(1) edge lookup
 * - O(n^2) space even for sparse graphs
 * 
 * 7. Return -1 Cases:
 * - Graph is disconnected
 * - Some nodes unreachable from source k
 * - Check: any dist[i] still infinity?
 * 
 * 8. Optimization Techniques:
 * - Skip nodes already visited with better distance
 * - Early termination if all nodes visited
 * - Use priority queue for efficient minimum finding
 * - Prune paths that can't improve
 * 
 * 9. Edge Cases:
 * - n = 1: return 0
 * - No outgoing edges from k
 * - Self-loops (allowed)
 * - Parallel edges (multiple edges same nodes)
 * - Cycles in graph
 * 
 * 10. Real-World Applications:
 * - Network latency analysis
 * - Broadcasting protocols
 * - Message propagation in distributed systems
 * - Infection spread modeling
 * - Information diffusion in social networks
 * 
 * 11. Why Priority Queue in Dijkstra:
 * - Need to efficiently get node with minimum distance
 * - Min-heap provides O(log n) operations
 * - Alternative: scan all nodes O(n) each time
 * 
 * 12. Common Mistakes:
 * - Forgetting 1-indexed nodes
 * - Not checking for unreachable nodes
 * - Using visited array incorrectly
 * - Not handling case when graph is empty
 * - Integer overflow with large weights
 * 
 * 13. When to Use Each Algorithm:
 * - Dijkstra (PQ): General purpose, sparse graphs
 * - Dijkstra (Matrix): Dense graphs, small n
 * - Bellman-Ford: Negative weights, simple code
 * - SPFA: When Bellman-Ford too slow
 * - DFS: Small graphs, need paths
 */
