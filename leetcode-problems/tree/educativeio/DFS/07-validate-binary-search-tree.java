import java.util.ArrayDeque;
import java.util.Deque;

/**
 * ============================================================================
 * STUDY NOTE: VALIDATE BINARY SEARCH TREE (BST)
 * ============================================================================
 *
 * 1. CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * - "Are duplicate values allowed in this BST?" 
 *   (Crucial: Some definitions put duplicates on the left, some on the right. The prompt says 'less than' and 'greater than', implying strict inequality. Duplicates mean invalid).
 * - "What data type are the values, and can they be Integer.MIN_VALUE or Integer.MAX_VALUE?" 
 *   (The constraints here say -10^4 to 10^4, so an `int` with Long boundaries works. But asking this shows you anticipate the classic overflow trap in unrestricted environments).
 * - "Does an empty tree count as a valid BST?" 
 *   (Constraints say minimum 1 node, but it's good defensive programming to establish that an empty tree is vacuously true).
 *
 *
 * 2. THE REASONING JOURNEY
 * ----------------------------------------------------------------------------
 * Core Constraint: 
 * The defining rule of a BST is not just local (node > left and node < right), it is GLOBAL. 
 * A node deep in the right subtree MUST still be strictly greater than the root. The bottleneck 
 * is figuring out how to enforce these global rules without constantly re-traversing the tree 
 * to verify every single ancestor.
 *
 * Approach 1: The Rookie Mistake (Local Checking Only) - FAILS
 * What I'd naturally try: Just check `node.val > node.left.val` and `node.val < node.right.val` recursively.
 * Why it fails: It completely ignores ancestors. In the tree [5, 1, 6, null, null, 3, 7], the node 
 * `3` is less than `6`, which locally looks fine. But `3` is in the RIGHT subtree of `5`, so it 
 * MUST be greater than `5`. This approach incorrectly returns true.
 *
 * Approach 2: Brute Force (Top-Down Global Checking)
 * What I'd naturally try: To fix the local trap, for every node, I'll recursively find the maximum 
 * value in its entire left subtree and ensure my node is bigger than it. Then find the minimum 
 * value in the right subtree and ensure my node is smaller than it.
 * Why it works: It rigorously enforces the global definition at every level.
 * Why it's too slow/costly: 
 * - Time Complexity: O(N^2) — because to validate the root, you scan all N nodes. To validate 
 *   the root's children, you scan their N/2 nodes again. We are doing massive repeat traversals.
 * - Space Complexity: O(N) — because the call stack can go N levels deep on a skewed tree.
 * 
 * Approach 3: In-Order Traversal (The Property Trick)
 * What I'd naturally try: A magical property of a valid BST is that an In-Order traversal 
 * (Left -> Node -> Right) visits the nodes in strictly ascending, sorted order. I can just do an 
 * in-order traversal and keep track of the `previous` value I saw. If `current <= previous`, it's invalid.
 * Why it works: It perfectly maps the 2D tree structure to a 1D sorted validation.
 * What property removes the bottleneck: We only visit each node once, validating strictly against 
 * the predecessor.
 * - Time Complexity: O(N) — because every node is visited exactly once.
 * - Space Complexity: O(N) — because of the call stack (or explicit Stack for iterative). 
 * 
 * Approach 4: Top-Down Range Passing (Optimal & Most Elegant)
 * What I'd naturally try: Instead of a node looking *down* at its whole subtree (Brute Force), or 
 * flattening the tree (In-Order), what if the parent just tells the child the rules? When branching 
 * left, the parent says: "You can be anything, as long as you are less than me." We pass a 
 * (MIN, MAX) valid range down the recursion.
 * Why it works: It flips the logic. Ancestors dictate the boundaries for their descendants. 
 * What property removes the bottleneck: State passing. By carrying the `min` and `max` limits 
 * down the call stack, we validate global rules in O(1) time per node.
 * - Time Complexity: O(N) — because we visit every node once, doing an O(1) bound check.
 * - Space Complexity: O(N) — for the call stack in the worst-case skewed tree (O(log N) balanced).
 *
 * The Interview Choice:
 * Write Approach 4 (Top-Down Range Passing). It is the most robust, allows for early-exit 
 * short-circuiting, and shows a deep understanding of passing state down a recursive stack. 
 * Use `Integer` wrapper objects for bounds instead of `Long.MIN_VALUE` to show you know how to 
 * handle absolute maximum integer inputs without type-hackery.
 *
 *
 * 3. EDGE CASES
 * ----------------------------------------------------------------------------
 * 1. Single Node Tree -> Unconstrained, automatically valid.
 * 2. Tree with exact duplicate values -> Must return false.
 * 3. The "Local Trap" Tree -> A node is valid locally with its parent but violates an ancestor.
 * 4. Extreme values (Node value == Integer.MAX_VALUE) -> Tests if our bound variables overflow.
 *
 *
 * 4. DRY RUN (Top-Down Range Passing)
 * ----------------------------------------------------------------------------
 * The "Local Trap" Tree:
 *       5
 *      / \
 *     1   6
 *        / \
 *       3   7
 *
 * Call Stack Trace (Initial bounds: MIN=null, MAX=null):
 * 1. dfs(node=5, min=null, max=null): 
 *    - 5 is within bounds.
 *    - Recurse Left: dfs(node=1, min=null, max=5).
 *    - Recurse Right: dfs(node=6, min=5, max=null).
 *
 * 2. [Left Branch] dfs(node=1, min=null, max=5):
 *    - 1 < 5. Valid. Leaves are null. Returns true.
 *
 * 3. [Right Branch] dfs(node=6, min=5, max=null):
 *    - 6 > 5. Valid.
 *    - Recurse Left: dfs(node=3, min=5, max=6)   <-- NOTICE HOW THE ANCESTOR '5' CARRIED DOWN!
 *    - Recurse Right: dfs(node=7, min=6, max=null)
 *
 * 4. [Rogue Node] dfs(node=3, min=5, max=6):
 *    - Is 3 > min(5)? FALSE!
 *    - Immediately returns false, short-circuiting the whole process.
 * Final Result: false.
 *
 * Pitfalls: 
 * - Initializing bounds with Integer.MIN_VALUE / Integer.MAX_VALUE. If the tree actually contains 
 *   a node with `Integer.MAX_VALUE`, the check `node.val < MAX` fails incorrectly. Using `null` 
 *   (or `Long`, if strictly dealing with integers) is required.
 * - Updating the wrong bound: When branching Left, the MAX updates. When branching Right, the MIN updates.
 *
 * Pattern Recognition: 
 * "When a tree node's validity depends on ALL of its ancestors -> Think Top-Down DFS passing a Range/State."
 *
 * Interview Script:
 * "Checking if a node is just larger than its left child isn't enough, because it might violate a 
 * rule set by a higher ancestor. To enforce global rules, I'll pass a valid numerical range (min, max) 
 * down a recursive DFS. Whenever we branch left, the current node's value becomes the new maximum limit. 
 * Whenever we branch right, it becomes the new minimum limit. This validates the tree in O(N) time 
 * and O(H) space without repeated traversals."
 *
 *
 * 5. FOLLOW-UPS
 * ----------------------------------------------------------------------------
 * Q1: If the tree is deeply skewed and recursion crashes, how do we fix it?
 * A1: We convert the Top-Down DFS to an Iterative approach using 3 parallel Stacks (or a Stack of 
 *     custom Objects): `Stack<TreeNode>`, `Stack<Integer> mins`, and `Stack<Integer> maxes`. This 
 *     moves the memory from the thread's call stack to the heap.
 *
 * Q2: What if we cannot use any extra space at all, not even O(N) stack memory?
 * A2: We can use a Morris In-Order Traversal. It temporarily modifies the tree's null leaf pointers 
 *     to point back to their in-order successors. This achieves O(1) space and O(N) time, verifying 
 *     the ascending property, though it's not thread-safe.
 */

