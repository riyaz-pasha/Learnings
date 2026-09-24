import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

/**
 * Cycle Detection in Undirected Graph using BFS
 *
 * Key Idea:
 * - BFS normally visits nodes level by level.
 * - In an undirected graph, we detect a cycle if:
 *      we find an already visited neighbor that is NOT the parent.
 *
 * Why parent is needed?
 * - Because in undirected graph, edge u--v exists both ways.
 * - So when we go from u -> v, later v will see u again.
 * - That should not be considered a cycle.
 *
 * Time Complexity: O(V + E)
 * Space Complexity: O(V)
 */
public class CycleDetectionUndirectedBFS {

    // Helper record to store (node, parent) in queue
    record Pair(int node, int parent) {}

    public boolean hasCycle(int V, List<List<Integer>> adj) {

        boolean[] visited = new boolean[V];

        // Graph may have multiple connected components
        for (int i = 0; i < V; i++) {
            if (!visited[i]) {
                if (bfsHasCycle(i, adj, visited)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean bfsHasCycle(int start, List<List<Integer>> adj, boolean[] visited) {

        Queue<Pair> queue = new ArrayDeque<>();

        visited[start] = true;
        queue.offer(new Pair(start, -1));

        while (!queue.isEmpty()) {

            Pair curr = queue.poll();
            int node = curr.node();
            int parent = curr.parent();

            for (int neighbor : adj.get(node)) {

                // If neighbor not visited -> normal BFS visit
                if (!visited[neighbor]) {
                    visited[neighbor] = true;
                    queue.offer(new Pair(neighbor, node));
                }

                // If neighbor visited and it is NOT parent -> cycle exists
                else if (neighbor != parent) {
                    return true;
                }
            }
        }

        return false;
    }
}

// Cycle condition in undirected graph:
// If we see an already visited neighbor that is NOT the parent,
// it means there is another path reaching that node -> cycle exists.
