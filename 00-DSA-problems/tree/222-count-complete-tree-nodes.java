/*
 * Given the root of a complete binary tree, return the number of the nodes in
 * the tree.
 * 
 * According to Wikipedia, every level, except possibly the last, is completely
 * filled in a complete binary tree, and all nodes in the last level are as far
 * left as possible. It can have between 1 and 2h nodes inclusive at the last
 * level h.
 * 
 * Design an algorithm that runs in less than O(n) time complexity.
 * 
 * Example 1:
 * Input: root = [1,2,3,4,5,6]
 * Output: 6
 * 
 * Example 2:
 * Input: root = []
 * Output: 0
 * 
 * Example 3:
 * Input: root = [1]
 * Output: 1
 */

class TreeNode {

    int val;
    TreeNode left;
    TreeNode right;

    TreeNode(int val) {
        this.val = val;
    }

}

/**
 * Solution 1: Optimal Approach Using Complete Tree Properties
 * 
 * Key Insight: In a complete binary tree, we can determine if a subtree
 * is a perfect binary tree by comparing left and right heights.
 * If heights are equal, it's perfect and we can calculate nodes using 2^h - 1.
 * 
 * Time Complexity: O(log²n) where n is number of nodes
 * Space Complexity: O(logn) for recursion stack
 */
class Solution {
    public int countNodes(TreeNode root) {
        if (root == null)
            return 0;

        int leftDepth = getLeftDepth(root);
        int rightDepth = getRightDepth(root);

        // If left and right depths are equal, it's a perfect binary tree
        if (leftDepth == rightDepth) {
            return (1 << leftDepth) - 1; // 2^depth - 1
        }

        // Otherwise, recursively count left and right subtrees
        return 1 + countNodes(root.left) + countNodes(root.right);
    }

    /**
     * Get depth by going only left (leftmost path in complete tree)
     */
    private int getLeftDepth(TreeNode root) {
        int depth = 0;
        while (root != null) {
            depth++;
            root = root.left;
        }
        return depth;
    }

    /**
     * Get depth by going only right (rightmost path in complete tree)
     */
    private int getRightDepth(TreeNode root) {
        int depth = 0;
        while (root != null) {
            depth++;
            root = root.right;
        }
        return depth;
    }
}

/**
 * Solution 2: Binary Search Approach on Last Level
 * 
 * Uses binary search to find the number of nodes in the last level.
 * First calculates the tree height, then binary searches for the
 * rightmost node in the last level.
 * 
 * Time Complexity: O(log²n)
 * Space Complexity: O(logn)
 */
class Solution2 {
    public int countNodes(TreeNode root) {
        if (root == null)
            return 0;

        // Get the height of the tree
        int height = getHeight(root);

        // If height is 1, only root exists
        if (height == 1)
            return 1;

        // Calculate nodes in all levels except the last: 2^(h-1) - 1
        int upperLevelsNodes = (1 << (height - 1)) - 1;

        // Binary search for the number of nodes in the last level
        int left = 1, right = 1 << (height - 1); // Max nodes possible in last level

        while (left <= right) {
            int mid = left + (right - left) / 2;

            if (nodeExists(root, height, mid)) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return upperLevelsNodes + right;
    }

    private int getHeight(TreeNode root) {
        int height = 0;
        while (root != null) {
            height++;
            root = root.left;
        }
        return height;
    }

    /**
     * Check if the nth node exists in the last level
     * Uses binary representation to navigate: 0 = left, 1 = right
     */
    private boolean nodeExists(TreeNode root, int height, int index) {
        int left = 1, right = 1 << (height - 1);

        for (int i = 0; i < height - 1; i++) {
            int mid = left + (right - left) / 2;

            if (index <= mid) {
                root = root.left;
                right = mid;
            } else {
                root = root.right;
                left = mid + 1;
            }
        }

        return root != null;
    }
}

/**
 * Solution 3: Detailed Implementation with Comments
 * 
 * Same as Solution 1 but with extensive comments explaining the logic
 */
class Solution3 {
    public int countNodes(TreeNode root) {
        if (root == null)
            return 0;

        // In a complete binary tree:
        // - All levels are filled except possibly the last
        // - Last level is filled from left to right

        int leftHeight = computeLeftHeight(root);
        int rightHeight = computeRightHeight(root);

        if (leftHeight == rightHeight) {
            // This is a perfect binary tree
            // Number of nodes = 2^height - 1
            return (1 << leftHeight) - 1;
        } else {
            // The tree is not perfect, so we need to recursively
            // count nodes in left and right subtrees
            return 1 + countNodes(root.left) + countNodes(root.right);
        }
    }

