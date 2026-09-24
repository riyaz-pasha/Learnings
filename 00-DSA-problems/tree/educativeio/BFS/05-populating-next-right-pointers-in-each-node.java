/**
 * ============================================================================
 * 0. PROBLEM STATEMENT
 * ============================================================================
 * Given a perfect binary tree, where each node contains an additional pointer 
 * called next. This pointer is initially set to NULL for all nodes. Your task 
 * is to connect all nodes of the same hierarchical level by setting the next 
 * pointer to its immediate right node.
 * 
 * The next pointer of the rightmost node at each level is set to NULL.
 * 
 * Constraints:
 * - The number of nodes in the tree is in the range [0, 500].
 * - -1000 <= Node.data <= 1000
 * 
 * ============================================================================
 * 1. CLARIFYING QUESTIONS
 * ============================================================================
 * - "Can I assume the tree is strictly 'perfect', meaning every internal node has exactly two children and all leaves are at the same depth?"
 *   Crucial, because it prevents us from having to hunt for the 'next available' child across gaps.
 * - "Does the implicit recursion stack count against our space complexity if we are asked for an O(1) space solution?"
 *   If the interviewer says "yes", it rules out a DFS approach, forcing us to think about pure pointer iteration.
 * - "Are we returning the original root node after modifying it in-place?"
 *   Confirms the expected return type and ensures we don't accidentally return a level-head.
 * - "Should we explicitly set the rightmost nodes' next pointers to NULL, or can we rely on the default object initialization?"
 *   Clarifies whether we need defensive assignment. (Usually, relying on the default NULL is fine).
 * 
 * ============================================================================
 * 2. THE REASONING JOURNEY
 * ============================================================================
 * [Binding Constraint] 
 * We need to connect nodes that share the same depth (horizontal relationship), but 
 * the tree structure only gives us parent-child (vertical) relationships. Sibling 
 * connections are easy (parent has both children), but "cousin" connections 
 * (across different parents) are the primary hurdle.
 * 
 * --- APPROACH 1: Standard BFS with a Queue ---
 * 1. What I'd naturally try: Use a Breadth-First Search. Capture the `size` of the queue 
 *    at each level. Pop nodes one by one. If it's not the last node in the level, point 
 *    its `next` to the element currently at the front of the queue (`queue.peek()`).
 * 2. Why it works: BFS naturally extracts nodes level-by-level in exact left-to-right order.
 * 3. Why it's too costly: The queue physically stores the nodes of the tree.
 * 4. What work is repeated: We allocate O(N) extra memory just to figure out the left-to-right 
 *    ordering, when the tree itself already has a deterministic structure we could exploit.
 * 5. Time Complexity: O(N) — because every node is queued and dequeued exactly once.
 * 6. Space Complexity: O(N) — because the queue holds the bottom level of the perfect tree, 
 *    which contains exactly (N+1)/2 nodes.
 * 
 * --- APPROACH 2: Recursive DFS (Preorder) ---
 * 1. What I'd try next (to save Queue memory): Walk the tree recursively. At any node, I can 
 *    connect its left child to its right child. But how do I connect its right child to its cousin? 
 *    If I ensure the parent level is fully connected *before* I go down to the children, I can use 
 *    the parent's `next` pointer! `root.right.next = root.next.left`.
 * 2. Why it works: It leverages the `next` pointers established at level L to bridge the cousin 
 *    gap at level L+1.
 * 3. Why it's sub-optimal: It relies on the call stack. While much better than O(N) queue space, 
 *    it's not truly O(1) auxiliary space.
 * 4. Time Complexity: O(N) — because we visit every node once.
 * 5. Space Complexity: O(H) or O(log N) — because the recursive call stack goes as deep as the tree height.
 * 
 * [The Core Observation for Optimal]
 * If Approach 2 works because level L is connected before level L+1, we don't need recursion 
 * at all! We can just use two pointers. One pointer (`leftmost`) keeps track of the start of 
 * the next level. Another pointer (`curr`) acts as an iterator, moving horizontally across 
 * level L using the `next` pointers, actively sewing together the nodes of level L+1.
 * 
 * --- APPROACH 3: Iterative Level-Linked Traversal (The Optimal Way) ---
 * 1. How it works: Start at the root. While we are not at the leaf level, use `curr` to scan 
 *    across the current level. For each `curr`, connect its children (`curr.left.next = curr.right`). 
 *    Then, if `curr.next` exists, bridge the gap to the cousins (`curr.right.next = curr.next.left`). 
 *    Move `curr` to `curr.next`. Once the level is done, drop down to the next level using `leftmost.left`.
 * 2. Time Complexity: O(N) — because we visit each node essentially once (we process a node's children 
 *    from the parent, and traverse across parents). Total operations scale strictly linearly with N.
 * 3. Space Complexity: O(1) — because we only use two tracking pointers (`leftmost` and `curr`). 
 *    No queue, no recursion stack.
 * 
 * [Which one I'd write in an interview]
 * Approach 3 (Iterative O(1) Space). It demonstrates a deep understanding of how to mutate a 
 * data structure to aid your own traversal, entirely sidestepping auxiliary memory.
 * 
 * ============================================================================
 * 3. EDGE CASES
 * ============================================================================
 * - Empty Tree: `root == null`. Must check immediately and return null to prevent NPEs.
 * - Single Node Tree: Root has no children. The outer loop condition (`leftmost.left != null`) 
 *   safely prevents us from trying to access non-existent children.
 * - The Rightmost Edge: The last node in any level has `curr.next == null`. We must explicitly 
 *   check this before trying to evaluate `curr.next.left`, otherwise we crash.
 * 
 * ============================================================================
 * 4. KEY INSIGHT, DIAGRAMS & DRY RUN
 * ============================================================================
 * [Key Insight]
 * Think of it like building a bridge while standing on the bridge you just built. 
 * You stand at Level L, which is already a fully connected linked list. You walk across Level L, 
 * using it as a scaffold to wire together Level L+1. Once finished, you drop down to Level L+1 
 * and repeat.
 * 
 * [Examples & Diagram]
 * Level 0:         1
 *                /   \
 * Level 1:      2     3
 *              / \   / \
 * Level 2:    4   5 6   7
 * 
 * [Dry Run (Optimal Iterative Approach)]
 * Init: leftmost = 1
 * 
 * Outer Loop 1: (leftmost = 1. Has left child? Yes)
 *   curr = 1
 *   Inner Loop:
 *     - Sibling Link: curr.left.next = curr.right => (2 -> 3)
 *     - Cousin Link: curr.next is null? Yes. (Skip cousin link).
 *     - curr = curr.next => null. Inner loop ends.
 *   leftmost moves to leftmost.left => 2
 * 
 * Outer Loop 2: (leftmost = 2. Has left child? Yes)
 *   curr = 2 (Level 1 is now a linked list: 2 -> 3)
 *   Inner Loop Iteration 1 (curr = 2):
 *     - Sibling Link: curr.left.next = curr.right => (4 -> 5)
 *     - Cousin Link: curr.next is 3 (not null). curr.right.next = curr.next.left => (5 -> 6)
 *     - curr = curr.next => 3
 *   Inner Loop Iteration 2 (curr = 3):
 *     - Sibling Link: curr.left.next = curr.right => (6 -> 7)
 *     - Cousin Link: curr.next is null. (Skip)
 *     - curr = curr.next => null. Inner loop ends.
 *   leftmost moves to leftmost.left => 4
 * 
 * Outer Loop 3: (leftmost = 4. Has left child? No.)
 *   Loop terminates. All levels properly connected!
 * 
 * [Pitfalls]
 * - Trying to evaluate `curr.next.left` without checking if `curr.next != null`. 
 *   The right-most node of every level will trigger a NullPointerException here if unguarded.
 * - Forgetting to move `leftmost` down the tree. If you just do `curr = root` and forget the 
 *   level-tracking pointer, you'll process the root and then get stuck or skip levels.
 * 
 * [Pattern Recognition]
 * When you see: "Connect nodes at the same level" + "O(1) extra space requirement".
 * Think: Use the `next` pointers of the *current* level to traverse horizontally, acting as 
 * a scaffolding to connect the *children's* level.
 * 
 * [Interview Script]
 * "A standard BFS would solve this easily, but it requires O(N) space for the queue. Since we 
 * are building a linked list at every level, we can actually use the linked list at level L to 
 * traverse horizontally and wire up level L+1. I'll use a `leftmost` pointer to keep track of 
 * the start of the next level to process, and a `curr` pointer to move across the current level. 
 * For every node, I'll connect its left child to its right child. Then, if it has a `next` 
 * neighbor, I'll connect its right child to its neighbor's left child. This gives us O(N) time 
 * and strictly O(1) space."
 * 
 * ============================================================================
 * 5. FOLLOW-UPS
 * ============================================================================
 * Q: What if the tree is NOT a perfect binary tree? (LeetCode 117 - Populating Next Right Pointers II)
 * A: Our optimal approach breaks because `curr.left`, `curr.right`, or `curr.next.left` might be null. 
 *    To fix this while keeping O(1) space, we introduce a `dummyHead` node at the start of every new 
 *    level. As we traverse Level L, we point `dummyHead.next` to the first non-null child we find, 
 *    and keep a `tail` pointer to stitch subsequent non-null children. Once Level L is done, we drop 
 *    down to `dummyHead.next` for the next level.
 * 
 * Q: How would you print the connected levels to verify your code?
 * A: I would start at the `root`. Print the node, follow `.next` until null, print a newline. 
 *    Then to move to the next level, I go back to the head of the current level and follow `.left`.
 * 
 * ============================================================================
 * 6. JAVA CODE
 * ============================================================================
 */

