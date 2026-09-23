/**
 * ============================================================================
 * 0. PROBLEM STATEMENT
 * ============================================================================
 * Given the root of a perfect binary tree, where each node is equipped with an 
 * additional pointer, `next`, connect all nodes from left to right. Do so in 
 * such a way that the next pointer of each node points to its immediate right 
 * sibling except for the rightmost node, which points to the first node of the 
 * next level.
 * 
 * The next pointer of the last node of the binary tree should be set to NULL.
 * 
 * Constraints:
 * - Number of nodes in the tree is in the range [0, 500].
 * - -1000 <= Node.data <= 1000
 * 
 * ============================================================================
 * 1. CLARIFYING QUESTIONS
 * ============================================================================
 * - "By connecting the rightmost node of level L to the leftmost node of level L+1, 
 *    are we effectively flattening the entire tree into a single linked list?"
 *    Yes. This changes the problem dramatically from standard sibling-connection.
 * - "Since it's a perfect binary tree, can we assume every non-leaf node has 
 *    exactly two children, and all leaves are at the exact same depth?"
 *    Yes, this mathematical guarantee prevents messy null-checks for missing children.
 * - "Are there strict memory constraints? Are we aiming for an O(1) auxiliary space solution?"
 *    This dictates whether we can just use a Queue or if we need to do pointer manipulation.
 * - "Are we returning the original root node, or the head of a new structure?"
 *    We modify the tree in-place and return the root.
 * 
 * ============================================================================
 * 2. THE REASONING JOURNEY
 * ============================================================================
 * [Binding Constraint] 
 * We need to traverse the tree in perfect level-order and link every node to the next. 
 * Standard DFS (pre-order/in-order) bounces up and down the tree, making horizontal 
 * and diagonal cross-level links difficult. We need a way to process nodes chronologically.
 * 
 * --- APPROACH 1: Raw BFS with a Queue (The Brute-Force Memory Way) ---
 * 1. What I'd naturally try: A standard Breadth-First Search. Normally, BFS requires an 
 *    inner loop to separate levels. But here, the problem WANTS us to connect the end of 
 *    level L to the start of level L+1.
 * 2. Why it works: If we just push left and right children into a Queue sequentially, the 
 *    Queue naturally lines up every node in the exact order we want to thread them. We just 
 *    pop a node and point its `next` to `queue.peek()`.
 * 3. Why it's too costly: It relies heavily on auxiliary memory. 
 * 4. What work/cost is repeated: We are allocating memory for a Queue when the nodes 
 *    themselves have a `next` pointer we could use to store the exact same chronological sequence.
 * 5. Time Complexity: O(N) — because every node is enqueued once and dequeued once.
 * 6. Space Complexity: O(N) — because the Queue stores the bottom level of the perfect binary 
 *    tree, which holds exactly (N+1)/2 nodes.
 * 
 * --- APPROACH 2: Level-by-Level with `next` pointers (The Intermediate Step) ---
 * 1. What I'd try next: Eliminate the Queue. Process level L to stitch together level L+1. 
 *    Keep a `levelStart` pointer. Use an inner loop to move a `curr` pointer across level L, 
 *    linking `curr.left.next = curr.right`.
 * 2. Why it works: We use the `next` pointers established in previous steps to traverse 
 *    horizontally without extra memory. 
 * 3. The bottleneck: It requires nested loops, and tracking the start of the next level is 
 *    clunky. We still treat levels as isolated rows that we manually bridge at the end of the loop.
 * 4. Time Complexity: O(N) — because we visit each node.
 * 5. Space Complexity: O(1) — because we only use a few tracking pointers.
 * 
 * [The Core Observation for Optimal]
 * In Approach 2, we artificially separate levels. But look at the problem requirement: the 
 * *entire tree* becomes one continuous linked list! If we just bootstrap the first cross-node 
 * connection (`root.next = root.left`), we don't need outer/inner loops. The `next` pointers 
 * form a continuous domino chain. We can just say `curr = curr.next` and traverse the entire 
 * tree in a single, seamless pass.
 * 
 * --- APPROACH 3: Single-Pass Domino Effect (The Optimal Way) ---
 * 1. How it works: Seed the chain by linking `root.next = root.left`. Then, in a single loop, 
 *    as `curr` moves along the `next` pointers we are building, we link its children (`curr.left` 
 *    to `curr.right`) AND we link across branches (`curr.right` to `curr.next.left`).
 * 2. Time Complexity: O(N) — because `curr` steps through each of the N nodes exactly once in 
 *    a single while-loop.
 * 3. Space Complexity: O(1) — because we only use one single pointer (`curr`) to traverse 
 *    the structure we are building in-place. No Queues, no recursion stack.
 * 
 * [Which one I'd write in an interview]
 * Approach 3. It shows deep algorithmic maturity to realize that by satisfying the "connect to 
 * next level" requirement, you actually eliminate the need for nested level-order loops entirely.
 * 
 * ============================================================================
 * 3. EDGE CASES
 * ============================================================================
 * - Empty Tree: `root == null`. Must fast-fail and return null.
 * - Single Node Tree: No children. The loop condition `curr.left != null` protects against 
 *   NullPointerExceptions and gracefully bypasses processing.
 * - The Absolute Last Node: When processing the second-to-last node, `curr.next` points to 
 *   the absolute last node. Because it's a leaf, `curr.next.left` is naturally `null`, which 
 *   correctly sets the final node's `next` pointer to `null`.
 * 
 * ============================================================================
 * 4. KEY INSIGHT, DIAGRAMS & DRY RUN
 * ============================================================================
 * [Key Insight]
 * Because we are pointing the rightmost node of level L to the leftmost node of level L+1, 
 * `curr.next` is mathematically GUARANTEED to never be null as long as `curr` has children. 
 * We are always building the road exactly one step ahead of where we are walking.
 * 
 * [Examples & Diagram]
 * Tree:
 *          1
 *        /   \
 *       2     3
 *      / \   / \
 *     4   5 6   7
 * 
 * [Dry Run (Optimal Domino Approach)]
 * Init: root = 1. `root.next = root.left` => (1 -> 2). `curr = 1`.
 * 
 * Iteration 1 (curr = 1):
 * - Link children: `curr.left.next = curr.right` => (2 -> 3)
 * - Link across:   `curr.right.next = curr.next.left` => 3 -> (2.left) => (3 -> 4)
 * - Advance:       `curr = curr.next` => moves to 2
 * 
 * Iteration 2 (curr = 2):
 * - Link children: `curr.left.next = curr.right` => (4 -> 5)
 * - Link across:   `curr.right.next = curr.next.left` => 5 -> (3.left) => (5 -> 6)
 * - Advance:       `curr = curr.next` => moves to 3
 * 
 * Iteration 3 (curr = 3):
 * - Link children: `curr.left.next = curr.right` => (6 -> 7)
 * - Link across:   `curr.right.next = curr.next.left` => 7 -> (4.left). Since 4 is a leaf, (7 -> null)
 * - Advance:       `curr = curr.next` => moves to 4
 * 
 * Iteration 4 (curr = 4):
 * - `curr.left` is null. Loop terminates! 
 * Final thread: 1 -> 2 -> 3 -> 4 -> 5 -> 6 -> 7 -> null.
 * 
 * [Pitfalls]
 * Forgetting to bootstrap with `root.next = root.left`. If you skip this, when `curr = root`, 
 * `curr.next` is null, and evaluating `curr.next.left` throws a NullPointerException. The chain 
 * reaction requires that first push.
 * 
 * [Pattern Recognition]
 * When you see: "Perfect binary tree" + "horizontal connections".
 * Think: In-place pointer manipulation. Look to use the pointers you just created to traverse 
 * the tree instead of allocating a Queue.
 * 
 * ============================================================================
 * 5. FOLLOW-UPS
 * ============================================================================
 * Q: What if the tree is NOT a perfect binary tree (i.e., missing children)?
 * A: The elegant O(1) single-pass trick breaks because `curr.next.left` might be null, breaking 
 *    the chain. We would have to fall back to Approach 1 (Queue) which naturally handles missing 
 *    nodes, or use a `dummyHead` pointer at each level to scan for the next available child, 
 *    costing us the single-loop elegance but maintaining O(1) space.
 * 
 * Q: How would you search for a specific value in this threaded tree?
 * A: Instead of standard DFS/BFS, we can just treat it as a linked list! Start at the root and 
 *    follow `.next` sequentially. It takes O(N) time but strictly O(1) space, avoiding recursion overhead.
 */

