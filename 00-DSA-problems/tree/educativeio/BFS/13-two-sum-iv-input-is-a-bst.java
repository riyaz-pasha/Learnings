/**
 * ============================================================================
 * 0. PROBLEM STATEMENT
 * ============================================================================
 * Given the root of a binary search tree and an integer k, determine whether 
 * there are two elements in the BST whose sum equals k. Return TRUE if such 
 * elements exist or FALSE otherwise.
 * 
 * Constraints:
 * - The number of nodes in the tree is in the range [1, 10^3].
 * - -10^3 <= Node.data <= 10^3
 * - root is guaranteed to be a valid binary search tree.
 * - -10^4 <= k <= 10^4
 * 
 * ============================================================================
 * 1. CLARIFYING QUESTIONS
 * ============================================================================
 * - "Can we use the exact same node twice to form the sum `k`?"
 *   No, the problem asks for two distinct elements. This prevents blindly checking if `k/2` exists.
 * - "Does the BST contain duplicate values?"
 *   Typically no, standard BSTs hold distinct values. If yes, it affects how we determine if pointers have met. (Assume distinct).
 * - "Are we strictly optimizing for space, or is an O(N) space solution acceptable?"
 *   Guides whether we should just use a HashSet or if we need to demonstrate mastery via BST Iterators.
 * - "Is it possible for the tree to have only one node?"
 *   Yes, constraints say [1, 10^3]. A single node cannot form a pair, so we should return FALSE immediately.
 * 
 * ============================================================================
 * 2. THE REASONING JOURNEY
 * ============================================================================
 * [Binding Constraint] 
 * We need to find two distinct elements in a data structure that sum to `k`. 
 * The data structure is inherently sorted (it's a BST), but not sequentially accessible like an array.
 * 
 * --- APPROACH 1: DFS / BFS + HashSet (The "Generic" Try) ---
 * 1. What I'd naturally try: Traverse the tree using DFS or BFS. For each node, check if 
 *    `k - node.data` exists in a HashSet. If not, add `node.data` to the set and continue.
 * 2. Why it works: It perfectly solves the Two Sum problem by trading space for lookup speed.
 * 3. Why it's suboptimal: It completely ignores the fact that the input is a Binary Search Tree! 
 *    This approach works on ANY random binary tree. 
 * 4. Time Complexity: O(N) — because we visit each node once.
 * 5. Space Complexity: O(N) — because the HashSet stores all N elements in the worst case.
 * 
 * --- APPROACH 2: Inorder Traversal Array + Two Pointers (The Intermediate Step) ---
 * 1. What I'd try next: Exploit the BST property. An inorder traversal of a BST yields a 
 *    strictly sorted array. I can flatten the tree into a List, then use a `left` pointer 
 *    at index 0 and a `right` pointer at index N-1, shifting them inward based on the sum.
 * 2. Why it works: It leverages the sorted property of the BST to find the pair deterministically.
 * 3. The bottleneck: We still have to allocate an entire array/list to hold the tree's values.
 * 4. Time Complexity: O(N) — to flatten the tree and traverse the array.
 * 5. Space Complexity: O(N) — to explicitly store the flattened sorted array.
 * 
 * [The Core Observation for Optimal]
 * Approach 2 is great, but we don't actually need the *entire* array in memory at once! 
 * At any given moment, the Two-Pointer technique only looks at the "next smallest" and 
 * "next largest" elements. If we can generate these elements *on demand*, we don't need the array.
 * 
 * --- APPROACH 3: Two Pointers + BST Iterators (The Optimal Way) ---
 * 1. How it works: We create a `BSTIterator` class. 
 *    - A "forward" iterator uses standard inorder traversal (Left-Root-Right) to yield smallest elements.
 *    - A "backward" iterator uses reverse inorder traversal (Right-Root-Left) to yield largest elements.
 *    We initialize `left = forward.next()` and `right = backward.next()` and run the standard two-pointer loop!
 * 2. Time Complexity: O(N) — because each node is pushed and popped from the iterator stacks 
 *    at most twice. The amortized time per `next()` call is O(1).
 * 3. Space Complexity: O(H) — where H is the height of the tree. The iterator stacks only store 
 *    the path from the root to the current leaf. For a balanced tree, this is O(log N).
 * 
 * [Which one I'd write in an interview]
 * Approach 3 (BST Iterators). It is the gold standard for this problem. It shows you understand 
 * the Two-Pointer technique, the BST property, AND advanced Object-Oriented design patterns (Iterators).
 * 
 * ============================================================================
 * 3. EDGE CASES
 * ============================================================================
 * - Single Node Tree: Cannot form a pair. The loop `left < right` handles this gracefully or we fast-fail.
 * - Target `k` is exactly 2 * root.data: Prevents accidentally using the root twice if implemented poorly.
 * - Skewed Tree (Linked List shape): Space complexity degrades from O(log N) to O(N), but this is 
 *   still mathematically equal to or better than Approaches 1 and 2 in all scenarios.
 * 
 * ============================================================================
 * 4. KEY INSIGHT, DIAGRAMS & DRY RUN
 * ============================================================================
 * [Key Insight]
 * A BST is just a sorted array waiting to be unwrapped. A Stack can pause and resume a 
 * recursive traversal, allowing you to fetch array elements "lazy-loaded" in O(1) amortized time.
 * 
 * [Examples & Diagram]
 * Tree:
 *          5
 *        /   \
 *       3     6
 *      / \     \
 *     2   4     7
 * Target K = 9
 * 
 * [Dry Run (Optimal BST Iterators)]
 * - Init Forward Iterator: Dives left. Stack = [5, 3, 2]. 
 * - Init Backward Iterator: Dives right. Stack = [5, 6, 7].
 * - Start: leftVal = 2, rightVal = 7
 * 
 * Iteration 1:
 * - Sum = 2 + 7 = 9.
 * - 9 == K. We found the target! Return TRUE.
 * 
 * What if K = 11?
 * - Iteration 1: left=2, right=7. Sum=9. 9 < 11. Move left pointer.
 * - Forward.next(): pops 2. Next smallest is 3. Stack = [5, 3].
 * - Iteration 2: left=3, right=7. Sum=10. 10 < 11. Move left pointer.
 * - Forward.next(): pops 3, pushes right child 4. Next smallest is 4. Stack = [5, 4].
 * - Iteration 3: left=4, right=7. Sum=11. 11 == K! Return TRUE.
 * 
 * [Pitfalls]
 * - Initializing the two pointers by searching the tree for `k - node.data` and finding 
 *   the *same* node (e.g., node is 4, k is 8. Search finds 4, and you return true!). 
 *   The iterator pattern natively prevents this collision because `leftVal < rightVal`.
 * 
 * [Pattern Recognition]
 * When you see: "Two Sum" + "Sorted Data / BST"
 * Think: Two Pointers. If the data is in a tree, use Stacks to create lazy-loaded Iterators.
 * 
 * [Interview Script]
 * "Since the input is a Binary Search Tree, we should take advantage of its sorted nature. 
 * A brute force approach using a HashSet works for any generic tree but takes O(N) space. 
 * If we flatten the BST using an inorder traversal, we get a sorted array, and we can apply 
 * the classic two-pointer technique in O(N) time, but it still takes O(N) space. To optimize 
 * space to O(H), I'll use two iterators—one doing a normal inorder traversal to yield the 
 * smallest elements, and one doing a reverse inorder traversal to yield the largest elements. 
 * We can then run the two-pointer technique on the fly. This gives us O(N) time and optimal O(H) space."
 * 
 * ============================================================================
 * 5. FOLLOW-UPS
 * ============================================================================
 * Q: What if we cannot use ANY extra space (strictly O(1) space)?
 * A: We could iterate through each node using Morris Traversal (O(1) space). For each node, 
 *    we search the BST for `k - node.data` (which takes O(H) time per search). However, this 
 *    degrades the time complexity to O(N log N) for balanced trees, or O(N^2) for skewed trees.
 * 
 * Q: What if this tree is heavily updated (inserts/deletes) and we need to run `findTarget` often?
 * A: The O(N) traversal might become a bottleneck. We could maintain a parallel `HashSet` inside 
 *    the tree class that updates on insert/delete, trading memory overhead for O(1) query time.
 * 
 * ============================================================================
 * 6. JAVA CODE
 * ============================================================================
 */

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class TwoSumBST {

    // Standard Binary Tree Node definition matching problem constraints
    public static class TreeNode {
        int data;
        TreeNode left;
        TreeNode right;
        TreeNode(int data) { this.data = data; }
    }

    /**
     * APPROACH 3: Optimal Two Pointers + BST Iterators
     * Time: O(N), Space: O(H)
     */
    public static boolean findTarget(TreeNode root, int k) {
        // Fast-fail if the tree is empty or has only one node (cannot form a pair)
        if (root == null || (root.left == null && root.right == null)) {
            return false;
        }

        // Initialize the two iterators
        BSTIterator leftIter = new BSTIterator(root, true);  // Yields smallest to largest
        BSTIterator rightIter = new BSTIterator(root, false); // Yields largest to smallest

        // Fetch the initial boundaries
        int leftVal = leftIter.next();
        int rightVal = rightIter.next();

        // Standard Two-Pointer approach
        while (leftVal < rightVal) {
            int currentSum = leftVal + rightVal;

            if (currentSum == k) {
                return true;
            } else if (currentSum < k) {
                // Sum is too small, move the left pointer forward
                leftVal = leftIter.next();
            } else {
                // Sum is too large, move the right pointer backward
                rightVal = rightIter.next();
            }
        }

        return false;
    }

    /**
     * A custom Iterator that can traverse a BST in either forward (ascending) 
     * or reverse (descending) order, lazy-loading values on demand using O(H) space.
     */
    private static class BSTIterator {
        private Stack<TreeNode> stack = new Stack<>();
        private boolean isForward;

        public BSTIterator(TreeNode root, boolean isForward) {
            this.isForward = isForward;
            pushAll(root);
        }

        /**
         * Returns the next value in the traversal.
         */
        public int next() {
            TreeNode node = stack.pop();
            
            // If we popped a node, we must now explore its opposite subtree
            if (isForward) {
                pushAll(node.right);
            } else {
                pushAll(node.left);
            }
            
            return node.data;
        }

        /**
         * Pushes all nodes along the directional path to the stack.
         */
        private void pushAll(TreeNode node) {
            while (node != null) {
                stack.push(node);
                // If forward, aggressively push left children to reach the minimum.
                // If reverse, aggressively push right children to reach the maximum.
                node = isForward ? node.left : node.right;
            }
        }
    }

    /**
     * APPROACH 1: HashSet (Included for conceptual contrast)
     * Time: O(N), Space: O(N)
     */
    public static boolean findTargetHashSet(TreeNode root, int k) {
        Set<Integer> set = new HashSet<>();
        return dfs(root, k, set);
    }

    private static boolean dfs(TreeNode node, int k, Set<Integer> set) {
        if (node == null) return false;
        
        // Check if the complement exists in our observed set
        if (set.contains(k - node.data)) {
            return true;
        }
        
        // Record the current node
        set.add(node.data);
        
        // Traverse rest of the tree
        return dfs(node.left, k, set) || dfs(node.right, k, set);
    }

    // ============================================================================
    // TESTING & CROSS-CHECKING
    // ============================================================================
    public static void main(String[] args) {
        // Build Test Tree:
        //          5
        //        /   \
        //       3     6
        //      / \     \
        //     2   4     7
        TreeNode root = new TreeNode(5);
        root.left = new TreeNode(3);
        root.right = new TreeNode(6);
        root.left.left = new TreeNode(2);
        root.left.right = new TreeNode(4);
        root.right.right = new TreeNode(7);

        // Test 1: Target exists (2 + 7 = 9)
        System.out.println("Test 1 (Target 9): " + findTarget(root, 9)); 
        // Expected: true

        // Test 2: Target exists (5 + 6 = 11)
        System.out.println("Test 2 (Target 11): " + findTarget(root, 11)); 
        // Expected: true

        // Test 3: Target does not exist
        System.out.println("Test 3 (Target 28): " + findTarget(root, 28)); 
        // Expected: false

        // Test 4: Target is double a node's value (e.g., 4*2=8) but only one 4 exists
        System.out.println("Test 4 (Target 8, prevents double-use): " + findTarget(root, 8)); 
        // Expected: false (Only combinations are 5+3, 6+2)

        // Test 5: Single Node Tree
        System.out.println("Test 5 (Single Node): " + findTarget(new TreeNode(5), 5)); 
        // Expected: false
    }
}

