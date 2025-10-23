import java.util.LinkedList;
import java.util.Queue;

/*
 * There is an undirected graph with n nodes, where each node is numbered
 * between 0 and n - 1. You are given a 2D array graph, where graph[u] is an
 * array of nodes that node u is adjacent to. More formally, for each v in
 * graph[u], there is an undirected edge between node u and node v. The graph
 * has the following properties:
 * 
 * There are no self-edges (graph[u] does not contain u).
 * There are no parallel edges (graph[u] does not contain duplicate values).
 * If v is in graph[u], then u is in graph[v] (the graph is undirected).
 * The graph may not be connected, meaning there may be two nodes u and v such
 * that there is no path between them.
 * A graph is bipartite if the nodes can be partitioned into two independent
 * sets A and B such that every edge in the graph connects a node in set A and a
 * node in set B.
 * 
 * Return true if and only if it is bipartite.
 * 
 * Example 1:
 * Input: graph = [[1,2,3],[0,2],[0,1,3],[0,2]]
 * Output: false
 * Explanation: There is no way to partition the nodes into two independent sets
 * such that every edge connects a node in one and a node in the other.
 * 
 * Example 2:
 * Input: graph = [[1,3],[0,2],[1,3],[0,2]]
 * Output: true
 * Explanation: We can partition the nodes into two sets: {0, 2} and {1, 3}.
 */

class IsGraphBipartite {

    public boolean isBipartite(int[][] graph) {
        int n = graph.length;
        int[] color = new int[n]; // 0 = unvisited, 1 / -1 = two colors

        for (int node = 0; node < n; node++) {
            if (color[node] == 0) { // new component
                if (!bfsCheck(graph, color, node)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean bfsCheck(int[][] graph, int[] color, int start) {
        int n = graph.length;
        Queue<Integer> q = new LinkedList<>();
        q.offer(start);
        color[start] = 1; // start with color 1

        while (!q.isEmpty()) {
            int u = q.poll();
            // iterate neighbors by scanning adjacency row
            for (int v = 0; v < n; v++) {
                if (graph[u][v] == 1) {
                    if (color[v] == 0) {
                        color[v] = -color[u];
                        q.offer(v);
                    } else if (color[v] == color[u]) {
                        return false; // same color on both ends -> not bipartite
                    }
                }
            }
        }
        return true;
    }

}

class Solution {
    public boolean isBipartite(int[][] graph) {
        int n = graph.length;
        int[] colors = new int[n];
        // 0: not colored, 1: color A, -1: color B

        // Check each component (graph may not be connected)
        for (int i = 0; i < n; i++) {
            if (colors[i] == 0) {
                if (!bfs(graph, colors, i)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean bfs(int[][] graph, int[] colors, int start) {
        Queue<Integer> queue = new LinkedList<>();
        queue.offer(start);
        colors[start] = 1;

        while (!queue.isEmpty()) {
            int node = queue.poll();

            for (int neighbor : graph[node]) {
                if (colors[neighbor] == 0) {
                    // Color with opposite color
                    colors[neighbor] = -colors[node];
                    queue.offer(neighbor);
                } else if (colors[neighbor] == colors[node]) {
                    // Same color as current node - not bipartite
                    return false;
                }
            }
        }
        return true;
    }
}

class Solution2 {
    public boolean isBipartite(int[][] graph) {
        int n = graph.length;
        int[] colors = new int[n];

        // Check each component
        for (int i = 0; i < n; i++) {
            if (colors[i] == 0) {
                if (!dfs(graph, colors, i, 1)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean dfs(int[][] graph, int[] colors, int node, int color) {
        colors[node] = color;

        for (int neighbor : graph[node]) {
            if (colors[neighbor] == 0) {
                // Color with opposite color and recurse
                if (!dfs(graph, colors, neighbor, -color)) {
                    return false;
                }
            } else if (colors[neighbor] == color) {
                // Same color as current node - not bipartite
                return false;
            }
        }
        return true;
    }
}

class Solution {
    public boolean isBipartite(int[][] graph) {
        int n = graph.length;
        UnionFind uf = new UnionFind(n);

        for (int u = 0; u < n; u++) {
            if (graph[u].length == 0)
                continue;

            int firstNeighbor = graph[u][0];
            for (int v : graph[u]) {
                // If u and v are in the same set, not bipartite
                if (uf.find(u) == uf.find(v)) {
                    return false;
                }
                // All neighbors of u should be in the same set
                uf.union(firstNeighbor, v);
            }
        }
        return true;
    }

    class UnionFind {
        int[] parent;
        int[] rank;

        public UnionFind(int n) {
            parent = new int[n];
            rank = new int[n];
            for (int i = 0; i < n; i++) {
                parent[i] = i;
            }
        }

        public int find(int x) {
            if (parent[x] != x) {
                parent[x] = find(parent[x]);
            }
            return parent[x];
        }

        public void union(int x, int y) {
            int rootX = find(x);
            int rootY = find(y);

            if (rootX != rootY) {
                if (rank[rootX] > rank[rootY]) {
                    parent[rootY] = rootX;
                } else if (rank[rootX] < rank[rootY]) {
                    parent[rootX] = rootY;
                } else {
                    parent[rootY] = rootX;
                    rank[rootX]++;
                }
            }
        }
    }
}

/*
 * Time Complexity: O(V + E) where V is the number of vertices and E is the
 * number of edges
 * Space Complexity: O(V) for the color/parent arrays
 */
