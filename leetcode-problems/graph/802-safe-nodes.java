import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/*
 * There is a directed graph of n nodes with each node labeled from 0 to n - 1. 
 * The graph is represented by a 0-indexed 2D integer array graph 
 * where graph[i] is an integer array of nodes adjacent to node i, 
 * meaning there is an edge from node i to each node in graph[i].
 * 
 * A node is a terminal node if there are no outgoing edges.
 * A node is a safe node if every possible path starting from that node 
 * leads to a terminal node (or another safe node).
 * 
 * Return an array containing all the safe nodes of the graph. 
 * The answer should be sorted in ascending order.
 */

// Solution 1: DFS with Cycle Detection (Three-Color Approach)
// Time Complexity: O(V + E) where V = nodes, E = edges
// Space Complexity: O(V) for color array and recursion stack
class SafeStateNodes {
    /*
     * APPROACH: Three-Color DFS
     * 
     * Color States:
     * - WHITE (0): Unvisited node
     * - GRAY (1): Currently being processed (in recursion stack)
     * - BLACK (2): Completely processed and determined to be safe
     * 
     * Key Insight: A node is safe if:
     * 1. It's a terminal node (no outgoing edges), OR
     * 2. All its neighbors are safe nodes
     * 
     * A node is unsafe if it's part of a cycle or leads to a cycle
     */
    public List<Integer> eventualSafeNodes(int[][] graph) {
        int n = graph.length;
        int[] color = new int[n]; // 0=white, 1=gray, 2=black
        List<Integer> result = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            if (isSafe(graph, i, color)) {
                result.add(i);
            }
        }

        return result;
    }

    private boolean isSafe(int[][] graph, int node, int[] color) {
        /*
         * Returns true if the node is safe (all paths lead to terminal nodes)
         * 
         * Process:
         * 1. If node is GRAY (being processed), we found a cycle -> unsafe
         * 2. If node is BLACK (already processed), it's safe
         * 3. If node is WHITE (unvisited):
         * - Mark as GRAY (currently processing)
         * - Check all neighbors recursively
         * - If any neighbor is unsafe, this node is unsafe
         * - If all neighbors are safe, mark this node as BLACK (safe)
         */
        if (color[node] == 1)
            return false; // Gray = cycle detected
        if (color[node] == 2)
            return true; // Black = already safe

        color[node] = 1; // Mark as gray (being processed)

        // Check all neighbors
        for (int neighbor : graph[node]) {
            if (!isSafe(graph, neighbor, color)) {
                return false; // If any neighbor is unsafe, this node is unsafe
            }
        }

        color[node] = 2; // Mark as black (safe)
        return true;
    }

}

// Solution 2: Reverse Graph + Topological Sort (Kahn's Algorithm)
// Time Complexity: O(V + E)
// Space Complexity: O(V + E) for reverse graph
class SafeStateNodes2 {
    /*
     * APPROACH: Reverse Graph + Topological Sort
     * 
     * Key Insight: Instead of finding unsafe nodes, we find safe nodes by:
     * 1. Building reverse graph (edge u->v becomes v->u in reverse)
     * 2. Starting from terminal nodes (nodes with indegree 0 in reverse graph)
     * 3. Using BFS/topological sort to propagate safety backwards
     * 
     * A node is safe if we can reach it by going backwards from terminal nodes
     */
    public List<Integer> eventualSafeNodes(int[][] graph) {
        int n = graph.length;

        // Build reverse graph and calculate indegrees
        List<List<Integer>> reverseGraph = new ArrayList<>();
        int[] indegree = new int[n];

        for (int i = 0; i < n; i++) {
            reverseGraph.add(new ArrayList<>());
        }

        // Build reverse graph: if there's edge i->j, add j->i in reverse
        for (int i = 0; i < n; i++) {
            for (int j : graph[i]) {
                reverseGraph.get(j).add(i);
                indegree[i]++;
            }
        }

        // Start BFS from terminal nodes (indegree = 0)
        Queue<Integer> queue = new LinkedList<>();
        for (int i = 0; i < n; i++) {
            if (indegree[i] == 0) {
                queue.offer(i);
            }
        }

        List<Integer> safeNodes = new ArrayList<>();

        // Topological sort - process nodes with indegree 0
        while (!queue.isEmpty()) {
            int node = queue.poll();
            safeNodes.add(node);

            // Reduce indegree of neighbors in reverse graph
            for (int neighbor : reverseGraph.get(node)) {
                indegree[neighbor]--;
                if (indegree[neighbor] == 0) {
                    queue.offer(neighbor);
                }
            }
        }

        Collections.sort(safeNodes);
        return safeNodes;
    }

}

// Solution 3: DFS with Memoization (Cleaner Implementation)
// Time Complexity: O(V + E)
// Space Complexity: O(V)
class SafeStateNodes3 {
    /*
     * APPROACH: DFS with Memoization using Boolean Array
     * 
     * Uses a visited array and safe array to memoize results:
     * - visited[i] = true means we're currently exploring node i (detect cycles)
     * - safe[i] = true means node i is confirmed to be safe
     */
    public List<Integer> eventualSafeNodes(int[][] graph) {
        int n = graph.length;
        boolean[] visited = new boolean[n];
        boolean[] safe = new boolean[n];
        List<Integer> result = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            if (dfs(graph, i, visited, safe)) {
                result.add(i);
            }
        }

        return result;
    }

    private boolean dfs(int[][] graph, int node, boolean[] visited, boolean[] safe) {
        if (visited[node])
            return false; // Cycle detected
        if (safe[node])
            return true; // Already computed as safe

        visited[node] = true;

        for (int neighbor : graph[node]) {
            if (!dfs(graph, neighbor, visited, safe)) {
                visited[node] = false; // Backtrack
                return false;
            }
        }

        visited[node] = false; // Backtrack
        safe[node] = true; // Mark as safe
        return true;
    }

}
