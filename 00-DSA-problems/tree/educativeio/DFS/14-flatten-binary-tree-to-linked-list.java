/**
 * ============================================================================
 * STUDY NOTE: FLATTEN BINARY TREE TO LINKED LIST
 * ============================================================================
 *
 * 1. CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * - "Are we allowed to use extra memory, or must the flattening be done strictly in-place?" 
 *   (Determines if a brute-force list collection is acceptable or if pointer-rewiring is required).
 * - "Does the function return a new root, or do we just mutate the existing tree and return void?" 
 *   (Usually, it's a void function that mutates the tree in place, modifying the `root` reference directly).
 * - "Since this runs in Preorder, if I overwrite a node's right child, won't I lose access to the original right subtree?" 
 *   (This is the crux of the problem! It leads the interviewer to see you understand the pointer-loss trap).
 * - "Are there any concurrent threads reading the tree while we flatten it?" 
 *   (If yes, an in-place Morris traversal is disqualified because it puts the tree in temporarily invalid states).
 *
 *
 * 2. THE REASONING JOURNEY
 * ----------------------------------------------------------------------------
 * Core Constraint: 
 * We must rewire the tree's pointers in Preorder (Node -> Left -> Right). If we process a node 
 * and immediately overwrite its right pointer to point to its left child, we permanently lose 
 * the memory address of the original right child. The bottleneck is safely preserving or 
 * circumventing this lost reference without using massive amounts of extra memory.
 *
 * Approach 1: Preorder Collection to List (The Brute Force)
 * What I'd naturally try: Do a standard recursive Preorder traversal. Instead of doing pointer 
 * math on the fly, I'll just push every visited node into a Java `ArrayList`. Once the list is 
 * fully populated, I'll loop through it from start to finish, setting `node.left = null` and 
 * `node.right = next_node_in_list`.
 * Why it works: By separating the traversal phase from the rewiring phase, we never lose a pointer.
 * Why it's too costly: We are given the tree nodes themselves to form the list, so allocating a 
 * separate array completely defeats the "in-place" spirit of the linked-list requirement.
 * - Time Complexity: O(N) — because we visit N nodes for the traversal, then N nodes for the loop.
 * - Space Complexity: O(N) — because the `ArrayList` stores N object references, alongside the O(N) recursion stack.
 * 
 * Approach 2: Reverse Post-Order Traversal (The Elegant Recursion)
 * What I'd naturally try: If overwriting the right child destroys it in a forward traversal, what 
 * if we traverse BACKWARDS? Preorder is (Node, Left, Right). The exact reverse is (Right, Left, Node). 
 * If I visit the rightmost tail of the flattened list first, I can wire the list from back to front, 
 * maintaining a `prev` pointer to track the node that should come *after* the current one.
 * Why it works: When we process the `Node`, both its left and right subtrees have already been 
 * flattened and tracked. We safely overwrite `Node.right = prev` without losing anything.
 * What property removes the bottleneck: Reversing the traversal order entirely eliminates the 
 * pointer-loss trap, allowing purely in-place rewiring.
 * - Time Complexity: O(N) — because every node is visited exactly once.
 * - Space Complexity: O(N) — bounded by the recursive call stack. On a skewed tree, it reaches N frames.
 *
 * Approach 3: Morris Traversal / Pointer Rewiring (The Ultimate O(1) Space Optimal)
 * What I'd naturally try: To eliminate the recursion stack entirely, let's look at the shape of a Preorder 
 * traversal. The *entire* left subtree must be processed before the right subtree. This means the 
 * LAST node visited in the left subtree (its rightmost node) must point directly to the FIRST node 
 * of the right subtree. 
 * Why it works: For any given node, if it has a left child, we find the rightmost node in that 
 * left subtree. We wire that rightmost node's `right` pointer to the current node's `right` child. 
 * Then, we move the current node's entire left subtree to the right side, nullify the left pointer, 
 * and step right.
 * What property removes the bottleneck: By dynamically mapping the "jump" back to the right subtree 
 * directly into the leaf nodes, we completely eliminate the need for a call stack or auxiliary arrays.
 * - Time Complexity: O(N) — because finding the rightmost nodes visits some edges twice, but asymptotically 
 *   bounds to 2N, which is linear.
 * - Space Complexity: O(1) — because we only use two pointers (`curr` and `runner`), requiring zero extra memory.
 *
 * The Interview Choice:
 * In an interview, Approach 2 (Reverse Post-Order) is the easiest to write bug-free in 5 minutes. 
 * However, the O(1) Space Morris approach (Approach 3) is a major flex that senior engineers love. 
 * I would verbally explain Reverse Post-Order, but write the Morris O(1) pointer-rewiring approach 
 * because it demonstrates deep graph manipulation intuition.
 *
 *
 * 3. EDGE CASES
 * ----------------------------------------------------------------------------
 * 1. Empty Tree (root == null) -> Returns safely without NullPointerExceptions.
 * 2. Tree with ONLY left children -> Tests the core rewiring logic heavily.
 * 3. Tree with ONLY right children -> The tree is ALREADY a flattened linked list. Code should just 
 *    skip through it in O(N) time doing zero work.
 *
 *
 * 4. DRY RUN (Optimal O(1) Space Morris Approach)
 * ----------------------------------------------------------------------------
 * Tree:
 *       1
 *      / \
 *     2   5
 *    / \   \
 *   3   4   6
 * 
 * Target Preorder: 1 -> 2 -> 3 -> 4 -> 5 -> 6
 *
 * Step 1: `curr` = 1. Has left child (2).
 *         Find rightmost of left subtree (which is 4).
 *         Wire 4.right to curr.right: `4.right = 5`.
 *         Move left subtree to right: `1.right = 2`. `1.left = null`.
 *         Tree is now:
 *             1
 *              \
 *               2
 *              / \
 *             3   4
 *                  \
 *                   5
 *                    \
 *                     6
 *         Move `curr` to `curr.right` (2).
 *
 * Step 2: `curr` = 2. Has left child (3).
 *         Find rightmost of left subtree (which is 3).
 *         Wire 3.right to curr.right: `3.right = 4`.
 *         Move left subtree to right: `2.right = 3`. `2.left = null`.
 *         Move `curr` to `curr.right` (3).
 *
 * Step 3: `curr` = 3. No left child. Move `curr` to `curr.right` (4).
 * Step 4: `curr` = 4. No left child. Move `curr` to `curr.right` (5).
 * Step 5: `curr` = 5. No left child. Move `curr` to `curr.right` (6).
 * Step 6: `curr` = 6. No left child. Move to null. Loop ends.
 *
 * Final Flattened Tree: 1 -> 2 -> 3 -> 4 -> 5 -> 6 (All left pointers are null).
 *
 * Pitfalls: 
 * - In Approach 2 (Reverse Post-Order), defining the `prev` variable inside the recursive method instead 
 *   of as an instance variable. It must persist globally across recursive unwinds!
 * - In Approach 3 (Morris), forgetting to set `curr.left = null` after moving the subtree to the right. 
 *   This leaves cyclical or garbage pointers, ruining the linked-list definition.
 *
 * Pattern Recognition: 
 * "When flattening a tree into a specific order without a stack -> Find the predecessor/successor 
 *  leaf in that order, and manually draw a pointer from it to the next branch."
 *
 * Interview Script:
 * "A standard preorder traversal overwrites right pointers, causing data loss. I could use a stack or 
 * traverse backwards to avoid this, taking O(N) space. But we can achieve O(1) space. In preorder, the 
 * right subtree is always visited immediately after the very last node of the left subtree. For every node, 
 * I'll find that rightmost node in its left subtree, connect it to the right child, and shift the left 
 * subtree over to the right. This flattens the tree in-place as we walk down it."
 *
 *
 * 5. FOLLOW-UPS
 * ----------------------------------------------------------------------------
 * Q1: How would this change if we wanted to flatten it into a DOUBLY linked list in-place?
 * A1: The Morris approach still works, but we also need to maintain the back-links. When we do 
 *     `curr.right = curr.left`, we must also do `curr.left.left = curr` (using the left pointer as 
 *     the `prev` link of the DLL). We'd also need a separate pass or track the tail to ensure all 
 *     `prev` links are fully connected.
 *
 * Q2: What if we needed to flatten it based on INORDER traversal instead of Preorder?
 * A2: That is actually easier! We can use a standard Morris In-Order traversal or a recursive 
 *     In-Order traversal with a `prev` pointer. As we visit the node, we wire `prev.right = curr` 
 *     and `curr.left = null`, updating `prev = curr`.
 */

