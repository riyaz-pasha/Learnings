class MaxSegmentTree {

    private int[] tree;
    private int n;

    /**
     * Constructor to build segment tree from array
     * 
     * @param arr input array
     */
    public MaxSegmentTree(int[] arr) {
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
            // Internal node stores maximum of its children
            tree[node] = Math.max(tree[2 * node + 1], tree[2 * node + 2]);
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
            // Update current node with maximum of children
            tree[node] = Math.max(tree[2 * node + 1], tree[2 * node + 2]);
        }
    }

    /**
     * Query maximum value in range [l, r]
     * 
     * @param l left boundary (inclusive, 0-based)
     * @param r right boundary (inclusive, 0-based)
     * @return maximum value in range
     */
    public int query(int l, int r) {
        return query(0, 0, n - 1, l, r);
    }

    /**
     * Recursive helper for range maximum query
     * 
     * @param node  current node index in tree
     * @param start start index of current segment
     * @param end   end index of current segment
     * @param l     query range left boundary
     * @param r     query range right boundary
     * @return maximum value in query range
     */
    private int query(int node, int start, int end, int l, int r) {
        if (r < start || end < l) {
            // Range represented by node is completely outside [l, r]
            return Integer.MIN_VALUE;
        }
        if (l <= start && end <= r) {
            // Range represented by node is completely inside [l, r]
            return tree[node];
        }
        // Range represented by node is partially inside and partially outside [l, r]
        int mid = (start + end) / 2;
        int leftMax = query(2 * node + 1, start, mid, l, r);
        int rightMax = query(2 * node + 2, mid + 1, end, l, r);
        return Math.max(leftMax, rightMax);
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

    /**
     * Get maximum value in entire array
     * 
     * @return maximum value
     */
    public int getMax() {
        return tree[0];
    }

    /**
     * Find index of maximum element in range [l, r]
     * 
     * @param l left boundary (inclusive, 0-based)
     * @param r right boundary (inclusive, 0-based)
     * @return index of maximum element
     */
    public int queryMaxIndex(int l, int r) {
        return queryMaxIndex(0, 0, n - 1, l, r);
    }

    /**
     * Recursive helper for finding index of maximum element
     * 
     * @param node  current node index in tree
     * @param start start index of current segment
     * @param end   end index of current segment
     * @param l     query range left boundary
     * @param r     query range right boundary
     * @return index of maximum element in query range
     */
    private int queryMaxIndex(int node, int start, int end, int l, int r) {
        if (r < start || end < l) {
            return -1; // Invalid range
        }
        if (start == end) {
            return start; // Leaf node - return the index
        }

        int mid = (start + end) / 2;
        int leftMaxIdx = queryMaxIndex(2 * node + 1, start, mid, l, r);
        int rightMaxIdx = queryMaxIndex(2 * node + 2, mid + 1, end, l, r);

        if (leftMaxIdx == -1)
            return rightMaxIdx;
        if (rightMaxIdx == -1)
            return leftMaxIdx;

        // Compare values at both indices and return index with larger value
        int leftMax = query(leftMaxIdx, leftMaxIdx);
        int rightMax = query(rightMaxIdx, rightMaxIdx);

        return leftMax >= rightMax ? leftMaxIdx : rightMaxIdx;
    }

    // Example usage and testing
    public static void main(String[] args) {
        // Test array
        int[] arr = { 1, 3, 2, 7, 9, 11, 4, 5 };

        // Create segment tree
        MaxSegmentTree segTree = new MaxSegmentTree(arr);

        System.out.println("Original array: ");
        for (int val : arr) {
            System.out.print(val + " ");
        }
        System.out.println();

        // Test queries
        System.out.println("Max in range [1, 3]: " + segTree.query(1, 3)); // Should be 7
        System.out.println("Max in range [4, 7]: " + segTree.query(4, 7)); // Should be 11
        System.out.println("Max in range [0, 7]: " + segTree.query(0, 7)); // Should be 11
        System.out.println("Max in entire array: " + segTree.getMax()); // Should be 11

        // Test update
        System.out.println("\nUpdating index 2 to value 15");
        segTree.update(2, 15);

        System.out.println("Max in range [1, 3]: " + segTree.query(1, 3)); // Should be 15
        System.out.println("Max in range [0, 7]: " + segTree.query(0, 7)); // Should be 15
        System.out.println("Max in entire array: " + segTree.getMax()); // Should be 15

        // Test edge cases
        System.out.println("Max in range [0, 0]: " + segTree.query(0, 0)); // Should be 1
        System.out.println("Max in range [7, 7]: " + segTree.query(7, 7)); // Should be 5

        // Test max index query
        System.out.println("Index of max in range [1, 3]: " + segTree.queryMaxIndex(1, 3)); // Should be 2
        System.out.println("Index of max in range [4, 6]: " + segTree.queryMaxIndex(4, 6)); // Should be 5
    }

}

// Time Complexities:
// Construction: O(n)
// Range Query: O(log n)
// Point Update: O(log n)
// Max Index Query: O(log n)

// Space Complexity: O(4n)
