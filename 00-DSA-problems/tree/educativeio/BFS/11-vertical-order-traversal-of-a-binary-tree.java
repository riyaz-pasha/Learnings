/**
 * ============================================================================
 * 0. PROBLEM STATEMENT
 * ============================================================================
 * Find the vertical order traversal of a binary tree when the root is given. 
 * Return the values of the nodes from top to bottom in each column, column 
 * by column from left to right. 
 * 
 * If there is more than one node in the same column and row, return the values 
 * from left to right.
 * 
 * Constraints:
 * - The number of nodes in the tree is in the range [1, 500].
 * - 0 <= Node.data <= 1000
 * 
 * ============================================================================
 * 1. CLARIFYING QUESTIONS
 * ============================================================================
 * - "The constraints say [1, 500] nodes. Does this mean I don't need to worry about a null root?"
 *   Yes, it guarantees at least 1 node. (Though writing defensive `if (root == null)` is still a best practice).
 * - "The problem states 'return the values from left to right' for ties in the same row/col. Does this mean we do NOT sort by value?"
 *   This is a critical distinction. We must preserve their natural geographical left-to-right order, NOT sort them numerically.
 * - "Can we assume the output should be a List of Lists?"
 *   Standard interface design implies `List<List<Integer>>` where each inner list represents a vertical column.
 * 
 * ============================================================================
 * 2. THE REASONING JOURNEY
 * ============================================================================
 * [Binding Constraint] 
 * We have to translate a structure built on "Left/Right/Parent/Child" into an X/Y 
 * coordinate grid. Furthermore, we must strictly respect top-to-bottom and left-to-right 
 * ordering when constructing our final lists.
 * 
 * --- APPROACH 1: Depth-First Search (DFS) with a TreeMap ---
 * 1. What I'd naturally try: Write a recursive function. Pass the current `col` and `row` 
 *    down the stack. I'd store nodes in a complex map: `TreeMap<Col, TreeMap<Row, List<Integer>>>`.
 * 2. Why it works: DFS hits every node. The TreeMaps automatically keep the columns sorted 
 *    (from leftmost to rightmost) and the rows sorted (top to bottom).
 * 3. Why it's too costly: DFS travels deep before it travels wide. A node at depth 10 might 
 *    be discovered and added to the list *before* a node at depth 2! This destroys the natural 
 *    top-to-bottom order. To fix it, we have to do heavy sorting at the end. 
 * 4. What work is repeated: We use heavy $O(\log K)$ insertions for every node and then have 
 *    to flatten and sort everything later.
 * 5. Time Complexity: O(N log N) — due to sorting the coordinates.
 * 6. Space Complexity: O(N) — for the maps and recursion stack.
 * 
 * [The Core Observation]
 * We need an algorithm that natively visits nodes exactly how the output demands: 
 * Top-to-Bottom first (Row by Row), and Left-to-Right second. Breadth-First Search (BFS) 
 * does exactly this natively! If we use BFS, we don't need to track rows at all.
 * 
 * --- APPROACH 2: Breadth-First Search (BFS) with Min/Max Pointers (The Optimal Way) ---
 * 1. How it works: Use a Queue to run a standard BFS. To track the columns, wrap the `TreeNode` 
 *    in a helper class alongside an `int col` (root is 0, left is col-1, right is col+1). 
 *    Add the nodes to a `HashMap<Integer, List<Integer>>` as they are popped.
 * 2. Why it's optimal: Because BFS goes level by level, top-to-bottom is guaranteed. Because 
 *    we queue the left child before the right child, left-to-right is guaranteed. 
 *    To avoid a TreeMap, we just keep track of `minCol` and `maxCol` as we go, and iterate 
 *    from `minCol` to `maxCol` to build the final list.
 * 3. Time Complexity: O(N) — Every node is queued once, dequeued once, and inserted into 
 *    a HashMap in O(1) time. Constructing the final list takes O(Width), which is at most O(N).
 * 4. Space Complexity: O(N) — The Queue holds up to N/2 nodes at the bottom level, and the 
 *    HashMap holds exactly N node values.
 * 
 * [Which one I'd write in an interview]
 * Approach 2. It shows that you understand the fundamental behavioral difference between 
 * BFS and DFS and how BFS perfectly maps to coordinate-based ordering without needing to sort.
 * 
 * ============================================================================
 * 3. EDGE CASES
 * ============================================================================
 * - Right-Skewed Tree (Linked List): E.g., `1 -> right:2 -> right:3`. 
 *   The `minCol` stays at 0, while `maxCol` reaches 2. Code handles the boundary naturally.
 * - Perfect Overlap (The "Diamond"): Node A's right child and Node B's left child land on 
 *   the exact same X/Y coordinate. BFS processes Node A before Node B (left-to-right), so 
 *   A's child enters the Queue before B's child. Ordering is preserved perfectly.
 * 
 * ============================================================================
 * 4. KEY INSIGHT, DIAGRAMS & DRY RUN
 * ============================================================================
 * [Key Insight]
 * Don't rely on sorting if the graph traversal algorithm can sort it for you geometrically. 
 * BFS is inherently a spatial traversal (rippling outward and downward).
 * 
 * [Examples & Diagram]
 * Let's look at the "Diamond" overlap:
 *          1   (Col 0)
 *        /   \
 *      2       3
 *       \     /
 *        4   5
 * Both 4 and 5 land at Column 0, Row 2. Because BFS visits 2 before 3, 4 is enqueued 
 * before 5. The list for Col 0 becomes [1, 4, 5].
 * 
 * [Dry Run (BFS)]
 * Init: Queue = [(Node:1, Col:0)], Map = {}, minCol = 0, maxCol = 0
 * 
 * Iteration 1 (Level 0):
 * - Pop (1, 0). Add to Map[0]. Map = {0: [1]}.
 * - Push Left: (2, -1). minCol becomes -1.
 * - Push Right: (3, 1). maxCol becomes 1.
 * 
 * Iteration 2 (Level 1):
 * - Pop (2, -1). Add to Map[-1]. Map = {-1: [2], 0: [1]}.
 * - Push Right: (4, 0). (Notice we queue this first!)
 * - Pop (3, 1). Add to Map[1]. Map = {-1: [2], 0: [1], 1: [3]}.
 * - Push Left: (5, 0).
 * 
 * Iteration 3 (Level 2):
 * - Pop (4, 0). Add to Map[0]. Map[0] is now [1, 4].
 * - Pop (5, 0). Add to Map[0]. Map[0] is now [1, 4, 5].
 * 
 * Final extraction from minCol (-1) to maxCol (1):
 * Col -1 -> [2]
 * Col  0 -> [1, 4, 5]
 * Col  1 -> [3]
 * 
 * [Pitfalls]
 * - Thinking you need a `TreeMap` to keep the columns ordered. While it works, it degrades 
 *   performance to $O(N \log N)$. Min/Max pointers with a HashMap keep it $O(N)$.
 * - Using DFS without tracking rows. If you just use DFS and append to a list, deeper 
 *   nodes will appear before shallower nodes in the same column!
 * 
 * [Pattern Recognition]
 * When you see: "Columns", "Top to Bottom", "Same Row/Col -> Left to Right".
 * Think: Breadth-First Search (BFS) tracking an X-coordinate modifier (-1 for left, +1 for right).
 * 
 * [Interview Script]
 * "Since the problem asks for nodes from top to bottom, a Breadth-First Search is the most 
 * natural fit. I'll maintain a queue that stores both the node and its column index. Starting 
 * with the root at column 0, its left child is at column -1 and right child is at +1. I'll 
 * collect these in a HashMap grouped by column. By tracking the minimum and maximum column 
 * indices during the traversal, I can easily construct the final list from left to right 
 * in strict O(N) time without ever needing to sort anything."
 * 
 * ============================================================================
 * 5. FOLLOW-UPS
 * ============================================================================
 * Q: What if the constraints were N <= 10^5, and hashing collisions in HashMap became a bottleneck?
 * A: Because we know the max width of a tree is bounded by N, we can use an Array instead of a HashMap! 
 *    We declare an array of Lists of size `2 * N + 1`. We offset all column calculations by `+N` 
 *    so the root is at index `N`. This gives us raw memory access and entirely skips hashing overhead.
 * 
 * Q: How does this change if the problem states: "If nodes share a row and column, sort them by value" 
 *    (This is LeetCode 987 - Hard)?
 * A: BFS is no longer enough to guarantee order because we must inject a value-sort rule. We would 
 *    have to store `(row, val)` pairs in our lists. After the traversal, we would iterate through 
 *    each column and run a custom sort: first by row (ascending), then by value (ascending).
 * 
 * ============================================================================
 * 6. JAVA CODE
 * ============================================================================
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class BinaryTreeVerticalOrder {

    // Standard Binary Tree Node definition
    public static class TreeNode {
        int data;
        TreeNode left;
        TreeNode right;
        TreeNode(int data) { this.data = data; }
    }

    /**
     * Helper class to bind a TreeNode to its computed column coordinate.
     * This avoids maintaining two separate parallel queues.
     */
    private static class NodeColumn {
        TreeNode node;
        int col;

        NodeColumn(TreeNode node, int col) {
            this.node = node;
            this.col = col;
        }
    }

    /**
     * Executes the vertical order traversal optimally using BFS and Min/Max trackers.
     * 
     * @param root The root of the binary tree.
     * @return A list of vertical columns, ordered left to right.
     */
    public static List<List<Integer>> getVerticalOrder(TreeNode root) {
        List<List<Integer>> result = new ArrayList<>();
        
        // Defensive check: Even though constraints say [1, 500] nodes, 
        // fast-failing on null is a standard engineering best practice.
        if (root == null) {
            return result;
        }

        // HashMap to group all node values that share the same vertical column.
        // E.g., columnMap.get(0) holds all nodes directly under the root.
        Map<Integer, List<Integer>> columnMap = new HashMap<>();
        
        // BFS Queue containing our wrapped nodes
        Queue<NodeColumn> queue = new LinkedList<>();
        queue.offer(new NodeColumn(root, 0));

        // Trackers for our iteration bounds. This allows us to use a fast HashMap 
        // instead of a slow TreeMap.
        int minCol = 0;
        int maxCol = 0;

        while (!queue.isEmpty()) {
            NodeColumn current = queue.poll();
            TreeNode node = current.node;
            int col = current.col;

            // Dynamically create the list for this column if it's the first time we see it.
            // 'computeIfAbsent' is an idiomatic and clean Java way to do this.
            columnMap.computeIfAbsent(col, k -> new ArrayList<>()).add(node.data);

            // Queue up the left child. It shifts one column to the left (-1).
            if (node.left != null) {
                queue.offer(new NodeColumn(node.left, col - 1));
                minCol = Math.min(minCol, col - 1); // Expand left boundary if needed
            }

            // Queue up the right child. It shifts one column to the right (+1).
            if (node.right != null) {
                queue.offer(new NodeColumn(node.right, col + 1));
                maxCol = Math.max(maxCol, col + 1); // Expand right boundary if needed
            }
        }

        // Construct the final result by iterating exactly from the leftmost column
        // we discovered to the rightmost column.
        for (int i = minCol; i <= maxCol; i++) {
            // We guarantee every integer between min and max exists in the map because
            // a continuous tree cannot skip column indices geometrically.
            result.add(columnMap.get(i));
        }

        return result;
    }

    // ============================================================================
    // TESTING & CROSS-CHECKING
    // ============================================================================
    public static void main(String[] args) {
        // Test Case 1: The Diamond Overlap (from Dry Run)
        //          1 
        //        /   \
        //       2     3 
        //        \   / 
        //         4 5 
        TreeNode root1 = new TreeNode(1);
        root1.left = new TreeNode(2);
        root1.right = new TreeNode(3);
        root1.left.right = new TreeNode(4);
        root1.right.left = new TreeNode(5);
        
        System.out.println("Test 1 (Diamond Overlap): " + getVerticalOrder(root1)); 
        // Expected: [[2], [1, 4, 5], [3]]
        // Notice 4 comes before 5, preserving left-to-right discovery!

        // Test Case 2: Standard wide tree
        //          3
        //        /   \
        //       9     20
        //            /  \
        //           15   7
        TreeNode root2 = new TreeNode(3);
        root2.left = new TreeNode(9);
        root2.right = new TreeNode(20);
        root2.right.left = new TreeNode(15);
        root2.right.right = new TreeNode(7);

        System.out.println("Test 2 (Standard): " + getVerticalOrder(root2));
        // Expected: [[9], [3, 15], [20], [7]]
        
        // Test Case 3: Single Node Edge Case (Minimum constraint bound)
        TreeNode root3 = new TreeNode(42);
        System.out.println("Test 3 (Single Node): " + getVerticalOrder(root3)); 
        // Expected: [[42]]
        
        // Test Case 4: Extreme Skew (Linked List style)
        //   10
        //     \
        //      20
        //        \
        //         30
        TreeNode root4 = new TreeNode(10);
        root4.right = new TreeNode(20);
        root4.right.right = new TreeNode(30);
        System.out.println("Test 4 (Right Skew): " + getVerticalOrder(root4));
        // Expected: [[10], [20], [30]]
    }
}

