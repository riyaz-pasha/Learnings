
/*
 * There are n cities numbered from 0 to n - 1 and n - 1 roads such that there
 * is only one way to travel between two different cities (this network form a
 * tree). Last year, The ministry of transport decided to orient the roads in
 * one direction because they are too narrow.
 * Roads are represented by connections where connections[i] = [ai, bi]
 * represents a road from city ai to city bi.
 * 
 * This year, there will be a big event in the capital (city 0), and many people
 * want to travel to this city.
 * 
 * Your task consists of reorienting some roads such that each city can visit
 * the city 0. Return the minimum number of edges changed.
 * 
 * It's guaranteed that each city can reach city 0 after reorder.
 * 
 */

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

class ReorderRoutesToMakeAllPathsLeadToTheCityZero {

    public int minReorder(int n, int[][] connections) {
        List<Integer>[] in = new ArrayList[n];
        List<Integer>[] out = new ArrayList[n];
        for (int i = 0; i < n; i++) {
            in[i] = new ArrayList<>();
            out[i] = new ArrayList<>();
        }

        for (int[] connection : connections) {
            out[connection[0]].add(connection[1]); // original direction
            in[connection[1]].add(connection[0]); // reverse direction
        }

        int count = 0;
        boolean[] visited = new boolean[n];
        Queue<Integer> queue = new LinkedList<>();
        queue.add(0);

        while (!queue.isEmpty()) {
            int node = queue.poll();
            if (visited[node])
                continue;
            visited[node] = true;

            for (int neighbor : out[node]) {
                if (!visited[neighbor]) {
                    count++; // edge needs to be reversed
                    queue.offer(neighbor);
                }
            }

            for (int neighbor : in[node]) {
                if (!visited[neighbor]) {
                    queue.offer(neighbor); // already points to node
                }
            }
        }

        return count;
    }

}

class ReorderRoutesToMakeAllPathsLeadToTheCityZero2 {

    public int minReorder(int n, int[][] connections) {
        List<int[]>[] graph = new ArrayList[n];
        for (int i = 0; i < n; i++) {
            graph[i] = new ArrayList<>();
        }

        // Build graph: store direction info
        for (int[] conn : connections) {
            graph[conn[0]].add(new int[] { conn[1], 1 }); // original direction (u → v)
            graph[conn[1]].add(new int[] { conn[0], 0 }); // reverse direction (v → u)
        }

        boolean[] visited = new boolean[n];
        Queue<Integer> queue = new LinkedList<>();
        queue.offer(0);
        visited[0] = true;

        int changes = 0;

        while (!queue.isEmpty()) {
            int node = queue.poll();

            for (int[] neighbor : graph[node]) {
                int next = neighbor[0];
                int needsReorder = neighbor[1];

                if (!visited[next]) {
                    visited[next] = true;
                    changes += needsReorder;
                    queue.offer(next);
                }
            }
        }

        return changes;
    }

}

class Solution {

    /**
     * Approach 1: DFS with adjacency list tracking edge directions
     * Time: O(n), Space: O(n)
     */
    public int minReorder(int n, int[][] connections) {
        // Build adjacency list with direction information
        List<List<int[]>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }

        // Add edges in both directions, marking original direction
        for (int[] conn : connections) {
            int a = conn[0], b = conn[1];
            graph.get(a).add(new int[] { b, 1 }); // Original direction (needs reversal)
            graph.get(b).add(new int[] { a, 0 }); // Reverse direction (no reversal needed)
        }

        boolean[] visited = new boolean[n];
        return dfs(0, graph, visited);
    }

    private int dfs(int city, List<List<int[]>> graph, boolean[] visited) {
        visited[city] = true;
        int changes = 0;

        for (int[] edge : graph.get(city)) {
            int neighbor = edge[0];
            int needsReversal = edge[1];

            if (!visited[neighbor]) {
                changes += needsReversal + dfs(neighbor, graph, visited);
            }
        }

        return changes;
    }

    /**
     * Approach 2: BFS solution
     * Time: O(n), Space: O(n)
     */
    public int minReorderBFS(int n, int[][] connections) {
        List<List<int[]>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }

        for (int[] conn : connections) {
            int a = conn[0], b = conn[1];
            graph.get(a).add(new int[] { b, 1 }); // Original edge
            graph.get(b).add(new int[] { a, 0 }); // Reverse edge
        }

        Queue<Integer> queue = new LinkedList<>();
        boolean[] visited = new boolean[n];
        queue.offer(0);
        visited[0] = true;
        int changes = 0;

        while (!queue.isEmpty()) {
            int city = queue.poll();

            for (int[] edge : graph.get(city)) {
                int neighbor = edge[0];
                int needsReversal = edge[1];

                if (!visited[neighbor]) {
                    visited[neighbor] = true;
                    changes += needsReversal;
                    queue.offer(neighbor);
                }
            }
        }

        return changes;
    }

    /**
     * Approach 3: Using Set for original edges (cleaner logic)
     * Time: O(n), Space: O(n)
     */
    public int minReorderWithSet(int n, int[][] connections) {
        // Store original edges in a set for quick lookup
        Set<String> originalEdges = new HashSet<>();
        List<List<Integer>> graph = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }

        for (int[] conn : connections) {
            int a = conn[0], b = conn[1];
            originalEdges.add(a + "," + b);
            graph.get(a).add(b);
            graph.get(b).add(a); // Bidirectional for traversal
        }

        boolean[] visited = new boolean[n];
        return dfsWithSet(0, -1, graph, originalEdges, visited);
    }

    private int dfsWithSet(int city, int parent, List<List<Integer>> graph,
            Set<String> originalEdges, boolean[] visited) {
        visited[city] = true;
        int changes = 0;

        for (int neighbor : graph.get(city)) {
            if (neighbor != parent && !visited[neighbor]) {
                // Check if we're traversing against the original direction
                if (originalEdges.contains(city + "," + neighbor)) {
                    changes++; // Need to reverse this edge
                }
                changes += dfsWithSet(neighbor, city, graph, originalEdges, visited);
            }
        }

        return changes;
    }

    // Test the solution
    public static void main(String[] args) {
        Solution sol = new Solution();

        // Test case 1: n=6, connections=[[0,1],[1,3],[2,3],[4,0],[4,5]]
        int[][] connections1 = { { 0, 1 }, { 1, 3 }, { 2, 3 }, { 4, 0 }, { 4, 5 } };
        System.out.println("Test 1: " + sol.minReorder(6, connections1)); // Expected: 3

        // Test case 2: n=5, connections=[[1,0],[1,2],[3,2],[3,4]]
        int[][] connections2 = { { 1, 0 }, { 1, 2 }, { 3, 2 }, { 3, 4 } };
        System.out.println("Test 2: " + sol.minReorder(5, connections2)); // Expected: 2

        // Test case 3: n=3, connections=[[2,0],[1,2]]
        int[][] connections3 = { { 2, 0 }, { 1, 2 } };
        System.out.println("Test 3: " + sol.minReorder(3, connections3)); // Expected: 0
    }

}
