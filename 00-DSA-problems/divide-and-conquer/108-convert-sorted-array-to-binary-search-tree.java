import java.util.*;
/*
 * Given an integer array nums where the elements are sorted in ascending order,
 * convert it to a height-balanced binary search tree.
 * 
 * Example 1:
 * Input: nums = [-10,-3,0,5,9]
 * Output: [0,-3,9,-10,null,5]
 * Explanation: [0,-10,5,null,-3,null,9] is also accepted:
 * 
 * Example 2:
 * Input: nums = [1,3]
 * Output: [3,1]
 * Explanation: [1,null,3] and [3,1] are both height-balanced BSTs.
 */

// Definition for a binary tree node
class TreeNode {
    int val;
    TreeNode left;
    TreeNode right;

    TreeNode() {
    }

    TreeNode(int val) {
        this.val = val;
    }

    TreeNode(int val, TreeNode left, TreeNode right) {
        this.val = val;
        this.left = left;
        this.right = right;
    }
}

// Solution 1: Basic Recursive Approach (Divide and Conquer)
class Solution1 {

    public TreeNode sortedArrayToBST(int[] nums) {
        if (nums == null || nums.length == 0) {
            return null;
        }
        return helper(nums, 0, nums.length - 1);
    }

    private TreeNode helper(int[] nums, int left, int right) {
        if (left > right) {
            return null;
        }

        // Always choose the middle element as root
        int mid = left + (right - left) / 2;
        TreeNode root = new TreeNode(nums[mid]);

        // Recursively build left and right subtrees
        root.left = helper(nums, left, mid - 1);
        root.right = helper(nums, mid + 1, right);

        return root;
    }

}

// Solution 2: Choose Left Middle for Consistent Results
class Solution2 {

    public TreeNode sortedArrayToBST(int[] nums) {
        return helper(nums, 0, nums.length - 1);
    }

    private TreeNode helper(int[] nums, int left, int right) {
        if (left > right) {
            return null;
        }

        // Always choose left middle to ensure consistent tree structure
        int mid = left + (right - left) / 2;
        TreeNode root = new TreeNode(nums[mid]);

        root.left = helper(nums, left, mid - 1);
        root.right = helper(nums, mid + 1, right);

        return root;
    }

}

// Solution 3: Choose Right Middle for Different Tree Structure
class Solution3 {

    public TreeNode sortedArrayToBST(int[] nums) {
        return helper(nums, 0, nums.length - 1);
    }

    private TreeNode helper(int[] nums, int left, int right) {
        if (left > right) {
            return null;
        }

        // Always choose right middle when array has even length
        int mid = left + (right - left + 1) / 2;
        TreeNode root = new TreeNode(nums[mid]);

        root.left = helper(nums, left, mid - 1);
        root.right = helper(nums, mid + 1, right);

        return root;
    }

}

// Solution 4: Iterative Approach using Stack
class Solution4 {

    public TreeNode sortedArrayToBST(int[] nums) {
        if (nums == null || nums.length == 0) {
            return null;
        }

        Stack<TreeNode> nodeStack = new Stack<>();
        Stack<Integer> leftStack = new Stack<>();
        Stack<Integer> rightStack = new Stack<>();

        int mid = nums.length / 2;
        TreeNode root = new TreeNode(nums[mid]);

        nodeStack.push(root);
        leftStack.push(0);
        rightStack.push(nums.length - 1);

        while (!nodeStack.isEmpty()) {
            TreeNode node = nodeStack.pop();
            int left = leftStack.pop();
            int right = rightStack.pop();

            int midIndex = left + (right - left) / 2;

            // Process left child
            if (left <= midIndex - 1) {
                int leftMid = left + (midIndex - 1 - left) / 2;
                node.left = new TreeNode(nums[leftMid]);
                nodeStack.push(node.left);
                leftStack.push(left);
                rightStack.push(midIndex - 1);
            }

            // Process right child
            if (midIndex + 1 <= right) {
                int rightMid = (midIndex + 1) + (right - (midIndex + 1)) / 2;
                node.right = new TreeNode(nums[rightMid]);
                nodeStack.push(node.right);
                leftStack.push(midIndex + 1);
                rightStack.push(right);
            }
        }

        return root;
    }

}

// Solution 5: Optimized with Preorder Construction
class Solution5 {

    private int index = 0;

