import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class CycleDetectionUndirectedBFS {

    /*
     * Time: O(V + E) – Standard BFS traversal
     * Space: O(V) for visited array and queue
     */
    public boolean hasCycle(int V, List<List<Integer>> adj) {
        boolean[] visited = new boolean[V];

        // Check for cycle in each connected component
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
        Queue<int[]> queue = new LinkedList<>(); // [node, parent]
        visited[start] = true;
        queue.offer(new int[] { start, -1 }); // start node has no parent

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int node = current[0];
            int parent = current[1];

            for (int neighbor : adj.get(node)) {
                if (!visited[neighbor]) {
                    visited[neighbor] = true;
                    queue.offer(new int[] { neighbor, node });
                } else if (neighbor != parent) {
                    // Visited neighbor not equal to parent => cycle
                    return true;
                }
            }
        }

        return false;
    }

}

/*
 * Time Complexity: O(N + 2E) + O(N), Where N = Nodes, 2E is for total degrees
 * as we traverse all adjacent nodes. In the case of connected components of a
 * graph, it will take another O(N) time.
 * 
 * Space Complexity: O(N) + O(N) ~ O(N), Space for queue data structure and
 * visited array.
 */
