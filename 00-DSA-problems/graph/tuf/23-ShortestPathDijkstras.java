import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

class ShortestPathDijkstras {

    record Edge(int to, int weight) {
    }

    record Pair(int node, int dist) {
    }

    public int[] shortestPath(int n, List<List<Edge>> adj, int src) {
        int[] dist = new int[n];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[src] = 0;

        PriorityQueue<Pair> pq = new PriorityQueue<>(Comparator.comparingInt(Pair::dist));
        pq.offer(new Pair(src, 0));

        while (!pq.isEmpty()) {
            Pair current = pq.poll();
            int u = current.node();
            int currentDist = current.dist();

            // Optimization: skip outdated entries
            if (currentDist > dist[u])
                continue;

            for (Edge edge : adj.get(u)) {
                int v = edge.to();
                int w = edge.weight();

                if (dist[u] + w < dist[v]) {
                    dist[v] = dist[u] + w;
                    pq.offer(new Pair(v, dist[v])); // ✅ push updated distance
                }
            }
        }

        return dist;
    }

    // Helper to build adjacency list
    public static List<List<Edge>> buildAdj(int n, int[][] edges) {
        List<List<Edge>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++)
            adj.add(new ArrayList<>());
        for (int[] e : edges) {
            int u = e[0], v = e[1], w = e[2];
            adj.get(u).add(new Edge(v, w));
            adj.get(v).add(new Edge(u, w)); // for undirected
        }
        return adj;
    }

    // Example run
    public static void main(String[] args) {
        int n = 6;
        int[][] edges = {
                { 0, 1, 4 }, { 0, 2, 4 }, { 1, 2, 2 }, { 2, 3, 3 },
                { 2, 4, 1 }, { 2, 5, 6 }, { 3, 5, 2 }, { 4, 5, 3 }
        };

        List<List<Edge>> adj = buildAdj(n, edges);
        ShortestPathDijkstras sp = new ShortestPathDijkstras();
        int[] dist = sp.shortestPath(n, adj, 0);

        System.out.println("Shortest distances from node 0:");
        for (int i = 0; i < n; i++) {
            System.out.println("Node " + i + " -> " + (dist[i] == Integer.MAX_VALUE ? "INF" : dist[i]));
        }
    }
}

/*
 * ✅ Works on:
 * Weighted graphs (directed or undirected)
 * Non-negative edge weights
 * 
 * ❌ Does not work if edges have negative weights
 * (use Bellman-Ford instead).
 */
