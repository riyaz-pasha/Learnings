import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Stack;

/**
 * Topological Sort using DFS (with Cycle Detection)
 *
 * Topological ordering exists ONLY if the graph is a DAG (Directed Acyclic Graph).
 *
 * DFS Logic:
 * - Do DFS for each node
 * - After visiting all neighbors, push the node into stack
 * - Final answer is reverse of finishing time order
 *
 * Cycle Detection:
 * - We maintain an extra array inPath[] (recursion stack)
 * - If we revisit a node that is currently inPath, we found a cycle.
 *
 * Time Complexity: O(V + E)
 * Space Complexity: O(V + E)
 */
class TopologicalSortDFS2 {

    public List<Integer> topoSort(int V, List<List<Integer>> adjList) {

        boolean[] visited = new boolean[V];

        // inPath[node] = true means node is currently in recursion stack
        boolean[] inPath = new boolean[V];

        // Using deque as stack (better than java.util.Stack)
        Deque<Integer> stack = new ArrayDeque<>();

        for (int node = 0; node < V; node++) {
            if (!visited[node]) {
                if (dfs(node, adjList, visited, inPath, stack)) {
                    throw new IllegalStateException("Cycle detected! Topological sort not possible.");
                }
            }
        }

        // stack already contains nodes in reverse order, pop to get topo order
        List<Integer> topoOrder = new ArrayList<>();
        while (!stack.isEmpty()) {
            topoOrder.add(stack.pop());
        }

        return topoOrder;
    }

    /**
     * Returns true if a cycle is detected.
     */
    private boolean dfs(int node,
                        List<List<Integer>> adjList,
                        boolean[] visited,
                        boolean[] inPath,
                        Deque<Integer> stack) {

        visited[node] = true;
        inPath[node] = true; // mark node as part of current DFS path

        for (int neighbor : adjList.get(node)) {

            // If neighbor not visited, DFS deeper
            if (!visited[neighbor]) {
                if (dfs(neighbor, adjList, visited, inPath, stack)) {
                    return true; // cycle found below
                }
            }

            // If neighbor is already in recursion path => cycle exists
            else if (inPath[neighbor]) {
                return true;
            }
        }

        // DFS finished for this node, remove from current path
        inPath[node] = false;

        // Push after processing all neighbors (postorder)
        stack.push(node);

        return false;
    }
}

/*
 * Why do we need inPath[]?
 *
 * visited[] only tells if a node was processed before.
 * But in directed graphs, visiting an already visited node does NOT always mean cycle.
 *
 * A cycle exists only when we reach a node that is still part of the CURRENT DFS recursion path.
 * That is why we track inPath[] (recursion stack).
 */


public class TopologicalSortDFS {

    public List<Integer> topoSort(int n, List<List<Integer>> adjList) {
        boolean[] visited = new boolean[n];
        Stack<Integer> stack = new Stack<>();

        // Call DFS for all unvisited nodes (in case of multiple components)
        for (int i = 0; i < n; i++) {
            if (!visited[i]) {
                dfs(i, adjList, visited, stack);
            }
        }

        // Reverse stack to get topological order
        List<Integer> result = new ArrayList<>();
        while (!stack.isEmpty()) {
            result.add(stack.pop());
        }
        return result;
    }

    private void dfs(int node, List<List<Integer>> adjList, boolean[] visited, Stack<Integer> stack) {
        visited[node] = true;

        for (int neighbor : adjList.get(node)) {
            if (!visited[neighbor]) {
                dfs(neighbor, adjList, visited, stack);
            }
        }

        // After visiting all neighbors, push node to stack
        stack.push(node);
    }

    // Example usage
    public static void main(String[] args) {
        int n = 6;
        List<List<Integer>> adjList = new ArrayList<>();
        for (int i = 0; i < n; i++)
            adjList.add(new ArrayList<>());

        // Example graph:
        // 5 → 0, 5 → 2
        // 4 → 0, 4 → 1
        // 2 → 3
        // 3 → 1
        adjList.get(5).add(0);
        adjList.get(5).add(2);
        adjList.get(4).add(0);
        adjList.get(4).add(1);
        adjList.get(2).add(3);
        adjList.get(3).add(1);

        TopologicalSortDFS sorter = new TopologicalSortDFS();
        List<Integer> result = sorter.topoSort(n, adjList);
        System.out.println("Topological Sort: " + result);
    }

}

/*
    DFS-Based Topological Sort Algorithm

    1. Create a visited array to track which nodes have been visited.
    2. Use a stack to store the topological sort in reverse order.
    3. For each unvisited node:
       a. Call DFS on the node.
       b. In DFS:
          i.   Mark the node as visited.
          ii.  Recursively visit all unvisited neighbors.
          iii. After all neighbors are visited, push the node onto the stack.
    4. After all nodes are processed:
       - Pop all elements from the stack to get the topological order.

    Note:
    - A node is pushed to the stack only after visiting all its dependencies.
    - This ensures the node appears after its dependencies in the final order.
*/

/*
    Time Complexity: O(V + E)
    - V = number of vertices
    - E = number of edges
    - Reason:
      -> Each node is visited exactly once: O(V)
      -> All edges are explored once via DFS: O(E)
      -> Total: O(V + E)

    Space Complexity: O(V + E)
    - visited array: O(V)
    - recursion stack depth (call stack): O(V) in the worst case
    - output stack: O(V)
    - adjacency list to store the graph: O(E)
    - Total: O(V + E)
*/
