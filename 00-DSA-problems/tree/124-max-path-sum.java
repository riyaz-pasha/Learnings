/*
 * Binary Tree Maximum Path Sum (Leetcode 124)
 *
 * Objective:
 * ----------
 * Find the **maximum path sum** in a binary tree.
 * A path is any sequence of nodes from some starting node to any node in the tree along the parent-child connections.
 * The path must contain at least one node and does not need to go through the root.
 *
 * For example:
 *         -10
 *         /  \
 *        9   20
 *           /  \
 *          15   7
 *
 * Possible path with maximum sum: 15 → 20 → 7 = 42
 *
 * Key Observations:
 * -----------------
 * - We perform a **postorder traversal** (left → right → root) because we need to calculate left and right subtree contributions first.
 * - For each node:
 *   1. Calculate max gain from left child (ignore negative paths with Math.max(0, left))
 *   2. Calculate max gain from right child (same)
 *   3. Calculate the **price of the current split**: node.val + left + right
 *      → This assumes path passes through this node and both children.
 *   4. Update global `max` if this split produces a larger sum.
 *   5. Return the **maximum gain** this node can contribute to its parent:
 *      node.val + max(left, right)
 *
 * Diagram:
 * --------
 * For node 20:
 *     20
 *    /  \
 *   15   7
 *
 * - left = 15, right = 7
 * - max at node 20 = 15 + 20 + 7 = 42
 * - return to parent: 20 + max(15, 7) = 35
 *
 * Final Answer: 42
 *
 * Time: O(n) — each node is visited once
 * Space: O(h) — call stack, h = height of tree
 */

class MaxPathSum {

    // Global max path sum found so far
    private int max = Integer.MIN_VALUE;

    public int maxPathSum(TreeNode root) {
        max = Integer.MIN_VALUE; // Reset before traversal
        postorder(root); // Start postorder DFS
        return max; // Final result stored in max
    }

    private int postorder(TreeNode node) {
        if (node == null)
            return 0;

        // Recursively compute max gain from left and right subtree
        // Ignore paths with negative sums (use 0 instead)
        int leftSum = Math.max(postorder(node.left), 0);
        int rightSum = Math.max(postorder(node.right), 0);

        // Max path sum including this node as the highest point
        int currentSum = node.val + leftSum + rightSum;

        // Update global max if needed
        max = Math.max(max, currentSum);

        // Return max gain if we continue the same path upwards
        return node.val + Math.max(leftSum, rightSum);
    }

}

class TreeNode {

    int val;
    TreeNode left;
    TreeNode right;

    TreeNode(int val) {
        this.val = val;
    }

}

/**
 * Solution 1: DFS with Global Maximum Tracking
 * 
 * Key Insight: For each node, we need to consider:
 * 1. The maximum path that passes through this node (for global max)
 * 2. The maximum path that starts from this node (to return to parent)
 * 
 * Time Complexity: O(n) where n is number of nodes
 * Space Complexity: O(h) where h is height of tree (recursion stack)
 */
class Solution1 {

    private int maxSum = Integer.MIN_VALUE;

    public int maxPathSum(TreeNode root) {
        maxSum = Integer.MIN_VALUE;
        maxPathDown(root);
        return maxSum;
    }

    /**
     * Returns the maximum sum of a path starting from the given node
     * and going down to any of its descendants (or just the node itself)
     */
    private int maxPathDown(TreeNode node) {
        if (node == null)
            return 0;

        // Get maximum path sum from left and right subtrees
        // Use Math.max(0, ...) to ignore negative paths
        int leftMax = Math.max(0, maxPathDown(node.left));
        int rightMax = Math.max(0, maxPathDown(node.right));

        // Calculate the maximum path sum that passes through current node
        int pathThroughNode = node.val + leftMax + rightMax;

        // Update global maximum
        maxSum = Math.max(maxSum, pathThroughNode);

        // Return the maximum path sum starting from current node
        // (can only choose one side when returning to parent)
        return node.val + Math.max(leftMax, rightMax);
    }

}

/**
 * Solution 2: DFS with Result Wrapper Class
 * 
 * Alternative approach using a wrapper class to avoid instance variables
 */
class Solution2 {

    static class Result {
        int maxSum = Integer.MIN_VALUE;
    }

    public int maxPathSum(TreeNode root) {
        Result result = new Result();
        maxPathDown(root, result);
        return result.maxSum;
    }

    private int maxPathDown(TreeNode node, Result result) {
        if (node == null)
            return 0;

        int leftMax = Math.max(0, maxPathDown(node.left, result));
        int rightMax = Math.max(0, maxPathDown(node.right, result));

        // Path through current node
        int pathThroughNode = node.val + leftMax + rightMax;
        result.maxSum = Math.max(result.maxSum, pathThroughNode);

        // Return max path starting from current node
        return node.val + Math.max(leftMax, rightMax);
    }

}

/**
 * Solution 3: Clean Implementation with Comments
 */
class Solution3 {

    private int globalMax = Integer.MIN_VALUE;

    public int maxPathSum(TreeNode root) {
        globalMax = Integer.MIN_VALUE;
        dfs(root);
        return globalMax;
    }

    /**
     * DFS function that returns the maximum sum of a path
     * that starts from the current node and goes down
     */
    private int dfs(TreeNode node) {
        if (node == null)
            return 0;

        // Recursively get the maximum path sum from left and right children
        // Use Math.max(0, ...) to ignore negative contributions
        int left = Math.max(0, dfs(node.left));
        int right = Math.max(0, dfs(node.right));

        // The maximum path sum that passes through the current node
        // This could be the answer we're looking for
        int currentMax = node.val + left + right;

        // Update the global maximum
        globalMax = Math.max(globalMax, currentMax);

        // Return the maximum sum of a path that starts from current node
        // We can only choose one side (left or right) when going up
        return node.val + Math.max(left, right);
    }

}

class TestMaxPathSum {

    public static void main(String[] args) {
        Solution1 solution = new Solution1();

        // Example 1: [1,2,3]
        TreeNode root1 = new TreeNode(1);
        root1.left = new TreeNode(2);
        root1.right = new TreeNode(3);
        System.out.println("Example 1 Result: " + solution.maxPathSum(root1)); // Expected: 6

        // Example 2: [-10,9,20,null,null,15,7]
        TreeNode root2 = new TreeNode(-10);
        root2.left = new TreeNode(9);
        root2.right = new TreeNode(20);
        root2.right.left = new TreeNode(15);
        root2.right.right = new TreeNode(7);
        System.out.println("Example 2 Result: " + solution.maxPathSum(root2)); // Expected: 42

        // Edge case: Single node with negative value
        TreeNode root3 = new TreeNode(-3);
        System.out.println("Single negative node: " + solution.maxPathSum(root3)); // Expected: -3
    }
}

/**
 * Detailed Explanation:
 * 
 * The key insight is that for each node, we need to make a decision:
 * 1. What's the maximum path sum that passes THROUGH this node?
 * 2. What's the maximum path sum that STARTS from this node and goes down?
 * 
 * For question 1: We consider paths that go left_child -> current_node ->
 * right_child
 * This gives us: left_max + node.val + right_max
 * 
 * For question 2: We can only choose one direction when returning to parent
 * This gives us: node.val + max(left_max, right_max)
 * 
 * We use Math.max(0, subtree_result) to ignore negative contributions,
 * since we can always choose not to include a negative path.
 * 
 * Time Complexity: O(n) - we visit each node exactly once
 * Space Complexity: O(h) - recursion depth equals tree height
 */