import java.util.LinkedList;
import java.util.Queue;

public class PopulatingNextRightPointers {

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
     * APPROACH 3: Iterative Level-Linked Traversal (Optimal O(1) Space)
     * 
     * @param root The root of the perfect binary tree.
     * @return The root of the modified tree.
     */
    public static Node connectOptimal(Node root) {
        // Fast-fail: If the tree is empty, return null immediately.
        if (root == null) {
            return null;
        }

        // 'leftmost' tracks the first node of the level we are currently STANDING on.
        // We start standing at the root.
        Node leftmost = root;

        // We stop when we reach the leaf level. A perfect binary tree guarantees 
        // that if a node has no left child, it is a leaf.
        while (leftmost.left != null) {
            
            // 'curr' will iterate horizontally across the current level.
            // Concrete example: leftmost is 2. curr starts at 2, then moves to 3.
            Node curr = leftmost;

            while (curr != null) {
                // 1. SIBLING CONNECTION (Share the same parent)
                // Connect the left child to the right child.
                // e.g., if curr is 2, this links 4 -> 5.
                curr.left.next = curr.right;

                // 2. COUSIN CONNECTION (Across different parents)
                // If curr has a neighbor to its right, connect curr's right child 
                // to the neighbor's left child.
                // e.g., if curr is 2 and curr.next is 3, this links 5 -> 6.
                if (curr.next != null) {
                    curr.right.next = curr.next.left;
                }

                // Move horizontally to the next node on the CURRENT level.
                curr = curr.next;
            }

            // The entire level below us is now fully stitched together.
            // Drop down to the next level to repeat the process.
            leftmost = leftmost.left;
        }

        // The original root is returned, but now all 'next' pointers internally are set.
        return root;
    }

