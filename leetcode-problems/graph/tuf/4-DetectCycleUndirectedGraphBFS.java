import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

class DetectCycleUndirectedGraphBFS {

    private record NodeParent(int node, int parent) {
        // parent = -1 indicates no parent (root node)
    }

    public boolean hasCycle(int V, List<List<Integer>> adj) {
        boolean[] visited = new boolean[V];

        for (int node = 0; node < V; node++) {
            if (!visited[node]) {
                if (hasCycleBFS(adj, visited, node)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasCycleBFS(List<List<Integer>> adj, boolean[] visited, int start) {
        Queue<NodeParent> queue = new LinkedList<>();
        queue.offer(new NodeParent(start, -1));
        visited[start] = true;

        while (!queue.isEmpty()) {
            NodeParent current = queue.poll();
            int currentNode = current.node();
            int parentNode = current.parent();

            for (int neighbor : adj.get(currentNode)) {
                if (!visited[neighbor]) {
                    queue.offer(new NodeParent(neighbor, currentNode));
                    visited[neighbor] = true;
                } else if (neighbor != parentNode) {
                    // Found a visited node that's not the parent -> cycle detected
                    return true;
                }
            }
        }

        return false;
    }
}
