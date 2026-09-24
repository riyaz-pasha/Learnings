import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/*
 * There is an undirected connected tree with n nodes labeled from 0 to n - 1
 * and n - 1 edges.
 * 
 * You are given the integer n and the array edges where edges[i] = [ai, bi]
 * indicates that there is an edge between nodes ai and bi in the tree.
 * 
 * Return an array answer of length n where answer[i] is the sum of the
 * distances between the ith node in the tree and all other nodes.
 * 
 * Example 1:
 * Input: n = 6, edges = [[0,1],[0,2],[2,3],[2,4],[2,5]]
 * Output: [8,12,6,10,10,10]
 * Explanation: The tree is shown above.
 * We can see that dist(0,1) + dist(0,2) + dist(0,3) + dist(0,4) + dist(0,5)
 * equals 1 + 1 + 2 + 2 + 2 = 8.
 * Hence, answer[0] = 8, and so on.
 * 
 * Example 2:
 * Input: n = 1, edges = []
 * Output: [0]
 * 
 * Example 3:
 * Input: n = 2, edges = [[1,0]]
 * Output: [1,1]
 */

class Solution {

    public int[] sumOfDistancesInTree(int n, int[][] edges) {
        // Time Complexity: O(n) where n is the number of nodes
        // Space Complexity: O(n) for adjacency list and arrays

        // Build adjacency list
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }

        for (int[] edge : edges) {
            graph.get(edge[0]).add(edge[1]);
            graph.get(edge[1]).add(edge[0]);
        }

        int[] answer = new int[n];
        int[] count = new int[n]; // count[i] = number of nodes in subtree rooted at i

        // Step 1: Calculate answer[0] and count array using DFS from node 0
        dfs1(0, -1, graph, answer, count);

        // Step 2: Re-root the tree to calculate answer for all other nodes
        dfs2(0, -1, graph, answer, count, n);

        return answer;
    }

    // First DFS: Calculate sum of distances from node 0 and subtree sizes
    private void dfs1(int node, int parent, List<List<Integer>> graph, int[] answer, int[] count) {
        count[node] = 1; // Count the node itself

        for (int child : graph.get(node)) {
            if (child != parent) {
                dfs1(child, node, graph, answer, count);
                count[node] += count[child]; // Add subtree size
                answer[0] += answer[child] + count[child]; // Add distances through this child
            }
        }
    }

    // Second DFS: Re-root the tree to calculate answer for all nodes
    private void dfs2(int node, int parent, List<List<Integer>> graph, int[] answer, int[] count, int n) {
        for (int child : graph.get(node)) {
            if (child != parent) {
                // When moving root from 'node' to 'child':
                // - Nodes in child's subtree get 1 step closer (subtract count[child])
                // - Nodes outside child's subtree get 1 step farther (add n - count[child])
                answer[child] = answer[node] - count[child] + (n - count[child]);

                dfs2(child, node, graph, answer, count, n);
            }
        }
    }

}

// Alternative implementation with more detailed comments
class SolutionDetailed {

    private List<List<Integer>> graph;
    private int[] subtreeSize; // Number of nodes in each subtree
    private int[] distanceSum; // Sum of distances from each node

    public int[] sumOfDistancesInTree(int n, int[][] edges) {
        // Time Complexity: O(n) - two DFS traversals
        // Space Complexity: O(n) - for graph and arrays

        // Initialize data structures
        graph = new ArrayList<>();
        subtreeSize = new int[n];
        distanceSum = new int[n];

        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }

        // Build undirected graph
        for (int[] edge : edges) {
            graph.get(edge[0]).add(edge[1]);
            graph.get(edge[1]).add(edge[0]);
        }

        // Phase 1: Calculate distances from root (node 0) and subtree sizes
        calculateFromRoot(0, -1, 0);

        // Phase 2: Re-root to calculate distances from all other nodes
        reroot(0, -1, n);

        return distanceSum;
    }

    // Calculate sum of distances from root and subtree sizes
    private void calculateFromRoot(int node, int parent, int depth) {
        subtreeSize[node] = 1;
        distanceSum[0] += depth; // Add distance from root to current node

        for (int child : graph.get(node)) {
            if (child != parent) {
                calculateFromRoot(child, node, depth + 1);
                subtreeSize[node] += subtreeSize[child];
            }
        }
    }

    // Re-root the tree to calculate distances from all nodes
    private void reroot(int node, int parent, int totalNodes) {
        for (int child : graph.get(node)) {
            if (child != parent) {
                // Formula for re-rooting:
                // When we move root from 'node' to 'child':
                // - subtreeSize[child] nodes get 1 unit closer
                // - (totalNodes - subtreeSize[child]) nodes get 1 unit farther
                distanceSum[child] = distanceSum[node] - subtreeSize[child] + (totalNodes - subtreeSize[child]);

                reroot(child, node, totalNodes);
            }
        }
    }

}