    public TreeNode sortedArrayToBST(int[] nums) {
        index = 0;
        return preorderHelper(nums, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    private TreeNode preorderHelper(int[] nums, int min, int max) {
        if (index >= nums.length) {
            return null;
        }

        int val = nums[index];
        if (val < min || val > max) {
            return null;
        }

        index++;
        TreeNode root = new TreeNode(val);
        root.left = preorderHelper(nums, min, val);
        root.right = preorderHelper(nums, val, max);

        return root;
    }

}

// Solution 6: Random Middle Selection for Balanced Trees
class Solution6 {

    private Random random = new Random();

    public TreeNode sortedArrayToBST(int[] nums) {
        return helper(nums, 0, nums.length - 1);
    }

    private TreeNode helper(int[] nums, int left, int right) {
        if (left > right) {
            return null;
        }

        // Randomly choose between left and right middle for even-length arrays
        int mid;
        if ((right - left) % 2 == 0) {
            mid = left + (right - left) / 2;
        } else {
            mid = left + (right - left) / 2 + random.nextInt(2);
        }

        TreeNode root = new TreeNode(nums[mid]);
        root.left = helper(nums, left, mid - 1);
        root.right = helper(nums, mid + 1, right);

        return root;
    }

}

// Solution 7: Memory Optimized with Array Slicing Alternative
class Solution7 {

    public TreeNode sortedArrayToBST(int[] nums) {
        return helper(nums, 0, nums.length - 1);
    }

    private TreeNode helper(int[] nums, int start, int end) {
        if (start > end) {
            return null;
        }

        // Use bit manipulation to avoid potential overflow
        int mid = start + ((end - start) >> 1);

        TreeNode node = new TreeNode(nums[mid]);

        // Build subtrees in parallel conceptually
        node.left = helper(nums, start, mid - 1);
        node.right = helper(nums, mid + 1, end);

        return node;
    }

}

// Utility class for testing and tree visualization
class TreeUtils {

    // Level-order traversal to visualize the tree
    public static List<Integer> levelOrder(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        if (root == null)
            return result;

        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            TreeNode node = queue.poll();
            if (node != null) {
                result.add(node.val);
                queue.offer(node.left);
                queue.offer(node.right);
            } else {
                result.add(null);
            }
        }

        // Remove trailing nulls
        while (!result.isEmpty() && result.get(result.size() - 1) == null) {
            result.remove(result.size() - 1);
        }

        return result;
    }

    // Check if tree is height-balanced
    public static boolean isBalanced(TreeNode root) {
        return checkHeight(root) != -1;
    }

    private static int checkHeight(TreeNode node) {
        if (node == null)
            return 0;

        int leftHeight = checkHeight(node.left);
        if (leftHeight == -1)
            return -1;

        int rightHeight = checkHeight(node.right);
        if (rightHeight == -1)
            return -1;

        if (Math.abs(leftHeight - rightHeight) > 1) {
            return -1;
        }

        return Math.max(leftHeight, rightHeight) + 1;
    }

    // Validate BST property
    public static boolean isValidBST(TreeNode root) {
        return validateBST(root, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    private static boolean validateBST(TreeNode node, long min, long max) {
        if (node == null)
            return true;

        if (node.val <= min || node.val >= max) {
            return false;
        }

        return validateBST(node.left, min, node.val) &&
                validateBST(node.right, node.val, max);
    }

}

// Test class to demonstrate all solutions
class SortedArrayToBSTTest {

    public static void main(String[] args) {
        Solution1 sol1 = new Solution1();
        Solution2 sol2 = new Solution2();
        Solution3 sol3 = new Solution3();
        Solution4 sol4 = new Solution4();
        Solution7 sol7 = new Solution7();

        int[][] testCases = {
                { -10, -3, 0, 5, 9 },
                { 1, 3 },
                { 1, 2, 3, 4, 5, 6, 7 },
                { 1 }
        };

        for (int i = 0; i < testCases.length; i++) {
            int[] nums = testCases[i];
            System.out.println("Input: " + Arrays.toString(nums));

            TreeNode tree1 = sol1.sortedArrayToBST(nums);
            TreeNode tree2 = sol2.sortedArrayToBST(nums);
            TreeNode tree3 = sol3.sortedArrayToBST(nums);
            TreeNode tree4 = sol4.sortedArrayToBST(nums);
            TreeNode tree7 = sol7.sortedArrayToBST(nums);

            System.out.println("Solution 1: " + TreeUtils.levelOrder(tree1));
            System.out.println("Solution 2: " + TreeUtils.levelOrder(tree2));
            System.out.println("Solution 3: " + TreeUtils.levelOrder(tree3));
            System.out.println("Solution 4: " + TreeUtils.levelOrder(tree4));
            System.out.println("Solution 7: " + TreeUtils.levelOrder(tree7));

            // Validate all trees
            System.out.println("All trees balanced: " +
                    TreeUtils.isBalanced(tree1) + " " +
                    TreeUtils.isBalanced(tree2) + " " +
                    TreeUtils.isBalanced(tree3) + " " +
                    TreeUtils.isBalanced(tree4) + " " +
                    TreeUtils.isBalanced(tree7));

            System.out.println("All trees valid BST: " +
                    TreeUtils.isValidBST(tree1) + " " +
                    TreeUtils.isValidBST(tree2) + " " +
                    TreeUtils.isValidBST(tree3) + " " +
                    TreeUtils.isValidBST(tree4) + " " +
                    TreeUtils.isValidBST(tree7));

            System.out.println();
        }
    }

}