public class FlattenBinaryTreeStudy {

    // Definition for a binary tree node.
    public static class TreeNode {
        int val;
        TreeNode left;
        TreeNode right;
        TreeNode() {}
        TreeNode(int val) { this.val = val; }
        TreeNode(int val, TreeNode left, TreeNode right) {
            this.val = val;
            this.left = left;
            this.right = right;
        }
    }

    /**
     * APPROACH 3: O(1) SPACE POINTER REWIRING (The Optimal Flex)
     * Time: O(N) | Space: O(1)
     */
    public void flattenOptimal(TreeNode root) {
        TreeNode curr = root;
        
        while (curr != null) {
            // If there's a left subtree, it needs to be flattened and inserted 
            // BEFORE the right subtree in a Preorder sequence.
            if (curr.left != null) {
                // 1. Find the rightmost node of the left subtree.
                // This is the node that conceptually precedes the right subtree in Preorder.
                TreeNode rightmost = curr.left;
                while (rightmost.right != null) {
                    rightmost = rightmost.right;
                }
                
                // 2. Wire the rightmost node's right pointer to the current node's right child.
                // This preserves the entire right branch from being lost!
                rightmost.right = curr.right;
                
                // 3. Move the entire left subtree over to the right side.
                curr.right = curr.left;
                
                // 4. Nullify the left pointer to conform to the singly-linked list requirement.
                curr.left = null;
            }
            
            // Move to the next node. 
            // Because we shifted the left subtree to the right, this traverses what WAS the left child.
            curr = curr.right;
        }
    }

