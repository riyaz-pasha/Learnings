/**
 * ============================================================================
 * 0. PROBLEM STATEMENT
 * ============================================================================
 * Given the root of a binary tree, check whether it is a symmetric tree. 
 * A symmetric tree refers to a tree that is a mirror of itself, i.e., 
 * symmetric around its root.
 * 
 * Constraints:
 * - The tree contains nodes in the range [1, 500].
 * - -10^3 <= Node.data <= 10^3
 * 
 * ============================================================================
 * 1. CLARIFYING QUESTIONS
 * ============================================================================
 * - "Does symmetry require both the structural shape AND the node values to be identical in reflection?"
 *   Yes, it prevents us from writing a solution that only checks if the tree is structurally balanced.
 * - "If a node has a left child but no right child on the left branch, and a left child but no right child on the right branch, is it symmetric?"
 *   No, a left child on the left branch MUST mirror a right child on the right branch. This clarifies the "opposite direction" traversal rule.
 * - "The constraints say minimum 1 node, so can I skip the `root == null` check?"
 *   Technically yes, but it is industry standard to keep it for defensive programming.
 * - "Do we need to worry about massive trees causing a StackOverflowError?"
 *   With 500 nodes max, recursion is perfectly safe. If the constraint was 10^5 nodes, we'd strongly consider an iterative approach.
 * 
 * ============================================================================
 * 2. THE REASONING JOURNEY
 * ============================================================================
 * [Binding Constraint] 
 * We must compare two distinct branches of a tree simultaneously, but moving in 
 * *opposite* directions. If the left explorer goes Left, the right explorer must go Right.
 * 
 * --- APPROACH 1: BFS with Level-by-Level Palindrome Checks (The "Visual" Try) ---
 * 1. What I'd naturally try: Run a Breadth-First Search (BFS). At each level, collect all 
 *    nodes (including explicit `null`s for missing children) into a List. Then, check if 
 *    the List reads the same forwards and backwards (a palindrome).
 * 2. Why it works: A symmetric tree's horizontal cross-sections are perfect palindromes.
 * 3. Why it's costly: We are allocating a new List for every level, adding string/value 
 *    comparisons, and critically, we have to push `null` values into the queue to maintain 
 *    structural gaps, which bloats the queue size exponentially at the bottom levels.
 * 4. What work is repeated: We build an array just to read it from both ends, when we 
 *    could just traverse the tree in that mirrored order natively.
 * 5. Time Complexity: O(N) — because we visit every node and perform a palindrome check.
 * 6. Space Complexity: O(N) — because we store explicit nulls to maintain structure, the 
 *    level arrays can become artificially large.
 * 
 * [The Core Observation]
 * Instead of dumping a level into an array to compare its ends, we can just spawn two 
 * pointers at the root. Pointer A explores the left subtree, Pointer B explores the right. 
 * If A goes left, B goes right. If A goes right, B goes left. If they always match, it's symmetric.
 * 
 * --- APPROACH 2: Recursive Dual-Traversal (The Optimal DFS) ---
 * 1. How it works: Create a helper function `isMirror(node1, node2)`. Check if their values 
 *    match. Then recursively ask: is `node1.left` a mirror of `node2.right`? AND is `node1.right` 
 *    a mirror of `node2.left`?
 * 2. Why it's optimal: It natively performs the opposite-direction traversal. It avoids all 
 *    list allocations and completely ignores the concept of "levels", relying on the call 
 *    stack to enforce structure.
 * 3. Time Complexity: O(N) — because in the worst case, we visit every node exactly once to 
 *    verify the entire tree is symmetric.
 * 4. Space Complexity: O(H) — where H is the height of the tree. In the worst case (a skewed 
 *    tree), this is O(N) due to the recursion call stack.
 * 
 * --- APPROACH 3: Iterative Dual-Traversal (The Stack-Safe Optimal) ---
 * 1. How it works: If we can't use recursion, we use a Queue. Push `root.left` and `root.right`. 
 *    Pop two nodes. Compare them. If they match, push their children in mirrored pairs: 
 *    `(left.left, right.right)` then `(left.right, right.left)`.
 * 2. Why it's useful: It avoids the Call Stack entirely, which is crucial if the tree depth 
 *    exceeds JVM limits (e.g., 10,000+ depth), while maintaining the exact same logic.
 * 3. Time Complexity: O(N) — because every node is queued and dequeued once.
 * 4. Space Complexity: O(N) — because the Queue will hold at most the widest level of the tree.
 * 
 * [Which one I'd write in an interview]
 * Approach 2 (Recursive DFS). It is the most elegant, expected answer for this problem. 
 * I would write it, but explicitly mention Approach 3 (Iterative) as my backup plan if 
 * the interviewer asks about handling massive, skewed trees that cause StackOverflowErrors.
 * 
 * ============================================================================
 * 3. EDGE CASES
 * ============================================================================
 * - Null Root: Technically outside constraints, but safely returns `true`.
 * - Single Node Tree: Root with no children. Evaluates `isMirror(null, null)`, returns `true`.
 * - Structurally Asymmetric: Left node has a left child. Right node has a left child. 
 *   (It must be a right child to be symmetric!).
 * - Value Asymmetric: Perfect mirror shape, but a node on the left has value 5 and the 
 *   mirrored node on the right has value 8.
 * 
 * ============================================================================
 * 4. KEY INSIGHT, DIAGRAMS & DRY RUN
 * ============================================================================
 * [Key Insight]
 * A single tree traversal is not enough. You must conceptualize this as traversing TWO 
 * separate trees simultaneously, where the "left" tree's instructions are inverted for 
 * the "right" tree.
 * 
 * [Examples & Diagram]
 * Symmetric Tree:
 *           1
 *         /   \
 *       2       2
 *      / \     / \
 *     3   4   4   3
 * 
 * [Dry Run (Recursive)]
 * Init: isMirror(root.left (2), root.right (2))
 * 
 * Level 1:
 * - node1=2, node2=2. Values match!
 * - Spawn branch A: isMirror(node1.left (3), node2.right (3))
 * - Spawn branch B: isMirror(node1.right (4), node2.left (4))
 * 
 * Level 2 (Branch A):
 * - node1=3, node2=3. Values match!
 * - Both have null children. isMirror(null, null) returns true. Branch A is true.
 * 
 * Level 2 (Branch B):
 * - node1=4, node2=4. Values match!
 * - Both have null children. isMirror(null, null) returns true. Branch B is true.
 * 
 * Result: True && True -> The tree is symmetric.
 * 
 * [Pitfalls]
 * - NullPointerExceptions: Checking `n1.val == n2.val` before checking if either `n1` or `n2` 
 *   are null. ALWAYS check nulls first.
 * - Comparing `left` to `left`: Writing `isMirror(n1.left, n2.left)`. This just checks if 
 *   two branches are exact clones, not mirrors!
 * 
 * [Pattern Recognition]
 * When you see: "Compare two trees", "Check if trees are identical", "Symmetric".
 * Think: Dual-Node Traversal. Pass two nodes into your recursive function and move them in tandem.
 * 
 * [Interview Script]
 * "To check if the tree is symmetric, we essentially need to verify if the left subtree is a 
 * perfect mirror of the right subtree. I'll use a recursive helper function that takes two nodes. 
 * At each step, I check three things: are both nodes null (true), is only one null (false), and 
 * do their values match. If they match, I recursively cross-check their children: the left child 
 * of node 1 with the right child of node 2, and vice versa. This gives an elegant O(N) time 
 * solution with O(H) space."
 * 
 * ============================================================================
 * 5. FOLLOW-UPS
 * ============================================================================
 * Q: How would you do this without recursion?
 * A: I would use a Queue. I'd enqueue the left and right children of the root as a pair. 
 *    In a loop, I'd poll two nodes, compare them using the same null/value logic, and then 
 *    enqueue their children in mirrored pairs: (n1.left, n2.right) and (n1.right, n2.left).
 * 
 * ============================================================================
 * 6. JAVA CODE
 * ============================================================================
 */