import java.util.LinkedList;
import java.util.Queue;

public class ConnectAllSiblingsPerfectTree {

    // Definition for a Node with a next pointer
    public static class Node {
        public int val;
        public Node left;
        public Node right;
        public Node next;

        public Node(int val) {
            this.val = val;
        }
    }

    /**
     * APPROACH 3: Single-Pass Domino Effect (Optimal O(1) Space)
     */
    public static Node connectOptimal(Node root) {
        // Fast-fail for empty tree
        if (root == null) {
            return null;
        }

        // BOOTSTRAP THE CHAIN: We must explicitly connect the root to its left 
        // child so that the single-pass loop has a `curr.next` to follow.
        // e.g., On a 7-node tree, this connects 1 -> 2.
        if (root.left != null) {
            root.next = root.left;
        }

        // curr traverses the flattened linked list as we build it.
        // Concrete state: starts at node 1.
        Node curr = root;

        // We stop when curr.left is null because leaf nodes have no children to stitch.
        while (curr != null && curr.left != null) {
            
            // 1. Link immediate children together.
            // e.g., if curr is 2, this links 4 -> 5.
            curr.left.next = curr.right;

            // 2. Link the right child to the start of the next branch.
            // Because we previously connected `curr` to its sibling/cousin, `curr.next` 
            // is guaranteed to be non-null here. We use it to bridge the gap.
            // e.g., if curr is 2, curr.next is 3. This links 5 -> (3's left child, which is 6).
            curr.right.next = curr.next.left;

            // 3. Advance to the next node in our newly minted linked list!
            // This seamlessly glides across levels without needing an outer loop.
            curr = curr.next;
        }

        return root;
    }

