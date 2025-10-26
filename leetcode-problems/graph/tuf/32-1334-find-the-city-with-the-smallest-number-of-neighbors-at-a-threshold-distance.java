import java.util.ArrayList;
import java.util.Arrays;
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
 * There are n cities numbered from 0 to n-1. Given the array edges where
 * edges[i] = [fromi, toi, weighti] represents a bidirectional and weighted edge
 * between cities fromi and toi, and given the integer distanceThreshold.
 * 
 * Return the city with the smallest number of cities that are reachable through
 * some path and whose distance is at most distanceThreshold, If there are
 * multiple such cities, return the city with the greatest number.
 * 
 * Notice that the distance of a path connecting cities i and j is equal to the
 * sum of the edges' weights along that path.
 * 
 * Example 1:
 * Input: n = 4, edges = [[0,1,3],[1,2,1],[1,3,4],[2,3,1]], distanceThreshold =
 * 4
 * Output: 3
 * Explanation: The figure above describes the graph.
 * The neighboring cities at a distanceThreshold = 4 for each city are:
 * City 0 -> [City 1, City 2]
 * City 1 -> [City 0, City 2, City 3]
 * City 2 -> [City 0, City 1, City 3]
 * City 3 -> [City 1, City 2]
 * Cities 0 and 3 have 2 neighboring cities at a distanceThreshold = 4, but we
 * have to return city 3 since it has the greatest number.
 * 
 * Example 2:
 * Input: n = 5, edges = [[0,1,2],[0,4,8],[1,2,3],[1,4,2],[2,3,1],[3,4,1]],
 * distanceThreshold = 2
 * Output: 0
 * Explanation: The figure above describes the graph.
 * The neighboring cities at a distanceThreshold = 2 for each city are:
 * City 0 -> [City 1]
 * City 1 -> [City 0, City 4]
 * City 2 -> [City 3, City 4]
 * City 3 -> [City 2, City 4]
 * City 4 -> [City 1, City 2, City 3]
 * The city 0 has 1 neighboring city at a distanceThreshold = 2.
 */

class FindTheCityWithTheSmallestNumberOfNeighbors {

    public int findTheCity(int n, int[][] edges, int distanceThreshold) {
        final int INF = 1_000_000_000; // safe sentinel to avoid overflow
        int[][] dist = new int[n][n];

        // init distances
        for (int i = 0; i < n; i++) {
            Arrays.fill(dist[i], INF);
            dist[i][i] = 0;
        }

        // fill direct edges (undirected)
        for (int[] edge : edges) {
            int u = edge[0], v = edge[1], w = edge[2];
            dist[u][v] = w;
            dist[v][u] = w;
        }

        // Floyd–Warshall
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                if (dist[i][k] == INF)
                    continue;
                for (int j = 0; j < n; j++) {
                    if (dist[k][j] == INF)
                        continue;
                    int throughK = dist[i][k] + dist[k][j];
                    if (throughK < dist[i][j]) {
                        dist[i][j] = throughK;
                    }
                }
            }
        }

        // find city with smallest number of reachable neighbors within threshold
        int result = -1;
        int minCount = Integer.MAX_VALUE;

        for (int city = 0; city < n; city++) {
            int count = 0;
            for (int other = 0; other < n; other++) {
                if (other == city)
                    continue; // exclude self
                if (dist[city][other] <= distanceThreshold) {
                    count++;
                }
            }
            // choose smaller count; in tie choose larger city index
            if (count < minCount || (count == minCount && city > result)) {
                minCount = count;
                result = city;
            }
        }

        return result;
    }

}

class CityWithSmallestNeighbors {

