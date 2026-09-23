/**
 * ============================================================================
 * 1. CLARIFYING QUESTIONS
 * ============================================================================
 * - "Is the output strictly 'None' for an empty tree, or an empty string?" 
 *   Prevents edge-case failures if the interviewer's test script strictly matches "None".
 * - "Do we need to worry about trailing colons in the output?"
 *   Changes how we format the string (e.g., `1:2:3:` vs `1:2:3`).
 * - "Can the tree contain duplicate values, and does that affect formatting?"
 *   Confirms we just append blindly and don't need a HashSet to deduplicate.
 * - "Are we constrained by memory if the tree is massively wide?"
 *   Helps gauge if a standard Queue is acceptable or if we need to discuss disk-backed queues.
 * 
 * ============================================================================
 * 2. THE REASONING JOURNEY
 * ============================================================================
 * [Binding Constraint] 
 * The tree's physical links traverse vertically (parent to child), but the requested 
 * output groups nodes horizontally (sibling to sibling). We have to bridge this dimensional gap.
 * 
 * --- APPROACH 1: DFS Preorder with Level Bucketing (The "Natural Try") ---
 * 1. What I'd naturally try: Traverse the tree using standard recursion (DFS). Pass a `level` 
 *    integer down. Whenever I reach a node at `level X`, I add its value to a `List` stored 
 *    in an array or HashMap at index `X`. 
 * 2. Why it works: DFS guarantees we visit every node. By carrying the depth along, we can 
 *    reconstruct the horizontal layers after the traversal is finished.
 * 3. Why it's costly: We can't build the final string in a single pass. We are forced to build 
 *    an intermediate data structure (lists of lists), and then do a *second* pass over that 
 *    structure to join everything with colons. 
 * 4. What work is being repeated: We iterate over the nodes to group them, and then iterate 
 *    over the grouped nodes again to serialize them.
 * 5. Time Complexity: O(N) — because we visit each of the N nodes once during DFS, and then process 
 *    all N nodes again to construct the string.
 * 6. Space Complexity: O(N) — because we store all N nodes in intermediate lists (O(N) auxiliary space) 
 *    AND we consume up to O(N) space on the call stack for a degenerate, skewed tree.
 * 
 * [The Core Observation]
 * We don't actually need to know the depth integer. We just need a way to say: "Process this node 
 * now, but put its children at the back of the line so they wait until all my peers are processed." 
 * A First-In-First-Out (FIFO) data structure exactly models this "wait in line" behavior.
 * 
 * --- APPROACH 2: Iterative BFS with a Queue (The Optimal Way) ---
 * 1. How it works: Put the root in a Queue. Loop while the Queue isn't empty: pop a node, append 
 *    its value to a StringBuilder, and push its left and right children to the back of the Queue.
 * 2. Time Complexity: O(N) — because every node is enqueued exactly once, dequeued exactly once, 
 *    and appended to the StringBuilder in amortized O(1) time.
 * 3. Space Complexity: O(N) — because the Queue at its maximum holds the widest level of the tree. 
 *    In a perfectly balanced binary tree, the leaf level contains roughly N/2 nodes. The StringBuilder 
 *    also holds an O(N) length string. No recursion stack overhead is used.
 * 
 * [Which one I'd write in an interview]
 * Approach 2 (BFS). It allows us to build the string on the fly without intermediate bucket 
 * structures. It perfectly maps to the "horizontal" nature of the problem, and is less prone to 
 * StackOverflow exceptions on deep trees compared to recursive DFS.
 * 
 * ============================================================================
 * 3. EDGE CASES
 * ============================================================================
 * - Empty Tree: Root is null. Must return exactly "None".
 * - Single Node Tree: Root with no children. Should just return "val" (no colons).
 * - Extreme Skew (Linked-List Tree): Every node only has a left child. Tests if we handle 
 *   narrow trees well (Queue size never exceeds 1).
 * - Negative Values: Tests if string formatting breaks on negative signs (e.g., "-1:-5").
 * 
 * ============================================================================
 * 4. KEY INSIGHT, DIAGRAMS & DRY RUN
 * ============================================================================
 * [Key Insight]
 * A Queue flattens a hierarchy into a chronological sequence based on discovery time. Because 
 * parents discover children, parents always enter the Queue before them. Because it's FIFO, 
 * parents are entirely processed before children are processed.
 * 
 * [Examples & Diagram]
 * Tree:
 *         10
 *        /  \
 *       5    15
 *      / \     \
 *     2   7    20
 * 
 * [Dry Run]
 * Init: Queue = [10], StringBuilder = ""
 * Iteration 1: 
 *   - Pop 10. Queue = []
 *   - Append "10". String = "10"
 *   - Push Left (5), Push Right (15). Queue = [5, 15]
 * Iteration 2:
 *   - Pop 5. Queue = [15]
 *   - Append ":5". String = "10:5"
 *   - Push Left (2), Push Right (7). Queue = [15, 2, 7]
 * Iteration 3:
 *   - Pop 15. Queue = [2, 7]
 *   - Append ":15". String = "10:5:15"
 *   - Push Right (20). Queue = [2, 7, 20]
 * (Continues... outputting 10:5:15:2:7:20)
 * 
 * [Pitfalls]
 * - String Concatenation in a loop: Using `result += node.val` inside a loop creates a new String 
 *   object every iteration, degrading time complexity to O(N^2). Always use a StringBuilder!
 * - Null checks: Pushing `null` to the queue and throwing NullPointerException when popping it. 
 *   Always verify `node.left != null` before pushing.
 * 
 * [Pattern Recognition]
 * When you see: "level by level", "shortest path in unweighted graph", "closest nodes first".
 * Think: Breadth-First Search (BFS) using a Queue.
 * Transfers to: Shortest path in a maze, Word Ladder, Populating next right pointers in a tree.
 * 
 * [Interview Script]
 * "Since the problem asks us to group node values by their horizontal levels, a Breadth-First Search 
 * is the perfect fit. I'll use a Queue to maintain the FIFO order of nodes. I'll enqueue the root, 
 * then loop while the queue isn't empty. In each step, I'll poll a node, append its value to a 
 * StringBuilder to maintain O(1) string appends, and enqueue its non-null children. If the tree 
 * is empty, I'll fast-fail and return 'None'. This gives us O(N) time and O(N) space."
 * 
 * ============================================================================
 * 5. FOLLOW-UPS
 * ============================================================================
 * Q: How would you group the output strictly by level, e.g., [[10], [5, 15], [2, 7, 20]]?
 * A: I would introduce an inner loop. Before popping nodes, I check `int size = queue.size()`. 
 *    I loop exactly `size` times to pop all nodes of the current level into a sub-list.
 * 
 * Q: What if the tree is too large to fit in memory?
 * A: Standard BFS fails because the queue size grows to N/2. If memory is heavily restricted, 
 *    we might have to revert to Iterative Deepening DFS (IDDFS). It runs in O(N) time overall 
 *    but only requires O(H) space for the stack, trading minor repeated traversals at upper levels 
 *    for massive memory savings.
 * 
 * ============================================================================
 * 6. SUMMARY
 * ============================================================================
 * - Core pattern: Queue-based BFS.
 * - Key observation: FIFO inherently guarantees level-order execution.
 * - Most common trap: O(N^2) string concatenation using `+=` instead of StringBuilder.
 * - Mental trigger: "Horizontal / Level" = Queue.
 */