    /**
     * APPROACH 1: Raw BFS Queue (Provided for conceptual comparison)
     */
    public static Node connectBFS(Node root) {
        if (root == null) return null;
        
        Queue<Node> queue = new LinkedList<>();
        queue.offer(root);
        
        while (!queue.isEmpty()) {
            Node curr = queue.poll();
            
            // Because we want to connect level L's end to level L+1's start, 
            // the queue's natural FIFO order is already perfectly aligned.
            if (!queue.isEmpty()) {
                curr.next = queue.peek();
            }
            
            if (curr.left != null) queue.offer(curr.left);
            if (curr.right != null) queue.offer(curr.right);
        }
        
        return root;
    }

    // ============================================================================
    // TESTING & CROSS-CHECKING
    // ============================================================================
    public static void main(String[] args) {
        // Helper to run tests
        System.out.println("--- Testing Optimal O(1) Space Approach ---");
        testTree(buildPerfectTree(3), "Depth 3 (7 nodes)");
        testTree(buildPerfectTree(1), "Depth 1 (1 node)");
        testTree(null, "Empty Tree (0 nodes)");

        System.out.println("\n--- Testing BFS Queue Approach ---");
        Node bfsTree = buildPerfectTree(3);
        connectBFS(bfsTree);
        System.out.print("BFS Output: ");
        printFlattenedTree(bfsTree); 
    }

    private static void testTree(Node root, String testName) {
        System.out.print(testName + ": ");
        connectOptimal(root);
        printFlattenedTree(root);
    }

    /**
     * Traverses the tree purely using the injected `next` pointers.
     */
    private static void printFlattenedTree(Node root) {
        if (root == null) {
            System.out.println("NULL");
            return;
        }
        Node curr = root;
        while (curr != null) {
            System.out.print(curr.val + (curr.next != null ? " -> " : ""));
            curr = curr.next;
        }
        System.out.println(" -> NULL");
    }

    /**
     * Utility to build a perfect binary tree of a given depth.
     */
    private static Node buildPerfectTree(int depth) {
        if (depth == 0) return null;
        Node root = new Node(1);
        Queue<Node> queue = new LinkedList<>();
        queue.offer(root);
        int val = 2;
        int maxNodes = (int) Math.pow(2, depth) - 1;
        
        while (val <= maxNodes) {
            Node curr = queue.poll();
            curr.left = new Node(val++);
            curr.right = new Node(val++);
            queue.offer(curr.left);
            queue.offer(curr.right);
        }
        return root;
    }
}