    /**
     * Compute height by traversing leftmost path
     * In a complete binary tree, this gives us the maximum depth
     */
    private int computeLeftHeight(TreeNode root) {
        int height = 0;
        while (root != null) {
            height++;
            root = root.left;
        }
        return height;
    }

    /**
     * Compute height by traversing rightmost path
     * In a complete binary tree, if this equals left height,
     * the tree is perfect (completely filled)
     */
    private int computeRightHeight(TreeNode root) {
        int height = 0;
        while (root != null) {
            height++;
            root = root.right;
        }
        return height;
    }
}

/**
 * Solution 4: Iterative Approach
 * 
 * Iterative version that avoids recursion
 */
class Solution4 {
    public int countNodes(TreeNode root) {
        if (root == null)
            return 0;

        int totalNodes = 0;

        while (root != null) {
            int leftDepth = getDepth(root, true);
            int rightDepth = getDepth(root, false);

            if (leftDepth == rightDepth) {
                // Perfect subtree - add all nodes and move to right subtree
                totalNodes += (1 << leftDepth) - 1;
                break;
            } else {
                // Not perfect - count current node and move to left subtree
                totalNodes += 1 << (rightDepth - 1);
                root = root.left;
            }
        }

        return totalNodes;
    }

    private int getDepth(TreeNode root, boolean goLeft) {
        int depth = 0;
        while (root != null) {
            depth++;
            root = goLeft ? root.left : root.right;
        }
        return depth;
    }
}

/**
 * Naive Solution for Comparison (O(n) time)
 * 
 * Simple recursive approach that visits every node
 * NOT the optimal solution but included for comparison
 */
class NaiveSolution {
    public int countNodes(TreeNode root) {
        if (root == null)
            return 0;
        return 1 + countNodes(root.left) + countNodes(root.right);
    }
}

/**
 * Test class with examples
 */
class TestCountNodes {
    public static void main(String[] args) {
        Solution solution = new Solution();

        // Example 1: [1,2,3,4,5,6]
        // 1
        // / \
        // 2 3
        // / \ /
        // 4 5 6
        TreeNode root1 = new TreeNode(1);
        root1.left = new TreeNode(2);
        root1.right = new TreeNode(3);
        root1.left.left = new TreeNode(4);
        root1.left.right = new TreeNode(5);
        root1.right.left = new TreeNode(6);

        System.out.println("Example 1 - Expected: 6, Got: " + solution.countNodes(root1));

        // Example 2: []
        System.out.println("Example 2 - Expected: 0, Got: " + solution.countNodes(null));

        // Example 3: [1]
        TreeNode root3 = new TreeNode(1);
        System.out.println("Example 3 - Expected: 1, Got: " + solution.countNodes(root3));

        // Additional test: Perfect binary tree
        // 1
        // / \
        // 2 3
        // / \ / \
        // 4 5 6 7
        TreeNode root4 = new TreeNode(1);
        root4.left = new TreeNode(2);
        root4.right = new TreeNode(3);
        root4.left.left = new TreeNode(4);
        root4.left.right = new TreeNode(5);
        root4.right.left = new TreeNode(6);
        root4.right.right = new TreeNode(7);

        System.out.println("Perfect tree - Expected: 7, Got: " + solution.countNodes(root4));

        // Performance comparison
        System.out.println("\nPerformance comparison:");
        long start = System.nanoTime();
        solution.countNodes(root1);
        long optimized = System.nanoTime() - start;

        start = System.nanoTime();
        new NaiveSolution().countNodes(root1);
        long naive = System.nanoTime() - start;

        System.out.println("Optimized approach: " + optimized + " ns");
        System.out.println("Naive approach: " + naive + " ns");
    }
}

/**
 * Complexity Analysis and Key Insights:
 * 
 * Why O(log²n) instead of O(n)?
 * 
 * 1. In each recursive call, we either:
 * a) Identify a perfect subtree and calculate nodes in O(1) using 2^h - 1
 * b) Eliminate one subtree and recurse on the other
 * 
 * 2. Height calculation takes O(log n) time
 * 3. We make at most O(log n) recursive calls
 * 4. Total: O(log n) calls × O(log n) height calculation = O(log²n)
 * 
 * Complete Binary Tree Properties Used:
 * - If left height == right height → perfect subtree
 * - All levels except last are completely filled
 * - Last level filled from left to right
 * - Height can be found by traversing leftmost path
 * 
 * Space Complexity: O(log n) due to recursion stack depth
 */
