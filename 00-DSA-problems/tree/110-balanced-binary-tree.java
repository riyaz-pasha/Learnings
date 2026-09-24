/**
 * PROBLEM ANALYSIS & INTERVIEW APPROACH
 * =====================================
 * 
 * This is a TREE VALIDATION problem testing:
 * 1. Understanding of height-balanced definition
 * 2. Ability to compute tree height efficiently
 * 3. Optimization from O(N²) to O(N)
 * 4. Bottom-up thinking (postorder traversal)
 * 5. Early termination optimization
 * 
 * KEY INSIGHTS:
 * -------------
 * 1. HEIGHT-BALANCED DEFINITION:
 *    "The heights of two subtrees of any node never differ by more than 1"
 *    - For EVERY node, |height(left) - height(right)| ≤ 1
 *    - Not just the root, but ALL nodes!
 * 
 * 2. NAIVE APPROACH: O(N²)
 *    - Calculate height at each node: O(N)
 *    - Do this for all N nodes: O(N²)
 *    - Too slow!
 * 
 * 3. OPTIMAL APPROACH: O(N)
 *    - Calculate height WHILE checking balance
 *    - Use postorder traversal (bottom-up)
 *    - Return special value (-1) to signal imbalance
 *    - Check each node exactly once!
 * 
 * VISUALIZATION:
 * --------------
 * 
 * BALANCED TREE:
 *        3
 *       / \
 *      9  20
 *         / \
 *        15  7
 * 
 * Height of 9: 1
 * Height of 20: 2
 * Difference at root: |1 - 2| = 1 ✓ (≤ 1)
 * Result: BALANCED
 * 
 * UNBALANCED TREE:
 *          1
 *         /
 *        2
 *       /
 *      3
 * 
 * At node 1: |height(left=2) - height(right=0)| = 2 ✗ (> 1)
 * Result: NOT BALANCED
 * 
 * INTERVIEW STRATEGY:
 * -------------------
 * 1. Clarify the definition
 *    "Just to confirm: balanced means the height difference
 *     of subtrees is at most 1 for EVERY node, correct?"
 * 
 * 2. Start with naive approach
 *    "I could calculate height at each node, but that's O(N²)..."
 * 
 * 3. Optimize to O(N)
 *    "Better: check balance while calculating height in one pass."
 * 
 * 4. Explain postorder choice
 *    "I'll use postorder because I need children's heights first."
 * 
 * 5. Discuss early termination
 *    "If any subtree is unbalanced, I can return immediately."
 */

// Definition for a binary tree node
class TreeNode {
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

class BalancedBinaryTreeSolution {
    
    /**
     * APPROACH 1: OPTIMAL SOLUTION - O(N) TIME
     * =========================================
     * 
     * ALGORITHM:
     * ----------
     * Use postorder traversal (bottom-up) to:
     * 1. Calculate height of subtrees
     * 2. Check if subtrees are balanced
     * 3. Return height or -1 (signals imbalance)
     * 
     * Key insight: Return -1 to propagate imbalance upward
     * 
     * POSTORDER (LEFT → RIGHT → ROOT):
     * --------------------------------
     * We need children's info before processing parent
     * - Get left subtree height
     * - Get right subtree height
     * - Check if current node is balanced
     * - Return height to parent
     * 
     * DETAILED WALKTHROUGH:
     * ---------------------
     *        3
     *       / \
     *      9  20
     *         / \
     *        15  7
     * 
     * Step 1: checkHeight(9)
     *   - No children, height = 1
     *   - Balanced ✓
     * 
     * Step 2: checkHeight(15)
     *   - No children, height = 1
     *   - Balanced ✓
     * 
     * Step 3: checkHeight(7)
     *   - No children, height = 1
     *   - Balanced ✓
     * 
     * Step 4: checkHeight(20)
     *   - left height = 1 (node 15)
     *   - right height = 1 (node 7)
     *   - |1 - 1| = 0 ≤ 1 ✓ Balanced!
     *   - Return height = max(1,1) + 1 = 2
     * 
     * Step 5: checkHeight(3)
     *   - left height = 1 (node 9)
     *   - right height = 2 (node 20)
     *   - |1 - 2| = 1 ≤ 1 ✓ Balanced!
     *   - Return height = max(1,2) + 1 = 3
     * 
     * Result: height = 3 (not -1) → BALANCED!
     * 
     * UNBALANCED EXAMPLE:
     * -------------------
     *      1
     *     /
     *    2
     *   /
     *  3
     * 
     * checkHeight(3): height = 1
     * checkHeight(2): 
     *   - left height = 1
     *   - right height = 0
     *   - |1 - 0| = 1 ✓ Balanced at this node
     *   - Return height = 2
     * checkHeight(1):
     *   - left height = 2
     *   - right height = 0
     *   - |2 - 0| = 2 > 1 ✗ NOT BALANCED!
     *   - Return -1 (signals imbalance)
     * 
     * Result: -1 → NOT BALANCED!
     * 
     * TIME COMPLEXITY: O(N)
     * - Visit each node exactly once
     * - Calculate height in same pass
     * 
     * SPACE COMPLEXITY: O(H)
     * - H = height of tree
     * - Recursion stack depth
     * - Balanced: O(log N)
     * - Skewed: O(N)
     * 
     * WHY THIS IS OPTIMAL:
     * --------------------
     * - Single pass through tree
     * - Early termination when imbalance found
     * - No redundant height calculations
     * - Clean, elegant code
     */
    public boolean isBalanced(TreeNode root) {
        // If checkHeight returns -1, tree is not balanced
        return checkHeight(root) != -1;
    }
    