    // Solution 1: Floyd-Warshall Algorithm (RECOMMENDED)
    // Time Complexity: O(n^3)
    // Space Complexity: O(n^2)
    public int findTheCity(int n, int[][] edges, int distanceThreshold) {
        // Initialize distance matrix
        int[][] dist = new int[n][n];

        // Set all distances to infinity initially
        for (int i = 0; i < n; i++) {
            Arrays.fill(dist[i], Integer.MAX_VALUE / 2); // Avoid overflow
            dist[i][i] = 0; // Distance to self is 0
        }

        // Fill in the edge weights
        for (int[] edge : edges) {
            int from = edge[0];
            int to = edge[1];
            int weight = edge[2];
            dist[from][to] = weight;
            dist[to][from] = weight; // Bidirectional
        }

        // Floyd-Warshall: find all-pairs shortest paths
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    dist[i][j] = Math.min(dist[i][j], dist[i][k] + dist[k][j]);
                }
            }
        }

        // Count reachable cities for each city
        int minReachable = n;
        int resultCity = 0;

        for (int i = 0; i < n; i++) {
            int reachableCount = 0;
            for (int j = 0; j < n; j++) {
                if (i != j && dist[i][j] <= distanceThreshold) {
                    reachableCount++;
                }
            }

            // Update result if this city has fewer reachable cities
            // If tie, prefer larger city number
            if (reachableCount <= minReachable) {
                minReachable = reachableCount;
                resultCity = i;
            }
        }

        return resultCity;
    }

    // Solution 2: Dijkstra from Each City
    // Time Complexity: O(n * (E log n))
    // Space Complexity: O(n + E)
    public int findTheCityDijkstra(int n, int[][] edges, int distanceThreshold) {
        // Build adjacency list
        Map<Integer, List<int[]>> graph = new HashMap<>();
        for (int i = 0; i < n; i++) {
            graph.put(i, new ArrayList<>());
        }

        for (int[] edge : edges) {
            graph.get(edge[0]).add(new int[] { edge[1], edge[2] });
            graph.get(edge[1]).add(new int[] { edge[0], edge[2] });
        }

        int minReachable = n;
        int resultCity = 0;

        // Run Dijkstra from each city
        for (int i = 0; i < n; i++) {
            int reachableCount = dijkstraCount(i, n, graph, distanceThreshold);

            if (reachableCount <= minReachable) {
                minReachable = reachableCount;
                resultCity = i;
            }
        }

        return resultCity;
    }

    private int dijkstraCount(int start, int n, Map<Integer, List<int[]>> graph, int threshold) {
        int[] dist = new int[n];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[start] = 0;

        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[1] - b[1]);
        pq.offer(new int[] { start, 0 });

        while (!pq.isEmpty()) {
            int[] curr = pq.poll();
            int node = curr[0];
            int currDist = curr[1];

            if (currDist > dist[node])
                continue;

            for (int[] neighbor : graph.get(node)) {
                int nextNode = neighbor[0];
                int weight = neighbor[1];
                int newDist = currDist + weight;

                if (newDist < dist[nextNode]) {
                    dist[nextNode] = newDist;
                    pq.offer(new int[] { nextNode, newDist });
                }
            }
        }

        // Count cities within threshold
        int count = 0;
        for (int i = 0; i < n; i++) {
            if (i != start && dist[i] <= threshold) {
                count++;
            }
        }

        return count;
    }

    // Solution 3: Bellman-Ford from Each City
    // Time Complexity: O(n^2 * E)
    // Space Complexity: O(n)
    public int findTheCityBellmanFord(int n, int[][] edges, int distanceThreshold) {
        int minReachable = n;
        int resultCity = 0;

        for (int i = 0; i < n; i++) {
            int reachableCount = bellmanFordCount(i, n, edges, distanceThreshold);

            if (reachableCount <= minReachable) {
                minReachable = reachableCount;
                resultCity = i;
            }
        }

        return resultCity;
    }

    private int bellmanFordCount(int start, int n, int[][] edges, int threshold) {
        int[] dist = new int[n];
        Arrays.fill(dist, Integer.MAX_VALUE / 2);
        dist[start] = 0;

        // Relax edges n-1 times
        for (int i = 0; i < n - 1; i++) {
            for (int[] edge : edges) {
                int u = edge[0], v = edge[1], w = edge[2];

                if (dist[u] + w < dist[v]) {
                    dist[v] = dist[u] + w;
                }
                if (dist[v] + w < dist[u]) {
                    dist[u] = dist[v] + w;
                }
            }
        }

        // Count cities within threshold
        int count = 0;
        for (int i = 0; i < n; i++) {
            if (i != start && dist[i] <= threshold) {
                count++;
            }
        }

        return count;
    }

    // Solution 4: SPFA (Shortest Path Faster Algorithm) from Each City
    // Time Complexity: O(n * E) average, O(n^2 * E) worst
    // Space Complexity: O(n + E)
    public int findTheCitySPFA(int n, int[][] edges, int distanceThreshold) {
        Map<Integer, List<int[]>> graph = new HashMap<>();
        for (int i = 0; i < n; i++) {
            graph.put(i, new ArrayList<>());
        }

        for (int[] edge : edges) {
            graph.get(edge[0]).add(new int[] { edge[1], edge[2] });
            graph.get(edge[1]).add(new int[] { edge[0], edge[2] });
        }

        int minReachable = n;
        int resultCity = 0;

        for (int i = 0; i < n; i++) {
            int reachableCount = spfaCount(i, n, graph, distanceThreshold);

            if (reachableCount <= minReachable) {
                minReachable = reachableCount;
                resultCity = i;
            }
        }

        return resultCity;
    }

    private int spfaCount(int start, int n, Map<Integer, List<int[]>> graph, int threshold) {
        int[] dist = new int[n];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[start] = 0;

        Queue<Integer> queue = new LinkedList<>();
        boolean[] inQueue = new boolean[n];
        queue.offer(start);
        inQueue[start] = true;

        while (!queue.isEmpty()) {
            int node = queue.poll();
            inQueue[node] = false;

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

        int count = 0;
        for (int i = 0; i < n; i++) {
            if (i != start && dist[i] <= threshold) {
                count++;
            }
        }

        return count;
    }

    // Helper method to visualize the graph
    public void visualizeGraph(int n, int[][] edges, int distanceThreshold) {
        System.out.println("\n=== Graph Visualization ===");
        System.out.println("Cities: " + n + " (numbered 0 to " + (n - 1) + ")");
        System.out.println("Distance Threshold: " + distanceThreshold);
        System.out.println("\nEdges (bidirectional):");

        for (int[] edge : edges) {
            System.out.printf("  City %d ↔ City %d (weight: %d)\n",
                    edge[0], edge[1], edge[2]);
        }
    }

    // Helper method to show detailed analysis
    public void showDetailedAnalysis(int n, int[][] edges, int distanceThreshold) {
        // Build distance matrix using Floyd-Warshall
        int[][] dist = new int[n][n];
        for (int i = 0; i < n; i++) {
            Arrays.fill(dist[i], Integer.MAX_VALUE / 2);
            dist[i][i] = 0;
        }

        for (int[] edge : edges) {
            dist[edge[0]][edge[1]] = edge[2];
            dist[edge[1]][edge[0]] = edge[2];
        }

        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    dist[i][j] = Math.min(dist[i][j], dist[i][k] + dist[k][j]);
                }
            }
        }

        System.out.println("\n=== Shortest Distance Matrix ===");
        System.out.print("     ");
        for (int i = 0; i < n; i++) {
            System.out.printf("%4d ", i);
        }
        System.out.println();

        for (int i = 0; i < n; i++) {
            System.out.printf("%4d ", i);
            for (int j = 0; j < n; j++) {
                if (dist[i][j] >= Integer.MAX_VALUE / 2) {
                    System.out.print(" ∞   ");
                } else {
                    System.out.printf("%4d ", dist[i][j]);
                }
            }
            System.out.println();
        }

        System.out.println("\n=== Reachable Cities Analysis ===");
        System.out.println("(Distance ≤ " + distanceThreshold + ")");

        int minReachable = n;
        int resultCity = 0;

        for (int i = 0; i < n; i++) {
            List<Integer> reachable = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                if (i != j && dist[i][j] <= distanceThreshold) {
                    reachable.add(j);
                }
            }

            System.out.printf("City %d -> %s (count: %d)\n",
                    i, reachable, reachable.size());

            if (reachable.size() <= minReachable) {
                minReachable = reachable.size();
                resultCity = i;
            }
        }

        System.out.println("\nResult: City " + resultCity +
                " with " + minReachable + " reachable cities");
    }

    // Helper to print distance matrix in a readable format
    public void printDistanceMatrix(int[][] dist, int n) {
        System.out.println("\nDistance Matrix:");
        System.out.print("   ");
        for (int i = 0; i < n; i++) {
            System.out.printf("%3d ", i);
        }
        System.out.println();

        for (int i = 0; i < n; i++) {
            System.out.printf("%2d ", i);
            for (int j = 0; j < n; j++) {
                if (dist[i][j] >= Integer.MAX_VALUE / 2) {
                    System.out.print(" ∞  ");
                } else {
                    System.out.printf("%3d ", dist[i][j]);
                }
            }
            System.out.println();
        }
    }

    // Test cases
    public static void main(String[] args) {
        CityWithSmallestNeighbors solution = new CityWithSmallestNeighbors();

        // Test Case 1
        int n1 = 4;
        int[][] edges1 = { { 0, 1, 3 }, { 1, 2, 1 }, { 1, 3, 4 }, { 2, 3, 1 } };
        int threshold1 = 4;

        System.out.println("Example 1:");
        System.out.println("Input: n = 4, distanceThreshold = 4");
        solution.visualizeGraph(n1, edges1, threshold1);

        System.out.println("\nOutput (Floyd-Warshall): " +
                solution.findTheCity(n1, edges1, threshold1));
        System.out.println("Output (Dijkstra): " +
                solution.findTheCityDijkstra(n1, edges1, threshold1));
        System.out.println("Output (Bellman-Ford): " +
                solution.findTheCityBellmanFord(n1, edges1, threshold1));
        System.out.println("Output (SPFA): " +
                solution.findTheCitySPFA(n1, edges1, threshold1));

        solution.showDetailedAnalysis(n1, edges1, threshold1);
        System.out.println("\nExpected: 3\n");

        System.out.println("=".repeat(70));

        // Test Case 2
        int n2 = 5;
        int[][] edges2 = { { 0, 1, 2 }, { 0, 4, 8 }, { 1, 2, 3 }, { 1, 4, 2 }, { 2, 3, 1 }, { 3, 4, 1 } };
        int threshold2 = 2;

        System.out.println("\nExample 2:");
        System.out.println("Input: n = 5, distanceThreshold = 2");
        solution.visualizeGraph(n2, edges2, threshold2);

        System.out.println("\nOutput (Floyd-Warshall): " +
                solution.findTheCity(n2, edges2, threshold2));
        System.out.println("Output (Dijkstra): " +
                solution.findTheCityDijkstra(n2, edges2, threshold2));

        solution.showDetailedAnalysis(n2, edges2, threshold2);
        System.out.println("\nExpected: 0\n");

        System.out.println("=".repeat(70));

        // Test Case 3: All cities reachable
        int n3 = 3;
        int[][] edges3 = { { 0, 1, 1 }, { 1, 2, 1 }, { 0, 2, 1 } };
        int threshold3 = 10;

        System.out.println("\nTest Case: All cities reachable");
        solution.visualizeGraph(n3, edges3, threshold3);
        System.out.println("\nOutput: " + solution.findTheCity(n3, edges3, threshold3));
        solution.showDetailedAnalysis(n3, edges3, threshold3);
        System.out.println("Expected: 2 (largest city when all have same count)\n");

        // Test Case 4: Disconnected graph
        int n4 = 4;
        int[][] edges4 = { { 0, 1, 1 }, { 2, 3, 1 } };
        int threshold4 = 5;

        System.out.println("\nTest Case: Disconnected graph");
        solution.visualizeGraph(n4, edges4, threshold4);
        System.out.println("\nOutput: " + solution.findTheCity(n4, edges4, threshold4));
        solution.showDetailedAnalysis(n4, edges4, threshold4);

        // Test Case 5: No reachable cities
        int n5 = 3;
        int[][] edges5 = { { 0, 1, 10 }, { 1, 2, 10 } };
        int threshold5 = 5;

        System.out.println("\nTest Case: No reachable cities within threshold");
        solution.visualizeGraph(n5, edges5, threshold5);
        System.out.println("\nOutput: " + solution.findTheCity(n5, edges5, threshold5));
        solution.showDetailedAnalysis(n5, edges5, threshold5);
        System.out.println("Expected: 2 (largest city when all have 0 reachable)\n");

        // Performance comparison
        System.out.println("=".repeat(70));
        System.out.println("\n=== Performance Comparison ===");
        int nLarge = 50;
        int[][] edgesLarge = generateDenseGraph(50, 300);
        int thresholdLarge = 100;

        long start = System.nanoTime();
        int result1 = solution.findTheCity(nLarge, edgesLarge, thresholdLarge);
        long end = System.nanoTime();
        System.out.println("Floyd-Warshall: " + result1 +
                " (Time: " + (end - start) / 1000000 + " ms)");

        start = System.nanoTime();
        int result2 = solution.findTheCityDijkstra(nLarge, edgesLarge, thresholdLarge);
        end = System.nanoTime();
        System.out.println("Dijkstra: " + result2 +
                " (Time: " + (end - start) / 1000000 + " ms)");

        start = System.nanoTime();
        int result3 = solution.findTheCitySPFA(nLarge, edgesLarge, thresholdLarge);
        end = System.nanoTime();
        System.out.println("SPFA: " + result3 +
                " (Time: " + (end - start) / 1000000 + " ms)");
    }

    // Helper to generate random dense graph
    private static int[][] generateDenseGraph(int n, int numEdges) {
        Random rand = new Random(42);
        Set<String> edgeSet = new HashSet<>();
        List<int[]> edges = new ArrayList<>();

        // Ensure connectivity
        for (int i = 0; i < n - 1; i++) {
            String key = i + "-" + (i + 1);
            edgeSet.add(key);
            edges.add(new int[] { i, i + 1, rand.nextInt(10) + 1 });
        }

        // Add random edges
        while (edges.size() < numEdges) {
            int u = rand.nextInt(n);
            int v = rand.nextInt(n);
            if (u == v)
                continue;

            String key = Math.min(u, v) + "-" + Math.max(u, v);
            if (!edgeSet.contains(key)) {
                edgeSet.add(key);
                edges.add(new int[] { u, v, rand.nextInt(10) + 1 });
            }
        }

        return edges.toArray(new int[edges.size()][]);
    }
}