/**
 * ============================================================================
 * 6. SUMMARY
 * ============================================================================
 * - Core pattern: In-place pointer manipulation / Domino-effect traversal.
 * - Key observation: Connecting level L to level L+1 eliminates the need for 
 *   standard level-order nested loops, turning the tree into a self-populating linked list.
 * - Most common trap: Forgetting to explicitly link `root.next = root.left` before the 
 *   loop, causing a NullPointerException on the very first iteration.
 * - Mental trigger: "Perfect Tree + Next Pointers" -> Write 1 level ahead using curr.next.
 */

import java.util.*;

/**
 * Node definition with next pointer
 */
class Node {
    int val;
    Node left, right, next;

    Node(int val) {
        this.val = val;
    }
}

public class ConnectNextPointers {

    /**
     * Approach 1: BFS Level Order Traversal
     *
     * Idea:
     * - Traverse level by level
     * - Maintain previous node in the same level
     * - Connect prev.next = current
     *
     * Time Complexity: O(N)
     * Space Complexity: O(N) -> Queue
     */
    public Node connectBFS(Node root) {
        if (root == null) return null;

        Queue<Node> queue = new LinkedList<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            int size = queue.size();

            Node prev = null; // track previous node in same level

            for (int i = 0; i < size; i++) {
                Node current = queue.poll();

                // connect previous node to current
                if (prev != null) {
                    prev.next = current;
                }

                prev = current;

                // add children
                if (current.left != null) queue.offer(current.left);
                if (current.right != null) queue.offer(current.right);
            }

            // last node of level points to NULL (already default)
        }

        return root;
    }
}