/**
 * ============================================================================
 * 7. SUMMARY
 * ============================================================================
 * - Core pattern: Breadth-First Search (BFS) combined with 1D X-coordinate tracking.
 * - Key observation: BFS naturally orders elements Top-to-Bottom and Left-to-Right. 
 *   Therefore, grouping nodes by column during a standard BFS inherently fulfills 
 *   all sorting requirements without actually running a sort function!
 * - Most common trap: Falling back to DFS. DFS processes deep nodes before shallow nodes, 
 *   which completely destroys the "top-to-bottom" requirement, forcing you to track 
 *   row integers and run heavy sorting operations.
 * - Mental trigger: "Vertical ordering" -> "BFS + Column Coordinate Modifier".
 */

import java.util.*;

/**
 * Vertical Order Traversal (BFS - Optimal)
 *
 * ---------------------------------------
 * 🧠 CORE IDEA:
 * ---------------------------------------
 * Every node is assigned a "column index" (also called horizontal distance).
 *
 *        root → column = 0
 *        left child  → column - 1
 *        right child → column + 1
 *
 * Example:
 *              3(0)
 *            /     \
 *         9(-1)   20(+1)
 *                /     \
 *             15(0)    7(+2)
 *
 * ---------------------------------------
 * 🎯 WHAT WE NEED:
 * ---------------------------------------
 * 1. Group nodes by column
 * 2. Traverse columns from left → right
 * 3. Within each column → nodes should be:
 *      ✔ top → bottom
 *      ✔ left → right (if same row)
 *
 * ---------------------------------------
 * ❗ WHY BFS (LEVEL ORDER)?
 * ---------------------------------------
 * BFS guarantees:
 *   ✔ Top → Bottom order
 *   ✔ Left → Right order (queue processing)
 *
 * DFS CANNOT guarantee this ordering without sorting.
 *
 * ---------------------------------------
 * 🧱 DATA STRUCTURES:
 * ---------------------------------------
 * Map<Integer, List<Integer>> columnMap
 *     → Stores nodes grouped by column
 *
 * Queue<Pair(node, column)>
 *     → Standard BFS traversal
 *
 * minCol, maxCol
 *     → Track range of columns to avoid sorting later
 *
 * ---------------------------------------
 * ⏱ COMPLEXITY:
 * ---------------------------------------
 * Time  : O(N)  → each node visited once
 * Space : O(N)  → map + queue
 *
 */