import java.util.LinkedList;
import java.util.Queue;

public class SymmetricTree {

    // Standard Binary Tree Node definition
    public static class TreeNode {
        int val;
        TreeNode left;
        TreeNode right;
        TreeNode(int val) { this.val = val; }
    }

    /**
     * APPROACH 2: Recursive Dual-Traversal (Optimal & Idiomatic)
     */
    public static boolean isSymmetricRecursive(TreeNode root) {
        // A null tree is symmetrically empty
        if (root == null) {
            return true;
        }
        // Launch the dual-traversal on the left and right subtrees
        return isMirror(root.left, root.right);
    }

    /**
     * Helper function to compare two branches simultaneously.
     */
    private static boolean isMirror(TreeNode t1, TreeNode t2) {
        // Base Case 1: Both nodes are null. The mirror is perfectly intact here.
        if (t1 == null && t2 == null) {
            return true;
        }
        
        // Base Case 2: Only one node is null. The structure is asymmetric.
        if (t1 == null || t2 == null) {
            return false;
        }
        
        // Base Case 3: The values don't match. The reflection is broken.
        if (t1.val != t2.val) {
            return false;
        }
        
        // Both nodes exist and their values match. 
        // Now, strictly verify their children in opposing directions.
        // t1's OUTER child must match t2's OUTER child.
        // t1's INNER child must match t2's INNER child.
        return isMirror(t1.left, t2.right) && isMirror(t1.right, t2.left);
    }