public class ValidateBSTStudy {

    // Definition for a binary tree node.
    public static class TreeNode {
        int val;
        TreeNode left;
        TreeNode right;
        TreeNode(int val) { this.val = val; }
        TreeNode(int val, TreeNode left, TreeNode right) {
            this.val = val;
            this.left = left;
            this.right = right;
        }
    }

    /**
     * APPROACH 4: TOP-DOWN RANGE PASSING (The optimal, professional choice)
     * Time: O(N) | Space: O(N)
     */
    public boolean isValidBST(TreeNode root) {
        // Start with null boundaries, representing negative and positive infinity.
        return validateRange(root, null, null);
    }

    private boolean validateRange(TreeNode node, Integer minBoundary, Integer maxBoundary) {
        // Base case: an empty node doesn't violate any rules
        if (node == null) {
            return true;
        }

        // Check against the boundaries handed down by ancestors
        if (minBoundary != null && node.val <= minBoundary) {
            return false;
        }
        if (maxBoundary != null && node.val >= maxBoundary) {
            return false;
        }

        // Branch Left: The current node's value becomes the strict upper ceiling (MAX)
        boolean isLeftValid = validateRange(node.left, minBoundary, node.val);
        
        // Branch Right: The current node's value becomes the strict lower floor (MIN)
        boolean isRightValid = validateRange(node.right, node.val, maxBoundary);

        // Both subtrees must strictly adhere to the rules
        return isLeftValid && isRightValid;
    }

    /**
     * APPROACH 3: IN-ORDER TRAVERSAL (The clever property-based alternative)
     * Time: O(N) | Space: O(N)
     */
    private Integer prevValue = null; // Tracks the previous node in the In-Order sequence

