import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
/*
 * 1101. The Earliest Moment When Everyone Become Friends**
 * 
 * You have **n** people in a social group labeled from `0` to `n - 1`. You are
 * given an array `logs`, where each `logs[i] = [timestampᵢ, xᵢ, yᵢ]` means that
 * at time `timestampᵢ`, person `xᵢ` and person `yᵢ` become friends.
 * 
 * Friendship is **symmetric**: if A is friends with B, then B is friends with
 * A.
 * A person A is **acquainted** with person B if:
 * 
 * they are direct friends, **or**
 * A is friends with someone who is acquainted (directly or indirectly) with B.
 * 
 * Your goal is to **return the earliest time** at which **every person** became
 * acquainted with **every other person** in the group. If there's **no such
 * time**, return `-1`.
 ** 
 * Example 1:**
 * ```
 * logs = [
 * [20190101, 0, 1],
 * [20190104, 3, 4],
 * [20190107, 2, 3],
 * [20190211, 1, 5],
 * [20190224, 2, 4],
 * [20190301, 0, 3],
 * [20190312, 1, 2],
 * [20190322, 4, 5]
 * ]
 * n = 6
 * Output: 20190301
 * ```
 * 
 * Explanation: Friend connections gradually merge the network, and by
 * `20190301`, all six people are in one connected group. ([leetcode.ca][1],
 * [algo.monster][2])
 ** 
 * Example 2:**
 * ```
 * logs = [
 * [0, 2, 0],
 * [1, 0, 1],
 * [3, 0, 3],
 * [4, 1, 2],
 * [7, 3, 1]
 * ]
 * n = 4
 * Output: 3
 * ```
 * At timestamp `3`, all four individuals (0, 1, 2, 3) have become acquainted.
 * ([leetcode.ca][1])
 *
 */

class Solution {

    public int earliestAcq(int[][] logs, int n) {
        // Sort logs by timestamp
        Arrays.sort(logs, (a, b) -> Integer.compare(a[0], b[0]));

        // Union-Find setup
        UnionFind uf = new UnionFind(n);

        // Process each log
        for (int[] log : logs) {
            int timestamp = log[0];
            int x = log[1];
            int y = log[2];

            // Union x and y
            if (uf.union(x, y)) {
                if (uf.getCount() == 1) {
                    return timestamp;
                }
            }
        }

        // Not all connected
        return -1;
    }

    // Union-Find (Disjoint Set Union) class
    class UnionFind {
        int[] parent;
        int[] rank;
        int count; // Number of connected components

        public UnionFind(int n) {
            parent = new int[n];
            rank = new int[n];
            count = n;
            for (int i = 0; i < n; i++)
                parent[i] = i;
        }

        public int find(int x) {
            if (parent[x] != x) {
                parent[x] = find(parent[x]); // Path compression
            }
            return parent[x];
        }

        public boolean union(int x, int y) {
            int px = find(x);
            int py = find(y);
            if (px == py)
                return false;

            // Union by rank
            if (rank[px] < rank[py]) {
                parent[px] = py;
            } else if (rank[px] > rank[py]) {
                parent[py] = px;
            } else {
                parent[py] = px;
                rank[px]++;
            }
            count--;
            return true;
        }

        public int getCount() {
            return count;
        }
    }

}

// Solution 1: Union-Find (Disjoint Set Union) - Most Efficient
class Solution2 {

    public int earliestAcq(int[][] logs, int n) {
        // Sort logs by timestamp to process friendships chronologically
        Arrays.sort(logs, (a, b) -> Integer.compare(a[0], b[0]));

        UnionFind uf = new UnionFind(n);

        for (int[] log : logs) {
            int timestamp = log[0];
            int person1 = log[1];
            int person2 = log[2];

            uf.union(person1, person2);

            // Check if everyone is connected (only 1 component left)
            if (uf.getComponentCount() == 1) {
                return timestamp;
            }
        }

        return -1; // Not everyone becomes friends
    }

    class UnionFind {

        private int[] parent;
        private int[] rank;
        private int componentCount;

        public UnionFind(int n) {
            parent = new int[n];
            rank = new int[n];
            componentCount = n;

            for (int i = 0; i < n; i++) {
                parent[i] = i;
                rank[i] = 0;
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

            componentCount--;
            return true;
        }

        public int getComponentCount() {
            return componentCount;
        }

    }

}

// Solution 2: Simple Union-Find without rank optimization
class SolutionSimple {

