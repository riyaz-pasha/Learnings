// Reverse Post Order
class FlattenBinaryTree {

    private TreeNode prev;

    public void flatten(TreeNode root) {
        if (root == null)
            return;

        flatten(root.right);
        flatten(root.left);

        root.right = prev;
        root.left = null;

        prev = root;
    }

}

/*
 * Flatten Binary Tree to Linked List — Explanation
 *
 * Goal:
 *   Convert a binary tree into a "linked list" using right pointers,
 *   following preorder traversal (root → left → right).
 *
 * Constraint:
 *   - Do it in-place.
 *   - All left pointers must be set to null.
 *
 * Example:
 *           1
 *         /   \
 *        2     5
 *       / \     \
 *      3   4     6
 *
 * Preorder Traversal: 1 → 2 → 3 → 4 → 5 → 6
 *
 * After Flattening (as linked list using right pointers):
 * 1
 *  \
 *   2
 *    \
 *     3
 *      \
 *       4
 *        \
 *         5
 *          \
 *           6
 *
 * Strategy:
 * =========
 * Use **reverse preorder traversal**: right → left → root
 *
 * Why reverse?
 *  - Because we process the list from tail to head (last to first),
 *    and build it in-place using a `prev` pointer to keep track of the
 *    previously processed node.
 *
 * Steps:
 * ======
 * 1. Recurse into the right subtree first.
 * 2. Then recurse into the left subtree.
 * 3. At each node:
 *      - Set node.right = prev (previously visited node)
 *      - Set node.left = null
 *      - Update prev = current node
 *
 * Example Trace:
 * --------------
 * Start recursion from node 1.
 *
 * - Visit 6 → right = null, left = null, prev = 6
 * - Visit 5 → right = 6, left = null, prev = 5
 * - Visit 4 → right = 5, left = null, prev = 4
 * - Visit 3 → right = 4, left = null, prev = 3
 * - Visit 2 → right = 3, left = null, prev = 2
 * - Visit 1 → right = 2, left = null, prev = 1
 *
 * Final Structure:
 * ----------------
 *   1
 *    \
 *     2
 *      \
 *       3
 *        \
 *         4
 *          \
 *           5
 *            \
 *             6
 *
 * Complexity:
 * ===========
 * Time: O(n) — Every node is visited once.
 * Space: O(h) — Due to recursion stack, where h is the height of the tree.
 */

class FlattenBinaryTree {
    public void flatten(TreeNode root) {
        if (root == null)
            return;

        flatten(root.left);
        flatten(root.right);

        TreeNode rightSubTree = root.right;

        root.right = root.left;
        root.left = null;

        TreeNode current = root;
        while (current.right != null) {
            current = current.right;
        }

        current.right = rightSubTree;
    }
}

/*
 * Flatten Binary Tree to Linked List — Explanation with Tracing
 *
 * Approach:
 * ---------
 * This version uses a **top-down recursive** strategy:
 *   - First flatten the left and right subtrees recursively.
 *   - Then move the left subtree to the right.
 *   - Finally, append the previously flattened right subtree to the end.
 *
 * Key Steps:
 * ----------
 * 1. Recursively flatten left and right subtrees.
 * 2. Store the original right subtree.
 * 3. Move the flattened left subtree to the right.
 * 4. Traverse to the end of this new right chain.
 * 5. Attach the original right subtree.
 *
 * Diagram - Original Tree:
 *         1
 *       /   \
 *      2     5
 *     / \     \
 *    3   4     6
 *
 * Step-by-step trace:
 * -------------------
 * 1. Flatten(3) and Flatten(4): They're leaves → nothing changes.
 * 2. At node 2:
 *    - root.left = 3-4 subtree → flattened to 3 → 4
 *    - root.right = 5 subtree (store it temporarily)
 *    - Move left to right: 2.right = 3, 2.left = null
 *    - Traverse to end: 3 → 4
 *    - Attach original right: 4.right = 5 subtree
 *
 *    Result so far:
 *        2
 *         \
 *          3
 *           \
 *            4
 *             \
 *              5
 *               \
 *                6
 *
 * 3. At node 1:
 *    - Flattened left (2 → 3 → 4 → 5 → 6), right is null
 *    - Move left to right: 1.right = 2, 1.left = null
 *    - Traverse to end: 1 → 2 → 3 → 4 → 5 → 6
 *    - Attach original right subtree (which is null here, so nothing to do)
 *
 * Final flattened tree:
 *    1
 *     \
 *      2
 *       \
 *        3
 *         \
 *          4
 *           \
 *            5
 *             \
 *              6
 *
 * Time Complexity: O(n²) in worst case due to repeated rightmost traversal
 * Space Complexity: O(h) due to recursion stack, h = height of tree
 */


// Morris
class FlattenBinaryTree {
    public void flatten(TreeNode root) {
        TreeNode current = root;
        while (current != null) {
            if (current.left != null) {
                TreeNode rightMost = current.left;
                while (rightMost.right != null) {
                    rightMost = rightMost.right;
                }

                rightMost.right = current.right;
                current.right = current.left;
                current.left = null;
            }
            current = current.right;
        }
    }
}

/*
 * Flatten Binary Tree to Linked List — Morris Traversal (O(n) time, O(1) space)
 *
 * Key Idea:
 * ---------
 * Use Morris Traversal to flatten the binary tree in-place without recursion or a stack.
 * This technique modifies the tree's structure temporarily to simulate traversal.
 *
 * Goal:
 *  - Rearrange the tree into a singly linked list (right pointers only) following preorder traversal.
 *
 * Tree Example:
 *         1
 *       /   \
 *      2     5
 *     / \     \
 *    3   4     6
 *
 * Preorder: 1 → 2 → 3 → 4 → 5 → 6
 *
 * Morris Strategy:
 * ----------------
 * For each node:
 *   1. If the node has a left child:
 *        a. Find the **rightmost node** in the left subtree.
 *        b. Connect that rightmost node's right pointer to the current node's right child.
 *        c. Move the left subtree to the right.
 *        d. Set left to null.
 *   2. Move to the next node using current.right.
 *
 * This effectively weaves the tree into a right-skewed linked list.
 *
 * Diagram - Step-by-step:
 * ------------------------
 * Start at node 1:
 *   - Left exists → find rightmost of 2 → 4
 *   - 4.right = 5 (original right)
 *   - 1.right = 2 (move left to right), 1.left = null
 *
 * Tree now looks like:
 *   1
 *    \
 *     2
 *    / \
 *   3   4
 *        \
 *         5
 *          \
 *           6
 *
 * Move to 2:
 *   - Left exists → rightmost of 3 → 3
 *   - 3.right = 4
 *   - 2.right = 3, 2.left = null
 *
 * Tree now:
 *   1
 *    \
 *     2
 *      \
 *       3
 *        \
 *         4
 *          \
 *           5
 *            \
 *             6
 *
 * Remaining nodes already follow the right-pointer-only chain.
 *
 * Final Linked List (flattened):
 *   1 → 2 → 3 → 4 → 5 → 6
 *
 * Complexity:
 * -----------
 * Time: O(n) — Each node is visited at most twice
 * Space: O(1) — No recursion or stack used
 *
 * This is the most efficient approach for in-place flattening.
 */