/**
 * ============================================================================
 * 7. SUMMARY
 * ============================================================================
 * - Core pattern: Two Pointers + Custom Stacks (Iterators).
 * - Key observation: You don't need to flatten the entire BST into an array to 
 *   run Two-Pointers. You can lazy-load the array's ends using O(H) stack space.
 * - Most common trap: Using a HashSet. While it gets the job done and is accepted, 
 *   it completely ignores the sorted nature of the BST and fails space optimization 
 *   questions in serious interviews.
 * - Mental trigger: "Two Sum in a BST" -> "Forward and Reverse Iterators".
 */


import java.util.HashSet;
import java.util.Set;

/**
 * Approach: DFS + HashSet (Two Sum style)
 *
 * Intuition:
 * - Traverse tree
 * - For each node:
 *      check if (k - node.val) exists
 *
 * Why it works:
 * - Same logic as Two Sum in array
 *
 * Time Complexity: O(N)
 * Space Complexity: O(N)
 */
public class TwoSumBST_HashSet {

    static class TreeNode {
        int val;
        TreeNode left, right;

        TreeNode(int v) { this.val = v; }
    }

    public boolean findTarget(TreeNode root, int k) {
        Set<Integer> seen = new HashSet<>();
        return dfs(root, k, seen);
    }

