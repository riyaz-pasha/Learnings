import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

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
    public TreeNode buildTree(int[] preorder, int[] inorder) {
        // Create a map for quick lookup of indices in inorder array
        Map<Integer, Integer> inorderMap = new HashMap<>();
        for (int i = 0; i < inorder.length; i++) {
            inorderMap.put(inorder[i], i);
        }

        return buildTreeHelper(preorder, 0, preorder.length - 1,
                inorder, 0, inorder.length - 1, inorderMap);
    }

    private TreeNode buildTreeHelper(int[] preorder, int preStart, int preEnd,
            int[] inorder, int inStart, int inEnd,
            Map<Integer, Integer> inorderMap) {
        if (preStart > preEnd || inStart > inEnd) {
            return null;
        }

        // Root is always the first element in preorder
        int rootVal = preorder[preStart];
        TreeNode root = new TreeNode(rootVal);

        // Find root position in inorder array
        int rootIdx = inorderMap.get(rootVal);

        // Calculate the size of left subtree
        int leftSubtreeSize = rootIdx - inStart;

        // Recursively build left and right subtrees
        root.left = buildTreeHelper(preorder, preStart + 1, preStart + leftSubtreeSize,
                inorder, inStart, rootIdx - 1, inorderMap);

        root.right = buildTreeHelper(preorder, preStart + leftSubtreeSize + 1, preEnd,
                inorder, rootIdx + 1, inEnd, inorderMap);

        return root;
    }

    // Solution 2: Using global index pointer (more space efficient)
    // Time: O(n), Space: O(n) for recursion stack + O(n) for hashmap
    private int preorderIdx = 0;

    public TreeNode buildTree2(int[] preorder, int[] inorder) {
        preorderIdx = 0; // Reset for multiple calls
        Map<Integer, Integer> inorderMap = new HashMap<>();
        for (int i = 0; i < inorder.length; i++) {
            inorderMap.put(inorder[i], i);
        }

        return buildTreeHelper2(preorder, inorderMap, 0, inorder.length - 1);
    }

    private TreeNode buildTreeHelper2(int[] preorder, Map<Integer, Integer> inorderMap,
            int inStart, int inEnd) {
        if (inStart > inEnd) {
            return null;
        }

        int rootVal = preorder[preorderIdx++];
        TreeNode root = new TreeNode(rootVal);

        int rootIdx = inorderMap.get(rootVal);

        // Build left subtree first (preorder: root -> left -> right)
        root.left = buildTreeHelper2(preorder, inorderMap, inStart, rootIdx - 1);
        root.right = buildTreeHelper2(preorder, inorderMap, rootIdx + 1, inEnd);

        return root;
    }

    // Solution 3: Without HashMap (less space efficient but educational)
    // Time: O(n²) in worst case, Space: O(n) for recursion stack only
    public TreeNode buildTree3(int[] preorder, int[] inorder) {
        return buildTreeHelper3(preorder, 0, preorder.length - 1,
                inorder, 0, inorder.length - 1);
    }

    private TreeNode buildTreeHelper3(int[] preorder, int preStart, int preEnd,
            int[] inorder, int inStart, int inEnd) {
        if (preStart > preEnd || inStart > inEnd) {
            return null;
        }

        int rootVal = preorder[preStart];
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

        root.left = buildTreeHelper3(preorder, preStart + 1, preStart + leftSubtreeSize,
                inorder, inStart, rootIdx - 1);

        root.right = buildTreeHelper3(preorder, preStart + leftSubtreeSize + 1, preEnd,
                inorder, rootIdx + 1, inEnd);

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

    // Test the solutions
    public static void main(String[] args) {
        Solution sol = new Solution();

        // Example 1
        int[] preorder1 = { 3, 9, 20, 15, 7 };
        int[] inorder1 = { 9, 3, 15, 20, 7 };

        System.out.println("Example 1:");
        System.out.println("Preorder: " + Arrays.toString(preorder1));
        System.out.println("Inorder: " + Arrays.toString(inorder1));

        TreeNode result1 = sol.buildTree(preorder1, inorder1);
        System.out.print("Result: ");
        printLevelOrder(result1);

        // Example 2
        int[] preorder2 = { -1 };
        int[] inorder2 = { -1 };

        System.out.println("\nExample 2:");
        System.out.println("Preorder: " + Arrays.toString(preorder2));
        System.out.println("Inorder: " + Arrays.toString(inorder2));

        TreeNode result2 = sol.buildTree(preorder2, inorder2);
        System.out.print("Result: ");
        printLevelOrder(result2);

        // Test edge case - single node
        int[] preorder3 = { 1, 2, 3 };
        int[] inorder3 = { 2, 1, 3 };

        System.out.println("\nExample 3:");
        System.out.println("Preorder: " + Arrays.toString(preorder3));
        System.out.println("Inorder: " + Arrays.toString(inorder3));

        TreeNode result3 = sol.buildTree(preorder3, inorder3);
        System.out.print("Result: ");
        printLevelOrder(result3);
    }

}

class ConstructBinaryTreeFromPreorderAndInorder {

    public TreeNode buildTree(int[] preorder, int[] inorder) {
        Map<Integer, Integer> inorderIndexMap = new HashMap<>();
        for (int i = 0; i < inorder.length; i++) {
            inorderIndexMap.put(inorder[i], i);
        }
        return this.build(preorder, 0, preorder.length - 1,
                inorder, 0, inorder.length - 1,
                inorderIndexMap);
    }

    private TreeNode build(int[] preorder, int preStart, int preEnd,
            int[] inorder, int inStart, int inEnd,
            Map<Integer, Integer> inorderIndexMap) {

        if (preStart > preEnd || inStart > inEnd) {
            return null;
        }

        int rootVal = preorder[preStart];
        int rootInorderIndex = inorderIndexMap.get(rootVal);
        int leftSize = rootInorderIndex - inStart;

        TreeNode root = new TreeNode(rootVal);

        root.left = this.build(preorder, preStart + 1, (preStart + 1) + (leftSize - 1),
                inorder, inStart, rootInorderIndex - 1,
                inorderIndexMap);

        root.right = this.build(preorder, (preStart + 1) + leftSize, preEnd,
                inorder, rootInorderIndex + 1, inEnd,
                inorderIndexMap);

        return root;
    }

}
