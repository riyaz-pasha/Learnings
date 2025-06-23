import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;
/*
 * Given the root of a binary tree, imagine yourself standing on the right side
 * of it, return the values of the nodes you can see ordered from top to bottom.
 * 
 * Example 1:
 * Input: root = [1,2,3,null,5,null,4]
 * Output: [1,3,4]
 * 
 * Example 2:
 * Input: root = [1,2,3,4,null,null,null,5]
 * Output: [1,3,4,5]
 * 
 * Example 3:
 * Input: root = [1,null,3]
 * Output: [1,3]
 * 
 * Example 4:
 * Input: root = []
 * Output: []
 */

// Definition for a binary tree node
class TreeNode {

    int val;
    TreeNode left;
    TreeNode right;

    TreeNode(int val) {
        this.val = val;
    }

}

class BinaryTreeRightSideView {

    // Solution 1: Level Order Traversal (BFS) - Most Intuitive
    // Time: O(n), Space: O(w) where w is max width of tree
    public List<Integer> rightSideView1(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        if (root == null)
            return result;

        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            int levelSize = queue.size();

            // Process all nodes at current level
            for (int i = 0; i < levelSize; i++) {
                TreeNode node = queue.poll();

                // Add the rightmost node of each level
                if (i == levelSize - 1) {
                    result.add(node.val);
                }

                // Add children for next level
                if (node.left != null)
                    queue.offer(node.left);
                if (node.right != null)
                    queue.offer(node.right);
            }
        }

        return result;
    }

    // Solution 2: DFS with Level Tracking - More Space Efficient
    // Time: O(n), Space: O(h) where h is height of tree
    public List<Integer> rightSideView2(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        dfs(root, 0, result);
        return result;
    }

    private void dfs(TreeNode node, int level, List<Integer> result) {
        if (node == null)
            return;

        // If this is the first node we visit at this level,
        // it's the rightmost node (due to our traversal order)
        if (level == result.size()) {
            result.add(node.val);
        }

        // Visit right subtree first, then left
        dfs(node.right, level + 1, result);
        dfs(node.left, level + 1, result);
    }

    // Solution 3: DFS with HashMap - Alternative approach
    // Time: O(n), Space: O(n)
    public List<Integer> rightSideView3(TreeNode root) {
        Map<Integer, Integer> rightmostValueAtDepth = new HashMap<>();
        int maxDepth = -1;

        Stack<TreeNode> nodeStack = new Stack<>();
        Stack<Integer> depthStack = new Stack<>();
        nodeStack.push(root);
        depthStack.push(0);

        while (!nodeStack.isEmpty()) {
            TreeNode node = nodeStack.pop();
            int depth = depthStack.pop();

            if (node != null) {
                maxDepth = Math.max(maxDepth, depth);

                // Update the rightmost value at this depth
                // Since we process right nodes after left nodes,
                // this ensures we keep the rightmost value
                rightmostValueAtDepth.put(depth, node.val);

                nodeStack.push(node.left);
                nodeStack.push(node.right);
                depthStack.push(depth + 1);
                depthStack.push(depth + 1);
            }
        }

        List<Integer> result = new ArrayList<>();
        for (int depth = 0; depth <= maxDepth; depth++) {
            result.add(rightmostValueAtDepth.get(depth));
        }

        return result;
    }

    // Solution 4: Optimized DFS - Cleaner version
    // Time: O(n), Space: O(h)
    public List<Integer> rightSideView4(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        dfsOptimized(root, 1, result);
        return result;
    }

    private void dfsOptimized(TreeNode node, int level, List<Integer> result) {
        if (node == null)
            return;

        // If this level hasn't been seen before, add this node
        if (result.size() < level) {
            result.add(node.val);
        }

        // Process right first to ensure rightmost nodes are captured
        dfsOptimized(node.right, level + 1, result);
        dfsOptimized(node.left, level + 1, result);
    }

    // Test method to demonstrate usage
    public static void main(String[] args) {
        BinaryTreeRightSideView solution = new BinaryTreeRightSideView();

        // Test Case 1: [1,2,3,null,5,null,4]
        TreeNode root1 = new TreeNode(1);
        root1.left = new TreeNode(2);
        root1.right = new TreeNode(3);
        root1.left.right = new TreeNode(5);
        root1.right.right = new TreeNode(4);

        System.out.println("Test 1 - BFS: " + solution.rightSideView1(root1));
        System.out.println("Test 1 - DFS: " + solution.rightSideView2(root1));
        // Output: [1, 3, 4]

        // Test Case 2: [1,2,3,4,null,null,null,5]
        TreeNode root2 = new TreeNode(1);
        root2.left = new TreeNode(2);
        root2.right = new TreeNode(3);
        root2.left.left = new TreeNode(4);
        root2.left.left.left = new TreeNode(5);

        System.out.println("Test 2 - BFS: " + solution.rightSideView1(root2));
        System.out.println("Test 2 - DFS: " + solution.rightSideView2(root2));
        // Output: [1, 3, 4, 5]

        // Test Case 3: [1,null,3]
        TreeNode root3 = new TreeNode(1);
        root3.right = new TreeNode(3);

        System.out.println("Test 3 - BFS: " + solution.rightSideView1(root3));
        System.out.println("Test 3 - DFS: " + solution.rightSideView2(root3));
        // Output: [1, 3]

        // Test Case 4: []
        System.out.println("Test 4 - Empty: " + solution.rightSideView1(null));
        // Output: []
    }

}

/*
 * Algorithm Explanations:
 * 
 * 1. BFS (Level Order Traversal):
 * - Use a queue to process nodes level by level
 * - For each level, keep track of the last (rightmost) node
 * - Add the rightmost node's value to result
 * - Time: O(n), Space: O(w) where w is maximum width
 * 
 * 2. DFS (Depth-First Search):
 * - Traverse right subtree first, then left
 * - Keep track of current level/depth
 * - Only add node to result if it's the first node seen at that level
 * - Since we go right first, first node at each level is rightmost
 * - Time: O(n), Space: O(h) where h is height
 * 
 * Key Insights:
 * - The rightmost node at each level is what we see from right side
 * - BFS naturally processes level by level
 * - DFS with right-first traversal ensures rightmost nodes are seen first
 * - Both approaches handle edge cases (empty tree, single node, etc.)
 * 
 * Complexity Analysis:
 * - All solutions have O(n) time complexity
 * - BFS uses O(w) space for queue (width of tree)
 * - DFS uses O(h) space for recursion stack (height of tree)
 * - In worst case: w = n/2, h = n (skewed tree)
 * - In best case: w = 1, h = log(n) (balanced tree)
 */