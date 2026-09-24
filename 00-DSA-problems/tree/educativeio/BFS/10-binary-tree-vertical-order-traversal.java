/**
 * ============================================================================
 * 0. PROBLEM STATEMENT
 * ============================================================================
 * Given the root of a binary tree, return the vertical order traversal of its 
 * nodes’ values, organized column by column from top to bottom.
 * 
 * For any two nodes that share the same row and column, they should appear 
 * from left to right in the output.
 * 
 * Constraints:
 * - The number of nodes in the tree is in the range [0, 100].
 * - -100 <= Node.val <= 100
 * 
 * ============================================================================
 * 1. CLARIFYING QUESTIONS
 * ============================================================================
 * - "If the tree is completely empty, what should be returned?"
 *   An empty list `[]`. This prevents NullPointerExceptions right off the bat.
 * - "When nodes overlap in the exact same row and column, do we sort them by value?"
 *   The problem says "left to right". This is crucial! It means we DO NOT sort by value, 
 *   we just keep their natural geographical order.
 * - "Do the column indices have to be strictly positive?"
 *   No, going left from the root (column 0) will naturally result in negative columns.
 *   We just need to return them ordered from the most negative to the most positive.
 * 
 * ============================================================================
 * 2. THE REASONING JOURNEY
 * ============================================================================
 * [Binding Constraint] 
 * A binary tree only knows "parent-child" (vertical) and "left-right" relationships. 
 * To group nodes by columns, we must invent a 2D coordinate system on top of the tree 
 * where the root is at column 0, going left is `col - 1`, and going right is `col + 1`.
 * 
 * --- APPROACH 1: Depth-First Search (DFS) with a TreeMap ---
 * 1. What I'd naturally try: Write a recursive DFS function. Pass a `col` integer down. 
 *    As I visit nodes, I store them in a `TreeMap<Integer, List<Integer>>` where the key 
 *    is the column index. 
 * 2. Why it works: The DFS will eventually visit every node and group them by column. 
 *    The TreeMap automatically keeps the columns sorted from minimum to maximum.
 * 3. Why it's flawed: DFS goes deep before it goes wide. It might put a node from depth 10 
 *    into column 0's list *before* it puts a node from depth 2 into column 0's list. 
 *    To fix this, we'd have to store the `row` as well and do complex sorting at the end.
 * 4. What work is repeated: If we use DFS, we lose the natural top-to-bottom ordering, 
 *    forcing us to do a heavy $O(N \log N)$ sort on every single column's list at the end.
 * 5. Time Complexity: O(N log N) — because sorting nodes by their row indices takes extra time, 
 *    and inserting into a TreeMap takes logarithmic time per node.
 * 6. Space Complexity: O(N) — to store all nodes in the map and recursion stack.
 * 
 * [The Core Observation]
 * We need an algorithm that naturally discovers nodes **top-to-bottom** (row by row), and 
 * **left-to-right** within those rows. Breadth-First Search (BFS) does EXACTLY this! 
 * If we use BFS, we don't need to track rows or sort anything. The discovery order 
 * is already the exact final order the problem demands.
 * 
 * --- APPROACH 2: Breadth-First Search (BFS) with Min/Max Tracking (The Optimal Way) ---
 * 1. How it works: Use a Queue that stores both the `TreeNode` and its `column` index. 
 *    Use a standard `HashMap` to group nodes by column. As we do BFS, we just track the 
 *    `minCol` and `maxCol` we've seen. 
 * 2. Why it's optimal: BFS guarantees top-to-bottom ordering. Enqueueing left children 
 *    before right children guarantees left-to-right ordering. Tracking `minCol`/`maxCol` 
 *    eliminates the need for a TreeMap, letting us just iterate from min to max at the end.
 * 3. Time Complexity: O(N) — because every node is enqueued once, dequeued once, inserted 
 *    into a HashMap in O(1) time, and our final result construction iterates from minCol to 
 *    maxCol (which is bounded by N). No sorting is ever required.
 * 4. Space Complexity: O(N) — because the Queue at the bottom level holds up to N/2 nodes, 
 *    and the HashMap holds exactly N nodes. No recursion stack overhead.
 * 
 * [Which one I'd write in an interview]
 * Approach 2 (BFS). It perfectly leverages the algorithmic properties of BFS to avoid 
 * sorting entirely. It's incredibly clean and runs in strict O(N) time.
 * 
 * ============================================================================
 * 3. EDGE CASES
 * ============================================================================
 * - Empty Tree: Fast-fail, returning an empty list.
 * - Skewed Tree (Linked List): E.g., every node only has a left child. `minCol` will 
 *   reach -N, `maxCol` stays 0. Handled flawlessly.
 * - The "Criss-Cross" Overlap: A node's right child and another node's left child 
 *   occupy the exact same (row, col) coordinate. BFS strictly evaluates the left parent 
 *   before the right parent, ensuring the left-most node gets added to the list first.
 * 
 * ============================================================================
 * 4. KEY INSIGHT, DIAGRAMS & DRY RUN
 * ============================================================================
 * [Key Insight]
 * A HashMap + Min/Max Trackers is always faster than a TreeMap if the keys are dense 
 * integers. We trade $O(\log N)$ insertions for $O(1)$ insertions.
 * 
 * [Examples & Diagram]
 * Tree:
 *          3  (Col 0)
 *        /   \
 * (-1)  9     8  (Col 1)
 *            / \
 *      (0)  4   7  (Col 2)
 * 
 * Notice that both 3 and 4 are in Column 0. 
 * 3 is row 0, 4 is row 2. They must appear as [3, 4].
 * 
 * [Dry Run (BFS)]
 * Init: Queue = [(Node:3, Col:0)], Map = {}, min = 0, max = 0
 * 
 * Iteration 1:
 * - Pop (3, 0). 
 * - Add 3 to Map[0]. Map = {0: [3]}.
 * - Push Left: (9, -1). Update min = -1.
 * - Push Right: (8, 1). Update max = 1.
 * 
 * Iteration 2:
 * - Pop (9, -1). 
 * - Add 9 to Map[-1]. Map = {0: [3], -1: [9]}.
 * - No children.
 * 
 * Iteration 3:
 * - Pop (8, 1). 
 * - Add 8 to Map[1]. Map = {0: [3], -1: [9], 1: [8]}.
 * - Push Left: (4, 0). 
 * - Push Right: (7, 2). Update max = 2.
 * 
 * Iteration 4:
 * - Pop (4, 0).
 * - Add 4 to Map[0]. Map = {0: [3, 4], -1: [9], 1: [8]}.  <-- Notice how 4 simply appends after 3!
 * 
 * Loop Ends. 
 * Iterate from min (-1) to max (2):
 * Col -1: [9]
 * Col  0: [3, 4]
 * Col  1: [8]
 * Col  2: [7]
 * Final Output: [[9], [3, 4], [8], [7]]
 * 
 * [Pitfalls]
 * - Trying to use a single Queue by mixing TreeNode objects and Integer objects. 
 *   While possible in Java with `Queue<Object>`, it requires nasty type-casting. 
 *   Always use a small helper class (e.g., `ColumnNode`) or two parallel queues.
 * 
 * [Pattern Recognition]
 * When you see: "Top to bottom", "Left to Right".
 * Think: Breadth-First Search (BFS).
 * When you see: "Vertical", "Columns".
 * Think: Horizontal coordinate tracking (Left = -1, Right = +1).
 * 
 * ============================================================================
 * 5. FOLLOW-UPS
 * ============================================================================
 * Q: What if the problem changed so that overlapping nodes in the exact same spot 
 *    must be sorted by their VALUE instead of left-to-right? (LeetCode 987)
 * A: Our natural BFS queue order is no longer enough. We would need to track the `row` 
 *    coordinate as well. Instead of storing just a list of values in the map, we'd store 
 *    a list of `(row, value)` pairs. After the traversal, for each column, we would 
 *    sort the list with a custom comparator: first by row, then by value.
 * 
 * Q: Can we do this without a HashMap to save hashing overhead?
 * A: Yes! We can do a quick preliminary DFS/BFS just to find the `minCol` and `maxCol`. 
 *    Once we know the total width, we can create an array of `ArrayList`s: 
 *    `List<Integer>[] cols = new ArrayList[maxCol - minCol + 1];`
 *    Then we do the BFS, calculating the array index as `currentCol - minCol`.
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

public class VerticalOrderTraversal {

    // Standard Binary Tree Node definition
    public static class TreeNode {
        int val;
        TreeNode left;
        TreeNode right;
        TreeNode(int val) { this.val = val; }
    }

    /**
     * Helper class to bind a TreeNode to its computed column index.
     * This avoids maintaining two separate parallel queues.
     */
    private static class ColumnNode {
        TreeNode node;
        int col;

        ColumnNode(TreeNode node, int col) {
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
    public static List<List<Integer>> verticalOrder(TreeNode root) {
        List<List<Integer>> result = new ArrayList<>();
        
        // Edge Case: Empty tree. Fast-fail and return empty list.
        if (root == null) {
            return result;
        }

        // Map to group all node values that share the same column index.
        // e.g., columnMap.get(0) will hold all nodes falling in the center vertical line.
        Map<Integer, List<Integer>> columnMap = new HashMap<>();
        
        // BFS Queue containing our wrapped nodes
        Queue<ColumnNode> queue = new LinkedList<>();
        queue.offer(new ColumnNode(root, 0));

        // Trackers for our iteration bounds so we don't need a sorted TreeMap
        int minCol = 0;
        int maxCol = 0;

        while (!queue.isEmpty()) {
            ColumnNode current = queue.poll();
            TreeNode node = current.node;
            int col = current.col;

            // Dynamically create the list for this column if it's the first time we see it
            // 'computeIfAbsent' is an idiomatic and clean Java way to do this.
            columnMap.computeIfAbsent(col, k -> new ArrayList<>()).add(node.val);

            // Queue up the left child. It shifts one column to the left (-1).
            if (node.left != null) {
                queue.offer(new ColumnNode(node.left, col - 1));
                minCol = Math.min(minCol, col - 1); // Expand left boundary if needed
            }

            // Queue up the right child. It shifts one column to the right (+1).
            if (node.right != null) {
                queue.offer(new ColumnNode(node.right, col + 1));
                maxCol = Math.max(maxCol, col + 1); // Expand right boundary if needed
            }
        }

        // Construct the final result by iterating exactly from the leftmost column
        // we discovered to the rightmost column. 
        for (int i = minCol; i <= maxCol; i++) {
            // We guarantee every integer between min and max exists in the map because
            // tree structures branch out continuously without skipping column integers.
            result.add(columnMap.get(i));
        }

        return result;
    }

    // ============================================================================
    // TESTING & CROSS-CHECKING
    // ============================================================================
    public static void main(String[] args) {
        // Test Case 1: Standard overlapping tree (from the dry run)
        //          3 
        //        /   \
        //       9     8 
        //            / \
        //           4   7 
        TreeNode root1 = new TreeNode(3);
        root1.left = new TreeNode(9);
        root1.right = new TreeNode(8);
        root1.right.left = new TreeNode(4);
        root1.right.right = new TreeNode(7);
        
        System.out.println("Test 1 (Overlapping center): " + verticalOrder(root1)); 
        // Expected: [[9], [3, 4], [8], [7]]
        // Notice 4 comes after 3 because of top-to-bottom BFS guarantees.

        // Test Case 2: Criss-Cross exactly on the same spot
        //          1
        //        /   \
        //       2     3
        //        \   /
        //         4 5
        // Both 4 and 5 are in column 0. Node 4 should appear before Node 5.
        TreeNode root2 = new TreeNode(1);
        root2.left = new TreeNode(2);
        root2.right = new TreeNode(3);
        root2.left.right = new TreeNode(4);
        root2.right.left = new TreeNode(5);

        System.out.println("Test 2 (Exact row/col overlap): " + verticalOrder(root2));
        // Expected: [[2], [1, 4, 5], [3]]
        
        // Test Case 3: Empty Tree Edge Case
        System.out.println("Test 3 (Empty): " + verticalOrder(null)); 
        // Expected: []
        
        // Test Case 4: Extreme Skew (Linked List style)
        //   1
        //  /
        // 2
        //  \
        //   3
        TreeNode root4 = new TreeNode(1);
        root4.left = new TreeNode(2);
        root4.left.right = new TreeNode(3);
        System.out.println("Test 4 (ZigZag): " + verticalOrder(root4));
        // Expected: [[2], [1, 3]]
    }
}

/**
 * ============================================================================
 * 7. SUMMARY
 * ============================================================================
 * - Core pattern: Breadth-First Search (BFS) combined with 1D horizontal coordinates.
 * - Key observation: BFS naturally orders elements Top-to-Bottom and Left-to-Right. 
 *   Therefore, if we just bin nodes by column during a BFS, they are already perfectly sorted!
 * - Most common trap: Using DFS, which destroys the top-to-bottom ordering and forces 
 *   you to track rows and run heavy sorting algorithms.
 * - Mental trigger: "Vertical alignment" -> "X-coordinate tracking" + "Queue/BFS".
 */


import java.util.*;

/**
 * Vertical Order Traversal using BFS (BEST APPROACH)
 *
 * ─────────────────────────────────────────────────────────────
 * 🧠 CORE IDEA
 * ─────────────────────────────────────────────────────────────
 * Each node is assigned a "column index" (horizontal distance):
 *
 *          col = 0 (root)
 *         /          \
 *    col = -1      col = +1
 *
 * We group nodes by column.
 *
 * ─────────────────────────────────────────────────────────────
 * ❗ WHY BFS?
 * ─────────────────────────────────────────────────────────────
 * Problem requirement:
 * 1. Top → Bottom
 * 2. Same row → Left → Right
 *
 * BFS automatically guarantees BOTH:
 * ✔ Level order (top to bottom)
 * ✔ Left processed before right (queue order)
 *
 * DFS cannot guarantee this without extra sorting.
 *
 * ─────────────────────────────────────────────────────────────
 * ⏱️ Time Complexity:  O(N)
 * 🧠 Space Complexity: O(N)
 */
public class VerticalOrderTraversal {

    // Standard Binary Tree Node
    static class TreeNode {
        int val;
        TreeNode left, right;

        TreeNode(int val) {
            this.val = val;
        }
    }

    /**
     * Main function
     */
    public List<List<Integer>> verticalOrder(TreeNode root) {

        // Final answer
        List<List<Integer>> result = new ArrayList<>();

        // Edge case: empty tree
        if (root == null) return result;

        // Map: column -> list of values in that column
        Map<Integer, List<Integer>> columnTable = new HashMap<>();

        // Queue for BFS
        // Each entry holds: (node, columnIndex)
        Deque<Pair> queue = new ArrayDeque<>();

        // Start BFS from root at column = 0
        queue.offer(new Pair(root, 0));

        // Track column boundaries to avoid sorting later
        int minCol = 0;
        int maxCol = 0;

        // ─────────────────────────────────────────────
        // BFS TRAVERSAL
        // ─────────────────────────────────────────────
        while (!queue.isEmpty()) {

            Pair current = queue.poll();

            TreeNode node = current.node;
            int col = current.col;

            // STEP 1: Add current node to its column bucket
            columnTable
                    .computeIfAbsent(col, k -> new ArrayList<>())
                    .add(node.val);

            // STEP 2: Update column boundaries
            minCol = Math.min(minCol, col);
            maxCol = Math.max(maxCol, col);

            // STEP 3: Push children into queue
            // IMPORTANT:
            // Left is pushed BEFORE right → ensures left-to-right order
            if (node.left != null) {
                queue.offer(new Pair(node.left, col - 1));
            }

            if (node.right != null) {
                queue.offer(new Pair(node.right, col + 1));
            }
        }

        // ─────────────────────────────────────────────
        // BUILD RESULT (leftmost column → rightmost)
        // ─────────────────────────────────────────────
        for (int col = minCol; col <= maxCol; col++) {
            result.add(columnTable.get(col));
        }

        return result;
    }

    /**
     * Helper structure to hold node + column
     *
     * Using record (Java 16+) → cleaner & immutable
     */
    record Pair(TreeNode node, int col) {}
}

import java.util.*;

/**
 * Vertical Order Traversal using DFS + Sorting
 *
 * ─────────────────────────────────────────────────────────────
 * 🧠 IDEA
 * ─────────────────────────────────────────────────────────────
 * DFS does NOT preserve:
 * - level order (top → bottom)
 * - left → right order
 *
 * So we store extra information:
 *      (column, row, value)
 *
 * Then sort:
 * 1. column
 * 2. row
 *
 * ─────────────────────────────────────────────────────────────
 * ⏱️ Time Complexity:  O(N log N)  (sorting)
 * 🧠 Space Complexity: O(N)
 */
public class VerticalOrderDFS {

    static class TreeNode {
        int val;
        TreeNode left, right;

        TreeNode(int val) {
            this.val = val;
        }
    }

    /**
     * Entry function
     */
    public List<List<Integer>> verticalOrder(TreeNode root) {

        // Stores all nodes with coordinates
        List<NodeInfo> nodes = new ArrayList<>();

        // Step 1: DFS traversal
        dfs(root, 0, 0, nodes);

        // Step 2: Sort nodes
        nodes.sort((a, b) -> {
            // First sort by column
            if (a.col != b.col) return Integer.compare(a.col, b.col);

            // If same column → sort by row (top to bottom)
            return Integer.compare(a.row, b.row);
        });

        // Step 3: Build result
        List<List<Integer>> result = new ArrayList<>();

        int prevCol = Integer.MIN_VALUE;

        for (NodeInfo node : nodes) {

            // New column → create new list
            if (node.col != prevCol) {
                result.add(new ArrayList<>());
                prevCol = node.col;
            }

            // Add value to latest column list
            result.get(result.size() - 1).add(node.val);
        }

        return result;
    }

    /**
     * DFS traversal
     *
     * col → horizontal position
     * row → depth (level)
     */
    private void dfs(TreeNode node, int col, int row, List<NodeInfo> nodes) {

        // Base case
        if (node == null) return;

        // Record current node
        nodes.add(new NodeInfo(col, row, node.val));

        // Traverse left
        dfs(node.left, col - 1, row + 1, nodes);

        // Traverse right
        dfs(node.right, col + 1, row + 1, nodes);
    }

    /**
     * Helper class
     */
    static class NodeInfo {
        int col, row, val;

        NodeInfo(int col, int row, int val) {
            this.col = col;
            this.row = row;
            this.val = val;
        }
    }
}
