import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Stack;
/*
 * Given the root of a binary tree, check whether it is a mirror of itself
 * (i.e., symmetric around its center).
 * 
 * Example 1:
 * Input: root = [1,2,2,3,4,4,3]
 * Output: true
 * 
 * Example 2:
 * Input: root = [1,2,2,null,3,null,3]
 * Output: false
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

    // Approach 1: Recursive DFS (Most elegant)
    // Time: O(n), Space: O(h) where h is height of tree
    public boolean isSymmetric(TreeNode root) {
        if (root == null) {
            return true;
        }

        return isMirror(root.left, root.right);
    }

    private boolean isMirror(TreeNode left, TreeNode right) {
        // Both null - symmetric
        if (left == null && right == null) {
            return true;
        }

        // One null, other not - not symmetric
        if (left == null || right == null) {
            return false;
        }

        // Check if current nodes have same value and their children are mirrors
        return (left.val == right.val) &&
                isMirror(left.left, right.right) && // left's left with right's right
                isMirror(left.right, right.left); // left's right with right's left
    }

    // Approach 2: Iterative using Queue (BFS)
    // Time: O(n), Space: O(w) where w is maximum width of tree
    public boolean isSymmetricIterativeBFS(TreeNode root) {
        if (root == null) {
            return true;
        }

        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root.left);
        queue.offer(root.right);

        while (!queue.isEmpty()) {
            TreeNode left = queue.poll();
            TreeNode right = queue.poll();

            // Both null - continue
            if (left == null && right == null) {
                continue;
            }

            // One null or different values - not symmetric
            if (left == null || right == null || left.val != right.val) {
                return false;
            }

            // Add children in mirror order
            queue.offer(left.left); // left's left
            queue.offer(right.right); // right's right
            queue.offer(left.right); // left's right
            queue.offer(right.left); // right's left
        }

        return true;
    }

    // Approach 3: Iterative using Stack (DFS)
    // Time: O(n), Space: O(h)
    public boolean isSymmetricIterativeDFS(TreeNode root) {
        if (root == null) {
            return true;
        }

        Stack<TreeNode> stack = new Stack<>();
        stack.push(root.left);
        stack.push(root.right);

        while (!stack.isEmpty()) {
            TreeNode right = stack.pop();
            TreeNode left = stack.pop();

            // Both null - continue
            if (left == null && right == null) {
                continue;
            }

            // One null or different values - not symmetric
            if (left == null || right == null || left.val != right.val) {
                return false;
            }

            // Add children in mirror order
            stack.push(left.left);
            stack.push(right.right);
            stack.push(left.right);
            stack.push(right.left);
        }

        return true;
    }

    // Approach 4: Using level order traversal comparison
    // Time: O(n), Space: O(w)
    public boolean isSymmetricLevelOrder(TreeNode root) {
        if (root == null) {
            return true;
        }

        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            int size = queue.size();
            List<Integer> level = new ArrayList<>();
            List<TreeNode> nextLevel = new ArrayList<>();

            // Process current level
            for (int i = 0; i < size; i++) {
                TreeNode node = queue.poll();

                if (node != null) {
                    level.add(node.val);
                    nextLevel.add(node.left);
                    nextLevel.add(node.right);
                } else {
                    level.add(null);
                }
            }

            // Check if current level is palindrome
            if (!isPalindrome(level)) {
                return false;
            }

            // Add next level nodes to queue
            boolean hasNonNull = false;
            for (TreeNode node : nextLevel) {
                queue.offer(node);
                if (node != null) {
                    hasNonNull = true;
                }
            }

            // If no more non-null nodes, we're done
            if (!hasNonNull) {
                break;
            }
        }

        return true;
    }

    private boolean isPalindrome(List<Integer> list) {
        int left = 0, right = list.size() - 1;
        while (left < right) {
            if (!Objects.equals(list.get(left), list.get(right))) {
                return false;
            }
            left++;
            right--;
        }
        return true;
    }

    // Approach 5: Inorder traversal comparison (Alternative approach)
    // Note: This approach has limitations and doesn't work for all cases
    // Included for educational purposes but not recommended
    public boolean isSymmetricInorder(TreeNode root) {
        if (root == null) {
            return true;
        }

        List<Integer> leftInorder = new ArrayList<>();
        List<Integer> rightInorder = new ArrayList<>();

        inorderTraversal(root.left, leftInorder);
        reverseInorderTraversal(root.right, rightInorder);

        return leftInorder.equals(rightInorder);
    }

    private void inorderTraversal(TreeNode node, List<Integer> result) {
        if (node == null) {
            result.add(null); // Important to add null for structure
            return;
        }

        inorderTraversal(node.left, result);
        result.add(node.val);
        inorderTraversal(node.right, result);
    }

    private void reverseInorderTraversal(TreeNode node, List<Integer> result) {
        if (node == null) {
            result.add(null);
            return;
        }

        reverseInorderTraversal(node.right, result);
        result.add(node.val);
        reverseInorderTraversal(node.left, result);
    }

    // Helper method to print tree structure for testing
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

    // Test method to demonstrate usage
    public static void main(String[] args) {
        Solution solution = new Solution();

        // Example 1: root = [1,2,2,3,4,4,3] - Expected: true
        // 1
        // / \
        // 2 2
        // / \ / \
        // 3 4 4 3
        TreeNode root1 = new TreeNode(1);
        root1.left = new TreeNode(2);
        root1.right = new TreeNode(2);
        root1.left.left = new TreeNode(3);
        root1.left.right = new TreeNode(4);
        root1.right.left = new TreeNode(4);
        root1.right.right = new TreeNode(3);

        System.out.println("Example 1:");
        System.out.print("Tree: ");
        solution.printLevelOrder(root1);
        System.out.println("Recursive: " + solution.isSymmetric(root1)); // true
        System.out.println("Iterative BFS: " + solution.isSymmetricIterativeBFS(root1)); // true
        System.out.println("Iterative DFS: " + solution.isSymmetricIterativeDFS(root1)); // true
        System.out.println("Level Order: " + solution.isSymmetricLevelOrder(root1)); // true

        // Example 2: root = [1,2,2,null,3,null,3] - Expected: false
        // 1
        // / \
        // 2 2
        // \ \
        // 3 3
        TreeNode root2 = new TreeNode(1);
        root2.left = new TreeNode(2);
        root2.right = new TreeNode(2);
        root2.left.right = new TreeNode(3);
        root2.right.right = new TreeNode(3);

        System.out.println("\nExample 2:");
        System.out.print("Tree: ");
        solution.printLevelOrder(root2);
        System.out.println("Recursive: " + solution.isSymmetric(root2)); // false
        System.out.println("Iterative BFS: " + solution.isSymmetricIterativeBFS(root2)); // false

        // Edge cases
        System.out.println("\nEdge Cases:");
        System.out.println("Null tree: " + solution.isSymmetric(null)); // true

        TreeNode singleNode = new TreeNode(1);
        System.out.println("Single node: " + solution.isSymmetric(singleNode)); // true

        // Asymmetric single child
        TreeNode asymmetric = new TreeNode(1);
        asymmetric.left = new TreeNode(2);
        System.out.print("Asymmetric tree: ");
        solution.printLevelOrder(asymmetric);
        System.out.println("Result: " + solution.isSymmetric(asymmetric)); // false

        // Perfect symmetric tree
        TreeNode symmetric = new TreeNode(1);
        symmetric.left = new TreeNode(2);
        symmetric.right = new TreeNode(2);
        System.out.print("Simple symmetric tree: ");
        solution.printLevelOrder(symmetric);
        System.out.println("Result: " + solution.isSymmetric(symmetric)); // true
    }

}
