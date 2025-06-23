/**
 * Sum Segment Tree Implementation with Detailed Explanations
 * 
 * CORE CONCEPT: A binary tree where each node represents a range [start, end]
 * and stores the sum of elements in that range.
 * 
 * TREE STRUCTURE:
 * - Root represents entire array [0, n-1]
 * - Each internal node has exactly 2 children
 * - Left child: [start, mid], Right child: [mid+1, end]
 * - Leaf nodes represent single elements
 * 
 * MEMORY LAYOUT: Array representation of binary tree
 * - Node at index i has:
 *   - Left child at 2*i + 1
 *   - Right child at 2*i + 2
 *   - Parent at (i-1)/2
 */

class SumSegmentTree {
    private int[] tree;  // The segment tree array
    private int n;       // Size of original array
    
    /**
     * CONSTRUCTOR: Build the segment tree from input array
     * 
     * WHY 4*n SIZE?
     * - Worst case: when n is not a power of 2
     * - Tree height = ceil(log2(n)) + 1
     * - Max nodes = 2^(height+1) - 1
     * - 4*n is a safe upper bound that always works
     * 
     * MEMORY TRICK: Think of it as "giving extra space to be safe"
     */
    public SumSegmentTree(int[] arr) {
        this.n = arr.length;
        this.tree = new int[4 * n];
        
        // Build the tree starting from root (index 0)
        // Root represents range [0, n-1]
        build(arr, 0, 0, n - 1);
    }
    
    /**
     * BUILD OPERATION: Construct the segment tree bottom-up
     * 
     * PARAMETERS MEANING:
     * - arr: original array
     * - node: current node index in tree array
     * - start, end: range that current node represents
     * 
     * RECURSIVE LOGIC:
     * 1. BASE CASE: If start == end, it's a leaf (single element)
     * 2. RECURSIVE CASE: Split range into two halves
     *    - Build left subtree for [start, mid]
     *    - Build right subtree for [mid+1, end]
     *    - Current node = sum of both children
     * 
     * MEMORY TRICK: "Leaf copies, internal nodes add children"
     */
    private void build(int[] arr, int node, int start, int end) {
        if (start == end) {
            // LEAF NODE: Copy original array value
            tree[node] = arr[start];
            System.out.println("Leaf: tree[" + node + "] = arr[" + start + "] = " + arr[start]);
        } else {
            // INTERNAL NODE: Build children first, then sum them
            int mid = start + (end - start) / 2;  // Avoid overflow
            
            int leftChild = 2 * node + 1;
            int rightChild = 2 * node + 2;
            
            // Build left subtree [start, mid]
            build(arr, leftChild, start, mid);
            
            // Build right subtree [mid+1, end]
            build(arr, rightChild, mid + 1, end);
            
            // Current node = sum of children
            tree[node] = tree[leftChild] + tree[rightChild];
            System.out.println("Internal: tree[" + node + "] = tree[" + leftChild + "] + tree[" + rightChild + "] = " + tree[node]);
        }
    }
    
    /**
     * QUERY OPERATION: Find sum in range [queryStart, queryEnd]
     * 
     * PUBLIC METHOD: User-friendly interface
     */
    public int query(int queryStart, int queryEnd) {
        if (queryStart < 0 || queryEnd >= n || queryStart > queryEnd) {
            throw new IllegalArgumentException("Invalid query range");
        }
        
        System.out.println("\n--- QUERYING RANGE [" + queryStart + ", " + queryEnd + "] ---");
        int result = queryHelper(0, 0, n - 1, queryStart, queryEnd);
        System.out.println("QUERY RESULT: " + result);
        return result;
    }
    
