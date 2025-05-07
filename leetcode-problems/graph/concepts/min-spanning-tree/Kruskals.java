import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

// A Minimum Spanning Tree(MST)is a fundamental concept in graph theory.
// Given a connected,undirected,weighted graph,a spanning tree is a subgraph that connects all the vertices together without any cycles and with the minimum possible total edge weight.

class Kurskals {
    public static List<Edge> findMST(int numVertices, List<Edge> edges) {
        Collections.sort(edges, Comparator.comparingInt(e -> e.weight));
        List<Edge> mst = new ArrayList<>();
        DisjointSet ds = new DisjointSet(numVertices);
        for (Edge edge : edges) {
            int rootSource = ds.find(edge.source);
            int rootDestination = ds.find(edge.destination);
            if (rootSource != rootDestination) {
                mst.add(edge);
                ds.union(rootSource, rootDestination);
            }
        }
        return mst;
    }

    public static void main(String[] args) {
        int numVertices = 4;
        List<Edge> edges = List.of(
                new Edge(0, 1, 10),
                new Edge(0, 2, 6),
                new Edge(0, 3, 5),
                new Edge(1, 3, 15),
                new Edge(2, 3, 4));

        List<Edge> mst = findMST(numVertices, edges);

        System.out.println("Edges in the Minimum Spanning Tree (Kruskal's):");
        int totalWeight = 0;
        for (Edge edge : mst) {
            System.out.println(edge.src + " -- " + edge.dest + " : " + edge.weight);
            totalWeight += edge.weight;
        }
        System.out.println("Total weight of MST: " + totalWeight);
    }
}

class DisjointSet {
    private final int[] parent, rank;

    public DisjointSet(int n) {
        this.parent = new int[n];
        this.rank = new int[n];
        for (int i = 0; i < n; i++) {
            this.parent[i] = i;
            this.rank[i] = 0;
        }
    }

    public int find(int i) {
        if (this.parent[i] != i) {
            this.parent[i] = find(this.parent[i]);
        }
        return this.parent[i];
    }

    public void union(int i, int j) {
        int rootI = find(i);
        int rootJ = find(j);
        if (rootI == rootJ)
            return;
        if (this.rank[rootI] < this.rank[rootJ]) {
            this.parent[rootI] = rootJ;
        } else if (this.rank[rootI] > this.rank[rootJ]) {
            this.parent[rootJ] = rootI;
        } else {
            this.parent[rootJ] = rootI;
            this.rank[rootI]++;
        }
    }
}

class Edge implements Comparable<Edge> {
    final int source, destination, weight;

    public Edge(int source, int destination, int weight) {
        this.source = source;
        this.destination = destination;
        this.weight = weight;
    }

    @Override
    public int compareTo(Edge o) {
        return this.weight - o.weight;
    }
}

/*
 * Kruskal’s Algorithm - Step-by-Step Explanation
 *
 * Goal:
 *   - Find the Minimum Spanning Tree (MST) of a connected, undirected, weighted graph.
 *   - MST includes all vertices with minimum total edge weight and no cycles.
 *
 * Step 1: Sort all edges
 *   - Sort the edges in non-decreasing order of their weight.
 *   - This allows us to consider the smallest available edge first.
 *
 * Step 2: Initialize Disjoint Set Union (Union-Find)
 *   - Each vertex is its own parent (self loop).
 *   - This helps us efficiently detect cycles when adding edges.
 *   - Use path compression and union by rank for optimization.
 *
 * Step 3: Iterate through sorted edges
 *   - For each edge (u, v):
 *       a) Find the root of u and the root of v using `find()`.
 *       b) If roots are different, the edge does not form a cycle:
 *           - Include this edge in the MST.
 *           - Perform `union(u, v)` to merge the sets.
 *       c) If roots are the same, skip the edge (it would create a cycle).
 *   - Continue until MST includes exactly (V - 1) edges.
 *
 * Step 4: Output the MST
 *   - Print the edges included in the MST.
 *   - Print the total weight of the MST.
 *
 * Time Complexity:
 *   - Sorting edges: O(E log E)
 *   - Union-Find operations: O(E α(V)) ≈ O(E), where α is the inverse Ackermann function (very slow-growing)
 *   - Overall: O(E log E), where E = number of edges
 *
 * Space Complexity:
 *   - O(V + E) for storing edges and parent/rank arrays
 *
 * Pros:
 *   - Simple and efficient for sparse graphs
 *   - Easy to implement with Union-Find
 *
 * Cons:
 *   - Needs edge list (not adjacency list/matrix)
 *   - Not efficient for dense graphs compared to Prim’s with a Fibonacci heap
 */