/*
 * COMPLEXITY ANALYSIS:
 * 
 * Solution 1: Floyd-Warshall Algorithm (RECOMMENDED)
 * - Time Complexity: O(n^3)
 * Three nested loops for Floyd-Warshall
 * O(n^2) to count reachable cities
 * Total: O(n^3)
 * - Space Complexity: O(n^2)
 * Distance matrix: n × n
 * Clean and simple implementation
 * 
 * Solution 2: Dijkstra from Each City
 * - Time Complexity: O(n * (E + n) * log n)
 * Run Dijkstra n times
 * Each Dijkstra: O((E + n) * log n)
 * - Space Complexity: O(n + E)
 * Adjacency list: O(E)
 * Distance array: O(n)
 * Priority queue: O(n)
 * 
 * Solution 3: Bellman-Ford from Each City
 * - Time Complexity: O(n^2 * E)
 * Run Bellman-Ford n times
 * Each Bellman-Ford: O(n * E)
 * - Space Complexity: O(n)
 * Only distance array needed
 * 
 * Solution 4: SPFA from Each City
 * - Time Complexity: O(n * E) average, O(n^2 * E) worst
 * Run SPFA n times
 * Each SPFA: O(E) average
 * - Space Complexity: O(n + E)
 * Adjacency list + queue + distance array
 * 
 * KEY INSIGHTS:
 * 
 * 1. Problem Breakdown:
 * - Find shortest paths from EACH city to ALL other cities
 * - Count cities reachable within threshold
 * - Return city with minimum reachable count
 * - Tie-breaker: choose larger city number
 * 
 * 2. Why Floyd-Warshall is Best:
 * + Simple implementation
 * + Computes all-pairs shortest paths at once
 * + O(n^3) is acceptable for n ≤ 100
 * + Clean and elegant
 * + Easy to understand and debug
 * 
 * 3. When to Use Each Algorithm:
 * Floyd-Warshall:
 * + Dense graphs
 * + Small n (≤ 100)
 * + Need all-pairs distances
 * 
 * Dijkstra:
 * + Sparse graphs
 * + Larger n
 * + Only need distances from specific sources
 * 
 * Bellman-Ford:
 * + Negative weights (not in this problem)
 * + Simple implementation
 * 
 * SPFA:
 * + Sparse graphs
 * + Often faster than Bellman-Ford
 * 
 * 4. Floyd-Warshall Intuition:
 * for k in cities:
 * for i in cities:
 * for j in cities:
 * dist[i][j] = min(dist[i][j],
 * dist[i][k] + dist[k][j])
 * 
 * Try using each city k as intermediate node
 * Update if path through k is shorter
 * 
 * 5. Tie-Breaking Rule:
 * if (reachableCount <= minReachable):
 * update result
 * 
 * Using <= instead of < ensures we pick
 * the LARGEST city number when tied
 * 
 * 6. Edge Cases:
 * - n = 1: return 0 (only one city)
 * - threshold = 0: no cities reachable
 * - All cities reachable from all: return n-1
 * - Disconnected graph: some cities unreachable
 * - threshold very large: all cities reachable
 * 
 * 7. Bidirectional Graph:
 * - Edges work both ways
 * - Must add edge in both directions
 * - dist[u][v] = dist[v][u]
 * 
 * 8. Avoiding Overflow:
 * - Initialize with Integer.MAX_VALUE / 2
 * - Not Integer.MAX_VALUE to avoid overflow
 * - When adding distances: a + b won't overflow
 * 
 * 9. Distance Matrix Properties:
 * - dist[i][i] = 0 (distance to self)
 * - dist[i][j] = dist[j][i] (symmetric)
 * - dist[i][j] ≤ dist[i][k] + dist[k][j] (triangle inequality)
 * 
 * 10. Optimization for Dijkstra:
 * - Can stop early once all distances found
 * - Don't need to explore beyond threshold
 * - Use min-heap for efficiency
 * 
 * 11. Why Not DFS:
 * - DFS doesn't find shortest paths
 * - Would need to explore all paths
 * - Much less efficient
 * 
 * 12. Time Complexity Comparison:
 * n = 100, E = 1000
 * 
 * Floyd-Warshall: O(100^3) = 1,000,000
 * Dijkstra n times: O(100 * 1000 * log 100) ≈ 670,000
 * Bellman-Ford: O(100 * 100 * 1000) = 10,000,000
 * 
 * For small n, Floyd-Warshall is often fastest!
 * 
 * 13. Space Complexity Comparison:
 * Floyd-Warshall: O(n^2) - stores all distances
 * Dijkstra: O(n + E) - only current distances
 * 
 * For n ≤ 100, both are negligible
 * 
 * 14. Practical Considerations:
 * - Floyd-Warshall: simplest code
 * - Dijkstra: better for large sparse graphs
 * - SPFA: good average case performance
 * - Bellman-Ford: handles negative weights
 * 
 * 15. Real-World Applications:
 * - Network topology optimization
 * - Server placement
 * - Warehouse location selection
 * - Emergency service station placement
 * - Finding least connected nodes
 * 
 * 16. Common Mistakes:
 * - Forgetting bidirectional edges
 * - Wrong tie-breaking (< instead of <=)
 * - Not handling disconnected components
 * - Integer overflow with MAX_VALUE
 * - Off-by-one in counting neighbors
 * 
 * 17. Why This Problem Needs All-Pairs:
 * - Need distances from EVERY city to EVERY other
 * - Can't just run from one source
 * - Floyd-Warshall perfect fit!
 * 
 * 18. Optimization Techniques:
 * - Early termination in Dijkstra
 * - Pruning unreachable nodes
 * - Caching distance computations
 * - Parallel processing (for large graphs)
 */