    private boolean dfs(TreeNode node, int k, Set<Integer> seen) {
        if (node == null) return false;

        // Check if complement exists
        if (seen.contains(k - node.val)) {
            return true;
        }

        // Add current value
        seen.add(node.val);

        // Traverse left or right
        return dfs(node.left, k, seen) || dfs(node.right, k, seen);
    }
}

import java.util.*;

/**
 * Approach: Inorder + Two Pointer
 *
 * Steps:
 * 1. Convert BST → sorted array
 * 2. Use two pointer
 *
 * Time: O(N)
 * Space: O(N)
 */
public class TwoSumBST_Inorder {

    static class TreeNode {
        int val;
        TreeNode left, right;

        TreeNode(int v) { this.val = v; }
    }

    public boolean findTarget(TreeNode root, int k) {
        List<Integer> list = new ArrayList<>();
        inorder(root, list);

        int left = 0, right = list.size() - 1;

        while (left < right) {
            int sum = list.get(left) + list.get(right);

            if (sum == k) return true;
            else if (sum < k) left++;
            else right--;
        }

        return false;
    }

    private void inorder(TreeNode node, List<Integer> list) {
        if (node == null) return;

        inorder(node.left, list);
        list.add(node.val);
        inorder(node.right, list);
    }
}

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Problem:
 * Find if there exist TWO nodes in BST such that their sum = k
 *
 * ------------------------------------------------------------
 * OPTIMAL APPROACH (O(H) space):
 * ------------------------------------------------------------
 * Instead of converting BST → sorted array (O(N) space),
 * we simulate TWO POINTERS using TWO BST ITERATORS:
 *
 * 1. Forward Iterator  → gives smallest values (inorder)
 * 2. Reverse Iterator  → gives largest values (reverse inorder)
 *
 * Then apply:
 *      classic TWO SUM (two pointers)
 *
 * ------------------------------------------------------------
 * Time Complexity  : O(N)
 * Space Complexity : O(H)   (H = height of tree)
 * ------------------------------------------------------------
 */