/**
 * Populating Next Right Pointers in Each Node
 *
 * Problem:
 * ----------
 * Given a PERFECT binary tree, connect each node to its next right node
 * on the same level using the `next` pointer.
 *
 * If there is no node to the right, `next` should be null.
 *
 *
 * Example:
 * ----------
 *
 *              1
 *            /   \
 *           2     3
 *          / \   / \
 *         4   5 6   7
 *
 * After connecting:
 *
 *              1
 *            /   \
 *           2  -> 3
 *          / \   / \
 *         4->5->6->7->null
 *
 *
 * Important:
 * ----------
 * This solution assumes the tree is PERFECT:
 *
 *              1
 *            /   \
 *           2     3
 *          / \   / \
 *         4   5 6   7
 *
 * Every internal node has exactly two children,
 * and all leaves are at the same level.
 *
 *
 * ---------------------------------------------------------
 * APPROACHES
 * ---------------------------------------------------------
 *
 * 1. BFS / Queue
 *
 * We could perform normal level-order traversal:
 *
 *              1
 *             / \
 *            2   3
 *           / \ / \
 *          4  5 6  7
 *
 * Queue:
 *
 * Level 1:
 * [1]
 *
 * Level 2:
 * [2, 3]
 *
 * Level 3:
 * [4, 5, 6, 7]
 *
 * We can connect nodes while processing each level.
 *
 * Time  : O(N)
 * Space : O(N)
 *
 *
 * 2. Optimal solution using existing `next` pointers
 *
 * Instead of maintaining a queue, we use the `next` pointers
 * that we have already created to move horizontally across
 * the current level.
 *
 * This allows us to achieve:
 *
 * Time  : O(N)
 * Space : O(1)
 *
 *
 * ---------------------------------------------------------
 * KEY IDEA
 * ---------------------------------------------------------
 *
 * For every node, there are TWO types of connections:
 *
 * CASE 1:
 * Connect the node's left child to its right child.
 *
 *              current
 *              /    \
 *             L      R
 *
 *              L ---> R
 *
 * Code:
 *
 * current.left.next = current.right;
 *
 *
 * CASE 2:
 * Connect the node's right child to the LEFT child
 * of the current node's next node.
 *
 *
 * Example:
 *
 *              current       next node
 *                 2  ---------->  3
 *                / \            / \
 *               4   5          6   7
 *
 * We want:
 *
 *               5 -----------> 6
 *
 * Therefore:
 *
 * current.right.next = current.next.left;
 *
 *
 * This is the most important line in the solution.
 *
 *
 * ---------------------------------------------------------
 * WHY CAN WE USE current.next?
 * ---------------------------------------------------------
 *
 * This is the clever part of the solution.
 *
 * Suppose we have already processed the root:
 *
 *              1
 *            /   \
 *           2  -> 3
 *
 * We now want to process level 2.
 *
 * `2.next` already points to `3`.
 *
 * Therefore, while processing node 2, we can move horizontally:
 *
 *              2  ---------->  3
 *              current         current.next
 *
 * Then:
 *
 * current = current.next;
 *
 * moves us from 2 to 3.
 *
 *
 * We don't need a queue!
 *
 * The `next` pointers themselves give us a way to traverse
 * horizontally across the level.
 *
 *
 * ---------------------------------------------------------
 * WHY SPACE IS O(1)?
 * ---------------------------------------------------------
 *
 * We do NOT create:
 *
 * - Queue
 * - Array
 * - List
 * - HashMap
 * - Recursion stack
 *
 * We only use a few variables:
 *
 *     levelStart
 *     current
 *
 * Therefore the EXTRA space is O(1).
 *
 *
 * ---------------------------------------------------------
 * HOW THE ALGORITHM WORKS
 * ---------------------------------------------------------
 *
 * We maintain:
 *
 *     levelStart
 *
 * which points to the first node of the current level.
 *
 *
 * Example:
 *
 *              1
 *            /   \
 *           2  -> 3
 *          / \   / \
 *         4->5->6->7
 *
 * Initially:
 *
 *     levelStart = 1
 *
 * We process all nodes of this level.
 *
 * Then move down:
 *
 *     levelStart = levelStart.left
 *
 * So:
 *
 *     levelStart = 1
 *              ↓
 *     levelStart = 2
 *              ↓
 *     levelStart = 4
 *
 * We stop when levelStart.left == null because
 * we have reached the leaf level.
 *
 *
 * ---------------------------------------------------------
 * COMPLETE VISUAL WALKTHROUGH
 * ---------------------------------------------------------
 *
 *
 * INITIAL TREE
 *
 *                    1
 *                  /   \
 *                 2     3
 *                / \   / \
 *               4   5 6   7
 *
 *
 * All `next` pointers are initially null:
 *
 *                    1
 *                  /   \
 *                 2     3
 *                / \   / \
 *               4   5 6   7
 *
 *
 * =========================================================
 * STEP 1: PROCESS LEVEL 1
 * =========================================================
 *
 * levelStart = 1
 *
 * current = 1
 *
 *
 * First connection:
 *
 *     current.left.next = current.right
 *
 * Therefore:
 *
 *     2.next = 3
 *
 *
 * Tree becomes:
 *
 *                    1
 *                  /   \
 *                 2 --->3
 *                / \   / \
 *               4   5 6   7
 *
 *
 * Now check:
 *
 *     current.next
 *
 * But:
 *
 *     1.next == null
 *
 * So there is no node to the right of 1.
 *
 *
 * Move horizontally:
 *
 *     current = current.next
 *
 *     current = null
 *
 * Therefore level 1 is finished.
 *
 *
 * =========================================================
 * STEP 2: MOVE TO NEXT LEVEL
 * =========================================================
 *
 * Execute:
 *
 *     levelStart = levelStart.left;
 *
 * Therefore:
 *
 *     levelStart = 1.left
 *                = 2
 *
 *
 * Current level:
 *
 *              2 ---> 3
 *             / \    / \
 *            4   5  6   7
 *
 *
 * =========================================================
 * STEP 3: PROCESS NODE 2
 * =========================================================
 *
 * current = 2
 *
 *
 * CASE 1:
 *
 *     current.left.next = current.right
 *
 * Therefore:
 *
 *     4.next = 5
 *
 *
 * We now have:
 *
 *              2 ---> 3
 *             / \    / \
 *            4->5  6   7
 *
 *
 * CASE 2:
 *
 *     current.right.next = current.next.left
 *
 *
 * Let's substitute the values:
 *
 *     current = 2
 *     current.right = 5
 *     current.next = 3
 *     current.next.left = 6
 *
 *
 * Therefore:
 *
 *     5.next = 6
 *
 *
 * Now:
 *
 *              2 ---> 3
 *             / \    / \
 *            4->5->6   7
 *
 *
 * Move horizontally:
 *
 *     current = current.next
 *
 *     current = 3
 *
 *
 * =========================================================
 * STEP 4: PROCESS NODE 3
 * =========================================================
 *
 * current = 3
 *
 *
 * CASE 1:
 *
 *     current.left.next = current.right
 *
 * Therefore:
 *
 *     6.next = 7
 *
 *
 * We now have:
 *
 *              2 ---> 3
 *             / \    / \
 *            4->5->6->7
 *
 *
 * CASE 2:
 *
 * Check:
 *
 *     current.next
 *
 * But:
 *
 *     3.next == null
 *
 * So there is no next node.
 *
 * We do not execute:
 *
 *     current.right.next = current.next.left
 *
 *
 * Move horizontally:
 *
 *     current = current.next
 *
 *     current = null
 *
 *
 * Level 2 is finished.
 *
 *
 * =========================================================
 * STEP 5: MOVE TO NEXT LEVEL
 * =========================================================
 *
 * Execute:
 *
 *     levelStart = levelStart.left;
 *
 * Current:
 *
 *     levelStart = 2
 *
 * Therefore:
 *
 *     levelStart = 2.left
 *                = 4
 *
 *
 * Current level:
 *
 *     4 ---> 5 ---> 6 ---> 7 ---> null
 *
 *
 * But these are leaf nodes.
 *
 * Therefore:
 *
 *     levelStart.left == null
 *
 * The outer while loop stops.
 *
 *
 * FINAL RESULT
 * =========================================================
 *
 *
 *                    1
 *                  /   \
 *                 2 ---> 3 ---> null
 *                / \   / \
 *               4->5->6->7 ---> null
 *
 *
 * More precisely:
 *
 *     1.next = null
 *
 *     2.next = 3
 *
 *     3.next = null
 *
 *     4.next = 5
 *
 *     5.next = 6
 *
 *     6.next = 7
 *
 *     7.next = null
 *
 *
 * ---------------------------------------------------------
 * WHY DOES THE ALGORITHM PROCESS ONLY UNTIL THE SECOND-LAST
 * LEVEL?
 * ---------------------------------------------------------
 *
 * We only need to create `next` pointers for CHILDREN.
 *
 * For example:
 *
 *              1
 *             / \
 *            2   3
 *
 * While processing 1, we create:
 *
 *     2.next = 3
 *
 * Once we reach:
 *
 *     2   3
 *
 * their children can be connected.
 *
 * But when we reach:
 *
 *     4   5   6   7
 *
 * these nodes have no children.
 *
 * There is nothing left to connect.
 *
 * Therefore:
 *
 *     while (levelStart.left != null)
 *
 * is enough.
 *
 *
 * ---------------------------------------------------------
 * WHY DOES EACH NODE GET PROCESSED ONLY ONCE?
 * ---------------------------------------------------------
 *
 * Every node is visited once while traversing its level.
 *
 * Example:
 *
 * Level 1:
 *
 *     1
 *
 * Level 2:
 *
 *     2 -> 3
 *
 * Level 3:
 *
 *     4 -> 5 -> 6 -> 7
 *
 * Total work:
 *
 *     1 + 2 + 4 = N
 *
 * Therefore:
 *
 *     Time = O(N)
 *
 *
 * ---------------------------------------------------------
 * IMPORTANT DISTINCTION
 * ---------------------------------------------------------
 *
 * `levelStart` moves DOWN:
 *
 *     levelStart = levelStart.left
 *
 *
 * `current` moves RIGHT:
 *
 *     current = current.next
 *
 *
 * Visually:
 *
 *                  1
 *                /   \
 *               2 --->3
 *              / \   / \
 *             4->5->6->7
 *
 *              ↓
 *         levelStart
 *
 * And:
 *
 *     current
 *        ↓
 *     2 ---> 3
 *
 *
 * So the algorithm has two directions:
 *
 *     levelStart -> DOWN
 *     current    -> RIGHT
 *
 *
 * ---------------------------------------------------------
 * COMPLEXITY
 * ---------------------------------------------------------
 *
 * Time Complexity:
 *
 *     O(N)
 *
 * Every node is processed once.
 *
 *
 * Space Complexity:
 *
 *     O(1)
 *
 * Only a constant number of variables are used.
 *
 *
 * ---------------------------------------------------------
 * IMPORTANT ASSUMPTION
 * ---------------------------------------------------------
 *
 * This implementation works because the tree is PERFECT.
 *
 * For example:
 *
 *                  1
 *                /   \
 *               2     3
 *              / \   / \
 *             4   5 6   7
 *
 * Every internal node has both:
 *
 *     left
 *     right
 *
 * Therefore:
 *
 *     current.left
 *     current.right
 *     current.next.left
 *
 * are safe to use when applicable.
 *
 * For an arbitrary/non-perfect binary tree, we need a different
 * approach because some nodes may have missing children.
 */
