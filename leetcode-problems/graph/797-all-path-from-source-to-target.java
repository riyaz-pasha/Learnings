import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;

/* Given a directed acyclic graph (DAG) of n nodes labeled from 0 to n - 1, 
 * find all possible paths from node 0 to node n - 1 and return them in any order.
 * The graph is given as follows: graph[i] is a list of all nodes you can visit
 * from node i (i.e., there is a directed edge from node i to node graph[i][j]). 
 */



/*
 ============================================================
 LeetCode 797: All Paths From Source to Target
 ============================================================

 Problem Summary:
 ----------------
 - Given a DAG (Directed Acyclic Graph)
 - Nodes are labeled from 0 to n - 1
 - graph[i] contains nodes reachable from i
 - Find ALL possible paths from node 0 to node n - 1

 Graph Properties (IMPORTANT):
 -----------------------------
 - DAG (no cycles)
 - n <= 15
 - Small constraints → exponential number of paths is acceptable

 ============================================================
 Core Approach:
 ============================================================

 We use DFS + Backtracking.

 Why DFS?
 --------
 - We must enumerate ALL paths (not shortest, not minimum, ALL)
 - DFS naturally explores one path fully before backtracking
 - DAG guarantees no cycles → no visited[] needed

 Algorithm:
 ----------
 1. Start DFS from node 0
 2. Maintain a list `path` representing the current path
 3. Add current node to path
 4. If current node == target (n - 1):
      - Add a COPY of path to result
 5. Else:
      - DFS into each neighbor
 6. Backtrack by removing last node

 ============================================================
 Time Complexity Analysis (VERY IMPORTANT):
 ============================================================

 Let:
 - P = number of valid paths from source to target
 - L = average length of each path (<= n)

 Time Complexity:
 ----------------
 O(P * L)

 Explanation:
 - Every valid path must be fully constructed and copied
 - Copying each path takes O(L)
 - In the worst case (complete DAG), P can be exponential

 This is OPTIMAL because:
 - We are REQUIRED to output all P paths
 - You cannot do better than output size

 ============================================================
 Space Complexity Analysis:
 ============================================================

 1. Recursion stack:
    - Max depth = path length ≤ n
    - O(n)

 2. Path list:
    - Stores current path → O(n)

 3. Result storage:
    - Stores all paths → O(P * L)
    - This is unavoidable (output requirement)

 Overall Space Complexity:
 -------------------------
 O(P * L)   (dominated by output size)

 ============================================================
 */

class AllPathsFromSourceToTarget {

    public List<List<Integer>> allPathsSourceTarget(int[][] graph) {
        List<List<Integer>> result = new ArrayList<>();
        List<Integer> path = new ArrayList<>();

        // Start DFS from source node (0)
        dfs(0, graph, path, result);

        return result;
    }

    /*
     ------------------------------------------------------------
     DFS Helper Function
     ------------------------------------------------------------

     Parameters:
     - node   : current node in DFS
     - graph  : adjacency list representation
     - path   : current path from source to this node
     - result : list of all valid paths

     Why no visited[]?
     -----------------
     The graph is a DAG → no cycles → safe to revisit nodes
     across different paths.
     ------------------------------------------------------------
     */
    private void dfs(int node,
                            int[][] graph,
                            List<Integer> path,
                            List<List<Integer>> result) {

        // Add current node to path
        path.add(node);

        // Base case: reached target node
        if (node == graph.length - 1) {
            // Must add a COPY of path
            result.add(new ArrayList<>(path));
        } else {
            // Explore all neighbors
            for (int next : graph[node]) {
                dfs(next, graph, path, result);
            }
        }

        // Backtrack: remove current node before returning
        path.remove(path.size() - 1);
    }

}


class DAGAllPaths {

    // Solution 1: DFS with Backtracking (Most Common)
    public List<List<Integer>> allPathsSourceTarget1(int[][] graph) {
        List<List<Integer>> result = new ArrayList<>();
        List<Integer> path = new ArrayList<>();
        path.add(0); // Start from node 0

        dfs(graph, 0, graph.length - 1, path, result);
        return result;
    }

    private void dfs(int[][] graph, int current, int target,
            List<Integer> path, List<List<Integer>> result) {
        if (current == target) {
            result.add(new ArrayList<>(path)); // Add copy of current path
            return;
        }

        for (int neighbor : graph[current]) {
            path.add(neighbor);
            dfs(graph, neighbor, target, path, result);
            path.remove(path.size() - 1); // Backtrack
        }
    }

    // Solution 2: BFS with Path Tracking
    public List<List<Integer>> allPathsSourceTarget2(int[][] graph) {
        List<List<Integer>> result = new ArrayList<>();
        Queue<List<Integer>> queue = new LinkedList<>();

        List<Integer> initialPath = new ArrayList<>();
        initialPath.add(0);
        queue.offer(initialPath);

        int target = graph.length - 1;

        while (!queue.isEmpty()) {
            List<Integer> currentPath = queue.poll();
            int currentNode = currentPath.get(currentPath.size() - 1);

            if (currentNode == target) {
                result.add(currentPath);
                continue;
            }

            for (int neighbor : graph[currentNode]) {
                List<Integer> newPath = new ArrayList<>(currentPath);
                newPath.add(neighbor);
                queue.offer(newPath);
            }
        }

        return result;
    }

