import java.util.List;

class DetectCycleUndirectedGraphDFS {

    /*
     * Time Complexity: O(V + E)
     * Space Complexity: O(V) for visited array + O(V) for recursion stack
     */

    public boolean hasCycle(int V, List<List<Integer>> adj) {
        boolean[] visited = new boolean[V];

        // Check each component of the graph
        for (int node = 0; node < V; node++) {
            if (!visited[node]) { // ✓ Only check unvisited nodes
                if (hasCycleDFS(adj, visited, node, -1)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasCycleDFS(List<List<Integer>> adj, boolean[] visited, int node, int parent) {
        visited[node] = true;

        // Check all neighbors
        for (int neighbor : adj.get(node)) {
            if (!visited[neighbor]) {
                // Recursively check unvisited neighbor
                if (hasCycleDFS(adj, visited, neighbor, node)) {
                    return true; // ✓ Only return true if cycle found
                }
            } else if (neighbor != parent) {
                // Found a visited node that's not the parent -> cycle!
                return true;
            }
        }

        // No cycle found in this branch
        return false;
    }
}

// adj -> [[1,2],[0,2],[0,1]]
// node = 0 , 1, 2
// parent = -1, 0, 1
// visited 0-true,1-true. 2-true
// neighbor=1 2 0