    /**
     * APPROACH 1: Raw BFS Queue (Included for conceptual contrast)
     */
    public static Node connectBFS(Node root) {
        if (root == null) return null;
        
        Queue<Node> queue = new LinkedList<>();
        queue.offer(root);
        
        while (!queue.isEmpty()) {
            int size = queue.size();
            
            for (int i = 0; i < size; i++) {
                Node curr = queue.poll();
                
                // If this is not the last node in the level, point to the next in queue
                if (i < size - 1) {
                    curr.next = queue.peek();
                }
                
                if (curr.left != null) queue.offer(curr.left);
                if (curr.right != null) queue.offer(curr.right);
            }
        }
        return root;
    }

    // ============================================================================
    // TESTING & CROSS-CHECKING
    // ============================================================================
    public static void main(String[] args) {
        // Test Case 1: Standard Depth-3 Perfect Tree
        System.out.println("--- Test 1: Standard Perfect Tree (Optimal) ---");
        Node root1 = buildPerfectTree(3);
        connectOptimal(root1);
        printLevelsUsingNextPointers(root1);
        // Expected Output:
        // Level: 1 -> NULL
        // Level: 2 -> 3 -> NULL
        // Level: 4 -> 5 -> 6 -> 7 -> NULL

        // Test Case 2: Standard Depth-3 Perfect Tree (BFS)
        System.out.println("\n--- Test 2: Standard Perfect Tree (BFS) ---");
        Node root2 = buildPerfectTree(3);
        connectBFS(root2);
        printLevelsUsingNextPointers(root2);

        // Test Case 3: Empty Tree
        System.out.println("\n--- Test 3: Empty Tree ---");
        Node root3 = null;
        connectOptimal(root3);
        printLevelsUsingNextPointers(root3);
        // Expected Output: (Nothing prints)

        // Test Case 4: Single Node Tree
        System.out.println("\n--- Test 4: Single Node ---");
        Node root4 = new Node(99);
        connectOptimal(root4);
        printLevelsUsingNextPointers(root4);
        // Expected Output: Level: 99 -> NULL
    }

