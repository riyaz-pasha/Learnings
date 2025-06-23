import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

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
