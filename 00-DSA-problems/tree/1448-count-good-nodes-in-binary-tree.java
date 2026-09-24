/*
 * Given a binary tree root, a node X in the tree is named good if in the path
 * from root to X there are no nodes with a value greater than X.
 * 
 * Return the number of good nodes in the binary tree.
 * 
 * Example 1:
 * Input: root = [3,1,4,3,null,1,5]
 * Output: 4
 * Explanation: Nodes in blue are good.
 * Root Node (3) is always a good node.
 * Node 4 -> (3,4) is the maximum value in the path starting from the root.
 * Node 5 -> (3,4,5) is the maximum value in the path
 * Node 3 -> (3,1,3) is the maximum value in the path.
 * 
 * Example 2:
 * Input: root = [3,3,null,4,2]
 * Output: 3
 * Explanation: Node 2 -> (3, 3, 2) is not good, because "3" is higher than it.
 * 
 * Example 3:
 * Input: root = [1]
 * Output: 1
 * Explanation: Root is considered as good.
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

    public int goodNodes(TreeNode root) {
        // Time Complexity: O(n) where n is the number of nodes
        // Space Complexity: O(h) where h is the height of the tree (recursion stack)

        if (root == null)
            return 0;

        // Start DFS with root value as the initial maximum
        return dfs(root, root.val);
    }

    private int dfs(TreeNode node, int maxSoFar) {
        if (node == null)
            return 0;

        int count = 0;

        // Check if current node is good
        // A node is good if its value >= maximum value in path from root
        if (node.val >= maxSoFar) {
            count = 1;
        }

        // Update maximum value for the path to children
        int newMax = Math.max(maxSoFar, node.val);

        // Recursively count good nodes in left and right subtrees
        count += dfs(node.left, newMax);
        count += dfs(node.right, newMax);

        return count;
    }

}

// Alternative implementation with cleaner logic
class SolutionAlternative {

    private int goodNodeCount = 0;

    public int goodNodes(TreeNode root) {
        // Time Complexity: O(n) where n is the number of nodes
        // Space Complexity: O(h) where h is the height of the tree

        if (root == null)
            return 0;

        goodNodeCount = 0;
        dfs(root, Integer.MIN_VALUE);
        return goodNodeCount;
    }

    private void dfs(TreeNode node, int maxSoFar) {
        if (node == null)
            return;

        // If current node value >= max value in path, it's a good node
        if (node.val >= maxSoFar) {
            goodNodeCount++;
        }

        // Update max value for children
        int newMax = Math.max(maxSoFar, node.val);

        // Traverse left and right subtrees
        dfs(node.left, newMax);
        dfs(node.right, newMax);
    }

}

// Iterative solution using stack
class SolutionIterative {

    public int goodNodes(TreeNode root) {
        // Time Complexity: O(n) where n is the number of nodes
        // Space Complexity: O(h) where h is the height of the tree

        if (root == null)
            return 0;

        java.util.Stack<NodeMaxPair> stack = new java.util.Stack<>();
        stack.push(new NodeMaxPair(root, root.val));

        int count = 0;

        while (!stack.isEmpty()) {
            NodeMaxPair current = stack.pop();
            TreeNode node = current.node;
            int maxSoFar = current.maxValue;

            // Check if current node is good
            if (node.val >= maxSoFar) {
                count++;
            }

            // Update max value for children
            int newMax = Math.max(maxSoFar, node.val);

            // Add children to stack
            if (node.right != null) {
                stack.push(new NodeMaxPair(node.right, newMax));
            }
            if (node.left != null) {
                stack.push(new NodeMaxPair(node.left, newMax));
            }
        }

        return count;
    }

    // Helper class to store node and its path maximum
    private static class NodeMaxPair {
        TreeNode node;
        int maxValue;

        NodeMaxPair(TreeNode node, int maxValue) {
            this.node = node;
            this.maxValue = maxValue;
        }
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

    // Print tree structure for visualization
    public static void printTree(TreeNode root) {
        if (root == null) {
            System.out.println("Empty tree");
            return;
        }
        printTreeHelper(root, "", true);
    }

    private static void printTreeHelper(TreeNode node, String prefix, boolean isLast) {
        if (node != null) {
            System.out.println(prefix + (isLast ? "└── " : "├── ") + node.val);
            if (node.left != null || node.right != null) {
                if (node.left != null) {
                    printTreeHelper(node.left, prefix + (isLast ? "    " : "│   "), node.right == null);
                }
                if (node.right != null) {
                    printTreeHelper(node.right, prefix + (isLast ? "    " : "│   "), true);
                }
            }
        }
    }

}

// Test cases
class TestCases {

    public static void main(String[] args) {
        Solution solution = new Solution();
        SolutionIterative iterSolution = new SolutionIterative();

        // Test Case 1: [3,1,4,3,null,1,5]
        // Expected: 4 (nodes 3, 4, 5, and the leaf 3)
        TreeNode root1 = TreeHelper.buildTree(new Integer[] { 3, 1, 4, 3, null, 1, 5 });
        System.out.println("Test 1:");
        TreeHelper.printTree(root1);
        System.out.println("Good nodes (recursive): " + solution.goodNodes(root1));

        // Reset tree for iterative test
        root1 = TreeHelper.buildTree(new Integer[] { 3, 1, 4, 3, null, 1, 5 });
        System.out.println("Good nodes (iterative): " + iterSolution.goodNodes(root1));
        System.out.println();

        // Test Case 2: [3,3,null,4,2]
        // Expected: 3 (root 3, left 3, and node 4)
        TreeNode root2 = TreeHelper.buildTree(new Integer[] { 3, 3, null, 4, 2 });
        System.out.println("Test 2:");
        TreeHelper.printTree(root2);
        System.out.println("Good nodes (recursive): " + solution.goodNodes(root2));

        root2 = TreeHelper.buildTree(new Integer[] { 3, 3, null, 4, 2 });
        System.out.println("Good nodes (iterative): " + iterSolution.goodNodes(root2));
        System.out.println();

        // Test Case 3: [1]
        // Expected: 1 (only root)
        TreeNode root3 = TreeHelper.buildTree(new Integer[] { 1 });
        System.out.println("Test 3:");
        TreeHelper.printTree(root3);
        System.out.println("Good nodes (recursive): " + solution.goodNodes(root3));

        root3 = TreeHelper.buildTree(new Integer[] { 1 });
        System.out.println("Good nodes (iterative): " + iterSolution.goodNodes(root3));
        System.out.println();

        // Test Case 4: Decreasing path [5,4,3,2,1]
        // Expected: 1 (only root)
        TreeNode root4 = new TreeNode(5);
        root4.left = new TreeNode(4);
        root4.left.left = new TreeNode(3);
        root4.left.left.left = new TreeNode(2);
        root4.left.left.left.left = new TreeNode(1);

        System.out.println("Test 4 (Decreasing path):");
        TreeHelper.printTree(root4);
        System.out.println("Good nodes (recursive): " + solution.goodNodes(root4));
        System.out.println("Good nodes (iterative): " + iterSolution.goodNodes(root4));
        System.out.println();

        // Test Case 5: Increasing path [1,2,3,4,5]
        // Expected: 5 (all nodes)
        TreeNode root5 = new TreeNode(1);
        root5.right = new TreeNode(2);
        root5.right.right = new TreeNode(3);
        root5.right.right.right = new TreeNode(4);
        root5.right.right.right.right = new TreeNode(5);

        System.out.println("Test 5 (Increasing path):");
        TreeHelper.printTree(root5);
        System.out.println("Good nodes (recursive): " + solution.goodNodes(root5));
        System.out.println("Good nodes (iterative): " + iterSolution.goodNodes(root5));
    }

}
