class SumSegmentTree {

    private final int[] segmentTree;
    private final int size;

    public SumSegmentTree(int[] inputArray) {
        this.size = inputArray.length;
        int treeSize = 4 * this.size;
        this.segmentTree = new int[treeSize];
        this.buildSegementTree(inputArray, 0, this.size - 1, 0);
    }

    private void buildSegementTree(int[] inputArray, int left, int right, int nodeIndex) {
        if (left == right) {
            this.segmentTree[nodeIndex] = inputArray[left];
            return;
        }
        int mid = (left + right) / 2;
        int leftNodeIndex = (2 * nodeIndex) + 1;
        int rightNodeIndex = (2 * nodeIndex) + 2;
        this.buildSegementTree(inputArray, left, mid, leftNodeIndex);
        this.buildSegementTree(inputArray, mid + 1, right, rightNodeIndex);
        this.segmentTree[nodeIndex] = this.segmentTree[leftNodeIndex] + this.segmentTree[rightNodeIndex];
    }

    public int rangeSumQuery(int queryLeft, int queryRight) {
        return this.rangeSumQueryUtil(0, size - 1, queryLeft, queryRight, 0);
    }

    private int rangeSumQueryUtil(int segLeft, int segRight, int qLeft, int qRight, int nodeIndex) {
        // no overlap
        if (qRight < segLeft || qLeft > segRight) {
            return 0;
        }

        // total overlap
        if (qLeft <= segLeft && segRight <= qRight) {
            return this.segmentTree[nodeIndex];
        }

        // partial overlap
        int mid = (segLeft + segRight) / 2;
        int leftSum = this.rangeSumQueryUtil(segLeft, mid, qLeft, qRight, (2 * nodeIndex) + 1);
        int rightSum = this.rangeSumQueryUtil(mid + 1, segRight, qLeft, qRight, (2 + nodeIndex) + 2);
        return leftSum + rightSum;
    }

}

/*
    ============================================
    📘 Segment Tree – Full Theory (Java Style)
    ============================================

    🔷 What is a Segment Tree?

    - A Segment Tree is a **binary tree data structure** used for answering range-based queries efficiently.
    - It is mainly used when:
        - The array is **static** (infrequent changes).
        - You want to answer **range queries** like:
            - Sum of elements in a range
            - Minimum/Maximum in a range
            - GCD, XOR, etc.
            - Frequency counts
        - And you want to update elements too (not just read-only queries).

    ============================================
    🔹 Segment Tree Structure
    ============================================

    - Each node in the tree represents a range or segment of the array.
    - The root represents the entire array.
    - Each internal node represents a subrange, and its value is the result (sum/min/max/etc.) of its children's segments.

    Example: For array A = [1, 3, 5, 7, 9, 11]
    
           [1,6]
          /     \
       [1,3]   [4,6]
       / \     /  \
     [1,2][3] [4,5][6]
     ... and so on
    
    - Usually stored in an array of size ≈ 4 * n (safe upper bound)

    ============================================
    🔹 Segment Tree Operations
    ============================================

    1. 🛠 Build Tree
    --------------------
    - Construct the tree from the input array.
    - Start from the root and recursively divide the array into halves.
    - Each leaf holds a value from the array.
    - Each internal node holds the result of merging (e.g., sum/min) its left and right child.

    - Time Complexity: O(n)

    2. ❓ Range Query (e.g., sum of range [l, r])
    -------------------------------------------------
    - Recursively check if the current segment:
        a. Is completely outside the query range → return neutral value (0 for sum, ∞ for min)
        b. Is completely inside the query range → return node value
        c. Partially overlaps → split query to left and right children, and merge their results

    - Time Complexity: O(log n)

    3. 🔁 Point Update (change one element)
    ---------------------------------------------
    - Change a value at a specific index.
    - Traverse from the root down to the leaf corresponding to that index.
    - Update the value, then propagate the changes upward to update parent nodes.

    - Time Complexity: O(log n)

    4. 🔁 Range Update (optional, advanced)
    ---------------------------------------------
    - Used when updating a range (e.g., increase all values from l to r by x)
    - To avoid O(n) time, **Lazy Propagation** is used
    - It defers the update to child nodes until necessary, saving computation.

    - Time Complexity with Lazy Propagation: O(log n)

    ============================================
    🔹 Time & Space Complexity Summary
    ============================================

    | Operation       | Time Complexity | Space Complexity |
    |-----------------|------------------|------------------|
    | Build           | O(n)             | O(4n)            |
    | Query (range)   | O(log n)         | O(4n)            |
    | Update (point)  | O(log n)         | O(4n)            |
    | Update (range)  | O(log n)*        | O(4n)            |

    *with lazy propagation

    ============================================
    🔹 When to Use Segment Trees
    ============================================

    - You need to handle both:
        - Frequent range queries (e.g., sum/min/max from i to j)
        - Frequent updates (e.g., change A[i] = x)
    - Too many queries for brute force (O(n) per query is too slow)
    - Binary Indexed Tree (Fenwick Tree) is not sufficient for your use case (e.g., range min/max)

    ============================================
    🔹 Alternatives to Segment Tree
    ============================================

    - ✅ Fenwick Tree / Binary Indexed Tree:
        - Easier to code, supports prefix sums efficiently
        - Works only for cumulative operations like sum/XOR
    - ✅ Sparse Table:
        - Faster for immutable arrays (only queries, no updates)
        - Uses O(n log n) space, O(1) query time
    - ✅ TreeMap / Ordered Set:
        - Useful for order statistics, not numeric range operations

*/
