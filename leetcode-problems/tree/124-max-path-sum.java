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
        postorder(root);         // Start postorder DFS
        return max;              // Final result stored in max
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
