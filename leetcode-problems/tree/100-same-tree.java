import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;
/*
 * Given the roots of two binary trees p and q, write a function to check if
 * they are the same or not.
 * 
 * Two binary trees are considered the same if they are structurally identical,
 * and the nodes have the same value.
 * 
 * Example 1:
 * Input: p = [1,2,3], q = [1,2,3]
 * Output: true
 * 
 * Example 2:
 * Input: p = [1,2], q = [1,null,2]
 * Output: false
 * 
 * Example 3:
 * Input: p = [1,2,1], q = [1,1,2]
 * Output: false
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

    // Approach 1: Recursive DFS (Most elegant)
    // Time: O(min(m,n)), Space: O(min(m,n)) for recursion stack
    public boolean isSameTree(TreeNode p, TreeNode q) {
        // Base cases
        if (p == null && q == null) {
            return true; // Both are null
        }

        if (p == null || q == null) {
            return false; // One is null, other is not
        }

        // Check current nodes and recursively check subtrees
        return (p.val == q.val) &&
                isSameTree(p.left, q.left) &&
                isSameTree(p.right, q.right);
    }

    // Approach 2: Iterative using Stack (DFS)
    // Time: O(min(m,n)), Space: O(min(m,n))
    public boolean isSameTreeIterativeDFS(TreeNode p, TreeNode q) {
        Stack<TreeNode> stack = new Stack<>();
        stack.push(p);
        stack.push(q);

        while (!stack.isEmpty()) {
            TreeNode node1 = stack.pop();
            TreeNode node2 = stack.pop();

            // Both null - continue
            if (node1 == null && node2 == null) {
                continue;
            }

            // One null, other not - return false
            if (node1 == null || node2 == null) {
                return false;
            }

            // Different values - return false
            if (node1.val != node2.val) {
                return false;
            }

            // Add children to stack
            stack.push(node1.left);
            stack.push(node2.left);
            stack.push(node1.right);
            stack.push(node2.right);
        }

        return true;
    }

    // Approach 3: Iterative using Queue (BFS)
    // Time: O(min(m,n)), Space: O(min(m,n))
    public boolean isSameTreeIterativeBFS(TreeNode p, TreeNode q) {
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(p);
        queue.offer(q);

        while (!queue.isEmpty()) {
            TreeNode node1 = queue.poll();
            TreeNode node2 = queue.poll();

            // Both null - continue
            if (node1 == null && node2 == null) {
                continue;
            }

            // One null, other not - return false
            if (node1 == null || node2 == null) {
                return false;
            }

            // Different values - return false
            if (node1.val != node2.val) {
                return false;
            }

            // Add children to queue
            queue.offer(node1.left);
            queue.offer(node2.left);
            queue.offer(node1.right);
            queue.offer(node2.right);
        }

        return true;
    }

    // Approach 4: Using preorder traversal strings (Less efficient but interesting)
    // Time: O(m+n), Space: O(m+n)
    public boolean isSameTreeString(TreeNode p, TreeNode q) {
        return preorderString(p).equals(preorderString(q));
    }

    private String preorderString(TreeNode root) {
        if (root == null) {
            return "null,";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(root.val).append(",");
        sb.append(preorderString(root.left));
        sb.append(preorderString(root.right));

        return sb.toString();
    }

    // Test method to demonstrate usage
    public static void main(String[] args) {
        Solution solution = new Solution();

        // Example 1: p = [1,2,3], q = [1,2,3] - Expected: true
        TreeNode p1 = new TreeNode(1);
        p1.left = new TreeNode(2);
        p1.right = new TreeNode(3);

        TreeNode q1 = new TreeNode(1);
        q1.left = new TreeNode(2);
        q1.right = new TreeNode(3);

        System.out.println("Example 1 - Recursive: " + solution.isSameTree(p1, q1)); // true
        System.out.println("Example 1 - Iterative DFS: " + solution.isSameTreeIterativeDFS(p1, q1)); // true
        System.out.println("Example 1 - Iterative BFS: " + solution.isSameTreeIterativeBFS(p1, q1)); // true

        // Example 2: p = [1,2], q = [1,null,2] - Expected: false
        TreeNode p2 = new TreeNode(1);
        p2.left = new TreeNode(2);

        TreeNode q2 = new TreeNode(1);
        q2.right = new TreeNode(2);

        System.out.println("Example 2 - Recursive: " + solution.isSameTree(p2, q2)); // false

        // Example 3: p = [1,2,1], q = [1,1,2] - Expected: false
        TreeNode p3 = new TreeNode(1);
        p3.left = new TreeNode(2);
        p3.right = new TreeNode(1);

        TreeNode q3 = new TreeNode(1);
        q3.left = new TreeNode(1);
        q3.right = new TreeNode(2);

        System.out.println("Example 3 - Recursive: " + solution.isSameTree(p3, q3)); // false

        // Edge cases
        System.out.println("Both null: " + solution.isSameTree(null, null)); // true
        System.out.println("One null: " + solution.isSameTree(new TreeNode(1), null)); // false

        // Single node comparison
        TreeNode single1 = new TreeNode(5);
        TreeNode single2 = new TreeNode(5);
        TreeNode single3 = new TreeNode(3);

        System.out.println("Same single nodes: " + solution.isSameTree(single1, single2)); // true
        System.out.println("Different single nodes: " + solution.isSameTree(single1, single3)); // false
    }

}
