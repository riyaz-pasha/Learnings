import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
/*
 * Given a binary tree, find the lowest common ancestor (LCA) of two given nodes
 * in the tree.
 * 
 * According to the definition of LCA on Wikipedia: “The lowest common ancestor
 * is defined between two nodes p and q as the lowest node in T that has both p
 * and q as descendants (where we allow a node to be a descendant of itself).”
 * 
 * Example 1:
 * Input: root = [3,5,1,6,2,0,8,null,null,7,4], p = 5, q = 1
 * Output: 3
 * Explanation: The LCA of nodes 5 and 1 is 3.
 * 
 * Example 2:
 * Input: root = [3,5,1,6,2,0,8,null,null,7,4], p = 5, q = 4
 * Output: 5
 * Explanation: The LCA of nodes 5 and 4 is 5, since a node can be a descendant
 * of itself according to the LCA definition.
 * 
 * Example 3:
 * Input: root = [1,2], p = 1, q = 2
 * Output: 1
 */

class LowetCommonAncestorBinaryTree {

    public TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {
        if (root == null) {
            return null;
        }
        if (root == p || root == q) {
            return root;
        }

        TreeNode left = lowestCommonAncestor(root.left, p, q);
        TreeNode right = lowestCommonAncestor(root.right, p, q);

        if (left != null && right != null) {
            return root;
        }

        return left != null ? left : right;
    }

}

/**
 * Definition for a binary tree node.
 */
class TreeNode {

    int val;
    TreeNode left;
    TreeNode right;

    TreeNode(int x) {
        val = x;
    }

}

/**
 * Solution 1: Recursive Approach (Most Elegant)
 * 
 * Key Insight: The LCA is the first node where p and q diverge.
 * If we find p or q, we return it. If both left and right subtrees
 * return non-null, current node is the LCA.
 * 
 * Time Complexity: O(n) where n is number of nodes
 * Space Complexity: O(h) where h is height of tree (recursion stack)
 */
class Solution {

    public TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {
        // Base case: if root is null or we found one of the target nodes
        if (root == null || root == p || root == q) {
            return root;
        }

        // Recursively search in left and right subtrees
        TreeNode left = lowestCommonAncestor(root.left, p, q);
        TreeNode right = lowestCommonAncestor(root.right, p, q);

        // If both left and right return non-null, current node is LCA
        if (left != null && right != null) {
            return root;
        }

        // Otherwise, return the non-null one (or null if both are null)
        return left != null ? left : right;
    }

}

/**
 * Solution 2: Iterative with Parent Pointers
 * 
 * First pass: Build parent pointers for all nodes
 * Second pass: Find all ancestors of p
 * Third pass: Traverse ancestors of q until we find common one
 * 
 * Time Complexity: O(n)
 * Space Complexity: O(n) for parent map and ancestors set
 */
class Solution2 {

    public TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {
        // Map to store parent pointers
        Map<TreeNode, TreeNode> parent = new HashMap<>();

        // Build parent pointers using iterative traversal
        Stack<TreeNode> stack = new Stack<>();
        parent.put(root, null);
        stack.push(root);

        // Continue until we find both p and q
        while (!parent.containsKey(p) || !parent.containsKey(q)) {
            TreeNode node = stack.pop();

            if (node.left != null) {
                parent.put(node.left, node);
                stack.push(node.left);
            }

            if (node.right != null) {
                parent.put(node.right, node);
                stack.push(node.right);
            }
        }

        // Get all ancestors of p
        Set<TreeNode> ancestors = new HashSet<>();
        while (p != null) {
            ancestors.add(p);
            p = parent.get(p);
        }

        // Find first common ancestor
        while (!ancestors.contains(q)) {
            q = parent.get(q);
        }

        return q;
    }

}

/**
 * Solution 3: Path-based Approach
 * 
 * Find paths from root to both nodes, then compare paths
 * to find the last common node.
 * 
 * Time Complexity: O(n)
 * Space Complexity: O(n) for paths
 */
class Solution3 {

    public TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {
        List<TreeNode> pathToP = new ArrayList<>();
        List<TreeNode> pathToQ = new ArrayList<>();

        // Find paths to both nodes
        findPath(root, p, pathToP);
        findPath(root, q, pathToQ);

        // Find last common node in both paths
        TreeNode lca = null;
        int minLength = Math.min(pathToP.size(), pathToQ.size());

        for (int i = 0; i < minLength; i++) {
            if (pathToP.get(i) == pathToQ.get(i)) {
                lca = pathToP.get(i);
            } else {
                break;
            }
        }

        return lca;
    }

    private boolean findPath(TreeNode root, TreeNode target, List<TreeNode> path) {
        if (root == null)
            return false;

        // Add current node to path
        path.add(root);

        // If target found, return true
        if (root == target)
            return true;

        // Search in left and right subtrees
        if (findPath(root.left, target, path) || findPath(root.right, target, path)) {
            return true;
        }

        // Backtrack: remove current node from path
        path.remove(path.size() - 1);
        return false;
    }

}

/**
 * Solution 4: Detailed Recursive with Comments
 * 
 * Same as Solution 1 but with extensive comments explaining the logic
 */
class Solution4 {

    public TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {
        // Base cases:
        // 1. If root is null, no LCA exists in this subtree
        // 2. If root is p or q, then root is a potential LCA
        if (root == null || root == p || root == q) {
            return root;
        }

        // Recursively search for LCA in left and right subtrees
        TreeNode leftLCA = lowestCommonAncestor(root.left, p, q);
        TreeNode rightLCA = lowestCommonAncestor(root.right, p, q);

        // Analyze the results:
        // Case 1: Both left and right subtrees return non-null
        // This means p and q are in different subtrees
        // Current root is their LCA
        if (leftLCA != null && rightLCA != null) {
            return root;
        }

        // Case 2: Only left subtree returns non-null
        // Both p and q are in left subtree
        // LCA is in left subtree
        if (leftLCA != null) {
            return leftLCA;
        }

        // Case 3: Only right subtree returns non-null
        // Both p and q are in right subtree
        // LCA is in right subtree
        if (rightLCA != null) {
            return rightLCA;
        }

        // Case 4: Both subtrees return null
        // Neither p nor q found in this subtree
        return null;
    }

}