    public boolean isValidBST_InOrder(TreeNode root) {
        prevValue = null; // Reset state for repeated calls
        return inOrderCheck(root);
    }

    private boolean inOrderCheck(TreeNode node) {
        if (node == null) return true;

        // 1. Traverse Left
        if (!inOrderCheck(node.left)) return false;

        // 2. Visit Node: Must be strictly greater than the previously seen value
        if (prevValue != null && node.val <= prevValue) {
            return false;
        }
        prevValue = node.val; // Update the tracking pointer

        // 3. Traverse Right
        return inOrderCheck(node.right);
    }

    /**
     * APPROACH 1: THE ROOKIE MISTAKE (Local checking only)
     * Included strictly to demonstrate WHY it fails in the main method.
     */
    public boolean isValidBST_RookieMistake(TreeNode root) {
        if (root == null) return true;
        
        if (root.left != null && root.left.val >= root.val) return false;
        if (root.right != null && root.right.val <= root.val) return false;
        
        return isValidBST_RookieMistake(root.left) && isValidBST_RookieMistake(root.right);
    }

    /**
     * Main method to cross-check approaches against edge cases.
     */
    public static void main(String[] args) {
        ValidateBSTStudy study = new ValidateBSTStudy();

        // Standard Case: Valid BST
        //       2
        //      / \
        //     1   3
        TreeNode validTree = new TreeNode(2, new TreeNode(1), new TreeNode(3));
        System.out.println("Valid Tree (Optimal): " + study.isValidBST(validTree));     // Expected: true
        System.out.println("Valid Tree (InOrder): " + study.isValidBST_InOrder(validTree)); // Expected: true

        // Edge Case 1: The "Local Trap" 
        //       5
        //      / \
        //     1   6
        //        / \
        //       3   7
        // Node 3 is locally valid (< 6), but violates the global rule (> 5)
        TreeNode localTrap = new TreeNode(5,
                new TreeNode(1),
                new TreeNode(6, new TreeNode(3), new TreeNode(7))
        );
        System.out.println("\nLocal Trap Tree (Rookie Mistake): " + study.isValidBST_RookieMistake(localTrap)); // incorrectly returns true!
        System.out.println("Local Trap Tree (Optimal): " + study.isValidBST(localTrap));             // Expected: false
        System.out.println("Local Trap Tree (InOrder): " + study.isValidBST_InOrder(localTrap));     // Expected: false

        // Edge Case 2: Duplicates
        //       2
        //      / \
        //     2   3
        TreeNode dupTree = new TreeNode(2, new TreeNode(2), new TreeNode(3));
        System.out.println("\nDuplicate Tree (Optimal): " + study.isValidBST(dupTree)); // Expected: false
        
        // Edge Case 3: Extreme Bounds
        // Constraints say -10^4 to 10^4, but this simulates max possible int limits.
        TreeNode extremeTree = new TreeNode(Integer.MAX_VALUE, 
                                            new TreeNode(Integer.MAX_VALUE - 1), 
                                            null);
        System.out.println("Extreme Bounds (Optimal): " + study.isValidBST(extremeTree)); // Expected: true
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core Pattern: Top-Down DFS with State / Range Passing.
 * Key Observation: A BST's validity is governed by ancestors. When you go left, the parent 
 *                  becomes the maximum cap. When you go right, the parent becomes the minimum floor.
 * Memorize: `isValid(node, min, max) -> return isValid(node.left, min, node.val) && isValid(node.right, node.val, max);`
 * Most Common Trap: Using `Integer.MIN_VALUE` / `MAX_VALUE` instead of `null` for boundaries, 
 *                   which fails on test cases where the tree contains actual max integers. 
 *                   Also, checking only local `node > left && node < right` is a fatal logic flaw.
 * One-Line Mental Trigger: "Valid BST? Pass min and max bounds down the tree."
 * ============================================================================
 */

import java.util.*;

class Solution {

    public boolean isValidBST(TreeNode root) {

        Deque<TreeNode> stack = new ArrayDeque<>();
        TreeNode current = root;

        Long prev = null; // tracks previous visited node in inorder

        while (current != null || !stack.isEmpty()) {

            /**
             * Step 1: Go to the leftmost node
             * Keep pushing nodes into stack
             */
            while (current != null) {
                stack.push(current);
                current = current.left;
            }

            /**
             * Step 2: Process the node
             */
            current = stack.pop();

            // ❌ Check BST violation (inorder must be strictly increasing)
            if (prev != null && current.val <= prev) {
                return false;
            }

            // Update prev
            prev = (long) current.val;

            /**
             * Step 3: Move to right subtree
             */
            current = current.right;
        }

        return true; // all nodes satisfied BST condition
    }
}
