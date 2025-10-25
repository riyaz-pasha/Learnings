
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

class ShortestPathDirectedAcyclicGraph {

    record Edge(int to, int weight) {
    };

    public int[] shortestPathDAG(int n, List<List<Edge>> adj, int src) {
        // 1. topological sort
        boolean[] visited = new boolean[n];
        Stack<Integer> stack = new Stack<>();

        for (int node = 0; node < n; node++) {
            if (!visited[node]) {
                topoSort(adj, visited, stack, node);
            }
        }

        // 2. initialize distances
        int[] dist = new int[n];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[src] = 0;

        // 3. Relax edges in topo order
        while (!stack.isEmpty()) {
            int current = stack.pop();
            if (dist[current] != Integer.MAX_VALUE) {
                for (Edge edge : adj.get(current)) {
                    int neighbor = edge.to, w = edge.weight;
                    if (dist[current] + w < dist[neighbor]) {
                        dist[neighbor] = dist[current] + w;
                    }
                }
            }
        }

        return dist;
    }

    private void topoSort(List<List<Edge>> adj, boolean[] visited, Stack<Integer> stack, int node) {
        visited[node] = true;

        for (Edge edge : adj.get(node)) {
            if (!visited[edge.to]) {
                topoSort(adj, visited, stack, edge.to);
            }
        }

        stack.push(node);
    }

}
/*
 * 🚀 Key Idea
 * 
 * In a DAG, there are no cycles — which means you can use Topological Sorting
 * to compute shortest paths efficiently.
 * 
 * The algorithm works for weighted DAGs (both positive and negative weights),
 * unlike Dijkstra which fails for negatives.
 * 
 * 🧩 Algorithm Intuition
 * 
 * Topologically sort all vertices (order such that for every directed edge u →
 * v, u comes before v).
 * 
 * Initialize all distances to infinity except the source (dist[src] = 0).
 * 
 * Process vertices in topological order:
 * 
 * For each vertex u, relax all its outgoing edges u → v (i.e., update dist[v] =
 * min(dist[v], dist[u] + weight(u,v))).
 * 
 * Because of the topological order, each vertex is processed after all possible
 * shortest paths to it have been computed.
 */
