import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;

/*
 * Given two integer arrays inorder and postorder where inorder is the inorder
 * traversal of a binary tree and postorder is the postorder traversal of the
 * same tree, construct and return the binary tree.
 * 
 * Example 1:
 * Input: inorder = [9,3,15,20,7], postorder = [9,15,7,20,3]
 * Output: [3,9,20,null,null,15,7]
 * 
 * Example 2:
 * Input: inorder = [-1], postorder = [-1]
 * Output: [-1]
 */

class ConstructBinaryTreeFromPostorderAndInorder {

    public TreeNode buildTree(int[] inorder, int[] postorder) {
        Map<Integer, Integer> inorderIndexMap = new HashMap<>();
        for (int i = 0; i < inorder.length; i++) {
            inorderIndexMap.put(inorder[i], i);
        }

        return this.build(postorder, 0, postorder.length - 1,
                inorder, 0, inorder.length - 1,
                inorderIndexMap);
    }

    private TreeNode build(int[] postorder, int postStart, int postEnd,
            int[] inorder, int inStart, int inEnd,
            Map<Integer, Integer> inorderIndexMap) {

        if (postStart > postEnd || inStart > inEnd) {
            return null;
        }

        int rootVal = postorder[postEnd];
        int rootInorderIndex = inorderIndexMap.get(rootVal);
        int leftSize = rootInorderIndex - inStart;

        TreeNode root = new TreeNode(rootVal);

        root.left = this.build(postorder, postStart, postStart + (leftSize - 1),
                inorder, inStart, rootInorderIndex - 1,
                inorderIndexMap);

        root.right = this.build(postorder, postStart + leftSize, postEnd - 1,
                inorder, rootInorderIndex + 1, inEnd,
                inorderIndexMap);

        return root;
    }

}

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

    // Solution 1: Recursive approach with HashMap for O(1) lookups
    // Time: O(n), Space: O(n)
    public TreeNode buildTree(int[] inorder, int[] postorder) {
        // Create a map for quick lookup of indices in inorder array
        Map<Integer, Integer> inorderMap = new HashMap<>();
        for (int i = 0; i < inorder.length; i++) {
            inorderMap.put(inorder[i], i);
        }

        return buildTreeHelper(inorder, 0, inorder.length - 1,
                postorder, 0, postorder.length - 1, inorderMap);
    }

    private TreeNode buildTreeHelper(int[] inorder, int inStart, int inEnd,
            int[] postorder, int postStart, int postEnd,
            Map<Integer, Integer> inorderMap) {
        if (inStart > inEnd || postStart > postEnd) {
            return null;
        }

        // Root is always the last element in postorder
        int rootVal = postorder[postEnd];
        TreeNode root = new TreeNode(rootVal);

        // Find root position in inorder array
        int rootIdx = inorderMap.get(rootVal);

        // Calculate the size of left subtree
        int leftSubtreeSize = rootIdx - inStart;

        // Recursively build left and right subtrees
        // In postorder: Left -> Right -> Root
        root.left = buildTreeHelper(inorder, inStart, rootIdx - 1,
                postorder, postStart, postStart + leftSubtreeSize - 1,
                inorderMap);

        root.right = buildTreeHelper(inorder, rootIdx + 1, inEnd,
                postorder, postStart + leftSubtreeSize, postEnd - 1,
                inorderMap);

        return root;
    }

    // Solution 2: Using global index pointer (traversing postorder backwards)
    // Time: O(n), Space: O(n) for recursion stack + O(n) for hashmap
    private int postorderIdx;

    public TreeNode buildTree2(int[] inorder, int[] postorder) {
        postorderIdx = postorder.length - 1; // Start from the end
        Map<Integer, Integer> inorderMap = new HashMap<>();
        for (int i = 0; i < inorder.length; i++) {
            inorderMap.put(inorder[i], i);
        }

        return buildTreeHelper2(postorder, inorderMap, 0, inorder.length - 1);
    }

    private TreeNode buildTreeHelper2(int[] postorder, Map<Integer, Integer> inorderMap,
            int inStart, int inEnd) {
        if (inStart > inEnd) {
            return null;
        }

        int rootVal = postorder[postorderIdx--];
        TreeNode root = new TreeNode(rootVal);

        int rootIdx = inorderMap.get(rootVal);

        // Build right subtree first (postorder backwards: root -> right -> left)
        root.right = buildTreeHelper2(postorder, inorderMap, rootIdx + 1, inEnd);
        root.left = buildTreeHelper2(postorder, inorderMap, inStart, rootIdx - 1);

        return root;
    }

    // Solution 3: Stack-based iterative approach
    // Time: O(n), Space: O(n)
    public TreeNode buildTree3(int[] inorder, int[] postorder) {
        if (inorder.length == 0)
            return null;

        Stack<TreeNode> stack = new Stack<>();
        TreeNode root = new TreeNode(postorder[postorder.length - 1]);
        stack.push(root);

        int postIdx = postorder.length - 2;
        int inIdx = inorder.length - 1;

        while (postIdx >= 0) {
            TreeNode curr = new TreeNode(postorder[postIdx]);
            TreeNode parent = null;

            // Find the correct parent for current node
            while (!stack.isEmpty() && stack.peek().val == inorder[inIdx]) {
                parent = stack.pop();
                inIdx--;
            }

            if (parent != null) {
                parent.left = curr;
            } else {
                stack.peek().right = curr;
            }

            stack.push(curr);
            postIdx--;
        }

        return root;
    }

    // Solution 4: Without HashMap (less efficient but educational)
    // Time: O(n²) in worst case, Space: O(n) for recursion stack only
    public TreeNode buildTree4(int[] inorder, int[] postorder) {
        return buildTreeHelper4(inorder, 0, inorder.length - 1,
                postorder, 0, postorder.length - 1);
    }

    private TreeNode buildTreeHelper4(int[] inorder, int inStart, int inEnd,
            int[] postorder, int postStart, int postEnd) {
        if (inStart > inEnd || postStart > postEnd) {
            return null;
        }

        int rootVal = postorder[postEnd];
        TreeNode root = new TreeNode(rootVal);

        // Find root in inorder array (O(n) operation)
        int rootIdx = -1;
        for (int i = inStart; i <= inEnd; i++) {
            if (inorder[i] == rootVal) {
                rootIdx = i;
                break;
            }
        }

        int leftSubtreeSize = rootIdx - inStart;

        root.left = buildTreeHelper4(inorder, inStart, rootIdx - 1,
                postorder, postStart, postStart + leftSubtreeSize - 1);

        root.right = buildTreeHelper4(inorder, rootIdx + 1, inEnd,
                postorder, postStart + leftSubtreeSize, postEnd - 1);

        return root;
    }

    // Utility method to print tree in level order (for testing)
    public static void printLevelOrder(TreeNode root) {
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

    // Utility methods to verify our solution
    public static void printInorder(TreeNode root, List<Integer> result) {
        if (root == null)
            return;
        printInorder(root.left, result);
        result.add(root.val);
        printInorder(root.right, result);
    }

    public static void printPostorder(TreeNode root, List<Integer> result) {
        if (root == null)
            return;
        printPostorder(root.left, result);
        printPostorder(root.right, result);
        result.add(root.val);
    }

    // Test the solutions
    public static void main(String[] args) {
        Solution sol = new Solution();

        // Example 1
        int[] inorder1 = { 9, 3, 15, 20, 7 };
        int[] postorder1 = { 9, 15, 7, 20, 3 };

        System.out.println("Example 1:");
        System.out.println("Inorder: " + Arrays.toString(inorder1));
        System.out.println("Postorder: " + Arrays.toString(postorder1));

        TreeNode result1 = sol.buildTree(inorder1, postorder1);
        System.out.print("Result (level order): ");
        printLevelOrder(result1);

        // Verify the result
        List<Integer> inorderResult = new ArrayList<>();
        List<Integer> postorderResult = new ArrayList<>();
        printInorder(result1, inorderResult);
        printPostorder(result1, postorderResult);
        System.out.println("Verification - Inorder: " + inorderResult);
        System.out.println("Verification - Postorder: " + postorderResult);

        // Example 2
        int[] inorder2 = { -1 };
        int[] postorder2 = { -1 };

        System.out.println("\nExample 2:");
        System.out.println("Inorder: " + Arrays.toString(inorder2));
        System.out.println("Postorder: " + Arrays.toString(postorder2));

        TreeNode result2 = sol.buildTree2(inorder2, postorder2);
        System.out.print("Result (level order): ");
        printLevelOrder(result2);

        // Example 3 - More complex tree
        int[] inorder3 = { 4, 2, 5, 1, 6, 3, 7 };
        int[] postorder3 = { 4, 5, 2, 6, 7, 3, 1 };

        System.out.println("\nExample 3:");
        System.out.println("Inorder: " + Arrays.toString(inorder3));
        System.out.println("Postorder: " + Arrays.toString(postorder3));

        TreeNode result3 = sol.buildTree3(inorder3, postorder3);
        System.out.print("Result (level order): ");
        printLevelOrder(result3);

        // Verify the result
        List<Integer> inorderResult3 = new ArrayList<>();
        List<Integer> postorderResult3 = new ArrayList<>();
        printInorder(result3, inorderResult3);
        printPostorder(result3, postorderResult3);
        System.out.println("Verification - Inorder: " + inorderResult3);
        System.out.println("Verification - Postorder: " + postorderResult3);
    }

}
