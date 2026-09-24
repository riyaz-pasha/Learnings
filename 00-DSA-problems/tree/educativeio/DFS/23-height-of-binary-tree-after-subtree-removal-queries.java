import java.util.*;

/**
 * Definition for TreeNode
 */
class TreeNode {
    int val;
    TreeNode left, right;

    TreeNode(int val) {
        this.val = val;
    }
}

public class HeightAfterSubtreeRemoval {

    // Stores subtree height of each node
    private Map<Integer, Integer> height = new HashMap<>();

    // Stores final answer for each node
    private Map<Integer, Integer> answer = new HashMap<>();

    /**
     * MAIN FUNCTION
     */
    public int[] treeQueries(TreeNode root, int[] queries) {

        // Step 1: Compute subtree heights
        computeHeight(root);

        // Step 2: Compute answers using rerooting
        computeAnswer(root, 0, 0);

        // Build result
        int[] result = new int[queries.length];
        for (int i = 0; i < queries.length; i++) {
            result[i] = answer.get(queries[i]);
        }

        return result;
    }

    /**
     * STEP 1: Compute subtree heights (Postorder)
     *
     * height[node] = max(height[left], height[right]) + 1
     *
     * Time: O(n)
     */
    private int computeHeight(TreeNode node) {
        if (node == null) return -1; // height in terms of edges

        int left = computeHeight(node.left);
        int right = computeHeight(node.right);

        int currHeight = Math.max(left, right) + 1;
        height.put(node.val, currHeight);

        return currHeight;
    }

    /**
     * STEP 2: Rerooting / DFS
     *
     * depth → current node depth
     * maxHeightFromParent → best height excluding this subtree
     *
     * answer[node] = maxHeightFromParent
     *
     * Time: O(n)
     */
    private void computeAnswer(TreeNode node, int depth, int maxHeightFromParent) {
        if (node == null) return;

        // Store result for this node
        answer.put(node.val, maxHeightFromParent);

        // Heights of children
        int leftHeight = node.left != null ? height.get(node.left.val) : -1;
        int rightHeight = node.right != null ? height.get(node.right.val) : -1;

        /**
         * For LEFT child:
         * We exclude left subtree, so consider:
         * 1. maxHeightFromParent
         * 2. path via right subtree
         */
        if (node.left != null) {
            int candidateFromRight = depth + 1 + rightHeight;

            int newMax = Math.max(maxHeightFromParent, candidateFromRight);

            computeAnswer(node.left, depth + 1, newMax);
        }

        /**
         * For RIGHT child:
         * We exclude right subtree, so consider:
         * 1. maxHeightFromParent
         * 2. path via left subtree
         */
        if (node.right != null) {
            int candidateFromLeft = depth + 1 + leftHeight;

            int newMax = Math.max(maxHeightFromParent, candidateFromLeft);

            computeAnswer(node.right, depth + 1, newMax);
        }
    }
}

