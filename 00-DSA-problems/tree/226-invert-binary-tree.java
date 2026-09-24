import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;
/*
 * Given the root of a binary tree, invert the tree, and return its root.
 * 
 * Example 1:
 * Input: root = [4,2,7,1,3,6,9]
 * Output: [4,7,2,9,6,3,1]
 * 
 * Example 2:
 * Input: root = [2,1,3]
 * Output: [2,3,1]
 * 
 * Example 3:
 * Input: root = []
 * Output: []
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

    // Approach 1: Recursive DFS (Most elegant and intuitive)
    // Time: O(n), Space: O(h) where h is height of tree
    public TreeNode invertTree(TreeNode root) {
        if (root == null) {
            return null;
        }

        // Swap left and right children
        TreeNode temp = root.left;
        root.left = root.right;
        root.right = temp;

        // Recursively invert left and right subtrees
        invertTree(root.left);
        invertTree(root.right);

        return root;
    }

    // Approach 2: Recursive with cleaner swap
    // Time: O(n), Space: O(h)
    public TreeNode invertTreeClean(TreeNode root) {
        if (root == null) {
            return null;
        }

        // Recursively invert and swap subtrees in one step
        TreeNode left = invertTreeClean(root.left);
        TreeNode right = invertTreeClean(root.right);

        root.left = right;
        root.right = left;

        return root;
    }

    // Approach 3: Iterative using Queue (BFS)
    // Time: O(n), Space: O(w) where w is maximum width of tree
    public TreeNode invertTreeIterativeBFS(TreeNode root) {
        if (root == null) {
            return null;
        }

        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            TreeNode current = queue.poll();

            // Swap left and right children
            TreeNode temp = current.left;
            current.left = current.right;
            current.right = temp;

            // Add children to queue if they exist
            if (current.left != null) {
                queue.offer(current.left);
            }
            if (current.right != null) {
                queue.offer(current.right);
            }
        }

        return root;
    }

    // Approach 4: Iterative using Stack (DFS)
    // Time: O(n), Space: O(h)
    public TreeNode invertTreeIterativeDFS(TreeNode root) {
        if (root == null) {
            return null;
        }

        Stack<TreeNode> stack = new Stack<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            TreeNode current = stack.pop();

            // Swap left and right children
            TreeNode temp = current.left;
            current.left = current.right;
            current.right = temp;

            // Add children to stack if they exist
            if (current.left != null) {
                stack.push(current.left);
            }
            if (current.right != null) {
                stack.push(current.right);
            }
        }

        return root;
    }

    // Approach 5: One-liner recursive (most concise)
    public TreeNode invertTreeOneLiner(TreeNode root) {
        if (root == null)
            return null;

        TreeNode temp = root.left;
        root.left = invertTreeOneLiner(root.right);
        root.right = invertTreeOneLiner(temp);

        return root;
    }

    // Helper method to print tree in level order for testing
    public void printLevelOrder(TreeNode root) {
        if (root == null) {
            System.out.println("[]");
            return;
        }

        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);
        List<String> result = new ArrayList<>();

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

    // Helper method to create a copy of the tree for testing multiple approaches
    public TreeNode copyTree(TreeNode root) {
        if (root == null) {
            return null;
        }

        TreeNode newNode = new TreeNode(root.val);
        newNode.left = copyTree(root.left);
        newNode.right = copyTree(root.right);

        return newNode;
    }

    // Test method to demonstrate usage
    public static void main(String[] args) {
        Solution solution = new Solution();

        // Example 1: root = [4,2,7,1,3,6,9]
        // 4 4
        // / \ / \
        // 2 7 => 7 2
        // / \ / \ / \ / \
        // 1 3 6 9 9 6 3 1
        TreeNode root1 = new TreeNode(4);
        root1.left = new TreeNode(2);
        root1.right = new TreeNode(7);
        root1.left.left = new TreeNode(1);
        root1.left.right = new TreeNode(3);
        root1.right.left = new TreeNode(6);
        root1.right.right = new TreeNode(9);

        System.out.println("Example 1:");
        System.out.print("Original: ");
        solution.printLevelOrder(root1);

        TreeNode inverted1 = solution.invertTree(solution.copyTree(root1));
        System.out.print("Inverted (Recursive): ");
        solution.printLevelOrder(inverted1);

        TreeNode inverted1BFS = solution.invertTreeIterativeBFS(solution.copyTree(root1));
        System.out.print("Inverted (BFS): ");
        solution.printLevelOrder(inverted1BFS);

        // Example 2: root = [2,1,3]
        TreeNode root2 = new TreeNode(2);
        root2.left = new TreeNode(1);
        root2.right = new TreeNode(3);

        System.out.println("\nExample 2:");
        System.out.print("Original: ");
        solution.printLevelOrder(root2);

        TreeNode inverted2 = solution.invertTreeClean(solution.copyTree(root2));
        System.out.print("Inverted: ");
        solution.printLevelOrder(inverted2);

        // Example 3: root = []
        System.out.println("\nExample 3:");
        System.out.print("Original: ");
        solution.printLevelOrder(null);

        TreeNode inverted3 = solution.invertTree(null);
        System.out.print("Inverted: ");
        solution.printLevelOrder(inverted3);

        // Edge case: Single node
        TreeNode singleNode = new TreeNode(1);
        System.out.println("\nSingle node:");
        System.out.print("Original: ");
        solution.printLevelOrder(singleNode);

        TreeNode invertedSingle = solution.invertTreeIterativeDFS(solution.copyTree(singleNode));
        System.out.print("Inverted: ");
        solution.printLevelOrder(invertedSingle);
    }

}