import java.util.LinkedList;
import java.util.Queue;

public class LevelOrderTraversal {

    // Standard Binary Tree Node definition
    public static class TreeNode {
        int val;
        TreeNode left;
        TreeNode right;

        TreeNode(int val) {
            this.val = val;
        }
    }

    /**
     * Executes a level order traversal of the binary tree.
     * 
     * @param root The root of the binary tree.
     * @return A colon-separated string of node values, or "None" if the tree is empty.
     */
    public static String getLevelOrder(TreeNode root) {
        // Fast-fail for the empty tree edge case explicitly requested in constraints.
        if (root == null) {
            return "None";
        }

        // Using a StringBuilder to avoid O(N^2) string concatenation overhead.
        // E.g., sb starts as "", eventually grows to "1:2:3"
        StringBuilder sb = new StringBuilder();

        // Queue to manage the traversal order. LinkedList is an idiomatic choice 
        // for Queue in Java. ArrayDeque is also fine and slightly more memory efficient.
        // Concrete state: initially holds just [Node(root.val)]
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);

        // Process nodes until the "waiting line" is completely empty.
        while (!queue.isEmpty()) {
            // Retrieve and remove the front node of the queue.
            TreeNode currentNode = queue.poll();

            // Formatting: If the StringBuilder isn't empty, it means we've already
            // processed at least one node, so we need a separator before the new value.
            if (sb.length() > 0) {
                sb.append(":");
            }
            sb.append(currentNode.val);

            // Queue up the next generation (children). 
            // We strictly check for null to avoid polluting the Queue with nulls, 
            // which would cause NullPointerExceptions on the next iterations.
            if (currentNode.left != null) {
                queue.offer(currentNode.left);
            }
            if (currentNode.right != null) {
                queue.offer(currentNode.right);
            }
        }

        return sb.toString();
    }

    // ============================================================================
    // TESTING & CROSS-CHECKING
    // ============================================================================
    public static void main(String[] args) {
        // Test Case 1: Standard Balanced Tree
        //       1
        //      / \
        //     2   3
        //    / \   \
        //   4   5   6
        TreeNode root1 = new TreeNode(1);
        root1.left = new TreeNode(2);
        root1.right = new TreeNode(3);
        root1.left.left = new TreeNode(4);
        root1.left.right = new TreeNode(5);
        root1.right.right = new TreeNode(6);
        System.out.println("Test 1 (Standard): " + getLevelOrder(root1)); 
        // Expected: 1:2:3:4:5:6

        // Test Case 2: Empty Tree Edge Case
        TreeNode root2 = null;
        System.out.println("Test 2 (Empty): " + getLevelOrder(root2)); 
        // Expected: None

        // Test Case 3: Extreme Skew (Linked List Tree)
        //   10
        //  /
        // 9
        // \
        //  8
        TreeNode root3 = new TreeNode(10);
        root3.left = new TreeNode(9);
        root3.left.right = new TreeNode(8);
        System.out.println("Test 3 (Skewed): " + getLevelOrder(root3)); 
        // Expected: 10:9:8

        // Test Case 4: Negative Values & Single Node
        TreeNode root4 = new TreeNode(-50);
        System.out.println("Test 4 (Single Negative): " + getLevelOrder(root4)); 
        // Expected: -50
    }
}