    /**
     * APPROACH 2: REVERSE POST-ORDER TRAVERSAL (The Elegant Recursion)
     * Time: O(N) | Space: O(N) bounded by recursion stack
     */
    // Instance variable to track the previously processed node
    private TreeNode prevNode = null;

    public void flattenRecursive(TreeNode root) {
        // Reset state in case this method is called multiple times on the same object instance
        prevNode = null;
        reversePostOrder(root);
    }

    private void reversePostOrder(TreeNode node) {
        // Base case
        if (node == null) {
            return;
        }

        // 1. Traverse Right FIRST
        reversePostOrder(node.right);
        
        // 2. Traverse Left SECOND
        reversePostOrder(node.left);

        // 3. Process the Node
        // Wire the current node's right pointer to the previously processed node
        node.right = prevNode;
        // Nullify the left pointer
        node.left = null;
        
        // The current node becomes the "prevNode" for the caller above it in the stack
        prevNode = node;
    }

    /**
     * Helper method to verify the flattened list in main()
     * Validates that all left pointers are null and prints the right pointers.
     */
    private static void printFlattenedTree(TreeNode node) {
        TreeNode current = node;
        boolean isValid = true;
        
        System.out.print("Flattened Path: ");
        while (current != null) {
            System.out.print(current.val + (current.right != null ? " -> " : ""));
            if (current.left != null) {
                isValid = false; // A valid flattened tree must have strictly null left pointers
            }
            current = current.right;
        }
        System.out.println("\nAll left pointers are null? " + isValid);
    }

