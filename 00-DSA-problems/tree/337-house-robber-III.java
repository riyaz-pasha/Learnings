class TreeNode {
    int val;
    TreeNode left;
    TreeNode right;

    TreeNode(int x) {
        val = x;
    }
}

class HouseRobberIII {

    public int rob(TreeNode root) {
        int[] result = robSubtree(root);
        return Math.max(result[0], result[1]);
    }

    // Returns an array of two elements:
    // result[0] = max amount if we do NOT rob current node
    // result[1] = max amount if we DO rob current node
    private int[] robSubtree(TreeNode node) {
        if (node == null)
            return new int[2];

        int[] left = robSubtree(node.left);
        int[] right = robSubtree(node.right);

        int[] result = new int[2];

        // If we don't rob current node, we can choose to rob or not rob its children
        result[0] = Math.max(left[0], left[1]) + Math.max(right[0], right[1]);

        // If we rob current node, we cannot rob its children
        result[1] = node.val + left[0] + right[0];

        return result;
    }

}

/*
 * Tree:
 *
 *           4
 *         /   \
 *        1     5
 *       / \   / \
 *      2   3 6   1
 *     /         \
 *    1           1
 *
 * Let's trace the recursion and dynamic programming at each node.
 *
 * Format per node:
 * Node(value) -> [maxIfNotRobbed, maxIfRobbed]
 * 
 * ----------------------------------------------
 * Leaf nodes:
 * Node(1) [leftmost bottom under 2]:
 * - Left, Right = null ⇒ [0, 0]
 * - Rob = 1 + 0 + 0 = 1
 * - Not rob = max(0, 0) + max(0, 0) = 0
 * → Node(1) ⇒ [0, 1]
 * 
 * ----------------------------------------------
 * Node(2):
 * - Left = Node(1) ⇒ [0, 1]
 * - Right = null ⇒ [0, 0]
 * - Rob = 2 + 0 + 0 = 2
 * - Not rob = max(0,1) + 0 = 1
 * → Node(2) ⇒ [1, 2]
 * 
 * ----------------------------------------------
 * Node(3):
 * - Leaf → [0, 0] ⇒ Rob = 3, Not rob = 0
 * → Node(3) ⇒ [0, 3]
 * 
 * ----------------------------------------------
 * Node(1) (left child of root):
 * - Left = Node(2) ⇒ [1, 2]
 * - Right = Node(3) ⇒ [0, 3]
 * - Rob = 1 + 1 (left[0]) + 0 (right[0]) = 2
 * - Not rob = max(1,2) + max(0,3) = 2 + 3 = 5
 * → Node(1) ⇒ [5, 2]
 * 
 * ----------------------------------------------
 * Node(1) (bottom right leaf):
 * - Null children ⇒ [0, 0]
 * - Rob = 1, Not rob = 0
 * → Node(1) ⇒ [0, 1]
 * 
 * ----------------------------------------------
 * Node(6):
 * - Leaf → [0, 0] ⇒ Rob = 6, Not rob = 0
 * → Node(6) ⇒ [0, 6]
 * 
 * ----------------------------------------------
 * Node(5) (right child of root):
 * - Left = Node(6) ⇒ [0, 6]
 * - Right = Node(1) ⇒ [0, 1]
 * - Rob = 5 + 0 + 0 = 5
 * - Not rob = max(0,6) + max(0,1) = 6 + 1 = 7
 * → Node(5) ⇒ [7, 5]
 * 
 * ----------------------------------------------
 * Root Node(4):
 * - Left = Node(1) ⇒ [5, 2]
 * - Right = Node(5) ⇒ [7, 5]
 * - Rob = 4 + 5 (left[0]) + 7 (right[0]) = 16
 * - Not rob = max(5,2) + max(7,5) = 5 + 7 = 12
 * → Root(4) ⇒ [12, 16]
 *
 * Final answer = max(12, 16) = 16
 *
 * ----------------------------------------------
 * Conclusion:
 * Maximum money that can be robbed from this tree without alerting the police is **16**.
 *
 * This tracing shows:
 * - How values flow up from leaves to root
 * - Robbing a node excludes its immediate children
 * - Not robbing allows freedom to choose optimal child combinations
 */

 
/*
 * This solution solves the "House Robber III" problem using post-order dynamic programming on a binary tree.
 * 
 * The idea is that at every node, we compute two values:
 *   - maxIfRobbed: Maximum money if we rob this node (can't rob its direct children)
 *   - maxIfNotRobbed: Maximum money if we skip this node (we can choose to rob or skip its children)
 *
 * These two values are returned in an array [maxIfNotRobbed, maxIfRobbed] for each node.
 *
 * ------------------------------------------------------------
 * Example of a more complex tree:
 *
 *           4
 *         /   \
 *        1     5
 *       / \   / \
 *      2   3 6   1
 *     /         \
 *    1           1
 *
 * This tree includes:
 * - Nodes with only one child
 * - Deep nested child (node 1 under node 2)
 * - A leaf node under right-most node (1 under 1)
 * 
 * We apply post-order traversal so we calculate left and right subtree results first before processing a node.
 *
 * At each node:
 * - If we rob it, we must add its value + maxIfNotRobbed of both children
 * - If we don't rob it, we can choose max of robbing or not robbing each child
 *
 * ------------------------------------------------------------
 * Edge Cases Covered:
 *
 * 1. Empty Tree:
 *    - If the root is null, we return 0.
 *
 * 2. Single Node Tree:
 *    - If the tree has only one node, we return its value.
 *
 * 3. Linear Tree (linked-list style):
 *    - Works regardless of skewed left or right.
 *    - Ensures alternate nodes are chosen to avoid parent-child conflict.
 *
 * 4. Full Binary Tree:
 *    - All levels completely filled; ensures optimal substructure is respected.
 *
 * 5. Sparse Tree:
 *    - Handles nodes with only one child (left or right).
 *    - Ensures we don’t assume both children always exist.
 *
 * 6. Negative Node Values:
 *    - Although not in original problem description, if node values can be negative,
 *      algorithm still works because it chooses max between rob and not rob.
 *
 * ------------------------------------------------------------
 * Why This Works:
 *
 * Dynamic programming stores two choices per node and makes an optimal decision by combining subtree results.
 * Because the decision to rob a node depends on whether its children were robbed,
 * this bottom-up DP ensures no parent-child pair is robbed together.
 *
 * Time Complexity: O(n) — we visit each node once.
 * Space Complexity: O(h) — recursion stack, where h is the tree height.
 */