public class VerticalOrderTraversal {

    /**
     * Basic Tree Node definition
     */
    static class TreeNode {
        int val;
        TreeNode left, right;

        TreeNode(int val) {
            this.val = val;
        }
    }

    /**
     * Pair of (node, column)
     *
     * Why needed?
     * → Because during BFS, we must carry column info along with each node
     */
    static class Pair {
        TreeNode node;
        int col;

        Pair(TreeNode node, int col) {
            this.node = node;
            this.col = col;
        }
    }

    public List<List<Integer>> verticalOrder(TreeNode root) {

        // Final answer
        List<List<Integer>> result = new ArrayList<>();

        // Edge case
        if (root == null) return result;

        /**
         * columnMap:
         * key   → column index
         * value → list of node values in BFS order
         *
         * Example:
         *  col = -1 → [9]
         *  col =  0 → [3, 15]
         *  col =  1 → [20]
         */
        Map<Integer, List<Integer>> columnMap = new HashMap<>();

        /**
         * BFS Queue
         *
         * IMPORTANT INVARIANT:
         * Queue always contains nodes in level-order sequence
         */
        Queue<Pair> queue = new LinkedList<>();

        // Start with root at column 0
        queue.offer(new Pair(root, 0));

        /**
         * Track column boundaries
         * This avoids sorting keys later (optimization)
         */
        int minCol = 0;
        int maxCol = 0;

        /**
         * -------------------------
         * 🚀 BFS TRAVERSAL STARTS
         * -------------------------
         */
        while (!queue.isEmpty()) {

            // Remove front element (FIFO)
            Pair current = queue.poll();

            TreeNode node = current.node;
            int col = current.col;

            /**
             * STEP 1: Put node into its column bucket
             *
             * computeIfAbsent:
             * - If column not present → create new list
             * - Else → reuse existing list
             */
            columnMap
                .computeIfAbsent(col, k -> new ArrayList<>())
                .add(node.val);

            /**
             * STEP 2: Update column range
             */
            minCol = Math.min(minCol, col);
            maxCol = Math.max(maxCol, col);

            /**
             * STEP 3: Push children into queue
             *
             * VERY IMPORTANT ORDER:
             * → Left first, then Right
             *
             * WHY?
             * To maintain left-to-right ordering when nodes
             * share the same row and column
             */
            if (node.left != null) {
                queue.offer(new Pair(node.left, col - 1));
            }

            if (node.right != null) {
                queue.offer(new Pair(node.right, col + 1));
            }
        }

        /**
         * -------------------------
         * 🧾 BUILD FINAL RESULT
         * -------------------------
         *
         * We iterate from leftmost column → rightmost column
         *
         * Why this works?
         * Because we tracked minCol and maxCol
         */
        for (int col = minCol; col <= maxCol; col++) {
            result.add(columnMap.get(col));
        }

        return result;
    }
}