    /**
     * Helper function to check height and balance simultaneously
     * 
     * Returns:
     * - Height of subtree if balanced (≥ 0)
     * - -1 if NOT balanced (propagates upward)
     * 
     * This is the KEY OPTIMIZATION: combine height calculation
     * with balance checking in one pass!
     */
    private int checkHeight(TreeNode node) {
        // Base case: null node has height 0
        if (node == null) {
            return 0;
        }
        
        // POSTORDER: Get children's heights first
        
        // Step 1: Check left subtree
        int leftHeight = checkHeight(node.left);
        if (leftHeight == -1) {
            // Left subtree is unbalanced, propagate upward
            return -1;
        }
        
        // Step 2: Check right subtree
        int rightHeight = checkHeight(node.right);
        if (rightHeight == -1) {
            // Right subtree is unbalanced, propagate upward
            return -1;
        }
        
        // Step 3: Check if CURRENT node is balanced
        // Height difference must be ≤ 1
        if (Math.abs(leftHeight - rightHeight) > 1) {
            // Current node violates balance property
            return -1;
        }
        
        // Step 4: Both subtrees balanced AND current balanced
        // Return height of this subtree
        return Math.max(leftHeight, rightHeight) + 1;
    }
    
    /**
     * APPROACH 2: NAIVE SOLUTION - O(N²) TIME
     * ========================================
     * 
     * ALGORITHM:
     * ----------
     * For each node:
     * 1. Calculate height of left subtree
     * 2. Calculate height of right subtree
     * 3. Check if difference ≤ 1
     * 4. Recursively check left and right subtrees
     * 
     * PROBLEM: Recalculates height multiple times!
     * 
     * Example:
     *        1
     *       / \
     *      2   3
     *     /
     *    4
     * 
     * When checking node 1:
     *   - Calculate height of subtree rooted at 2
     *   - This visits nodes 2 and 4
     * 
     * When checking node 2:
     *   - Calculate height again!
     *   - Visits node 4 again
     * 
     * Height is calculated for each node from every ancestor!
     * For a balanced tree of height H:
     * - Node at depth d is visited H-d times
     * - Total operations: O(N²) in worst case
     * 
     * TIME COMPLEXITY: O(N²)
     * - For each node: O(N) to calculate height
     * - N nodes: O(N²) total
     * 
     * SPACE COMPLEXITY: O(H)
     * - Recursion stack
     * 
     * WHY SHOW THIS:
     * --------------
     * - Demonstrates understanding of problem
     * - Shows ability to optimize
     * - Good starting point in interview
     */
    public boolean isBalancedNaive(TreeNode root) {
        // Empty tree is balanced
        if (root == null) {
            return true;
        }
        
        // Check current node's balance
        int leftHeight = height(root.left);
        int rightHeight = height(root.right);
        
        if (Math.abs(leftHeight - rightHeight) > 1) {
            return false;
        }
        
        // Recursively check subtrees
        return isBalancedNaive(root.left) && isBalancedNaive(root.right);
    }
    
    // Helper: Calculate height (called many times - inefficient!)
    private int height(TreeNode node) {
        if (node == null) {
            return 0;
        }
        return Math.max(height(node.left), height(node.right)) + 1;
    }
    
    /**
     * APPROACH 3: USING RESULT CLASS (Alternative O(N))
     * ==================================================
     * 
     * Instead of using -1 sentinel, use a Result class
     * to explicitly track both height and balance status
     * 
     * PROS:
     * - More explicit and self-documenting
     * - Easier to understand for some people
     * 
     * CONS:
     * - More code / overhead
     * - Sentinel value (-1) is cleaner
     * 
     * This shows alternative design choices in interviews!
     */
    
    // Result class to hold both height and balance status
    private static class Result {
        boolean isBalanced;
        int height;
        