public class PopulatingNextRightPointers {

    /**
     * Connect every node to its next right node.
     *
     * Example:
     *
     *              1
     *            /   \
     *           2     3
     *          / \   / \
     *         4   5 6   7
     *
     * Result:
     *
     *              1
     *            /   \
     *           2 ---> 3
     *          / \   / \
     *         4->5->6->7->null
     *
     * @param root root of the perfect binary tree
     * @return root after connecting next pointers
     */
    public Node connectOptimal(Node root) {

        // Empty tree:
        //
        //     root = null
        //
        // Nothing to connect.
        if (root == null) {
            return null;
        }

        /*
         * levelStart points to the FIRST node of the level
         * currently being processed.
         *
         * Initially:
         *
         *              1
         *            /   \
         *           2     3
         *
         * levelStart
         *     |
         *     v
         *     1
         */
        Node levelStart = root;

        /*
         * We stop when there are no children.
         *
         * In a perfect tree, if:
         *
         *     levelStart.left == null
         *
         * then we are at the leaf level.
         *
         * Example:
         *
         *              1
         *            /   \
         *           2     3
         *          / \   / \
         *         4   5 6   7
         *
         * When:
         *
         *     levelStart = 4
         *
         * then:
         *
         *     levelStart.left == null
         *
         * so we are done.
         */
        while (levelStart.left != null) {

            /*
             * current traverses the CURRENT LEVEL horizontally
             * using the `next` pointers.
             *
             * At the beginning:
             *
             *     current = levelStart
             *
             *
             * Example after processing level 1:
             *
             *              1
             *            /   \
             *           2 ---> 3
             *
             * Then when processing level 2:
             *
             *     current = 2
             *
             * and later:
             *
             *     current = current.next
             *
             * which moves:
             *
             *     2 ---> 3
             */
            Node current = levelStart;

            /*
             * Traverse every node in the current level.
             *
             * We don't need a queue because the nodes in this
             * level are already connected through `next`.
             */
            while (current != null) {

                /*
                 * -------------------------------------------------
                 * CONNECTION 1: LEFT CHILD -> RIGHT CHILD
                 * -------------------------------------------------
                 *
                 * Example:
                 *
                 *              2
                 *             / \
                 *            4   5
                 *
                 * Create:
                 *
                 *            4 ---> 5
                 *
                 * Code:
                 */
                current.left.next = current.right;

                /*
                 * -------------------------------------------------
                 * CONNECTION 2: RIGHT CHILD -> NEXT NODE'S LEFT CHILD
                 * -------------------------------------------------
                 *
                 * This handles the connection BETWEEN two parents.
                 *
                 *
                 * Example:
                 *
                 *              2 ------------> 3
                 *             / \             / \
                 *            4   5           6   7
                 *
                 * We already know:
                 *
                 *     current.next == 3
                 *
                 * Therefore:
                 *
                 *     current.right       == 5
                 *     current.next.left    == 6
                 *
                 * We create:
                 *
                 *              5 ----------> 6
                 *
                 * Code:
                 */
                if (current.next != null) {
                    current.right.next = current.next.left;
                }

                /*
                 * Move horizontally to the next node.
                 *
                 * Example:
                 *
                 *     2 ---> 3 ---> null
                 *
                 * current = 2
                 *
                 * After:
                 *
                 *     current = current.next
                 *
                 * we get:
                 *
                 *     current = 3
                 *
                 * Then:
                 *
                 *     current = 3.next
                 *
                 *     current = null
                 *
                 * Level processing is complete.
                 */
                current = current.next;
            }

            /*
             * We have finished processing the current level.
             *
             * Move DOWN to the next level.
             *
             * Because this is a perfect tree, the first node
             * of the next level is the LEFT child of the
             * current level's first node.
             *
             *
             * Example:
             *
             * Before:
             *
             *              1
             *            /   \
             *           2 ---> 3
             *
             * levelStart = 1
             *
             *
             * After:
             *
             *              1
             *            /   \
             *           2 ---> 3
             *
             * levelStart
             *     |
             *     v
             *     2
             *
             *
             * So:
             *
             *     levelStart = levelStart.left;
             */
            levelStart = levelStart.left;
        }

        /*
         * The original root is still the root of the tree.
         *
         * We return it with all `next` pointers populated.
         */
        return root;
    }
}
