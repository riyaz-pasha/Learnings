import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class Edge implements Comparable<Edge> {
    int src, dest, weight;

    public Edge(int src, int dest, int weight) {
        this.src = src;
        this.dest = dest;
        this.weight = weight;
    }

    @Override
    public int compareTo(Edge other) {
        return this.weight - other.weight;
    }

    @Override
    public String toString() {
        return src + " -- " + dest + " (weight: " + weight + ")";
    }
}

class DisjointSet {
    private List<Integer> parent;
    private List<Integer> size;

    public DisjointSet(int n) {
        parent = new ArrayList<>();
        size = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            parent.add(i);
            size.add(1);
        }
    }

    public int find(int x) {
        if (parent.get(x) != x) {
            parent.set(x, find(parent.get(x)));
        }
        return parent.get(x);
    }

    public boolean union(int x, int y) {
        int rootX = find(x);
        int rootY = find(y);

        if (rootX == rootY)
            return false;

        if (size.get(rootX) < size.get(rootY)) {
            parent.set(rootX, rootY);
            size.set(rootY, size.get(rootY) + size.get(rootX));
        } else {
            parent.set(rootY, rootX);
            size.set(rootX, size.get(rootX) + size.get(rootY));
        }
        return true;
    }
}

class KruskalMST {

    private int vertices;
    private List<Edge> edges;

    public KruskalMST(int vertices) {
        this.vertices = vertices;
        this.edges = new ArrayList<>();
    }

    public void addEdge(int src, int dest, int weight) {
        edges.add(new Edge(src, dest, weight));
    }

    public List<Edge> findMST() {
        List<Edge> mst = new ArrayList<>();

        // Step 1: Sort all edges by weight - O(E log E)
        Collections.sort(edges);

        // Step 2: Initialize Disjoint Set - O(V)
        DisjointSet ds = new DisjointSet(vertices);

        int edgesAdded = 0;
        int totalWeight = 0;

        // Step 3: Process edges in sorted order - O(E × α(V))
        for (Edge edge : edges) {
            // If we already have V-1 edges, MST is complete
            if (edgesAdded == vertices - 1)
                break;

            int srcRoot = ds.find(edge.src);
            int destRoot = ds.find(edge.dest);

            // Check if adding this edge creates a cycle
            if (srcRoot != destRoot) {
                mst.add(edge);
                ds.union(edge.src, edge.dest);
                totalWeight += edge.weight;
                edgesAdded++;
            }
        }

        // Check if MST is valid (graph was connected)
        if (edgesAdded != vertices - 1) {
            System.out.println("Graph is not connected - MST not possible!");
            return null;
        }

        System.out.println("Total MST weight: " + totalWeight);
        return mst;
    }

    public static void main(String[] args) {
        // Example graph with 5 vertices
        /*
         * 2 3
         * 0-------1-------2
         * | | |
         * 6 | | 8 | 7
         * | | |
         * 3-------4-------5
         * 1 5
         */

        KruskalMST graph = new KruskalMST(6);

        // Add edges
        graph.addEdge(0, 1, 2);
        graph.addEdge(0, 3, 6);
        graph.addEdge(1, 2, 3);
        graph.addEdge(1, 3, 8);
        graph.addEdge(1, 4, 5);
        graph.addEdge(2, 4, 7);
        graph.addEdge(3, 4, 1);
        graph.addEdge(4, 5, 5);

        System.out.println("Edges in the Minimum Spanning Tree:");
        List<Edge> mst = graph.findMST();

        if (mst != null) {
            for (Edge edge : mst) {
                System.out.println(edge);
            }
        }

        System.out.println("\n--- Another Example ---");

        // Another example with 4 vertices
        KruskalMST graph2 = new KruskalMST(4);
        graph2.addEdge(0, 1, 10);
        graph2.addEdge(0, 2, 6);
        graph2.addEdge(0, 3, 5);
        graph2.addEdge(1, 3, 15);
        graph2.addEdge(2, 3, 4);

        System.out.println("\nEdges in MST for second graph:");
        List<Edge> mst2 = graph2.findMST();

        if (mst2 != null) {
            for (Edge edge : mst2) {
                System.out.println(edge);
            }
        }
    }

}