    /**
     * Helper method to verify our code works. 
     * It uses the `next` pointers to print horizontal levels.
     */
    private static void printLevelsUsingNextPointers(Node root) {
        if (root == null) {
            System.out.println("Tree is empty.");
            return;
        }
        
        Node leftmost = root;
        while (leftmost != null) {
            Node curr = leftmost;
            System.out.print("Level: ");
            while (curr != null) {
                System.out.print(curr.val + " -> ");
                curr = curr.next;
            }
            System.out.println("NULL");
            
            // Move down to the next level
            leftmost = leftmost.left;
        }
    }

    /**
     * Helper method to build a perfect binary tree of a given depth.
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
 * 7. SUMMARY
 * ============================================================================
 * - Core pattern: Two-pointer horizontal traversal (Leftmost + Curr).
 * - Key observation: You can use the already-connected level L as a scaffold 
 *   to traverse and connect level L+1, eliminating the need for a Queue.
 * - Most common trap: Forgetting to check `curr.next != null` before assigning 
 *   `curr.right.next = curr.next.left`, resulting in a NullPointerException on 
 *   the right boundary of the tree.
 * - Mental trigger: "O(1) space Level Order" -> Iterate horizontally using `.next`.
 */


import java.util.*;

/**
 * Definition for a Node.
 */
class Node {
    int val;
    Node left, right, next;

    Node(int val) {
        this.val = val;
    }
}

public class ConnectNextPointersBFS {

    /**
     * Approach 1: BFS (Level Order)
     *
     * Intuition:
     * - Use queue to process nodes level by level
     * - Maintain previous node to connect .next
     *
     * Time Complexity: O(N)
     * Space Complexity: O(N) (queue)
     */
    public Node connect(Node root) {
        if (root == null) return null;

        Queue<Node> queue = new ArrayDeque<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            int size = queue.size();
            Node prev = null;

            // Process one level
            for (int i = 0; i < size; i++) {
                Node curr = queue.poll();

                // Connect previous node to current
                if (prev != null) {
                    prev.next = curr;
                }
                prev = curr;

                // Add children
                if (curr.left != null) queue.offer(curr.left);
                if (curr.right != null) queue.offer(curr.right);
            }

            // Last node points to null (default)
        }

        return root;
    }
}