/**
 * Solution 5: Optimized with Early Termination
 * 
 * Includes a flag to stop searching once both nodes are found
 */
class Solution5 {

    private boolean foundBoth = false;

    public TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {
        foundBoth = false;
        TreeNode result = findLCA(root, p, q);
        return foundBoth ? result : null;
    }

    private TreeNode findLCA(TreeNode root, TreeNode p, TreeNode q) {
        if (root == null)
            return null;

        // Check if current node is one of the target nodes
        boolean isCurrentTarget = (root == p || root == q);

        // Search in left and right subtrees
        TreeNode left = findLCA(root.left, p, q);
        TreeNode right = findLCA(root.right, p, q);

        // Count how many targets we've found
        int count = (isCurrentTarget ? 1 : 0) +
                (left != null ? 1 : 0) +
                (right != null ? 1 : 0);

        // If we found both targets, mark as found
        if (count == 2) {
            foundBoth = true;
        }

        // Return LCA logic
        if (left != null && right != null) {
            return root;
        }

        if (isCurrentTarget) {
            return root;
        }

        return left != null ? left : right;
    }

}

/**
 * Test class with examples
 */
class TestLCA {

    public static void main(String[] args) {
        Solution solution = new Solution();

        // Build the tree from Example 1: [3,5,1,6,2,0,8,null,null,7,4]
        // 3
        // / \
        // 5 1
        // / \ / \
        // 6 2 0 8
        // / \
        // 7 4

        TreeNode root = new TreeNode(3);
        root.left = new TreeNode(5);
        root.right = new TreeNode(1);
        root.left.left = new TreeNode(6);
        root.left.right = new TreeNode(2);
        root.right.left = new TreeNode(0);
        root.right.right = new TreeNode(8);
        root.left.right.left = new TreeNode(7);
        root.left.right.right = new TreeNode(4);

        TreeNode p = root.left; // Node 5
        TreeNode q = root.right; // Node 1

        System.out.println("Example 1:");
        System.out.println("LCA of " + p.val + " and " + q.val + " is: " +
                solution.lowestCommonAncestor(root, p, q).val); // Expected: 3

        // Example 2: p = 5, q = 4
        TreeNode p2 = root.left; // Node 5
        TreeNode q2 = root.left.right.right; // Node 4

        System.out.println("Example 2:");
        System.out.println("LCA of " + p2.val + " and " + q2.val + " is: " +
                solution.lowestCommonAncestor(root, p2, q2).val); // Expected: 5

        // Example 3: [1,2], p = 1, q = 2
        TreeNode root3 = new TreeNode(1);
        root3.left = new TreeNode(2);

        TreeNode p3 = root3; // Node 1
        TreeNode q3 = root3.left; // Node 2

        System.out.println("Example 3:");
        System.out.println("LCA of " + p3.val + " and " + q3.val + " is: " +
                solution.lowestCommonAncestor(root3, p3, q3).val); // Expected: 1

        // Test all solutions
        System.out.println("\nTesting all solutions:");
        testAllSolutions(root, p, q, 3);
        testAllSolutions(root, p2, q2, 5);
        testAllSolutions(root3, p3, q3, 1);
    }

    private static void testAllSolutions(TreeNode root, TreeNode p, TreeNode q, int expected) {
        Solution1 sol1 = new Solution1();
        Solution2 sol2 = new Solution2();
        Solution3 sol3 = new Solution3();

        System.out.println("Testing p=" + p.val + ", q=" + q.val + " (expected: " + expected + ")");
        System.out.println("Solution 1: " + sol1.lowestCommonAncestor(root, p, q).val);
        System.out.println("Solution 2: " + sol2.lowestCommonAncestor(root, p, q).val);
        System.out.println("Solution 3: " + sol3.lowestCommonAncestor(root, p, q).val);
        System.out.println();
    }

}

// Additional class definitions for testing
class Solution1 {

    public TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {
        if (root == null || root == p || root == q)
            return root;
        TreeNode left = lowestCommonAncestor(root.left, p, q);
        TreeNode right = lowestCommonAncestor(root.right, p, q);
        if (left != null && right != null)
            return root;
        return left != null ? left : right;
    }

}

/**
 * Step-by-step walkthrough for Example 1:
 * 
 * Tree: 3
 * / \
 * 5 1
 * /| |\
 * 6 2 0 8
 * /|
 * 7 4
 * 
 * Finding LCA of 5 and 1:
 * 
 * 1. Start at root (3)
 * 2. Search left subtree (5): finds 5, returns 5
 * 3. Search right subtree (1): finds 1, returns 1
 * 4. Both left and right return non-null, so 3 is LCA
 * 
 * Finding LCA of 5 and 4:
 * 
 * 1. Start at root (3)
 * 2. Search left subtree (5):
 * - At node 5: search left (6) returns null, search right (2)
 * - At node 2: search left (7) returns null, search right (4) returns 4
 * - Back at 5: left is null, right is 4, return 4
 * - But wait! Node 5 is also target, so return 5
 * 3. Search right subtree (1): returns null
 * 4. Left returns 5, right returns null, so return 5
 * 
 * Time Complexity: O(n) - visit each node at most once
 * Space Complexity: O(h) - recursion stack depth
 */