public class TwoSumBST_Optimal_Clean {

    // Basic TreeNode definition
    static class TreeNode {
        int val;
        TreeNode left, right;

        TreeNode(int val) {
            this.val = val;
        }
    }

    /**
     * BST Iterator
     *
     * This iterator can work in TWO modes:
     *
     * 1. Forward  (reverse = false)
     *      → gives values in ASCENDING order
     *      → behaves like inorder traversal
     *
     * 2. Reverse  (reverse = true)
     *      → gives values in DESCENDING order
     *      → behaves like reverse inorder traversal
     *
     * ------------------------------------------------------------
     * KEY IDEA:
     * Instead of storing entire inorder traversal,
     * we lazily generate next element using a stack.
     *
     * Stack always maintains the "path" to next node.
     * ------------------------------------------------------------
     */
    static class BSTIterator {

        private final Deque<TreeNode> stack = new ArrayDeque<>();
        private final boolean reverse; // controls direction

        /**
         * Constructor
         *
         * @param root    root of BST
         * @param reverse false → smallest first
         *                true  → largest first
         */
        BSTIterator(TreeNode root, boolean reverse) {
            this.reverse = reverse;
            pushAll(root);
        }

        /**
         * Push all nodes along one direction:
         *
         * Forward iterator  → push LEFT chain (smallest first)
         * Reverse iterator  → push RIGHT chain (largest first)
         *
         * This ensures:
         * Top of stack always contains NEXT element
         */
        private void pushAll(TreeNode node) {
            while (node != null) {
                stack.push(node);

                // Direction decides traversal
                if (reverse) {
                    node = node.right;  // go towards larger values
                } else {
                    node = node.left;   // go towards smaller values
                }
            }
        }