// Naive O(n²) solution for comparison and verification
class SolutionNaive {

    public int[] sumOfDistancesInTree(int n, int[][] edges) {
        // Time Complexity: O(n²) - BFS from each node
        // Space Complexity: O(n) - for graph and BFS queue

        // Build adjacency list
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }

        for (int[] edge : edges) {
            graph.get(edge[0]).add(edge[1]);
            graph.get(edge[1]).add(edge[0]);
        }

        int[] answer = new int[n];

        // For each node, calculate sum of distances to all other nodes
        for (int i = 0; i < n; i++) {
            answer[i] = calculateDistanceSum(i, graph, n);
        }

        return answer;
    }

    private int calculateDistanceSum(int start, List<List<Integer>> graph, int n) {
        Queue<Integer> queue = new LinkedList<>();
        boolean[] visited = new boolean[n];
        int[] distance = new int[n];

        queue.offer(start);
        visited[start] = true;
        distance[start] = 0;

        while (!queue.isEmpty()) {
            int node = queue.poll();

            for (int neighbor : graph.get(node)) {
                if (!visited[neighbor]) {
                    visited[neighbor] = true;
                    distance[neighbor] = distance[node] + 1;
                    queue.offer(neighbor);
                }
            }
        }

        int sum = 0;
        for (int i = 0; i < n; i++) {
            sum += distance[i];
        }

        return sum;
    }

}

// Test cases and demonstration
class TestCases {

    public static void main(String[] args) {
        Solution solution = new Solution();
        SolutionDetailed solutionDetailed = new SolutionDetailed();
        SolutionNaive solutionNaive = new SolutionNaive();

        // Test Case 1: Example from problem
        int n1 = 6;
        int[][] edges1 = { { 0, 1 }, { 0, 2 }, { 2, 3 }, { 2, 4 }, { 2, 5 } };

        System.out.println("Test Case 1:");
        System.out.println("n = " + n1);
        System.out.println("edges = " + Arrays.deepToString(edges1));
        System.out.println("Tree structure:");
        System.out.println("    1");
        System.out.println("    |");
        System.out.println("    0 --- 2 --- 3");
        System.out.println("          |\\");
        System.out.println("          4 5");
        System.out.println();

        int[] result1 = solution.sumOfDistancesInTree(n1, edges1);
        int[] result1Detailed = solutionDetailed.sumOfDistancesInTree(n1, edges1);
        int[] result1Naive = solutionNaive.sumOfDistancesInTree(n1, edges1);

        System.out.println("Optimized Solution: " + Arrays.toString(result1));
        System.out.println("Detailed Solution:  " + Arrays.toString(result1Detailed));
        System.out.println("Naive Solution:     " + Arrays.toString(result1Naive));
        System.out.println("Expected:           [8, 12, 6, 10, 10, 10]");
        System.out.println();

        // Test Case 2: Single node
        int n2 = 1;
        int[][] edges2 = {};

        System.out.println("Test Case 2 (Single node):");
        int[] result2 = solution.sumOfDistancesInTree(n2, edges2);
        System.out.println("Result: " + Arrays.toString(result2));
        System.out.println("Expected: [0]");
        System.out.println();

        // Test Case 3: Two nodes
        int n3 = 2;
        int[][] edges3 = { { 1, 0 } };

        System.out.println("Test Case 3 (Two nodes):");
        int[] result3 = solution.sumOfDistancesInTree(n3, edges3);
        System.out.println("Result: " + Arrays.toString(result3));
        System.out.println("Expected: [1, 1]");
        System.out.println();

        // Test Case 4: Linear tree
        int n4 = 5;
        int[][] edges4 = { { 0, 1 }, { 1, 2 }, { 2, 3 }, { 3, 4 } };

        System.out.println("Test Case 4 (Linear tree):");
        System.out.println("Tree: 0 - 1 - 2 - 3 - 4");
        int[] result4 = solution.sumOfDistancesInTree(n4, edges4);
        int[] result4Naive = solutionNaive.sumOfDistancesInTree(n4, edges4);
        System.out.println("Optimized: " + Arrays.toString(result4));
        System.out.println("Naive:     " + Arrays.toString(result4Naive));
        System.out.println();

        // Performance comparison for larger tree
        System.out.println("Performance test (larger tree):");
        int n5 = 1000;
        int[][] edges5 = new int[n5 - 1][2];
        // Create a star tree: all nodes connected to node 0
        for (int i = 1; i < n5; i++) {
            edges5[i - 1] = new int[] { 0, i };
        }

        long startTime = System.currentTimeMillis();
        int[] result5 = solution.sumOfDistancesInTree(n5, edges5);
        long endTime = System.currentTimeMillis();
        System.out.println("Optimized solution time: " + (endTime - startTime) + "ms");
        System.out.println("First few results: " + Arrays.toString(Arrays.copyOf(result5, 5)));

        // Note: Naive solution would be too slow for n=1000, so we skip it
    }

}
