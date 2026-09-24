import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
/*
 * You are given an unrooted weighted tree with n vertices representing servers
 * numbered from 0 to n - 1, an array edges where edges[i] = [ai, bi, weighti]
 * represents a bidirectional edge between vertices ai and bi of weight weighti.
 * You are also given an integer signalSpeed.
 * 
 * Two servers a and b are connectable through a server c if:
 * 
 * a < b, a != c and b != c.
 * The distance from c to a is divisible by signalSpeed.
 * The distance from c to b is divisible by signalSpeed.
 * The path from c to b and the path from c to a do not share any edges.
 * Return an integer array count of length n where count[i] is the number of
 * server pairs that are connectable through the server i.
 * 
 * Example 1:
 * Input: edges = [[0,1,1],[1,2,5],[2,3,13],[3,4,9],[4,5,2]], signalSpeed = 1
 * Output: [0,4,6,6,4,0]
 * Explanation: Since signalSpeed is 1, count[c] is equal to the number of pairs
 * of paths that start at c and do not share any edges.
 * In the case of the given path graph, count[c] is equal to the number of
 * servers to the left of c multiplied by the servers to the right of c.
 * 
 * Example 2:
 * Input: edges = [[0,6,3],[6,5,3],[0,3,1],[3,2,7],[3,1,6],[3,4,2]], signalSpeed
 * = 3
 * Output: [2,0,0,0,0,0,2]
 * Explanation: Through server 0, there are 2 pairs of connectable servers: (4,
 * 5) and (4, 6).
 * Through server 6, there are 2 pairs of connectable servers: (4, 5) and (0,
 * 5).
 * It can be shown that no two servers are connectable through servers other
 * than 0 and 6.
 */

class Solution {

    /**
     * Main function to count connectable server pairs through each server
     * Time Complexity: O(n^2) where n is number of servers
     * Space Complexity: O(n) for adjacency list and visited array
     */
    public int[] countPairsOfConnectableServers(int[][] edges, int signalSpeed) {
        int n = edges.length + 1;
        int[] result = new int[n];

        // Build adjacency list representation of the tree
        List<List<int[]>> graph = buildGraph(edges, n);

        // For each server as potential intermediate server
        for (int c = 0; c < n; c++) {
            result[c] = countPairsForServer(graph, c, signalSpeed);
        }

        return result;
    }

    /**
     * Build adjacency list from edges array
     */
    private List<List<int[]>> buildGraph(int[][] edges, int n) {
        List<List<int[]>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }

        for (int[] edge : edges) {
            int u = edge[0], v = edge[1], w = edge[2];
            graph.get(u).add(new int[] { v, w });
            graph.get(v).add(new int[] { u, w });
        }