    /**
     * QUERY HELPER: Recursive implementation
     * 
     * PARAMETERS:
     * - node: current node in tree
     * - start, end: range that current node represents
     * - queryStart, queryEnd: range we want to query
     * 
     * THREE CASES TO HANDLE:
     * 1. NO OVERLAP: query range completely outside current range
     * 2. COMPLETE OVERLAP: current range completely inside query range
     * 3. PARTIAL OVERLAP: need to check both children
     * 
     * VISUAL MEMORY:
     * Current:    [----start====end----]
     * Query:  [queryStart====queryEnd]
     * 
     * Case 1: [query]     [current]  OR  [current]     [query]
     * Case 2:     [current] inside [query]
     * Case 3: Overlapping but not completely
     */
    private int queryHelper(int node, int start, int end, int queryStart, int queryEnd) {
        System.out.println("Visiting node " + node + " representing range [" + start + ", " + end + "]");
        
        // CASE 1: NO OVERLAP - return 0 (identity for sum)
        if (queryEnd < start || queryStart > end) {
            System.out.println("  No overlap - returning 0");
            return 0;
        }
        
        // CASE 2: COMPLETE OVERLAP - return entire node value
        if (queryStart <= start && end <= queryEnd) {
            System.out.println("  Complete overlap - returning " + tree[node]);
            return tree[node];
        }
        
        // CASE 3: PARTIAL OVERLAP - check both children
        System.out.println("  Partial overlap - checking children");
        int mid = start + (end - start) / 2;
        
        int leftSum = queryHelper(2 * node + 1, start, mid, queryStart, queryEnd);
        int rightSum = queryHelper(2 * node + 2, mid + 1, end, queryStart, queryEnd);
        
        int totalSum = leftSum + rightSum;
        System.out.println("  Combining: " + leftSum + " + " + rightSum + " = " + totalSum);
        return totalSum;
    }
    
    /**
     * UPDATE OPERATION: Change single element and update tree
     * 
     * PUBLIC METHOD: User-friendly interface
     */
    public void update(int index, int newValue) {
        if (index < 0 || index >= n) {
            throw new IllegalArgumentException("Invalid index");
        }
        
        System.out.println("\n--- UPDATING INDEX " + index + " TO " + newValue + " ---");
        updateHelper(0, 0, n - 1, index, newValue);
        System.out.println("UPDATE COMPLETE");
    }
    
    /**
     * UPDATE HELPER: Recursive implementation
     * 
     * LOGIC:
     * 1. Find the leaf node corresponding to the index
     * 2. Update the leaf with new value
     * 3. Propagate changes up the tree (update all ancestors)
     * 
     * PATH TO UPDATE:
     * - Only visit nodes whose range contains the target index
     * - At each level, choose left or right child based on index
     * - Update current node after updating children
     * 
     * MEMORY TRICK: "Find leaf, update ancestors on the way back"
     */
    private void updateHelper(int node, int start, int end, int index, int newValue) {
        System.out.println("Visiting node " + node + " [" + start + ", " + end + "] (value: " + tree[node] + ")");
        
        if (start == end) {
            // LEAF NODE: Update directly
            System.out.println("  Leaf found! Updating from " + tree[node] + " to " + newValue);
            tree[node] = newValue;
        } else {
            // INTERNAL NODE: Update appropriate child, then recalculate
            int mid = start + (end - start) / 2;
            
            if (index <= mid) {
                // Target is in left subtree
                System.out.println("  Going to left child (index " + index + " <= mid " + mid + ")");
                updateHelper(2 * node + 1, start, mid, index, newValue);
            } else {
                // Target is in right subtree
                System.out.println("  Going to right child (index " + index + " > mid " + mid + ")");
                updateHelper(2 * node + 2, mid + 1, end, index, newValue);
            }
            
            // Recalculate current node value
            int oldValue = tree[node];
            tree[node] = tree[2 * node + 1] + tree[2 * node + 2];
            System.out.println("  Updated node " + node + " from " + oldValue + " to " + tree[node]);
        }
    }
    
    /**
     * UTILITY METHODS FOR DEBUGGING AND VISUALIZATION
     */
    
    // Print the tree structure for debugging
    public void printTree() {
        System.out.println("\n--- SEGMENT TREE STRUCTURE ---");
        System.out.println("Array representation: ");
        for (int i = 0; i < Math.min(tree.length, 20); i++) {
            if (tree[i] == 0 && i >= 2 * n) break; // Skip unused positions
            System.out.println("tree[" + i + "] = " + tree[i]);
        }
    }
    