    /**
     * APPROACH 3: Iterative Queue (Robust against StackOverflow)
     */
    public static boolean isSymmetricIterative(TreeNode root) {
        if (root == null) {
            return true;
        }
        
        // The queue will hold pairs of nodes that are supposed to mirror each other.
        // Concrete state: Initially holds [Node(2), Node(2)]
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root.left);
        queue.offer(root.right);
        
        while (!queue.isEmpty()) {
            // Always pull out nodes in pairs
            TreeNode t1 = queue.poll();
            TreeNode t2 = queue.poll();
            
            // If both are null, this branch is symmetrically empty, continue checking rest.
            if (t1 == null && t2 == null) continue;
            
            // If one is null or values differ, asymmetry found.
            if (t1 == null || t2 == null || t1.val != t2.val) {
                return false;
            }
            
            // Enqueue children in mirrored pairs for future validation!
            // 1. The outer edges
            queue.offer(t1.left);
            queue.offer(t2.right);
            
            // 2. The inner edges
            queue.offer(t1.right);
            queue.offer(t2.left);
        }
        
        return true;
    }

    // ============================================================================
    // TESTING & CROSS-CHECKING
    // ============================================================================
    public static void main(String[] args) {
        // Test Case 1: Standard Symmetric Tree
        //           1
        //         /   \
        //       2       2
        //      / \     / \
        //     3   4   4   3
        TreeNode root1 = new TreeNode(1);
        root1.left = new TreeNode(2);
        root1.right = new TreeNode(2);
        root1.left.left = new TreeNode(3);
        root1.left.right = new TreeNode(4);
        root1.right.left = new TreeNode(4);
        root1.right.right = new TreeNode(3);
        
        System.out.println("Test 1 (Symmetric Recursive): " + isSymmetricRecursive(root1)); // Expected: true
        System.out.println("Test 1 (Symmetric Iterative): " + isSymmetricIterative(root1)); // Expected: true

        // Test Case 2: Structurally Asymmetric
        //           1
        //         /   \
        //       2       2
        //        \       \
        //         3       3
        // Notice both 3s are RIGHT children. A reflection requires one to be left.
        TreeNode root2 = new TreeNode(1);
        root2.left = new TreeNode(2);
        root2.right = new TreeNode(2);
        root2.left.right = new TreeNode(3);
        root2.right.right = new TreeNode(3);

        System.out.println("\nTest 2 (Structurally Asymmetric Recursive): " + isSymmetricRecursive(root2)); // Expected: false
        System.out.println("Test 2 (Structurally Asymmetric Iterative): " + isSymmetricIterative(root2)); // Expected: false

        // Test Case 3: Value Asymmetric
        //           1
        //         /   \
        //       2       2
        //      /         \
        //     3           9
        TreeNode root3 = new TreeNode(1);
        root3.left = new TreeNode(2);
        root3.right = new TreeNode(2);
        root3.left.left = new TreeNode(3);
        root3.right.right = new TreeNode(9);

        System.out.println("\nTest 3 (Value Asymmetric Recursive): " + isSymmetricRecursive(root3)); // Expected: false

        // Test Case 4: Single Node
        TreeNode root4 = new TreeNode(50);
        System.out.println("\nTest 4 (Single Node): " + isSymmetricRecursive(root4)); // Expected: true
    }
}

