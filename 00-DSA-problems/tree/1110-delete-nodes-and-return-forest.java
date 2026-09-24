import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/*
 * Given the root of a binary tree, each node in the tree has a distinct value.
 * 
 * After deleting all nodes with a value in to_delete, we are left with a forest
 * (a disjoint union of trees).
 * 
 * Return the roots of the trees in the remaining forest. You may return the
 * result in any order.
 * 
 * Example 1:
 * Input: root = [1,2,3,4,5,6,7], to_delete = [3,5]
 * Output: [[1,2,null,4],[6],[7]]
 * 
 * Example 2:
 * Input: root = [1,2,4,null,3], to_delete = [3]
 * Output: [[1,2,4]]
 */

class TreeNode {

    int val;
    TreeNode left;
    TreeNode right;

    TreeNode(int val) {
        this.val = val;
    }

}

class DeleteNodesReturnForest {

    /**
     * Solution 1: Post-order DFS with Set
     * Time: O(n), Space: O(n + h) where h is height
     */
    public List<TreeNode> delNodes1(TreeNode root, int[] to_delete) {
        Set<Integer> toDelete = new HashSet<>();
        for (int val : to_delete) {
            toDelete.add(val);
        }

        List<TreeNode> result = new ArrayList<>();

        // If root is not deleted, it becomes a tree in forest
        if (deleteHelper(root, toDelete, result) != null) {
            result.add(root);
        }

        return result;
    }

    private TreeNode deleteHelper(TreeNode node, Set<Integer> toDelete, List<TreeNode> result) {
        if (node == null)
            return null;

        // Post-order: process children first
        node.left = deleteHelper(node.left, toDelete, result);
        node.right = deleteHelper(node.right, toDelete, result);

        // If current node should be deleted
        if (toDelete.contains(node.val)) {
            // Add non-null children to result as new trees
            if (node.left != null)
                result.add(node.left);
            if (node.right != null)
                result.add(node.right);
            return null; // Delete current node
        }

        return node; // Keep current node
    }

    /**
     * Solution 2: DFS with parent tracking
     * More explicit about parent-child relationships
     * Time: O(n), Space: O(n + h)
     */
    public List<TreeNode> delNodes2(TreeNode root, int[] to_delete) {
        Set<Integer> toDelete = new HashSet<>();
        for (int val : to_delete) {
            toDelete.add(val);
        }

        List<TreeNode> result = new ArrayList<>();
        dfs(root, true, toDelete, result);
        return result;
    }

    private TreeNode dfs(TreeNode node, boolean isRoot, Set<Integer> toDelete, List<TreeNode> result) {
        if (node == null)
            return null;

        boolean shouldDelete = toDelete.contains(node.val);

        // If this node is a root (original root or child of deleted node) and not
        // deleted
        if (isRoot && !shouldDelete) {
            result.add(node);
        }

        // Children of deleted nodes become potential roots
        node.left = dfs(node.left, shouldDelete, toDelete, result);
        node.right = dfs(node.right, shouldDelete, toDelete, result);

        // Return null if node should be deleted, otherwise return the node
        return shouldDelete ? null : node;
    }

    /**
     * Solution 3: Iterative BFS approach
     * Uses queue to process nodes level by level
     * Time: O(n), Space: O(n)
     */
    public List<TreeNode> delNodes3(TreeNode root, int[] to_delete) {
        Set<Integer> toDelete = new HashSet<>();
        for (int val : to_delete) {
            toDelete.add(val);
        }

        List<TreeNode> result = new ArrayList<>();
        Queue<TreeNode> queue = new LinkedList<>();

        if (!toDelete.contains(root.val)) {
            result.add(root);
        }
        queue.offer(root);

        while (!queue.isEmpty()) {
            TreeNode node = queue.poll();

            if (node.left != null) {
                queue.offer(node.left);
                if (toDelete.contains(node.left.val)) {
                    node.left = null;
                } else if (toDelete.contains(node.val)) {
                    result.add(node.left);
                }
            }

            if (node.right != null) {
                queue.offer(node.right);
                if (toDelete.contains(node.right.val)) {
                    node.right = null;
                } else if (toDelete.contains(node.val)) {
                    result.add(node.right);
                }
            }
        }

        return result;
    }

    // Helper method to build tree from array (for testing)
    public static TreeNode buildTree(Integer[] arr) {
        if (arr == null || arr.length == 0 || arr[0] == null)
            return null;

        TreeNode root = new TreeNode(arr[0]);
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);

        int i = 1;
        while (!queue.isEmpty() && i < arr.length) {
            TreeNode node = queue.poll();

            if (i < arr.length && arr[i] != null) {
                node.left = new TreeNode(arr[i]);
                queue.offer(node.left);
            }
            i++;

            if (i < arr.length && arr[i] != null) {
                node.right = new TreeNode(arr[i]);
                queue.offer(node.right);
            }
            i++;
        }

        return root;
    }

    // Helper method to convert tree to array (for testing)
    public static List<Integer> treeToList(TreeNode root) {
        if (root == null)
            return new ArrayList<>();

        List<Integer> result = new ArrayList<>();
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

    // Test the solutions
    public static void main(String[] args) {
        DeleteNodesReturnForest solution = new DeleteNodesReturnForest();

        // Test Example 1: [1,2,3], delete [1]
        TreeNode root1 = buildTree(new Integer[] { 1, 2, 3 });
        List<TreeNode> result1 = solution.delNodes1(root1, new int[] { 1 });
        System.out.println("Example 1 Result:");
        for (TreeNode tree : result1) {
            System.out.println(treeToList(tree));
        }

        // Test Example 2: [1,2,3,4], delete [4]
        TreeNode root2 = buildTree(new Integer[] { 1, 2, 3, 4 });
        List<TreeNode> result2 = solution.delNodes1(root2, new int[] { 4 });
        System.out.println("\nExample 2 Result:");
        for (TreeNode tree : result2) {
            System.out.println(treeToList(tree));
        }

        // Test Example 3: Complete binary tree with 15 nodes, delete [3,5]
        TreeNode root3 = buildTree(new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 });
        List<TreeNode> result3 = solution.delNodes1(root3, new int[] { 3, 5 });
        System.out.println("\nExample 3 Result:");
        for (TreeNode tree : result3) {
            System.out.println(treeToList(tree));
        }
    }

}
