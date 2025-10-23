import java.util.List;

class DetectCycleDirectedGraphDFS {

    public boolean hasCycle(int V, List<List<Integer>> adj) {
        boolean[] visited = new boolean[V];
        boolean[] recStack = new boolean[V]; // Recursion stack

        for (int node = 0; node < V; node++) {
            if (!visited[node]) {
                if (hasCycleDFS(adj, visited, recStack, node)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasCycleDFS(List<List<Integer>> adj, boolean[] visited,
            boolean[] recStack, int node) {
        visited[node] = true;
        recStack[node] = true; // Add to current path

        for (int neighbor : adj.get(node)) {
            // If not visited, explore recursively
            if (!visited[neighbor]) {
                if (hasCycleDFS(adj, visited, recStack, neighbor)) {
                    return true;
                }
            }
            // If neighbor is in current path, cycle found
            else if (recStack[neighbor]) {
                return true;
            }
        }

        recStack[node] = false; // Remove from current path (backtrack)
        return false;
    }
}

class DetectCycleDirectedGraphDFS2 {

    public boolean hasCycle(int V, List<List<Integer>> adj) {
        int[] state = new int[V]; // 0: unvisited, 1: visiting, 2: visited

        for (int node = 0; node < V; node++) {
            if (state[node] == 0) {
                if (hasCycleDFS(adj, state, node)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasCycleDFS(List<List<Integer>> adj, int[] state, int node) {
        state[node] = 1; // Mark as visiting (in current path)

        for (int neighbor : adj.get(node)) {
            if (state[neighbor] == 1) { // Back edge found - cycle!
                return true;
            }
            if (state[neighbor] == 0) { // Unvisited, explore
                if (hasCycleDFS(adj, state, neighbor)) {
                    return true;
                }
            }
            // If state[neighbor] == 2 (visited), skip (cross edge)
        }

        state[node] = 2; // Mark as visited (done processing)
        return false;
    }
}

/*
 * | Aspect | Undirected Graph | Directed Graph |
 * |--------|------------------|----------------|
 * | Cycle Detection | Single `visited[]` array | Need `visited[]` +
 * `recStack[]` |
 * | Cycle Condition | Visiting an already visited neighbor (except parent) |
 * Visiting a node in current recursion path |
 * | Back Edge | Any edge to visited node | Edge to node in current path |
 * 
 * ## Example Walkthrough
 * ```
 * Graph:
 * 0 → 1 → 2
 * ↓ ↑
 * 3 ──────┘ (cycle: 1 → 2 → 3 → back to 1? NO, 3→2 exists)
 * 
 * Actually: 0→1→2, 0→3
 */