/**
 * ============================================================================
 * 7. SUMMARY
 * ============================================================================
 * - Core pattern: Dual-Node Traversal.
 * - Key observation: Tree mirroring means crossing directions. When pointer A goes 
 *   left, pointer B MUST go right. 
 * - Most common trap: Checking `node.left.val == node.right.val` in a single-pointer 
 *   DFS. That only checks the immediate children, failing to verify the outer/inner 
 *   edges of the entire subtrees below them. You MUST pass two distinct nodes down.
 * - Mental trigger: "Is it a mirror?" -> "Helper function with (left, right) nodes".
 */


import java.util.*;

/**
 * Approach 1: Recursive Mirror Check
 *
 * Idea:
 * - Check if left and right subtree are mirror images
 *
 * Time Complexity: O(N)
 * Space Complexity: O(H) (recursion stack)
 */
public class SymmetricTree {

    // Tree Node
    static class TreeNode {
        int val;
        TreeNode left, right;

        TreeNode(int val) {
            this.val = val;
        }
    }

    public boolean isSymmetric(TreeNode root) {
        if (root == null) return true;

        // Compare left and right subtree
        return isMirror(root.left, root.right);
    }

    /**
     * Compare two trees for mirror symmetry
     */
    private boolean isMirror(TreeNode left, TreeNode right) {

        // Case 1: Both null → symmetric
        if (left == null && right == null) return true;

        // Case 2: One null → not symmetric
        if (left == null || right == null) return false;

        // Case 3: Values must match
        if (left.val != right.val) return false;

        // Case 4: Cross comparison (IMPORTANT)
        return isMirror(left.left, right.right)   // outer
            && isMirror(left.right, right.left); // inner
    }
}

import java.util.*;

/**
 * Approach 2: Iterative using Queue (Level-wise Mirror Check)
 *
 * Idea:
 * - Push nodes in pairs that should be mirrors
 *
 * Time Complexity: O(N)
 * Space Complexity: O(N)
 */
public class SymmetricTreeIterative {

    static class TreeNode {
        int val;
        TreeNode left, right;

        TreeNode(int val) {
            this.val = val;
        }
    }

    public boolean isSymmetric(TreeNode root) {
        if (root == null) return true;

        Queue<TreeNode> queue = new LinkedList<>();

        // Start with left and right
        queue.offer(root.left);
        queue.offer(root.right);

        while (!queue.isEmpty()) {

            TreeNode left = queue.poll();
            TreeNode right = queue.poll();

            // Case 1: Both null → continue
            if (left == null && right == null) continue;

            // Case 2: One null → fail
            if (left == null || right == null) return false;

            // Case 3: Value mismatch → fail
            if (left.val != right.val) return false;

            // Add children in mirror order
            queue.offer(left.left);   // outer
            queue.offer(right.right);

            queue.offer(left.right);  // inner
            queue.offer(right.left);
        }

        return true;
    }
}

