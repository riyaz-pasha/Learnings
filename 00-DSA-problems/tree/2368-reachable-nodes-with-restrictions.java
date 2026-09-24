import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
/*
 * There is an undirected tree with n nodes labeled from 0 to n - 1 and n - 1
 * edges.
 * 
 * You are given a 2D integer array edges of length n - 1 where edges[i] = [ai,
 * bi] indicates that there is an edge between nodes ai and bi in the tree. You
 * are also given an integer array restricted which represents restricted nodes.
 * 
 * Return the maximum number of nodes you can reach from node 0 without visiting
 * a restricted node.
 * 
 * Note that node 0 will not be a restricted node.
 * 
 * Example 1:
 * Input: n = 7, edges = [[0,1],[1,2],[3,1],[4,0],[0,5],[5,6]], restricted =
 * [4,5]
 * Output: 4
 * Explanation: The diagram above shows the tree.
 * We have that [0,1,2,3] are the only nodes that can be reached from node 0
 * without visiting a restricted node.
 * 
 * Example 2:
 * Input: n = 7, edges = [[0,1],[0,2],[0,5],[0,4],[3,2],[6,5]], restricted =
 * [4,2,1]
 * Output: 3
 * Explanation: The diagram above shows the tree.
 * We have that [0,5,6] are the only nodes that can be reached from node 0
 * without visiting a restricted node.
 */

class Solution {

    public int reachableNodes(int n, int[][] edges, int[] restricted) {
        // Time Complexity: O(n) where n is the number of nodes
        // Space Complexity: O(n) for adjacency list and visited set

        // Build adjacency list representation of the tree
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }

        for (int[] edge : edges) {
            graph.get(edge[0]).add(edge[1]);
            graph.get(edge[1]).add(edge[0]);
        }

        // Convert restricted array to set for O(1) lookup
        Set<Integer> restrictedSet = new HashSet<>();
        for (int node : restricted) {
            restrictedSet.add(node);
        }

        // Perform BFS/DFS from node 0, avoiding restricted nodes
        return bfsCount(graph, restrictedSet);
    }

    private int bfsCount(List<List<Integer>> graph, Set<Integer> restricted) {
        Queue<Integer> queue = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();

        queue.offer(0);
        visited.add(0);
        int count = 0;

        while (!queue.isEmpty()) {
            int current = queue.poll();
            count++;

            // Visit all neighbors
            for (int neighbor : graph.get(current)) {
                // Skip if already visited or restricted
                if (visited.contains(neighbor) || restricted.contains(neighbor)) {
                    continue;
                }

                visited.add(neighbor);
                queue.offer(neighbor);
            }
        }

        return count;
    }

}

// Alternative DFS solution
class SolutionDFS {

    public int reachableNodes(int n, int[][] edges, int[] restricted) {
        // Time Complexity: O(n) where n is the number of nodes
        // Space Complexity: O(n) for adjacency list, visited set, and recursion stack

        // Build adjacency list
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }

        for (int[] edge : edges) {
            graph.get(edge[0]).add(edge[1]);
            graph.get(edge[1]).add(edge[0]);
        }

        // Convert restricted to set
        Set<Integer> restrictedSet = new HashSet<>();
        for (int node : restricted) {
            restrictedSet.add(node);
        }

        // DFS from node 0
        Set<Integer> visited = new HashSet<>();
        return dfs(0, graph, restrictedSet, visited);
    }

    private int dfs(int node, List<List<Integer>> graph, Set<Integer> restricted, Set<Integer> visited) {
        visited.add(node);
        int count = 1; // Count current node

        for (int neighbor : graph.get(node)) {
            if (!visited.contains(neighbor) && !restricted.contains(neighbor)) {
                count += dfs(neighbor, graph, restricted, visited);
            }
        }

        return count;
    }

}

// Optimized solution using boolean array instead of HashSet
class SolutionOptimized {

    public int reachableNodes(int n, int[][] edges, int[] restricted) {
        // Time Complexity: O(n) where n is the number of nodes
        // Space Complexity: O(n) for adjacency list and boolean arrays

        // Build adjacency list
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }

        for (int[] edge : edges) {
            graph.get(edge[0]).add(edge[1]);
            graph.get(edge[1]).add(edge[0]);
        }

        // Mark restricted nodes
        boolean[] isRestricted = new boolean[n];
        for (int node : restricted) {
            isRestricted[node] = true;
        }

        // BFS with boolean array for visited tracking
        boolean[] visited = new boolean[n];
        Queue<Integer> queue = new LinkedList<>();

        queue.offer(0);
        visited[0] = true;
        int count = 0;

        while (!queue.isEmpty()) {
            int current = queue.poll();
            count++;

            for (int neighbor : graph.get(current)) {
                if (!visited[neighbor] && !isRestricted[neighbor]) {
                    visited[neighbor] = true;
                    queue.offer(neighbor);
                }
            }
        }

        return count;
    }

}

// Solution using Union-Find (Disjoint Set Union)
class SolutionUnionFind {

