import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;
/*
 * Given the root of a binary tree, return its maximum depth.
 * 
 * A binary tree's maximum depth is the number of nodes along the longest path
 * from the root node down to the farthest leaf node.
 * 
 * Example 1:
 * Input: root = [3,9,20,null,null,15,7]
 * Output: 3
 * 
 * Example 2:
 * Input: root = [1,null,2]
 * Output: 2
 */

// Definition for a binary tree node
class TreeNode {

    int val;
    TreeNode left;
    TreeNode right;

    TreeNode(int val) {
        this.val = val;
    }

}

class Solution {

    // Approach 1: Recursive DFS (Most intuitive)
    // Time: O(n), Space: O(h) where h is height of tree
    public int maxDepth(TreeNode root) {
        if (root == null) {
            return 0;
        }

        int leftDepth = maxDepth(root.left);
        int rightDepth = maxDepth(root.right);

        return Math.max(leftDepth, rightDepth) + 1;
    }

    // Approach 2: Iterative BFS (Level-order traversal)
    // Time: O(n), Space: O(w) where w is maximum width of tree
    public int maxDepthBFS(TreeNode root) {
        if (root == null) {
            return 0;
        }

        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);
        int depth = 0;

        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            depth++;

            for (int i = 0; i < levelSize; i++) {
                TreeNode node = queue.poll();

                if (node.left != null) {
                    queue.offer(node.left);
                }
                if (node.right != null) {
                    queue.offer(node.right);
                }
            }
        }

        return depth;
    }

    // Approach 3: Iterative DFS using Stack
    // Time: O(n), Space: O(h)
    public int maxDepthDFS(TreeNode root) {
        if (root == null) {
            return 0;
        }

        Stack<TreeNode> nodeStack = new Stack<>();
        Stack<Integer> depthStack = new Stack<>();

        nodeStack.push(root);
        depthStack.push(1);

        int maxDepth = 0;

        while (!nodeStack.isEmpty()) {
            TreeNode node = nodeStack.pop();
            int currentDepth = depthStack.pop();

            maxDepth = Math.max(maxDepth, currentDepth);

            if (node.left != null) {
                nodeStack.push(node.left);
                depthStack.push(currentDepth + 1);
            }

            if (node.right != null) {
                nodeStack.push(node.right);
                depthStack.push(currentDepth + 1);
            }
        }

        return maxDepth;
    }

    // Approach 4: One-liner recursive solution
    public int maxDepthOneLiner(TreeNode root) {
        return root == null ? 0 : 1 + Math.max(maxDepthOneLiner(root.left), maxDepthOneLiner(root.right));
    }

    // Test method to demonstrate usage
    public static void main(String[] args) {
        Solution solution = new Solution();

        // Example: Tree [3,9,20,null,null,15,7]
        // 3
        // / \
        // 9 20
        // / \
        // 15 7
        TreeNode root = new TreeNode(3);
        root.left = new TreeNode(9);
        root.right = new TreeNode(20);
        root.right.left = new TreeNode(15);
        root.right.right = new TreeNode(7);

        System.out.println("Recursive DFS: " + solution.maxDepth(root)); // Output: 3
        System.out.println("Iterative BFS: " + solution.maxDepthBFS(root)); // Output: 3
        System.out.println("Iterative DFS: " + solution.maxDepthDFS(root)); // Output: 3
        System.out.println("One-liner: " + solution.maxDepthOneLiner(root)); // Output: 3

        // Edge case: empty tree
        System.out.println("Empty tree: " + solution.maxDepth(null)); // Output: 0

        // Edge case: single node
        TreeNode singleNode = new TreeNode(1);
        System.out.println("Single node: " + solution.maxDepth(singleNode)); // Output: 1
    }

}