/**
 * Problem: Populating Next Right Pointers in Each Node
 *
 * ============================================================
 * STATEMENT
 * ============================================================
 *
 * Given a PERFECT binary tree, where each node contains an
 * additional pointer called `next`.
 *
 * The `next` pointer is initially NULL for every node.
 *
 * The task is to connect every node to its immediate right
 * node on the SAME level.
 *
 * The rightmost node of every level must point to NULL.
 *
 *
 * Example:
 *
 *                         1
 *                       /   \
 *                      2     3
 *                     / \   / \
 *                    4   5 6   7
 *
 *
 * After connecting:
 *
 *                         1
 *                       /   \
 *                      2 ---> 3 ---> NULL
 *                     / \   / \
 *                    4->5->6->7 ---> NULL
 *
 *
 * Connections:
 *
 *     1.next = null
 *
 *     2.next = 3
 *     3.next = null
 *
 *     4.next = 5
 *     5.next = 6
 *     6.next = 7
 *     7.next = null
 *
 *
 * ============================================================
 * CONSTRAINTS
 * ============================================================
 *
 * Number of nodes:
 *
 *     [0, 500]
 *
 * Node.data:
 *
 *     -1000 <= Node.data <= 1000
 *
 *
 * IMPORTANT:
 *
 * The tree is PERFECT.
 *
 * A perfect binary tree means:
 *
 * 1. Every internal node has exactly TWO children.
 * 2. Every leaf is at the SAME level.
 *
 *
 * Example of a perfect tree:
 *
 *                    1
 *                  /   \
 *                 2     3
 *                / \   / \
 *               4   5 6   7
 *
 *
 * Example of NOT a perfect tree:
 *
 *                    1
 *                  /   \
 *                 2     3
 *                / \
 *               4   5
 *
 * The second tree is not perfect because node 3 has no children
 * while node 2 does.
 *
 *
 * ============================================================
 * KEY OBSERVATION
 * ============================================================
 *
 * We need to connect nodes horizontally:
 *
 *
 *                    1
 *                  /   \
 *                 2     3
 *                / \   / \
 *               4   5 6   7
 *
 *
 * Desired `next` connections:
 *
 *     1
 *
 *     2 ------------> 3
 *
 *     4 ---> 5 ---> 6 ---> 7
 *
 *
 * The interesting question is:
 *
 * How do we connect 5 -> 6?
 *
 *
 * 5 and 6 do NOT have the same parent.
 *
 * Their parents are:
 *
 *     2       3
 *    / \     / \
 *   4   5   6   7
 *
 *
 * So we need to somehow move from parent 2 to parent 3.
 *
 *
 * This is where the `next` pointer of the CURRENT LEVEL helps.
 *
 *
 * Once we have:
 *
 *     2.next = 3
 *
 * we can use:
 *
 *     current.next
 *
 * to reach the next parent.
 *
 *
 * Therefore:
 *
 *     5.next = 6
 *
 * can be obtained from:
 *
 *     current.right.next = current.next.left
 *
 *
 * ============================================================
 * TWO TYPES OF CONNECTIONS
 * ============================================================
 *
 * For every internal node, we make two possible connections.
 *
 *
 * ------------------------------------------------------------
 * CONNECTION 1: LEFT CHILD -> RIGHT CHILD
 * ------------------------------------------------------------
 *
 * Example:
 *
 *              2
 *             / \
 *            4   5
 *
 *
 * Connect:
 *
 *     4.next = 5
 *
 *
 * Code:
 *
 *     current.left.next = current.right;
 *
 *
 * ------------------------------------------------------------
 * CONNECTION 2: RIGHT CHILD -> NEXT NODE'S LEFT CHILD
 * ------------------------------------------------------------
 *
 * Example:
 *
 *              2 ------------> 3
 *             / \             / \
 *            4   5           6   7
 *
 *
 * We need:
 *
 *     5.next = 6
 *
 *
 * Since:
 *
 *     current = 2
 *
 *     current.right = 5
 *
 *     current.next = 3
 *
 *     current.next.left = 6
 *
 *
 * Therefore:
 *
 *     current.right.next = current.next.left;
 *
 *
 * which means:
 *
 *     5.next = 6
 *
 *
 * ============================================================
 * WHY IS THIS O(1) EXTRA SPACE?
 * ============================================================
 *
 * A normal level-order traversal would use a Queue:
 *
 *
 *                    1
 *                   / \
 *                  2   3
 *                 / \ / \
 *                4  5 6  7
 *
 *
 * Queue:
 *
 *     [1]
 *
 * then:
 *
 *     [2, 3]
 *
 * then:
 *
 *     [4, 5, 6, 7]
 *
 *
 * The queue requires O(N) additional memory in the worst case.
 *
 *
 * But we don't actually need a queue.
 *
 * Once a level has been connected:
 *
 *
 *              2 ----------> 3
 *
 *
 * we can use those `next` pointers to move across the level.
 *
 *
 * Therefore:
 *
 *     current = current.next;
 *
 * moves us horizontally.
 *
 *
 * We only need a few variables:
 *
 *     levelStart
 *     current
 *
 *
 * Therefore:
 *
 *     Extra Space = O(1)
 *
 *
 * ============================================================
 * HIGH-LEVEL ALGORITHM
 * ============================================================
 *
 * 1. Start from the root.
 *
 * 2. `levelStart` represents the first node of the current
 *    level.
 *
 * 3. Traverse the current level using `next`.
 *
 * 4. For every node:
 *
 *       left.next = right
 *
 *    and, if a next node exists:
 *
 *       right.next = next.left
 *
 * 5. Move `levelStart` to the next level.
 *
 * 6. Stop when we reach the leaf level.
 *
 *
 * ============================================================
 * VISUAL WALKTHROUGH
 * ============================================================
 *
 * Let's use:
 *
 *                    1
 *                  /   \
 *                 2     3
 *                / \   / \
 *               4   5 6   7
 *
 *
 * Initially all `next` pointers are NULL:
 *
 *
 *                    1
 *                  /   \
 *                 2     3
 *                / \   / \
 *               4   5 6   7
 *
 * next pointers:
 *
 *     1 -> null
 *     2 -> null
 *     3 -> null
 *     4 -> null
 *     5 -> null
 *     6 -> null
 *     7 -> null
 *
 *
 * ============================================================
 * STEP 1: START AT ROOT
 * ============================================================
 *
 *     levelStart = root
 *
 *
 * So:
 *
 *
 *                    1
 *                    ^
 *                    |
 *              levelStart
 *
 *
 * `current` also starts at 1:
 *
 *
 *              current
 *                 |
 *                 v
 *                 1
 *
 *
 * ============================================================
 * STEP 2: PROCESS NODE 1
 * ============================================================
 *
 * Current node:
 *
 *                 1
 *                / \
 *               2   3
 *
 *
 * CONNECTION 1:
 *
 *     current.left.next = current.right
 *
 *
 * becomes:
 *
 *     2.next = 3
 *
 *
 * Visual:
 *
 *                 1
 *                / \
 *               2 ---> 3
 *
 *
 * Now we have successfully connected level 2.
 *
 *
 * CONNECTION 2:
 *
 * Check:
 *
 *     current.next
 *
 *
 * But:
 *
 *     1.next == null
 *
 *
 * So there is no node to the right of 1.
 *
 * Therefore we don't need another connection.
 *
 *
 * Then:
 *
 *     current = current.next
 *
 *
 * Since:
 *
 *     1.next == null
 *
 *
 * we get:
 *
 *     current = null
 *
 *
 * The current level is finished.
 *
 *
 * ============================================================
 * STEP 3: MOVE TO LEVEL 2
 * ============================================================
 *
 * We execute:
 *
 *     levelStart = levelStart.left;
 *
 *
 * Previously:
 *
 *     levelStart = 1
 *
 *
 * Now:
 *
 *     levelStart = 2
 *
 *
 * Tree:
 *
 *
 *                    1
 *                  /   \
 *                 2 ---> 3
 *                / \   / \
 *               4   5 6   7
 *
 *              ^
 *              |
 *        levelStart
 *
 *
 * We now process:
 *
 *     2 ---> 3
 *
 *
 * ============================================================
 * STEP 4: PROCESS NODE 2
 * ============================================================
 *
 *     current = 2
 *
 *
 * Current level:
 *
 *              2 ------------> 3
 *             / \             / \
 *            4   5           6   7
 *
 *
 * CONNECTION 1:
 *
 *     current.left.next = current.right
 *
 *
 * Therefore:
 *
 *     4.next = 5
 *
 *
 * We now have:
 *
 *              2 ------------> 3
 *             / \             / \
 *            4 ---> 5        6   7
 *
 *
 * CONNECTION 2:
 *
 *     current.right.next = current.next.left
 *
 *
 * Let's substitute:
 *
 *     current             = 2
 *
 *     current.right       = 5
 *
 *     current.next        = 3
 *
 *     current.next.left   = 6
 *
 *
 * Therefore:
 *
 *     5.next = 6
 *
 *
 * Result:
 *
 *              2 ------------> 3
 *             / \             / \
 *            4 ---> 5 -----> 6   7
 *
 *
 * This is the most important connection in the algorithm.
 *
 *
 * ============================================================
 * STEP 5: MOVE FROM NODE 2 TO NODE 3
 * ============================================================
 *
 * We execute:
 *
 *     current = current.next;
 *
 *
 * Since:
 *
 *     2.next = 3
 *
 *
 * we get:
 *
 *     current = 3
 *
 *
 * This is how we traverse the level WITHOUT A QUEUE.
 *
 *
 * Current level:
 *
 *              2 ------------> 3
 *             / \             / \
 *            4 ---> 5 -----> 6   7
 *                             ^
 *                             |
 *                          current
 *
 *
 * ============================================================
 * STEP 6: PROCESS NODE 3
 * ============================================================
 *
 *     current = 3
 *
 *
 * CONNECTION 1:
 *
 *     current.left.next = current.right
 *
 *
 * Therefore:
 *
 *     6.next = 7
 *
 *
 * Result:
 *
 *              2 ------------> 3
 *             / \             / \
 *            4 ---> 5 -----> 6 ---> 7
 *
 *
 * CONNECTION 2:
 *
 * Check:
 *
 *     current.next
 *
 *
 * But:
 *
 *     3.next == null
 *
 *
 * Therefore there is no node to the right of 3.
 *
 * We don't create another connection.
 *
 *
 * Then:
 *
 *     current = current.next
 *
 *
 * becomes:
 *
 *     current = null
 *
 *
 * Level 2 is now complete.
 *
 *
 * ============================================================
 * STEP 7: MOVE TO LEVEL 3
 * ============================================================
 *
 * Execute:
 *
 *     levelStart = levelStart.left;
 *
 *
 * Previously:
 *
 *     levelStart = 2
 *
 *
 * Now:
 *
 *     levelStart = 4
 *
 *
 * We have:
 *
 *              4 ---> 5 ---> 6 ---> 7 ---> null
 *
 *
 * These are leaf nodes.
 *
 * They don't have children.
 *
 * Therefore:
 *
 *     levelStart.left == null
 *
 *
 * The outer loop stops.
 *
 *
 * ============================================================
 * FINAL RESULT
 * ============================================================
 *
 *
 *                    1
 *                  /   \
 *                 2 ---> 3 ---> NULL
 *                / \   / \
 *               4->5->6->7 ---> NULL
 *
 *
 * Every level is now connected from left to right.
 *
 *
 * ============================================================
 * POINTER TABLE
 * ============================================================
 *
 * Node        next
 * ---------------------
 * 1           null
 * 2           3
 * 3           null
 * 4           5
 * 5           6
 * 6           7
 * 7           null
 *
 *
 * ============================================================
 * WHY DOES `current.right.next = current.next.left` WORK?
 * ============================================================
 *
 * This is easier to understand if we look at TWO adjacent
 * parents:
 *
 *
 *          current             current.next
 *              2 -------------------> 3
 *             / \                  / \
 *            4   5                6   7
 *
 *
 * The children of the first parent are:
 *
 *     4, 5
 *
 * The children of the second parent are:
 *
 *     6, 7
 *
 *
 * The correct level order is:
 *
 *     4 -> 5 -> 6 -> 7
 *
 *
 * We already connect:
 *
 *     4 -> 5
 *
 *
 * So the missing connection is:
 *
 *     5 -> 6
 *
 *
 * And:
 *
 *     5 = current.right
 *
 *     6 = current.next.left
 *
 *
 * Therefore:
 *
 *     current.right.next = current.next.left;
 *
 *
 * ============================================================
 * WHY DO WE NEED THE `if` CHECK?
 * ============================================================
 *
 * Consider the RIGHTMOST node of a level:
 *
 *
 *              2 ------------> 3
 *
 *
 * When:
 *
 *     current = 3
 *
 *
 * There is no node after 3.
 *
 * Therefore:
 *
 *     current.next == null
 *
 *
 * We cannot do:
 *
 *     current.next.left
 *
 * because that would mean:
 *
 *     null.left
 *
 * which causes a NullPointerException.
 *
 *
 * Therefore:
 *
 *     if (current.next != null)
 *
 * protects us.
 *
 *
 * It also naturally leaves the rightmost node's `next`
 * pointer as NULL.
 *
 *
 * ============================================================
 * WHY CAN WE STOP AT THE LEAF LEVEL?
 * ============================================================
 *
 * The purpose of this algorithm is to connect CHILDREN.
 *
 *
 * Example:
 *
 *                    1
 *                  /   \
 *                 2     3
 *
 *
 * Processing 1 creates:
 *
 *     2 -> 3
 *
 *
 * Then we process:
 *
 *              2 -> 3
 *             /    \
 *            4      6
 *
 *
 * and create:
 *
 *     4 -> 5 -> 6 -> 7
 *
 *
 * Once we reach:
 *
 *     4 -> 5 -> 6 -> 7
 *
 * these nodes have no children.
 *
 * There is nothing more to connect.
 *
 *
 * Therefore:
 *
 *     while (levelStart.left != null)
 *
 * is sufficient.
 *
 *
 * ============================================================
 * CORRECTNESS INTUITION
 * ============================================================
 *
 * For every node on a level, we establish:
 *
 *
 * 1. Its left child points to its right child.
 *
 *
 *        parent
 *        /   \
 *       L --> R
 *
 *
 * 2. Its right child points to the left child of the
 *    parent's next node.
 *
 *
 *        parent ------------> next parent
 *        /   \                 /   \
 *       L     R ------------> L     R
 *
 *
 * Therefore, if the current level is already connected,
 * we can construct all connections for the next level.
 *
 *
 * The first level starts with the root, whose `next` is NULL.
 *
 * We repeatedly use the already-connected level to construct
 * the next level.
 *
 * Hence, every level becomes completely connected.
 *
 *
 * ============================================================
 * COMPLEXITY
 * ============================================================
 *
 * Let N = number of nodes.
 *
 *
 * Time Complexity:
 *
 *     O(N)
 *
 * Every node is processed exactly once.
 *
 *
 * Space Complexity:
 *
 *     O(1)
 *
 * Only a constant number of variables are used.
 *
 * No queue, array, map, or recursion is required.
 *
 *
 * ============================================================
 * FINAL MENTAL MODEL
 * ============================================================
 *
 * Remember these TWO movements:
 *
 *
 * `levelStart` moves DOWN:
 *
 *              1
 *              |
 *              v
 *              2
 *              |
 *              v
 *              4
 *
 *
 * `current` moves RIGHT:
 *
 *              2 ----------> 3
 *
 *
 * And remember these TWO connections:
 *
 *
 * SAME PARENT:
 *
 *              2
 *             / \
 *            4   5
 *
 *            4 ----> 5
 *
 *
 * DIFFERENT PARENTS:
 *
 *              2 ------------> 3
 *             / \             / \
 *            4   5           6   7
 *
 *                5 --------> 6
 *
 *
 * Therefore the entire algorithm is essentially:
 *
 *
 *     current.left.next = current.right;
 *
 *     if (current.next != null) {
 *         current.right.next = current.next.left;
 *     }
 *
 *
 * ============================================================
 * IMPLEMENTATION
 * ============================================================
 */