        /**
         * @return true if more elements exist
         */
        public boolean hasNext() {
            return !stack.isEmpty();
        }

        /**
         * Returns next element in sorted order
         *
         * STEPS:
         * 1. Pop current node
         * 2. Depending on direction:
         *      - Forward  → explore RIGHT subtree
         *      - Reverse  → explore LEFT subtree
         * 3. Push path again using pushAll()
         */
        public int next() {
            TreeNode current = stack.pop();

            // If forward iterator:
            // next values are in RIGHT subtree
            if (!reverse) {
                pushAll(current.right);
            }
            // If reverse iterator:
            // next values are in LEFT subtree
            else {
                pushAll(current.left);
            }

            return current.val;
        }
    }

    /**
     * Main function
     *
     * Applies TWO POINTER technique using iterators
     */
    public boolean findTarget(TreeNode root, int target) {
        if (root == null) return false;

        // Left pointer → smallest values
        BSTIterator leftIterator = new BSTIterator(root, false);

        // Right pointer → largest values
        BSTIterator rightIterator = new BSTIterator(root, true);

        // Initialize two pointers
        int leftValue = leftIterator.next();   // smallest
        int rightValue = rightIterator.next(); // largest

        /**
         * IMPORTANT CONDITION:
         * leftValue < rightValue ensures:
         * - we don't use same node twice
         * - equivalent to left < right pointer in array
         */
        while (leftValue < rightValue) {

            int currentSum = leftValue + rightValue;

            if (currentSum == target) {
                return true; // Found valid pair
            }

            // Need larger sum → move LEFT pointer forward
            else if (currentSum < target) {
                if (!leftIterator.hasNext()) return false;
                leftValue = leftIterator.next();
            }

            // Need smaller sum → move RIGHT pointer backward
            else {
                if (!rightIterator.hasNext()) return false;
                rightValue = rightIterator.next();
            }
        }

        // No valid pair found
        return false;
    }
}

