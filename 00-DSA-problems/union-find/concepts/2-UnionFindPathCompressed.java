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

/*
 * FIND OPERATION WITH PATH COMPRESSION
 * 
 * Purpose: Find the root (representative) of the set containing element x
 * 
 * How it works:
 * 1. If parent[x] == x, then x is already the root, so return x
 * 2. If parent[x] != x, then x is not the root, so we need to find its root
 * 
 * PATH COMPRESSION OPTIMIZATION:
 * Instead of just following the chain parent[x] -> parent[parent[x]] -> ... ->
 * root,
 * we compress the path by making x point directly to the root.
 * 
 * Example without path compression:
 * Initial: 0->1->2->3 (where 3 is root)
 * find(0) traverses: 0->1->2->3, returns 3
 * Structure remains: 0->1->2->3
 * 
 * Example with path compression:
 * Initial: 0->1->2->3 (where 3 is root)
 * find(0) process:
 * - find(0): parent[0]=1, not root, so call find(1)
 * - find(1): parent[1]=2, not root, so call find(2)
 * - find(2): parent[2]=3, not root, so call find(3)
 * - find(3): parent[3]=3, this IS root, return 3
 * - Back to find(2): set parent[2] = 3 (compress!)
 * - Back to find(1): set parent[1] = 3 (compress!)
 * - Back to find(0): set parent[0] = 3 (compress!)
 * 
 * After compression: 0->3, 1->3, 2->3, 3->3
 * Now all nodes point directly to root!
 * 
 * Benefits:
 * - Future find(0), find(1), find(2) operations are O(1)
 * - Tree becomes flatter, improving overall performance
 * - Amortized time complexity becomes nearly O(1)
 */