    /**
     * Main method to cross-check approaches against edge cases.
     */
    public static void main(String[] args) {
        FlattenBinaryTreeStudy study = new FlattenBinaryTreeStudy();

        System.out.println("--- APPROACH 3: O(1) SPACE OPTIMAL ---");
        // Standard Case: 
        //       1
        //      / \
        //     2   5
        //    / \   \
        //   3   4   6
        TreeNode root1 = new TreeNode(1,
                new TreeNode(2, new TreeNode(3), new TreeNode(4)),
                new TreeNode(5, null, new TreeNode(6))
        );
        study.flattenOptimal(root1);
        printFlattenedTree(root1); // Expected: 1 -> 2 -> 3 -> 4 -> 5 -> 6


        System.out.println("\n--- APPROACH 2: REVERSE POST-ORDER ---");
        // Re-creating the identical tree to test recursive approach
        TreeNode root2 = new TreeNode(1,
                new TreeNode(2, new TreeNode(3), new TreeNode(4)),
                new TreeNode(5, null, new TreeNode(6))
        );
        study.flattenRecursive(root2);
        printFlattenedTree(root2); // Expected: 1 -> 2 -> 3 -> 4 -> 5 -> 6


        System.out.println("\n--- EDGE CASE: FULLY LEFT SKEWED ---");
        // Tests heavy shifting logic
        //     1
        //    /
        //   2
        //  /
        // 3
        TreeNode root3 = new TreeNode(1, new TreeNode(2, new TreeNode(3), null), null);
        study.flattenOptimal(root3);
        printFlattenedTree(root3); // Expected: 1 -> 2 -> 3


        System.out.println("\n--- EDGE CASE: ALREADY FLATTENED ---");
        // Ensure algorithm gracefully handles right-skewed trees with zero work
        // 1 -> 2 -> 3
        TreeNode root4 = new TreeNode(1, null, new TreeNode(2, null, new TreeNode(3)));
        study.flattenOptimal(root4);
        printFlattenedTree(root4); // Expected: 1 -> 2 -> 3
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core Pattern: In-Place Traversal Pointer Rewiring.
 * Key Observation: In Preorder, the right subtree is processed immediately after the 
 *                  rightmost node of the left subtree. Draw a bridge between them.
 * Memorize: `rightmost = curr.left; while(rightmost.right != null); rightmost.right = curr.right; curr.right = curr.left; curr.left = null;`
 * Most Common Trap: Trying to recursively overwrite `root.right` in a standard Preorder 
 *                   traversal without storing `root.right` in a temporary variable first, 
 *                   resulting in the permanent deletion of the right half of the tree.
 * One-Line Mental Trigger: "Flatten Tree? Wire left's rightmost leaf to right child, then shift left to right."
 * ============================================================================
 */

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * ============================================================================
 * FLATTEN BINARY TREE TO LINKED LIST
 * ============================================================================
 *
 * Given a binary tree, flatten it in-place into a linked list.
 *
 * The linked list must follow PREORDER traversal:
 *
 *      Node -> Left -> Right
 *
 *
 * After flattening:
 *
 *      left == null
 *
 * and:
 *
 *      right -> next node in preorder
 *
 *
 * ============================================================================
 * EXAMPLE
 * ============================================================================
 *
 * Original tree:
 *
 *              1
 *             / \
 *            2   5
 *           / \   \
 *          3   4   6
 *
 *
 * Preorder:
 *
 *      1 -> 2 -> 3 -> 4 -> 5 -> 6
 *
 *
 * Flattened tree:
 *
 *      1
 *       \
 *        2
 *         \
 *          3
 *           \
 *            4
 *             \
 *              5
 *               \
 *                6
 *
 *
 * Every left pointer must be null.
 *
 *
 * ============================================================================
 * IMPORTANT OBSERVATION
 * ============================================================================
 *
 * The required linked-list order is exactly PREORDER.
 *
 * So the real problem is:
 *
 *      "How can I rearrange the existing pointers so that the tree's
 *       preorder sequence becomes a right-only chain?"
 *
 *
 * We are NOT creating new nodes.
 *
 * We are reusing the existing TreeNode objects.
 *
 *
 * ============================================================================
 * APPROACHES
 * ============================================================================
 *
 * 1. Recursive + previous node
 *      Time:  O(N)
 *      Space: O(H)
 *
 * 2. Iterative + explicit stack
 *      Time:  O(N)
 *      Space: O(H)
 *
 * 3. Morris-style pointer manipulation
 *      Time:  O(N)
 *      Space: O(1)
 *
 * The iterative solution is usually the easiest to explain in an interview.
 * The O(1) solution is the most space-efficient.
 */
public class FlattenBinaryTreeToLinkedList {

    // ========================================================================
    // TREE NODE
    // ========================================================================

    static class TreeNode {

        int data;

        TreeNode left;
        TreeNode right;

        TreeNode(int data) {
            this.data = data;
        }

        TreeNode(int data, TreeNode left, TreeNode right) {
            this.data = data;
            this.left = left;
            this.right = right;
        }
    }


    // ========================================================================
    // APPROACH 1: RECURSIVE
    // ========================================================================

    /**
     * Flatten the tree recursively.
     *
     * ------------------------------------------------------------------------
     * IDEA
     * ------------------------------------------------------------------------
     *
     * We want preorder:
     *
     *      Node -> Left -> Right
     *
     * Suppose:
     *
     *              1
     *             / \
     *            2   5
     *           / \   \
     *          3   4   6
     *
     * We want:
     *
     *      1 -> 2 -> 3 -> 4 -> 5 -> 6
     *
     *
     * One way to think about this is:
     *
     *      Flatten left subtree
     *      Flatten right subtree
     *      Connect them:
     *
     *          current
     *             |
     *          left list
     *             |
     *          right list
     *
     *
     * The tricky part is finding the last node of the flattened left
     * subtree so that we can connect it to the right subtree.
     *
     *
     * ------------------------------------------------------------------------
     * COMPLEXITY
     * ------------------------------------------------------------------------
     *
     * Time:  O(N)
     *
     * Space: O(H)
     *
     * H = height of the tree because of recursion.
     */
    public void flattenRecursive(TreeNode root) {

        if (root == null) {
            return;
        }

        /*
         * First flatten both subtrees.
         *
         * We save the original right subtree before modifying pointers.
         *
         * This is IMPORTANT.
         *
         * Once we change root.right, the original right subtree would
         * otherwise become inaccessible.
         */
        TreeNode originalRightSubtree = root.right;

        flattenRecursive(root.left);
        flattenRecursive(originalRightSubtree);

        /*
         * After recursion:
         *
         *      root.left
         *
         * points to the flattened left list.
         *
         *      originalRightSubtree
         *
         * points to the flattened right list.
         */

        /*
         * If there is no left subtree, the current structure is already:
         *
         *      root -> right
         *
         * so there is nothing to rearrange.
         */
        if (root.left == null) {
            return;
        }

        /*
         * Find the LAST node of the flattened left subtree.
         *
         * Since it is already flattened, we can follow right pointers.
         */
        TreeNode leftSubtreeTail = root.left;

        while (leftSubtreeTail.right != null) {
            leftSubtreeTail = leftSubtreeTail.right;
        }

        /*
         * Connect:
         *
         *      root
         *        \
         *        left list
         *            \
         *          right list
         */
        leftSubtreeTail.right = originalRightSubtree;

        /*
         * The left pointer must always be null in the final linked list.
         */
        root.right = root.left;
        root.left = null;
    }


    // ========================================================================
    // APPROACH 2: ITERATIVE + STACK
    // ========================================================================

    /**
     * Flatten using an explicit stack.
     *
     * This is probably the easiest solution to explain during an interview.
     *
     *
     * ------------------------------------------------------------------------
     * IDEA
     * ------------------------------------------------------------------------
     *
     * Preorder traversal:
     *
     *      Node -> Left -> Right
     *
     * A stack is LIFO.
     *
     * Therefore:
     *
     *      Push Right first.
     *      Push Left second.
     *
     * Then Left will be popped first.
     *
     *
     * But instead of simply collecting the preorder result, we immediately
     * connect the nodes together using right pointers.
     *
     *
     * Example:
     *
     *              1
     *             / \
     *            2   5
     *           / \
     *          3   4
     *
     * Stack initially:
     *
     *      [1]
     *
     * Pop 1.
     *
     * Push 5, then 2:
     *
     *      [5, 2]
     *
     * Pop 2.
     *
     * Push 4, then 3:
     *
     *      [5, 4, 3]
     *
     * Therefore order becomes:
     *
     *      1 -> 2 -> 3 -> 4 -> 5
     *
     *
     * ------------------------------------------------------------------------
     * COMPLEXITY
     * ------------------------------------------------------------------------
     *
     * Time:  O(N)
     *
     * Space: O(H)
     *
     * In the worst case the stack can contain O(N) nodes.
     */
    public void flattenIterative(TreeNode root) {

        if (root == null) {
            return;
        }

        /*
         * Stack stores nodes that still need to be processed.
         */
        Deque<TreeNode> nodeStack = new ArrayDeque<>();

        nodeStack.push(root);

        /*
         * This represents the previous node in preorder.
         *
         * Every new node will be attached to previousNode.right.
         */
        TreeNode previousNode = null;

        while (!nodeStack.isEmpty()) {

            /*
             * Get the next node in preorder.
             */
            TreeNode currentNode = nodeStack.pop();

            /*
             * If we already processed a node, connect it to currentNode.
             *
             * This creates:
             *
             *      previousNode -> currentNode
             *
             * using the right pointer.
             */
            if (previousNode != null) {
                previousNode.left = null;
                previousNode.right = currentNode;
            }

            /*
             * Push RIGHT first.
             *
             * Stack is LIFO, so LEFT will come out first.
             */
            if (currentNode.right != null) {
                nodeStack.push(currentNode.right);
            }

            /*
             * Push LEFT second.
             */
            if (currentNode.left != null) {
                nodeStack.push(currentNode.left);
            }

            /*
             * Current node becomes the previous node for the next
             * preorder node.
             */
            previousNode = currentNode;
        }

        /*
         * The final node has no next node.
         *
         * Therefore its right pointer should be null.
         *
         * Its left pointer must also be null.
         */
        if (previousNode != null) {
            previousNode.left = null;
            previousNode.right = null;
        }
    }


    // ========================================================================
    // APPROACH 3: MORRIS-STYLE O(1) SPACE
    // ========================================================================

    /**
     * Flatten the tree in O(1) EXTRA SPACE.
     *
     * ------------------------------------------------------------------------
     * CORE IDEA
     * ------------------------------------------------------------------------
     *
     * At every node:
     *
     *              current
     *              /     \
     *             L       R
     *
     * Preorder requires:
     *
     *      current -> L -> ... -> R
     *
     * So if current has a left subtree, we need to place the original
     * right subtree AFTER the entire left subtree.
     *
     *
     * Therefore:
     *
     *      1. Find the RIGHTMOST node of the left subtree.
     *
     *      2. Save current.right.
     *
     *      3. Connect:
     *
     *             rightmostLeftNode.right = originalRightSubtree
     *
     *      4. Move the left subtree to the right:
     *
     *             current.right = current.left
     *
     *      5. Remove the left pointer:
     *
     *             current.left = null
     *
     *      6. Continue with current.right.
     *
     *
     * ------------------------------------------------------------------------
     * EXAMPLE
     * ------------------------------------------------------------------------
     *
     *              1
     *             / \
     *            2   5
     *           / \
     *          3   4
     *
     *
     * Current = 1
     *
     * Left subtree:
     *
     *              2
     *             / \
     *            3   4
     *
     * Rightmost node of left subtree = 4.
     *
     * Original right subtree = 5.
     *
     * Connect:
     *
     *      4.right = 5
     *
     * Then:
     *
     *      1.right = 2
     *      1.left = null
     *
     * Tree becomes:
     *
     *      1
     *       \
     *        2
     *       / \
     *      3   4
     *           \
     *            5
     *
     * Continue from 2.
     *
     * Eventually:
     *
     *      1 -> 2 -> 3 -> 4 -> 5
     *
     *
     * ------------------------------------------------------------------------
     * WHY O(1) SPACE?
     * ------------------------------------------------------------------------
     *
     * We don't use:
     *
     *      - recursion
     *      - stack
     *      - additional nodes
     *
     * We only use a few TreeNode references.
     *
     *
     * ------------------------------------------------------------------------
     * TIME COMPLEXITY
     * ------------------------------------------------------------------------
     *
     * O(N)
     *
     * Although we sometimes walk through the rightmost part of a subtree,
     * each relevant pointer is rearranged in a way that keeps the total
     * work linear.
     *
     *
     * SPACE COMPLEXITY
     * ------------------------------------------------------------------------
     *
     * O(1) extra space.
     */
    public void flattenOptimal(TreeNode root) {

        TreeNode currentNode = root;

        while (currentNode != null) {

            /*
             * If there is no left subtree:
             *
             *      current
             *          \
             *           right
             *
             * the structure is already correct.
             *
             * Just move forward.
             */
            if (currentNode.left == null) {

                currentNode = currentNode.right;

                continue;
            }

            /*
             * Find the rightmost node of the LEFT subtree.
             *
             * Example:
             *
             *              1
             *             /
             *            2
             *             \
             *              4
             *
             * predecessor = 4
             *
             * This is the node that should be immediately followed
             * by the original right subtree.
             */
            TreeNode rightmostLeftNode = currentNode.left;

            while (rightmostLeftNode.right != null) {
                rightmostLeftNode = rightmostLeftNode.right;
            }

            /*
             * Save the original right subtree before overwriting
             * currentNode.right.
             *
             * Example:
             *
             *              1
             *             / \
             *            2   5
             *
             * originalRightSubtree = 5
             */
            TreeNode originalRightSubtree = currentNode.right;

            /*
             * Put the original right subtree after the entire left subtree.
             *
             * Before:
             *
             *      2 ... 4
             *
             *      5
             *
             * After:
             *
             *      2 ... 4 -> 5
             */
            rightmostLeftNode.right = originalRightSubtree;

            /*
             * Move the left subtree to the right.
             *
             * Before:
             *
             *              current
             *              /    \
             *             left   oldRight
             *
             * After:
             *
             *              current
             *                 \
             *                 left
             *                    \
             *                   ... -> oldRight
             */
            currentNode.right = currentNode.left;

            /*
             * The flattened list must NEVER have a left pointer.
             */
            currentNode.left = null;

            /*
             * Continue down the newly-created right chain.
             */
            currentNode = currentNode.right;
        }
    }


    // ========================================================================
    // HELPER: BUILD EXAMPLE TREE
    // ========================================================================

    /**
     * Creates:
     *
     *              1
     *             / \
     *            2   5
     *           / \   \
     *          3   4   6
     */
    private static TreeNode createExampleTree() {

        TreeNode node3 = new TreeNode(3);
        TreeNode node4 = new TreeNode(4);

        TreeNode node2 =
                new TreeNode(2, node3, node4);

        TreeNode node6 = new TreeNode(6);

        TreeNode node5 =
                new TreeNode(5, null, node6);

        return new TreeNode(
                1,
                node2,
                node5
        );
    }


    // ========================================================================
    // HELPER: PRINT FLATTENED TREE
    // ========================================================================

    /**
     * Prints the flattened structure by following right pointers.
     *
     * Expected:
     *
     *      1 -> 2 -> 3 -> 4 -> 5 -> 6
     */
    private static void printFlattenedTree(TreeNode root) {

        TreeNode currentNode = root;

        while (currentNode != null) {

            /*
             * Verify the requirement:
             *
             * Every left pointer must be null.
             */
            if (currentNode.left != null) {
                throw new IllegalStateException(
                        "Flattened tree contains a non-null left pointer."
                );
            }

            System.out.print(currentNode.data);

            if (currentNode.right != null) {
                System.out.print(" -> ");
            }

            currentNode = currentNode.right;
        }

        System.out.println();
    }


    // ========================================================================
    // MAIN
    // ========================================================================

    public static void main(String[] args) {

        FlattenBinaryTreeToLinkedList solution =
                new FlattenBinaryTreeToLinkedList();

        /*
         * ------------------------------------------------------------
         * Test 1: Recursive
         * ------------------------------------------------------------
         */
        TreeNode recursiveTree =
                createExampleTree();

        solution.flattenRecursive(recursiveTree);

        System.out.print("Recursive: ");
        printFlattenedTree(recursiveTree);

        /*
         * Expected:
         *
         * Recursive: 1 -> 2 -> 3 -> 4 -> 5 -> 6
         */


        /*
         * ------------------------------------------------------------
         * Test 2: Iterative
         * ------------------------------------------------------------
         */
        TreeNode iterativeTree =
                createExampleTree();

        solution.flattenIterative(iterativeTree);

        System.out.print("Iterative: ");
        printFlattenedTree(iterativeTree);

        /*
         * Expected:
         *
         * Iterative: 1 -> 2 -> 3 -> 4 -> 5 -> 6
         */


        /*
         * ------------------------------------------------------------
         * Test 3: O(1) Space
         * ------------------------------------------------------------
         */
        TreeNode optimalTree =
                createExampleTree();

        solution.flattenOptimal(optimalTree);

        System.out.print("Optimal:   ");
        printFlattenedTree(optimalTree);

        /*
         * Expected:
         *
         * Optimal:   1 -> 2 -> 3 -> 4 -> 5 -> 6
         */


        /*
         * ------------------------------------------------------------
         * Edge Case: Empty tree
         * ------------------------------------------------------------
         */
        TreeNode emptyTree = null;

        solution.flattenOptimal(emptyTree);

        System.out.println("Empty tree: handled");


        /*
         * ------------------------------------------------------------
         * Edge Case: Single node
         * ------------------------------------------------------------
         */
        TreeNode singleNode = new TreeNode(42);

        solution.flattenOptimal(singleNode);

        System.out.print("Single node: ");
        printFlattenedTree(singleNode);

        /*
         * Expected:
         *
         * Single node: 42
         */
    }
}


/**
 * ============================================================================
 * INTERVIEW CHEAT SHEET
 * ============================================================================
 *
 *
 * PROBLEM:
 *
 *      Binary Tree
 *          ↓
 *      Preorder sequence
 *          ↓
 *      Right-only linked list
 *
 *
 * ============================================================================
 * PREORDER
 * ============================================================================
 *
 *      Node -> Left -> Right
 *
 *
 * ============================================================================
 * APPROACH 1: RECURSION
 * ============================================================================
 *
 * Flatten left subtree.
 * Flatten right subtree.
 *
 * Then:
 *
 *      root
 *        \
 *        left list
 *            \
 *           right list
 *
 * Time:  O(N)
 * Space: O(H)
 *
 *
 * ============================================================================
 * APPROACH 2: STACK
 * ============================================================================
 *
 * Simulate preorder.
 *
 *      Pop
 *      Connect to previous
 *      Push Right
 *      Push Left
 *
 * Why push Right first?
 *
 *      Stack = LIFO
 *
 * So Left must be pushed last.
 *
 * Time:  O(N)
 * Space: O(H)
 *
 *
 * ============================================================================
 * APPROACH 3: O(1) SPACE
 * ============================================================================
 *
 * At every node:
 *
 * If LEFT is null:
 *
 *      move right.
 *
 *
 * Otherwise:
 *
 *      1. Find rightmost node of LEFT subtree.
 *
 *      2. Save original RIGHT subtree.
 *
 *      3. Connect:
 *
 *             rightmostLeftNode.right = originalRightSubtree
 *
 *      4. Move LEFT subtree to RIGHT:
 *
 *             current.right = current.left
 *
 *      5. Remove LEFT:
 *
 *             current.left = null
 *
 *      6. Continue.
 *
 *
 * ============================================================================
 * MEMORY TRIGGER
 * ============================================================================
 *
 * "Preorder flattening:"
 *
 *      Find left subtree's tail
 *      Attach old right after it
 *      Move left to right
 *      Clear left
 *
 *
 * ============================================================================
 * INTERVIEW EXPLANATION
 * ============================================================================
 *
 * "The required linked-list order is preorder, so I need to preserve
 * Node-Left-Right ordering while changing the pointers.
 *
 * If the current node has a left subtree, the flattened left subtree
 * must come immediately after the current node, and the original right
 * subtree must come after the flattened left subtree.
 *
 * I find the rightmost node of the left subtree, connect its right pointer
 * to the original right subtree, then move the current node's left subtree
 * to its right pointer and set the left pointer to null.
 *
 * I then continue from the new right child.
 *
 * This modifies the tree in-place and uses O(1) extra space."
 *
 * ============================================================================
 */
