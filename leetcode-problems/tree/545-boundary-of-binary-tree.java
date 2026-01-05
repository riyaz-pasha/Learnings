import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * LeetCode 545 - Boundary of Binary Tree
 *
 * Includes:
 * 1. Standard DFS Boundary Traversal (Recommended)
 * 2. Iterative Boundary Traversal
 */
class BoundaryOfBinaryTree {

    // ---------- TreeNode Definition ----------
    static class TreeNode {
        int val;
        TreeNode left, right;

        TreeNode(int val) {
            this.val = val;
        }
    }

    // =========================================================
    // 1️⃣ STANDARD / OPTIMAL SOLUTION (DFS)
    // =========================================================
    public List<Integer> boundaryOfBinaryTree(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        if (root == null) return result;

        if (!isLeaf(root)) {
            result.add(root.val);
        }

        addLeftBoundary(root.left, result);
        addLeaves(root, result);
        addRightBoundary(root.right, result);

        return result;
    }

    private void addLeftBoundary(TreeNode node, List<Integer> res) {
        while (node != null) {
            if (!isLeaf(node)) {
                res.add(node.val);
            }
            node = (node.left != null) ? node.left : node.right;
        }
    }

    private void addRightBoundary(TreeNode node, List<Integer> res) {
        Stack<Integer> stack = new Stack<>();
        while (node != null) {
            if (!isLeaf(node)) {
                stack.push(node.val);
            }
            node = (node.right != null) ? node.right : node.left;
        }
        while (!stack.isEmpty()) {
            res.add(stack.pop());
        }
    }

    private void addLeaves(TreeNode node, List<Integer> res) {
        if (node == null) return;

        if (isLeaf(node)) {
            res.add(node.val);
            return;
        }

        addLeaves(node.left, res);
        addLeaves(node.right, res);
    }

    private boolean isLeaf(TreeNode node) {
        return node.left == null && node.right == null;
    }

    // =========================================================
    // 2️⃣ ITERATIVE VERSION (NO RECURSION FOR LEAVES)
    // =========================================================
    public List<Integer> boundaryOfBinaryTreeIterative(TreeNode root) {
        List<Integer> res = new ArrayList<>();
        if (root == null) return res;

        if (!isLeaf(root)) {
            res.add(root.val);
        }

        // Left boundary
        TreeNode cur = root.left;
        while (cur != null) {
            if (!isLeaf(cur)) res.add(cur.val);
            cur = (cur.left != null) ? cur.left : cur.right;
        }

        // Leaves using stack
        Stack<TreeNode> stack = new Stack<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            TreeNode node = stack.pop();
            if (isLeaf(node)) {
                if (node != root) res.add(node.val);
            } else {
                if (node.right != null) stack.push(node.right);
                if (node.left != null) stack.push(node.left);
            }
        }

        // Right boundary
        Stack<Integer> right = new Stack<>();
        cur = root.right;
        while (cur != null) {
            if (!isLeaf(cur)) right.push(cur.val);
            cur = (cur.right != null) ? cur.right : cur.left;
        }
        while (!right.isEmpty()) {
            res.add(right.pop());
        }

        return res;
    }

    // =========================================================
    // 3️⃣ MAIN METHOD (OPTIONAL – FOR LOCAL TESTING)
    // =========================================================
    public static void main(String[] args) {
        BoundaryOfBinaryTree sol = new BoundaryOfBinaryTree();

        TreeNode root = new TreeNode(1);
        root.right = new TreeNode(2);
        root.right.left = new TreeNode(3);
        root.right.right = new TreeNode(4);

        System.out.println(sol.boundaryOfBinaryTree(root));          // [1, 3, 4, 2]
        System.out.println(sol.boundaryOfBinaryTreeIterative(root)); // [1, 3, 4, 2]
    }
}
