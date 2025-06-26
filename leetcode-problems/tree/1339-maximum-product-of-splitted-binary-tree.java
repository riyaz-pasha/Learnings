/*
 * Given the root of a binary tree, split the binary tree into two subtrees by
 * removing one edge such that the product of the sums of the subtrees is
 * maximized.
 * 
 * Return the maximum product of the sums of the two subtrees. Since the answer
 * may be too large, return it modulo 109 + 7.
 * 
 * Note that you need to maximize the answer before taking the mod and not after
 * taking it.
 * 
 * Example 1:
 * Input: root = [1,2,3,4,5,6]
 * Output: 110
 * Explanation: Remove the red edge and get 2 binary trees with sum 11 and 10.
 * Their product is 110 (11*10)
 * 
 * Example 2:
 * Input: root = [1,null,2,3,4,null,null,5,6]
 * Output: 90
 * Explanation: Remove the red edge and get 2 binary trees with sum 15 and
 * 6.Their product is 90 (15*6)
 */

class TreeNode {

    int val;
    TreeNode left;
    TreeNode right;

    TreeNode(int val) {
        this.val = val;
    }

}

class Solution {

    private static final int MOD = 1000000007;
    private long maxProduct = 0;
    private long totalSum = 0;

    public int maxProduct(TreeNode root) {
        // Time Complexity: O(n) where n is the number of nodes
        // Space Complexity: O(h) where h is the height of the tree (recursion stack)

        // First pass: calculate total sum of all nodes
        totalSum = calculateSum(root);

        // Second pass: try removing each edge and calculate product
        calculateSubtreeSums(root);

        return (int) (maxProduct % MOD);
    }

    // Helper method to calculate total sum of the tree
    // Time Complexity: O(n)
    private long calculateSum(TreeNode node) {
        if (node == null)
            return 0;
        return node.val + calculateSum(node.left) + calculateSum(node.right);
    }

    // Helper method to calculate subtree sums and find maximum product
    // Time Complexity: O(n)
    private long calculateSubtreeSums(TreeNode node) {
        if (node == null)
            return 0;

        // Calculate sum of current subtree
        long leftSum = calculateSubtreeSums(node.left);
        long rightSum = calculateSubtreeSums(node.right);
        long currentSubtreeSum = node.val + leftSum + rightSum;

        // If we remove the edge connecting this node to its parent,
        // one subtree will have sum = currentSubtreeSum
        // other subtree will have sum = totalSum - currentSubtreeSum
        if (node != null) { // We don't remove the root
            long otherSubtreeSum = totalSum - currentSubtreeSum;
            long product = currentSubtreeSum * otherSubtreeSum;
            maxProduct = Math.max(maxProduct, product);
        }

        return currentSubtreeSum;
    }

}

// Alternative implementation with single pass
class SolutionOptimized {

    private static final int MOD = 1000000007;
    private long maxProduct = 0;
    private long totalSum = 0;

    public int maxProduct(TreeNode root) {
        // Time Complexity: O(n) where n is the number of nodes
        // Space Complexity: O(h) where h is the height of the tree

        // First pass: calculate total sum
        totalSum = dfs(root);

        // Second pass: find maximum product
        dfs(root);

        return (int) (maxProduct % MOD);
    }

    private long dfs(TreeNode node) {
        if (node == null)
            return 0;

        long leftSum = dfs(node.left);
        long rightSum = dfs(node.right);
        long subtreeSum = node.val + leftSum + rightSum;

        // Only calculate product if totalSum is already calculated
        // (i.e., in the second pass)
        if (totalSum != 0) {
            // Try removing left child
            if (node.left != null) {
                long product = leftSum * (totalSum - leftSum);
                maxProduct = Math.max(maxProduct, product);
            }

            // Try removing right child
            if (node.right != null) {
                long product = rightSum * (totalSum - rightSum);
                maxProduct = Math.max(maxProduct, product);
            }
        }

        return subtreeSum;
    }

}

class Solution2 {

    private long totalTreeSum = 0;
    private long maxProduct = 0;
    private final int MOD = 1_000_000_007;

    public int maxProduct(TreeNode root) {
        // First pass to calculate the total sum of all nodes in the tree
        totalTreeSum = calculateTotalSum(root);

        // Second pass to calculate max product by trying every possible subtree split
        computeSubtreeSum(root);

        // Return the max product modulo 1e9+7
        return (int) (maxProduct % MOD);
    }

    // First DFS: Calculate total sum of the entire tree
    private long calculateTotalSum(TreeNode node) {
        if (node == null)
            return 0;
        return node.val + calculateTotalSum(node.left) + calculateTotalSum(node.right);
    }

    // Second DFS: For each subtree, compute the product of the two resulting parts
    private long computeSubtreeSum(TreeNode node) {
        if (node == null)
            return 0;

        long leftSum = computeSubtreeSum(node.left);
        long rightSum = computeSubtreeSum(node.right);
        long currentSubtreeSum = leftSum + rightSum + node.val;

        // Compute product of current split: (subtreeSum) * (total - subtreeSum)
        long product = currentSubtreeSum * (totalTreeSum - currentSubtreeSum);
        maxProduct = Math.max(maxProduct, product);

        return currentSubtreeSum;
    }

}

// Test cases
class TestCases {

    public static void main(String[] args) {
        Solution solution = new Solution();

        // Test Case 1: [1,2,3,4,5,6]
        TreeNode root1 = new TreeNode(1);
        root1.left = new TreeNode(2);
        root1.right = new TreeNode(3);
        root1.left.left = new TreeNode(4);
        root1.left.right = new TreeNode(5);
        root1.right.left = new TreeNode(6);

        System.out.println("Test 1: " + solution.maxProduct(root1)); // Expected: 110

        // Test Case 2: [1,null,2,3,4,null,null,5,6]
        TreeNode root2 = new TreeNode(1);
        root2.right = new TreeNode(2);
        root2.right.left = new TreeNode(3);
        root2.right.right = new TreeNode(4);
        root2.right.left.left = new TreeNode(5);
        root2.right.left.right = new TreeNode(6);

        System.out.println("Test 2: " + solution.maxProduct(root2)); // Expected: 90
    }

}
