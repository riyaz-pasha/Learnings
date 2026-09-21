import java.util.ArrayDeque;
import java.util.Deque;

/**
 * ============================================================================
 * STUDY NOTE: INVERT (MIRROR) BINARY TREE
 * ============================================================================
 *
 * 1. CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * - "Should the tree be mutated in-place, or should I return a deep copy?" 
 *   (Crucial: In-place saves memory but destroys the original data structure, which might be read concurrently).
 * - "Does the inversion only apply to non-null children?" 
 *   (Confirms understanding that even if a left child exists and the right is null, the right now gets the left child and left becomes null).
 * - "What is the maximum depth of the tree?" 
 *   (Determines if a recursive approach could trigger a StackOverflowError in production).
 *
 *
 * 2. THE REASONING JOURNEY
 * ----------------------------------------------------------------------------
 * Core Constraint: 
 * We need to swap the left and right pointers of every single node in the tree. 
 * The constraint isn't mathematical complexity—it's memory management and state tracking 
 * while traversing a graph data structure to ensure no node is missed or swapped twice.
 *
 * Approach 1: Recursive DFS (Top-Down)
 * What I'd naturally try: I'd look at the root, swap its left and right pointers, and then tell 
 * its new left child and new right child to do the exact same thing recursively.
 * Why it works: The call stack naturally keeps track of which nodes still need their children swapped.
 * What work is being repeated: None. Every node is visited exactly once.
 * What property removes the bottleneck: The implicit call stack delegates state management to the JVM.
 * - Time Complexity: O(N) — because we visit every single node in the tree exactly once to perform an O(1) swap.
 * - Space Complexity: O(N) — because in a fully skewed tree (worst case), the call stack holds N frames 
 *   simultaneously. For a perfectly balanced tree, space would be O(log N).
 * 
 * Approach 2: Iterative BFS (Level-Order)
 * What I'd naturally try: If the tree is huge and recursion crashes the thread, I need to allocate 
 * memory on the heap. I'll read the tree layer by layer using a Queue.
 * Why it works: We take a node out of the queue, swap its children, and then add those children 
 * back to the queue (if they aren't null). This guarantees every node gets swapped.
 * What property removes the bottleneck: By storing nodes in a Queue, we decouple traversal from 
 * the thread's call stack limitation.
 * - Time Complexity: O(N) — because every node is enqueued exactly once, dequeued exactly once, and swapped.
 * - Space Complexity: O(N) — because a perfectly balanced tree has N/2 nodes at the bottom level, 
 *   all of which would sit in the queue simultaneously.
 *
 * The Interview Choice:
 * In an interview, immediately write the Recursive DFS. It is typically 5 lines of code and represents 
 * the "correct" functional approach to tree mutations. However, as you finish, proactively mention: 
 * "In production, if this tree could be a million nodes deep, I would use an Iterative BFS with a 
 * Queue to prevent StackOverflowErrors."
 *
 *
 * 3. EDGE CASES
 * ----------------------------------------------------------------------------
 * 1. Empty Tree (root == null) -> Must return null safely.
 * 2. Single Node -> Swaps nulls, returns the node itself.
 * 3. Unbalanced/Asymmetric Trees (e.g., node only has a left child) -> The left child becomes the right child, left becomes null.
 *
 *
 * 4. DRY RUN (Recursive DFS)
 * ----------------------------------------------------------------------------
 * Original Tree:
 *       4
 *     /   \
 *    2     7
 *   / \   / \
 *  1   3 6   9
 *
 * Call Stack Trace:
 * 1. invertTree(4): Temp holds 2. 4.left = 7, 4.right = 2. 
 *    - Calls invertTree(7) (which is now on the left).
 *    - Calls invertTree(2) (which is now on the right).
 * 2. invertTree(7): Temp holds 6. 7.left = 9, 7.right = 6. 
 *    - Calls invertTree(9). (Base case: leaf node swaps nulls, returns).
 *    - Calls invertTree(6). (Base case: leaf node swaps nulls, returns).
 * 3. invertTree(2): Temp holds 1. 2.left = 3, 2.right = 1.
 *    - Calls invertTree(3). (Base case).
 *    - Calls invertTree(1). (Base case).
 * Resulting Tree:
 *       4
 *     /   \
 *    7     2
 *   / \   / \
 *  9   6 3   1
 *
 * Pitfalls: 
 * - Attempting to swap without a temporary variable: `root.left = invertTree(root.right); root.right = invertTree(root.left);` 
 *   This fails because by the time the second line runs, `root.left` has already been overwritten.
 * - In Iterative BFS, skipping the push of nodes if they only have one child. You must push *all* non-null children.
 *
 * Pattern Recognition: 
 * "When an operation must be applied uniformly to every node -> Think standard DFS/BFS traversal."
 * "When structural mutation is required -> Secure pointers in temp variables before overwriting."
 *
 * Interview Script:
 * "Since we need to swap the children of every node in the tree, I'll use a top-down DFS. At each 
 * node, I'll swap the left and right pointers using a temporary variable, then recursively call the 
 * function on those children. That gives us O(N) time because we visit every node once, and O(N) 
 * space bounded by the tree's height for the call stack."
 *
 *
 * 5. FOLLOW-UPS
 * ----------------------------------------------------------------------------
 * Q1: How would you determine if two trees are mirrors of each other?
 * A1: I would traverse the left branch of Tree A while simultaneously traversing the right branch 
 *     of Tree B. If at any point the values differ or one node is null while the other isn't, they 
 *     aren't mirrors (this is LeetCode's "Symmetric Tree" problem).
 *
 * Q2: Can this be done Bottom-Up instead of Top-Down?
 * A2: Yes. You can recursively call `invertTree(root.left)` and `invertTree(root.right)` *before* 
 *     the swap. The children will be fully inverted, and then you swap them at the current root. 
 *     Both approaches are valid O(N) solutions.
 */