    public int reachableNodes(int n, int[][] edges, int[] restricted) {
        // Time Complexity: O(n * α(n)) where α is inverse Ackermann function
        // Space Complexity: O(n) for parent and size arrays

        // Mark restricted nodes
        boolean[] isRestricted = new boolean[n];
        for (int node : restricted) {
            isRestricted[node] = true;
        }

        UnionFind uf = new UnionFind(n);

        // Union non-restricted connected nodes
        for (int[] edge : edges) {
            int u = edge[0], v = edge[1];
            if (!isRestricted[u] && !isRestricted[v]) {
                uf.union(u, v);
            }
        }

        // Return size of component containing node 0
        return uf.getSize(0);
    }

    class UnionFind {
        private int[] parent;
        private int[] size;

        public UnionFind(int n) {
            parent = new int[n];
            size = new int[n];
            for (int i = 0; i < n; i++) {
                parent[i] = i;
                size[i] = 1;
            }
        }

        public int find(int x) {
            if (parent[x] != x) {
                parent[x] = find(parent[x]); // Path compression
            }
            return parent[x];
        }

        public void union(int x, int y) {
            int rootX = find(x);
            int rootY = find(y);

            if (rootX != rootY) {
                // Union by size
                if (size[rootX] < size[rootY]) {
                    parent[rootX] = rootY;
                    size[rootY] += size[rootX];
                } else {
                    parent[rootY] = rootX;
                    size[rootX] += size[rootY];
                }
            }
        }

        public int getSize(int x) {
            return size[find(x)];
        }
    }

}

// Test cases
class TestCases {

    public static void main(String[] args) {
        Solution solution = new Solution();
        SolutionDFS solutionDFS = new SolutionDFS();
        SolutionOptimized solutionOpt = new SolutionOptimized();
        SolutionUnionFind solutionUF = new SolutionUnionFind();

        // Test Case 1: n = 7, edges = [[0,1],[1,2],[3,1],[4,0],[0,5],[5,6]], restricted
        // = [4,5]
        // Expected: 4 (nodes [0,1,2,3])
        int n1 = 7;
        int[][] edges1 = { { 0, 1 }, { 1, 2 }, { 3, 1 }, { 4, 0 }, { 0, 5 }, { 5, 6 } };
        int[] restricted1 = { 4, 5 };

        System.out.println("Test Case 1:");
        System.out.println("n = " + n1);
        System.out.println("edges = " + Arrays.deepToString(edges1));
        System.out.println("restricted = " + Arrays.toString(restricted1));
        System.out.println("BFS Solution: " + solution.reachableNodes(n1, edges1, restricted1));
        System.out.println("DFS Solution: " + solutionDFS.reachableNodes(n1, edges1, restricted1));
        System.out.println("Optimized Solution: " + solutionOpt.reachableNodes(n1, edges1, restricted1));
        System.out.println("Union-Find Solution: " + solutionUF.reachableNodes(n1, edges1, restricted1));
        System.out.println();

        // Test Case 2: n = 7, edges = [[0,1],[0,2],[0,5],[0,4],[3,2],[6,5]], restricted
        // = [4,2,1]
        // Expected: 3 (nodes [0,5,6])
        int n2 = 7;
        int[][] edges2 = { { 0, 1 }, { 0, 2 }, { 0, 5 }, { 0, 4 }, { 3, 2 }, { 6, 5 } };
        int[] restricted2 = { 4, 2, 1 };

        System.out.println("Test Case 2:");
        System.out.println("n = " + n2);
        System.out.println("edges = " + Arrays.deepToString(edges2));
        System.out.println("restricted = " + Arrays.toString(restricted2));
        System.out.println("BFS Solution: " + solution.reachableNodes(n2, edges2, restricted2));
        System.out.println("DFS Solution: " + solutionDFS.reachableNodes(n2, edges2, restricted2));
        System.out.println("Optimized Solution: " + solutionOpt.reachableNodes(n2, edges2, restricted2));
        System.out.println("Union-Find Solution: " + solutionUF.reachableNodes(n2, edges2, restricted2));
        System.out.println();

        // Test Case 3: Simple linear tree with no restrictions
        int n3 = 5;
        int[][] edges3 = { { 0, 1 }, { 1, 2 }, { 2, 3 }, { 3, 4 } };
        int[] restricted3 = {};

        System.out.println("Test Case 3 (No restrictions):");
        System.out.println("n = " + n3);
        System.out.println("edges = " + Arrays.deepToString(edges3));
        System.out.println("restricted = " + Arrays.toString(restricted3));
        System.out.println("BFS Solution: " + solution.reachableNodes(n3, edges3, restricted3));
        System.out.println("DFS Solution: " + solutionDFS.reachableNodes(n3, edges3, restricted3));
        System.out.println("Optimized Solution: " + solutionOpt.reachableNodes(n3, edges3, restricted3));
        System.out.println("Union-Find Solution: " + solutionUF.reachableNodes(n3, edges3, restricted3));
        System.out.println();

        // Test Case 4: Single node
        int n4 = 1;
        int[][] edges4 = {};
        int[] restricted4 = {};

        System.out.println("Test Case 4 (Single node):");
        System.out.println("n = " + n4);
        System.out.println("BFS Solution: " + solution.reachableNodes(n4, edges4, restricted4));
        System.out.println("DFS Solution: " + solutionDFS.reachableNodes(n4, edges4, restricted4));
        System.out.println("Optimized Solution: " + solutionOpt.reachableNodes(n4, edges4, restricted4));
        System.out.println("Union-Find Solution: " + solutionUF.reachableNodes(n4, edges4, restricted4));
    }

}