public class PopulatingNextRightPointers {

    public Node connect(Node root) {

        // Empty tree.
        //
        // There are no nodes to connect.
        if (root == null) {
            return null;
        }

        /*
         * `levelStart` points to the first node of the
         * current level.
         *
         * Initially:
         *
         *                 1
         *               /   \
         *              2     3
         *
         * levelStart = 1
         */
        Node levelStart = root;

        /*
         * Process every level that has children.
         *
         * Since the tree is perfect:
         *
         *     levelStart.left != null
         *
         * means that another level exists below us.
         *
         * When we reach the leaf level:
         *
         *     levelStart.left == null
         *
         * and we are done.
         */
        while (levelStart.left != null) {

            /*
             * `current` traverses the current level
             * horizontally using already-created `next`
             * pointers.
             *
             * Example:
             *
             *     2 ---> 3 ---> null
             *
             * current = 2
             *
             * then:
             *
             *     current = current.next
             *
             * makes:
             *
             *     current = 3
             */
            Node current = levelStart;

            /*
             * Traverse all nodes in this level.
             */
            while (current != null) {

                /*
                 * --------------------------------------------
                 * CONNECTION 1
                 * --------------------------------------------
                 *
                 * Connect:
                 *
                 *     left child -> right child
                 *
                 * Example:
                 *
                 *          2
                 *         / \
                 *        4   5
                 *
                 * becomes:
                 *
                 *          2
                 *         / \
                 *        4 -> 5
                 */
                current.left.next = current.right;

                /*
                 * --------------------------------------------
                 * CONNECTION 2
                 * --------------------------------------------
                 *
                 * If another node exists to the right on the
                 * current level, connect:
                 *
                 *     current.right
                 *
                 * to:
                 *
                 *     current.next.left
                 *
                 *
                 * Example:
                 *
                 *          2 ------------> 3
                 *         / \              / \
                 *        4   5            6   7
                 *
                 * creates:
                 *
                 *        5 ------------> 6
                 */
                if (current.next != null) {
                    current.right.next = current.next.left;
                }

                /*
                 * Move horizontally to the next node.
                 *
                 * The important point is that `next` pointers
                 * for this level already exist.
                 *
                 * Therefore no queue is required.
                 */
                current = current.next;
            }

            /*
             * The current level has been completely processed.
             *
             * Move down to the next level.
             *
             * Since this is a perfect binary tree, the left
             * child of the first node is the first node of
             * the next level.
             *
             * Example:
             *
             *     levelStart = 1
             *
             *                 ↓
             *
             *     levelStart = 2
             *
             *                 ↓
             *
             *     levelStart = 4
             */
            levelStart = levelStart.left;
        }

        /*
         * Return the original root.
         *
         * The tree structure hasn't changed.
         *
         * Only the `next` pointers were populated.
         */
        return root;
    }
}

