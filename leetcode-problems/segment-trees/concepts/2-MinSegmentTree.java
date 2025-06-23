class MinSegmentTree {

    private int[] tree;
    private int n;

    /**
     * Constructor to build segment tree from array
     * 
     * @param arr input array
     */
    public MinSegmentTree(int[] arr) {
        n = arr.length;
        tree = new int[4 * n]; // Tree size is 4*n to handle all cases
        build(arr, 0, 0, n - 1);
    }

    /**
     * Build the segment tree recursively
     * 
     * @param arr   input array
     * @param node  current node index in tree
     * @param start start index of current segment
     * @param end   end index of current segment
     */
    private void build(int[] arr, int node, int start, int end) {
        if (start == end) {
            // Leaf node
            tree[node] = arr[start];
        } else {
            int mid = (start + end) / 2;
            // Build left and right subtrees
            build(arr, 2 * node + 1, start, mid);
            build(arr, 2 * node + 2, mid + 1, end);
            // Internal node stores minimum of its children
            tree[node] = Math.min(tree[2 * node + 1], tree[2 * node + 2]);
        }
    }

    /**
     * Update value at given index
     * 
     * @param idx index to update (0-based)
     * @param val new value
     */
    public void update(int idx, int val) {
        update(0, 0, n - 1, idx, val);
    }

    /**
     * Recursive helper for update operation
     * 
     * @param node  current node index in tree
     * @param start start index of current segment
     * @param end   end index of current segment
     * @param idx   index to update
     * @param val   new value
     */
    private void update(int node, int start, int end, int idx, int val) {
        if (start == end) {
            // Leaf node - update the value
            tree[node] = val;
        } else {
            int mid = (start + end) / 2;
            if (idx <= mid) {
                // Update left subtree
                update(2 * node + 1, start, mid, idx, val);
            } else {
                // Update right subtree
                update(2 * node + 2, mid + 1, end, idx, val);
            }
            // Update current node with minimum of children
            tree[node] = Math.min(tree[2 * node + 1], tree[2 * node + 2]);
        }
    }

    /**
     * Query minimum value in range [l, r]
     * 
     * @param l left boundary (inclusive, 0-based)
     * @param r right boundary (inclusive, 0-based)
     * @return minimum value in range
     */
    public int query(int l, int r) {
        return query(0, 0, n - 1, l, r);
    }

    /**
     * Recursive helper for range minimum query
     * 
     * @param node  current node index in tree
     * @param start start index of current segment
     * @param end   end index of current segment
     * @param l     query range left boundary
     * @param r     query range right boundary
     * @return minimum value in query range
     */
    private int query(int node, int start, int end, int l, int r) {
        if (r < start || end < l) {
            // Range represented by node is completely outside [l, r]
            return Integer.MAX_VALUE;
        }
        if (l <= start && end <= r) {
            // Range represented by node is completely inside [l, r]
            return tree[node];
        }
        // Range represented by node is partially inside and partially outside [l, r]
        int mid = (start + end) / 2;
        int leftMin = query(2 * node + 1, start, mid, l, r);
        int rightMin = query(2 * node + 2, mid + 1, end, l, r);
        return Math.min(leftMin, rightMin);
    }

    /**
     * Get the original array size
     * 
     * @return size of the array
     */
    public int size() {
        return n;
    }

    /**
     * Print the segment tree (for debugging)
     */
    public void printTree() {
        System.out.println("Segment Tree: ");
        for (int i = 0; i < tree.length && tree[i] != 0; i++) {
            System.out.print(tree[i] + " ");
        }
        System.out.println();
    }

    // Example usage and testing
    public static void main(String[] args) {
        // Test array
        int[] arr = { 1, 3, 2, 7, 9, 11, 4, 5 };

        // Create segment tree
        MinSegmentTree segTree = new MinSegmentTree(arr);

        System.out.println("Original array: ");
        for (int val : arr) {
            System.out.print(val + " ");
        }
        System.out.println();

        // Test queries
        System.out.println("Min in range [1, 3]: " + segTree.query(1, 3)); // Should be 2
        System.out.println("Min in range [4, 7]: " + segTree.query(4, 7)); // Should be 4
        System.out.println("Min in range [0, 7]: " + segTree.query(0, 7)); // Should be 1

        // Test update
        System.out.println("\nUpdating index 2 to value 0");
        segTree.update(2, 0);

        System.out.println("Min in range [1, 3]: " + segTree.query(1, 3)); // Should be 0
        System.out.println("Min in range [0, 7]: " + segTree.query(0, 7)); // Should be 0

        // Test edge cases
        System.out.println("Min in range [0, 0]: " + segTree.query(0, 0)); // Should be 1
        System.out.println("Min in range [7, 7]: " + segTree.query(7, 7)); // Should be 5
    }

}
