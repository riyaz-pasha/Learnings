import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * ============================================================================
 * STUDY NOTE: Kth SMALLEST ELEMENT IN A BST
 * ============================================================================
 *
 * 1. CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * - "Is the tree static, or will there be frequent inserts and deletes between queries?" 
 *   (Crucial: A one-time query just needs a traversal. Frequent queries on a mutating tree require an augmented data structure).
 * - "Are we guaranteed that the tree has at least k nodes?" 
 *   (The constraints say 1 <= k <= n, so yes, but asking confirms you are thinking about out-of-bounds errors).
 * - "Can we modify the tree's pointers temporarily?" 
 *   (Determines if Morris Traversal is allowed to achieve O(1) space, as it temporarily rewires null leaves).
 *
 *
 * 2. THE REASONING JOURNEY
 * ----------------------------------------------------------------------------
 * Core Constraint: 
 * A Binary Search Tree (BST) inherently maintains sorted order, but its 2D graphical structure 
 * obscures the 1D sorted index. The constraint is finding a way to count elements in strictly 
 * increasing order without blindly flattening the entire tree into memory when `k` might be very small.
 *
 * Approach 1: Full In-Order Traversal to a List (The Brute Force)
 * What I'd naturally try: I know that an In-Order traversal (Left -> Node -> Right) on a BST 
 * visits nodes in perfectly ascending order. I can just traverse the whole tree, put every value 
 * into a Java `ArrayList`, and then return `list.get(k - 1)`.
 * Why it works: It maps the 2D tree into a 1D sorted array, making index lookups trivial.
 * Why it's too slow/costly: It completely ignores the opportunity to stop early. If the tree has 
 * 10,000 nodes and we just want the 1st smallest (k=1), this approach still visits and stores the 
 * other 9,999 nodes unnecessarily.
 * - Time Complexity: O(N) — because every single node is visited unconditionally.
 * - Space Complexity: O(N) — because an auxiliary array of size N is created to store all values, 
 *   plus the O(H) recursion stack.
 * 
 * Approach 2: Recursive In-Order with Early Exit (The Intuitive Step-Up)
 * What I'd naturally try: Instead of a list, I just need a counter. I'll do the same recursive 
 * In-Order traversal, but increment a `count` integer each time I "visit" a node. When `count == k`, 
 * I save the node's value and stop recursing.
 * Why it works: It processes elements in the same ascending order but halts the moment we find our answer.
 * What property removes the bottleneck: We only visit nodes up to the kth smallest. 
 * - Time Complexity: O(H + k) — because we first dive down to the leftmost leaf (taking O(H) steps), 
 *   and then we process exactly `k` nodes.
 * - Space Complexity: O(H) — because the maximum depth of the recursive call stack is the height 
 *   of the tree (O(log N) for balanced, O(N) for skewed).
 *
 * Approach 3: Iterative In-Order with Stack (The Production Optimal)
 * What I'd naturally try: Approach 2 requires global/instance variables (or passing mutable arrays/objects) 
 * to track the `count` and `result` across recursive frames. In a concurrent environment, this is dangerous. 
 * I'll simulate the recursion locally using an explicit Stack.
 * Why it works: I push all left children onto a stack until I hit null. Then I pop, increment my 
 * counter, check if it equals `k`, and if not, move to the right child and repeat.
 * What property removes the bottleneck: It retains the O(H + k) early-exit performance of Approach 2 
 * but encapsulates all state locally within the method, avoiding shared mutable state.
 * - Time Complexity: O(H + k) — because we push O(H) nodes to reach the minimum, then process `k` nodes.
 * - Space Complexity: O(H) — because the explicit Stack holds at most one complete root-to-leaf path 
 *   at any given time.
 *
 * The Interview Choice:
 * Write Approach 3 (Iterative In-Order). It proves you know how to decouple traversal from the JVM's 
 * call stack and gives you exact, granular control over when to break out of the loop without messy 
 * global variables.
 *
 *
 * 3. EDGE CASES
 * ----------------------------------------------------------------------------
 * 1. k = 1 (The minimum element) -> Tests if the code can immediately return after the first left-dive.
 * 2. k = n (The maximum element) -> Tests if the traversal successfully reaches the absolute end.
 * 3. Skewed Tree (Left-leaning linked list) -> Tests the O(N) worst-case space bound for the stack.
 *
 *
 * 4. DRY RUN (Iterative In-Order Stack)
 * ----------------------------------------------------------------------------
 * Tree:
 *        5
 *       / \
 *      3   6
 *     / \
 *    2   4
 * 
 * Target: k = 3
 *
 * State Tracker (stack = right side is TOP, count = 0):
 * Initial: current = 5.
 * Step 1: Dive left. Push 5, Push 3, Push 2. current = null. 
 *         -> stack = [5, 3, 2]
 * Step 2: current is null, so POP. popped = 2.
 *         count becomes 1. Is 1 == 3? No. 
 *         current = popped.right (null).
 *         -> stack = [5, 3]
 * Step 3: current is null, so POP. popped = 3.
 *         count becomes 2. Is 2 == 3? No.
 *         current = popped.right (4).
 *         -> stack = [5]
 * Step 4: current is 4. Dive left. Push 4. current = 4.left (null).
 *         -> stack = [5, 4]
 * Step 5: current is null, so POP. popped = 4.
 *         count becomes 3. Is 3 == 3? YES!
 *         Return popped.val (4).
 * Final Result: 4.
 *
 * Pitfalls: 
 * - Checking `count == k` BEFORE the left dive. In Pre-Order (Node, Left, Right), the first node 
 *   you see is the root, which is NOT the smallest. You must evaluate the count strictly when 
 *   POPPING from the stack (which represents the 'Node' step in Left-Node-Right).
 *
 * Pattern Recognition: 
 * "When a problem asks for the Kth [smallest/largest] in a BST -> Think In-Order (or Reverse In-Order) 
 * traversal with an early-exit counter."
 *
 * Interview Script:
 * "Since an In-Order traversal of a BST yields values in ascending order, we don't need to sort anything. 
 * To avoid the O(N) space of building a full list, and the messy global variables of recursion, I'll 
 * use an iterative approach with an explicit Stack. I'll traverse left as far as possible, pop nodes 
 * off, increment a local counter, and return exactly when the counter hits K. This takes O(H) space 
 * for the stack and O(H + K) time, terminating as early as possible."
 *
 *
 * 5. FOLLOW-UPS
 * ----------------------------------------------------------------------------
 * Q1: What if the BST is modified (insert/delete) often and you need to find the kth smallest frequently?
 * A1: O(H + k) per query is too slow for frequent queries. I would augment the BST node structure 
 *     to include a `leftSubtreeSize` integer. When querying, if `k == leftSubtreeSize + 1`, the 
 *     current node is the answer. If `k <= leftSubtreeSize`, we go left. If `k > leftSubtreeSize + 1`, 
 *     we go right and search for `k - (leftSubtreeSize + 1)`. This reduces query time to strictly O(H).
 *
 * Q2: Can we do this in O(1) space?
 * A2: Yes, using a Morris In-Order Traversal. It temporarily modifies the tree by making the 
 *     rightmost node of the left subtree point back to the current node. This eliminates the stack, 
 *     but it mutates the tree during traversal, which isn't thread-safe.
 */

