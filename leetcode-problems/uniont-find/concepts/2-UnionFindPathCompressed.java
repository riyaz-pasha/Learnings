class UnionFindPathCompressed {
    private int[] parent;

    public UnionFindPathCompressed(int n) {
        parent = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i; // Each element is initially its own set
        }
    }

    // Path Compression (used in the 'find' function of Disjoint Set / Union-Find):
    // This technique flattens the structure of the tree whenever 'find' is called,
    // by making each node on the path from a child to the root point directly to
    // the root.
    // This drastically reduces the time complexity of future 'find' operations.
    //
    // Without path compression, the tree can become tall, leading to slower
    // queries.
    // With path compression, the tree becomes almost flat over time,
    // resulting in nearly constant time (amortized) for both 'find' and 'union'
    // operations.
    // Time complexity per operation becomes O(α(n)), where α is the inverse
    // Ackermann function.
    // Example: a -> b -> c -> d becomes a -> d, b -> d, c -> d after find(d)

    public int find(int i) {
        if (parent[i] == i) {
            return i;
        }
        parent[i] = find(parent[i]); // Path compression
        return parent[i];
    }

    public void union(int i, int j) {
        int rootI = find(i);
        int rootJ = find(j);
        if (rootI != rootJ) {
            parent[rootJ] = rootI;
        }
    }
}
