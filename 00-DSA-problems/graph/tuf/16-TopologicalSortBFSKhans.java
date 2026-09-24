import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

class TopologicalSortBFSKhans {

    public List<Integer> topoSort(int V, List<List<Integer>> adj) {
        int[] indegree = new int[V];

        for (int node = 0; node < V; node++) {
            for (int neighbor : adj.get(node)) {
                indegree[neighbor]++;
            }
        }

        Queue<Integer> queue = new LinkedList<>();
        for (int node = 0; node < V; node++) {
            if (indegree[node] == 0) {
                queue.offer(node);
            }
        }

        List<Integer> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            int current = queue.poll();
            result.add(current);

            for (int neighbor : adj.get(current)) {
                indegree[neighbor]--;
                if (indegree[neighbor] == 0) {
                    queue.offer(neighbor);
                }
            }
        }

        if (result.size() != V) {
            throw new IllegalStateException(
                    "Cycle detected! Topological sort not possible for graph with " + V + " vertices.");
        }

        return result;
    }

}