public class KthSmallestBSTStudy {

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
     * APPROACH 3: ITERATIVE IN-ORDER (The professional, thread-safe choice)
     * Time: O(H + k) | Space: O(H)
     */
    public int kthSmallestIterative(TreeNode root, int k) {
        // Deque is preferred over java.util.Stack for performance and explicit thread-unsafety.
        // E.g., for tree [5,3,6,2,4], diving left will initially fill stack with [5, 3, 2]
        Deque<TreeNode> stack = new ArrayDeque<>();
        TreeNode current = root;
        int count = 0;

        while (current != null || !stack.isEmpty()) {
            // Step 1: Dive as deep left as possible.
            // These are the strictly smaller elements.
            while (current != null) {
                stack.push(current);
                current = current.left;
            }

            // Step 2: Pop the smallest available element.
            // By the nature of BST and our left-dive, this is guaranteed to be the 
            // next smallest element in the entire tree.
            current = stack.pop();
            count++;

            // Step 3: Check if this is the target.
            if (count == k) {
                return current.data;
            }

            // Step 4: Move right.
            // The next slightly larger elements exist in the right subtree of the node we just popped.
            current = current.right;
        }

        // Constraints guarantee 1 <= k <= n, so we will always return inside the loop.
        return -1; 
    }

    /**
     * APPROACH 2: RECURSIVE IN-ORDER (For completeness)
     * Time: O(H + k) | Space: O(H)
     * Demonstrates the annoyance of needing instance variables for state tracking.
     */
    private int recursiveCount = 0;
    private int recursiveResult = -1;