import java.util.*;

/**
 * LeetCode 987 - Vertical Order Traversal of Binary Tree
 *
 * ---------------------------------------
 * 🧠 CORE IDEA:
 * ---------------------------------------
 * We must sort nodes by:
 *
 * 1. Column (ascending)
 * 2. Row (ascending)
 * 3. Value (ascending if same row & col)
 *
 * ---------------------------------------
 * ❗ WHY BFS IS NOT ENOUGH?
 * ---------------------------------------
 * BFS ensures row order but DOES NOT guarantee:
 * → correct ordering when nodes share same (row, col)
 *
 * So we need explicit sorting.
 *
 * ---------------------------------------
 * 🧱 DATA STRUCTURE:
 * ---------------------------------------
 * TreeMap<col, TreeMap<row, PriorityQueue<values>>>
 *
 * colMap:
 *   key   → column
 *   value → rowMap
 *
 * rowMap:
 *   key   → row
 *   value → minHeap (sorted values)
 *
 * ---------------------------------------
 * ⏱ COMPLEXITY:
 * ---------------------------------------
 * Time  : O(N log N)
 * Space : O(N)
 *
 */
public class VerticalTraversal987 {

    static class TreeNode {
        int val;
        TreeNode left, right;

        TreeNode(int val) {
            this.val = val;
        }
    }