    // Solution 3: Iterative DFS with Stack
    public List<List<Integer>> allPathsSourceTarget3(int[][] graph) {
        List<List<Integer>> result = new ArrayList<>();
        Stack<List<Integer>> stack = new Stack<>();

        List<Integer> initialPath = new ArrayList<>();
        initialPath.add(0);
        stack.push(initialPath);

        int target = graph.length - 1;

        while (!stack.isEmpty()) {
            List<Integer> currentPath = stack.pop();
            int currentNode = currentPath.get(currentPath.size() - 1);

            if (currentNode == target) {
                result.add(currentPath);
                continue;
            }

            for (int neighbor : graph[currentNode]) {
                List<Integer> newPath = new ArrayList<>(currentPath);
                newPath.add(neighbor);
                stack.push(newPath);
            }
        }

        return result;
    }

    // Solution 4: Memoized DFS (Most Efficient for overlapping subproblems)
    public List<List<Integer>> allPathsSourceTarget4(int[][] graph) {
        Map<Integer, List<List<Integer>>> memo = new HashMap<>();
        return dfsWithMemo(graph, 0, graph.length - 1, memo);
    }

    private List<List<Integer>> dfsWithMemo(int[][] graph, int current, int target,
            Map<Integer, List<List<Integer>>> memo) {
        if (memo.containsKey(current)) {
            return memo.get(current);
        }

        List<List<Integer>> paths = new ArrayList<>();

        if (current == target) {
            List<Integer> path = new ArrayList<>();
            path.add(target);
            paths.add(path);
        } else {
            for (int neighbor : graph[current]) {
                List<List<Integer>> subPaths = dfsWithMemo(graph, neighbor, target, memo);
                for (List<Integer> subPath : subPaths) {
                    List<Integer> newPath = new ArrayList<>();
                    newPath.add(current);
                    newPath.addAll(subPath);
                    paths.add(newPath);
                }
            }
        }

        memo.put(current, paths);
        return paths;
    }

    // Test method
    public static void main(String[] args) {
        DAGAllPaths solution = new DAGAllPaths();

        // Test case 1: [[1,2],[3],[3],[]]
        int[][] graph1 = { { 1, 2 }, { 3 }, { 3 }, {} };
        System.out.println("Test 1 - DFS Backtrack: " + solution.allPathsSourceTarget1(graph1));
        System.out.println("Test 1 - BFS: " + solution.allPathsSourceTarget2(graph1));
        System.out.println("Test 1 - Iterative DFS: " + solution.allPathsSourceTarget3(graph1));
        System.out.println("Test 1 - Memoized DFS: " + solution.allPathsSourceTarget4(graph1));

        // Test case 2: [[4,3,1],[3,2,4],[3],[4],[]]
        int[][] graph2 = { { 4, 3, 1 }, { 3, 2, 4 }, { 3 }, { 4 }, {} };
        System.out.println("\nTest 2 - DFS Backtrack: " + solution.allPathsSourceTarget1(graph2));
        System.out.println("Test 2 - BFS: " + solution.allPathsSourceTarget2(graph2));
        System.out.println("Test 2 - Iterative DFS: " + solution.allPathsSourceTarget3(graph2));
        System.out.println("Test 2 - Memoized DFS: " + solution.allPathsSourceTarget4(graph2));
    }
}

/*
 * Time & Space Complexity Analysis:
 * 
 * 1. DFS with Backtracking:
 * - Time: O(2^N * N) in worst case, where N is number of nodes
 * - Space: O(N) for recursion stack + O(2^N * N) for storing all paths
 * - Most intuitive and commonly used approach
 * 
 * 2. BFS with Path Tracking:
 * - Time: O(2^N * N)
 * - Space: O(2^N * N) for queue and paths
 * - Good for understanding all paths level by level
 * 
 * 3. Iterative DFS with Stack:
 * - Time: O(2^N * N)
 * - Space: O(2^N * N) for stack and paths
 * - Avoids recursion stack overflow for deep graphs
 * 
 * 4. Memoized DFS:
 * - Time: O(2^N * N) in worst case, but much better with overlapping
 * subproblems
 * - Space: O(2^N * N) for memoization + paths
 * - Most efficient when there are many overlapping subproblems
 * 
 * Key Points:
 * - All solutions work because the graph is a DAG (no cycles)
 * - The exponential time complexity is unavoidable since we need to find ALL
 * paths
 * - Choose based on your specific needs:
 * Solution 1 for interviews (most standard)
 * Solution 2 for level-by-level exploration
 * Solution 3 to avoid recursion
 * Solution 4 for graphs with many overlapping subproblems
 */