        Result(boolean isBalanced, int height) {
            this.isBalanced = isBalanced;
            this.height = height;
        }
    }
    
    public boolean isBalancedWithClass(TreeNode root) {
        return checkBalanceWithClass(root).isBalanced;
    }
    
    private Result checkBalanceWithClass(TreeNode node) {
        // Base case: null node
        if (node == null) {
            return new Result(true, 0);
        }
        
        // Check left subtree
        Result left = checkBalanceWithClass(node.left);
        if (!left.isBalanced) {
            return new Result(false, 0);
        }
        
        // Check right subtree
        Result right = checkBalanceWithClass(node.right);
        if (!right.isBalanced) {
            return new Result(false, 0);
        }
        
        // Check current node
        boolean balanced = Math.abs(left.height - right.height) <= 1;
        int height = Math.max(left.height, right.height) + 1;
        
        return new Result(balanced, height);
    }
}

/**
 * COMPLEXITY COMPARISON
 * =====================
 * 
 * Approach        | Time  | Space | Code Simplicity
 * ----------------|-------|-------|------------------
 * Optimal (-1)    | O(N)  | O(H)  | Clean
 * With Result     | O(N)  | O(H)  | Explicit
 * Naive           | O(N²) | O(H)  | Simple but slow
 * 
 * RECOMMENDATION:
 * - Interview: Use optimal (-1) approach
 * - Show naive first, then optimize
 * - Mention Result class as alternative
 */

/**
 * COMMON MISTAKES & EDGE CASES
 * =============================
 * 
 * MISTAKES:
 * 1. Only checking root's children (forgetting ALL nodes)
 * 2. Using O(N²) solution in production
 * 3. Forgetting to propagate imbalance upward
 * 4. Off-by-one errors in height calculation
 * 
 * EDGE CASES:
 * 1. Empty tree (null) → Balanced
 * 2. Single node → Balanced
 * 3. Perfectly balanced tree
 * 4. Left-skewed tree → Not balanced (if height > 1)
 * 5. Right-skewed tree → Not balanced (if height > 1)
 * 6. Tree balanced at root but not at deeper levels
 */

/**
 * RELATED PROBLEMS
 * ================
 * 
 * 1. Maximum Depth of Binary Tree
 *    - Same height calculation logic
 * 
 * 2. Minimum Depth of Binary Tree
 *    - Similar but need to handle leaf nodes carefully
 * 
 * 3. Diameter of Binary Tree
 *    - Uses similar postorder pattern
 *    - Calculate left/right info, process at current
 * 
 * 4. Binary Tree Maximum Path Sum
 *    - Similar bottom-up approach
 * 
 * 5. Validate Binary Search Tree
 *    - Different property but similar recursive pattern
 */

// Comprehensive test suite
class Main {
    public static void main(String[] args) {
        BalancedBinaryTreeSolution sol = new BalancedBinaryTreeSolution();
        
        System.out.println("=== Test Case 1: Balanced Tree ===");
        //        3
        //       / \
        //      9  20
        //         / \
        //        15  7
        TreeNode root1 = new TreeNode(3);
        root1.left = new TreeNode(9);
        root1.right = new TreeNode(20);
        root1.right.left = new TreeNode(15);
        root1.right.right = new TreeNode(7);
        
        System.out.println("Optimal:  " + sol.isBalanced(root1));
        System.out.println("Naive:    " + sol.isBalancedNaive(root1));
        System.out.println("Expected: true");
        
        System.out.println("\n=== Test Case 2: Unbalanced Tree ===");
        //          1
        //         /
        //        2
        //       / \
        //      3   3
        //     / \
        //    4   4
        TreeNode root2 = new TreeNode(1);
        root2.left = new TreeNode(2);
        root2.left.left = new TreeNode(3);
        root2.left.right = new TreeNode(3);
        root2.left.left.left = new TreeNode(4);
        root2.left.left.right = new TreeNode(4);
        
        System.out.println("Optimal:  " + sol.isBalanced(root2));
        System.out.println("Naive:    " + sol.isBalancedNaive(root2));
        System.out.println("Expected: false");
        
        System.out.println("\n=== Test Case 3: Empty Tree ===");
        System.out.println("Result:   " + sol.isBalanced(null));
        System.out.println("Expected: true");
        
        System.out.println("\n=== Test Case 4: Single Node ===");
        TreeNode root4 = new TreeNode(1);
        System.out.println("Result:   " + sol.isBalanced(root4));
        System.out.println("Expected: true");
        
        System.out.println("\n=== Test Case 5: Left-Skewed (Unbalanced) ===");
        //     3
        //    /
        //   2
        //  /
        // 1
        TreeNode root5 = new TreeNode(3);
        root5.left = new TreeNode(2);
        root5.left.left = new TreeNode(1);
        System.out.println("Result:   " + sol.isBalanced(root5));
        System.out.println("Expected: false");
        
        System.out.println("\n=== Test Case 6: Two Levels (Balanced) ===");
        //     1
        //    / \
        //   2   3
        TreeNode root6 = new TreeNode(1);
        root6.left = new TreeNode(2);
        root6.right = new TreeNode(3);
        System.out.println("Result:   " + sol.isBalanced(root6));
        System.out.println("Expected: true");
        
        System.out.println("\n=== Test Case 7: Almost Balanced ===");
        //       1
        //      / \
        //     2   3
        //    /
        //   4
        TreeNode root7 = new TreeNode(1);
        root7.left = new TreeNode(2);
        root7.right = new TreeNode(3);
        root7.left.left = new TreeNode(4);
        System.out.println("Result:   " + sol.isBalanced(root7));
        System.out.println("Expected: true (height diff at root = 1)");
        
        System.out.println("\n=== Test Case 8: Balanced But Not Perfect ===");
        //         1
        //       /   \
        //      2     3
        //     / \   /
        //    4   5 6
        TreeNode root8 = new TreeNode(1);
        root8.left = new TreeNode(2);
        root8.right = new TreeNode(3);
        root8.left.left = new TreeNode(4);
        root8.left.right = new TreeNode(5);
        root8.right.left = new TreeNode(6);
        System.out.println("Result:   " + sol.isBalanced(root8));
        System.out.println("Expected: true");
        
        // Performance comparison
        System.out.println("\n=== Performance Comparison ===");
        TreeNode large = buildBalancedTree(10); // Height 10
        
        long start = System.nanoTime();
        sol.isBalanced(large);
        long optimalTime = System.nanoTime() - start;
        
        start = System.nanoTime();
        sol.isBalancedNaive(large);
        long naiveTime = System.nanoTime() - start;
        
        System.out.println("Tree height: 10");
        System.out.println("Optimal O(N):  " + optimalTime / 1000 + " μs");
        System.out.println("Naive O(N²):   " + naiveTime / 1000 + " μs");
        System.out.println("Speedup: " + (naiveTime / (double) optimalTime) + "x");
    }
    