    public List<List<Integer>> verticalTraversal(TreeNode root) {

        /**
         * col → row → minHeap(values)
         */
        TreeMap<Integer, TreeMap<Integer, PriorityQueue<Integer>>> colMap =
                new TreeMap<>();

        // Start DFS
        dfs(root, 0, 0, colMap);

        /**
         * Build result
         */
        List<List<Integer>> result = new ArrayList<>();

        for (TreeMap<Integer, PriorityQueue<Integer>> rowMap : colMap.values()) {

            List<Integer> column = new ArrayList<>();

            for (PriorityQueue<Integer> pq : rowMap.values()) {

                // Extract sorted values
                while (!pq.isEmpty()) {
                    column.add(pq.poll());
                }
            }

            result.add(column);
        }

        return result;
    }

    /**
     * DFS traversal
     *
     * row → depth (top = 0)
     * col → horizontal distance
     */
    private void dfs(TreeNode node,
                     int row,
                     int col,
                     TreeMap<Integer, TreeMap<Integer, PriorityQueue<Integer>>> colMap) {

        if (node == null) return;

        /**
         * Insert into structure:
         *
         * col → row → values
         */
        colMap
            .computeIfAbsent(col, c -> new TreeMap<>())
            .computeIfAbsent(row, r -> new PriorityQueue<>())
            .offer(node.val);

        /**
         * Traverse children
         */
        dfs(node.left, row + 1, col - 1, colMap);
        dfs(node.right, row + 1, col + 1, colMap);
    }
}
