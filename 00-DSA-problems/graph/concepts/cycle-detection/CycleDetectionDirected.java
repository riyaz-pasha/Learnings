import java.util.List;

public class CycleDetectionDirected {

    /**
     * Detect cycle in a Directed Graph using DFS.
     *
     * Key Idea:
     * - visited[node] -> node has been completely processed (DFS done)
     * - inPath[node] -> node is currently in recursion stack (DFS path)
     *
     * If during DFS we visit a node that is already in the current recursion path,
     * it means we found a BACK EDGE => cycle exists.
     *
     * Time Complexity: O(V + E)
     * Space Complexity: O(V) for visited + O(V) for inPath + O(V) recursion stack
     */
    public static boolean hasCycleDirectedGraph(List<List<Integer>> graph) {

        int V = graph.size();

        boolean[] visited = new boolean[V];
        boolean[] inPath = new boolean[V]; // recursion stack marker

        // Graph can have multiple components, so run DFS from every node
        for (int node = 0; node < V; node++) {
            if (!visited[node]) {
                if (dfsCycleCheck(node, graph, visited, inPath)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * DFS helper function to detect cycle.
     *
     * Returns true if a cycle is found starting from this node.
     */
    private static boolean dfsCycleCheck(int node,
            List<List<Integer>> graph,
            boolean[] visited,
            boolean[] inPath) {

        // Mark node as visited (we started exploring it)
        visited[node] = true;

        // Mark node as part of current recursion path
        inPath[node] = true;

        // Explore all outgoing edges node -> neighbor
        for (int neighbor : graph.get(node)) {

            // Case 1: If neighbor is not visited, explore it
            if (!visited[neighbor]) {
                if (dfsCycleCheck(neighbor, graph, visited, inPath)) {
                    return true;
                }
            }

            // Case 2: If neighbor is already in current recursion path
            // This means we found a BACK EDGE => cycle exists
            else if (inPath[neighbor]) {
                return true;
            }
        }

        // Remove node from recursion path before returning
        // because DFS exploration of this node is now complete
        inPath[node] = false;

        return false;
    }
}

// Why do we need inPath[] (recursion stack) in Directed Graph cycle detection?
//
// visited[node] only tells us: "Have we ever visited this node before?"
// But in a directed graph, reaching an already visited node does NOT always mean cycle.
//
// Example (NO cycle):
// 0 -> 1
// 0 -> 2
// 2 -> 1
//
// Here, node 1 will be visited from 0 first.
// Later, while exploring 2, we again reach 1 (visited = true),
// but there is still no cycle.
//
// So visited[] alone cannot detect cycles correctly.
//
// inPath[node] tells: "Is this node currently in the active DFS call stack / current DFS path?"
//
// If during DFS we reach a node that is already inPath[],
// that means we found a BACK EDGE (edge pointing to an ancestor in current DFS path),
// which guarantees a cycle.
//
// Example (cycle):
// 0 -> 1 -> 2 -> 3
//      ^         |
//      |_________|
//
// While exploring 3, we reach 1 again.
// Since 1 is still in the recursion stack (inPath[1] = true),
// we confirm a cycle exists.