    public int earliestAcq(int[][] logs, int n) {
        Arrays.sort(logs, (a, b) -> a[0] - b[0]);

        int[] parent = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
        }

        int components = n;

        for (int[] log : logs) {
            int time = log[0], x = log[1], y = log[2];

            if (union(parent, x, y)) {
                components--;
                if (components == 1) {
                    return time;
                }
            }
        }

        return -1;
    }

    private int find(int[] parent, int x) {
        if (parent[x] != x) {
            parent[x] = find(parent, parent[x]);
        }
        return parent[x];
    }

    private boolean union(int[] parent, int x, int y) {
        int rootX = find(parent, x);
        int rootY = find(parent, y);

        if (rootX == rootY)
            return false;

        parent[rootX] = rootY;
        return true;
    }

}

// Solution 3: DFS approach (less efficient but educational)
class SolutionDFS {

    public int earliestAcq(int[][] logs, int n) {
        Arrays.sort(logs, (a, b) -> a[0] - b[0]);

        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }

        for (int[] log : logs) {
            int time = log[0], x = log[1], y = log[2];

            // Add edge
            graph.get(x).add(y);
            graph.get(y).add(x);

            // Check if all nodes are connected
            if (isAllConnected(graph, n)) {
                return time;
            }
        }

        return -1;
    }

    private boolean isAllConnected(List<List<Integer>> graph, int n) {
        boolean[] visited = new boolean[n];
        dfs(graph, 0, visited);

        for (boolean v : visited) {
            if (!v)
                return false;
        }
        return true;
    }

    private void dfs(List<List<Integer>> graph, int node, boolean[] visited) {
        visited[node] = true;
        for (int neighbor : graph.get(node)) {
            if (!visited[neighbor]) {
                dfs(graph, neighbor, visited);
            }
        }
    }

}

// Solution 4: BFS approach
class SolutionBFS {

    public int earliestAcq(int[][] logs, int n) {
        Arrays.sort(logs, (a, b) -> a[0] - b[0]);

        Map<Integer, List<Integer>> graph = new HashMap<>();
        for (int i = 0; i < n; i++) {
            graph.put(i, new ArrayList<>());
        }

        for (int[] log : logs) {
            int time = log[0], x = log[1], y = log[2];

            graph.get(x).add(y);
            graph.get(y).add(x);

            if (isConnected(graph, n)) {
                return time;
            }
        }

        return -1;
    }

    private boolean isConnected(Map<Integer, List<Integer>> graph, int n) {
        Queue<Integer> queue = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();

        queue.offer(0);
        visited.add(0);

        while (!queue.isEmpty()) {
            int node = queue.poll();
            for (int neighbor : graph.get(node)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.offer(neighbor);
                }
            }
        }

        return visited.size() == n;
    }

}

// Test class
class TestEarliestMoment {

    public static void main(String[] args) {
        Solution solution = new Solution();

        // Test case 1
        int[][] logs1 = { { 20190101, 0, 1 }, { 20190104, 3, 4 }, { 20190107, 2, 3 }, { 20190211, 1, 5 },
                { 20190224, 2, 4 }, { 20190301, 0, 3 }, { 20190312, 1, 2 }, { 20190322, 4, 5 } };
        int n1 = 6;
        System.out.println("Test 1: " + solution.earliestAcq(logs1, n1)); // Expected: 20190301

        // Test case 2
        int[][] logs2 = { { 0, 2, 0 }, { 1, 0, 1 }, { 3, 0, 3 }, { 4, 1, 2 }, { 7, 3, 1 } };
        int n2 = 4;
        System.out.println("Test 2: " + solution.earliestAcq(logs2, n2)); // Expected: 3

        // Test case 3: Impossible case
        int[][] logs3 = { { 0, 0, 1 }, { 1, 0, 2 } };
        int n3 = 4;
        System.out.println("Test 3: " + solution.earliestAcq(logs3, n3)); // Expected: -1

        // Performance comparison
        long start = System.nanoTime();
        solution.earliestAcq(logs1, n1);
        long unionFindTime = System.nanoTime() - start;

        SolutionDFS dfs = new SolutionDFS();
        start = System.nanoTime();
        dfs.earliestAcq(logs1, n1);
        long dfsTime = System.nanoTime() - start;

        System.out.println("Union-Find time: " + unionFindTime + "ns");
        System.out.println("DFS time: " + dfsTime + "ns");
        System.out.println("Union-Find is " + (dfsTime / Math.max(unionFindTime, 1)) + "x faster");
    }

}