    // Helper to build a balanced tree of given height
    private static TreeNode buildBalancedTree(int height) {
        if (height == 0) return null;
        TreeNode node = new TreeNode(1);
        node.left = buildBalancedTree(height - 1);
        node.right = buildBalancedTree(height - 1);
        return node;
    }
}

/**
 * INTERVIEW TALKING POINTS
 * =========================
 * 
 * When presenting this solution in an interview:
 * 
 * 1. "A height-balanced tree means for every node, the height
 *    difference of its left and right subtrees is at most 1."
 * 
 * 2. "I could calculate height at each node and check, but
 *    that would be O(N²) because I'd recalculate heights."
 *    [Sketch naive approach on board]
 * 
 * 3. "Better approach: Calculate height while checking balance
 *    in a single pass. I'll use postorder traversal because
 *    I need children's heights before checking the parent."
 * 
 * 4. "I'll use -1 as a sentinel value to signal imbalance.
 *    Once we detect imbalance anywhere, we propagate it upward."
 *    [Write optimal solution]
 * 
 * 5. "This achieves O(N) time - we visit each node exactly once.
 *    Space is O(H) for the recursion stack."
 * 
 * 6. "Edge cases: empty tree (balanced), single node (balanced),
 *    and checking all nodes, not just the root."
 * 
 * KEY POINTS TO EMPHASIZE:
 * - Why postorder (need children's info first)
 * - Sentinel value for early termination
 * - Single pass optimization
 * - Bottom-up approach
 * 
 * FOLLOW-UP QUESTIONS TO EXPECT:
 * - Can you do it iteratively? [Yes, but complex]
 * - What if we want to balance the tree? [AVL/Red-Black trees]
 * - How would you modify for "k-balanced"? [Change threshold]
 * - What's the height of a balanced tree? [O(log N)]
 * - Can you find where the imbalance is? [Track node in sentinel]
 */

class BalancedBinaryTree {
   public boolean isBalanced(TreeNode root) {
        return checkBalancedAndHeight(root) != -1;
    }

    private int checkBalancedAndHeight(TreeNode node) {
        if (node == null) {
            return 0;
        }

        int left = checkBalancedAndHeight(node.left);
        if (left == -1) {
            return -1;
        }

        int right = checkBalancedAndHeight(node.right);
        if (right == -1) {
            return -1;
        }

        if (Math.abs(left - right) > 1) {
            return -1;
        }

        return 1 + Math.max(left, right);
    }

}
