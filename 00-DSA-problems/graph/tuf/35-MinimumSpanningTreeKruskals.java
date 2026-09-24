import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class MinimumSpanningTreeKruskals {

    public List<Edge> minimumSpanningTree(int n, List<Edge> edges) {
        Collections.sort(edges, Comparator.comparing(Edge::weight));

        List<Edge> result = new ArrayList<>();

        DisjointSet ds = new DisjointSet(n);

        for (Edge edge : edges) {
            if (ds.union(edge.src(), edge.dst())) {
                result.add(edge);
            }
        }

        return result;
    }

}

record Edge(int src, int dst, int weight) {
};

class DisjointSet {

    private int[] parent;
    private int[] size;

    public DisjointSet(int n) {
        this.parent = new int[n];
        this.size = new int[n];
        for (int i = 0; i < n; i++) {
            this.parent[i] = i;
            this.size[i] = 1;
        }
    }

    public int find(int x) {
        if (this.parent[x] != x) {
            this.parent[x] = this.find(this.parent[x]);
        }
        return this.parent[x];
    }

    public boolean union(int x, int y) {
        int rootX = this.find(x);
        int rootY = this.find(y);

        if (rootX == rootY) {
            return false;
        }
        if (this.size[rootX] < this.size[rootY]) {
            this.parent[rootX] = rootY;
            this.size[rootY] += this.size[rootX];
        } else {
            this.parent[rootY] = rootX;
            this.size[rootX] += this.size[rootY];
        }
        return true;
    }

}

/*
 * Time Complexity Analysis
 * Overall: O(E log E) or O(E log V)
 * Breaking down each step:
 * 
 * Sorting Edges: O(E log E)
 * 
 * We sort all E edges by weight
 * This is the dominant operation
 * 
 * 
 * Initialize Disjoint Set: O(V)
 * 
 * Create parent and size arrays for V vertices
 * 
 * 
 * Processing Edges: O(E × α(V))
 * 
 * Iterate through E edges
 * For each edge, perform 2 find operations: O(α(V))
 * Perform union if needed: O(α(V))
 * Since α(V) ≈ O(1), this step is effectively O(E)
 * 
 * 
 * 
 * Final Time Complexity: O(E log E + E × α(V)) = O(E log E)
 * Note:
 * 
 * Since E ≤ V² in a simple graph, log E ≤ 2 log V
 * Therefore, O(E log E) = O(E log V)
 * Both representations are correct!
 * 
 * 
 * Space Complexity: O(E + V)
 * 
 * Edge List: O(E)
 * 
 * Store all edges in the graph
 * 
 * 
 * MST Result: O(V)
 * 
 * MST contains exactly V-1 edges
 * 
 * 
 * Disjoint Set: O(V)
 * 
 * Parent list: O(V)
 * Size list: O(V)
 * 
 * 
 * 
 * Total Space: O(E + V) = O(E) (since E typically dominates)
 */