    // Print tree in level-order (breadth-first)
    public void printLevels() {
        System.out.println("\n--- TREE BY LEVELS ---");
        int level = 0;
        int nodesInLevel = 1;
        int index = 0;
        
        while (index < tree.length && tree[index] != 0) {
            System.out.print("Level " + level + ": ");
            for (int i = 0; i < nodesInLevel && index < tree.length; i++, index++) {
                if (tree[index] != 0 || index < n) {
                    System.out.print(tree[index] + " ");
                }
            }
            System.out.println();
            level++;
            nodesInLevel *= 2;
            
            if (index >= 4 * n || level > 10) break; // Safety check
        }
    }
    
    // Get original array by querying each element
    public int[] getOriginalArray() {
        int[] result = new int[n];
        for (int i = 0; i < n; i++) {
            result[i] = query(i, i);
        }
        return result;
    }
    
    /**
     * DEMONSTRATION AND TESTING
     */
    public static void main(String[] args) {
        System.out.println("=== SUM SEGMENT TREE - STEP BY STEP EXPLANATION ===\n");
        
        // Test array
        int[] arr = {1, 3, 5, 7, 9, 11};
        System.out.println("Original array: ");
        for (int i = 0; i < arr.length; i++) {
            System.out.print("arr[" + i + "]=" + arr[i] + " ");
        }
        System.out.println("\n");
        
        // BUILD PHASE
        System.out.println("=== BUILDING SEGMENT TREE ===");
        SumSegmentTree st = new SumSegmentTree(arr);
        st.printTree();
        st.printLevels();
        
        // QUERY PHASE
        System.out.println("\n=== TESTING QUERIES ===");
        
        // Test different query ranges
        st.query(1, 3);  // Should return 3+5+7=15
        st.query(0, 2);  // Should return 1+3+5=9
        st.query(4, 5);  // Should return 9+11=20
        st.query(2, 2);  // Should return 5 (single element)
        st.query(0, 5);  // Should return sum of entire array
        
        // UPDATE PHASE
        System.out.println("\n=== TESTING UPDATE ===");
        System.out.println("Before update - query(1,3): " + st.query(1, 3));
        
        st.update(2, 10); // Change arr[2] from 5 to 10
        st.printTree();
        
        System.out.println("After update - query(1,3): " + st.query(1, 3)); // Should be 3+10+7=20
        
        // COMPLEXITY ANALYSIS
        System.out.println("\n=== COMPLEXITY ANALYSIS ===");
        System.out.println("Array size (n): " + arr.length);
        System.out.println("Tree size: " + 4 * arr.length + " (4n space)");
        System.out.println("Build time: O(n) - visit each node once");
        System.out.println("Query time: O(log n) - visit at most 4*log(n) nodes");
        System.out.println("Update time: O(log n) - visit at most log(n) nodes");
        
        // MEMORY TRICKS TO REMEMBER
        System.out.println("\n=== MEMORY TRICKS ===");
        System.out.println("1. TREE SIZE: 4n is always safe (remember '4 times original')");
        System.out.println("2. CHILD INDICES: left=2i+1, right=2i+2 (remember '2i plus 1 or 2')");
        System.out.println("3. QUERY CASES: No overlap (0), Complete overlap (return), Partial (recurse)");
        System.out.println("4. UPDATE PATH: Always goes down to leaf, updates ancestors on way back");
        System.out.println("5. BUILD ORDER: Post-order (children first, then parent)");
        
        // COMMON MISTAKES TO AVOID
        System.out.println("\n=== COMMON MISTAKES TO AVOID ===");
        System.out.println("1. Integer overflow: Use long for large sums");
        System.out.println("2. Wrong mid calculation: Use start+(end-start)/2");
        System.out.println("3. Boundary errors: Check query bounds carefully");
        System.out.println("4. Tree size: Don't use 2n, always use 4n");
        System.out.println("5. Update propagation: Don't forget to recalculate ancestors");
    }

}
