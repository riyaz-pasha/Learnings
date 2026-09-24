class MinMaxSegmentTree {

    private int[] minTree;
    private int[] maxTree;
    private int n;
    
    // Constructor: Build the segment tree from input array
    public MinMaxSegmentTree(int[] arr) {
        n = arr.length;
        // Tree needs 4*n space to handle all cases safely
        minTree = new int[4 * n];
        maxTree = new int[4 * n];
        build(arr, 0, 0, n - 1);
    }
    
    /**
     * BUILD OPERATION
     * Purpose: Construct the segment tree from the input array
     * Time Complexity: O(n)
     * 
     * Reasoning:
     * - We recursively divide the array into segments
     * - At each node, we store the min and max of its segment
     * - Leaf nodes store individual array elements
     * - Internal nodes store min/max of their children
     */
    private void build(int[] arr, int node, int start, int end) {
        if (start == end) {
            // Leaf node: store the array element
            minTree[node] = arr[start];
            maxTree[node] = arr[start];
        } else {
            int mid = (start + end) / 2;
            int leftChild = 2 * node + 1;
            int rightChild = 2 * node + 2;
            
            // Recursively build left and right subtrees
            build(arr, leftChild, start, mid);
            build(arr, rightChild, mid + 1, end);
            
            // Current node stores min/max of its children
            minTree[node] = Math.min(minTree[leftChild], minTree[rightChild]);
            maxTree[node] = Math.max(maxTree[leftChild], maxTree[rightChild]);
        }
    }
    
    /**
     * QUERY OPERATION (Public interface)
     * Purpose: Find min and max in range [queryStart, queryEnd]
     * Time Complexity: O(log n)
     */
    public int[] queryRange(int queryStart, int queryEnd) {
        int[] result = query(0, 0, n - 1, queryStart, queryEnd);
        return result; // [min, max]
    }
    
    /**
     * QUERY OPERATION (Internal implementation)
     * 
     * Three cases to handle:
     * 1. No overlap: Query range doesn't intersect current segment
     * 2. Complete overlap: Current segment is completely inside query range
     * 3. Partial overlap: Query range partially intersects current segment
     * 
     * Memory trick: "No overlap = return neutral, Complete overlap = return current, 
     *               Partial overlap = combine children"
     */
    private int[] query(int node, int start, int end, int queryStart, int queryEnd) {
        // Case 1: No overlap
        if (queryEnd < start || queryStart > end) {
            // Return neutral values (won't affect min/max operations)
            return new int[]{Integer.MAX_VALUE, Integer.MIN_VALUE};
        }
        
        // Case 2: Complete overlap - current segment is inside query range
        if (queryStart <= start && end <= queryEnd) {
            return new int[]{minTree[node], maxTree[node]};
        }
        
        // Case 3: Partial overlap - need to check both children
        int mid = (start + end) / 2;
        int leftChild = 2 * node + 1;
        int rightChild = 2 * node + 2;
        
        int[] leftResult = query(leftChild, start, mid, queryStart, queryEnd);
        int[] rightResult = query(rightChild, mid + 1, end, queryStart, queryEnd);
        
        // Combine results from both children
        int minResult = Math.min(leftResult[0], rightResult[0]);
        int maxResult = Math.max(leftResult[1], rightResult[1]);
        
        return new int[]{minResult, maxResult};
    }
    
    /**
     * UPDATE OPERATION (Public interface)
     * Purpose: Update element at index with new value
     * Time Complexity: O(log n)
     */
    public void update(int index, int newValue) {
        update(0, 0, n - 1, index, newValue);
    }
    
    /**
     * UPDATE OPERATION (Internal implementation)
     * 
     * Process:
     * 1. Navigate to the leaf node representing the index
     * 2. Update the leaf node with new value
     * 3. Propagate changes up to root, updating all ancestors
     * 
     * Memory trick: "Go down to find leaf, come back up updating parents"
     */
    private void update(int node, int start, int end, int index, int newValue) {
        if (start == end) {
            // Leaf node: update with new value
            minTree[node] = newValue;
            maxTree[node] = newValue;
        } else {
            int mid = (start + end) / 2;
            int leftChild = 2 * node + 1;
            int rightChild = 2 * node + 2;
            
            // Decide which child contains the index
            if (index <= mid) {
                update(leftChild, start, mid, index, newValue);
            } else {
                update(rightChild, mid + 1, end, index, newValue);
            }
            
            // Update current node based on updated children
            minTree[node] = Math.min(minTree[leftChild], minTree[rightChild]);
            maxTree[node] = Math.max(maxTree[leftChild], maxTree[rightChild]);
        }
    }
    
    /**
     * RANGE UPDATE OPERATION (Bonus - with lazy propagation concept)
     * Purpose: Add a value to all elements in range [updateStart, updateEnd]
     * This is a simplified version without full lazy propagation
     */
    public void rangeUpdate(int updateStart, int updateEnd, int delta) {
        rangeUpdate(0, 0, n - 1, updateStart, updateEnd, delta);
    }
    
    private void rangeUpdate(int node, int start, int end, int updateStart, int updateEnd, int delta) {
        // Case 1: No overlap
        if (updateEnd < start || updateStart > end) {
            return;
        }
        
        // Case 2: Complete overlap
        if (updateStart <= start && end <= updateEnd) {
            minTree[node] += delta;
            maxTree[node] += delta;
            return;
        }
        
        // Case 3: Partial overlap
        int mid = (start + end) / 2;
        int leftChild = 2 * node + 1;
        int rightChild = 2 * node + 2;
        
        rangeUpdate(leftChild, start, mid, updateStart, updateEnd, delta);
        rangeUpdate(rightChild, mid + 1, end, updateStart, updateEnd, delta);
        
        // Update current node
        minTree[node] = Math.min(minTree[leftChild], minTree[rightChild]);
        maxTree[node] = Math.max(maxTree[leftChild], maxTree[rightChild]);
    }
    
    // Utility method to print the tree structure (for debugging)
    public void printTree() {
        System.out.println("Min Tree: ");
        printArray(minTree, Math.min(15, minTree.length));
        System.out.println("Max Tree: ");
        printArray(maxTree, Math.min(15, maxTree.length));
    }
    
    private void printArray(int[] arr, int limit) {
        for (int i = 0; i < limit; i++) {
            System.out.print(arr[i] + " ");
        }
        System.out.println();
    }
    
    // Demo and test method
    public static void main(String[] args) {
        // Test the segment tree
        int[] arr = {1, 3, 2, 7, 9, 11, 5, 4};
        MinMaxSegmentTree st = new MinMaxSegmentTree(arr);
        
        System.out.println("Original array: [1, 3, 2, 7, 9, 11, 5, 4]");
        st.printTree();
        
        // Test queries
        System.out.println("\n=== QUERY TESTS ===");
        int[] result1 = st.queryRange(1, 5); // Query range [1,5]
        System.out.println("Range [1,5]: Min = " + result1[0] + ", Max = " + result1[1]);
        
        int[] result2 = st.queryRange(0, 3); // Query range [0,3]
        System.out.println("Range [0,3]: Min = " + result2[0] + ", Max = " + result2[1]);
        
        // Test update
        System.out.println("\n=== UPDATE TEST ===");
        System.out.println("Updating index 2 from 2 to 15");
        st.update(2, 15);
        
        int[] result3 = st.queryRange(0, 3); // Query same range after update
        System.out.println("Range [0,3] after update: Min = " + result3[0] + ", Max = " + result3[1]);
        
        // Test range update
        System.out.println("\n=== RANGE UPDATE TEST ===");
        System.out.println("Adding 10 to range [1,3]");
        st.rangeUpdate(1, 3, 10);
        
        int[] result4 = st.queryRange(0, 7); // Query entire range
        System.out.println("Entire range after range update: Min = " + result4[0] + ", Max = " + result4[1]);
    }

}

/*
=== KEY CONCEPTS FOR EASY MEMORY ===

1. TREE STRUCTURE:
   - Use array representation: parent at i, children at 2i+1 and 2i+2
   - Need 4*n space for safety
   - Root is at index 0

2. THREE MAIN CASES (for both query and update):
   - No Overlap: ranges don't intersect
   - Complete Overlap: current segment fully inside query
   - Partial Overlap: need to check both children

3. OPERATION PATTERNS:
   - Build: Bottom-up (leaves to root)
   - Query: Top-down with pruning
   - Update: Top-down to leaf, then bottom-up propagation

4. TIME COMPLEXITIES:
   - Build: O(n)
   - Query: O(log n)
   - Update: O(log n)
   - Space: O(n)

5. MEMORY TRICKS:
   - "Divide and Conquer" - always split at mid
   - "Combine Results" - merge children's min/max
   - "Lazy Updates" - defer updates when possible (for range updates)
*/
