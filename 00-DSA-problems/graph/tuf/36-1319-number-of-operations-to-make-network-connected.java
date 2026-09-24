
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/*
 * There are n computers numbered from 0 to n - 1 connected by ethernet cables
 * connections forming a network where connections[i] = [ai, bi] represents a
 * connection between computers ai and bi. Any computer can reach any other
 * computer directly or indirectly through the network.
 * 
 * You are given an initial computer network connections. You can extract
 * certain cables between two directly connected computers, and place them
 * between any pair of disconnected computers to make them directly connected.
 * 
 * Return the minimum number of times you need to do this in order to make all
 * the computers connected. If it is not possible, return -1.
 * 
 * Example 1:
 * Input: n = 4, connections = [[0,1],[0,2],[1,2]]
 * Output: 1
 * Explanation: Remove cable between computer 1 and 2 and place between
 * computers 1 and 3.
 * 
 * Example 2:
 * Input: n = 6, connections = [[0,1],[0,2],[0,3],[1,2],[1,3]]
 * Output: 2
 * 
 * Example 3:
 * Input: n = 6, connections = [[0,1],[0,2],[0,3],[1,2]]
 * Output: -1
 * Explanation: There are not enough cables.
 */

class NumberOfOperationsToMakeNetworkConnected {

    public int makeConnected(int n, int[][] connections) {
        // If there are fewer than n-1 edges, impossible to connect all nodes.
        if (connections.length < n - 1)
            return -1;

        DisjointSet ds = new DisjointSet(n);

        for (int[] conn : connections) {
            ds.union(conn[0], conn[1]);
        }

        // number of operations required = number of connected components - 1
        return ds.count() - 1;
    }

    class DisjointSet {
        private int[] parent;
        private int[] size;
        private int count;

        public DisjointSet(int n) {
            parent = new int[n];
            size = new int[n];
            count = n;
            for (int i = 0; i < n; i++) {
                parent[i] = i;
                size[i] = 1; // <-- fixed (was size[i] = i)
            }
        }

        public int find(int x) {
            if (parent[x] != x) {
                parent[x] = find(parent[x]);
            }
            return parent[x];
        }

        public boolean union(int x, int y) {
            int rootX = find(x);
            int rootY = find(y);
            if (rootX == rootY)
                return false;

            if (size[rootX] < size[rootY]) {
                parent[rootX] = rootY;
                size[rootY] += size[rootX];
            } else {
                parent[rootY] = rootX;
                size[rootX] += size[rootY];
            }
            count--;
            return true;
        }

        public int count() {
            return count;
        }
    }

}

class Solution {

    // APPROACH 1: Union-Find (Disjoint Set Union)
    // Time Complexity: O(E * α(N)) where E = edges, α = inverse Ackermann (nearly
    // constant)
    // Space Complexity: O(N) for parent and rank arrays
    public int makeConnected(int n, int[][] connections) {
        // Need at least n-1 cables to connect n computers
        if (connections.length < n - 1) {
            return -1;
        }

        UnionFind uf = new UnionFind(n);

        // Count redundant cables (cables in same component)
        int redundant = 0;
        for (int[] conn : connections) {
            if (!uf.union(conn[0], conn[1])) {
                redundant++;
            }
        }

        // Number of components - 1 = cables needed
        int components = uf.getComponentCount();
        int cablesNeeded = components - 1;

        return cablesNeeded;
    }

    class UnionFind {
        private int[] parent;
        private int[] rank;
        private int components;

        public UnionFind(int n) {
            parent = new int[n];
            rank = new int[n];
            components = n;
            for (int i = 0; i < n; i++) {
                parent[i] = i;
            }
        }

        public int find(int x) {
            if (parent[x] != x) {
                parent[x] = find(parent[x]); // Path compression
            }
            return parent[x];
        }

        public boolean union(int x, int y) {
            int rootX = find(x);
            int rootY = find(y);

            if (rootX == rootY) {
                return false; // Already connected
            }

            // Union by rank
            if (rank[rootX] < rank[rootY]) {
                parent[rootX] = rootY;
            } else if (rank[rootX] > rank[rootY]) {
                parent[rootY] = rootX;
            } else {
                parent[rootY] = rootX;
                rank[rootX]++;
            }

            components--;
            return true;
        }

        public int getComponentCount() {
            return components;
        }
    }

    // APPROACH 2: DFS (Depth-First Search)
    // Time Complexity: O(N + E) where N = nodes, E = edges
    // Space Complexity: O(N + E) for graph and visited array
    public int makeConnectedDFS(int n, int[][] connections) {
        // Need at least n-1 cables to connect n computers
        if (connections.length < n - 1) {
            return -1;
        }

        // Build adjacency list
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }

        for (int[] conn : connections) {
            graph.get(conn[0]).add(conn[1]);
            graph.get(conn[1]).add(conn[0]);
        }

        // Count connected components using DFS
        boolean[] visited = new boolean[n];
        int components = 0;

        for (int i = 0; i < n; i++) {
            if (!visited[i]) {
                dfs(i, graph, visited);
                components++;
            }
        }

        // Need (components - 1) cables to connect all components
        return components - 1;
    }

    private void dfs(int node, List<List<Integer>> graph, boolean[] visited) {
        visited[node] = true;
        for (int neighbor : graph.get(node)) {
            if (!visited[neighbor]) {
                dfs(neighbor, graph, visited);
            }
        }
    }

    // APPROACH 3: BFS (Breadth-First Search)
    // Time Complexity: O(N + E)
    // Space Complexity: O(N + E) for graph, visited array, and queue
    public int makeConnectedBFS(int n, int[][] connections) {
        if (connections.length < n - 1) {
            return -1;
        }

        // Build adjacency list
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }

        for (int[] conn : connections) {
            graph.get(conn[0]).add(conn[1]);
            graph.get(conn[1]).add(conn[0]);
        }

        // Count connected components using BFS
        boolean[] visited = new boolean[n];
        int components = 0;

        for (int i = 0; i < n; i++) {
            if (!visited[i]) {
                bfs(i, graph, visited);
                components++;
            }
        }

        return components - 1;
    }

    private void bfs(int start, List<List<Integer>> graph, boolean[] visited) {
        Queue<Integer> queue = new LinkedList<>();
        queue.offer(start);
        visited[start] = true;

        while (!queue.isEmpty()) {
            int node = queue.poll();
            for (int neighbor : graph.get(node)) {
                if (!visited[neighbor]) {
                    visited[neighbor] = true;
                    queue.offer(neighbor);
                }
            }
        }
    }
}

// Test class
class NetworkConnectionTest {
    public static void main(String[] args) {
        Solution solution = new Solution();

        // Test Case 1
        int n1 = 4;
        int[][] connections1 = { { 0, 1 }, { 0, 2 }, { 1, 2 } };
        System.out.println("Test 1 (Union-Find): " + solution.makeConnected(n1, connections1)); // Expected: 1
        System.out.println("Test 1 (DFS): " + solution.makeConnectedDFS(n1, connections1)); // Expected: 1
        System.out.println("Test 1 (BFS): " + solution.makeConnectedBFS(n1, connections1)); // Expected: 1

        // Test Case 2
        int n2 = 6;
        int[][] connections2 = { { 0, 1 }, { 0, 2 }, { 0, 3 }, { 1, 2 }, { 1, 3 } };
        System.out.println("\nTest 2 (Union-Find): " + solution.makeConnected(n2, connections2)); // Expected: 2
        System.out.println("Test 2 (DFS): " + solution.makeConnectedDFS(n2, connections2)); // Expected: 2
        System.out.println("Test 2 (BFS): " + solution.makeConnectedBFS(n2, connections2)); // Expected: 2

        // Test Case 3
        int n3 = 6;
        int[][] connections3 = { { 0, 1 }, { 0, 2 }, { 0, 3 }, { 1, 2 } };
        System.out.println("\nTest 3 (Union-Find): " + solution.makeConnected(n3, connections3)); // Expected: -1
        System.out.println("Test 3 (DFS): " + solution.makeConnectedDFS(n3, connections3)); // Expected: -1
        System.out.println("Test 3 (BFS): " + solution.makeConnectedBFS(n3, connections3)); // Expected: -1
    }
}

/*
 * COMPLEXITY ANALYSIS SUMMARY:
 * 
 * 1. Union-Find Approach (RECOMMENDED):
 * - Time: O(E * α(N)) ≈ O(E) - Nearly linear due to path compression
 * - Space: O(N)
 * - Best for: Most efficient for this problem, handles online queries well
 * 
 * 2. DFS Approach:
 * - Time: O(N + E)
 * - Space: O(N + E) - Graph storage + recursion stack
 * - Best for: When graph is already built, simple implementation
 * 
 * 3. BFS Approach:
 * - Time: O(N + E)
 * - Space: O(N + E) - Graph storage + queue
 * - Best for: Iterative approach preferred over recursion
 * 
 * KEY INSIGHTS:
 * - Minimum cables needed to connect n computers: n-1
 * - If connections.length < n-1, impossible to connect all
 * - Answer = Number of disconnected components - 1
 * - Redundant cables can be reused to connect components
 */
