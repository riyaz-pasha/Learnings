import java.util.ArrayDeque;
import java.util.Deque;

/**
 * ============================================================================
 * STUDY NOTE: UNI-VALUED BINARY TREE
 * ============================================================================
 *
 * 1. CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * - "The constraints say the tree has at least 1 node, but should I handle a null root just in case?" 
 *   (Demonstrates defensive programming. A null tree is technically 'vacuously' univalued).
 * - "Are we optimizing for the worst-case (every node matches) or best-case (mismatch found early)?" 
 *   (Guides the implementation toward short-circuiting logic rather than full traversal).
 * - "Can the tree values be negative?" 
 *   (The constraints say 0 <= Node.data < 100, but clarifying ensures we don't use -1 as a magic 'uninitialized' value if negative values were allowed).
 *
 *
 * 2. THE REASONING JOURNEY
 * ----------------------------------------------------------------------------
 * Core Constraint: 
 * Every node must share the exact same value as the root. The bottleneck isn't the comparison 
 * itself, but avoiding unnecessary traversals once a single mismatch is found. We must design 
 * an approach that "bails out" immediately upon seeing a wrong value.
 *
 * Approach 1: Full Traversal to a Collection (The Naive Brute Force)
 * What I'd naturally try: I know how to traverse a tree. I'll just write a standard DFS, put every 
 * single node's value into a Java `HashSet`, and at the end, check if `set.size() == 1`.
 * Why it works: A Set removes duplicates. If all values are the same, only one element remains.
 * Why it's too slow/costly: It completely ignores the opportunity to stop early. If the root is 2 
 * and its left child is 99, this approach still visits the remaining 98 nodes before returning false.
 * - Time Complexity: O(N) — because it visits every single node unconditionally.
 * - Space Complexity: O(N) — because it duplicates the tree's values into an auxiliary HashSet, 
 *   plus the O(H) recursion stack.
 * 
 * Approach 2: Iterative BFS with Early Exit (The Heap-Safe Approach)
 * What I'd naturally try: Instead of dumping to a set, let's check nodes as we see them. I can 
 * use a Queue to do a Level-Order traversal. I'll read the root's value, and then loop through 
 * the queue. If any popped node != root's value, return `false` instantly.
 * Why it works: We validate as we go.
 * What property removes the bottleneck: The early `return false;` statement prevents us from enqueuing 
 * or visiting any more nodes the millisecond we find a mismatch.
 * - Time Complexity: O(N) in the worst case (all match), but O(1) best case (mismatch at the root's child).
 * - Space Complexity: O(N) — because the Queue stores the leaf level, which holds N/2 nodes in a 
 *   perfectly balanced tree.
 *
 * Approach 3: Recursive DFS with Short-Circuiting (The Optimal/Elegant Approach)
 * What I'd naturally try: Let's translate the iterative early-exit to recursion. A tree is univalued 
 * if: the current node matches the target, AND its left subtree is univalued, AND its right subtree is univalued.
 * Why it works: Java's logical AND (`&&`) operator short-circuits. If `node.val == target` evaluates 
 * to false, it immediately returns false and *never even calls* the left or right subtrees.
 * What property removes the bottleneck: We achieve early exit using native language features, completely 
 * eliminating the auxiliary Queue/Set structures.
 * - Time Complexity: O(N) worst case (all match), O(1) best case (mismatch found instantly).
 * - Space Complexity: O(N) worst case (call stack on a skewed tree), O(log N) average balanced case. 
 *   We store NO extra data structures, only execution frames.
 *
 * The Interview Choice:
 * Always write Approach 3 (Recursive DFS with Short-Circuiting). It is functionally pure, takes exactly 
 * 4 lines of code, and demonstrates an understanding of how logical operators govern execution flow.
 *
 *
 * 3. EDGE CASES
 * ----------------------------------------------------------------------------
 * 1. Single Node Tree -> Must return true (a tree of 1 is always uniform).
 * 2. Null Root -> Should safely return true (defensive programming).
 * 3. Mismatch on the extreme right leaf -> Tests worst-case traversal (forces full evaluation).
 * 4. Mismatch on the immediate left child -> Tests early exit / short-circuiting.
 *
 *
 * 4. DRY RUN (Recursive DFS)
 * ----------------------------------------------------------------------------
 * Tree with early mismatch:
 *       2
 *      / \
 *     5   2
 *
 * Call Stack Trace (target = 2):
 * 1. isUnivalTree(node=2): node(2) == 2 is TRUE.
 *    - Java evaluates the left side of the `&&`: calls isUnivalTree(node.left)
 * 2. isUnivalTree(node=5): node(5) == 2 is FALSE.
 *    - Returns FALSE immediately.
 * 3. Back to root: `true && FALSE && isUnivalTree(node.right)`
 *    - Because the middle term is FALSE, the `&&` short-circuits.
 *    - `isUnivalTree(node.right)` (the right child containing 2) is NEVER called.
 * Result: false.
 *
 * Pitfalls: 
 * - Writing:
 *   `boolean left = isUnivalTree(node.left);`
 *   `boolean right = isUnivalTree(node.right);`
 *   `return left && right && node.val == target;`
 *   This DESTROYS the early exit. By eagerly evaluating `left` and `right` into variables before 
 *   the `&&` operator, you force the program to traverse the entire tree every time.
 *
 * Pattern Recognition: 
 * "When verifying a uniform property across an entire tree -> Think DFS with short-circuiting (&&)."
 *
 * Interview Script:
 * "The naive way is to collect all values into a Set, but that loses the ability to stop early. 
 * Instead, I'll pass the root's value down a recursive DFS. By chaining the node comparison and the 
 * recursive calls with a logical AND (`&&`), Java will short-circuit and stop traversing the exact 
 * moment we hit a mismatch. This gives us O(N) time worst-case, O(1) best-case, and O(H) space for the call stack."
 *
 *
 * 5. FOLLOW-UPS
 * ----------------------------------------------------------------------------
 * Q1: What if instead of checking the whole tree, I asked you to count HOW MANY subtrees within this tree are uni-valued?
 * A1: This is a classic twist (LeetCode 250). Top-down DFS fails here because we'd repeat work. 
 *     We must switch to Bottom-Up DFS (Post-order). We ask the children "are you univalued?", 
 *     and if both are, and they match the current node, we increment a global counter and return true upward.
 *
 * Q2: What if this tree is continuously updating in real-time, and we need to frequently query `isUnivalued()`?
 * A2: Scanning O(N) every time is too slow. We could augment the `TreeNode` structure to store a 
 *     `minVal` and `maxVal` for its subtree. If `root.minVal == root.maxVal`, the tree is uni-valued in O(1) time. 
 *     We'd update these values recursively on write/insert.
 */