        return graph;
    }

    /**
     * Count pairs of connectable servers through server c
     */
    private int countPairsForServer(List<List<int[]>> graph, int c, int signalSpeed) {
        List<Integer> validServers = new ArrayList<>();
        boolean[] visited = new boolean[graph.size()];

        // Find all servers reachable from c with distance divisible by signalSpeed
        dfs(graph, c, c, 0, signalSpeed, visited, validServers);

        // Count pairs where both servers satisfy the distance constraint
        // and have non-overlapping paths from c
        return countValidPairs(graph, c, validServers, signalSpeed);
    }

    /**
     * DFS to find all servers with distance from start divisible by signalSpeed
     */
    private void dfs(List<List<int[]>> graph, int start, int current, int distance,
            int signalSpeed, boolean[] visited, List<Integer> validServers) {
        visited[current] = true;

        // If distance is divisible by signalSpeed and not the start server
        if (current != start && distance % signalSpeed == 0) {
            validServers.add(current);
        }

        // Continue DFS to neighbors
        for (int[] neighbor : graph.get(current)) {
            int next = neighbor[0];
            int weight = neighbor[1];

            if (!visited[next]) {
                dfs(graph, start, next, distance + weight, signalSpeed, visited, validServers);
            }
        }
    }

    /**
     * Count valid pairs ensuring non-overlapping paths
     */
    private int countValidPairs(List<List<int[]>> graph, int c, List<Integer> validServers, int signalSpeed) {
        int count = 0;
        int n = validServers.size();

        // Check all pairs of valid servers
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                int a = validServers.get(i);
                int b = validServers.get(j);

                // Ensure a < b as per problem constraint
                if (a > b) {
                    int temp = a;
                    a = b;
                    b = temp;
                }

                // Check if paths from c to a and c to b don't share edges
                if (hasNonOverlappingPaths(graph, c, a, b)) {
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * Check if paths from c to a and c to b don't share any edges
     */
    private boolean hasNonOverlappingPaths(List<List<int[]>> graph, int c, int a, int b) {
        // Find path from c to a
        List<int[]> pathToA = findPath(graph, c, a);
        // Find path from c to b
        List<int[]> pathToB = findPath(graph, c, b);

        if (pathToA == null || pathToB == null)
            return false;

        // Convert paths to sets of edges for comparison
        Set<String> edgesA = new HashSet<>();
        Set<String> edgesB = new HashSet<>();

        for (int[] edge : pathToA) {
            edgesA.add(getEdgeKey(edge[0], edge[1]));
        }

        for (int[] edge : pathToB) {
            edgesB.add(getEdgeKey(edge[0], edge[1]));
        }

        // Check if any edge is shared
        for (String edge : edgesA) {
            if (edgesB.contains(edge)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Find path between two nodes using DFS
     */
    private List<int[]> findPath(List<List<int[]>> graph, int start, int end) {
        boolean[] visited = new boolean[graph.size()];
        List<int[]> path = new ArrayList<>();

        if (findPathDFS(graph, start, end, visited, path)) {
            return path;
        }
        return null;
    }

    /**
     * DFS helper to find path
     */
    private boolean findPathDFS(List<List<int[]>> graph, int current, int target,
            boolean[] visited, List<int[]> path) {
        if (current == target) {
            return true;
        }

        visited[current] = true;

        for (int[] neighbor : graph.get(current)) {
            int next = neighbor[0];

            if (!visited[next]) {
                path.add(new int[] { current, next });
                if (findPathDFS(graph, next, target, visited, path)) {
                    return true;
                }
                path.remove(path.size() - 1);
            }
        }

        return false;
    }

    /**
     * Create a unique key for an edge (undirected)
     */
    private String getEdgeKey(int u, int v) {
        return Math.min(u, v) + "-" + Math.max(u, v);
    }

}

// Optimized solution using subtree counting
class SolutionOptimized {

    /**
     * Optimized approach: For each server, count reachable servers in each subtree
     * Time Complexity: O(n^2)
     * Space Complexity: O(n)
     */
    public int[] countPairsOfConnectableServers(int[][] edges, int signalSpeed) {
        int n = edges.length + 1;
        int[] result = new int[n];

        // Build adjacency list
        List<List<int[]>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }

        for (int[] edge : edges) {
            int u = edge[0], v = edge[1], w = edge[2];
            graph.get(u).add(new int[] { v, w });
            graph.get(v).add(new int[] { u, w });
        }

        // For each potential intermediate server
        for (int c = 0; c < n; c++) {
            List<Integer> subtreeCounts = new ArrayList<>();

            // Count reachable servers in each subtree rooted at c's neighbors
            for (int[] neighbor : graph.get(c)) {
                int next = neighbor[0];
                int weight = neighbor[1];

                int count = countReachableServers(graph, next, c, weight, signalSpeed);
                if (count > 0) {
                    subtreeCounts.add(count);
                }
            }

            // Calculate pairs from different subtrees
            int totalPairs = 0;
            int prefixSum = 0;

            for (int count : subtreeCounts) {
                totalPairs += prefixSum * count;
                prefixSum += count;
            }

            result[c] = totalPairs;
        }

        return result;
    }

    /**
     * Count servers reachable from start with distance divisible by signalSpeed
     */
    private int countReachableServers(List<List<int[]>> graph, int current, int parent,
            int distance, int signalSpeed) {
        int count = 0;

        // If current distance is divisible by signalSpeed, count this server
        if (distance % signalSpeed == 0) {
            count = 1;
        }

        // Recursively count in subtrees
        for (int[] neighbor : graph.get(current)) {
            int next = neighbor[0];
            int weight = neighbor[1];

            if (next != parent) {
                count += countReachableServers(graph, next, current, distance + weight, signalSpeed);
            }
        }

        return count;
    }

}

// Test cases
class TestCases {

    public static void main(String[] args) {
        SolutionOptimized solution = new SolutionOptimized();

        // Test Case 1
        int[][] edges1 = { { 0, 1, 1 }, { 1, 2, 5 }, { 2, 3, 13 }, { 3, 4, 9 }, { 4, 5, 2 } };
        int signalSpeed1 = 1;
        int[] result1 = solution.countPairsOfConnectableServers(edges1, signalSpeed1);
        System.out.println("Test 1: " + Arrays.toString(result1));
        // Expected: [0,4,6,6,4,0]

        // Test Case 2
        int[][] edges2 = { { 0, 6, 3 }, { 6, 5, 3 }, { 0, 3, 1 }, { 3, 2, 7 }, { 3, 1, 6 }, { 3, 4, 2 } };
        int signalSpeed2 = 3;
        int[] result2 = solution.countPairsOfConnectableServers(edges2, signalSpeed2);
        System.out.println("Test 2: " + Arrays.toString(result2));
        // Expected: [2,0,0,0,0,0,2]
    }

}