    public int kthSmallestRecursive(TreeNode root, int k) {
        recursiveCount = 0;   // Reset for repeated calls
        recursiveResult = -1; 
        inOrderDFS(root, k);
        return recursiveResult;
    }

    private void inOrderDFS(TreeNode node, int k) {
        // Base case or early exit if we already found the answer in another branch
        if (node == null || recursiveCount >= k) {
            return;
        }

        // Left
        inOrderDFS(node.left, k);
        
        // Node
        recursiveCount++;
        if (recursiveCount == k) {
            recursiveResult = node.data;
            return; // We found it, start unwinding
        }
        
        // Right
        inOrderDFS(node.right, k);
    }

    /**
     * APPROACH 1: BRUTE FORCE (List collection)
     * Time: O(N) | Space: O(N)
     * Shows exactly what we are avoiding.
     */
    public int kthSmallestBruteForce(TreeNode root, int k) {
        List<Integer> sortedValues = new ArrayList<>();
        buildList(root, sortedValues);
        return sortedValues.get(k - 1);
    }

    private void buildList(TreeNode node, List<Integer> list) {
        if (node == null) return;
        buildList(node.left, list);
        list.add(node.data);
        buildList(node.right, list);
    }

    /**
     * Main method to cross-check approaches against edge cases.
     */
    public static void main(String[] args) {
        KthSmallestBSTStudy study = new KthSmallestBSTStudy();

        // Standard Case: 
        //        5
        //       / \
        //      3   6
        //     / \
        //    2   4
        TreeNode standard = new TreeNode(5,
                new TreeNode(3, new TreeNode(2), new TreeNode(4)),
                new TreeNode(6)
        );
        
        System.out.println("Standard Iterative (k=3): " + study.kthSmallestIterative(standard, 3)); // Expected: 4
        System.out.println("Standard Recursive (k=3): " + study.kthSmallestRecursive(standard, 3)); // Expected: 4

        // Edge Case 1: Minimum element (k=1)
        // Tests if it exits immediately after finding the leftmost leaf
        System.out.println("Minimum Element (k=1): " + study.kthSmallestIterative(standard, 1)); // Expected: 2

        // Edge Case 2: Maximum element (k=5)
        // Tests full traversal logic
        System.out.println("Maximum Element (k=5): " + study.kthSmallestIterative(standard, 5)); // Expected: 6

        // Edge Case 3: Fully Skewed Right Tree
        //  1
        //   \
        //    2
        //     \
        //      3
        TreeNode skewed = new TreeNode(1, null, new TreeNode(2, null, new TreeNode(3)));
        System.out.println("Skewed Tree (k=3): " + study.kthSmallestIterative(skewed, 3)); // Expected: 3
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core Pattern: In-Order Traversal with an Early Exit.
 * Key Observation: A BST intrinsically sorts its elements if read Left-Node-Right. We can stop 
 *                  traversing the instant we've 'visited' (popped) K elements.
 * Memorize: The iterative stack pattern. `while(curr != null || !stack.isEmpty()) { dive left; pop; check; go right; }`
 * Most Common Trap: Trying to increment the counter before pushing to the stack (Pre-Order), 
 *                   which counts structural depth rather than sorted value order.
 * One-Line Mental Trigger: "Kth BST = Iterative In-Order, pop and count."
 * ============================================================================
 */

import ds_v1.BinaryTree.TreeNode;

public class KthSmallestBST {

    // Wrapper result class (cleaner than global variables)
    static class Result {
        int count = 0;
        int answer = -1;
    }

    public int kthSmallest(TreeNode<Integer> root, int k) {
        Result res = new Result();
        inorder(root, k, res);
        return res.answer;
    }

    private void inorder(TreeNode<Integer> node, int k, Result res) {
        if (node == null) return;

        // 1. Left
        inorder(node.left, k, res);

        // 2. Node
        res.count++;

        if (res.count == k) {
            res.answer = node.data;
            return; // stop early
        }

        // 3. Right (only if answer not found)
        if (res.answer == -1) {
            inorder(node.right, k, res);
        }
    }
}

