/*
 * Given a binary tree root and an integer target, delete all the leaf nodes
 * with value target.
 * 
 * Note that once you delete a leaf node with value target, if its parent node
 * becomes a leaf node and has the value target, it should also be deleted (you
 * need to continue doing that until you cannot).
 * 
 * Example 1:
 * Input: root = [1,2,3,2,null,2,4], target = 2
 * Output: [1,null,3,null,4]
 * Explanation: Leaf nodes in green with value (target = 2) are removed (Picture
 * in left).
 * After removing, new nodes become leaf nodes with value (target = 2) (Picture
 * in center).
 * 
 * Example 2:
 * Input: root = [1,3,3,3,2], target = 3
 * Output: [1,3,null,null,2]
 * 
 * Example 3:
 * Input: root = [1,2,null,2,null,2], target = 2
 * Output: [1]
 * Explanation: Leaf nodes in green with value (target = 2) are removed at each
 * step.
 */

/**
 * Definition for a binary tree node.
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

    public TreeNode removeLeafNodes(TreeNode root, int target) {
        // Time Complexity: O(n) where n is the number of nodes
        // Space Complexity: O(h) where h is the height of the tree (recursion stack)

        // Base case: if node is null, return null
        if (root == null) {
            return null;
        }

        // Recursively process left and right subtrees first (post-order)
        root.left = removeLeafNodes(root.left, target);
        root.right = removeLeafNodes(root.right, target);

        // After processing children, check if current node should be removed
        // A node should be removed if:
        // 1. It's a leaf node (both children are null)
        // 2. Its value equals target
        if (root.left == null && root.right == null && root.val == target) {
            return null; // Remove this node
        }

        return root; // Keep this node
    }

}

// Alternative iterative solution using stack
class SolutionIterative {

    public TreeNode removeLeafNodes(TreeNode root, int target) {
        // Time Complexity: O(n * h) where n is nodes and h is height
        // Space Complexity: O(h) for the stack

        if (root == null)
            return null;

        // Keep removing until no more changes
        boolean changed = true;
        while (changed) {
            changed = false;
            root = removeLeafNodesOnePass(root, target);
            if (root != null) {
                changed = hasTargetLeaf(root, target);
            }
        }

        return root;
    }

    private TreeNode removeLeafNodesOnePass(TreeNode node, int target) {
        if (node == null)
            return null;

        node.left = removeLeafNodesOnePass(node.left, target);
        node.right = removeLeafNodesOnePass(node.right, target);

        if (node.left == null && node.right == null && node.val == target) {
            return null;
        }

        return node;
    }

    private boolean hasTargetLeaf(TreeNode node, int target) {
        if (node == null)
            return false;

        if (node.left == null && node.right == null && node.val == target) {
            return true;
        }

        return hasTargetLeaf(node.left, target) || hasTargetLeaf(node.right, target);
    }

}

// Helper class for testing and tree construction
class TreeHelper {

    // Build tree from array representation (level order)
    public static TreeNode buildTree(Integer[] arr) {
        if (arr == null || arr.length == 0 || arr[0] == null) {
            return null;
        }

        TreeNode root = new TreeNode(arr[0]);
        java.util.Queue<TreeNode> queue = new java.util.LinkedList<>();
        queue.offer(root);

        int i = 1;
        while (!queue.isEmpty() && i < arr.length) {
            TreeNode node = queue.poll();

            if (i < arr.length && arr[i] != null) {
                node.left = new TreeNode(arr[i]);
                queue.offer(node.left);
            }
            i++;

            if (i < arr.length && arr[i] != null) {
                node.right = new TreeNode(arr[i]);
                queue.offer(node.right);
            }
            i++;
        }

        return root;
    }

    // Print tree in level order for verification
    public static void printTree(TreeNode root) {
        if (root == null) {
            System.out.println("[]");
            return;
        }

        java.util.Queue<TreeNode> queue = new java.util.LinkedList<>();
        queue.offer(root);
        java.util.List<String> result = new java.util.ArrayList<>();

        while (!queue.isEmpty()) {
            TreeNode node = queue.poll();
            if (node != null) {
                result.add(String.valueOf(node.val));
                queue.offer(node.left);
                queue.offer(node.right);
            } else {
                result.add("null");
            }
        }

        // Remove trailing nulls
        while (!result.isEmpty() && result.get(result.size() - 1).equals("null")) {
            result.remove(result.size() - 1);
        }

        System.out.println(result);
    }

}

// Test cases
class TestCases {

    public static void main(String[] args) {
        Solution solution = new Solution();

        // Test Case 1: [1,2,3,2,null,2,4], target = 2
        // Expected: [1,null,3,null,4]
        TreeNode root1 = TreeHelper.buildTree(new Integer[] { 1, 2, 3, 2, null, 2, 4 });
        System.out.print("Test 1 - Input: ");
        TreeHelper.printTree(root1);
        TreeNode result1 = solution.removeLeafNodes(root1, 2);
        System.out.print("Output: ");
        TreeHelper.printTree(result1);
        System.out.println();

        // Test Case 2: [1,3,3,3,2], target = 3
        // Expected: [1,3,null,null,2]
        TreeNode root2 = TreeHelper.buildTree(new Integer[] { 1, 3, 3, 3, 2 });
        System.out.print("Test 2 - Input: ");
        TreeHelper.printTree(root2);
        TreeNode result2 = solution.removeLeafNodes(root2, 3);
        System.out.print("Output: ");
        TreeHelper.printTree(result2);
        System.out.println();

        // Test Case 3: [1,2,null,2,null,2], target = 2
        // Expected: [1]
        TreeNode root3 = TreeHelper.buildTree(new Integer[] { 1, 2, null, 2, null, 2 });
        System.out.print("Test 3 - Input: ");
        TreeHelper.printTree(root3);
        TreeNode result3 = solution.removeLeafNodes(root3, 2);
        System.out.print("Output: ");
        TreeHelper.printTree(result3);
        System.out.println();

        // Edge Case: Single node with target value
        TreeNode root4 = new TreeNode(2);
        System.out.print("Test 4 - Input: ");
        TreeHelper.printTree(root4);
        TreeNode result4 = solution.removeLeafNodes(root4, 2);
        System.out.print("Output: ");
        TreeHelper.printTree(result4);
        System.out.println();

        // Edge Case: All nodes have target value
        TreeNode root5 = TreeHelper.buildTree(new Integer[] { 2, 2, 2, 2, 2 });
        System.out.print("Test 5 - Input: ");
        TreeHelper.printTree(root5);
        TreeNode result5 = solution.removeLeafNodes(root5, 2);
        System.out.print("Output: ");
        TreeHelper.printTree(result5);
    }

}