public class InvertBinaryTreeStudy {

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
     * APPROACH 1: RECURSIVE DFS (Top-Down)
     * The standard, elegant interview solution.
     */
    public TreeNode invertTreeRecursive(TreeNode root) {
        // Base case: If the tree is empty or we've reached past a leaf
        if (root == null) {
            return null;
        }

        // 1. Store the left child securely so we don't lose it during overwrite
        TreeNode temp = root.left;
        
        // 2. Perform the swap
        root.left = root.right;
        root.right = temp;

        // 3. Recurse down the children. 
        // Note: root.left is actually the OLD right child at this point.
        invertTreeRecursive(root.left);
        invertTreeRecursive(root.right);

        // Return the modified root to the caller
        return root;
    }

    /**
     * APPROACH 2: ITERATIVE BFS (Queue-based)
     * The production-safe choice for massive trees to avoid StackOverflowError.
     */
    public TreeNode invertTreeIterativeBFS(TreeNode root) {
        if (root == null) {
            return null;
        }

        // ArrayDeque acts as our Queue, allocating traversal memory on the heap
        Deque<TreeNode> queue = new ArrayDeque<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            TreeNode current = queue.poll();

            // Swap the children of the current node
            TreeNode temp = current.left;
            current.left = current.right;
            current.right = temp;

            // Enqueue the new children to be processed in future iterations.
            // Nulls are skipped to save queue space.
            if (current.left != null) {
                queue.offer(current.left);
            }
            if (current.right != null) {
                queue.offer(current.right);
            }
        }

        return root;
    }

    /**
     * Main method to cross-check approaches against edge cases.
     */
    public static void main(String[] args) {
        InvertBinaryTreeStudy study = new InvertBinaryTreeStudy();

        // Edge Case 1: Empty Tree
        System.out.println("Empty Tree (Recursive): " + study.invertTreeRecursive(null)); // Expected: null

        // Edge Case 2: Single Node
        TreeNode single = new TreeNode(42);
        single = study.invertTreeRecursive(single);
        System.out.println("Single Node (val): " + single.val + ", Left: " + single.left + ", Right: " + single.right); 
        // Expected: 42, null, null

        // Standard Case: 
        //       1
        //      / \
        //     2   3
        TreeNode standard = new TreeNode(1, new TreeNode(2), new TreeNode(3));
        standard = study.invertTreeRecursive(standard);
        System.out.println("Standard Root: " + standard.val);     // 1
        System.out.println("Standard Left: " + standard.left.val); // 3 (was 2)
        System.out.println("Standard Right: " + standard.right.val); // 2 (was 3)

        // Edge Case 3: Asymmetric Tree (Only left child)
        //       1
        //      /
        //     2
        TreeNode asymmetric = new TreeNode(1, new TreeNode(2), null);
        asymmetric = study.invertTreeIterativeBFS(asymmetric);
        System.out.println("Asymmetric Left: " + asymmetric.left); // Expected: null
        System.out.println("Asymmetric Right: " + asymmetric.right.val); // Expected: 2 (moved from left)
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core Pattern: Uniform Tree Traversal (DFS/BFS).
 * Key Observation: The subtree of any node mirrors if you swap its immediate children 
 *                  and recursively apply the same rule to those children.
 * Memorize: The 5-line Recursive DFS. Use a `temp` variable for the swap.
 * Most Common Trap: Recursing and assigning simultaneously (e.g. root.left = invert(root.right)) 
 *                   without storing root.left first, resulting in data loss.
 * One-Line Mental Trigger: "Invert Tree = Temp swap children, then recurse left and right."
 * ============================================================================
 */