public class UnivaluedBinaryTreeStudy {

    // Definition for a binary tree node.
    public static class TreeNode {
        int data;
        TreeNode left;
        TreeNode right;
        TreeNode() {}
        TreeNode(int data) { this.data = data; }
        TreeNode(int data, TreeNode left, TreeNode right) {
            this.data = data;
            this.left = left;
            this.right = right;
        }
    }

    /**
     * APPROACH 3: RECURSIVE DFS (The elegant, optimal choice)
     * Time: O(N) worst case | Space: O(N) worst case, O(log N) best case
     */
    public boolean isUnivalTreeRecursive(TreeNode root) {
        // Defensive check: empty trees don't violate the rule
        if (root == null) return true;
        
        // Kick off the recursion passing the root's value as the source of truth
        return dfs(root, root.data);
    }

    private boolean dfs(TreeNode node, int targetValue) {
        // Base case: we reached a null leaf-pointer without failing. This path is good.
        if (node == null) {
            return true;
        }

        // The core logic relies on the ORDER of evaluation here.
        // 1. Check current node first. If it fails, return false instantly.
        // 2. If it passes, evaluate the left subtree.
        // 3. ONLY if the left subtree passes, evaluate the right subtree.
        return node.data == targetValue 
            && dfs(node.left, targetValue) 
            && dfs(node.right, targetValue);
    }

    /**
     * APPROACH 2: ITERATIVE BFS (The robust choice for extremely deep trees)
     * Time: O(N) worst case | Space: O(N)
     */
    public boolean isUnivalTreeBFS(TreeNode root) {
        if (root == null) return true;

        // Establish our target value to compare everything against
        int targetValue = root.data;

        // ArrayDeque is faster than LinkedList and serves as our Queue
        Deque<TreeNode> queue = new ArrayDeque<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            TreeNode current = queue.poll();

            // Early exit: The moment a mismatch is found, halt everything and return false
            if (current.data != targetValue) {
                return false;
            }

            // Enqueue children for future inspection
            if (current.left != null) {
                queue.offer(current.left);
            }
            if (current.right != null) {
                queue.offer(current.right);
            }
        }

        // If the queue runs dry, every node passed the test
        return true;
    }

    /**
     * Main method to cross-check approaches against edge cases.
     */
    public static void main(String[] args) {
        UnivaluedBinaryTreeStudy study = new UnivaluedBinaryTreeStudy();

        // Edge Case 1: Empty Tree (Constraint says >= 1, but handled defensively)
        System.out.println("Empty Tree (DFS): " + study.isUnivalTreeRecursive(null)); // Expected: true

        // Edge Case 2: Single Node
        TreeNode single = new TreeNode(42);
        System.out.println("Single Node (DFS): " + study.isUnivalTreeRecursive(single)); // Expected: true

        // Standard Case 1: Valid Univalued Tree
        //       1
        //      / \
        //     1   1
        //    / \   \
        //   1   1   1
        TreeNode validTree = new TreeNode(1,
                new TreeNode(1, new TreeNode(1), new TreeNode(1)),
                new TreeNode(1, null, new TreeNode(1))
        );
        System.out.println("Valid Tree (DFS): " + study.isUnivalTreeRecursive(validTree)); // Expected: true
        System.out.println("Valid Tree (BFS): " + study.isUnivalTreeBFS(validTree));       // Expected: true

        // Standard Case 2: Mismatch deeply nested on the right
        //       1
        //      / \
        //     1   1
        //          \
        //           99
        TreeNode deepMismatch = new TreeNode(1,
                new TreeNode(1),
                new TreeNode(1, null, new TreeNode(99))
        );
        System.out.println("Deep Mismatch (DFS): " + study.isUnivalTreeRecursive(deepMismatch)); // Expected: false

        // Edge Case 3: Mismatch at the root's left child (Tests early exit)
        //       2
        //      / \
        //     5   2
        TreeNode earlyMismatch = new TreeNode(2, new TreeNode(5), new TreeNode(2));
        System.out.println("Early Mismatch (DFS): " + study.isUnivalTreeRecursive(earlyMismatch)); // Expected: false
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core Pattern: DFS / Traversal with Short-Circuit Evaluation.
 * Key Observation: You only need to know the root's value. Everything else must match it. 
 *                  The first violation invalidates the whole tree.
 * Memorize: `return node.val == target && dfs(left) && dfs(right);`
 * Most Common Trap: Computing the recursive calls into local variables before the `&&`, 
 *                   which completely destroys the early-exit optimization.
 * One-Line Mental Trigger: "Univalued? Target is Root, AND-gate everything."
 * ============================================================================
 */
